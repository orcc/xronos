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

import net.sf.openforge.util.HexString;

/**
 * HexConstant is a part of Forge
 *
 * <P>
 *
 * Created: Thu Mar 01 2001
 *
 * @author abk
 * @version $Id: HexConstant.java 2 2005-06-09 20:00:48Z imiller $
 */
public class HexConstant extends Constant
{

    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    public HexConstant(Number n)
    {
        super(n);
    }

    public HexConstant(String hex) 
    {
        super(Long.valueOf(hex, HEX));
    } // HexConstant()
    
    public HexConstant(String hex, int size) 
    {
        super(Long.parseLong(hex, HEX), size);
    } // HexConstant()
    
    public HexConstant(long l)
    {
        super(l);
    }
    
    public HexConstant(long l, int size)
    {
        super(l, size);
    }
    
    public int getRadix()
    {
        return HEX;
    }
    
    public String getToken()
    {       
        return HexString.valueToHex(getValue(), getSize());
    }
    
} // end of class HexConstant
