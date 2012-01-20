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


import java.util.*;

import net.sf.openforge.verilog.model.*;
import net.sf.openforge.verilog.pattern.OrMany;

/**
 * StateMachine controls the advancement of the arguement and result
 * indices as well as supplying the 'go' for each task.  This is the
 * main functionality for sequencing the simulation and ensuring that
 * all vectors to a given task have completed before advancing to the
 * next task.
 *
 * <p>Created: Wed Jan  8 15:43:51 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: StateMachine.java 2 2005-06-09 20:00:48Z imiller $
 */
public class StateMachine 
{
    private static final String _RCS_ = "$Rev: 2 $";

    // The global signals
    private Register clock;
    private Register reset;

    private List taskHandles;

    private Register argIndex;
    private Register resIndex;
    private Wire expectedValid;

    // For driving the lgsr signal.
    private Register lgsr;
    // For starting the simulation
    private Register start;
    // Indicates that the simulation is complete
    private Register done;
    // Indicates that one or more tasks has failed.
    private Wire fail;

    // The number of GOs that have been applied to the current task,
    // but have not yet received a done for.
    private Register pendingCount;
    // Pauses the assertion of the next GO (only used when we are
    // asserting GOs based on go spacing and not necessarily waiting
    // for dones)
    private Wire pause;
    // The previous goMask
    private Register prevGo;
    // A wire which is all task GOs ored together
    private Wire allGoWire;
    // A wire which is all task DONEs ored together
    private Wire allDoneWire;

    // A wire which is ARGIndex != maxArgCount.  Used to mask task
    // next go's at endof simulation
    private Wire notMaxArg;
    
    private Wire advanceResults;
    
    private int vectorCount;
    
    public StateMachine (List taskHandles, int vectorCount, Memories mems)
    {
        this.vectorCount = vectorCount;
        
        this.clock = new Register("clk", 1);
        this.reset = new Register("reset", 1);
        
        this.lgsr = new Register("LGSR", 1);
        this.start = new Register("start", 1);
        this.done = new Register("done", 1);
        this.fail = new Wire("fail", 1);
        
        this.pendingCount = new Register("pendingCount", 32);
        this.pause = new Wire("pause", 1);
        this.prevGo = new Register("previousGoState", mems.getNextGoMemory().getWidth());
        this.allGoWire = new Wire("allGos", 1);
        this.allDoneWire = new Wire("allDones", 1);
        this.notMaxArg = new Wire("notMaxArg", 1);
        
        this.advanceResults = new Wire("advanceResults", 1);
        
        this.taskHandles = taskHandles;
        
        int indexSize = (int)java.lang.Math.ceil(java.lang.Math.log(mems.elementCount())/java.lang.Math.log(2));
        this.argIndex = new Register("ARG_index", indexSize);
        this.resIndex = new Register("RES_index", indexSize);
        
        this.expectedValid = new Wire("expected_valid",1);
    }
    
    /**
     * Initializes all the registers
     */
    public void stateInits (InitialBlock ib)
    {
        ib.add(new Assign.NonBlocking(clock, new Constant(0,clock.getWidth())));
        ib.add(new Assign.NonBlocking(reset, new Constant(1,reset.getWidth())));
        ib.add(new Assign.NonBlocking(lgsr, new Constant(1,lgsr.getWidth())));
        ib.add(new Assign.NonBlocking(start, new Constant(0,start.getWidth())));
        ib.add(new Assign.NonBlocking(done, new Constant(0,done.getWidth())));
        ib.add(new Assign.NonBlocking(pendingCount, new Constant(0,pendingCount.getWidth())));
        ib.add(new Assign.NonBlocking(prevGo, new Constant(0,prevGo.getWidth())));
        ib.add(new Assign.NonBlocking(argIndex, new Constant(0,argIndex.getWidth())));
        ib.add(new Assign.NonBlocking(resIndex, new Constant(0,resIndex.getWidth())));

        Assign assign = new Assign.NonBlocking(lgsr, new Constant(0,lgsr.getWidth()));
        ib.add(new DelayStatement(assign, 1));
        assign = new Assign.NonBlocking(reset, new Constant(1,1));
        ib.add(new DelayStatement(assign, 149));
        assign = new Assign.NonBlocking(reset, new Constant(0,1));
        ib.add(new DelayStatement(assign, 500));
        
        ib.add(new InlineComment("10 cycles", Comment.SHORT));
        assign = new Assign.NonBlocking(start, new Constant(1,1));
        ib.add(new DelayStatement(assign, 100));
        assign = new Assign.NonBlocking(start, new Constant(0,1));
        ib.add(new DelayStatement(assign, 50));
    }

