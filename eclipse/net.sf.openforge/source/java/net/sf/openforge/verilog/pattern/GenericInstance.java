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

import java.util.*;

import net.sf.openforge.verilog.model.*;

/**
 * GenericInstance.java
 *
 *
 * <p>Created: Thu Feb 20 13:08:56 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: GenericInstance.java 2 2005-06-09 20:00:48Z imiller $
 */
public class GenericInstance extends ModuleInstance implements ForgePattern
{
    private static final String _RCS_ = "$Rev: 2 $";

    private HashSet produced = new HashSet();
    private HashSet consumed = new HashSet();
    
    public GenericInstance (Module module, String id)
    {
        super(module, id);
    }

    public void connect (Net port, Expression e)
    {
        super.connect(port, e);
        if (port instanceof Input && e instanceof Net)
            this.consumed.add(e);
        if (port instanceof Output && e instanceof Net)
            this.produced.add(e);
    }

    /**
     * Provides the collection of Nets which this statement of verilog
     * uses as input signals.
     */
    public Collection getConsumedNets()
    {
        return Collections.unmodifiableSet(this.consumed);
    }
    
    public Collection getProducedNets ()
    {
        return Collections.unmodifiableSet(this.produced);
    }
    
}// GenericInstance
