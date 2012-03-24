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
import java.util.Iterator;
import java.util.List;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Gateway;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.util.naming.ID;

/**
 * Acts as a gateway for Memory read/writes into and out of a procedure
 * 
 * the gateway has has sets of data, enable and address ports for each
 * MemoryWrite that exists on the local side of the gateway, and one
 * corresponding data, enable and address bus on the global side.
 * 
 * There is also a pair of global side ports - data and enable - that are wired
 * through to the local side MemoryReads. In the static case these are straight
 * wires, in cases where there is a delay (memory), there may be delays or muxes
 * in this area.
 * 
 * @author Andreas Kollegger
 * @version $Id: MemoryGateway.java 2 2005-06-09 20:00:48Z imiller $
 */
public class MemoryGateway extends Gateway {

	private List<ReadSlot> readSlots;
	private List<WriteSlot> writeSlots;

	// private List localDataPorts;

	private Port memoryDonePort;
	private Port memoryDataReadPort;
	private Bus memoryReadEnableBus;
	private Bus memoryWriteEnableBus;
	private Bus memoryAddressBus;
	private Bus memorySizeBus;
	private Bus memoryDataWriteBus;

	/**
	 * Creates a MemoryGateway with size entries (one per MemoryWrite)
	 * 
	 * @param size
	 *            number of entries into the register writes to connect to the
	 *            gateway.
	 * 
	 * @param resource
	 *            the memory being accessed
	 * @param reads
	 *            the number of read accesses
	 * @param writes
	 *            the number of write accesses
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	public MemoryGateway(LogicalMemoryPort resource, int reads, int writes,
			int maxAddressWidth) {
		super(resource);
		assert (writes + reads) > 0 : "Illegal number of accesses: "
				+ Integer.toString(writes + reads) + " for memory gateway";
		assert (resource != null) : "Can't create a gateway for a null LogicalMemoryPort";

		final StructuralMemory structMem = resource.getStructuralMemory();
		final int dataWidth = structMem.getDataWidth();
		final int sizeWidth = LogicalMemory.SIZE_WIDTH;
		final String baseName = ID.showLogical(resource);

		Bus internalReadEnable = null;
		Bus internalAddress = null;
		Bus internalSize = null;
		Bus internalWriteEnable = null;
		Bus internalWriteData = null;

		Exit exit = makeExit(0);
		exit.setLatency(Latency.ZERO);

		final Bus gatedDataBus;
		final Bus gatedDoneBus = exit.getDoneBus();

		Mux addressMux = null;
		Mux sizeMux = null;
		Mux dataMux = null;
		Or readOr = null;
		Or writeOr = null;

		if (reads > 0) {
			gatedDataBus = exit.makeDataBus();
			gatedDataBus.setSize(dataWidth, false);
			gatedDataBus.setIDLogical(baseName + "_DATA");
		} else {
			gatedDataBus = null;
		}

		if (writes > 1) {
			dataMux = new Mux(writes);
			dataMux.setIDLogical(baseName + "_dmux");
			Bus dataMuxBus = dataMux.getResultBus();
			dataMuxBus.setIDLogical(baseName + "_dmux");

			writeOr = new Or(writes);
			Bus writeOrBus = writeOr.getResultBus();
			writeOrBus.setIDLogical(baseName + "_wor");

			addComponent(dataMux);
			addComponent(writeOr);

			internalWriteEnable = writeOrBus;
			internalWriteData = dataMuxBus;
		}

		if (reads > 1) {
			readOr = new Or(reads);
			Bus readOrBus = readOr.getResultBus();
			readOrBus.setIDLogical(baseName + "_ror");
			addComponent(readOr);

			internalReadEnable = readOrBus;
		}

		if ((reads + writes) > 1) {
			addressMux = new Mux(reads + writes);
			addressMux.setIDLogical(baseName + "_amux");
			Bus addressMuxBus = addressMux.getResultBus();
			addressMuxBus.setIDLogical(baseName + "_amux");

			addComponent(addressMux);

			internalAddress = addressMuxBus;

			sizeMux = new Mux(reads + writes);
			sizeMux.setIDLogical(baseName + "_smux");
			internalSize = sizeMux.getResultBus();
			internalSize.setIDLogical(baseName + "_smux");
			addComponent(sizeMux);
		}

		readSlots = new ArrayList<ReadSlot>(reads);
		writeSlots = new ArrayList<WriteSlot>(writes);

		Iterator<Port> addressMuxPorts = (addressMux != null) ? addressMux
				.getGoPorts().iterator() : null;
		Iterator<Port> sizeMuxPorts = (sizeMux != null) ? sizeMux.getGoPorts()
				.iterator() : null;

		if (writes > 0) {
			Iterator<Port> dataMuxPorts = (dataMux != null) ? dataMux
					.getGoPorts().iterator() : null;
			Iterator<Port> writeOrPorts = (writeOr != null) ? writeOr
					.getDataPorts().iterator() : null;

			for (int i = 0; i < writes; i++) {
				Port localWriteEnablePort = makeDataPort();
				Port localAddressPort = makeDataPort();
				Port localDataPort = makeDataPort();
				Port localSizePort = makeDataPort();

				writeSlots.add(new WriteSlot(localWriteEnablePort,
						localAddressPort, localDataPort, localSizePort,
						gatedDoneBus));

				if (addressMux != null) {
					Port addressMuxGoPort = addressMuxPorts.next();
					addressMuxGoPort.setBus(localWriteEnablePort.getPeer());
					Port addressMuxDataPort = addressMux
							.getDataPort(addressMuxGoPort);
					addressMuxDataPort.setBus(localAddressPort.getPeer());
				} else {
					internalAddress = localAddressPort.getPeer();
				}

				if (sizeMux != null) {
					Port p = sizeMuxPorts.next();
					p.setBus(localWriteEnablePort.getPeer());
					p = sizeMux.getDataPort(p);
					p.setBus(localSizePort.getPeer());
				} else {
					internalSize = localSizePort.getPeer();
				}

				/*
				 * Just in case the incoming data isn't the same size as the
				 * memory that's being written, cast it to the width of the data
				 * path to the memory.
				 */
				final CastOp castOp = new CastOp(dataWidth, false);
				castOp.getDataPort().setBus(localDataPort.getPeer());
				addComponent(castOp);

