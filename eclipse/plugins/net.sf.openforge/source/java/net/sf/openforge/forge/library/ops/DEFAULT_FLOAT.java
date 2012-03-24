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
 * on the Java <code>float</code> primitive type. These operations will be
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
public class DEFAULT_FLOAT {
	// modulo
	private IPCore remCore = new IPCore(RemWriter.getName());
	private PinOut remA = new PinOut(remCore, "A", 32);
	private PinOut remB = new PinOut(remCore, "B", 32);
	private PinIn remQ = new PinIn(remCore, "Q", 32);

	// division
	private IPCore divCore = new IPCore(DivWriter.getName());
	private PinOut divA = new PinOut(divCore, "A", 32);
	private PinOut divB = new PinOut(divCore, "B", 32);
	private PinIn divQ = new PinIn(divCore, "Q", 32);

	// multiply
	private IPCore mulCore = new IPCore(MulWriter.getName());
	private PinOut mulA = new PinOut(mulCore, "A", 32);
	private PinOut mulB = new PinOut(mulCore, "B", 32);
	private PinIn mulQ = new PinIn(mulCore, "Q", 32);

	// subtract
	private IPCore subCore = new IPCore(SubWriter.getName());
	private PinOut subA = new PinOut(subCore, "A", 32);
	private PinOut subB = new PinOut(subCore, "B", 32);
	private PinIn subQ = new PinIn(subCore, "Q", 32);

	// add
	private IPCore addCore = new IPCore(AddWriter.getName());
	private PinOut addA = new PinOut(addCore, "A", 32);
	private PinOut addB = new PinOut(addCore, "B", 32);
	private PinIn addQ = new PinIn(addCore, "Q", 32);

	// Minus
	private IPCore minusCore = new IPCore(MinusWriter.getName());
	private PinOut minusA = new PinOut(minusCore, "A", 32);
	private PinIn minusQ = new PinIn(minusCore, "Q", 32);

	// GTE
	private IPCore gteCore = new IPCore(GteWriter.getName());
	private PinOut gteA = new PinOut(gteCore, "A", 32);
	private PinOut gteB = new PinOut(gteCore, "B", 32);
	private PinIn gteQ = new PinIn(gteCore, "Q", 32);

	// GT
	private IPCore gtCore = new IPCore(GtWriter.getName());
	private PinOut gtA = new PinOut(gtCore, "A", 32);
	private PinOut gtB = new PinOut(gtCore, "B", 32);
	private PinIn gtQ = new PinIn(gtCore, "Q", 32);

	// LTE
	private IPCore lteCore = new IPCore(LteWriter.getName());
	private PinOut lteA = new PinOut(lteCore, "A", 32);
	private PinOut lteB = new PinOut(lteCore, "B", 32);
	private PinIn lteQ = new PinIn(lteCore, "Q", 32);

	// LT
	private IPCore ltCore = new IPCore(LtWriter.getName());
	private PinOut ltA = new PinOut(ltCore, "A", 32);
	private PinOut ltB = new PinOut(ltCore, "B", 32);
	private PinIn ltQ = new PinIn(ltCore, "Q", 32);

	// NE
	private IPCore neCore = new IPCore(NeWriter.getName());
	private PinOut neA = new PinOut(neCore, "A", 32);
	private PinOut neB = new PinOut(neCore, "B", 32);
	private PinIn neQ = new PinIn(neCore, "Q", 32);

	// EQ
	private IPCore eqCore = new IPCore(EqWriter.getName());
	private PinOut eqA = new PinOut(eqCore, "A", 32);
	private PinOut eqB = new PinOut(eqCore, "B", 32);
	private PinIn eqQ = new PinIn(eqCore, "Q", 32);

	// B2F
	private IPCore b2fCore = new IPCore(B2fWriter.getName());
	private PinOut b2fA = new PinOut(b2fCore, "A", 8);
	private PinIn b2fQ = new PinIn(b2fCore, "Q", 32);

