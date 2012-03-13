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
 * BlockElement describes a single component of a block which may be one
 * complete parameter, the result, a block of 'state' information or a sub-set
 * of any of these data.
 */
public abstract class BlockElement {
	/**
	 * Encoded value used in the {@link BlockElement#getStreamFormat} returned
	 * array to define a valid data byte.
	 */
	public static final byte DATA = 1;
	/**
	 * Encoded value used in the {@link BlockElement#getStreamFormat} returned
	 * array to define an unused PAD byte.
	 */
	public static final byte PAD = 2;

	/**
	 * Returns the total allocated size (memory allocation size) for the element
	 * handled here. in C, this is what the sizeof() operator returns.
	 */
	public abstract int getAllocatedSize();

	/**
	 * Returns the {@link BlockDescriptor} to which this element belongs
	 */
	public abstract BlockDescriptor getBlockDescriptor();

	/**
	 * Returns a {@link DeclarationGenerator} for this element.
	 */
	public abstract DeclarationGenerator getDeclaredType();

	/**
	 * Returns the string name declared for this element or "return" if this is
	 * the result. ("return" is a reserved word in C, and can not be used as an
	 * identifier)
	 */
	public abstract String getFormalName();

	/**
	 * Returns an array of bytes which defines the organization of this data on
	 * the interface data path. Each element of the array is one of the public
	 * static fields of this class. DATA means the next byte of data, PAD means
	 * that a random byte value may be inserted into the stream. Using this
	 * array a correctly organized stream may be assembled. ie, if the interface
	 * width is 4 bytes and this method returns [D, D, P, P, ...] then the first
	 * transfer over the interface would be 0x00002211 (2 represents the 2nd
	 * valid byte of data and 1 represents the first).
	 * 
	 * values are transferred LSB to MSB
	 */
	public abstract byte[] getStreamFormat();

	/**
	 * Remove the bytes starting at the <code>start</code> index and removing
	 * <code>length</code> elements from this stream as represented in the array
	 * returned by {@link #getStreamFormat}
	 * 
	 * @param start
	 *            a non-negative int, less than {@link #getStreamFormat}.length,
	 *            and less than or equal to <code>end</code>
	 * @param length
	 *            a non-negative int, indicating the number of elements,
	 *            starting at <code>start</code> to delete.
	 */
	public abstract void deleteStreamBytes(int start, int length);

	/**
	 * returns the number of cycles necessary to transfer this element over the
	 * fifo. This value is auto-generated from the byte width of the interface
	 * as obtained from the {@link BlockDescriptor} and the number of bytes in
	 * this element as determined from the length of the
	 * {@link BlockElement#getStreamFormat} array
	 */
	public int getNumberOfCycles() {
		final int streamLength = getStreamFormat().length;
		final int fifoWidth = getBlockDescriptor().getByteWidth();

		// Simple sanity check.
		assert ((streamLength % fifoWidth) == 0) : "Illegal block element format.  Number of bytes is not integral multiple of fifo width.  Bytes: "
				+ streamLength + " fifo width " + fifoWidth;

		return streamLength / fifoWidth;
	}

}
