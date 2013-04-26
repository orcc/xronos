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
 * $Id: LDependencyGraph.java 538 2007-11-21 06:22:39Z imiller $
 *
 * 
 */

package org.xronos.openforge.lim.graph;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.ClockDependency;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.PinRead;
import org.xronos.openforge.lim.PinWrite;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.PriorityMux;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterReferee;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.ResetDependency;
import org.xronos.openforge.lim.ResourceDependency;
import org.xronos.openforge.lim.Scoreboard;
import org.xronos.openforge.lim.Switch;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.WaitDependency;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimplePinAccess;
import org.xronos.openforge.lim.io.SimplePinRead;
import org.xronos.openforge.lim.io.SimplePinWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.AggregateConstant;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryReferee;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.ComplementOp;
import org.xronos.openforge.lim.op.ConditionalAndOp;
import org.xronos.openforge.lim.op.ConditionalOrOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.EqualsOp;
import org.xronos.openforge.lim.op.GreaterThanEqualToOp;
import org.xronos.openforge.lim.op.GreaterThanOp;
import org.xronos.openforge.lim.op.LeftShiftOp;
import org.xronos.openforge.lim.op.LessThanEqualToOp;
import org.xronos.openforge.lim.op.LessThanOp;
import org.xronos.openforge.lim.op.MinusOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.NoOp;
import org.xronos.openforge.lim.op.NotEqualsOp;
import org.xronos.openforge.lim.op.NotOp;
import org.xronos.openforge.lim.op.NumericPromotionOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.PlusOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.RightShiftUnsignedOp;
import org.xronos.openforge.lim.op.ShortcutIfElseOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.TimingOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Mux;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.util.graphviz.Circle;
import org.xronos.openforge.util.graphviz.Edge;
import org.xronos.openforge.util.graphviz.Graph;
import org.xronos.openforge.util.graphviz.Node;
import org.xronos.openforge.util.naming.ID;

/**
 * A hack of {@link LGraph} with reduced complexity. Single {@link Entry
 * Entries} and {@link Exit Exits} are drawn as part of the main node.
 * {@link NoOp NoOps} with no data flows are omitted.
 * <P>
 * Only the dependencies connections are drawn, not the physical connections.
 * 
 * @version $Id: LDependencyGraph.java 538 2007-11-21 06:22:39Z imiller $
 */
public class LDependencyGraph extends DefaultVisitor {

	private static class EntryPort {
		static EntryPort get(Entry entry, Port port) {
			return new EntryPort(entry, port);
		}

		private Entry entry;

		private Port port;

		EntryPort(Entry entry, Port port) {
			this.entry = entry;
			this.port = port;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof EntryPort) {
				EntryPort ep = (EntryPort) obj;
				return ep.entry == entry && ep.port == port;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return entry.hashCode() + port.hashCode();
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

		public void addSource(Node s, Edge e) {
			sources.add(s);
			edges.add(e);
		}

		public Bus getBus() {
			return bus;
		}

		public List<Edge> getEdges() {
			return edges;
		}

		public List<Node> getSources() {
			return sources;
		}

		@Override
		public String toString() {
			return "Unresolved: bus: " + bus + " owner: "
					+ bus.getOwner().getOwner();
		}

	}

	/** Use a scanner for traversing module contents */
	private org.xronos.openforge.lim.Scanner scanner;

	/** The top level Graph -- put all edges at this level */
	private Graph topGraph;

	/** The graph instance for the current level being visited */
	private Graph graph;
	private static final boolean graphComposable = false;
	/** True if the insides of Selectors and other primitive modules to be drawn */
	@SuppressWarnings("unused")
	private boolean isDetailed = false;
	/** show clock and reset */
	private boolean drawCR = false;
	/** show dependencies */
	private boolean graphDependencies = false;
	/** show logical connections */
	private boolean graphLogical = false;
	/** show data connections */
	@SuppressWarnings("unused")
	private boolean graphData = false;
	/** show control connections */
	@SuppressWarnings("unused")
	private boolean graphControl = false;

	/** show driving exits */
	@SuppressWarnings("unused")
	private boolean graphDExits = false;

	@SuppressWarnings("unused")
	private boolean hashcodes = false;

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
	private static final int WT_EXIT = 8000;
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
	private static final int DEXITS = 0x8;
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

	/** Include hashcode in compnent name */
	public static final int HASHCODES = 0x200;

	public static final int DEFAULT = DATA | CONTROL | LOGICAL | PHYSICAL;

