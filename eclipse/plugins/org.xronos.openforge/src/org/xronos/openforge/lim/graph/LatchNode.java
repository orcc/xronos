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
 * $Id: LatchNode.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package org.xronos.openforge.lim.graph;

import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.util.graphviz.Record;

/**
 * A LatchNode is a node for a {@link Latch} in an {@link LXGraph}. It labels
 * its enable port as "en".
 * 
 * @version $Id: LatchNode.java 2 2005-06-09 20:00:48Z imiller $
 */
class LatchNode extends ComponentNode {
	LatchNode(Latch latch, String id, int fontSize) {
		super(latch, id, fontSize);
	}

	@Override
	protected void graphPorts(Record.Port boundingBox) {
		if (needPortGraph()) {
			Record.Port entryBox = boundingBox.getPort(ENTRY);
			entryBox.setSeparated(false);

			Latch latch = (Latch) getComponent();
			graphPort(latch.getEnablePort(), entryBox, "en", "en");
			graphPort(latch.getDataPort(), entryBox, "din", "d");
		}
	}
}
