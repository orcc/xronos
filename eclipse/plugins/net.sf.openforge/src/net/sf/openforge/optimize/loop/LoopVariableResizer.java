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
package net.sf.openforge.optimize.loop;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.util.BusResizer;
import net.sf.openforge.optimize.ComponentSwapVisitor;
import net.sf.openforge.optimize.Optimization;
import net.sf.openforge.util.SizedInteger;
import net.sf.openforge.util.naming.ID;

/**
 * @author gandhij
 * 
 *         This is an optimization to resize all the loop feedback variable
 *         registers. This type of resizing is not handled by constant
 *         propagation because it forms a loop in the LIM structure. The
 *         LoopVariableAnalysis object is used to emulate the loop and ranges of
 *         all the feedback registers are observed to decide on the size of the
 *         feedback variables.
 * 
 *         This optimization will not be done on feedback registers whose range
 *         cannot be determined statically. Also it is not done on unrolled
 *         loops.
 * 
 * 
 */
public class LoopVariableResizer extends ComponentSwapVisitor implements
		Optimization {

	/**
	 * Variable to keep track of LIM modification
	 */
	private boolean didModify = false;

	/**
	 * List of previously resized loops. We do not resize a loop twice.
	 */
	private static List<Loop> resizedLoops = new LinkedList<Loop>();

	/**
	 * Total number of variables resized by this optimization
	 */
	private int numResizedVariables = 0;

	/**
	 * Loop currently being resized
	 */
	private Loop loop;

	/**
	 * LoopVariableReisizer Constructor
	 */
	public LoopVariableResizer() {
		super();
	}

	/**
	 * Get the number of bits required to hold this {@link SizedInteger} value.
	 * For example if the value is 5, the number of bits required is 4 (one more
	 * than actually required). The extra bit is required so that sign extension
	 * can be done based on the msb.
	 * 
	 * @param value
	 *            {@link SizedInteger} SizedInteger value for which the size of
	 *            bits are to be calculated.
	 * 
	 * @return int size or the number of bits
	 * 
	 */
	public static int getSizeForValue(SizedInteger value) {
		int size = 1;
		long mask = 1;
		for (int i = 0; i < 64; i++) {
			SizedInteger maskVal = SizedInteger.valueOf(mask, value.getSize(),
					value.isSigned());
			if (maskVal.compareTo(value) >= 0) {
				return (size + 1); // This is your minimum required size
			}
			mask = mask << 1;
			mask |= 1;
			size++;
		}

		return size;
	}

	/**
	 * In this function we try to emulate/analyze the loop to find the ranges of
	 * the feedback variables so that we can resize them.
	 * 
	 * @param loop
	 *            the loop to be analyzed
	 * @return returns a HashMap containing (Reg feedbackregister, SizedInteger
	 *         max) pairs. The SizedInteger signifies the maximum value that the
	 *         feedback register will ever hold. If the SizedInteger is null, it
	 *         indicates that the max value could not be found statically. If
	 *         the return value is null, it means that the loop was not a
	 *         suitable candidate for analysis or the loop could not be
	 *         emulated.
	 */
	private Map<Reg, SizedInteger> analyzeLoop(Loop loop) {

		this.loop = loop;
		Map<Reg, SizedInteger> feedbackRegsters = null;

		LoopVariableAnalysis lva = new LoopVariableAnalysis(loop);
		feedbackRegsters = lva.analyzeLoop();

		return feedbackRegsters;
	}

	/**
	 * Resize the feedbackregisters
	 * 
	 * @param feedbackRegisters
	 *            HashMap containing (Reg feedbackregisters, SignedInteger max)
	 *            pairs. max should contain the maximum value that the feedback
	 *            register will ever hold or null. If null, the register will
	 *            not be resized.
	 */
	private void resizeRegisters(Map<Reg, SizedInteger> feedbackRegisters) {
		/*
		 * Resize the register. This is done by a transformation of the LIM by
		 * adding two cast ops. One to reduce the size to the reqested size and
		 * the other to cast it back to the original size. Two cast ops are
		 * required to keep the constant propagator happy. This effectively
		 * throws away the extra bits and causes the final logic to be pruned.
		 * To make scheduling easier, these components are actually inserted
		 * into the loopBody right before the outbuf.
		 */
		int loopRegistersResizedCount = 0;

		Iterator<Reg> iter = feedbackRegisters.keySet().iterator();
		while (iter.hasNext()) {
			Reg register = iter.next();
			SizedInteger value = feedbackRegisters.get(register);
			if (value != null) {
				int size = getSizeForValue(value);
				/* only resize to a smaller size */
				if (register.getDataPort().getSize() > size) {
					/* get the bus to be masked from the register */
					Dependency dep = register.getEntries().get(0)
							.getDependencies(register.getDataPort()).iterator()
							.next();
					Port obp = dep.getLogicalBus().getPeer();
					Dependency dep2 = obp.getOwner().getMainEntry()
							.getDependencies(obp).iterator().next();
					// Port exp = dep2.getLogicalBus().getPeer();
					Bus busToMask = dep2.getLogicalBus();

					/* do the actual resizing transformation */
					// Job.info("        Resizing register - " + register +
					// " to " + size + " bits");

					// Be aware of the Decision module, it always
					// generates a control signal from the And gate
					// for both true and false path, so if the loop
					// contains a Decision module, there is no need to
					// duplicate the control signal which has been
					// coporated in.
					// -- comment added CWU
					final boolean needControlDependency = (loop.getBody() != null)
							&& (loop.getBody().getDecision() == null);
					BusResizer busMask = new BusResizer(size,
							needControlDependency);
					busMask.resizeBus(busToMask);
					loopRegistersResizedCount++;
					didModify = true;
				}
			}
		}

		EngineThread.getGenericJob().verbose(
				" downsized " + loopRegistersResizedCount
						+ " variables in loop:" + ID.showLogical(loop));
		numResizedVariables += loopRegistersResizedCount;
	}

	/**
	 * Visit the loop. This fucntion looks for potentially resizable feedback
	 * registers and resizes them. The optimization involves two parts. Finding
	 * the feedback register's final size which involves emulating the loop and
	 * finding the max value that the register takes. Second part involves
	 * actually resizing the register.
	 */
	@Override
	public void visit(Loop loop) {
		didModify = false;

		/* we dont resize already analyzed loops */
		if (resizedLoops.contains(loop)) {
			// System.out.println("Already Resized !!!");
			return;
		} else {
			resizedLoops.add(loop);
		}

		if (loop != null) {
			if (loop.isIterative()) {

				// _optimize.d.graph(loop, "before loop resize", "/tmp/pre.dot",
				// Debug.GR_DEFAULT);

				// Job.info("       Analyzing feedback variables for "+ loop);

				// Analyze the loop for resizable feedback registers //
				Map<Reg, SizedInteger> feedbackRegisters = analyzeLoop(loop);

				// Do the actual resziging//
				if (feedbackRegisters != null) {
					// Job.info("        FeedbackRegister->RangeMax:" +
					// feedbackRegisters.toString());
					resizeRegisters(feedbackRegisters);
				} else {
					return;
				}

				// _optimize.d.graph(loop, "after loop resize", "/tmp/post.dot",
				// Debug.GR_DEFAULT);
			} else {
				EngineThread
						.getGenericJob()
						.info("Loop is not iterative - register resize not performed");
			}
		}
	}

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		target.accept(this);
	}

	/**
	 * Method called prior to performing the optimization, should use Job (info,
	 * verbose, etc) to report to the user what action is being performed.
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info("downsizing loop variables...");
	}

	/**
	 * Method called after performing the optimization, should use Job (info,
	 * verbose, etc) to report to the user the results (if any) of running the
	 * optimization
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().verbose(
				numResizedVariables + " loop variables were downsized");
	}

	/**
	 * Should return true if the optimization modified the LIM <b>and</b> that
	 * other optimizations in its grouping should be re-run
	 */
	@Override
	public boolean didModify() {
		return didModify;
	}

	/**
	 * The clear method is called after each complete visit to the optimization
	 * and should free up as much memory as possible, and reset any per run
	 * status gathering.
	 */
	@Override
	public void clear() {
		numResizedVariables = 0;
	}
}
