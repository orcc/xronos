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
package net.sf.openforge.lim;

/**
 * This is a tristateable buffer having an 2 inputs (1 data, one enable),
 * and an output. You'll need to size the result bus, and it is a 'raw'
 * compoenent not suitable for scheduling and such.
 *
 * @author "C. Schanck" <cschanck@xilinx.com>
 * @version 1.0
 * @since 1.0
 * @see Component
 */

/**
 * @version $Id: TriBuf.java 2 2005-06-09 20:00:48Z imiller $
 */
public class TriBuf extends Component {

	private Bus resultBus;

	/**
	 * Constructs a Tribus. result buf is unsized.
	 * 
	 * @param size
	 *            size of data in / data out
	 */
	public TriBuf() {
		super(2); // 2 data port
		Exit exit = makeExit(1);
		resultBus = exit.getDataBuses().iterator().next();
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	public Port getInputPort() {
		return getDataPorts().get(0);
	}

	public Port getEnablePort() {
		return getDataPorts().get(1);
	}

	public Bus getResultBus() {
		return resultBus;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this TriBuf
	 * according to these rules:
	 * 
	 * Result value is all pass throughs of input value
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		return false;
	}

	/**
	 * Reverse constant prop on a TriBuf simply propagates the consumed value
	 * back to the Port. The port has the same size with the consumed value.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		return false;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}
