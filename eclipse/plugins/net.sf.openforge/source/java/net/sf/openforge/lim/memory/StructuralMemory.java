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

package net.sf.openforge.lim.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.UnexpectedVisitationException;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.OrOpMulti;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.op.SubtractOp;

/**
 * StructuralMemory is a Module which contains a structural representation of a
 * given memory. The structure is built out of sufficient {@link MemoryBank
 * MemoryBanks} to cover the width of the memory. This class will instantiate
 * the necessary logic to manage data movement into and out of the memory banks
 * based on addresses. The byte lane enables are generated based on the address
 * and the size port. The bank width may be varied to any power of 8.
 * <p>
 * The enable port of a structural memory is the <b>read enable</b> port. This
 * differs from physical implementations which have an EN port that is used in
 * both read and write modes. This disparity is accounted for inside the
 * StructuralMemory by ORing the incoming enable and the write enable to
 * generate a memory enable.
 * <p>
 * <b>NOTE: This implementation assumes that the addressing scheme used in the
 * LIM (for all addresses to this memory) is based on the
 * {@link StructuralMemory#bankWidth}. Thus one address is allocated for each
 * 'bankWidth' bits in the memory, regardless of the memories accessible
 * width.</b>
 * 
 * 
 * <p>
 * Created: Wed Feb 12 12:56:50 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: StructuralMemory.java 536 2007-11-19 22:10:55Z imiller $
 */
public class StructuralMemory extends Module {

	/**
	 * The width of the Bus used to represent the 1-hot encoded bank selects.
	 */
	@SuppressWarnings("unused")
	private static final int SELECT_BITS = 9;

	/**
	 * The bit width of the memory implementation banks in this memory, {@see
	 * MemoryBuilder}. All banks in this memory will have the same bank width.
	 */
	private final int bankWidth;

	/**
	 * A Constant whose value is the number of ones that equals the value of
	 * bankWidth. E.g., if this.bankWidth is 4, then bankWidthConst is a
	 * constant representing binary "1111".
	 */
	private final Constant bankWidthConst;

	/** List of MemoryBank */
	private List<MemoryBank> banks = new ArrayList<MemoryBank>();

	/** A 1-bit Constant representing the value 0. */
	private Constant zero = new SimpleConstant(0, 1, false);
	/**
	 * A Constant representing 0, with the same width as the data path
	 */
	private Constant dataZero;

	/** Map of MemoryPort -> StructuralMemoryPort */
	private Map<LogicalMemoryPort, StructuralMemoryPort> memPorts = new LinkedHashMap<LogicalMemoryPort, StructuralMemoryPort>();

	/** Map of StructuralMemoryPort to List of MemoryBank.BankPort */
	private Map<StructuralMemoryPort, List<MemoryBank.BankPort>> portToBankPorts = new LinkedHashMap<StructuralMemoryPort, List<MemoryBank.BankPort>>();

	/**
	 * The number of lsb address bits that are to be used in selecting which
	 * elements (memory banks) of a memory line to access, thus those bits are
	 * not significant in moving from one memory line to the next.
	 */
	private int addrShiftBits = 0;

	/**
	 * The max number of stages by which the incoming/outgoing data will be
	 * shifted.
	 */
	private int DATA_SHIFT_STAGES;

	/** The width of this memory in bits. */
	private int dataWidth;

	/**
	 * Addressable locations per line multiplied by total number of lines
	 */
	private int totalAddressableLocations = 0;

	private int addrWidth = 0;

	private MemoryImplementation memImpl = null;

	/**
	 * Builds a structural memory whose Addressing policy is
	 * {@link AddressStridePolicy#BYTE_ADDRESSING}, thus each addressable
	 * location is a byte.
	 */
	public StructuralMemory(int memoryWidth, int memDepth, int bankWidth,
			List<LogicalMemoryPort> memoryPorts, String baseName,
			MemoryImplementation impl, int maxAddressWidth) {
		this(memoryWidth, memDepth, bankWidth, memoryPorts, baseName, impl,
				AddressStridePolicy.BYTE_ADDRESSING, maxAddressWidth);
	}

	/**
	 * Generate a structural memory based on the parameters.
	 * 
	 * @param memoryWidth
	 *            the number of bits wide a line of memory is.
	 * @param memDepth
	 *            the number of lines in this memory.
	 * @param bankWidth
	 *            the number of bits per memory bank.
	 * @param memoryPorts
	 *            a 'List' of MemoryPort objects, one per interface of the
	 *            memory.
	 * @param baseName
	 *            a 'String' used to name generated signals (prepended).
	 * @param impl
	 *            a value of type 'MemoryImplementation'
	 * @param addrPolicy
	 *            is the AddressStridePolicy in effect for this memory which
	 *            determines the number of bits per address.
	 */