	// C2F
	private IPCore c2fCore = new IPCore(C2fWriter.getName());
	private PinOut c2fA = new PinOut(c2fCore, "A", 16);
	private PinIn c2fQ = new PinIn(c2fCore, "Q", 32);

	// S2F
	private IPCore s2fCore = new IPCore(S2fWriter.getName());
	private PinOut s2fA = new PinOut(s2fCore, "A", 16);
	private PinIn s2fQ = new PinIn(s2fCore, "Q", 32);

	// I2F
	private IPCore i2fCore = new IPCore(I2fWriter.getName());
	private PinOut i2fA = new PinOut(i2fCore, "A", 32);
	private PinIn i2fQ = new PinIn(i2fCore, "Q", 32);

	// L2F
	private IPCore l2fCore = new IPCore(L2fWriter.getName());
	private PinOut l2fA = new PinOut(l2fCore, "A", 64);
	private PinIn l2fQ = new PinIn(l2fCore, "Q", 32);

	// F2B
	private IPCore f2bCore = new IPCore(F2bWriter.getName());
	private PinOut f2bA = new PinOut(f2bCore, "A", 32);
	private PinIn f2bQ = new PinIn(f2bCore, "Q", 8);

	// F2C
	private IPCore f2cCore = new IPCore(F2cWriter.getName());
	private PinOut f2cA = new PinOut(f2cCore, "A", 32);
	private PinIn f2cQ = new PinIn(f2cCore, "Q", 16);

	// F2S
	private IPCore f2sCore = new IPCore(F2sWriter.getName());
	private PinOut f2sA = new PinOut(f2sCore, "A", 32);
	private PinIn f2sQ = new PinIn(f2sCore, "Q", 16);

	// F2I
	private IPCore f2iCore = new IPCore(F2iWriter.getName());
	private PinOut f2iA = new PinOut(f2iCore, "A", 32);
	private PinIn f2iQ = new PinIn(f2iCore, "Q", 32);

	// F2L
	private IPCore f2lCore = new IPCore(F2lWriter.getName());
	private PinOut f2lA = new PinOut(f2lCore, "A", 32);
	private PinIn f2lQ = new PinIn(f2lCore, "Q", 64);

	// F2D
	private IPCore f2dCore = new IPCore(F2dWriter.getName());
	private PinOut f2dA = new PinOut(f2dCore, "A", 32);
	private PinIn f2dQ = new PinIn(f2dCore, "Q", 64);

	public DEFAULT_FLOAT() {
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

		b2fCore.setWriter(new B2fWriter());
		c2fCore.setWriter(new C2fWriter());
		s2fCore.setWriter(new S2fWriter());
		i2fCore.setWriter(new I2fWriter());
		l2fCore.setWriter(new L2fWriter());

		f2bCore.setWriter(new F2bWriter());
		f2cCore.setWriter(new F2cWriter());
		f2sCore.setWriter(new F2sWriter());
		f2iCore.setWriter(new F2iWriter());
		f2lCore.setWriter(new F2lWriter());
		f2dCore.setWriter(new F2dWriter());

	}

	public float rem(float aIn, float bIn) {
		remA.setNow(aIn);
		remB.setNow(bIn);
		return remQ.getFloat();
	}

	public float div(float aIn, float bIn) {
		divA.setNow(aIn);
		divB.setNow(bIn);
		return divQ.getFloat();
	}

	public float mult(float aIn, float bIn) {
		mulA.setNow(aIn);
		mulB.setNow(bIn);
		return mulQ.getFloat();
	}

	public float sub(float aIn, float bIn) {
		subA.setNow(aIn);
		subB.setNow(bIn);
		return subQ.getFloat();
	}

	public float add(float aIn, float bIn) {
		addA.setNow(aIn);
		addB.setNow(bIn);
		return addQ.getFloat();
	}

	public float minus(float aIn) {
		minusA.setNow(aIn);
		return minusQ.getFloat();
	}

	public int greaterThanEqual(float aIn, float bIn) {
		gteA.setNow(aIn);
		gteB.setNow(bIn);
		return gteQ.get();
	}

