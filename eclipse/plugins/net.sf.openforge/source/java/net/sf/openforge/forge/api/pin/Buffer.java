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

package net.sf.openforge.forge.api.pin;


import java.util.List;
import java.util.ArrayList;

import net.sf.openforge.forge.api.ForgeApiException;
import net.sf.openforge.forge.api.internal.*;
import net.sf.openforge.forge.api.ipcore.*;
import net.sf.openforge.forge.api.ucf.*;

/**
 * The <code>Buffer</code> class is the superclass of all hardware
 * buffers.  The core methods that are used to interract with a buffer
 * are described in this class and are inherited by the
 * respective <code>Buffer</code> subclass.  Buffers represent
 * hardware IO buffers, and this class contains methods to interract
 * with them as you would expect to interact with the hardware
 * implementation.  Methods allow a design to <code>set</code>
 * a value to a <code>Buffer</code>, <code>get</code> a value from the
 * <code>Buffer</code>, put the <code>Buffer</code> in a high
 * impedence (tri-state) state with <code>release</code>, and drive
 * the <code>Buffer</code> with the <code>drive</code> method.
 * <code>Buffer</code> classes support two ways to assign a value to
 * the <code>Buffer</code>; first with the <code>setNow</code> method
 * which places the value on the output during the current clock, and
 * <code>setNext</code> which schedules the value to become active on
 * the output after the next clock edge.  For Buffers that support
 * three stating, a set operation only supplies the value, it does not
 * change the enable pin; use the <code>drive/release</code> methods
 * for this, or make use of the <code>assertNow/Next</code> methods
 * which both supply a value and changes the state. Like hardware, a
 * <code>Buffer</code> has a bit width which is defined during
 * construction.  <code>Buffer</code> classes also provide
 * <code>get</code> methods that return all of the Java primitive
 * types: <code>getBoolean, getByte, getChar, getShort, getInt,
 * getLong, getFloat, getDouble, getUnsignedByte, getUnsignedChar,
 * getUnsignedShort, getUnsignedInt,</code>and
 * <code>getUnsignedLong</code>.
 *
 */
public abstract class Buffer
{   
    protected String name;
    private      int size;
    private  boolean isSigned;
    private     long resetValue;
    private  boolean resetEnabled;
    private  boolean resetValueSupplied;
    protected    int inputPipelineDepth = 0;
    
    /** A List of {@link net.sf.openforge.forge.api.ucf.UCFAttribute} which
     * apply to this buffer. */
    private List pinAttributes = new ArrayList(32);

    /** The clock domain with which this buffer is associated. */
    private ClockDomain domain;
    
    /**
     * Creates a new <code>Buffer</code> with the given characteristics.
     *
     * @param name a <code>String</code> representing the buffer name
     * @param size an <code>int</code> representing the bit width of
     * this buffer
     * @param resetValue a <code>long</code> representing the initial value
     * @param driveOnReset a <code>boolean</code> representing if the
     * <code>Buffer</code> should actively drive its initial value
     * during and subsequent to reset.  a <code>false</code> value
     * indicates that the Buffer will be three-stated upon reset and
     * will not drive a value until a <code>drive</code> method is
     * called.
     */
    protected Buffer(String name, int size, long resetValue,
        boolean driveOnReset, boolean resetValueSupplied)
    {
        // perform some error checking
        if((size <= 0) || (size > 64))
        {
            throw new ForgeApiException("Pin: " + name + " with size: " +
                size + " is out of range (1-64)");
        }

        this.name = name;
        this.size = size;
        
        this.resetValue = mapValueToSize(resetValue);
        
        this.resetEnabled = driveOnReset;
        this.resetValueSupplied = resetValueSupplied;


        // if size is 64, any reset value can be contained
        if(size < 64)
        {
            if(((resetValue >= 0) && ((resetValue >> size) != 0)) ||
                ((resetValue < 0) && ((resetValue >> size) != -1)))
            {
                throw new ForgeApiException("Pin: " + name + " with size: " +
                    size + " can't represent reset value: 0x" +
                    Long.toHexString(resetValue));
            }
        }
    }
    
