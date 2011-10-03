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

package net.sf.openforge.optimize.constant;

import java.util.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.optimize.*;
import net.sf.openforge.util.naming.ID;



/**
 * TwoPassPartialConstant uses the {@link DataFlowVisitor} to traverse
 * the visited target first in foward data flow order calling
 * {@link Component#forwardPropagate()} on each visited component.
 * Once the {@link Visitable} target has been traversed forward, it is
 * then traversed in reverse data flow order, calling
 * {@link Component#reversePropagate()} on each visited component.
 * This 2-pass traversal continues until no new modifications have
 * been made.  Special handling has been added to support
 * {@link Design} level structures such as pins, referee's, etc.
 *
 * <p>Created: Thu Nov 14 12:20:09 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: TwoPassPartialConstant.java 28 2005-09-15 15:13:55Z imiller $
 */
public class TwoPassPartialConstant extends DataFlowVisitor
{
    private static final String _RCS_ = "$Rev: 28 $";

    private boolean modified = false;
    private long nodeCount = 0;

    /** True if global components should be visited, false otherwise */
    private boolean shouldVisitGlobals;
    
    /**
     * Creates a new <code>TwoPassPartialConstant</code> instance.
     *
     * @param shouldVisitGlobals true if global components should be visited,
     *          false otherwise
     */
    private TwoPassPartialConstant (boolean shouldVisitGlobals)
    {
        this.shouldVisitGlobals = shouldVisitGlobals;
    }

    /**
     * Creates a new <code>TwoPassPartialConstant</code> instance
     * which visits all global components by default.
     */
    private TwoPassPartialConstant ()
    {
        this(true);
    }
    
    /**
     * Fully propagates partial constant information through the
     * {@link Visitable} target by iteratively traversing the design
     * in forward data flow order calling forwardPropagate, then in
     * reverse data flow order calling reversePropagate.  Visits all
     * global components.  Does not affect user defined verbosity
     * level. 
     *
     * @param target a Visitable target, from which all contained
     * components will be recursively traversed.
     * @param additionalOpts a Collection of
     * {@link ComponentSwapVisitor} visitors that will be called on
     * the Visitable target after each complete run of forward then
     * reverse visitation.
     */
    public static void propagate (Visitable target, Collection additionalOpts)
    {
        propagate(target, additionalOpts, true, false);
    }
    
    /**
     * Fully propagates partial constant information through the
     * {@link Visitable} target by iteratively traversing the design
     * in forward data flow order calling forwardPropagate, then in
     * reverse data flow order calling reversePropagate.  Visits all
     * global components.  Does not affect user defined verbosity
     * level 
     *
     * @param target a Visitable target, from which all contained
     * components will be recursively traversed.
     * @param additionalOpts a Collection of
     * {@link ComponentSwapVisitor} visitors that will be called on
     * the Visitable target after each complete run of forward then
     * reverse visitation.
     */
    public static void propagate (Visitable target, boolean visitGlobals)
    {
        propagate(target, Collections.EMPTY_LIST, visitGlobals, false);
    }

    /**
     * Propagates partial constants on the visitable target with user
     * defined verbosity overridden (to be 'quiet').  Used to set the
     * initial state of components.
     *
     * @param target a value of type 'Visitable'
     */
    public static void propagateQuiet (Visitable target)
    {
        propagate(target, Collections.EMPTY_LIST, true, true);
    }

