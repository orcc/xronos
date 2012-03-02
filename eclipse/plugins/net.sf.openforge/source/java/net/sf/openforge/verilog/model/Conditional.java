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
package net.sf.openforge.verilog.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * Conditional selects an expression basd on the value of a condition expression.
 * If the given condition expression is multi-bit, then it will be wrapped in a
 * logical compare against non-zero.
 * <P>
 * Example:<BR>
 * <CODE>
 * a ? 16'hFACE : arg0[15:0]
 * </CODE>
 * <P>
 * Created: Fri Mar 02 2001
 *
 * @author abk
 * @version $Id: Conditional.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Conditional implements Expression
{

    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";
    
    private Expression condition;
    private Expression left;
    private Expression right;

    public Conditional(Expression condition, Expression left, Expression right) 
    {
        this.left = left;
        this.right = right;
        
        if (left.getWidth() != right.getWidth()) throw new Operation.UnbalancedOperationException(left, right);
        
        if (condition.getWidth() > 1) {
            this.condition = new Group(new Compare.NEQ(condition, new HexNumber(0, condition.getWidth())));
        } else {
            this.condition = condition;
        }
    } // Conditional()

    public int getWidth()
    {
        return left.getWidth(); // left and right are guaranteed to be the same size
    } // getWidth()

    public Collection getNets()
    {
        HashSet nets = new HashSet();
        
        nets.addAll(condition.getNets());
        nets.addAll(left.getNets());
        nets.addAll(right.getNets());

        return nets;
    } // getNets()
    
    public Lexicality lexicalify()
    {
        Lexicality lex = new Lexicality();
        lex.append(this.condition);
        lex.append(Symbol.CONDITION);
        lex.append(new Else(left, right));
        return lex;
    } // Conditional()

    public String toString()
    {
        return lexicalify().toString();
    }

    ////////////////////////////////////////////////
    //
    // inner classes
    //
    
    public static final class Else extends Operation
    {
        public Else(Expression left, Expression right)
        {
            super(Symbol.CONDITION_ELSE, left, right);
        }
        public int precedence() { return CONDITIONAL_PRECEDENCE; }
        public boolean isOrdered() { return true; }
    } // end of inner class Else
    
} // end of class Conditional
