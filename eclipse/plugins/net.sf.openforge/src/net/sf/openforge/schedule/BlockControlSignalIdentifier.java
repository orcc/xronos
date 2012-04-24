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

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;

/**
 * BlockControlSignalIdentifier is a utility class that allows block
 * scheduling to identify the specific control signal that is to be
 * used for a particular component when that component is a stall
 * feedback signal.  This class assumes that in most cases the control
 * bus is derived from the DONE exit of a component.
 * <p>This class is used in generating control signals specifically
 * for block based scheduling.  These control signals are the 'stall'
 * signals, or reverse flow control for processing.  If the stalling
 * component has a GO signal, then we can use the generator of that GO
 * signal as the stall signal driver. Otherwise we must rely on the
 * control signal for the component itself (which is equivalent to its
 * DONE). <b>NOT TRUE</b>.  If the component is a block, then the
 * actual end point may be embedded in that block (or loop) and
 * executes later than the GO.  If this is the case then we will
 * 'release' the process too early.  We could optimize and use the GO
 * iff this comp is not a block.  This class provides that
 * optimization by the way that it selects the appropriate control
 * signal. 
 *
 *
 * <p>Created: Mon Nov  8 15:15:52 2004
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: BlockControlSignalIdentifier.java 88 2006-01-11 22:39:52Z imiller $
 */
public class BlockControlSignalIdentifier implements Visitor
{
    private static final String _RCS_ = "$Rev: 88 $";

    /** The identified signal*/
    private Bus signal;

    /** The latency tracker containing control signals for the owner
     * of the component being identified. */
    private LatencyTracker latencyTracker;
    
    /** No outside instances. */
    private BlockControlSignalIdentifier (LatencyTracker tracker)
    {
        this.latencyTracker = tracker;
    }

    /**
     * Returns the specific control Bus that should be used if the
     * specified {@link Visitable} node is a stall feedback point.
     *
     * @param vis a value of type 'Visitable'
     * @param tracker the LatencyTracker that contains the control
     * signals for the owning module of the specified visitable.
     * @return a non-null Bus
     */
    public static Bus getControlSignal (Visitable vis, LatencyTracker tracker)
    {
        BlockControlSignalIdentifier id = new BlockControlSignalIdentifier(tracker);
        vis.accept(id);
        assert id.signal != null;
        // Convert the identified bus into the legitimate scheduling
        // bus.
        final Bus controlBus = tracker.getControlBus(id.signal);
        assert controlBus != null : "No control bus for identified control signal";
        return controlBus;
    }
    
    /**
     * Captures the GO signal to the component.  If there is no
     * connection to the GO port, then the control signal is simply
     * obtained from the LatencyTracker.
     *
     * @param comp a non-null Component
     */
    private void captureGOSignal (Component comp)
    {
        if (comp.getGoPort().getBus() != null)
        {
            this.signal = comp.getGoPort().getBus();
        }
        else
        {
            this.signal = this.latencyTracker.getControlBus(comp);
        }
    }
    
    /**
     * Returns a bus that indicates that the component is complete, in
     * the case of multiple exits from a component, the done bus of
     * each exit is logically ORed to generate this 'complete'
     * signal. 
     *
     * @param comp a non-null Component
     */
    private void captureDONESignal (Component comp)
    {
        final Set doneBuses = new HashSet();
        for (Iterator iter = comp.getExits().iterator(); iter.hasNext();)
        {
            final Exit exit = (Exit)iter.next();
            if (exit.getTag().getType() == Exit.SIDEBAND)
                continue;
            doneBuses.add(this.latencyTracker.getControlBus(exit.getDoneBus()));
        }
        assert doneBuses.size() > 0;
        if (doneBuses.size() == 1)
        {
            this.signal = (Bus)doneBuses.iterator().next();
        }
        else
        {
            this.signal = this.latencyTracker.getOr(doneBuses, comp.getOwner()).getResultBus();
        }
    }

    private void failUnexpected (Visitable vis)
    {
        throw new UnexpectedVisitationException("Unexpected traversal of node " + vis);
    }

    /** The outbuf has no exits, thus we must use its GO.  */
    public void visit (OutBuf vis) { captureGOSignal(vis); }
    /** Each of these components is guaranteed to execute in one clock
     * cycle, thus by using the GO signal we are saving that one cycle
     * while still ensuring that there is no contention on the
     * resource. */
    public void visit (Reg vis) { captureGOSignal(vis); }
    public void visit (Latch vis) { captureGOSignal(vis); }
    public void visit (RegisterRead vis) { captureGOSignal(vis); }
    public void visit (RegisterWrite vis) { captureGOSignal(vis); }
    public void visit (MemoryRead vis) { captureGOSignal(vis); }
    public void visit (MemoryWrite vis) { captureGOSignal(vis); }

