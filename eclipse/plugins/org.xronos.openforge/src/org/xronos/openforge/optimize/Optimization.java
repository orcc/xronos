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

package org.xronos.openforge.optimize;

import org.xronos.openforge.lim.Visitable;

/**
 * Implemented by all optimizations invoked by {@link Optimizer}.
 *
 * @version $Id: Optimization.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface Optimization
{
    /**
     * Applies this optimization to a given target.
     *
     * @param target the target on which to run this optimization
     */
    public void run (Visitable target);

    /**
     * Method called prior to performing the optimization, should use
     * Job (info, verbose, etc) to report to the user what action is
     * being performed.
     */
    public void preStatus ();

    /**
     * Method called after performing the optimization, should use
     * Job (info, verbose, etc) to report to the user the results
     * (if any) of running the optimization
     */
    public void postStatus ();
    
    /**
     * Should return true if the optimization modified the LIM
     * <b>and</b> that other optimizations in its grouping should be
     * re-run
     */
    public boolean didModify ();

    /**
     * The clear method is called after each complete visit to the
     * optimization and should free up as much memory as possible, and
     * reset any per run status gathering.
     */
    public void clear ();

    
}// Optimization
