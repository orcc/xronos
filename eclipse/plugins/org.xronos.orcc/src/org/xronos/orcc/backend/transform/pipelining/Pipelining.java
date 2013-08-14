/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package org.xronos.orcc.backend.transform.pipelining;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;

import org.xronos.orcc.backend.transform.pipelining.coloring.PipeliningOptimization;
import org.xronos.orcc.backend.transform.pipelining.coloring.TestBench;

/**
 * The pipelining engine transformation
 * 
 * @author Endri Bezati
 * 
 */
public class Pipelining extends DfVisitor<Void> {

	/**
	 * Define the time of a Stage
	 */
	private float stageTime;

	public Pipelining(float stageTime) {
		this.stageTime = stageTime;
	}

	@Override
	public Void caseAction(Action action) {
		// Apply iff the action has the xronos_pipeline tag
		if (action.hasAttribute("xronos_pipeline")) {
			// Get the Input and Output matrix of the operators found on the
			// BlockBasic of the action
			ExtractOperatorsIO opIO = new ExtractOperatorsIO();
			opIO.doSwitch(action.getBody());
			// opIO.printTablesForCTestbench();

			// Create the TestBench for this action
			TestBench tb = opIO.createTestBench(stageTime);

			// Create and run the PipelineOptimization
			String logPath = System.getProperty("user.home") + File.separator
					+ "Pipeline.txt";
			PipeliningOptimization pOptimization = new PipeliningOptimization(
					tb, logPath);

			pOptimization.run();

			// Create Actors
			int stages = pOptimization.getNbrStages();
			List<Actor> pipeActors = new ArrayList<Actor>();
			for (int i = 0; i < stages; i++) {
				PipelineActor pipelineActor = new PipelineActor(action, opIO,
						i, pOptimization.getStageInputs(i),
						pOptimization.getStageOutputs(i),
						pOptimization.getStageOperators(i));
				Actor actor = pipelineActor.getActor();
				pipeActors.add(actor);
			}

		}
		return null;
	}
}
