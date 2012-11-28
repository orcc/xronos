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
 * A ResetDependency describes the source of a reset signal.
 * 
 * @author Stephen Edwards
 * @version $Id: ResetDependency.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ResetDependency extends Dependency {

	/**
	 * Constructs a ResetDependency.
	 * 
	 * @param logicalBus
	 *            the bus which provides the reset signal
	 */
	public ResetDependency(Bus logicalBus) {
		super(logicalBus);
	}

	@Override
	public Dependency createSameType(Bus logicalBus) {
		return new ResetDependency(logicalBus);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ResetDependency) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