    /**
     * Fully propagates partial constant information through the
     * {@link Visitable} target by iteratively traversing the design
     * in forward data flow order calling forwardPropagate, then in
     * reverse data flow order calling reversePropagate.  Conditionally
     * visits global components.
     *
     * @param target a Visitable target, from which all contained
     * components will be recursively traversed.
     * @param additionalOpts a Collection of
     * {@link ComponentSwapVisitor} visitors that will be called on
     * the Visitable target after each complete run of forward then
     * reverse visitation.
     * @param shouldVisitGlobals true if global components should be visited,
     *          false if they should be skipped
     * @param quiet true if typical messages should be suppressed, eg
     * this propagation is not a true run of constant prop on the
     * design but is being used to initialize a component
     */
    public static void propagate (Visitable target, Collection additionalOpts, boolean shouldVisitGlobals, boolean quiet)
    {
        //if (_optimize.db && target instanceof Design) _optimize.d.launchXGraph((Design)target, false);
        // Make runs forward, then reverse over the entire target
        TwoPassPartialConstant visitor = new TwoPassPartialConstant(shouldVisitGlobals);
        boolean changed = true;
        visitor.nodeCount = 0;
        int passCount = 0;
        while (changed)
        {
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "============================================");
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "\tStarting partial constant propagation pass " + passCount + " on " + target);
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "============================================");

            if (!quiet)
            {
                EngineThread.getGenericJob().verbose("\tStarting partial constant propagation pass " + passCount + " on " + ID.showLogical(target));
            }
            passCount++;
            
            changed = false;

            // Forward
            visitor.reset();
            visitor.setRunForward(true);
            target.accept(visitor);
            changed |= visitor.didModify();

            // Reverse
            visitor.reset();
            visitor.setRunForward(false);
            target.accept(visitor);
            boolean reverseChanged=visitor.didModify();
            
            changed |= reverseChanged;
            

            // Other optimizations to run
            for (Iterator iter = additionalOpts.iterator(); iter.hasNext();)
            {
                ComponentSwapVisitor csv = (ComponentSwapVisitor)iter.next();
                target.accept(csv);
                changed |= csv.didModify();
                csv.clear();
            }
        }
        //System.out.println("\t\tDEBUG: VISITED " + visitor.nodeCount + " nodes");
    }

    /**
     * Propagates partial constant information through the Visitable by iteratively
     * traversing the design in forward data flow order calling forwardPropagate
     *
     * @param target a Visitable target, from which all contained components will be
     * recursively traversed.
     */
    public static void forward (Visitable target)
    {        
        TwoPassPartialConstant visitor = new TwoPassPartialConstant();
        boolean changed = true;
        visitor.nodeCount = 0;
        int passCount = 0;
        while (changed)
        {
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "============================================");
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "\tStarting partial constant propagation forward pass only " + passCount + " on " + target);
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "============================================");
            passCount++;
            
            changed = false;

            // Forward
            visitor.reset();
            visitor.setRunForward(true);
            target.accept(visitor);
            changed |= visitor.didModify();
        }
    }

    private boolean didModify ()
    {
        return this.modified;
    }

    private void reset ()
    {
        this.modified = false;
    }

    /**
     * Override of super method to call forward or reverse propagate
     * depending on the direction of traversal
     */
    protected void preFilterAny (Component c)
    {
        this.nodeCount++;
        if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "comp: " + c.showIDLogical() + " " + c.show(true));

        if (isForward())
        {
            boolean forwardMod = c.propagateValuesForward();
            this.modified |= forwardMod;
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "\tfwd: " + c.cpDebug(true) + " \tmodified " + forwardMod);
        }
        else
        {
            boolean reverseMod = c.propagateValuesBackward();
            this.modified |= reverseMod;
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "\trev: " + c.cpDebug(true) + " \tmodified " + reverseMod);
        }
        super.preFilterAny(c);
    }
    
    /**
     * Propogates {@link Value Values} through a {@link Call}.
     * <P>
     * In the forward direction, the following steps are taken:
     * <ul>
     * <li>the values are computed for the call's input {@link Port Ports}
     * <li>these values are pushed to the input {@link Port Ports} of the {@link Procedure}
     * <li>the {@link Procedure} is traversed
     * <li>the values of the call's {@link Bus Buses} are pulled
             from the {@link Bus Buses} of the {@link Procedure}
     * </ul>
     * <P>
     * In the reverse direction, the following steps are taken:
     * <ul>
     * <li>the values are computed for the call's output {@link Bus Buses}
     * <li>these values are pushed to the output {@link Bus Buses} of the {@link Procedure}
     * <li>the {@link Procedure} is traversed
     * <li>the values of the call's {@link Port Ports} are pulled
             from the {@link Port Ports} of the {@link Procedure}
     * </ul>
     *
     * @param call the call through which values are to be propagated
     *
     * @see Call#copyInputValuesToProcedure()
     * @see Call#copyInputValuesFromProcedure()
     * @see Call#copyOutputValuesToProcedure()
     * @see Call#copyOutputValuesFromProcedure()
     */
    public void visit (Call call)
    {
        if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "Call: " + call + " proc: " + call.getProcedure());
        if (_optimize.db && call.getProcedure() != null) _optimize.ln(_optimize.PARTIAL, "\tbody: " + call.getProcedure().getBody());
        
        if (isForward())
        {
            /*
             * Update the call's Port values.
             */
            boolean callMod = call.propagateValuesForward();
            this.modified |= callMod;

            /*
             * Propagate the call Port values to the procedure body Ports.
             */
            callMod = call.copyInputValuesToProcedure();
            modified |= callMod;

            /*
             * Traverse into the Procedure.
             */
            super.traverse(call.getProcedure());

            /*
             * Propagate the procedure body Bus values to the call's Buses.
             */
            callMod = call.copyOutputValuesFromProcedure();
            modified |= callMod;
        }
        else
        {
            /*
             * Update the call's Bus values.
             */
            boolean callMod = call.propagateValuesBackward();
            modified |= callMod;

            /*
             * Propagate the call's Bus values to the procedure body Buses.
             */
            callMod = call.copyOutputValuesToProcedure();
            modified |= callMod;

            /*
             * Traverse into the Procedure.
             */
            super.traverse(call.getProcedure());

            /*
             * Propagate the procedure body Port values to the call's Ports.
             */
            callMod = call.copyInputValuesFromProcedure();
            modified |= callMod;
        }
    }
    
    /**
     * Override the super in order to push the unioned value info from
     * all Call's onto the Procedures body.
     */
    protected void preFilter (Procedure procedure)
    {
        super.preFilter(procedure);
    }

    /**
     * Override to run partial constant prop on the design level.
     */
    public void visit (Design design)
    {
        if (isForward())
        {
            super.preFilter((Visitable)design);
            // The designProp goes through all components in the
            // design module, no need to traverse the tasks
            //traverse(design);
            
            // NOTE!  this line used to read:
            //    this.modified |= designProp(design);
            // However this FAILED TO WORK!  The 'this.modified'
            // variable was NOT getting updated correctly and we were
            // iterating only 1 time.  Why/how this fixes this I do
            // not know!
            boolean designModified = designProp(design);
            this.modified |= designModified;
//             this.modified |= designProp(design);
            super.postFilter((Visitable)design);
        }
        else
        {
            super.preFilter((Visitable)design);
            // The designProp goes through all components in the
            // design module, no need to traverse the tasks
            
            // NOTE!  this line used to read:
            //    this.modified |= designProp(design);
            // However this FAILED TO WORK!  The 'this.modified'
            // variable was NOT getting updated correctly and we were
            // iterating only 1 time.  Why/how this fixes this I do
            // not know!
            boolean designModified = designProp(design);
            this.modified |= designModified;
//             this.modified |= designProp(design);
            //traverse(design);
            super.postFilter((Visitable)design);
        }
    }
    
    
    /**
     * Runs partial constant propagation on all the resources
     * contained at the design level.
     *
     * @return true if any information was changed by this traversal.
     */
    private boolean designProp (Design design)
    {
        boolean mod = false;
        
        if (!shouldVisitGlobals)
        {
            return mod;
        }

        // Ensure we visit the task calls first.
        Set taskCalls = new HashSet();
        for (Iterator iter = design.getTasks().iterator(); iter.hasNext();)
        {
            Task task = (Task)iter.next();
            taskCalls.add(task.getCall());
            task.accept(this);
        }

        for (Iterator iter = design.getDesignModule().getComponents().iterator(); iter.hasNext();)
        {
            Visitable vis = (Visitable)iter.next();
            
            if (taskCalls.contains(vis))
                continue;
            
            try
            {
                vis.accept(this);
            }
            catch (UnexpectedVisitationException uve)
            {
                if (vis instanceof Module)
                {
                    catchAll((Module)vis);
                }
                else
                {
                    throw uve;
                }
            }
        }
        
        if (!design.getLogicalMemories().isEmpty())
        {
            // TBD LogicalMemory
            // mod |= mem.updateSizes();
            //System.err.println("Need constant prop support for LogicalMemory???");
        }

        for (Iterator iter = design.getPins().iterator(); iter.hasNext();)
        {
            mod |= visitPin((Pin)iter.next());
        }

        return mod;
    }

    public void visit (SimplePin pin)
    {
        // Ensure that the port/bus of each SimplePin in the fifo
        // interfaces as well as other 'simple pins' are correctly
        // propagated. 
        boolean pinMod;
        if (isForward())
        {
            pinMod = pin.propagateValuesForward();
        }
        else
        {
            pinMod = pin.propagateValuesBackward();
        }
        this.modified |= pinMod;
    }
    
    private boolean visitPin (Pin pin)
    {
        boolean mod = false;
        
        if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "comp: " + pin.show(true));
        if (isForward())
        {
            boolean isMod = pin.propagateValuesForward();
            mod |= isMod;
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "\tfwd: " + pin.cpDebug(true) + " \tmodified " + mod);
        }
        else
        {
            boolean isMod = pin.propagateValuesBackward();
            mod |= isMod;
            if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "\trev: " + pin.cpDebug(true) + " \tmodified " + mod);
        }
        if (pin.getInPinBuf() != null && pin.getInPinBuf().getPhysicalComponent() != null)
        {
            Module phys = pin.getInPinBuf().getPhysicalComponent();
            //phys.accept(this);
            // XXX FIXME.  The pin bufs physical have no support in
            // the Visitor so we need to handle them specifically.
            // ICK!
            //visitGenericModule(phys);
            catchAll(phys);
        }
        if (pin.getOutPinBuf() != null && pin.getOutPinBuf().getPhysicalComponent() != null)
        {
            Module phys = pin.getOutPinBuf().getPhysicalComponent();
            //phys.accept(this);
            // XXX FIXME.  The pin bufs physical have no support in
            // the Visitor so we need to handle them specifically.
            // ICK!
            //visitGenericModule(phys);
            catchAll(phys);
        }
        return mod;
    }

    private void catchAll (Module comp)
    {
        if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "Module catchAll " + comp + " " + comp.getClass());
        preFilter(comp);
        traverse(comp);
        postFilter(comp);
    }
    private void catchAll (Component comp)
    {
        if (_optimize.db) _optimize.ln(_optimize.PARTIAL, "Component catchAll " + comp + " " + comp.getClass());
        preFilter(comp);
        traverse(comp);
        postFilter(comp);
    }
    
    
}// TwoPassPartialConstant
