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
package net.sf.openforge.lim;

import java.util.*;


/**
 * OrLatency consists of two or more Latencies that represent
 * a choice of possibilities.  Typically such a Latency would
 * result from the merging of control paths, each of which had
 * a different latency.
 * <P>
 * Each Latency is associated with a key Object that can differentiate
 * the use of that Latency when accessed as a component in
 * a different OrLatency.
 *
 * @author  Stephen Edwards
 * @version $Id: OrLatency.java 109 2006-02-24 18:10:34Z imiller $
 */
class OrLatency extends Latency implements Cloneable
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 109 $";

    /** Set of constituent Latency objects */
    private Set latencies;
    /** Cached state for the isOpen method.  Because latency objects
     * are immutable a latency will be either open or not open for its
     * lifetime */
    private boolean openState;

    public boolean isOpen ()
    {
        return this.openState;
    }

    public boolean isGT (Latency latency)
    {
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            Latency next = (Latency)iter.next();
            if (!next.isGT(latency))
            {
                return false;
            }
        }
        return true;
    }

    public boolean isGE (Latency latency)
    {
        if (this.equals(latency))
        {
            return true;
        }

        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            Latency next = (Latency)iter.next();
            if (!next.isGE(latency))
            {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns false, or latencies are never fixed.
     *
     * @return false
     */
    public boolean isFixed()
    {
        return false;
    }
    
    public Latency addTo (Latency latency)
    {
        Set newSet = new HashSet(latencies.size());
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            Latency next = (Latency)iter.next();
            newSet.add(next.addTo(latency));
        }
        return new OrLatency(newSet, getKey());
    }

    public boolean equals (Object object)
    {
        if (object == this)
        {
            return true;
        }
        else if (object instanceof OrLatency)
        {
            OrLatency latency = (OrLatency)object;
            return latencies.equals(latency.latencies) && getKey().equals(latency.getKey());
        }
        else
        {
            return false;
        }
    }

    public int hashCode ()
    {
        return latencies.hashCode() + getKey().hashCode();
    }

    boolean isDescendantOf (Latency latency)
    {
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            Latency next = (Latency)iter.next();
            if (!next.isDescendantOf(latency))
            {
                return false;
            }
        }
        return true;
    }

    protected Latency increment (int minClocks, int maxClocks)
    {
        Set newSet = new HashSet(latencies.size());
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            Latency next = (Latency)iter.next();
            newSet.add(next.increment(minClocks, maxClocks));
        }
        return new OrLatency(newSet, getKey());
    }

    protected Latency increment (int minClocks, LatencyKey key)
    {
        Set newSet = new HashSet(latencies.size());
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            Latency next = (Latency)iter.next();
            newSet.add(next.increment(minClocks, key));
        }
        // The key for the OrLatency is the current key.  The 'open'
        // key is factored into each of the constituent latencies
        return new OrLatency(newSet, getKey());
    }

    OrLatency (Set lats, LatencyKey key)
    {
        super(getMinClocks(lats),
            getMaxClocks(lats), key);
        
        this.latencies = flatten(lats);
        
        // Cache the 'open' state
        this.openState = false;
        for (Iterator iter = this.latencies.iterator(); iter.hasNext();)
        {
            if (((Latency)iter.next()).isOpen())
            {
                this.openState = true;
            }
        }
    }

    OrLatency (Latency l1, Latency l2, LatencyKey key)
    {
        this(createSet(l1, l2), key);
    }

    private static Set createSet (Latency l1, Latency l2)
    {
        Set set = new HashSet(3);
        set.add(l1);
        set.add(l2);
        return set;
    }

    private static Set flatten (Set lats)
    {
        final Set newSet = new HashSet();
        for (Iterator iter = lats.iterator(); iter.hasNext();)
        {
            final Latency next = (Latency)iter.next();
            if (next instanceof OrLatency)
            {
                newSet.addAll(((OrLatency)next).latencies);
            }
            else
            {
                newSet.add(next);
            }
        }
        return newSet;
    }
    
    private static int getMinClocks (Collection latencies)
    {
        boolean minValid = false;
        int min = 0;
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            int clocks = ((Latency)iter.next()).getMinClocks();
            min = minValid ? Math.min(min, clocks) : clocks;
            minValid = true;
        }
        return min;
    }

    private static int getMaxClocks (Collection latencies)
    {
        boolean maxValid = false;
        int max = 0;
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            int clocks = ((Latency)iter.next()).getMaxClocks();
            if (clocks == Latency.UNKNOWN)
            {
                return clocks;
            }
            max = maxValid ? Math.max(max, clocks) : clocks;
            maxValid = true;
        }
        return max;
    }

    public String toString()
    {
        String ret = "OrLatency<"+getKey()+"> {";
        for (Iterator iter = this.latencies.iterator(); iter.hasNext();)
        {
            ret += iter.next();
            
            if (iter.hasNext())
            {
                ret += ", ";
            }
        }
        ret += "}";
        return ret;
    }

    /**
     * Returns a semi-shallow clone of this latency object in which
     * the map of latencies has been cloned, but the latencies
     * contained in the map have not been cloned.
     *
     * @return an Object of type OrLatency
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone() throws CloneNotSupportedException
    {
        OrLatency clone = (OrLatency)super.clone();

        clone.latencies = new HashSet();

        for (Iterator iter = this.latencies.iterator(); iter.hasNext();)
        {
            clone.latencies.add(iter.next());
        }
        
        return clone;
    }
    
}
