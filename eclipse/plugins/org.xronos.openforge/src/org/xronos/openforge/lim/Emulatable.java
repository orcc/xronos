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

import java.util.Map;

import org.xronos.openforge.util.SizedInteger;


/**
 * Implemented by classes (typically {@link Component} subclasses) whose
 * instances can perform a numerical emulation of their logic. Currently this
 * capability is used to determine the iteration count for unrollable
 * {@link Loop Loops}.
 * 
 * @version $Id: Emulatable.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface Emulatable {
	/**
	 * Performes a high level numerical emulation of this component. It is
	 * assumed that since this is a logical emulation, the implementer is only
	 * responsible for emulating data computations and not control; control
	 * issues are the responsibility of the caller. Therefore control ports and
	 * buses, such as <code>go</code> and <code>done</code>, are not included in
	 * the emulation.
	 * 
	 * @param portValues
	 *            a map of {@link Port} to {@link SizedInteger} input value
	 * @return a map of {@link Bus} to {@link SizedInteger} result value
	 */
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues);
}
