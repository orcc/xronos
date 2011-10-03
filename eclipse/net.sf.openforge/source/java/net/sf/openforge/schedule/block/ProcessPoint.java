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

package net.sf.openforge.schedule.block;

import java.util.*;

import net.sf.openforge.lim.*;

/**
 * ProcessPoint is one endpoint of a block oriented process in the
 * LIM.  It is characterized by a Component (or several components if
 * they are in multiple mutually exclusive paths) which define the
 * reason for the endpoint (which should be either a first or last
 * access to a resource) as well as an appropriate context for that
 * endpoint.
 * For example, an endpoint which exists inside a loop body must use
 * that loop as its context (since the LIM is not done with that
 * component until the loop itself is completed) unless all the
 * endpoints exist within that loop.  Thus the context for the process
 * (defined as the module from which ALL endpoints are reachable by a
 * downward traversal) is used to find the containing module for the
 * endpoints defined in this process point.  It is that containing
 * module (or the endpoint itself) that is the critical context for
 * this endpoint.
 * <p>ProcessPoint is an immutable class in which the defined endpoint
 * and containing context are both set during construction and may not
 * be modified.
 *
 *
 * <p>Created: Wed Sep  8 16:47:54 2004
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ProcessPoint.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ProcessPoint 
{
    private static final String _RCS_ = "$Rev: 2 $";

    /** The component that defines the endpoint */
    private final Set criticalPoints;
    /** The containing component that defines the highest level
     * (significant) context for the critical point.  eg, containing
     * loop, branch, etc.*/
    private final Component criticalContext;
    
    /**
     * Constructs an instance of this class, representing one endpoint
     * of a schedulable process.  This process point is defined by the
     * collection of endpoints and the schedulable context of those
     * endpoints.  The context is the containing module (or the
     * endpoint itself) which exists at the same hierarchical level as
     * all other endpoints for the given process.  Thus the
     * processContext is the common module from which all these
     * endpoints descend  from.  
     *
     * @param criticalPoints a Collection of Component objects.  All
     * the Components in criticalPoints must be reachable from the
     * processContext (in a downward traversal).
     * @param processContext a Component which defines the containing
     * module from which all endpoints of the process descend.  This
     * is the 'deepest' module that contains all endpoints for a given
     * process. 
     */
    public ProcessPoint (Collection criticalPoints, Component processContext)
    {
        if (criticalPoints == null)
        {
            throw new IllegalArgumentException("Process endpoint cannot be null.");
        }
        if (criticalPoints.contains(null))
        {
            throw new IllegalArgumentException("Process endpoint cannot be specified by null.");
        }
        if (processContext == null)
        {
            throw new IllegalArgumentException("Process context cannot be null.");
        }
        
        this.criticalPoints = Collections.unmodifiableSet(new HashSet(criticalPoints));
        Component critContext = null;
        for (Iterator iter = this.criticalPoints.iterator(); iter.hasNext();)
        {
            Component nextComp = (Component)iter.next();
            final Component context = findCriticalContext(nextComp, processContext);
            if (critContext == null)
            {
                critContext = context;
            }
            assert critContext == context : "All common endpoints must share same critical context";
        }
        this.criticalContext = critContext;
    }
    
    private static Component findCriticalContext (Component criticalPoint, Component processContext)
    {
        // NOTE: This assumes that there are no Calls in the hierarchy
        Component container = criticalPoint;
        while (container != null)
        {
            if (container.getOwner() == processContext)
                return container;
            container = container.getOwner();
        }
        throw new IllegalEndpointException("End point does not appear in process context.");
    }
    
    /**
     * Get the component(s) which define this process endpoint.  This is
     * the actual identified set of endpoints and not the containing
     * context.
     *
     * @return a non-null Collection of Components
     */
    public Collection getPoints ()
    {
        return this.criticalPoints;
    }

    /**
     * Returns the significant context for the endpoint identified by
     * this instance.  This may be the outermost containing loop or
     * branch, or it may be the component itself if it is not
     * contained in any loop or branch.
     *
     * @return a non-null Component
     */
    public Component getCriticalContext ()
    {
        return this.criticalContext;
    }
    
    public String toString ()
    {
        String ret = super.toString();
        ret = ret.substring(ret.lastIndexOf(".")+1);
        return ret + " pts: " + this.criticalPoints + " ctxt: " + this.criticalContext;
    }

    static class IllegalEndpointException extends RuntimeException
    {
        public IllegalEndpointException (String msg)
        {
            super(msg);
        }
    }
    
}// ProcessPoint
