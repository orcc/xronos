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
 * Wire is a Net which of type "wire".
 *
 * <P>
 *
 * Created: Fri Feb 09 2001
 *
 * @author abk
 * @version $Id: Wire.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Wire extends Net
{
    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    public Wire(Identifier id, int width) 
    {
        super(Keyword.WIRE, id, width);
    } // Wire(Identifier, width)
    
    public Wire(String id, int width)
    {
        this(new Identifier(id), width);
    } // Wire(String, width)

    public Wire(Identifier id, int msb, int lsb) 
    {
        super(Keyword.WIRE, id, msb, lsb);
    } // Wire(Identifier, msb, lsb)
    
    public Wire(String id, int msb, int lsb)
    {
        this(new Identifier(id), msb, lsb);
    } // Wire(String, msb, lsb)

} // end of class Wire
