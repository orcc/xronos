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

import net.sf.openforge.lim.*;


/**
 * Characterizes a {@link Loop} with respect to its unroll-ability.  This
 * class determines whether the loop is unrollable, as well as the number
 * of times it will iterate (regardless of whether it is unrollable).
 *
 * @version $Id: LoopAnalysis.java 2 2005-06-09 20:00:48Z imiller $
 */
class LoopAnalysis
{
    /** Revision */
    private static final String _RCS_ = "$Rev: 2 $";

    /** True if the loop can be unrolled, false otherwise. */
    private boolean isUnrollable = true;

    /** The number of iterations, if known, regardless of unrollability */
    private int iterationCount = Loop.ITERATIONS_UNKNOWN;

    private String whyNotUnrollable="";
    
    /**
     * Creates a new <code>LoopAnalysis</code> instance.  This constructor
     * has the side effect of calling {@link Loop#setIterations(int)} on
     * <code>loop</code> with the number of iterations it calculates.
     *
     * @param loop the loop to be analyzed
     */
    LoopAnalysis (Loop loop)
    {
        /*
         * Test whether the loop is still iterative, since it may
         * have already been unrolled in a previous iteration.
         */
        if (loop.isIterative())
        {
            try
            {

                if (hasExtraExits(loop))
                {
                    throw new LoopUnrollingException("contains multiple exits");
                }

                DecisionCircuit decisionCircuit = new DecisionCircuit(loop);
                this.iterationCount = decisionCircuit.getIterationCount();

                this.isUnrollable = (iterationCount != Loop.ITERATIONS_UNKNOWN);
                if (this.isUnrollable)
                {
                    whyNotUnrollable="loop iterations are unknown";
                }
            }
            catch (LoopUnrollingException e)
            {
                this.isUnrollable = false;
                this.whyNotUnrollable=e.toString();
            }
        }
        else
        {
            this.isUnrollable = false;
            this.whyNotUnrollable="not iterative";
            this.iterationCount = 0;
        }

        loop.setIterations(iterationCount);
    }

    /**
     * Tests whether the loop was found to be unrollable or not.
     *
     * @return true if the loop can be unrolled, false otherwise
     */
    boolean isUnrollable ()
    {
        return isUnrollable;
    }

    /**
     * Gets the number of times the loop will iterate, regardless of whether
     * it can be unrolled or not.
     *
     * @return the non-negative number of iterations, or {@link Loop#ITERATIONS_UNKNOWN}
     *         if the bounds of the loop could not be determined
     */
    int getIterationCount ()
    {
        return iterationCount;
    }



    private static boolean hasExtraExits (Loop loop) throws LoopUnrollingException
    {
        final Component body = loop.getBody().getBody();
        if (body != null)
        {
            final Set exits = new HashSet(body.getExits());
            exits.remove(body.getExit(Exit.DONE));
            if (!exits.isEmpty())
            {
                return true;
            }
        }

        return false;
    }
    public String toString ()
    {
        if (isUnrollable)
        {
            return "Unrollable";
        }
        else
        {
            return whyNotUnrollable;
        }
    }
    
        
}
