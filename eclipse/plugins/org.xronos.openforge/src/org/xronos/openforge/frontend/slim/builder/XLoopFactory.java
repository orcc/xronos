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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.LoopBody;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.primitive.Reg;

/**
 * The XLoopFactory is used to build loops from an SLIM loop construct. Most
 * functionality is deferred to other factories to build the constituent parts
 * with a bit of intelligence here to build the latches and feedback registers.
 * <p>
 * A typical SLIM loop construct looks like:
 * 
 * <pre>
 *  	<module kind="loop">
 * 	  <PHI name="b">
 * 		<port dir="in" source="const0_0"/>
 * 		<port dir="in" source="add_2"/>
 * 		<port dir="out" typeName="int" name="b_merged" source="b_merged"/>
 * 	  </PHI>
 * 	  <PHI>
 * 		<port dir="in" source="const0_1"/>
 * 		<port dir="in" source="add_1"/>
 * 		<port dir="out" typeName="int" name="i_merged" source="i_merged"/>
 * 	  </PHI>
 * 	  <module kind="test" decision="lt1">
 * 		<operation kind="$literal_Integer" value="7">
 * 		  <port name="const7" dir="out" size="32" source="const7" typeName="int"/>
 * 		</operation>
 * 		<operation kind="$lt">
 * 		  <port dir="in" source="i_merged"/>
 * 		  <port dir="in" source="const7"/>
 * 		  <port dir="out" name="lt1" typeName="bool" source="lt1"/>
 * 		</operation>
 * 	  </module>
 * 
 * 	  <module kind="body">
 * 		<operation kind="$add">
 * 		  <port dir="in" source="i_merged"/>
 * 		  <port dir="in" source="a"/>
 * 		  <port name="add_1" dir="out" typeName="int" source="add_1"/>
 * 		</operation>
 * 		<operation kind="$literal_Integer" value="1">
 * 		  <port name="const1" dir="out" size="2" source="const1" typeName="int"/>
 * 		</operation>
 * 		<operation kind="$add">
 * 		  <port dir="in" source="b_merged"/>
 * 		  <port dir="in" source="const1"/>
 * 		  <port name="add_2" dir="out" typeName="int" source="add_2"/>
 * 		</operation>
 * 	  </module>
 * 	  <port name="b_out" dir="out" source="b_merged" typeName="int"/>
 * 	</module>
 * </pre>
 */
public class XLoopFactory extends XModuleFactory {

	public XLoopFactory(ResourceCache cache) {
		super(cache);
	}

	public Loop buildLoop(Element loopElement) {
		// The loop element contains:
		// ports
		// loopbody
		// test
		// body

		// First, construct the loop body
		List<Node> loopModules = getChildNodesByTag(loopElement,
				SLIMConstants.MODULE);
		assert loopModules.size() == 1 : "Must be exactly one module in a loop";
		Element loopBodyModule = (Element) loopModules.get(0);

		XLoopBodyFactory bodyFactory = new XLoopBodyFactory(getResourceCache());
		LoopBody loopBody = (LoopBody) bodyFactory
				.buildComponent(loopBodyModule);

		bodyFactory.publishPorts(getPortCache());

		Loop loop = new Loop(loopBody);
		setModule(loop);

		createInterface(loopElement, getPortCache());

		buildDependencies(loopElement, getPortCache());

		if (loopElement.getAttribute("endflop").toUpperCase().equals("NO"))
			loop.getBody().setLoopFlopNeeded(false);

		getResourceCache().registerConfigurable(loopElement, loop);

		buildOptionScope(loopElement, loop);

		return loop;
	}

