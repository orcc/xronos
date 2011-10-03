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

package net.sf.openforge.optimize.constant.rule;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.optimize.*;

/**
 * ShiftOpRule.java
 * <pre>
 * a >> 0 = a
 * 0 << a = 0
 *
 * a << 0 = a
 * 0 << a = 0
 *
 * a >>> 0 = a
 * 0 >>> a = 0
 * </pre>
 * <p>Created: Thu Jul 18 09:24:39 2002
 *
 * @author imiller
 * @version $Id: ShiftOpRule.java 131 2006-04-07 15:18:04Z imiller $
 */
public class ShiftOpRule 
{
    private static final String _RCS_ = "$Rev: 131 $";

    public static boolean halfConstant(BinaryOp op, Number[] consts, ComponentSwapVisitor visit)
    {
        assert consts.length == 2 : "Expecting exactly 2 port constants for Shift Op";
        Number p1 = consts[0];
        Number p2 = consts[1];

        if ((p1 == null && p2 == null) ||
            (p1 != null && p2 != null))
        {
            return false;
        }

        Number constant = p1 == null ? p2 : p1;
        Port nonConstantPort = p1 == null ? (Port)op.getDataPorts().get(0) : (Port)op.getDataPorts().get(1);
        Port constantPort = p1 != null ? (Port)op.getDataPorts().get(0) : (Port)op.getDataPorts().get(1);

        if(p2 != null && p2.longValue() == 0)
        {
            if (_optimize.db) _optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op + " due to (a << 0)");
            // a << 0 = a.  Simply delete the component and wire
            // through the non-constant port

            // wire through the control.
            ComponentSwapVisitor.wireControlThrough(op);

            // Wire the non constant port through.
            ComponentSwapVisitor.shortCircuit(nonConstantPort, op.getResultBus());

            // Delete the op.
            //op.getOwner().removeComponent(op);
            boolean removed = ComponentSwapVisitor.removeComp(op);

            assert removed : "ShiftOp was not able to be removed!";
            
            return true;
        }
        else if(p1 != null && p1.longValue() == 0)
        {
            if (_optimize.db) _optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op + " due to (0 << a)");
            // 0 << a = 0, 0 >> a, 0 >>> a,  Simply delete the component and replace with 0
            visit.replaceComponent(op, new SimpleConstant(0, op.getResultBus().getValue().getSize(), op.getResultBus().getValue().isSigned()));
            
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Replaces a left shift with a constant 0 if the bitwise AND of the shift magnitude 
     * and the result width minus one is 0.  Technically, the behavior of C in the case
     * that the magnitude is greater than or equal to the result width is undefined.  However,
     * here we are mirroring the behavior of gcc.
     *
     * @param lso the LeftShiftOp to be tested/removed.
     * @param consts the constant values of the data ports 'Number[]'
     * @param visit the {@link ComponentSwapVisitor} to use.
     * @return true if the op was replaced by a constant.
     */
    public static boolean leftHalfConstant (LeftShiftOp lso, Number[] consts, ComponentSwapVisitor visit)
    {
        /* GCC always masks off the MSB bits of the shift magnitude
         * and performs the modulo shift.  Thus there is never the '0'
         * case unless the value to be shifted is a constant 0.
        assert consts.length == 2;
        final Number magnitude = consts[1];
        final int resWidth = lso.getResultBus().getValue().getSize();

        //int widthMask = resWidth - 1;
        //if (magnitude != null && ((magnitude.longValue() & widthMask) == 0))
        //if (magnitude != null && (magnitude.longValue() >= resWidth))
        if (magnitude != null && (magnitude.longValue() == resWidth))
        {
            visit.replaceComponent(lso, new SimpleConstant(0, lso.getResultBus().getValue().getSize(), lso.getResultBus().getValue().isSigned()));
            return true;
        }
        */
        assert consts.length == 2;
        final Number value = consts[0];
        if (value != null && (value.longValue() == 0))
        {
            visit.replaceComponent(lso, new SimpleConstant(0, lso.getResultBus().getValue().getSize(), lso.getResultBus().getValue().isSigned()));
            return true;
        }
        return false;
    }
    
    /**
     * Replaces a right shift with a constant 0 if the shift magnitude
     * is greater than or equal to the width of the value input AND
     * the msb of the value is a 0, otherwise the left bit has to be
     * sign extended to the size of the op (natural size) and then
     * shifted.  This isn't the place to handle that!
     *
     * @param rsuo the RightShiftUnsignedOp to be tested/removed.
     * @param consts the constant values of the data ports 'Number[]'
     * @param visit the {@link ComponentSwapVisitor} to use.
     * @return true if the op was replaced by a constant.
     */
    public static boolean unsignedRightHalfConstant (RightShiftUnsignedOp rsuo, Number[] consts, ComponentSwapVisitor visit)
    {
        assert consts.length == 2;
        Number magnitude = consts[1];
        Value leftValue = rsuo.getLeftDataPort().getValue();
        int valueWidth = leftValue.getSize();
        boolean msbIsZero = leftValue.getBit(valueWidth-1) == Bit.ZERO;

        if (magnitude != null && magnitude.longValue() >= valueWidth && msbIsZero)
        {
            visit.replaceComponent(rsuo, new SimpleConstant(0, rsuo.getResultBus().getValue().getSize(), rsuo.getResultBus().getValue().isSigned()));
            return true;
        }
        
        return false;
    }

    /**
     * Replaces a right shift with a constant 0 if the shift magnitude
     * is greater than or equal to the width of the value input AND
     * the msb of the value is a 0.
     *
     * @param rso the RightShiftOp to be tested/removed.
     * @param consts the constant values of the data ports 'Number[]'
     * @param visit the {@link ComponentSwapVisitor} to use.
     * @return true if the op was replaced by a constant.
     */
    public static boolean rightHalfConstant (RightShiftOp rso, Number[] consts, ComponentSwapVisitor visit)
    {
        assert consts.length == 2;
        Number magnitude = consts[1];
        Value leftValue = rso.getLeftDataPort().getValue();
        int valueWidth = leftValue.getSize();
        boolean msbIsZero = leftValue.getBit(valueWidth-1) == Bit.ZERO;

        if (magnitude != null && magnitude.longValue() >= valueWidth && msbIsZero)
        {
            visit.replaceComponent(rso, new SimpleConstant(0, rso.getResultBus().getValue().getSize(), rso.getResultBus().getValue().isSigned()));
            return true;
        }
        
        return false;
    }
    
}// ShiftOpRule
