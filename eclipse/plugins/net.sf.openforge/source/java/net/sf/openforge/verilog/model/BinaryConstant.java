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
package net.sf.openforge.verilog.model;

/**
 * BinaryConstant is a part of Forge
 * 
 * <P>
 * 
 * Created: Thu Mar 01 2001
 * 
 * @author abk
 * @version $Id: BinaryConstant.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BinaryConstant extends Constant {

	String bitstring = null;

	public BinaryConstant(Number n) {
		super(n);
	}

	public BinaryConstant(String binary) {
		super(parseBinary(binary));
		setString(binary, compactString(binary).length());
	} // BinaryConstant()

	public BinaryConstant(String binary, int size) {
		super(parseBinary(binary), size);
		setString(binary, size);
	} // BinaryConstant()

	public BinaryConstant(long l) {
		super(l);
	}

	public BinaryConstant(long l, int size) {
		super(l, size);
	}

	public int getRadix() {
		return BINARY;
	}

	public String getToken() {
		if (bitstring == null) {
			bitstring = Long.toBinaryString(getValue().longValue());
		}
		return bitstring;
	}

	private void setString(String binary, int length) {
		boolean isMixed = false;
		binary = compactString(binary);
		char[] binary_chars = binary.toCharArray();

		StringBuffer bit_buffer = new StringBuffer();
		for (int i = 1; i <= length; i++) {
			int j = binary_chars.length - i;
			char c = (j < 0) ? binary_chars[0] : binary_chars[j];
			bit_buffer.insert(0, c);
			if ((c == 'z') || (c == 'Z') || (c == 'x') || (c == 'X')) {
				isMixed = true;
			}
		}

		if (isMixed) {
			bitstring = bit_buffer.toString();
		}
	}

	/**
	 * Removes whitespace and underscore characters from a String.
	 */
	public static String compactString(String binary) {
		String compact = binary.trim();
		compact = compact.replaceAll("_", "");
		return compact;
	}

	/**
	 * Parses a String as a binary value, returning the value as a long.
	 * 
	 * @param binary
	 *            the string to parse
	 * @return the parsed value, or just 0 (zero) if the bits are mixed (Z or X
	 *         with 0 and 1)
	 */
	public static long parseBinary(String binary) {
		long result = 0;
		if (!verify(binary))
			throw new NumberFormatException(binary);

		try {
			binary = compactString(binary);
			result = Long.parseLong(binary, BINARY);
		} catch (NumberFormatException nfe) {
			result = 0;
		}
		;

		return result;
	}

	/**
	 * Verifies that a given string can be used as a valid binary
	 * representation.
	 * 
	 */
	public static boolean verify(String binary) {
		boolean valid = true;

		char[] binary_chars = binary.toCharArray();

		if (binary_chars.length > 0) {
			valid = isBitCharacter(binary_chars[0]);

			if (valid) {
				for (int i = 1; i < binary_chars.length; i++) {
					char c = binary_chars[i];

					if (!(isBitCharacter(c) || (c == '_'))) {
						valid = false;
						break;
					}
				}
			}
		}

		return valid;

	} // verify()

	public static boolean isBitCharacter(char c) {
		return ((c == 'Z') || (c == 'z') || (c == 'x') || (c == 'X')
				|| (c == '1') || (c == '0'));
	}

	public static boolean isMixed(String binary) {
		boolean mixed = false;

		return mixed;
	}
} // end of class BinaryConstant