				if (dataMux != null) {
					Port dataMuxGoPort = dataMuxPorts.next();
					dataMuxGoPort.setBus(localWriteEnablePort.getPeer());

					Port dataMuxDataPort = dataMux.getDataPort(dataMuxGoPort);
					dataMuxDataPort.setBus(castOp.getResultBus());
				} else {
					internalWriteData = castOp.getResultBus();
				}

				if (writeOr != null) {
					Port orDataPort = writeOrPorts.next();
					orDataPort.setBus(localWriteEnablePort.getPeer());
				} else {
					internalWriteEnable = localWriteEnablePort.getPeer();
				}
			}
		}

		if (reads > 0) {
			Iterator<Port> readOrPorts = (readOr != null) ? readOr
					.getDataPorts().iterator() : null;

			for (int i = 0; i < reads; i++) {
				Port localReadEnablePort = makeDataPort();
				Port localAddressPort = makeDataPort();
				Port localSizePort = makeDataPort();

				readSlots.add(new ReadSlot(localReadEnablePort,
						localAddressPort, localSizePort, gatedDoneBus,
						gatedDataBus));

				if (addressMux != null) {
					Port addressMuxGoPort = addressMuxPorts.next();
					addressMuxGoPort.setBus(localReadEnablePort.getPeer());
					Port addressMuxDataPort = addressMux
							.getDataPort(addressMuxGoPort);
					addressMuxDataPort.setBus(localAddressPort.getPeer());
				} else {
					internalAddress = localAddressPort.getPeer();
				}

				if (sizeMux != null) {
					Port p = sizeMuxPorts.next();
					p.setBus(localReadEnablePort.getPeer());
					p = sizeMux.getDataPort(p);
					p.setBus(localSizePort.getPeer());
				} else {
					internalSize = localSizePort.getPeer();
				}

				if (readOr != null) {
					Port orDataPort = readOrPorts.next();
					orDataPort.setBus(localReadEnablePort.getPeer());
				} else {
					internalReadEnable = localReadEnablePort.getPeer();
				}
			}
		}

		memoryDonePort = makeDataPort(Component.SIDEBAND);

		Bus memoryDonePortBus = memoryDonePort.getPeer();
		gatedDoneBus.getPeer().setBus(memoryDonePortBus);

		if (reads > 0) {
			memoryReadEnableBus = exit.makeDataBus(Component.SIDEBAND);
			memoryReadEnableBus.setSize(1, false);
			memoryReadEnableBus.setIDLogical(baseName + "_SIDE_RE");
			memoryDataReadPort = makeDataPort(Component.SIDEBAND);

			Bus memoryDataReadPortBus = memoryDataReadPort.getPeer();

			memoryReadEnableBus.getPeer().setBus(internalReadEnable);
			gatedDataBus.getPeer().setBus(memoryDataReadPortBus);
		}

		if (writes > 0) {
			memoryWriteEnableBus = exit.makeDataBus(Component.SIDEBAND);
			memoryWriteEnableBus.setSize(1, false);
			memoryWriteEnableBus.setIDLogical(baseName + "_SIDE__WE");

			memoryDataWriteBus = exit.makeDataBus(Component.SIDEBAND);
			memoryDataWriteBus.setSize(dataWidth, false);
			memoryDataWriteBus.setIDLogical(baseName + "_SIDE__WDATA");

			memoryWriteEnableBus.getPeer().setBus(internalWriteEnable);
			memoryDataWriteBus.getPeer().setBus(internalWriteData);
		}

		memoryAddressBus = exit.makeDataBus(Component.SIDEBAND);
		memoryAddressBus.setSize(maxAddressWidth, false);
		memoryAddressBus.setIDLogical(baseName + "_SIDE_ADDR");
		memoryAddressBus.getPeer().setBus(internalAddress);

		memorySizeBus = exit.makeDataBus(Component.SIDEBAND);
		memorySizeBus.setSize(sizeWidth, false);
		memorySizeBus.setIDLogical(baseName + "_SIDE_SIZE");
		memorySizeBus.getPeer().setBus(internalSize);

	} // MemoryGateway()

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	// public List getLocalDataPorts() {
	// return localDataPorts;
	// }

	/**
	 * Returns the port which receives the done signal for the current memory
	 * operation. Should be attached to the MemoryPort's Done bus.
	 */
	public Port getMemoryDonePort() {
		return memoryDonePort;
	}

	/**
	 * Returns the port which receives the data for the current read operation.
	 * Should be attached to the MemoryPort's data output bus.
	 */
	public Port getMemoryDataReadPort() {
		return memoryDataReadPort;
	}

	/**
	 * Returns the bus which is used to assert a read operation. Should be
	 * attached to the read enable port of the MemoryPort.
	 */
	public Bus getMemoryReadEnableBus() {
		return memoryReadEnableBus;
	}

	/**
	 * Returns the bus which is used to assert a write operation. Should be
	 * attached to the write enable port of the MemoryPort.
	 */
	public Bus getMemoryWriteEnableBus() {
		return memoryWriteEnableBus;
	}

	/**
	 * Returns the bus which provides the address for the current read or write
	 * operation. This should get attached to the MemoryPort's address port.
	 */
	public Bus getMemoryAddressBus() {
		return memoryAddressBus;
	}

	/**
	 * Returns the bus which provides data for the current write. This should
	 * get attached to the MemoryPort's data in port.
	 */
	public Bus getMemoryDataWriteBus() {
		return memoryDataWriteBus;
	}

	/**
	 * Returns the bus which provides the size value for the access to the
	 * memory.
	 */
	public Bus getMemorySizeBus() {
		return memorySizeBus;
	}

	/**
	 * Gets a List of {@link ReadSlots} which provide the grouped Ports and
	 * Buses needed for a memory read access.
	 */
	public List<ReadSlot> getReadSlots() {
		return readSlots;
	}

	/**
	 * Gets a List of {@link WriteSlot}s which provide the grouped Ports and
	 * Buses needed for a memory write access.
	 */
	public List<WriteSlot> getWriteSlots() {
		return writeSlots;
	}

	public class ReadSlot {
		Port enable;
		Port address;
		Port size;
		Bus ready;
		Bus data;

		public ReadSlot(Port enable, Port address, Port size, Bus ready,
				Bus data) {
			this.enable = enable;
			this.address = address;
			this.size = size;
			this.ready = ready;
			this.data = data;
		}

		public Port getEnable() {
			return enable;
		}

		public Port getAddress() {
			return address;
		}

		public Port getSizePort() {
			return size;
		}

		public Bus getReady() {
			return ready;
		}

		public Bus getData() {
			return data;
		}
	}

	public class WriteSlot {
		Port enable;
		Port address;
		Port size;
		Bus done;
		Port data;

		public WriteSlot(Port enable, Port address, Port data, Port size,
				Bus done) {
			this.enable = enable;
			this.address = address;
			this.done = done;
			this.data = data;
			this.size = size;
		}

		public Port getEnable() {
			return enable;
		}

		public Port getAddress() {
			return address;
		}

		public Bus getDone() {
			return done;
		}

		public Port getData() {
			return data;
		}

		public Port getSizePort() {
			return size;
		}
	}

}
