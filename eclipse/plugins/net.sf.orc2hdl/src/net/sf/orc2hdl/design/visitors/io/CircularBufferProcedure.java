/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
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
package net.sf.orc2hdl.design.visitors.io;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Design;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;

/**
 * 
 * @author Endri Bezati
 * 
 */
public class CircularBufferProcedure extends DfVisitor<Void> {

	private Map<Port, CircularBuffer> circularBufferPortMap;

	private Design design;

	private List<Procedure> procedures;

	private ResourceCache resourceCache;

	public CircularBufferProcedure(Design design, ResourceCache resourceCache) {
		super();
		this.design = design;
		this.resourceCache = resourceCache;
		circularBufferPortMap = new HashMap<Port, CircularBuffer>();
	}

	@Override
	public Void caseActor(Actor actor) {
		// Get Input Ports
		for (Port port : actor.getInputs()) {
			Var buffer = resourceCache.getActorInputsVar(actor).get(port);
			Integer size = resourceCache.getActorMaxPortRepeat(actor).get(port);
			CircularBuffer circularBuffer = new CircularBuffer(port, buffer,
					size);
			circularBufferPortMap.put(port, circularBuffer);
			procedures.add(createReadProcedure(port, circularBuffer));
		}
		// Get Output Ports
		for (Port port : actor.getOutputs()) {
			Var buffer = resourceCache.getActorInputsVar(actor).get(port);
			Integer size = resourceCache.getActorMaxPortRepeat(actor).get(port);
			CircularBuffer circularBuffer = new CircularBuffer(port, buffer,
					size);
			circularBufferPortMap.put(port, circularBuffer);
			procedures.add(createWriteProcedure(port, circularBuffer));
		}
		return null;
	}

	private Procedure createReadProcedure(Port port,
			CircularBuffer circularBuffer) {
		String name = port.getName();
		Procedure read = IrFactory.eINSTANCE.createProcedure(
				"circularBufferRead_" + name, 0,
				IrFactory.eINSTANCE.createTypeVoid());
		BlockBasic blockRead = IrFactory.eINSTANCE.createBlockBasic();
		// Put the block to the procedure
		read.getBlocks().add(blockRead);
		return null;
	}

	private Procedure createWriteProcedure(Port port,
			CircularBuffer circularBuffer) {
		return null;
	}

}
