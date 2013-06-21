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

package org.xronos.orcc.design;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;

import org.xronos.openforge.app.Engine;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.JobHandlerAdapter;
import org.xronos.openforge.lim.Design;

/**
 * Creates an Engine with a given {@Design}
 * 
 * @author Endri Bezati
 * 
 */
public class DesignEngine extends Engine {

	Actor actor;

	Network network;

	ResourceCache resourceCache;

	GenericJob genJob;

	public DesignEngine(GenericJob genJob, Actor actor,
			ResourceCache resourceCache) {
		super(genJob);
		this.genJob = genJob;
		this.actor = actor;
		this.resourceCache = resourceCache;
		this.jobHandler = new JobHandlerAdapter("Forging: "
				+ actor.getSimpleName());
	}

	public DesignEngine(GenericJob genJob, Network network,
			ResourceCache resourceCache) {
		super(genJob);
		this.genJob = genJob;
		this.network = network;
		this.resourceCache = resourceCache;
		this.jobHandler = new JobHandlerAdapter("Forging: "
				+ actor.getSimpleName());
	}

	@Override
	public Design buildLim() {

		// Single file generation
		if (network != null) {
			NetworkToDesign networkToDesign = new NetworkToDesign(network,
					resourceCache);
			design = networkToDesign.buildDesign();
		}

		// Multiple file generation
		if (actor != null) {
			ActorToDesign instanceToDesign = new ActorToDesign(actor,
					resourceCache);
			design = instanceToDesign.buildDesign();
		}

		// Generate Project File
		// File projectFile = new File("/tmp/test.xml");
		// ForgeProjectWriter projectWriter = new
		// ForgeProjectWriter(projectFile,
		// genJob, this, true);
		// try {
		// projectWriter.write();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		return design;
	}

}
