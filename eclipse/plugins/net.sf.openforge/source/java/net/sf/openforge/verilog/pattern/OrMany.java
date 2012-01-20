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
 * OrMany is the bitwise 'or' of many terms eg:
 * <pre>
 * go1 | go2 | done1 | done2
 *</pre>
 *
 * <p>Created: Fri Aug 23 11:57:40 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OrMany.java 2 2005-06-09 20:00:48Z imiller $
 */
public class OrMany implements Expression
{
    private static final String _RCS_ = "$Rev: 2 $";

    private List exprs;
    
    public OrMany ()
    {
        this.exprs = new ArrayList();
    }

    public OrMany (Collection many)
    {
        this();
        this.exprs.addAll(many);
    }

    public void add (Expression expr)
    {
        this.exprs.add(expr);
    }

    public Collection getNets ()
    {
        Set nets = new LinkedHashSet();
        for (Iterator iter = exprs.iterator(); iter.hasNext();)
        {
            nets.addAll(((Expression)iter.next()).getNets());
        }
        return nets;
    }

    public int getWidth ()
    {
        int max = 0;
        for (Iterator iter = exprs.iterator(); iter.hasNext();)
        {
            max = java.lang.Math.max(max, ((Expression)iter.next()).getWidth());
        }
        return max;
    }

    public Lexicality lexicalify ()
    {
        Lexicality lex = new Lexicality();
        for (Iterator iter = exprs.iterator(); iter.hasNext();)
        {
            lex.append((Expression)iter.next());
            if (iter.hasNext())
            {
                /*
                 * Changed to bitwise OR because doutConcatOr in
                 * StructualMemory was getting sliced down to a
                 * single bit by the boolean OR.  According to Ian,
                 * our Verilog should never need anything other than
                 * a bitwise OR. --SGE
                 */
                lex.append(Symbol.OR);
            }
        }
        return lex;
    }
    
}// OrMany
