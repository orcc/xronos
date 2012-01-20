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
 * $Id: LWireGraph.java 88 2006-01-11 22:39:52Z imiller $
 *
 * 
 */

package net.sf.openforge.lim.graph;

import java.io.*;
import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.graphviz.*;
import net.sf.openforge.util.naming.*;

/**
 * Yet another hack of {@link LGraph}, this time to draw only physical connections (those
 * between {@link Bus Buses} and {@link Port Ports}).
 *
 * @version $Id: LWireGraph.java 88 2006-01-11 22:39:52Z imiller $
 */
public class LWireGraph extends DefaultVisitor
{
    private static final String _RCS_ = "$Rev: 88 $";

    /** Use a scanner for traversing module contents */
    private net.sf.openforge.lim.Scanner scanner;

    /** The top level Graph -- put all edges at this level */
    private Graph topGraph;

    /** The top-most component being graphed -- to avoid trying to graphOutside. */
    private Visitable top;
    
    /** The graph instance for the current level being visited */
    private Graph graph;

    /** True if the insides of Selectors and other primitive modules to be drawn */
    private boolean isDetailed = false;
    /** show clock and reset */
    private boolean drawCR = false;
    /** show dependencies */
    private boolean graphDependencies=false;
    /** show logical connections */
    private boolean graphLogical=false;
    /** show structural connections */
    private boolean graphStructural=false;
    /** show data connections */
    private boolean graphData=false;
    /** show control connections */
    private boolean graphControl=false;

    /** used to handle unresolved nodes in graphing */
    private Map unresolvedNodes = new HashMap();

    /**
     * The graph stack; pushed when a new level is visited, popped when that
     * level is exited.
     */
    private LinkedList graphStack = new LinkedList();
    
    /** The number of nodes so far, used to generate unique identifiers */
    private int nodeCount = 0;

    /** 
     * Map of LIM object to graph Node - for dependencies and generic 
     *   connections (exits, etc) 
     */
    private Map nodeMapDeps = new HashMap();
    /** Map of LIM object to graph Node - for physical connections */
    private Map nodeMapPhys = new HashMap();
    
    /** Weights for the various types of edges */
    private static final int WT_ENTRY = 1200;
    private static final int WT_EXIT  = 8000;
    private static final int WT_DEP   = 4;
    private static final int WT_FEEDBK= 1;
    private static final int WT_ENTRYFEEDBACK=12;

    /** show data connections */
    public static final int DATA=0x1;     
    /** show control connections */
    public static final int CONTROL=0x2;  
    /** show logical connections */
    public static final int LOGICAL=0x4;  
    /** show structural connections */
    public static final int STRUCTURAL=0x8; 
    /** used internally to denote any of the above */
    private static final int DEPENDENCY=DATA|CONTROL|LOGICAL|STRUCTURAL;

    /** show clock & reset connections */
    public static final int CLOCKRESET=0x20;
    /** show physical connections (bus/port) */
    public static final int PHYSICAL=0x40;  
    /** 
     *show detailed primitive contents: if set the insides of Selectors and other
     * pass-through modules are to be graphed; if false, they will be graphed as
     * opaque boxes */
    public static final int DETAIL=0x80;  
    /** print in landscape mode */
    public static final int LANDSCAPE=0x100;
    
    public static final int DEFAULT=DATA|CONTROL|LOGICAL|STRUCTURAL|PHYSICAL;
    
