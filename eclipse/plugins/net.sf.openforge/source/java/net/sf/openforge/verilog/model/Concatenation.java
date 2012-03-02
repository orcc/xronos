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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A Concatenation is an expression which represents the
 * joining of a set of zero or more sub-expressions.
 * <P>
 * It takes the form...<BR>
 * <B>{</B>expression {,expression} <B>}</B>
 * <P>
 * Example<BR>
 * <CODE>
 * {or1_reult, ARG0, and5_result}<BR>
 * </CODE>
 * <P>
 *
 * Created: Tue Feb 27 2001
 *
 * @author abk
 * @version $Id: Concatenation.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Concatenation implements Expression
{

	@SuppressWarnings("unused")
	private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    int total_width = 0;

    List<Expression> elements = new ArrayList<Expression>();

    public Concatenation()
    {
        ;
    } // Concatenation()

    public Concatenation(Expression e)
    {
       add(e);
    } // Concatenation()

    public Concatenation(Expression[] e) 
    {
        for (int i=0;i<e.length; i++)
        {
            add(e[i]);
        }
    } // Concatenation()

    public void add(Expression e)
    {
        total_width += e.getWidth();
        elements.add(e);
    } // add()
    
    public Lexicality lexicalify()
    {
        Lexicality lex = new Lexicality();
        
        lex.append(Symbol.OPEN_CURLY);

        for (Iterator it = elements.iterator(); it.hasNext();)
        {
            lex.append((Expression)it.next());
            
            if (it.hasNext()) 
            {
                lex.append(Symbol.COMMA);
            }
        }
        
        lex.append(Symbol.CLOSE_CURLY);

        return lex;
        
    } // lexicalify()
    
    public Collection getNets()
    {
        HashSet nets = new HashSet();
    
        for (Iterator it = elements.iterator(); it.hasNext();)
        {
            nets.addAll(((Expression)it.next()).getNets());
        }
        return nets;
    } // getNets()
    
    public int getWidth()
    {
        return total_width;
    }

    public String toString()
    {
        return lexicalify().toString();
    }

} // end of class Concatenation
