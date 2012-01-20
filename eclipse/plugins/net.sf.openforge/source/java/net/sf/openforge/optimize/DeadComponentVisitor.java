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

package net.sf.openforge.optimize;


import java.util.*;

import net.sf.openforge.app.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.naming.*;

/**
 * DeadComponentVisitor is a visitor used to remove Components from
 * the LIM which have no logical dependents.  When accepting a Design,
 * this visitor will continue to visit that design until no more
 * modifications have been made.
 * <p>The initial version of this visitor may leave some modules in
 * place even though they do no productive work.  This is the case for
 * Branches, loops, switches, etc.  At some point we need to go back
 * in and make sure that we remove these modules when we can because
 * they will keep a higher level module from removing itself (such as
 * a procedure/call).
 *
 *
 * Created: Thu Jul 11 10:08:39 2002
 *
 * @author imiller
 * @version $Id: DeadComponentVisitor.java 44 2005-10-27 16:21:59Z imiller $
 */
public class DeadComponentVisitor extends ComponentSwapVisitor implements Optimization
{
    private static final String _RCS_ = "$Rev: 44 $";

    /** set of call's from each Task.  Don't remove these */
    private Set topLevelCalls = new HashSet(3);

    /** Blocks which are procedure bodies.  Their exits don't drive
     *  anything directly, but don't delete the connections on the
     *  inside. This is a map of Module => Procedure to facilitate
     *  determining if a module is a procedure body, but then having
     *  access to that procedure. */
    private Map procedureBlocks = new HashMap(3);

    /**
     * Removes unused memories from a given design, which normal
     * visiting does not do.
     *
     * @param design a <code>Design</code> whose unaccessed memories
     *          are to be removed
     * @return true if one or more memories were removed, false otherwise
     */
    public static boolean pruneMemories (Design design)
    {
        boolean isModified = false;

        // Remove any memory with no references to it.
        for (Iterator iter = (new HashSet(design.getLogicalMemories())).iterator(); iter.hasNext();)
        {
            LogicalMemory memory = (LogicalMemory)iter.next();
            if (memory.getLValues().isEmpty() && memory.getLocationConstants().isEmpty())
            {
            	EngineThread.getGenericJob().verbose("Removing: "+ID.showLogical(memory));
                design.removeMemory(memory);
                isModified = true;
            }
        }

        for (Iterator iter = (new HashSet(design.getRegisters())).iterator(); iter.hasNext();)
        {
            Register reg = (Register)iter.next();
            if (reg.getReferences().size() == 0)
            {
            	EngineThread.getGenericJob().verbose("Removing: "+ID.showLogical(reg));
                design.removeRegister(reg);
                isModified = true;
            }
        }

        return isModified;
    }

    /**
     * Applies this optimization to a given target.
     *
     * @param target the target on which to run this optimization
     */
    public void run (Visitable target)
    {
        target.accept(this);
    }

    /**
     * Accepts a Design to be pruned of LIM Components which have no
     * logical dependents.  The Design is iterated over until no more
     * nodes can be eliminated.
     *
     * @param design a 'Design' to traverse.
     */
    public void visit (Design design)
    {
        int i=0;
        do
        {
            if (_optimize.db) _optimize.ln("===============================================");
            if (_optimize.db) _optimize.ln("# Starting Dead Component Removal iteration " + (i++));
            if (_optimize.db) _optimize.ln("===============================================");
            reset();
            super.visit(design);
        }
        while(isModified());

        //if (_optimize.db) _optimize.d.launchGraph(design, "POST OPTIMIZE", Debug.GR_DEFAULT, false);
    }

    /**
     * Keeps track of the {@link Call} for each task so that we don't
     * remove a top level call.
     *
     * @param task a value of type 'Task'
     */
    public void visit (Task task)
    {
        topLevelCalls.add(task.getCall());
        super.visit(task);
    }

