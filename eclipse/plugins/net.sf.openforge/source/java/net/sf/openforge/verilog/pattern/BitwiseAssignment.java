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

import net.sf.openforge.lim.op.*;
import net.sf.openforge.verilog.model.*;

/**
 * A BitwiseAssignment is a verilog math operation based on a LIM op which assigns the 
 * result to a wire. 
 * <P>
 *
 * Created: Tue Mar 12 09:46:58 2002
 *
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: BitwiseAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class BitwiseAssignment extends BinaryOpAssignment
{
    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";
    
    public BitwiseAssignment(BinaryOp bo)
    {
        super(bo);
    }
        
    ////////////////////////////////////////////////
    //
    // inner classes
    //
    
    public static final class And extends BitwiseAssignment
    {
        private boolean dbug = false;

        public And(AndOp and)
        {
            super(and);
        }
        
        protected Expression makeOpExpression(Expression left, Expression right)
        {
            return (new net.sf.openforge.verilog.model.Bitwise.And(left, right));
        }
    } // class And
    
    
    public static final class Or extends BitwiseAssignment
    {
        public Or(OrOp or)
        {
            super(or);
        }
        
        protected Expression makeOpExpression(Expression left, Expression right)
        {
            return (new net.sf.openforge.verilog.model.Bitwise.Or(left, right));
        }
    } // class Or
    
    public static final class Xor extends BitwiseAssignment
    {
        public Xor(XorOp xor)
        {
            super(xor);
        }
        
        protected Expression makeOpExpression(Expression left, Expression right)
        {
            return (new net.sf.openforge.verilog.model.Bitwise.Xor(left, right));
        }
    } // class Xor
    
} // class BitwiseAssignment
