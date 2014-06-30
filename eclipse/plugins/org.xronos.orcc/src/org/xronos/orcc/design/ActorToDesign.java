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
 */

package org.xronos.orcc.design;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.util.Void;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.lim.Design;
import org.xronos.orcc.design.visitors.DesignActor;

/**
 * This class transforms an Orcc {@link Actor} Object to an OpenForge
 * {@link Design} Object
 * 
 * @author Endri Bezati
 */
public class ActorToDesign {
	Design design;
	Actor actor;
	ResourceCache resourceCache;
	boolean schedulerInformation;

	public ActorToDesign(Actor actor, ResourceCache resourceCache,
			boolean schedulerInformation) {
		this.actor = actor;
		this.resourceCache = resourceCache;
		design = new Design();
		this.schedulerInformation = schedulerInformation;
	}

	public Design buildDesign() {
		// Get Instance name
		String designName = actor.getName();
		design.setIDLogical(designName);
		GenericJob job = EngineThread.getGenericJob();
		job.getOption(OptionRegistry.TOP_MODULE_NAME).setValue(
				design.getSearchLabel(), designName);

		DesignActor designVisitor = new DesignActor(design, resourceCache,
				schedulerInformation);
		designVisitor.doSwitch(actor);

		// Optimization Flags
		new DfVisitor<Void>(new OptimizationFlags()).doSwitch(actor);
		return design;
	}
}
