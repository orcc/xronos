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
package net.sf.openforge.verilog.mapping;

/**
 * Immutable class for naming primitives we no about
 *
 * @author "C. Schanck" <cschanck@cschanck>
 * @version 1.0
 * @since 1.0
 */
public class MappingName
{
    private static final String _RCS_ = "$Rev: 2 $";

    // synchronous 1 bit flop variants (posedge)
    public static MappingName FLOP_SYNC=new MappingName("FlopSync");
    public static MappingName FLOP_SYNC_ENABLE=new MappingName("FlopSyncEnable");
    public static MappingName FLOP_SYNC_RESET=new MappingName("FlopSyncReset");
    public static MappingName FLOP_SYNC_SET=new MappingName("FlopSyncSet");
    public static MappingName FLOP_SYNC_ENABLE_RESET=new MappingName("FlopSyncEnableReset");
    public static MappingName FLOP_SYNC_ENABLE_SET=new MappingName("FlopSyncEnableSet");
    public static MappingName FLOP_SYNC_SET_RESET=new MappingName("FlopSyncSetReset");
    public static MappingName FLOP_SYNC_ENABLE_SET_RESET=new MappingName("FlopSyncEnableSetReset");
    public static MappingName SHIFT_REGISTER_LUT=new MappingName("SRL16");
    public static MappingName SHIFT_REGISTER_LUT_ENABLE=new MappingName("SRL16E");
    public static MappingName SHIFT_REGISTER_LUT_NEG_EDGE=new MappingName("SRL16_1");

    private String name;
    
    public MappingName(String name)
    {
        this.name=name;
    }

    public String getName() { return name; }
    
    public String toString() { return name; }
}
