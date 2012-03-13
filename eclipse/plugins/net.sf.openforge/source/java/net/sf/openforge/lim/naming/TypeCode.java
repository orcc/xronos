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

package net.sf.openforge.lim.naming;

import net.sf.openforge.util.naming.IDSourceInfo;

/**
 * TypeCode provides a look-up for the string representation of data types.
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: TypeCode.java 2 2005-06-09 20:00:48Z imiller $
 */
public class TypeCode {

	public static final TypeCode BOOLEAN = new TypeCode("Z");
	public static final TypeCode CHAR = new TypeCode("C");
	public static final TypeCode SCHAR = new TypeCode("H");
	public static final TypeCode UCHAR = new TypeCode("UC");
	public static final TypeCode BYTE = new TypeCode("B");
	public static final TypeCode SHORT = new TypeCode("S");
	public static final TypeCode USHORT = new TypeCode("US");
	public static final TypeCode INT = new TypeCode("I");
	public static final TypeCode UINT = new TypeCode("UI");
	public static final TypeCode LONG = new TypeCode("L");
	public static final TypeCode ULONG = new TypeCode("UL");
	public static final TypeCode LONGLONG = new TypeCode("X");
	public static final TypeCode ULONGLONG = new TypeCode("UX");
	public static final TypeCode FLOAT = new TypeCode("F");
	public static final TypeCode DOUBLE = new TypeCode("D");
	public static final TypeCode LONGDOUBLE = new TypeCode("P");
	public static final TypeCode VOID = new TypeCode("V");

	private String code;

	private TypeCode(String code) {
		this.code = code;
	}

	/**
	 * Produces a string which is appropriate for representing an array of the
	 * given type.
	 */
	public static TypeCode forArrayOf(TypeCode base) {
		return new TypeCode("arrayof" + base.toString());
	}

	/**
	 * Produces a string which is appropriate for representing an array of the
	 * given type.
	 */
	public static TypeCode forPointerTo(TypeCode base) {
		return new TypeCode("pointerto" + base.toString());
	}

	/**
	 * Produces a String which is appropriate for representing a class type.
	 * 
	 * @param info
	 *            the IDSourceInfo which identifies the class
	 */
	public static TypeCode forClassOf(IDSourceInfo info) {
		return new TypeCode(info.getFullyQualifiedName());
	}

	public String toString() {
		return code;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TypeCode))
			return false;
		if (o.equals(null))
			return false;
		return toString().equals(o.toString());
	}

	public int hashCode() {
		return code.hashCode();
	}

	/**
	 * Converts a List of TypeCode into a procedure signature.
	 * 
	 * @param typecodes
	 *            the array of typecodes
	 * @return String the procedure signature
	 */
	public static String signatureFor(TypeCode[] typecodes) {
		StringBuffer signature = new StringBuffer();
		for (int i = 0; i < typecodes.length; i++) {
			signature.append(typecodes[i].toString());
		}
		return signature.toString();
	}

}
