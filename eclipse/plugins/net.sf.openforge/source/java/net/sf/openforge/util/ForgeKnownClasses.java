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

/**
 * ForgeKnownClasses provides utilities for identifying a class/package as
 * belonging to any of several known sets of classes such as system, api, or
 * simulation classes.
 * 
 * Created: Thu Jun 20 10:03:49 2002
 * 
 * @author imiller
 * @version $Id: ForgeKnownClasses.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ForgeKnownClasses {

	private static String[] systemClasses = { "java.", "javax.", "org.omg." };
	private static String[] apiClassPackages = { "net.sf.openforge.forge.api.",
			"net.sf.openforge.forge.api.hw.",
			"net.sf.openforge.forge.internal.hw." };

	/**
	 * Returns true if the given class name exists in one of the known api class
	 * Strings.
	 * 
	 * @param name
	 *            a value of type 'String'
	 * @return a value of type 'boolean'
	 */
	public static boolean isAPIClass(String name) {
		for (int i = 0; i < apiClassPackages.length; i++) {
			if (name.startsWith(apiClassPackages[i]))
				return true;
		}
		return false;
	}

	/**
	 * Returns true if the class's name starts with any known system package.
	 * 
	 * @param clazz
	 *            a value of type 'Class'
	 * @return a value of type 'boolean'
	 */
	public static boolean isSystemClass(Class clazz) {
		return isSystemClass(clazz.getName());
	}

	/**
	 * Returns true if the class's name starts with any known system package.
	 * 
	 * @param name
	 *            a value of type 'String'
	 * @return a value of type 'boolean'
	 */
	public static boolean isSystemClass(String name) {
		for (int i = 0; i < systemClasses.length; i++) {
			if (name.startsWith(systemClasses[i])) {
				return true;
			}
		}
		return false;
	}

}// ForgeKnownClasses
