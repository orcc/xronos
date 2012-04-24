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

import java.io.File;

/**
 * This class parse a filename of the formate [$envvarname]filespec to its
 * platform specific code. filespec is expected to use '/' as the path separator
 * 
 * @author <a href="mailto:Jonathan.Harris@xilinx.com">Jonathan C. Harris</a>
 * @version $Id
 */
public class VarFilename {

	private VarFilename() {
	}

	/**
	 * / to \ is tbd!
	 * 
	 * @param name
	 *            a value of type 'String'
	 */
	public static String parse(String name) {
		// start with $
		if (name.charAt(0) == '$') {
			// do we have an end
			int i = name.indexOf('/');
			String rest = "";
			// is there more? set rest if there is ...
			if (i >= 0) {
				rest = name.substring(i);
			}
			// get the env name
			String env = name.substring(1, i);
			// try
			String s = Environment.getEnv(env);
			if (s == null) {
				// try again, upper case
				s = Environment.getEnv(env.toUpperCase());
			}
			// did we find it?
			if (s != null) {
				// mangle name for return
				name = s + rest;
			}
			// if we don't find it --- pass it through

		}
		return name.replace(File.separatorChar, '/');
	}

}
