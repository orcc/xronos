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

package net.sf.openforge.optimize.pipeline;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionInt;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.optimize.Optimization;
import net.sf.openforge.util.Debug;

/**
 * The Optimization entry to the pipelining functionality. The optimization
 * makes 2 passes over the design. The first pass does a traversal to determine
 * several things:
 * <ul>
 * <li>Maximum combinational path (max gate depth)
 * <li>Maximum 'unbreakable' depth
 * <li>Estimated number of registers that would be inserted
 * </ul>
 * Based on these criteria and the values of the options:
 * <ul>
 * <li>{@link OptionRegistry#SCHEDULE_PIPELINE_ENABLE} - turns pipelining on or
 * off
 * <li>{@link OptionRegistry#SCHEDULE_PIPELINE_AUTO_LEVEL} - pipelining will be
 * done with a global target level of maxdepth / apl level
 * <li>{@link OptionRegistry#SCHEDULE_PIPELINE_GATE_DEPTH} - pipelining will be
 * done with a global level of the specified depth
 * </ul>
 * 
 * <p>
 * July 05, 2006. The pipelining engine has been modified to allow different
 * target gate depth at each Module scope. If the gate depth is specified for a
 * particular scope, then it will apply to that scope and ALL logic within that
 * scope unless re-set at one or more of the contained scopes.
 * 
 * @author cschanck
 * @version $Id: PipelineOptimization.java 198 2006-07-06 13:12:51Z imiller $
 */
public class PipelineOptimization implements Optimization, _pipeline.Debug {

	int pipelineCount = 0;

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		if (_d) {
			_dbg.ln("Calculating gate depth prior to pipelining -- pre-scheduling");
		}

		GenericJob gj = EngineThread.getGenericJob();
		Design design = (Design) target;
		PipelineEngine pipelineEngine = new PipelineEngine();
		// This first call is run solely as an analysis. The 1
		// argument pipeline method will not actually modify the
		// design in any way.
		pipelineEngine.pipeline(design);
		final int predictedRegCount = pipelineEngine.getPipelineCount();
		gj.info("Maximum combinational complexity: " + design.getMaxGateDepth());
		gj.info("Maximum unbreakable combinational complexity: "
				+ design.getUnbreakableGateDepth());
		if (_d) {
			Debug.depGraphTo(design, "Pipelining", "pipe-before.dot",
					Debug.GR_DEFAULT);
		}

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.SCHEDULE_PIPELINE_ENABLE)) {
			Option option = gj
					.getOption(OptionRegistry.SCHEDULE_PIPELINE_AUTO_LEVEL);
			final int auto_level = ((OptionInt) option)
					.getValueAsInt(CodeLabel.UNSCOPED);
			option = gj.getOption(OptionRegistry.SCHEDULE_PIPELINE_GATE_DEPTH);
			final int spec_level = ((OptionInt) option)
					.getValueAsInt(CodeLabel.UNSCOPED);

			if (spec_level > 0) {
				final String gateDepthOpt = OptionRegistry.SCHEDULE_PIPELINE_GATE_DEPTH
						.getHelpFormattedKeyList();
				// gj.warn("Use of a deprecated option '" + gateDepthOpt +
				// "' detected.");
				if (auto_level > 0) {
					gj.warn("\tThe auto pipelining level specification will overide the value supplied by '"
							+ gateDepthOpt + "'");
				}
			}

			int targetGateDepth = -1;

			if (_d) {
				_dbg.ln("-->level " + auto_level);
			}
			if (auto_level >= 1) {
				targetGateDepth = design.getMaxGateDepth() / auto_level;
				if (design.getMaxGateDepth() % auto_level != 0) {
					targetGateDepth += 1;
				}
				gj.info("Auto pipeline level: " + auto_level);
			} else if (spec_level > 0) {
				targetGateDepth = spec_level;
			}

			// In the absence of a global setting for the design,
			// there may still be specific pipeline targets for
			// sub-scopes. In this case the predicted count will
			// indicate that some pipelining should be done.
			if ((targetGateDepth > 0) || (predictedRegCount > 0)) {
				if (targetGateDepth > 0) {
					gj.info("Target post-pipelining combinational complexity: "
							+ targetGateDepth);
				} else {
					gj.info("Expected register insertion count: "
							+ predictedRegCount);
				}
				pipelineEngine.pipeline(design, targetGateDepth);
				pipelineCount = pipelineEngine.getPipelineCount();
				gj.info("Added " + pipelineCount + " additional registers");

				if (_d) {
					Debug.depGraphTo(design, "Pipelining", "pipe-after.dot",
							Debug.GR_DEFAULT);
				}
			}
		}
		gj.dec();
	}

	public int getCount() {
		return pipelineCount;
	}

	/**
	 * Method called prior to performing the optimization, should use Job (info,
	 * verbose, etc) to report to the user what action is being performed.
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info("pipelining...");
		EngineThread.getGenericJob().inc();
	}

	/**
	 * Method called after performing the optimization, should use Job (info,
	 * verbose, etc) to report to the user the results (if any) of running the
	 * optimization
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().dec();
	}

	/**
	 * Should return true if the optimization modified the LIM <b>and</b> that
	 * other optimizations in its grouping should be re-run
	 */
	@Override
	public boolean didModify() {
		return pipelineCount > 0;
	}

	/**
	 * The clear method is called after each complete visit to the optimization
	 * and should free up as much memory as possible, and reset any per run
	 * status gathering.
	 */
	@Override
	public void clear() {
		pipelineCount = 0;
	}

}
