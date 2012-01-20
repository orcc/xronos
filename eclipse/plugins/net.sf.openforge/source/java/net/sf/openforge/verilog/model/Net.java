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
 * Net is an Expression represented by an Identifier which has a specific Type
 * and size. When used as directly, the Net represents itself with just the identifier.
 * Utility methods are available for creating sub-nets, ranged LValues, and more.
 * <P>
 * Created: Thu Feb 08 2001
 *
 * @author abk
 * @version $Id: Net.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Net implements NetLValue, SimpleExpression
{

    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    Keyword type;
    Identifier id;
    int width;
    int lsb;
    int msb;

    /**
     * Constructs a full sized net.
     */
    public Net(Keyword type, Identifier id, int width) 
    {
        this.type = type;
        this.id = id;
        
        if (width > 0)
        {
            this.width = width;
            this.lsb = 0;
            this.msb = width-1;
        }
        else 
        {
            throw new IllegalNetWidth(width, id.toString());
        }
        
    } // Net()

    public Net(Keyword type, Identifier id, int msb, int lsb)
    {
        this.type = type;
        setIdentifier(id);
        if ((msb >= lsb) && (lsb >= 0))
        {
            this.lsb = lsb;
            this.msb = msb;
            this.width = (msb - lsb) + 1;
        }
        else 
        {
            throw new IllegalBitRange(msb, lsb);
        }
    } // Net()
    
    public Keyword getType()
    {
        return type;
    }
    
    public Identifier getIdentifier()
    {
        return id;
    }

    protected void setIdentifier(Identifier id)
    {
        this.id = id;
    }
    
    public int getWidth()
    {
        return width;
    }

    public int getMSB()
    {
        return msb;
    }
    
    public int getLSB()
    {
        return lsb;
    }
    
    public Collection getNets()
    {
        Collection c = new HashSet(1);
        c.add(this);
        return c;
    }
    
    public Lexicality lexicalify()
    {
        return id.lexicalify();
    }

    public Expression getBitSelect(int bit)
    {
        return getRange(bit, bit);    
    }
    
    public Expression getRange(int msb, int lsb)
    {
        return new NetRange(msb, lsb);
    }

    public Expression getFullRange()
    {
        return new NetRange();
    }
    
    public String toString()
    {
        return lexicalify().toString();
    }

    //////////////////// 
    // nested classes

    /**
     * Net.Range is a Net which presents the fully qualified
     * Net (identifier plus a range). If MSB==LSB, then the
     * Range is presented as a bit select.
     * <P>
     * Example:<BR>
     * <CODE><PRE>
     * a[32:0]
     * a[14]
     * </PRE>
     * </CODE>
     * <P>
     *
     * Created: Tue Feb 13 2001
     *
     * @author abk
     * @version $Id: Net.java 2 2005-06-09 20:00:48Z imiller $
     */
    public class NetRange implements NetLValue
    {

        private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";
        
        Range range;

        public NetRange()
        {
            this(msb, lsb);
        } // NetRange();
    
        public NetRange(int msb, int lsb) 
        {
            if ((msb > Net.this.msb) || (lsb < Net.this.lsb))
            {
                throw new IllegalBitRange(msb, lsb);
            }
            
            range = new Range(msb, lsb);
        } // NetRange()

        public int getWidth()
        {
            return range.getWidth();
        }

        public Collection getNets()
        {
            return Collections.singletonList(Net.this);
        }
    
        public Lexicality lexicalify()
        {
            Lexicality lex = new Lexicality();
            
            lex.append(id);

            // If this is a scalar (1 bit wide) net and we get the
            // range we really just want the net id and not a vector
            // select.
            //if (range.getWidth() > 1 || Net.this.getWidth() > 1)
            if (range.getWidth() != Net.this.getWidth())
                lex.append(range);
            
            return lex;
        } // lexicalify()

        public String toString()
        {
            return lexicalify().toString();
        }
    
    } // end of class Range
    
    ////////////////////
    // Nested exceptions
    //
     
    public class IllegalNetWidth extends VerilogSyntaxException
    {

        private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

        public IllegalNetWidth(int width, String id) 
        {
            super (new String("Illegal width for net " + width + " id: " + id));
        } // IllegalNetWidth()

    } // end of nested class IllegalNetWidth

    public class IllegalBitRange extends VerilogSyntaxException
    {

        private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

        public IllegalBitRange(int msb, int lsb) 
        {
            super (new String("Illegal bit range -- msb:lsb " + msb + ":" + lsb));
        } // InvalidVerilogIdentifierException()

    } // end of nested class InvalidVerilogIdentifierException

} // end of class Net