    /**
     * Constructs a new LWireGraph.
     *
     * @param name a title for the graph 
     * @param top the LIM element whose contents are to be graphed; typically
     *          a {@link Design}, but only the {@link Procedure Procedures}
     *          down will be depicted
     * @param flags is a bitwise or of DATA, CONTROL, LOGICAL, 
     *          STRUCTURAL, CLOCKRESET, PHYSICAL
     */
    public LWireGraph (String name, Visitable top, int flags)
    {
        super();
        parseFlags(flags);
        
        this.scanner = new net.sf.openforge.lim.Scanner(this);

        this.graph = new Graph(name);
        //        this.graph.setSize(7, 10); ruins the ratio statement below...
        this.graph.setLabel(name);
        //some attributes to make the graph smaller, and to print
        // to several pages when printed via: dot -Tps file.dot | lpr
        this.graph.setGVAttribute("ratio","auto");
        this.graph.setGVAttribute("ranksep",".5");
        this.graph.setGVAttribute("nodesep",".12");
        this.graph.setGVAttribute("fontsize","10");
        this.graph.setGVAttribute("fontname","Helvetica");
        this.graph.setGVAttribute("page","8.5,11.0");
        if ((flags & LANDSCAPE) == LANDSCAPE)
        {
            setLandscape();
        }
        this.topGraph = graph;

        this.top = top;
        top.accept(this);
    }
    /** parse the flags, setting fields as appropriate */
    private void parseFlags (int flags)
    {
        if ((flags & DATA) == DATA)
        {
            graphDependencies=true;
            graphData=true;
        }
        if ((flags & CONTROL) == CONTROL)
        {
            graphDependencies=true;
            graphControl=true;
        }
        if ((flags & LOGICAL) == LOGICAL)
        {
            graphDependencies=true;
            graphLogical=true;
        }
        if ((flags & STRUCTURAL) == STRUCTURAL)
        {
            graphDependencies=true;
            graphStructural=true;
        }
        if ((flags & CLOCKRESET) == CLOCKRESET)
        {
            drawCR=true;
        }
        if ((flags & DETAIL) == DETAIL)
        {
            this.isDetailed = true;
        }
    }
    
    /**
     * Constructs a new LWireGraph that will not draw the insides of pass-through
     * modules like Selector.
     *
     * @param name a title for the graph 
     * @param top the LIM element whose contents are to be graphed; typically
     *          a {@link Design}, but only the {@link Procedure Procedures}
     *          down will be depicted
     */
    public LWireGraph (String name, Visitable top)
    {
        this(name, top, DEFAULT);
    }

    /**
     * Prints a text representation of this graph readable by the
     * <i>dotty</i> viewer.
     *
     * @param writer the writer to receive the text
     */
    public void print (PrintWriter writer)
    {
        // if there are any unresolved nodes at this point, they must have come from 
        // somewhere off the graph, so we add a circle node to the topgraph and
        // connect them
        if (unresolvedNodes.values().size() > 0)
        {
            ArrayList urList=new ArrayList(unresolvedNodes.values());
            
            for (Iterator iter = urList.iterator(); iter.hasNext();)
            {
                Unresolved ur = (Unresolved)iter.next();
                Node target=(Node) nodeMapDeps.get(ur.getBus());
                if (target == null) 
                {
                    target=(Node) nodeMapPhys.get(ur.getBus());
                }
                
                if (target != null)
                {
                    assert target != null
                    : "Found an unresolved node in nodeMapDeps "+ur.getBus()+" "+ur.getBus().getOwner().getOwner();
                    
                    Circle circle=new Circle("unknownSrc"+nodeCount++);
                    circle.setLabel(getName(ur.getBus().getOwner().getOwner())+"::"+
                                    Integer.toHexString(ur.getBus().getOwner().getOwner().hashCode())+"::"+
                                    Integer.toHexString(ur.getBus().hashCode()));
                    topGraph.add(circle);
                    addToNodeMap(ur.getBus(),circle, DEPENDENCY);
                }
            }
        }
        
        graph.print(writer);
        writer.flush();
    }

    /**
     * causes the graph to be printed in landscape mode - this may or may not 
     * use less paper - it depends on the graph.  hence it is an option
     */
    public void setLandscape ()
    {
        this.topGraph.setGVAttribute("rotate","90");
    }

    private void pushGraph (Object obj)
    {
        graphStack.addFirst(graph);
        graph = graph.getSubgraph("cluster" + nodeCount++);
        addToNodeMap(obj, graph, DEPENDENCY);
    }

    private void popGraph ()
    {
        graph = (Graph)graphStack.removeFirst();
    }


    private static String getName (Object o, String defaultName)
    {
        return ID.showLogical(o);
    /*
        String name = namedThing.getIDLogical();
        if (name == null)
        {
            name = namedThing.getIDGlobalType();
        }
        return name == null ? defaultName : name;
    */
    }

    private static String getName (Object o)
    {
        return ID.showLogical(o); //getName(namedThing, namedThing.getClass().getName());
    }