	public StructuralMemory(int memoryWidth, int memDepth, int bankWidth,
			List<LogicalMemoryPort> memoryPorts, String baseName,
			MemoryImplementation impl, AddressStridePolicy addrPolicy,
			int maxAddressWidth) {
		super();

		final int bitsPerAddress = addrPolicy.getStride();
		//
		// Generate the constants used by this structural memory.
		//
		// assert ((bankWidth % 8 == 0) || (memoryWidth == bankWidth)):
		// "Memory Bank width must be power of 8, or width of memory";
		assert ((bankWidth % bitsPerAddress == 0) || (memoryWidth == bankWidth)) : "Memory Bank width must be power of 8, or width of memory";
		assert ((memoryWidth % bitsPerAddress) == 0) : "Memory width ("
				+ memoryWidth + ") must be multiple of addressable size ("
				+ bitsPerAddress + ")";

		this.bankWidth = bankWidth;
		addrShiftBits = calculateAddrOffsetBits(memoryWidth, bitsPerAddress);

		// In case the banks are larger than the memory width do the Math.min
		final int locationsPerLine = (int) Math.ceil(((double) memoryWidth)
				/ ((double) Math.min(bitsPerAddress, memoryWidth)));
		// Note that we are asserting here that there are 1, 2, 4, or
		// 8 addressable locations per line. This is to remain
		// compatible with the logic below for generating the select
		// bus and the data shifting logic. This logic creates
		// 'lane enables' and moves the input and output data buses to
		// the appropriate lanes, but is only designed to support 1,
		// 2, 4, or 8 location accesses.
		assert (locationsPerLine == 1) || (locationsPerLine == 2)
				|| (locationsPerLine == 4) || (locationsPerLine == 8) : "Structural memory only supports 1, 2, 4, or 8 addressable locations per line of memory";

		totalAddressableLocations = locationsPerLine * memDepth;

		addrWidth = maxAddressWidth;
		dataWidth = memoryWidth;
		memImpl = impl;

		assert this.bankWidth <= 64 : "Bank width is too large for constant mask";
		long bankMask = 0;
		for (int j = 0; j < this.bankWidth; j++) {
			bankMask <<= 1;
			bankMask |= 1L;
		}
		bankWidthConst = new SimpleConstant(bankMask, this.bankWidth, false);
		addComponent(bankWidthConst);

		DATA_SHIFT_STAGES = log2(memoryWidth);
		final int bankCount = (int) Math.ceil(((double) memoryWidth)
				/ ((double) this.bankWidth));

		if (_memory.db) {
			_memory.ln(_memory.STRUCT, "\tmem width " + memoryWidth);
			_memory.ln(_memory.STRUCT, "\tmem depth " + memDepth);
			_memory.ln(_memory.STRUCT, "\taddressable locations "
					+ totalAddressableLocations);
			_memory.ln(_memory.STRUCT, "\tbank width " + this.bankWidth);
			_memory.ln(_memory.STRUCT, "\tlocation width " + bitsPerAddress);
			_memory.ln(_memory.STRUCT,
					"\tBank mask 0x" + Long.toHexString(bankMask));
			_memory.ln(_memory.STRUCT, "\tAddr shift bits " + addrShiftBits);
			_memory.ln(_memory.STRUCT,
					"\tData Shift Stages (num stages for data shifter) "
							+ DATA_SHIFT_STAGES);
		}

		getClockPort().setUsed(true);
		getClockPort().getPeer().setUsed(true);
		getClockPort().getPeer().setIDLogical("CLK");
		@SuppressWarnings("unused")
		final Exit exit = makeExit(0, Exit.DONE);

		dataZero = new SimpleConstant(0, memoryWidth, false);
		addComponent(zero);
		addComponent(dataZero);

		// Create the banks.
		for (int i = 0; i < bankCount; i++) {
			MemoryBank bank = new MemoryBank(this.bankWidth, memDepth,
					addrPolicy, impl, getAddrWidth());
			bank.getClockPort().setBus(getClockPort().getPeer());
			bank.setIDLogical(baseName + "_bank_" + i);
			addComponent(bank);
			banks.add(bank);
		}

		//
		// Create the logic needed for each access port of the memory.
		//
		for (LogicalMemoryPort memPort : memoryPorts) {
			StructuralMemoryPort structMemPort = createInterface(memPort,
					memoryWidth);

			// The input of the 'or' will be populated after we create
			// the other logic, but the output of the 'or' is needed
			// to connect to the data out shifter. Thats why we
			// create it here.
			OrOpMulti doutConcatOr = new OrOpMulti();
			addComponent(doutConcatOr);
			doutConcatOr.getResultBus().setIDLogical("doutConcatOr");

			// Generate the DONE
			final Bus doneBus = createDone(structMemPort, getClockPort()
					.getPeer(), getResetPort().getPeer(), impl, baseName);
			structMemPort.getDoneBus().getPeer().setBus(doneBus);

			final Bus shiftedDataIn;
			final Bus shiftMagnitude;

			Bus dataOut = null;
			final Bus selectBus; // The n-bit select for all banks.
			//
			// Create logic to move the input and output data.
			//
			if (addrShiftBits > 0) {
				boolean noShifter = removeByteLaneShifter(memPort,
						locationsPerLine);

				// There are multiple banks so we need to move the
				// data around and generate the necessary selects.

				// First, shift the address to the left by the 'bank power'
				if (getGenericJob().getUnscopedBooleanOptionValue(
						OptionRegistry.LITTLE_ENDIAN)) {
					shiftMagnitude = getLittleEndianDataShiftMagnitude(
							structMemPort.getAddressPort().getPeer(),
							bitsPerAddress);

					shiftedDataIn = getLittleEndianShiftedDataIn(structMemPort,
							shiftMagnitude, memoryWidth, noShifter);

					dataOut = getLittleEndianShiftedDataOut(structMemPort,
							doutConcatOr.getResultBus(), shiftMagnitude,
							noShifter);
				} else {
					shiftMagnitude = getBigEndianDataShiftMagnitude(
							structMemPort.getSizePort().getPeer(),
							structMemPort.getAddressPort().getPeer(),
							bitsPerAddress);

					shiftedDataIn = getBigEndianShiftedDataIn(structMemPort,
							shiftMagnitude, memoryWidth, noShifter,
							bitsPerAddress);

					dataOut = getBigEndianShiftedDataOut(structMemPort,
							doutConcatOr.getResultBus(), shiftMagnitude,
							memoryWidth, noShifter, bitsPerAddress);
				}

				// Create the n-bit select bus
				selectBus = getSelectBus(structMemPort.getSizePort().getPeer(),
						structMemPort.getAddressPort().getPeer(),
						(locationsPerLine / bankCount));
			} else {
				// There is only one bank being instantiated so we
				// don't need to worry about moving data or generating
				// selects

				if (structMemPort.isWrite())
					shiftedDataIn = structMemPort.getDataInPort().getPeer();
				else
					shiftedDataIn = dataZero.getValueBus();

				// If only 1 it is always selected.
				Constant select = new SimpleConstant(1, 1, false);
				addComponent(select);
				selectBus = select.getValueBus();
			}

			// Calculate the address supplied to each bank.
			final Bus addressBus = getShiftedAddress(structMemPort
					.getAddressPort().getPeer());

			final Bus enableBus = createEnableBus(structMemPort);

			// Connect up the ports/buses of each bank.
			List<MemoryBank.BankPort> bankPorts = new ArrayList<MemoryBank.BankPort>();
			for (int i = 0; i < bankCount; i++) {
				MemoryBank bank = banks.get(i);
				MemoryBank.BankPort bankPort = bank.createPort(
						structMemPort.isRead(), structMemPort.isWrite(),
						memPort.getWriteMode());
				bankPorts.add(bankPort);

				// Connect the address
				bankPort.getAddressPort().setBus(addressBus);

				// Connect the select
				bank.instantiateSelectLogic(this, i, selectBus, enableBus,
						structMemPort, bankPort);

				// Connects data input and generates data output.
				Bus bankBus = bank.instantiateDataLogic(this, shiftedDataIn,
						bankWidthConst.getValueBus(), i, memoryWidth, bankPort);

				doutConcatOr.makeDataPort().setBus(bankBus);

				// If the dataOut has not yet been specified, and
				// there is only 1 bank, then the data out comes
				// directly from the single bank.
				if (dataOut == null && bankCount == 1) {
					dataOut = bankPort.getDataOutBus();
				}
			}
			// Store an association between the structuralMemoryPort
			// and the bank ports it attaches to.
			portToBankPorts.put(structMemPort, bankPorts);

			// Connect the finalized read data bus to the outbuf.
			if (structMemPort.isRead()) {
				structMemPort.getDataOutBus().getPeer().setBus(dataOut);
			}
		}
	}

