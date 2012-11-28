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

package org.xronos.openforge.lim.io.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Referencer;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoID;
import org.xronos.openforge.lim.io.FifoIF;
import org.xronos.openforge.lim.io.FifoOutput;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimpleFifoPin;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.lim.io.SimplePinRead;
import org.xronos.openforge.lim.io.SimplePinWrite;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.SimpleConstant;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;


/**
 * ActorScalarOutput is a specialized fifo output interface which contains the
 * necessary infrastructure to support scalar data types. This includes:
 * <p>
 * <ul>
 * <li>Data output</li>
 * <li>Send output</li>
 * <li>Ack input</li>
 * <li>Ready input</li>
 * <li>count output</li>
 * </ul>
 * 
 * 
 * <p>
 * Created: Fri Aug 26 15:14:55 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ActorScalarOutput.java 280 2006-08-11 17:00:32Z imiller $
 */
public class ActorScalarOutput extends FifoOutput implements ActorPort {

	private final String baseName;
	private final SimplePin data;
	private final SimplePin send;
	private final SimplePin ack;
	private final SimplePin rdy;
	private final SimplePin tokenCount;

	// public ActorScalarOutput (String idString, int width)
	public ActorScalarOutput(FifoID fifoID) {
		super(fifoID.getBitWidth());

		baseName = fifoID.getName();
		final String pinBaseName = buildPortBaseName(baseName);

		data = new SimpleFifoPin(this, getWidth(), pinBaseName + "_DATA");
		send = new SimpleFifoPin(this, 1, pinBaseName + "_SEND");
		ack = new SimpleFifoPin(this, 1, pinBaseName + "_ACK");
		rdy = new SimpleFifoPin(this, 1, pinBaseName + "_RDY");
		tokenCount = new SimpleFifoPin(this, ActorPort.COUNT_PORT_WIDTH,
				pinBaseName + "_COUNT");

		addPin(data);
		addPin(send);
		addPin(ack);
		addPin(rdy);
		addPin(tokenCount);
	}

	/**
	 * <code>getType</code> returns {@link FifoIF#TYPE_ACTOR_QUEUE}
	 * 
	 * @return an <code>int</code> value
	 */
	@Override
	public int getType() {
		return FifoIF.TYPE_ACTOR_QUEUE;
	}

	@Override
	public String getPortBaseName() {
		return baseName;
	}

	/**
	 * ActorScalarOutput ports have no special naming requirements, this method
	 * returns portname
	 */
	@Override
	protected String buildPortBaseName(String portName) {
		return portName;
	}

	/**
	 * asserts false
	 */
	@Override
	public void setAttribute(int type, String value) {
		assert false : "No supported attributes";
	}

	/**
	 * Returns a subset of {@link #getPins} that are the output pins of the
	 * interface, containing only the data, write, and ctrl pins.
	 */
	@Override
	public Collection<SimplePin> getOutputPins() {
		List<SimplePin> list = new ArrayList<SimplePin>();
		list.add(data);
		list.add(send);
		list.add(tokenCount);

		return Collections.unmodifiableList(list);
	}

	/**
	 * Returns a {@link FifoWrite} object that is used to obtain data from this
	 * FifoIF.
	 * 
	 * @return a blocking {@link FifoAccess}
	 */
	@Override
	public FifoAccess getAccess() {
		return getAccess(true);
	}

	/**
	 * Returns a {@link FifoWrite} object that is used to obtain data from this
	 * FifoIF.
	 * 
	 * @param blocking
	 *            if set true returns a blocking fifo write otherwise a
	 *            non-blocking access.
	 * @return a {@link FifoAccess}
	 */
	@Override
	public FifoAccess getAccess(boolean blocking) {
		if (blocking) {
			return new ActorScalarOutputWrite(this);
		} else {
			return new ActorScalarSimpleOutputWrite(this);
		}
	}

	/** Returns the output data pin for this interface */
	@Override
	public SimplePin getDataPin() {
		return data;
	}

