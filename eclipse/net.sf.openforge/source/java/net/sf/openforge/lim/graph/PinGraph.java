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
 * $Id: PinGraph.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package net.sf.openforge.lim.graph;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.util.graphviz.*;
import net.sf.openforge.util.naming.*;

/**
 * A helper class to {@link LXGraph}, PinGraph is a sub-Graph for a {@link Module}.
 * It draws each of its components as a black box {@link Node}.
 *
 * @version $Id: PinGraph.java 2 2005-06-09 20:00:48Z imiller $
 */
class PinGraph extends BlackBoxGraph
{
    /**
     * For classes which extend PinGraph
     *
     * @param nodeCount a value of type 'int'
     */
    PinGraph (String name,int nodeCount,int fontSize)
    {
        super(name,nodeCount,fontSize);
    }

    
    static final class Input extends PinGraph
    {
        Input(InputPin ipin,int nodeCount,int fontSize)
        {
            super(ID.showLogical(ipin),nodeCount,fontSize);
            graph(ipin, nodeCount++);
            _graph.ln("Input Pin: "+ipin);
            
            Bus b=ipin.getBus();
            for(Iterator it2=b.getPorts().iterator();it2.hasNext();)
            {
                Port p=(Port)it2.next();
                Component c=(Component)p.getOwner();
                graph(c, nodeCount++);
                graphEdges(c,b);
            }
        }

        /**
         * Graphs the incoming connections to a component's ports.
         */
        protected void graphEdges (Component component,Bus src)
        {
            ComponentNode componentNode = (ComponentNode)nodeMap.get(component);
            graphEdge(componentNode, component.getGoPort());
            for (Iterator iter = component.getDataPorts().iterator(); iter.hasNext();)
            {
                net.sf.openforge.lim.Port port = (net.sf.openforge.lim.Port)iter.next();
                if(port.getBus()==src)
                    graphEdge(componentNode, port);
            }
        }
    }

    static final class Output extends PinGraph
    {
        Output(OutputPin opin,int nodeCount,int fontSize)
        {
            super(ID.showLogical(opin),nodeCount,fontSize);
            _graph.ln("Output Pin: "+opin);
            graph(opin, nodeCount++);
            Port p=opin.getPort();
            Bus b=p.getBus();
            graph(b.getOwner().getOwner(), nodeCount++);
            
            graphEdges(opin);
        }
    }
}

