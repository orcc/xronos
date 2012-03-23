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

package net.sf.openforge.lim.io;

/**
 * FifoID is a class that is simply used to completely encapsulate the syntax
 * and format of the encoded String names that are used by the C Model to create
 * 'stub' functions which are used as place holders until LIM construction for
 * FIFO access nodes. This class can convert the relevant FIFO characteristics
 * into an encoded string function name and vice-versa.
 * 
 * 
 * <p>
 * Created: Mon Dec 15 12:48:56 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoID.java 236 2006-07-20 16:57:10Z imiller $
 */
public abstract class FifoID {

	public static final int TYPE_FSL = 1;
	public static final int TYPE_ACTION_SCALAR = 2;
	public static final int TYPE_ACTION_NATIVE_SCALAR = 3;
	public static final int TYPE_ACTION_OBJECT = 4;

	private static final String READ_PREFIX = "fifoRead";
	private static final String WRITE_PREFIX = "fifoWrite";

	// These are the tokens used to identify the relevent
	// characteristics of the FifoID from the encoded string.
	private static final String DELIMITER = "&";
	private static final String EQUALS = "=";
	private static final String ID = "ID";
	private static final String WIDTH = "WIDTH";
	private static final String DIR = "DIR";
	private static final String TYPE = "TYPE";

	// private int interfaceByteWidth = -1;
	private int interfaceWidth = -1;
	private String interfaceName = "unspecified";
	private boolean isInput = true;
	private int interfaceType = TYPE_FSL;

	/**
	 * Generates a default state FifoID instance that can be populated as
	 * needed.
	 */
	public FifoID() {
	}

