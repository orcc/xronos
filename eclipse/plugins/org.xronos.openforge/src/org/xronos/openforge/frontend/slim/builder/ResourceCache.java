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

package org.xronos.openforge.frontend.slim.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xronos.openforge.app.project.Configurable;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LogicalMemory;

/**
 * ResourceCache maintains mappings from the Node objects to all the Design
 * level resources in the implementation created for those Nodes. This includes
 * input/output structures and memory allocated for state variables.
 * 
 * 
 * <p>
 * Created: Tue Jul 12 16:00:41 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public class ResourceCache extends CacheBase {

	/** Map of Node to ActionIOHandler objects */
	private final Map<Node, ActionIOHandler> ioHandlers = new HashMap<Node, ActionIOHandler>();
	/** Map of Node to Location objects */
	private final Map<Node, Location> memLocations = new HashMap<Node, Location>();
	/** Map of Node to TaskCall objects */
	private final Map<Element, TaskCall> taskCalls = new HashMap<Element, TaskCall>();
	/** Map of Node to net.sf.openforge.app.project.Configurable object */
	private final Map<Configurable, Element> configurableElements = new HashMap<Configurable, Element>();

	public ResourceCache() {
	}

	/**
	 * Specifies that the given {@link Location} was created as the
	 * implementation of the specified {@link Node}. The specific
	 * {@link LogicalMemory} in which the location is allocated may be obtained
	 * directly from the Location object.
	 * 
	 * @param node
	 *            a non-null Node
	 * @param loc
	 *            a non-null Location
	 */
	public void addLocation(Node node, Location loc) {
		memLocations.put(node, loc);
	}

	/**
	 * Returns the {@link Location} that was defined for the Node whose key
	 * attribute ({@see CacheBase#getNodeForString}) has the specified value.
	 * 
	 * @param nodeName
	 *            a non-null String
	 * @return the non-null Location which was associated with the specified
	 *         Node.
	 */
	public Location getLocation(String nodeName) {
		return memLocations.get(getNodeForString(nodeName, memLocations));
	}

	/**
	 * Specifies the specific {@link ActionIOHandler} which was created as the
	 * implementation for the given {@link Node}.
	 * 
	 * @param node
	 *            a non-null Node
	 * @param io
	 *            a non-null ActionIOHandler
	 */
	public void addIOHandler(Node node, ActionIOHandler io) {
		ioHandlers.put(node, io);
	}

	/**
	 * Returns the {@link ActionIOHandler} that was defined for the Node whose
	 * key attribute ({@see CacheBase#getNodeForString}) has the specified
	 * value.
	 * 
	 * @param nodeName
	 *            a non-null String
	 * @return the non-null ActionIOHandler which was associated with the
	 *         specified Node.
	 */
	public ActionIOHandler getIOHandler(String nodeName) {
		return ioHandlers.get(getNodeForString(nodeName, ioHandlers));
	}

	public void addTaskCall(Element node, TaskCall call) {
		taskCalls.put(node, call);
	}

	public Set<Element> getTaskCallNodes() {
		return taskCalls.keySet();
	}

	public TaskCall getTaskCall(Element node) {
		return taskCalls.get(node);
	}

	/**
	 * If needed, the specified Node and Configurable are registered as
	 * containing compiler configuration information. This information is
	 * annotated to the options database after the entire design is constructed.
	 */
	public void registerConfigurable(Node node, Configurable config) {
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node child = nodeList.item(i);
			if ((child instanceof Element)
					&& ((Element) child).getNodeName().equalsIgnoreCase(
							SLIMConstants.CONFIG_OPTION)) {
				configurableElements.put(config, (Element) node);
			}
		}
	}

	/**
	 * Returns a Map of Configurable to Element. Each of the Elements contains
	 * at least one child node of type: {@link SLIMConstants#CONFIG_OPTION}.
	 */
	public Map<Configurable, Element> getConfigurableMap() {
		return Collections.unmodifiableMap(configurableElements);
	}

}// ResourceCache
