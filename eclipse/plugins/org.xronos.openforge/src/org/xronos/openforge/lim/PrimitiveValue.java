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

package org.xronos.openforge.lim;

/**
 * PrimitiveValue represents the initial value of fields in a users design which
 * hold a primitive (byte, short, char, int, long, float, double, boolean).
 * 
 * Created: Wed Jun 19 16:24:49 2002
 * 
 * @author imiller
 * @version $Id: PrimitiveValue.java 2 2005-06-09 20:00:48Z imiller $
 */
public class PrimitiveValue implements InitialValue {

	Number initialValue;

	/**
	 * the location at which this PointerValue was created. USED ONLY FOR
	 * REPORTING.
	 */
	private String decClassName = "";
	private String decFieldName = "";

	/**
	 * Constructs a primitive value with the specified inital value.
	 * 
	 * @param value
	 *            a value of type 'Number'
	 */
	public PrimitiveValue(Number value) {
		initialValue = value;
	}

	/**
	 * returns false.
	 */
	@Override
	public boolean isObjectHandle() {
		return false;
	}

	/**
	 * returns true
	 */
	@Override
	public boolean isPrimitive() {
		return true;
	}

	/**
	 * Returns the long representation of the initial value.
	 * 
	 * @return a value of type 'long'
	 */
	@Override
	public Number toNumber() {
		return initialValue;
	}

	/**
	 * Identifies where this primitive value was created in the users design for
	 * reporting.
	 */
	@Override
	public void setDeclaringLocation(String clazz, String field) {
		decClassName = clazz;
		decFieldName = field;
	}

	/**
	 * Retrieves the name of the users class that is responsible for the
	 * instantiation of this PointerValue.
	 */
	@Override
	public String getDeclaringClass() {
		return decClassName;
	}

	/**
	 * Retrieves the name of the users field (within the declaring class) that
	 * is responsible for the instantiation of this PointerValue.
	 */
	@Override
	public String getDeclaringField() {
		return decFieldName;
	}

	/**
	 * Returns a new PrimitiveValue with the same Number initial value.
	 */
	@Override
	public InitialValue treeCopy() {
		return new PrimitiveValue(initialValue);
	}

	@Override
	public String toString() {
		return "PrimitiveValue: " + toNumber();
	}

}// PrimitiveValue
