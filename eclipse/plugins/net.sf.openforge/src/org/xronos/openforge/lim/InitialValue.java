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
 * InitialValue is the interface implemented by all classes that can represent
 * the initial value of a field in the users design.
 * 
 * Created: Thu Jun 20 14:11:21 2002
 * 
 * @author imiller
 * @version $Id: InitialValue.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface InitialValue {

	/**
	 * Returns true if this InitialValue represents an object reference.
	 */
	public boolean isObjectHandle();

	/**
	 * Returns true if this InitialValue represents a primitive value.
	 */
	public boolean isPrimitive();

	/**
	 * Gets the numeric representation of this value.
	 */
	public Number toNumber();

	/**
	 * Retrieves the string name of the class in the users design responsible
	 * for the creation of this initial value.
	 */
	public String getDeclaringClass();

	/**
	 * Retrieves the string name of the field in the users class responsible for
	 * the creation of this initial value.
	 */
	public String getDeclaringField();

	/**
	 * Identifies the location at which this InitialValue was created in the
	 * users design.
	 */
	public void setDeclaringLocation(String clazz, String field);

	/**
	 * Returns a completely unique InitialValue modeled after this InitialValue.
	 * For PointerValues this means that a complete replication of the tree of
	 * values will be performed.
	 */
	public InitialValue treeCopy();

}// InitialValue

