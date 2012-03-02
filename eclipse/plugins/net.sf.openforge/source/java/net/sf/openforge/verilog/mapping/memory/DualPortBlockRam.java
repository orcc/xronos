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

import net.sf.openforge.verilog.model.BinaryNumber;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Concatenation;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.ParameterSetting;
import net.sf.openforge.verilog.model.Wire;
import net.sf.openforge.verilog.pattern.StatementBlock;

/**
 * A base class of all of the XST supported dual-port block ram models.
 * <P>
 *
 * Created: Tue Jun 18 12:37:06 2002
 *
 * @author cwu
 * @version $Id: DualPortBlockRam.java 538 2007-11-21 06:22:39Z imiller $
 */

public abstract class DualPortBlockRam extends DualPortRam
{
    
    private static int instancecnt = 0;
    
    public abstract String getName ();
    
    public abstract int getWidth ();
    
    public abstract int getDepth ();
    
    public abstract int getCost ();
    
    
    public boolean isDataOutputRegistered ()
    {
        return(true);
    }
    
    public int getParityWidth ()
    {
        // If our data width of the memory isn't 2^N, then we have
        // parity bits
        switch(getWidth())
        {
            case 9:
                return 1;
            case 18:
                return 2;
            case 36:
                return 4;
            default:
                return 0;
        }
    }

    public StatementBlock initialize ()
    {
        StatementBlock initial_block = new StatementBlock();
        initial_block.add(new InlineComment(" Initialization of Dual Port Block ram now done through explicit parameter setting.", Comment.SHORT));
        return initial_block;
    }
    
    private void initialize (ModuleInstance instance)
    {
        // The init strings for LUT memories use the syntax INIT for
        // X1 memories, and INIT_NM where NM is the output data bit
        // the init value corresponds to.  Each INIT string is written
        // as hex values and is as long as the memory is deep.  The
        // ordering is MSB to LSB.  The array generator will always
        // supply us with all the reset bits to fill our memory, even
        // if some of the data bits are not used.

        // loop for each INIT string
        int initcnt = 0;
        int shift = 0;
        int digitIndex = 0;
        byte[] hexDigitsData = new byte[64];
        
        // Stick in the write mode selection so a simulataneous
        // write and read works
        // This parameter now set in DualPortBlockWriter based on LogicalMemoryPort.getWriteMode
//         if(isBlockRam16())
//         {
//             instance.addParameterValue(new ParameterSetting("WRITE_MODE_A", "\"READ_FIRST\""));
//             instance.addParameterValue(new ParameterSetting("WRITE_MODE_B", "\"READ_FIRST\""));
//         }
        
        for (int i = 0; i < (getDepth() * getWidth()); i = i + getWidth())
        {
            int bitoffset = i;
            // Grab a widths worth of bits for the data
            for (int k=0; k<(getWidth() - getParityWidth()); k++)
            {
                hexDigitsData[digitIndex] |= (getInitBit(bitoffset++) << shift);
                shift++;
                
                if(shift == 4)
                {
                    shift = 0;
                    digitIndex++;
                }
            }

            if (digitIndex == hexDigitsData.length)
            {
                // Compose the INIT string we just accumulated
                String initParam = "INIT_" + Integer.toHexString(0x80000000 | initcnt++).substring(6,8).toUpperCase();
                String initValue = "256'h";
                
                for (int digit = (hexDigitsData.length - 1); digit >= 0; digit--)
                {
                    initValue += Integer.toHexString(hexDigitsData[digit] & 0xf);
                }
                
                shift = 0;
                digitIndex = 0;
                for (int clr=0; clr<hexDigitsData.length; clr++)
                {
                    hexDigitsData[clr] = 0;
                }
                instance.addParameterValue(new ParameterSetting(initParam, initValue));
            }
        }

        // loop through array by rows and calculate each INIP string
        if (getParityWidth() != 0)
        {
            int initparcnt = 0;
            byte[] hexDigitsPar = new byte[64];
            shift = 0;
            digitIndex = 0;
            
            for (int i=0; i<(getDepth() * getWidth()); i = i + getWidth())
            {
                for (int j=0; j<getParityWidth(); j++)
                {
                    hexDigitsPar[digitIndex] |= (getInitBit(i + (getWidth() - getParityWidth()) + j)) << shift;
                    shift++;
                    if (shift == 4)
                    {
                        shift = 0;
                        digitIndex++;
                    }
                }
                
                if (digitIndex == hexDigitsPar.length)
                {
                    // compose the INITP string according to what we have
                    String initParam = "INITP_0" + (initparcnt++ & 0xf);
                    String initValue = "256'h";
                    
                    for (int digit = (hexDigitsPar.length - 1); digit >= 0; digit--)
                    {
                        initValue += Integer.toHexString(hexDigitsPar[digit] & 0xf);
                    }
                    
                    digitIndex = 0;
                    shift = 0;
                    for (int clr=0; clr<hexDigitsPar.length; clr++)
                    {
                        hexDigitsPar[clr] = 0;
                    }
                    instance.addParameterValue(new ParameterSetting(initParam, initValue));
                }
            }
        }
    }
    
