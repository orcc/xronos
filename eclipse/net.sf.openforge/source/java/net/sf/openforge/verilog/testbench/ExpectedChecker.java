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

package net.sf.openforge.verilog.testbench;

import net.sf.openforge.verilog.model.*;
import net.sf.openforge.verilog.pattern.CommaDelimitedStatement;


/**
 * ExpectedChecker contains the logic necessary to validate a received
 * result against the value contained in the results memory.
 *
 * <p>Created: Wed Jan  8 15:43:11 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ExpectedChecker.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ExpectedChecker 
{
    private static final String _RCS_ = "$Rev: 2 $";

    // The TaskHandle which is to be validated with this checker.
    private TaskHandle taskHandle;
    // A signal indicating that this task has failed a validation.
    private Register fail;
    
    public ExpectedChecker (TaskHandle handle)
    {
        this.taskHandle = handle;
        this.fail = new Register(this.taskHandle.getBaseName()+"_fail", 1);
    }

    /**
     * Initializes the fail register.
     */
    public void stateInits (InitialBlock ib)
    {
        ib.add(new Assign.NonBlocking(fail, new Constant(0,fail.getWidth())));
    }

    /**
     * States the checking logic and file write for failures.
     */
    public void stateLogic (Module module, StateMachine mach, Memories mems, SimFileHandle resultFile)
    {
        module.state(new InlineComment("Check expected result", Comment.SHORT));
        stateCheck(module, mach, mems, resultFile);
    }
    
    private void stateCheck (Module module, StateMachine mach,
        Memories mems, SimFileHandle resultFile)
    {
        SequentialBlock block = new SequentialBlock();
        Logical.And condition = new Logical.And(
            new Compare.CASE_NEQ(
                this.taskHandle.getExpectedResultWire(),
                this.taskHandle.getResultWire()), mach.getExpectedValidWire());

        CommaDelimitedStatement cds = new CommaDelimitedStatement();
        String fail = "FAIL: Incorrect result.  Iteration %d expected %x found %x\\n";
        cds.append(new StringStatement(fail));
        cds.append(mach.getResIndex());
        cds.append(taskHandle.getExpectedResultWire());
        cds.append(taskHandle.getResultWire());
        block.add(new FStatement.FWrite(resultFile.getHandle(), cds));
        block.add(new Assign.NonBlocking(this.fail, new Constant(1,1)));
        block.add(new DelayStatement(new FStatement.Finish(), 500));
        
        ConditionalStatement test = new ConditionalStatement(condition, block);
        
        ConditionalStatement cs = new ConditionalStatement(this.taskHandle.getDoneWire(), test);
        
        ProceduralTimingBlock ptb = new ProceduralTimingBlock(
            new EventControl(new EventExpression.PosEdge(mach.getClock())),
            cs);

        module.state(new Always(ptb));
    }

    /**
     * Returns the Register indicating a failure of this check
     */
    public Register getFailWire ()
    {
        return this.fail;
    }
    
}// ExpectedChecker
