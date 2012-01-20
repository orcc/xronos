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

package net.sf.openforge.forge.api.pin;

/**
 * DonePin is a specific type of ControlPin that is used as an output
 * to indicate that processing of data has completed.
 */
public class DonePin extends ControlPin
{
    /** The default done signal used whenever a done is needed but not
     * explicitly provided.
     */
    public final static DonePin GLOBAL = new DonePin("DONE");
    
    /**
     * Constructs a new active high DonePin.
     */
    public DonePin()
    {
        super("DONE");
    }

    /**
     * Constructs a new active high named DonePin.
     * 
     * @param name
     */
    public DonePin(String name)
    {
        super(name);
    }
}
