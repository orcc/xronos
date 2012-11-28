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

package org.xronos.openforge.lim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.SimpleConstant;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.util.naming.ID;


/**
 * PinReferee is a {@link Referee} which controls all access to one {@link Pin}
 * from any number of {@link PinWrite}, {@link PinRead} or
 * {@link PinStateChange}.
 * 
 * @version $Id: PinReferee.java 23 2005-09-09 18:45:32Z imiller $
 */
public class PinReferee extends Referee implements Cloneable {

	private PinLogic logic;

	/**
	 * Creates a new PinReferee for the given {@link Pin}
	 */
	public PinReferee(Pin pin) {
		super(null);

		Port reset = getResetPort();
		reset.setUsed(true);

		Port clock = getClockPort();
		clock.setUsed(true);

		PinSlot pinSlot = new PinSlot(pin);
		logic = new PinLogic(pinSlot);
	}

	public void addWriteNow(PinAccess acc) {
		logic.addWriteNow(acc, new WriteSlot());
	}

	public void addWriteNext(PinAccess acc) {
		logic.addWriteNext(acc, new WriteSlot());
	}

	public void addDriveNow(PinAccess acc) {
		logic.addDriveNow(acc, new WriteSlot());
	}

	public void addDriveNext(PinAccess acc) {
		logic.addDriveNext(acc, new WriteSlot());
	}

	public void addRead(PinRead acc) {
		logic.addRead(acc, new ReadSlot(acc));
	}

	public void build() {
		logic.build();
	}

	public PinSlot getPinSlot(Pin p) {
		return logic.pinSlot;
	}

	public ReadSlot getReadSlot(PinRead pinRead) {
		return logic.reads.get(pinRead);
	}

	public WriteSlot getWriteNowSlot(PinAccess pinAccess) {
		return logic.writeNows.get(pinAccess);
	}

	public WriteSlot getWriteNextSlot(PinAccess pinAccess) {
		return logic.writeNexts.get(pinAccess);
	}

	public WriteSlot getDriveNowSlot(PinAccess pinAccess) {
		return logic.driveNows.get(pinAccess);
	}

	public WriteSlot getDriveNextSlot(PinAccess pinAccess) {
		return logic.driveNexts.get(pinAccess);
	}

	@Override
	public void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	public boolean removeDataBus(Bus bus) {
		assert false : "remove data bus not supported on " + this;
		return false;
	}

	@Override
	public boolean removeDataPort(Port port) {
		assert false : "remove data port not supported on " + this;
		return false;
	}

	/**
	 * Returns a complete copy of this PinReferee including the same number of
	 * 'write slots' or data/enable port pairs as this PinReferee.
	 * 
	 * @return a PinReferee Object.
	 * 
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * A PinSlot holds the collection of external Ports and Buses which get
	 * wired directly to a single Pin's InPinBuf and/or OutPinBuf.
	 */
	public class PinSlot {
		Pin pin;
		Exit exit;
		Port readData;
		Bus writeNowEnable;
		Bus writeNowData;
		Bus writeNextEnable;
		Bus writeNextData;
		Bus driveNowEnable;
		Bus driveNowState;
		Bus driveNextEnable;
		Bus driveNextState;

		private PinSlot(Pin p) {
			pin = p;
			exit = makeExit(0, Exit.SIDEBAND, ID.showLogical(p));

			// Only create the interfaces necessary for the type of pin.
			int width = p.getWidth();
			if (p.getInPinBuf() != null) {
				makeReadSide(width);
			}

			if (p.getOutPinBuf() != null) {
				makeWriteSide(width);
			}
		}

		private void makeWriteSide(int width) {
			writeNowEnable = exit.makeDataBus(Component.SIDEBAND);
			writeNowData = exit.makeDataBus(Component.SIDEBAND);
			writeNextEnable = exit.makeDataBus(Component.SIDEBAND);
			writeNextData = exit.makeDataBus(Component.SIDEBAND);
			driveNowEnable = exit.makeDataBus(Component.SIDEBAND);
			driveNowState = exit.makeDataBus(Component.SIDEBAND);
			driveNextEnable = exit.makeDataBus(Component.SIDEBAND);
			driveNextState = exit.makeDataBus(Component.SIDEBAND);
			// this.writeNowEnable.setSize(1, true);
			// this.writeNowData.setSize(width, true);
			// this.writeNextEnable.setSize(1, true);
			// this.writeNextData.setSize(width, true);
			// this.driveNowEnable.setSize(1, true);
			// this.driveNowState.setSize(1, true);
			// this.driveNextEnable.setSize(1, true);
			// this.driveNextState.setSize(1, true);
		}

		private void makeReadSide(int width) {
			readData = makeDataPort(Component.SIDEBAND);
			// this.readData.getPeer().setSize(width, true);
		}

		/**
		 * Returns true if this pin slot has populated the pin write (and drive)
		 * interface.
		 */
		public boolean containsWriteSide() {
			// If the writeNowEnable exists, all the write side exists.
			return writeNowEnable != null;
		}

