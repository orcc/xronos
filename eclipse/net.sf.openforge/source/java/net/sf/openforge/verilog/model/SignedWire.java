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
 * SignedWire is a Verilog (2001) wire with type 'signed', thus:
 * <code>wire signed xxxxx</code>
 *
 *
 * <p>Created: Fri Aug  6 09:40:25 2004
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SignedWire.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SignedWire extends Net
{
    private static final String _RCS_ = "$Rev: 2 $";

    public SignedWire(Identifier id, int width) 
    {
        super(Keyword.SIGNED_WIRE, id, width);
    } // SignedWire(Identifier, width)
    
    public SignedWire(String id, int width)
    {
        this(new Identifier(id), width);
    } // SignedWire(String, width)

    public SignedWire(Identifier id, int msb, int lsb) 
    {
        super(Keyword.SIGNED_WIRE, id, msb, lsb);
    } // SignedWire(Identifier, msb, lsb)
    
    public SignedWire(String id, int msb, int lsb)
    {
        this(new Identifier(id), msb, lsb);
    } // SignedWire(String, msb, lsb)

}// SignedWire
