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

/**
 * ForgeFileTyper contains static utility methods which identify the file type
 * based on the filename.
 * 
 * <P>
 * Created: Mon Oct 22 10:53:00 2001
 * 
 * @author <a href="mailto: andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: ForgeFileTyper.java 48 2005-11-02 15:43:19Z imiller $
 */

public class ForgeFileTyper {

	public static final int JAVA = 0;
	public static final int CLASS = 1;
	public static final int FORGE = 2;

	public static boolean isJavaSource(String filename) {
		// javac only supports lowercase .java files on solaris and windows!
		return (filename.endsWith(".java"));
	}

	public static boolean isCSource(String filename) {
		return filename.toLowerCase().endsWith(".c");
	}

	public static boolean isXLIMSource(String filename) {
		return filename.toLowerCase().endsWith(".xlim")
				|| filename.toLowerCase().endsWith(".sxlim");
	}

	public static boolean isJavaClass(String filename) {
		return (filename.endsWith(".class") || filename.endsWith(".CLASS"));
	}

	public static boolean isForgeProject(String filename) {
		return (filename.endsWith(".forge") || filename.endsWith(".FORGE"));
	}

	public static boolean isForgePref(String filename) {
		return (filename.endsWith(".pref") || filename.endsWith(".PREF"));
	}

	public static boolean isForgeUcf(String filename) {
		return (filename.endsWith(".ucf") || filename.endsWith(".UCF"));
	}

}// end of class ForgeFileTyper
