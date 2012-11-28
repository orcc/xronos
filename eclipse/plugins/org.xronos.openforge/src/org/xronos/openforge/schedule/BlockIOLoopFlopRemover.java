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

package org.xronos.openforge.schedule;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.MemoryAccessBlock;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;

/**
 * The idea of this visitor is to find any loop in the top level application
 * which contains exactly one memory access and one fifo access. If the memory
 * access or the fifo access has a latency of more than 0 cycles then the loop
 * flop is removable. This is because we know that in the structure of the block
 * IO loops the memory access and the fifo access are dependent on one another,
 * and that they will not execute in the same cycle. This means that for block
 * IO input the fifo read may be 0 cycles, but the memory write will take 1
 * cycle and will thus not occur in both the first and last cycles (which the
 * default analysis currently shows is 'possible'). Similarly for block IO
 * output, the memory read may be 0 cycles, but the fifo write will take at
 * least one cycle. This means that the memory access cannot occur in both the
 * first and last cycle.
 * <p>
 * This visitor will not descend into any calls thus it only analyzes the
 * wrapper level.
 * 
 * <p>
 * Created: Thu Apr 1 10:57:09 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: BlockIOLoopFlopRemover.java 20 2005-08-31 20:14:15Z imiller $
 */
public class BlockIOLoopFlopRemover extends DefaultVisitor {

	private static final boolean debug = false;

	/**
	 * A new tracker is created for each loop entered. Its job is to simply keep
	 * track of whether we have found a fifo access and a memory access.
	 */
	private Tracker tracker = null;

	/**
	 * Traverses the block IO module wrapper looking for any loops, and for each
	 * found loop marks that loop with whether or not the loop flop can be
	 * removed.
	 * 
	 * @param design
	 *            a non null Design
	 */
	public static void analyze(Design design) {
		if (!EngineThread.getGenericJob().getUnscopedBooleanOptionValue(
				OptionRegistry.NO_BLOCK_IO)) // If Do BlockIO
		{
			try {
				design.accept(new BlockIOLoopFlopRemover());
			} catch (UnexpectedStructureException e) {
				EngineThread.getGenericJob().warn(
						"Unexpected internal structure in block IO loop.  Recovered normally. "
								+ e);
			}
		}
	}

	/**
	 * Made private so that nobody can enter this analysis except via a Design.
	 */
	private BlockIOLoopFlopRemover() {
	}

	/**
	 * Jumps straight to the procedure so that we can override Call to do
	 * nothing. This gets us the top level call (ie the block IO wrapper and
	 * nothing else to analyze)
	 */
	@Override
	public void visit(Task vis) {
		if (vis.getCall() != null && vis.getCall().getProcedure() != null) {
			vis.getCall().getProcedure().accept(this);
		}
	}

	/**
	 * Creates a new Tracker, then begins processing the loops. After processing
	 * the loop the tracker is analyzed to determine if the loop flop can be
	 * removed.
	 */
	@Override
	public void visit(Loop vis) {
		if (tracker != null) {
			// throw new
			// UnexpectedStructureException("Nested loop in block IO loop");

			// Just skip the nested loop in a block io loop so this remover can
			// continue processing
			return;
		}

		/*
		 * Unrolled loop perhaps
		 */
		if (vis.getBody() == null || !vis.isIterative())
			return;

		/*
		 * Already marked as not needed, no need to analyze.
		 */
		if (!vis.getBody().isLoopFlopNeeded())
			return;

		tracker = new Tracker();
		super.visit(vis);

		if (tracker.isRemovable()) {
			vis.getBody().setLoopFlopNeeded(false);
			if (debug)
				System.out.println("Marked loop " + vis + " "
						+ vis.getBody().getBody().getComponents());
		}

		tracker = null;
	}

