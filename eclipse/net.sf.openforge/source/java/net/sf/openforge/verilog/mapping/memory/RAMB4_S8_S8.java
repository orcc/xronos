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

package net.sf.openforge.verilog.mapping.memory;

class RAMB4_S8_S8 extends DualPortBlockRam
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    protected RAMB4_S8_S8 ()
    {
    }
    
    public String getName ()
    {
        return("RAMB4_S8_S8");
    }
        
    public int getWidth ()
    {
        return(8);
    }
    
    public int getDepth ()
    {
        return(512);
    }

    public int getCost ()
    {
        return(1);
    }

    public boolean isBlockRam16 ()
    {
        return(false);
    }
}
