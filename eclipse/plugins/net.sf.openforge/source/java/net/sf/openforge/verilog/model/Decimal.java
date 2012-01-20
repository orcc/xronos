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

/**
 * Decimal represents a decimal value.
 *
 * <P>
 *
 * Created: Wed Feb 28 2001
 *
 * @author abk
 * @version $Id: Decimal.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Decimal extends BaseNumber
{

    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    public Decimal(Number n)
    {
        this(new Constant(n));
    }
    
    public Decimal(long l, int size)
    {
        this(new Constant(l, size));
    }

    public Decimal(Constant n) 
    {
        super(n);
    } // Decimal()
    
} // end of class Decimal
