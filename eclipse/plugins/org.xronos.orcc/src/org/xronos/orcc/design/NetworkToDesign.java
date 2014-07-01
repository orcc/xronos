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

import net.sf.orcc.df.Network;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.lim.Design;
import org.xronos.orcc.design.visitors.DesignNetwork;

/**
 * This class transforms an Orcc {@link Network} Object to an OpenForge
 * {@link Design} Object
 * 
 * @author Endri Bezati
 */
public class NetworkToDesign {

	Design design;

	Network network;

	ResourceCache resourceCache;

	boolean schedulerInformation;

	public NetworkToDesign(Network network, ResourceCache resourceCache,
			boolean schedulerInformation) {
		this.network = network;
		this.resourceCache = resourceCache;
		design = new Design();
		this.schedulerInformation = schedulerInformation;
	}

	public Design buildDesign() {
		// Get Instance name
		String designName = network.getName();
		design.setIDLogical(designName);
		GenericJob job = EngineThread.getGenericJob();
		job.getOption(OptionRegistry.TOP_MODULE_NAME).setValue(
				design.getSearchLabel(), designName);

		DesignNetwork designVisitor = new DesignNetwork(design, resourceCache,
				schedulerInformation);
		designVisitor.doSwitch(network);

		return design;
	}

}