    /**
     *
     * Removes the branch and replaces it with a new block containing
     * the Decision and the executed branch iff the Decision evaluates
     * to a constant value, indicating that one branch will always be
     * taken to the exclusion of the other.  <b>Note:</b> This
     * simplification will not be executed until after at least 1 pass
     * of the Partial Constant Propagation since the determination of
     * whether one branch is always executed is made based off of the
     * Decision's {@link Not}'s result bus' value whose value is set
     * during constant propagation.
     *
     * @param branch a value of type 'Branch'
     */
    public void visit (Branch branch)
    {
        branch.getTrueBranch().setNonRemovable();
        branch.getFalseBranch().setNonRemovable();
        branch.getDecision().setNonRemovable();

        super.visit(branch);

        final Decision decision = branch.getDecision();

        final Bus decisionNot = decision.getNot().getResultBus();

        // Only removable if the 'Not' has a constant value AND there
        // is only one exit, and that exit is the DONE exit.  Also,
        // only remove (swap for an empty block) the non taken branch
        // if there are 2 entries on the branch output buffer.  If
        // there is only 1 entry on that outbuf that means that this
        // Branch has already been optimized and the entry already
        // removed.
        if (decisionNot.getValue() != null && decisionNot.getValue().isConstant() &&
            branch.getExits().size() == 1 && branch.getExit(Exit.DONE) != null
            && branch.getExit(Exit.DONE).getPeer().getEntries().size() == 2)
        {
            final Component executed; // The branch that is always taken
            final Component excluded; // The branch that is never taken
            if (decisionNot.getValue().getBit(0).isOn())
            {
                executed = branch.getFalseBranch();
                excluded = branch.getTrueBranch();
            }
            else
            {
                executed = branch.getTrueBranch();
                excluded = branch.getFalseBranch();
            }

            // Create a new empty block to put in place of the
            // 'deleted' side of the branch.
            final Block repBlock = new Block(Collections.EMPTY_LIST, false);
            repBlock.setNonRemovable();
            
            // Set up the correlations so that the done of the empty
            // block follows the connection of the done of the
            // replaced branch.  Ditto the GO port.
            final Exit excludedDone = excluded.getExit(Exit.DONE);
            final Exit repDone = repBlock.getExit(Exit.DONE);
            assert excludedDone != null && repDone != null;
            final Map pCor = Collections.singletonMap(excluded.getGoPort(), repBlock.getGoPort());
            final Map bCor = Collections.singletonMap(excludedDone.getDoneBus(), repDone.getDoneBus());
            final Map eCor = Collections.singletonMap(excludedDone, repDone);
            
            replaceConnections(pCor, bCor, eCor);
            
            // Now, in order to get rid of the Mux that merges the
            // data back together we must eliminate one of the 2
            // entries on the outbuf of the branch.  Eliminate the
            // one connected to the new replacement block (since
            // the connections have already been replaced).
            Entry excludedOBEntry=null;
            final OutBuf branchOutbuf = branch.getExit(Exit.DONE).getPeer();
            assert branchOutbuf.getEntries().size() <= 2;
            for (Iterator iter = branchOutbuf.getEntries().iterator(); iter.hasNext();)
            {
                Entry e = (Entry)iter.next();
                for (Iterator entryIter = e.getDependencies(branchOutbuf.getGoPort()).iterator(); entryIter.hasNext();)
                {
                    Dependency dep = (Dependency)entryIter.next();
                    if (dep.getLogicalBus().getOwner().getOwner() == repBlock)
                    {
                        assert excludedOBEntry == null : "Removed branch has multiple paths to exit";
                        excludedOBEntry = e;
                    }
                }
            }
            assert excludedOBEntry != null;
            branchOutbuf.removeEntry(excludedOBEntry);
            
            branch.replaceComponent(excluded, repBlock);

            // Process any Access components in the removed branch so
            // that they do not linger (by being stored in the Referent) 
            MemoryAccessCleaner.clean(excluded);
        }
    }
    
    /**
     * Used to remove any {@link MemoryAccesBlock MemoryAccessBlocks} that may
     * reside in a pruned module from their respective {@link LogicalMemory LogicalMemorys}.
     */
    private static class MemoryAccessCleaner extends DefaultVisitor
    {
        /** The set of MemoryAccessBlocks that are encountered during visiting. */
        private Set accesses = new HashSet();

        private Set registerAccesses = new HashSet();


        /**
         * Cleans up the MemoryAccessBlocks that are found when visiting
         * a given Visitable.
         *
         * @param target the target in which to look for memory accesses;
         *          when found, they will be removed from their memories
         */
        static void clean (Visitable target)
        {
            final MemoryAccessCleaner cleaner = new MemoryAccessCleaner();
            target.accept(cleaner);
            cleaner.clean();
        }

        private void clean ()
        {
            for (Iterator iter = accesses.iterator(); iter.hasNext();)
            {
                final MemoryAccessBlock access = (MemoryAccessBlock)iter.next();
                access.removeFromMemory();
            }

            for (Iterator iter = registerAccesses.iterator(); iter.hasNext();)
            {
                final Access access = (Access)iter.next();
                access.removeFromResource();
            }
        }

        public void visit (HeapRead heapRead)
        {
            accesses.add(heapRead);
        }

