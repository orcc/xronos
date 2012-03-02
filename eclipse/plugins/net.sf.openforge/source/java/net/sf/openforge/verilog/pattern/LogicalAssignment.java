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
package net.sf.openforge.verilog.pattern;

import net.sf.openforge.lim.op.BinaryOp;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ConditionalOrOp;
import net.sf.openforge.verilog.model.Expression;

/**
 * A LogicalAssignment is a verilog math operation based on a LIM op which
 * assigns the result to a wire.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: LogicalAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class LogicalAssignment extends BinaryOpAssignment {

	public LogicalAssignment(BinaryOp bo) {
		super(bo);
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class And extends LogicalAssignment {
		public And(ConditionalAndOp and) {
			super(and);
		}

		protected Expression makeOpExpression(Expression left, Expression right) {
			return (new net.sf.openforge.verilog.model.Logical.And(left, right));
		}
	} // class And

	public static final class Or extends LogicalAssignment {
		public Or(ConditionalOrOp or) {
			super(or);
		}

		protected Expression makeOpExpression(Expression left, Expression right) {
			return (new net.sf.openforge.verilog.model.Logical.Or(left, right));
		}
	} // class Or

} // class LogicalAssignment
