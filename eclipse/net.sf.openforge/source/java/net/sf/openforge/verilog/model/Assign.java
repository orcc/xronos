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

import java.util.*;

/**
 * Assign produces an assignment statement, setting a NetLValue to an Expression.
 *
 * <P>
 *
 * Created: Fri Feb 09 2001
 *
 * @author abk
 * @version $Id: Assign.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Assign
    implements Statement
{

    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    Symbol assign;
    NetLValue net;
    Expression expression;

    public Assign(Symbol assign, NetLValue net, Expression expression, boolean checkBalance) 
    {
        this.assign = assign;
        this.net = net;
        this.expression = expression;
        
        /** It is the expression's responsibility to correctly report
         * the width of the result it produces. Assignments should be
         * exactly matched, so any problems should be corrected by
         * re-considering the width reported by the expression.
         */
        if (checkBalance)
        {
            if (net.getWidth() < expression.getWidth()) 
                throw new UnbalancedAssignmentException(net, expression);
        }
    } // Assign()
    
    public Assign(Symbol assign, NetLValue net, Expression expression) 
    {
        this(assign, net, expression, true);
    } // Assign()

    public Lexicality lexicalify()
    {
        Lexicality lex = new Lexicality();
        
        lex.append(net);
        lex.append(assign);
        lex.append(expression);
        lex.append(Symbol.SEMICOLON);
        
        return lex;
    } // lexicalify()

    public Collection getNets()
    {
        HashSet nets = new HashSet();

        // Hack to work around MemoryElements which are NetLValues but
        // are not nets....  This is an awful hack, we should re-work
        // MemoryElements to extend Net when we have time.
        // 12/09/02  Determined that this isn't needed.  If you are
        // getting class cast exceptions when lexicalifying you may
        // need to re-assert this.
        //        if (net instanceof Net)
        {
            nets.add(net);
        }
        nets.addAll(expression.getNets());

        return nets;
    } // getNets();
    

    public String toString()
    {
        return lexicalify().toString();
    }

    ////////////////////////////////
    //
    // inner classes
    //
    
    public static final class Blocking extends Assign
    {
        public Blocking(NetLValue net, Expression expression) 
        {
            super(Symbol.BLOCKING_ASSIGN, net, expression);
        }
        
    } // end of inner class Blocking
    
    public static final class NonBlocking extends Assign
    {
        public NonBlocking(NetLValue net, Expression expression) 
        {
            super(Symbol.NONBLOCKING_ASSIGN, net, expression);
        }
        
    } // end of inner class NonBlocking
    
    public static final class Continuous extends Assign
    {
        public Continuous(NetLValue net, Expression expression, boolean checkBalance)
        {
            super (Symbol.CONTINUOUS_ASSIGN, net, expression, checkBalance);
        }
        
        public Continuous(NetLValue net, Expression expression)
        {
            super(Symbol.CONTINUOUS_ASSIGN, net, expression);
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = new Lexicality();

            lex.append(Keyword.ASSIGN);
            lex.append(net);
            lex.append(assign);
            lex.append(expression);
            lex.append(Symbol.SEMICOLON);

            return lex;
        } // lexicalify()
        
    } // end of inner class Continuous
    
    public static class UnbalancedAssignmentException extends VerilogSyntaxException
    {

        private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

        public UnbalancedAssignmentException(NetLValue net, Expression expression) 
        {
            super (new String("continuous assignment has mismatched bit sizes: " + 
                net.toString() + "(" + net.getWidth() + ")" + 
                "!=" + 
                expression.toString() + "(" + expression.getWidth() + ")"));
        } // UnbalancedAssignmentException()

    } // end of nested class UnbalancedAssignmentException
    
} // end of class Assign
