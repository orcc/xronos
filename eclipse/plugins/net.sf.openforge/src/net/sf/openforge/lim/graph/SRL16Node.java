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

package net.sf.openforge.lim.graph;

import net.sf.openforge.lim.SRL16;

/**
 * SRL16Node.java
 */
public class SRL16Node extends ComponentNode {

	SRL16Node(SRL16 component, String id, int fontSize) {
		super(component, id, fontSize);
	}

	@Override
	protected String getBodyLabel() {
		StringBuffer labelBuf = new StringBuffer();
		labelBuf.append(getShortClassName(getComponent()));
		final int stages = ((SRL16) getComponent()).getStages();
		labelBuf.append("(" + stages + ")");
		labelBuf.append("\\n");
		labelBuf.append("@");
		labelBuf.append(Integer.toHexString(getComponent().hashCode()));
		return labelBuf.toString();
	}

}// SRL16Node
