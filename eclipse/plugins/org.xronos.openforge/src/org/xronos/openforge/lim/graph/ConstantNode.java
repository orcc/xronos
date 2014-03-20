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
 * $Id: ConstantNode.java 568 2008-03-31 17:23:31Z imiller $
 *
 * 
 */

package org.xronos.openforge.lim.graph;

import java.math.BigInteger;

import org.xronos.openforge.lim.memory.AddressableUnit;
import org.xronos.openforge.lim.op.Constant;

/**
 * ConstantNode is used to represent a {@link Constant} in a {@link LXGraph}. It
 * adds its value to its body label.
 * 
 * @version $Id: ConstantNode.java 568 2008-03-31 17:23:31Z imiller $
 */
class ConstantNode extends ComponentNode {
	public static String value(Constant con) {
		AddressableUnit[] rep = con.getRepBundle().getRep();
		if (rep == null) {
			return "null rep";
		}

		int[] simpleRep = new int[rep.length];
		for (int i = 0; i < rep.length; i++) {
			// FIXME: Possible truncation of value here.
			try {
				simpleRep[i] = rep[i].getValue().intValue();
			} catch (UnsupportedOperationException e) {

			}
		}
		int[] littleRep = new int[simpleRep.length];
		for (int i = 0; i < simpleRep.length; i++) {
			littleRep[littleRep.length - 1 - i] = simpleRep[i];
		}

		int bpu = con.getRepBundle().getBitsPerUnit();
		long mask = 0;
		for (int i = 0; i < bpu; i++) {
			mask = mask << 1 | 1L;
		}
		BigInteger biValue = BigInteger.valueOf(0);
		for (int element : littleRep) {
			biValue = biValue.shiftLeft(bpu);
			biValue = biValue.or(BigInteger.valueOf(element));
		}
		String hexUnModified = biValue.toString(16); // (new
														// BigInteger(littleRep)).toString(16);

		String hex = hexUnModified;
		// Make sure the string is 2*length bytes long
		for (int i = hex.length(); i < rep.length * 2; i++) {
			hex = "0" + hex;
		}
		// Now put in 'UU' for any non-locked byte
		String masked = "";
		boolean someNotLocked = false;
		for (int i = 0; i < rep.length; i++) {
			String next2 = hex.substring(2 * i, 2 * i + 2);
			if (!rep[i].isLocked()) {
				someNotLocked = true;
				next2 = "UU";
			}
			masked = masked + next2;
		}
		if (someNotLocked) {
			return "0x" + masked;
		} else {
			return "0x" + hexUnModified;
		}
	}

	ConstantNode(Constant constant, String id, int fontSize) {
		super(constant, id, fontSize);
	}

	@Override
	protected String getBodyLabel() {
		String label = super.getBodyLabel();
		Constant constant = (Constant) getComponent();
		return label + "\\n" + value(constant);
	}

}
