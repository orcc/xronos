/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */

package org.xronos.orcc.forge.mapping;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;

import org.xronos.openforge.frontend.slim.builder.ActionIOHandler;
import org.xronos.openforge.frontend.slim.builder.ActionIOHandler.FifoIOHandler;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.io.FifoID;

/**
 * This visitor constructs the actors {@link Design} IO {@link ActionIOHandler}
 * 
 * @author Endri Bezati
 * 
 */
public class DesignPorts extends DfVisitor<Void> {

	private Design design;

	public DesignPorts(Design design) {
		this.design = design;
	}

	@Override
	public Void caseActor(Actor actor) {

		for (Port port : actor.getInputs()) {
			ActionIOHandler actionIOHandler = createDesignPort(port, "in");
			port.setAttribute("ioHandler", actionIOHandler);
		}

		for (Port port : actor.getOutputs()) {
			ActionIOHandler actionIOHandler = createDesignPort(port, "out");
			port.setAttribute("ioHandler", actionIOHandler);
		}

		return super.caseActor(actor);
	}

	/**
	 * This method takes a List of an {@link Actor} I/O ports and it creates the
	 * associated {@link Design} {@link FifoIOHandler}
	 * 
	 * @param port
	 * @param direction
	 * @return
	 */
	private ActionIOHandler createDesignPort(Port port, String direction) {
		ActionIOHandler actionIOHandler = null;
		if (port.isNative()) {
			actionIOHandler = new ActionIOHandler.NativeIOHandler(direction,
					port.getName(), Integer.toString(port.getType()
							.getSizeInBits()));
			actionIOHandler.build(design);

		} else {
			actionIOHandler = new ActionIOHandler.FifoIOHandler(direction,
					port.getName(), Integer.toString(port.getType()
							.getSizeInBits()), FifoID.TYPE_ACTION_SCALAR);
			actionIOHandler.build(design);

		}
		return actionIOHandler;
	}

}
