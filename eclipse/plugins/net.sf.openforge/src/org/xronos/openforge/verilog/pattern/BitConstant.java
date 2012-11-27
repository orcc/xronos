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
package org.xronos.openforge.verilog.pattern;

import java.util.Collection;
import java.util.List;

import org.xronos.openforge.lim.Bit;
import org.xronos.openforge.verilog.model.BinaryNumber;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.Lexicality;
import org.xronos.openforge.verilog.model.Net;


/**
 * A verilog expression which is based on a contiguous set of LIM {@link Bit
 * Bits}, which have constant values.
 * 
 * Created: August 6, 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: BitConstant.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BitConstant implements Expression {

	Expression constant;

	public BitConstant(List<Bit> bits) {
		long constant_value = 0;
		int constant_size = 0;
		long bitmask = 0x1;
		for (Bit bit : bits) {
			if (bit.isOn()) {
				constant_value |= bitmask;
			}
			bitmask <<= 1;
			constant_size++;
		}
		constant = new BinaryNumber(constant_value, constant_size);
	}

	@Override
	public int getWidth() {
		return constant.getWidth();
	}

	@Override
	public Collection<Net> getNets() {
		return constant.getNets();
	}

	@Override
	public Lexicality lexicalify() {
		return constant.lexicalify();
	}
}
