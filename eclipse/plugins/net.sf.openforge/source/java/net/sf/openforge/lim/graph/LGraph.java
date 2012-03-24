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
 * $Id: LGraph.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package net.sf.openforge.lim.graph;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Composable;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.FilteredVisitor;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.util.graphviz.Box;
import net.sf.openforge.util.graphviz.Circle;
import net.sf.openforge.util.graphviz.Edge;
import net.sf.openforge.util.graphviz.Graph;
import net.sf.openforge.util.graphviz.Node;
import net.sf.openforge.util.graphviz.Record;
import net.sf.openforge.util.naming.ID;

/**
 * LGraph uses <a
 * href="http://www.research.att.com/sw/tools/graphviz/">GraphViz</a> to
 * generate an ASCII description of a LIM graph. A graph viewer, <i>dotty</i>
 * may then be used to view the generated ASCII. It may also be printed by: <i>
 * dot -Tps file.dot | lpr </i>
 * 
 * @version $Id: LGraph.java 2 2005-06-09 20:00:48Z imiller $
 */
public class LGraph extends FilteredVisitor implements Visitor {

	/** The top level Graph -- put all edges at this level */
	private Graph topGraph;

	/** The graph instance for the current level being visited */
	private Graph graph;

	/** True if the insides of Selectors and other primitive modules to be drawn */
	@SuppressWarnings("unused")
	private boolean isDetailed = false;
	/** show clock and reset */
	private boolean drawCR = false;
	/** show dependencies */
	private boolean graphDependencies = false;
	/** show physical conenctions */
	private boolean graphPhysical = false;
	/** show logical connections */
	private boolean graphLogical = false;
	/** show data connections */
	private boolean graphData = false;
	/** show control connections */
	private boolean graphControl = false;

	/** used to handle unresolved nodes in graphing */
	private Map<Bus, Unresolved> unresolvedNodes = new HashMap<Bus, Unresolved>();

	/**
	 * The graph stack; pushed when a new level is visited, popped when that
	 * level is exited.
	 */
	private LinkedList<Graph> graphStack = new LinkedList<Graph>();

	/** The number of nodes so far, used to generate unique identifiers */
	private int nodeCount = 0;

	/**
	 * Map of LIM object to graph Node - for dependencies and generic
	 * connections (exits, etc)
	 */
	private Map<Object, Object> nodeMapDeps = new HashMap<Object, Object>();
	/** Map of LIM object to graph Node - for physical connections */
	private Map<Object, Object> nodeMapPhys = new HashMap<Object, Object>();

	/** Weights for the various types of edges */
	private static final int WT_ENTRY = 1200;
	private static final int WT_EXIT = 800;
	private static final int WT_DEP = 4;
	private static final int WT_FEEDBK = 1;
	private static final int WT_ENTRYFEEDBACK = 12;

	/** show data connections */
	public static final int DATA = 0x1;
	/** show control connections */
	public static final int CONTROL = 0x2;
	/** show logical connections */
	public static final int LOGICAL = 0x4;
	/** used internally to denote any of the above */
	private static final int DEPENDENCY = DATA | CONTROL | LOGICAL;

	/** show clock & reset connections */
	public static final int CLOCKRESET = 0x20;
	/** show physical connections (bus/port) */
	public static final int PHYSICAL = 0x40;
	/**
	 * show detailed primitive contents: if set the insides of Selectors and
	 * other pass-through modules are to be graphed; if false, they will be
	 * graphed as opaque boxes
	 */
	public static final int DETAIL = 0x80;
	/** print in landscape mode */
	public static final int LANDSCAPE = 0x100;

	public static final int DEFAULT = DATA | CONTROL | LOGICAL | PHYSICAL;

