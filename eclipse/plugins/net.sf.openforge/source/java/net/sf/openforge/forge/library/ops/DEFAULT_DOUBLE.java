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
package net.sf.openforge.forge.library.ops;

import java.util.Collections;
import java.util.List;

import net.sf.openforge.forge.api.ipcore.HDLWriter;
import net.sf.openforge.forge.api.ipcore.IPCore;
import net.sf.openforge.forge.api.pin.PinIn;
import net.sf.openforge.forge.api.pin.PinOut;

/**
 * This library contains code that can be used with the Forge Operation
 * Replacement feature to provide implementations for all operations that work
 * on the Java <code>double</code> primitive type. These operations will be
 * replaced with a call to one of the public methods contained in this library
 * according to the defined naming conventions. The implementation of each
 * operations is to supply values to an IPCore that has been defined for each
 * type of operation. The return value is retrieved from an output of that
 * IPCore. The core's implementation is defined by the associated
 * <code>HDLWriter</code> and is simply an empty module with the black box
 * attribute set. This leaves the actual implementation of the functionality of
 * each core to the user.
 * <p>
 * The cores defined in this library are all defined in non-static fields of
 * this library class. This means that a <b>unique</b> instance of the core will
 * be created for <b>each</b> operation that is replaced. The alternative is to
 * store the core and associated pins in static fields of this class, in which
 * case only 1 core will be allocated for each type of operation, and all
 * replacements will target that one core (which will be correctly arbitrated by
 * the Forge scheduling algorithms).
 */
public class DEFAULT_DOUBLE {
	// modulo
	private IPCore remCore = new IPCore(RemWriter.getName());
	private PinOut remA = new PinOut(remCore, "A", 64);
	private PinOut remB = new PinOut(remCore, "B", 64);
	private PinIn remQ = new PinIn(remCore, "Q", 64);

	// division
	private IPCore divCore = new IPCore(DivWriter.getName());
	private PinOut divA = new PinOut(divCore, "A", 64);
	private PinOut divB = new PinOut(divCore, "B", 64);
	private PinIn divQ = new PinIn(divCore, "Q", 64);

	// multiply
	private IPCore mulCore = new IPCore(MulWriter.getName());
	private PinOut mulA = new PinOut(mulCore, "A", 64);
	private PinOut mulB = new PinOut(mulCore, "B", 64);
	private PinIn mulQ = new PinIn(mulCore, "Q", 64);

	// subtract
	private IPCore subCore = new IPCore(SubWriter.getName());
	private PinOut subA = new PinOut(subCore, "A", 64);
	private PinOut subB = new PinOut(subCore, "B", 64);
	private PinIn subQ = new PinIn(subCore, "Q", 64);

	// add
	private IPCore addCore = new IPCore(AddWriter.getName());
	private PinOut addA = new PinOut(addCore, "A", 64);
	private PinOut addB = new PinOut(addCore, "B", 64);
	private PinIn addQ = new PinIn(addCore, "Q", 64);

	// Minus
	private IPCore minusCore = new IPCore(MinusWriter.getName());
	private PinOut minusA = new PinOut(minusCore, "A", 64);
	private PinIn minusQ = new PinIn(minusCore, "Q", 64);

	// GTE
	private IPCore gteCore = new IPCore(GteWriter.getName());
	private PinOut gteA = new PinOut(gteCore, "A", 64);
	private PinOut gteB = new PinOut(gteCore, "B", 64);
	private PinIn gteQ = new PinIn(gteCore, "Q", 32);

	// GT
	private IPCore gtCore = new IPCore(GtWriter.getName());
	private PinOut gtA = new PinOut(gtCore, "A", 64);
	private PinOut gtB = new PinOut(gtCore, "B", 64);
	private PinIn gtQ = new PinIn(gtCore, "Q", 32);

	// LTE
	private IPCore lteCore = new IPCore(LteWriter.getName());
	private PinOut lteA = new PinOut(lteCore, "A", 64);
	private PinOut lteB = new PinOut(lteCore, "B", 64);
	private PinIn lteQ = new PinIn(lteCore, "Q", 32);

	// LT
	private IPCore ltCore = new IPCore(LtWriter.getName());
	private PinOut ltA = new PinOut(ltCore, "A", 64);
	private PinOut ltB = new PinOut(ltCore, "B", 64);
	private PinIn ltQ = new PinIn(ltCore, "Q", 32);

	// NE
	private IPCore neCore = new IPCore(NeWriter.getName());
	private PinOut neA = new PinOut(neCore, "A", 64);
	private PinOut neB = new PinOut(neCore, "B", 64);
	private PinIn neQ = new PinIn(neCore, "Q", 32);

	// EQ
	private IPCore eqCore = new IPCore(EqWriter.getName());
	private PinOut eqA = new PinOut(eqCore, "A", 64);
	private PinOut eqB = new PinOut(eqCore, "B", 64);
	private PinIn eqQ = new PinIn(eqCore, "Q", 32);

	// B2D
	private IPCore b2dCore = new IPCore(B2dWriter.getName());
	private PinOut b2dA = new PinOut(b2dCore, "A", 8);
	private PinIn b2dQ = new PinIn(b2dCore, "Q", 64);

