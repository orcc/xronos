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

package net.sf.openforge.optimize.constant.rule;

import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.optimize.ComponentSwapVisitor;
import net.sf.openforge.optimize._optimize;

/**
 * AddOpRule.java
 * 
 * <pre>
 * a + 0 = a
 * </pre>
 * <p>
 * Created: Thu Jul 18 09:24:39 2002
 * 
 * @author imiller
 * @version $Id: AddOpRule.java 2 2005-06-09 20:00:48Z imiller $
 */
public class AddOpRule {

	public static boolean halfConstant(AddOp op, Number[] consts) {
		assert consts.length == 2 : "Expecting exactly 2 port constants for Add Op";
		Number p1 = consts[0];
		Number p2 = consts[1];

		if ((p1 == null && p2 == null) || (p1 != null && p2 != null)) {
			return false;
		}

		Number constant = p1 == null ? p2 : p1;
		Port nonConstantPort = p1 == null ? (Port) op.getDataPorts().get(0)
				: (Port) op.getDataPorts().get(1);

		if (constant.longValue() == 0) {
			if (_optimize.db)
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (a + 0)");
			// a + 0 = a. Simply delete the component and wire
			// through the non-constant port

			// wire through the control.
			ComponentSwapVisitor.wireControlThrough(op);

			// Wire the non constant port through.
			ComponentSwapVisitor.shortCircuit(nonConstantPort,
					op.getResultBus());

			// Delete the op.
			// op.getOwner().removeComponent(op);
			boolean removed = ComponentSwapVisitor.removeComp(op);

			assert removed : "unable to remove " + op;

			return true;
		} else {
			return false;
		}
	}

}// AddOpRule
