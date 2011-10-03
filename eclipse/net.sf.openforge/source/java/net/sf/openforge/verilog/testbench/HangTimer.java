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

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.verilog.model.*;


/**
 * HangTimer maintains the wires and logic necessary to instantiate a
 * hang timer in the testbench.  The hang timer reset's anytime
 * go/done activity is seen on any task being tested, and increments
 * every cycle otherwise.  When the pre-defined value is hit
 * (specified by {@link Project#getHangTimer}) the simulation
 * completes with the message Hang Timer Expired in the results file.
 *
 * <p>Created: Thu Jan  9 10:38:19 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: HangTimer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class HangTimer 
{
    private static final String _RCS_ = "$Rev: 2 $";

    private Register hangTimer;
    
    public HangTimer ()
    {
        this.hangTimer = new Register("hangTimer", 32);
    }

    /**
     * Returns the hang timer register.
     */
    private Register getHangTimer ()
    {
        return this.hangTimer;
    }
    
    /**
     * Adds an initialization statement for the hang timer register to
     * the InitialBlock
     */
    public void stateInits (InitialBlock ib)
    {
        ib.add(new Assign.NonBlocking(hangTimer, new Constant(0,hangTimer.getWidth())));
    }

    /**
     * Instantiates all logic necessary to run the HangTimer as well
     * as write the failed message to the results file upon expiry.
     */
    public void stateLogic (Module module, SimFileHandle resFile, StateMachine mach)
    {
        stateTimer(module, resFile, mach);
    }
    
    /**
     * <pre>
     * always @(posedge clk)
     * begin
     *   if (hangTimer > 150) begin
     *    $fwrite(resultFile, "FAIL: Hang Timer Expired\n");
     *    $finish;
     * end
     * 
     * if (foo_go || foo_done || bar_go || bar_done) hangTimer <= 0;
     * else hangTimer <= hangTimer + 1;
     * </pre>
     */
    private void stateTimer (Module module, SimFileHandle resFile, StateMachine mach)
    {
    	Option op = EngineThread.getGenericJob().getOption(OptionRegistry.HANG_TIMER);
        int HANGTIMER = Integer.parseInt(op.getValue(CodeLabel.UNSCOPED).toString(), 10);
        
        SequentialBlock trueBlock = new SequentialBlock();

        // Hang timer expiry

        FStatement.FWrite write = new FStatement.FWrite(resFile.getHandle(),
            new StringStatement("FAIL: Hang Timer Expired\\n"));
        trueBlock.add(write);
        trueBlock.add(new FStatement.Finish());
        
        ConditionalStatement cs = new ConditionalStatement(
            new Compare.GT(getHangTimer(), new Constant(HANGTIMER, getHangTimer().getWidth())),
            trueBlock);

        SequentialBlock block = new SequentialBlock(cs);

        // Hang timer increment
//         List goDone = new ArrayList();
//         for (Iterator iter = this.taskHandles.iterator(); iter.hasNext();)
//         {
//             TaskHandle th = (TaskHandle)iter.next();
//             goDone.add(th.getGoWire());
//             goDone.add(th.getDoneWire());
//         }

        Assign htReset = new Assign.NonBlocking(getHangTimer(), new Constant(0,getHangTimer().getWidth()));
        Assign htInc = new Assign.NonBlocking(getHangTimer(), new net.sf.openforge.verilog.model.Math.Add(getHangTimer(), new Constant(1,getHangTimer().getWidth())));
//         ConditionalStatement htCond = new ConditionalStatement(
//             new OrMany(goDone), htReset, htInc);
        ConditionalStatement htCond = new ConditionalStatement(
            new Logical.Or(mach.getAllGoWire(), mach.getAllDoneWire()),
            htReset, htInc);
        block.add(htCond);
        
        ProceduralTimingBlock ptb = new ProceduralTimingBlock(
            new EventControl(new EventExpression.PosEdge(mach.getClock())),
            block);

        module.state(new Always(ptb));
    }
    
}// HangTimer
