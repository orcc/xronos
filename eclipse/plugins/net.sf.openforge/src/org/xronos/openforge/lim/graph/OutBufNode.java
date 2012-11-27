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
 * $Id: OutBufNode.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package org.xronos.openforge.lim.graph;

import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.OutBuf;

/**
 * OutBufNode represents an {@link OutBuf} in a {@link LXGraph}. In particular
 * it is labeled with the Tag of its peer {@link Exit}.
 * 
 * @version $Id: OutBufNode.java 2 2005-06-09 20:00:48Z imiller $
 */
class OutBufNode extends ComponentNode {
	OutBufNode(OutBuf outbuf, String id, int fontSize) {
		super(outbuf, id, fontSize);
	}

	@Override
	protected String getBodyLabel() {
		String label = super.getBodyLabel();
		OutBuf outbuf = (OutBuf) getComponent();
		return label + "\\n" + outbuf.getPeer().getTag();
	}
}
