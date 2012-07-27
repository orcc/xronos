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
 * $Id: ComponentNode.java 105 2006-02-15 21:38:08Z imiller $
 *
 * 
 */

package net.sf.openforge.lim.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.primitive.Reg;
import net.sf.openforge.util.graphviz.Graph;
import net.sf.openforge.util.graphviz.Node;
import net.sf.openforge.util.graphviz.Record;
import net.sf.openforge.util.naming.ID;

/**
 * The {@link Node} that is created for a generic {@link Component} in an
 * {@link LXGraph}. In addition to itself, the ComponentNode may create
 * additional nodes, one for each {@link Exit}, if the {@link Component} has
 * more than one. These nodes can be retrieved with
 * {@link ComponentNode#getExitNodes()} so that they may also be added to the
 * containing {@link Graph}.
 * 
 * @version $Id: ComponentNode.java 105 2006-02-15 21:38:08Z imiller $
 */
class ComponentNode extends Record {
	public static final boolean DO_SIDEBAND = false;
	public static final boolean DO_CLKRESET = false;

	/** The component represented by this node */
	protected Component component;

	/** A map of LIM object (ports/buses/exits) to the sub-Node created for each */
	private Map<ID, Record> nodeMap = new HashMap<ID, Record>();

	/** The external Nodes, if any, created to represent multiple Exits */
	private Collection<Node> exitNodes = new LinkedList<Node>();

	protected static final String MAIN = "main";
	protected static final String ENTRY = "entry";
	protected static final String BODY = "body";
	protected static final String EXIT = "exit";
	protected static final String BUSES = "buses";

	/**
	 * Creates a ComponentNode.
	 * 
	 * @param component
	 *            the component to be represented
	 * @param id
	 *            a unique identifier for the node
	 */
	ComponentNode(Component component, String id, int fontSize) {
		super(id);

		assert component != null;

		this.component = component;
		setFontSize(fontSize);

		Record.Port boundingBox = getPort(MAIN);
		graphPorts(boundingBox);
		graphBody(boundingBox);
		graphExits(boundingBox);
	}

	/**
	 * Gets the sub-node created for a given {@link Port} of the component. In
	 * general, nodes are created for all data ports and for those control ports
	 * that are connected.
	 */
	Node getNode(net.sf.openforge.lim.Port port) {
		return nodeMap.get(port);
	}

	/**
	 * Gets the sub-node created for a given {@link Exit} of the component. If
	 * there are multiple exits, then no sub-node is created; instead, external
	 * Nodes are created for each and are obtained with
	 * {@link ComponentNode#getExitNodes()}.
	 */
	Node getNode(Exit exit) {
		return nodeMap.get(exit);
	}

	/**
	 * Gets the sub-node created for a given {@link Bus} of the component. If
	 * there are multiple exits, then no sub-node is created; instead, the bus
	 * node should be retrieved from the external node returned by
	 * {@link ComponentNode#getExitNodes}.
	 */
	Node getNode(Bus bus) {
		return nodeMap.get(bus);
	}

	/**
	 * Gets the component represented by this node.
	 */
	Component getComponent() {
		return component;
	}

	/**
	 * Gets the external Nodes for the Exits, which are created if there is more
	 * than one Exit on the component.
	 */
	Collection<Node> getExitNodes() {
		return exitNodes;
	}

	/**
	 * Gets the brief name of a given object's class (e.g., "String").
	 */
	static String getShortClassName(Object object) {
		String[] parts = object.getClass().getName().split("\\.");
		return parts[parts.length - 1];
	}

	/**
	 * Gets the label for the component's body.
	 */
	protected String getBodyLabel() {
		StringBuffer labelBuf = new StringBuffer();
		labelBuf.append(getShortClassName(component));
		labelBuf.append("\\n");
		labelBuf.append("@");
		labelBuf.append(Integer.toHexString(component.hashCode()));
		labelBuf.append("\\n");
		labelBuf.append(net.sf.openforge.util.naming.ID.glob(component));
		if (component instanceof SimplePinRead
				|| component instanceof SimplePinWrite) {
			labelBuf.append("\\n");
			labelBuf.append(((SimplePin) ((Referencer) component)
					.getReferenceable()).getName());
		}
		if (component instanceof Call || component instanceof Pin
				|| component instanceof SimplePin || component instanceof Reg) {
			labelBuf.append("\\n");
			labelBuf.append(component.showIDLogical());
		}
		return labelBuf.toString();
	}

	/**
	 * Creates sub-nodes in a given bounding box for each port of the component.
	 */
	protected void graphPorts(Record.Port boundingBox) {
		if (needPortGraph()) {
			Record.Port entryBox = boundingBox.getPort(ENTRY);
			entryBox.setSeparated(false);
			graphPort(component.getGoPort(), entryBox, "go", "G");
			if (DO_CLKRESET) {
				net.sf.openforge.lim.Port reset = component.getResetPort();
				if (reset.isConnected()) {
					graphPort(reset, entryBox, "reset", "R");
				}
				net.sf.openforge.lim.Port clock = component.getClockPort();
				if (clock.isConnected()) {
					graphPort(clock, entryBox, "clock", "C");
				}
			}
			int index = 0;
			for (net.sf.openforge.lim.Port port : component.getDataPorts()) {
				graphPort(port, entryBox, "din" + index, "d" + index);
				index++;
			}
		}
	}

