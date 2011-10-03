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

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.verilog.model.*;

/**
 * A PrimitiveAssignment is a verilog math operation based on a LIM op which assigns the 
 * result to a wire. 
 * <P>
 *
 * Created: Tue Mar 12 09:46:58 2002
 *
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: PrimitiveAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class PrimitiveAssignment extends ExpressionAssignment
{
    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";
    
    public PrimitiveAssignment(Component c)
    {
        super(c);
    }
        
    ////////////////////////////////////////////////
    //
    // inner classes
    //
    
    public static final class And extends PrimitiveAssignment
    {
        public And(net.sf.openforge.lim.And and)
        {
            super(and);
        }
        
        protected Expression makeExpression(List operands)
        {
	    Expression e=(Expression)operands.get(0);
	    for(int i=1;i<operands.size();i++)
	    {
		e=new net.sf.openforge.verilog.model.Bitwise.And(e,(Expression)operands.get(i));
	    }
	    return e;
        }
    } // class And
    
    
    public static final class Or extends PrimitiveAssignment
    {
        public Or(net.sf.openforge.lim.Or or)
        {
            super(or);
        }
        
        protected Expression makeExpression(List operands)
        {
	    Expression e=(Expression)operands.get(0);
	    for(int i=1;i<operands.size();i++)
	    {
		e=new net.sf.openforge.verilog.model.Bitwise.Or(e,(Expression)operands.get(i));
	    }
	    return e;
        }
    } // class Or
    
    public static final class Not extends PrimitiveAssignment
    {
        public Not(net.sf.openforge.lim.Not not)
        {
            super(not);
        }
        
        protected Expression makeExpression(List operands)
        {
	    assert operands.size()==1 : "Logical NOT must have only 1 operand";
	    Expression e=(Expression)operands.get(0);
            return (new net.sf.openforge.verilog.model.Unary.Negate(e));
        }
    } // class Not
    
} // class PrimitiveAssignment


