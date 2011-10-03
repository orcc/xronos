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

package net.sf.openforge.util.stats;

import java.io.*;

import java.util.*;


/**
 * Class for keeping stats.
 * Knows about:
 *       - simple counters
 *       - threshold counters
 *       - min/max/avg/count
 */
public class Stats
{

    /** DOCUMENT ME! */
    static final String rcs_id = "RCS_REVISION: $Rev: 2 $";
    private String groupName;
    private List stats = new ArrayList(5);

    private Stats ()
    {
        ;
    }

    /**
     * Creates a new Stats object. DOCUMENT ME!
     *
     * @param groupName DOCUMENT ME!
     */
    public Stats (String groupName)
    {
        this.groupName = groupName;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public StatSimpleCounter addSimpleCounter (String name)
    {
        StatSimpleCounter ssc = new StatSimpleCounter(name);
        stats.add(ssc);
        return ssc;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     * @param threshold DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public StatThresholdCounter addThresholdCounter (String name, long threshold)
    {
        StatThresholdCounter stc = new StatThresholdCounter(name, threshold);
        stats.add(stc);
        return stc;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public StatDistributionCounter addDistributionCounter (String name)
    {
        StatDistributionCounter sd = new StatDistributionCounter(name);
        stats.add(sd);
        return sd;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public List getStats ()
    {
        return stats;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String toString ()
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);

        pw.println("Stats for: " + groupName);
        for (Iterator it = getStats().iterator(); it.hasNext();)
        {
            Stat stat = (Stat)it.next();
            pw.println("\t" + stat);
            for (Iterator it2 = stat.getTokens().iterator(); it2.hasNext();)
            {
                pw.println("\t\t- " + it2.next());
            }
        }

        pw.flush();

        return bos.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param args DOCUMENT ME!
     */
    public static void main (String[] args)
    {
        Stats s = new Stats("Test");
        StatSimpleCounter sc1 = s.addSimpleCounter("Test 1");
        StatThresholdCounter sc2 = s.addThresholdCounter("Test 2", 2);
        sc1.inc();
        sc1.inc();
        sc1.inc();
        sc1.inc();

        sc2.inc();
        sc2.inc(2);
        sc2.inc();
        sc2.inc(2);

        StatDistributionCounter sd = s.addDistributionCounter("Test 3");
        for (int i = 0; i < 10; i++)
        {
            sd.update(i);
        }

        System.out.println(s);
    }
}


/*--- formatting done in "Lavalogic Coding Convention" style on 11-23-1999 ---*/
