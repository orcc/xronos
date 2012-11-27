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

package org.xronos.openforge.util;

/**
 * HexString has lots of ways to create hex strings out of the basic Java types.
 * <P>
 * 
 * Created: Wed Jun 24 17:39:01 1998
 * 
 * @author Micheal Barr, modified: Jim Jensen, modified: Andreas Kollegger
 * @version $Id: HexString.java 2 2005-06-09 20:00:48Z imiller $
 */
public class HexString {
	private static final char[] HEXDIGITS = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	public static String prefix = "0x";

	/**
	 * Convert a byte of data to a hexadecimal string.
	 * 
	 * @param data
	 *            the byte of data
	 * @param prefixed
	 *            whether or not to include the hexadecimal prefic 0x
	 * @return a hexadecimal representation of the data
	 */
	public static String toHexString(byte data, boolean prefixed) {
		StringBuffer sb = new StringBuffer();

		if (prefixed) {
			sb.append(prefix);
		}

		sb.append(HEXDIGITS[(data >> 4) & 0xF]);
		sb.append(HEXDIGITS[data & 0xF]);

		return (sb.toString());
	}

	/**
	 * Convert a byte of data to a hexadecimal string.
	 * 
	 * @param data
	 *            the byte of data
	 * @return a hexadecimal representation of the data
	 */
	public static String toHexString(byte data) {
		return (toHexString(data, true));
	}

	/**
	 * Convert two bytes of data to a hexadecimal string.
	 * 
	 * @param data
	 *            the bytes of data
	 * @return a hexadecimal representation of the data
	 */
	public static String toHexString(short data) {
		String s = prefix + HEXDIGITS[(data >> 12) & 0xF]
				+ HEXDIGITS[(data >> 8) & 0xF] + HEXDIGITS[(data >> 4) & 0xF]
				+ HEXDIGITS[data & 0xF];

		return (s);
	}

	/**
	 * Convert four bytes of data to a hexadecimal string.
	 * 
	 * @param data
	 *            the bytes of data
	 * @return a hexadecimal representation of the data
	 */
	public static String toHexString(int data) {
		String s = prefix + HEXDIGITS[(data >> 28) & 0xF]
				+ HEXDIGITS[(data >> 24) & 0xF] + HEXDIGITS[(data >> 20) & 0xF]
				+ HEXDIGITS[(data >> 16) & 0xF] + HEXDIGITS[(data >> 12) & 0xF]
				+ HEXDIGITS[(data >> 8) & 0xF] + HEXDIGITS[(data >> 4) & 0xF]
				+ HEXDIGITS[data & 0xF];

		return (s);
	}

	/**
	 * Convert eight bytes of data to a hexadecimal string.
	 * 
	 * @param data
	 *            the bytes of data
	 * @return a hexadecimal representation of the data
	 */
	public static String toHexString(long data) {
		String s = prefix + HEXDIGITS[(int) (data >> 60) & 0xF]
				+ HEXDIGITS[(int) (data >> 56) & 0xF]
				+ HEXDIGITS[(int) (data >> 52) & 0xF]
				+ HEXDIGITS[(int) (data >> 48) & 0xF]
				+ HEXDIGITS[(int) (data >> 44) & 0xF]
				+ HEXDIGITS[(int) (data >> 40) & 0xF]
				+ HEXDIGITS[(int) (data >> 36) & 0xF]
				+ HEXDIGITS[(int) (data >> 32) & 0xF]
				+ HEXDIGITS[(int) (data >> 28) & 0xF]
				+ HEXDIGITS[(int) (data >> 24) & 0xF]
				+ HEXDIGITS[(int) (data >> 20) & 0xF]
				+ HEXDIGITS[(int) (data >> 16) & 0xF]
				+ HEXDIGITS[(int) (data >> 12) & 0xF]
				+ HEXDIGITS[(int) (data >> 8) & 0xF]
				+ HEXDIGITS[(int) (data >> 4) & 0xF]
				+ HEXDIGITS[(int) data & 0xF];

		return (s);
	}

	/**
	 * Convert a byte array of data to a hexadecimal string.
	 * 
	 * @param data
	 *            the array of data bytes
	 * @return a hexadecimal representation of the data
	 */

