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
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Emulatable;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.memory.AddressableUnit;
import net.sf.openforge.util.SizedInteger;

/**
 * The parent class for any operation that generates a constant value. All Ports
 * and Buses are marked unused, except the single data bus, whose value is (or
 * will be) a constant.
 * <p>
 * <b>Constant classes hierarchy</b>
 * <p>
 * <img src=doc-files/Constants.png>
 * 
 * @author Stephen Edwards
 * @version $Id: Constant.java 568 2008-03-31 17:23:31Z imiller $
 */
public abstract class Constant extends ValueOp implements Emulatable {

	/**
	 * Creates a new <code>Constant</code> instance with no initial numeric
	 * value, but with an output bus of width <code>busBitWidth</code> and the
	 * signedness indicated.
	 * 
	 * @throws IllegalArgumentException
	 *             if busBitWidth is less than or equal to 0.
	 */
	protected Constant(int busBitWidth, boolean isSigned) {
		super(0);

		if (busBitWidth <= 0) {
			throw new IllegalArgumentException("Width of constant too small. ");
		}

		getValueBus().setSize(busBitWidth, isSigned);
	}

	/**
	 * Returns the endian specific byte representation of this Constant as
	 * represented by an array of ByteRep objects which encapsulate the
	 * numerical value of each byte as well as whether or not that byte is fixed
	 * or indeterminate (indeterminate results from deferred constants that are
	 * not yet locked).
	 * 
	 * @return a non null, non zero length array of ByteRep objects.
	 */
	public abstract AURepBundle getRepBundle();

	/**
	 * Returns a List of Constant objects which identifies the Constants which
	 * make up this Constant object. This list is 'flat' such that every
	 * Constant in the list (<i>list(i)</i>) will return a single element List
	 * containing only itself when <code>getConstituents</code> is called upon
	 * it. That is:
	 * <p>
	 * <i>list(i)</i><code>.getConstituents()</code> == <i>list(i)</i>. This
	 * list is the ordered collection of constants that make up the definition
	 * of this constants value.
	 * 
	 * @return a List of Constant objects.
	 */
	public abstract List<? extends Constant> getConstituents();

	/**
	 * An unordered Set view of all the Constants that go into making up this
	 * Constant, including all AggregateConstants, SliceConstants, and any
	 * 'backing' or hidden constants within a slice.
	 * 
	 * @return a Set of Constant objects.
	 */
	public abstract Set<? extends Constant> getContents();

	/**
	 * Push the numerical value of the constant onto the bus
	 */
	@Override
	public abstract boolean pushValuesForward();

	/**
	 * Reverse constant prop has no rules applied on a Constant.
	 */
	@Override
	public abstract boolean pushValuesBackward();

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Returns true
	 */
	@Override
	public boolean isConstant() {
		return true;
	}

	/**
	 * Derives, as needed, the numeric value represented by this constant. Does
	 * nothing here as by default constants are locked.
	 */
	public void lock() {
	}

	/**
	 * Returns true if this constant has a fixed numerical value.
	 */
	public boolean isLocked() {
		return true;
	}

	/**
	 * Returns true if this constant is a Constant which represents an address.
	 */
	public boolean isPointerValue() {
		return false;
	}

	/**
	 * Tests whether the given constant has the same numerical value, or will
	 * have the same value if it is a deferred constant.
	 * 
	 * <p>
	 * requires: none
	 * <p>
	 * modifies: none
	 * <p>
	 * effects: returns true if the constant will ultimately resolve to the same
	 * numerical value as this Constant.
	 * 
	 * @param constant
	 *            a Constant, may be null
	 * @return true if the given constant is not null and has the same numerical
	 *         value.
	 */
	public boolean isSameValue(Constant constant) {
		// here defer testing to Value.
		// Value does not now (8/28/03) test for float in it's equiv.
		if (constant.isLocked()) {
			return getValueBus().getValue().equivalent(
					constant.getValueBus().getValue());
		}
		return false;
	}

	/**
	 * Performs a high level numerical emulation of this component. Just returns
	 * the current {@link Value} of the result {@link Bus}.
	 * 
	 * @param portValues
	 *            a map of owner {@link Port} to
	 *            {@link net.sf.openforge.util.SizedInteger} input value
	 * @return a map of {@link Bus} to
	 *         {@link net.sf.openforge.util.SizedInteger} result value
	 */
	@Override
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		return Collections.singletonMap(getValueBus(), getValueBus().getValue()
				.toNumber());
	}

	@Override
	public String toString() {
		String ret = getValueBus().showIDLogical() + " = "
				+ this.getIDGlobalType() + "(" + this.getValueBus().getSize()
				+ ",";
		if (getValueBus() != null && getValueBus().getValue() != null) {
			long valueBits = getValueBus().getValue().getConstantMask()
					& getValueBus().getValue().getValueMask();
			ret += "<Value=" + Long.toString(valueBits) + ">";
		} else {
			ret += "<value=NULL>";
		}
		return ret + ")";
	}

	/**
	 * A class for associating the addressable units of a constant value and the
	 * stride size represented by each unit.
	 */
	public class AURepBundle {
		private AddressableUnit[] rep;
		private int bitsPerUnit = -1;

		public AURepBundle(AddressableUnit[] rep, int bitsPerUnit) {
			if (rep == null)
				throw new IllegalArgumentException(
						"Cannot specify a null representation for Addressable Unit Values");
			if (bitsPerUnit < 0)
				throw new IllegalArgumentException(
						"Bits per unit of addressable unit bundle is out of range: "
								+ bitsPerUnit);

			this.rep = rep;
			this.bitsPerUnit = bitsPerUnit;
		}

		public AddressableUnit[] getRep() {
			return rep;
		}

		public int getLength() {
			return rep.length;
		}

		public int getBitsPerUnit() {
			return bitsPerUnit;
		}

		@Override
		public String toString() {
			String values = "";
			for (int i = 0; i < rep.length; i++)
				values += rep[rep.length - 1 - i]
						+ (i == rep.length - 1 ? "" : ", ");
			return "AURepBundle<" + bitsPerUnit + "> 0x[" + values + "]";
		}
	}

	/**
	 * A simple class tying together the numerical value of a byte along with a
	 * state flag which indicates whether or not that byte has a fixed value.
	 */
	public class _AURep {
		private int value;
		private boolean locked;

		/**
		 * Constructs a new ByteRep with the specified value and whose value is
		 * fixed (isLocked will return true).
		 */
		public _AURep(int value) {
			this(value, true);
		}

		/**
		 * Constructs a new ByteRep from the specified value and locked status.
		 */
		public _AURep(int value, boolean locked) {
			this.value = value;
			this.locked = locked;
		}

		/**
		 * Returns the numerical value of this byte.
		 * 
		 * @return a byte
		 */
		public int value() {
			return value;
		}

		/**
		 * Returns false if the numerical value of this byte is not yet
		 * determinate. This can happen as a result of a DeferredConstant whose
		 * value is not yet locked.
		 * 
		 * @return true if the value method returns a valid value.
		 */
		public boolean isLocked() {
			return locked;
		}

		@Override
		public String toString() {
			return (isLocked() ? "" : "?") + value();
		}
	}

}