	/**
	 * Constructs a new LGraph.
	 * 
	 * @param name
	 *            a title for the graph
	 * @param top
	 *            the LIM element whose contents are to be graphed; typically a
	 *            {@link Design}, but only the {@link Procedure Procedures} down
	 *            will be depicted
	 * @param flags
	 *            is a bitwise or of DATA, CONTROL, LOGICAL, CLOCKRESET,
	 *            PHYSICAL
	 */
	public LGraph(String name, Visitable top, int flags) {
		super();
		parseFlags(flags);

		graph = new Graph(name);
		// this.graph.setSize(7, 10); ruins the ratio statement below...
		graph.setLabel(name);
		// some attributes to make the graph smaller, and to print
		// to several pages when printed via: dot -Tps file.dot | lpr
		graph.setGVAttribute("ratio", "auto");
		graph.setGVAttribute("ranksep", ".5");
		graph.setGVAttribute("nodesep", ".12");
		graph.setGVAttribute("fontsize", "10");
		graph.setGVAttribute("fontname", "Helvetica");
		graph.setGVAttribute("page", "8.5,11.0");
		if ((flags & LANDSCAPE) == LANDSCAPE) {
			setLandscape();
		}
		topGraph = graph;

		if (top != null) {
			top.accept(this);
		}
	}

	/** parse the flags, setting fields as appropriate */
	private void parseFlags(int flags) {
		if ((flags & DATA) == DATA) {
			graphDependencies = true;
			graphData = true;
		}
		if ((flags & CONTROL) == CONTROL) {
			graphDependencies = true;
			graphControl = true;
		}
		if ((flags & LOGICAL) == LOGICAL) {
			graphDependencies = true;
			graphLogical = true;
		}
		if ((flags & CLOCKRESET) == CLOCKRESET) {
			drawCR = true;
		}
		if ((flags & PHYSICAL) == PHYSICAL) {
			graphPhysical = true;
		}
		if ((flags & DETAIL) == DETAIL) {
			isDetailed = true;
		}
	}

	/**
	 * Constructs a new LGraph that will not draw the insides of pass-through
	 * modules like Selector.
	 * 
	 * @param name
	 *            a title for the graph
	 * @param top
	 *            the LIM element whose contents are to be graphed; typically a
	 *            {@link Design}, but only the {@link Procedure Procedures} down
	 *            will be depicted
	 */
	public LGraph(String name, Visitable top) {
		this(name, top, DEFAULT);
	}

	/**
	 * Prints a text representation of this graph readable by the <i>dotty</i>
	 * viewer.
	 * 
	 * @param writer
	 *            the writer to receive the text
	 */
	public void print(PrintWriter writer) {
		// if there are any unresolved nodes at this point, they must have come
		// from
		// somewhere off the graph, so we add a circle node to the topgraph and
		// connect them
		if (unresolvedNodes.values().size() > 0) {
			List<Unresolved> urList = new ArrayList<Unresolved>(
					unresolvedNodes.values());

			for (Unresolved ur : urList) {
				Node target = (Node) nodeMapDeps.get(ur.getBus());
				if (target == null) {
					target = (Node) nodeMapPhys.get(ur.getBus());
				}

				if (target != null) {
					assert target != null : "Found an unresolved node in nodeMapDeps "
							+ ur.getBus()
							+ " "
							+ ur.getBus().getOwner().getOwner();

					Circle circle = new Circle("unknownSrc" + nodeCount++);
					circle.setLabel(getName(ur.getBus().getOwner().getOwner())
							+ "::"
							+ Integer.toHexString(ur.getBus().getOwner()
									.getOwner().hashCode()) + "::"
							+ Integer.toHexString(ur.getBus().hashCode()));
					topGraph.add(circle);
					addToNodeMap(ur.getBus(), circle, DEPENDENCY);
				}
			}
		}

		graph.print(writer);
		writer.flush();
	}

	/**
	 * causes the graph to be printed in landscape mode - this may or may not
	 * use less paper - it depends on the graph. hence it is an option
	 */
	public void setLandscape() {
		topGraph.setGVAttribute("rotate", "90");
	}

	private void pushGraph(Object obj) {
		graphStack.addFirst(graph);
		graph = graph.getSubgraph("cluster" + nodeCount++);
		addToNodeMap(obj, graph, DEPENDENCY);
	}

	private void popGraph() {
		graph = graphStack.removeFirst();
	}

	private static String getName(Object o, String defaultName) {
		return ID.showLogical(o);
	}

	private static String getName(Object o) {
		return ID.showLogical(o);
	}

