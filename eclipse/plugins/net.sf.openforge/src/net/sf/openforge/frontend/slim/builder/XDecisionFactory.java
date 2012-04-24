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

package net.sf.openforge.frontend.slim.builder;

import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Port;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * XDecisionFactory generates structurally correct {@link Decision} objects for
 * slim modules with kind=test.
 * 
 * <p>
 * Created: Mon Jul 11 14:01:38 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public class XDecisionFactory extends XModuleFactory {

	public XDecisionFactory(ResourceCache cache) {
		super(cache);
	}

	/**
	 * Builds a correct {@link Decision} component for the specified moduleNode.
	 * 
	 * @param moduleNode
	 *            an Element object
	 */
	@Override
	public Component buildComponent(Node moduleNode) {
		assert moduleNode instanceof Element;
		Element testElement = (Element) moduleNode;

		Block testBlock = (Block) super.buildComponent(testElement);
		Component decisionComp = findDecision(testElement, getComponentMap());

		Decision decision = new Decision(testBlock, decisionComp);

		setModule(decision);

		// Any data inputs to the decision need to be propagated from
		// the block to the decision. There should be no output ports
		// to propagate. They are inferred true/false.
		propagateInputs(decision, testBlock);

		getResourceCache().registerConfigurable(moduleNode, decision);

		buildOptionScope(moduleNode, decision);

		return decision;
	}

	/**
	 * Identifies the component which is the actual 'decision' component for the
	 * branching. The test block may contain a number of operations to achieve
	 * the computation of the decision. The returned component is a boolean (ie
	 * one bit output) Component defining the result of the decision.
	 * 
	 * @param testElement
	 *            the Element of type 'test' from the XLIM if structure.
	 * @param operations
	 *            a Map of {@link Node} to {@link Component} for all the element
	 *            of the 'test' block in the XLIM 'if'.
	 * @return a non-null Component
	 */
	private Component findDecision(Element testElement,
			Map<Node, Component> operations) {
		// Capture the component which is the 'test'
		String testTag = testElement.getAttribute(SLIMConstants.DECISION);
		for (Node node : operations.keySet()) {
			Element operation = (Element) node;
			// Find the only exit of the node. Then look for the
			// named bus in the exit
			List<Node> exits = getChildNodesByTag(operation, SLIMConstants.EXIT);
			if (exits.size() != 1) {
				System.err.println("Unexpected number (" + exits.size()
						+ ") of exits in test block");
				continue;
			}
			Element exit = (Element) exits.get(0);
			List<Node> ports = getChildNodesByTag(exit, SLIMConstants.PORT);
			for (Node nd : ports) {
				Element port = (Element) nd;
				if (port.getAttribute(SLIMConstants.NAME).equals(testTag)) {
					Component testComp = operations.get(operation);
					return testComp;
				}
			}
		}

		throw new IllegalStateException("Decision component not found");
	}

	/**
	 * The LIM branch structure has an additional level of hierarchy not
	 * reflected in the XLIM document. Specifically the Decision has a test
	 * block within it. The 'test' element in the XLIM document corresponds with
	 * the LIM test block. The Decision contains the logical Not and And
	 * components to generate 2 active high outputs, a true and a false. This
	 * method propagates/routes the testBlock input ports out to the Decision
	 * block and re-maps the port definitions accordingly.
	 * 
	 * @param decision
	 *            a non-null Decision
	 * @param testBlock
	 *            a non-null Block
	 */
	private void propagateInputs(Decision decision, Block testBlock) {
		final PortCache cache = getPortCache();

		for (Port port : testBlock.getDataPorts()) {
			Port decisionPort = decision.makeDataPort();
			Entry entry = port.getOwner().getEntries().get(0);
			entry.addDependency(port,
					new DataDependency(decisionPort.getPeer()));
			cache.replaceTarget(port, decisionPort);
		}
	}

} // XDecisionFactory

