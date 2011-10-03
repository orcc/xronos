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

import net.sf.openforge.verilog.model.*;


/**
 * A SyncBlock is the traditional Forge pattern of a
 * an always block with Clock and Reset sensitivity list.
 * <P>
 * Example:<BR>
 * <CODE>
 * always @(posedge RESET or posedge CLK)<BR>
 * begin<<BR>
 * if (RESET)<BR>
 *   begin<BR>
 *     iadd2_reg <=0;<BR>
 *   end<BR>
 * else<BR>
 *   begin<BR>
 *     iadd2_reg <= iadd2;<BR>
 *   end<BR>
 * end<BR>
 * </CODE>
 *
 * <P>
 *
 * Created: Tue Mar 12 09:46:58 2002
 *
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: SyncBlock.java 2 2005-06-09 20:00:48Z imiller $
 */

public class SyncBlock implements ForgePattern  
{

    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    private Always always;
    private Net clock;
    private Net reset;
    private EventControl control;
    private SequentialBlock reset_block;
    private SequentialBlock clock_block;
    
    private Set produced_nets = new LinkedHashSet();
    private Set consumed_nets = new LinkedHashSet();

    public SyncBlock (Net clock, Net reset)
    {
        this.clock = clock;
        this.reset = reset;
        
        consumed_nets.add(clock);
        consumed_nets.add(reset);

        EventExpression reset_event = new EventExpression.PosEdge(reset);
        EventExpression clock_event = new EventExpression.PosEdge(clock);
        EventExpression ee = new EventExpression(new EventExpression[] {clock_event, reset_event});
        control = new EventControl(ee);

        reset_block = new SequentialBlock();
        clock_block = new SequentialBlock();

        ConditionalStatement cs = new ConditionalStatement(reset, reset_block, clock_block);

        SequentialBlock body = new SequentialBlock(cs);
        
        ProceduralTimingBlock ptb = new ProceduralTimingBlock(control, body);
        
        always = new Always(ptb);

    } // SyncBlock


    /**
     * Add sync code. Code can only be added in complementary pairs --
     * a reset and a clock portion. This is to encourage correct coding.
     *
     * @param on_reset the statement which occurs during reset edges
     * @param on_clock the statement which occurs during clock edges
     */
    public void add(ForgePattern on_reset, ForgePattern on_clock)
    {
        reset_block.add(on_reset);
        clock_block.add(on_clock);
        
        consumed_nets.addAll(on_reset.getConsumedNets());
        consumed_nets.addAll(on_clock.getConsumedNets());
        produced_nets.addAll(on_reset.getProducedNets());
        produced_nets.addAll(on_clock.getProducedNets());
    } // add()

    /**
     * Add sync code for a register. The explicit clock-edge statement
     * is complimented by an auto-generated zero-assign to the given
     * register.
     *
     * @param reg the register being synchronized
     * @param on_clock the statement which occurs during clock edges
     */
    public void add(Register reg, ForgePattern on_clock)
    {
        add(new ForgeStatement(Collections.singleton(reg), 
                new Assign.NonBlocking(reg, makeZero(reg))), 
            on_clock);
    } // add()

    private Constant makeZero(Net n)
    {
        return new Constant(0, n.getWidth());
    }

    /**
     *
     * @return <description>
     */
    public Lexicality lexicalify()
    {
        return always.lexicalify();
    } // lexicalify()

    /**
     *
     * @return <description>
     */
    public Collection getNets()
    {
        HashSet nets = new HashSet();
        
        nets.addAll(control.getNets());
        nets.addAll(reset_block.getNets());
        nets.addAll(clock_block.getNets());

        return nets;
    } // getNets()

    /**
     * Provides the collection of Nets which this statement of verilog
     * uses as input signals.
     */
    public Collection getConsumedNets()
    {
        return consumed_nets;
    }
    
    /**
     * Provides the collection of Nets which this statement of verilog
     * produces as output signals.
     */
    public Collection getProducedNets()
    {
        return produced_nets;
    }
    
    public String toString()
    {
        return lexicalify().toString();
    }
    
} // class SyncBlock
