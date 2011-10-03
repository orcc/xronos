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

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.optimize.*;

/**
 * SubtractOpRule.java
 * <pre>
 * a - 0 = a
 * 0 - a = -a
 * a - 0x80000000 = a ^ 0x80000000
 * </pre>
 * <p>Created: Thu Jul 18 09:24:39 2002
 *
 * @author imiller
 * @version $Id: SubtractOpRule.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SubtractOpRule 
{
    private static final String _RCS_ = "$Rev: 2 $";

    public static boolean halfConstant(SubtractOp op, Number[] consts)
    {
        assert consts.length == 2 : "Expecting exactly 2 port constants for Subtract Op";
        Number p1 = consts[0];
        Number p2 = consts[1];

        if ((p1 == null && p2 == null) || (p1 != null && p2 != null))
        {
            return false;
        }

        Number constant = p1 == null ? p2 : p1;
        Port nonConstantPort = p1 == null ? (Port)op.getDataPorts().get(0) : (Port)op.getDataPorts().get(1);

        if (p2 != null && p2.longValue() == 0)
        {
            if (_optimize.db) _optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op + " due to (a - 0)");
            // a - 0 = a.  Simply delete the component and wire
            // through the non-constant port

            // wire through the control.
            ComponentSwapVisitor.wireControlThrough(op);

            // Wire the non constant port through.
            ComponentSwapVisitor.shortCircuit(nonConstantPort, op.getResultBus());

            // Delete the op.
            //op.getOwner().removeComponent(op);
            boolean removed = ComponentSwapVisitor.removeComp(op);

            assert removed : "SubtractOp was not able to be removed!";
            
            return true;
        }
        else if(p1 != null && p1.longValue() == 0)
        {
            if (_optimize.db) _optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op + " due to (0 - a)");
            // 0 - a = -a. Simply replace the component with a MinusOp on the non-constant port
            MinusOp mop = new MinusOp();
            Module owner = op.getOwner();
            assert owner != null : "Cannot replace a component which is not contained in a module";
            
            // map the dependencies/connections
            Map portCorrelation = new HashMap();
            portCorrelation.put(op.getClockPort(), mop.getClockPort());
            portCorrelation.put(op.getResetPort(), mop.getResetPort());
            portCorrelation.put(op.getGoPort(), mop.getGoPort());
            portCorrelation.put(op.getDataPorts().get(1), mop.getDataPorts().get(0));
            
            assert op.getExits().size() == 1 : "Only expecting one exit on node to be replaced";
            Exit exit = op.getOnlyExit();
            assert exit.getDataBuses().size() == 1 : "Only expecting one data bus on component to be replaced";

            Map busCorrelation = new HashMap();
            busCorrelation.put(exit.getDataBuses().get(0), mop.getResultBus());
            assert mop.getExits().size() == 1 : "Only expecting one exit on MinusOp";
            busCorrelation.put(exit.getDoneBus(), mop.getOnlyExit().getDoneBus());

            assert mop.getExits().size() == 1 : "Only expecting one exit on node to be replaced";
            Map exitCorrelation = new HashMap();
            exitCorrelation.put(exit, mop.getOnlyExit());
            
            ComponentSwapVisitor.replaceConnections(portCorrelation, busCorrelation, exitCorrelation);
            
            /** set the unary Minus port and bus size with this SubtractOp port0 size info **/
//             assert false : "New constant prop: fix these lines. --SGE";
//              Value argValue = ((Port)op.getDataPorts().get(1)).getValue();
//              ((Port)mop.getDataPorts().get(0)).setValue(argValue);
//              mop.getResultBus().setBits(argValue.size());
            

            if(owner instanceof Block)
            {
                ((Block)owner).replaceComponent(op, mop);
            }
            else
            {
                owner.removeComponent(op);
                owner.addComponent(mop);
            }
            op.disconnect();
            mop.propagateValuesForward();

            return true;
        }
        // following doesn't work correctly when subtract is a long subtract
//         else if(p2 != null && p2.longValue() == 0x80000000L)
//         {   
//             if (_optimize.db) _optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op + " due to (a - 0x80000000)");
//             // a - 0x80000000 = a ^ 0x80000000. Simply replace the component with a bitwise xor
//             XorOp xor = new XorOp();
//             Module owner = op.getOwner();
//             assert owner != null : "Cannot replace a component which is not contained in a module";

//             // map the dependencies/connections
//             Map portCorrelation = new HashMap();
//             portCorrelation.put(op.getClockPort(), xor.getClockPort());
//             portCorrelation.put(op.getResetPort(), xor.getResetPort());
//             portCorrelation.put(op.getGoPort(), xor.getGoPort());
//             portCorrelation.put(op.getDataPorts().get(0), xor.getDataPorts().get(0));
//             portCorrelation.put(op.getDataPorts().get(1), xor.getDataPorts().get(1));
            
//             assert op.getExits().size() == 1 : "Only expecting one exit on node to be replaced";
//             Exit exit = (Exit)op.getExits().iterator().next();
//             assert exit.getDataBuses().size() == 1 : "Only expecting one data bus on component to be replaced";

//             Map busCorrelation = new HashMap();
//             busCorrelation.put(exit.getDataBuses().get(0), xor.getResultBus());
//             assert xor.getExits().size() == 1 : "Only expecting one exit on MinusOp";
//             busCorrelation.put(exit.getDoneBus(), ((Exit)xor.getExits().iterator().next()).getDoneBus());

//             assert xor.getExits().size() == 1 : "Only expecting one exit on node to be replaced";
//             Map exitCorrelation = new HashMap();
//             exitCorrelation.put(exit, xor.getExits().get(0));
            
//             ComponentSwapVisitor.replaceConnections(portCorrelation, busCorrelation, exitCorrelation);

//             /** set the  data ports and bus size with this SubtractOp data ports and bus size info **/
//             Value arg0Value = ((Port)op.getDataPorts().get(0)).getValue();
//             Value arg1Value = ((Port)op.getDataPorts().get(1)).getValue();
//             ((Port)xor.getDataPorts().get(0)).setValue(arg0Value);
//             ((Port)xor.getDataPorts().get(1)).setValue(arg1Value);
//             Value resultValue = op.getResultBus().getValue();
//             xor.getResultBus().setBits(resultValue.size());

//             if(owner instanceof Block)
//             {
//                 ((Block)owner).replaceComponent(op, xor);
//             }
//             else
//             {
//                 owner.removeComponent(op);
//                 owner.addComponent(xor);
//             }
//             op.disconnect();

//             return true;
//         }
        else
        {
            return false;
        }
    }
}// SubtractOpRule
