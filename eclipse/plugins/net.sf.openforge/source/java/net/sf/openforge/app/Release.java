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
package net.sf.openforge.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.sf.openforge.util.ForgeResource;

/**
 * Release provides access to release notes for Forge.
 * 
 * 
 * Created: Thu Mar 28 11:33:02 2002
 * 
 * @author <a href="mailto:abk@ladd">Andy Kollegger</a>
 * @version $Id: Release.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Release {

	/**
	 * method to return the release notes as a string
	 * 
	 * 
	 * @return the release notes
	 */
	public static String releaseNotes() {
		String rnotes_comment = "";
		InputStream is = ForgeResource
				.loadForgeResourceStream("RELEASE_NOTES_TEXT");

		if (is == null) {
			return ("release notes not provided");
		}

		InputStreamReader isr = new InputStreamReader(is);

		if (isr.equals(null)) {
			return ("release notes not provided");
		}

		BufferedReader rnotes_reader = new BufferedReader(isr);

		if (rnotes_reader.equals(null)) {
			return ("release notes not provided");
		}

		try {
			while (rnotes_reader.ready()) {
				rnotes_comment += rnotes_reader.readLine() + "\n";
			}
		} catch (Exception ioe) {
			rnotes_comment = "Resource disabled version.";
		}

		return (rnotes_comment);

		// return("Version information temporarily disabled");
	}

} // class Release