	public static String toHexString(byte[] data) {
		String s = "";

		for (int i = 0; i < data.length; i++) {
			s += toHexString(data[i]) + " ";
		}

		return (s);
	}

	/**
	 * Convert a byte array of data to a hexadecimal string.
	 * 
	 * @param data
	 *            the array of data bytes
	 * @param prefixed
	 *            whether or not to add the 0x prefix to each byte
	 * @return a hexadecimal representation of the data
	 */

	public static String toHexString(byte[] data, boolean prefixed) {
		String s = "";

		for (int i = 0; i < data.length; i++) {
			s += toHexString(data[i], prefixed) + " ";
		}

		return (s);
	}

	/**
	 * Convert a hex string to an unsigned byte value. The string may have a
	 * hexadecimal prefix.
	 * 
	 * @param string
	 *            the string to convert
	 * @return the converted byte value
	 */
	public static byte toByte(String data) throws NumberFormatException {
		// int current_digit = 0;
		int int_value = Integer.parseInt(data, 16);

		byte byte_value = (byte) (int_value);

		return byte_value;
	}

	/**
	 * Convert a hexadecimal string (as formatted by toHexString(data, false))
	 * to a byte array.
	 * 
	 * @param string
	 *            the string of hex data
	 * @return the byte array representation
	 */
	public static byte[] toByteArray(String data) throws NumberFormatException {
		int space_finder = 0;
		int previous_space = space_finder;
		int byte_count = 0;

		data = data.trim();

		while ((space_finder = data.indexOf(' ', space_finder + 1)) > 0) {
			byte_count++;
		}

		byte_count += 1;

		byte[] result_bytes = new byte[byte_count];

		String byte_substring = null;

		space_finder = 0;

		for (int i = 0; i < byte_count; i++) {
			space_finder = data.indexOf(' ', space_finder + 1);

			if (space_finder > 0) {
				byte_substring = data.substring(previous_space, space_finder);
			} else {
				byte_substring = data.substring(previous_space);
			}

			result_bytes[i] = HexString.toByte(byte_substring);
			previous_space = space_finder + 1;
		}

		return result_bytes;
	}

