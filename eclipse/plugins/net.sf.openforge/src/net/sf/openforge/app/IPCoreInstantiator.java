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

package net.sf.openforge.app;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.forge.api.ForgeApiException;
import net.sf.openforge.forge.api.internal.Core;
import net.sf.openforge.forge.api.internal.IPCoreStorage;
import net.sf.openforge.forge.api.pin.Buffer;
import net.sf.openforge.forge.api.pin.PinIn;
import net.sf.openforge.forge.api.pin.PinInOutTS;
import net.sf.openforge.forge.api.pin.PinOut;
import net.sf.openforge.forge.api.pin.PinOutTS;
import net.sf.openforge.lim.BidirectionalPin;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.InputPin;
import net.sf.openforge.lim.OutputPin;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.SimpleConstant;

/**
 * DEPRECATED CLASS! (IDM 08.14.2006)
 * 
 * IPCoreInstantiator is responsible for adding each IPCore module instantiation
 * to the design. Each instantiation's ports and buses connection is being
 * created also.
 * 
 * @author ysyu
 * @version $Id: IPCoreInstantiator.java 282 2006-08-14 21:25:33Z imiller $
 */
public class IPCoreInstantiator {

	private Design design = null;

	/** IPCoreStorage to IPCoreCall map */
	private final Map<IPCoreStorage, Call> ipCoreCallMap = new HashMap<IPCoreStorage, Call>(
			3);

	/** API Buffer to LIM Pin map */
	private final Map<Buffer, Pin> apiToLimPins = new HashMap<Buffer, Pin>(3);

	/**
	 * IPCoreStorage -> Collection of user accessed pins to the core tracked by
	 * that storage. This contains only those pins that the user actually
	 * accesses in their design.
	 */
	private final Map<IPCoreStorage, Collection<Buffer>> coreStorageToUserPins = new HashMap<IPCoreStorage, Collection<Buffer>>();

	/**
	 * Instantiates each ipcore module and ports & buses.
	 * 
	 * @param design
	 *            the whole design
	 */
	public IPCoreInstantiator(Design design) {
		this.design = design;
		// Core.clearIPCoreMap();
	}

	public void makeIPCore() {
		/** prep work */
		instantiate();
		checkPublishPinNames();

		/** make ip core connections */
		makeControl();
		makeDataPortsAndBuses();
		makePublishPins();
		makeNoConnectPins();
		makeUnusedPins();
		makeName();
	}

	/**
	 * 1. ip core pins will be put into each ip core's pin list 2. make ip core
	 * module instantiation 3. add each ip core call to the design
	 */
	private void instantiate() {
		for (Pin pin : design.getPins()) {

			final IPCoreStorage storage = Core
					.getIPCoreStorage(pin.getApiPin());
			if (storage != null) {
				Collection<Buffer> pins = coreStorageToUserPins.get(storage);
				if (pins == null) {
					pins = new HashSet<Buffer>();
					coreStorageToUserPins.put(storage, pins);
				}
				pins.add(pin.getApiPin());
			}
		}

		for (Pin pin : design.getPins()) {
			final IPCoreStorage storage = Core
					.getIPCoreStorage(pin.getApiPin());
			if (storage != null) {
				apiToLimPins.put(pin.getApiPin(), pin);

				/*
				 * Create ip core module instantiation and a dummy procedure to
				 * go with it
				 */
				Call call = ipCoreCallMap.get(storage);
				if (call == null) {
					Block block = new Block(
							Collections.<Component> emptyList(), true);
					final Procedure proc = new Procedure(block);
					call = new IPCoreCall(null);
					call.setProcedure(proc);
					ipCoreCallMap.put(storage, call);
				}
			}
		}

		for (IPCoreStorage ipcs : ipCoreCallMap.keySet()) {
			final IPCoreCall call = (IPCoreCall) ipCoreCallMap.get(ipcs);
			final Task core_task = new Task(call);
			design.addTask(core_task);
			if (ipcs.getHDLSource() != null) {
				design.addIncludeStatement(ipcs.getHDLSource());
			}
		}
	}

