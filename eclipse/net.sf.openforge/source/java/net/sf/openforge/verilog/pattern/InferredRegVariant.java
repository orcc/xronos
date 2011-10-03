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

package net.sf.openforge.verilog.pattern;


import java.util.*;

import net.sf.openforge.lim.Reg;
import net.sf.openforge.verilog.model.*;

/**
 * InferredRegVariant.java
 * There are 14 possible inferable Reg varaints (see lim.reg).
 *
 *
 * <p>Created: Wed Dec 10 04:03:14 2003
 *
 * @author imiller
 * @version $Id: InferredRegVariant.java 280 2006-08-11 17:00:32Z imiller $
 */
public class InferredRegVariant extends StatementBlock implements ForgePattern,MappedModuleSpecifier
{
    private static final String _RCS_ = "$Rev: 280 $";

    /**
     * The BusWire created to represent the output of the inferred
     * reg.
     */
    private Net resultWire;
    
    /**
     * The PortWire created to represent the input of the inferred
     * reg.
     */
    private PortWire dataWire;

    /**
     * The PortWire created to represent the clock signal of the
     * inferred reg.
     */
    private PortWire clockWire;
    
    /**
     * A Collection of Wire objects that are consumed (used/referred
     * to) by the Verilog code block generated in this instance.
     */
    private Collection consumedNets;

    /**
     * A Collection of Wire objects that are produced by the Verilog
     * code block generated in this instance.
     */
    private Collection producedNets;
    