	/*
	 * These are the things that we are looking for so we mark them in the
	 * tracker for analysis later.
	 */
	/*
	 * Ignore memory read/write. They are always contained INSIDE a MemoryAccess
	 * public void visit (MemoryRead vis){ fail(vis); } public void visit
	 * (MemoryWrite vis){ fail(vis); }
	 */
	@Override
	public void visit(AbsoluteMemoryRead vis) {
		markMem(vis);
	}

	@Override
	public void visit(AbsoluteMemoryWrite vis) {
		markMem(vis);
	}

	@Override
	public void visit(ArrayRead vis) {
		markMem(vis);
	}

	@Override
	public void visit(ArrayWrite vis) {
		markMem(vis);
	}

	@Override
	public void visit(HeapWrite vis) {
		markMem(vis);
	}

	@Override
	public void visit(HeapRead vis) {
		markMem(vis);
	}

	@Override
	public void visit(FifoAccess vis) {
		markFifo(vis);
	}

	@Override
	public void visit(FifoRead vis) {
		markFifo(vis);
	}

	@Override
	public void visit(FifoWrite vis) {
		markFifo(vis);
	}

	/**
	 * Do nothing so that we do not descend into any calls.
	 */
	@Override
	public void visit(Call vis) {
	}

	/**
	 * The tracker will be null if we are outside of a loop. This can happen for
	 * the write to memory of any result value from the entry method.
	 */
	private void markMem(MemoryAccessBlock mab) {
		if (tracker != null)
			tracker.mark(mab);
	}

	private void markFifo(FifoAccess fa) {
		if (tracker != null)
			tracker.mark(fa);
	}

	/*
	 * public void visit (AddOp vis){ fail(vis); } public void visit (And vis){
	 * fail(vis); } public void visit (AndOp vis){ fail(vis); } public void
	 * visit (Block vis){ fail(vis); } public void visit (Branch vis){
	 * fail(vis); } public void visit (CastOp vis){ fail(vis); } public void
	 * visit (ComplementOp vis){ fail(vis); } public void visit
	 * (ConditionalAndOp vis){ fail(vis); } public void visit (ConditionalOrOp
	 * vis){ fail(vis); } public void visit (Constant vis){ fail(vis); } public
	 * void visit (Decision vis){ fail(vis); } public void visit (Design vis) {
	 * fail(vis); } public void visit (DivideOp vis){ fail(vis); } public void
	 * visit (EncodedMux vis){ fail(vis); } public void visit (EqualsOp vis){
	 * fail(vis); } public void visit (ForBody vis){ fail(vis); } public void
	 * visit (GreaterThanEqualToOp vis){ fail(vis); } public void visit
	 * (GreaterThanOp vis){ fail(vis); } public void visit (InBuf vis){
	 * fail(vis); } public void visit (IPCoreCall vis){ fail(vis); } public void
	 * visit (Kicker vis){ fail(vis); } public void visit (Latch vis){
	 * fail(vis); } public void visit (LeftShiftOp vis){ fail(vis); } public
	 * void visit (LessThanEqualToOp vis){ fail(vis); } public void visit
	 * (LessThanOp vis){ fail(vis); } public void visit (LocationConstant vis){
	 * fail(vis); } public void visit (MemoryBank vis){ fail(vis); } public void
	 * visit (MemoryGateway vis){ fail(vis); } public void visit (MemoryReferee
	 * vis){ fail(vis); } public void visit (MinusOp vis){ fail(vis); } public
	 * void visit (ModuloOp vis){ fail(vis); } public void visit (MultiplyOp
	 * vis){ fail(vis); } public void visit (Mux vis){ fail(vis); } public void
	 * visit (NoOp vis){ fail(vis); } public void visit (Not vis){ fail(vis); }
	 * public void visit (NotEqualsOp vis){ fail(vis); } public void visit
	 * (NotOp vis){ fail(vis); } public void visit (NumericPromotionOp vis){
	 * fail(vis); } public void visit (Or vis){ fail(vis); } public void visit
	 * (OrOp vis){ fail(vis); } public void visit (OutBuf vis){ fail(vis); }
	 * public void visit (PinRead vis){ fail(vis); } public void visit
	 * (PinReferee vis){ fail(vis); } public void visit (PinStateChange vis){
	 * fail(vis); } public void visit (PinWrite vis){ fail(vis); } public void
	 * visit (PlusOp vis){ fail(vis); } public void visit (PriorityMux vis){
	 * fail(vis); } public void visit (Procedure vis){ fail(vis); } public void
	 * visit (ReductionOrOp vis){ fail(vis); } public void visit (Reg vis){
	 * fail(vis); } public void visit (RegisterGateway vis){ fail(vis); } public
	 * void visit (RegisterRead vis){ fail(vis); } public void visit
	 * (RegisterReferee vis){ fail(vis); } public void visit (RegisterWrite
	 * vis){ fail(vis); } public void visit (RightShiftOp vis){ fail(vis); }
	 * public void visit (RightShiftUnsignedOp vis){ fail(vis); } public void
	 * visit (Scoreboard vis){ fail(vis); } public void visit (ShortcutIfElseOp
	 * vis){ fail(vis); } public void visit (SimplePinRead vis) { fail(vis); }
	 * public void visit (SimplePinWrite vis) { fail(vis); } public void visit
	 * (SRL16 vis){ fail(vis); } public void visit (SubtractOp vis){ fail(vis);
	 * } public void visit (Switch vis){ fail(vis); } public void visit
	 * (TimingOp vis){ fail(vis); } public void visit (TriBuf vis){ fail(vis); }
	 * public void visit (UntilBody vis){ fail(vis); } public void visit
	 * (WhileBody vis){ fail(vis); } public void visit (XorOp vis){ fail(vis); }
	 */

