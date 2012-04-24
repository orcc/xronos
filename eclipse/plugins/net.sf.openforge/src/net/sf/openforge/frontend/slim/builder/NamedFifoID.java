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

package net.sf.openforge.frontend.slim.builder;

import net.sf.openforge.lim.io.FifoID;

/**
 * NamedFifoID is a concrete implementation of the {@link FifoID} class which
 * defers to the getID() method to determine the name of the interface.
 * 
 * <p>
 * Created: Tue Jul 12 14:55:48 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public class NamedFifoID extends FifoID {

	public NamedFifoID() {
		super();
	}

	public NamedFifoID(String encoded) {
		super(encoded);
	}

	/**
	 * Simply return the defined String ID without any modification.
	 * 
	 * @return a value of type 'String'
	 */
	@Override
	public String getName() {
		return getID();
	}

}// NamedFifoID
