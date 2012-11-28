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
 * A DataDependency describes the dependence of a data input {@link Port Port}
 * on the arrival of a value from a data output {@link Bus Bus}. Scheduling will
 * resolve this dependency by ensuring the validity of the data value from the
 * specified bus.
 * 
 * @author Stephen Edwards
 * @version $Id: DataDependency.java 2 2005-06-09 20:00:48Z imiller $
 */
public class DataDependency extends Dependency {

	/**
	 * Constructs a DataDependency.
	 * 
	 * @param logicalBus
	 *            the data bus on which the data port logically depends
	 */
	public DataDependency(Bus logicalBus) {
		super(logicalBus);
	}

	@Override
	public Dependency createSameType(Bus logicalBus) {
		return new DataDependency(logicalBus);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DataDependency) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