	/**
	 * Returns the output send pin, indicating that the interface is outputting
	 * valid data
	 */
	@Override
	public SimplePin getSendPin() {
		return send;
	}

	/**
	 * Returns the input acknowledge pin, indicating that the queue that the
	 * interface is sending to has acknowledged reciept of the data
	 */
	@Override
	public SimplePin getAckPin() {
		return ack;
	}

	/**
	 * Returns the input ready pin, indicating that the queue is ready to accept
	 * at least one token.
	 */
	@Override
	public SimplePin getReadyPin() {
		return rdy;
	}

	/**
	 * Unsupported on output interface
	 * 
	 * @throws UnsupportedOperationException
	 *             always
	 */
	@Override
	public Component getCountAccess() {
		throw new UnsupportedOperationException(
				"Output channels do not have token count facility");
	}

	@Override
	public Component getPeekAccess() {
		throw new UnsupportedOperationException(
				"Peeking at output interface not yet supported");
		// return new ActionTokenPeek(this);
	}

	@Override
	public Component getStatusAccess() {
		// throw new
		// UnsupportedOperationException("Status of output interface not yet supported");
		return new ActionPortStatus(this);
	}

	/**
	 * Tests the referencer types and then returns 1 or 0 depending on the types
	 * of each accessor.
	 * 
	 * @param from
	 *            the prior accessor in source document order.
	 * @param to
	 *            the latter accessor in source document order.
	 */
	@Override
	public int getSpacing(Referencer from, Referencer to) {
		// Options for accesses to an output are
		// FifoWrite (ActorScalarOutputWrite)
		// ActionPortStatus

		if (from instanceof FifoWrite) {
			return 1;
		} else if (from instanceof ActionPortStatus) {
			return 0;
		} else {
			throw new IllegalArgumentException("Source access to " + this
					+ " is of unknown type " + from.getClass());
		}
	}

