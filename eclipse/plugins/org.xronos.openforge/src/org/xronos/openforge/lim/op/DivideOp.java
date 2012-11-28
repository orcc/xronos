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
import java.util.Map;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Emulatable;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.util.SizedInteger;


/**
 * A binary arithmetic operation in a form of /.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: DivideOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class DivideOp extends BinaryOp implements Emulatable {

	/** The natural size of this operation */
	private int naturalSize = -1;

	/**
	 * Constructs an arithmetic divide operation.
	 * 
	 */
	public DivideOp(int naturalSize) {
		super();
		this.naturalSize = naturalSize;
	}

	/**
	 * Returns the natural size of this operation, used by optimizations to
	 * build shifts for implementation.
	 */
	public int getNaturalSize() {
		return naturalSize;
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	@Override
	public int getGateDepth() {
		final int width = Math.max(getLeftDataPort().getValue().getSize(),
				getRightDataPort().getValue().getSize());
		return 5 * width * width;
	}

	public String getReplacementMethodName() {
		// return OperationReplacementVisitor.DIVNAME;

		// FIXME: Do this until Ian resolves the problems with
		// OperationReplacementVisitor.
		return "div";
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
		final SizedInteger lval = portValues.get(getLeftDataPort());
		final SizedInteger rval = portValues.get(getRightDataPort());
		return Collections.singletonMap(getResultBus(), lval.divide(rval));
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
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value in0 = getLeftDataPort().getValue();
		Value in1 = getRightDataPort().getValue();

		int newSize = Math.max(in0.getSize(), in1.getSize());
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
	@Override
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
