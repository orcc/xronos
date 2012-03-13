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

package net.sf.openforge.lim.op;

import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Operation;
import net.sf.openforge.lim.Visitor;

/**
 * @version $Id: TimingOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class TimingOp extends Operation {

	private boolean isGlobal;

	/**
	 * Constructs a TimingOp.
	 * 
	 * @param lateny
	 *            latency for this timing op
	 */
	public TimingOp(Latency latency, boolean isGlobal) {
		super(0);
		this.isGlobal = isGlobal;
		Exit exit = makeExit(0, Exit.DONE);
		exit.setLatency(latency);
	}

	public boolean isGlobal() {
		return isGlobal;
	}

	private void setGlobal(boolean b) {
		isGlobal = b;
	}

	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	public Object clone() throws CloneNotSupportedException {
		TimingOp clone = (TimingOp) super.clone();
		clone.setGlobal(isGlobal);
		return clone;
	}

	public boolean consumesGo() {
		return true;
	}
}
