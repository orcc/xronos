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
 * 
 *
 * 
 */

package net.sf.openforge.lim;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * A Switch is implemented in terms of other constructs. Primarily, it is a
 * {@link Block} that contains only one component: another {@link Block} that is
 * the content of the switch. The reason for this indirection is that the BREAK
 * {@link Exit Exits} of the body must be diverted to DONE {@link Exit} of the
 * Switch. Other than this variation in its construction, a Switch behaves
 * exactly like any other {@link Block}. So it is up to the caller to provide
 * appropriate contents.
 * <P>
 * It is possible that this class may eventually be deprecated.
 * 
 * @version $Id: Switch.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Switch extends Block {

	/** The contents of the switch */
	private Component body;

	/**
	 * Constructs a new Switch.
	 * 
	 * @param body
	 *            the contents of the switch.
	 */
	public Switch(Component body) {
		super(Collections.singletonList(body));
		this.body = body;
	}

	/**
	 * Gets the contents of this switch.
	 */
	public Component getBody() {
		return body;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	protected void cloneNotify(Module module, Map cloneMap) {
		super.cloneNotify(module, cloneMap);
		final Switch clone = (Switch) module;
		clone.body = (Block) cloneMap.get(body);
	}

	protected void setControlDependencies() {
		super.setControlDependencies(false);

		/*
		 * Merge plain BREAK Exit and Exit with same label as the switch
		 * statement into the DONE Exit.
		 */
		Exit breakExit = getExit(Exit.BREAK);
		if (breakExit != null) {
			mergeBreakExit(breakExit);
		}

		String label = getOptionLabel();
		if (label != null) {
			Exit labeledBreakExit = getExit(Exit.getTag(Exit.BREAK, label));
			if (labeledBreakExit != null) {
				mergeBreakExit(labeledBreakExit);
			}
		}
	}

	/**
	 * Transfers the control dependencies from a given {@link Exit} to the done
	 * {@link Exit}, creating one if necessary. The given {@link Exit} and its
	 * peer are then removed.
	 */
	private void mergeBreakExit(Exit breakExit) {
		Exit doneExit = getExit(Exit.DONE);
		if (doneExit == null) {
			doneExit = makeExit(0, Exit.DONE);
		}

		OutBuf breakOutBuf = breakExit.getPeer();
		OutBuf doneOutBuf = doneExit.getPeer();
		Port donePort = doneOutBuf.getGoPort();

		for (Iterator iter = breakOutBuf.getEntries().iterator(); iter
				.hasNext();) {
			Entry breakEntry = (Entry) iter.next();
			Exit drivingExit = breakEntry.getDrivingExit();
			Entry doneEntry = doneOutBuf.makeEntry(drivingExit);
			doneEntry.addDependency(donePort,
					new ControlDependency(drivingExit.getDoneBus()));
		}

		breakOutBuf.disconnect();
		breakExit.disconnect();
		removeExit(breakExit);
	}
}
