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

package net.sf.openforge.schedule.loop;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.DesignCopier;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.LoopBody;
import net.sf.openforge.lim.MatchingVisitor;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.schedule.ScheduleVisitor;

/**
 * The LoopCycleOptimizer improves the performance of the generated
 * implementation by eliminating extra cycles from the running of loop bodies.
 * This is done through multiple cooperative phases which transform a loop so
 * that ultimately the loop flop may be removed. These phases are:
 * <ul>
 * <li>LoopConditionalBranchBalancer - in this phase multiple parallel branches
 * of a loop are analyzed for combinational vs sequential operation. In the case
 * that there are parallel branches in which one is combinational (or may be)
 * and the other is sequential, then a resource dependency is put into the graph
 * to ensure that the combinational path is delayed by one cycle. This ensures
 * that there are no combinational paths through the loop and thus satisfies one
 * condition of loop flop removal.
 * <li>LoopResourceDependencyInsertion - in this phase accesses to shared
 * resources are analyzed. Accesses to any resource which is accessed in both
 * the first and last cycle of the loop body will have a resource dependency
 * added such that they are guaranteed to not execute in the same clock cycle
 * when the loop flop is removed.
 * </ul>
 * 
 * 
 * @author imiller
 * @version $Id: LoopCycleOptimizer.java 106 2006-02-15 21:39:37Z imiller $
 */
public class LoopCycleOptimizer {

	public static void optimize(Design design) {
		final GenericJob job = EngineThread.getEngine().getGenericJob();

		job.info("Optimizing Loop Implementation Structure");

		if (job.getUnscopedBooleanOptionValue(OptionRegistry.LOOP_BRANCH_BALANCE)) {
			design.accept(new LoopConditionalBranchBalancer());
		}

		final DesignCopier designCopier = new DesignCopier();
		final Design schedTestDesign = designCopier.copyLogicOnly(design);

		if (_loop.db)
			_loop.ln("Predictive scheduling of " + schedTestDesign);
		final ScheduleVisitor scheduler = ScheduleVisitor
				.schedule(schedTestDesign);
		if (_loop.db)
			_loop.ln("\tDone Predictive scheduling of " + schedTestDesign);

		// Analyze each loop body to find all resource conflicts that
		// cause the loop flop to be not removable.
		LoopBodyFinder bodyFinder = new LoopBodyFinder();
		schedTestDesign.accept(bodyFinder);

		// Turn the correlation map around so that we can look up the
		// original node based on the cloned node.
		Map reversed = new HashMap();
		for (Map.Entry entry : (Set<Map.Entry>) designCopier.getCorrelation()
				.entrySet()) {
			assert !reversed.containsKey(entry.getValue());
			reversed.put(entry.getValue(), entry.getKey());
		}

		if (_loop.db)
			_loop.ln("Analysing loop flop status of cloned design");
		for (LoopBody loopBody : (List<LoopBody>) bodyFinder.getMatchingNodes()) {
			Map<Object, Set<LoopFlopConflict>> conflictMap = LoopFlopAnalysis
					.analyzeLoopFlopStatus(loopBody,
							scheduler.getLatencyCache());

			if (_loop.db)
				_loop.ln("Conflicts for body " + loopBody + " are "
						+ conflictMap);

			final Set<LoopFlopConflict> allConflicts = new HashSet<LoopFlopConflict>();
			for (Set<LoopFlopConflict> conf : conflictMap.values()) {
				allConflicts.addAll(conf);
			}

			final LoopFlopConflictSet conflictSet = LoopFlopConflictSet
					.getConflictSet(allConflicts, reversed);

			// Determine if stallable conflict accesses exist in all paths
			// through the loop body. If so, then there is no need to
			// move the loop flop as the performance will not change
			// because all paths will be stalled.
			final BranchBodyFinder branchFinder = new BranchBodyFinder();
			loopBody.accept(branchFinder);

			Set<LoopBody> branchBodies = new HashSet<LoopBody>(
					branchFinder.getMatchingNodes());
			branchBodies
					.removeAll(conflictSet.getFixableAccessPathComponents());
			if (!branchBodies.isEmpty()) {
				conflictSet.resolveConflicts();
			}
		}
		if (_loop.db)
			_loop.ln("DONE Analysing loop flop status of cloned design");
	}

	private static class LoopBodyFinder extends MatchingVisitor {
		public LoopBodyFinder() {
			super(FIFO);
		}

		@Override
		public void visit(ForBody body) {
			super.visit(body);
			addMatchingNode(body);
		}

		@Override
		public void visit(UntilBody body) {
			super.visit(body);
			addMatchingNode(body);
		}

		@Override
		public void visit(WhileBody body) {
			super.visit(body);
			addMatchingNode(body);
		}
	}

	private static class BranchBodyFinder extends MatchingVisitor {
		public BranchBodyFinder() {
			super(FIFO);
		}

		@Override
		public void visit(Branch body) {
			super.visit(body);
			addMatchingNode(body.getTrueBranch());
			if (body.getFalseBranch() != null) {
				addMatchingNode(body.getFalseBranch());
			}
		}
	}

}
