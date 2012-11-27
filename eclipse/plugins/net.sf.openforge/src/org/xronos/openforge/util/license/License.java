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
package org.xronos.openforge.util.license;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xronos.openforge.app.ForgeFatalException;
import org.xronos.openforge.util.ForgeResource;


/**
 * License has some convenient utility methods for dealing with License issues.
 * 
 * @version $Id: License.java 2 2005-06-09 20:00:48Z imiller $
 */
public class License {
	//private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

	// ************************************************************
	// * *
	// * Public Static methods (main at bottom) *
	// * *
	// ************************************************************
	/**
	 * verify that the forge has not expired.
	 * 
	 * @return true if ok, false if not
	 */
	public static boolean isDateOK(long now) {
		long exptime = 973054799485L;
		int x = 3;
		long diff = exptime - now;
		x *= 3; // 9
		x++; // 10
		x *= 3; // 30
		x *= 5; // 150
		x *= 2; // 300
		x += 3; // 303
		x += 3; // 306
		x += 3; // 309
		x++; // majic number is 310!
		if (exptime % 485 != x) {
			throw new ForgeFatalException("jar file modified!");
		}
		if (diff < (3600000 * 24 * 7) && diff > 0) {
			//java.util.Date expDate = new java.util.Date(exptime);
			/*
			 * System.err.println(
			 * "Warning: Your Forge evaluation copy will expire in "+
			 * diff/(3600000*24)+" day(s), on: "+expDate+"\n");
			 */
		}

		return (diff > 0);
	}

	/**
	 * function to return the license file as a String
	 * 
	 * 
	 * @return a string which is the license file
	 */
	@SuppressWarnings("unused")
	public static String getLicenseFile() {
		String licenseFile = "";

		InputStream is = ForgeResource.loadForgeResourceStream("LICENSE_FILE");

		if (is == null) {
			return "license file not found";
		}

		InputStreamReader isr = new InputStreamReader(is);

		if (isr == null) {
			return "license file not found";
		}

		BufferedReader br = new BufferedReader(isr);

		if (br == null) {
			return "license file not found";
		}

		try {
			while (br.ready()) {
				licenseFile += br.readLine() + "\n";
			}
		} catch (Exception ioe) {
			return licenseFile;
		}

		return (licenseFile);
	}

	public static boolean isLicenseCurrent(String key) {
		return Key.isLicenseCurrent(key);
	}

	public static boolean isLicenseCurrent() {
		return isLicenseCurrent(getLicenseFile());
	}

	public static String getLicenseInfo() {
		return getLicenseInfo(getLicenseFile());
	}

	public static String getLicenseInfo(String licenseString) {
		//String result = "";

		if (licenseString.equals("license file not found")) {
			return ("License Info:\n  Unable to locate license file: license.dat.\n"
					+ "  Please check your installation\n");
		} else {
			return ("License Info:\n         Key: "
					+ Key.formatKey(licenseString) + "\n  Expiration: "
					+ Key.getExpirationAsString(licenseString) + "\n");
		}
	}

}
