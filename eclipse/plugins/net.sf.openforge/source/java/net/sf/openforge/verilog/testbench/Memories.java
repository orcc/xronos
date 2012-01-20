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

package net.sf.openforge.verilog.testbench;

import java.util.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.*;

/**
 * Memories contains the myriad of {@link InitializedMemory} object
 * needed for the simulation.  One for each arguement (up to the
 * maximum number of arguements for any 1 given task), one for the
 * results, one for a resultsValid flag, and one for the nextGo.
 *
 * <p>Created: Wed Jan  8 16:46:40 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: Memories.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Memories 
{
    private static final String _RCS_ = "$Rev: 2 $";

    // The InitializedMemories
    private List argMemories;
    private InitializedMemory resultMem;
    private InitializedMemory resultValidMem;
    private InitializedMemory nextGo;
    
    /**
     * Creates the necessary memories and generates their initial
     * values from the supplied {@link TestVector TestVectors}
     *
     * @param vectors a List of {@link TestVector TestVectors}
     * @param taskHandles a Map of Task to TaskHandle
     */
    public Memories (List vectors, Map taskHandles)
    {
        int maxNumberOfArgs = 0;
        for (Iterator iter = vectors.iterator(); iter.hasNext();)
        {
            TestVector tv = (TestVector)iter.next();
            maxNumberOfArgs = java.lang.Math.max(tv.getArgValues().size(), maxNumberOfArgs);
        }

        // Create the argument memories
        this.argMemories = new ArrayList(maxNumberOfArgs);
        for (int i=0; i < maxNumberOfArgs; i++)
        {
            int width = 0;
            for (Iterator iter = taskHandles.values().iterator(); iter.hasNext();)
            {
                width = java.lang.Math.max(width, ((TaskHandle)iter.next()).getWidthOfPort(i));
            }
            argMemories.add(new InitializedMemory("arg" + i + "_data", width));
        }

        // Initialize each argument memory
        for (Iterator iter = vectors.iterator(); iter.hasNext();)
        {
            TestVector tv = (TestVector)iter.next();
            List argValues = tv.getArgValues();
            for (int i=0; i < maxNumberOfArgs; i++)
            {
                InitializedMemory im = (InitializedMemory)argMemories.get(i);
                if (i < argValues.size())
                {
                    im.addInitValue(getConstantFor(argValues.get(i), im.getWidth()));
                }
                else
                {
                    im.addInitValue(getConstantFor(new Long(0), im.getWidth()));
                }
            }
        }

        // Create the result memory
        int maxResultWidth = 1;
        for (Iterator iter = taskHandles.values().iterator(); iter.hasNext();)
        {
            TaskHandle th = (TaskHandle)iter.next();
            maxResultWidth = java.lang.Math.max(maxResultWidth, th.getMaxResultWidth());
        }
        this.resultMem = new InitializedMemory("expected_result", maxResultWidth);
        this.resultValidMem = new InitializedMemory("expected_result_valid", 1);
        
        // Initialize the result memory
        Set warnedTasks = new HashSet();
        for (Iterator iter = vectors.iterator(); iter.hasNext();)
        {
            TestVector tv = (TestVector)iter.next();
            // Use expected wire's width (which same as result width)
            // to test against the constants significant bits width.
            int expectedWidth = ((TaskHandle)taskHandles.get(tv.getTask())).getMaxResultWidth();
            if (expectedWidth > 0)
            {
                resultMem.addInitValue(getConstantFor(tv.getResultValue(), maxResultWidth, true, expectedWidth));
            }
            else
            {
                // Only throw one warning per task.
                if (!warnedTasks.contains(tv.getTask()))
                {
                    String taskName = ID.toVerilogIdentifier(ID.showLogical(tv.getTask()));
                    EngineThread.getGenericJob().warn("Test Vector for task with no result (" + taskName + ").  Cannot check result width.");
                }
                warnedTasks.add(tv.getTask());
                resultMem.addInitValue(getConstantFor(tv.getResultValue(), maxResultWidth));
            }
            resultValidMem.addInitValue(getConstantFor(new Boolean(tv.resultValid()), 1));
        }

        // Create the next Go memory
        assert taskHandles.keySet().size() <= 64 : "Only supporting up to 64 entry methods";
        this.nextGo = new InitializedMemory("nextGo", taskHandles.keySet().size());
        long nextMask = 1;
        for (Iterator iter = vectors.iterator(); iter.hasNext();)
        {
            TestVector tv = (TestVector)iter.next();
            TaskHandle th = (TaskHandle)taskHandles.get(tv.getTask());
            long mask = th.getGoMask();
            if (mask == -1)
            {
                mask = nextMask;
                th.setGoMask(mask);
                nextMask <<= 1;
            }

            this.nextGo.addInitValue(getConstantFor(new Long(mask), nextGo.getWidth()));
        }

        // Pad each memory.  See StateMachine.stateMachine generation
        // of valid flag for why.
        padMemories();

        // Check that all the memories have the same number of vectors
        Set allMems = new HashSet(this.argMemories);
        allMems.add(getResultMemory());
        allMems.add(getResultValidMemory());
        allMems.add(getNextGoMemory());
        int count = getNextGoMemory().depth();
        for (Iterator iter = allMems.iterator(); iter.hasNext();)
        {
            assert count == ((InitializedMemory)iter.next()).depth();
        }
    }

    /**
     * States the initial values for each memory.
     */
    public void stateInits (InitialBlock ib)
    {
        for (Iterator iter = this.argMemories.iterator(); iter.hasNext();)
        {
            ib.add((InitializedMemory)iter.next());
        }
        ib.add(getResultMemory());
        ib.add(getResultValidMemory());
        ib.add(getNextGoMemory());
    }

    /**
     * Does nothing.
     */
    public void stateLogic ()
    {
        // Nothing to do
    }
    

    /**
     * Returns the {@link InitializedMemory} for the numbered argument
     * position.
     */
    public InitializedMemory getArgMemory (int index)
    {
        return (InitializedMemory)this.argMemories.get(index);
    }

    /**
     * Returns the {@link InitializedMemory} for the results values.
     */
    public InitializedMemory getResultMemory ()
    {
        return this.resultMem;
    }

    /**
     * Returns the {@link InitializedMemory} for the results valid values.
     */
    public InitializedMemory getResultValidMemory ()
    {
        return this.resultValidMem;
    }

    /**
     * Returns the {@link InitializedMemory} for the next GO values.
     */
    public InitializedMemory getNextGoMemory ()
    {
        return this.nextGo;
    }

    /**
     * Returns the number of vectors in the memories.
     */
    public int elementCount ()
    {
        // Since we verify that all are equal in the constructor, we
        // can simply choose one of the memories... choose one we KNOW
        // will exist.
        return getNextGoMemory().depth();
    }
    

    /**
     * Add one extra vector of all 0's to the end of each memory to
     * provide a 'resting' state with known values at the end of the
     * simulation.
     */
    private void padMemories ()
    {
        InitializedMemory mem;
        for (Iterator iter = argMemories.iterator(); iter.hasNext();)
        {
            mem = (InitializedMemory)iter.next();
            mem.addInitValue(getConstantFor(new Long(0), mem.getWidth()));
        }
        mem = getResultMemory();
        mem.addInitValue(getConstantFor(new Long(0), mem.getWidth()));

        mem = getResultValidMemory();
        mem.addInitValue(getConstantFor(new Long(0), mem.getWidth()));

        mem = getNextGoMemory();
        mem.addInitValue(getConstantFor(new Long(0), mem.getWidth()));
    }
    
    /**
     * Utility method for generating a Constant given a java lang
     * wrapper class.
     *
     * @param o a value of type 'Object'
     * @param width a value of type 'int'
     * @return a value of type 'Expression'
     */
    private static Expression getConstantFor(Object o, int width, boolean test, int testWidth)
    {
        long longValue = 0;
        if (o instanceof Double)
            longValue = Double.doubleToRawLongBits(((Number)o).doubleValue());
        else if (o instanceof Float)
            longValue = Float.floatToRawIntBits(((Number)o).floatValue());
        else if (o instanceof Number)
            longValue = ((Number)o).longValue();
        else if (o instanceof Boolean)
            longValue = ((Boolean)o).booleanValue() ? 1:0;
        else if (o instanceof Character)
            longValue = ((Character)o).charValue();
        else if (o == null)
            longValue = 0;
        else
            throw new RuntimeException("Unknown type of number for test vector: " + o + " " + o.getClass());

        if (test && !isEnoughBits(o, testWidth))
        {
            throw new RuntimeException("Insufficient pin width to cover test result: Value: 0x"
                + Long.toHexString(longValue) + " pin width " + width);
        }

        return new HexNumber(new HexConstant(longValue, width));
    }

    static Expression getConstantFor(Object o, int width)
    {
        return getConstantFor(o, width, false, 64);
    }
    
    /**
     * Test that 'width' is enough bits to cover the value.
     *
     * @param longValue a value of type 'long'
     * @param width a value of type 'int'
     * @return true if the width is sufficient to cover the specified value.
     */
    static boolean isEnoughBits(Object o, int width)
    {
        long longValue = 0;
        if (o instanceof Number)
            longValue = ((Number)o).longValue();
        else if (o instanceof Boolean)
            longValue = ((Boolean)o).booleanValue() ? 1:0;
        else if (o instanceof Character)
            longValue = ((Character)o).charValue();

        int neededWidth = 1;

        // the -1 case will not detect any zeros in the value and will
        // default to the initial neededWidth of 1, the same with the
        // 0 case but no ones will be found and the neededWidth will
        // be 1.  All other cases will be caught by the loops below.
     
        if (longValue > 0)
        {
            // neededWidth = top most 1 + 2 (position + 1 (for width) + 1)
            for (int i=63; i >= 0; i--)
            {
                if (((longValue >>> i) & 0x1L) != 0)
                {
                    neededWidth = i+1;
                    break;
                }
            }
        }
        else if (longValue < -1)
        {   // neededWidth = top most 0 + 2 (postion + 1 (for width) + 1)
            for (int i=63; i >= 0; i--)
            {
                if (((longValue >>> i) & 0x1L) == 0)
                {
                    neededWidth = i+2;
                    break;
                }
            }
        }
        
        // calculate the max width
        int maxWidth = 64;

        if(o instanceof Boolean)
            maxWidth = 1;
        else if(o instanceof Byte)
            maxWidth = 8;
        else if(o instanceof Character)
            maxWidth = 16;
        else if(o instanceof Short)
            maxWidth = 16;
        else if(o instanceof Integer || o instanceof Float)
            maxWidth = 32;
        else if(o instanceof Long || o instanceof Double)
            maxWidth = 64;

        if(neededWidth > maxWidth)
            neededWidth = maxWidth;


//         // XXX: Temporary fix for C tests to allow boolean values that
//         // are contained in an integer to not flag the constant prop
//         // algorithm as overly aggressive since constant prop makes
//         // the results of boolean operations 1 bit, but C puts them in
//         // integers which are signed, and would normally require 2
//         // bits to represent the number.  This should be fixed again
//         // once unsigned types are added to C, since the C logical
//         // result is really a 1 bit unsigned value.
//         if((width == 1) && (neededWidth == 2))
//         {
//             // We hit the C logical case, let it go through
//             return(true);
//         }
        
        return width >= neededWidth;
    }
    
}// Memories