	/**
	 * Sets the initial values of each bank in this memory according to the map
	 * of values established by the values array.
	 */
	public void setInitialValues(AddressableUnit[][] values) {
		for (int i = 0; i < getBankCount(); i++) {
			getBanks().get(i).setInitValues(values, i);
		}
	}

	public void showInitValues() {
		System.out.println("Banks: " + getBankCount());
		for (int i = 0; i < getBankCount(); i++) {
			AddressableUnit[][] vals = getBanks().get(i).getInitValues();
			int q = vals.length;
			int r = q > 0 ? vals[0].length : -1;
			System.out.println("Bank " + i + "[" + q + "][" + r + "]");
			MemoryBuilder.printArr(vals);
		}
	}

	/**
	 * Calculates the equation:
	 * <p>
	 * log2(width / divisor)
	 * 
	 * @param width
	 *            a value of type 'int'
	 * @param divisor
	 *            a value of type 'int'
	 * @return a value of type 'int'
	 */
	private static int calculateAddrOffsetBits(int width, int divisor) {
		return log2(Math.ceil(((double) width) / ((double) divisor)));
	}

	/**
	 * Returns the base 2 log of the value.
	 */
	public static int log2(double a) {
		return net.sf.openforge.util.MathStuff.log2((int) a);
	}

