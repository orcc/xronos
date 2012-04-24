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
package net.sf.openforge.verilog.pattern;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Wire;

/**
 * A Verilog Wire which is based on a LIM {@link Bus}.
 * <P>
 * 
 * Created: May 7, 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: BusWire.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BusWire extends Wire implements BusNet {

	Bus bus;

	/**
	 * Constructs a BusWire based on a LIM Bus. {@link Bus#getSource()} is used
	 * to retrieve the upstream Bus which actually produces a signal.
	 * 
	 * @param bus
	 *            The LIM Bus upon which to base the Net
	 * @param name
	 *            String name for this BusWire
	 */
	public BusWire(Bus bus, String name) {
		/*
		 * Get the size from the given Bus (not its source), since this is the
		 * correct size as set by constant propagation.
		 */
		super(name, bus.getValue().getSize());
		this.bus = bus;
		// assert(bus.getValue().isConstant()==false);
	}

	/**
	 * Constructs a BusWire based on a LIM Bus. Bus.getSource(0 is used to
	 * retrieve the upstream Bus which actually produces a signal.
	 * 
	 * @param bus
	 *            The LIM Bus upon which to base the Net
	 */
	public BusWire(Bus bus) {
		// this(bus,ID.toVerilogIdentifier(ID.showLogical(bus.getSource())));
		this(bus, ID.toVerilogIdentifier(ID.showLogical(bus)));
	}

	public Bus getBus() {
		return bus;
	}

} // BusWire

