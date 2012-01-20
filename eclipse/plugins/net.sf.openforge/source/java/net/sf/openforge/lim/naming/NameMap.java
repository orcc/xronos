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
package net.sf.openforge.lim.naming;

import java.util.*;

import net.sf.openforge.lim.op.*;

/**
 * Created: Tue Mar 19 01:24:24 2002
 *
 * @author YS Yu
 * @version $Id: NameMap.java 2 2005-06-09 20:00:48Z imiller $
 */

public class NameMap 
{
    /** RCS ID */
    private static final String _RCS_ = "$Rev: 2 $";
    
    /** 
     * This map is used to store association between net.sf.openforge.lim.op 
     * classes(the keys) and the name(the values) which is used to
     * set the base logical ID for each operation's result bus .
     */
    private static Map opClassToOpBusBaseNameMap = new HashMap();
    
    /*
     * Initializes HashMap opClassToOpBusBaseNameMap with each Operation class
     * and its expected name in string.
     */
    static
    {
        opClassToOpBusBaseNameMap.put(AddOp.class, "add");
        opClassToOpBusBaseNameMap.put(AndOp.class, "and");
        opClassToOpBusBaseNameMap.put(NumericPromotionOp.class, "numericPromote");
        opClassToOpBusBaseNameMap.put(CastOp.class, "cast");
        opClassToOpBusBaseNameMap.put(ComplementOp.class, "complement");
        opClassToOpBusBaseNameMap.put(ConditionalAndOp.class, "logicalAnd");
        opClassToOpBusBaseNameMap.put(ConditionalOrOp.class, "logicalOr");
        opClassToOpBusBaseNameMap.put(net.sf.openforge.lim.op.Constant.class, "const");
        opClassToOpBusBaseNameMap.put(DivideOp.class, "div");
        opClassToOpBusBaseNameMap.put(EqualsOp.class, "compareEQ");
        opClassToOpBusBaseNameMap.put(GreaterThanEqualToOp.class, "compareGTET");
        opClassToOpBusBaseNameMap.put(GreaterThanOp.class, "compareGT");
        opClassToOpBusBaseNameMap.put(LeftShiftOp.class, "lshift");
        opClassToOpBusBaseNameMap.put(LessThanEqualToOp.class, "compareLTET");
        opClassToOpBusBaseNameMap.put(LessThanOp.class, "compareLT");
        opClassToOpBusBaseNameMap.put(MinusOp.class, "minus");
        opClassToOpBusBaseNameMap.put(ModuloOp.class, "mod");
        opClassToOpBusBaseNameMap.put(MultiplyOp.class, "mult");
        opClassToOpBusBaseNameMap.put(NotEqualsOp.class, "compareNEQ");
        opClassToOpBusBaseNameMap.put(NotOp.class, "not");
        opClassToOpBusBaseNameMap.put(OrOp.class, "or");
        opClassToOpBusBaseNameMap.put(PlusOp.class, "plus");
        opClassToOpBusBaseNameMap.put(RightShiftOp.class, "rshift");
        opClassToOpBusBaseNameMap.put(RightShiftUnsignedOp.class, "urshift");
        opClassToOpBusBaseNameMap.put(ShortcutIfElseOp.class, "shortcutIfElse");
        opClassToOpBusBaseNameMap.put(SubtractOp.class, "sub");
        opClassToOpBusBaseNameMap.put(XorOp.class, "xor");
    }
    
    /**
     * Get the bus name in String for this Operation class
     *
     * @param opClass the operation that needs a name
     * @return name of the operation in String
     */
    public static String getOpClassBusBaseName (Class opClass)
    {
        return (String)opClassToOpBusBaseNameMap.get(opClass);
    }

    
}
