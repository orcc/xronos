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

package org.xronos.openforge.lim.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.lim.Arbitratable;
import org.xronos.openforge.lim.Bit;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Referee;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.EqualsOp;
import org.xronos.openforge.lim.op.SimpleConstant;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.schedule.GlobalConnector;
import org.xronos.openforge.util.naming.ID;

/**
 * MemoryReferee is a {@link Module} which controls access to a
 * {@link MemoryPort}.
 * 
 * when multiple {@link Task tasks} attempt to access the port, the go(s) from
 * the task (local) side are propagated to the MemoryPort (global) side (and the
 * global to local done) as follows:
 * <p>
 * <img src="../doc-files/MemoryReferee-GlobalMux.png">
 * <p>
 * where the task information is registered according to:
 * <p>
 * <img src="../doc-files/MemoryReferee-TaskCapture.png">
 * <p>
 * and the state is updated according to:
 * <p>
 * <img src="../doc-files/MemoryReferee-StateMachine.png">
 * <p>
 * 
 * @author Jim Jensen based on design by Ian Miller and Jonathan Harris
 * 
 * @version $Id: MemoryReferee.java 280 2006-08-11 17:00:32Z imiller $
 */
public class MemoryReferee extends Referee {

	/**
	 * a DoneFilter encapsulates the hardware to filter the global done signal
	 * to the correct TaskCapture
	 */
	class DoneFilter {
		And and;

		/**
		 * build the hardware
		 * 
		 * @param globalDoneBus
		 * @param delayStateBus
		 * @param stage
		 *            int describing the current stage (0...(N-1))
		 * @param stateWidth
		 *            how many bits to represent state
		 */
		DoneFilter(Bus globalDoneBus, Bus delayStateBus, int stage,
				int stateWidth) {
			Constant constant = new SimpleConstant(stage, stateWidth, false);
			addComponent(constant);
			constant.setIDLogical(this, "DFconst_" + stage);

			EqualsOp equalsOp = new EqualsOp();
			addComponent(equalsOp);
			equalsOp.setIDLogical(this, "DFequalsOp+" + stage);

			and = new And(2);
			addComponent(and);
			and.setIDLogical(this, "DFand_" + stage);

			equalsOp.getDataPorts().get(0).setBus(constant.getValueBus());
			equalsOp.getDataPorts().get(1).setBus(delayStateBus);

			and.getDataPorts().get(0).setBus(equalsOp.getResultBus());
			and.getDataPorts().get(1).setBus(globalDoneBus);
		}

		Bus getResultBus() {
			return and.getResultBus();
		}
	}

	public class GlobalSlot {
		private Port done;
		private Port readData;
		private Bus writeData;
		private Bus address;
		private Bus writeEnable;
		private Bus go;
		private Bus size;

		public GlobalSlot(MemoryReferee parent) {
			done = parent.makeDataPort();
			readData = parent.makeDataPort();

			Exit exit = parent.getExit(Exit.DONE);
			writeData = exit.makeDataBus();
			address = exit.makeDataBus();
			writeEnable = exit.makeDataBus();
			go = exit.makeDataBus();
			size = exit.makeDataBus();
		}

		public Bus getAddressBus() {
			return address;
		}

		public Port getDonePort() {
			return done;
		}

		public Bus getGoBus() {
			return go;
		}

		public Port getReadDataPort() {
			return readData;
		}

		public Bus getSizeBus() {
			return size;
		}

		public Bus getWriteDataBus() {
			return writeData;
		}

		public Bus getWriteEnableBus() {
			return writeEnable;
		}

		public void setSizes(int dataWidth, int addrWidth, int sizeWidth) {
			done.setSize(1, false);
			readData.setSize(dataWidth, false);
			writeData.setSize(dataWidth, false);
			address.setSize(addrWidth, false);
			writeEnable.setSize(1, false);
			go.setSize(1, false);
			size.setSize(sizeWidth, false);
		}

		@Override
		public String toString() {
			String ret = super.toString();
			ret += " donep: " + getDonePort() + "/" + getDonePort().getPeer();
			ret += " rdp: " + getReadDataPort() + "/"
					+ getReadDataPort().getPeer();
			ret += " wdb: " + getWriteDataBus() + "/"
					+ getWriteDataBus().getPeer();
			ret += " addr: " + getAddressBus() + "/"
					+ getAddressBus().getPeer();
			ret += " wen: " + getWriteEnableBus() + "/"
					+ getWriteEnableBus().getPeer();
			ret += " go: " + getGoBus() + "/" + getGoBus().getPeer();
			ret += " size: " + getSizeBus() + "/" + getSizeBus().getPeer();
			return ret;
		}
	}

	/**
	 * StateMachine encapsulates the selection of the appropriate go signal to
	 * propagate to the global resource, in a fair (round-robbin) priority
	 * scheme. It is implemented as follows: <img
	 * src="doc-files/MemoryReferee-StateMachine.png"> where gcX is defined as
	 * the oputput of the Ors in the go capture module {@link MemoryReferee
	 * shown} here.
	 */
	class StateMachine {
		Or stateOr;

		AndOp stateAndOp;
		And stateAnd;

