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

package net.sf.openforge.optimize.memory;

import java.util.*;

import net.sf.openforge.lim.memory.*;

/**
 * PointerRetargetVis is an implementation of the MemoryVisitor that
 * is used to retarget {@link Pointer} LogicalValues from one Location
 * to a correlated Location.  This is necessary any time that the
 * structure of a memory is changed by new allocation of memory.
 *
 * <p>Created: Fri Oct  3 09:02:57 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: PointerRetargetVis.java 2 2005-06-09 20:00:48Z imiller $
 */
public class PointerRetargetVis extends DefaultMemoryVisitor implements MemoryVisitor
{
    private static final String _RCS_ = "$Rev: 2 $";

    private Map locMap;
    
    public PointerRetargetVis (Map oldLocToNewLoc)
    {
        this.locMap = oldLocToNewLoc;
    }

    /**
     * Visits the given {@link Pointer}, and retargets the
     * {@link Location} that it points to based on the values in the
     * location map with which this class was constructed.
     *
     * @param ptr a non-null {@link Pointer} object to be
     * retargetted.
     * @throws NullPointerException if ptr is null
     */
    public void visit (Pointer ptr)
    {
        super.visit(ptr);
        Location target = ptr.getTarget();

        if (target == null)
            return;
        
        boolean retarget = locMap.containsKey(target);
        do
        {
            target = target.getBaseLocation();
            if (locMap.containsKey(target))
                retarget = true;
        } while (target.getBaseLocation() != target);

        if (retarget)
        {
            Location newLoc = Variable.getCorrelatedLocation(this.locMap, ptr.getTarget());
            ptr.setTarget(newLoc);
        }
    }
    
    
}// PointerRetargetVis