	/**
	 * Creates one complete set of interface ports/buses for this Structural
	 * memory. May be called multiple times for multi-port memories
	 */
	private StructuralMemoryPort createInterface(LogicalMemoryPort memPort,
			int memWidth) {
		final Port address = makeDataPort();
		address.setSize(getAddrWidth(), false);
		address.getPeer().setSize(getAddrWidth(), false);

		final Port size = makeDataPort();
		size.setSize(LogicalMemory.SIZE_WIDTH, false);
		size.getPeer().setSize(LogicalMemory.SIZE_WIDTH, false);

		// The en coming from the graph is the READ ENABLE. Even
		// though the memory has a port that is ENx which is used for
		// both read and writes. This is accounted for in the
		// structural memory by ORing the WEx into the EN line. Thus,
		// 'en' on the StructuralMemory interface is only for reads.
		Port en = null;
		Port wen = null;
		Port din = null;
		Bus dout = null;

		boolean read = false;
		boolean write = false;

		if (!memPort.isWriteOnly()) {
			read = true;
			en = makeDataPort();
			en.setSize(1, false);
			en.getPeer().setSize(1, false);
			dout = getExit(Exit.DONE).makeDataBus();
			dout.setSize(memWidth, true);
		}

		if (!memPort.isReadOnly()) {
			write = true;
			wen = makeDataPort();
			wen.setSize(1, false);
			din = makeDataPort();
			din.setSize(memWidth, true);
		}

		final Bus done = getExit(Exit.DONE).makeDataBus();
		done.setSize(1, true);

		StructuralMemoryPort port = new StructuralMemoryPort(address, din, en,
				wen, size, dout, done, read, write);

		memPorts.put(memPort, port);
		return port;
	}

	/**
	 * Returns the {@link StructuralMemoryPort} which was created based on the
	 * given {@link MemoryPort}
	 */
	public StructuralMemoryPort getStructuralMemoryPort(LogicalMemoryPort port) {
		return memPorts.get(port);
	}

	/**
	 * Returns a List of all the structural memory ports for this memory.
	 */
	public List<StructuralMemoryPort> getStructuralMemoryPorts() {
		return new LinkedList<StructuralMemoryPort>(memPorts.values());
	}

	/**
	 * Removes the {@link StructuralMemoryPort} that was created as a result of
	 * the given {@link MemoryPort}.
	 */
	public void removePort(LogicalMemoryPort port) {
		StructuralMemoryPort structPort = getStructuralMemoryPort(port);
		if (structPort == null) {
			return;
		}

		// Remove the interface allocated on the MemoryBank based on
		// the given memory port.
		List<MemoryBank.BankPort> bankPorts = portToBankPorts.get(structPort);
		for (MemoryBank.BankPort mb : bankPorts) {
			mb.remove();
		}

		memPorts.remove(port);
		structPort.remove();
	}

	/**
	 * Returns the 1-bit wide (control) zero constant.
	 */
	Constant getZero() {
		return zero;
	}

	/**
	 * Returns the number of bits per bank used in this StructuralMemory.
	 */
	int getBankWidth() {
		return bankWidth;
	}

	/**
	 * Returns the number of Banks in this StructuralMemory.
	 */
	public int getBankCount() {
		return getBanks().size();
	}

	public List<MemoryBank> getBanks() {
		return banks;
	}

	public int getDataWidth() {
		return dataWidth;
	}

	public int getAddrWidth() {
		return addrWidth;
	}

	public int getAddressableLocations() {
		return totalAddressableLocations;
	}

	public boolean allowsCombinationalReads() {
		return memImpl.getReadLatency().getMinClocks() == 0;
	}

	/**
	 * Shifts the address right by the number of bits to account for the number
	 * of banks that make this memory. This generates an address to be supplied
	 * to each bank.
	 * 
	 * @param address
	 *            a value of type 'Bus'
	 * @return a value of type 'Bus'
	 */
	private Bus getShiftedAddress(Bus address) {
		final Bus addressBus;
		if (addrShiftBits > 0) {
			// Generate the address. Shift it right by addrShiftBits
			// which is the number of LSB address bits used to move data
			// to the correct byte lanes.
			// int addrStages =
			// (int)Math.ceil(Math.log(this.addrShiftBits)/Math.log(2.0));
			final Constant addrConst = new SimpleConstant(addrShiftBits, 32,
					false);
			final int stages = addrConst.getValueBus().getValue().getSize();
			final RightShiftUnsignedOp addrShift = new RightShiftUnsignedOp(
					stages);
			addrShift.getLeftDataPort().setBus(address);
			addrShift.getRightDataPort().setBus(addrConst.getValueBus());

			addComponent(addrConst);
			addComponent(addrShift);

			addressBus = addrShift.getResultBus();
		} else {
			addressBus = address;
		}
		return addressBus;
	}

	/**
	 * Generates the enable signal to the memory banks. This signal is the read
	 * enable (en signal to the structural memory) logically ORed with the write
	 * enable to ensure that the memory is enabled during read or write
	 * operations.
	 */
	private Bus createEnableBus(StructuralMemoryPort structMemPort) {
		final Bus enBus;
		if (structMemPort.isRead() && structMemPort.isWrite()) {
			Or enOr = new Or(2);
			addComponent(enOr);
			enOr.getDataPorts().get(0)
					.setBus(structMemPort.getEnablePort().getPeer());
			enOr.getDataPorts().get(1)
					.setBus(structMemPort.getWriteEnablePort().getPeer());
			enBus = enOr.getResultBus();
		} else if (structMemPort.isRead()) {
			// In case of read only the bank enables are simply the
			// read enables
			enBus = structMemPort.getEnablePort().getPeer();
		} else {
			// In case of write only the bank enables are simply the
			// write enables.
			enBus = structMemPort.getWriteEnablePort().getPeer();
		}
		return enBus;
	}