    /**
     * Draw the box for the component.
     */
    private void graph (Component component)
    {
        net.sf.openforge.util.graphviz.Record box = new net.sf.openforge.util.graphviz.Record("component" + nodeCount++);
        box.setLabel(getName(component) + " (Cmp)");

        net.sf.openforge.util.graphviz.Record.Port main = box.getPort("main");
        net.sf.openforge.util.graphviz.Record.Port entryPort = main.getPort("entry");
        entryPort.setSeparated(false);

        if (component.getGoPort().isConnected())
        {
            net.sf.openforge.util.graphviz.Record.Port goPort = entryPort.getPort("go");
            goPort.setLabel("g");
            goPort.setSeparated(true);
            addToNodeMap(component.getGoPort(), goPort, DEPENDENCY);
        }
            
        if (drawCR)
        {
            if (component.getClockPort().isConnected())
            {
                net.sf.openforge.util.graphviz.Record.Port clockPort = entryPort.getPort("clock");
                clockPort.setLabel("c");
                clockPort.setSeparated(true);
                addToNodeMap(component.getClockPort(), clockPort, DEPENDENCY);
            }
            
            if (component.getResetPort().isConnected())
            {
                net.sf.openforge.util.graphviz.Record.Port resetPort = entryPort.getPort("reset");
                resetPort.setLabel("r");
                resetPort.setSeparated(true);
                addToNodeMap(component.getResetPort(), resetPort, DEPENDENCY);
            }
        }
        
        for (int i = 0; i < component.getDataPorts().size(); i++)
        {
            Port port = (Port)component.getDataPorts().get(i);
            if (port.isConnected())
            {
                String label = "d" + i;
                net.sf.openforge.util.graphviz.Record.Port dataPort = entryPort.getPort("p_" + label);
                dataPort.setLabel(label);
                dataPort.setSeparated(true);
                addToNodeMap(port, dataPort, DEPENDENCY);
            }
        }

        net.sf.openforge.util.graphviz.Record.Port bodyPort = main.getPort("body");
        bodyPort.setLabel(getName(component));
        bodyPort.setSeparated(false);

        if (component.getExits().size() == 1)
        {
            if (component instanceof InBuf)
            {
                graphInBufExit((InBuf)component, main);
            }
            else
            {
                graphSingleExit(component, main);
            }
        }

        addToNodeMap(component, box, DEPENDENCY);
        graph.add(box);
            
        graphOutside(component, false);
        
        if (component.getExits().size() > 1)
        {
            connectExits(component, component);
        }
    }

    /**
     * Draw the box for the component.
     */
    private void graph (InBuf inbuf)
    {
        final Module owner = inbuf.getOwner();

        net.sf.openforge.util.graphviz.Record box = new net.sf.openforge.util.graphviz.Record("component" + nodeCount++);
        box.setLabel(getName(inbuf) + " (Cmp)");

        net.sf.openforge.util.graphviz.Record.Port main = box.getPort("main");
        net.sf.openforge.util.graphviz.Record.Port entryPort = main.getPort("entry");
        entryPort.setSeparated(false);

        if (owner.getGoPort().isConnected())
        {
            net.sf.openforge.util.graphviz.Record.Port goPort = entryPort.getPort("go");
            goPort.setLabel("g");
            goPort.setSeparated(true);
            addToNodeMap(owner.getGoPort(), goPort, DEPENDENCY);
        }
            
        if (drawCR)
        {
            if (owner.getClockPort().isConnected())
            {
                net.sf.openforge.util.graphviz.Record.Port clockPort = entryPort.getPort("clock");
                clockPort.setLabel("c");
                clockPort.setSeparated(true);
                addToNodeMap(owner.getClockPort(), clockPort, DEPENDENCY);
            }
            
            if (owner.getResetPort().isConnected())
            {
                net.sf.openforge.util.graphviz.Record.Port resetPort = entryPort.getPort("reset");
                resetPort.setLabel("r");
                resetPort.setSeparated(true);
                addToNodeMap(owner.getResetPort(), resetPort, DEPENDENCY);
            }
        }
        
        for (int i = 0; i < owner.getDataPorts().size(); i++)
        {
            Port port = (Port)owner.getDataPorts().get(i);
            if (port.isConnected())
            {
                String label = "d" + i;
                net.sf.openforge.util.graphviz.Record.Port dataPort = entryPort.getPort("p_" + label);
                dataPort.setLabel(label);
                dataPort.setSeparated(true);
                addToNodeMap(port, dataPort, DEPENDENCY);
            }
        }

        net.sf.openforge.util.graphviz.Record.Port bodyPort = main.getPort("body");
        bodyPort.setLabel(getName(inbuf));
        bodyPort.setSeparated(false);

        if (inbuf.getExits().size() == 1)
        {
            graphInBufExit(inbuf, main);
        }

        addToNodeMap(inbuf, box, DEPENDENCY);
        graph.add(box);
            
        graphOutside(inbuf, false);
        
        if (inbuf.getExits().size() > 1)
        {
            connectExits(inbuf, inbuf);
        }
    }