	public static void graphTo(Module module, String filename) {
		try {
			LDependencyGraph graph = new LDependencyGraph(module.toString(),
					module);
			graph.print(new PrintWriter(new FileOutputStream(filename)));
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	/**
	 * Constructs a new LDependencyGraph that will not draw the insides of
	 * pass-through modules like Selector.
	 * 
	 * @param name
	 *            a title for the graph
	 * @param top
	 *            the LIM element whose contents are to be graphed; typically a
	 *            {@link Design}, but only the {@link Procedure Procedures} down
	 *            will be depicted
	 */
	public LDependencyGraph(String name, Visitable top) {
		this(name, top, DEFAULT);
	}

	/**
	 * Constructs a new LDependencyGraph.
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
	public LDependencyGraph(String name, Visitable top, int flags) {
		super();
		parseFlags(DEFAULT | DEXITS); // flags);

		scanner = new org.xronos.openforge.lim.Scanner(this);

		graph = new Graph(name);
		// this.graph.setSize(7, 10); ruins the ratio statement below...
		graph.setLabel(name);
		// some attributes to make the graph smaller, and to print
		// to several pages when printed via: dot -Tps file.dot | lpr
		graph.setGVAttribute("ratio", "auto");
		graph.setGVAttribute("ranksep", ".1");
		graph.setGVAttribute("nodesep", ".12");
		graph.setGVAttribute("fontsize", "10");
		graph.setGVAttribute("fontname", "Helvetica");
		graph.setGVAttribute("page", "8.5,11.0");
		if ((flags & LANDSCAPE) == LANDSCAPE) {
			setLandscape();
		}
		topGraph = graph;

		top.accept(this);
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

	private void connectEntries(Component component, Component target) {
		if (component.getEntries().size() > 1) {
			connectEntries(component, target, false);
		} else if (component.getEntries().size() == 1) {
			graphDependencies(component, component.getMainEntry(), false);
		}
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

			topGraph.connect(entryNode, targetNode, edge);

			assert entryNode != null;
			assert targetNode != null;

			/*
			 * if((graphDExits)&&(entry.getDrivingExit()!=null)) { edge = new
			 * Edge(WT_ENTRY); edge.setLabel("dexit" + index++);
			 * edge.setDirection(Edge.DIR_BACK); edge.setColor("orange"); Exit
			 * ex=entry.getDrivingExit(); Node exitNode =
			 * (Node)nodeMapDeps.get(ex); assert exitNode != null;
			 * topGraph.connect(exitNode,entryNode,edge); }
			 */

		}
	}

	private void connectEntries(Module module, Component target,
			boolean feedback) {
		connectEntries((Component) module, target, feedback);
	}

	/**
	 * Connects the Exits of the component. If the Exit has a peer, a dashed
	 * line is drawn to it; else a solid line will be drawn to the component.
	 */
	@SuppressWarnings("unused")
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