        public void visit (HeapWrite heapWrite)
        {
            accesses.add(heapWrite);
        }

        public void visit (AbsoluteMemoryRead absoluteMemoryRead)
        {
            accesses.add(absoluteMemoryRead);
        }

        public void visit (AbsoluteMemoryWrite absoluteMemoryWrite)
        {
            accesses.add(absoluteMemoryWrite);
        }
            
        public void visit (ArrayRead arrayRead)
        {
            accesses.add(arrayRead);
        }

        public void visit (ArrayWrite arrayWrite)
        {
            accesses.add(arrayWrite);
        }
            
        public void visit (RegisterRead registerRead)
        {
            registerAccesses.add(registerRead);
        }

        public void visit (RegisterWrite registerWrite)
        {
            registerAccesses.add(registerWrite);
        }
    }

    
    public void visit (TimingOp top)
    {
        top.setNonRemovable();
        super.visit(top);
    }
    
    /**
     * Implemented to keep the test block from being removed from the
     * decision.
     *
     * @param decision a value of type 'Decision'
     */
    public void visit (Decision decision)
    {
        decision.getTestBlock().setNonRemovable();
        super.visit(decision);
    }
    
    
    public void visit (Loop loop)
    {
        if(loop.getBody() != null)
        {
            loop.getBody().setNonRemovable();
            // We determine the output latency of a bounded loop by
            // including the latency of the loop body's body, so it
            // must have one, even if it does nothing.
            loop.getBody().getBody().setNonRemovable();
        }
        loop.getInitBlock().setNonRemovable();
        super.visit(loop);
    }

    public void visit (Switch swich)
    {
        visit((Block)swich);
    }

    /**
     * Removes NoOps since they are simply wire-throughs of control
     * and data.
     *
     * @param nop a value of type 'NoOp'
     */
    public void visit (NoOp nop)
    {
        Component owner=nop.getOwner();
        super.visit(nop);

        // The 'super' may have removed the NoOp if no-one listened to
        // it's output.  If so, it's owner will be null and we don't
        // need to do this step....  
        if (nop.getOwner() == null || nop.isNonRemovable())
        {
            return;
        }
        
        // No-Ops are just passthroughs.  Map the go port to each port
        // targetted by the NoOp's done.  Ditto the data path.
        assert nop.getExits().size() == 1 : "NoOp's are only to have 1 exit";
        Exit nopExit = nop.getExit();
        wireControlThrough(nop);

        assert nopExit.getDataBuses().size() == nop.getDataPorts().size() : "Expecting 1:1 correlation between data ports and buses on nop";
        for (Iterator busIter = nopExit.getDataBuses().iterator(),
             portIter = nop.getDataPorts().iterator();
             busIter.hasNext();)
        {
            shortCircuit((Port)portIter.next(),(Bus)busIter.next());
        }

        assert nop.getEntries().size()<=1;
        if (nop.getEntries().size() == 1)
        {
            Entry entry = (Entry)nop.getEntries().get(0);
            for (Iterator iter = new LinkedList(nopExit.getDrivenEntries()).iterator(); iter.hasNext();)
            {
                ((Entry)iter.next()).setDrivingExit(entry.getDrivingExit());
            }
        }

        // Now that the nop has been bypassed (by copying connections)
        // we can remove it.
        removeComponent(nop);
    }
    

    /**
     * Implemented to test any non-{@link Reference} nodes and remove
     * them if there are no logical dependencies on their data buses
     * or there are no dependencies attached to any of the data ports.
     *
     * @param o any 'Operation'
     */
    public void filter (Operation o)
    {
        super.filter(o);
        if (o instanceof Reference)
        {
            if (_optimize.db) _optimize.ln(_optimize.DEAD_CODE, "NOT testing Reference: " + o);
        }
        else
        {
            testAndRemove(o);
        }
    }

    /**
     * Will remove any {@link Primitive} which has no logical
     * dependents on any data {@link Port} or no dependents on any
     * data {@link Bus}
     *
     * @param p any 'Primitive'
     */
    public void filter (Primitive p)
    {
        testAndRemove(p);
    }

    
    /**
     * Marks the procedure body as non-removable.
     *
     * @param c a value of type 'Call'
     */
    public void preFilter (Call c)
    {
        procedureBlocks.put(c.getProcedure().getBody(), c.getProcedure());
        c.getProcedure().getBody().setNonRemovable();
        preFilterAny(c);
    }

