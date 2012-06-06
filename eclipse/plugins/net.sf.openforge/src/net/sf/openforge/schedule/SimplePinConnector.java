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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.RegisterGateway;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.UntilBody;
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
import net.sf.openforge.lim.memory.MemoryGateway;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.op.OrOpMulti;
import net.sf.openforge.util.naming.ID;

/**
 * SimplePinConnector traverses the LIM and finds all accesses to any
 * {@link SimplePin} objects. These accesses are then routed to the SimplePin by
 * simple merging of any write accesses (via a simple OrOpMulti) and via simple
 * wired connections for read accesses.
 * 
 * <p>
 * Created: Wed Jan 21 09:53:01 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimplePinConnector.java 88 2006-01-11 22:39:52Z imiller $
 */
public class SimplePinConnector extends DefaultVisitor {

	private Frame currentFrame;
	private Stack<Frame> frameStack = new Stack<Frame>();

	private List<Frame> taskFrames;

	public SimplePinConnector() {
	}

	@Override
	public void visit(Design des) {
		taskFrames = new ArrayList<Frame>();

		super.visit(des);

		// For every pin in the design, find all writers and arbitrate
		// them appropriately into the pin
		final Map<SimplePin, Set<Bus>> pinToWriters = new HashMap<SimplePin, Set<Bus>>();
		for (Frame frame : taskFrames) {
			for (SimplePin pin : frame.getWritePins()) {
				Set<Bus> writers = pinToWriters.get(pin);
				if (writers == null) {
					writers = new LinkedHashSet<Bus>();
					pinToWriters.put(pin, writers);
				}
				writers.addAll(frame.writeBuses.get(pin));
			}
		}
		for (SimplePin pin : pinToWriters.keySet()) {
			Set<Bus> writeBuses = pinToWriters.get(pin);
			Bus result;
			if (writeBuses.size() < 2) {
				result = writeBuses.iterator().next();
			} else {
				final boolean doSimpleArbiter = EngineThread.getGenericJob()
						.getUnscopedBooleanOptionValue(
								OptionRegistry.SIMPLE_STATE_ARBITRATION);
				if (!doSimpleArbiter) {
					throw new IllegalStateException(
							"Pin writes from multiple tasks is only supported with simple arbitration.");
				}

				Or or = new Or(writeBuses.size());
				Iterator<Port> portIter = or.getDataPorts().iterator();
				for (Bus bus : writeBuses) {
					portIter.next().setBus(bus);
				}
				des.addComponentToDesign(or);
				result = or.getResultBus();
			}

			pin.connectPort(result);
		}

		// With XLIM, it is now possible to have SimplePinRead and
		// SimplePinWrite objects at the design level. These need to
		// be directly connected to the appropriate pins. It is an
		// error if there is a pinwrite to a pin which has another
		// writer (either task or another pin write at this level).
		for (Component comp : des.getDesignModule().getComponents()) {
			if (comp instanceof SimplePinRead) {
				SimplePinRead read = (SimplePinRead) comp;
				SimplePin pin = (SimplePin) read.getReferenceable();
				Port port = getReadPort(read);
				pin.connectBus(Collections.singleton(port));
			} else if (comp instanceof SimplePinWrite) {
				SimplePinWrite write = (SimplePinWrite) comp;
				SimplePin pin = (SimplePin) write.getReferenceable();
				Bus bus = getWriteBus(write);
				pin.connectPort(bus);
				assert !write.getGoPort().isConnected();
				assert write.getDataPort().isConnected();
				write.getGoPort().setBus(
						write.getDataPort().getBus().getOwner().getDoneBus());
			}
		}
	}