		EncodedMux stateMux;
		Reg stateReg;
		// list of 2 input muxes for the feedback ring
		List<EncodedMux> stateMuxList;
		Bus resetBus;
		Bus advanceStateBus;
		Not stateNot;
		Reg delayStateReg;
		int stateWidth;

		/**
		 * A Set of Components which represent the points of feedback in this
		 * statemachine
		 */
		Set<Component> feedbackPoints = new HashSet<Component>(7);

		/**
		 * create the state machine
		 * 
		 * @param reset
		 *            bus is the global reset
		 * @param advanceStateBus
		 *            is the advance state signal
		 */
		StateMachine(Bus clockBus, Bus resetBus, Bus advanceStateBus,
				int stateWidth) {
			assert resetBus != null;
			assert advanceStateBus != null;
			assert stateWidth > 0;

			this.stateWidth = stateWidth;

			stateReg = Reg.getConfigurableReg(Reg.REGRE, ID.showLogical(this)
					+ "stateReg");
			stateReg.getClockPort().setBus(getClockPort().getPeer());
			stateReg.getInternalResetPort().setBus(getResetPort().getPeer());
			feedbackPoints.add(stateReg);
			stateReg.getResultBus().setSize(stateWidth, false);

			delayStateReg = Reg.getConfigurableReg(Reg.REGR,
					ID.showLogical(this) + "delayStateReg");
			delayStateReg.getClockPort().setBus(getClockPort().getPeer());
			delayStateReg.getInternalResetPort().setBus(
					getResetPort().getPeer());

			addComponent(stateReg);
			addComponent(delayStateReg);

			this.resetBus = resetBus;
			this.advanceStateBus = advanceStateBus;

		}

		Bus getDelayStateBus() {
			return delayStateReg.getResultBus();
		}

		public Set<Component> getFeedbackPoints() {
			return feedbackPoints;
		}

		Bus getResultBus() {
			return stateReg.getResultBus();
		}

		/**
		 * instantiate its components and wire it up as shown above
		 * 
		 * @param taskCaptures
		 *            is the list of inputs to the state machine
		 */
		void setTaskCaptures(List<TaskCapture> taskCaptures) {
			int size = taskCaptures.size();
			stateNot = new Not();
			addComponent(stateNot);
			stateNot.setIDLogical(this, "stateNot");

			stateOr = new Or(size);
			addComponent(stateOr);
			stateOr.setIDLogical(this, "stateOr");

			stateAnd = new And(2);
			addComponent(stateAnd);
			stateAnd.setIDLogical(this, "stateAnd");

			stateAndOp = new AndOp();
			addComponent(stateAndOp);
			stateAndOp.setIDLogical(this, "stateAndOp");
			stateAndOp.getResultBus().setIDLogical("stateAndOp");

			stateMux = new EncodedMux(size);

			addComponent(stateMux);
			stateMux.setIDLogical(this, "stateMux");
			stateMuxList = new ArrayList<EncodedMux>(size);

			// stateAnd gets inverted global reset, stateOr and last mux (done
			// below)
			stateNot.getDataPort().setBus(resetBus);
			stateAnd.getDataPorts().get(0).setBus(stateNot.getResultBus());
			stateAnd.getDataPorts().get(1).setBus(stateOr.getResultBus());

			/*
			 * stateAnd only produces 1 bit, so it has to be extended to the
			 * state width. Make it signed so the bit will be replicated. Then
			 * cast it to be unsigned before plugging it into the stateAndOp.
			 */
			final CastOp stateAndSignChange = new CastOp(1, true);
			stateAndSignChange.getDataPort().setBus(stateAnd.getResultBus());
			addComponent(stateAndSignChange);

			final CastOp stateAndCast = new CastOp(stateWidth, false);
			stateAndCast.getDataPort()
					.setBus(stateAndSignChange.getResultBus());
			addComponent(stateAndCast);

			stateAndOp.getDataPorts().get(0)
					.setBus(stateAndCast.getResultBus());

			Bus mux0Input = stateAndOp.getResultBus();
			for (int i = 0; i < size; i++) {
				EncodedMux m = new EncodedMux(2);
				// m.setIDLogical("state inner mux "+i);
				addComponent(m);
				m.setIDLogical(this, "stateMux_" + i);
				stateMuxList.add(m);

				TaskCapture tc = taskCaptures.get(i);
				Bus tcResultBus = tc.getTaskMemGoBus();

				// first mux port is the output of the previous mux, or the
				// stateAnd
				Port port = m.getDataPort(0);
				port.setBus(mux0Input);

				Constant stateValue = new SimpleConstant(i, stateWidth, false);
				// stateValue.setIDLogical("state constant "+i);
				addComponent(stateValue);
				stateValue.setIDLogical(this, "stateValue_" + i);
				port = m.getDataPort(1);
				port.setBus(stateValue.getValueBus());

				port = m.getSelectPort();
				port.setBus(tcResultBus);

				// update the mux0Input to the output of this mux
				mux0Input = m.getResultBus();

				// add the output to the i+1 port of the statemux
				if (i < size - 1) {
					port = stateMux.getDataPort(i + 1);
					port.setBus(mux0Input);
				} else // this is the last mux, its output feeds the 0th port of
						// stateMux
				{
					port = stateMux.getDataPort(0);
					port.setBus(mux0Input);
					// and the last input to the stateAnd
					port = stateAndOp.getDataPorts().get(1);
					port.setBus(mux0Input);

					feedbackPoints.add(m);
					m.getResultBus().setSize(stateWidth, false);
				}

				port = stateOr.getDataPorts().get(i);
				port.setBus(tcResultBus);
			}

			// stateReg gets D from the state mux, enable from the global done
			Port port = stateReg.getDataPort();
			port.setBus(stateMux.getResultBus());

			port = stateReg.getEnablePort();
			port.setBus(advanceStateBus);

			// stateMux gets its select signal from the stateReg
			port = stateMux.getSelectPort();
			port.setBus(stateReg.getResultBus());

			// delayStateReg gets its output from stateReg
			delayStateReg.getDataPort().setBus(stateReg.getResultBus());
		}
	}

