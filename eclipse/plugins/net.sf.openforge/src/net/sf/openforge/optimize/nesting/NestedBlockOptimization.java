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

package net.sf.openforge.optimize.nesting;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.MatchingVisitor;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.optimize.Optimization;
import net.sf.openforge.optimize._optimize;
import net.sf.openforge.util.Debug;

/**
 * NestedBlockVisitor analyzes each loop in the LIM and determines if it is
 * unrollable, and if so, annotates is with the information necessary for
 * unrolling. Later the Loop.unroll() method can be called to unroll this loop
 * (ie: during optimization or scheduling)
 * 
 * @author cschanck
 * @version $Id: NestedBlockOptimization.java 100 2006-02-03 22:49:08Z imiller $
 */
public class NestedBlockOptimization implements Optimization {

	private NestedBlockVisitor vis = new NestedBlockVisitor();
	private UnNestingEngine engine = new UnNestingEngine();
	private int nestedCount = 0;
	private int passCount = 0;

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		if (isUnnestingEnabled()) {
			if (_optimize.db) {
				Debug.depGraphTo(target, "Block Unnesting", "bu-before"
						+ passCount + ".dot", Debug.GR_DEFAULT);
			}
			target.accept(vis);
			nestedCount = vis.getMatchingNodes().size();
			engine.unnest(vis.getMatchingNodes());
			if (_optimize.db) {
				Debug.depGraphTo(target, "Block Unnesting", "bu-after"
						+ passCount + ".dot", Debug.GR_DEFAULT);
			}
			passCount++;
		}
	}

	public int getCount() {
		return nestedCount;
	}

	public boolean isUnnestingEnabled() {
		return !EngineThread.getGenericJob().getUnscopedBooleanOptionValue(
				OptionRegistry.BLOCK_NOUNNESTING);
	}

	/**
	 * Method called prior to performing the optimization, should use Job (info,
	 * verbose, etc) to report to the user what action is being performed.
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info("block unnesting...");
	}

	/**
	 * Method called after performing the optimization, should use Job (info,
	 * verbose, etc) to report to the user the results (if any) of running the
	 * optimization
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().info(
				nestedCount + " block" + ((nestedCount != 1) ? "s" : "")
						+ " unnested...");
	}

	/**
	 * Should return true if the optimization modified the LIM <b>and</b> that
	 * other optimizations in its grouping should be re-run
	 */
	@Override
	public boolean didModify() {
		return nestedCount > 0;
	}

	/**
	 * The clear method is called after each complete visit to the optimization
	 * and should free up as much memory as possible, and reset any per run
	 * status gathering.
	 */
	@Override
	public void clear() {
		nestedCount = 0;
		vis.clear();
		engine.clear();
	}

	/**
	 * NestedBlockVisitor analyzes each loop in the LIM and determines if it is
	 * unrollable, and if so, annotates is with the information necessary for
	 * unrolling. Later the Loop.unroll() method can be called to unroll this
	 * loop (ie: during optimization or scheduling)
	 * 
	 * @author cschanck
	 * @version $Id: NestedBlockOptimization.java 100 2006-02-03 22:49:08Z
	 *          imiller $
	 */
	class NestedBlockVisitor extends MatchingVisitor {

		public NestedBlockVisitor() {
			super(FIFO);
		}

		// owner must be a true block, not a subclass of block ...
		@Override
		public void visit(Block b) {
			super.visit(b);
			if (!b.isMutexModule()) {
				if (b.getClass().equals(net.sf.openforge.lim.Block.class)) {
					if (b.getOwner() != null) {
						// only if it is nested in a block
						if (b.getOwner().getClass()
								.equals(net.sf.openforge.lim.Block.class)) {
							//
							// xxxx tbd extra case to skip for now
							//
							boolean two2one = false;
							for (OutBuf ob : b.getOutBufs()) {
								if (ob.getEntries().size() > 1) {
									two2one = true;
								}
							}
							if (!two2one) {
								addMatchingNode(b);
							}
						}
					}
				}
			}
		}
	}
}
