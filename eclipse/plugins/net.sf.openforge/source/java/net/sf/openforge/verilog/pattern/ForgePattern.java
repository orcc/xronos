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

import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Statement;


/**
 * A Forge pattern is a verilog Statement which distinguishes
 * between consumed and produced {@link Net Nets}. Consumed Nets
 * typically provide "input" signals to the statement, and should
 * be declared by another block which provides their value. Produced
 * Nets are the "output" signals which are the result of the statement. 
 */
public interface ForgePattern extends Statement
{
    static final String _RCS_ = "$Rev: 2 $";
    /**
     * Provides the collection of Nets which this statement of verilog
     * uses as input signals.
     */
    public Collection getConsumedNets();
    
    /**
     * Provides the collection of Nets which this statement of verilog
     * produces as output signals. These are any signals which need to
     * be declared, even if the statement itself also consumes them.
     */
    public Collection getProducedNets();
    
} // interface ForgePattern
