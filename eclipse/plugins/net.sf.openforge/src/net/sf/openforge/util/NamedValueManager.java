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

package net.sf.openforge.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides simple named key :: object mappoing for classes that need to store
 * complex objects by name
 * 
 * @author <a href="mailto:Jonathan.Harris@xilinx.com">Jonathan C. Harris</a>
 * @version $Id
 */
public class NamedValueManager {

	private HashMap<String, Object> values = new HashMap<String, Object>();

	public NamedValueManager() {
	}

	public Object getNamedValue(String name) {
		return values.get(name);
	}

	public void putNamedValue(String name, Object o) {
		values.put(name, o);
	}

	public boolean containsNamedValue(String name) {
		return values.containsKey(name);
	}

	public void removeNamedValue(String name) {
		values.remove(name);
	}

	public void clearNamedValues() {
		values.clear();
	}

	public Map<String, Object> getNamedValueMap() {
		return values;
	}
}