	/**
	 * Draw the box for the component
	 * 
	 * if we are graphing physical connections, then draw the ports at the top
	 * of the component, else draw just the component
	 */
	private void graph(Component component) {
		if (graphPhysical) {
			Component portOwner;
			if (component instanceof InBuf) {
				portOwner = component.getOwner();
			} else {
				portOwner = component;
			}

			Record record = new Record("portRecord" + nodeCount++);
			record.setTitle(getName(component) + "::" + component.toString());
			record.setColor("green");
			addToNodeMap(component, record, DEPENDENCY);

			if (portOwner instanceof Mux) {
				int gindex = 0;
				for (Port go_port : ((Mux) portOwner).getGoPorts()) {
					String gname = "s" + gindex++;
					Record.Port go = record.getPort(gname);
					go.setLabel(gname);
					addToNodeMap(go_port, go, PHYSICAL);
				}
			} else if (portOwner instanceof EncodedMux) {
				Port selectPort = ((EncodedMux) portOwner).getSelectPort();
				String name = "sel";
				Record.Port sel = record.getPort(name);
				sel.setLabel(name);
				addToNodeMap(selectPort, sel, PHYSICAL);
			} else {
				Record.Port go = record.getPort("go");
				go.setLabel("g");
				addToNodeMap(portOwner.getGoPort(), go, PHYSICAL);
			}
			if (drawCR) {
				Record.Port clock = record.getPort("clk");
				clock.setLabel("c");
				addToNodeMap(portOwner.getClockPort(), clock, PHYSICAL);

				Record.Port reset = record.getPort("rst");
				reset.setLabel("r");
				addToNodeMap(portOwner.getResetPort(), reset, PHYSICAL);
			}

			int dindex = 0;

			for (Port port : portOwner.getDataPorts()) {
				String dname = "d" + dindex++;
				Record.Port data = record.getPort(dname);
				data.setLabel(dname);
				addToNodeMap(port, data, PHYSICAL);
			}

			graph.add(record);
			graphOutside(component, false);
			if (!(component instanceof InBuf)) // don't graph connections to
												// InBuf because the inbuf's
												// owners will graph them
			{
				// graph from the buses that feed the port owners ports to the
				// ports
				for (Port port : portOwner.getPorts()) {
					if (!drawCR
							&& ((port == portOwner.getClockPort()) || (port == portOwner
									.getResetPort()))) {
						continue;
					}
					if (port.getBus() != null) {
						graphPhysical(port.getBus(), port, false);
					}
				}
			}
		} else {
			Box box = new Box("component" + nodeCount++);
			box.setLabel(getName(component));
			addToNodeMap(component, box, DEPENDENCY);
			graph.add(box);

			graphOutside(component, false);
		}
		if (graphDependencies) {
			connectEntries(component, component);
		}
		connectExits(component, component);
	}

	private void graphPreVisit(Module module) {
		graphPreVisit(module, false);
	}

	/**
	 * previsit for a module
	 * 
	 * @param module
	 *            the module being visited
	 * @param feedback
	 *            true if the module has a feedback entry
	 */
	private void graphPreVisit(Module module, boolean feedback) {
		/*
		 * Graph external elements in the current graph.
		 */
		graphOutside(module, feedback);

		/*
		 * Push into a new graph for the module internals.
		 */
		pushGraph(module);
		graph.setLabel(getName(module) + "::"
				+ Integer.toHexString(module.hashCode()));
		graph.setColor("red");
	}

	private void graphPostVisit(Module module) {
		graphPostVisit(module, false);
	}

	private void graphPostVisit(Module module, boolean feedback) {
		InBuf inBuf = module.getInBuf();
		if (drawCR) {
			Node clock = (Node) nodeMapDeps.get(inBuf.getClockBus());
			clock.setLabel("c");
			Node reset = (Node) nodeMapDeps.get(inBuf.getResetBus());
			reset.setLabel("r");
		}

		if (graphDependencies) {
			Node go = (Node) nodeMapDeps.get(inBuf.getGoBus());
			go.setLabel("g");

			int index = 0;

			for (Bus bus : inBuf.getDataBuses()) {
				Node node = (Node) nodeMapDeps.get(bus);
				node.setLabel("d" + index++);
			}
		}
		/*
		 * Pop back out to connect externals to internals.
		 */
		popGraph();
		if (graphDependencies) {
			connectEntries(module, module.getInBuf(), feedback);
		}
		if (graphPhysical) {
			connectPorts(module, feedback);
		}
		connectExits(module);
	}

