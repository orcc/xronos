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

import java.util.Collection;

/**
 * An element which represents a complete expression of verilog code. Statements
 * are the actual code, comprised of a valid sequence of Tokens. Statements
 * typically apply operations to nets.
 * <P>
 * Examples:<BR>
 * <CODE>
 * wire [31:0] a;
 * assign a = 0xFACE;
 * </CODE>
 */
public interface Statement extends VerilogElement {

	/**
	 * Returns all Nets used in this statement.
	 */
	public Collection<Net> getNets();

} // end of interface Statement
