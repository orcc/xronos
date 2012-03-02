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

import java.io.File;
import java.io.IOException;

import net.sf.openforge.util.VarFilename;
import net.sf.openforge.verilog.model.FStatement;
import net.sf.openforge.verilog.model.InitialBlock;
import net.sf.openforge.verilog.model.IntegerWire;
import net.sf.openforge.verilog.model.StringStatement;

/**
 * SimFileHandle is a convenience class used to keep track of the Verilog file
 * handle associated with a given results file as well as a convenient way of
 * instantiating the fopen statement in the Verilog testbench.
 * 
 * <p>
 * Created: Thu Jan 9 13:05:54 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimFileHandle.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SimFileHandle {

	/** The file which is to be used. */
	private File file;
	/** The 'handle' used in Verilog to refer to the file. */
	private IntegerWire handle;

	public SimFileHandle(File file, String handleName) {
		this.file = file;
		this.handle = new IntegerWire(handleName, 1);
	}

	/**
	 * Writes out the fopen statement needed to open the file and generate the
	 * filehandle which refers to it.
	 */
	public void stateInits(InitialBlock ib) {
		try {
			ib.add(new FStatement.FOpen(this.handle, new StringStatement(
					VarFilename.parse(file.getCanonicalPath()))));
		} catch (IOException e) {
			throw new RuntimeException("Bad filename for results file " + file
					+ " " + e.getMessage());
		}
	}

	/**
	 * Returns the {@link IntegerWire} file handle suitable for passing to
	 * fwrite statements to write to this file.
	 */
	public IntegerWire getHandle() {
		return this.handle;
	}

}// SimFileHandle