	private void connectEntries(Component component, Component target) {
		connectEntries(component, target, false);
	}

	/**
	 * Attach each Entry to its peer, or to its owner if there is no peer.
	 * 
	 * @param component
	 *            the component whose entries are to be attached
	 * @param target
	 *            the component to which the entries are attached
	 */
	private void connectEntries(Component component, Component target,
			boolean feedback) {
		Node targetNode = (Node) nodeMapDeps.get(target);
		int index = 0;
		for (Entry entry : component.getEntries()) {
			Node entryNode = (Node) nodeMapDeps.get(entry);
			Edge edge;
			if (feedback) {
				edge = new Edge(WT_ENTRYFEEDBACK);
			} else {
				edge = new Edge(WT_ENTRY);
			}
			edge.setLabel("en" + index++);
			edge.setDirection(Edge.DIR_NONE);
			// edge.setNodePorts("s","n");
			if (target != component) {
				/*
				 * If connecting to something inside the owner,(ie an InBuf)
				 * then use a dashed line.
				 */
				edge.setStyle(Edge.STYLE_DASHED);
			}
			topGraph.connect(entryNode, targetNode, edge);

			assert entryNode != null;
			assert targetNode != null;
		}
	}

	/**
	 * Connects the Exits of the component. If the Exit has a peer, a dashed
	 * line is drawn to it; else a solid line will be drawn to the component.
	 */
	private void connectExits(Component component) {
		connectExits(component, null);
	}

	/**
	 * Connects the Exits of a Component.
	 * 
	 * @param component
	 *            the component whose exits are to be connected
	 * @param target
	 *            the node to which the exits are connected; if null and there
	 *            is a peer, then a dashed line will be drawn to the peer;
	 *            otherwise a solid line will be drawn to the component
	 */
	private void connectExits(Component component, Component target) {
		int index = 0;
		for (Exit exit : component.getExits()) {
			Component workingTarget = target;
			Node exitNode = (Node) nodeMapDeps.get(exit);
			Edge edge = new Edge(WT_EXIT);
			edge.setLabel("ex" + index++);
			edge.setDirection(Edge.DIR_NONE);
			// System.err.println("Connecting exits:  component: "+component+", workingTarget: "+workingTarget+", exit: "+exit);
			// System.err.println("Connecting exits:  exit peer: "+exit.getPeer()+", index: "+(index-1));
			// if (exit.getPeer() != null)
			// {
			// _graph.d.whereAmI(exit.getPeer());
			// }

			// edge.setNodePorts("s","n");

			if (workingTarget == null) {
				workingTarget = exit.getPeer();
				if (workingTarget == null) {
					workingTarget = component;
				} else {
					edge.setStyle(Edge.STYLE_DASHED);
				}
			}

			Node targetNode = (Node) nodeMapDeps.get(workingTarget);
			topGraph.connect(targetNode, exitNode, edge);

			assert targetNode != null : "Couldn't find node for "
					+ workingTarget + ", source is: " + component;
			assert exitNode != null : "Couldn;t find exit node for "
					+ component + ", exit: " + exit;
		}
	}

	private void graphOutside(Component component, boolean feedback) {
		if (graphDependencies) {
			graphEntries(component, feedback);
		}
		// don't need to do physical "entry" (ports) because they are drawn as a
		// part of
		// the component (or inbuf if a module)
		graphExits(component);
	}

	private void graphEntries(Component component, boolean feedback) {
		for (Entry entry : component.getEntries()) {
			graphEntry(entry, feedback);
		}
	}

