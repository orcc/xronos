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

import net.sf.openforge.app.ForgeFatalException;
import net.sf.openforge.util.*;
import net.sf.openforge.verilog.model.*;
import net.sf.openforge.verilog.pattern.*;


/**
 * A base class of all of the XST supported ram models.
 * <P>
 *
 * Created: Tue Jun 18 12:37:06 2002
 *
 * @author cwu
 * @version $Id: Ram.java 432 2007-04-26 18:41:08Z imiller $
 */

public abstract class Ram implements Cloneable
{
    static final String rcs_id = "RCS_REVISION: $Rev: 432 $";

    protected String clkName = "";
    protected String weName = "";
    protected String oeName = "";
    
    protected String addrName = "";
    protected int addrWidth = 0;
    protected int addrStartBit = 0;
    protected boolean addrScalar = false;
    
    protected String dataInName = "";
    protected int dataInStartBit = 0;

    protected String dataOutName = "";
    protected int dataOutStartBit = 0;
    protected String dataOutExtraName = "";
    
    protected int dataAttachWidth = 0;
    protected boolean datScalar = false;
    
    protected int[] initValues = null;
    protected int currentInitBit = 0;

    // Virtex4/pro lut based ram primitives
    // V4 CANNOT use the 128X1S primitive.  ngdbuild throws away the
    // INIT value!
    private static Ram[] v4lut = {
        new RAM16X1S(),new RAM16X2S(),new RAM16X4S(),new RAM16X8S(),
        new RAM32X1S(),new RAM32X2S(),new RAM32X4S(),new RAM32X8S(),
        new RAM64X1S(),new RAM64X2S()};

    // Virtex4/pro block ram primitives
    private static Ram[] v4block = {
        new RAMB16_S1(),new RAMB16_S2(),new RAMB16_S4(),
        new RAMB16_S9(),new RAMB16_S18(),new RAMB16_S36()};
    
    // Virtex2/pro lut based ram primitives
    private static Ram[] v2lut = {
        new RAM16X1S(),new RAM16X2S(),new RAM16X4S(),new RAM16X8S(),
        new RAM32X1S(),new RAM32X2S(),new RAM32X4S(),new RAM32X8S(),
        new RAM64X1S(),new RAM64X2S(),new RAM128X1S()};

    // Virtex2/pro block ram primitives
    private static Ram[] v2block = {
        new RAMB16_S1(),new RAMB16_S2(),new RAMB16_S4(),
        new RAMB16_S9(),new RAMB16_S18(),new RAMB16_S36()};

    // Virtex/spartan2 lut ram primitives
    private static Ram[] vlut = {new RAM16X1S(),new RAM32X1S()};

    // Virtex/spartan2 block ram primitives
    private static Ram[] vblock = {
        new RAMB4_S1(),new RAMB4_S2(),new RAMB4_S4(),
        new RAMB4_S8(),new RAMB4_S16()};

    // Spartan/XC4000EX/E/XLA/XV lut ram primitives
    private static Ram[] spartanLut = {new RAM16X1S(),new RAM32X1S()};
    
    protected Ram ()
    {
    }
    
    public static Ram[] getMappers (XilinxDevice xd, boolean lut_map)
    {
        switch(xd.getFamily())
        {
            case XilinxDevice.VIRTEX2P:
            case XilinxDevice.VIRTEX2:
                return lut_map ? v2lut : v2block;
                
            case XilinxDevice.VIRTEX4SX:
            case XilinxDevice.VIRTEX4FX:
            case XilinxDevice.VIRTEX4LX:
                return lut_map ? v4lut : v4block;

                // XXX: We don't have docs on spartan 3 internal
                // resources, play it really safe with the basics that
                // we'd expect them to support.
            case XilinxDevice.SPARTAN3:
                return lut_map ? vlut : v2block;

            case XilinxDevice.VIRTEX:
            case XilinxDevice.VIRTEXE:
            case XilinxDevice.SPARTAN2:
            case XilinxDevice.SPARTAN2E:
                return lut_map ? vlut : vblock;

            case XilinxDevice.SPARTAN:
            case XilinxDevice.SPARTANXL:
            case XilinxDevice.XC4000EX:
            case XilinxDevice.XC4000E:
            case XilinxDevice.XC4000XLA:
            case XilinxDevice.XC4000XV:
                // These devices only have LUT memory, so ignore the
                // lut request and only supply LUT mappers.
                lut_map = true;
                return spartanLut;
                
            case XilinxDevice.XC4000:
            case XilinxDevice.XC4000L:
            case XilinxDevice.XC5200:
            case XilinxDevice.XC3000:
            case XilinxDevice.XC9500:
            case XilinxDevice.XC9500XL:
            case XilinxDevice.XC9500XV:
                // What are they doing!!!  no mappers for these guys!
                return null;
            default:
                // They must not have defined a part in the
                // preferences, no mappers for you!
                return null;
        }    
        
    }
    
    
    public void setClkName (String name)
    {
        clkName = name;
    }


