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

import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Decision;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * XBranchFactory constructs a {@link Branch} object for an 'if' element in the
 * SLIM document.
 * 
 * 
 * <p>
 * Created: Wed Jul 13 12:50:32 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public class XBranchFactory extends XModuleFactory {

	/**
	 * Constructs a new XBranchFactory which may be used to build a Branch whose
	 * implementation makes use of the resources defined in the specified
	 * {@link ResourceCache}
	 * 
	 * @param resources
	 *            a value of type 'ResourceCache'
	 */
	public XBranchFactory(ResourceCache resources) {
		super(resources);
	}

	public Branch buildBranch(Element ifElement) {
		// A branch has 3 parts. A decision block, a true block and a
		// (potentially empty) false block
		Element testElement = null;
		Element thenElement = null;
		Element elseElement = null;
		List<Node> modules = getChildNodesByTag(ifElement, SLIMConstants.MODULE);
		for (Node ele : modules) {
			Element element = (Element) ele;
			String moduleType = element
					.getAttribute(SLIMConstants.MODULE_STYLE);
			if (moduleType.equals(SLIMConstants.DECISION_TEST)) {
				testElement = element;
			} else if (moduleType.equals(SLIMConstants.THEN)) {
				thenElement = element;
			} else if (moduleType.equals(SLIMConstants.ELSE)) {
				elseElement = element;
			} else {
				assert false : "Unknown module type in 'if' structure: "
						+ moduleType;
			}
		}
		assert testElement != null : "'if' structure must have a test element";
		assert thenElement != null : "'if' structure must have a then element";

		XDecisionFactory decisionFactory = new XDecisionFactory(
				getResourceCache());
		Decision decision = (Decision) decisionFactory
				.buildComponent(testElement);

		XModuleFactory thenFactory = new XModuleFactory(getResourceCache());
		Block thenBlock = (Block) thenFactory.buildComponent(thenElement);

		decisionFactory.publishPorts(getPortCache());
		thenFactory.publishPorts(getPortCache());

		Branch branch;
		if (elseElement != null) {
			XModuleFactory elseFactory = new XModuleFactory(getResourceCache());
			Block elseBlock = (Block) elseFactory.buildComponent(elseElement);
			branch = new Branch(decision, thenBlock, elseBlock);
			elseFactory.publishPorts(getPortCache());
		} else {
			branch = new Branch(decision, thenBlock);
		}

		setModule(branch);

		createInterface(ifElement, getPortCache());
		buildDependencies(ifElement, getPortCache());

		getResourceCache().registerConfigurable(ifElement, branch);

		buildOptionScope(ifElement, branch);

		return branch;
	}

}// XBranchFactory
