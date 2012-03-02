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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.sf.openforge.lim.Bit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.verilog.model.Concatenation;
import net.sf.openforge.verilog.model.Constant;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.HexConstant;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.Lexicality;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Wire;

/**
 * A verilog expression which is based on a LIM {@link Port}. The expression may
 * be as simple as a {@link Net} or a {@link Constant}, but could also be a
 * {@link Concatenation} of bit selects and constant values.
 * <P>
 * 
 * Created: August 6, 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: PortWire.java 280 2006-08-11 17:00:32Z imiller $
 */
public class PortWire extends Wire implements Expression {

	Port port;
	Expression expression;
	Value value;

	/**
	 * Constructs a new PortWire in which the Value attached to the Port is
	 * compacted (redundant MSB bits trimmed) prior to building the expression
	 * for the port.
	 * 
	 * @param port
	 *            a value of type 'Port'
	 * @param compactPort
	 *            a value of type 'boolean'
	 */
	public PortWire(Port port, boolean compactPort) {
		super("portwire", 1);
		this.port = port;
		this.value = port.getValue();

		assert (value != null) : "Port (" + port + ") is missing a value.";
		if (compactPort) {
			expression = getCompactedRange();
		} else {
			expression = getFullRange();
		}
	}

	/**
	 * Constructs a PortWire based on a LIM Port. The upstream Bus Value Bits
	 * which actually produce the are assembled into a concatenation, if needed.
	 * 
	 * @param port
	 *            The LIM Port upon which to base the Net
	 */
	public PortWire(Port port) {
		this(port, false);
	}

	/**
	 * Gets a group of similar bits from a Value, starting from a given bit
	 * position.
	 * 
	 * @param value
	 *            the Value which contains the bits
	 * @param start
	 *            the starting bit position for the group
	 * @param end
	 *            the last bit position to include in the group
	 */
	private List<Bit> getBitGroup(Value value, int start, int end) {
		List<Bit> bitgroup = new ArrayList<Bit>();
		Bit first_bit = value.getBit(start);

		bitgroup.add(first_bit);
		start++;

		if (first_bit.isCare()) {
			if (first_bit.isConstant()) {
				for (int i = start; (i < value.getSize()) && (i <= end); i++) {
					Bit bit = value.getBit(i);
					if (!bit.isCare())
						break;
					if (!bit.isConstant())
						break;
					bitgroup.add(bit);
				}
			} else if (first_bit.getInvertedBit() != null) {
				int bit_position = first_bit.getPosition();
				int inverted_bit_position = first_bit.getInvertedBit()
						.getPosition();
				for (int i = start; (i < value.getSize()) && (i <= end); i++) {
					Bit bit = value.getBit(i);
					Bit invertedBit = value.getBit(i).getInvertedBit();

					if (!bit.isCare())
						break;
					if (bit.isConstant())
						break;
					if (invertedBit == null)
						break;
					if (first_bit.getOwner() != bit.getOwner())
						break;
					if (!(bit.getPosition() == bit_position + 1))
						break;
					if (!(invertedBit.getPosition() == inverted_bit_position + 1))
						break;
					bitgroup.add(bit);
					bit_position++;
					inverted_bit_position++;
				}
			} else {
				int bit_position = first_bit.getPosition();
				for (int i = start; (i < value.getSize()) && (i <= end); i++) {
					Bit bit = value.getBit(i);

					if (!bit.isCare())
						break;
					if (bit.isConstant())
						break;
					if (bit.getInvertedBit() != null)
						break;
					if (first_bit.getOwner() != bit.getOwner())
						break;
					if (!(bit.getPosition() == bit_position + 1))
						break;
					bitgroup.add(bit);
					bit_position++;
				}
			}
		} else {
			for (int i = start; (i < value.getSize()) && (i <= end); i++) {
				Bit bit = value.getBit(i);
				if (bit.isCare())
					break;
				bitgroup.add(bit);
			}
		}
		return bitgroup;
	}

	public Expression getExpression() {
		return expression;
	}

	public Port getPort() {
		return port;
	}

	public int getWidth() {
		return expression.getWidth();
	}

	public Expression getBitSelect(int bit) {
		if (value.getBit(bit).isConstant()) {
			long val = value.getBit(bit).isOn() ? 1L : 0L;
			return new HexNumber(val, 1);
		} else {
			if (value.getBit(bit).getInvertedBit() != null) {
				return new InvertedBitWire(value.getBit(bit).getInvertedBit());
			} else {
				return new BitWire(value.getBit(bit));
			}
		}
	}

