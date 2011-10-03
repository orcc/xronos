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
package net.sf.openforge.verilog.pattern;

import net.sf.openforge.lim.InputPin;
import net.sf.openforge.lim.Value;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.*;

/**
 * A Verilog Input which is based on a LIM {@link InputPin}.<P>
 *
 * Created:   May 7, 2002
 *
 * @author    <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version   $Id: InputPinPort.java 2 2005-06-09 20:00:48Z imiller $
 */
public class InputPinPort extends Input
{
    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";
    
    /**
     * Constructs a InputPinPort based on a LIM InputPin.
     *
     * @param bus  The LIM Pin upon which to base the Net
     */
    public InputPinPort(InputPin pin)
    {
        // Since RegisterReferee and other stuff @ the top level
        // will use the input pin's bus for naming internally we
        // need to be sure that we name it only once.  To do that
        // we have to use the pin's bus in VerilogNamer and also here
        //super(ID.toVerilogIdentifier(ID.showLogical(pin)), 
        //super(ID.toVerilogIdentifier(ID.showLogical(pin.getBus())), 
        //    pin.getWidth());
        super(ID.toVerilogIdentifier(ID.showLogical(pin)),
            (pin.isInferred() ? getSignificantWidth(pin) : pin.getWidth()));
    }

    /**
     * Gets the minimum width needed to represent an input pin.  For an inferred pin,
     * this is the number of bits that range from 0 to the most significant care bit
     * (minimum of 1); otherwise, it is the pin width specified by the user.
     *
     * @param pin an input pin of the design
     * @return the number of bits needed to represent the pin
     */
    private static int getSignificantWidth (InputPin pin)
    {
        /*
         * Find the index + 1 of the most significant care bit, or 1 if there are none.
         */
        final Value value = pin.getBus().getValue();
        int msbIndex = value.getSize() - 1;
        while ((msbIndex > 1) && !value.getBit(msbIndex).isCare())
        {
            msbIndex--;
        }
        return msbIndex + 1;
    }
    
} // InputPinPort

