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

import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.IPCoreCall;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Kicker;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.PinRead;
import org.xronos.openforge.lim.PinReferee;
import org.xronos.openforge.lim.PinStateChange;
import org.xronos.openforge.lim.PinWrite;
import org.xronos.openforge.lim.PriorityMux;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.RegisterGateway;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterReferee;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.Scoreboard;
import org.xronos.openforge.lim.Switch;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.TriBuf;
import org.xronos.openforge.lim.UnexpectedVisitationException;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.lim.io.SimplePinAccess;
import org.xronos.openforge.lim.io.SimplePinRead;
import org.xronos.openforge.lim.io.SimplePinWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.EndianSwapper;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.lim.memory.MemoryGateway;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryReferee;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.ComplementOp;
import org.xronos.openforge.lim.op.ConditionalAndOp;
import org.xronos.openforge.lim.op.ConditionalOrOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.EqualsOp;
import org.xronos.openforge.lim.op.GreaterThanEqualToOp;
import org.xronos.openforge.lim.op.GreaterThanOp;
import org.xronos.openforge.lim.op.LeftShiftOp;
import org.xronos.openforge.lim.op.LessThanEqualToOp;
import org.xronos.openforge.lim.op.LessThanOp;
import org.xronos.openforge.lim.op.MinusOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.NoOp;
import org.xronos.openforge.lim.op.NotEqualsOp;
import org.xronos.openforge.lim.op.NotOp;
import org.xronos.openforge.lim.op.NumericPromotionOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.PlusOp;
import org.xronos.openforge.lim.op.ReductionOrOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.RightShiftUnsignedOp;
import org.xronos.openforge.lim.op.ShortcutIfElseOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.TimingOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Mux;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.lim.primitive.SRL16;


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
