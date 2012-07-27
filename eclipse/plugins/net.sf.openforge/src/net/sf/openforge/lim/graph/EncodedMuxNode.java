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

import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.primitive.EncodedMux;
import net.sf.openforge.util.graphviz.Record;

/**
 * EncodedMuxNode.java
 * 
 * 
 * <p>
 * Created: Tue Dec 17 15:43:25 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: EncodedMuxNode.java 2 2005-06-09 20:00:48Z imiller $
 */
public class EncodedMuxNode extends ComponentNode {

	public EncodedMuxNode(Component component, String id, int fontSize) {
		super(component, id, fontSize);
	}

	@Override
	protected void graphPorts(Record.Port boundingBox) {
		EncodedMux em = (EncodedMux) getComponent();

		Record.Port entryBox = boundingBox.getPort(ENTRY);
		entryBox.setSeparated(false);
		graphPort(em.getGoPort(), entryBox, "go", "G");
		net.sf.openforge.lim.Port reset = em.getResetPort();
		if (reset.isConnected()) {
			graphPort(reset, entryBox, "reset", "R");
		}
		net.sf.openforge.lim.Port clock = em.getClockPort();
		if (clock.isConnected()) {
			graphPort(clock, entryBox, "clock", "C");
		}
		graphPort(em.getSelectPort(), entryBox, "select", "SEL");
		int index = 0;
		for (net.sf.openforge.lim.Port port : em.getDataPorts()) {
			graphPort(port, entryBox, "din" + index, "d" + index);
			index++;
		}
	}

}// EncodedMuxNode