	@Override
	public void visit(Task task) {
		// Create a new frame to work from. This assumes that only 1
		// task will ever access a given SimplePin
		currentFrame = new Frame();
		super.visit(task);

		taskFrames.add(currentFrame);

		for (SimplePin pin : currentFrame.getReadPins()) {
			// here we actually need to refer to them as simple pins.
			Collection<Port> readPorts = currentFrame.readPorts.get(pin);
			pin.connectBus(readPorts);
		}
		/*
		 * for (Iterator iter = this.currentFrame.getWritePins().iterator();
		 * iter.hasNext();) { final SimplePin pin = (SimplePin)iter.next(); //
		 * The connectPort method will ensure that there is only // one write
		 * bus coming from the task. It should be true // since the only thing
		 * in a task is a call which should // merge multiple contained writers
		 * for us. Collection buses =
		 * (Collection)this.currentFrame.writeBuses.get(pin); for (Iterator
		 * busIter = buses.iterator(); busIter.hasNext();) {
		 * pin.connectPort((Bus)busIter.next()); } }
		 */
	}

	@Override
	public void visit(Call call) {
		handleCall(call);
	}

	@Override
	public void visit(SimplePinRead read) {
		// final Port port = (Port)read.getDataPorts().get(0);
		final Port port = getReadPort(read);
		currentFrame.addReadPort(read.getReferenceable(), port);
	}

	private Port getReadPort(SimplePinRead read) {
		assert read.getDataPorts().isEmpty();
		final Port port = read.makeDataPort(Component.SIDEBAND);
		port.setUsed(true);
		return port;
	}

	@Override
	public void visit(SimplePinWrite write) {
		final Bus bus = getWriteBus(write);
		currentFrame.addWriteBus(write.getReferenceable(), bus);
	}

	public Bus getWriteBus(SimplePinWrite write) {
		assert write.getExit(Exit.DONE).getDataBuses().isEmpty();
		final Bus bus = write.getExit(Exit.DONE)
				.makeDataBus(Component.SIDEBAND);
		bus.setUsed(true);
		return bus;
	}

	//
	// For each type of Module in the Visitor interface provide a
	// visit method that enters the Module, traverses it, and exits
	// it.
	//