    /**
     * Creates a new <code>Buffer</code> with the given
     * characteristics that is to be used in interacting with the
     * given <code>IPCore</code> object.  This Buffer will not create
     * a port at the top level of the design, but will rather be used
     * to supply values to and/or retrieve values from the
     * instantiated IP Core.  
     *
     * @param coreObj an <code>IPCore</code> object to which the pin
     * will belong.
     * @param name a <code>String</code> representing the name of the
     * port on the IP Core that will be controlled via this Buffer.
     * @param size an <code>int</code> representing the bit width
     * @param resetValue a <code>long</code> representing the initial value
     * @param driveOnReset a <code>boolean</code> representing if the
     * <code>Buffer</code> should actively drive during reset.
     */
    protected Buffer(IPCore coreObj, String name, int size,
        long resetValue, boolean driveOnReset, boolean resetValueSupplied)
    {
        this(name, size, resetValue, driveOnReset, resetValueSupplied);
        Core.addToPinIPCoreMap(this,coreObj);
    }    
    
    /**
     * Creates a new <code>Buffer</code> instance that drives a reset value on reset.
     *
     * @param name a <code>String</code> representing our name.
     * @param size an <code>int</code> representing the bit size.
     * @param resetValue a <code>long</code> representing the reset value.
     */
    protected Buffer(String name, int size, long resetValue)
    {
        this(name,size,resetValue,true,true);
    }

    /**
     * Creates a new <code>Buffer</code> for interacting with the IP
     * Core that has a specified reset value and will be active during
     * and subsequent to reset.
     *
     * @param coreObj an <code>IPCore</code> object
     * @param name a <code>String</code> representing the Buffer name.
     * @param size an <code>int</code> representing the bit size.
     * @param resetValue a <code>long</code> representing the reset value.
     */
    protected Buffer(IPCore coreObj, String name, int size, long resetValue)
    {
        this(coreObj,name,size,resetValue,true,true);
    }

    /**
     * Creates a new <code>Buffer</code> with an implied resetValue of 0.
     * that is not driven on reset.
     *
     * @param name a <code>String</code> representing the Buffer name.
     * @param size an <code>int</code> representing the bit size.
     */
    protected Buffer(String name, int size)
    {
        this(name,size,0,false,false);
    }
    
    /**
     * Creates a new <code>Buffer</code> with an implied resetValue of 0.
     * that isn't driven on reset for a IP Core object.
     *
     * @param coreObj a <code>IPCore</code> object
     * @param name a <code>String</code> representing the Buffer name.
     * @param size an <code>int</code> representing the bit size.
     */
    protected Buffer(IPCore coreObj, String name, int size)
    {
        this(coreObj,name,size,0,false,false);
    }

    /**
     * Creates a new <code>Buffer</code> with an implied size of 32
     * and resetValue of 0 that is not driven on reset.
     *
     * @param name a <code>String</code> representing the Buffer name.
     */
    protected Buffer(String name)
    {
        this(name,32,0,false,false);
    }
    
    /**
     * Creates a new <code>Buffer</code> with an implied size of 32
     * and resetValue of 0 that is not driven on reset that is used to
     * interact with an IP Core.
     *
     * @param coreObj a <code>IPCore</code> object
     * @param name a <code>String</code> representing the Buffer name.
     */
    protected Buffer(IPCore coreObj, String name)
    {
        this(coreObj,name,32,0,false,false);
    }
    
    protected Buffer()
    {
        name = null;
        size = 0;
        resetValue = 0L;
        resetEnabled = false;
        resetValueSupplied = false;
    }

    /**
     * Adds the specified {@link UCFAttribute} to this Buffer.
     *
     * @param ucf the {@link UCFAttribute} to add
     */
    public void addUCFAttribute(UCFAttribute ucf)
    {
        pinAttributes.add(ucf);
    }

    /**
     * Gets the List of {@link UCFAttribute UCFAttributes} for this
     * Buffer.
     */
    public List getUCFAttributes()
    {
        return pinAttributes;
    }

    /**
     * Returns the bit width of this <code>Buffer</code>.
     *
     * @return an <code>int</code> value, always greater than 0.
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Returns whether this Buffer uses signed values.
     * 
     * @return true for signed values, false for unsigned
     */
    public boolean isSigned() 
    {
        return isSigned;
    }
    
    /**
     * Sets whether this Buffer uses signed values.
     * 
     * @param isSigned true for signed, false for unsigned
     */
    public void setSigned(boolean isSigned)
    {
        this.isSigned = isSigned;
    }
    
    /**
     * <code>getInputPipelineDepth</code> returns the current input
     * pipeline depth setting.
     *
     * @return a non-negative <code>int</code> value
     */
    public int getInputPipelineDepth()
    {
        return inputPipelineDepth;
    }
    