	public Expression getRange(int msb, int lsb) {
		Expression expression;

		int size = msb - lsb + 1;

		if (value.isConstant()) {
			long constValue = 0L;
			int bit_index = 0;
			for (int i = lsb; i <= msb; i++) {
				Bit bit = value.getBit(i);
				if (bit.isOn()) {
					constValue |= (1L << bit_index);
				}
				bit_index++;
			}
			expression = new HexNumber(new HexConstant(constValue, size));
		}
		// else if (value.isMixed())
		else {
			// build a concatentation
			List<Object> cat_parts = new ArrayList<Object>();
			int i = lsb;
			while (i <= msb) {
				List<Bit> bitgroup = getBitGroup(value, i, msb);
				Bit first_bit = (Bit) bitgroup.get(0);
				if (first_bit.isCare()) {
					if (first_bit.isConstant()) {
						cat_parts.add(0, new BitConstant(bitgroup));
					} else if (first_bit.getInvertedBit() != null) {
						cat_parts.add(0, new InvertedBitWire(bitgroup));
					} else {
						cat_parts.add(0, new BitWire(bitgroup));
					}
				} else {
					assert false : "Can't handle don't cares mixed in with cares on Port ("
							+ port + ") at index " + i;
				}
				i += bitgroup.size();
			}

			assert cat_parts.size() > 0 : "Must have at least 1 part to the port value "
					+ value.debug();
			if (cat_parts.size() > 1) {
				Concatenation cat = new Concatenation();
				for (Iterator it = cat_parts.iterator(); it.hasNext();) {
					cat.add((Expression) it.next());
				}
				expression = cat;
			} else {
				expression = (Expression) cat_parts.get(0);
			}
		}

		// else
		// {
		// /*
		// * All the Bits are from the same source. Just get the BusWire from
		// the source,
		// * or a range if only a subset of the Bits are required.
		// */

		// //final Bus sourceBus = port.getBus().getSource();
		// //
		// // IDM. The source bus should be obtained from the Value
		// // so that we take full advantage of the propagation of
		// // Bits through the LIM. This eliminates the need for
		// // cast ops and NumericPromotionOps in translation. We
		// // still use the getSource to ensure that we traverse
		// // backwards across module/call boundries as necessary
		// // (or as not necessary). Since we are in this 'else' we
		// // are assured that the bus is contiguous.
		// //
		// final Bus sourceBus = value.getBit(0).getOwner().getSource();
		// //
		// //
		// // CRSS uncomment this to see the problem
		// //
		// //
		// //assert(!sourceBus.getValue().isConstant()) :
		// "Source bus in PortWire is constant and Port value is not!";
		// final BusWire fullWire = new BusWire(sourceBus);

		// // if (size < sourceBus.getValue().size())
		// // expression = fullWire.getRange(msb, lsb);
		// // else if (size > sourceBus.getValue().size())
		// // {
		// // Concatenation cat = new Concatenation();
		// // cat.add(new Replication(size - sourceBus.getValue().size(),
		// // fullWire.getBitSelect(fullWire.getWidth()-1)));
		// // cat.add(fullWire);
		// // expression = cat;
		// // }
		// // else
		// // expression = fullWire;

		// expression = (size < sourceBus.getValue().size()) ?
		// fullWire.getRange(msb, lsb) : fullWire;
		// }
		assert (expression.getWidth() == size) : "PortWire constructed expression ("
				+ expression
				+ " "
				+ expression.getWidth()
				+ ") doesn't match desired size (" + size + ")";

		return expression;
	}

	public Expression getFullRange() {
		return getRange(value.getSize() - 1, 0);
	}

	public Expression getCompactedRange() {
		int compactedWidth = value.getCompactedSize();
		assert compactedWidth > 0 : "Illegal compacted wire size: "
				+ compactedWidth;
		return getRange(value.getCompactedSize() - 1, 0);
	}

	public int getLSB() {
		return 0;
	}

	public int getMSB() {
		return getWidth() - 1;
	}

	public Collection getNets() {
		return expression.getNets();
	}

	public Lexicality lexicalify() {
		// _pattern.d.ln(expression.getClass()+"-->"+expression.lexicalify());
		return expression.lexicalify();
	}

} // PortWire

