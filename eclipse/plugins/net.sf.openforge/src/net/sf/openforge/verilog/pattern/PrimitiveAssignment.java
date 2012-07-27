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

import java.util.List;

import net.sf.openforge.lim.Component;
import net.sf.openforge.verilog.model.Expression;

/**
 * A PrimitiveAssignment is a verilog math operation based on a LIM op which
 * assigns the result to a wire.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: PrimitiveAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class PrimitiveAssignment extends ExpressionAssignment {

	public PrimitiveAssignment(Component c) {
		super(c);
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class And extends PrimitiveAssignment {
		public And(net.sf.openforge.lim.primitive.And and) {
			super(and);
		}

		@Override
		protected Expression makeExpression(List<Expression> operands) {
			Expression e = operands.get(0);
			for (int i = 1; i < operands.size(); i++) {
				e = new net.sf.openforge.verilog.model.Bitwise.And(e,
						operands.get(i));
			}
			return e;
		}
	} // class And

	public static final class Or extends PrimitiveAssignment {
		public Or(net.sf.openforge.lim.primitive.Or or) {
			super(or);
		}

		@Override
		protected Expression makeExpression(List<Expression> operands) {
			Expression e = operands.get(0);
			for (int i = 1; i < operands.size(); i++) {
				e = new net.sf.openforge.verilog.model.Bitwise.Or(e,
						operands.get(i));
			}
			return e;
		}
	} // class Or

	public static final class Not extends PrimitiveAssignment {
		public Not(net.sf.openforge.lim.primitive.Not not) {
			super(not);
		}

		@Override
		protected Expression makeExpression(List<Expression> operands) {
			assert operands.size() == 1 : "Logical NOT must have only 1 operand";
			Expression e = operands.get(0);
			return (new net.sf.openforge.verilog.model.Unary.Negate(e));
		}
	} // class Not

} // class PrimitiveAssignment

