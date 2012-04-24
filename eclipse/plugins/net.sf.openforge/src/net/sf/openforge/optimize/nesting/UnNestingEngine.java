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

package net.sf.openforge.optimize.nesting;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.optimize.*;
import net.sf.openforge.util.*;

public class UnNestingEngine
{

    private Block deadBlock;
    private InBuf deadInBuf;
    private Exit lastExit;
    private int blkCount=0;
    
    //
    // ----- actual unnesting code starts here ----------
    //

    public void clear()
    {
        deadBlock=null;
        deadInBuf=null;
        lastExit=null;
    }
    
    public void unnest(Collection c)
    {
        _optimize.d.inc();
        for(Iterator it=c.iterator();it.hasNext();)
        {
            Block b=(Block)it.next();
            Block owner=(Block)b.getOwner();
            if (_optimize.db) Debug.depGraphTo(owner,"Block Unnesting","bu-nest-before"+blkCount+".dot",Debug.GR_DEFAULT);
            unnest(b);
            if (_optimize.db) Debug.depGraphTo(owner,"Block Unnesting","bu-nest-after"+blkCount+".dot",Debug.GR_DEFAULT);
            blkCount++;
        }             
        _optimize.d.dec();
    }
    
    /**
     * Unnest a single block into its parent block
     *
     * @param src a value of type 'Block'
     */
    public void unnest(Block b)
    {
        deadBlock=b;
        if(_optimize.db) _dbgln("Unnesting Block: "+deadBlock+" blk: "+blkCount);
        _optimize.d.inc();
        // first find the offset
        Block dest=(Block)deadBlock.getOwner();
        int offset=dest.getSequence().indexOf(deadBlock);
        assert(offset>=0);

        // get list of floating components
        Set components = new LinkedHashSet(deadBlock.getComponents());
        components.removeAll(deadBlock.getSequence());
        components.remove(deadBlock.getInBuf());
        components.removeAll(deadBlock.getOutBufs());

        // add the floating components to the parent
        dest.addComponents(components);

        // insert the sequence
        dest.insertComponents(deadBlock.getSequence(),offset);

        // add the sequence into the list
        components.addAll(deadBlock.getSequence());

        deadInBuf=deadBlock.getInBuf();
        
        // reconnect all the components in the parent block, except the block we got rid of
        for(Iterator it=dest.getComponents().iterator();it.hasNext();)
        {
            Component c=(Component)it.next();
            if(!c.equals(deadBlock))
            {
                connect(c);                
            }
        }

        // now remove the old block
        //
        deadBlock.getOwner().removeComponent(deadBlock);

        // disconnect
        deadBlock.disconnect();
        
        // clear driving exits in parent block
        dest.accept(new Exit.ClearDrivingExitVisitor());
        
        _optimize.d.dec();
    }

    private void connect(Component c)
    {
        if(_optimize.db) _dbgln("Connecting: "+c);
        _optimize.d.inc();
        // for each entry
        for(Iterator itEntries=c.getEntries().iterator();itEntries.hasNext();)
        {
            Entry e=(Entry)itEntries.next();
            if(_optimize.db) _dbgln("Has Entry: "+e);
            if(_optimize.db) _optimize.d.inc();
            // for entries' ports
            for(Iterator itPorts=e.getPorts().iterator();itPorts.hasNext();)
            {
                Port p=(Port)itPorts.next();
                if(_optimize.db) _dbgln("Has Port: "+p);
                connectPort(e,p);
            }
            if(_optimize.db) _optimize.d.dec();
        }

        _optimize.d.dec();
    }

    /**
     * This finds the bus that a given port should be connected to. If
     * it is in the deadblock, it reconnectes it, killing the old connection
     *
     * @param e a value of type 'Entry'
     * @param p a value of type 'Port'
     * @param deadBlock a value of type 'Block'
     * @return a value of type 'Bus'
     */
    private void connectPort(Entry currentEntry,Port p)
    {
        if(_optimize.db) _dbgln("Tracing for"+currentEntry+"/"+p);

        // find each bus this port currently depends on
        Collection deps = currentEntry.getDependencies(p);
        // for all deps
        for(Iterator itDeps=new ArrayList(deps).iterator();itDeps.hasNext();)
        {
            Dependency currDep=(Dependency)itDeps.next();

            Bus currBus=currDep.getLogicalBus();

            if(_optimize.db) _dbgln("\t** Owner: "+currBus.getOwner().getOwner());
            
            // if it connects to the old blocks inbuf, or outbuf
            if((currBus.getOwner().getOwner().equals(deadBlock.getInBuf()))||
                (currBus.getOwner().getOwner().equals(deadBlock)))
            {
                if(_optimize.db) _dbgln("\tdeadblock!!!!!!");

                // reconnect
                reconnect(currentEntry,currDep,currBus,p);
                
                // may now be connected to the inbuf. try again
                connectPort(currentEntry,p);
            }
        }
    }

    private void reconnect(Entry currentEntry,Dependency currDep,Bus currBus,Port p)
    {
        // connects to the old block stuff. Chase further
        // in this case, we want the bus that is connected to the peerport

        // get the peer port
        Port peerPort=currBus.getPeer();

        if(_optimize.db) _dbgln("\tbus: "+currBus+" ; skipping though: "+peerPort.getOwner());
        
        // better have 1 entry
        assert peerPort.getOwner().getEntries().size() == 1: "entries!=1 "+peerPort.getOwner().getEntries().size();
        Entry entry = (Entry)peerPort.getOwner().getEntries().get(0);
        // get the deps
        Collection deps = entry.getDependencies(peerPort);
        //for each dep
        for(Iterator itDeps=new ArrayList(deps).iterator();itDeps.hasNext();)
        {
            Dependency oldDep=(Dependency)itDeps.next();
            Bus newBus=oldDep.getLogicalBus();
            Dependency newDep = (Dependency)currDep.createSameType(newBus);
            currentEntry.addDependency(p, newDep);
            if(_optimize.db) _dbgln("\treconnected to: "+newBus+" :: "+newBus.getSize()+" owned: "+newBus.getOwner().getOwner());
        
            assert p.getBus()==null; // better not be structural flow 
        }
        currDep.zap();
    }

    private static final void _dbgln(Object o) 
    {
        _optimize.d.ln(_optimize.BLOCK_UNNEST,o);        
    }

    private static final void _dbg(Object o)
    {
        _optimize.d.o(_optimize.BLOCK_UNNEST,o);        
    }
    
    private static final void _dbgln()
    {
        _optimize.d.ln(_optimize.BLOCK_UNNEST);        
    }
    
}