	private void graphEntry(Entry entry, boolean feedback) {
		Component component = entry.getOwner();
		Record record = new Record("entry" + nodeCount++);
		addToNodeMap(entry, record, DEPENDENCY);

		Record.Port go = record.getPort("go");
		go.setLabel("g");
		addToNodeMap(EntryPort.get(entry, entry.getGoPort()), go, DEPENDENCY);
		if (drawCR) {
			Record.Port clock = record.getPort("clk");
			clock.setLabel("c");
			addToNodeMap(EntryPort.get(entry, entry.getClockPort()), clock,
					DEPENDENCY);

			Record.Port reset = record.getPort("rst");
			reset.setLabel("r");
			addToNodeMap(EntryPort.get(entry, entry.getResetPort()), reset,
					DEPENDENCY);
		}

		int dindex = 0;
		for (Port port : component.getDataPorts()) {
			String dname = "d" + dindex++;
			Record.Port data = record.getPort(dname);
			data.setLabel(dname);
			addToNodeMap(EntryPort.get(entry, port), data, DEPENDENCY);
		}

		graph.add(record);

		for (Port port : entry.getPorts()) {
			if (!drawCR
					&& ((port == component.getClockPort()) || (port == component
							.getResetPort()))) {
				continue;
			}
			for (Dependency dep : entry.getDependencies(port)) {
				graphDependency(dep, feedback);
			}
		}
	}

	private void connectPorts(Component component, boolean feedback) {
		for (Port port : component.getPorts()) {
			if (port.getBus() == null
					|| !drawCR
					&& ((port == component.getClockPort()) || (port == component
							.getResetPort()))) {
				continue;
			}
			graphPhysical(port.getBus(), port, feedback);
		}
	}

	private void graphExits(Component component) {
		int index = 0;
		for (Exit exit : component.getExits()) {
			graphExit(exit, index++);
		}
	}

	private void graphExit(Exit exit, int id) {
		@SuppressWarnings("unused")
		Component component = exit.getOwner();
		Record record = new Record("exit" + nodeCount++);
		record.setTitle(getName(exit) + "::"
				+ Integer.toHexString(exit.hashCode()));
		addToNodeMap(exit, record, DEPENDENCY);

		Record.Port done = record.getPort("done");
		done.setLabel("D");
		addToNodeMap(exit.getDoneBus(), done, DEPENDENCY);

		int dindex = 0;
		Collection<Bus> dataBuses = new LinkedHashSet<Bus>(exit.getDataBuses());
		if (!drawCR) {
			if (exit.getOwner() instanceof InBuf) {
				// remove the clock and reset bus
				dataBuses.remove(((InBuf) exit.getOwner()).getClockBus());
				dataBuses.remove(((InBuf) exit.getOwner()).getResetBus());
			}
		}
		for (Bus bus : dataBuses) {
			String dname = "d" + dindex++;
			Record.Port data = record.getPort(dname);
			data.setLabel(dname);
			addToNodeMap(bus, data, DEPENDENCY);
		}

		graph.add(record);
	}

	private void graphDependency(Dependency dep, boolean feedback) {
		Object key = EntryPort.get(dep.getEntry(), dep.getPort());
		Node source = (Node) nodeMapDeps.get(key);

		Bus logicalBus = dep.getLogicalBus();
		if (graphLogical
				&& ((graphControl && dep instanceof ControlDependency) || (graphData && dep instanceof DataDependency))
				&& logicalBus != null) {
			Node target = (Node) nodeMapDeps.get(logicalBus);
			Edge edge;
			if (feedback) {
				edge = new Edge(WT_FEEDBK);
				edge.setGVAttribute("constraint", "false");
			} else {
				edge = new Edge(WT_DEP);
			}
			edge.setLabel(getName(logicalBus, " "));
			edge.setDirection(Edge.DIR_BACK);
			edge.setStyle(Edge.STYLE_DOTTED);
			// edge.setNodePorts("s","n");

			if (target != null) {
				topGraph.connect(target, source, edge);
			} else {
				Unresolved ur = unresolvedNodes.get(logicalBus);
				if (ur == null) {
					ur = new Unresolved(logicalBus, source, edge);
					unresolvedNodes.put(logicalBus, ur);
				} else// already exists in the unresolved list, just
						// add our source and edge to the list
				{
					ur.addSource(source, edge);
				}
			}

			assert source != null : "source: " + key;
		}
	}

