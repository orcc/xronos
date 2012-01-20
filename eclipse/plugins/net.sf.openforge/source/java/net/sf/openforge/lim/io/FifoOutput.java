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


import java.util.*;

import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.SimpleConstant;
/**
 * FifoOutput creates pins necessary for obtaining data from an output
 * FIFO interface.
 *
 *
 * <p>Created: Tue Dec 16 12:10:31 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoOutput.java 98 2006-02-02 20:08:45Z imiller $
 */
public abstract class FifoOutput extends FifoIF 
{
    private static final String _RCS_ = "$Rev: 98 $";

    /**
     * Constructs a new FifoOutput instance
     *
     * @param width an int, the byte width of the Fifo data path.
     */
    public FifoOutput (int width)
    {
        super(width);
    }

    /**
     * Returns a {@link FifoWrite} object that is used to send data to
     * this FifoIF.
     *
     * @return a {@link FifoAccess}, specifically of type {@link
     * FifoWrite}
     */
    public FifoAccess getAccess ()
    {
        return new FifoWrite(this);
    }

    /**
     * Returns false due to the fact that the data pin for this
     * interface is an output to the design.
     *
     * @return false
     */
    public boolean isInput ()
    {
        return false;
    }

    /** Returns the output data pin for this interface */
    public abstract SimplePin getDataPin ();
    /** Returns the output send pin, indicating that the interface is
     * outputting valid data*/
    public abstract SimplePin getSendPin ();
    /** Returns the input acknowledge pin, indicating that the queue
     * that the interface is sending to has acknowledged reciept of
     * the data */
    public abstract SimplePin getAckPin ();
    /** If available, returns the input pin indicating that the output
     * WILL immediately ack a send token. May be null. */
    public abstract SimplePin getReadyPin ();
    
    

    public String toString ()
    {
        return super.toString().replaceAll("net.sf.openforge.lim.io.","")+"{" + getDataPin().getName() + "}";
    }
    
}// FifoOutput