    private void graphSingleExit (Component component, net.sf.openforge.util.graphviz.Record node)
    {
        final Exit exit = (Exit)component.getExits().iterator().next();
        net.sf.openforge.util.graphviz.Record.Port exitPort = node.getPort("exit");
        addToNodeMap(exit, exitPort, DEPENDENCY);
        
        if (exit.getDoneBus().isConnected())
        {
            net.sf.openforge.util.graphviz.Record.Port donePort = exitPort.getPort("done");
            donePort.setLabel("D");
            donePort.setSeparated(true);
            addToNodeMap(exit.getDoneBus(), donePort, DEPENDENCY);
        }
        
        for (int i = 0; i < exit.getDataBuses().size(); i++)
        {
            final Bus bus = (Bus)exit.getDataBuses().get(i);
            if (bus.isConnected())
            {
                String label = "d" + i;
                net.sf.openforge.util.graphviz.Record.Port dataPort = exitPort.getPort("b_" + label);
                dataPort.setLabel(label);
                dataPort.setSeparated(true);
                addToNodeMap(bus, dataPort, DEPENDENCY);
            }
        }
    }

    private void graphInBufExit (InBuf inbuf, net.sf.openforge.util.graphviz.Record node)
    {
        final Exit exit = (Exit)inbuf.getExits().iterator().next();
        net.sf.openforge.util.graphviz.Record.Port exitPort = node.getPort("exit");
        addToNodeMap(exit, exitPort, DEPENDENCY);
        
        if (inbuf.getGoBus().isConnected())
        {
            net.sf.openforge.util.graphviz.Record.Port goPort = exitPort.getPort("go");
            goPort.setLabel("G");
            goPort.setSeparated(true);
            addToNodeMap(inbuf.getGoBus(), goPort, DEPENDENCY);
        }

        if (drawCR)
        {
            if (inbuf.getClockBus().isConnected())
            {
                net.sf.openforge.util.graphviz.Record.Port clockPort = exitPort.getPort("clock");
                clockPort.setLabel("clk");
                clockPort.setSeparated(true);
                addToNodeMap(inbuf.getClockBus(), clockPort, DEPENDENCY);
            }

            if (inbuf.getResetBus().isConnected())
            {
                net.sf.openforge.util.graphviz.Record.Port resetPort = exitPort.getPort("reset");
                resetPort.setLabel("rst");
                resetPort.setSeparated(true);
                addToNodeMap(inbuf.getResetBus(), resetPort, DEPENDENCY);
            }
        }

        int i = 0;
        for (Iterator iter = inbuf.getDataBuses().iterator(); iter.hasNext();)
        {
            final Bus bus = (Bus)iter.next();
            if (bus.isConnected())
            {
                String label = "d" + i;
                net.sf.openforge.util.graphviz.Record.Port dataPort = exitPort.getPort("b_" + label);
                dataPort.setLabel(label);
                dataPort.setSeparated(true);
                addToNodeMap(bus, dataPort, DEPENDENCY);
            }
        }
    }


    private void graphPreVisit (Module module)
    {
        graphPreVisit(module,false);
    }    
    /**
     * previsit for a module
     * @param module the module being visited
     * @param feedback true if the module has a feedback entry
     */
    private void graphPreVisit (Module module, boolean feedback)
    {
        /*
         * Graph external elements in the current graph.
         */
        graphOutside(module,feedback);

        /*
         * Push into a new graph for the module internals.
         */
        pushGraph(module);
        graph.setLabel(getName(module) + "::" 
                       + Integer.toHexString(module.hashCode()));
        graph.setColor("red");
    }

