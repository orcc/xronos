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

package net.sf.openforge.schedule;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.naming.*;

/**
 * This visitor visits the entire design and keeps track of which
 * components are timing ops. These include both explicit
 * {@link TimingOp TimingOps} and anything which can reach a timing op
 * (modules which contain a TimingOp).  This establishes the
 * characteristic 'hourglass' shape to the dependencies, in which a
 * TimingOp depends on everything before it completing, while
 * everything which follows a TimingOp depends on that op completing.
 * 
 * @version $Id: TimingSequencer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class TimingSequencer extends FilteredVisitor
{
    private static final String _RCS_ = "$Rev: 2 $";

    /**
     * The idea here is all a component has to do is set
     * tracker.found=true during it's visit method and it will get
     * caught and bubbled up appropriately
     */
    private TimingTracker globalTracker=new TimingTracker();
    private TimingTracker localTracker=new TimingTracker();
    
    public TimingSequencer ()
    {
        super();
    }

    /**
     *
     * This is called prior to each Module being traversed so that we
     * can set the 'timing op found' flag in the tracker to false,
     * thus allowing us to detect if the module contains a timing op.
     *
     * @param c a value of type 'Module'
     */
    public void preFilter(Module c)
    {
        if(_schedule.db)
        { 
            if(c instanceof Module)
            {
                _schedule.d.inc();
            }
            _schedule.d.ln("Entering: "+ID.showLogical(c)+" "+globalTracker.isTiming(c)); 
        }
        super.preFilter(c);
        globalTracker.enterModule();
        localTracker.enterModule();
    }
    
    /**
     *
     * This method is called after processing the internals of a
     * Module in order to mark the module as being a timing op if it
     * contains any timing ops.  By doing this the Module will cause
     * all control to merge before execution and any components after
     * the module will depend on its completion.
     *
     * @param c a value of type 'Module'
     */
    public void filter(Module c)
    {
        super.filter(c);

        globalTracker.exitModule(c);
        localTracker.exitModule(c);
        
        if(_schedule.db)
        {
            _schedule.d.ln("Leaving: "+ID.showLogical(c)+" "+globalTracker.isTiming(c)); 
            if(c instanceof Module)
            {
                _schedule.d.dec();
            }
        }
    }

    /**
     * Ensures that if a called procedure contains a waitclock that
     * the Call reports that it is a TimingOp within its context.
     */
    public void filter (Call c)
    {
        super.filter(c);
        if (globalTracker.isTiming(c.getProcedure().getBody()))
        {
            globalTracker.found(c);
        }
    }
    
    /**
     * Visit the block
     *
     * @param block a value of type 'Block'
     */
    public void visit (Block block)
    {
        preFilter(block);
        traverse(block);

        /*
         * A Block is the only context (other than Tasks) in which the contents may
         * execute in parallel, so this is where resource dependencies may need to
         * be added.
         */

        Component lastTimingOp=null; // last timing op found in the sequence
        ArrayList sinceLastTimingOp=new ArrayList(); // all ops since last timing op
        
        /*
         * Travese the sequence of Components.
         */
        for (Iterator iter = block.getSequence().iterator(); iter.hasNext();)
        {
            Component component = (Component)iter.next();
            if(_schedule.db) { _schedule.d.ln("-- "+ID.showLogical(component)); }

            // if we have already hit a timing op, then create a
            // control dependency from the last timingops done to the
            // current compoenents go
            if(lastTimingOp!=null)
            {
                Bus timingDoneBus = lastTimingOp.getExit(Exit.DONE).getDoneBus();
                WaitDependency dep = new WaitDependency(timingDoneBus);
                
                assert component.getEntries().size() == 1 : "Entry count " + component.getEntries().size() + " (expecting 1)";
                    
                Entry entry = (Entry)component.getEntries().get(0);

                entry.addDependency(component.getGoPort(), dep);
                if(_schedule.db) { _schedule.d.ln("@Added dependency on last timing op's donebus for component: "+ID.showLogical(component)); }
            }

            // Is it a timing op? ... yes
            boolean timingFound=false;
            if(globalTracker.isTiming(component))
            {
                //
                // hey! we found one! record it so that this block get's marked as a timing op
                //
                globalTracker.found=true;
                timingFound=true;
            }
            else if(localTracker.isTiming(component))
            {
                //
                // hey! we found one! record it so that this block get's marked as a timing op
                //
                localTracker.found=true;
                timingFound=true;
            }
            
            if(timingFound)
            {
                if(_schedule.db) { _schedule.d.ln("Found timing op: "+ID.showLogical(component)); }
                
                // here we make our go dependent on evryone since the last timing op's done
                for(Iterator it=sinceLastTimingOp.iterator();it.hasNext();)
                {
                    Component c=(Component)it.next();

                    // now set our done to be dependnet on the timing op
                    Bus timingDoneBus = c.getExit(Exit.DONE).getDoneBus();
                    WaitDependency dep = new WaitDependency(timingDoneBus);
                    
                    assert component.getEntries().size() == 1 : "Entry count " + component.getEntries().size() + " (expecting 1)";
                    
                    Entry entry = (Entry)component.getEntries().get(0);
                    
                    entry.addDependency(component.getGoPort(), dep);
                    if(_schedule.db) { _schedule.d.ln("@Added dependency for timing op: "+ID.showLogical(component)+" on timing Done Bus of: "+ID.showLogical(c)); }
                }
                
                // save off the onse we hit ... clear out our queue of non timing ops...
                sinceLastTimingOp.clear();
                sinceLastTimingOp.add(component);

                // this is now the last timing op
                lastTimingOp=component;
            }
            else
            {
                // record this to the list of compoenents we have seen
                sinceLastTimingOp.add(component);
            }
        }

        filter(block);
    }

    public void visit (TimingOp timingOp)
    {
        super.visit(timingOp);
        if(timingOp.isGlobal())
        {
            globalTracker.found(timingOp);
        }
        else
        {
            localTracker.found(timingOp);
        }
    }

    final static class TimingTracker
    {
        private HashSet timingOps=new HashSet();
        public boolean found=false;
        
        boolean isTiming(Component c)
        {
            return timingOps.contains(c);
        }

        void found(Component c)
        {
            found=true;
            timingOps.add(c);
        }
        
        void enterModule()
        {
            found=false;
        }

        /**
         * Adds the specified Module as a timing op if the 'found'
         * state is true.  This works only if 'enterModule' is called
         * before entering the module, and this method is called
         * immediately after processing the module.
         *
         * @param c a value of type 'Module'
         */
        void exitModule(Module c)
        {
            if(found)
                timingOps.add(c);
        }
            
    }
    
}