	/**
	 * TaskCapture encapsulates the hardware associated with a task that is part
	 * of an arbitrated referee (single task referee does not require this
	 * hardware
	 */
	class TaskCapture {
		// these are defined so the accessor methods will know where to point.
		// the rest are defined in the constructor
		Or readOr;
		Or writeOr;
		EncodedMux addrRegMux;
		EncodedMux sizeRegMux;
		EncodedMux dinMux;

		/**
		 * A Set of Components which represent the points of feedback in this
		 * taskCapture
		 */
		Set<Component> feedbackPoints = new HashSet<Component>(7);

		/**
		 * build the hardware for a task capture
		 * 
		 * @param stateWidth
		 *            number of bits required to represent the state
		 * @param depth
		 *            number of bits required to represent the address
		 * @param width
		 *            number of bits required to represent the data
		 * @param resetBus
		 *            global reset
		 * @param doneBus
		 *            bus providing the done (from done filter)
		 * @param dataOutBus
		 *            bus providing data (used by read)
		 * @param taskSlot
		 *            TaskSlot that corresponds to this task
		 * @param index
		 *            unique int for naming
		 * @param combinationalRead
		 *            true if memory read takes 0 clocks, therefore adjust the
		 *            logic to break the combinational feedback loop from go to
		 *            done of the memory.
		 */
		TaskCapture(int stateWidth, int depth, int width, Bus resetBus,
				Bus doneBus, Bus dataOutBus, TaskSlot ts, int index,
				boolean combinationalRead) {
			boolean readUsed = ts.getGoRPort() != null;
			boolean writeUsed = ts.getGoWPort() != null;

			Bus goRBus = readUsed ? ts.getGoRPort().getPeer() : null;
			Bus goWBus = writeUsed ? ts.getGoWPort().getPeer() : null;

			Bus addrBus = ts.getAddressPort().getPeer();
			Bus sizeBus = ts.getSizePort().getPeer();
			Bus dataInBus = writeUsed ? ts.getDataInPort().getPeer() : null;

			Bus clockBus = getClockPort().getPeer();
			Bus oneBus = getOneConstant().getValueBus();

			//
			// first instantiate the components and set the bus sizes
			//
			// go capture logic
			Reg readGoReg = null;

			// We always create the readAnd which turns off the memory
			// read enable we send to the memory when a done is
			// returned from the memory. We place it in one of two
			// locations based on the combinational read status of the
			// memory, since if it goes before the readMux as shown in
			// the documentation drawings, then we end up creating a
			// combinational feedback loop from the go through the
			// done of the memory. If the memory is combinational, we
			// simple move the readAnd to after the readMux so it only
			// clears the readGoReg and doesn't alter the current
			// state of the memory read enable signal, thereby
			// preventing a combinational feedback loop.
			And readAnd = null;
			Not readNot = null;
			EncodedMux readMux = null;

			if (readUsed) {
				readGoReg = Reg.getConfigurableReg(Reg.REGR,
						ID.showLogical(this) + "TCreadGoReg_" + index);
				addComponent(readGoReg);
				readGoReg.getInternalResetPort().setBus(resetBus);
				readGoReg.getClockPort().setBus(clockBus);
				feedbackPoints.add(readGoReg);
				readGoReg.getResultBus().setSize(1, false);

				readAnd = new And(2);
				addComponent(readAnd);
				readAnd.setIDLogical(this, "TCreadAnd_" + index);

				readNot = new Not();
				addComponent(readNot);
				readNot.setIDLogical(this, "TCreadNot_" + index);

				readMux = new EncodedMux(2);
				addComponent(readMux);
				readMux.setIDLogical(this, "TCreadMux_" + index);
			}

			if (!readUsed || !writeUsed) // only 2 inputs if we only read or
											// only write
			{
				readOr = new Or(2);
			} else {
				readOr = new Or(4);
			}
			addComponent(readOr);
			readOr.setIDLogical(this, "TCreadOr_" + index);

			Reg writeGoReg = null;
			And writeAnd = null;
			Not writeNot = null;
			EncodedMux writeMux = null;

			if (writeUsed) {
				writeGoReg = Reg.getConfigurableReg(Reg.REGR,
						ID.showLogical(this) + "TCwriteGoReg_" + index);
				addComponent(writeGoReg);
				writeGoReg.getInternalResetPort().setBus(resetBus);
				writeGoReg.getClockPort().setBus(clockBus);
				feedbackPoints.add(writeGoReg);
				writeGoReg.getResultBus().setSize(1, false);

				writeAnd = new And(2);
				addComponent(writeAnd);
				writeAnd.setIDLogical(this, "TCwriteAnd_" + index);

				writeNot = new Not();
				addComponent(writeNot);
				writeNot.setIDLogical(this, "TCwriteNot_" + index);

				writeMux = new EncodedMux(2);
				addComponent(writeMux);
				writeMux.setIDLogical(this, "TCwriteMux_" + index);

				writeOr = new Or(2);
				addComponent(writeOr);
				writeOr.setIDLogical(this, "TCwriteOr_" + index);
			}

			// addr capture logic (same as size capture logic)
			Or addrOr = null;
			if (readUsed && writeUsed)// only necessary if both reading and
										// writing
			{
				addrOr = new Or(2);
				addComponent(addrOr);
				addrOr.setIDLogical(this, "TCaddrOr_" + index);
			}

			Reg addrReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this)
					+ "TCaddrReg_" + index);
			addComponent(addrReg);
			addrReg.getInternalResetPort().setBus(resetBus);
			addrReg.getClockPort().setBus(clockBus);
			feedbackPoints.add(addrReg);
			addrReg.getResultBus().setSize(depth, false);

