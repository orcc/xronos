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
 * $Id: ModuleGraph.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package net.sf.openforge.lim.graph;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.graphviz.*;

/**
 * A helper class to {@link LXGraph}, ModuleGraph is a sub-Graph for a {@link Module}.
 * It draws each of its components as a black box {@link Node}.
 *
 * @version $Id: ModuleGraph.java 2 2005-06-09 20:00:48Z imiller $
 */
class ModuleGraph extends Graph
{
    public static final boolean COLORIZE_EDGES = false;
    
    /** The module represented by this graph */
    protected Module module;

    /** A map of Component to Node */
    protected Map nodeMap = new HashMap();

    /** A running node count, used to create unique identifiers */
    protected int nodeCount;

    protected int fontSize;
    
    private final static String SUBGRAPH = "subgraph";
    private final static String CLUSTER = "cluster";

    private static int color = 0;
    private static String[] colors = {
        "black","blue","yellow","cyan",
        "red","brown","magenta","bisque3"};
    
    
    /**
     * Adds a child nod to this graph.
     */
    public void add (Node node)
    {
        super.add(node);
        nodeCount++;
        this.setGVAttribute("fontsize",""+fontSize);
    }

    /**
     * For classes which extend ModuleGraph
     *
     * @param nodeCount a value of type 'int'
     */
    ModuleGraph (int nodeCount,int fontSize)
    {
        super(SUBGRAPH, CLUSTER + nodeCount);
        this.nodeCount=nodeCount;
        this.fontSize=fontSize;
        this.setGVAttribute("fontsize",""+fontSize);
    }
    
    /**
     * Constructs a ModuleGraph.
     *
     * @param module the module whose components are to be graphed
     * @param nodeCount the number of nodes created so far; a running tally
     *          is used to create unique identifiers
     */
    ModuleGraph (Module module, int nodeCount, int fontSize)
    {
        super(SUBGRAPH, CLUSTER + nodeCount);
        this.module = module;
        this.nodeCount = nodeCount;
        this.fontSize=fontSize;
        this.setGVAttribute("fontsize",""+fontSize);
        setLabel(ComponentNode.getShortClassName(module)
            + " @" + Integer.toHexString(module.hashCode()));
        graphComponents();
    }

    /**
     * Gets the current node count.  This includes the nodes that have
     * been added to this graph.
     */
    int getNodeCount ()
    {
        return nodeCount;
    }

    /**
     * Gets the module represented by this graph.
     */
    Module getModule ()
    {
        return module;
    }

    /**
     * Creates a node for each component of the module.  Also creates edges
     * for the port-to-bus connections.
     */
    protected void graphComponents ()
    {
        for (Iterator iter = module.getComponents().iterator(); iter.hasNext();)
        {
            graph((Component)iter.next(), nodeCount);
        }

        for (Iterator iter = module.getComponents().iterator(); iter.hasNext();)
        {
            graphEdges((Component)iter.next());
        }
    }

    /**
     * Creates a new node in this graph for a given component.
     *
     * @param component the component to graph
     * @param index the current node count, used to create a unique identifier for
     *          the new node
     */
    protected void graph (Component component, int index)
    {
        String id = "node" + index;
        ComponentNode node = null;
        if (component instanceof OutBuf)
        {
            node = new OutBufNode((OutBuf)component, id, fontSize);
        }
        else if (component instanceof Constant)
        {
            node = new ConstantNode((Constant)component, id, fontSize);
        }
        else if (component instanceof SRL16)
        {
            node = new SRL16Node((SRL16)component, id, fontSize);
        }
        else if (component instanceof Reg)
        {
            node = new RegNode((Reg)component, id, fontSize);
        }
        else if (component instanceof InBuf)
        {
            node = new InBufNode((InBuf)component, id, fontSize);
        }
        else if (component instanceof Latch)
        {
            node = new LatchNode((Latch)component, id, fontSize);
        }
//         else if (component instanceof MemoryPort.Implementation)
//         {
//             node = new MemoryImplementationNode((MemoryPort.Implementation)component, id, fontSize);
//         }
        else if (component instanceof EncodedMux)
        {
            node = new EncodedMuxNode((EncodedMux)component, id, fontSize);
        }
        else if (component instanceof MemoryRead)
        {
            Component physComp = ((MemoryRead)component).getPhysicalComponent();
            if (physComp != null)
            {
                ComponentNode phys = new ComponentNode(physComp, "node_phys" + index, fontSize);
                addComp(physComp, phys);
            }
            node = new ComponentNode(component, id, fontSize);
        }
        else if (component instanceof MemoryWrite)
        {
            Component physComp = ((MemoryWrite)component).getPhysicalComponent();
            if (physComp != null)
            {
                ComponentNode phys = new ComponentNode(physComp, "node_phys" + index, fontSize);
                addComp(physComp, phys);
            }
            node = new ComponentNode(component, id, fontSize);
        }        
        else
        {
            node = new ComponentNode(component, id, fontSize);
        }

        addComp(component, node);
    }

    private void addComp (Component component, ComponentNode node)
    {
        add(node);
        for (Iterator iter = node.getExitNodes().iterator(); iter.hasNext();)
        {
            Node exitNode = (Node)iter.next();
            add(exitNode);
            connect(node, exitNode, new Edge(100));
        }
        nodeMap.put(component, node);
    }
    

    /**
     * Graphs the incoming connections to a component's ports.
     */
    protected void graphEdges (Component component)
    {
        ComponentNode componentNode = (ComponentNode)nodeMap.get(component);
        for (Iterator iter = component.getPorts().iterator(); iter.hasNext();)
        {
            net.sf.openforge.lim.Port port = (net.sf.openforge.lim.Port)iter.next();
            graphEdge(componentNode, port);
        }
        if (component instanceof MemoryRead)
        {
            if (((MemoryRead)component).getPhysicalComponent() != null)
                graphEdges(((MemoryRead)component).getPhysicalComponent());
        }
        else if (component instanceof MemoryWrite)
        {
            if (((MemoryWrite)component).getPhysicalComponent() != null)
                graphEdges(((MemoryWrite)component).getPhysicalComponent());
        }
    }

    /**
     * Creates an edge to a component's port.
     *
     * @param componentNode the node for the port's owner
     * @param port the port whose incoming bus connection is to be graphed
     */
    protected void graphEdge (ComponentNode componentNode, net.sf.openforge.lim.Port port)
    {
        if (port.isConnected())
        {
            Node portNode = componentNode.getNode(port);
            Bus bus = port.getBus();
            Component busOwner = bus.getOwner().getOwner();
            ComponentNode busOwnerNode = (ComponentNode)nodeMap.get(busOwner);
            // if added CRSS
            if(busOwnerNode!=null)
            {
                Node busNode = busOwnerNode.getNode(bus);
                
                // YSYU: no need to print these unless you are debugging
                if (_graph.db)
                {   
                    if (portNode == null)
                    {
                        System.err.println("no node for port of " + port.getOwner());
                        System.err.println("  port=" + port);
                    }
                    
                    if (busNode == null)
                    {
                        System.err.println("no node for bus of " + busOwner);
                        System.err.println("  bus=" + bus);
                    }
                }
                
                // if added CRSS
                if((busNode!=null)&&(portNode!=null))
                {
                    Edge e = new Edge(1);
                    if (COLORIZE_EDGES)
                    {
                        e.setColor(colors[color]);
                        color++;
                        if (color >= 8)
                            color = 0;
                    }
                    connect(busNode, portNode, e);
                }
            }
        }
    }
}