	private void makeControl() {
		// for(Iterator it = ipCoreCallMap.keySet().iterator(); it.hasNext();)
		// {
		// final IPCoreStorage ipcs = (IPCoreStorage)it.next();
		// final Call call = (Call)ipCoreCallMap.get(ipcs);
		// final HashMap clockMap=ipcs.getClockMap();
		// final Procedure procedure=call.getProcedure();

		// if(clockMap.size() > 0)
		// {
		// for (Iterator iter=clockMap.keySet().iterator(); iter.hasNext();)
		// {
		// String portName=(String) iter.next();
		// ClockPin apiClockPin=(ClockPin) clockMap.get(portName);
		// InputPin clockPin;
		// if (apiClockPin == IPCore.AUTOCONNECT.getClockPin())
		// {
		// clockPin = findStandinClock(ipcs);
		// }
		// else
		// {
		// clockPin = design.getClockPin(apiClockPin);
		// }
		// createPort(call, portName, clockPin.getBus(), 1);
		// }
		// }

		// final HashMap resetMap=ipcs.getResetMap();
		// if (resetMap.size() > 0)
		// {
		// for (Iterator iter=resetMap.keySet().iterator(); iter.hasNext();)
		// {
		// String portName=(String) iter.next();
		// ResetPin apiResetPin=(ResetPin) resetMap.get(portName);
		// InputPin resetPin;
		// if (apiResetPin == IPCore.AUTOCONNECT.getResetPin())
		// {
		// resetPin = findStandinReset(ipcs);
		// }
		// else
		// {
		// resetPin=design.getResetPin(apiResetPin);
		// }
		// createPort(call, portName, resetPin.getBus(), 1);
		// }
		// }
		// }
	}

	@SuppressWarnings("unused")
	private static void createPort(Call call, String portName, Bus bus,
			int width) {
		Port port = call.makeDataPort();
		port.setIDLogical(portName);
		port.setBus(bus);
		port.setUsed(true);
		final Port bodyPort = call.getProcedure().getBody().makeDataPort();
		bodyPort.getPeer().setIDLogical(portName);
	}

	/**
	 * The clock pin to use in place of the AUTOCONNECT domains clock is the
	 * clock that is associated with each data pin connected to the core.
	 * 
	 * @param ipcs
	 *            a value of type 'IPCoreStorage'
	 * @return a value of type 'InputPin'
	 */
	@SuppressWarnings("unused")
	private InputPin findStandinClock(IPCoreStorage ipcs) {
		InputPin ckPin = null;
		// // Look at all the pins and see what clock pin/reset pin
		// // they have
		// for (Iterator iter = ipcs.getAllPins().iterator(); iter.hasNext();)
		// {
		// Buffer buf = (Buffer)iter.next();
		// Pin pin = (Pin)apiToLimPins.get(buf);
		// if (pin == null || pin.getClockPin() == null)
		// {
		// // The pin may not have a clock pin associated if the
		// // user never accesses a pin they create. There must
		// // be another, however that they do access. Also, the
		// // LIM pin may be null if the api pin was not accessed.
		// continue;
		// }

		// if (ckPin == null)
		// {
		// ckPin = pin.getClockPin();
		// }
		// else
		// {
		// if (ckPin != pin.getClockPin())
		// {
		// EngineThread.getEngine().fatalError(
		// "Cannot use stand in clock domain for core '" +
		// ipcs.getModuleName() + "' because it is accessed "+
		// "by entry methods in more than 1 clock " +
		// "domain.  Domain 1: " + ckPin.getApiPin().getName() +
		// " Domain 2: " +
		// pin.getClockPin().getApiPin().getName());
		// }
		// }
		// }
		// assert ckPin != null;
		return ckPin;
	}

	/**
	 * The reset pin to use in place of the AUTOCONNECT domains reset is the
	 * reset that is associated with each data pin connected to the core.
	 * 
	 * @param ipcs
	 *            a value of type 'IPCoreStorage'
	 * @return a value of type 'InputPin'
	 */
	@SuppressWarnings("unused")
	private InputPin findStandinReset(IPCoreStorage ipcs) {
		InputPin rstPin = null;
		// for (Iterator iter = ipcs.getAllPins().iterator(); iter.hasNext();)
		// {
		// Buffer buf = (Buffer)iter.next();
		// Pin pin = (Pin)apiToLimPins.get(buf);
		// if (pin == null || pin.getResetPin() == null)
		// {
		// // The pin may not have a reset pin associated if the
		// // user never accesses a pin they create. There must
		// // be another, however that they do access. Also, not
		// // all pins the user declares may be accessed (thus
		// // the LIM pin is null)
		// continue;
		// }
		// if (rstPin == null)
		// {
		// rstPin = pin.getResetPin();
		// }
		// else
		// {
		// if (rstPin != pin.getResetPin())
		// {
		// EngineThread.getEngine().fatalError(
		// "Cannot use stand in clock domain for core '" +
		// ipcs.getModuleName() + "' because it is accessed "+
		// "by entry methods in more than 1 clock " +
		// "domain (Resets are different).  Domain 1: " +
		// rstPin.getApiPin().getName() + " Domain 2: " +
		// pin.getResetPin().getApiPin().getName());
		// }
		// }
		// }
		// assert rstPin != null;
		return rstPin;
	}

