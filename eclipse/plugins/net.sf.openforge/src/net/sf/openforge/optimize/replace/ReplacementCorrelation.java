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

package net.sf.openforge.optimize.replace;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.SubtractOp;

/**
 * ReplacementCorrelation maintains the correlation between the LIM component
 * (class of that component) and the name of the method that is searched for in
 * the Operation Replacement libraries. Any new components to be supported
 * should simply add entries into the static mapping of class to instance of
 * ReplacementCorrelation.
 * 
 * <p>
 * Created: Thu Mar 27 11:00:02 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ReplacementCorrelation.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ReplacementCorrelation {

	/**
	 * A map of Class object to ReplacementCorrelation. Each entry defines one
	 * type of lim component that can be replaced from the user defined
	 * libraries.
	 */
	private static Map<Class<?>, ReplacementCorrelation> classToCorrelation;
	static {
		classToCorrelation = new LinkedHashMap<Class<?>, ReplacementCorrelation>();

		classToCorrelation.put(ModuloOp.class, new ReplacementCorrelation(
				"modulo", "rem", 16));
		classToCorrelation.put(DivideOp.class, new ReplacementCorrelation(
				"division", "div", 15));
		classToCorrelation.put(MultiplyOp.class, new ReplacementCorrelation(
				"multiplication", "mult", 14));
		classToCorrelation.put(SubtractOp.class, new ReplacementCorrelation(
				"subtraction", "sub", 13));
		classToCorrelation.put(AddOp.class, new ReplacementCorrelation(
				"addition", "add", 12));
		classToCorrelation.put(MinusOp.class, new ReplacementCorrelation(
				"negation", "minus", 11));

		classToCorrelation.put(GreaterThanEqualToOp.class,
				new ReplacementCorrelation("greater than equal",
						"greaterThanEqual", 19));
		classToCorrelation.put(GreaterThanOp.class, new ReplacementCorrelation(
				"greater than", "greaterThan", 18));
		classToCorrelation.put(LessThanEqualToOp.class,
				new ReplacementCorrelation("less than equal", "lessThanEqual",
						17));
		classToCorrelation.put(LessThanOp.class, new ReplacementCorrelation(
				"less than", "lessThan", 16));
		classToCorrelation.put(NotEqualsOp.class, new ReplacementCorrelation(
				"not equal", "notEqual", 15));
		classToCorrelation.put(EqualsOp.class, new ReplacementCorrelation(
				"equal", "equal", 14));

		final String b = "byte";
		final String c = "char";
		final String s = "short";
		final String i = "int";
		final String l = "long";
		final String f = "float";
		final String d = "double";
		//
		// NOTE, Some conversions may come in as cast _or_ numeric promotions
		//
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(b, f)
				.getClass(), new ReplacementCorrelation("byte to float",
				"byteToFloat", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(c, f)
				.getClass(), new ReplacementCorrelation("char to float",
				"charToFloat", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(s, f)
				.getClass(), new ReplacementCorrelation("short to float",
				"shortToFloat", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(i, f)
				.getClass(), new ReplacementCorrelation("int to float",
				"intToFloat", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(l, f)
				.getClass(), new ReplacementCorrelation("long to float",
				"longToFloat", 13));

		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(b, d)
				.getClass(), new ReplacementCorrelation("byte to double",
				"byteToDouble", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(c, d)
				.getClass(), new ReplacementCorrelation("char to double",
				"charToDouble", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(s, d)
				.getClass(), new ReplacementCorrelation("short to double",
				"shortToDouble", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(i, d)
				.getClass(), new ReplacementCorrelation("int to double",
				"intToDouble", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(l, d)
				.getClass(), new ReplacementCorrelation("long to double",
				"longToDouble", 13));
		classToCorrelation.put(NumericPromotionOp.getNumericPromotionOp(f, d)
				.getClass(), new ReplacementCorrelation("float to double",
				"floatToDouble", 13));

		// Cast ops need a size/signedness. Since all we care about
		// is the class, just use 32 and true.
		classToCorrelation.put(CastOp.getCastOp(b, f, 32, true).getClass(),
				new ReplacementCorrelation("byte to float", "byteToFloat", 13));
		classToCorrelation.put(CastOp.getCastOp(c, f, 32, true).getClass(),
				new ReplacementCorrelation("char to float", "charToFloat", 13));
		classToCorrelation
				.put(CastOp.getCastOp(s, f, 32, true).getClass(),
						new ReplacementCorrelation("short to float",
								"shortToFloat", 13));
		classToCorrelation.put(CastOp.getCastOp(i, f, 32, true).getClass(),
				new ReplacementCorrelation("int to float", "intToFloat", 13));
		classToCorrelation.put(CastOp.getCastOp(l, f, 32, true).getClass(),
				new ReplacementCorrelation("long to float", "longToFloat", 13));

		classToCorrelation
				.put(CastOp.getCastOp(b, d, 32, true).getClass(),
						new ReplacementCorrelation("byte to double",
								"byteToDouble", 13));
		classToCorrelation
				.put(CastOp.getCastOp(c, d, 32, true).getClass(),
						new ReplacementCorrelation("char to double",
								"charToDouble", 13));
		classToCorrelation.put(CastOp.getCastOp(s, d, 32, true).getClass(),
				new ReplacementCorrelation("short to double", "shortToDouble",
						13));
		classToCorrelation.put(CastOp.getCastOp(i, d, 32, true).getClass(),
				new ReplacementCorrelation("int to double", "intToDouble", 13));
		classToCorrelation
				.put(CastOp.getCastOp(l, d, 32, true).getClass(),
						new ReplacementCorrelation("long to double",
								"longToDouble", 13));
		classToCorrelation.put(CastOp.getCastOp(f, d, 32, true).getClass(),
				new ReplacementCorrelation("float to double", "floatToDouble",
						13));

		classToCorrelation.put(CastOp.getCastOp(f, b, 32, true).getClass(),
				new ReplacementCorrelation("float to byte", "floatToByte", 13));
		classToCorrelation.put(CastOp.getCastOp(f, c, 32, true).getClass(),
				new ReplacementCorrelation("float to char", "floatToChar", 13));
		classToCorrelation
				.put(CastOp.getCastOp(f, s, 32, true).getClass(),
						new ReplacementCorrelation("float to short",
								"floatToShort", 13));
		classToCorrelation.put(CastOp.getCastOp(f, i, 32, true).getClass(),
				new ReplacementCorrelation("float to int", "floatToInt", 13));
		classToCorrelation.put(CastOp.getCastOp(f, l, 32, true).getClass(),
				new ReplacementCorrelation("float to long", "floatToLong", 13));

		classToCorrelation
				.put(CastOp.getCastOp(d, b, 32, true).getClass(),
						new ReplacementCorrelation("double to byte",
								"doubleToByte", 13));
		classToCorrelation
				.put(CastOp.getCastOp(d, c, 32, true).getClass(),
						new ReplacementCorrelation("double to char",
								"doubleToChar", 13));
		classToCorrelation.put(CastOp.getCastOp(d, s, 32, true).getClass(),
				new ReplacementCorrelation("double to short", "doubleToShort",
						13));
		classToCorrelation.put(CastOp.getCastOp(d, i, 32, true).getClass(),
				new ReplacementCorrelation("double to int", "doubleToInt", 13));
		classToCorrelation
				.put(CastOp.getCastOp(d, l, 32, true).getClass(),
						new ReplacementCorrelation("double to long",
								"doubleToLong", 13));
		classToCorrelation.put(CastOp.getCastOp(d, f, 32, true).getClass(),
				new ReplacementCorrelation("double to float", "doubleToFloat",
						13));

		classToCorrelation.put(CastOp.getCastOp(f, f, 32, true).getClass(),
				new ReplacementCorrelation("float to float", "doubleToDouble",
						13));
		classToCorrelation.put(CastOp.getCastOp(d, d, 32, true).getClass(),
				new ReplacementCorrelation("double to double", "floatToFloat",
						13));
	}

	/**
	 * A name, presentable to the user, to identify the type of operation that
	 * this correlation defines a replacement method name for.
	 */
	private String replacedOpDesc;

	/**
	 * The case INSENSITIVE method name that the user must implement to replace
	 * the specified component (class of LIM).
	 */
	private String methodName;

	/**
	 * A rank of the operations complexity. The higher the value the more
	 * complex.
	 */
	private int complexity = 0;

	private ReplacementCorrelation(String replaces, String methodName,
			int complexity) {
		replacedOpDesc = replaces;
		this.methodName = methodName;
		this.complexity = complexity;
	}

	/**
	 * Retrieves the ReplacementCorrelation defined for the given components
	 * type, or null if the component is not replaceable.
	 * 
	 * @param comp
	 *            a value of type 'Component'
	 * @return a value of type 'ReplacementCorrelation'
	 */
	public static ReplacementCorrelation getCorrelation(Component comp) {
		return getCorrelation(comp.getClass());
	}

	/**
	 * Returns true if the specified component has a mapping from its class to
	 * an instance of ReplacementCorrelation stored.
	 */
	public static boolean isReplaceable(Component comp) {
		return getCorrelation(comp) != null;
	}

	/**
	 * Retrieves the ReplacementCorrelation defined for the given class, or null
	 * if the class does not have a defined ReplacementCorrelation.
	 * 
	 * @param clazz
	 *            a value of type 'Class'
	 * @return a value of type 'ReplacementCorrelation'
	 */
	public static ReplacementCorrelation getCorrelation(
			Class<? extends Component> clazz) {
		return classToCorrelation.get(clazz);
	}

	/**
	 * Returns a String that describes which components are replaceable
	 * according to the currently defined correlations. The string has only
	 * <u>very</u> rudimentary formatting.
	 * 
	 * @return a value of type 'String'
	 */
	public static String getHelpString() {
		String s = "";
		for (Iterator<ReplacementCorrelation> iter = classToCorrelation
				.values().iterator(); iter.hasNext();) {
			ReplacementCorrelation corr = iter.next();
			s += corr.getHelpDescription();
			if (iter.hasNext())
				s += ", ";
		}
		return s;
	}

	/**
	 * Retrieves the case <b>insensitive</b> method name to use when searching
	 * the user defined libraries for an implementation to replace the component
	 * used to retrieve this correlation.
	 * 
	 * @return a value of type 'String'
	 */
	public String getReplacementMethodName() {
		return methodName;
	}

	/**
	 * Returns the numerical 'rank' of complexity of the operation characterized
	 * by this class, divide and remainder are the most complex while bitwise
	 * ops (and/or/xor) are the least complex.
	 * 
	 * @return a non-negative int
	 */
	public int getComplexityRank() {
		return complexity;
	}

	/**
	 * Returns a very short (1-3 words) description of the type of operation
	 * tracked by this correlation.
	 * 
	 * @return a value of type 'String'
	 */
	String getReplacedOperationDescription() {
		return replacedOpDesc;
	}

	private String getHelpDescription() {
		return getReplacedOperationDescription() + " => "
				+ getReplacementMethodName();
	}

}// ReplacementCorrelation
