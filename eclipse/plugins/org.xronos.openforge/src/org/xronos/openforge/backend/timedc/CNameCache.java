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

import java.util.HashMap;
import java.util.Map;

import org.xronos.openforge.lim.Component;


/**
 * CNameCache is a simple name cache which ensures a unique base name for each
 * component. This class also ensures that the generated base name is a legal C
 * identifier.
 * 
 * 
 * <p>
 * Created: Wed Apr 13 15:59:27 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CNameCache.java 2 2005-06-09 20:00:48Z imiller $
 */
public class CNameCache {

	private final Map<Component, String> cacheMap = new HashMap<Component, String>();

	public CNameCache() {
	}

	/**
	 * Returns the cached and unique String name for the given component. The
	 * name is guaranteed to be legal C syntax.
	 * 
	 * @param comp
	 *            a value of type 'Component'
	 * @return a value of type 'String'
	 */
	public String getName(Component comp) {
		if (cacheMap.containsKey(comp)) {
			return cacheMap.get(comp);
		}

		String baseName = uniquify(getLegalIdentifier(comp.toString()));
		this.cacheMap.put(comp, baseName);
		return baseName;
	}

	/**
	 * Ensures that the given string is not a value in the cache map, and
	 * appends _# where # is the lowest integer necessary to make the string
	 * unique.
	 * 
	 * @param s
	 *            a value of type 'String'
	 * @return a value of type 'String'
	 */
	private String uniquify(String s) {
		int i = 0;
		String unique = new String(s);
		while (cacheMap.values().contains(unique)) {
			unique = s + "_" + i;
			i++;
		}
		return unique;
	}

	/**
	 * Returns a String in which any illegal characters (for a C identifier) in
	 * the input string have been replaced with legal characters.
	 * 
	 * @param s
	 *            a value of type 'String'
	 * @return a value of type 'String'
	 */
	public static String getLegalIdentifier(String s) {
		// Only allow alphabets, digits, or underscores to name a C
		// identifier. Any character besides previously mentioned is
		// eliminated or substitued by an underscore.

		char underscore = '_';
		String cId = "";
		char last_char = ' ';

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (Character.isLowerCase(c) || Character.isUpperCase(c)
					|| Character.isDigit(c)) {
				cId += c;
				last_char = c;
			} else if (c == underscore) {
				if (last_char != underscore) {
					cId += c;
					last_char = c;
				} else {
					cId += 'a';
					last_char = 'a';
				}
			} else {
				cId += underscore;
				last_char = underscore;
			}
		}
		// make sure the first character is an alpha
		char c = s.charAt(0);
		if (Character.isDigit(c)) {
			return "const" + cId;
		}

		return cId;

	}

}// CNameCache
