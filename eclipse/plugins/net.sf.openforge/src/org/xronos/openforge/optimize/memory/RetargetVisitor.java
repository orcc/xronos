/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package org.xronos.openforge.optimize.memory;

import java.util.Collections;
import java.util.Map;

import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.FailVisitor;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.Variable;


/**
 * RetargetVisitor is a small visitor which is used to retarget LValue accesses
 * when the Locations on which they were based have changed. It will NOT work
 * for retargeting LValues when the structure of the Locations they target have
 * changed. Effectively, this class finds the LocationConstant inside absolute
 * accesses and modifies them to point to a new Location as defined by the
 * correlation map with which this class is constructed.
 * 
 * 
 * <p>
 * Created: Fri Oct 31 17:00:05 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: RetargetVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class RetargetVisitor extends FailVisitor {
	private Map<Location, Location> correlate;

	public RetargetVisitor(Map<Location, Location> corr) {
		super("location letargetting");
		correlate = Collections.unmodifiableMap(corr);
	}

	// Nothing to do...
	@Override
	public void visit(ArrayRead comp) {
	}

	@Override
	public void visit(ArrayWrite comp) {
	}

	// Nothing to do so long as we arent modifying the structure
	// of what is being accessed (which we arent here)
	@Override
	public void visit(HeapRead comp) {
	}

	@Override
	public void visit(HeapWrite comp) {
	}

	@Override
	public void visit(AbsoluteMemoryRead comp) { // Let the
													// visit(LocationConstant do
													// the work for us!)
		comp.getAddressConstant().accept(this);
	}

	@Override
	public void visit(AbsoluteMemoryWrite comp) { // Let the
													// visit(LocationConstant do
													// the work for us!)
		comp.getAddressConstant().accept(this);
	}

	@Override
	public void visit(LocationConstant comp) {
		Location oldLoc = comp.getTarget();
		Location newLoc = Variable.getCorrelatedLocation(correlate, oldLoc);
		comp.setTarget(newLoc);
	}
}
