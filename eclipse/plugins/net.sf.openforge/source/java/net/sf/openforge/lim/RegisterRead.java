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
package net.sf.openforge.lim;

import net.sf.openforge.util.naming.*;

/**
 * A {@link Register} read access.
 *
 * @author  Stephen Edwards
 * @version $Id: RegisterRead.java 88 2006-01-11 22:39:52Z imiller $
 */
public class RegisterRead extends Access implements StateAccessor
{
    private static final String _RCS_ = "$Rev: 88 $";

    /** records whether this read is a signed memory read. */
    private boolean isSigned;
    
    /**
     * Creates a new RegisterRead targetted at the specified register
     *
     * @param register a non-null 'Register' object.
     */
    RegisterRead (Register register, boolean isSigned)
    {
        super(register, 0, register.isVolatile());

        this.isSigned = isSigned;

        getGoPort().setUsed(true);

        /*
         * One bus for the data.
         */
        makeExit(1);

        getResultBus().setFloat(register.isFloat());
    }

    /**
     * Accept method for the Visitor interface
     */
    public void accept (Visitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * Returns the targetted Register which is a StateHolder object.
     *
     * @return the targetted Register
     */
    public StateHolder getStateHolder ()
    {
        return getRegister();
    }
    
    public Bus getResultBus ()
    {
        return (Bus)getExit(Exit.DONE).getDataBuses().get(0);
    }

    /**
     * Returns true if the accessed register is stores a floating
     * point value.
     */
    public boolean isFloat ()
    {
        return getRegister().isFloat();
    }

    /**
     * Returns true if this is a signed access to the backing register.
     *
     * @return a value of type 'boolean'
     */
    public boolean isSigned ()
    {
        return this.isSigned;
    }

    public Port getSidebandDataPort ()
    {
        if (getDataPorts().size() > 0)
        {
            return (Port)getDataPorts().get(0);
        }
        
        return null;
    }

    /**
     * Returns a fixed latency of ZERO since all register reads are
     * combinational.
     */
    public Latency getLatency ()
    {
        return Latency.ZERO;
    }
    

    /**
     * Overwrites the method in {@link Component} to return
     * <em>true</em> since the {@link RegisterRead} operation needs a
     * <em>go</em> and a <em>done</em>.
     */
    public boolean isControlled ()
    {
        return true;
    }

    /**
     * This accessor may execute in parallel with other similar (non
     * state modifying) accesses.
     */
    public boolean isSequencingPoint ()
    {
        return false;
    }
    
    /**
     * Creates the sideband data/control connections necessary to
     * connect this Operation to the resource it targets.
     */
    public void makeSidebandConnections ()
    {
        assert getDataPorts().size() < 1 : "Can only create sideband connections once " + this;

        // The data in port from the register.
        makeDataPort(Component.SIDEBAND);
    }
    
    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */

    /**
     * Pushes size, care, and constant information forward through
     * this RegisterRead from the Register's access Value.
     *
     * @return a value of type 'boolean'
     */
    public boolean pushValuesForward ()
    {
        boolean mod = false;

        Value newValue;
        if (getSidebandDataPort() == null)
        {
            newValue = new Value(getRegister().getInitWidth(), this.isSigned);
        }
        else
        {
            Value inValue = getSidebandDataPort().getValue();
            newValue = new Value(inValue.getSize(), this.isSigned);
            for (int i=0; i < inValue.getSize(); i++)
            {
                newValue.setBit(i, inValue.getBit(i));
            }
        }
        mod |= getResultBus().pushValueForward(newValue);
        
        return mod;
    }

    /**
     * Reverse partial constant prop on an RegisterRead just updates
     * the consumed Bus Value.
     *
     * @return a value of type 'boolean'
     */
    public boolean pushValuesBackward ()
    {
        boolean mod = false;

        Port sideband = getSidebandDataPort();
        if (sideband != null)
        {
            Value resultBusValue = getResultBus().getValue();
            Value newValue = new Value(resultBusValue.getSize(), this.isSigned);
            for (int i=0; i < resultBusValue.getSize(); i++)
            {
                Bit bit = resultBusValue.getBit(i);
                if (!bit.isCare() || bit.isConstant())
                {
                    newValue.setBit(i, Bit.DONT_CARE);
                }
            }
            mod |= sideband.pushValueBackward(newValue);
        }
        
        return mod;
    }

    /*
     *    End new constant prop rules implementation.
     * =================================================
     */
    
    /**
     * Returns a copy of this RegisterRead by creating a new register read
     * off of the {@link Register} associated with this node.  We
     * create a new access instead of cloning because of the way that
     * the Register stores references (not in Referent).  Creating a
     * new access correctly sets up the Referent/Reference
     * relationship.
     *
     * @return a RegisterRead object.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        RegisterRead clone = getRegister().makeReadAccess(this.isSigned);
        this.copyComponentAttributes(clone);
        return clone;
    }
    
    /**
     * Just for convenience...
     */
    private Register getRegister ()
    {
        return (Register)getResource();
    }
}
