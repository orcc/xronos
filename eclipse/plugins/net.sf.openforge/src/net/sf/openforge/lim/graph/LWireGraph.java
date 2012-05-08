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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ConditionalOrOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NotOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.PlusOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.ShortcutIfElseOp;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.TimingOp;
import net.sf.openforge.lim.op.XorOp;
import net.sf.openforge.util.graphviz.Circle;
import net.sf.openforge.util.graphviz.Edge;
import net.sf.openforge.util.graphviz.Graph;
import net.sf.openforge.util.graphviz.Node;
import net.sf.openforge.util.naming.ID;

/**
 * Yet another hack of {@link LGraph}, this time to draw only physical
 * connections (those between {@link Bus Buses} and {@link Port Ports}).
 * 
 * @version $Id: LWireGraph.java 88 2006-01-11 22:39:52Z imiller $
 */
public class LWireGraph extends DefaultVisitor {

	/** Use a scanner for traversing module contents */
	private final net.sf.openforge.lim.Scanner scanner;

	/** The top level Graph -- put all edges at this level */
	private final Graph topGraph;

	/** The top-most component being graphed -- to avoid trying to graphOutside. */
	private final Visitable top;

	/** The graph instance for the current level being visited */
	private Graph graph;

	/** True if the insides of Selectors and other primitive modules to be drawn */
	private boolean isDetailed = false;
	/** show clock and reset */
	private boolean drawCR = false;
	/** show dependencies */
	@SuppressWarnings("unused")
	private boolean graphDependencies = false;
	/** show logical connections */
	@SuppressWarnings("unused")
	private boolean graphLogical = false;
	/** show structural connections */
	@SuppressWarnings("unused")
	private boolean graphStructural = false;
	/** show data connections */
	@SuppressWarnings("unused")
	private boolean graphData = false;
	/** show control connections */
	@SuppressWarnings("unused")
	private boolean graphControl = false;

	/** used to handle unresolved nodes in graphing */
	private final Map<Node, Unresolved> unresolvedNodes = new HashMap<Node, Unresolved>();

	/**
	 * The graph stack; pushed when a new level is visited, popped when that
	 * level is exited.
	 */
	private final LinkedList<Graph> graphStack = new LinkedList<Graph>();

	/** The number of nodes so far, used to generate unique identifiers */
	private int nodeCount = 0;

	/**
	 * Map of LIM object to graph Node - for dependencies and generic
	 * connections (exits, etc)
	 */
	private final Map<Object, Object> nodeMapDeps = new HashMap<Object, Object>();
	/** Map of LIM object to graph Node - for physical connections */
	private final Map<Object, Object> nodeMapPhys = new HashMap<Object, Object>();

	/** Weights for the various types of edges */
	@SuppressWarnings("unused")
	private static final int WT_ENTRY = 1200;
	private static final int WT_EXIT = 8000;
	@SuppressWarnings("unused")
	private static final int WT_DEP = 4;
	@SuppressWarnings("unused")
	private static final int WT_FEEDBK = 1;
	private static final int WT_ENTRYFEEDBACK = 12;

	/** show data connections */
	public static final int DATA = 0x1;
	/** show control connections */
	public static final int CONTROL = 0x2;
	/** show logical connections */
	public static final int LOGICAL = 0x4;
	/** show structural connections */
	public static final int STRUCTURAL = 0x8;
	/** used internally to denote any of the above */
	private static final int DEPENDENCY = DATA | CONTROL | LOGICAL | STRUCTURAL;

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

	public static final int DEFAULT = DATA | CONTROL | LOGICAL | STRUCTURAL
			| PHYSICAL;

	/**
	 * Constructs a new LWireGraph.
	 * 
	 * @param name
	 *            a title for the graph
	 * @param top
	 *            the LIM element whose contents are to be graphed; typically a
	 *            {@link Design}, but only the {@link Procedure Procedures} down
	 *            will be depicted
	 * @param flags
	 *            is a bitwise or of DATA, CONTROL, LOGICAL, STRUCTURAL,
	 *            CLOCKRESET, PHYSICAL
	 */
	public LWireGraph(String name, Visitable top, int flags) {
		super();
		parseFlags(flags);

		scanner = new net.sf.openforge.lim.Scanner(this);

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

		this.top = top;
		top.accept(this);
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
		if ((flags & STRUCTURAL) == STRUCTURAL) {
			graphDependencies = true;
			graphStructural = true;
		}
		if ((flags & CLOCKRESET) == CLOCKRESET) {
			drawCR = true;
		}
		if ((flags & DETAIL) == DETAIL) {
			isDetailed = true;
		}
	}