    /**
     * Removes the Call if the called procedure contains no components
     * (other than in or out bufs) unless that call is one of the 'top
     * level calls' identified when visiting a {@link Task}.
     *
     * @param call a value of type 'Call'
     */
    public void filter (Call call)
    {
        super.filter(call);

        if (!topLevelCalls.contains(call) && !call.isNonRemovable())
        {
            Procedure proc = call.getProcedure();
            Block body = proc.getBody();
            // Can't use isRemovable(body) because that method looks
            // for only a DONE exit and procedure bodies have a return exit.
            if (body.getExits().size() == 1 &&
                body.getExit(Exit.RETURN) != null &&
                body.getEntries().size() <= 1 && // Procedure bodies have 0 entries.
                body.getComponents().size() == (body.getOutBufs().size() + 1))
            {
                for (Iterator iter = body.getBuses().iterator(); iter.hasNext();)
                {
                    Bus moduleBus = (Bus)iter.next();
                    Port modulePort = getModulePortForBus(moduleBus);
                    if (modulePort == null)
                    {
                        continue;
                    }
                    // We actually want to copy the port/bus from the
                    // call.  So....
                    Port callPort = call.getPortFromProcedurePort(modulePort);
                    Bus callBus = call.getBusFromProcedureBus(moduleBus);
                    assert callPort != null : "Call port not found";
                    assert callBus != null : "Call bus not found";
                    shortCircuit(callPort, callBus);
                }
                removeComponent(call);
            }
        }
    }
    
    /**
     * Visits an {@link ArrayRead}.  Skips preFilter'ing of the
     * component, but will remove the operation as an atomic component
     * if there are no consumers of the data bus.
     *
     * @param arrayRead the operation to visit
     */
    public void visit (ArrayRead arrayRead)
    {
        /*
         * Skip the call to preFilter(), which might rip out the ArrayRead's result Bus.
         * Later users will expect that Bus to be there, even if it isn't connected.
         */
        traverse(arrayRead);
        filter(arrayRead);
        testAndRemoveOffsetMemRead(arrayRead);
    }

    /**
     * Visits an {@link ArrayWrite}.  Skips preFilter'ing of the
     * component.
     *
     * @param arrayWrite the operation to visit
     */
    public void visit (ArrayWrite arrayWrite)
    {
        /*
         * Skip the call to preFilter(), which might rip out the
         * ArrayWrite's ValuePort or OffsetPort.
         */
        traverse(arrayWrite);
        filter(arrayWrite);
    }
    
    /**
     * Visits a {@link HeapRead}.  Skips preFilter'ing of the
     * component, but will remove the operation as an atomic component
     * if there are no consumers of the data bus.
     *
     * @param heapRead the operation to visit
     */
    public void visit (HeapRead heapRead)
    {
        traverse(heapRead);
        filter(heapRead);
        testAndRemoveOffsetMemRead(heapRead);
    }
    
    /**
     * Visits an {@link HeapWrite}.  Skips preFilter'ing of the
     * component.
     *
     * @param heapWrite the operation to visit
     */
    public void visit (HeapWrite heapWrite)
    {
        /*
         * Skip the call to preFilter(), which might rip out the
         * HeapWrite's ValuePort.
         */
        traverse(heapWrite);
        filter(heapWrite);
    }

    private void testAndRemoveOffsetMemRead (OffsetMemoryRead omr)
    {
        // If there are no listeners to the result bus, remove the
        // whole module in one shot.
        if (omr.getResultBus().getLogicalDependents().size() == 0 &&
            !omr.getResultBus().isConnected() && !omr.isNonRemovable())
        {
            // Wire the go of the read to anything that listens to the
            // done.
            wireControlThrough(omr);

            // Then delete any dependencies on the exit's done bus.
            for (Iterator doneBusIter = omr.getExit(Exit.DONE).getDoneBus().getLogicalDependents().iterator(); doneBusIter.hasNext();)
            {
                ((Dependency)doneBusIter.next()).zap();
            }
            removeComponent(omr);

            // This should remove the reference from the memory port.
            omr.removeFromMemory();
        }
    }

    public void visit (AbsoluteMemoryRead read)
    {
        // Dont pre-filter since we handle it specially.
        traverse(read);
        filter(read);
        if (read.getResultBus().getLogicalDependents().size() == 0 &&
            !read.getResultBus().isConnected() && !read.isNonRemovable())
        {
            wireControlThrough(read);
            
            // Then delete any dependencies on the exit's done bus.
            for (Iterator doneBusIter = read.getExit(Exit.DONE).getDoneBus().getLogicalDependents().iterator(); doneBusIter.hasNext();)
            {
                ((Dependency)doneBusIter.next()).zap();
            }
            removeComponent(read);

            // This should remove the reference from the memory port.
            read.removeFromMemory();
        }
    }

