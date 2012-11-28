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
 * Inout a Net which represents a Module input port.
 * 
 * <P>
 * 
 * Created: Fri Feb 09 2001
 * 
 * @author abk
 * @version $Id: Inout.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Inout extends Net {

	public Inout(Identifier id, int width) {
		super(Keyword.INOUT, id, width);
	} // Inout(Identifier)

	public Inout(String id, int width) {
		this(new Identifier(id), width);
	} // Inout(String)

} // end of class Inout