	/**
	 * A simple class to capture the memory access and the fifo access
	 * components that are expected in block IO loops. It also provides the
	 * analysis which determines if a loop conforms to the 'removable loop flop'
	 * criteria.
	 */
	class Tracker {
		private MemoryAccessBlock memAcc = null;
		private FifoAccess fifoAcc = null;

		public Tracker() {
		}

		public void mark(MemoryAccessBlock acc) {
			// if (this.memAcc != null)
			// throw new
			// UnexpectedStructureException("Multiple memory accesses in block IO loop");
			// this.memAcc = acc;

			// Set to null and let method isRemovable to catch
			if (memAcc != null)
				memAcc = null;
			else
				memAcc = acc;
		}

		public void mark(FifoAccess acc) {
			// if (this.fifoAcc != null)
			// throw new
			// UnexpectedStructureException("Multiple fifo accesses in block IO loop");
			// this.fifoAcc = acc;

			// Set to null and let method isRemovable to catch
			if (fifoAcc != null)
				fifoAcc = null;
			else
				fifoAcc = acc;

		}

		/**
		 * The loop flop is removable if we found both a fifo access and a
		 * memory access AND the memory access or the fifo access takes at least
		 * one cycle to complete.
		 */
		public boolean isRemovable() {
			if (memAcc == null || fifoAcc == null) {
				if (BlockIOLoopFlopRemover.debug)
					System.out
							.println("Wrong signature, not removable. memAcc "
									+ memAcc + " fifoAcc " + fifoAcc);
				return false;
			}

			if (fifoAcc.getLatency().isGT(Latency.ZERO)
					|| memAcc.getMemoryAccess().getLatency().isGT(Latency.ZERO)) {
				return true;
			}

			if (BlockIOLoopFlopRemover.debug)
				System.out.println("memAcc latency not GT zero "
						+ memAcc.getMemoryAccess().getLatency());
			return false;
		}
	}

	@SuppressWarnings("serial")
	class UnexpectedStructureException extends RuntimeException {
		public UnexpectedStructureException(String msg) {
			super(msg);
		}
	}

	/**
	 * <b>ONLY FOR UNIT TESTING DO NOT USE</b>
	 */
	static BlockIOLoopFlopRemover _getTestInstance() {
		return new BlockIOLoopFlopRemover();
	}

}// BlockIOLoopFlopRemover