		public Pin getPin() {
			return pin;
		}

		public Port getReadData() {
			return readData;
		}

		public Bus getWriteNowEnable() {
			return writeNowEnable;
		}

		public Bus getWriteNowData() {
			return writeNowData;
		}

		public Bus getWriteNextEnable() {
			return writeNextEnable;
		}

		public Bus getWriteNextData() {
			return writeNextData;
		}

		public Bus getDriveNowEnable() {
			return driveNowEnable;
		}

		public Bus getDriveNowState() {
			return driveNowState;
		}

		public Bus getDriveNextEnable() {
			return driveNextEnable;
		}

		public Bus getDriveNextState() {
			return driveNextState;
		}
	}

	/**
	 * A WriteSlot holds a collection of external Ports which can be used for
	 * any write operation (write now, write next, drive change), and which
	 * should be wired directly to the write access.
	 */
	public class WriteSlot {
		Port address;
		Port enable;
		Port data;

		public WriteSlot() {
			address = makeDataPort(Component.SIDEBAND);
			enable = makeDataPort(Component.SIDEBAND);
			data = makeDataPort(Component.SIDEBAND);
		}

		public Port getAddress() {
			return address;
		}

		public Port getEnable() {
			return enable;
		}

		public Port getData() {
			return data;
		}
	}

	/**
	 * A ReadSlot holds a collection of external Ports and Buses which can be
	 * used for a {@link PinRead} access).
	 */
	public class ReadSlot {
		Port address;
		Bus data;

		public ReadSlot(PinRead read) {
			address = makeDataPort(Component.SIDEBAND);
			Exit exit = makeExit(0, Exit.SIDEBAND, ID.showLogical(read) + "@"
					+ Integer.toHexString(read.hashCode()));
			data = exit.makeDataBus(Component.SIDEBAND);
		}

		public Port getAddress() {
			return address;
		}

		public Bus getData() {
			return data;
		}
	}

	/**
	 * PinLogic is all the logic necessary to manage multiple accesses to a
	 * given pin. This class ties together all the read/write slots and the pin
	 * slot associated with a given pin.
	 */
	public class PinLogic {

		/** A List of PinAccess -> WriteSlot. */
		private Map<PinAccess, WriteSlot> writeNows = new LinkedHashMap<PinAccess, WriteSlot>();

		/** A Map of PinAccess -> WriteSlot. */
		private Map<PinAccess, WriteSlot> writeNexts = new LinkedHashMap<PinAccess, WriteSlot>();

		/** A Map of PinAccess -> WriteSlot. */
		private Map<PinAccess, WriteSlot> driveNows = new LinkedHashMap<PinAccess, WriteSlot>();

		/** A Map of PinAccess -> WriteSlot. */
		private Map<PinAccess, WriteSlot> driveNexts = new LinkedHashMap<PinAccess, WriteSlot>();

		/** A Map of PinAccess -> ReadSlot. */
		private Map<PinAccess, ReadSlot> reads = new LinkedHashMap<PinAccess, ReadSlot>();

		/** The pin slot that is being accessed by this pinLogic. */
		private PinSlot pinSlot;

		public PinLogic(PinSlot slot) {
			pinSlot = slot;
		}

		public void addWriteNow(PinAccess access, WriteSlot write) {
			writeNows.put(access, write);
		}

		public void addWriteNext(PinAccess access, WriteSlot write) {
			writeNexts.put(access, write);
		}

		public void addDriveNow(PinAccess access, WriteSlot write) {
			driveNows.put(access, write);
		}

		public void addDriveNext(PinAccess access, WriteSlot write) {
			driveNexts.put(access, write);
		}

		public void addRead(PinAccess access, ReadSlot read) {
			reads.put(access, read);
		}

		/**
		 * Creates a PinWriteLogic for each function of the pin (writenow,
		 * writenext, drivenow, drivenext) and a readlogic for wiring the pin
		 * data to the consumers. When built each of these types of logic will
		 * make the necessary connections between the pinslot and read/write
		 * slot as well as adding the generated logic to this referee.
		 */

