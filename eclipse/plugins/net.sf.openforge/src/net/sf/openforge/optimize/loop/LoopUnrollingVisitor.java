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

package net.sf.openforge.optimize.loop;

import java.util.*;

import net.sf.openforge.app.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.optimize.*;
import net.sf.openforge.optimize.memory.*;
import net.sf.openforge.util.*;


/**
 * LoopUnrollingVisitor analyzes each loop in the LIM and determines if it is
 * unrollable, and if so, annotates is with the information necessary for unrolling.
 * Later the Loop.unroll() method can be called to unroll this loop (ie: during
 * optimization or scheduling)
 *
 * @author Jim Jensen
 * @version $Id: LoopUnrollingVisitor.java 558 2008-03-14 14:14:48Z imiller $
 */
public class LoopUnrollingVisitor extends MatchingVisitor implements Optimization
{
    private static final String _RCS_ = "$Rev: 558 $";

    private int passCount=0;

    private boolean isModified = false;
    private int unrolledLoopCount = 0;
    private LoopUnrollingEngine engine=new LoopUnrollingEngine();

    public LoopUnrollingVisitor()
    {
        super(FIFO); // FIFO
    }

    public void run (Visitable target)
    {
        assert target instanceof Design; // why is this so?

        if (_optimize.db) Debug.depGraphTo(target,"Loop Unrolling","lu-before"+passCount+".dot",Debug.GR_DEFAULT);
        if (_optimize.db) _optimize.d.ln(_optimize.LOOP_UNROLLING,"======================================");
        if (_optimize.db) _optimize.d.ln(_optimize.LOOP_UNROLLING,"# Starting loop unrolling");
        if (_optimize.db) _optimize.d.ln(_optimize.LOOP_UNROLLING,"======================================");

        ObjectResolver.resolve((Design)target);

        clear();

        // visit the design; find all the matchings
        target.accept(this);

        // unroll the found loops
        unrollLoops();
        
        if(isModified)
        {
            if (_optimize.db) Debug.depGraphTo(target,"Loop Unrolling","lu-after"+passCount+".dot",Debug.GR_DEPENDENCY);
        }

        passCount++;
    }

    public void preStatus ()
    {
    	EngineThread.getGenericJob().info("looking for unrollable loops...");
    }

    public void postStatus ()
    {
    	EngineThread.getGenericJob().verbose("unrolled " + unrolledLoopCount + " loops");
    }
    
    public boolean didModify ()
    {
        return isModified;
    }

    public int getNumberUnrolledLoops ()
    {
        return this.unrolledLoopCount;
    }

    public boolean didUnroll ()
    {
        return this.unrolledLoopCount > 0;
    }
    
    public void clear ()
    {
        this.unrolledLoopCount = 0;
        this.isModified = false;
        super.clear();
    }

    private void unrollLoops()
    {
        if (_optimize.db) _optimize.d.ln(_optimize.LOOP_UNROLLING,"Actually unrolling");
        _optimize.d.inc();
        // for each loop to be unrolled
        for(Iterator it=getMatchingNodes().iterator();it.hasNext();)
        {
            Loop l=(Loop)it.next();
            if (_optimize.db) _optimize.d.ln(_optimize.LOOP_UNROLLING,"Unrolling Loop: "+l);
            engine.unroll(l);
            unrolledLoopCount++;
            isModified = true;
        }
        _optimize.d.dec();
    }
    
    public void visit(Loop loop)
    {
        // this means inner loops will be unrolled first, since th matches are
        // processed in fifo order

        super.visit(loop);

        // analysis
        final LoopAnalysis loopAnalysis = new LoopAnalysis(loop);
        
        // enabled?
        if(loop.isLoopUnrollingEnabled())
        {        	
            if (loopAnalysis.isUnrollable())
            {
                final int iterations = loopAnalysis.getIterationCount();
                if (iterations < loop.getUnrollLimit())
                {
                    if (_optimize.db) _optimize.d.ln(_optimize.LOOP_UNROLLING,"Found loop to unroll");
                    addMatchingNode(loop);
                }
            }
            else
            {
                if (_optimize.db) _optimize.d.ln(_optimize.LOOP_UNROLLING, "Unrollable reason: " + loopAnalysis.toString());
            }
        }
    }
}

        


