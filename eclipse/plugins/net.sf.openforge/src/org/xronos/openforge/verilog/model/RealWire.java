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

package org.xronos.openforge.verilog.model;

/**
 * 
 * RealWire is to enable the use of 'real' variables in the testbench for
 * behavioural code. 'real' is not exactly a net type but is used here as a hack
 * !
 * 
 * <p>
 * Created: Wed Jan 8 15:43:29 2003
 * 
 * @author gandhij, last modified by $Author: imiller $
 * @version $Id: RealWire.java 2 2005-06-09 20:00:48Z imiller $
 */

public class RealWire extends Net {

	public RealWire(Identifier id, int width) {
		super(Keyword.REAL, id, width);
	} // RealWire(Identifier, width)

	public RealWire(String id, int width) {
		this(new Identifier(id), width);
	} // RealWire(String, width)

	public RealWire(Identifier id, int msb, int lsb) {
		super(Keyword.REAL, id, msb, lsb);
	} // RealWire(Identifier, msb, lsb)

	public RealWire(String id, int msb, int lsb) {
		this(new Identifier(id), msb, lsb);
	} // RealWire(String, msb, lsb)

}