	/**
	 * Generates a Bus which is used as the magnitude by which to shift the data
	 * on both reads and writes.
	 * 
	 * @param addrBus
	 *            a value of type 'Bus'
	 * @return a value of type 'Bus'
	 */
	private Bus getLittleEndianDataShiftMagnitude(Bus addrBus, int locationWidth) {
		// The data bus is shifted by Addr[x:0] * bitsPerAddress
		final Bus shiftMagnitude;
		final int log2_width = log2(locationWidth);
		if ((2 << log2_width) == locationWidth) {
			final Constant powerConstant = new SimpleConstant(log2_width, 32,
					false);
			final int stages = powerConstant.getValueBus().getValue().getSize();
			LeftShiftOp addrShift = new LeftShiftOp(stages);
			addrShift.getLeftDataPort().setBus(addrBus);
			addrShift.getRightDataPort().setBus(powerConstant.getValueBus());
			addComponent(powerConstant);
			addComponent(addrShift);
			shiftMagnitude = addrShift.getResultBus();
		} else {
			final Constant locSizeConstant = new SimpleConstant(locationWidth,
					32, false);
			final MultiplyOp multiply = new MultiplyOp(32);
			multiply.getLeftDataPort().setBus(addrBus);
			multiply.getRightDataPort().setBus(locSizeConstant.getValueBus());

			addComponent(locSizeConstant);
			addComponent(multiply);
			shiftMagnitude = multiply.getResultBus();
		}

		return shiftMagnitude;
	}

	/**
	 * Create the shared data input shifter which moves write data to the
	 * appropriate byte lanes as specified by the shiftMagnitude bus.
	 */
	private Bus getLittleEndianShiftedDataIn(
			StructuralMemoryPort structMemPort, Bus shiftMagnitude,
			int memWidth, boolean noShifter) {
		Bus shiftedDataIn;
		if (structMemPort.isWrite()) {
			if (noShifter) {
				shiftedDataIn = structMemPort.getDataInPort().getPeer();
			} else {
				// Create the data in shifter.
				LeftShiftOp dataShift = new LeftShiftOp(DATA_SHIFT_STAGES);

				dataShift.getLeftDataPort().setBus(
						structMemPort.getDataInPort().getPeer());
				dataShift.getRightDataPort().setBus(shiftMagnitude);
				addComponent(dataShift);

				shiftedDataIn = dataShift.getResultBus();
			}
		} else {
			shiftedDataIn = dataZero.getValueBus();
		}

		return shiftedDataIn;
	}

	/**
	 * Create the data shifter that is used to move read data to the correct
	 * byte lane. This one shifter is used for all banks because it takes its
	 * input from after the concatenation 'or'.
	 */
	private Bus getLittleEndianShiftedDataOut(
			StructuralMemoryPort structMemPort, Bus dataBus,
			Bus shiftMagnitude, boolean noShifter) {
		Bus dataOut;
		if (structMemPort.isRead()) {
			if (noShifter) {
				dataOut = dataBus;
			} else {
				// If the latency of the memory banks is greater than 0,
				// then we need to delay the shift magnitude bus (derived
				// from the address bus) by that many cycles to keep it in
				// line with the right address
				Bus delayedShiftMagnitude = delayBus(shiftMagnitude,
						"shiftDelay", memImpl.getReadLatency().getMinClocks(),
						getClockPort().getPeer(), getResetPort().getPeer());

				// Create the data out shifter.
				RightShiftUnsignedOp dataOutShift = new RightShiftUnsignedOp(
						DATA_SHIFT_STAGES);
				final Bus doutBus = dataOutShift.getResultBus();
				dataOutShift.getLeftDataPort().setBus(dataBus);
				dataOutShift.getRightDataPort().setBus(delayedShiftMagnitude);
				addComponent(dataOutShift);
				dataOut = doutBus;
			}
		} else {
			dataOut = dataZero.getValueBus();
		}

		return dataOut;
	}

