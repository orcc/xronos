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
 * $Id: FeedbackRegSourceNode.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package org.xronos.openforge.lim.graph;

import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.primitive.Reg;

/**
 * In a {@link LXGraph}, FeedbackRegSourceNode is one of the two nodes that are
 * created to represent a feedback register in a {@link Loop}. In particular, it
 * shows the register as a data source (ie, at the top of the loop).
 * 
 * @version $Id: FeedbackRegSourceNode.java 2 2005-06-09 20:00:48Z imiller $
 */
class FeedbackRegSourceNode extends ComponentNode {
	FeedbackRegSourceNode(Reg reg, String id, int fontSize) {
		super(reg, id, fontSize);
	}

	@Override
	protected String getBodyLabel() {
		StringBuffer labelBuf = new StringBuffer();
		labelBuf.append(getShortClassName(getComponent()));
		labelBuf.append("-FB\\n");
		labelBuf.append("@");
		labelBuf.append(Integer.toHexString(getComponent().hashCode()));
		return labelBuf.toString();
	}

	@Override
	protected boolean needPortGraph() {
		return false;
	}
}
