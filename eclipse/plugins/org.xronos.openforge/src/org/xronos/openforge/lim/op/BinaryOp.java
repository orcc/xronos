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

import org.xronos.openforge.lim.Bit;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Operation;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Value;

/**
 * Base class of all operations, which require two operands and generate one
 * result.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: BinaryOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class BinaryOp extends Operation {

	private Bus result_bus;

	/**
	 * Constructs a binary operation. By default, the Done of a BinaryOp is
	 * unused.
	 */
	public BinaryOp() {
		super(2);
		Exit exit = makeExit(1);
		result_bus = exit.getDataBuses().iterator().next();
	}

	/**
	 * Clones this BinaryOp and correctly set's the 'resultBus'
	 * 
	 * @return a BinaryOp clone of this operations.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		BinaryOp clone = (BinaryOp) super.clone();
		clone.result_bus = clone.getExit(Exit.DONE).getDataBuses().iterator()
				.next();
		return clone;
	}

	/**
	 * Gets the left-hand side input Port.
	 */
	public Port getLeftDataPort() {
		return getDataPorts().get(0);
	}

	public Bus getResultBus() {
		return result_bus;
	}

	/**
	 * Gets the right-hand side input Port.
	 */
	public Port getRightDataPort() {
		return getDataPorts().get(1);
	}

	/**
	 * Tests whether this is a passthrough operation based on a bitwise
	 * comparison of the operands. This method is used in subclasses for bitwise
	 * operations. For each pair of {@link Bit Bits} from the same positions in
	 * each operand, the method {@link #isSignificant()} is called until it
	 * returns true or the number of bits has been exhausted.
	 * 
	 * @return true if the output of this component can be reduced to a constant
	 *         or the value of a single input, based upon the current
	 *         {@link Value} of each data {@link Port}
	 */
	protected boolean isBitwisePassthrough() {
		final Value leftValue = getLeftDataPort().getValue();
		final Value rightValue = getRightDataPort().getValue();
		final int length = Math.min(leftValue.getSize(), rightValue.getSize());

		for (int i = 0; i < length; i++) {
			if (isSignificant(leftValue.getBit(i), rightValue.getBit(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if this BinaryOp returns a floating point value.
	 */
	@Override
	public boolean isFloat() {
		return getResultBus().isFloat();
	}

	/**
	 * Tests whether this is a passthrough operation.
	 * 
	 * @return true if the output of this component can be reduced to a constant
	 *         or the value of a single input, based upon the current
	 *         {@link Value} of each data {@link Port}
	 */
	protected boolean isPassthrough() {
		return (getLeftDataPort().getValue().isConstant() || getRightDataPort()
				.getValue().isConstant());
	}

	/**
	 * Tests whether this is a signed operation. An operation is signed if both
	 * the left and right port {@link Value values} are signed, otherwise the
	 * operation is considered unsigned.
	 * 
	 * @return boolean
	 */
	public boolean isSigned() {
		Value leftValue = getLeftDataPort().getValue();
		Value rightValue = getRightDataPort().getValue();

		return (leftValue.isSigned() && rightValue.isSigned());
	}

	/**
	 * Tests whether the states of two bits from the same position of each
	 * operand will cause work to be done in a bitwise binary operation. Called
	 * as a helper method by {@link #isBitwisePassthrough()}.
	 * 
	 * @param leftBit
	 *            the left bit from a given position
	 * @param rightBit
	 *            the right bit from a given position
	 * @return true if logic will be generated based upon these bits, false if
	 *         the output is constant or a wire
	 */
	protected boolean isSignificant(Bit leftBit, Bit rightBit) {
		// return (leftBit == Bit.CARE) && (rightBit == Bit.CARE);
		final boolean leftSig = !leftBit.isConstant() && leftBit.isCare();
		final boolean rightSig = !rightBit.isConstant() && rightBit.isCare();
		return (leftSig && rightSig);
	}

	/**
	 * Calls the super, then removes any reference to the given bus in this
	 * class.
	 */
	@Override
	public boolean removeDataBus(Bus bus) {
		if (super.removeDataBus(bus)) {
			if (bus == result_bus) {
				result_bus = null;
			}
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		String string = "null";
		try {
			string = result_bus.showIDLogical() + " = " + getIDGlobalType()
					+ "(" + getLeftDataPort().showIDLogical() + ", "
					+ getRightDataPort().showIDLogical() + ")";
		} catch (NullPointerException e) {
			string = "NullPointer";
		}

		return string;

	}
}
