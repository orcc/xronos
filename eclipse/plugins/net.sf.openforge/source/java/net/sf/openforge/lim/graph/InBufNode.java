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
 * $Id: InBufNode.java 2 2005-06-09 20:00:48Z imiller $
 *
 * 
 */

package net.sf.openforge.lim.graph;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.util.graphviz.Record;

/**
 * InBufNode represents an {@link InBuf} in a {@link LXGraph}. In particular it
 * relabels its buses.
 * 
 * @version $Id: InBufNode.java 2 2005-06-09 20:00:48Z imiller $
 */
class InBufNode extends ComponentNode {
	InBufNode(InBuf inbuf, String id, int fontSize) {
		super(inbuf, id, fontSize);
	}

	@Override
	protected void graphBuses(Exit exit, Record.Port busBox) {
		InBuf inbuf = (InBuf) getComponent();

		graphBus(inbuf.getGoBus(), busBox, "go", "G");
		Bus reset = inbuf.getResetBus();
		if (reset.isConnected()) {
			graphBus(reset, busBox, "reset", "R");
		}
		Bus clock = inbuf.getClockBus();
		if (clock.isConnected()) {
			graphBus(clock, busBox, "clock", "C");
		}
		int index = 0;
		for (Bus bus : inbuf.getDataBuses()) {
			graphBus(bus, busBox, "din" + index, "d" + index);
			index++;
		}
	}
}