	/**
	 * For testing only.
	 */
	public static void main(String[] args) {
		byte[] test_data = new byte[] { (byte) 0x0f, (byte) 0x80, (byte) 0x00,
				(byte) 0x05, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
				(byte) 0x61, (byte) 0x69, (byte) 0x6e, (byte) 0x67,
				(byte) 0x2e, (byte) 0x2e, (byte) 0x43, (byte) 0x6f,
				(byte) 0x6c, (byte) 0x52, (byte) 0x65, (byte) 0x73,
				(byte) 0x6f, (byte) 0x6b, (byte) 0x53, (byte) 0xf9,
				(byte) 0x9f, (byte) 0x02, (byte) 0x00, (byte) 0x00,
				(byte) 0x78, (byte) 0x61, (byte) 0x76, (byte) 0x61,
				(byte) 0x2e, (byte) 0x43, (byte) 0x6f, (byte) 0x6c,
				(byte) 0x6f, (byte) 0x83, (byte) 0x10, (byte) 0x8f,
				(byte) 0x33, (byte) 0x46, (byte) 0x00, (byte) 0x06,
				(byte) 0x66, (byte) 0x61, (byte) 0x49, (byte) 0x00,
				(byte) 0x05, (byte) 0x65, (byte) 0x4c, (byte) 0x00,
				(byte) 0x02, (byte) 0x1b, (byte) 0x4c, (byte) 0x6a,
				(byte) 0x61, (byte) 0x77, (byte) 0x74, (byte) 0x2f,
				(byte) 0x63, (byte) 0x2f, (byte) 0x43, (byte) 0x6f,
				(byte) 0x6c, (byte) 0x61, (byte) 0x63, (byte) 0x65,
				(byte) 0x3b, (byte) 0x72, (byte) 0x67, (byte) 0x62,
				(byte) 0x76, (byte) 0x74, (byte) 0x00, (byte) 0x02,
				(byte) 0x5b, (byte) 0x66, (byte) 0x76, (byte) 0x61,
				(byte) 0x6c, (byte) 0x7e, (byte) 0x00, (byte) 0x03,
				(byte) 0x4c, (byte) 0x65, (byte) 0x43, (byte) 0x6f,
				(byte) 0x6e, (byte) 0x74, (byte) 0x00, (byte) 0x17,
				(byte) 0x4c, (byte) 0x2f, (byte) 0x61, (byte) 0x77,
				(byte) 0x74, (byte) 0x6e, (byte) 0x74, (byte) 0x43,
				(byte) 0x6f, (byte) 0x74, (byte) 0x3b, (byte) 0x78,
				(byte) 0x70, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		String test_data_encoded = HexString.toHexString(test_data, false);

		System.out.println("\nEncoded byte array: " + test_data_encoded);

		byte[] decoded_data = HexString.toByteArray(test_data_encoded);

		String decoded_data_encoded = HexString
				.toHexString(decoded_data, false);

		System.out.println("\nRe-encoded byte array: " + decoded_data_encoded);

		String spaced_hex_data = "ba be ca fe ";

		String tight_hex_data = "ba be ca fe";

		decoded_data = HexString.toByteArray(spaced_hex_data);

		decoded_data_encoded = HexString.toHexString(decoded_data, false);

		System.out.println("\nSpaced hex data: " + decoded_data_encoded);

		decoded_data = HexString.toByteArray(tight_hex_data);

		decoded_data_encoded = HexString.toHexString(decoded_data, false);

		System.out.println("\nTight hex data: " + decoded_data_encoded);

	}

	/**
	 * Converts the standard primitive wrapper objects to a hex value
	 * representation.
	 * 
	 * @param value
	 *            the object wrapper
	 * @param size
	 *            the desired bitwidth
	 * @return the hex representation
	 */
	public static String valueToHex(Object value, int size) {
		String constant_hex;

		if (value instanceof Long) {
			constant_hex = longToHex(((Long) value).longValue(), size);
		} else if (value instanceof Integer) {
			if (size <= 32) {
				constant_hex = intToHex(((Integer) value).intValue(), size);
			} else {
				constant_hex = longToHex(((Integer) value).longValue(), size);
			}
		} else if (value instanceof Short) {
			constant_hex = shortToHex(((Short) value).shortValue(), size);
		} else if (value instanceof Byte) {
			constant_hex = byteToHex(((Byte) value).byteValue(), size);
		} else if (value instanceof Character) {
			constant_hex = shortToHex(
					(short) (((Character) value).charValue()), size);
		} else if (value instanceof Boolean) {
			if (((Boolean) value).booleanValue()) {
				constant_hex = "1";
			} else {
				constant_hex = "0";
			}
		} else {
			constant_hex = value.toString();
		}

		return constant_hex;

	} // valueToHex()

	/**
	 * Converts the standard primitive wrapper objects to a hex value
	 * representation using the minimum bitwidth needed to represent the value.
	 * 
	 * @param value
	 *            the object wrapper
	 * @return the hex representation
	 */
	public static String valueToHex(Object value) {
		long l = objectToLong(value);
		long shifter = l;

		int bits = 1;

		if (shifter >= 0) {
			while (shifter != 0) {
				shifter = shifter >>> 1;
				bits++;
			}
		} else {
			while (shifter != 0xFFFFFFFF) {
				shifter = shifter >> 1;
				bits++;
			}
		}

		return longToHex(l, bits);

	} // valueToHex()

	/**
	 * Converts the standard primitive wrapper objects to a long value
	 * representation.
	 * 
	 * @param value
	 *            the object wrapper
	 * @param size
	 *            the desired bitwidth
	 * @return the long representation
	 */
	public static long objectToLong(Object value) {
		long reply = -1;

		if (value instanceof Long) {
			reply = ((Long) value).longValue();
		} else if (value instanceof Integer) {
			reply = ((Integer) value).intValue();
		} else if (value instanceof Short) {
			reply = ((Short) value).shortValue();
		} else if (value instanceof Byte) {
			reply = ((Byte) value).byteValue();
		} else if (value instanceof Character) {
			reply = ((Character) value).charValue();
		} else if (value instanceof Boolean) {
			if (((Boolean) value).booleanValue()) {
				reply = 1;
			} else {
				reply = 0;
			}
		}

		return reply;

	} // objectToLong()

	/**
	 * Inverts, and then converts the standard primitive wrapper objects to a
	 * hex value representation.
	 * 
	 * @param value
	 *            the object wrapper
	 * @param size
	 *            the desired bitwidth
	 * @return the hex representation
	 */
	public static String invertValueToHex(Object value, int size) {
		String constant_hex;

		if (value instanceof Long) {
			constant_hex = longToHex(~((Long) value).longValue(), size);
		} else if (value instanceof Integer) {
			constant_hex = intToHex(~((Integer) value).intValue(), size);
		} else if (value instanceof Short) {
			constant_hex = shortToHex((short) ~((Short) value).shortValue(),
					size);
		} else if (value instanceof Byte) {
			constant_hex = byteToHex((byte) ~((Byte) value).byteValue(), size);
		} else if (value instanceof Character) {
			constant_hex = charToHex(
					(short) ~(Character.getNumericValue(((Character) value)
							.charValue())), size);
		} else if (value instanceof Boolean) {
			if (((Boolean) value).booleanValue()) {
				constant_hex = "0";
			} else {
				constant_hex = "1";
			}
		} else {
			constant_hex = value.toString();
		}

		return constant_hex;

	} // valueToHex()

	/**
	 * Converts an integer value to an exactly sized hex representation.
	 * 
	 * @param value
	 *            the integer value
	 * @param size
	 *            the desired bitwidth
	 * @return the hex representation
	 */
	public static String intToHex(int value, int size) {
		String reply;

		int size_mask = 0;

		for (int i = 0; i < size; i++) {
			size_mask |= 1L << i;
		}

		if (size < 32) {
			reply = new String(Integer.toHexString(value & size_mask));
		} else {
			reply = new String(Integer.toHexString(value));
		}

		return reply;
	} // intToHex()

	/**
	 * Converts a short value to an exactly sized hex representation.
	 * 
	 * @param value
	 *            the short value
	 * @param size
	 *            the desired bitwidth
	 * @return the hex representation
	 */
	public static String shortToHex(short value, int size) {
		String reply;

		short size_mask = 0;

		for (int i = 0; i < size; i++) {
			size_mask |= 1L << i;
		}

		if (size < 16) {
			reply = new String(Integer.toHexString((value & size_mask)));
		} else {
			reply = longToHex(value, size);
		}

		return reply;
	} // shortToHex()

	/**
	 * Converts a character value to an exactly sized hex representation.
	 * 
	 * @param value
	 *            the character value, as a short
	 * @param size
	 *            the desired bitwidth
	 * @return the hex representation
	 */
	public static String charToHex(short value, int size) {
		String reply;

		short size_mask = 0;

		for (int i = 0; i < size; i++) {
			size_mask |= 1L << i;
		}

		if (size < 16) {
			reply = new String(Integer.toHexString((value & size_mask)));
		} else {
			reply = longToHex((long) value & 0xFFFF, size);
		}

		return reply;
	} // shortToHex()

	/**
	 * Converts a byte value to an exactly sized hex representation.
	 * 
	 * @param value
	 *            the byte value
	 * @param size
	 *            the desired bitwidth
	 * @return the hex representation
	 */
	public static String byteToHex(byte value, int size) {
		String reply;

		byte size_mask = 0;

		for (int i = 0; i < size; i++) {
			size_mask |= 1L << i;
		}

		if (size < 8) {
			reply = new String(Integer.toHexString((value & size_mask)));
		} else {
			reply = longToHex(value, size);
		}

		return reply;
	} // byteToHex()

	/**
	 * Converts a long value to an exactly sized hex representation.
	 * 
	 * @param value
	 *            the i]long value
	 * @param size
	 *            the desired bitwidth
	 * @return the hex representation
	 */
	public static String longToHex(long value, int size) {
		String reply;

		long size_mask = 0;

		for (int i = 0; i < size; i++) {
			size_mask |= 1L << i;
		}

		if (size < 64) {
			reply = new String(Long.toHexString(value & size_mask));
		} else {
			reply = new String(Long.toHexString(value));
		}

		return reply;
	} // intToHex()

} // HexString

/*--- formatting done in "Lavalogic Coding Convention" style on 11-23-1999 ---*/