    /**
     * Fully populates this InferredRegVariant {@link StatementBlock}
     * with the necessary structures (always block) to infer a
     * register of the correct type and configuration based on the
     * {@link Reg} parameter.
     *
     * @param reg a non-null 'Reg'
     * @throws NullPointerException if Reg is null
     * @throws UnknownRegConfigurationException if Reg is not of a
     *         type or configuration that can be inferred by this class.
     */
    public InferredRegVariant (Reg reg) throws NullPointerException, UnknownRegConfigurationException
    {
        /* This constructor must construct a fully populated always
         * block based on the type of the register (reg.getType()).
         * For all synchronous flops (those that dont use CLEAR or
         * PRESET) the sensitivity list for the always block is simply
         * always @(posedge <clocksignal>).  The structure of the
         * contents of the always block determines the flops
         * behavior. 
         */

        if (reg == null)
        {
            throw new NullPointerException();
        }

        this.resultWire = NetFactory.makeNet(reg.getResultBus());
        this.dataWire=new PortWire(reg.getDataPort(), true);
        this.clockWire=new PortWire(reg.getClockPort());
        
        this.producedNets = new LinkedHashSet();
        producedNets.add(this.resultWire);
        
        this.consumedNets = new LinkedHashSet();
        consumedNets.add(this.clockWire);
        consumedNets.add(this.dataWire);
       
        final int resultWireWidth = this.resultWire.getWidth();
        final EventExpression clock_event = new EventExpression.PosEdge(clockWire);
        EventExpression[] eventExpressions = new EventExpression[] {clock_event};
        SequentialBlock blockBody = new SequentialBlock();
        Statement statements = new Assign.NonBlocking(this.resultWire, this.dataWire);
        
        final HexNumber allZero = new HexNumber(0, resultWireWidth);
        final HexNumber allOne = new HexNumber(-1, resultWireWidth);
        //HexNumber resetValue = allZero;

        final HexNumber resetValue;
        final HexNumber setValue;
        if (reg.getInitialValue() != null)
        {
            long mask = reg.getInitialValue().getConstantMask();
            long value = reg.getInitialValue().getValueMask() & mask;
            resetValue = new HexNumber(value, resultWireWidth);
            setValue = new HexNumber(~value, resultWireWidth);
        }
        else
        {
            resetValue = new HexNumber(0, resultWireWidth);
            setValue = new HexNumber(-1, resultWireWidth);
        }

        PortWire setWire, resetWire, enableWire, presetWire, clearWire;
        Statement setStatement, resetStatement, presetStatement, clearStatement;
         
        /*
         * Those comments of each case is to show the verilog
         * output of always block which need to be gererated.
         * For the purpose of documentation, we are assuming the reg
         * is 8 bits wide.
         */
        switch(reg.getType())
        {
            case Reg.REG:
                /*
                 * always @(posedge CLK)
                 *   begin
                 *     dout <= din;
                 *   end
                 */
                break;
            case Reg.REGS:
                /*
                 * always @(posedge CLK)
                 *   begin
                 *     if (SET) 
                 *       dout <= 8'hff;
                 *     else 
                 *       dout <= din;
                 *   end
                 */
                setWire = new PortWire(reg.getSetPort());
                setStatement = new Assign.NonBlocking(this.resultWire, setValue);
                statements = new ConditionalStatement(setWire, setStatement, statements);
                consumedNets.add(setWire);
                break;
            case Reg.REGR:
                /*
                 * always @(posedge CLK)
                 *   begin
                 *     if (RESET) 
                 *       dout <= 8'h0;
                 *     else 
                 *       dout <= din;
                 *   end
                 */
                resetWire = new PortWire(reg.getInternalResetPort());
                resetStatement = new Assign.NonBlocking(this.resultWire, resetValue);
                statements = new ConditionalStatement(resetWire, resetStatement, statements);
                consumedNets.add(resetWire);
                break;
            case Reg.REGRS:
                /*
                 * always @(posedge CLK)
                 *   begin
                 *     if (RESET) 
                 *       dout <= 8'h0;
                 *     else if (SET) 
                 *       dout <= 8'hff;
                 *     else 
                 *       dout <= din;
                 *   end
                 */
                setWire = new PortWire(reg.getSetPort());
                setStatement = new Assign.NonBlocking(this.resultWire, setValue);
                statements = new ConditionalStatement(setWire, setStatement, statements);
                resetWire = new PortWire(reg.getInternalResetPort());
                resetStatement = new Assign.NonBlocking(this.resultWire, resetValue);
                statements = new ConditionalStatement(resetWire, resetStatement, statements);
                consumedNets.add(setWire);
                consumedNets.add(resetWire);
                break;
            case Reg.REGE:
                /*
                 * always @(posedge CLK)
                 *   begin
                 *     if (ENABLE) 
                 *       dout <= din;
                 *   end
                 */
                enableWire = new PortWire(reg.getEnablePort());
                statements = new ConditionalStatement(enableWire, statements);
                consumedNets.add(enableWire);
                break;
            case Reg.REGSE:
                /*
                 * always @(posedge CLK)
                 *   begin
                 *     if (SET) 
                 *       dout <= 8'hff;
                 *     else if (ENABLE) 
                 *       dout <= din;
                 *   end
                 */
                enableWire = new PortWire(reg.getEnablePort());
                statements = new ConditionalStatement(enableWire, statements);
                setWire = new PortWire(reg.getSetPort());
                setStatement = new Assign.NonBlocking(this.resultWire, setValue);
                statements = new ConditionalStatement(setWire, setStatement, statements);
                consumedNets.add(enableWire);
                consumedNets.add(setWire);
                break;
            case Reg.REGRE:
                /*
                 * always @(posedge CLK)
                 *   begin
                 *   if (RESET) 
                 *     dout <= 8'h0;
                 *   else if (ENABLE) 
                 *     dout <= din;
                 *   end
                 */
                enableWire = new PortWire(reg.getEnablePort());
                statements = new ConditionalStatement(enableWire, statements);
                resetWire = new PortWire(reg.getInternalResetPort());
                resetStatement = new Assign.NonBlocking(this.resultWire, resetValue);
                statements = new ConditionalStatement(resetWire, resetStatement, statements);
                consumedNets.add(enableWire);
                consumedNets.add(resetWire);
                break;
            case Reg.REGRSE:
                /*
                 * always @(posedge CLK)
                 *   begin
                 *     if (RESET) 
                 *       dout <= 8'h0;
                 *     else if (SET) 
                 *       dout <= 8'hff;
                 *     else if (ENABLE) 
                 *       dout <= din;
                 *   end
                 */
                enableWire = new PortWire(reg.getEnablePort());
                statements = new ConditionalStatement(enableWire, statements);
                setWire = new PortWire(reg.getSetPort());
                setStatement = new Assign.NonBlocking(this.resultWire, setValue);
                statements = new ConditionalStatement(setWire, setStatement, statements);
                resetWire = new PortWire(reg.getInternalResetPort());
                resetStatement = new Assign.NonBlocking(this.resultWire, resetValue);
                statements = new ConditionalStatement(resetWire, resetStatement, statements);
                consumedNets.add(enableWire);
                consumedNets.add(setWire);
                consumedNets.add(resetWire);
                break;
            case Reg.REGP:
                /*
                 * always @(posedge CLK or posedge PRESET)
                 *   begin
                 *     if (PRESET) 
                 *       dout <= 8'hff;
                 *     else 
                 *       dout <= din;
                 *   end
                 */
                presetWire = new PortWire(reg.getSetPort());
                presetStatement = new Assign.NonBlocking(this.resultWire, setValue);
                statements = new ConditionalStatement(presetWire, presetStatement, statements);
                eventExpressions = new EventExpression[] {clock_event, new EventExpression.PosEdge(presetWire)};
                consumedNets.add(presetWire);
                break;
            case Reg.REGC:
                /*
                 * always @(posedge CLK or posedge CLEAR)
                 *   begin
                 *     if (CLEAR) 
                 *       dout <= 8'h0;
                 *     else 
                 *       dout <= din;
                 *   end
                 */                
                clearWire = new PortWire(reg.getInternalResetPort());
                clearStatement = new Assign.NonBlocking(this.resultWire, resetValue);
                statements = new ConditionalStatement(clearWire, clearStatement, statements);
                eventExpressions = new EventExpression[] {clock_event, new EventExpression.PosEdge(clearWire)};
                consumedNets.add(clearWire);
                break;
            case Reg.REGPE:
                /*
                 * always @(posedge CLK or posedge PRESET)
                 *   begin
                 *     if (PRESET) 
                 *       dout <= 8'hff;
                 *     else if (ENABLE) 
                 *       dout <= din;
                 *   end
                 */
                enableWire = new PortWire(reg.getEnablePort());
                statements = new ConditionalStatement(enableWire, statements);
                presetWire = new PortWire(reg.getSetPort());
                presetStatement = new Assign.NonBlocking(this.resultWire, setValue);
                statements = new ConditionalStatement(presetWire, presetStatement, statements);
                eventExpressions = new EventExpression[] {clock_event, new EventExpression.PosEdge(presetWire)};
                consumedNets.add(enableWire);
                consumedNets.add(presetWire);
                break;
            case Reg.REGCE:
                /*
                 * always @(posedge CLK or posedge CLEAR)
                 *   begin
                 *     if (CLEAR) 
                 *       dout <= 8'h0;
                 *     else if (ENABLE) 
                 *       dout <= din;
                 *   end
                 */
                enableWire = new PortWire(reg.getEnablePort());
                statements = new ConditionalStatement(enableWire, statements);
                clearWire = new PortWire(reg.getInternalResetPort());
                clearStatement = new Assign.NonBlocking(this.resultWire, resetValue);
                statements = new ConditionalStatement(clearWire, clearStatement, statements);
                eventExpressions = new EventExpression[] {clock_event, new EventExpression.PosEdge(clearWire)};
                consumedNets.add(enableWire);
                consumedNets.add(clearWire);
                break;
            case Reg.REGCP:
                /*
                 * always @(posedge CLK or posedge PRESET or posedge CLEAR)
                 *   begin
                 *     if (CLEAR) 
                 *       dout <= 8'h0;
                 *     else if (PRESET) 
                 *       dout <= 8'hff;
                 *     else 
                 *       dout <= din;
                 *   end
                 */
                presetWire = new PortWire(reg.getSetPort());
                presetStatement = new Assign.NonBlocking(this.resultWire, setValue);
                statements = new ConditionalStatement(presetWire, presetStatement, statements);
                clearWire = new PortWire(reg.getInternalResetPort());
                clearStatement = new Assign.NonBlocking(this.resultWire, resetValue);
                statements = new ConditionalStatement(clearWire, clearStatement, statements);
                eventExpressions = new EventExpression[] {clock_event, new EventExpression.PosEdge(presetWire), new EventExpression.PosEdge(clearWire)};
                consumedNets.add(presetWire);
                consumedNets.add(clearWire);
                break;
            case Reg.REGCPE:
                /*
                 * always @(posedge CLK or posedge PRESET or posedge CLEAR)
                 *   begin
                 *     if (CLEAR) 
                 *       dout <= 8'h0;
                 *     else if (PRESET) 
                 *       dout <= 8'hff;
                 *     else if (ENABLE) 
                 *       dout <= din;
                 *   end
                 */
                enableWire = new PortWire(reg.getEnablePort());
                statements = new ConditionalStatement(enableWire, statements);
                presetWire = new PortWire(reg.getSetPort());
                presetStatement = new Assign.NonBlocking(this.resultWire, setValue);
                statements = new ConditionalStatement(presetWire, presetStatement, statements);
                clearWire = new PortWire(reg.getInternalResetPort());
                clearStatement = new Assign.NonBlocking(this.resultWire, resetValue);
                statements = new ConditionalStatement(clearWire, clearStatement, statements);
                eventExpressions = new EventExpression[] {clock_event, new EventExpression.PosEdge(presetWire), new EventExpression.PosEdge(clearWire)};
                consumedNets.add(enableWire);
                consumedNets.add(presetWire);
                consumedNets.add(clearWire);
                break;
            default:
                throw new UnknownRegConfigurationException ();
        }
        
        blockBody.add(statements);
        statements = buildAlwaysBlock(new EventExpression(eventExpressions), blockBody);
        add(statements);
    }
    
    /**
     * Provides the collection of Nets which this statement of verilog
     * uses as input signals.
     */
    public Collection getConsumedNets ()
    {
        return consumedNets;
    }
    
    /**
     * Provides the collection of Nets which this statement of verilog
     * produces as output signals.
     */
    public Collection getProducedNets ()
    {
        return Collections.singleton(this.resultWire);
    }

    /**
     * Returns the empty set because the Reg is inferred and does not
     * depend on any external Verilog for its definition.
     */
    public Set getMappedModules ()
    {
        return Collections.EMPTY_SET;
    }

    /**
     * Utility method for building the frame structure of a verilog
     * always block.
     * 
     */
    private Statement buildAlwaysBlock (EventExpression eventExpressions, SequentialBlock body)
    {
        final EventControl eventControl = new EventControl(eventExpressions);
        final ProceduralTimingBlock ptb = new ProceduralTimingBlock(eventControl, body);
        final Statement always = new Always(ptb);
        
        return always;
    }
    
}// InferredRegVariant
