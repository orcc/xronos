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
package net.sf.openforge.verilog.testbench;

import net.sf.openforge.util.Debug;

/**
 * _linker.java
 * 
 * 
 * Created: Mon May 13 11:06:46 2002
 * 
 * @author YS Yu
 * @version $Id: _testbench.java 2 2005-06-09 20:00:48Z imiller $
 */

public class _testbench {

	public static boolean db = Debug.COMPILED_OUT;
	public final static Debug d = new Debug(
			net.sf.openforge.verilog.testbench._testbench.class, "Test Bench",
			db, Debug.VISIBLE,
			net.sf.openforge.app.logging.ForgeLogger.consoleOut);
}