			if (workingTarget == null) {
				workingTarget = exit.getPeer();
				if (workingTarget == null) {
					workingTarget = component;
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

	private void connectExits(Module module) {
		connectExits(module, null);
	}

	private String getName(Object o) {
		String id = ID.showLogical(o);

		if (o instanceof Call) {
			return id + " [" + Integer.toHexString(o.hashCode()) + "]";
		} else if (o instanceof Constant) {
			if (o instanceof AggregateConstant) {
				return "agg" + id;
			}
			return id + "=" + ConstantNode.value((Constant) o);
		} else {
			return id + ":" + Integer.toHexString(o.hashCode());
			// return ID.showLogical(o);
		}
	}

	private String getName(Object o, String defaultName) {
		// return ID.showLogical(o);
		return getName(o);
	}

	/**
	 * Draw the box for the component
	 * 
	 * if we are graphing physical connections, then draw the ports at the top
	 * of the component, else draw just the component
	 */
	private void graph(Component component) {
		org.xronos.openforge.util.graphviz.Record box = new org.xronos.openforge.util.graphviz.Record(
				"component" + nodeCount++);
		box.setLabel(getName(component) + " (Cmp)");

		org.xronos.openforge.util.graphviz.Record.Port main = box
				.getPort("main");
		if (component.getEntries().size() == 1) {
			final Entry entry = component.getEntries().iterator().next();
			org.xronos.openforge.util.graphviz.Record.Port entryPort = main
					.getPort("entry");
			addToNodeMap(entry, entryPort, DEPENDENCY);
			entryPort.setSeparated(false);

			org.xronos.openforge.util.graphviz.Record.Port goPort = entryPort
					.getPort("go");
			goPort.setLabel("g");
			goPort.setSeparated(true);
			addToNodeMap(EntryPort.get(entry, entry.getGoPort()), goPort,
					DEPENDENCY);

			if (drawCR) {
				org.xronos.openforge.util.graphviz.Record.Port clockPort = entryPort
						.getPort("clock");
				clockPort.setLabel("c");
				clockPort.setSeparated(true);
				addToNodeMap(EntryPort.get(entry, entry.getClockPort()),
						clockPort, DEPENDENCY);

				org.xronos.openforge.util.graphviz.Record.Port resetPort = entryPort
						.getPort("reset");
				resetPort.setLabel("r");
				resetPort.setSeparated(true);
				addToNodeMap(EntryPort.get(entry, entry.getResetPort()),
						resetPort, DEPENDENCY);
			}

			for (int i = 0; i < component.getDataPorts().size(); i++) {
				Port dp = component.getDataPorts().get(i);
				String label = "d" + i;
				org.xronos.openforge.util.graphviz.Record.Port dataPort = entryPort
						.getPort("p_" + label);
				if (dp == component.getThisPort()) {
					label += "'";
				}
				dataPort.setLabel(label);
				dataPort.setSeparated(true);
				addToNodeMap(EntryPort.get(entry, dp), dataPort, DEPENDENCY);
			}
		}

		org.xronos.openforge.util.graphviz.Record.Port bodyPort = main
				.getPort("body");
		bodyPort.setLabel(getName(component));
		bodyPort.setSeparated(false);

		if (component.getExits().size() == 1) {
			if (component instanceof InBuf) {
				graphSingleExit((InBuf) component, main);
			} else {
				graphSingleExit(component, main);
			}
		}

		addToNodeMap(component, box, DEPENDENCY);
		graph.add(box);

		graphOutside(component, false);

		connectEntries(component, component);
		if (component.getExits().size() > 1) {
			connectExits(component, component);
		}
	}

	private void graphDependencies(Component component, Entry entry,
			boolean feedback) {
		for (Port port : entry.getPorts()) {
			if (!drawCR
					&& (port == component.getClockPort() || port == component
							.getResetPort())) {
				continue;
			}

			for (Dependency dep : entry.getDependencies(port)) {
				graphDependency(dep, feedback);
			}
		}
	}

	private void graphDependency(Dependency dep, boolean feedback) {
		Object key = EntryPort.get(dep.getEntry(), dep.getPort());
		Node source = (Node) nodeMapDeps.get(key);

		Bus logicalBus = dep.getLogicalBus();
		if (graphLogical && logicalBus != null) {
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
			if (dep instanceof ClockDependency) {
				edge.setColor("yellow3");
			} else if (dep instanceof ResetDependency) {
				edge.setColor("orange");
			} else if (dep instanceof DataDependency) {
				edge.setColor("black");
			} else if (dep instanceof ResourceDependency) {
				edge.setColor("indianred");
			} else if (dep instanceof WaitDependency) {
				edge.setColor("blue");
			} else if (dep instanceof ControlDependency) {
				edge.setColor("green3");
			}

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

	private void graphEntries(Component component, boolean feedback) {
		for (Entry entry : component.getEntries()) {
			graphEntry(entry, feedback);
		}
	}

	private void graphEntry(Entry entry, boolean feedback) {
		Component component = entry.getOwner();
		org.xronos.openforge.util.graphviz.Record record = new org.xronos.openforge.util.graphviz.Record(
				"entry" + nodeCount++);
		addToNodeMap(entry, record, DEPENDENCY);

		org.xronos.openforge.util.graphviz.Record.Port go = record
				.getPort("go");
		go.setLabel("g");
		go.setSeparated(true);
		addToNodeMap(EntryPort.get(entry, entry.getGoPort()), go, DEPENDENCY);
		if (drawCR) {
			org.xronos.openforge.util.graphviz.Record.Port clock = record
					.getPort("clk");
			clock.setLabel("c");
			clock.setSeparated(true);
			addToNodeMap(EntryPort.get(entry, entry.getClockPort()), clock,
					DEPENDENCY);

			org.xronos.openforge.util.graphviz.Record.Port reset = record
					.getPort("rst");
			reset.setLabel("r");
			reset.setSeparated(true);
			addToNodeMap(EntryPort.get(entry, entry.getResetPort()), reset,
					DEPENDENCY);
		}

		int dindex = 0;
		for (Port port : component.getDataPorts()) {
			String dname = "d" + dindex++;
			org.xronos.openforge.util.graphviz.Record.Port data = record
					.getPort(dname);
			if (port == component.getThisPort()) {
				dname += "'";
			}
			data.setLabel(dname);
			data.setSeparated(true);
			addToNodeMap(EntryPort.get(entry, port), data, DEPENDENCY);
		}

		graph.add(record);
		graphDependencies(component, entry, feedback);
	}

	private void graphExit(Exit exit, int id) {
		// Component component = exit.getOwner();

		org.xronos.openforge.util.graphviz.Record record = new org.xronos.openforge.util.graphviz.Record(
				"exit" + nodeCount++);
		addToNodeMap(exit, record, DEPENDENCY);

		org.xronos.openforge.util.graphviz.Record.Port done = record
				.getPort("done");
		done.setLabel("D");
		done.setSeparated(true);
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
			org.xronos.openforge.util.graphviz.Record.Port data = record
					.getPort(dname);
			data.setLabel(dname);
			data.setSeparated(true);
			addToNodeMap(bus, data, DEPENDENCY);
		}

		graph.add(record);
	}

	private void graphExits(Component component) {
		int index = 0;
		for (Exit exit : component.getExits()) {
			graphExit(exit, index++);
		}
	}

	private void graphOutside(Component component, boolean feedback) {
		if (component.getEntries().size() > 1) {
			graphEntries(component, feedback);
		}

		// don't need to do physical "entry" (ports) because they are drawn as a
		// part of
		// the component (or inbuf if a module)

		if (component.getExits().size() > 1) {
			graphExits(component);
		}
	}

	private void graphOutside(Module module, boolean feedback) {
		graphEntries(module, feedback);
		graphExits(module);
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
		Node go = (Node) nodeMapDeps.get(inBuf.getGoBus());
		go.setLabel("g");
		int index = 0;

		for (Bus bus : inBuf.getDataBuses()) {
			Node node = (Node) nodeMapDeps.get(bus);
			String label = "d" + index++;
			if (bus == inBuf.getThisBus()) {
				label += "'";
			}
			node.setLabel(label);
		}
		/*
		 * Pop back out to connect externals to internals.
		 */
		popGraph();
		if (graphDependencies) {
			connectEntries(module, module.getInBuf(), feedback);
		}
		connectExits(module);
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

	private void graphSingleExit(Component component,
			org.xronos.openforge.util.graphviz.Record node) {
		final Exit exit = component.getExits().iterator().next();
		org.xronos.openforge.util.graphviz.Record.Port exitPort = node
				.getPort("exit");
		addToNodeMap(exit, exitPort, DEPENDENCY);

		org.xronos.openforge.util.graphviz.Record.Port donePort = exitPort
				.getPort("done");
		donePort.setLabel("D");
		donePort.setSeparated(true);
		addToNodeMap(exit.getDoneBus(), donePort, DEPENDENCY);
		for (int i = 0; i < exit.getDataBuses().size(); i++) {
			Bus dBus = exit.getDataBuses().get(i);
			String label = "d" + i;
			org.xronos.openforge.util.graphviz.Record.Port dataPort = exitPort
					.getPort("b_" + label);
			dataPort.setLabel(label);
			dataPort.setSeparated(true);
			addToNodeMap(dBus, dataPort, DEPENDENCY);
		}
	}

	private void graphSingleExit(InBuf inbuf,
			org.xronos.openforge.util.graphviz.Record node) {
		final Exit exit = inbuf.getExits().iterator().next();
		org.xronos.openforge.util.graphviz.Record.Port exitPort = node
				.getPort("exit");
		addToNodeMap(exit, exitPort, DEPENDENCY);

		org.xronos.openforge.util.graphviz.Record.Port goPort = exitPort
				.getPort("go");
		goPort.setLabel("G");
		goPort.setSeparated(true);
		addToNodeMap(inbuf.getGoBus(), goPort, DEPENDENCY);

		if (drawCR) {
			org.xronos.openforge.util.graphviz.Record.Port clockPort = exitPort
					.getPort("clock");
			clockPort.setLabel("clk");
			clockPort.setSeparated(true);
			addToNodeMap(inbuf.getClockBus(), clockPort, DEPENDENCY);

			org.xronos.openforge.util.graphviz.Record.Port resetPort = exitPort
					.getPort("reset");
			resetPort.setLabel("rst");
			resetPort.setSeparated(true);
			addToNodeMap(inbuf.getResetBus(), resetPort, DEPENDENCY);
		}

		int i = 0;
		for (Bus dBus : inbuf.getDataBuses()) {
			String label = "d" + i;
			org.xronos.openforge.util.graphviz.Record.Port dataPort = exitPort
					.getPort("b_" + label);
			if (inbuf.getThisBus() == dBus) {
				label += "'";
			}
			dataPort.setLabel(label);
			dataPort.setSeparated(true);
			addToNodeMap(dBus, dataPort, DEPENDENCY);
			i++;
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
		if ((flags & DETAIL) == DETAIL) {
			isDetailed = true;
		}
		if ((flags & HASHCODES) == HASHCODES) {
			hashcodes = true;
		}
		if ((flags & DEXITS) == DEXITS) {
			graphDExits = true;
		}
	}

	private void popGraph() {
		graph = graphStack.removeFirst();
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
			ArrayList<Unresolved> urList = new ArrayList<Unresolved>(
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

	private void pushGraph(Object obj) {
		graphStack.addFirst(graph);
		graph = graph.getSubgraph("cluster" + nodeCount++);
		addToNodeMap(obj, graph, DEPENDENCY);
	}

	/**
	 * causes the graph to be printed in landscape mode - this may or may not
	 * use less paper - it depends on the graph. hence it is an option
	 */
	public void setLandscape() {
		topGraph.setGVAttribute("rotate", "90");
	}

	@Override
	public void visit(AbsoluteMemoryRead comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	@Override
	public void visit(AbsoluteMemoryWrite comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	@Override
	public void visit(AddOp op) {
		graph(op);
	}

	@Override
	public void visit(And and) {
		graph(and);
	}

	@Override
	public void visit(AndOp op) {
		graph(op);
	}

	@Override
	public void visit(ArrayRead comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	@Override
	public void visit(ArrayWrite comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	@Override
	public void visit(Block block) {
		graphPreVisit(block);
		scanner.enter(block);
		graphPostVisit(block);
	}

	@Override
	public void visit(Branch branch) {
		graphPreVisit(branch);
		scanner.enter(branch);
		graphPostVisit(branch);
	}

	// public void visit (Switch.Case caze)
	// {
	// graphPreVisit(caze);
	// scanner.enter(caze);
	// graphPostVisit(caze);
	// }

	// public void visit (Switch.SwitchController swc)
	// {
	// graphPreVisit(swc);
	// scanner.enter(swc);
	// graphPostVisit(swc);
	// }

	@Override
	public void visit(Call call) {
		/*
		 * XXX -- Calls are also tbd.
		 */
		graph(call);
		scanner.enter(call);
	}

	@Override
	public void visit(CastOp op) {
		graph(op);
	}

	@Override
	public void visit(ComplementOp op) {
		graph(op);
	}

	@Override
	public void visit(ConditionalAndOp op) {
		graph(op);
	}

	@Override
	public void visit(ConditionalOrOp op) {
		graph(op);
	}

	@Override
	public void visit(Constant op) {
		graph(op);
	}

	@Override
	public void visit(Decision decision) {
		graphPreVisit(decision);
		scanner.enter(decision);
		graphPostVisit(decision);
	}

	@Override
	public void visit(Design design) {
		/*
		 * XXX -- We don't do designs yet.
		 */
		scanner.enter(design);
	}

	@Override
	public void visit(DivideOp op) {
		graph(op);
	}

	@Override
	public void visit(EncodedMux mux) {
		graph(mux);
	}

	@Override
	public void visit(EqualsOp op) {
		graph(op);
	}

	@Override
	public void visit(FifoAccess comp) {
		if (graphComposable) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	@Override
	public void visit(FifoRead comp) {
		if (graphComposable) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	@Override
	public void visit(FifoWrite comp) {
		if (graphComposable) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	@Override
	public void visit(ForBody body) {
		graphPreVisit(body);
		scanner.enter(body);
		graphPostVisit(body);
	}

	@Override
	public void visit(GreaterThanEqualToOp op) {
		graph(op);
	}

	@Override
	public void visit(GreaterThanOp op) {
		graph(op);
	}

	@Override
	public void visit(HeapRead comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	@Override
	public void visit(HeapWrite comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	@Override
	public void visit(InBuf buf) {
		graph(buf);
	}

	@Override
	public void visit(Latch latch) {
		graph(latch);
		// graphPreVisit(latch);
		// scanner.enter(latch);
		// graphPostVisit(latch);
	}

	@Override
	public void visit(LeftShiftOp op) {
		graph(op);
	}

	@Override
	public void visit(LessThanEqualToOp op) {
		graph(op);
	}

	@Override
	public void visit(LessThanOp op) {
		graph(op);
	}

	@Override
	public void visit(Loop loop) {
		graphPreVisit(loop);
		scanner.enter(loop);
		graphPostVisit(loop);
	}

	@Override
	public void visit(MemoryRead op) {
		graph(op);
	}

	@Override
	public void visit(MemoryReferee memReferee) {
		graphPreVisit(memReferee);
		scanner.enter(memReferee);
		graphPostVisit(memReferee);
	}

	@Override
	public void visit(MemoryWrite op) {
		graph(op);
	}

	@Override
	public void visit(MinusOp op) {
		graph(op);
	}

	@Override
	public void visit(ModuloOp op) {
		graph(op);
	}

	@Override
	public void visit(MultiplyOp op) {
		graph(op);
	}

	@Override
	public void visit(Mux mux) {
		graph(mux);
	}

	@Override
	public void visit(NoOp nop) {
		/*
		 * If a NoOp has no data flows, then it is not significant: skip it.
		 */
		boolean isNeeded = !nop.getDataPorts().isEmpty();
		if (!isNeeded) {
			for (Exit exit : nop.getExits()) {
				if (!exit.getDataBuses().isEmpty()) {
					isNeeded = true;
					break;
				}
			}
		}

		if (isNeeded) {
			graph(nop);
		}
	}

	@Override
	public void visit(Not not) {
		graph(not);
	}

	@Override
	public void visit(NotEqualsOp op) {
		graph(op);
	}

	@Override
	public void visit(NotOp op) {
		graph(op);
	}

	@Override
	public void visit(NumericPromotionOp op) {
		graph(op);
	}

	@Override
	public void visit(Or or) {
		graph(or);
	}

	@Override
	public void visit(OrOp op) {
		graph(op);
	}

	@Override
	public void visit(OutBuf buf) {
		graph(buf);
	}

	@Override
	public void visit(PinRead op) {
		graph(op);
	}

	@Override
	public void visit(PinWrite op) {
		graph(op);
	}

	@Override
	public void visit(PlusOp op) {
		graph(op);
	}

	@Override
	public void visit(PriorityMux pmux) {
		graphPreVisit(pmux);
		scanner.enter(pmux);
		graphPostVisit(pmux);
	}

	@Override
	public void visit(Procedure procedure) {
		pushGraph(procedure);
		graph.setLabel(getName(procedure));

		procedure.getBody().accept(this);

		popGraph();
	}

	@Override
	public void visit(Reg reg) {
		graph(reg);
	}

	@Override
	public void visit(RegisterRead op) {
		graph(op);
	}

	@Override
	public void visit(RegisterReferee regReferee) {
		graphPreVisit(regReferee);
		scanner.enter(regReferee);
		graphPostVisit(regReferee);
	}

	@Override
	public void visit(RegisterWrite op) {
		graph(op);
	}

	@Override
	public void visit(RightShiftOp op) {
		graph(op);
	}

	@Override
	public void visit(RightShiftUnsignedOp op) {
		graph(op);
	}

	@Override
	public void visit(Scoreboard scbd) {
		graph(scbd);
	}

	@Override
	public void visit(ShortcutIfElseOp op) {
		graph(op);
	}

	@Override
	public void visit(SimplePinAccess comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	@Override
	public void visit(SimplePinRead op) {
		graph(op);
	}

	@Override
	public void visit(SimplePinWrite op) {
		graph(op);
	}

	@Override
	public void visit(SubtractOp op) {
		graph(op);
	}

	@Override
	public void visit(Switch sw) {
		graphPreVisit(sw);
		scanner.enter(sw);
		graphPostVisit(sw);
	}

	@Override
	public void visit(Task task) {
		/*
		 * XXX -- nor tasks.
		 */
		scanner.enter(task);
	}

	@Override
	public void visit(TaskCall comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	@Override
	public void visit(TimingOp op) {
		graph(op);
	}

	@Override
	public void visit(UntilBody body) {
		graphPreVisit(body);
		scanner.enter(body);
		graphPostVisit(body);
	}

	@Override
	public void visit(WhileBody body) {
		graphPreVisit(body);
		scanner.enter(body);
		graphPostVisit(body);
	}

	@Override
	public void visit(XorOp op) {
		graph(op);
	}
}
