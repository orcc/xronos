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

/**
 * Range provides either a bit-select or part-select, as in [7] or [31:0],
 * respectively.
 *
 * <P>
 *
 * Created: Fri Feb 09 2001
 *
 * @author abk
 * @version $Id: Range.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Range implements VerilogElement
{
    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    int msb;
    int lsb;

    /**
     * A Range based on width presumes to run from
     * (width - 1) to 0. If the width is zero, the
     * result is a bit-select of bit 0.
     *
     */
    public Range(int width) 
    {
        this(width - 1, 0);
    } // Range()


    /**
     * A Range from msb to lsb, producing a part-select
     * if msb is greater than lsb, or a bit select if
     * msb is equal to lsb.
     */
    public Range(int msb, int lsb)
    {
        if (lsb > msb) {
            throw new IllegalBitRange(msb, lsb);
        }
        this.msb = msb;
        this.lsb = lsb;
    }
    
    public int getWidth()
    {
        return (msb - lsb) + 1;
    }
    
    public int getMSB()
    {
        return msb;
    }
    
    public int getLSB()
    {
        return lsb;
    }
    
    public Lexicality lexicalify()
    {
        Lexicality lex = new Lexicality();
        
        lex.append(Symbol.OPEN_SQUARE);
        lex.append(new Constant(msb));
        if (msb != lsb)
        {
            lex.append(Symbol.COLON);
            lex.append(new Constant(lsb));
        }
        lex.append(Symbol.CLOSE_SQUARE);
        
        return lex;
    } // lexicalify()
    
    public class IllegalBitRange extends VerilogSyntaxException
    {

        private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

        public IllegalBitRange(int msb, int lsb) 
        {
            super (new String("Illegal bit range -- msb:lsb " + msb + ":" + lsb));
        } // InvalidVerilogIdentifierException()

    } // end of nested class InvalidVerilogIdentifierException
    
} // end of class Range
