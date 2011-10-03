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


import java.util.*;
import java.math.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.verilog.model.*;
import net.sf.openforge.verilog.pattern.MemoryModule;

/**
 * A VerilogMemory is a base class for a memory.
 * <P>
 *
 * Created: Tue Jun 18 12:37:06 2002
 *
 * @author cwu
 * @version $Id: VerilogMemory.java 70 2005-12-01 17:43:11Z imiller $
 */

public abstract class VerilogMemory
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 70 $";
    
    protected static int memory_module_id = 0;
    
    private MemoryBank memBank;
    private int depth;
    private int width;
    private int addrWidth;

    public static final String clk  = "CLK";
    public static final String ren  = "EN";
    public static final String wen  = "WE";
    public static final String adr  = "ADDR";
    public static final String done = "DONE";
    public static final String din  = "DIN";
    public static final String dout = "DOUT";
    
    /**
     * Constructs a <code>VerilogMemory</code> based on a {@link MemoryBank}.
     *
     * @param memBank the {@link MemoryBank} which is being instantiated(accessed).
     */
    public VerilogMemory (MemoryBank memBank)
    {
        this.memBank = memBank;
        this.depth = memBank.getDepth();
        this.width = memBank.getWidth();
        this.addrWidth = memBank.getAddrWidth();
    }

    public int getDepth ()
    {
        return this.depth;
    }

    public int getAddrWidth ()
    {
        return this.addrWidth;
    }

    public int getDataWidth ()
    {
        return this.width;
    }

    protected MemoryBank getMemBank ()
    {
        return this.memBank;
    }

    protected boolean isLUT ()
    {
        return this.memBank.getImplementation().isLUT();
    }

    public abstract Module defineModule ();
    
    //public abstract Module defineInferredModule ();
    
    public abstract ModuleInstance instantiate (MemoryBank bank);
    
    /**
     * Gets the instantiated memory module name
     *
     * @return A String of memory module name
     */
    public abstract String getName ();

    public Ram getLowestCost (Ram[] availableMaps)
    {
        // We assume speed mapping, but if the synth_opt flow is
        // verilog_area.opt then put area as the highest priority
        boolean opt_for_speed = true;
        
        // Now we will cycle through each map function and determine
        // how we would build a memory that meets the width and depth
        // requirements we are given out of that memory map element.
        // For each configuration we will calculate the cost in LUTs
        // or block rams (as returned by the map function), the waste
        // (number of bits allocated but never accessed), the depth (a
        // less deep memory is faster to access), and the number of
        // elements needed (all other things being equal, less is
        // more).  These four factors will then be minimized with the
        // most minimal configuration winning.  The most minimal
        // configuration is dependent on if we are going for speed or
        // area.  Speed puts the most emphasis on depth of the array,
        // to minimize the output muxing, area puts the most emphasis
        // on waste, to most compactly map the memory to the architecture.
        Ram match = null;
        int min_cost = Integer.MAX_VALUE;
        int min_waste = Integer.MAX_VALUE;
        int min_depth = Integer.MAX_VALUE;
        int min_count = Integer.MAX_VALUE;
        //int result_width = 0;
        //int result_depth = 0;

        for(int i = 0; i < availableMaps.length; i++)
        {
            Ram targetRam = availableMaps[i];
            
            //System.out.println("\nTrying ram map: " + targetRam);
            
            // Calculate what it will take to build the required
            // memory array out of the targetRam element.
            int elements_width = getDataWidth() / targetRam.getWidth();
            if ((getDataWidth() % targetRam.getWidth()) != 0)
            {
                elements_width++;
            }
            
            int elements_depth = this.depth / targetRam.getDepth();
            if ((this.depth % targetRam.getDepth()) != 0)
            {
                elements_depth++;
            }
            
            int array_width = elements_width * targetRam.getWidth();
            int array_depth = elements_depth * targetRam.getDepth();
            
            int elements_count = elements_width * elements_depth;
            int elements_cost = elements_count * targetRam.getCost();
            
            int array_waste = (array_width * array_depth) - (getDataWidth() * this.depth);
            
            boolean new_match = false;

//             System.out.println(" Testing: " + targetRam);
//             System.out.println("   depth: " + elements_depth);
//             System.out.println("   count: " + elements_count);
//             System.out.println("    cost: " + elements_cost);
//             System.out.println("   waste: " + array_waste);

            if(opt_for_speed)
            {
                if(elements_depth < min_depth)
                {
                    new_match = true;
                }
                else if(elements_depth == min_depth)
                {
                    //
                    // This used to be count over cost priority but we
                    // switched so that a 16x5 memory takes 5 16x1's
                    // instead of 1 16x8.  This will not affect the
                    // block rams because the cost for any block ram
                    // == 1 so that the cost will always equal the
                    // count.
                    //
                    if(elements_cost < min_cost)
                    {
                        new_match = true;
                    }
                    else if(elements_cost == min_cost)
                    {
                        if(elements_count < min_count)
                        {
                            new_match = true;
                        }
                        else if(elements_count == min_count)
                        {
                            if(array_waste < min_waste)
                            {
                                new_match = true;
                            }
                        }
                    }
                }
            }
            else
            {
                if(array_waste < min_waste)
                {
                    new_match = true;
                }
                else if(array_waste == min_waste)
                {
                    if(elements_cost < min_cost)
                    {
                        new_match = true;
                    }
                    else if(elements_cost == min_cost)
                    {
                        if(elements_depth < min_depth)
                        {
                            new_match = true;
                        }
                        else if(elements_depth == min_depth)
                        {
                            if(elements_count < min_count)
                            {
                                new_match = true;
                            }
                        }
                    }
                }
            }
            
            if(new_match == true)
            {
                match = targetRam;
                min_cost = elements_cost;
                min_waste = array_waste;
                min_depth = elements_depth;
                min_count = elements_count;
                
//                 System.out.println("   New Minimum:");
//                 System.out.println(" element array: " + elements_width + " X " + elements_depth);
//                 System.out.println("       element: " + targetRam);
//                 System.out.println("          cost: " + min_cost);
//                 System.out.println("         waste: " + min_waste);
//                 System.out.println("         depth: " + min_depth);
//                 System.out.println("         count: " + min_count);
            }
        }
//         System.out.println("match: " + match);
        return match;
    }

    /**
     * Retrieves the init values from the {@link MemoryBank} and
     * converts them to a BitSet.
     *
     * @return a value of type 'BitSet'
     */
    protected BitSet getInitValuesAsBitSet ()
    {
        //assert getDepth() == getMemBank().getInitValues().size();
        
        BitSet result = new BitSet(getDataWidth() * getDepth());
        int currentBit = 0;
        for (BigInteger lineInitValue : getInitValuesByLine())
        {
            for (int bitIndex = 0; bitIndex < getDataWidth(); bitIndex++)
            {
                if (lineInitValue.testBit(bitIndex))
                    result.set(currentBit);
                else
                    result.clear(currentBit);
                currentBit++;
            }
        }

        return result;
    }

    protected List<BigInteger> getInitValuesByLine ()
    {
        assert getDepth() == getMemBank().getInitValues().length;
        AddressableUnit[][] initValues = getMemBank().getInitValues();
        final int stride = getMemBank().getAddressStridePolicy().getStride();
        
        final List<BigInteger> ret = new ArrayList();
        assert initValues.length == getDepth();
        for (int line=0; line < getDepth(); line++)
        {
            AddressableUnit[] lineValue = initValues[line];
            ret.add(AddressableUnit.getCompositeValue(lineValue, getMemBank().getAddressStridePolicy()));
//             assert lineValue.length <= 8;
//             long value = 0;
//             for (int i=0; i < lineValue.length; i++)
//             {
//                 value |= ((((long)lineValue[i]) & 0xFFL) << (8*i));
//             }
//             ret.add(new Long(value));
        }
        return ret;
    }
    

    protected void debugContents (MemoryModule memoryModule)
    {
        if (EngineThread.getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.DEBUG_MEM_LOCATIONS))
        {
            memoryModule.state(new InlineComment(getClass().toString(), Comment.SHORT));
            memoryModule.state(new InlineComment("Memory contents", Comment.SHORT));        
            List initVals = getInitValuesByLine();
            for (int i=0; i < initVals.size(); i++)
            {
                memoryModule.state(new InlineComment(i + ": " + initVals.get(i).toString() + "(" +
                                       ((BigInteger)initVals.get(i)).toString(16) + ")",
//                                        Long.toHexString(((Long)initVals.get(i)).longValue()) + ")",
                                       Comment.SHORT));
            }
        }
    }
    
} // class VerilogMemory
