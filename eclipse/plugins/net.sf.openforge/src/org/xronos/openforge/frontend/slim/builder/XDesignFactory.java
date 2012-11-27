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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.Forge;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.OptionKey;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.Configurable;
import org.xronos.openforge.app.project.SearchLabel;
import org.xronos.openforge.frontend.slim.builder.ActionIOHandler.NativeIOHandler;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.memory.AddressStridePolicy;
import org.xronos.openforge.lim.memory.AddressableUnit;
import org.xronos.openforge.lim.memory.Allocation;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.lim.memory.Record;
import org.xronos.openforge.lim.memory.Scalar;

/**
 * XDesignFactory is a factory class used to generate a fully populated
 * {@link Design} object from an XLIM XML document. The design includes one or
 * more {@link LogicalMemory} objects for the state variables and a {@link Task}
 * for each Action module.
 * 
 * 
 * <p>
 * Created: Tue Jul 12 12:07:03 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public class XDesignFactory extends XFactory {

	// Used for assigning configuration values at the unscoped config level
	private static final Configurable UNSCOPED_CONFIG = new Configurable() {
		@Override
		public Configurable getConfigurableParent() {
			return null;
		}

		@Override
		public String getOptionLabel() {
			return null;
		}

		@Override
		public SearchLabel getSearchLabel() {
			return CodeLabel.UNSCOPED;
		}

		@Override
		public GenericJob getGenericJob() {
			return EngineThread.getGenericJob();
		}
	};

	/**
	 * Constructs a new DesignFactory
	 * 
	 */
	public XDesignFactory() {
	}

	/**
	 * Build the {@link Design} object and populate it with the structures
	 * necessary to implement the XLIM specification in the document.
	 * 
	 * @param doc
	 *            a non-null Document, XLIM representation.
	 * @return a non-null Design
	 */
	public Design buildDesign(Element designElement) {
		final ResourceCache resources = new ResourceCache();

		String designName = designElement.getAttribute(SLIMConstants.NAME);
		if (designName.length() == 0) {
			designName = "xlimApp_"
					+ Integer.toHexString(designElement.hashCode());
		}

		Design design = new Design();
		design.setIDLogical(designName);
		GenericJob job = EngineThread.getGenericJob();
		job.getOption(OptionRegistry.TOP_MODULE_NAME).setValue(
				design.getSearchLabel(), designName);

		NodeList directChildren = designElement.getChildNodes();
		List<Node> topLevelModules = new ArrayList<Node>();
		List<Node> topLevelOps = new ArrayList<Node>();
		List<Node> topLevelConnections = new ArrayList<Node>();
		Map<LogicalValue, Node> stateVars = new HashMap<LogicalValue, Node>();
		for (int i = 0; i < directChildren.getLength(); i++) {
			Node node = directChildren.item(i);
			if (node.getNodeName().equals(SLIMConstants.ACTOR_PORT)) {
				// <port name="I" dbgTag="" size="10" dir="input" tag="d2e7"/>
				ActionIOHandler ioHandler = new ActionIOHandler.FifoIOHandler(
						node);
				ioHandler.build(design);
				resources.addIOHandler(node, ioHandler);
			} else if (node.getNodeName().equals(
					SLIMConstants.ACTOR_NATIVE_PORT)) {
				NativeIOHandler ioHandler = new ActionIOHandler.NativeIOHandler(
						node);
				ioHandler.build(design);
				resources.addIOHandler(node, ioHandler);
			} else if (node.getNodeName().equals(SLIMConstants.INTERNAL_PORT)) {
				// <internal-port name="internal_action1_go" typeName="bool"
				// size="1" tag="idm0"/>
				ActionIOHandler ioHandler = new ActionIOHandler.InternalPinHandler(
						node);
				ioHandler.build(design);
				resources.addIOHandler(node, ioHandler);
			} else if (node.getNodeName().equals(SLIMConstants.STATE_VAR)) {
				/*
				 * if (logicalMemory == null) { // 32 should be more than enough
				 * for max address // width logicalMemory = new
				 * LogicalMemory(32); logicalMemory.createLogicalMemoryPort();
				 * design.addMemory(logicalMemory); } // Create a 'location' for
				 * the state var that is // appropriate for its type/size.
				 * Allocation loc =
				 * logicalMemory.allocate(createInitialValue(node));
				 * setAttributes(node, loc); resources.addLocation(node, loc);
				 */
				stateVars.put(createInitialValue(node), node);
			} else if (node.getNodeName().equals(SLIMConstants.OPERATION)) {
				topLevelOps.add(node);
			} else if (node.getNodeName().equals(SLIMConstants.CONNECTION)) {
				topLevelConnections.add(node);
			} else if (node.getNodeName().equals(SLIMConstants.MODULE)) {
				topLevelModules.add(node);
			} else if (node.getNodeName().equals(SLIMConstants.CONFIG_OPTION)) {
				resources.registerConfigurable(designElement, UNSCOPED_CONFIG);
			} else if (node.getNodeType() == Node.TEXT_NODE) {
			} // Do nothing with text nodes
			else {
				System.out
						.println("WARNING!!! Unknown element type at design level "
								+ node.getNodeName());
			}
		}

		// Allocate each LogicalValue (State Variable) in a memory
		// with a matching address stride. This provides consistency
		// in the memories and allows for state vars to be co-located
		// if area is of concern.
		Map<Integer, LogicalMemory> memories = new HashMap<Integer, LogicalMemory>();
		for (LogicalValue lvalue : stateVars.keySet()) {
			int stride = lvalue.getAddressStridePolicy().getStride();
			LogicalMemory mem = memories.get(stride);
			if (mem == null) {
				// 32 should be more than enough for max address
				// width
				mem = new LogicalMemory(32);
				mem.createLogicalMemoryPort();
				design.addMemory(mem);
			}
			// Create a 'location' for the state var that is
			// appropriate for its type/size.
			Allocation loc = mem.allocate(lvalue);
			Node node = stateVars.get(lvalue);
			setAttributes(node, loc);
			resources.addLocation(node, loc);

		}

		PortCache portCache = new PortCache();
		final Map<String, Task> taskModules = new HashMap<String, Task>();
		for (Node moduleNode : topLevelModules) {
			final XCallFactory callFactory = new XCallFactory(resources);
			Call call = (Call) callFactory.buildComponent(moduleNode);
			setAttributes(moduleNode, call.getProcedure().getBody());
			setAttributes(moduleNode, call.getProcedure());
			setAttributes(moduleNode, call);
			callFactory.publishPorts(portCache);
			initTopLevel(call);

			Task task = new Task(call);
			String taskName = ((Element) moduleNode)
					.getAttribute(SLIMConstants.NAME);
			String autostart = ((Element) moduleNode)
					.getAttribute(SLIMConstants.AUTOSTART);
			task.setKickerRequired(false);
			if (autostart != null) {
				task.setKickerRequired(autostart.toUpperCase().equals("TRUE"));
			}
			call.setIDLogical(taskName);
			task.setSourceName(taskName);
			taskModules.put(taskName, task);
			design.addTask(task);
		}
		// Associate all taskCall nodes with the created task for
		// their target
		for (Element taskCallNode : resources.getTaskCallNodes()) {
			final String taskID = taskCallNode
					.getAttribute(SLIMConstants.RESOURCE_TARGET);
			final Task task = taskModules.get(taskID);
			if (task == null)
				throw new IllegalStateException(
						"Task Call to non-existant task "
								+ SLIMConstants.RESOURCE_TARGET + ": " + taskID);
			final TaskCall taskCall = resources.getTaskCall(taskCallNode);
			taskCall.setTarget(task);
		}

		connectDesign(topLevelOps, topLevelConnections, design, portCache,
				resources);

		for (Task task : design.getTasks()) {
			Call call = task.getCall();
			if (call.getExit(Exit.DONE).getDoneBus().isConnected()) {
				call.getProcedure().getBody().setProducesDone(true);
			}
			if (call.getGoPort().isConnected()) {
				call.getProcedure().getBody().setConsumesGo(true);
			}
		}

		// Apply configuration attributes to the compilation now that
		// the entire design has been constructed. We MUST wait until
		// here in order for the search labels to be properly
		// configured for each Component.
		applyConfiguration(design, resources);

		if (_parser.db) {
			System.out.println("Created design with "
					+ design.getLogicalMemories());
			for (LogicalMemory mem : design.getLogicalMemories()) {
				System.out.println(mem.getLValues());
				mem.showContents();
			}
		}

		return design;
	}

	/**
	 * Generates a {@link LogicalValue} from a {@link SLIMConstants#STATE_VAR}
	 * Node.
	 * 
	 * @param node
	 *            a Node of type {@link SLIMConstants#STATE_VAR}
	 * @return the LogicalValue representation of the initial value of the state
	 *         var node.
	 */
	private LogicalValue createInitialValue(Node node) {
		assert node.getNodeName().equals(SLIMConstants.STATE_VAR);
		assert node.getNodeType() == Node.ELEMENT_NODE;
		final Element element = (Element) node;

		final List<Node> contained = getChildNodesByTag(element,
				SLIMConstants.IVALUE);
		assert contained.size() == 1 : "State Var element must contain exactly one initValue element";

		Element initValue = (Element) contained.get(0);

		return makeLogicalValue(initValue);
	}

	private LogicalValue makeLogicalValue(Element initValue) {
		LogicalValue logicalValue;

		if (initValue.getAttribute(SLIMConstants.TYPENAME).equalsIgnoreCase(
				SLIMConstants.IVALUE_LIST_TYPE)) {
			List<Node> children = getChildNodesByTag(initValue,
					SLIMConstants.IVALUE);
			List<LogicalValue> subElements = new ArrayList<LogicalValue>(
					children.size());
			for (Node node : children) {
				Element child = (Element) node;
				LogicalValue childLogicalValue = makeLogicalValue(child);
				subElements.add(childLogicalValue);
			}

			logicalValue = new Record(subElements);
		} else {
			// scalar
			// final boolean isSigned = isSignedPort(initValue);
			final String sizeString = initValue
					.getAttribute(SLIMConstants.IVALUE_SIZE);
			final int bitSize = Integer.parseInt(sizeString);
			String valueString = initValue
					.getAttribute(SLIMConstants.IVALUE_VALUE);
			if (valueString.length() == 0) {
				System.out
						.println("WARNING.  Uninitialized state var.  Defaulting to 0");
				valueString = "0";
			}

			// For now we are only supporting address strides that are
			// in terms of whole 'initValue' elements. That is, the
			// address stride for each logical value is the bit width
			// of that logical value. Thus each logical value is one
			// and only one address. This could be relaxed by
			// introducing an addressStride attribute to initValue,
			// however that would imply that the parser knows how to
			// divide a numeric 'value' attribute value into the
			// appropriate AddressableUnits.
			final BigInteger value;
			if (valueString.trim().toUpperCase().startsWith("0X")) {
				value = new BigInteger(valueString.trim().substring(2), 16);
			} else {
				value = new BigInteger(valueString);
			}

			AddressStridePolicy addrPolicy = new AddressStridePolicy(bitSize);
			logicalValue = new Scalar(new AddressableUnit(value), addrPolicy);
		}

		assert logicalValue != null;
		if (_parser.db)
			System.out.println("Created logical value");
		if (_parser.db)
			System.out.println(logicalValue.toString());
		return logicalValue;
	}

	private static void connectDesign(List<Node> ops, List<Node> conns,
			Design design, PortCache portCache, ResourceCache resources) {
		XOperationFactory opFactory = new XOperationFactory(resources);
		for (Node nd : ops) {
			Element node = (Element) nd;
			assert node.getAttribute(SLIMConstants.ELEMENT_KIND).equals(
					"pinRead")
					|| node.getAttribute(SLIMConstants.ELEMENT_KIND).equals(
							"pinWrite");
			Component comp = opFactory.makeOperation(node, portCache);
			design.addComponentToDesign(comp);
		}

		// Now use the connections and the port cache
		for (Node nd : conns) {
			Element connection = (Element) nd;
			// Find source and destination
			// <connection name="zd116e229" source="dzd2e60" dest="zd71e99"/>
			String sourceName = connection
					.getAttribute(SLIMConstants.CONN_SOURCE);
			String destName = connection.getAttribute(SLIMConstants.CONN_DEST);
			Bus source = portCache.getSource(sourceName);
			Port dest = portCache.getTarget(destName);
			if (dest.isConnected())
				throw new IllegalStateException(
						"Connection defined for previously connected port");
			if (_parser.db)
				System.out.println("Connecting source: " + source
						+ " to dest: " + dest + " of "
						+ source.getOwner().getOwner().show() + " to "
						+ dest.getOwner().show());
			dest.setBus(source);
		}
	}

	private static void initTopLevel(Call call) {
		call.getClockPort().setSize(1, false);
		call.getResetPort().setSize(1, false);
		call.getGoPort().setSize(1, false);
	}

	/**
	 * <code>applyConfiguration</code> uses the configuration map from the
	 * {@link ResourceCache} to process all LIM Components/nodes which have
	 * configuration information attached. Each entry in the map has at least
	 * one {@link SLIMConstants#CONFIG_OPTION}. These options are annotated to
	 * the options database as appropriate.
	 * 
	 * @param des
	 *            a <code>Design</code> value
	 * @param res
	 *            a <code>ResourceCache</code> value
	 */
	private static void applyConfiguration(Design des, ResourceCache res) {
		// For each element in the resource cache configurables, find
		// any configuration options and apply them.
		final Map<Configurable, Element> configMap = res.getConfigurableMap();
		for (Configurable config : configMap.keySet()) {
			Element element = configMap.get(config);
			NodeList elementChildren = element.getChildNodes();
			for (int i = 0; i < elementChildren.getLength(); i++) {
				Node node = elementChildren.item(i);
				if (!node.getNodeName().equalsIgnoreCase(
						SLIMConstants.CONFIG_OPTION)) {
					continue;
				}

				Element option = (Element) node;
				String name = option.getAttribute(SLIMConstants.CONFIG_NAME);
				String value = option.getAttribute(SLIMConstants.CONFIG_VALUE);

				if (name.equalsIgnoreCase(SLIMConstants.CONFIG_LOOPUNROLL)) {
					// Special case since we want to enable pipelining
					// as well as setting the value. Chances are good
					// we could permanently enable pipelining and not
					// make this a special case.
					final Integer valueInt = Integer.valueOf(value);
					if (_parser.db)
						System.out
								.println("Setting loop unroll limit of "
										+ config + " w/ search label: ->"
										+ config.getSearchLabel() + "<- to "
										+ valueInt);
					String enable = "true";
					if (valueInt.intValue() < 0) {
						enable = "false";
					}
					config.getGenericJob()
							.getOption(OptionRegistry.LOOP_UNROLLING_ENABLE)
							.setValue(config.getSearchLabel(), enable);
					config.getGenericJob()
							.getOption(OptionRegistry.LOOP_UNROLLING_LIMIT)
							.setValue(config.getSearchLabel(), valueInt);
				} else {
					// Try matching against all known options
					boolean match = false;
					for (OptionKey key : OptionRegistry.OPTION_KEYS) {
						if (name.equals(key.getKey())) {
							match = true;
							if (_parser.db)
								System.out.println("Setting config option "
										+ key.getKey() + " of config scope "
										+ config + " w/ search label: ->"
										+ config.getSearchLabel() + "<- to "
										+ value);
							config.getGenericJob().getOption(key)
									.setValue(config.getSearchLabel(), value);
							break;
						}
					}
					if (!match) {
						throw new IllegalArgumentException("The "
								+ SLIMConstants.CONFIG_OPTION + " with name \""
								+ name + "\" is unknown");
					}
				}
			}
			// Sanity check the options
			Forge.checkOptions(EngineThread.getGenericJob());
		}
	}

}// XDesignFactory