    /**
     * Returns a <code>String</code> representation of this
     * <code>Buffer</code>.
     *
     * @return a <code>String</code> value
     */
    public String toString()
    {
        return getFullName();
    }

    /**
     * Returns the name of this <code>Buffer</code>.
     *
     * @return a <code>String</code> value
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of this <code>Buffer</code>.
     *
     * @param name a <code>String</code> value
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets the bit width of this <code>Buffer</code>
     *
     * @param size a value of type 'int'
     */
    public void setSize(int size)
    {
        // perform some error checking
        if((size <= 0) || (size > 64))
        {
            throw new ForgeApiException("Pin: " + name + " has new size: " + size + " out of range (1-64)");
        }
        this.size=size;
    }

    /**
     * Returns the name of this <code>Buffer</code> with vector size
     * notation appended.
     *
     * @return a <code>String</code> value
     */
    protected String getFullName()
    {
        if (size == 1)
        {
            return name;
        }
        else
        {
            return name + "[" + (size - 1) + ":0]";
        }
    }
    
    
    /**
     * Returns the defined Reset Value of this <code>Buffer</code>.
     *
     * @return an <code>int</code> value
     */
    public long getResetValue()
    {
        return resetValue;
    }


    /**
     * Identifies the reset behavior of this Buffer.  The Buffer will
     * behave in one of two ways depending on how it is constructed.
     * <ul>
     * <li>If this method returns true, the Buffer will come out of
     * Reset in the active state.  The output buffer will not be
     * tri-stated and the specified reset value will be driven on the
     * port until changed via a call to a <code>set</code> method, or
     * until this Buffer is tri-stated via a call to a
     * <code>release</code> method on this Buffer.
     * <li>If this method returns false, the Buffer will come out of
     * Reset in the high impedence (tri-state) state until the first
     * call to a <code>drive</code> method on this Buffer.
     * </ul>
     *
     * @return true if this Buffer is to drive its value during and
     * subsequent to Reset, or false if the Buffer is tri-stated.
     */
    public boolean getDriveOnReset()
    {
        return resetEnabled;
    }


    /**
     * Returns if this <code>Buffer</code> had a reset value supplied
     * by the user during construction.
     *
     * @return a <code>boolean</code> value
     */
    public boolean getResetValueSupplied()
    {
        return resetValueSupplied;
    }

    /**
     * Sets the {@link ClockDomain} with which this is associated.
     *
     * @param domain the associated clock domain
     */
    public void setDomain(ClockDomain domain)
    {
        this.domain = domain;
    }

    /**
     * Gets the {@link ClockDomain} with which this is associated.
     *
     * @return the associated clock domain
     */
    public ClockDomain getDomain()
    {
        return this.domain;
    }
    
    /**
     * Maps the supplied value to a sign extended version that fits
     * into the bit width of this <code>Buffer</code>.
     *
     * @param dat an <code>int</code> value
     * @return an <code>int</code> value
     */
    protected final long mapValueToSize (long dat)
    {
        int shift = 64 - getSize();

        // sign extend to the sub long bit size
        return (dat << shift) >> shift;
    }

    
    /**
     * Maps the value contained on the input port to a sign extended
     * version that fits into the bit width of this
     * <code>Buffer</code>
     *
     * @return a <code>long</code> value
     */
    protected final long mapValueToSize ()
    {
        return mapValueToSize(readValue());
    }

    /**
     * Returns a long value with one bit set for each bit in this
     * Buffer, LSB aligned.
     *
     * @return a <code>long</code> value.
     */
    protected long getMask()
    {
        int lsize = getSize();
        if(lsize == 64)
            return -1L;
        else
            return ~(-(1L << lsize));
    }

    
    // Primitive method calls used by sub-classes to interact with the
    // Buffer.

    protected void writeValueNow(long value) {}
    
    protected void writeValueNext(long value) {}
    
    
    protected long readValue() { return 0; }
    
    
    protected void changeDriveNow(boolean enable) {}
    
    protected void changeDriveNext(boolean enable){}
    

    protected void writeFloatValueNow (float value) {}
    
    protected void writeFloatValueNext (float value) {}
    
    protected float readFloatValue () { return 0; }
    
    
    protected void writeDoubleValueNow (double value) {}
    
    protected void writeDoubleValueNext (double value) {}
    
    protected double readDoubleValue () { return 0; }
}
