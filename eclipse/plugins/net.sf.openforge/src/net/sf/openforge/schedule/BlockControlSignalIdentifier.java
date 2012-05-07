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

package net.sf.openforge.schedule;

import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.RegisterGateway;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.SRL16;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.lim.UnexpectedVisitationException;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.EndianSwapper;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.lim.memory.MemoryGateway;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.memory.MemoryWrite;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ConditionalOrOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NotOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.PlusOp;
import net.sf.openforge.lim.op.ReductionOrOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.ShortcutIfElseOp;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.TimingOp;
import net.sf.openforge.lim.op.XorOp;

/**
 * BlockControlSignalIdentifier is a utility class that allows block scheduling
 * to identify the specific control signal that is to be used for a particular
 * component when that component is a stall feedback signal. This class assumes
 * that in most cases the control bus is derived from the DONE exit of a
 * component.
 * <p>
 * This class is used in generating control signals specifically for block based
 * scheduling. These control signals are the 'stall' signals, or reverse flow
 * control for processing. If the stalling component has a GO signal, then we
 * can use the generator of that GO signal as the stall signal driver. Otherwise
 * we must rely on the control signal for the component itself (which is
 * equivalent to its DONE). <b>NOT TRUE</b>. If the component is a block, then
 * the actual end point may be embedded in that block (or loop) and executes
 * later than the GO. If this is the case then we will 'release' the process too
 * early. We could optimize and use the GO iff this comp is not a block. This
 * class provides that optimization by the way that it selects the appropriate
 * control signal.
 * 
 * 
 * <p>
 * Created: Mon Nov 8 15:15:52 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: BlockControlSignalIdentifier.java 88 2006-01-11 22:39:52Z
 *          imiller $
 */
public class BlockControlSignalIdentifier implements Visitor {

	/** The identified signal */
	private Bus signal;

	/**
	 * The latency tracker containing control signals for the owner of the
	 * component being identified.
	 */
	private LatencyTracker latencyTracker;

	/** No outside instances. */
	private BlockControlSignalIdentifier(LatencyTracker tracker) {
		latencyTracker = tracker;
	}

	/**
	 * Returns the specific control Bus that should be used if the specified
	 * {@link Visitable} node is a stall feedback point.
	 * 
	 * @param vis
	 *            a value of type 'Visitable'
	 * @param tracker
	 *            the LatencyTracker that contains the control signals for the
	 *            owning module of the specified visitable.
	 * @return a non-null Bus
	 */
	public static Bus getControlSignal(Visitable vis, LatencyTracker tracker) {
		BlockControlSignalIdentifier id = new BlockControlSignalIdentifier(
				tracker);
		vis.accept(id);
		assert id.signal != null;
		// Convert the identified bus into the legitimate scheduling
		// bus.
		final Bus controlBus = tracker.getControlBus(id.signal);
		assert controlBus != null : "No control bus for identified control signal";
		return controlBus;
	}

	/**
	 * Captures the GO signal to the component. If there is no connection to the
	 * GO port, then the control signal is simply obtained from the
	 * LatencyTracker.
	 * 
	 * @param comp
	 *            a non-null Component
	 */
	private void captureGOSignal(Component comp) {
		if (comp.getGoPort().getBus() != null) {
			signal = comp.getGoPort().getBus();
		} else {
			signal = latencyTracker.getControlBus(comp);
		}
	}

	/**
	 * Returns a bus that indicates that the component is complete, in the case
	 * of multiple exits from a component, the done bus of each exit is
	 * logically ORed to generate this 'complete' signal.
	 * 
	 * @param comp
	 *            a non-null Component
	 */
	private void captureDONESignal(Component comp) {
		final Set<Bus> doneBuses = new HashSet<Bus>();
		for (Exit exit : comp.getExits()) {
			if (exit.getTag().getType() == Exit.SIDEBAND)
				continue;
			doneBuses.add(latencyTracker.getControlBus(exit.getDoneBus()));
		}
		assert doneBuses.size() > 0;
		if (doneBuses.size() == 1) {
			signal = doneBuses.iterator().next();
		} else {
			signal = latencyTracker.getOr(doneBuses, comp.getOwner())
					.getResultBus();
		}
	}

	private void failUnexpected(Visitable vis) {
		throw new UnexpectedVisitationException("Unexpected traversal of node "
				+ vis);
	}

	/** The outbuf has no exits, thus we must use its GO. */
	@Override
	public void visit(OutBuf vis) {
		captureGOSignal(vis);
	}

	/**
	 * Each of these components is guaranteed to execute in one clock cycle,
	 * thus by using the GO signal we are saving that one cycle while still
	 * ensuring that there is no contention on the resource.
	 */
	@Override
	public void visit(Reg vis) {
		captureGOSignal(vis);
	}

	@Override
	public void visit(Latch vis) {
		captureGOSignal(vis);
	}

	@Override
	public void visit(RegisterRead vis) {
		captureGOSignal(vis);
	}

	@Override
	public void visit(RegisterWrite vis) {
		captureGOSignal(vis);
	}

	@Override
	public void visit(MemoryRead vis) {
		captureGOSignal(vis);
	}

	@Override
	public void visit(MemoryWrite vis) {
		captureGOSignal(vis);
	}

	/**
	 * Most components will simply use their DONE signals.
	 */
	@Override
	public void visit(Call vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Block vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Loop vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(WhileBody vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(UntilBody vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ForBody vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(AddOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(AndOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(CastOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ComplementOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ConditionalAndOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ConditionalOrOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Constant vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(DivideOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(EqualsOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(GreaterThanEqualToOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(GreaterThanOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(LeftShiftOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(LessThanEqualToOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(LessThanOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(LocationConstant vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(MinusOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ModuloOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(MultiplyOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(NotEqualsOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(NotOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(OrOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(PlusOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ReductionOrOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(RightShiftOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(RightShiftUnsignedOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ShortcutIfElseOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(SubtractOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(NumericPromotionOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(XorOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Branch vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Decision vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Switch vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(InBuf vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Mux vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(EncodedMux vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(PriorityMux vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(And vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Not vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Or vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(Scoreboard vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(NoOp vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(HeapRead vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ArrayRead vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(HeapWrite vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(ArrayWrite vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(AbsoluteMemoryRead vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(AbsoluteMemoryWrite vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(SRL16 vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(TaskCall vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(SimplePinAccess vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(SimplePinRead vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(SimplePinWrite vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(FifoAccess vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(FifoRead vis) {
		captureDONESignal(vis);
	}

	@Override
	public void visit(FifoWrite vis) {
		captureDONESignal(vis);
	}

	/**
	 * This is the stuff we do not expect to have to traverse
	 */
	@Override
	public void visit(Design vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(Task vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(IPCoreCall vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(Procedure vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(TimingOp vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(RegisterGateway vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(RegisterReferee vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(MemoryReferee vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(MemoryGateway vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(MemoryBank vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(Kicker vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(PinRead vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(PinWrite vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(PinStateChange vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(PinReferee vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(TriBuf vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(EndianSwapper vis) {
		failUnexpected(vis);
	}

	@Override
	public void visit(SimplePin vis) {
		failUnexpected(vis);
	}

}// BlockControlSignalIdentifier