    public void visit (FifoAccess comp)
    {
        traverse(comp);
        filter(comp);
    }
    
    public void visit (FifoRead comp)
    {
        traverse(comp);
        filter(comp);
        Bus result = (Bus)comp.getExit(Exit.DONE).getDataBuses().get(0);
        if ((result.getLogicalDependents().size() == 0) &&
            !result.isConnected() && !comp.isNonRemovable())
        {
            wireControlThrough(comp);

            // Then delete any dependencies on the exit's done bus.
            for (Iterator doneBusIter = result.getOwner().getDoneBus().getLogicalDependents().iterator(); doneBusIter.hasNext();)
            {
                ((Dependency)doneBusIter.next()).zap();
            }
            
            removeComponent(comp);
        }
    }

    public void visit (FifoWrite comp)
    {
        traverse(comp);
        filter(comp);
        Port din = (Port)comp.getDataPorts().get(0);
        boolean used = din.isConnected();
        for (Iterator iter = comp.getEntries().iterator(); iter.hasNext() && !used;)
        {
            Entry entry = (Entry)iter.next();
            used |= entry.getDependencies(din).size() > 0;
        }
        if (!used && !comp.isNonRemovable())
        {
            wireControlThrough(comp);
            
            // Then delete any dependencies on the exit's done bus.
            for (Iterator doneBusIter = comp.getExit(Exit.DONE).getDoneBus().getLogicalDependents().iterator(); doneBusIter.hasNext();)
            {
                ((Dependency)doneBusIter.next()).zap();
            }
            
            removeComponent(comp);
        }
    }

    public void visit (SimplePinRead comp)
    {
        // Remove a pin read when there is nothing that is consuming
        // its result.
        if (!comp.isNonRemovable() && 
            !comp.getResultBus().isConnected() &&
            comp.getResultBus().getLogicalDependents().size() == 0)
        {
            wireControlThrough(comp);
            
            // Then delete any dependencies on the exit's done bus.
            for (Iterator doneBusIter = comp.getResultBus().getOwner().getDoneBus().getLogicalDependents().iterator(); doneBusIter.hasNext();)
            {
                ((Dependency)doneBusIter.next()).zap();
            }
            removeComponent(comp);
        }
    }
    
    public void visit (SimplePinWrite comp)
    {
        // Remove a pin write when there is nothing coming into its
        // data port.
        if (!comp.getDataPort().isConnected() && !comp.isNonRemovable())
        {
            boolean hasDep = false;
            for (Iterator iter = comp.getEntries().iterator(); iter.hasNext() && !hasDep;)
            {
                Entry entry = (Entry)iter.next();
                hasDep |= entry.getDependencies(comp.getDataPort()).size() > 0;
            }
            if (!hasDep)
            {
                wireControlThrough(comp);
            
                // Then delete any dependencies on the exit's done bus.
                for (Iterator doneBusIter = comp.getExit(Exit.DONE).getDoneBus().getLogicalDependents().iterator(); doneBusIter.hasNext();)
                {
                    ((Dependency)doneBusIter.next()).zap();
                }
                
                removeComponent(comp);
            }
        }
    }

