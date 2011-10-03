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
 * $Id: LoopGraph.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package net.sf.openforge.lim.graph;

import java.util.*;

import net.sf.openforge.lim.*;

/**
 * A sub-graph for a {@link Loop} in an {@link LXGraph}.  This specialization]
 * of {@link ModuleGraph} handles the cycles that appear in loop graphs.  In
 * particular, the feedback registers are drawn in two parts: one node to
 * represent the register as a source of data (ie at the top of the graph)
 * and another node to represent the register as a data sink (ie at the bottom
 * of the graph).
 *
 * @version $Id: LoopGraph.java 2 2005-06-09 20:00:48Z imiller $
 */
class LoopGraph extends ModuleGraph
{
    /** Map of Reg to FeedbackRegSinkNode */
    private Map sinkNodeMap = new HashMap();

    LoopGraph (Loop loop, int id, int fontSize)
    {
        super(loop, id, fontSize);
        graphComponentsDelayed();
    }

    protected void graphComponents ()
    {
        /*
         * Delay until we are fully constructed.
         */
    }

    private void graphComponentsDelayed ()
    {
        Loop loop = (Loop)getModule();
        Reg controlRegister = loop.getControlRegister();
        Collection dataRegisters = loop.getDataRegisters();

        Collection components = new HashSet(loop.getComponents());
        components.remove(controlRegister);
        components.removeAll(dataRegisters);

        for (Iterator iter = components.iterator(); iter.hasNext();)
        {
            graph((Component)iter.next(), nodeCount++);
        }
        if (controlRegister != null)
        {
            graphRegister(controlRegister);
        }
        for (Iterator iter = dataRegisters.iterator(); iter.hasNext();)
        {
            graphRegister((Reg)iter.next());
        }

        for (Iterator iter = components.iterator(); iter.hasNext();)
        {
            graphEdges((Component)iter.next());
        }

        for (Iterator iter = sinkNodeMap.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry entry = (Map.Entry)iter.next();
            Reg reg = (Reg)entry.getKey();
            ComponentNode regNode = (ComponentNode)entry.getValue();
            graphEdge(regNode, reg.getEnablePort());
            graphEdge(regNode, reg.getDataPort());
        }
    }

    private void graphRegister (Reg reg)
    {
        FeedbackRegSourceNode sourceNode = new FeedbackRegSourceNode(reg, "node" + nodeCount++,fontSize);
        add(sourceNode);
        nodeMap.put(reg, sourceNode);

        FeedbackRegSinkNode sinkNode = new FeedbackRegSinkNode(reg, "node" + nodeCount++,fontSize);
        add(sinkNode);
        sinkNodeMap.put(reg, sinkNode);
    }
}
