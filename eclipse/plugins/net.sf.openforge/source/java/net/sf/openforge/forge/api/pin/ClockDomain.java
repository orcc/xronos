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

package net.sf.openforge.forge.api.pin;


import java.util.*;

import net.sf.openforge.forge.api.entry.EntryMethod;

/**
 * A ClockDomain pairs a ClockPin with a ResetPin, and
 * associates {@link EntryMethod EntryMethods} which
 * operate within the same clock domain.
 * <P>
 * EntryMethods can only be associated with a single domain.
 * 
 */
public class ClockDomain
{
    /** A clock domain which uses the global clock and reset. */
    public final static ClockDomain GLOBAL = new ClockDomain(
        "CLK", ClockPin.UNDEFINED_HZ);
    
    /** This domain's clock. */
    private ClockPin clockPin;

    /** This domain's reset. */
    private ResetPin resetPin;

    /** The <code>EntryMethods</code> which are associated with this
        domain. */
    private Collection entryMethods = new HashSet();
    
    /**
     * Constructs a ClockDomain with a specifically named set of pins.
     *
     * @param clock the name of the clock signal
     * @param frequency the frequency of the clock, in Hz
     */
    public ClockDomain (String clock, long frequency)
    {
        this.clockPin = new ClockPin(clock, frequency);
        this.resetPin = new ResetPin(clock + "_RESET");
        clockPin.setDomain(this);
        resetPin.setDomain(this);
    }

    /**
     * Gets the clock pin used by this domain.
     * 
     * @return the {@link ClockPin} used by this domain.
     */
    public ClockPin getClockPin()
    {
        return clockPin;
    }
    
    /**
     * Gets the reset pin used by this domain.
     * @return the {@link ResetPin} used by this domain.
     */
    public ResetPin getResetPin()
    {
        return resetPin;
    }
}
