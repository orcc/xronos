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
/* $Rev: 2 $ */

package org.xronos.openforge.forge.api.runtime;

import java.io.File;

/**
 * The <code>RunTime</code> class is supplied to give the user's program access
 * to Forge compilation settings. The class is very helpful when creating
 * generic IPCore abstraction classes that self configure based on the runtime
 * settings of Forge such as the target device family. This class is structured
 * such that the methods can be called from within Forgeable code. <div>NOTE:
 * some values will change based on the compilation step in Forge. If your code
 * relies on contex sensitive information, such as the source directory then the
 * value should be stored locally for later use. An example of this would be an
 * <code>HDLWriter</code> used for an <code>IPCore</code> that is contained in
 * an operation replacement library and the writer needs the source directory
 * associated with the replacement class, not the design that is being compiled,
 * then the writer must keep a copy of the source directory during construction
 * of its class since during translation the source directory will be set to the
 * design that is being compiled. <div>NOTE: Any <code>File</code> returned by a
 * method of this class might not exist in the file system depending on the
 * compilation step Forge is in.
 */
public class RunTime {

	/**
	 * The <code>int</code> value representing the <code>VIRTEX</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int VIRTEX = 0x00000001;

	/**
	 * The <code>int</code> value representing the <code>VIRTEX2P</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int VIRTEX2P = 0x00000002;

	/**
	 * The <code>int</code> value representing the <code>VIRTEX2</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int VIRTEX2 = 0x00000004;

	/**
	 * The <code>int</code> value representing the <code>VIRTEXE</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int VIRTEXE = 0x00000008;

	/**
	 * The <code>int</code> value representing the <code>SPARTAN</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int SPARTAN = 0x00000010;

	/**
	 * The <code>int</code> value representing the <code>SPARTAN2</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int SPARTAN2 = 0x00000020;

	/**
	 * The <code>int</code> value representing the <code>SPARTAN2E</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int SPARTAN2E = 0x00000040;

	/**
	 * The <code>int</code> value representing the <code>SPARTANXL</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int SPARTANXL = 0x00000080;

	/**
	 * The <code>int</code> value representing the <code>SPARTAN3</code> family
	 * returned by <code>getFamily</code>.
	 * 
	 */
	public static final int SPARTAN3 = 0x00000100;

	private static int family = 0;

	private static File sourceDir = null;

	private static File destinationDir = null;

	private static File xflowDir = null;

	private RunTime() {
	}

	/**
	 * Indicates if the target FPGA family is Virtex.
	 * 
	 * @return <code>true</code> if the target FPGA family is Virtex.
	 */
	public static boolean isVirtex() {
		return ((family & VIRTEX) == VIRTEX);
	}

	/**
	 * Indicates if the target FPGA family is Virtex2P.
	 * 
	 * @return <code>true</code> if the target FPGA family is Virtex2P.
	 */
	public static boolean isVirtex2P() {
		return ((family & VIRTEX2P) == VIRTEX2P);
	}

	/**
	 * Indicates if the target FPGA family is Virtex2.
	 * 
	 * @return <code>true</code> if the target FPGA family is Virtex2.
	 */
	public static boolean isVirtex2() {
		return ((family & VIRTEX2) == VIRTEX2);
	}

	/**
	 * Indicates if the target FPGA family is VirtexE.
	 * 
	 * @return <code>true</code> if the target FPGA family is VirtexE.
	 */
	public static boolean isVirtexE() {
		return ((family & VIRTEXE) == VIRTEXE);
	}

	/**
	 * Indicates if the target FPGA family is Spartan.
	 * 
	 * @return <code>true</code> if the target FPGA family is Spartan.
	 */
	public static boolean isSpartan() {
		return ((family & SPARTAN) == SPARTAN);
	}

	/**
	 * Indicates if the target FPGA family is Spartan2.
	 * 
	 * @return <code>true</code> if the target FPGA family is Spartan2.
	 */
	public static boolean isSpartan2() {
		return ((family & SPARTAN2) == SPARTAN2);
	}

	/**
	 * Indicates if the target FPGA family is Spartan2E.
	 * 
	 * @return <code>true</code> if the target FPGA family is Spartan2E.
	 */
	public static boolean isSpartan2E() {
		return ((family & SPARTAN2E) == SPARTAN2E);
	}

	/**
	 * Indicates if the target FPGA family is SpartanXL.
	 * 
	 * @return <code>true</code> if the target FPGA family is SpartanXL.
	 */
	public static boolean isSpartanXL() {
		return ((family & SPARTANXL) == SPARTANXL);
	}

	/**
	 * Indicates if the target FPGA family is Spartan3.
	 * 
	 * @return <code>true</code> if the target FPGA family is Spartan3.
	 */
	public static boolean isSpartan3() {
		return ((family & SPARTAN3) == SPARTAN3);
	}

	/**
	 * Retrives the target FPGA family as an <code>int</code>. The family value
	 * can be compared against the constant fields of this class such as
	 * <code>VIRTEX2</code>.
	 * 
	 * @return and <code>int</code> representing the target FPGA family.
	 */
	public static int getFamily() {
		return family;
	}

	/**
	 * Retrieves the directory that contained the source file Forge is currently
	 * processing.
	 * 
	 * @return a <code>File</code> representing the source directory.
	 */
	public static File getSourceDir() {
		return (sourceDir);
	}

	/**
	 * Retrieves the directory that Forge will put its output in.
	 * 
	 * @return a <code>File</code> representing the destination directory.
	 */
	public static File getDestinationDir() {
		return (destinationDir);
	}

	/**
	 * Retrieves the directory that Forge will run xflow in.
	 * 
	 * @return a <code>File</code> representing the source directory.
	 */
	public static File getXflowDir() {
		return (xflowDir);
	}

	// private methods accessed using reflection
	private static void setFamily(int fam) {
		family = fam;
	}

	private static void setSourceDir(File f) {
		sourceDir = f;
	}

	private static void setDestinationDir(File f) {
		destinationDir = f;
	}

	private static void setXflowDir(File f) {
		xflowDir = f;
	}

	@SuppressWarnings("unused")
	private static void setValues(int fam, File srcDir, File dstDir, File xDir) {
		setFamily(fam);
		setSourceDir(srcDir);
		setDestinationDir(dstDir);
		setXflowDir(xDir);
	}
}