    public void setWeName (String name)
    {
        weName = name;
    }


    public void setOeName (String name)
    {
        oeName = name;
    }
    
    public void setAddr (String name, int width, int startBit, boolean scalar)
    {
        setAddrName(name);
        setAddrWidth(width);
        setAddrStartBit(startBit);
        setAddrScalar(scalar);
    }
    
    public void setAddrName (String name)
    {
        addrName = name;
    }


    public void setAddrWidth (int width)
    {
        if(width > getLibAddressWidth())
        {
            addrWidth = getLibAddressWidth();
        }
        else
        {
            addrWidth = width;
        }
    }


    public void setAddrStartBit (int startBit)
    {
        addrStartBit = startBit;
    }


    public void setAddrScalar (boolean scalar)
    {
        addrScalar = scalar;
    }
    

    public void setDataInName (String name)
    {
        dataInName = name;
    }


    public void setDataAttachWidth (int width)
    {
        if(width > getWidth())
        {
            dataAttachWidth = getWidth();
        }
        else
        {
            dataAttachWidth = width;
        }
    }

    public void setDataScalar (boolean scalar)
    {
        datScalar = scalar;
    }
    

    public void setDataInStartBit (int startBit)
    {
        dataInStartBit = startBit;
    }


    public void setDataOutName (String name)
    {
        dataOutName = name;
    }


    public void setDataOutExtraName (String name)
    {
        dataOutExtraName = name;
    }
    
    
    public void setDataOutStartBit (int startBit)
    {
        dataOutStartBit = startBit;
    }
    

    public int getLibAddressWidth ()
    {
        int indexSize = 0;

        if(getDepth() == 0)
        {
            indexSize = 0;
        }
        else if(getDepth() == 1)
        {
            indexSize = 1;
        }
        else
        {
            // Find the top 1 in the number
            int size = 31;
            
            while(((1 << size) & (getDepth() - 1)) == 0)
            {
                size--;
            }
            
            indexSize = size + 1;
        }

        return indexSize;
    }

    public void setNextInitBit (int bit)
    {
        if(initValues == null)
        {
            // allocate enough storage for all the bits of the memory.
            int size = getWidth() * getDepth();

            if((size % 32) == 0)
            {
                initValues = new int[(size / 32)];
            }
            else
            {
                initValues = new int[(size / 32) + 1];
            }
        }
        
        int index = currentInitBit / 32;
        int offset = currentInitBit % 32;

        if(index < initValues.length)
        {
            // Add the given bit to the location
            initValues[index] |= (bit & 1) << offset;
            currentInitBit++;
        }
        else
        {
            throw new ForgeFatalException(this.getClass().getName() + ".setNextInitBit(): array index out of bounds");
        }
        
    }
    
    
    public int getInitBit (int addr)
    {
        if(initValues == null)
        {
            return 0;
        }
        
        int index = addr / 32;
        int offset = addr % 32;

        if(index < initValues.length)
        {
            return((initValues[index] >> offset) & 1);
        }
        else
        {
            throw new ForgeFatalException(this.getClass().getName() + ".getInitBit(" + addr + "): array index out of bounds");
        }
    }    

    
    public String toString ()
    {
        return(getName());
    }
    

    public Object clone ()
    {
        try
        {
            return super.clone();
        }
        catch(CloneNotSupportedException cnse)
        {
            return null;
        }
    }

    
    // Abstract methods subclasses must implement

    public abstract String getName ();
    
    public abstract int getWidth ();

    public abstract int getDepth ();

    public abstract int getCost ();

    public abstract boolean isDataOutputRegistered ();
    
    public abstract StatementBlock initialize ();
    
    public abstract ModuleInstance instantiate();

}


