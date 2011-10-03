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

import net.sf.openforge.verilog.model.*;

/**
 * CommaDelimitedStatement represents a sequence of
 * {@link VerilogElement VerilogElements} seperated by commas.  This
 * is particularly usefull with the FStatement.FWrite class.
 *
 * <p>Created: Fri Aug 23 09:31:04 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CommaDelimitedStatement.java 2 2005-06-09 20:00:48Z imiller $
 */
public class CommaDelimitedStatement implements Statement
{
    private static final String _RCS_ = "$Rev: 2 $";

    private List elements = new ArrayList();
    
    public CommaDelimitedStatement (Collection elements)
    {
        this.elements = new ArrayList(elements);
    }

    public CommaDelimitedStatement ()
    {
    }

    /**
     * Adds another element the sequence of comma delimited elements.
     */
    public void append (VerilogElement element)
    {
        this.elements.add(element);
    }

    public void prepend (VerilogElement element)
    {
        this.elements.add(0, element);
    }
    

    public Collection getNets ()
    {
        HashSet nets = new HashSet();
        for (Iterator iter = elements.iterator(); iter.hasNext();)
        {
            VerilogElement ve = (VerilogElement)iter.next();
            if (ve instanceof Statement)
                nets.addAll(((Statement)ve).getNets());
            else if (ve instanceof Expression)
                nets.addAll(((Expression)ve).getNets());
            else if (ve instanceof Net)
                nets.add(ve);
        }
        return nets;
    }

    public Lexicality lexicalify ()
    {
        Lexicality lex = new Lexicality();
        for (Iterator iter = elements.iterator(); iter.hasNext();)
        {
            lex.append((VerilogElement)iter.next());
            if (iter.hasNext())
                lex.append(Symbol.COMMA);
        }
        return lex;
    }

    public String toString ()
    {
        return lexicalify().toString();
    }
    
}// CommaDelimitedStatement
