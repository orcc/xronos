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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A record node. A record may have zero or more ports, which are nested
 * records. Ports may also be the sources/targets of edges.
 * 
 * @author Stephen Edwards
 * @version $Id: Record.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Record extends Node {

	protected List<Port> ports = new LinkedList<Port>();
	protected String nodeLabel = null;
	// title is printed below the ports if defined
	private String title = null;

	/**
	 * True if a gap is to be inserted between this record and the next when
	 * nested
	 */
	private boolean isSeparated = true;

	/**
	 * Constructs a new record.
	 * 
	 * @param id
	 *            the identifier of the node
	 */
	public Record(String id) {
		super(id, "record");
		setLabel(id);
	}

	/**
	 * sets the title for the record
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	public void setSeparated(boolean isSeparated) {
		this.isSeparated = isSeparated;
	}

	public boolean isSeparated() {
		return isSeparated;
	}

	/**
	 * Creates and returns a new port.
	 * 
	 * @param id
	 *            the identifier of the port
	 * @return the new port
	 */
	public Port getPort(String id) {
		Port port = new Port(id);
		ports.add(port);
		return port;
	}

	/**
	 * Creates and returns a new anonymous port.
	 */
	public Port getPort() {
		return getPort(null);
	}

	/**
	 * A port within a record.
	 */
	public class Port extends Record {
		/**
		 * Creates and returns a new port within this port.
		 * 
		 * @param id
		 *            the identifier of the new port
		 * @return the new port
		 */
		@Override
		public Port getPort(String id) {
			Record.Port port = Record.this.new Port(id);
			ports.add(port);
			return port;
		}

		@Override
		public String getEdgeSourceId() {
			return getEdgeTargetId();
		}

		@Override
		public String getEdgeTargetId() {
			/*
			 * Gets the "name:port" form of the id, for use in connections.
			 */
			return Record.this.getEdgeTargetId() + ":" + getId();
		}

		@Override
		public Graph getGraph() {
			return Record.this.getGraph();
		}

		String getEmbeddedLabel() {
			StringBuffer buf = new StringBuffer();
			if (!ports.isEmpty()) {
				/*
				 * Sub-record case.
				 */
				buf.append("{");
				buf.append(getRecordLabel());
				buf.append("}");
			} else {
				/*
				 * Leaf record case.
				 */
				String id = getId();

				/*
				 * No default label for ports.
				 */
				String label = getNodeLabel();
				label = label.replaceAll(">", "GT");
				label = label.replaceAll("<", "LT");
				if (id != null) {
					buf.append("<");
					buf.append(id);
					buf.append(">");
					if (label != null) {
						buf.append(" ");
					}
				}
				if (label != null) {
					buf.append(label);
				}
			}
			return buf.toString();
		}

		private Port(String id) {
			super(id);
			setLabel("");
		}

	}

	@Override
	protected String getAttribute(String attr) {
		return (attr.equals("label") ? getRecordLabel() : super
				.getAttribute(attr));
	}

	@Override
	protected void setAttribute(String attr, String value) {
		if (attr.equals("label")) {
			nodeLabel = value;
		}
		super.setAttribute(attr, value);
	}

	protected String getNodeLabel() {
		return nodeLabel;
	}

	protected String getRecordLabel() {
		if (ports.isEmpty()) {
			return getNodeLabel();
		} else {
			StringBuffer buf = new StringBuffer();
			if (title != null) {
				// these characters need to be quoted
				char[] specialChars = { '>', '<', '|', '}', '{' };
				for (int i = 0; i < specialChars.length; i++) {
					int start = 0;
					int loc;

					while ((loc = title.indexOf(specialChars[i], start)) > -1) {
						String l = "";
						if (loc > 0) {
							l = title.substring(0, loc);
						}
						l += "\\" + title.substring(loc);
						start = loc + 2;
						title = l;
					}
				}

				buf.append("{{");
			}
			for (Iterator<Port> iter = ports.iterator(); iter.hasNext();) {
				Port port = iter.next();
				buf.append(port.getEmbeddedLabel());
				if (iter.hasNext()) {
					buf.append("|");
					if (port.isSeparated()) {
						buf.append("|");
					}
				}
			}
			if (title != null) {
				buf.append("} | " + title + "}");
			}

			return buf.toString();
		}
	}
}