	/**
	 * Generates a fully 'populated' FifoID instance from a legally encoded
	 * string name {@link #getEncodedID}, {@link #getReadID}, or
	 * {@link #getWriteID}.
	 * 
	 * @param encoded
	 *            a value of type 'String'
	 * @throws IllegalArgumentException
	 *             if the encoded string is not a legally encoded string.
	 */
	public FifoID(String encoded) {

		if (encoded.indexOf(DELIMITER) < 0) {
			throw new IllegalArgumentException(
					"Unknown syntax for encoded fifo identifier");
		}

		String tokens[] = encoded.split(DELIMITER);
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if (token.equals(READ_PREFIX) || token.equals(WRITE_PREFIX)) {
				// Skip the read and write prefixes that may be applied.
				continue;
			}

			// The token should now be of the form KEY=VALUE
			String keyValue[] = token.split(EQUALS);
			assert keyValue.length == 2;

			if (keyValue[0].equals(ID)) {
				// this.interfaceNumber = new Integer(keyValue[1]).intValue();
				this.interfaceName = keyValue[1];
			} else if (keyValue[0].equals(WIDTH)) {
				this.interfaceWidth = new Integer(keyValue[1]).intValue();
			} else if (keyValue[0].equals(DIR)) {
				this.isInput = new Boolean(keyValue[1]).booleanValue();
			} else if (keyValue[0].equals(TYPE)) {
				this.interfaceType = new Integer(keyValue[1]).intValue();
			} else {
				throw new IllegalArgumentException(
						"Illegal Syntax for Fifo Identification " + encoded
								+ " Unknown key: " + token);
			}
		}
	}

	/**
	 * Sets the specified value as the width (in bytes) of the Fifo interface.
	 * 
	 * @param width
	 *            a positive int value.
	 * @throws IllegalArgumentException
	 *             if width is 0 or negative.
	 */
	public void setByteWidth(int width) {
		if (width <= 0) {
			throw new IllegalArgumentException("Fifo byte width must be > 0");
		}

		// this.interfaceByteWidth = width;
		setBitWidth(width * 8);
	}

	/**
	 * Returns the defined width of the Fifo interface in bytes.
	 * 
	 * @return a positive int, the byte width of the interface.
	 */
	public int getByteWidth() {
		return this.interfaceWidth / 8;
	}

	/**
	 * Sets the width in bits.
	 * 
	 * @param width
	 *            a value of type 'int'
	 */
	public void setBitWidth(int width) {
		if (width <= 0) {
			throw new IllegalArgumentException("Fifo bit width must be > 0");
		}

		this.interfaceWidth = width;
	}

	/**
	 * Gets the width in bits.
	 * 
	 * @param width
	 *            a value of type 'int'
	 */
	public int getBitWidth() {
		return this.interfaceWidth;
	}

	/**
	 * Sets the id number of this FifoID. ID numbers are used to uniquely
	 * identify one fifo interface from another in the design.
	 * 
	 * @param id
	 *            any String
	 */
	public void setID(String id) {
		this.interfaceName = id;
	}

	/**
	 * Returns the unique interface identifying name for this identifier.
	 * 
	 * @return a 'String'
	 */
	public String getID() {
		return this.interfaceName;
	}

	public void setType(int type) {
		if ((type != TYPE_FSL) && (type != TYPE_ACTION_SCALAR)
				&& (type != TYPE_ACTION_NATIVE_SCALAR)
				&& (type != TYPE_ACTION_OBJECT)) {
			throw new IllegalArgumentException(
					"Unknown interface type specified " + type);
		}
		this.interfaceType = type;
	}

	/**
	 * Returns the type of fifo interface represented by this id class.
	 * 
	 * @return an int, matching one of the public static final ints in this
	 *         class.
	 */
	public int getType() {
		return this.interfaceType;
	}

	/**
	 * Return the unique name (prefix) for this identified interface, eg FSLnum,
	 * etc.
	 * 
	 * @return a value of type 'String'
	 */
	public abstract String getName();

	/**
	 * Sets the direction of the identified Fifo interface, true if the Fifo
	 * interface is an input to the design, false otherwise.
	 * 
	 * @param read
	 *            a value of type 'boolean'
	 */
	public void setDirection(boolean read) {
		this.isInput = read;
	}

	/**
	 * Returns true if this identifier specifies an input (read) fifo interface
	 * .
	 * 
	 * @return a 'boolean'
	 */
	public boolean isInputFifo() {
		return this.isInput;
	}

	/**
	 * Returns an encoded String representation of this FifoID which
	 * incorporates all the relevent characteristics set via the 'setXXX'
	 * methods. This string is parseable by the constructor of this class which
	 * accepts a string.
	 * 
	 * @return an encoded, non-empty, String
	 */
	private String getEncodedID() {
		// &dir=XXXX&id=YYYY&width=ZZZZ&type=QQQQ
		String encoded = DELIMITER + DIR + EQUALS + this.isInput;
		encoded += DELIMITER + ID + EQUALS + this.interfaceName;
		encoded += DELIMITER + WIDTH + EQUALS + this.interfaceWidth;
		encoded += DELIMITER + TYPE + EQUALS + this.interfaceType;
		return encoded;
	}

	/**
	 * Returns a String which fully encodes all the properties of a read access
	 * to the Fifo indicated by this FifoID
	 * 
	 * @return a value of type 'String'
	 * @throws UnsupportedOperationException
	 *             if the direction of this fifo is not input.
	 */
	public String getReadID() {
		if (!this.isInput) {
			throw new UnsupportedOperationException(
					"Cannot read from an output fifo interface");
		}
		return READ_PREFIX + this.getEncodedID();
	}

	/**
	 * Returns a String which fully encodes all the properties of a write access
	 * to the Fifo indicated by this FifoID
	 * 
	 * @return a value of type 'String'
	 * @throws UnsupportedOperationException
	 *             if the direction of this fifo is not output.
	 */
	public String getWriteID() {
		if (this.isInput) {
			throw new UnsupportedOperationException(
					"Cannot write to an input fifo interface");
		}
		return WRITE_PREFIX + this.getEncodedID();
	}

}// FifoID
