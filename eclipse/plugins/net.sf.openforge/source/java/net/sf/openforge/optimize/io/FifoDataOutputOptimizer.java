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

package net.sf.openforge.optimize.io;


import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;

/**
 * FifoDataOutputOptimizer is a class which sets the status of
 * consumesGo on {@link SimplePinWrite} objects based on the number of
 * accesses to that pin.  In specific, a data pin of a FifoOutput
 * needs to be qualified with the GO only when the target pin has
 * multiple writers.  A fifo output data pin with only one writer can
 * allow the data value to 'float' as there will not be a wired-or
 * structure between the writer and the pin.  
 *
 * <p>Created: Fri Mar 02 10:37:09 2007
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoDataOutputOptimizer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class FifoDataOutputOptimizer extends DefaultVisitor
{
    private static final String _RCS_ = "$Rev:$";

    
    public FifoDataOutputOptimizer () {}

    /**
     * Builds a correlation of SimplePin to all accesses to that
     * SimplePin and then marks any data pin of output fifo interfaces
     * as not needing the GO iff that pin has fewer than 2 accesses.
     */
    public void visit (Design design)
    {
        final PinAccessCorrelator correlator = new PinAccessCorrelator();
        design.accept(correlator);
        
        for (FifoIF fifoIF : design.getFifoInterfaces())
        {
            if (fifoIF.isInput())
            {
                continue;
            }
            

            final FifoOutput output = (FifoOutput)fifoIF;
            final SimplePin dataPin = output.getDataPin();
            final Set<Referencer> refs = correlator.getRefs(dataPin);
            if (refs.size() < 2)
            {
                for (Referencer ref : refs)
                {
                    assert ref instanceof SimplePinWrite : "Unknown access type to fifo output data pin";
                    ((SimplePinWrite)ref).setGoNeeded(false);
                }
            }
        }
    }

    /**
     * A simple visitor class to correlate SimplePin and SimplePin
     * Read/Write/Access nodes.
     */
    private static class PinAccessCorrelator extends DefaultVisitor
    {
        Map<SimplePin, Set<Referencer>> correlation = new HashMap();

        public Set<Referencer> getRefs (SimplePin pin)
        {
            Set<Referencer>refs = this.correlation.get(pin);

            // Ensure that we never return null
            if (refs == null)
            {
                return (Set<Referencer>)Collections.EMPTY_SET;
            }
            
            return refs;
        }
        
        public void visit (SimplePinWrite acc) { putAccess(acc, acc.getReferenceable()); }
        public void visit (SimplePinRead acc) { putAccess(acc, acc.getReferenceable()); }
        public void visit (SimplePinAccess acc) { putAccess(acc, acc.getReferenceable()); }

        private void putAccess (Referencer ref, Referenceable refable)
        {
            assert refable instanceof SimplePin : "Only expecting simple pins in correlation of pin accesses";
            SimplePin pin = (SimplePin)refable;
            Set<Referencer> refs = this.correlation.get(pin);
            if (refs == null)
            {
                refs = new HashSet();
                this.correlation.put(pin, refs);
            }
            refs.add(ref);
        }
        
    }
    
    
}// FifoDataOutputOptimizer
