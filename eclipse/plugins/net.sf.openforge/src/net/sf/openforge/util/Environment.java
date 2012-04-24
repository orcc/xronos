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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Class to get the current OS environment settings.
 * 
 * Uses Runtime.exec to run a command to get the environment settings then
 * parses them. Under *nix variants, it uses "env". Under windoze variants it
 * uses "command /c set" (95/98) or "cmd /c set" (NT/2000/XP).
 * 
 * Alternatively, the command to get <name>=<value> pairs cen be set as the
 * system property "lava.env.cmd".
 * 
 * Names of properties are stored in upper case.
 * 
 * $Id: Environment.java 2 2005-06-09 20:00:48Z imiller $
 * 
 */
public class Environment {

	// ************************************************************
	// * *
	// * public fields *
	// * *
	// ************************************************************

	public static final String FORGEHOME = "FORGEHOME";

	// ************************************************************
	// * *
	// * private/protected fields *
	// * *
	// ************************************************************

	private static Properties envProps = new Properties();

	// ************************************************************
	// * *
	// * Constructors *
	// * *
	// ************************************************************

	/**
	 * Constructor declaration
	 * 
	 * 
	 * @see
	 */
	private Environment() {
	}

	// ************************************************************
	// * *
	// * Public Static methods (main at bottom) *
	// * *
	// ************************************************************

	/**
	 * Get the setting of a specific operating system setting. Returns null if
	 * not found.
	 * 
	 * @param envName
	 *            Name of the environment variable
	 * @return Value
	 */
	public static String getEnv(String envName) {
		return (String) envProps.get(envName);
	}

	/**
	 * Set an arbitrary value. Useful for testing.
	 * 
	 * @param envName
	 *            a value of type 'String'
	 * @param val
	 *            a value of type 'String'
	 * @return a value of type 'String'
	 */
	public static void setEnv(String envName, String val) {
		envProps.put(envName, val);
	}

	/**
	 * Get the setting of a specific operating system setting. Returns
	 * defaultValue value if not found.
	 * 
	 * @param envName
	 *            Name of the environment variable
	 * @param defaultValue
	 *            Default value for this setting
	 * @return Value
	 */
	public static String getEnv(String envName, String defaultValue) {
		String ret = (String) envProps.get(envName);

		if (ret == null) {
			ret = defaultValue;
		}

		return ret;
	}

	/**
	 * Return the Properties Object for the System environment space. This
	 * object is static for an instance of the VM
	 * 
	 * @return Properties object
	 */
	public static Properties getProperties() {
		return envProps;
	}

	// ************************************************************
	// * *
	// * Public Instance Accessor methods (get/set/is) *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Public Instance methods *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Protected Static methods *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Protected Instance methods *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Private Static methods *
	// * *
	// ************************************************************

	/**
	 * Method declaration
	 * 
	 * 
	 * @see
	 */
	private static void storeEnv() {
		try {
			String cmd = System.getProperty("lava.env.cmd");

			if (cmd == null) {
				if (File.separator.equals("/")) {
					cmd = "env";
				} else if (System.getProperty("os.name").equals("Windows NT")) {
					cmd = "cmd.exe /c set";
				} else if (System.getProperty("os.name").equals("Windows 2000")) {
					cmd = "cmd.exe /c set";
				} else if (System.getProperty("os.name").equals("Windows XP")) {
					cmd = "cmd.exe /c set";
				} else {
					cmd = "command.com /c set";
				}
			}

			InputStreamReader isr = new InputStreamReader(Runtime.getRuntime()
					.exec(cmd).getInputStream());
			BufferedReader br = new BufferedReader(isr);

			String line;

			// Read in the buffer
			while ((line = br.readLine()) != null) {
				int index = line.indexOf("=");

				// Make sure that there is an equals sign and at least 1
				// character in the name and value fields prior to parsing
				if ((index > 0) && ((index + 1) < line.length())) {
					String name = line.substring(0, index).trim().toUpperCase();
					String val = line.substring(index + 1).trim();

					envProps.put(name, val);
				}
			}
		} catch (IOException ioe) {
			System.err.println("Unable to read os environment settings " + ioe);
		}
	}

	// ************************************************************
	// * *
	// * Private Instance methods *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Public static void main(String[]) *
	// * *
	// ************************************************************
	// NONE

	static {
		storeEnv();
	}

	/**
	 * Main entry, iterates over and displays all available Environment vars.
	 * 
	 * 
	 * @param args
	 *            the command line args
	 * 
	 */
	public static void main(String args[]) {
		for (Enumeration e = Environment.getProperties().keys(); e
				.hasMoreElements();) {
			String name = (String) e.nextElement();
			String val = getEnv(name);

			System.out.println(name + " = " + val);
		}
	}

}

/*--- formatting done in "Lavalogic Coding Convention" style on 11-23-1999 ---*/

