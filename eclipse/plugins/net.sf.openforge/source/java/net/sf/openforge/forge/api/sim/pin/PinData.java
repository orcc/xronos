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
/* $Rev: 2 $ */
package net.sf.openforge.forge.api.sim.pin;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract parent class of all pin data. Not to be user-instantiated.
 */
public abstract class PinData {

	/**
	 * Return the cycle count of this data set
	 * 
	 * @return count
	 */
	public abstract int getCycleCount();

	/**
	 * Get value at an arbitrary clock tick, which must be between 0 &
	 * getCycleCount()-1.
	 * 
	 * @param clockTick
	 *            clock tick whose value you want
	 * @return value
	 */
	public abstract SignalValue valueAt(int clockTick);

	/**
	 * Empty this data set
	 * 
	 */
	public abstract void clear();

	public String toString() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		pw.println(this.getClass().getName());
		for (int i = 0; i < getCycleCount(); i++) {
			pw.println("\t" + valueAt(i) + "@" + i);
		}
		pw.flush();
		return baos.toString();
	}

	/**
	 * Return the data set as a List of SignalValues
	 * 
	 * @return List of SignalValues
	 */
	public List<SignalValue> asList() {
		List<SignalValue> al = new ArrayList<SignalValue>(getCycleCount());
		for (int i = 0; i < getCycleCount(); i++) {
			al.add((SignalValue) valueAt(i));
		}
		return al;
	}

}
