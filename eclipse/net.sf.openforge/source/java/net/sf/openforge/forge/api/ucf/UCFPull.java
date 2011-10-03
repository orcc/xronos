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
/* $Rev: 2 $ */

package net.sf.openforge.forge.api.ucf;

/**
 * The UCF pull attribute for pins. May be PULLUP, PULLDOWN or KEEPER
 */
public final class UCFPull implements UCFAttribute
{

    public final static UCFPull PULLUP = new UCFPull("PULLUP");
    public final static UCFPull PULLDOWN = new UCFPull("PULLDOWN");
    public final static UCFPull KEEPER = new UCFPull("KEEPER");

    private String value;
    
    private UCFPull(String value)
    {
        this.value = value;
    }
    
    /**
     * Produces the ucf attribute "value".
     */
    public String toString()
    {
        return value;
    }
    
    /**
     * @see net.sf.openforge.forge.api.ucf.UCFAttribute#getBit
     */
    public int getBit()
    {
        return -1;
    }
}