	// C2D
	private IPCore c2dCore = new IPCore(C2dWriter.getName());
	private PinOut c2dA = new PinOut(c2dCore, "A", 16);
	private PinIn c2dQ = new PinIn(c2dCore, "Q", 64);

	// S2D
	private IPCore s2dCore = new IPCore(S2dWriter.getName());
	private PinOut s2dA = new PinOut(s2dCore, "A", 16);
	private PinIn s2dQ = new PinIn(s2dCore, "Q", 64);

	// I2D
	private IPCore i2dCore = new IPCore(I2dWriter.getName());
	private PinOut i2dA = new PinOut(i2dCore, "A", 32);
	private PinIn i2dQ = new PinIn(i2dCore, "Q", 64);

	// L2D
	private IPCore l2dCore = new IPCore(L2dWriter.getName());
	private PinOut l2dA = new PinOut(l2dCore, "A", 64);
	private PinIn l2dQ = new PinIn(l2dCore, "Q", 64);

	// D2B
	private IPCore d2bCore = new IPCore(D2bWriter.getName());
	private PinOut d2bA = new PinOut(d2bCore, "A", 64);
	private PinIn d2bQ = new PinIn(d2bCore, "Q", 8);

	// D2C
	private IPCore d2cCore = new IPCore(D2cWriter.getName());
	private PinOut d2cA = new PinOut(d2cCore, "A", 64);
	private PinIn d2cQ = new PinIn(d2cCore, "Q", 16);

	// D2S
	private IPCore d2sCore = new IPCore(D2sWriter.getName());
	private PinOut d2sA = new PinOut(d2sCore, "A", 64);
	private PinIn d2sQ = new PinIn(d2sCore, "Q", 16);

	// D2I
	private IPCore d2iCore = new IPCore(D2iWriter.getName());
	private PinOut d2iA = new PinOut(d2iCore, "A", 64);
	private PinIn d2iQ = new PinIn(d2iCore, "Q", 32);

	// D2L
	private IPCore d2lCore = new IPCore(D2lWriter.getName());
	private PinOut d2lA = new PinOut(d2lCore, "A", 64);
	private PinIn d2lQ = new PinIn(d2lCore, "Q", 64);

	// D2F
	private IPCore d2fCore = new IPCore(D2fWriter.getName());
	private PinOut d2fA = new PinOut(d2fCore, "A", 64);
	private PinIn d2fQ = new PinIn(d2fCore, "Q", 32);

	public DEFAULT_DOUBLE() {
		remCore.setWriter(new RemWriter());
		divCore.setWriter(new DivWriter());
		mulCore.setWriter(new MulWriter());
		subCore.setWriter(new SubWriter());
		addCore.setWriter(new AddWriter());
		minusCore.setWriter(new MinusWriter());
		gteCore.setWriter(new GteWriter());
		gtCore.setWriter(new GtWriter());
		lteCore.setWriter(new LteWriter());
		ltCore.setWriter(new LtWriter());
		neCore.setWriter(new NeWriter());
		eqCore.setWriter(new EqWriter());

		b2dCore.setWriter(new B2dWriter());
		c2dCore.setWriter(new C2dWriter());
		s2dCore.setWriter(new S2dWriter());
		i2dCore.setWriter(new I2dWriter());
		l2dCore.setWriter(new L2dWriter());

		d2bCore.setWriter(new D2bWriter());
		d2cCore.setWriter(new D2cWriter());
		d2sCore.setWriter(new D2sWriter());
		d2iCore.setWriter(new D2iWriter());
		d2lCore.setWriter(new D2lWriter());
		d2fCore.setWriter(new D2fWriter());
	}

	public double rem(double aIn, double bIn) {
		remA.setNow(aIn);
		remB.setNow(bIn);
		return remQ.getDouble();
	}

	public double div(double aIn, double bIn) {
		divA.setNow(aIn);
		divB.setNow(bIn);
		return divQ.getDouble();
	}

	public double mult(double aIn, double bIn) {
		mulA.setNow(aIn);
		mulB.setNow(bIn);
		return mulQ.getDouble();
	}

	public double sub(double aIn, double bIn) {
		subA.setNow(aIn);
		subB.setNow(bIn);
		return subQ.getDouble();
	}

	public double add(double aIn, double bIn) {
		addA.setNow(aIn);
		addB.setNow(bIn);
		return addQ.getDouble();
	}

	public double minus(double aIn) {
		minusA.setNow(aIn);
		return minusQ.getDouble();
	}

	public int greaterThanEqual(double aIn, double bIn) {
		gteA.setNow(aIn);
		gteB.setNow(bIn);
		return gteQ.get();
	}

	public int greaterThan(double aIn, double bIn) {
		gtA.setNow(aIn);
		gtB.setNow(bIn);
		return gtQ.get();
	}

	public int lessThanEqual(double aIn, double bIn) {
		lteA.setNow(aIn);
		lteB.setNow(bIn);
		return lteQ.get();
	}

	public int lessThan(double aIn, double bIn) {
		ltA.setNow(aIn);
		ltB.setNow(bIn);
		return ltQ.get();
	}

