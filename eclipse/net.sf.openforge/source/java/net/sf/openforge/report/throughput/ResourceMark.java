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
 * ResourceMark tracks 1 starting point for a given resource to all
 * possible end points for the longest existing path.  Generally this
 * will be a path from the start point to one end point.  However, if
 * the latencies of 2 end points cannot be compared, both are kept as
 * potential end points.  Once an instance of this class is created,
 * any access may be added as a potential end point and this class
 * will determine if it does indeed match the resource being tracked
 * (as defined by the type of the starting point).  Only complementary
 * accesses are kept.  ie if the base access is a register read, then
 * only register write's to the same register will be kept when
 * provided via the mark method.
 *
 * <p>Created: Tue Jan 21 12:22:05 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ResourceMark.java 88 2006-01-11 22:39:52Z imiller $
 */
public class ResourceMark 
{
    private static final String _RCS_ = "$Rev: 88 $";

    private AccessId base = null;
    private Latency baseLatency = null;

    /** A Map of AccessId (complementary access of the base +
     * location) to the Latency of that access.  AccessId->Latency */
    private Map latestComplement = new HashMap();

    /**
     * Constructs a new ResourceMark with the specified access as the
     * base access, and the given Latency of the base.
     *
     * @param base a value of type 'Access'
     * @param latency a value of type 'Latency'
     * @param location a value of type 'ID'
     */
    public ResourceMark (Access base, Latency latency, ID location)
    {
        this.base = new AccessId(base, location);
        this.baseLatency = latency;
    }

    /**
     * Marks the given access by adding it to the collection of latest
     * accesses iff it is the complement
     * ({@link Access#isComplementOf}) of the base access.  Then the
     * collection of complement accesses is pruned down to contain
     * only the latest accesses ({@link Latency#getLatest}).
     *
     * @param access a value of type 'Access'
     * @param lat a value of type 'Latency'
     * @param location a value of type 'ID'
     */
    public void mark (Access access, Latency lat, ID location)
    {
        assert false : "no longer support 'isComplementOf'";
        
//         if (!access.isComplementOf(base.getAccess()))
//         {
//             return;
//         }

        if (baseLatency.isGT(lat))
        {
//             System.out.println("ERROR!!! Access is earlier than base! ");
//             System.out.println("\tAcc: " + access + " lat " + lat + " in " + access.showOwners());
//             System.out.println("\tbase " + base.getAccess() + " base lat " + baseLatency + " in " + base.getAccess().showOwners());
            return;
        }

        this.latestComplement.put(new AccessId(access, location), lat);
        this.latestComplement = Latency.getLatest(latestComplement);
    }

    /**
     * Returns the Access from which this ResoruceMark was started.
     */
    public Access getBaseAccess ()
    {
        return this.base.getAccess();
    }
    
    /**
     * Returns the largest difference between a 'latest accessor' and
     * the base latency specified during construction or
     * {@link Task#INDETERMINATE_GO_SPACING} if any access has a
     * latency with {@link Latency#UNKNOWN} max number of clocks.
     *
     * @return the number of clock cycles between the base access and
     * the latest recorded access.
     */
    public int getLongestSpace ()
    {
        int largestSpace = -1;
        for (Iterator iter = this.latestComplement.keySet().iterator(); iter.hasNext();)
        {
            AccessId accessid = (AccessId)iter.next();
            Latency latency = (Latency)this.latestComplement.get(accessid);
            Access access = accessid.getAccess();
            if (latency.getMaxClocks() == Latency.UNKNOWN)
            {
                return Task.INDETERMINATE_GO_SPACING;
            }
            
            final int space = latency.getMaxClocks() - this.baseLatency.getMinClocks();
            largestSpace = Math.max(space, largestSpace);
        }
        
        return largestSpace;
    }

    /**
     * Prints a report of the paths tracked in this Resource Mark.
     * Generally there is only one path from 1st to last, but if there
     * are multiple 'lasts' that cannot be compared, then there may be
     * multiple end points, and each will be reported.  If there are
     * NO end points, no entry is made.
     *
     * @param ps a value of type 'PrintStream'
     */
    public void reportPaths (PrintStream ps)
    {
        if (this.latestComplement.isEmpty())
        {
            return;
        }

        IDSourceInfo baseInfo = this.base.getAccess().getIDSourceInfo();
        ps.println("  Path start point: " + this.base.getAccess().showIDLocation() +
            " in method/function " + this.base.getLocation().showIDLogical());

        for (Iterator iter = this.latestComplement.keySet().iterator(); iter.hasNext();)
        {
            AccessId id = (AccessId)iter.next();
            Latency latency = (Latency)this.latestComplement.get(id);
            IDSourceInfo accInfo = id.getAccess().getIDSourceInfo();
            ps.println("\tend point: " + id.getAccess().showIDLocation() +
                " in method/function " + id.getLocation().showIDLogical() +
                " length is: " + latDiff(this.baseLatency, latency));
        }
    }
    
    /**
     * Returns the number of end points stored in this mark.
     *
     * @return a value of type 'int'
     */
    public int getPathCount ()
    {
        return this.latestComplement.keySet().size();
    }

    /**
     * Generates a String from the 2 latencies indicating their
     * difference (end.max - base.min), of the form:
     * <ul>
     * <li><i>indeterminate clock cycles</i>
     * <li><i>1 clock cycle</i>
     * <li><i>2 clock cycles</i>
     * </ul>
     */
    public static String latDiff (Latency base, Latency end)
    {
        String length;
        String cycles = "cycles";
        if (end.getMaxClocks() == Latency.UNKNOWN || base.getMaxClocks() == Latency.UNKNOWN)
        {
            length = "indeterminate";
        }
        else
        {
            int diff = end.getMaxClocks() - base.getMinClocks();
            length = Integer.toString(diff);
            if (diff == 1)
            {
                cycles = "cycle";
            }
        }
        return length + " clock " + cycles;
    }
    
    static class AccessId
    {
        private Access access = null;
        private ID location = null;
        
        public AccessId (Access acc, ID loc)
        {
            this.access = acc;
            this.location = loc;
        }
        
        public Access getAccess ()
        {
            return this.access;
        }
        
        public ID getLocation ()
        {
            return this.location;
        }
    }
    
}// ResourceMark


