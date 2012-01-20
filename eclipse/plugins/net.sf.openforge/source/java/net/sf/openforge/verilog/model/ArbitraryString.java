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
 * An ArbitraryString is a Token composed of an arbitraty sequence of characters.
 * <P>
 *
 * Created: Wed Feb 07 2001
 *
 * @author abk
 * @version $Id: ArbitraryString.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ArbitraryString 
    extends Token
{

    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    public static final int TYPE = 7;

    String s;
    
    public ArbitraryString(String s)
    {
        super(); 
        this.s = s;
    }
 
    /**
     * Gets the string representation of the token.
     *
     * 
     */
    public String getToken()
    {
        return s;
    }
    
   
    public int getType()
    {
        return TYPE;
    }
    
} // end class ArbitraryString
    