	/**
	 * Constructs a new LWireGraph that will not draw the insides of
	 * pass-through modules like Selector.
	 * 
	 * @param name
	 *            a title for the graph
	 * @param top
	 *            the LIM element whose contents are to be graphed; typically a
	 *            {@link Design}, but only the {@link Procedure Procedures} down
	 *            will be depicted
	 */
	public LWireGraph(String name, Visitable top) {
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

	@SuppressWarnings("unused")
	private static String getName(Object o, String defaultName) {
		return ID.showLogical(o);
		/*
		 * String name = namedThing.getIDLogical(); if (name == null) { name =
		 * namedThing.getIDGlobalType(); } return name == null ? defaultName :
		 * name;
		 */
	}

	private static String getName(Object o) {
		return ID.showLogical(o); // getName(namedThing,
									// namedThing.getClass().getName());
	}

	/**
	 * Draw the box for the component.
	 */
	private void graph(Component component) {
		net.sf.openforge.util.graphviz.Record box = new net.sf.openforge.util.graphviz.Record(
				"component" + nodeCount++);
		box.setLabel(getName(component) + " (Cmp)");

		net.sf.openforge.util.graphviz.Record.Port main = box.getPort("main");
		net.sf.openforge.util.graphviz.Record.Port entryPort = main
				.getPort("entry");
		entryPort.setSeparated(false);

		if (component.getGoPort().isConnected()) {
			net.sf.openforge.util.graphviz.Record.Port goPort = entryPort
					.getPort("go");
			goPort.setLabel("g");
			goPort.setSeparated(true);
			addToNodeMap(component.getGoPort(), goPort, DEPENDENCY);
		}

		if (drawCR) {
			if (component.getClockPort().isConnected()) {
				net.sf.openforge.util.graphviz.Record.Port clockPort = entryPort
						.getPort("clock");
				clockPort.setLabel("c");
				clockPort.setSeparated(true);
				addToNodeMap(component.getClockPort(), clockPort, DEPENDENCY);
			}

			if (component.getResetPort().isConnected()) {
				net.sf.openforge.util.graphviz.Record.Port resetPort = entryPort
						.getPort("reset");
				resetPort.setLabel("r");
				resetPort.setSeparated(true);
				addToNodeMap(component.getResetPort(), resetPort, DEPENDENCY);
			}
		}

		for (int i = 0; i < component.getDataPorts().size(); i++) {
			Port port = component.getDataPorts().get(i);
			if (port.isConnected()) {
				String label = "d" + i;
				net.sf.openforge.util.graphviz.Record.Port dataPort = entryPort
						.getPort("p_" + label);
				dataPort.setLabel(label);
				dataPort.setSeparated(true);
				addToNodeMap(port, dataPort, DEPENDENCY);
			}
		}

		net.sf.openforge.util.graphviz.Record.Port bodyPort = main
				.getPort("body");
		bodyPort.setLabel(getName(component));
		bodyPort.setSeparated(false);

		if (component.getExits().size() == 1) {
			if (component instanceof InBuf) {
				graphInBufExit((InBuf) component, main);
			} else {
				graphSingleExit(component, main);
			}
		}

		addToNodeMap(component, box, DEPENDENCY);
		graph.add(box);

		graphOutside(component, false);

		if (component.getExits().size() > 1) {
			connectExits(component, component);
		}
	}

	/**
	 * Draw the box for the component.
	 */
	private void graph(InBuf inbuf) {
		final Module owner = inbuf.getOwner();

		net.sf.openforge.util.graphviz.Record box = new net.sf.openforge.util.graphviz.Record(
				"component" + nodeCount++);
		box.setLabel(getName(inbuf) + " (Cmp)");

		net.sf.openforge.util.graphviz.Record.Port main = box.getPort("main");
		net.sf.openforge.util.graphviz.Record.Port entryPort = main
				.getPort("entry");
		entryPort.setSeparated(false);

		if (owner.getGoPort().isConnected()) {
			net.sf.openforge.util.graphviz.Record.Port goPort = entryPort
					.getPort("go");
			goPort.setLabel("g");
			goPort.setSeparated(true);
			addToNodeMap(owner.getGoPort(), goPort, DEPENDENCY);
		}

		if (drawCR) {
			if (owner.getClockPort().isConnected()) {
				net.sf.openforge.util.graphviz.Record.Port clockPort = entryPort
						.getPort("clock");
				clockPort.setLabel("c");
				clockPort.setSeparated(true);
				addToNodeMap(owner.getClockPort(), clockPort, DEPENDENCY);
			}

			if (owner.getResetPort().isConnected()) {
				net.sf.openforge.util.graphviz.Record.Port resetPort = entryPort
						.getPort("reset");
				resetPort.setLabel("r");
				resetPort.setSeparated(true);
				addToNodeMap(owner.getResetPort(), resetPort, DEPENDENCY);
			}
		}

		for (int i = 0; i < owner.getDataPorts().size(); i++) {
			Port port = owner.getDataPorts().get(i);
			if (port.isConnected()) {
				String label = "d" + i;
				net.sf.openforge.util.graphviz.Record.Port dataPort = entryPort
						.getPort("p_" + label);
				dataPort.setLabel(label);
				dataPort.setSeparated(true);
				addToNodeMap(port, dataPort, DEPENDENCY);
			}
		}

		net.sf.openforge.util.graphviz.Record.Port bodyPort = main
				.getPort("body");
		bodyPort.setLabel(getName(inbuf));
		bodyPort.setSeparated(false);

		if (inbuf.getExits().size() == 1) {
			graphInBufExit(inbuf, main);
		}

		addToNodeMap(inbuf, box, DEPENDENCY);
		graph.add(box);

		graphOutside(inbuf, false);

		if (inbuf.getExits().size() > 1) {
			connectExits(inbuf, inbuf);
		}
	}

	private void graphSingleExit(Component component,
			net.sf.openforge.util.graphviz.Record node) {
		final Exit exit = component.getExits().iterator().next();
		net.sf.openforge.util.graphviz.Record.Port exitPort = node
				.getPort("exit");
		addToNodeMap(exit, exitPort, DEPENDENCY);

		if (exit.getDoneBus().isConnected()) {
			net.sf.openforge.util.graphviz.Record.Port donePort = exitPort
					.getPort("done");
			donePort.setLabel("D");
			donePort.setSeparated(true);
			addToNodeMap(exit.getDoneBus(), donePort, DEPENDENCY);
		}

		for (int i = 0; i < exit.getDataBuses().size(); i++) {
			final Bus bus = exit.getDataBuses().get(i);
			if (bus.isConnected()) {
				String label = "d" + i;
				net.sf.openforge.util.graphviz.Record.Port dataPort = exitPort
						.getPort("b_" + label);
				dataPort.setLabel(label);
				dataPort.setSeparated(true);
				addToNodeMap(bus, dataPort, DEPENDENCY);
			}
		}
	}

	private void graphInBufExit(InBuf inbuf,
			net.sf.openforge.util.graphviz.Record node) {
		final Exit exit = inbuf.getExits().iterator().next();
		net.sf.openforge.util.graphviz.Record.Port exitPort = node
				.getPort("exit");
		addToNodeMap(exit, exitPort, DEPENDENCY);

		if (inbuf.getGoBus().isConnected()) {
			net.sf.openforge.util.graphviz.Record.Port goPort = exitPort
					.getPort("go");
			goPort.setLabel("G");
			goPort.setSeparated(true);
			addToNodeMap(inbuf.getGoBus(), goPort, DEPENDENCY);
		}

		if (drawCR) {
			if (inbuf.getClockBus().isConnected()) {
				net.sf.openforge.util.graphviz.Record.Port clockPort = exitPort
						.getPort("clock");
				clockPort.setLabel("clk");
				clockPort.setSeparated(true);
				addToNodeMap(inbuf.getClockBus(), clockPort, DEPENDENCY);
			}

			if (inbuf.getResetBus().isConnected()) {
				net.sf.openforge.util.graphviz.Record.Port resetPort = exitPort
						.getPort("reset");
				resetPort.setLabel("rst");
				resetPort.setSeparated(true);
				addToNodeMap(inbuf.getResetBus(), resetPort, DEPENDENCY);
			}
		}

		int i = 0;
		for (Bus bus : inbuf.getDataBuses()) {
			if (bus.isConnected()) {
				String label = "d" + i;
				net.sf.openforge.util.graphviz.Record.Port dataPort = exitPort
						.getPort("b_" + label);
				dataPort.setLabel(label);
				dataPort.setSeparated(true);
				addToNodeMap(bus, dataPort, DEPENDENCY);
			}
		}
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
		final InBuf inbuf = module.getInBuf();
		if (drawCR) {
			if (inbuf.getClockBus().isConnected()) {
				Node clock = (Node) nodeMapDeps.get(inbuf.getClockBus());
				clock.setLabel("c");
			}
			if (inbuf.getResetBus().isConnected()) {
				Node reset = (Node) nodeMapDeps.get(inbuf.getResetBus());
				reset.setLabel("r");
			}
		}

		if (inbuf.getGoBus().isConnected()) {
			Node go = (Node) nodeMapDeps.get(inbuf.getGoBus());
			go.setLabel("g");
		}

		int index = 0;
		for (Bus bus : inbuf.getDataBuses()) {
			if (bus.isConnected()) {
				Node node = (Node) nodeMapDeps.get(bus);
				node.setLabel("d" + index++);
			}
		}

		/*
		 * Pop back out to connect externals to internals.
		 */
		popGraph();
		if (module != top) {
			connectPorts(module, false);
			connectExits(module);
		}
	}

	private void connectPorts(Component component, boolean isFeedback) {
		if (drawCR) {
			connectPort(component.getClockPort(), isFeedback);
			connectPort(component.getResetPort(), isFeedback);
		}

		connectPort(component.getGoPort(), isFeedback);
		for (Port port : component.getDataPorts()) {
			connectPort(port, isFeedback);
		}
	}

	private void connectPort(Port port, boolean isFeedback) {
		if (port.isConnected()) {
			final Node portNode = (Node) nodeMapDeps.get(port);
			final Node busNode = (Node) nodeMapDeps.get(port.getBus());
			final Edge edge = new Edge(isFeedback ? WT_ENTRYFEEDBACK : WT_EXIT);

			assert port.getBus() != null : "no bus for port of "
					+ port.getOwner();
			assert busNode != null : "null busNode for bus of "
					+ port.getBus().getOwner().getOwner();
			assert portNode != null : ("null portNode for port of " + port
					.getOwner());
			topGraph.connect(busNode, portNode, edge);

		}
	}

	/**
	 * Connects the Exits of the component. If the Exit has a peer, a dashed
	 * line is drawn to it; else a solid line will be drawn to the component.
	 */
	@SuppressWarnings("unused")
	private void connectExits(Component component) {
		connectExits(component, null);
	}

	private void connectExits(Module module) {
		connectExits(module, null);
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

	/**
	 * Graph connections outside of the component, unless the Component is the
	 * top.
	 */
	private void graphOutside(Component component, boolean feedback) {
		if (component != top) {
			if (component.getExits().size() > 1) {
				graphExits(component);
			}
			connectPorts(component, feedback);
		}
	}

	private void graphOutside(Module module, boolean feedback) {
		graphExits(module);
	}

	private void graphExits(Component component) {
		int index = 0;
		for (Exit exit : component.getExits()) {
			graphExit(exit, index++);
		}
	}

	private void graphExit(Exit exit, int id) {
		// Component component = exit.getOwner();

		net.sf.openforge.util.graphviz.Record record = new net.sf.openforge.util.graphviz.Record(
				"exit" + nodeCount++);
		addToNodeMap(exit, record, DEPENDENCY);

		net.sf.openforge.util.graphviz.Record.Port done = record
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
			net.sf.openforge.util.graphviz.Record.Port data = record
					.getPort(dname);
			data.setLabel(dname);
			data.setSeparated(true);
			addToNodeMap(bus, data, DEPENDENCY);
		}

		graph.add(record);
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
				Node sourceNode = sources.get(i);
				assert sourceNode != null : "null node for unresolved index "
						+ i;
				topGraph.connect(target, sourceNode, edges.get(i));
			}
			unresolvedNodes.remove(key);
		}
	}

	/**
	 * holds unresolved edges, allows adding new edges to an existing bus (key
	 * in the hashMap), and retrieval of the edges
	 */
	private static class Unresolved {
		private final Bus bus;
		private final List<Node> sources = new ArrayList<Node>();
		private final List<Edge> edges = new ArrayList<Edge>();

		@SuppressWarnings("unused")
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

		@SuppressWarnings("unused")
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

	@Override
	public void visit(Design design) {
		/*
		 * XXX -- We don't do designs yet.
		 */
		scanner.enter(design);
	}

	@Override
	public void visit(Task task) {
		/*
		 * XXX -- nor tasks.
		 */
		scanner.enter(task);
	}

	@Override
	public void visit(Call call) {
		/*
		 * XXX -- Calls are also tbd.
		 */
		scanner.enter(call);
	}

	@Override
	public void visit(Procedure procedure) {
		pushGraph(procedure);
		graph.setLabel(getName(procedure));

		procedure.getBody().accept(this);

		popGraph();
	}

	@Override
	public void visit(Block block) {
		graphPreVisit(block);
		visit(block.getComponents());
		graphPostVisit(block);
	}

	@Override
	public void visit(Branch branch) {
		graphPreVisit(branch);
		visit(branch.getComponents());
		graphPostVisit(branch);
	}

	@Override
	public void visit(Decision decision) {
		graphPreVisit(decision);
		visit(decision.getComponents());
		graphPostVisit(decision);
	}

	@Override
	public void visit(Loop loop) {
		graphPreVisit(loop);

		/*
		 * Draw the feedback registers and input latches.
		 */
		Collection<Reg> registers = new LinkedList<Reg>(loop.getDataRegisters());
		registers.add(loop.getControlRegister());
		for (Reg reg : registers) {
			drawFeedbackRegister(reg);
		}

		final Collection<Component> components = new HashSet<Component>(
				loop.getComponents());
		components.removeAll(registers);
		visit(components);

		graphPostVisit(loop);

		for (Reg reg : registers) {
			drawFeedbackInput(reg);
		}
	}

	private void visit(Collection<Component> components) {
		final LinkedList<Component> queue = new LinkedList<Component>(
				components);
		while (!queue.isEmpty()) {
			final Component component = queue.removeFirst();
			if (isInputReady(component)) {
				component.accept(this);
			} else {
				queue.add(component);
			}
		}
	}

	private void drawFeedbackRegister(Reg reg) {
		net.sf.openforge.util.graphviz.Record box = new net.sf.openforge.util.graphviz.Record(
				"component" + nodeCount++);
		box.setLabel(getName(reg) + " (Cmp)");

		net.sf.openforge.util.graphviz.Record.Port main = box.getPort("main");
		net.sf.openforge.util.graphviz.Record.Port entryPort = main
				.getPort("entry");
		entryPort.setSeparated(false);

		net.sf.openforge.util.graphviz.Record.Port dataPort = entryPort
				.getPort("din");
		dataPort.setLabel("din");
		addToNodeMap(reg.getDataPort(), dataPort, DEPENDENCY);

		net.sf.openforge.util.graphviz.Record.Port bodyPort = main
				.getPort("body");
		bodyPort.setLabel(getName(reg));
		bodyPort.setSeparated(false);

		graphSingleExit(reg, main);

		addToNodeMap(reg, box, DEPENDENCY);
		graph.add(box);
	}

	private void drawFeedbackInput(Reg reg) {
		connectPort(reg.getDataPort(), true);
	}

	private boolean isInputReady(Component component) {
		final Port goPort = component.getGoPort();
		if (goPort.isConnected() && !nodeMapDeps.containsKey(goPort.getBus())) {
			return false;
		}

		if (drawCR) {
			final Port clockPort = component.getClockPort();
			if (clockPort.isConnected()
					&& !nodeMapDeps.containsKey(clockPort.getBus())) {
				return false;
			}
			final Port resetPort = component.getResetPort();
			if (resetPort.isConnected()
					&& !nodeMapDeps.containsKey(resetPort.getBus())) {
				return false;
			}
		}

		for (Port dataPort : component.getDataPorts()) {
			if (dataPort.isConnected()
					&& !nodeMapDeps.containsKey(dataPort.getBus())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void visit(Switch sw) {
		visit((Block) sw);
	}

	@Override
	public void visit(WhileBody body) {
		graphPreVisit(body);
		visit(body.getComponents());
		graphPostVisit(body);
	}

	@Override
	public void visit(UntilBody body) {
		graphPreVisit(body);
		visit(body.getComponents());
		graphPostVisit(body);
	}

	@Override
	public void visit(Latch l) {
		if (isDetailed) {
			graphPreVisit(l);
			visit(l.getComponents());
			graphPostVisit(l);
		} else {
			graph(l);
		}
	}

	@Override
	public void visit(Scoreboard sb) {
		if (isDetailed) {
			graphPreVisit(sb);
			visit(sb.getComponents());
			graphPostVisit(sb);
		} else {
			graph(sb);
		}
	}

	@Override
	public void visit(InBuf buf) {
		graph(buf);
	}

	@Override
	public void visit(OutBuf buf) {
		graph(buf);
	}

	@Override
	public void visit(Reg reg) {
		graph(reg);
	}

	@Override
	public void visit(Mux mux) {
		graph(mux);
	}

	@Override
	public void visit(EncodedMux mux) {
		graph(mux);
	}

	@Override
	public void visit(TimingOp op) {
		graph(op);
	}

	@Override
	public void visit(TaskCall comp) {
		if (isDetailed) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	@Override
	public void visit(SimplePinAccess comp) {
		if (isDetailed) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	@Override
	public void visit(FifoAccess comp) {
		if (isDetailed) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	@Override
	public void visit(FifoRead comp) {
		visit((FifoAccess) comp);
	}

	@Override
	public void visit(FifoWrite comp) {
		visit((FifoAccess) comp);
	}

	@Override
	public void visit(PriorityMux pmux) {
		if (isDetailed) {
			graphPreVisit(pmux);
			scanner.enter(pmux);
			graphPostVisit(pmux);
		} else {
			graph(pmux);
		}
	}

	@Override
	public void visit(RegisterReferee regReferee) {
		if (isDetailed) {
			graphPreVisit(regReferee);
			scanner.enter(regReferee);
			graphPostVisit(regReferee);
		} else {
			graph(regReferee);
		}
	}

	@Override
	public void visit(MemoryReferee memReferee) {
		if (isDetailed) {
			graphPreVisit(memReferee);
			scanner.enter(memReferee);
			graphPostVisit(memReferee);
		} else {
			graph(memReferee);
		}
	}

	@Override
	public void visit(And and) {
		graph(and);
	}

	@Override
	public void visit(Not not) {
		graph(not);
	}

	@Override
	public void visit(Or or) {
		graph(or);
	}

	@Override
	public void visit(AddOp op) {
		graph(op);
	}

	@Override
	public void visit(AndOp op) {
		graph(op);
	}

	@Override
	public void visit(NumericPromotionOp op) {
		graph(op);
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
	public void visit(DivideOp op) {
		graph(op);
	}

	@Override
	public void visit(EqualsOp op) {
		graph(op);
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
	public void visit(NotEqualsOp op) {
		graph(op);
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
	public void visit(RegisterRead op) {
		graph(op);
	}

	@Override
	public void visit(RegisterWrite op) {
		graph(op);
	}

	@Override
	public void visit(NotOp op) {
		graph(op);
	}

	@Override
	public void visit(OrOp op) {
		graph(op);
	}

	@Override
	public void visit(PlusOp op) {
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
	public void visit(ShortcutIfElseOp op) {
		graph(op);
	}

	@Override
	public void visit(SubtractOp op) {
		graph(op);
	}

	@Override
	public void visit(XorOp op) {
		graph(op);
	}
}
