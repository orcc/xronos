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

import net.sf.openforge.forge.api.pin.ClockDomain;

/**
 *
 * This class is used to describe a data for the reset pin. It is a convenience
 * class only. It let's you specify a specific number of clocks to hold
 * reset high, thena  number of clocks to hold reset low. In a constructor, use
 * it like:
 *
 * <code><pre>
 *
 * PinSimData.setDriveData(Entry.getDesignResetPin(),new ResetPinData(2,100));
 *
 * or
 *
 * ResetPinData.init(2,100); 
 *
 * </pre></code>
 *
 * This has the effect of setting 2 clocks to high, followed by 100
 * to low.
 */
public class ResetPinData extends SequentialPinData
{
    /**
     * Create a pin data set consisting of a high pattern
     * followed by low. Useful shortcut for the design
     * reset pin.
     *
     * @param ticksHigh number of clock ticks to hold the
     * the value high
     * @param ticksLow number of bits to hold it low
     */
    public ResetPinData(int ticksHigh, int ticksLow)
    {
        add(new RepeatingPinData(ticksHigh,1));
        add(new RepeatingPinData(ticksLow,0));
    }

    /**
     * Create a pin data set consisting of 3 high clocks
     * followed by specified amount of low. Useful shortcut
     * for the design reset pin.
     *
     * Describe constructor here.
     *
     * @param ticksLow number of clocks to hold low
     */
    public ResetPinData(int ticksLow)
    {
        add(new RepeatingPinData(3,1));
        add(new RepeatingPinData(ticksLow,0));
    }

    /**
     * Create a pin data set consisting of a high pattern
     * followed by low, then set it to drive the design reset
     * pin. Convenience method.
     *
     * @param ticksHigh number of clock ticks to hold the
     * the value high
     * @param ticksLow number of bits to hold it low
     */
    public static void init(int ticksHigh,int ticksLow)
    {
        //PinSimData.setDriveData(Entry.getDesignResetPin(),
        //    new ResetPinData(ticksHigh,ticksLow));
        // FYNEH: This only works for the global reset,
        // not for individual EntryMethod resets.
        PinSimData.setDriveData(ClockDomain.GLOBAL.getResetPin(),
            new ResetPinData(ticksHigh,ticksLow));
    }

    /**
     * Create a pin data set consisting of a default (3 clock)
     * high pattern followed by the specified low period,
     * then set it to drive the design reset pin. Convenience method.
     *
     * @param ticksHigh number of clock ticks to hold the
     * the value high
     * @param ticksLow number of bits to hold it low
     */
    public static void init(int ticksLow)
    {
        PinSimData.setDriveData(ClockDomain.GLOBAL.getResetPin(),
            new ResetPinData(ticksLow));
        
    }
}
