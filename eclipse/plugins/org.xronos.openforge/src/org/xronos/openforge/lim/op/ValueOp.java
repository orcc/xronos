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
package org.xronos.openforge.lim.op;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Operation;

/**
 * An operation that generates a single value.
 * 
 * @author Stephen Edwards
 * @version $Id: ValueOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class ValueOp extends Operation {

	/** The value data bus */
	private Bus valueBus;

	/**
	 * Constructs a long constant. By default, the ValueOp has an unused done
	 * Bus.
	 * 
	 * @param lval
	 *            the long value of the constant
	 */
	public ValueOp(int dataPortCount) {
		super(dataPortCount);
		Exit exit = makeExit(1);
		// setMainExit(exit);
		// exit.getDoneBus().setUsed(false);
		valueBus = exit.getDataBuses().iterator().next();
	}

	/**
	 * Gets the value data bus.
	 */
	public Bus getValueBus() {
		return valueBus;
	}

	/**
	 * Returns true if this ValueOp returns a floating point value.
	 */
	@Override
	public boolean isFloat() {
		return getValueBus().isFloat();
	}

	/**
	 * Calls the super, then removes any reference to the given bus in this
	 * class.
	 */
	@Override
	public boolean removeDataBus(Bus bus) {
		if (super.removeDataBus(bus)) {
			if (bus == valueBus)
				valueBus = null;
			return true;
		}
		return false;
	}

	/**
	 * Clones this ValueOp and correctly set's the 'valueBus'
	 * 
	 * @return a ValueOp clone of this operations.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		ValueOp clone = (ValueOp) super.clone();
		clone.valueBus = clone.getExit(Exit.DONE).getDataBuses().iterator()
				.next();
		return clone;
	}

}