	/**
	 * Generates a Bus which is used as the magnitude by which to shift the data
	 * on both reads and writes.
	 * 
	 * @param addrBus
	 *            a value of type 'Bus'
	 * @return a value of type 'Bus'
	 */
	private Bus getBigEndianDataShiftMagnitude(Bus size, Bus addrBus,
			int locationWidth) {
		final double locationsPerLine = ((double) getDataWidth())
				/ ((double) locationWidth);
		// final double dataBytes = ((double)this.getDataWidth()) / ((double)8);

		Constant s0 = null;
		Constant s1 = null;
		Constant s2 = null;
		Constant s3 = null;
		if (locationsPerLine == 8) {
			s0 = new SimpleConstant(4, addrBus.getSize(), false);
			s1 = new SimpleConstant(7, addrBus.getSize(), false);
			s2 = new SimpleConstant(6, addrBus.getSize(), false);
			s3 = new SimpleConstant(0, addrBus.getSize(), false);
		} else if (locationsPerLine == 4) {
			s0 = new SimpleConstant(0, addrBus.getSize(), false);
			s1 = new SimpleConstant(3, addrBus.getSize(), false);
			s2 = new SimpleConstant(2, addrBus.getSize(), false);
			s3 = new SimpleConstant(0, addrBus.getSize(), false);
		} else if (locationsPerLine == 2) {
			s0 = new SimpleConstant(0, addrBus.getSize(), false);
			s1 = new SimpleConstant(1, addrBus.getSize(), false);
			s2 = new SimpleConstant(0, addrBus.getSize(), false);
			s3 = new SimpleConstant(0, addrBus.getSize(), false);
		} else {
			throw new IllegalArgumentException(
					"In big endian compilations, memory must be laid out with 8, 4, 2, or 1 address per line");
		}

		EncodedMux mux = new EncodedMux(4);
		mux.getDataPort(0).setBus(s0.getValueBus());
		mux.getDataPort(1).setBus(s1.getValueBus());
		mux.getDataPort(2).setBus(s2.getValueBus());
		mux.getDataPort(3).setBus(s3.getValueBus());
		mux.getSelectPort().setBus(size);

		SubtractOp subtract = new SubtractOp();
		subtract.getLeftDataPort().setBus(mux.getResultBus());
		subtract.getRightDataPort().setBus(addrBus);

		addComponent(mux);
		addComponent(subtract);

		// The data bus is shifted by Addr[x:0] * bitsPerAddress where
		// Addr[x:0] must be re-arranged to account for the reversed
		// ordering of bytes in the memory (the memory is in little
		// endian order)
		final Bus shiftMagnitude;
		final int log2_width = log2(locationWidth);
		if ((2 << log2_width) == locationWidth) {
			final Constant powerConstant = new SimpleConstant(log2_width, 32,
					false);
			final int stages = powerConstant.getValueBus().getValue().getSize();
			LeftShiftOp addrShift = new LeftShiftOp(stages);
			addrShift.getLeftDataPort().setBus(subtract.getResultBus());
			addrShift.getRightDataPort().setBus(powerConstant.getValueBus());
			addComponent(powerConstant);
			addComponent(addrShift);
			shiftMagnitude = addrShift.getResultBus();
		} else {
			final Constant locSizeConstant = new SimpleConstant(locationWidth,
					32, false);
			final MultiplyOp multiply = new MultiplyOp(32);
			multiply.getLeftDataPort().setBus(subtract.getResultBus());
			multiply.getRightDataPort().setBus(locSizeConstant.getValueBus());

			addComponent(locSizeConstant);
			addComponent(multiply);
			shiftMagnitude = multiply.getResultBus();
		}

		return shiftMagnitude;
	}

	private Bus getBigEndianShiftedDataIn(StructuralMemoryPort structMemPort,
			Bus shiftMagnitude, int memWidth, boolean noShifter,
			int bitsPerAddress) {
		Bus shiftedDataIn;
		if (structMemPort.isWrite()) {
			final EndianSwapper swap = new EndianSwapper(memWidth,
					bitsPerAddress);
			swap.getInputPort().setBus(structMemPort.getDataInPort().getPeer());
			addComponent(swap);
			if (noShifter) {
				shiftedDataIn = swap.getOutputBus();
			} else {
				// Create the data in shifter.
				RightShiftUnsignedOp dataShift = new RightShiftUnsignedOp(
						DATA_SHIFT_STAGES);
				final Bus dataBus = dataShift.getResultBus();
				dataShift.getLeftDataPort().setBus(swap.getOutputBus());
				dataShift.getRightDataPort().setBus(shiftMagnitude);
				addComponent(dataShift);
				shiftedDataIn = dataBus;
			}
		} else {
			shiftedDataIn = dataZero.getValueBus();
		}

		return shiftedDataIn;

	}

	private Bus getBigEndianShiftedDataOut(StructuralMemoryPort structMemPort,
			Bus dataBus, Bus shiftMagnitude, int memWidth, boolean noShifter,
			int bitsPerAddress) {
		Bus dataOut;
		if (structMemPort.isRead()) {
			final EndianSwapper swap = new EndianSwapper(memWidth,
					bitsPerAddress);
			dataOut = swap.getOutputBus();
			if (noShifter) {
				swap.getInputPort().setBus(dataBus);
				addComponent(swap);
			} else {
				// If the latency of the memory banks is greater than
				// 0, then we need to delay the shift magnitude bus
				// (derived from the address bus) by that many cycles
				// to keep it in line with the right address
				final Bus delayedShiftMagnitude = delayBus(shiftMagnitude,
						"shiftDelay", memImpl.getReadLatency().getMinClocks(),
						getClockPort().getPeer(), getResetPort().getPeer());

				// Create the data out shifter.
				LeftShiftOp dataOutShift = new LeftShiftOp(DATA_SHIFT_STAGES);
				swap.getInputPort().setBus(dataOutShift.getResultBus());
				dataOutShift.getLeftDataPort().setBus(dataBus);
				dataOutShift.getRightDataPort().setBus(delayedShiftMagnitude);
				addComponent(dataOutShift);
				addComponent(swap);
			}
		} else {
			dataOut = dataZero.getValueBus();
		}

		return dataOut;
	}