	public int greaterThan(float aIn, float bIn) {
		gtA.setNow(aIn);
		gtB.setNow(bIn);
		return gtQ.get();
	}

	public int lessThanEqual(float aIn, float bIn) {
		lteA.setNow(aIn);
		lteB.setNow(bIn);
		return lteQ.get();
	}

	public int lessThan(float aIn, float bIn) {
		ltA.setNow(aIn);
		ltB.setNow(bIn);
		return ltQ.get();
	}

	public int notEqual(float aIn, float bIn) {
		neA.setNow(aIn);
		neB.setNow(bIn);
		return neQ.get();
	}

	public int equal(float aIn, float bIn) {
		eqA.setNow(aIn);
		eqB.setNow(bIn);
		return eqQ.get();
	}

	public float byteToFloat(byte aIn) {
		b2fA.setNow(aIn);
		return b2fQ.getFloat();
	}

	public float charToFloat(char aIn) {
		c2fA.setNow(aIn);
		return c2fQ.getFloat();
	}

	public float shortToFloat(short aIn) {
		s2fA.setNow(aIn);
		return s2fQ.getFloat();
	}

	public float intToFloat(int aIn) {
		i2fA.setNow(aIn);
		return i2fQ.getFloat();
	}

	public float longToFloat(long aIn) {
		l2fA.setNow(aIn);
		return l2fQ.getFloat();
	}

	public byte floatToByte(float aIn) {
		f2bA.setNow(aIn);
		return f2bQ.getByte();
	}

	public char floatToChar(float aIn) {
		f2cA.setNow(aIn);
		return f2cQ.getChar();
	}

	public short floatToShort(float aIn) {
		f2sA.setNow(aIn);
		return f2sQ.getShort();
	}

	public int floatToInt(float aIn) {
		f2iA.setNow(aIn);
		return f2iQ.get();
	}

	public long floatToLong(float aIn) {
		f2lA.setNow(aIn);
		return f2lQ.getLong();
	}

	/**
	 * Degenerate case which is purely a pass through.
	 */
	public float floatToFloat(float aIn) {
		return aIn;
	}

	public double floatToDouble(float aIn) {
		f2dA.setNow(aIn);
		return f2dQ.getDouble();
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
			return 32;
		}

		int outWidth() {
			return 32;
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
			return "remCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class DivWriter extends DefaultWriter {
		public static String getName() {
			return "divCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class MulWriter extends DefaultWriter {
		public static String getName() {
			return "mulCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class SubWriter extends DefaultWriter {
		public static String getName() {
			return "subCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class AddWriter extends DefaultWriter {
		public static String getName() {
			return "addCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class MinusWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "minusCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class GteWriter extends DefaultWriter {
		public static String getName() {
			return "gteCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class GtWriter extends DefaultWriter {
		public static String getName() {
			return "gtCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class LteWriter extends DefaultWriter {
		public static String getName() {
			return "lteCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class LtWriter extends DefaultWriter {
		public static String getName() {
			return "ltCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class NeWriter extends DefaultWriter {
		public static String getName() {
			return "neCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class EqWriter extends DefaultWriter {
		public static String getName() {
			return "eqCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class B2fWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "b2fCore";
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

	private static class C2fWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "c2fCore";
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

	private static class S2fWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "s2fCore";
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

	private static class I2fWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "i2fCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class L2fWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "l2fCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int inWidth() {
			return 64;
		}
	}

	private static class F2bWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "f2bCore";
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

	private static class F2cWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "f2cCore";
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

	private static class F2sWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "f2sCore";
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

	private static class F2iWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "f2iCore";
		}

		@Override
		String name() {
			return getName();
		}
	}

	private static class F2lWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "f2lCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 64;
		}
	}

	private static class F2dWriter extends DefaultWriterOneInput {
		public static String getName() {
			return "f2dCore";
		}

		@Override
		String name() {
			return getName();
		}

		@Override
		int outWidth() {
			return 64;
		}
	}

}