	/**
	 * Override of the super to ensure that appropriate feedback registers and
	 * input data latches get created. Defines the data and control flow
	 * relationships between the components of the block according to the
	 * {@link SLIMConstants#DEPENDENCY} elements of the XLIM module. This method
	 * creates only dependencies and not any physical connectivity. The
	 * implemented connectivity is currently handled by the Forge scheduling
	 * code.
	 * <p>
	 * This functionality could be extended by defining explicit connectivity
	 * and not scheduling the block.
	 * 
	 * @param moduleElement
	 *            a value of type 'Element'
	 * @param portCache
	 *            a value of type 'PortCache'
	 */
	@Override
	protected void buildDependencies(Element moduleElement, PortCache portCache) {
		// List dependencies =
		// getChildNodesByTag(moduleElement,XLIMConstants.DEPENDENCY);
		List<Node> dependencies = getDependencies(moduleElement);

		// Build dependencies. They fall into 3 categores:
		// 1. Feedback dependency. From loop body output to loop body
		// input in group 1. Create a feedback register for it.
		// 2. Initial dependency. From loop input to loop body input
		// in group 0. There are 2 flavors of this type of
		// dependency. Those that share the port with a feedback
		// dependency and those that do not. If they do not, then the
		// need a data latch.
		// 3. Output dependency. From loop body output to loop
		// output. This is a standard 'direct' dependency.

		// Convenience class
		class DepTuple {
			Bus source;
			@SuppressWarnings("unused")
			Port target;
			@SuppressWarnings("unused")
			int group;

			public DepTuple(Bus source, Port target, int group) {
				this.source = source;
				this.target = target;
				this.group = group;
			}
		}

		// Maps of port to dependency. We expect that when broken
		// down into these categories that there will only be 1
		// dependency of each type for a given port.
		Map<Port, DepTuple> feedbackDeps = new LinkedHashMap<Port, DepTuple>();
		Map<Port, DepTuple> initialDeps = new LinkedHashMap<Port, DepTuple>();
		Map<Port, DepTuple> outputDeps = new LinkedHashMap<Port, DepTuple>();
		final Loop loop = (Loop) getModule();
		final Exit fbExit = loop.getBody().getFeedbackExit();
		final Exit doneExit = loop.getBody().getLoopCompleteExit();
		final Exit initExit = loop.getInBuf().getExit(Exit.DONE);
		for (Node depNode : dependencies) {
			String sourceId = depNode.getAttributes()
					.getNamedItem(SLIMConstants.DEP_SOURCE).getNodeValue();
			String targetId = depNode.getAttributes()
					.getNamedItem(SLIMConstants.DEP_TARGET).getNodeValue();
			String groupId = depNode.getAttributes()
					.getNamedItem(SLIMConstants.DEP_GROUP).getNodeValue();
			int group = Integer.parseInt(groupId);
			Bus sourceBus = portCache.getSource(sourceId);
			Port targetPort = portCache.getTarget(targetId);
			if (sourceBus.getOwner() == fbExit) {
				assert !feedbackDeps.containsKey(targetPort) : targetId;
				feedbackDeps.put(targetPort, new DepTuple(sourceBus,
						targetPort, group));
			} else if (sourceBus.getOwner() == doneExit) {
				assert !outputDeps.containsKey(targetPort) : targetId;
				outputDeps.put(targetPort, new DepTuple(sourceBus, targetPort,
						group));
			} else if (sourceBus.getOwner() == initExit) {
				// It is possible, in the case of a loop consuming an
				// initial value, that the initial value is connected
				// to both the initial entry and the feedback entry.
				// Alternatively, the feedback entry may be
				// unconnected which is handled below (where the latch
				// is created)
				// assert !initialDeps.containsKey(targetPort) : targetId;
				initialDeps.put(targetPort, new DepTuple(sourceBus, targetPort,
						group));
			} else {
				throw new IllegalArgumentException(
						"Unknown dependency structure for loop "
								+ ((Element) depNode).getAttribute("tag"));
			}
		}

		// Snag the entries for the loop body. One is feedback and
		// one is the initial.
		final Entry initEntry = loop.getBodyInitEntry();
		final Entry fbEntry = loop.getBodyFeedbackEntry();
		// Also grab the bus which is driving the init entry.
		final Collection<Dependency> goInitDeps = initEntry
				.getDependencies(loop.getBody().getGoPort());
		assert goInitDeps.size() == 1;
		final Bus initDoneBus = goInitDeps.iterator().next().getLogicalBus();
		// Populate the initial entry with dependencies. Latch any
		// input which does not have a feedback dependency.
		for (java.util.Map.Entry<Port, DepTuple> mapEntry : initialDeps
				.entrySet()) {
			Port targetPort = mapEntry.getKey();
			Bus sourceBus = mapEntry.getValue().source;
			if (!feedbackDeps.containsKey(targetPort)) {
				Latch latch = loop.createDataLatch();
				Entry latchEntry = latch.makeEntry(initDoneBus.getOwner());
				latchEntry.addDependency(latch.getEnablePort(),
						new ControlDependency(initDoneBus));
				latchEntry.addDependency(latch.getDataPort(),
						new DataDependency(sourceBus));
				sourceBus = latch.getResultBus();
				// If there is not a connection for the feedback entry
				// for a given port, the port must be constant value
				// for the duration of the loop execution. Add a
				// feedback entry from the latch.
				Dependency dep = (targetPort == targetPort.getOwner()
						.getGoPort()) ? new ControlDependency(sourceBus)
						: new DataDependency(sourceBus);
				fbEntry.addDependency(targetPort, dep);
			}
			Dependency dep = (targetPort == targetPort.getOwner().getGoPort()) ? new ControlDependency(
					sourceBus) : new DataDependency(sourceBus);
			initEntry.addDependency(targetPort, dep);
		}

		// Build the feedback dependencies.
		// final Bus fbControl = fbExit.getDoneBus();
		for (java.util.Map.Entry<Port, DepTuple> mapEntry : feedbackDeps
				.entrySet()) {
			Port targetPort = mapEntry.getKey();
			Bus sourceBus = mapEntry.getValue().source;
			Reg fbReg = loop.createDataRegister();
			Entry entry = fbReg.makeEntry(fbExit);
			// XXX FIXME! Not doing a dep to the enable port. See
			// ScheduleVisitor.fixLoopDataRegisters for note on why.
			// UGLY!
			// entry.addDependency(fbReg.getEnablePort(), new
			// ControlDependency(fbControl));
			entry.addDependency(fbReg.getDataPort(), new DataDependency(
					sourceBus));
			fbEntry.addDependency(targetPort,
					new DataDependency(fbReg.getResultBus()));
		}

		// Build the output dependencies
		Entry outbufEntry = loop.getExit(Exit.DONE).getPeer().getEntries()
				.get(0);
		for (java.util.Map.Entry<Port, DepTuple> entry : outputDeps.entrySet()) {
			Port targetPort = entry.getKey();
			Bus sourceBus = entry.getValue().source;
			Dependency dep = (targetPort == targetPort.getOwner().getGoPort()) ? new ControlDependency(
					sourceBus) : new DataDependency(sourceBus);
			outbufEntry.addDependency(targetPort, dep);
		}
	}