    private void graphPostVisit (Module module)
    {
        graphPostVisit(module,false);
    }

    private void graphPostVisit (Module module, boolean feedback)
    {
        final InBuf inbuf = module.getInBuf();
        if (drawCR)
        {
            if (inbuf.getClockBus().isConnected())
            {
                Node clock = (Node)nodeMapDeps.get(inbuf.getClockBus());
                clock.setLabel("c");
            }
            if (inbuf.getResetBus().isConnected())
            {
                Node reset = (Node)nodeMapDeps.get(inbuf.getResetBus());
                reset.setLabel("r");
            }
        }

        if (inbuf.getGoBus().isConnected())
        {
            Node go = (Node)nodeMapDeps.get(inbuf.getGoBus());
            go.setLabel("g");
            int index = 0;
        }
        
        int index = 0;
        for (Iterator iter = inbuf.getDataBuses().iterator(); iter.hasNext();)
        {
            final Bus bus = (Bus)iter.next();
            if (bus.isConnected())
            {
                Node node = (Node)nodeMapDeps.get(bus);
                node.setLabel("d" + index++);
            }
        }

        /*
         * Pop back out to connect externals to internals.
         */
        popGraph();
        if (module != top)
        {
            connectPorts(module, false);
            connectExits(module);
        }
    }

    private void connectPorts (Component component, boolean isFeedback)
    {
        if (drawCR)
        {
            connectPort(component.getClockPort(), isFeedback);
            connectPort(component.getResetPort(), isFeedback);
        }

        connectPort(component.getGoPort(), isFeedback);
        for (Iterator iter = component.getDataPorts().iterator(); iter.hasNext();)
        {
            connectPort((Port)iter.next(), isFeedback);
        }
    }

    private void connectPort (Port port, boolean isFeedback)
    {
        if (port.isConnected())
        {
            final Node portNode = (Node)nodeMapDeps.get(port);
            final Node busNode = (Node)nodeMapDeps.get(port.getBus());
            final Edge edge = new Edge(isFeedback ? WT_ENTRYFEEDBACK : WT_EXIT);
            
            assert port.getBus() != null : "no bus for port of " + port.getOwner();
            assert busNode != null : "null busNode for bus of " + port.getBus().getOwner().getOwner();
            assert portNode != null : ("null portNode for port of " + port.getOwner());
            topGraph.connect(busNode, portNode, edge);

        }
    }

    /**
     * Connects the Exits of the component. If the Exit has a peer, a dashed
     * line is drawn to it; else a solid line will be drawn to the component.
     */
    private void connectExits (Component component)
    {
        connectExits(component, null);
    }

    private void connectExits (Module module)
    {
        connectExits(module, null);
    }

    /**
     * Connects the Exits of a Component.
     *
     * @param component the component whose exits are to be connected
     * @param target the node to which the exits are connected; if null and
     *          there is a peer, then a dashed line will be drawn to the peer;
     *          otherwise a solid line will be drawn to the component
     */
    private void connectExits (Component component, Component target)
    {
        int index = 0;
        for (Iterator iter = component.getExits().iterator(); iter.hasNext();)
        {
            Component workingTarget = target;
            Exit exit = (Exit)iter.next();
            Node exitNode = (Node)nodeMapDeps.get(exit);
            Edge edge = new Edge(WT_EXIT);
            edge.setLabel("ex" + index++);
            edge.setDirection(Edge.DIR_NONE);

            if (workingTarget == null)
            {
                workingTarget = exit.getPeer();
                if (workingTarget == null)
                {
                    workingTarget = component;
                }
            }

            Node targetNode = (Node)nodeMapDeps.get(workingTarget);
            topGraph.connect(targetNode, exitNode, edge);

            assert targetNode != null:"Couldn't find node for "+workingTarget+", source is: "+component;
            assert exitNode != null:"Couldn;t find exit node for "+component+", exit: "+exit;
        }
    }

    /**
     * Graph connections outside of the component, unless the Component
     * is the top.
     */
    private void graphOutside (Component component,boolean feedback)
    {
        if (component != top)
        {
            if (component.getExits().size() > 1)
            {
                graphExits(component);
            }
            connectPorts(component, feedback);
        }
    }

