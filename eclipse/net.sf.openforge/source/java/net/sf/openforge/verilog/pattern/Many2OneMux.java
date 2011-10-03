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
 * An old favorite which or's together select/data pairs to produce an output Net.<P>
 * Takes the form:<P>
 * <CODE><PRE>
 * assign result = ({32{sel_a}} & data_a[31:0]) |
 *                 ({32{sel_b}} & data_b[31:0]);
 * </PRE></CODE>
 *
 * Created:   May 7, 2002
 *
 * @author    <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version   $Id: Many2OneMux.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Many2OneMux implements Statement
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    Statement many2one_statement;


    /**
     * Constructs a Many2OneMux based on an array of paired select-data nets.
     *
     * @param result  The result wire to which the N-1 mux will be assigned
     * @param pairs An nx2 array of select/data pairs
     */
    public Many2OneMux(Net result, Expression[][] pairs)
    {
        assert pairs[0].length == 2;
        
        Expression all_pairs = null;
        for (int i=0; i<pairs.length; i++)
        {
            Replication r = new Replication(pairs[i][1].getWidth(), pairs[i][0]);
            Bitwise.And anded_pair = new Bitwise.And(r, pairs[i][1]);
            Group g = new Group(anded_pair);
        
            if (all_pairs == null)
            {
                all_pairs = g;
            }
            else 
            {
                all_pairs = new Bitwise.Or(all_pairs, g);
            }
        }
        many2one_statement = new Assign.Continuous(result, all_pairs);
    }


    /**
     *  Gets the nets attribute of the Many2OneMux object
     *
     * @return   The nets value
     */
    public Collection getNets()
    {
        return many2one_statement.getNets();
    } // getNets()


    /**
     *  Description of the Method
     *
     * @return   Description of the Return Value
     */
    public Lexicality lexicalify()
    {
        return many2one_statement.lexicalify();
    } // lexicalify()

    public String toString()
    {
        return lexicalify().toString();
    }
    
} // Many2OneMux

