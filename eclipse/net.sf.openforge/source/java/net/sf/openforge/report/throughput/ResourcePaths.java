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

package net.sf.openforge.report.throughput;

import java.util.*;
import java.io.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.util.naming.*;

/**
 * This class collects up all the first->last paths that exist for a
 * given resource and manages marking each of those paths for each
 * access.  This class uses the ResourceMark class to ensure that the
 * 2 accesses are complementary.  This means that this class is
 * responsible for tracking the earliest -> latest path through which
 * the data storage represented by the resource can be modified.
 */
public class ResourcePaths implements ThroughputLimit
{
    private Set marks = new HashSet();
    private Resource resource;

    public ResourcePaths (Resource resource)
    {
        this.resource = resource;
    }

    /**
     * Marks the access by calling mark on any {@link ResourceMark}
     * for the same resource.  The input latency is used when marking
     * in any existing ResourceMarks.  The output latency is used when
     * constructing new ResourceMarks.  This is because the longest
     * path exists between the OUTPUT of the 1st access and the INPUT
     * of the last (complementary) access.
     *
     * @param access a value of type 'Access'
     * @param headLat a value of type 'Latency'
     * @param tailLat a value of type 'Latency'
     * @param location a value of type 'ID' identifying the location
     * of the access.
     */
    public void mark (Access access, Latency headLat, Latency tailLat, ID location)
    {
        boolean alreadyTracked = false;
        for (Iterator iter = marks.iterator(); iter.hasNext();)
        {
            ResourceMark mark = (ResourceMark)iter.next();
            mark.mark(access, tailLat, location);
            
            // We are already tracking it if it is NOT a complment of
            // the current mark.
            assert false : " no longer support 'iscomplementof'";
            
//             alreadyTracked |= !mark.getBaseAccess().isComplementOf(access);
        }
        if (!alreadyTracked)
        {
            marks.add(new ResourceMark(access, headLat, location));
        }
    }

    /**
     * Returns an unmodifiable Set of {@link ResourceMark} objects
     * representing the longest path between 2 or more complementary
     * accesses (earliest to latest) for the resource being tracked.
     */
    private Set getPaths ()
    {
        return Collections.unmodifiableSet(marks);
    }

    /**
     * Returns the longest 'space' indicated by any path tracked here.
     */
    public int getLimit ()
    {
        int longestPath = 0;
        for (Iterator pathIter = getPaths().iterator(); pathIter.hasNext();)
        {
            ResourceMark mark = (ResourceMark)pathIter.next();
            longestPath = Math.max(longestPath, mark.getLongestSpace());
        }
        return longestPath;
    }

    public void writeReport (PrintStream ps, int tabDepth)
    {
        boolean writeIt = false;
        for (Iterator iter = getPaths().iterator(); iter.hasNext() && !writeIt;)
        {
            if (((ResourceMark)iter.next()).getPathCount() > 0)
            {
                writeIt = true;
            }
        }
        if (!writeIt)
        {
            return;
        }
        
        ps.println("Resource: \"" + resource.showIDLogical() + "\"");
        for (Iterator pathIter = getPaths().iterator(); pathIter.hasNext();)
        {
            ResourceMark mark = (ResourceMark)pathIter.next();
            mark.reportPaths(ps);
        }
    }

    public String toString ()
    {
        return "ResourcePaths for " + this.resource + " " + getPaths().size() + " paths";
    }
}

