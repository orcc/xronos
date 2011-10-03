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

package net.sf.openforge.optimize.replace;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.optimize.Optimization;


/**
 * FloatBlackBoxVisitor is responsible for finding all floating point
 * operations (minus pin accesses) left in the LIM after operation
 * replacement has completed.  Any which are found will be replaced by
 * a call to a default implementation for that operation which may be
 * simply a black box to be implemented by the user in HDL.
 *
 * <p>Created: Wed Apr  2 12:04:09 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FloatBlackBoxVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class FloatBlackBoxVisitor extends ReplacementVisitor implements Optimization
{
    private static final String _RCS_ = "$Rev: 2 $";

    /**
     * Applies this optimization to a given target.
     *
     * @param target the target on which to run this optimization
     */
    public void run (Visitable target)
    {
        target.accept(this);
    }

    public void visit (Design des)
    {
//         if(_optimize.db) _optimize.d.modGraph(des, "dotDir");
        super.visit(des);
    }
    
    /**
     * Finds all floating point operations (ignoring PinAccess) and
     * throws a fatal error.  Eventually we will do a replacement here
     * with a black box.
     *
     * @param op a value of type 'Operation'
     */
    public void filter (Operation op)
    {
        super.filter(op);

        if (op instanceof Constant || op instanceof Access) return;

        if (op.isFloat() && !(op instanceof PinAccess))
        {
            ReplacementCorrelation correlation = ReplacementCorrelation.getCorrelation(op);
            String type = (correlation != null) ? correlation.getReplacedOperationDescription() : "unknown type: " + op;
            EngineThread.getEngine().fatalError("Non replaced floating point operation found.  " +
                "All floating point operations must be replaced: " + type);
//             Job.error("Non replaced floating point operation found.  All floating point operations must be replaced: " + op);
//             if (ReplacementCorrelation.isReplaceable(op))
//             {
//                 Call rep = getImplementationFromLibs(
//                     Collections.singletonList("DEFAULT_FLOAT.java"),
//                     op, false);
//                 if (rep == null)
//                 {
//                     Job.fatalError("Illegal state.  Replaceable floating point operation has no default implementation");
//                 }
//                 replace(op, rep);
//             }
//             else
//             {
//                 Job.fatalError("Illegal state.  Non replaceable floating point operation " + op);
//             }
        }
    }
    
    /**
     * Reports, via {@link Job#info}, what optimization is being
     * performed
     */
    public void preStatus ()
    {
        //Job.info("reducing expressions with constants...");
    }
    
    /**
     * Reports, via {@link Job#verbose}, the results of <b>this</b>
     * pass of the optimization.
     */
    public void postStatus ()
    {
        //Job.verbose("reduced " + getReplacedNodeCount() + " expressions");
    }
    
}// FloatBlackBoxVisitor