    /**
     * Try to push lack of connectivity across module boundries,
     * removing any unused ports and buses on modules.  Special care
     * is taken with procedure bodies and top level calls.
     *
     * @param m a value of type 'Module'
     */
    public void preFilter (Module m)
    {
        super.preFilter(m);
        if (m instanceof Composable)
        {
            return;
        }

        
        if (!isRemovable(m) || m.isNonRemovable())
        {
            boolean topLevel = false;
            // Procedure blocks have no 'sink' for their buses
            // directly (only through calls) so special case them
            if (!procedureBlocks.containsKey(m))
            {
                for (Iterator exitIter = m.getExits().iterator(); exitIter.hasNext();)
                {
                    List buses = new ArrayList(((Exit)exitIter.next()).getDataBuses());
                    for (Iterator busIter = buses.iterator(); busIter.hasNext();)
                    {
                        Bus bus = (Bus)busIter.next();
                        if (bus.getLogicalDependents().size() == 0 && !bus.isConnected())
                        {
                            if (_optimize.db) _optimize.ln(_optimize.DEAD_CODE, "Module output bus " + bus + " has no dependents");
                            removeBus(bus);
                        }
                    }
                }
            }
            else // Procedures...
            {
                // Make sure it isn't a top level call.  All top level
                // calls need all ports/buses.
                Procedure proc = (Procedure)procedureBlocks.get(m);
                Set calls = new HashSet(proc.getCalls());
                topLevel = calls.removeAll(this.topLevelCalls);
                if (!topLevel)
                {
                    for (Iterator exitIter = proc.getBody().getExits().iterator(); exitIter.hasNext();)
                    {
                        Exit exit = (Exit)exitIter.next();
                        List buses = new ArrayList(exit.getDataBuses());
                        for (Iterator busIter = buses.iterator(); busIter.hasNext();)
                        {
                            Bus procBus = (Bus)busIter.next();
                            int depCount = 0;
                            for (Iterator callIter = proc.getCalls().iterator(); callIter.hasNext();)
                            {
                                Call call = (Call)callIter.next();
                                Bus callBus = call.getBusFromProcedureBus(procBus);
                                depCount += callBus.getLogicalDependents().size();
                                if (callBus.isConnected())
                                {
                                    // Consider it a dependency if the
                                    // bus is connected!
                                    depCount++;
                                }
                            }
                            if (depCount == 0)
                            {
                                // removeBus takes care of removing the
                                // bus from all Calls since this is a
                                // procedure.
                                removeBus(procBus);
                            }
                        }
                    }
                }
            }

            // We don't want to remove ports from a top level call.
            if (!topLevel)
            {
                // If an inbuf bus isn't used remove the port.  removeBus
                // will take care of the case where this is a procedure
                // input and remove the port from all calls.
                List buses = new ArrayList(m.getInBuf().getDataBuses());
                for (Iterator busIter = buses.iterator(); busIter.hasNext();)
                {
                    Bus bus = (Bus)busIter.next();
                    if (bus.getLogicalDependents().size() == 0 && !bus.isConnected())
                    {
                        if (_optimize.db) _optimize.ln(_optimize.DEAD_CODE, "Module inbuf bus " + bus + " has no dependents");
                        removeBus(bus);
                    }
                }
            }
        }
    }

    /**
     * Any module that contains nothing but In/OutBufs will be
     * eliminated unless it is a procedure body {@link Block}
     *
     * @param module a value of type 'Module'
     */
    public void filter (Module module)
    {
        super.filter(module);
        if (!module.isNonRemovable() && isRemovable(module))
        {
            remove(module);
        }
    }

    protected void reset ()
    {
        super.reset();
        this.procedureBlocks.clear();
        this.topLevelCalls.clear();
    }
    

    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////

    
    /**
     * Returns true if the module can be removed (because it performs
     * no actual functionality beyond a wire through of inputs to
     * outputs).
     *
     * @param module a value of type 'Module'
     * @return a value of type 'boolean'
     */
    private static boolean isRemovable (Module module)
    {
        //
        // If you make a change to these criteria, then you need to
        // update the criteria in filter (Call) as well.
        //
        
        /*
         * The Module can't be removed if it is needed to generate branch control
         * signals.  This is indicated by the presence of one or more Exits
         * with tags other than DONE (e.g., RETURN, BREAK, CONTINUE).
         *
         * XXX: We need a more sophisticated approach, since this will prevent
         * Calls to empty Procedures from being removed (since the Procedure
         * body Block will have a RETURN Exit).
         */
        if ((module.getExits().size() > 1) || (module.getExit(Exit.DONE) == null))
        {
            if (_optimize.db) _optimize.ln(_optimize.DEAD_CODE, "Module unremovable. exit count " + module.getExits().size() + " done exit is " + module.getExit(Exit.DONE));
            return false;
        }

        /**
         * If there are multiple entries on this module don't remove
         * it.
         *
         * XXX: Here too we need a better approach to determine
         * whether we can figure out how to wire the control through
         * even though there are multiple dependencies.
         */
        if (module.getEntries().size() > 1)
        {
            if (_optimize.db) _optimize.ln(_optimize.DEAD_CODE, "Module unremovable. entry count " + module.getEntries().size());
            return false;
        }
        

        /*
         * We can remove iff there's nothing left but OutBufs and the InBuf.
         */
        return module.getComponents().size() == module.getOutBufs().size() + 1;
    }