    /**
     * Adds all the state machine logic, including the expectedValid
     * wire, arg/result indices, PASSED, and pause logic.
     */
    public void stateLogic (Module module, Memories mems, SimFileHandle resFile)
    {
        stateLGSR(module);
        stateExpectedValid(module, mems);
        stateMachine(module);
        statePassed(module, resFile);
        statePause(module, mems);
    }

    private void stateLGSR (Module module)
    {
        Module m = new Module("glbl");
        Net w = new Wire("GSR",1);
        QualifiedNet qn = new QualifiedNet(m,w);
        module.state(new Assign.Continuous(qn,lgsr));

        m = new Module("glbl");
        w = new Wire("GTS",1);
        qn = new QualifiedNet(m,w);
        module.state(new Assign.Continuous(qn,new Constant(0,1)));

        m = new Module("glbl");
        w = new Wire("PRLD",1);
        qn = new QualifiedNet(m,w);
        module.state(new Assign.Continuous(qn,new Constant(0,1)));
    }
    
    private void stateExpectedValid (Module module, Memories mems)
    {
        InitializedMemory valids = mems.getResultValidMemory();
        
        Assign assign = new Assign.Continuous(getExpectedValidWire(),
            new MemoryElement(valids, getResIndex()));

        module.state(assign);
    }
    
    /**
     * All the stuff that sequences things (the arg/result indices, etc).
     */
    private void stateMachine (Module module)
    {
        // The design clock.
        Assign assign = new Assign.NonBlocking(getClock(), new Unary.Negate(getClock()));
        module.state(new Always(new DelayStatement(assign, 25)));

        List dones = new ArrayList();
        List taskFails = new ArrayList();
        for (Iterator iter = this.taskHandles.iterator(); iter.hasNext();)
        {
            TaskHandle th = (TaskHandle)iter.next();
            dones.add(th.getDoneWire());
            taskFails.add(th.getExpectedChecker().getFailWire());
        }
        dones.add(this.start);

        // Define fail
        module.state(new Assign.Continuous(this.fail, new OrMany(taskFails)));
        
        // signal to advance the results index
        module.state(new Assign.Continuous(advanceResults, new OrMany(dones)));
        
        // Generate the notMaxArg flag.  Use vectorCount not
        // 'vectorCount-1' so that we generate the 'go' for the last
        // element.  Has the side effect of advancing the arg index to
        // vectorCount+1, but that's ok since we pad the memories.
        Compare.NEQ neq = new Compare.NEQ(getArgIndex(),
            new Constant(this.vectorCount, getArgIndex().getWidth()));
        module.state(new Assign.Continuous(this.notMaxArg, neq));

        
        // Now for advancing the indices and generating flags
        SequentialBlock block = new SequentialBlock();

        // Generate the done flag
        Compare.EQ eq = new Compare.EQ(getResIndex(),
            new Constant(this.vectorCount-1, getResIndex().getWidth()));
        block.add(new Assign.NonBlocking(getDone(),
                      new Logical.And(advanceResults, eq)));
        
        
        // Arg index increment.
        ConditionalStatement cond1 = new ConditionalStatement(this.allGoWire,
            new Assign.NonBlocking(argIndex,
                new net.sf.openforge.verilog.model.Math.Add(
                    argIndex, new Constant(1, argIndex.getWidth()))));
        block.add(cond1);
        
        // Result index increment.
        ConditionalStatement cond2 = new ConditionalStatement(
            this.allDoneWire, new Assign.NonBlocking(resIndex,
                new net.sf.openforge.verilog.model.Math.Add(
                    resIndex, new Constant(1, resIndex.getWidth()))));
        block.add(cond2);
        
        ProceduralTimingBlock ptb = new ProceduralTimingBlock(
            new EventControl(new EventExpression.PosEdge(getClock())),
            block);

        module.state(new Always(ptb));
    }

    private void statePassed (Module module, SimFileHandle resFile)
    {
        SequentialBlock trueBlock = new SequentialBlock();
        
        trueBlock.add(new FStatement.FWrite(resFile.getHandle(),
                          new StringStatement("PASSED\\n")));
        trueBlock.add(new DelayStatement(new FStatement.Finish(), 500));

        ConditionalStatement cs = new ConditionalStatement(
            new Logical.And(getDone(), new Unary.Not(getFail())), trueBlock);

        ProceduralTimingBlock ptb = new ProceduralTimingBlock(
            new EventControl(new EventExpression.PosEdge(getClock())),
            cs);
        
        module.state(new Always(ptb));
    }

