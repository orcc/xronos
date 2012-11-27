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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.xronos.openforge.lim.Bit;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.report.FPGAResource;
import org.xronos.openforge.util.SizedInteger;


/**
 * AddMultiOp is an 'n' input add operation. An add of the form A + B + C + D
 * ....
 * 
 * <p>
 * Created: Wed Nov 6 10:25:34 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: AddMultiOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class AddMultiOp extends AddOp {

	public AddMultiOp() {
		super();
	}

	/**
	 * returns true
	 */
	@Override
	public boolean hasMulti() {
		return getDataPorts().size() > 2;
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	@Override
	public FPGAResource getHardwareResourceUsage() {
		FPGAResource hwResource = new FPGAResource();

		for (int i = 1; i < getDataPorts().size(); i++) {
			hwResource.addResourceUsage(super.getHardwareResourceUsage());
		}

		return hwResource;
	}

	/**
	 * Performes a high level numerical emulation of this component.
	 * 
	 * @param portValues
	 *            a map of owner {@link Port} to {@link SizedInteger} input
	 *            value
	 * @return a map of {@link Bus} to {@link SizedInteger} result value
	 */
	@Override
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {

		final Iterator<Port> iter = getDataPorts().iterator();
		SizedInteger result = portValues.get(iter.next());
		while (iter.hasNext()) {
			result = result.add(portValues.get(iter.next()));
		}

		return Collections.singletonMap(getResultBus(), result);
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Overrides the super to take into account all data ports
	 * 
	 * @return true if a change was made to the result bus Value.
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		int newSize = 0;
		boolean isSigned = false;

		for (Port port : getDataPorts()) {
			Value portValue = port.getValue();
			newSize = Math.max(newSize, portValue.getSize());
			isSigned &= portValue.isSigned();
		}

		Value newValue = new Value(newSize, isSigned);

		for (int i = 0; i < newSize; i++) {
			boolean hasCare = false;
			for (Port port : getDataPorts()) {
				Value portValue = port.getValue();
				if (i < portValue.getSize()) {
					Bit bit = portValue.getBit(i);
					if (bit.isCare()) {
						hasCare = true;
						break;
					}
				}
			}

			if (!hasCare) {
				newValue.setBit(i, Bit.DONT_CARE);
			}
		}

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}// AddMultiOp

