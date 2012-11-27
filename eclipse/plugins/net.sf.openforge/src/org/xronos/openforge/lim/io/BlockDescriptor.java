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

package org.xronos.openforge.lim.io;

/**
 * BlockDescriptor holds information about a specific fifo, including direction,
 * and the organization of the contents of the block where a block is defined as
 * one complete data set necessary for complete processing by the function.
 */
public abstract class BlockDescriptor {

	/**
	 * Gets a {@link DeclarationGenerator} capable of declaring a variable to
	 * store the return value of the function described by this descriptor.
	 */
	public abstract DeclarationGenerator getFunctionDeclaredType();

	/**
	 * name of the function that this block belongs to. only useful when we
	 * support multiple entry methods
	 */
	public abstract String getFunctionName();

	/**
	 * Returns the unique numerical ID of this interface. Thus the interface
	 * will have names FSLx_y_zzz where x is the numerical ID
	 */
	public abstract String getInterfaceID();

	/**
	 * isSlave returns true if this fifo is an input fifo, false if it is an
	 * output interface.
	 */
	public abstract boolean isSlave();

	/**
	 * Returns an array which encodes the organization of data within the block.
	 * Each position represents 1 transfer on the fifo, and the value identifies
	 * the BlockElement transferred on that 'cycle'. The block organization is
	 * derived from the position of the BlockElement in the List returned from
	 * getBlockElements(). Thus a return of [0,0,0,0,1,1,1,1,2,2] would indicate
	 * 4 transfers from getBlockElements().get(0) followed by 4 transfers from
	 * getBlockElements().get(1) followed by 2 transfers from
	 * getBlockElements().get(2), etc. An alternative might be a return of
	 * [0,1,2,0,1,2,0,1,0,1] in which case the transfers are all interleaved,
	 * etc.
	 */
	public abstract int[] getBlockOrganization();

	/**
	 * Returns a list of BlockElement objects
	 */
	public abstract BlockElement[] getBlockElements();

	/**
	 * Returns the width of the interface data path in bytes.
	 */
	public abstract int getByteWidth();

}
