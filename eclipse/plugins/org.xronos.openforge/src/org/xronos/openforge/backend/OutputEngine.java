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

package org.xronos.openforge.backend;

import java.io.IOException;

import org.xronos.openforge.lim.Design;


/**
 * The OutputEngine interface is to be implemented by any class which generates
 * any File output of the compiler. The interface provides methods for
 * initializing itself and translating (generating) the output.
 * 
 * <p>
 * Created: Fri Mar 17 20:50:55 2006
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OutputEngine.java 112 2006-03-21 15:41:57Z imiller $
 */
public interface OutputEngine {

	/**
	 * Initialize the target or destination for the output of this engine. This
	 * may include registering files with the ForgeFileHandler.
	 */
	public void initEnvironment();

	/**
	 * Generate the output for this engine, including creation of the output
	 * files and/or directories.
	 */
	public void translate(Design design) throws IOException;

	/**
	 * Returns a unique string which identifies this engine.
	 */
	public String getOutputPhaseId();

}