	@Override
	public void visit(AbsoluteMemoryRead mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(AbsoluteMemoryWrite mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(ArrayRead mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(ArrayWrite mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(Block mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(Branch mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(Decision mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(TaskCall mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(SimplePinAccess mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(FifoAccess mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(FifoRead mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(FifoWrite mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(ForBody mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(HeapRead mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(HeapWrite mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(Kicker mod) // even though it never has pin accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(Latch mod) // even though it never has pin accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(Loop mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(MemoryGateway mod) // even though it never has pin
											// accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(MemoryReferee mod) // even though it never has pin
											// accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(PinReferee mod) // even though it never has pin accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(PriorityMux mod) // even though it never has pin accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(RegisterGateway mod) // even though it never has pin
											// accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(RegisterReferee mod) // even though it never has pin
											// accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(Scoreboard mod) // even though it never has pin accesses
	{
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(Switch mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(UntilBody mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	@Override
	public void visit(WhileBody mod) {
		enterModule();
		super.visit(mod);
		exitModule(mod);
	}

	private void enterModule() {
		frameStack.push(currentFrame);
		currentFrame = new Frame();
	}

	private void exitModule(Module mod) {
		Frame parentFrame = frameStack.pop();

		// For each read pin create a new port on the module. Tie the
		// peer bus to each read port.
		for (SimplePin pin : currentFrame.getReadPins()) {
			Referenceable readPin = pin;

			Port readPort = mod.makeDataPort(Component.SIDEBAND);
			parentFrame.addReadPort(readPin, readPort);

			List<Port> pinPorts = currentFrame.readPorts.get(readPin);
			for (Port port : pinPorts) {
				port.setBus(readPort.getPeer());
			}
		}

		// For each write pin create a new bus on the module's
		// sideband exit. Tie the write buses together via an
		// OrOpMulti and tie that to the module bus peer.
		for (SimplePin pin : currentFrame.getWritePins()) {
			Referenceable writePin = pin;

			Exit exit = mod.getExit(Exit.SIDEBAND);
			if (exit == null) {
				exit = mod.makeExit(0, Exit.SIDEBAND);
			}

			// We can use an OrOpMulti to merge the write data because
			// each SimplePinWrite masks the data that it is sending
			// with its GO port. Thus, unless actively sending data
			// the SimplePinWrite will be sending all zeros.
			// Scheduling will ensure that we dont have multiple
			// SimplePinWrites to the same pin active in the same cycle.
			OrOpMulti or = new OrOpMulti();
			or.setIDLogical(ID.showLogical(writePin) + "_merge");
			mod.addComponent(or);

			Bus writeBus = exit.makeDataBus(Component.SIDEBAND);
			writeBus.getPeer().setBus(or.getResultBus());
			parentFrame.addWriteBus(writePin, writeBus);

			List<Bus> pinBuses = currentFrame.writeBuses.get(writePin);
			for (Bus bus : pinBuses) {
				or.makeDataPort().setBus(bus);
			}
		}

		currentFrame = parentFrame;
	}

	public void handleCall(Call call) {
		if (call.getProcedure() == null) {
			EngineThread.getGenericJob().warn(
					"Unexpected internal structure when connecting pins");
			return; // do nothing???
		}

		Frame parentFrame = currentFrame;
		currentFrame = new Frame();

		// Visit the block so that all its accesses can be
		// trickled up.
		call.getProcedure().getBody().accept(this);

		// For each port and bus in the frame create a corresponding
		// port and bus on the call.
		for (SimplePin p : currentFrame.getReadPins()) {
			Referenceable pin = p;

			// There must be only one port for that pin on the procedure
			assert currentFrame.readPorts.get(pin).size() == 1;
			Port procPort = currentFrame.readPorts.get(pin).iterator().next();

			// Create the Call port
			Port callPort = call.makeDataPort(Component.SIDEBAND);
			parentFrame.addReadPort(pin, callPort);

			// Map them
			call.setProcedurePort(callPort, procPort);
		}

		for (SimplePin p : currentFrame.getWritePins()) {
			Referenceable pin = p;

			// There must be only one bus for that pin on the procedure
			assert currentFrame.writeBuses.get(pin).size() == 1;
			Bus procBus = currentFrame.writeBuses.get(pin).iterator().next();

			// Create the Call bus
			Exit callExit = call.getExit(Exit.SIDEBAND);
			if (callExit == null) {
				callExit = call.makeExit(0, Exit.SIDEBAND);
				call.setProcedureBus(callExit.getDoneBus(), procBus.getOwner()
						.getDoneBus());
			}
			Bus callBus = callExit.makeDataBus(Component.SIDEBAND);
			parentFrame.addWriteBus(pin, callBus);

			// Map them
			call.setProcedureBus(callBus, procBus);
		}

		// Restore the state
		currentFrame = parentFrame;
	}

	class Frame {
		private Map<SimplePin, List<Port>> readPorts = new HashMap<SimplePin, List<Port>>();
		private Map<SimplePin, List<Bus>> writeBuses = new HashMap<SimplePin, List<Bus>>();

		public void addReadPort(Referenceable pin, Port port) {
			List<Port> ports = readPorts.get(pin);
			if (ports == null) {
				ports = new ArrayList<Port>();
				readPorts.put((SimplePin) pin, ports);
			}
			ports.add(port);
		}

		public void addWriteBus(Referenceable pin, Bus bus) {
			List<Bus> buses = writeBuses.get(pin);
			if (buses == null) {
				buses = new ArrayList<Bus>();
				writeBuses.put((SimplePin) pin, buses);
			}
			buses.add(bus);
		}

		public Collection<SimplePin> getReadPins() {
			return readPorts.keySet();
		}

		public Collection<SimplePin> getWritePins() {
			return writeBuses.keySet();
		}
	}

}// SimplePinConnector
