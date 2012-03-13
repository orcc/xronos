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

package net.sf.openforge.lim.op;

/**
 * Base class of all operations, which can be terned into a verilog reduction
 * operation.
 * 
 * Created: Thu May 19 16:39:34 2003
 * 
 * @author Conor Wu
 * @version $Id: ReductionOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class ReductionOp extends UnaryOp {

	/**
	 * Constructs a reduction opeation.
	 */
	public ReductionOp() {
		super();
	}
}