    private void graphOutside (Module module, boolean feedback)
    {
        graphExits(module);
    }

    private void graphExits (Component component)
    {
        int index = 0;
        for (Iterator iter = component.getExits().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            graphExit(exit, index++);
        }
    }

    private void graphExit (Exit exit, int id)
    {
        Component component = exit.getOwner();

        net.sf.openforge.util.graphviz.Record record = new net.sf.openforge.util.graphviz.Record("exit" + nodeCount++);
        addToNodeMap(exit, record, DEPENDENCY);

        net.sf.openforge.util.graphviz.Record.Port done = record.getPort("done");
        done.setLabel("D");
        done.setSeparated(true);
        addToNodeMap(exit.getDoneBus(), done, DEPENDENCY);

        int dindex = 0;
        Collection dataBuses = new LinkedHashSet(exit.getDataBuses());
        if (!drawCR)
        {
            if (exit.getOwner() instanceof InBuf)
            {
                // remove the clock and reset bus
                dataBuses.remove(((InBuf)exit.getOwner()).getClockBus());
                dataBuses.remove(((InBuf)exit.getOwner()).getResetBus());
            }
        }
        for (Iterator iter = dataBuses.iterator(); iter.hasNext();)
        {
            Bus bus = (Bus)iter.next();
            String dname = "d" + dindex++;
            net.sf.openforge.util.graphviz.Record.Port data = record.getPort(dname);
            data.setLabel(dname);
            data.setSeparated(true);
            addToNodeMap(bus, data, DEPENDENCY);
        }

        graph.add(record);
    }

    /**
     * type defines which nodeMap to look into - 
     * DEPENDENCY==nodeMapDeps, PHYSICAL==nodeMapPhys
     */
    private void addToNodeMap(Object key, Object value, int type)
    {
        assert type == PHYSICAL||type == DEPENDENCY : "Illegal type to addToNodeMap";
        assert value != null;
        assert key != null;
        
        if (type == PHYSICAL)
        {
            nodeMapPhys.put(key, value);
        }
        else
        {
            nodeMapDeps.put(key, value);
        }

        if (unresolvedNodes.containsKey(key))
        {
            Unresolved ur = (Unresolved)unresolvedNodes.get(key);
            Node target = (Node)nodeMapDeps.get(ur.getBus());
            assert target != null : "How can the target still be null!";
            List sources=ur.getSources();
            List edges=ur.getEdges();
            for (int i=0; i<sources.size(); i++)
            {
                Node sourceNode = (Node)sources.get(i);
                assert sourceNode != null : "null node for unresolved index " + i;
                topGraph.connect(target, sourceNode, (Edge)edges.get(i));
            }
            unresolvedNodes.remove(key);
        }
    }
    
    /**
     * holds unresolved edges, allows adding new edges to an existing bus
     * (key in the hashMap), and retrieval of the edges
     */    
    private static class Unresolved
    {
        private Bus bus;
        private List sources=new ArrayList();
        private List edges=new ArrayList();
        Unresolved(Bus b, Node s, Edge e)
        {
            bus = b;
            sources.add(s);
            edges.add(e);
        }

        public Bus getBus() { return bus; }
        
        public List getSources() { return sources; }
        
        public List getEdges() { return edges; }

        public void addSource (Node s, Edge e)
        {
            sources.add(s);
            edges.add(e);
        }
        
        public String toString()
        {
            return "Unresolved: bus: " + bus + " owner: " + bus.getOwner().getOwner();            
        }
        
    }

    public void visit (Design design)
    {
        /*
         * XXX -- We don't do designs yet.
         */
        scanner.enter(design);
    }

    public void visit (Task task)
    {
        /*
         * XXX -- nor tasks.
         */
        scanner.enter(task);
    }

    public void visit (Call call)
    {
        /*
         * XXX -- Calls are also tbd.
         */
        scanner.enter(call);
    }


    public void visit (Procedure procedure)
    {
        pushGraph(procedure);
        graph.setLabel(getName(procedure));

        procedure.getBody().accept(this);

        popGraph();
    }

    public void visit (Block block)
    {
        graphPreVisit(block);
        visit(block.getComponents());
        graphPostVisit(block);
    }

