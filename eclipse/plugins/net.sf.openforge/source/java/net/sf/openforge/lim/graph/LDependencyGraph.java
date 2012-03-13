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

package net.sf.openforge.lim.graph;

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

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.ClockDependency;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.ResetDependency;
import net.sf.openforge.lim.ResourceDependency;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.WaitDependency;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.AggregateConstant;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.memory.MemoryWrite;
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
 * A hack of {@link LGraph} with reduced complexity. Single {@link Entry
 * Entries} and {@link Exit Exits} are drawn as part of the main node.
 * {@link NoOp NoOps} with no data flows are omitted.
 * <P>
 * Only the dependencies connections are drawn, not the physical connections.
 * 
 * @version $Id: LDependencyGraph.java 538 2007-11-21 06:22:39Z imiller $
 */
public class LDependencyGraph extends DefaultVisitor {

	/** Use a scanner for traversing module contents */
	private net.sf.openforge.lim.Scanner scanner;

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

		this.scanner = new net.sf.openforge.lim.Scanner(this);

		this.graph = new Graph(name);
		// this.graph.setSize(7, 10); ruins the ratio statement below...
		this.graph.setLabel(name);
		// some attributes to make the graph smaller, and to print
		// to several pages when printed via: dot -Tps file.dot | lpr
		this.graph.setGVAttribute("ratio", "auto");
		this.graph.setGVAttribute("ranksep", ".1");
		this.graph.setGVAttribute("nodesep", ".12");
		this.graph.setGVAttribute("fontsize", "10");
		this.graph.setGVAttribute("fontname", "Helvetica");
		this.graph.setGVAttribute("page", "8.5,11.0");
		if ((flags & LANDSCAPE) == LANDSCAPE) {
			setLandscape();
		}
		this.topGraph = graph;

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
		if ((flags & CLOCKRESET) == CLOCKRESET) {
			drawCR = true;
		}
		if ((flags & DETAIL) == DETAIL) {
			this.isDetailed = true;
		}
		if ((flags & HASHCODES) == HASHCODES) {
			this.hashcodes = true;
		}
		if ((flags & DEXITS) == DEXITS) {
			this.graphDExits = true;
		}
	}

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

	/**
	 * causes the graph to be printed in landscape mode - this may or may not
	 * use less paper - it depends on the graph. hence it is an option
	 */
	public void setLandscape() {
		this.topGraph.setGVAttribute("rotate", "90");
	}

	private void pushGraph(Object obj) {
		graphStack.addFirst(graph);
		graph = graph.getSubgraph("cluster" + nodeCount++);
		addToNodeMap(obj, graph, DEPENDENCY);
	}

	private void popGraph() {
		graph = (Graph) graphStack.removeFirst();
	}

	private String getName(Object o, String defaultName) {
		// return ID.showLogical(o);
		return getName(o);
	}

	private String getName(Object o) {
		String id = ID.showLogical(o);

		if (o instanceof Call)
			return id + " [" + Integer.toHexString(o.hashCode()) + "]";
		else if (o instanceof Constant) {
			if (o instanceof AggregateConstant)
				return "agg" + id;
			return id + "=" + ConstantNode.value((Constant) o);
		} else
			return id + ":" + Integer.toHexString(o.hashCode());
		// return ID.showLogical(o);
	}

	/**
	 * Draw the box for the component
	 * 
	 * if we are graphing physical connections, then draw the ports at the top
	 * of the component, else draw just the component
	 */
	private void graph(Component component) {
		net.sf.openforge.util.graphviz.Record box = new net.sf.openforge.util.graphviz.Record(
				"component" + nodeCount++);
		box.setLabel(getName(component) + " (Cmp)");

		net.sf.openforge.util.graphviz.Record.Port main = box.getPort("main");
		if (component.getEntries().size() == 1) {
			final Entry entry = (Entry) component.getEntries().iterator()
					.next();
			net.sf.openforge.util.graphviz.Record.Port entryPort = main
					.getPort("entry");
			addToNodeMap(entry, entryPort, DEPENDENCY);
			entryPort.setSeparated(false);

			net.sf.openforge.util.graphviz.Record.Port goPort = entryPort
					.getPort("go");
			goPort.setLabel("g");
			goPort.setSeparated(true);
			addToNodeMap(EntryPort.get(entry, entry.getGoPort()), goPort,
					DEPENDENCY);

			if (drawCR) {
				net.sf.openforge.util.graphviz.Record.Port clockPort = entryPort
						.getPort("clock");
				clockPort.setLabel("c");
				clockPort.setSeparated(true);
				addToNodeMap(EntryPort.get(entry, entry.getClockPort()),
						clockPort, DEPENDENCY);

				net.sf.openforge.util.graphviz.Record.Port resetPort = entryPort
						.getPort("reset");
				resetPort.setLabel("r");
				resetPort.setSeparated(true);
				addToNodeMap(EntryPort.get(entry, entry.getResetPort()),
						resetPort, DEPENDENCY);
			}

			for (int i = 0; i < component.getDataPorts().size(); i++) {
				Port dp = (Port) component.getDataPorts().get(i);
				String label = "d" + i;
				net.sf.openforge.util.graphviz.Record.Port dataPort = entryPort
						.getPort("p_" + label);
				if (dp == component.getThisPort())
					label += "'";
				dataPort.setLabel(label);
				dataPort.setSeparated(true);
				addToNodeMap(EntryPort.get(entry, dp), dataPort, DEPENDENCY);
			}
		}

		net.sf.openforge.util.graphviz.Record.Port bodyPort = main
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

	private void graphSingleExit(Component component,
			net.sf.openforge.util.graphviz.Record node) {
		final Exit exit = (Exit) component.getExits().iterator().next();
		net.sf.openforge.util.graphviz.Record.Port exitPort = node
				.getPort("exit");
		addToNodeMap(exit, exitPort, DEPENDENCY);

		net.sf.openforge.util.graphviz.Record.Port donePort = exitPort
				.getPort("done");
		donePort.setLabel("D");
		donePort.setSeparated(true);
		addToNodeMap(exit.getDoneBus(), donePort, DEPENDENCY);
		for (int i = 0; i < exit.getDataBuses().size(); i++) {
			Bus dBus = (Bus) exit.getDataBuses().get(i);
			String label = "d" + i;
			net.sf.openforge.util.graphviz.Record.Port dataPort = exitPort
					.getPort("b_" + label);
			dataPort.setLabel(label);
			dataPort.setSeparated(true);
			addToNodeMap(dBus, dataPort, DEPENDENCY);
		}
	}

	private void graphSingleExit(InBuf inbuf,
			net.sf.openforge.util.graphviz.Record node) {
		final Exit exit = (Exit) inbuf.getExits().iterator().next();
		net.sf.openforge.util.graphviz.Record.Port exitPort = node
				.getPort("exit");
		addToNodeMap(exit, exitPort, DEPENDENCY);

		net.sf.openforge.util.graphviz.Record.Port goPort = exitPort
				.getPort("go");
		goPort.setLabel("G");
		goPort.setSeparated(true);
		addToNodeMap(inbuf.getGoBus(), goPort, DEPENDENCY);

		if (drawCR) {
			net.sf.openforge.util.graphviz.Record.Port clockPort = exitPort
					.getPort("clock");
			clockPort.setLabel("clk");
			clockPort.setSeparated(true);
			addToNodeMap(inbuf.getClockBus(), clockPort, DEPENDENCY);

			net.sf.openforge.util.graphviz.Record.Port resetPort = exitPort
					.getPort("reset");
			resetPort.setLabel("rst");
			resetPort.setSeparated(true);
			addToNodeMap(inbuf.getResetBus(), resetPort, DEPENDENCY);
		}

		int i = 0;
		for (Bus dBus : inbuf.getDataBuses()) {
			String label = "d" + i;
			net.sf.openforge.util.graphviz.Record.Port dataPort = exitPort
					.getPort("b_" + label);
			if (inbuf.getThisBus() == dBus)
				label += "'";
			dataPort.setLabel(label);
			dataPort.setSeparated(true);
			addToNodeMap(dBus, dataPort, DEPENDENCY);
			i++;
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
			if (bus == inBuf.getThisBus())
				label += "'";
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

	private void connectEntries(Component component, Component target) {
		if (component.getEntries().size() > 1) {
			connectEntries(component, target, false);
		} else if (component.getEntries().size() == 1) {
			graphDependencies(component, component.getMainEntry(), false);
		}
	}

	private void connectEntries(Module module, Component target,
			boolean feedback) {
		connectEntries((Component) module, target, feedback);
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

	private void graphEntries(Component component, boolean feedback) {
		for (Entry entry : component.getEntries()) {
			graphEntry(entry, feedback);
		}
	}

	private void graphEntry(Entry entry, boolean feedback) {
		Component component = entry.getOwner();
		net.sf.openforge.util.graphviz.Record record = new net.sf.openforge.util.graphviz.Record(
				"entry" + nodeCount++);
		addToNodeMap(entry, record, DEPENDENCY);

		net.sf.openforge.util.graphviz.Record.Port go = record.getPort("go");
		go.setLabel("g");
		go.setSeparated(true);
		addToNodeMap(EntryPort.get(entry, entry.getGoPort()), go, DEPENDENCY);
		if (drawCR) {
			net.sf.openforge.util.graphviz.Record.Port clock = record
					.getPort("clk");
			clock.setLabel("c");
			clock.setSeparated(true);
			addToNodeMap(EntryPort.get(entry, entry.getClockPort()), clock,
					DEPENDENCY);

			net.sf.openforge.util.graphviz.Record.Port reset = record
					.getPort("rst");
			reset.setLabel("r");
			reset.setSeparated(true);
			addToNodeMap(EntryPort.get(entry, entry.getResetPort()), reset,
					DEPENDENCY);
		}

		int dindex = 0;
		for (Port port : component.getDataPorts()) {
			String dname = "d" + dindex++;
			net.sf.openforge.util.graphviz.Record.Port data = record
					.getPort(dname);
			if (port == component.getThisPort())
				dname += "'";
			data.setLabel(dname);
			data.setSeparated(true);
			addToNodeMap(EntryPort.get(entry, port), data, DEPENDENCY);
		}

		graph.add(record);
		graphDependencies(component, entry, feedback);
	}

	private void graphDependencies(Component component, Entry entry,
			boolean feedback) {
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

	private void graphExits(Component component) {
		int index = 0;
		for (Exit exit : component.getExits()) {
			graphExit(exit, index++);
		}
	}

	private void graphExit(Exit exit, int id) {
		//Component component = exit.getOwner();

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
			if (dep instanceof ClockDependency)
				edge.setColor("yellow3");
			else if (dep instanceof ResetDependency)
				edge.setColor("orange");
			else if (dep instanceof DataDependency)
				edge.setColor("black");
			else if (dep instanceof ResourceDependency)
				edge.setColor("indianred");
			else if (dep instanceof WaitDependency)
				edge.setColor("blue");
			else if (dep instanceof ControlDependency)
				edge.setColor("green3");

			if (target != null) {
				topGraph.connect(target, source, edge);
			} else {
				Unresolved ur = (Unresolved) unresolvedNodes.get(logicalBus);
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
			Unresolved ur = (Unresolved) unresolvedNodes.get(key);
			Node target = (Node) nodeMapDeps.get(ur.getBus());
			assert target != null : "How can the target still be null!";
			List<Node> sources = ur.getSources();
			List<Edge> edges = ur.getEdges();
			for (int i = 0; i < sources.size(); i++) {
				topGraph.connect(target, (Node) sources.get(i),
						(Edge) edges.get(i));
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

		public int hashCode() {
			return entry.hashCode() + port.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj instanceof EntryPort) {
				EntryPort ep = (EntryPort) obj;
				return ep.entry == entry && ep.port == port;
			}
			return false;
		}
	}

	public void visit(Design design) {
		/*
		 * XXX -- We don't do designs yet.
		 */
		scanner.enter(design);
	}

	public void visit(Task task) {
		/*
		 * XXX -- nor tasks.
		 */
		scanner.enter(task);
	}

	public void visit(Call call) {
		/*
		 * XXX -- Calls are also tbd.
		 */
		graph(call);
		scanner.enter(call);
	}

	public void visit(Procedure procedure) {
		pushGraph(procedure);
		graph.setLabel(getName(procedure));

		procedure.getBody().accept(this);

		popGraph();
	}

	public void visit(Block block) {
		graphPreVisit(block);
		scanner.enter(block);
		graphPostVisit(block);
	}

	public void visit(Branch branch) {
		graphPreVisit(branch);
		scanner.enter(branch);
		graphPostVisit(branch);
	}

	public void visit(Decision decision) {
		graphPreVisit(decision);
		scanner.enter(decision);
		graphPostVisit(decision);
	}

	public void visit(Loop loop) {
		graphPreVisit(loop);
		scanner.enter(loop);
		graphPostVisit(loop);
	}

	public void visit(Switch sw) {
		graphPreVisit(sw);
		scanner.enter(sw);
		graphPostVisit(sw);
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

	public void visit(ForBody body) {
		graphPreVisit(body);
		scanner.enter(body);
		graphPostVisit(body);
	}

	public void visit(WhileBody body) {
		graphPreVisit(body);
		scanner.enter(body);
		graphPostVisit(body);
	}

	public void visit(UntilBody body) {
		graphPreVisit(body);
		scanner.enter(body);
		graphPostVisit(body);
	}

	public void visit(ArrayWrite comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	public void visit(ArrayRead comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	public void visit(AbsoluteMemoryRead comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	public void visit(AbsoluteMemoryWrite comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	public void visit(HeapWrite comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	public void visit(HeapRead comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	public void visit(FifoAccess comp) {
		if (graphComposable) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	public void visit(FifoWrite comp) {
		if (graphComposable) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	public void visit(FifoRead comp) {
		if (graphComposable) {
			graphPreVisit(comp);
			scanner.enter(comp);
			graphPostVisit(comp);
		} else {
			graph(comp);
		}
	}

	public void visit(InBuf buf) {
		graph(buf);
	}

	public void visit(OutBuf buf) {
		graph(buf);
	}

	public void visit(Reg reg) {
		graph(reg);
	}

	public void visit(Scoreboard scbd) {
		graph(scbd);
	}

	public void visit(Latch latch) {
		graph(latch);
		// graphPreVisit(latch);
		// scanner.enter(latch);
		// graphPostVisit(latch);
	}

	public void visit(Mux mux) {
		graph(mux);
	}

	public void visit(EncodedMux mux) {
		graph(mux);
	}

	public void visit(PriorityMux pmux) {
		graphPreVisit(pmux);
		scanner.enter(pmux);
		graphPostVisit(pmux);
	}

	public void visit(RegisterReferee regReferee) {
		graphPreVisit(regReferee);
		scanner.enter(regReferee);
		graphPostVisit(regReferee);
	}

	public void visit(MemoryReferee memReferee) {
		graphPreVisit(memReferee);
		scanner.enter(memReferee);
		graphPostVisit(memReferee);
	}

	public void visit(And and) {
		graph(and);
	}

	public void visit(Not not) {
		graph(not);
	}

	public void visit(Or or) {
		graph(or);
	}

	public void visit(AddOp op) {
		graph(op);
	}

	public void visit(AndOp op) {
		graph(op);
	}

	public void visit(NumericPromotionOp op) {
		graph(op);
	}

	public void visit(CastOp op) {
		graph(op);
	}

	public void visit(ComplementOp op) {
		graph(op);
	}

	public void visit(ConditionalAndOp op) {
		graph(op);
	}

	public void visit(ConditionalOrOp op) {
		graph(op);
	}

	public void visit(Constant op) {
		graph(op);
	}

	public void visit(DivideOp op) {
		graph(op);
	}

	public void visit(EqualsOp op) {
		graph(op);
	}

	public void visit(GreaterThanEqualToOp op) {
		graph(op);
	}

	public void visit(GreaterThanOp op) {
		graph(op);
	}

	public void visit(LeftShiftOp op) {
		graph(op);
	}

	public void visit(LessThanEqualToOp op) {
		graph(op);
	}

	public void visit(LessThanOp op) {
		graph(op);
	}

	public void visit(MinusOp op) {
		graph(op);
	}

	public void visit(MemoryRead op) {
		graph(op);
	}

	public void visit(MemoryWrite op) {
		graph(op);
	}

	public void visit(ModuloOp op) {
		graph(op);
	}

	public void visit(MultiplyOp op) {
		graph(op);
	}

	public void visit(NotEqualsOp op) {
		graph(op);
	}

	public void visit(TimingOp op) {
		graph(op);
	}

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

	public void visit(RegisterRead op) {
		graph(op);
	}

	public void visit(RegisterWrite op) {
		graph(op);
	}

	public void visit(NotOp op) {
		graph(op);
	}

	public void visit(OrOp op) {
		graph(op);
	}

	public void visit(PinRead op) {
		graph(op);
	}

	public void visit(PinWrite op) {
		graph(op);
	}

	public void visit(PlusOp op) {
		graph(op);
	}

	public void visit(RightShiftOp op) {
		graph(op);
	}

	public void visit(RightShiftUnsignedOp op) {
		graph(op);
	}

	public void visit(ShortcutIfElseOp op) {
		graph(op);
	}

	public void visit(SubtractOp op) {
		graph(op);
	}

	public void visit(XorOp op) {
		graph(op);
	}

	public void visit(SimplePinAccess comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	public void visit(TaskCall comp) {
		graphPreVisit(comp);
		scanner.enter(comp);
		graphPostVisit(comp);
	}

	public void visit(SimplePinRead op) {
		graph(op);
	}

	public void visit(SimplePinWrite op) {
		graph(op);
	}
}
