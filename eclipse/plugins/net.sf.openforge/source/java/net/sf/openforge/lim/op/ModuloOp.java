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

import java.util.Collections;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Emulatable;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.util.SizedInteger;

/**
 * A binary arithmetic operation in a form of %.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: ModuloOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ModuloOp extends BinaryOp implements Emulatable {

	/**
	 * Constructs an arithmetic modulo operation.
	 * 
	 */
	public ModuloOp() {
		super();
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	public int getGateDepth() {
		final int width = Math.max(getLeftDataPort().getValue().getSize(),
				getRightDataPort().getValue().getSize());
		return 5 * width * width;
	}

	/**
	 * Accept method for the Visitor interface
	 */
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	public String getReplacementMethodName() {
		// return OperationReplacementVisitor.REMNAME;

		// FIXME: Do this until Ian resolves the problems with
		// OperationReplacementVisitor.
		return "rem";
	}

	/**
	 * Performes a high level numerical emulation of this component.
	 * 
	 * @param portValues
	 *            a map of owner {@link Port} to {@link SizedInteger} input
	 *            value
	 * @return a map of {@link Bus} to {@link SizedInteger} result value
	 */
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		final SizedInteger lval = (SizedInteger) portValues
				.get(getLeftDataPort());
		final SizedInteger rval = (SizedInteger) portValues
				.get(getRightDataPort());
		return Collections.singletonMap(getResultBus(), lval.mod(rval));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this DivideOp
	 * according to this rule:
	 * 
	 * Result size is port 0 size. All result bit are cares
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean pushValuesForward() {
		boolean mod = false;

		Value in0 = getLeftDataPort().getValue();
		Value in1 = getRightDataPort().getValue();

		int newSize = in0.getSize();
		boolean isSigned = in0.isSigned() && in1.isSigned();

		Value newValue = new Value(newSize, isSigned);

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * Reverse constant prop on a DivideOp simply propagates the consumed value
	 * back to the Ports. The first port has the same size with the consumed
	 * value, and all bits are care.
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		Value pushBackValue = new Value(resultBusValue.getSize(),
				resultBusValue.isSigned());

		mod |= getLeftDataPort().pushValueBackward(pushBackValue);

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}
