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
 *  An old favorite which uses a conditional expression to pick an input
 * to assign to an output Net.<P>
 * Takes the form:<P>
 * <PRE>
 * assign result = sel_a ? data_a : data_b;
 * </PRE>
 *
 * Created:   May 7, 2002
 *
 * @author    <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version   $Id: Two2OneMux.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Two2OneMux implements Statement
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    Statement two2one_statement;

    /**
     * Constructs a Two2OneMux based on either of two selection Nets,
     * which specify which input data Net will be assigned to the result Net.
     *
     * @param result  The result wire to which the 2-1 mux will be assigned
     * @param sel     Expression which selects data_a if true and
     * data_b if false as the output value
     * @param data_a  The first input value
     * @param data_b  The second input value
     */
    public Two2OneMux(Net result, Expression sel, Expression dataA, Expression dataB)
    {
        /*
         * The boolean test expression is grouped in parens because ModelTech
         * can't parse it when it's a literal value (ie, "1 'h 1?").
         */
        Conditional conditional = new Conditional(new Group(sel), dataA, dataB);
        if(_pattern.db) _pattern.d.ln("Select: "+sel);
        two2one_statement = new Assign.Continuous(result, conditional);
    }


    /**
     * Builds a VMux ignoring the selB input since the 'b' input will
     * be selected when the selA is not active (true).
     */
    private Two2OneMux(Net result, Expression selA, Expression selB, Expression dataA, Expression dataB)
    {
        this(result, selA, dataA, dataB);
    }
    
    public Two2OneMux(Net result, Expression[][] entries)
    {
        this(result, entries[0][0], entries[1][0], entries[0][1], entries[1][1]);
    }
    
    /**
     *  Gets the nets attribute of the Two2OneMux object
     *
     * @return   The nets value
     */
    public Collection getNets()
    {
        return two2one_statement.getNets();
    } // getNets()


    /**
     *  Description of the Method
     *
     * @return   Description of the Return Value
     */
    public Lexicality lexicalify()
    {
        return two2one_statement.lexicalify();
    } // lexicalify()

    public String toString()
    {
        return lexicalify().toString();
    }
    
} // Two2OneMux