    /**
     * Most components will simply use their DONE signals.
     */
    public void visit (Call vis) { captureDONESignal(vis); }
    public void visit (Block vis) { captureDONESignal(vis); }
    public void visit (Loop vis) { captureDONESignal(vis); }
    public void visit (WhileBody vis) { captureDONESignal(vis); }
    public void visit (UntilBody vis) { captureDONESignal(vis); }
    public void visit (ForBody vis) { captureDONESignal(vis); }
    public void visit (AddOp vis) { captureDONESignal(vis); }
    public void visit (AndOp vis) { captureDONESignal(vis); }
    public void visit (CastOp vis) { captureDONESignal(vis); }
    public void visit (ComplementOp vis) { captureDONESignal(vis); }
    public void visit (ConditionalAndOp vis) { captureDONESignal(vis); }
    public void visit (ConditionalOrOp vis) { captureDONESignal(vis); }
    public void visit (Constant vis) { captureDONESignal(vis); }
    public void visit (DivideOp vis) { captureDONESignal(vis); }
    public void visit (EqualsOp vis) { captureDONESignal(vis); }
    public void visit (GreaterThanEqualToOp vis) { captureDONESignal(vis); }
    public void visit (GreaterThanOp vis) { captureDONESignal(vis); }
    public void visit (LeftShiftOp vis) { captureDONESignal(vis); }
    public void visit (LessThanEqualToOp vis) { captureDONESignal(vis); }
    public void visit (LessThanOp vis) { captureDONESignal(vis); }
    public void visit (LocationConstant vis) { captureDONESignal(vis); }
    public void visit (MinusOp vis) { captureDONESignal(vis); }
    public void visit (ModuloOp vis) { captureDONESignal(vis); }
    public void visit (MultiplyOp vis) { captureDONESignal(vis); }
    public void visit (NotEqualsOp vis) { captureDONESignal(vis); }
    public void visit (NotOp vis) { captureDONESignal(vis); }
    public void visit (OrOp vis) { captureDONESignal(vis); }
    public void visit (PlusOp vis) { captureDONESignal(vis); }
    public void visit (ReductionOrOp vis) { captureDONESignal(vis); }
    public void visit (RightShiftOp vis) { captureDONESignal(vis); }
    public void visit (RightShiftUnsignedOp vis) { captureDONESignal(vis); }
    public void visit (ShortcutIfElseOp vis) { captureDONESignal(vis); }
    public void visit (SubtractOp vis) { captureDONESignal(vis); }
    public void visit (NumericPromotionOp vis) { captureDONESignal(vis); }
    public void visit (XorOp vis) { captureDONESignal(vis); }
    public void visit (Branch vis) { captureDONESignal(vis); }
    public void visit (Decision vis) { captureDONESignal(vis); }
    public void visit (Switch vis) { captureDONESignal(vis); }
    public void visit (InBuf vis) { captureDONESignal(vis); }
    public void visit (Mux vis) { captureDONESignal(vis); }
    public void visit (EncodedMux vis) { captureDONESignal(vis); }
    public void visit (PriorityMux vis) { captureDONESignal(vis); }
    public void visit (And vis) { captureDONESignal(vis); }
    public void visit (Not vis) { captureDONESignal(vis); }
    public void visit (Or vis) { captureDONESignal(vis); }
    public void visit (Scoreboard vis) { captureDONESignal(vis); }
    public void visit (NoOp vis) { captureDONESignal(vis); }
    public void visit (HeapRead vis) { captureDONESignal(vis); }
    public void visit (ArrayRead vis) { captureDONESignal(vis); }
    public void visit (HeapWrite vis) { captureDONESignal(vis); }
    public void visit (ArrayWrite vis) { captureDONESignal(vis); }
    public void visit (AbsoluteMemoryRead vis) { captureDONESignal(vis); }
    public void visit (AbsoluteMemoryWrite vis) { captureDONESignal(vis); }
    public void visit (SRL16 vis) { captureDONESignal(vis); }
    public void visit (TaskCall vis) { captureDONESignal(vis); }
    public void visit (SimplePinAccess vis) { captureDONESignal(vis); }
    public void visit (SimplePinRead vis) { captureDONESignal(vis); }
    public void visit (SimplePinWrite vis) { captureDONESignal(vis); }
    public void visit (FifoAccess vis) { captureDONESignal(vis); }
    public void visit (FifoRead vis) { captureDONESignal(vis); }
    public void visit (FifoWrite vis) { captureDONESignal(vis); }

    /**
     * This is the stuff we do not expect to have to traverse
     */
    public void visit (Design vis) { failUnexpected(vis); }
    public void visit (Task vis) { failUnexpected(vis); }
    public void visit (IPCoreCall vis) { failUnexpected(vis); }
    public void visit (Procedure vis) { failUnexpected(vis); }
    public void visit (TimingOp vis) { failUnexpected(vis); }
    public void visit (RegisterGateway vis) { failUnexpected(vis); }
    public void visit (RegisterReferee vis) { failUnexpected(vis); }
    public void visit (MemoryReferee vis) { failUnexpected(vis); }
    public void visit (MemoryGateway vis) { failUnexpected(vis); }
    public void visit (MemoryBank vis) { failUnexpected(vis); }
    public void visit (Kicker vis) { failUnexpected(vis); }
    public void visit (PinRead vis) { failUnexpected(vis); }
    public void visit (PinWrite vis) { failUnexpected(vis); }
    public void visit (PinStateChange vis) { failUnexpected(vis); }
    public void visit (PinReferee vis) { failUnexpected(vis); }
    public void visit (TriBuf vis) { failUnexpected(vis); }
    public void visit (EndianSwapper vis) { failUnexpected(vis); }
    public void visit (SimplePin vis) { failUnexpected(vis); }

}// BlockControlSignalIdentifier
