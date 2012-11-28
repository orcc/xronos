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

package org.xronos.openforge.optimize;

import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.Option;
import org.xronos.openforge.app.project.OptionBoolean;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.Task;

/**
 * IODepthVisitor sets the gate depth of the top call's InBuf and OutBuf for
 * each task in order to characterize the delay they add to a design. This
 * allows Pipelining to insert a line of input and output registers for all
 * designs that use inferred I/O.
 * 
 * 
 * <p>
 * Created: Fri Mar 21 16:46:06 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: IODepthVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class IODepthVisitor extends DefaultVisitor {

	public static final int IOB_INPUT_DELAY = 10;
	public static final int IOB_OUTPUT_DELAY = 74;

	public IODepthVisitor() {
	}

	/**
	 * Sets the gate depth of the task call procedures InBuf and all OutBufs.
	 */
	@Override
	public void visit(Task task) {
		Option option;
		// If enabled, insert a NoOp on each data input to the top
		// level call.

		if (task.getCall() == null || task.getCall().getProcedure() == null
				|| task.getCall().getProcedure().getBody() == null) {
			return;
		}

		Block body = task.getCall().getProcedure().getBody();

		option = body.getInBuf().getGenericJob()
				.getOption(OptionRegistry.SCHEDULE_PIPELINE_NO_BOUNDRY_DEPTH);
		if (!((OptionBoolean) option).getValueAsBoolean(body.getInBuf()
				.getSearchLabel()))
			;
		{
			body.getInBuf().setGateDepth(InBuf.IOB_DEFAULT);
		}

		for (Exit exit : body.getExits()) {
			OutBuf buf = exit.getPeer();
			option = buf.getGenericJob().getOption(
					OptionRegistry.SCHEDULE_PIPELINE_NO_BOUNDRY_DEPTH);
			if (((OptionBoolean) option)
					.getValueAsBoolean(buf.getSearchLabel())) {
				continue;
			}
			buf.setGateDepth(OutBuf.IOB_DEFAULT);
		}
	}

}// IODepthVisitor
