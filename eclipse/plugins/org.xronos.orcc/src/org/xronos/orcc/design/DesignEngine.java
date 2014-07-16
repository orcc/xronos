/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */

package org.xronos.orcc.design;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;

import org.xronos.openforge.app.Engine;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.JobHandlerAdapter;
import org.xronos.openforge.lim.Design;
import org.xronos.orcc.forge.mapping.DesignActor;

/**
 * Creates an Engine with a given {@Design}
 * 
 * @author Endri Bezati
 * 
 */
public class DesignEngine extends Engine {

	private Actor actor;

	private Network network;

	private ResourceCache resourceCache;

	private GenericJob genJob;

	private boolean schedulerInformation;

	private boolean newLimGen = false;

	public DesignEngine(GenericJob genJob, Actor actor,
			ResourceCache resourceCache, boolean schedulerInformation) {
		super(genJob);
		this.genJob = genJob;
		this.actor = actor;
		this.resourceCache = resourceCache;
		jobHandler = new JobHandlerAdapter("Forging: " + actor.getSimpleName());
		this.schedulerInformation = schedulerInformation;
	}

	public DesignEngine(GenericJob genJob, Network network,
			ResourceCache resourceCache, boolean schedulerInformation) {
		super(genJob);
		this.genJob = genJob;
		this.network = network;
		this.resourceCache = resourceCache;
		jobHandler = new JobHandlerAdapter("Forging: "
				+ network.getSimpleName());
		this.schedulerInformation = schedulerInformation;
	}

	public DesignEngine(GenericJob genJob, Actor actor,
			boolean schedulerInformation) {
		super(genJob);
		this.genJob = genJob;
		this.actor = actor;
		jobHandler = new JobHandlerAdapter("Forging: " + actor.getSimpleName());
		this.schedulerInformation = schedulerInformation;
		this.newLimGen = true;
	}

	@Override
	public Design buildLim() {

		// Single file generation
		if (network != null) {
			NetworkToDesign networkToDesign = new NetworkToDesign(network,
					resourceCache, schedulerInformation);
			long t0 = System.currentTimeMillis();
			design = networkToDesign.buildDesign();
			long t1 = System.currentTimeMillis();
			System.out.println("- Orcc IR to LIM transformed in: "
					+ (float) (t1 - t0) / 1000 + "s");
		}

		if (newLimGen) {
			if(actor != null){
				long t0 = System.currentTimeMillis();
				design = new DesignActor().doSwitch(actor);
				long t1 = System.currentTimeMillis();
				System.out.println(actor.getName()+" :- Orcc IR to LIM transformed in: "
						+ (float) (t1 - t0) / 1000 + "s");
			}
		} else {
			// Multiple file generation
			if (actor != null) {
				ActorToDesign instanceToDesign = new ActorToDesign(actor,
						resourceCache, schedulerInformation);
				long t0 = System.currentTimeMillis();
				design = instanceToDesign.buildDesign();
				long t1 = System.currentTimeMillis();
				System.out.println("- Orcc IR to LIM transformed in: "
						+ (float) (t1 - t0) / 1000 + "s");
			}
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