	private void graphPhysical(Bus bus, Port port, boolean feedback) {
		assert bus != null;

		Node source = (Node) nodeMapPhys.get(bus);
		if (source == null) {
			Object o = nodeMapDeps.get(bus);
			if (o instanceof Node) {
				source = (Node) o;
			} else {
				assert source == null;
			}
		}

		Node target = (Node) nodeMapPhys.get(port);
		if (target == null) {
			target = (Node) nodeMapDeps.get(port);
		}

		Edge edge;
		if (feedback) {
			edge = new Edge(WT_FEEDBK);
		} else {
			edge = new Edge(WT_DEP);
		}
		edge.setLabel(getName(bus, " "));
		assert target != null : "Problem with missing target in graphPhysical("
				+ bus + "," + port + ")";
		edge.setColor("purple");

		edge.setDirection(Edge.DIR_FORWARD);
		// edge.setStyle(Edge.STYLE_BOLD);
		// edge.setNodePorts("s","n");

		if (source != null) {
			topGraph.connect(source, target, edge);
		} else {
			Unresolved ur = unresolvedNodes.get(bus);
			if (ur == null) {
				ur = new Unresolved(bus, target, edge);
				unresolvedNodes.put(bus, ur);
			} else// already exists in the unresolved list, just add our source
					// and edge to the list
			{
				ur.addSource(target, edge);
			}
		}
	}

	/**
	 * type defines which nodeMap to look into - DEPENDENCY==nodeMapDeps,
	 * PHYSICAL==nodeMapPhys
	 */
	private void addToNodeMap(Object key, Object value, int type) {
		assert type == PHYSICAL || type == DEPENDENCY : "Illegal type to addToNodeMap";
		assert value != null;
		assert key != null;

		if (type == PHYSICAL) {
			nodeMapPhys.put(key, value);
		} else {
			nodeMapDeps.put(key, value);
		}

		if (unresolvedNodes.containsKey(key)) {
			Unresolved ur = unresolvedNodes.get(key);
			Node target = (Node) nodeMapDeps.get(ur.getBus());
			assert target != null : "How can the target still be null!";
			List<Node> sources = ur.getSources();
			List<Edge> edges = ur.getEdges();
			for (int i = 0; i < sources.size(); i++) {
				topGraph.connect(target, sources.get(i), edges.get(i));
			}
			unresolvedNodes.remove(key);
		}
	}

	/**
	 * holds unresolved edges, allows adding new edges to an existing bus (key
	 * in the hashMap), and retrieval of the edges
	 */
	private static class Unresolved {
		private Bus bus;
		private List<Node> sources = new ArrayList<Node>();
		private List<Edge> edges = new ArrayList<Edge>();

		Unresolved(Bus b, Node s, Edge e) {
			bus = b;
			sources.add(s);
			edges.add(e);
		}

		public Bus getBus() {
			return bus;
		}

		public List<Node> getSources() {
			return sources;
		}

		public List<Edge> getEdges() {
			return edges;
		}

		public void addSource(Node s, Edge e) {
			sources.add(s);
			edges.add(e);
		}

		@Override
		public String toString() {
			return "Unresolved: bus: " + bus + " owner: "
					+ bus.getOwner().getOwner();
		}

	}

	private static class EntryPort {
		private Entry entry;
		private Port port;

		static EntryPort get(Entry entry, Port port) {
			return new EntryPort(entry, port);
		}

		EntryPort(Entry entry, Port port) {
			this.entry = entry;
			this.port = port;
		}

		@Override
		public int hashCode() {
			return entry.hashCode() + port.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof EntryPort) {
				EntryPort ep = (EntryPort) obj;
				return ep.entry == entry && ep.port == port;
			}
			return false;
		}
	}

	@Override
	public void visit(Procedure procedure) {
		pushGraph(procedure);
		graph.setLabel(getName(procedure));

		procedure.getBody().accept(this);

		popGraph();
	}

	@Override
	public void preFilter(Module m) {
		if (!(m instanceof Composable))
			graphPreVisit(m);
	}

	@Override
	public void filter(Module m) {
		if (!(m instanceof Composable))
			graphPostVisit(m);
	}

	@Override
	public void filterAny(Component c) {
		graph(c);
	}
}