	private class ActorScalarOutputWrite extends FifoWrite {
		private ActorScalarOutputWrite(ActorScalarOutput aso) {
			// super(aso, Latency.ZERO.open(new Object()));
			super(aso, null);
			final Exit exit = getExit(Exit.DONE);
			exit.setLatency(Latency.ZERO.open(exit));

			setProducesDone(true);
			setDoneSynchronous(true);
			// super(aso);
			// Because the write protocol is different from FSL we
			// need to fully populate the logic here.

			final Port data = getDataPorts().get(0);
			final Bus done = exit.getDoneBus();
			/*
			 * Build the following code: dataLatch = writeData(port) : en GO
			 * fifo_data = writeData (port); pending = (GO || GO'); send =
			 * pending GO' <= pending & !ack write_done = pending & ack
			 */
			// Needs RESET b/c it is in the control path
			final Reg flop = Reg.getConfigurableReg(Reg.REGR,
					"fifoWritePending");
			final Or pending = new Or(2);
			final And done_and = new And(2);
			final And flop_and = new And(2);
			final Not not = new Not();
			final CastOp doutCast = new CastOp(aso.getDataPin().getWidth(),
					false);
			final SimplePinWrite dout = new SimplePinWrite(aso.getDataPin());
			final SimplePinWrite send = new SimplePinWrite(aso.getSendPin());
			final SimplePinRead ack = new SimplePinRead(aso.getAckPin());
			final Latch capture = new Latch();

			// Give the flops an initial size.
			flop.getResultBus().pushValueForward(new Value(1, false));

			// Connect the clock ports
			flop.getClockPort().setBus(getClockPort().getPeer());
			flop.getResetPort().setBus(getResetPort().getPeer());
			flop.getInternalResetPort().setBus(getResetPort().getPeer());
			capture.getClockPort().setBus(getClockPort().getPeer());

			// add all the components
			addComponent(flop);
			addComponent(pending);
			addComponent(done_and);
			addComponent(flop_and);
			addComponent(not);
			addComponent(doutCast);
			addComponent(dout);
			addComponent(send);
			addComponent(ack);
			addComponent(capture);

			// Hook up data capture latch
			capture.getDataPort().setBus(data.getPeer());
			capture.getEnablePort().setBus(getGoPort().getPeer());

			// Hook fifo data through
			doutCast.getDataPort().setBus(capture.getResultBus());
			dout.getDataPort().setBus(doutCast.getResultBus());
			dout.getGoPort().setBus(done_and.getResultBus());

			// Calculate pending
			pending.getDataPorts().get(0).setBus(flop.getResultBus());
			pending.getDataPorts().get(1).setBus(getGoPort().getPeer());

			// calculate the pending term
			not.getDataPort().setBus(ack.getResultBus());
			flop_and.getDataPorts().get(0).setBus(pending.getResultBus());
			flop_and.getDataPorts().get(1).setBus(not.getResultBus());

			// Connect the flop input
			flop.getDataPort().setBus(flop_and.getResultBus());

			// Connect the fifoWR. It is set HIGH during the entire
			// pending write.
			send.getDataPort().setBus(pending.getResultBus());
			send.getGoPort().setBus(pending.getResultBus());

			// calculate the write complete term
			done_and.getDataPorts().get(0).setBus(pending.getResultBus());
			done_and.getDataPorts().get(1).setBus(ack.getResultBus());

			// Connect the done
			done.getPeer().setBus(done_and.getResultBus());

			// Define the feedback point
			// this.feedbackPoints = Collections.singleton(flop);
			addFeedbackPoint(flop);

			// Add a write of a constant 1 to the output tokenCount.
			final SimplePinWrite countWrite = new SimplePinWrite(aso.tokenCount);
			final Constant index1 = new SimpleConstant(1,
					aso.tokenCount.getWidth(), false);
			final Constant index1_1 = new SimpleConstant(1, 1, false);
			index1.pushValuesForward(); // ensures the bus has a value.
			index1_1.pushValuesForward(); // ensures the bus has a value.
			addComponent(index1);
			addComponent(index1_1);
			addComponent(countWrite);
			countWrite.getDataPort().setBus(index1.getValueBus());
			countWrite.getGoPort().setBus(index1_1.getValueBus()); // ALWAYS
																	// enabled
		}
	}

	private class ActorScalarSimpleOutputWrite extends FifoWrite {
		// This class assumes that the output write will always
		// succeed. So, set the data, strobe the send high and
		// finish.
		private ActorScalarSimpleOutputWrite(ActorScalarOutput aso) {
			// super(aso, Latency.ONE);
			// The spacing (handled by the getSpacing method) ensures
			// that we do not have a conflict on the resource.
			super(aso, Latency.ZERO);

			final SimplePinWrite dout = new SimplePinWrite(aso.getDataPin());
			final SimplePinWrite write = new SimplePinWrite(aso.getSendPin());
			addComponent(dout);
			addComponent(write);

			dout.getDataPort().setBus(getDataPorts().get(0).getPeer());
			dout.getGoPort().setBus(getGoPort().getPeer());

			write.getDataPort().setBus(getGoPort().getPeer());
			write.getGoPort().setBus(getGoPort().getPeer());

			// Add a write of a constant 1 to the output tokenCount.
			final SimplePinWrite countWrite = new SimplePinWrite(aso.tokenCount);
			final Constant index1 = new SimpleConstant(1,
					aso.tokenCount.getWidth(), false);
			final Constant index1_1 = new SimpleConstant(1, 1, false);
			index1.pushValuesForward(); // ensures the bus has a value.
			index1_1.pushValuesForward(); // ensures the bus has a value.
			addComponent(index1);
			addComponent(index1_1);
			addComponent(countWrite);
			countWrite.getDataPort().setBus(index1.getValueBus());
			countWrite.getGoPort().setBus(index1_1.getValueBus()); // ALWAYS
																	// enabled
		}
	}

}// ActorScalarOutput