	/**
	 * Generates an n-bit select bus, in which there is one bit for each bank of
	 * this memory, and where bit 0 is the select for the LSB bank, bit 1 the
	 * next, etc. These selects do NOT account for the read enable or write
	 * enable to this memory, but merely indicate which byte lanes are active
	 * for a given address/size combination.
	 * 
	 * @param size
	 *            a value of type 'Bus'
	 * @param address
	 *            a value of type 'Bus'
	 * @param locationsPerBank
	 *            , the number of addressable locations per line in each bank
	 * @return a value of type 'Bus'
	 */
	private Bus getSelectBus(Bus size, Bus address, int locationsPerBank) {
		//
		// The position of the constants on the data bus corresponds
		// to the encoding set up in encodeAccessCount below. Since
		// 4 locations is encoded to size 0, then the 0th data port of
		// the encoded mux must contain 4 select bits (if bank width
		// is 1 location).
		//
		EncodedMux mux = new EncodedMux(4);
		// final double bankBytes = ((double)this.bankWidth) / ((double)8);
		// The 4,1,2,8 here are not related to bytes, but rather
		// indicate the number of active banks if that input to the
		// mux is selected. Thus, this mux is selecting the number of
		// active enables. This will then be sub-sampled if there are
		// more than 1 bank per addressable location.
		Constant s0 = getConstant((int) Math.ceil(((double) 4)
				/ locationsPerBank));
		Constant s1 = getConstant((int) Math.ceil(((double) 1)
				/ locationsPerBank));
		Constant s2 = getConstant((int) Math.ceil(((double) 2)
				/ locationsPerBank));
		Constant s3 = getConstant((int) Math.ceil(((double) 8)
				/ locationsPerBank));
		mux.getDataPort(0).setBus(s0.getValueBus());
		mux.getDataPort(1).setBus(s1.getValueBus());
		mux.getDataPort(2).setBus(s2.getValueBus());
		mux.getDataPort(3).setBus(s3.getValueBus());
		mux.getSelectPort().setBus(size);

		Bus shlMagnitude = address;
		if (locationsPerBank > 1) {
			// If there are multiple locations in a bank then the lsb
			// bits of the address bus must be right shifted when used
			// as the shift magnitude for the encoded bank select, ie
			// they are sub-sampled. This ensures that the bank select
			// moves to the next bank only when the correct number of
			// locations has been reached.
			final int magValue = log2(locationsPerBank);
			assert magValue > 0;
			final Constant addrShiftMagnitude = new SimpleConstant(magValue,
					32, false);
			final RightShiftUnsignedOp selAddrShift = new RightShiftUnsignedOp(
					log2(magValue) + 1);
			selAddrShift.getLeftDataPort().setBus(address);
			selAddrShift.getRightDataPort().setBus(
					addrShiftMagnitude.getValueBus());

			addComponent(addrShiftMagnitude);
			addComponent(selAddrShift);

			shlMagnitude = selAddrShift.getResultBus();
		}

		final int leftShiftSize = log2(getBankCount());
		final LeftShiftOp sizeShift = new LeftShiftOp(leftShiftSize);
		sizeShift.getLeftDataPort().setBus(mux.getResultBus());
		sizeShift.getRightDataPort().setBus(shlMagnitude);

		addComponent(mux);
		addComponent(sizeShift);

		return sizeShift.getResultBus();
	}

	/**
	 * The Done signal for a memory port is the logical OR of read done and
	 * write done. The read/write dones are calculated by delaying the read and
	 * write enable signals by the number of cycles specified by the
	 * {@link MemoryImplementation}.
	 * 
	 * @param re
	 *            the read enable {@link Bus}
	 * @param we
	 *            the write enable {@link Bus}
	 * @param clk
	 *            the clock {@link Bus}
	 * @param rst
	 *            the reset {@link Bus}
	 * @param imp
	 *            the {@link MemoryImplementation}
	 * @param baseName
	 *            the String to which new signal names are appended.
	 * @return the generated 'done' {@link Bus}
	 */
	private Bus createDone(StructuralMemoryPort memPort, Bus clk, Bus rst,
			MemoryImplementation imp, String baseName) {
		assert memPort.isRead() || memPort.isWrite() : "Must be either read or written";

		final Bus done;
		// if (re != null && we != null)
		if (memPort.isRead() && memPort.isWrite()) {
			Bus readDelayed = delayBus(memPort.getEnablePort().getPeer(),
					baseName + "_re_delay",
					imp.getReadLatency().getMaxClocks(), clk, rst);
			Bus writeDelayed = delayBus(memPort.getWriteEnablePort().getPeer(),
					baseName + "_we_delay", imp.getWriteLatency()
							.getMaxClocks(), clk, rst);

			if (imp.getReadLatency().getMaxClocks() == 0) {
				// Equation for done is (en && !we) || (we_done)
				And and = new And(2);
				addComponent(and);
				Not not = new Not();
				addComponent(not);
				not.getDataPort()
						.setBus(memPort.getWriteEnablePort().getPeer());
				and.getDataPorts().get(0).setBus(readDelayed);
				and.getDataPorts().get(1).setBus(not.getResultBus());
				readDelayed = and.getResultBus();
			}

			Or or = new Or(2);
			addComponent(or);
			or.getDataPorts().get(0).setBus(readDelayed);
			or.getDataPorts().get(1).setBus(writeDelayed);
			done = or.getResultBus();
		} else if (memPort.isRead()) {
			done = delayBus(memPort.getEnablePort().getPeer(), baseName
					+ "_re_delay", imp.getReadLatency().getMaxClocks(), clk,
					rst);
		} else {
			done = delayBus(memPort.getWriteEnablePort().getPeer(), baseName
					+ "_we_delay", imp.getWriteLatency().getMaxClocks(), clk,
					rst);
		}
		return done;
	}