	public int notEqual(double aIn, double bIn) {
		neA.setNow(aIn);
		neB.setNow(bIn);
		return neQ.get();
	}

	public int equal(double aIn, double bIn) {
		eqA.setNow(aIn);
		eqB.setNow(bIn);
		return eqQ.get();
	}

	public double byteToDouble(byte aIn) {
		b2dA.setNow(aIn);
		return b2dQ.getDouble();
	}

	public double charToDouble(char aIn) {
		c2dA.setNow(aIn);
		return c2dQ.getDouble();
	}

	public double shortToDouble(short aIn) {
		s2dA.setNow(aIn);
		return s2dQ.getDouble();
	}

	public double intToDouble(int aIn) {
		i2dA.setNow(aIn);
		return i2dQ.getDouble();
	}

	public double longToDouble(long aIn) {
		l2dA.setNow(aIn);
		return l2dQ.getDouble();
	}

	public byte doubleToByte(double aIn) {
		d2bA.setNow(aIn);
		return d2bQ.getByte();
	}

	public char doubleToChar(double aIn) {
		d2cA.setNow(aIn);
		return d2cQ.getChar();
	}

	public short doubleToShort(double aIn) {
		d2sA.setNow(aIn);
		return d2sQ.getShort();
	}

	public int doubleToInt(double aIn) {
		d2iA.setNow(aIn);
		return d2iQ.get();
	}

	public long doubleToLong(double aIn) {
		d2lA.setNow(aIn);
		return d2lQ.getLong();
	}

	public float doubleToFloat(double aIn) {
		d2fA.setNow(aIn);
		return d2fQ.getFloat();
	}

	/**
	 * Degenerate case which is purely a pass through.
	 */
	public double doubleToDouble(double aIn) {
		return aIn;
	}

	//
	// HDLWriters for each type of operation
	//

	private abstract static class DefaultWriter implements HDLWriter {
		@Override
		public List<String> writeVerilog(IPCore core, java.io.PrintWriter pw) {
			pw.println("// synthesis attribute BOX_TYPE of " + name()
					+ " is \"BLACK_BOX\"");
			pw.println("module " + name() + "(A, B, Q);");
			pw.println("  input  [" + (inWidth() - 1) + ":0] A, B;");
			pw.println("  output [" + (outWidth() - 1) + ":0] Q;");
			pw.println("endmodule // " + name());

			return Collections.emptyList();
		}

		abstract String name();

		int inWidth() {
			return 64;
		}

		int outWidth() {
			return 64;
		}
	}

	private abstract static class DefaultWriterOneInput extends DefaultWriter {
		@Override
		public List<String> writeVerilog(IPCore core, java.io.PrintWriter pw) {
			pw.println("// synthesis attribute BOX_TYPE of " + name()
					+ " is \"BLACK_BOX\"");
			pw.println("module " + name() + "(A, Q);");
			pw.println("  input  [" + (inWidth() - 1) + ":0] A;");
			pw.println("  output [" + (outWidth() - 1) + ":0] Q;");
			pw.println("endmodule // " + name());

			return Collections.emptyList();
		}
	}

	private static class RemWriter extends DefaultWriter {
		public static String getName() {
			return "remCore_double";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class DivWriter extends DefaultWriter {
		public static String getName() {
			return "divCore_double";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class MulWriter extends DefaultWriter {
		public static String getName() {
			return "mulCore_double";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class SubWriter extends DefaultWriter {
		public static String getName() {
			return "subCore_double";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class AddWriter extends DefaultWriter {
		public static String getName() {
			return "addCore_double";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class MinusWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "minusCore_double";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class GteWriter extends DefaultWriter {
		public static String getName() {
			return "gteCore_double";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 32;
		}
	}

	private static class GtWriter extends DefaultWriter {
		public static String getName() {
			return "gtCore_double";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 32;
		}
	}

	private static class LteWriter extends DefaultWriter {
		public static String getName() {
			return "lteCore_double";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 32;
		}
	}

	private static class LtWriter extends DefaultWriter {
		public static String getName() {
			return "ltCore_double";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 32;
		}
	}

	private static class NeWriter extends DefaultWriter {
		public static String getName() {
			return "neCore_double";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 32;
		}
	}

	private static class EqWriter extends DefaultWriter {
		public static String getName() {
			return "eqCore_double";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 32;
		}
	}

	private static class B2dWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "b2dCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int inWidth() {
			return 8;
		}
	}

	private static class C2dWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "c2dCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int inWidth() {
			return 16;
		}
	}

	private static class S2dWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "s2dCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int inWidth() {
			return 16;
		}
	}

	private static class I2dWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "i2dCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int inWidth() {
			return 32;
		}
	}

	private static class L2dWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "l2dCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class D2bWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "d2bCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 8;
		}
	}

	private static class D2cWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "d2cCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 16;
		}
	}

	private static class D2sWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "d2sCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 16;
		}
	}

	private static class D2iWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "d2iCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 32;
		}
	}

	private static class D2lWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "d2lCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class D2fWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "d2fCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 32;
		}
	}

}