    /**
     * Removes the given module after reconnecting any data or control
     * flow which 'percolates' through the module.
     *
     * @param module a value of type 'Module'
     */
    private void remove (Module module)
    {
        // Now that the connections are copied, remove the module.  If
        // a module is the body of a procedure so we don't remove it.
        if(module.isNonRemovable())
        {
            return;
        }

        //
        // Remove the module, but first figure out what data flows
        // exist through the module and re-create those flows
        // without going through the module.            
        //
        for (Iterator iter = module.getExits().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            for (Iterator busIter = exit.getBuses().iterator(); busIter.hasNext();)
            {
                Bus bus = (Bus)busIter.next();
                // Each bus has a peer port.  That port
                // should/must have exactly 1 entry and 1 logical
                // dependency otherwise this module isn't
                // removable.
                Port modulePort = getModulePortForBus(bus);
                if (modulePort == null)
                {
                    continue;
                }
                
                assert modulePort != null : "Module inbuf bus has no peer";
                shortCircuit(modulePort, bus);
            }
        }
        
        removeComponent(module);
    }
    
    /**
     * Analyzes each data bus of the given {@link Component} to see if
     * there are any logical or structural dependents, or any physical
     * connections to that bus and if none exist, the Component is
     * removed from the LIM.
     *
     * @param c a value of type 'Component'
     */
    private void testAndRemove (Component c)
    {
        if(c instanceof TimingOp)
            return;

        if (c.isNonRemovable())
            return;

        if (_optimize.db) _optimize.ln(_optimize.DEAD_CODE, "Examining " + c.toString());;
        boolean dataBusUsed = isDataBusUsed(c);//false;

        // dataPortUsed is initialized to true if component has 0 ports.
        boolean dataPortUsed = (c.getDataPorts().size() == 0);
        for (Iterator entryIter = c.getEntries().iterator(); entryIter.hasNext() && !dataPortUsed;)
        {
            Entry entry = (Entry)entryIter.next();
            for (Iterator portIter = c.getDataPorts().iterator(); portIter.hasNext();)
            {
                Port port = (Port)portIter.next();
                // Assume that if it has a dependency that the logical
                // bus is not null....
                if (entry.getDependencies(port).size() > 0)
                {
                    dataPortUsed = true;
                    break;
                }
            }
        }

        /*
         * Check for Bus connections, too.
         */
        if (!dataPortUsed)
        {
            for (Iterator iter = c.getDataPorts().iterator(); iter.hasNext();)
            {
                if (((Port)iter.next()).isConnected())
                {
                    dataPortUsed = true;
                    break;
                }
            }
        }

        // if we have no output or input 
        if (!dataBusUsed || !dataPortUsed)
        {            
            wireControlThrough(c);
            removeComponent(c);
        }
    }

    /**
     * Returns true if there are any dependencies for any data bus of
     * the given component or any of its data buses are connected.
     *
     * @param c the {@link Component} to test.
     * @return true if any data bus is used in any way.
     */
    private static boolean isDataBusUsed (Component c)
    {
        for (Iterator exitIter = c.getExits().iterator(); exitIter.hasNext();)
        {
            Exit exit = (Exit)exitIter.next();
            for (Iterator busIter = exit.getDataBuses().iterator(); busIter.hasNext();)
            {
                Bus bus = (Bus)busIter.next();
                if ((bus.getLogicalDependents().size() > 0) || bus.isConnected())
                {
                    return true;
                }
            }
        }
        return false;
    }
    

    /**
     * This method is used to remove a Bus which has a peer, including
     * module output buses, inbuf buses, procedure output buses, and
     * procedure inbufs.
     *
     * @param bus a value of type 'Bus'
     */
    private void removeBus (Bus bus)
    {
        Port peer = bus.getPeer();
        Component owner = peer.getOwner();
        assert owner != null : "This method only meant to be used for buses with peers.";
        for (Iterator entryIter = owner.getEntries().iterator(); entryIter.hasNext();)
        {
            Entry entry = (Entry)entryIter.next();
            for (Iterator depIter = (new ArrayList(entry.getDependencies(peer))).iterator(); depIter.hasNext();)
            {
                ((Dependency)depIter.next()).zap();
            }
        }
        peer.setUsed(false);
        // This will remove Port from outbuf if a Module and port of
        // Module if bus is on an inbuf
        if (_optimize.db) _optimize.ln(_optimize.DEAD_CODE, "Removing Bus " + bus + " and peer " + bus.getPeer());
        bus.getOwner().getOwner().removeDataBus(bus);
        
        // If Port is input to a procedure, remove port from calls
        if (procedureBlocks.containsKey(owner))
        {
            Procedure proc = (Procedure)procedureBlocks.get(owner);
            for (Iterator callIter = proc.getCalls().iterator(); callIter.hasNext();)
            {
                Call call = (Call)callIter.next();
                Port callPort = call.getPortFromProcedurePort(peer);
                call.removeDataPort(callPort);
            }
        }

        // If bus is output from procedure, remove bus from calls
        if (procedureBlocks.containsKey(bus.getOwner().getOwner()))
        {
            Procedure proc = (Procedure)procedureBlocks.get(bus.getOwner().getOwner());
            for (Iterator callIter = proc.getCalls().iterator(); callIter.hasNext();)
            {
                Call call = (Call)callIter.next();
                Bus callBus = call.getBusFromProcedureBus(bus);
                call.removeDataBus(callBus);
            }
        }
        
        setModified(true);
    }    
    
