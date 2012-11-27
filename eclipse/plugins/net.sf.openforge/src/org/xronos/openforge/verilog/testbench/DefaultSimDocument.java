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

package org.xronos.openforge.verilog.testbench;

import java.io.File;
import java.util.List;

import org.xronos.openforge.verilog.model.DelayStatement;
import org.xronos.openforge.verilog.model.FStatement;
import org.xronos.openforge.verilog.model.InitialBlock;
import org.xronos.openforge.verilog.model.InlineComment;
import org.xronos.openforge.verilog.model.Module;
import org.xronos.openforge.verilog.model.StringStatement;
import org.xronos.openforge.verilog.model.VerilogDocument;


/**
 * DefaultSimDocument is a verilog document populated with a single initial
 * block that opens up result files and writes a message to them before
 * quitting.
 * 
 * <p>
 * Created: Wed Oct 30 15:05:53 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DefaultSimDocument.java 2 2005-06-09 20:00:48Z imiller $
 */

public class DefaultSimDocument extends VerilogDocument {

	/**
	 * Used to create a unique identifier for each instance in a given run of
	 * Forge. Allows for concurrent simulations.
	 */
	private static int uniqueID = 0;

	/**
	 * Creates a default simulation verilog document which writes the given
	 * message to each file in the list of files and then finishes.
	 * 
	 * @param message
	 *            a value of type 'String'
	 * @param files
	 *            a value of type 'List'
	 */
	public DefaultSimDocument(String message, List<File> files) {
		super();
		Module testModule = new Module("fixture_default_" + uniqueID++);
		InitialBlock ib = new InitialBlock();
		for (File file : files) {
			ResultFile resultFile = new ResultFile(file);
			ib.add(resultFile.init());
			ib.add(new DelayStatement(resultFile.write(new StringStatement(
					message)), 100));
		}

		ib.add(new DelayStatement(new FStatement.Finish(), 500));

		testModule.state(new InlineComment("Default Simulation Document"));
		testModule.state(ib);

		append(testModule);
	}

}// DefaultSimDocument

