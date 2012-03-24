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

package net.sf.openforge.lim.op;

/**
 * ConditionalOp is the super class of all comparison operations (gt, gte, lt,
 * lte, ne, eq) and provides a setFloat method to override the functionality in
 * BinaryOp. This is necessary because even though a conditional may take in 2
 * float values (and thus is a floating point comparison) it will return an
 * int/boolean and thus we cannot rely on its result bus to determine its type.
 * 
 * 
 * <p>
 * Created: Thu May 1 16:08:46 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ConditionalOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class ConditionalOp extends BinaryOp {

	private boolean isFloatType = false;

	/**
	 * Used to set the floating point type of this operation.
	 */
	public void setFloat(boolean value) {
		isFloatType = value;
	}

	/**
	 * Returns true if this operation has been marked as floating point, the
	 * default value is false.
	 */
	@Override
	public boolean isFloat() {
		return isFloatType;
	}

	/**
	 * Clones this ConditionalOp and correctly set's the 'float type'
	 * 
	 * @return a ConditionalOp clone of this operations.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		ConditionalOp clone = (ConditionalOp) super.clone();
		clone.isFloatType = isFloatType;
		return clone;
	}

}// ConditionalOp
