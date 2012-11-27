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
package org.xronos.openforge.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xronos.openforge.util.ForgeResource;


/**
 * Version provides version information.
 * 
 * 
 * Created: Thu Mar 28 11:24:35 2002
 * 
 * @author <a href="mailto:abk@ladd">Andy Kollegger</a>
 * @version $Id: Version.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Version {

	/**
	 * function to return version information
	 * 
	 * 
	 * @return a string with the version information.
	 */
	public static String versionInfo() {
		String version_comment = "";
		InputStream is = ForgeResource.loadForgeResourceStream("VERSION_TEXT");

		if (is == null) {
			return ("version information not provided");
		}

		InputStreamReader isr = new InputStreamReader(is);

		if (isr.equals(null)) {
			return ("version information not provided");
		}

		BufferedReader version_reader = new BufferedReader(isr);

		if (version_reader.equals(null)) {
			return ("version information not provided");
		}

		try {
			while (version_reader.ready()) {
				version_comment += version_reader.readLine() + "\n";
			}
		} catch (Exception ioe) {
			version_comment = "Resource disabled version.";
		}

		return (version_comment);

		// return("Version information temporarily disabled");
	}

	/**
	 * function to return version number or build date if none available
	 * 
	 * @return a string which represent the version number
	 */
	public static String versionNumber() {
		String key1 = "Build Date: ";

		String vi = versionInfo();

		int loc = vi.indexOf(key1);
		String result = "";

		if (loc == -1) {
			loc = 0;

			if (vi.length() < 1)
				result = "Unknown";
			else {
				// Parse out the build number
				while ((loc < vi.length())
						&& (Character.isLetterOrDigit(vi.charAt(loc))
								|| (vi.charAt(loc) == '.') || (vi.charAt(loc) == '-'))) {
					result += vi.charAt(loc);
					loc++;
				}
			}
		} else {
			// Parse out the version number
			loc += key1.length();

			while ((loc < vi.length())
					&& (Character.isSpaceChar(vi.charAt(loc))
							|| Character.isLetterOrDigit(vi.charAt(loc)) || (vi
							.charAt(loc) == ':'))) {
				result += vi.charAt(loc);
				loc++;
			}
		}
		return (result);
	}

} // class Version
