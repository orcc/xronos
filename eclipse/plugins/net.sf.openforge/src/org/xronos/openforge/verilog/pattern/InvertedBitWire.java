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
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.Group;
import org.xronos.openforge.verilog.model.Lexicality;
import org.xronos.openforge.verilog.model.Net;
import org.xronos.openforge.verilog.model.NetFactory;
import org.xronos.openforge.verilog.model.Unary;


/**
 * A verilog expression which is based on a contiguous set of LIM {@link Bit
 * Bits}.
 * 
 * Created: Nov 20, 2002
 * 
 * @author Conor Wu
 * @version $Id: InvertedBitWire.java 2 2005-06-09 20:00:48Z imiller $
 */
public class InvertedBitWire implements Expression {

	Expression bitselect;

	public InvertedBitWire(Bit bit) {
		assert bit.getOwner() != null : "Getting owner of floating bit " + bit;
		Bus source = bit.getOwner();
		Net full_wire = NetFactory.makeNet(source);
		bitselect = full_wire.getBitSelect(bit.getPosition());
	}

	public InvertedBitWire(List<Bit> bits) {
		Bit lsb = bits.get(0).getInvertedBit();
		Bit msb = bits.get(bits.size() - 1).getInvertedBit();
		// Bus source = lsb.getParent().getSource();
		assert lsb.getOwner() != null : "Getting owner of floating bit " + lsb;
		Bus source = lsb.getOwner();
		Net full_wire = NetFactory.makeNet(source);
		bitselect = full_wire.getRange(msb.getPosition(), lsb.getPosition());
	}

	@Override
	public int getWidth() {
		return bitselect.getWidth();
	}

	@Override
	public Collection<Net> getNets() {
		return bitselect.getNets();
	}

	@Override
	public Lexicality lexicalify() {
		if (bitselect.getWidth() > 1) {
			return new Group(new Unary.Negate(bitselect)).lexicalify();
		} else {
			return new Group(new Unary.Not(bitselect)).lexicalify();
		}
	}
}
