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
 * $Id: BlackBoxGraph.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package net.sf.openforge.lim.graph;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.util.graphviz.*;

/**
 * A helper class to {@link LXGraph}, BlackBoxGraph is a sub-Graph for a {@link BlackBox}.
 * It draws each of its components as a black box {@link Node}.
 *
 * @version $Id: BlackBoxGraph.java 2 2005-06-09 20:00:48Z imiller $
 */
class BlackBoxGraph extends ModuleGraph
{
    /** List of components to be added */
    protected HashSet components=new HashSet();
    //protected ArrayList components=new ArrayList();
    
    BlackBoxGraph (String name,Object obj,int nodeCount, int fontSize)
    {
        super(nodeCount,fontSize);
        setLabel(name+ " @" + Integer.toHexString(obj.hashCode()));
    }
    
    BlackBoxGraph (String name,int nodeCount,int fontSize)
    {
        super(nodeCount,fontSize);
        setLabel(name);
    }

    void addComponent(Component c)
    {
        assert c != null;
        components.add(c);
    }
    
    void addComponents(Collection c)
    {
        assert c != null;
        components.addAll(c);
    }
    
    /**
     * Creates a node for each component of the BlackBox.  Also creates edges
     * for the port-to-bus connections.
     */
    void graphAddedComponents()
    {
        for (Iterator iter = components.iterator(); iter.hasNext();)
        {
            graph((Component)iter.next(), nodeCount);
        }
        for (Iterator iter = components.iterator(); iter.hasNext();)
        {
            graphEdges((Component)iter.next());
        }
    }

}







