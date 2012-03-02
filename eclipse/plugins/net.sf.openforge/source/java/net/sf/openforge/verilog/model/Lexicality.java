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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * 
 * A Lexicality is a collection of Tokens.
 * <P>
 * Project: Forge
 * 
 * Created: abk on Wed May 16 2001
 * Modified: $Date: 2005-06-09 13:00:48 -0700 (Thu, 09 Jun 2005) $ by $Author: imiller $
 * 
 * @author Andreas Kollegger
 * @version $Rev: 2 $
 */
 
public class Lexicality 
{

    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";
    
    LinkedList tokens = new LinkedList();
    
    public Lexicality()
    {
        ;
    } // Lexicality

    public Lexicality(Token[] t)
    {
        for (int i=0; i < t.length; i++)
        {
            append(t[i]);
        }
    }
    
    public void prepend(VerilogElement t)
    {
        tokens.addFirst(t);
    }

    public void append(VerilogElement e)
    {
        tokens.add(e);
    } // append()

    /**
     * The number of elements (Tokens and plain VerilogElements) in this Lexicality. 
     * This is not a cumulative accounting of all Tokens accessible through 
     * a LexicalIterator (which extracts all tokens from sub-levels).
     * 
     */
    public int size()
    {
        return tokens.size();
    }

    public Iterator iterator()
    {
        return new LexicalIterator();
    } // Iterator()

    /**
     * Retrieves the last token.
     */
    public Token getLast()
    {
        return (Token)tokens.getLast();
    }

    /**
     * Produces a mildly formatted String comprised of each Token,
     * with some whitespace interspersed. This is not intended to
     * be used for production quality code. It is here only for
     * convenience and quick debug.
     * <P>
     * Preferably, a custom rule-based writer would process all
     * the tokens returned by the LexicalIterator.
     *
     */
    public String toString() 
    {
        StringBuffer text = new StringBuffer();
        
//         int prev_type = Token.TYPE;

        for (Iterator it = iterator(); it.hasNext(); )
        {
            Token t = (Token)it.next();
//             int current_type = t.getType();
            
//             switch (current_type)
//             {
//                 case Token.TYPE:
//                     break;
//                 case Identifier.TYPE:
//                     if (prev_type != Control.TYPE)
//                     {
//                         text.append(" ");
//                     }
//                     break;
//                 case Constant.TYPE:
//                     break;
//                 case Keyword.TYPE:
//                     if ((prev_type == Keyword.TYPE) ||
//                         (prev_type == Identifier.TYPE) ||
//                         (prev_type == Constant.TYPE) 
//                         )
//                     {
//                         text.append(" ");
//                     }
//                     break;
//                 case Symbol.TYPE:
//                     if (prev_type == Keyword.TYPE)
//                     {
//                         text.append(" ");
//                     }
//                     break;
//                 case Control.TYPE:
//                     break;
//                 default:
//             }
            
            text.append(t.toString());

//             prev_type = current_type;

//             switch (current_type)
//             {
//                 case Token.TYPE:
//                     break;
//                 case Identifier.TYPE:
//                     break;
//                 case Constant.TYPE:
//                     break;
//                 case Keyword.TYPE:
//                     text.append(" ");
//                     prev_type = Control.TYPE;
//                     break;
//                 case Symbol.TYPE:
//                     break;
//                 case Control.TYPE:
//                     break;
//                 default:
//             }
            
            
        } // for(tokens)

        return text.toString();
        
    } // toString()
    
    public class LexicalIterator implements Iterator
    {
        int index = 0;
        Iterator sub_it = null;

        public LexicalIterator()
        {
            assert (!tokens.isEmpty()) : "Created iterator for empty lexicality.";
        } // LexicalIterator()
        
        /**
         *
         * @return <description>
         */
        public Object next()
        {
            VerilogElement next;

            if (sub_it == null)
            {
                next = (VerilogElement)tokens.get(index++);

                if (!(next instanceof Token))
                {
                    sub_it = next.lexicalify().iterator();
                    
                    if (sub_it.hasNext())
                    {
                        next = (VerilogElement)sub_it.next();
                    }
                    else
                    {
                        sub_it = null;
                        next = (VerilogElement)next();
                    }
                }
                
            }
            else 
            {
                next = (VerilogElement)sub_it.next();
            }
            
            if (sub_it != null) 
            {
                if (!sub_it.hasNext()) {
                    sub_it = null;
                }
            }

            return next;
            
        } // next()

        /**
         *
         * @return <description>
         */
        public boolean hasNext()
        {
            boolean has_next = false;
            
            if (tokens.isEmpty())
            {
                has_next = false;
            }
            else if (sub_it == null)
            {
                has_next = (index < tokens.size());
            }
            else 
            {
                has_next = sub_it.hasNext();
            }
                
            return has_next;
        } // hasNext()

        /**
         *
         */
        public void remove()
        {
            // TODO: implement this java.util.Iterator method
        }

    } // end nested-class LexicalIterator
    
    
} // end class Lexicality

