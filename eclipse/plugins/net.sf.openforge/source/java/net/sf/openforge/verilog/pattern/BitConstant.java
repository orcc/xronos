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

import net.sf.openforge.lim.*;
import net.sf.openforge.verilog.model.*;

/**
 * A verilog expression which is based on a contiguous set of LIM {@link Bit Bits},
 * which have constant values.
 * 
 * Created:   August 6, 2002
 *
 * @author    <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version   $Id: BitConstant.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BitConstant implements Expression
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    Expression constant;
    
    public BitConstant(List bits)
    {
        long constant_value = 0;
        int constant_size = 0;
        long bitmask = 0x1;
        for (Iterator it = bits.iterator(); it.hasNext();)
        {
            Bit bit = (Bit)it.next();
            if (bit.isOn())
            {
                constant_value |= bitmask;
            }
            bitmask <<= 1;
            constant_size++;
        }
        constant = new BinaryNumber(constant_value, constant_size);
    }
    
    public int getWidth()
    {
        return constant.getWidth();
    }
    
    public Collection getNets()
    {
        return constant.getNets();
    }
    
    public Lexicality lexicalify()
    {
        return constant.lexicalify();
    }
}
