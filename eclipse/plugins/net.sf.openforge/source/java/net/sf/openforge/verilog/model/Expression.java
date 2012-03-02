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
package net.sf.openforge.verilog.model;

import java.util.Collection;

/**
 * An Expression is a verilog element which represents a
 * value of specific width composed exclusively from
 * Identifiers and Symbols.  
 * <P>
 *
 * Created: Fri Feb 09 2001
 *
 * @author abk
 * @version $Id: Expression.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface Expression extends VerilogElement
{
    //    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    /**
     * Gets the total number of bits resulting from this expression.
     */
    public int getWidth();
    
    /**
     * Gets all Nets which participate in this Expression.
     */
    public Collection<Object> getNets();
    
} // end of interface Expression
