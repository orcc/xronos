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
import net.sf.openforge.lim.Value;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.Register;

/**
 * A Verilog Register which is based on a LIM {@link Bus}.
 * <P>
 * 
 * Created: May 7, 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: BusRegister.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BusRegister extends Register implements BusNet {

	Bus bus;
	Value initialValue;

	/**
	 * Constructs a BusRegister based on a LIM Bus and its initial value.
	 * 
	 * @param bus
	 *            The LIM Bus upon which to base the Net
	 * @param initialValue
	 *            The initial value of the bus
	 */
	public BusRegister(Bus bus, Value initialValue) {
		super(ID.toVerilogIdentifier(ID.showLogical(bus)), bus.getValue()
				.getCompactedSize());
		this.bus = bus;
		this.initialValue = initialValue;
	}

	/**
	 * Constructs a BusRegister based on a LIM Bus.
	 * 
	 * @param bus
	 *            The LIM Bus upon which to base the Net
	 */
	public BusRegister(Bus bus) {
		this(bus, null);
	}

	/**
	 * Returns the Bus upon which the Net is based.
	 * 
	 * @return The LIM Bus upon which the Net is based
	 */
	public Bus getBus() {
		return bus;
	}

	/**
	 * Returns an Expression (the constant value) of the initial value of this
	 * BusRegister, used in the declaration.
	 * 
	 * @return a value of type 'Expression'
	 * @throws NonConstantInitialValueException
	 *             if the initial value is not a constant
	 */
	public Expression getDeclarationInitial()
			throws NonConstantInitialValueException {
		int size = getWidth(); // ensures the width matches the net width
		Expression init = null;

		if (this.initialValue == null) {
			init = new HexNumber(0, size);
		} else if (!this.initialValue.isConstant()) {
			throw new NonConstantInitialValueException();
		} else {
			long initNum = this.initialValue.toNumber().numberValue()
					.longValue();
			init = new HexNumber(initNum, size);
		}

		return init;
	}

} // BusRegister