    /**
     * Generates a 'pause' signal which keeps any new 'gos' from being
     * applied to the design until all pending accesses have
     * completed.
     * <pre>
     * pause = (nextGo != currentGo || pause) && pendingCount != 0;
     * </pre>
     *
     * @param module a value of type 'Module'
     * @param mems a value of type 'Memories'
     */
    private void statePause (Module module, Memories mems)
    {
        // Generate an allGo's and allDones signal for inc/dec of pendingCount
        List allGos = new ArrayList();
        List allDones = new ArrayList();
        for (Iterator iter = this.taskHandles.iterator(); iter.hasNext();)
        {
            TaskHandle th = (TaskHandle)iter.next();
            allGos.add(th.getGoWire());
            allDones.add(th.getDoneWire());
        }
        
        final InitializedMemory nextGo = mems.getNextGoMemory();
        
        final Wire nextGoWire = new Wire("nextGoWire",mems.getNextGoMemory().getWidth());
        module.state(new Assign.Continuous(this.allGoWire, new OrMany(allGos)));
        module.state(new Assign.Continuous(this.allDoneWire, new OrMany(allDones)));
        module.state(new Assign.Continuous(nextGoWire, 
                         new MemoryElement(nextGo, getArgIndex())));
        
        // pause <= (nextGo != prevGo) || (pause & pending != 0);
        final Compare.NEQ neq = new Compare.NEQ(nextGoWire, this.prevGo);
        final Compare.NEQ pendNEQ = new Compare.NEQ(this.pendingCount,
            new Constant(0, this.pendingCount.getWidth()));
        final Logical.Or or = new Logical.Or(neq, getPause());
        module.state(new Assign.Continuous(
                this.pause, new Logical.And(new Group(or), pendNEQ)));
        
        SequentialBlock block = new SequentialBlock();

        // Capture the 'previous' go.
        block.add(new Assign.NonBlocking(this.prevGo, nextGoWire));

        // Increment/decrement pendingCount
        SequentialBlock incBlock = new SequentialBlock();
        Assign increment = new Assign.NonBlocking(pendingCount,
            new net.sf.openforge.verilog.model.Math.Add(
                pendingCount, new Constant(1, pendingCount.getWidth())));
        incBlock.add(increment);
        
        SequentialBlock elseBlock = new SequentialBlock();
        Assign decrement = new Assign.NonBlocking(pendingCount,
            new net.sf.openforge.verilog.model.Math.Subtract(
                pendingCount, new Constant(1, pendingCount.getWidth())));
        elseBlock.add(new ConditionalStatement(
                new Logical.And(this.allDoneWire, new Unary.Not(this.allGoWire)),
                decrement));
        
        ConditionalStatement cs = new ConditionalStatement(
            new Logical.And(this.allGoWire, new Unary.Not(this.allDoneWire)), incBlock, elseBlock);

        block.add(cs);
        
        ProceduralTimingBlock ptb = new ProceduralTimingBlock(
            new EventControl(new EventExpression.PosEdge(getClock())),
            block);
        
        module.state(new Always(ptb));
    }
    
    /**
     * Returns the simulation master clock
     */
    public Register getClock ()
    {
        return this.clock;
    }
    
    /**
     * Returns the simulation master reset signal
     */
    public Register getReset ()
    {
        return this.reset;
    }

    /**
     * Returns the Register that indicates the simulation is complete.
     */
    public Register getDone ()
    {
        return this.done;
    }

    /**
     * Returns the Wire that indicates a simulation failure
     */
    public Wire getFail ()
    {
        return this.fail;
    }

    /**
     * Returns the Wire that, when true, masks the GO to the task and
     * prevents incrementing of the arg/results indices.
     */
    public Wire getPause ()
    {
        return this.pause;
    }
    
    /**
     * Returns the argument index
     */
    public Register getArgIndex ()
    {
        return this.argIndex;
    }

    /**
     * Returns the result index
     */
    public Register getResIndex ()
    {
        return this.resIndex;
    }

    /**
     * Returns the start signal
     */
    public Register getStart ()
    {
        return this.start;
    }
    

    /**
     * Returns the Wire that indicates that the expected results wire
     * is valid.
     */
    public Wire getExpectedValidWire ()
    {
        return this.expectedValid;
    }

    /**
     * Returns the signal indicating that any go is being asserted.
     */
    public Wire getAllGoWire ()
    {
        return this.allGoWire;
    }

    /**
     * Returns the signal indicating that any done is being asserted.
     */
    public Wire getAllDoneWire ()
    {
        return this.allDoneWire;
    }

    /**
     * Returns the signal indicating whether or not we are at the
     * maximum argument (vector count + 1).
     */
    public Wire getNotMaxArg ()
    {
        return this.notMaxArg;
    }
    

}// StateMachine