			addrRegMux = new EncodedMux(2);
			addComponent(addrRegMux);
			addrRegMux.setIDLogical(this, "TCaddrRegMux_" + index);

			// size capture logic (same as addr capture logic)
			Reg sizeReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this)
					+ "TCsizeReg_" + index);
			addComponent(sizeReg);
			// sizeReg.setIDLogical(this,"TCsizeReg_"+index);
			sizeReg.getInternalResetPort().setBus(resetBus);
			sizeReg.getClockPort().setBus(clockBus);
			feedbackPoints.add(sizeReg);
			sizeReg.getResultBus().setSize(LogicalMemory.SIZE_WIDTH, false);

			sizeRegMux = new EncodedMux(2);
			addComponent(sizeRegMux);
			sizeRegMux.setIDLogical(this, "TCsizeRegMux_" + index);

			// datain capture logic
			Reg dinReg = null;
			dinMux = null;
			if (writeUsed) {
				dinReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this)
						+ "TCdinReg_" + index);
				addComponent(dinReg);
				dinReg.getInternalResetPort().setBus(resetBus);
				dinReg.getClockPort().setBus(clockBus);
				dinReg.getResultBus().setSize(width, false);
				feedbackPoints.add(dinReg);

				dinMux = new EncodedMux(2);

				addComponent(dinMux);
				dinMux.setIDLogical(this, "TCdinMux_" + index);
			}

			//
			// now make the connections between components
			//
			// go capture
			ts.getDoneBus().getPeer().setBus(doneBus);
			if (readUsed) {
				readMux.getSelectPort().setBus(goRBus);
				readMux.getDataPort(1).setBus(oneBus);

				if (combinationalRead) {
					readMux.getDataPort(0).setBus(readGoReg.getResultBus());
					readGoReg.getDataPort().setBus(readAnd.getResultBus());
					readAnd.getDataPorts().get(0)
							.setBus(readMux.getResultBus());
				} else {
					readMux.getDataPort(0).setBus(readAnd.getResultBus());
					readGoReg.getDataPort().setBus(readMux.getResultBus());
					readAnd.getDataPorts().get(0)
							.setBus(readGoReg.getResultBus());
				}

				readAnd.getDataPorts().get(1).setBus(readNot.getResultBus());

				readNot.getDataPort().setBus(doneBus);
			}
			if (writeUsed) {
				writeMux.getSelectPort().setBus(goWBus);
				writeMux.getDataPort(1).setBus(oneBus);
				writeMux.getDataPort(0).setBus(writeAnd.getResultBus());

				writeGoReg.getDataPort().setBus(writeMux.getResultBus());

				writeAnd.getDataPorts().get(0)
						.setBus(writeGoReg.getResultBus());
				writeAnd.getDataPorts().get(1).setBus(writeNot.getResultBus());

				writeNot.getDataPort().setBus(doneBus);
			}
			// if both are used:
			if (readUsed && writeUsed) {
				if (combinationalRead) {
					readOr.getDataPorts().get(0)
							.setBus(readGoReg.getResultBus());
				} else {
					readOr.getDataPorts().get(0).setBus(readAnd.getResultBus());
				}

				readOr.getDataPorts().get(1).setBus(goRBus);
				readOr.getDataPorts().get(2).setBus(writeAnd.getResultBus());
				readOr.getDataPorts().get(3).setBus(goWBus);

				writeOr.getDataPorts().get(0).setBus(writeAnd.getResultBus());
				writeOr.getDataPorts().get(1).setBus(goWBus);
			} else if (readUsed) // read only - write not used!
			{
				if (combinationalRead) {
					readOr.getDataPorts().get(0)
							.setBus(readGoReg.getResultBus());
				} else {
					readOr.getDataPorts().get(0).setBus(readAnd.getResultBus());
				}

				readOr.getDataPorts().get(1).setBus(goRBus);
			} else // write only
			{
				readOr.getDataPorts().get(0).setBus(writeAnd.getResultBus());
				readOr.getDataPorts().get(1).setBus(goWBus);

				writeOr.getDataPorts().get(0).setBus(writeAnd.getResultBus());
				writeOr.getDataPorts().get(1).setBus(goWBus);
			}

			// addr capture
			Bus addrSelectBus;
			if (readUsed && writeUsed) {
				addrOr.getDataPorts().get(0).setBus(goRBus);
				addrOr.getDataPorts().get(1).setBus(goWBus);
				addrSelectBus = addrOr.getResultBus();
			} else if (readUsed)// read only
			{
				addrSelectBus = goRBus;
			} else // write only
			{
				addrSelectBus = goWBus;
			}
			addrRegMux.getSelectPort().setBus(addrSelectBus);
			addrRegMux.getDataPort(0).setBus(addrReg.getResultBus());
			addrRegMux.getDataPort(1).setBus(addrBus);
			addrReg.getDataPort().setBus(addrRegMux.getResultBus());
			sizeRegMux.getSelectPort().setBus(addrSelectBus);
			sizeRegMux.getDataPort(0).setBus(sizeReg.getResultBus());
			sizeRegMux.getDataPort(1).setBus(sizeBus);
			sizeReg.getDataPort().setBus(sizeRegMux.getResultBus());

			if (writeUsed) {
				// datain capture
				dinMux.getSelectPort().setBus(goWBus);
				dinMux.getDataPort(0).setBus(dinReg.getResultBus());
				dinMux.getDataPort(1).setBus(dataInBus);

				dinReg.getDataPort().setBus(dinMux.getResultBus());
			}
			// dataout link (memory --> task slot data, from read)
			if (readUsed) {
				ts.getDataOutBus().getPeer().setBus(dataOutBus);
			}
		}

		public Set<Component> getFeedbackPoints() {
			return feedbackPoints;
		}

		Bus getTaskMemAddrBus() {
			return addrRegMux.getResultBus();
		}

		Bus getTaskMemDataInBus() {
			if (dinMux == null) {
				// return getZeroConstant().getValueBus();
				return zeroDataConstant.getValueBus();
			}
			return dinMux.getResultBus();
		}

		Bus getTaskMemGoBus() {
			return readOr.getResultBus();
		}

		Bus getTaskMemSizeBus() {
			return sizeRegMux.getResultBus();
		}

		Bus getTaskMemWrBus() {
			if (writeOr == null) {
				return getZeroConstant().getValueBus();
			}
			return writeOr.getResultBus();
		}
	}

	/**
	 * a collection of ports and buses to which a task can connect. encapsulates
	 * both reading and writing. Note that this contains no hardware, it is just
	 * an interface. the TaskCapture contains the hardware.
	 */
	public class TaskSlot {
		/** read go */
		private Port goR = null;
		/** write go */
		private Port goW = null;
		/** address */
		private Port address;
		/** data in */
		private Port dataIn = null;
		/** size in */
		private Port size = null;
		/** data out */
		private Bus dataOut = null;
		/** done */
		private Bus done;
		/* is read used */
		@SuppressWarnings("unused")
		private boolean readUsed;
		/* is write used */
		@SuppressWarnings("unused")
		private boolean writeUsed;

		TaskSlot(MemoryReferee parent, int dataWidth, int addrWidth,
				boolean readUsed, boolean writeUsed) {
			this.readUsed = readUsed;
			this.writeUsed = writeUsed;

			Exit exit = parent.getExit(Exit.DONE);

			if (readUsed) {
				goR = parent.makeDataPort();
				dataOut = exit.makeDataBus();
			}
			if (writeUsed) {
				goW = parent.makeDataPort();
				dataIn = parent.makeDataPort();
			}

			address = parent.makeDataPort();
			size = parent.makeDataPort();

			done = exit.makeDataBus();
			setSizes(dataWidth, addrWidth, LogicalMemory.SIZE_WIDTH);
		}

		public Port getAddressPort() {
			return address;
		}

		public Port getDataInPort() {
			return dataIn;
		}

		public Bus getDataOutBus() {
			return dataOut;
		}

		public Bus getDoneBus() {
			return done;
		}

		public Port getGoRPort() {
			return goR;
		}

		public Port getGoWPort() {
			return goW;
		}

		public Port getSizePort() {
			return size;
		}

		private void setSizes(int dataWidth, int addrWidth, int sizeWidth) {
			if (goR != null) {
				goR.setSize(1, false);
				dataOut.setSize(dataWidth, false);
			}
			if (goW != null) {
				goW.setSize(1, false);
				dataIn.setSize(dataWidth, false);
			}
			address.setSize(addrWidth, false);
			size.setSize(sizeWidth, false);
			done.setSize(1, false);
		}

		@Override
		public String toString() {
			String ret = super.toString();
			ret += " erp: "
					+ getGoRPort()
					+ "/"
					+ (getGoRPort() == null ? "null" : getGoRPort().getPeer()
							.toString());
			ret += " ewp: "
					+ getGoWPort()
					+ "/"
					+ (getGoWPort() == null ? "null" : getGoWPort().getPeer()
							.toString());
			ret += " ap: "
					+ getAddressPort()
					+ "/"
					+ (getAddressPort() == null ? "null" : getAddressPort()
							.getPeer().toString());
			ret += " dp: "
					+ getDataInPort()
					+ "/"
					+ (getDataInPort() == null ? "null" : getDataInPort()
							.getPeer().toString());
			ret += " sp: "
					+ getSizePort()
					+ "/"
					+ (getSizePort() == null ? "null" : getSizePort().getPeer()
							.toString());
			ret += " db: "
					+ getDataOutBus()
					+ "/"
					+ (getDataOutBus() == null ? "null" : getDataOutBus()
							.getPeer().toString());
			ret += " doneb: "
					+ getDoneBus()
					+ "/"
					+ (getDoneBus() == null ? "null" : getDoneBus().getPeer()
							.toString());
			return ret;
		}
	}

	private List<TaskSlot> taskSlots = new ArrayList<TaskSlot>();

	private GlobalSlot globalSlot;

	/**
	 * A Set of Components which represent the points of feedback in this
	 * MemoryReferee, populated from the sets contained in TaskCapture and
	 * StateMachine
	 */
	Set<Component> feedbackPoints = new HashSet<Component>(11);

	Constant zeroConstant;

	Constant zeroDataConstant;

	Constant oneConstant;

	private int dataWidth = -1;

	protected MemoryReferee(Arbitratable resource) {
		super();
		dataWidth = resource.getDataPathWidth();
		@SuppressWarnings("unused")
		Exit mainExit = makeExit(0, Exit.DONE);

		// Make the global side interface
		globalSlot = new GlobalSlot(this);
	}

	/**
	 * create a referee to control access to the memory port
	 * 
	 * @param memoryPort
	 *            what the referee is controlling access to
	 * @param readList
	 *            list of reads - null if task doesn't read
	 * @param writeList
	 *            list of writes - null if task doesn't writes
	 */
	// public MemoryReferee (LogicalMemoryPort memoryPort, List readList, List
	// writeList)
	public MemoryReferee(Arbitratable resource,
			List<GlobalConnector.Connection> readList,
			List<GlobalConnector.Connection> writeList) {
		// super(memoryPort);
		this(resource);
		assert readList.size() == writeList.size() : "readList must match writeList size";

		int numTaskSlots = readList.size();

		final int memoryWidth = resource.getDataPathWidth();
		final int addressWidth = resource.getAddrPathWidth();
		final boolean isAddressable = resource.isAddressable();
		final boolean combinationalMemoryReads = resource
				.allowsCombinationalReads();
		globalSlot
				.setSizes(memoryWidth, addressWidth, LogicalMemory.SIZE_WIDTH);
		getClockPort().setUsed(true);
		getResetPort().setUsed(true);

		zeroConstant = new SimpleConstant(0, 1, false);
		zeroConstant.setIDLogical(this, "zeroConstant");
		addComponent(zeroConstant);

		zeroDataConstant = new SimpleConstant(0, memoryWidth, false);
		zeroDataConstant.setIDLogical(this, "zeroDataConstant");
		addComponent(zeroDataConstant);

		oneConstant = new SimpleConstant(1, 1, false);
		oneConstant.setIDLogical(this, "oneConstant");
		addComponent(oneConstant);

		int stateWidth = 0;
		// find number of bits required to hold state
		for (int i = numTaskSlots; i > 0; i = i >> 1) {
			stateWidth++;
		}

		for (int i = 0; i < numTaskSlots; i++) {
			boolean doesRead = readList.get(i) != null;
			boolean doesWrite = writeList.get(i) != null;
			TaskSlot ts = new TaskSlot(this, memoryWidth, addressWidth,
					doesRead, doesWrite);
			addTaskSlot(ts);
		}

		// if one accessor, then wire straight thru:
		// globalGo=goR | goW
		// globalAddr=addr
		// globalDataIn = dataIn
		// dataOut = globalDataOut
		// done = globalDone
		final int taskCount = getTaskSlots().size();
		if (taskCount == 1) {
			TaskSlot ts = getTaskSlots().get(0);

			boolean readUsed = ts.getGoRPort() != null;
			boolean writeUsed = ts.getGoWPort() != null;

			// first allocate the components & size
			Or goOr = null;
			if (readUsed && writeUsed) {
				goOr = new Or(2);
				addComponent(goOr);
				goOr.setIDLogical(this, "goOr");
				// now connect the components
				goOr.getDataPorts().get(0).setBus(ts.getGoRPort().getPeer());
				goOr.getDataPorts().get(1).setBus(ts.getGoWPort().getPeer());

				globalSlot.getGoBus().getPeer().setBus(goOr.getResultBus());
				globalSlot.getWriteEnableBus().getPeer()
						.setBus(ts.getGoWPort().getPeer());
			} else if (readUsed) // read only
			{
				globalSlot.getGoBus().getPeer()
						.setBus(ts.getGoRPort().getPeer());
				globalSlot.getWriteEnableBus().getPeer()
						.setBus(getZeroConstant().getValueBus());
			} else // write only. now why would a single task be write only?
			{
				globalSlot.getGoBus().getPeer()
						.setBus(ts.getGoWPort().getPeer());
				globalSlot.getWriteEnableBus().getPeer()
						.setBus(ts.getGoWPort().getPeer());
			}

			globalSlot.getAddressBus().getPeer()
					.setBus(ts.getAddressPort().getPeer());
			globalSlot.getSizeBus().getPeer()
					.setBus(ts.getSizePort().getPeer());

			if (writeUsed) {
				globalSlot.getWriteDataBus().getPeer()
						.setBus(ts.getDataInPort().getPeer());
			} else {
				// globalSlot.getWriteDataBus().getPeer().setBus(getZeroConstant().getValueBus());
				globalSlot.getWriteDataBus().getPeer()
						.setBus(zeroDataConstant.getValueBus());
			}
			if (readUsed) {
				ts.getDataOutBus().getPeer()
						.setBus(globalSlot.getReadDataPort().getPeer());
			}

			ts.getDoneBus().getPeer()
					.setBus(globalSlot.getDonePort().getPeer());

			return;
		}
		// else there are multiple tasks... so build the full, arbitrated
		// referee...
		EncodedMux goMux = new EncodedMux(taskCount);
		addComponent(goMux);
		goMux.setIDLogical(this, "goMux");

		EncodedMux addrMux = null;
		// if (memoryDepth > 1)
		if (isAddressable) {
			addrMux = new EncodedMux(taskCount);
			addComponent(addrMux);
			addrMux.setIDLogical(this, "addrMux");
		}

		EncodedMux sizeMux = new EncodedMux(taskCount);
		addComponent(sizeMux);
		sizeMux.setIDLogical(this, "sizeMux");

		EncodedMux dataInMux = new EncodedMux(taskCount);
		addComponent(dataInMux);
		dataInMux.setIDLogical(this, "dataInMux");

		EncodedMux writeEnableMux = new EncodedMux(taskCount);
		addComponent(writeEnableMux);
		writeEnableMux.setIDLogical(this, "writeEnableMux");

		Or advanceOr = new Or(taskCount);
		addComponent(advanceOr);
		advanceOr.setIDLogical(this, "advanceOr");

		// make the state machine
		StateMachine stateMachine = new StateMachine(getClockPort().getPeer(),
				getResetPort().getPeer(), advanceOr.getResultBus(), stateWidth);
		// and hook up the state to the mux select ports
		goMux.getSelectPort().setBus(stateMachine.getResultBus());
		if (addrMux != null) {
			addrMux.getSelectPort().setBus(stateMachine.getResultBus());
		}
		sizeMux.getSelectPort().setBus(stateMachine.getResultBus());
		dataInMux.getSelectPort().setBus(stateMachine.getResultBus());
		writeEnableMux.getSelectPort().setBus(stateMachine.getResultBus());

		// make the task captures, populate the global muxes/or
		// (goMux, advanceOr, addrMux, sizeMux dataInMux, writeEnableMux)
		List<TaskCapture> taskCaptures = new ArrayList<TaskCapture>();

		for (int i = 0; i < taskCount; i++) {
			DoneFilter df = new DoneFilter(globalSlot.getDonePort().getPeer(),
					stateMachine.getDelayStateBus(), i, stateWidth);

			TaskCapture tc = new TaskCapture(stateWidth, addressWidth,
					memoryWidth, getResetPort().getPeer(), df.getResultBus(),
					globalSlot.getReadDataPort().getPeer(), getTaskSlots().get(
							i), i, combinationalMemoryReads);
			taskCaptures.add(tc);
			feedbackPoints.addAll(tc.getFeedbackPoints());
			// hook up the task capture to the mux ports
			goMux.getDataPort(i).setBus(tc.getTaskMemGoBus());
			advanceOr.getDataPorts().get(i).setBus(tc.getTaskMemGoBus());
			if (addrMux != null) {
				addrMux.getDataPort(i).setBus(tc.getTaskMemAddrBus());
			}
			sizeMux.getDataPort(i).setBus(tc.getTaskMemSizeBus());

			/*
			 * Cast all data inputs to the memory width.
			 */
			final CastOp castOp = new CastOp(memoryWidth, false);
			addComponent(castOp);
			castOp.getDataPort().setBus(tc.getTaskMemDataInBus());
			dataInMux.getDataPort(i).setBus(castOp.getResultBus());

			writeEnableMux.getDataPort(i).setBus(tc.getTaskMemWrBus());
		}

		// update the state machine with the task captures
		stateMachine.setTaskCaptures(taskCaptures);
		feedbackPoints.addAll(stateMachine.getFeedbackPoints());

		// update the global slot with the mux outputs
		globalSlot.getGoBus().getPeer().setBus(goMux.getResultBus());
		if (addrMux != null) {
			globalSlot.getAddressBus().getPeer().setBus(addrMux.getResultBus());
		} else {
			Constant addrConst = new SimpleConstant(0, addressWidth, false);
			globalSlot.getAddressBus().getPeer()
					.setBus(addrConst.getValueBus());
		}
		globalSlot.getSizeBus().getPeer().setBus(sizeMux.getResultBus());
		globalSlot.getWriteDataBus().getPeer().setBus(dataInMux.getResultBus());
		globalSlot.getWriteEnableBus().getPeer()
				.setBus(writeEnableMux.getResultBus());
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	protected void addTaskSlot(TaskSlot slot) {
		taskSlots.add(slot);
	}

	public void connectImplementation(StructuralMemory.StructuralMemoryPort port) {
		GlobalSlot globalSlot = getGlobalSlot();
		port.getAddressPort().setBus(globalSlot.getAddressBus());

		if (port.isWrite()) {
			port.getDataInPort().setBus(globalSlot.getWriteDataBus());
			port.getWriteEnablePort().setBus(globalSlot.getWriteEnableBus());
		}

		if (port.isRead()) {
			port.getEnablePort().setBus(globalSlot.getGoBus());
			globalSlot.getReadDataPort().setBus(port.getDataOutBus());
		} else {
			/*
			 * The reason a Constant object is created here is because the read
			 * data port needs to be tied to something which is outside of
			 * MemoryReferee; Otherwise, it will cause the const prop looping
			 * infinitely.
			 */
			Constant zeroCon = new SimpleConstant(0, dataWidth, false);
			if (getOwner() != null) {
				getOwner().addComponent(zeroCon);
			}
			globalSlot.getReadDataPort().setBus(zeroCon.getValueBus());
		}

		port.getSizePort().setBus(globalSlot.getSizeBus());
		globalSlot.getDonePort().setBus(port.getDoneBus());
	}

	/**
	 * Returns a Set of {@link Component Components} that represent the feedback
	 * points in this Module. This set is populated from the components created
	 * by the TaskCapture and StateMachine classes.
	 * 
	 * @return a 'Set' of {@link Component Components}
	 */
	@Override
	public Set<Component> getFeedbackPoints() {
		Set<Component> feedback = new HashSet<Component>();
		feedback.addAll(super.getFeedbackPoints());
		feedback.addAll(feedbackPoints);
		return Collections.unmodifiableSet(feedback);
	}

	protected GlobalSlot getGlobalSlot() {
		return globalSlot;
	}

	private Constant getOneConstant() {
		return oneConstant;
	}

	public List<TaskSlot> getTaskSlots() {
		return Collections.unmodifiableList(taskSlots);
	}

	private Constant getZeroConstant() {
		return zeroConstant;
	}

	/**
	 * Tests whether this component is opaque. If true, then this component is
	 * to be treated as a self-contained entity. This means that its internal
	 * definition can make no direct references to external entitities. In
	 * particular, external {@link Bit Bits} are not pushed into this component
	 * during constant propagation, nor are any of its internal {@link Bit Bits}
	 * propagated to its external {@link Bus Buses}.
	 * <P>
	 * Typically this implies that the translator will either generate a
	 * primitive definition or an instantiatable module for this component.
	 * 
	 * @return true if this component is opaque, false otherwise
	 */
	@Override
	public boolean isOpaque() {
		return true;
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

	@Override
	public String show() {
		String ret = toString();
		for (Port port : getPorts()) {
			if (port == getGoPort()) {
				ret = ret + " go:" + port + "/" + port.getPeer();
			} else if (port == getClockPort()) {
				ret = ret + " ck:" + port + "/" + port.getPeer();
			} else if (port == getResetPort()) {
				ret = ret + " rs:" + port + "/" + port.getPeer() + "\n";
			} else if (port == globalSlot.getDonePort()) {
				ret = ret + " GL_done:" + port + "/" + port.getPeer();
			} else if (port == globalSlot.getReadDataPort()) {
				ret = ret + " GL_read:" + port + "/" + port.getPeer() + "\n";
			} else {
				String id = null;
				for (TaskSlot ts : taskSlots) {
					if (port == ts.getGoRPort()) {
						id = " task go R: " + port + "/" + port.getPeer();
					} else if (port == ts.getGoWPort()) {
						id = " task go W: " + port + "/" + port.getPeer();
					} else if (port == ts.getAddressPort()) {
						id = " task ad: " + port + "/" + port.getPeer();
					} else if (port == ts.getDataInPort()) {
						id = " task data in: " + port + "/" + port.getPeer()
								+ "\n";
					} else if (port == ts.getSizePort()) {
						id = " size in: " + port + "/" + port.getPeer() + "\n";
					}
				}

				if (id == null) {
					id = " p:" + port + "/" + port.getPeer();
				}
				ret += id;
			}
		}
		for (Exit exit : getExits()) {
			for (Bus bus : exit.getBuses()) {
				if (bus == exit.getDoneBus()) {
					ret = ret + " done:" + bus + "/" + bus.getPeer();
				} else if (bus == globalSlot.getWriteDataBus()) {
					ret = ret + " GS_wd_bus:" + bus + "/" + bus.getPeer();
				} else if (bus == globalSlot.getAddressBus()) {
					ret = ret + " GS_ad_bus:" + bus + "/" + bus.getPeer();
				} else if (bus == globalSlot.getWriteEnableBus()) {
					ret = ret + " GS_we_bus:" + bus + "/" + bus.getPeer();
				} else if (bus == globalSlot.getGoBus()) {
					ret = ret + " GS_go_bus:" + bus + "/" + bus.getPeer()
							+ "\n";
				} else if (bus == globalSlot.getSizeBus()) {
					ret = ret + " GS_size_bus:" + bus + "/" + bus.getPeer()
							+ "\n";
				} else {
					String id = null;
					for (TaskSlot ts : taskSlots) {
						if (bus == ts.getDataOutBus()) {
							id = " task data out bus:" + bus + "/"
									+ bus.getPeer();
						} else if (bus == ts.getDoneBus()) {
							id = " task done bus: " + bus + "/" + bus.getPeer();
						}
					}
					if (id == null) {
						id = " data:" + bus;
					}
					ret += id;
				}
			}
		}
		return ret;
	}
}