	/**
	 * Naming
	 */
	private void makeName() {
		for (IPCoreStorage ipcs : ipCoreCallMap.keySet()) {
			final Call call = ipCoreCallMap.get(ipcs);

			if (ipcs.getHDLInstanceName() == null) {
				call.setIDLogical(ipcs.getModuleName() + "_instance");
			} else {
				call.setIDLogical(ipcs.getModuleName() + "_"
						+ ipcs.getHDLInstanceName());
			}

			/*
			 * Name procedure ports & buses
			 */
			@SuppressWarnings("unused")
			final Block block = call.getProcedure().getBody();
			call.getProcedure().setIDLogical(ipcs.getModuleName());
		}
	}

	/**
	 * creates neccessary inputs and outputs for the ip core
	 */
	private void makeDataPortsAndBuses() {
		for (IPCoreStorage ipcs : ipCoreCallMap.keySet()) {
			final IPCoreCall call = (IPCoreCall) ipCoreCallMap.get(ipcs);
			final Procedure proc = call.getProcedure();
			Collection<Buffer> userPins = coreStorageToUserPins.get(ipcs);

			for (Buffer buffer : userPins) {
				final String name = buffer.getName();
				// final int width = buffer.getSize();
				final Pin pin = apiToLimPins.get(buffer);

				if ((pin instanceof InputPin)) {
					// make a port on both call and procedure
					final Port callPort = call.makeDataPort();
					final Port procPort = proc.getBody().makeDataPort();

					// connecct the result bus of the pin with the
					// port on ipcore call.
					callPort.setBus(pin.getInPinBuf().getPhysicalComponent()
							.getResultBus());
					procPort.setUsed(true);
					callPort.setUsed(true);

					// name the procedure port with the name of ipcore
					// user pin.
					callPort.setIDLogical(name);
					procPort.setIDLogical(name);
					procPort.getPeer().setIDLogical(name);
				} else if ((pin instanceof OutputPin)
						|| (pin instanceof BidirectionalPin)) {
					// This is backwards! The output pin is an output
					// from the design and an INPUT to the core! This
					// was fixed in K3_2 branch but the merge created
					// errors in compilation which were not
					// resolved. (Thus I am leaving you this note)
					assert false : "Unsupported operation on IP Cores";
					// // make a bus on both call and procedure
					// final Bus callBus =
					// call.getExit(Exit.DONE).makeDataBus();
					// final Bus procBus =
					// proc.getBody().getExit(Exit.RETURN).makeDataBus();

					// // connecct the ipcore call bus with the input
					// // port of the pin.
					// pin.getOutPinBuf().getPhysicalComponent().getNowValuePort().setBus(callBus);
					// procBus.setUsed(true);
					// callBus.setUsed(true);

					// // name the bus on procedure and call with the name of
					// ipcore
					// // user pin.
					// callBus.setIDLogical(name);
					// procBus.setIDLogical(name);
				}
			}

			// associates a procedure's ports and buses with the
			// related ports and buses on the call.
			call.updateNotify();
		}
	}

