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
 * SequentialBlock is a chunk of statements bounded by begin and end.
 *
 * <P>
 * Example:<BR>
 * <CODE>
 * begin<BR>
 *   result<=32'hFACE;<BR>
 * end<BR>
 * </CODE>
 *<P>
 * Created: Fri Mar 02 2001
 *
 * @author abk
 * @version $Id: SequentialBlock.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SequentialBlock implements Statement
{

    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    List body = new ArrayList();

    public SequentialBlock()
    {
        ;
    } // SequentialBlock()

    public SequentialBlock(Statement s) 
    {
        body.add(s);
    } // SequentialBlock()

    public void add(Statement s)
    {
        body.add(s);
    }

    public Collection getNets()
    {
        HashSet nets = new HashSet();
        
        for (Iterator it = body.iterator(); it.hasNext();)
        {
            nets.addAll(((Statement)it.next()).getNets());
        }
        
        return nets;
    } // getNets()
    

    public Lexicality lexicalify()
    {
        Lexicality lex = new Lexicality();
        
        lex.append(Keyword.BEGIN);
        
        for (Iterator it = body.iterator(); it.hasNext();)
        {
            Statement s = (Statement)it.next();
            
            lex.append(s);
        }

        lex.append(Keyword.END);

        return lex;

    } // lexicalify()
    
    public String toString()
    {
        return lexicalify().toString();
    }
    
} // end of class SequentialBlock