	/**
	 * Delays the specified bus by the given number of stages. Adds all
	 * necessary logic to this module.
	 */
	private Bus delayBus(Bus bus, String id, int stages, Bus clk, Bus rst) {
		Bus delayed = bus;
		for (int i = 0; i < stages; i++) {
			// Needs RESET b/c some delayed buses are factored into
			// the control path
			Reg sync = Reg.getConfigurableReg(Reg.REGR, id + i);
			sync.getDataPort().setBus(delayed);
			sync.getClockPort().setBus(clk);
			sync.getResetPort().setBus(rst);
			sync.getInternalResetPort().setBus(rst);
			delayed = sync.getResultBus();
			addComponent(sync);
		}
		return delayed;
	}

	/**
	 * Creates, adds to this module, and returns a Constant value with the
	 * specified number of bits set to 1 in the LSB positions of the value. Thus
	 * getConstant(4) would return a Constant whose value is 0xF. getConstant(8)
	 * would return a Constant whose value is 0xFF.
	 * 
	 * @param numOneBits
	 *            the number of 1 bits to set in the LSB positions
	 * @return a {@link Constant}
	 */
	private Constant getConstant(int numOneBits) {
		long value = 0;
		for (int i = 0; i < numOneBits; i++) {
			value <<= 1;
			value |= 1;
		}
		Constant constant = new SimpleConstant(value, 8, false);
		addComponent(constant);
		return constant;
	}

	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		if (super.removeComponent(removed)) {
			addComponent(inserted);
			return true;
		}
		return false;
	}

	/**
	 * Byte lane shifters are only used when there is an access to the memory
	 * that is smaller than the width of the memory.
	 * 
	 * @param memPort
	 *            LogicalMemoryPort
	 * @return true if byte lane shifters has been eliminated
	 */
	private boolean removeByteLaneShifter(LogicalMemoryPort memPort,
			int locationsPerLine) {
		for (LValue lvalue : memPort.getReadAccesses()) {
			if (locationsPerLine != lvalue.getAccessLocationCount()) {
				return false;
			}
		}

		for (LValue lvalue : memPort.getWriteAccesses()) {
			if (locationsPerLine != lvalue.getAccessLocationCount()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean isOpaque() {
		return true;
	}

	@Override
	public void accept(Visitor vis) {
		throw new UnexpectedVisitationException(
				"Unexepected visitation of StructuralMemory");
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Cannot clone Structural Memories");
	}

	/**
	 * Retrieves the {@link Constant} value used to refer to the given number of
	 * bytes being accessed.
	 * 
	 * @param accessCount
	 *            , the number of addressable locations being accessed.
	 * @return a {@link Constant} whose value is suitable for sending to the
	 *         Memory on the size port.
	 */
	public static Constant encodeAccessCount(int accessCount) {
		//
		// NOTE, if you change this encoding, you MUST change the
		// getSelectBus method above to correlate.
		//
		int size = 0;
		switch (accessCount) {
		case 1:
			size = 1;
			break;
		case 2:
			size = 2;
			break;
		case 4:
			size = 0;
			break;
		case 8:
			size = 3;
			break;
		default:
			assert false : "Unknown number of addressable locations accessed "
					+ accessCount;
			size = -1;
			break;
		}
		return new SimpleConstant(size, LogicalMemory.SIZE_WIDTH, false);
	}

	/**
	 * A simple class used to associate and identify all the ports and buses for
	 * one interface to the memory.
	 */
	public class StructuralMemoryPort {
		public Port addr;
		public Port din;
		public Port en;
		public Port we;
		public Port size;

		public Bus dout;
		public Bus done;

		public boolean read;
		public boolean write;

		public StructuralMemoryPort(Port a, Port d, Port e, Port w, Port s,
				Bus b, Bus dn, boolean rd, boolean wr) {
			addr = a;
			din = d;
			en = e;
			we = w;
			size = s;
			dout = b;
			done = dn;
			read = rd;
			write = wr;
		}

		public Port getAddressPort() {
			return addr;
		}

		public Port getDataInPort() {
			return din;
		}

		public Port getEnablePort() {
			return en;
		}

		public Port getWriteEnablePort() {
			return we;
		}

		public Port getSizePort() {
			return size;
		}

		public Bus getDataOutBus() {
			return dout;
		}

		public Bus getDoneBus() {
			return done;
		}

		public boolean isRead() {
			return read;
		}

		public boolean isWrite() {
			return write;
		}

		public void remove() {
			if (addr != null)
				removeDataPort(addr);

			if (din != null)
				removeDataPort(din);

			if (en != null)
				removeDataPort(en);

			if (we != null)
				removeDataPort(we);

			if (size != null)
				removeDataPort(size);

			if (dout != null)
				StructuralMemory.this.getExit(Exit.DONE).removeDataBus(dout);

			if (done != null)
				StructuralMemory.this.getExit(Exit.DONE).removeDataBus(done);
		}
	}

}// StructuralMemory
