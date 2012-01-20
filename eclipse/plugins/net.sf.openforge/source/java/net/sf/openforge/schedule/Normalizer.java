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
package net.sf.openforge.schedule;


import java.util.*;

import net.sf.openforge.lim.*;

/**
 * The Normalizer is a clean-up visit of the graph, making various states (like Port.isUsed())
 * logically consistent. Created: Thu Mar 21 12:22:58 2002
 *
 * Created:    April 25, 2002
 *
 * @author     akollegger
 * @version    $Id: Normalizer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Normalizer extends FilteredVisitor
{
    private final static String _RCS_ = "$Rev: 2 $";


    /** Constructor for the Normalizer object */
    protected Normalizer()
    {
        ;
    }

    /**
     * The "used" state of the go {@link Port} and done {@link Bus} of the component is pushed
     * out to the edge of the containing module.
     *
     * @param  c  the component whose control will be extended
     */
    public void filterAny(Component c)
    {
        if (_schedule.db) _schedule.ln(_schedule.VISIT, "Normalizer generic visiting " + c);

        dependencyCheck(c);

        /*
        Port go = c.getGoPort();
        Port clk = c.getClockPort();
        Port reset = c.getResetPort();

        if (go.isUsed() || clk.isUsed() || reset.isUsed())
        {
            if (_schedule.db) _schedule.ln(_schedule.VISIT, "Normalizer pushing signals up...");
            Stack antecedents = new Stack();
            antecedents.push(c);

            while (antecedents.size() > 0)
            {
                Component antecedent = (Component)antecedents.pop();
                Port pre_go = antecedent.getGoPort();
                Port pre_clk = antecedent.getClockPort();
                Port pre_reset = antecedent.getResetPort();

                if (_schedule.db) _schedule.ln(_schedule.VISIT, "\tantecedent: " + antecedent);

                for (Iterator entries = antecedent.getEntries().iterator(); entries.hasNext(); )
                {
                    Entry e = (Entry)entries.next();
                    HashSet new_antecedents = new HashSet();

                    if (pre_go.isUsed())
                    {
                        for (Iterator deps = e.getDependencies(pre_go).iterator(); deps.hasNext(); )
                        {
                            Dependency d = (Dependency)deps.next();
                            if (d instanceof ControlDependency)
                            {
                                Bus b = d.getStructuralBus();

                                if (!b.isUsed())
                                {
                                    b.setUsed(true);
                                    Component owner = b.getOwner().getOwner();

                                    if (owner instanceof InBuf)
                                    {
                                        b.getPeer().setUsed(true);
                                    }
                                    else
                                    {
                                        owner.getGoPort().setUsed(true);
                                        new_antecedents.add(owner);
                                    }
                                }
                            }
                        }
                    }
                    if (pre_clk.isUsed())
                    {
                        for (Iterator deps = e.getDependencies(pre_clk).iterator(); deps.hasNext(); )
                        {
                            Dependency d = (Dependency)deps.next();
                            if (d instanceof ClockDependency)
                            {
                                Bus b = d.getStructuralBus();

                                if (!b.isUsed())
                                {
                                    b.setUsed(true);
                                    Component owner = b.getOwner().getOwner();

                                    if (owner instanceof InBuf)
                                    {
                                        b.getPeer().setUsed(true);
                                    }
                                    else
                                    {
                                        owner.getClockPort().setUsed(true);
                                        new_antecedents.add(owner);
                                    }
                                }
                            }
                        }
                    }
                    if (pre_reset.isUsed())
                    {
                        for (Iterator deps = e.getDependencies(pre_reset).iterator(); deps.hasNext(); )
                        {
                            Dependency d = (Dependency)deps.next();
                            if (d instanceof ResetDependency)
                            {
                                Bus b = d.getStructuralBus();

                                if (!b.isUsed())
                                {
                                    b.setUsed(true);
                                    Component owner = b.getOwner().getOwner();

                                    if (owner instanceof InBuf)
                                    {
                                        b.getPeer().setUsed(true);
                                    }
                                    else
                                    {
                                        owner.getResetPort().setUsed(true);
                                        new_antecedents.add(owner);
                                    }
                                }
                            }
                        }
                    }
                    for (Iterator it = new_antecedents.iterator(); it.hasNext();)
                    {
                        antecedents.push(it.next());
                    }
                }
            }
        }

        Bus done = c.getMainExit().getDoneBus();

        if (done.isUsed())
        {
            Stack dependedents = new Stack();
            dependedents.push(c);

            while (dependedents.size() > 0)
            {
                Component dependent = (Component)dependedents.pop();
                Bus dep_done = dependent.getMainExit().getDoneBus();

                for (Iterator deps = dep_done.getStructuralDependents().iterator(); deps.hasNext(); )
                {
                    Dependency d = (Dependency)deps.next();
                    if (d instanceof ControlDependency)
                    {
                        Port p = d.getPort();
                        p.setUsed(true);

                        Component owner = p.getOwner();

                        if (owner instanceof OutBuf)
                        {
                            p.getPeer().setUsed(true);
                        }
                        else
                        {
                            owner.getMainExit().getDoneBus().setUsed(true);
                            dependedents.push(owner);
                        }
                    }
                }
            }
        }
        */
    }// genericVisit()


    /**
     * Does a check which forces ALL ports in the LIM to have at least
     * 1 logical dependency.
     *
     * @param c a value of type 'Component'
     */
    private static void dependencyCheck (Component c)
    {
        // Don't look at the top level Block (for now)
        if (c.getOwner() == null)
        {
            if (_schedule.db) _schedule.ln("Not checking dependencies on top level block: " + c);
            return;
        }
        
        // Make sure that every port in the LIM has at least 1 logical
        // dependency.
        if (!(c instanceof InBuf))
        {
            for (Iterator iter = c.getPorts().iterator(); iter.hasNext();)
            {
                Port port = (Port)iter.next();
                if (port.isUsed())
                {
                    boolean hasLogicalDep = false;
                    for (Iterator eIter = c.getEntries().iterator(); eIter.hasNext();)
                    {
                        Entry entry = (Entry)eIter.next();
                        Collection deps = entry.getDependencies(port);
                        for (Iterator dIter = deps.iterator(); dIter.hasNext();)
                        {
                            Dependency d = (Dependency)dIter.next();
                            if (d.getLogicalBus() != null)
                            {
                                hasLogicalDep = true;
                                break;
                            }
                        }
                        if (hasLogicalDep)
                        {
                            break;
                        }
                    }
                    
                    if (!hasLogicalDep)
                    {
                        String identifier = "port";
                        if (port == c.getClockPort())
                        {
                            identifier = "clock";
                        }
                        else if (port == c.getResetPort())
                        {
                            identifier = "reset";
                        }
                        else if (port == c.getGoPort())
                        {
                            identifier = "go";
                        }
                        else if (c.getDataPorts().contains(port))
                        {
                            identifier = "data port " + c.getDataPorts().indexOf(port);
                        }
                        
                        assert hasLogicalDep : "Component " + c + " has no logical dependency on port " + identifier;
                    }
                }
            }
        }
    }
    
}

