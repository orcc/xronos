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

import net.sf.openforge.verilog.pattern.CommaDelimitedStatement;

/**
 * FStatement is the parent class for all the "$f..." statements.
 *
 * <p>Created: Fri Aug 23 08:59:55 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FStatement.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class FStatement implements Statement
{
    private static final String _RCS_ = "$Rev: 2 $";

    protected Keyword type;
    protected Statement expr;
    
    public FStatement (Keyword type, Statement state)
    {
        this.type = type;
        setStatement(state);
    }

    public FStatement (Keyword type)
    {
        this(type, null);
    }

    protected void setStatement (Statement state)
    {
        this.expr = state;
    }

    public Lexicality lexicalify()
    {
        Lexicality lex = new Lexicality();
        
        lex.append(Symbol.DOLLAR);
        lex.append(this.type);

        if (this.expr != null)
        {
            lex.append(Symbol.OPEN_PARENTHESIS);
            lex.append(expr);
            lex.append(Symbol.CLOSE_PARENTHESIS);
        }

        lex.append(Symbol.SEMICOLON);
        
        return lex;
    } // lexicalify()

    public Collection getNets()
    {
        HashSet nets = new HashSet();

        if (expr != null)
            nets.addAll(expr.getNets());

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
    
    /**
     * A statement of form:
     * <pre>$fwrite(<fileId>, <statement>);</pre>
     */
    public static final class FWrite extends FStatement
    {
        public FWrite (IntegerWire file, Statement state)
        {
            super(Keyword.FWRITE);
            CommaDelimitedStatement cds = new CommaDelimitedStatement();
            cds.append(state);
            cds.prepend(file);
            setStatement(cds);
        }
        
    } // end of inner class FWrite
    
    /**
     * A statement of form:
     * <pre>file <= $fopen(<statement>); </pre>
     */
    public static final class FOpen extends FStatement
    {
        IntegerWire file;
        public FOpen (IntegerWire file, StringStatement name) 
        {
            super(Keyword.FOPEN);
            this.file = file;            
            setStatement(name);
        }
        
        /**
         * Not using an assign since assigns need sized expressions...
         */
        public Lexicality lexicalify ()
        {
            Lexicality lex = new Lexicality();
            lex.append(file);
            lex.append(Symbol.NONBLOCKING_ASSIGN);
            lex.append(Symbol.DOLLAR);
            lex.append(this.type);
            lex.append(Symbol.OPEN_PARENTHESIS);
            lex.append(this.expr);
            lex.append(Symbol.CLOSE_PARENTHESIS);
            lex.append(Symbol.SEMICOLON);
            return lex;
        }

        public Collection getNets ()
        {
            Collection nets = super.getNets();
            nets.add(file);
            return nets;
        }
        
    } // end of inner class FOpen
    
    /**
     * A statement of form: $finish;
     */
    public static final class Finish extends FStatement
    {
        public Finish ()
        {
            super(Keyword.FINISH);
        }
        
    } // end of inner class Finish
    
}// FStatement

