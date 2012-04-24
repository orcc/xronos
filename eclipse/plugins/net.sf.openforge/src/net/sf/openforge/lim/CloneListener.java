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

package net.sf.openforge.lim;

import java.util.Map;

/**
 * Implemented by classes that want a view into the cloning process of
 * {@link Component Components} and {@link Module Modules}. During a clone, the
 * cloned object provides the listener with a map of original objects to their
 * clones.
 * 
 * @version $Id: CloneListener.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface CloneListener {

	/**
	 * Notifies the listener of the cloning of one or more {@link Component
	 * Components}. This method may be invoked more than once during before
	 * cloning is complete.
	 * 
	 * @param cloneMap
	 *            a map of original objects to cloned objects; these include the
	 *            cloned {@link Component Components} and their {@link Entry
	 *            Entries}
	 */
	public void setCloneMap(Map cloneMap);
}
