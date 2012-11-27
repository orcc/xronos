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
 * 
 *
 * 
 */
package org.xronos.openforge.util.graphviz;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * A GraphViz graph. Nodes and edge specifications can be added to the graph.
 * The graph can then be printed to a file that can be displayed with the
 * <em>dotty</em> program.
 * 
 * @author Stephen Edwards
 * @version $Id: Graph.java 131 2006-04-07 15:18:04Z imiller $
 */
public class Graph extends GVObject {

	private String type;
	private String id;
	private List<Node> nodes = new LinkedList<Node>();
	private List<Connection> connections = new LinkedList<Connection>();
	private List<Graph> clusters = new LinkedList<Graph>();

	protected static final boolean DO_GRAPH_LABEL = false;

	/**
	 * Constructs a new Graph of type "digraph" with a given identifier.
	 * 
	 * @param id
	 *            the internal identifier of the graph
	 */
	public Graph(String id) {
		this("digraph", id);
	}

	/**
	 * Constructs a new graph.
	 * 
	 * @param type
	 *            the graph type, eg "digraph"
	 * @param id
	 *            the internal identifier of the graph
	 */
	public Graph(String type, String id) {
		this.id = id;
		this.type = type;
	}

	@Override
	public void setLabel(String label) {
		if (DO_GRAPH_LABEL) {
			super.setLabel(label);
		}
	}

	/**
	 * Sets the output size of this graph.
	 * 
	 * @param width
	 *            the width in inches
	 * @param height
	 *            the height in inches
	 */
	public void setSize(float width, float height) {
		setAttribute("size",
				"\"" + Float.toString(width) + "," + Float.toString(height)
						+ "\"");
	}

	/**
	 * Sets whether to allow edge concentrators or not.
	 */
	public void setConcentrate(boolean concentrate) {
		setAttribute("concentrate", concentrate ? "TRUE" : "FALSE");
	}

	/**
	 * Prints the graph to the given output stream.
	 */
	public void print(PrintWriter out) {
		out.print(type);
		out.print(" \"");
		out.print(id);
		out.println("\"");
		out.println("{");

		printAttributes(out);

		for (Node node : nodes) {
			node.print(out);
		}
		for (Graph graph : clusters) {
			graph.print(out);
		}
		for (Connection connection : connections) {
			connection.print(out);
		}
		out.println("}");
	}

	/**
	 * Adds a node to this graph.
	 */
	public void add(Node node) {
		nodes.add(node);
		node.setGraph(this);
	}

	/**
	 * Connects two nodes within this sgraph.
	 * 
	 * @param source
	 *            the source node of the connection
	 * @param target
	 *            the target node of the connection
	 * @param edge
	 *            the edge to use to draw the connection
	 */
	public void connect(Node source, Node target, Edge edge) {
		if (source == null) {
			throw new IllegalArgumentException("source: " + source);
		}
		if (target == null) {
			throw new IllegalArgumentException("target: " + target);
		}
		if (edge == null) {
			throw new IllegalArgumentException("edge: " + edge);
		}

		connections.add(new Connection(source, target, edge));
	}

	/**
	 * Creates and returns a subgraph of this Graph.
	 * 
	 * @param id
	 *            the identifier of the subgraph
	 * @return the new subgraph
	 */
	public Graph getSubgraph(String id) {
		Graph graph = new Graph("subgraph", id);
		clusters.add(graph);
		return graph;
	}

	public void addSubgraph(Graph subgraph) {
		clusters.add(subgraph);
	}

	@Override
	void printAttributes(PrintWriter out) {
		if (!properties.isEmpty()) {
			for (Enumeration<?> enumeration = properties.propertyNames(); enumeration
					.hasMoreElements();) {
				String attr = (String) enumeration.nextElement();
				out.print(attr);
				out.print("=");
				if (attr.equals("label")) {
					out.print("\"");
				}
				out.print(getAttribute(attr));

				if (attr.equals("label")) {
					out.print("\"");
				}
				out.println(";");
			}
		}
	}
}

/**
 * A connection between two nodes.
 */
class Connection {
	private Node source;
	private Node target;
	private Edge edge;

	Connection(Node source, Node target, Edge edge) {
		this.source = source;
		this.target = target;
		this.edge = edge;
	}

	void print(PrintWriter out) {
		out.print(source.getEdgeSourceId());
		out.print(" -> ");
		out.print(target.getEdgeTargetId());
		out.print(" ");
		edge.printAttributes(out);
		out.println(";");
	}
}
