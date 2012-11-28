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

package org.xronos.openforge.verilog.model;

/**
 * HexNumber is a number expressed in hexadecimal.
 * 
 * <P>
 * 
 * Created: Wed Feb 28 2001
 * 
 * @author abk
 * @version $Id: HexNumber.java 2 2005-06-09 20:00:48Z imiller $
 */

public class HexNumber extends BaseNumber {

	public HexNumber(Constant n) {
		super(n);
	}

	public HexNumber(Number n) {
		this(new HexConstant(n));
	}

	public HexNumber(long l, int size) {
		this(new HexConstant(l, size));
	}

} // end of class HexNumber
