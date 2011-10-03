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

package net.sf.openforge.lim.io;

import net.sf.openforge.lim.*;

/**
 * SimplePinAccess is the superclass of specific types of accesses to a
 * {@link SimplePin}.
 *
 *
 * <p>Created: Tue Dec 16 11:22:23 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimplePinAccess.java 40 2005-10-17 15:08:12Z imiller $
 */
public abstract class SimplePinAccess extends Module implements Referencer, StateAccessor, Visitable
{
    private static final String _RCS_ = "$Rev: 40 $";

    private SimplePin targetPin;
    
    /**
     * Constructs a new SimplePinAccess instance which targets the
     * specified SimplePin.
     *
     * @param pin a value of type 'SimplePin'
     * @throws IllegalArgumentException if pin is null
     */
    public SimplePinAccess (SimplePin pin)
    {
        if (pin == null)
        {
            throw new IllegalArgumentException("Target pin cannot be null");
        }
        
        this.targetPin = pin;
    }

    /**
     * Accept the specified visitor
     *
     * @param visitor a Visitor
     */
    public void accept (Visitor visitor)
    {
        visitor.visit(this);
    }
    
    public boolean replaceComponent (Component removed, Component inserted)
    {
        // TBD
        assert false;
        return false;
    }

    /**
     * Returns the targetted {@link SimplePin}.
     *
     * @return a non null 'SimplePin'.
     */
    public SimplePin getTargetPin ()
    {
        return this.targetPin;
    }

    /**
     * determines if this component can be scheduled to execute in
     * fixed known time (all paths through take same time), but
     * because pin accesses may block this overrides the one in
     * component to return false.
     *
     * @return false
     */
    public boolean isBalanceable ()
    {
        return false;
    }
    
    /**
     * Returns the {@link Referenceable} {@link SimplePin} which this
     * access targets.
     *
     * @return a non-null {@link Referenceable}
     */
    public Referenceable getReferenceable ()
    {
        return this.getTargetPin();
    }
    
    /**
     * Returns the {@link SimplePin} object that this access targets.
     *
     * @return a non-null StateHolder
     */
    public StateHolder getStateHolder ()
    {
        return this.getTargetPin();
    }
    
}// SimplePinAccess