	/**
	 * Helper class for building the loop body.
	 */
	static class XLoopBodyFactory extends XModuleFactory {
		public XLoopBodyFactory(ResourceCache cache) {
			super(cache);
		}

		/**
		 * Override the super so that we can generate both the decision and body
		 * and wrap them in a LoopBody.
		 */
		public Component buildComponent(Element loopBodyElement) {
			// Get the two modules out of the loop body
			List<Node> bodyModules = getChildNodesByTag(loopBodyElement,
					SLIMConstants.MODULE);
			assert bodyModules.size() == 2 : "Must be exactly two modules in loop body";
			Element testElement = null;
			Element bodyElement = null;
			for (Node nd : bodyModules) {
				Element element = (Element) nd;
				String modStyle = element
						.getAttribute(SLIMConstants.MODULE_STYLE);
				if (modStyle.equals(SLIMConstants.DECISION_TEST)) {
					testElement = element;
				} else if (modStyle.equals(SLIMConstants.LOOP_BODY)) {
					bodyElement = element;
				}
			}
			assert testElement != null;
			assert bodyElement != null;

			XDecisionFactory decFactory = new XDecisionFactory(
					getResourceCache());
			Decision decision = (Decision) decFactory
					.buildComponent(testElement);

			XModuleFactory bodyFactory = new XModuleFactory(getResourceCache());
			Block bodyBlock = (Block) bodyFactory.buildComponent(bodyElement);

			LoopBody loopBody = new WhileBody(decision, bodyBlock);
			setModule(loopBody);

			XOperationFactory opFactory = new XOperationFactory(
					getResourceCache());
			List<Component> otherComps = new ArrayList<Component>();
			for (Node node : getChildNodesByTag(loopBodyElement,
					SLIMConstants.OPERATION)) {
				Component comp = opFactory.makeOperation(node, getPortCache());
				otherComps.add(comp);
			}
			loopBody.addComponents(otherComps);

			decFactory.publishPorts(getPortCache());
			bodyFactory.publishPorts(getPortCache());

			// Build the ports of the loop body
			createInterface(loopBodyElement, getPortCache());

			// Now build the dependencies
			buildDependencies(loopBodyElement, getPortCache());

			getResourceCache().registerConfigurable(loopBodyElement, loopBody);

			buildOptionScope(loopBodyElement, loopBody);

			return loopBody;
		}

		/**
		 * Overrides the super to get the exit from the loop body (which creates
		 * its own exits when constructed) instead of creating a new one.
		 * 
		 * @param node
		 *            a Node, of type exit
		 * @param portCache
		 *            the PortCache which will be updated with the constructed
		 *            output ports.
		 */
		@Override
		protected void makeModuleExit(Node node, PortCache portCache) {
			final String exitType = node.getAttributes()
					.getNamedItem(SLIMConstants.EXIT_KIND).getNodeValue();

			final Exit exit;
			if (exitType.equals(SLIMConstants.EXIT_DONE))
				exit = ((LoopBody) getModule()).getLoopCompleteExit();
			else if (exitType.equals(SLIMConstants.EXIT_FEEDBACK))
				exit = ((LoopBody) getModule()).getFeedbackExit();
			else
				throw new IllegalArgumentException("Unsupported exit type "
						+ exitType);

			populateExit(exit, node, portCache);
		}

	}

}
