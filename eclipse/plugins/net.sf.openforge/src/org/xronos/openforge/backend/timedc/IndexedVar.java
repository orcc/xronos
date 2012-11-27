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

package org.xronos.openforge.backend.timedc;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;

/**
 * IndexedVar is the common superclass for stateful resources which require an
 * index (ie xxx[yy]) to select the output value.
 * 
 * <p>
 * Created: Wed Mar 2 21:21:30 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: IndexedVar.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class IndexedVar extends OpHandle {

	private final Bus output;

	public IndexedVar(Component comp, Bus output, CNameCache cache) {
		super(comp, cache);
		this.output = output;
	}

	protected abstract String getIndex();

	/**
	 * Overrides the super method so that we can append the array select to
	 * point to the 'index' element
	 */
	@Override
	public String getBusName(Bus b) {
		String name = getDefaultBusName(b);
		if (b == this.output) {
			name += "[" + getIndex() + "]";
		}
		return name;
	}

	/**
	 * Returns the non modified version of the output bus name, that is the name
	 * of the output without any indexing applied.
	 * 
	 * @param b
	 *            a non-null Bus
	 * @return a value of type 'String'
	 */
	protected String getDefaultBusName(Bus b) {
		return super.getBusName(b);
	}

}// IndexedVar