	/**
	 * Tests whether or not it is necessary to graph any Ports. By default, this
	 * is true only if there is at least one data port or a connected go port.
	 */
	protected boolean needPortGraph() {
		return !component.getDataPorts().isEmpty()
				|| component.getGoPort().isConnected();
	}

	/**
	 * Graphs the exits of the component in a given bounding box node. If there
	 * are multiple exits, then a separate {@link Record} is created for each
	 * and added to the list returned by {@link ComponentNode#getExitNodes()}.
	 */
	protected void graphExits(Record.Port boundingBox) {
		if (!component.getExits().isEmpty()) {
			Collection<Exit> exits = component.getExits();
			if (exits.isEmpty()) {
				return;
			} else if (exits.size() == 1) {
				/*
				 * Rotate the bounding box.
				 */
				boundingBox = boundingBox.getPort(EXIT);
				boundingBox.setSeparated(false);
				Record.Port exitBox = boundingBox.getPort(EXIT);
				exitBox.setSeparated(false);
				graphExit(exits.iterator().next(), exitBox);
			} else {
				int index = 0;
				for (Exit exit : exits) {
					Record exitBox = new Record(getId() + "_exit" + index++);
					graphExit(exit, exitBox.getPort(EXIT));
					exitNodes.add(exitBox);
				}
			}
		}
	}

	/**
	 * Creates a sub-node in a given bounding box for an Exit.
	 */
	protected void graphExit(Exit exit, Record exitBox) {
		if (!DO_SIDEBAND && exit.getTag().getType() == Exit.SIDEBAND) {
			return;
		}
		Record.Port bodyBox = exitBox.getPort(BODY);
		bodyBox.setSeparated(false);

		StringBuffer labelBuf = new StringBuffer();
		labelBuf.append("Exit\\n");
		labelBuf.append("@");
		labelBuf.append(Integer.toHexString(exit.hashCode()));
		labelBuf.append("\\n");
		labelBuf.append(exit.getTag());
		bodyBox.setLabel(labelBuf.toString());

		Record.Port busBox = exitBox.getPort(BUSES);
		busBox.setSeparated(false);
		graphBuses(exit, busBox);
		nodeMap.put(exit, exitBox);
	}

	/**
	 * Creates a sub-node for each bus of an Exit in a given bounding box.
	 */
	protected void graphBuses(Exit exit, Record.Port busBox) {
		graphBus(exit.getDoneBus(), busBox, "done", "D");
		int index = 0;
		for (Bus bus : exit.getDataBuses()) {
			graphBus(bus, busBox, "dout" + index, "d" + index);
			index++;
		}
	}

	/**
	 * Creates a sub-node for a bus in a given bounding box.
	 * 
	 * @param bus
	 *            the bus to be graphed
	 * @param parentNode
	 *            the bounding box in which the bus's node is created
	 * @param id
	 *            the node identifier
	 * @param label
	 *            the node label; the size will be appended
	 */
	protected void graphBus(Bus bus, Record.Port parentNode, String id,
			String label) {
		if (bus.isConnected() || !(bus.getOwner().getDoneBus() == bus)) {
			Record.Port busNode = parentNode.getPort(id);

			String size = "-";
			Value value = bus.getValue();
			if (value != null) {
				size = Integer.toString(value.getSize());
				if (value.isConstant()) {
					size += ("=" + Long.toHexString(value.getValueMask()));
				}
			}

			busNode.setLabel(label + "/" + size);
			busNode.setSeparated(true);
			nodeMap.put(bus, busNode);
		}
	}

	/**
	 * Creates a sub-node for a port in a given bounding box.
	 * 
	 * @param port
	 *            the port to be graphed
	 * @param parentNode
	 *            the bounding box in which the port's node is created
	 * @param id
	 *            the node identifier
	 * @param label
	 *            the node label; the size will be appended
	 */
	protected void graphPort(net.sf.openforge.lim.Port port,
			Record.Port parentNode, String id, String label) {
		if (!DO_SIDEBAND
				&& port.getTag() == net.sf.openforge.lim.Component.SIDEBAND) {
			return;
		}

		if (port.isConnected() || (port != port.getOwner().getGoPort())) {
			Record.Port portNode = parentNode.getPort(id);

			String size = "-";
			Value value = port.getValue();
			if (value != null) {
				size = Integer.toString(value.getSize());
				if (value.isConstant()) {
					size += ("=" + Long.toHexString(value.getValueMask()));
				}
			}

			portNode.setLabel(label + "/" + size);
			portNode.setSeparated(true);
			nodeMap.put(port, portNode);
		}
	}

	/**
	 * Graphs the main body of the component in a given bounding box.
	 */
	protected void graphBody(Record.Port boundingBox) {
		Record.Port bodyBox = boundingBox.getPort(BODY);
		bodyBox.setSeparated(false);
		bodyBox.setLabel(getBodyLabel());
	}
}
