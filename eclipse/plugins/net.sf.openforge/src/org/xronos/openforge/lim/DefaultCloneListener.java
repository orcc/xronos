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

import java.util.HashMap;
import java.util.Map;

/**
 * A default implementation of {@link CloneListener} which saves the final clone
 * {@link Map} and allows it to be be queried for cloned {@link Component
 * Components} and {@link Entry Entries}.
 * 
 * @version $Id: DefaultCloneListener.java 2 2005-06-09 20:00:48Z imiller $
 */
class DefaultCloneListener implements CloneListener {

	private Map<Component, Component> cloneMap = new HashMap<Component, Component>();

	/**
	 * The effect of calling this method multiple times is cumulative.
	 */
	@Override
	public void setCloneMap(Map<Component, Component> cloneMap) {
		this.cloneMap.putAll(cloneMap);
	}

	/**
	 * Gets the clone map that was set by the cloning component.
	 * 
	 * @return a map of original {@link Component Components} and {@link Entry
	 *         Entries} to their clones
	 */
	public Map<Component, Component> getCloneMap() {
		return cloneMap;
	}

	/**
	 * Gets the clone of a given {@link Component}.
	 */
	public Component getClone(Component component) {
		return cloneMap.get(component);
	}

	/**
	 * Gets the clone of a given {@link Entry}.
	 */
	/*
	 * public Entry getClone(Entry entry) { return (Entry) cloneMap.get(entry);
	 * }
	 */
}