    public void visit (Branch branch)
    {
        graphPreVisit(branch);
        visit(branch.getComponents());
        graphPostVisit(branch);
    }

    public void visit (Decision decision)
    {
        graphPreVisit(decision);
        visit(decision.getComponents());
        graphPostVisit(decision);
    }

    public void visit (Loop loop)
    {
        graphPreVisit(loop);

        /*
         * Draw the feedback registers and input latches.
         */
        Collection registers = new LinkedList(loop.getDataRegisters());
        registers.add(loop.getControlRegister());
        for (Iterator iter = registers.iterator(); iter.hasNext();)
        {
            drawFeedbackRegister((Reg)iter.next());
        }

        final Collection components = new HashSet(loop.getComponents());
        components.removeAll(registers);
        visit(components);

        graphPostVisit(loop);

        for (Iterator iter = registers.iterator(); iter.hasNext();)
        {
            drawFeedbackInput((Reg)iter.next());
        }
    }

    private void visit (Collection components)
    {
        final LinkedList queue = new LinkedList(components);
        while (!queue.isEmpty())
        {
            final Component component = (Component)queue.removeFirst();
            if (isInputReady(component))
            {
                component.accept(this);
            }
            else
            {
                queue.add(component);
            }
        }
    }

    private void drawFeedbackRegister (Reg reg)
    {
        net.sf.openforge.util.graphviz.Record box = new net.sf.openforge.util.graphviz.Record("component" + nodeCount++);
        box.setLabel(getName(reg) + " (Cmp)");

        net.sf.openforge.util.graphviz.Record.Port main = box.getPort("main");
        net.sf.openforge.util.graphviz.Record.Port entryPort = main.getPort("entry");
        entryPort.setSeparated(false);

        net.sf.openforge.util.graphviz.Record.Port dataPort = entryPort.getPort("din");
        dataPort.setLabel("din");
        addToNodeMap(reg.getDataPort(), dataPort, DEPENDENCY);
            
        net.sf.openforge.util.graphviz.Record.Port bodyPort = main.getPort("body");
        bodyPort.setLabel(getName(reg));
        bodyPort.setSeparated(false);

        graphSingleExit(reg, main);

        addToNodeMap(reg, box, DEPENDENCY);
        graph.add(box);
    }

    private void drawFeedbackInput (Reg reg)
    {
        connectPort(reg.getDataPort(), true);
    }


    private boolean isInputReady (Component component)
    {
        final Port goPort = component.getGoPort();
        if (goPort.isConnected() && !nodeMapDeps.containsKey(goPort.getBus()))
        {
            return false;
        }

        if (drawCR)
        {
            final Port clockPort = component.getClockPort();
            if (clockPort.isConnected() && !nodeMapDeps.containsKey(clockPort.getBus()))
            {
                return false;
            }
            final Port resetPort = component.getResetPort();
            if (resetPort.isConnected() && !nodeMapDeps.containsKey(resetPort.getBus()))
            {
                return false;
            }
        }

        for (Iterator iter = component.getDataPorts().iterator(); iter.hasNext();)
        {
            final Port dataPort = (Port)iter.next();
            if (dataPort.isConnected() && !nodeMapDeps.containsKey(dataPort.getBus()))
            {
                return false;
            }
        }

        return true;
    }


    public void visit (Switch sw)
    {
        visit((Block)sw);
    }

    public void visit (WhileBody body)
    {
        graphPreVisit(body);
        visit(body.getComponents());
        graphPostVisit(body);
    }

    public void visit (UntilBody body)
    {
        graphPreVisit(body);
        visit(body.getComponents());
        graphPostVisit(body);
    }

    public void visit (Latch l)
    {
        if (isDetailed)
        {
            graphPreVisit(l);
            visit(l.getComponents());
            graphPostVisit(l);
        }
        else
        {
            graph((Component)l);
        }
    }

    public void visit (Scoreboard sb)
    {
        if (isDetailed)
        {
            graphPreVisit(sb);
            visit(sb.getComponents());
            graphPostVisit(sb);
        }
        else
        {
            graph((Component)sb);
        }
    }

    public void visit (InBuf buf) { graph(buf); }

    public void visit (OutBuf buf) { graph(buf); }

    public void visit (Reg reg) { graph(reg); }

