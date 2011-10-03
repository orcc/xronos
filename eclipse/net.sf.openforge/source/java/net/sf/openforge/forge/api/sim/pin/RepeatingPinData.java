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
package net.sf.openforge.forge.api.sim.pin;

/**
 *
 * This class is used to describe a repeating, contiguous set of pin data.
 * For example, if you want to set the next seven pin values to 0x10, you can do
 * that by doing:
 *
 * <code><pre>
 *
 * RepeatingPinData rpd=new RepeatingPinData(7,0x10);
 *
 * </pre></code>
 *
 * This has the effect of setting the next 7 clock ticks to 1 thru 7<p>
 *
 * You can also use this to create ascending/descending patterns.
 */
public class RepeatingPinData extends SequentialPinData
{
    /**
     * Create a repeating pin value based on a specified SignalValue
     * which is certain number of clocks long
     *
     * @param clockTicks number of clocks to repeat value
     * @param value value to repeat
     */
    public RepeatingPinData(int clockTicks, SignalValue value)
    {
        for(int i=0;i<clockTicks;i++)
        {
            add(value);
        }
    }

    /**
     * Create a repeating pin value based on a specified long
     * which is certain number of clocks in length
     *
     * @param clockTicks number of clocks to repeat value
     * @param value value to repeat
     */
    public RepeatingPinData(int clockTicks, long value)
    {
        this(clockTicks,new SignalValue(value));
    }

    /**
     * Create a repeating pin value based on a specified float
     * which is certain number of clocks in length
     *
     * @param clockTicks number of clocks to repeat value
     * @param value value to repeat
     */
    public RepeatingPinData(int clockTicks, float value)
    {
        this(clockTicks,new SignalValue(value));
    }

    /**
     * Create a repeating pin value based on a specified double
     * which is certain number of clocks in length
     *
     * @param clockTicks number of clocks to repeat value
     * @param value value to repeat
     */
    public RepeatingPinData(int clockTicks, double value)
    {
        this(clockTicks,new SignalValue(value));
    }

    /**
     * Create a repeating pin value based on a specified long
     * which is certain number of clocks in length, which increments
     * by a certain value as it repeats. The value may be negative.
     *
     * @param clockTicks number of clocks to repeat value
     * @param value value to repeat
     * @param increment value to increment by as you repeat
     */
    public RepeatingPinData(int clockTicks, long value, long increment)
    {
        for(int i=0;i<clockTicks;i++)
        {
            add(value);
            value=value+increment;
        }
    }

    /**
     * Repeat a sequence of SignalValue objects
     *
     * @param clockTicks clocks to repeat
     * @param values sequence to repeat
     */
    public RepeatingPinData(int clockTicks, SignalValue[] values)
    {
        for(int i=0;i<clockTicks;i++)
        {
            add(values);
        }
    }

    /**
     * Repeat a sequence of long objects
     *
     * @param clockTicks clocks to repeat
     * @param values sequence to repeat
     */
    public RepeatingPinData(int clockTicks, long[] values)
    {
        for(int i=0;i<clockTicks;i++)
        {
            add(values);
        }
    }

    /**
     * Repeat a sequence of float objects
     *
     * @param clockTicks clocks to repeat
     * @param values sequence to repeat
     */
    public RepeatingPinData(int clockTicks, float[] values)
    {
        for(int i=0;i<clockTicks;i++)
        {
            add(values);
        }
    }

    /**
     * Repeat a sequence of double objects
     *
     * @param clockTicks clocks to repeat
     * @param values sequence to repeat
     */
    public RepeatingPinData(int clockTicks, double[] values)
    {
        for(int i=0;i<clockTicks;i++)
        {
            add(values);
        }
    }

    /**
     * Repeat a string sequence of values
     *
     * @param clockTicks clocks to repeat
     * @param values string sequence to repeat
     */
    public RepeatingPinData(int clockTicks,String s)
    {
        for(int i=0;i<clockTicks;i++)
        {
            add(s);
        }
    }
}