    public ModuleInstance instantiate ()
    {
        String instance_name = getName() + "_instance_" + instancecnt++;
        
        ModuleInstance moduleInstance = new ModuleInstance(getName(), instance_name);

        initialize(moduleInstance);
        
        writePortInstantiation(moduleInstance, PORTA, "A");
        writePortInstantiation(moduleInstance, PORTB, "B");

        return moduleInstance;
    }

    private void writePortInstantiation (ModuleInstance inst, int index, String id)
    {
        BinaryNumber zero = new BinaryNumber(0, 1);
        
        /* Connecting CLK port */
        Expression clk_exp = zero;
        if (clkName[index].length() > 0)
        {
            clk_exp = new Wire(clkName[index], 1);
        }
        inst.connect(new Wire("CLK"+id, 1), clk_exp);
        
        /* Connecting WE port */
        Expression we_exp = zero; 
        if (weName[index].length() > 0)
        {
            we_exp = new Wire(weName[index], 1);
        }
        inst.connect(new Wire("WE"+id, 1), we_exp);
        
        /* Connecting EN port */
        Expression en_exp = zero;
        if (oeName[index].length() > 0)
        {
            en_exp = new Wire(oeName[index], 1);
        }
        inst.connect(new Wire("EN"+id, 1), en_exp);
        
        /* Connecting RST/SSR port */
        Expression reset_exp = new Wire("RST"+id, 1);
        if (isBlockRam16())
        {
            reset_exp = new Wire("SSR"+id, 1);
        }
        inst.connect((Net)reset_exp, zero);
        
        /* Connecting ADDR port */
        Expression addr_exp = new Wire(addrName[index], addrWidth);
        if (getLibAddressWidth() > addrWidth && addrName[index].length() > 0)
        {
            // We need to pad with zeros since the memory has more
            // address bits than are attached to us
            int zerobits = getLibAddressWidth() - addrWidth;
            Concatenation padded_addr = new Concatenation();
            padded_addr.add(new BinaryNumber(0, zerobits));
            if (!addrScalar[index])
            {
                int msb = addrWidth + addrStartBit[index] -1;
                int lsb = addrStartBit[index];
                padded_addr.add(((Net)addr_exp).getRange(msb, lsb));
            }
            else
            {
                padded_addr.add(addr_exp);
            }
            addr_exp = padded_addr;
        }
        else if (getLibAddressWidth() <= addrWidth && addrName[index].length() > 0)
        {
            if (!addrScalar[index])
            {
                int msb = addrStartBit[index] + getLibAddressWidth() - 1;
                int lsb =  addrStartBit[index];
                addr_exp = ((Net)addr_exp).getRange(msb, lsb);
            }
        }
        else
        {
            addr_exp = new BinaryNumber(0,getLibAddressWidth());
        }
        inst.connect(new Wire("ADDR"+id, getLibAddressWidth()), addr_exp);
        
        /* Connecting DI port */
        int nonpardatwidth = getWidth() - getParityWidth();
        Expression di_exp = new Wire(dataInName[index], 256);
        if (nonpardatwidth > dataAttachWidth[index] && dataInName[index].length() > 0)
        {
            // padded with zeros
            int zerobits = nonpardatwidth - dataAttachWidth[index];
            Concatenation padded_di = new Concatenation();
            padded_di.add(new BinaryNumber(0, zerobits));
            if (!datScalar[index])
            {
                int msb = dataAttachWidth[index] + dataInStartBit[index] - 1;
                int lsb = dataInStartBit[index];
                padded_di.add(((Net)di_exp).getRange(msb, lsb));
            }
            else
            {
                di_exp = new Wire(dataInName[index], 1);
                padded_di.add(di_exp);
            }
            di_exp = padded_di;
        }
        else if (nonpardatwidth <= dataAttachWidth[index] && dataInName[index].length() > 0)
        {
            if (!datScalar[index])
            {
                int msb = dataInStartBit[index] + nonpardatwidth - 1;
                int lsb = dataInStartBit[index];
                di_exp = ((Net)di_exp).getRange(msb, lsb);
            }
            else
            {
                di_exp = new Wire(dataInName[index], 1);
            }
        }
        else
        {
            di_exp = new BinaryNumber(0, nonpardatwidth);
        }
        inst.connect(new Wire("DI"+id, nonpardatwidth), di_exp);
        
        /* Connecting DO port */
        Expression do_exp = new Wire(dataOutName[index], 256);
        if (nonpardatwidth > dataAttachWidth[index] && dataOutName[index].length() > 0)
        {
            // padded with zeros
            int zerobits = nonpardatwidth - dataAttachWidth[index];
            Concatenation padded_do = new Concatenation();
            padded_do.add(new Wire(dataOutExtraName[index], zerobits+getParityWidth()).getRange(zerobits-1, 0));
            if (!datScalar[index])
            {
                int msb = dataAttachWidth[index] + dataOutStartBit[index] - 1;
                int lsb = dataOutStartBit[index];
                padded_do.add(((Net)do_exp).getRange(msb, lsb));
            }
            else
            {
                do_exp = new Wire(dataOutName[index], 1);
                padded_do.add(do_exp);
            }
            do_exp = padded_do;
        }
        else if (nonpardatwidth <= dataAttachWidth[index] && dataOutName[index].length() > 0)
        {
            if (!datScalar[index])
            {
                int msb = dataOutStartBit[index] + nonpardatwidth - 1;
                int lsb = dataOutStartBit[index];
                do_exp = ((Net)do_exp).getRange(msb, lsb);
            }
            else
            {
                do_exp = new Wire(dataOutName[index], 1);
            }
        }
        else
        {
            do_exp = new Wire("", nonpardatwidth);
        }
        inst.connect(new Wire("DO"+id, nonpardatwidth), do_exp);
        if (getParityWidth() == 0)
        {
            return;
        }
        
        /* Connecting DIP and DOP port */
        int pardatwidth = dataAttachWidth[index] - nonpardatwidth;
        // can't have negative widths!
        if (pardatwidth < 0)
        {
            pardatwidth = 0;
        }
        // If for some reason they gave us more bits than we can use
        if (pardatwidth > getParityWidth())
        {
            pardatwidth = getParityWidth();
        }
        if (getParityWidth() != 0)
        {
            // We have parity bits to hook up too!
            int zerobits = getParityWidth() - pardatwidth;
            /* Connecting DIP port */
            Expression dip_exp =  new Wire(dataInName[index], 256);
            if (getParityWidth() > pardatwidth && zerobits == getParityWidth())
            {
                dip_exp = new BinaryNumber(0, getParityWidth());
            }
            else if (getParityWidth() > pardatwidth && dataInName[index].length() > 0)
            {
                // padd with zeros
                Concatenation padded_dip = new Concatenation();
                padded_dip.add(new BinaryNumber(0, zerobits));
                if (!datScalar[index])
                {
                    int msb = pardatwidth + dataInStartBit[index] + nonpardatwidth - 1;
                    int lsb = dataInStartBit[index] + nonpardatwidth;
                    padded_dip.add(((Net)dip_exp).getRange(msb, lsb));
                }
                else
                {
                    dip_exp =  new Wire(dataInName[index], 1);
                    padded_dip.add(dip_exp);
                }
                dip_exp = padded_dip;
            }
            else if (getParityWidth() <= pardatwidth && dataInName[index].length() > 0)
            {
                if (!datScalar[index])
                {
                    int msb = pardatwidth + dataInStartBit[index] + nonpardatwidth - 1;
                    int lsb = dataInStartBit[index] + nonpardatwidth;
                    dip_exp = ((Net)dip_exp).getRange(msb, lsb); 
                }
                else
                {
                    dip_exp =  new Wire(dataInName[index], 1);
                }
            }
            else
            {
                dip_exp = new BinaryNumber(0, getParityWidth());
            }
            inst.connect(new Wire("DIP"+id, getParityWidth()), dip_exp);
            
            /* Connecting DOP port */
            Expression dop_exp =  new Wire(dataOutName[index], 256);
            if (getParityWidth() > pardatwidth && zerobits == getParityWidth())
            {
                dop_exp = new Wire("", getParityWidth());
            }
            else if (getParityWidth() > pardatwidth && dataOutName[index].length() > 0)
            {
                // padd with zeros
                Expression dox_exp = new Wire(dataOutExtraName[index], zerobits).getFullRange();
                Concatenation padded_dop = new Concatenation();
                // Check to prevent invalid bit(ie: extra_0[-1:-1])
                if((nonpardatwidth - dataAttachWidth[index] + zerobits - 1) <= 0)
                {
                    padded_dop.add(dox_exp);
                }
                else
                {
                    int msb = nonpardatwidth - dataAttachWidth[index] + zerobits - 1;
                    int lsb = nonpardatwidth - dataAttachWidth[index];
                    padded_dop.add(((Net)dox_exp).getRange(msb, lsb));
                }
                if (!datScalar[index])
                {
                    int msb = pardatwidth + dataOutStartBit[index] + nonpardatwidth - 1;
                    int lsb = dataOutStartBit[index] + nonpardatwidth;
                    padded_dop.add(((Net)dop_exp).getRange(msb, lsb));
                }
                else
                {
                    dop_exp =  new Wire(dataOutName[index], 1);
                    padded_dop.add(dop_exp);
                }
                dop_exp = padded_dop;
            }
            else if (getParityWidth() <= pardatwidth && dataOutName[index].length() > 0)
            {
                if (!datScalar[index])
                {
                    int msb = pardatwidth + dataOutStartBit[index] + nonpardatwidth - 1;
                    int lsb = dataOutStartBit[index] + nonpardatwidth;
                    dop_exp = ((Net)dop_exp).getRange(msb, lsb); 
                }
                else
                {
                    dop_exp =  new Wire(dataOutName[index], 1);
                }
            }
            else
            {
                dop_exp = new Wire("", pardatwidth);
            }
            inst.connect(new Wire("DOP"+id, getParityWidth()), dop_exp);
        }
        
    }
    
}