		public void build() {
			// Since the pin slot only creates ports/buses based on
			// the type of pin, we don't want to try to connect to the
			// write stuff unless it's an output/bidir pin.
			if (pinSlot.containsWriteSide()) {
				PinWriteLogic wnow = (writeNows.size() == 0) ? ((PinWriteLogic) new TieOffLogic(
						pinSlot.getPin().getWidth()))
						: ((PinWriteLogic) new MergeLogic(
								new ArrayList<WriteSlot>(writeNows.values()),
								pinSlot.pin.getWidth()));
				PinWriteLogic wnext = (writeNexts.size() == 0) ? ((PinWriteLogic) new TieOffLogic(
						pinSlot.getPin().getWidth()))
						: ((PinWriteLogic) new MergeLogic(
								new ArrayList<WriteSlot>(writeNexts.values()),
								pinSlot.pin.getWidth()));
				PinWriteLogic dnext = (driveNexts.size() == 0) ? ((PinWriteLogic) new TieOffLogic(
						1)) : ((PinWriteLogic) new MergeLogic(
						new ArrayList<WriteSlot>(driveNexts.values()),
						pinSlot.pin.getWidth()));

				// Drive now is special. If no-one accesses either
				// drive now or drive next, then set drive now on full
				// time.
				boolean alwaysOn = (driveNows.size() == 0
						&& driveNexts.size() == 0 && pinSlot.getPin()
						.isDriveOnReset());
				PinWriteLogic dnow = (driveNows.size() == 0) ? ((PinWriteLogic) new TieOffLogic(
						alwaysOn, 1)) : ((PinWriteLogic) new MergeLogic(
						new ArrayList<WriteSlot>(driveNows.values()),
						pinSlot.pin.getWidth()));
				pinSlot.getWriteNowEnable().getPeer()
						.setBus(wnow.getEnableBus());
				pinSlot.getWriteNowData().getPeer().setBus(wnow.getDataBus());
				pinSlot.getDriveNowEnable().getPeer()
						.setBus(dnow.getEnableBus());
				pinSlot.getDriveNowState().getPeer().setBus(dnow.getDataBus());
				pinSlot.getWriteNextEnable().getPeer()
						.setBus(wnext.getEnableBus());
				pinSlot.getWriteNextData().getPeer().setBus(wnext.getDataBus());
				pinSlot.getDriveNextEnable().getPeer()
						.setBus(dnext.getEnableBus());
				pinSlot.getDriveNextState().getPeer()
						.setBus(dnext.getDataBus());
			} else {
				getClockPort().setUsed(false);
				getResetPort().setUsed(false);
			}
			@SuppressWarnings("unused")
			PinReadLogic readLogic = new PinReadLogic(new ArrayList<ReadSlot>(
					reads.values()), pinSlot);
		}
	}

	/**
	 * implements the wire through connections of pin data (input to design) to
	 * each read slot.
	 */
	public class PinReadLogic {
		public PinReadLogic(List<ReadSlot> reads, PinSlot slot) {
			for (ReadSlot read : reads) {
				Bus readBus = slot.getReadData().getPeer();
				// read.getData().setSize(readBus.getSize(),
				// readBus.getValue().isSigned());
				read.getData().getPeer().setBus(readBus);
			}
		}
	}

	/**
	 * Generates a constant 0 to tie off all unused pin write/drive functions.
	 */
	public class TieOffLogic implements PinWriteLogic {
		private Constant constant;

		public TieOffLogic(int width) {
			this(false, width);
		}

		public TieOffLogic(boolean on, int width) {
			constant = new SimpleConstant(on ? 1 : 0, width, false);
			addComponent(constant);
		}

		@Override
		public Bus getEnableBus() {
			return constant.getValueBus();
		}

		@Override
		public Bus getDataBus() {
			return constant.getValueBus();
		}
	}

	/**
	 * Implements an or for all enables and a priority mux for all data for one
	 * write/drive function of a pin.
	 */
	public class MergeLogic implements PinWriteLogic {
		private Bus enableBus;
		private Bus dataBus;

		public MergeLogic(List<WriteSlot> writeSlots, int width) {
			if (writeSlots.size() > 1) {
				Or or = new Or(writeSlots.size());
				PriorityMux mux = new PriorityMux(writeSlots.size());

				enableBus = or.getResultBus();
				dataBus = mux.getResultBus();

				// mux.getResultBus().setSize(width, true);

				addComponent(or);
				addComponent(mux);

				for (Iterator<?> iter = writeSlots.iterator(), orIter = or
						.getDataPorts().iterator(), muxIter = mux
						.getSelectPorts().iterator(); iter.hasNext();) {
					WriteSlot slot = (WriteSlot) iter.next();
					Port orPort = (Port) orIter.next();
					Port muxGoPort = (Port) muxIter.next();
					Port muxDataPort = mux.getDataPort(muxGoPort);
					// slot.getData().getPeer().setSize(width, true);
					// slot.getEnable().getPeer().setSize(1, true);

					orPort.setBus(slot.getEnable().getPeer());
					muxGoPort.setBus(slot.getEnable().getPeer());
					muxDataPort.setBus(slot.getData().getPeer());
				}
			} else {
				WriteSlot writeSlot = writeSlots.get(0);
				dataBus = writeSlot.getData().getPeer();
				enableBus = writeSlot.getEnable().getPeer();
				// this.dataBus.setSize(width, true);
				// this.enableBus.setSize(1, true);
			}
		}

		@Override
		public Bus getEnableBus() {
			return enableBus;
		}

		@Override
		public Bus getDataBus() {
			return dataBus;
		}
	}

	public interface PinWriteLogic {
		public Bus getEnableBus();

		public Bus getDataBus();
	}
}
// class PinReferee