    public void visit (Mux mux) { graph(mux); }

    public void visit (EncodedMux mux) { graph(mux); }

    public void visit (TimingOp op) { graph(op); }
    
    public void visit (TaskCall comp) 
    { 
        if (isDetailed)
        {
            graphPreVisit(comp);
            scanner.enter(comp);
            graphPostVisit(comp);
        }
        else
        {
            graph((Component)comp);
        }
    }
    
    public void visit (SimplePinAccess comp) 
    { 
        if (isDetailed)
        {
            graphPreVisit(comp);
            scanner.enter(comp);
            graphPostVisit(comp);
        }
        else
        {
            graph((Component)comp);
        }
    }

    public void visit (FifoAccess comp) 
    { 
        if (isDetailed)
        {
            graphPreVisit(comp);
            scanner.enter(comp);
            graphPostVisit(comp);
        }
        else
        {
            graph((Component)comp);
        }
    }

    public void visit (FifoRead comp)
    {
        visit((FifoAccess)comp);
    }
    
    public void visit (FifoWrite comp)
    {
        visit((FifoAccess)comp);
    }
    
    public void visit (PriorityMux pmux) 
    { 
        if (isDetailed)
        {
            graphPreVisit(pmux);
            scanner.enter(pmux);
            graphPostVisit(pmux);
        }
        else
        {
            graph((Component)pmux);
        }
    }

    public void visit (RegisterReferee regReferee)
    {
        if (isDetailed)
        {
            graphPreVisit(regReferee);
            scanner.enter(regReferee);
            graphPostVisit(regReferee);
        }
        else
        {
            graph((Component)regReferee);
        }
    }

    public void visit (MemoryReferee memReferee)
    {
        if (isDetailed)
        {
            graphPreVisit(memReferee);
            scanner.enter(memReferee);
            graphPostVisit(memReferee);
        }
        else
        {
            graph((Component)memReferee);
        }
    }
    
    public void visit (And and) { graph(and); }

    public void visit (Not not) { graph(not); }

    public void visit (Or or) { graph(or); }

    public void visit (AddOp op) { graph(op); }

    public void visit (AndOp op) { graph(op); }

    public void visit (NumericPromotionOp op) { graph(op); }

    public void visit (CastOp op) { graph(op); }

    public void visit (ComplementOp op) { graph(op); }

    public void visit (ConditionalAndOp op) { graph(op); }

    public void visit (ConditionalOrOp op) { graph(op); }

    public void visit (Constant op) { graph(op); }

    public void visit (DivideOp op) { graph(op); }
    
    public void visit (EqualsOp op) { graph(op); }

    public void visit (GreaterThanEqualToOp op) { graph(op); }

    public void visit (GreaterThanOp op) { graph(op); }

    public void visit (LeftShiftOp op) { graph(op); }

    public void visit (LessThanEqualToOp op) { graph(op); }

    public void visit (LessThanOp op) { graph(op); }

    public void visit (MinusOp op) { graph(op); }

    public void visit (ModuloOp op) { graph(op); }

    public void visit (MultiplyOp op) { graph(op); }

    public void visit (NotEqualsOp op) { graph(op); }

    public void visit (NoOp nop)
    {
        /*
         * If a NoOp has no data flows, then it is not significant: skip it.
         */
        boolean isNeeded = !nop.getDataPorts().isEmpty();
        if (!isNeeded)
        {
            for (Iterator iter = nop.getExits().iterator(); iter.hasNext();)
            {
                final Exit exit = (Exit)iter.next();
                if (!exit.getDataBuses().isEmpty())
                {
                    isNeeded = true;
                    break;
                }
            }
        }

        if (isNeeded)
        {
            graph(nop);
        }
    }

    public void visit (RegisterRead op) { graph(op); }

    public void visit (RegisterWrite op) { graph(op); }

    public void visit (NotOp op) { graph(op); }

    public void visit (OrOp op) { graph(op); }

    public void visit (PlusOp op) { graph(op); }

    public void visit (RightShiftOp op) { graph(op); }

    public void visit (RightShiftUnsignedOp op) { graph(op); }

    public void visit (ShortcutIfElseOp op) { graph(op); }

    public void visit (SubtractOp op) { graph(op); }

    public void visit (XorOp op) { graph(op); }
}