	/**
	 * Publishes ip core pins up to the design
	 */
	private void makePublishPins() {
		for (IPCoreStorage ipcs : ipCoreCallMap.keySet()) {
			final IPCoreCall call = (IPCoreCall) ipCoreCallMap.get(ipcs);
			final Procedure proc = call.getProcedure();

			Iterator<String> pni = ipcs.getPublishedNames().iterator();
			for (Buffer buffer : ipcs.getPublishedPins()) {
				final int width = buffer.getSize();
				final String publish_name = pni.next();

				if (buffer instanceof PinIn) {
					final Bus bus = call.getExits().iterator().next()
							.makeDataBus();
					bus.setUsed(true);

					final Exit exit = proc.getBody().getExits().iterator()
							.next();
					final Bus procBus = exit.makeDataBus();
					procBus.setIDLogical(buffer.getName());
					procBus.setUsed(true);

					OutputPin out = new OutputPin(width, buffer.isSigned());
					out.setIDLogical(publish_name);
					out.getPort().setBus(bus);
					out.setApiPin(buffer);
					design.addOutputPin(out);
				}
				if (buffer instanceof PinOut || buffer instanceof PinOutTS) {
					final InputPin in = new InputPin(width, buffer.isSigned());
					in.setIDLogical(publish_name);
					in.setApiPin(buffer);
					final Port port = call.makeDataPort();
					port.setBus(in.getBus());
					port.setUsed(true);

					final Port bport = proc.getBody().makeDataPort();
					bport.getPeer().setIDLogical(buffer.getName());
					design.addInputPin(in);
				}
				if (buffer instanceof PinInOutTS) {
					final Bus bus = call.getExits().iterator().next()
							.makeDataBus();
					bus.setUsed(true);

					final Exit exit = proc.getBody().getExits().iterator()
							.next();
					final Bus procBus = exit.makeDataBus();
					procBus.setIDLogical(buffer.getName());
					procBus.setUsed(true);

					final BidirectionalPin io = new BidirectionalPin(width,
							buffer.isSigned());
					io.setIDLogical(publish_name);
					io.getPort().setBus(bus);
					io.setApiPin(buffer);

					throw new IllegalStateException(
							"No longer supports bidirectional pins");
					// design.addBidirectionalPin(io);
				}
			}
			call.setProcedure(proc);
		}
	}

	/**
	 * make no connect pins
	 */
	private void makeNoConnectPins() {
		for (IPCoreStorage ipcs : ipCoreCallMap.keySet()) {
			final IPCoreCall call = (IPCoreCall) ipCoreCallMap.get(ipcs);
			final Procedure proc = call.getProcedure();

			for (Buffer buffer : ipcs.getNoConnectPins()) {
				final int width = buffer.getSize();

				/** we need a dummy bus, so why not from a Pin? */
				final InputPin in = new InputPin(width, buffer.isSigned());
				final Port port = call.makeDataPort();
				port.setBus(in.getBus());
				port.setUsed(true);

				final Port bport = proc.getBody().makeDataPort();
				bport.getPeer().setIDLogical(buffer.getName());
				call.addNoConnectPort(port);
			}
			call.setProcedure(proc);
		}
	}

	/**
	 * Creates user declared pins but not used in the design
	 * 
	 * PinIn => no connect pin PinOut => feed it with its reset value
	 */
	private void makeUnusedPins() {
		for (IPCoreStorage ipcs : ipCoreCallMap.keySet()) {
			final IPCoreCall call = (IPCoreCall) ipCoreCallMap.get(ipcs);
			final Procedure proc = call.getProcedure();
			Collection<Buffer> userPins = coreStorageToUserPins.get(ipcs);
			for (Buffer buffer : ipcs.getAllPins()) {

				if (userPins.contains(buffer)
						|| ipcs.getPublishedPins().contains(buffer)
						|| ipcs.getNoConnectPins().contains(buffer)) {
					continue;
				}

				final int width = buffer.getSize();

				if (buffer instanceof PinIn || buffer instanceof PinOutTS
						|| buffer instanceof PinInOutTS) {
					/** we need a dummy bus, so why not from a Pin? */
					final InputPin in = new InputPin(width, true);

					final Port port = call.makeDataPort();
					port.setSize(width, false);
					port.setBus(in.getBus());
					port.setUsed(true);

					final Port bport = proc.getBody().makeDataPort();
					bport.getPeer().setIDLogical(buffer.getName());
					call.addNoConnectPort(port);
				}
				if (buffer instanceof PinOut) {
					Constant constant = new SimpleConstant(
							buffer.getResetValue(), buffer.getSize(), true);
					constant.getValueBus().setUsed(true);

					final Port port = call.makeDataPort();
					port.setUsed(true);

					final Port bport = proc.getBody().makeDataPort();
					bport.getPeer().setIDLogical(buffer.getName());
					bport.setSize(width, false);
					bport.getPeer().setSize(width, false);
				}
			}
			call.setProcedure(proc);
		}
	}

	/**
	 * Each IP Core published pin should also have the unique name in the user
	 * application. All the published name will be checked againest all the
	 * names of pins on user application.
	 */
	private void checkPublishPinNames() {
		Set<String> names = new HashSet<String>();
		for (IPCoreStorage storage : Core.getIPCoreStorages()) {
			// Check for duplicated name
			for (String pname : storage.getPublishedNames()) {
				if (!names.add(pname)) {
					throw new ForgeApiException(
							"Duplicate Publish Pin Name Found: " + pname);
				}
			}
		}
	}
}
