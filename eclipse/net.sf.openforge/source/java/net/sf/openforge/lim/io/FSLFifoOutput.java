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
 * FSLFifoOutput creates pins necessary for obtaining data from an
 * output FIFO interface that is FSL compliant.
 *
 *
 * <p>Created: Tue Dec 16 12:10:31 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FSLFifoOutput.java 128 2006-04-04 15:05:31Z imiller $
 */
public class FSLFifoOutput extends FifoOutput
{
    private static final String _RCS_ = "$Rev: 88 $";

    private String baseName;
    private SimplePin data;
    private SimplePin full;
    private SimplePin write;
    private SimplePin ctrl;
    private SimplePin clk;
    
    /**
     * Constructs a new FSLFifoOutput instance, creating all the
     * necessary pin objects and assigning them names of the form
     * <code><i>idString</i>_M_DOUT</code>, etc.
     *
     * @param idString a uniquifying String that is pre-pended to the
     * signal names which must conform to the specification for signal
     * naming (ie FSLx where x is the instance number)
     * @param width an int, the byte width of the Fifo data path.
     */
    public FSLFifoOutput (String idString, int width)
    {
        super(width);

        // This class creates all of the FSL master pins according to
        // the specified interface.  The idString will be 
        // of the form FSLx.  To this idString we must append the
        // _M_yyyyy signal names to generate the following pins:
        /*
         * FSLx_M_DATA <width> bits wide output
         * FSLx_M_FULL 1 bit input
         * FSLx_M_WRITE 1 bit output
         * FSLx_M_CONTROL 1 bit output, 
         * FSLx_M_CLK 1 bit input
         */

        this.baseName = idString;
        final String pinBaseName = buildPortBaseName(idString);
        //this.data = new SimpleFifoPin(this, width * 8, pinBaseName + "_DATA");
        this.data = new SimpleFifoPin(this, width, pinBaseName + "_DATA");
        this.full = new SimpleFifoPin(this, 1, pinBaseName + "_FULL");
        this.write = new SimpleFifoPin(this, 1, pinBaseName + "_WRITE");
        this.ctrl = new SimpleFifoPin(this, 1, pinBaseName + "_CONTROL");
        this.clk = new SimpleFifoPin(this, 1, pinBaseName + "_CLK");

        // The order that these are added here determines the order
        // they show up in the translated inteface.
        this.addPin(data);
        this.addPin(full);
        this.addPin(write);
        this.addPin(ctrl);
        this.addPin(clk);
        
        // Tie off the unused outputs.
        final Constant ctrl0 = new SimpleConstant(0, this.ctrl.getWidth());
        ctrl0.pushValuesForward(); // ensures the bus has a value.
        this.ctrl.connectPort(ctrl0.getValueBus());
    }

    /**
     * <code>getType</code> returns {@link FifoIF#TYPE_FSL_FIFO}
     *
     * @return an <code>int</code> value
     */
    public int getType ()
    {
        return FifoIF.TYPE_FSL_FIFO;
    }
    
    public String getPortBaseName ()
    {
        return this.baseName;
    }
    
    /**
     * Fifo output ports are master queues, this method returns
     * portname_M
     */
    protected String buildPortBaseName (String portName)
    {
        return portName + "_M";
    }
    
    /**
     * Returns a subset of {@link #getPins} that are the output pins
     * of the interface, containing only the data, write, and ctrl pins.
     */
    public Collection<SimplePin> getOutputPins ()
    {
        ArrayList list = new ArrayList();
        list.add(this.data);
        list.add(this.write);
        list.add(this.ctrl);
        
        return Collections.unmodifiableList(list);
    }

    public SimplePin getDataPin () { return this.data; }
    public SimplePin getAckPin () { return this.full; }
    public SimplePin getSendPin () { return this.write; }
    public SimplePin getReadyPin () { return null; }

}// FSLFifoOutput
