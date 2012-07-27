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
package net.sf.openforge.lim;

import java.util.ArrayList;
import java.util.List;

import net.sf.openforge.lim.primitive.Mux;
import net.sf.openforge.lim.primitive.Or;

/**
 * Acts as a gateway for register read/writes into and out of a procedure.
 * 
 * <p>
 * The gateway has pairs of data and enable ports for each RegisterWrite that
 * exists on the local side of the gateway, and one corresponding data and
 * enable bus on the global side.
 * 
 * <p>
 * There is also a pair of global side ports - data and enable - that are wired
 * through to the local side RegisterReads. In the static case these are
 * straight wires, in cases where there is a delay (memory), there may be delays
 * or muxes in this area.
 * <p>
 * ABK -- these have been removed since RegisterRead doesn't use a GO signal and
 * all reads get wired directly to the sideband data.
 * <P>
 * 
 * <pre>
 * note that the local->global enable bus is the exit's done bus,
 * local->global data is exit data bus 0
 * global->local enable is exit data bus 1
 * global->local data is exit data bus 2
 * </pre>
 * 
 * @author Jim Jensen
 * @version $Id: RegisterGateway.java 2 2005-06-09 20:00:48Z imiller $
 */
public class RegisterGateway extends Gateway {

	/** the mux at the center of the RegisterGateway */
	private Mux mux;
	/** or the go signals together */
	private Or or;
	private List<Port> localEnablePorts;
	private List<Port> localDataPorts;

	/**
	 * creates a RegisterGateway with size entries (one per RegisterWrite)
	 * 
	 * @param size
	 *            number of entries into the register writes to connect to the
	 *            gateway
	 */
	public RegisterGateway(int size, Register resource) {
		super(resource);
		assert size > 0 : "Illegal size: " + size + " for register gateway";

		mux = new Mux(size);
		Bus mux_result = mux.getResultBus();
		mux_result.setSize(resource.getInitWidth(), false);
		mux_result.setIDLogical(resource.showIDLogical() + "_mux_out");

		or = new Or(size);
		Bus or_result = or.getResultBus();
		or_result.setIDLogical(resource.showIDLogical() + "_or_result");

		addComponent(mux);
		addComponent(or);
		Exit exit = makeExit(1); // one for the local->global data (ABK: reduced
									// to 1)
		exit.setLatency(Latency.ZERO);
		localEnablePorts = new ArrayList<Port>(size);
		localDataPorts = new ArrayList<Port>(size);

		for (int i = 0; i < size; i++) {
			// create the ports
			Port localEnablePort = makeDataPort();
			Port localDataPort = makeDataPort();

			localEnablePorts.add(localEnablePort);
			localDataPorts.add(localDataPort);

			Port muxGoPort = mux.getGoPorts().get(i);
			Port muxDataPort = mux.getDataPort(muxGoPort);
			Bus inbufEnable = localEnablePort.getPeer();
			inbufEnable.setSize(1, true);
			inbufEnable.setIDLogical(resource.showIDLogical() + "_enable_" + i);

			Bus inbufData = localDataPort.getPeer();
			inbufData.setSize(resource.getInitWidth(), false);
			inbufData.setIDLogical(resource.showIDLogical() + "_write_" + i);

			// and wire them into the mux
			muxGoPort.setBus(inbufEnable);
			muxDataPort.setBus(inbufData);

			// and to the Or
			Port orDataPort = or.getDataPorts().get(i);
			orDataPort.setBus(inbufEnable);
		}

		// wire the mux and enable buses to the outbuf
		OutBuf outbuf = getExit(Exit.DONE).getPeer();
		Port enablePort = outbuf.getGoPort();
		enablePort.setBus(or.getResultBus());

		Port dataPort = outbuf.getDataPorts().get(0);
		dataPort.setBus(mux.getResultBus());
		dataPort.getPeer().setSize(resource.getInitWidth(), false);
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
		// ABK -- well, RegisterGateways get visited for now so that they can be
		// graphed
		// throw new
		// UnsupportedOperationException("RegisterGateway should not be visited - see Steve for details");
	}

	public Bus getGlobalEnableBus() {
		return getExit(Exit.DONE).getDoneBus();
	}

	public Bus getGlobalDataBus() {
		return getExit(Exit.DONE).getDataBuses().get(0);
	}

	public Bus getLocalEnableBus() {
		return getExit(Exit.DONE).getDataBuses().get(1);
	}

	public Bus getLocalDataBus() {
		return getExit(Exit.DONE).getDataBuses().get(2);
	}

	/**
	 * return a list of enable ports on the local side of the gateway
	 */
	public List<Port> getLocalEnablePorts() {
		return localEnablePorts;
	}

	public List<Port> getLocalDataPorts() {
		return localDataPorts;
	}

	/**
	 * Throws a CloneNotSupportedExcpetion or asserts false since there is, as
	 * yet, no implementation.
	 * 
	 * @return a value of type 'Object'
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		assert false : "tbd";

		throw new CloneNotSupportedException(
				"Clone not implemented for RegisterGateway yet");
	}

}