    protected void removeComponent (Component c)
    {
        assert !c.isNonRemovable();
        
        // IDM 01/10/2005  Too much information is generated when
    	//reporting every removed node.  Also, when obfuscated, the
    	//reported information is non-usefull
        // (often just obfuscated class names)
    	//EngineThread.getGenericJob().verbose("Removing: "+ID.showGlobal(c));
        Set drivers = new HashSet();
        for (Iterator iter = c.getDataPorts().iterator(); iter.hasNext();)
        {
            drivers.addAll(getDrivers((Port)iter.next()));
        }
        if (removeComp(c))
        {
            if (c instanceof LocationConstant)
            {
                ((LocationConstant)c).getTarget().getLogicalMemory().removeLocationConstant((LocationConstant)c);
            }
            this.removedNodeCount++;
            this.removedNodeCountTotal++;
            setModified(true);
        }

        // Visit everything that drove what was removed.  This will
        // help us to remove the chain much faster.
        for (Iterator iter = drivers.iterator(); iter.hasNext();)
        {
            ((Visitable)iter.next()).accept(this);
        }
    }

    /**
     * Returns a Set of {@link Component}s that drive the specified
     * port directly if port.getBus() is not null or via dependencies
     * if port.getBus is null.
     *
     * @param port the port to find drivers of
     * @return a 'Set' of Components that drive the specified port.
     */
    private static Set getDrivers (Port port)
    {
        if (port.getBus() != null)
        {
            return Collections.singleton(port.getBus().getOwner().getOwner());
        }
        Set drivers = new HashSet();
        for (Iterator iter = port.getOwner().getEntries().iterator(); iter.hasNext();)
        {
            Entry entry = (Entry)iter.next();
            for (Iterator depIter = entry.getDependencies(port).iterator(); depIter.hasNext();)
            {
                Dependency dep = (Dependency)depIter.next();
                drivers.add(dep.getLogicalBus().getOwner().getOwner());
            }
        }
        return drivers;
    }

    /**
     * Traverses backwards through an empty module (contains
     * <b>only</b>) an inbuf and outbufs to find the module Port which
     * supplies the value for a given module Bus.
     *
     * @param bus a value of type 'Bus'
     * @return a value of type 'Port'
     */
    private static Port getModulePortForBus (Bus bus)
    {
        // Port of outbuf inside module
        Port peer = bus.getPeer();
        assert peer != null : "Module bus has no peer";
        return getModulePortForPort(peer);
    }

    /**
     * Returns the Port of the module which sources the specified port
     * (if any).  The given port must have a single logical dependency
     * which is from the inbuf.
     *
     * @param port a value of type 'Port'
     * @return a value of type 'Port'
     */
    private static Port getModulePortForPort (Port port)
    {
        assert port.getOwner().getEntries().size() == 1 : "Port can only have 1 entry to find module port for port";
        Entry entry = (Entry)port.getOwner().getEntries().get(0);
        Collection dependencies = entry.getDependencies(port);
        
        if (dependencies.size() == 0)
        {
            return null;
        }
        
        assert dependencies.size() == 1 : "Must be only 1 dependency for finding module port for port. Found " + dependencies.size() + " " + dependencies;
        // Dependency for outbuf port
        Dependency dep = (Dependency)dependencies.iterator().next();
        // Bus driving outbuf.  Must be from inbuf
        Bus inBufBus = dep.getLogicalBus();
        assert inBufBus.getOwner().getOwner() instanceof InBuf : "Source of connection to port must be inbuf for finding module port from port";
        // Port on the module which is, ultimately, directly
        // connected to module output bus.
        return inBufBus.getPeer();
    }
    
    /**
     * Reports, via {@link Job#info}, what optimization is being
     * performed
     */
    public void preStatus ()
    {
    	EngineThread.getGenericJob().info("pruning dead code...");
    }
    
    /**
     * Reports, via {@link Job#verbose}, the results of <b>this</b>
     * pass of the optimization.
     */
    public void postStatus ()
    {
    	EngineThread.getGenericJob().verbose("pruned " + getRemovedNodeCount() + " expressions");
    }
    
}// DeadComponentVisitor
