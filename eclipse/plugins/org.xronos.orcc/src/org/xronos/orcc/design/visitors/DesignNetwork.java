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

package org.xronos.orcc.design.visitors;

import net.sf.orcc.df.util.DfVisitor;

import org.xronos.openforge.lim.Design;
import org.xronos.orcc.design.ResourceCache;

public class DesignNetwork extends DfVisitor<Void> {

	Design design;
	ResourceCache resourceCache;
	boolean schedulerInformation;

	public DesignNetwork(Design design, ResourceCache resourceCache,
			boolean schedulerInformation) {
		this.design = design;
		this.resourceCache = resourceCache;
		this.schedulerInformation = schedulerInformation;
	}

}
