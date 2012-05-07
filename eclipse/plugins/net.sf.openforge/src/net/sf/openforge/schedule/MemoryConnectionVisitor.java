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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.RegisterGateway;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.Resource;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.MemoryAccess;
import net.sf.openforge.lim.memory.MemoryGateway;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.memory.MemoryWrite;
import net.sf.openforge.lim.memory.StructuralMemory;
import net.sf.openforge.util.naming.ID;

/**
 * MemoryConnectionVisitor is responsible for traversing the LIM and
 * establishing all the sideband connections necessary for attaching each
 * {@link MemoryAccess} to the backing structural memory. This involves
 * generating a {@link MemoryGateway} at each module level, and a
 * {@link MemoryReferee} at the top level to interface with the targetted memory
 * port.
 * 
 * <p>
 * Created: Tue Mar 11 11:32:43 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryConnectionVisitor.java 282 2006-08-14 21:25:33Z imiller $
 */
public class MemoryConnectionVisitor extends DefaultVisitor {

	private Stack accessFrames = new Stack();
	private Frame currentFrame = new Frame();
	private Map designResourceBundles = new HashMap();

	public MemoryConnectionVisitor() {
	}

	/**
	 * Visits the design, generating MemoryGateways at each module, then
	 * connecting up the top level accesses to a memory referee.
	 */
	@Override
	public void visit(Design design) {
		super.visit(design);

		for (Iterator memories = design.getLogicalMemories().iterator(); memories
				.hasNext();) {
			LogicalMemory memory = (LogicalMemory) memories.next();
			StructuralMemory structMem = memory.getStructuralMemory();

			// InputPin clock = null;
			// InputPin reset = null;

			for (Iterator memports = memory.getLogicalMemoryPorts().iterator(); memports
					.hasNext();) {
				LogicalMemoryPort memport = (LogicalMemoryPort) memports.next();
				/*
				 * query the memory port for clock and reset. If different ports
				 * have different clock or reset then fatally err out.
				 */
				// InputPin newClock = memport.getClockPin();
				// InputPin newReset = memport.getResetPin();

				// if ((clock != null && newClock != clock) || (reset != null &&
				// newReset != reset))
				// {
				// EngineThread.getEngine().fatalError("Memory "+memory+" spans clock domains.  Clocks: "+ID.showLogical(clock)+" and "+ID.showLogical(newClock)
				// +" Resets: "+ID.showLogical(reset)+" and "+ID.showLogical(newReset));
				// }
				// else
				// {
				// clock=newClock;
				// reset=newReset;
				// }

				// memport.setClockPin(clock);
				// memport.setResetPin(reset);

				connectMemoryPort(memport, structMem, design);
			}

			// structMem.getClockPort().setBus(clock.getBus());
			// structMem.getResetPort().setBus(reset.getBus());
			structMem.getClockPort().setUsed(true);
			structMem.getResetPort().setUsed(true);
			structMem.setConsumesReset(true);
		}
	}

	private void connectMemoryPort(LogicalMemoryPort memport,
			StructuralMemory structMem, Design design) {
		StructuralMemory.StructuralMemoryPort structPort = structMem
				.getStructuralMemoryPort(memport);

		// The bundle contains all the read and write accesses for a
		// given resource (memory port). The list of reads and list
		// of writes must be the same length and each position
		// represents 1 accessing task. The value stored in each
		// read/write list at each position will be either a MemAccess
		// or null if that task did not read or write.
		ResourceBundle bundle = (ResourceBundle) designResourceBundles
				.get(memport);

		if (bundle == null)
			return;
		if (bundle.getReads().isEmpty() && bundle.getWrites().isEmpty())
			return;

		assert bundle.getReads().size() == bundle.getWrites().size();
		MemoryReferee referee = memport.makePhysicalComponent(
				bundle.getReads(), bundle.getWrites());
		// Clock and reset will be handled along with all other design
		// entities during the global connector
		// referee.getClockPort().setBus(memport.getClockPin().getBus());
		// referee.getResetPort().setBus(memport.getResetPin().getBus());
		design.addComponentToDesign(referee);
		design.addComponentToDesign(structMem);

		// We currently support only 1 clock per memory.
		// If multiple clocks are desired, create clock/reset ports
		// per StructuralMemoryPort and connect here.
		referee.connectImplementation(structPort);

		for (int i = 0; i < referee.getTaskSlots().size(); i++) {
			MemoryReferee.TaskSlot slot = referee.getTaskSlots().get(i);
			if (bundle.getReads().get(i) != null) {
				((MemAccess) bundle.getReads().get(i)).connect(slot);
			}

			if (bundle.getWrites().get(i) != null) {
				((MemAccess) bundle.getWrites().get(i)).connect(slot);
			}
		}
	}

	/**
	 * Traverses the task and then stores the top level accesses (if any) into
	 * the resource bundle for each targetted resource.
	 */
	@Override
	public void visit(Task task) {
		// Create a unique frame for the task to collect its data in.
		final Frame superFrame = currentFrame;
		currentFrame = new Frame();

		super.visit(task);

		// Add any read and/or write access to the read/write list for
		// each resource.
		for (Iterator iter = currentFrame.getResources().iterator(); iter
				.hasNext();) {
			Resource res = (Resource) iter.next();
			ResourceBundle bundle = currentFrame.getBundle(res);
			assert bundle.getReads().size() < 2;
			assert bundle.getWrites().size() < 2;

			if (bundle.getReads().isEmpty() && bundle.getWrites().isEmpty()) {
				continue;
			}

			ResourceBundle designBundle = (ResourceBundle) designResourceBundles
					.get(res);
			if (designBundle == null) {
				designBundle = new ResourceBundle(res);
				designResourceBundles.put(res, designBundle);
			}
			if (bundle.getReads().size() > 0)
				designBundle.addRead((MemAccess) bundle.getReads().get(0));
			else
				designBundle.addRead(null);
			if (bundle.getWrites().size() > 0)
				designBundle.addWrite((MemAccess) bundle.getWrites().get(0));
			else
				designBundle.addWrite(null);
		}

		currentFrame = superFrame;
	}

	/**
	 * Traverse the call, making special effort to ensure that the accesses that
	 * percolated up to the procedure, get duplicated on the call.
	 */
	@Override
	public void visit(Call call) {
		// Create a frame within our frame so that we can see just
		// what was added to the procedure block.
		Frame superFrame = currentFrame;
		currentFrame = new Frame();
		super.visit(call);

		// Map duplication = new HashMap();
		for (Iterator iter = currentFrame.getAllAccesses().iterator(); iter
				.hasNext();) {
			MemAccess acc = (MemAccess) iter.next();
			// MemAccess pushed = acc.pushAcrossCall(call, duplication);
			MemAccess pushed = acc.pushAcrossCall(call);
			superFrame.addAccess(pushed);
		}

		// Restore the current frame now that we've populated it with
		// accesses pulled out from the procedure to the call.
		currentFrame = superFrame;

		for (Iterator iter = call.getBuses().iterator(); iter.hasNext();) {
			Bus callBus = (Bus) iter.next();
			Bus procBus = call.getProcedureBus(callBus);
		}
	}

	/**
	 * Record the memory access.
	 */
	@Override
	public void visit(MemoryRead access) {
		super.visit(access);
		access.getOwner().addComponent(access.makePhysicalComponent());
		currentFrame.addAccess(new MemReadAccess(access));
	}

	/**
	 * Record the memory access.
	 */
	@Override
	public void visit(MemoryWrite access) {
		super.visit(access);
		access.getOwner().addComponent(access.makePhysicalComponent());
		currentFrame.addAccess(new MemWriteAccess(access));
	}

	//
	// All modules must be entered and then exited to maintain scoping
	// frames
	//

	@Override
	public void visit(AbsoluteMemoryRead module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(AbsoluteMemoryWrite module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(ArrayRead module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(ArrayWrite module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(Block module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(Branch module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(Decision module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(SimplePinAccess module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(FifoAccess module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(FifoRead module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(FifoWrite module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(ForBody module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(HeapRead module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(HeapWrite module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(Kicker module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(Latch module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(MemoryGateway module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(MemoryReferee module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(PinReferee module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(PriorityMux module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(RegisterGateway module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(RegisterReferee module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(Scoreboard module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(Switch module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(UntilBody module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(WhileBody module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	@Override
	public void visit(Loop module) {
		enterModule();
		super.visit(module);
		exitModule(module);
	}

	private void enterModule() {
		accessFrames.push(currentFrame);
		currentFrame = new Frame();
	}

	/**
	 * Pop the frame, create a gateway for each resource accessed in the
	 * traversed module, and create accesses in the current frame for the global
	 * side of each gateway from the subframe.
	 */
	private void exitModule(Module module) {
		Frame frame = currentFrame;
		currentFrame = (Frame) accessFrames.pop();

		for (Iterator iter = frame.getResources().iterator(); iter.hasNext();) {
			final LogicalMemoryPort memoryPort = (LogicalMemoryPort) iter
					.next();
			ResourceBundle bundle = frame.getBundle(memoryPort);
			List reads = bundle.getReads();
			List writes = bundle.getWrites();

			MemoryGateway gateway = new MemoryGateway(memoryPort, reads.size(),
					writes.size(), memoryPort.getMaxAddressWidth());
			module.addComponent(gateway);

			for (int i = 0; i < reads.size(); i++) {
				((MemAccess) reads.get(i)).connect(gateway, i);
			}

			for (int i = 0; i < writes.size(); i++) {
				((MemAccess) writes.get(i)).connect(gateway, i);
			}

			Map duplicate = new HashMap();

			if (reads.size() > 0) {
				currentFrame.addAccess(new MemReadAccess(gateway, module,
						duplicate));
			}

			if (writes.size() > 0) {
				currentFrame.addAccess(new MemWriteAccess(gateway, module,
						duplicate));
			}
		}
	}

	/**
	 * A Frame is a scoping unit for tracking accesses. It is used to track all
	 * accesses to all resources in a given scope. The accesses are stored in a
	 * map of {@link Resource} to ResourceBundle.
	 */
	private static class Frame {
		/** A map of Resource (memory port) to ResourceBundle. */
		private Map resourceToBundle = new HashMap();

		/**
		 * Adds a MemAccess to this frame.
		 */
		public void addAccess(MemAccess acc) {
			ResourceBundle bundle = (ResourceBundle) resourceToBundle.get(acc
					.getTarget());
			if (bundle == null) {
				bundle = new ResourceBundle(acc.getTarget());
				resourceToBundle.put(acc.getTarget(), bundle);
			}

			if (acc.isRead())
				bundle.addRead(acc);
			else
				bundle.addWrite(acc);
		}

		/**
		 * Retrieve the ResourceBundle for a given Resource.
		 */
		public ResourceBundle getBundle(Resource target) {
			return (ResourceBundle) resourceToBundle.get(target);
		}

		/**
		 * Retrieve all the Resoruces tracked in this frame.
		 */
		public Set getResources() {
			return resourceToBundle.keySet();
		}

		/**
		 * Retrieve every MemAccess allocated in this frame.
		 */
		public Set getAllAccesses() {
			Set accesses = new HashSet();
			for (Iterator iter = resourceToBundle.values().iterator(); iter
					.hasNext();) {
				ResourceBundle bundle = (ResourceBundle) iter.next();
				accesses.addAll(bundle.getReads());
				accesses.addAll(bundle.getWrites());
			}
			return accesses;
		}
	}

	/**
	 * A ResourceBundle tracks all read and write accesses to a given Resource.
	 */
	private static class ResourceBundle {
		/** A List of MemAccess objects */
		private List reads = new ArrayList();
		/** A List of MemAccess objects */
		private List writes = new ArrayList();
		private Resource target;

		public ResourceBundle(Resource target) {
			this.target = target;
		}

		/** acc MAY be null @ task level */
		public void addRead(MemAccess acc) {
			reads.add(acc);
		}

		/** acc MAY be null @ task level */
		public void addWrite(MemAccess acc) {
			writes.add(acc);
		}

		/**
		 * Retrieves a List of MemAccess objects, each of which is a read access
		 */
		public List getReads() {
			return reads;
		}

		/**
		 * Retrieves a List of MemAccess objects, each of which is a write
		 * access
		 */
		public List getWrites() {
			return writes;
		}

		public int getAccessCount() {
			return reads.size() + writes.size();
		}
	}

	/**
	 * MemAccess is a class which ties together all the ports and buses related
	 * to a single access of a given resource.
	 */
	private static abstract class MemAccess {
		private List ports = new ArrayList(7);
		private List buses = new ArrayList(7);
		private boolean isRead = false;
		private Resource target;

		protected MemAccess(boolean read, Resource target) {
			isRead = read;
			this.target = target;
		}

		public boolean isRead() {
			return isRead;
		}

		public Resource getTarget() {
			return target;
		}

		protected void addPort(Port p) {
			ports.add(p);
		}

		protected void addBus(Bus b) {
			buses.add(b);
		}

		protected Port getPort(int index) {
			return (Port) ports.get(index);
		}

		protected Bus getBus(int index) {
			return (Bus) buses.get(index);
		}

		/**
		 * Copies the ports and buses (creates new sideband ones) of this
		 * MemAccess to the specified call.
		 * 
		 * @param call
		 *            the {@link Call} to which this access needs to be copied.
		 * @return a MemAccess tying together the duplicated ports/buses.
		 */
		public MemAccess pushAcrossCall(Call call) {
			MemAccess copy = copy();
			for (Iterator iter = ports.iterator(); iter.hasNext();) {
				Port orig = (Port) iter.next();
				// Port cPort = (Port)duplicateMap.get(orig);
				Port cPort = call.getPortFromProcedurePort(orig);
				if (cPort == null) {
					cPort = call.makeDataPort(Component.SIDEBAND);
					call.setProcedurePort(cPort, orig);
				}
				copy.addPort(cPort);
			}

			for (Iterator iter = buses.iterator(); iter.hasNext();) {
				Bus orig = (Bus) iter.next();
				// Bus cBus = (Bus)duplicateMap.get(orig);
				Bus cBus = call.getBusFromProcedureBus(orig);

				if (cBus == null) {
					Exit exit = call.getExit(Exit.SIDEBAND);
					if (exit == null) {
						exit = call.makeExit(0, Exit.SIDEBAND);

						/*
						 * Don't forget to map the DONE Buses, which get created
						 * automatically. Even though they may not be used, the
						 * association should still exist.
						 */
						call.setProcedureBus(exit.getDoneBus(), orig.getOwner()
								.getDoneBus());
					}
					cBus = exit.makeDataBus(Component.SIDEBAND);
					cBus.setIDLogical(ID.showLogical(orig));
					cBus.copyAttributes(orig);
					call.setProcedureBus(cBus, orig);
				}

				copy.addBus(cBus);
			}

			return copy;
		}

		/** Connect this MemAccess to the given MemoryGateway */
		public abstract void connect(MemoryGateway gateway, int index);

		/** Connect this MemAccess to the given MemoryReferee task slot */
		public abstract void connect(MemoryReferee.TaskSlot slot);

		/** Returns a new MemAccess which is of the same subclass. */
		public abstract MemAccess copy();
	}

	private static class MemReadAccess extends MemAccess {
		private MemReadAccess(Resource target) {
			super(true, target);
		}

		public MemReadAccess(MemoryRead memRead) {
			this(memRead.getMemoryPort());
			MemoryRead.Physical physical = (MemoryRead.Physical) memRead
					.getPhysicalComponent();
			addBus(physical.getSideEnableBus());
			addBus(physical.getSideAddressBus());
			addBus(physical.getSideSizeBus());
			addPort(physical.getSideDataReadyPort());
			addPort(physical.getSideDataPort());
		}

		/**
		 * Create a new MemReadAccess representing the 'global' side of the
		 * MemoryGateway by duplicating the global ports/buses on the containing
		 * module (as provided) and grouping those newly created ports/buses.
		 * Avoids duplication with ports/buses shared with a write access via
		 * the duplicate map.
		 */
		public MemReadAccess(MemoryGateway gateway, Module mod, Map duplicate) {
			this(gateway.getResource());
			// Create ports/buses on module for each on gateway
			Exit exit = mod.getExit(Exit.SIDEBAND);
			if (exit == null)
				exit = mod.makeExit(0, Exit.SIDEBAND);

			Bus en = (Bus) duplicate.get(gateway.getMemoryReadEnableBus());
			if (en == null) {
				en = exit.makeDataBus(Component.SIDEBAND);
				en.setIDLogical(getTarget().showIDLogical() + "_RE");
				en.getPeer().setBus(gateway.getMemoryReadEnableBus());
				// en.setSize(1, false);
			}
			Bus addr = (Bus) duplicate.get(gateway.getMemoryAddressBus());
			if (addr == null) {
				addr = exit.makeDataBus(Component.SIDEBAND);
				addr.setIDLogical(getTarget().showIDLogical() + "_ADDR");
				addr.getPeer().setBus(gateway.getMemoryAddressBus());
				// addr.setSize(32, false); // THIS SHOULD BE BASED ON THE
				// BACKING MEMORY!
			}
			Bus size = (Bus) duplicate.get(gateway.getMemorySizeBus());
			if (size == null) {
				size = exit.makeDataBus(Component.SIDEBAND);
				size.setIDLogical(getTarget().showIDLogical() + "_SIZE");
				size.getPeer().setBus(gateway.getMemorySizeBus());
				// size.setSize(Memory.SIZE_WIDTH, false);
			}

			Port ready = (Port) duplicate.get(gateway.getMemoryDonePort());
			if (ready == null) {
				ready = mod.makeDataPort(Component.SIDEBAND);
				ready.getPeer().setIDLogical(
						getTarget().showIDLogical() + "_DONE");
				// ready.getPeer().setSize(1, false);
				gateway.getMemoryDonePort().setBus(ready.getPeer());
			}
			Port data = (Port) duplicate.get(gateway.getMemoryDataReadPort());
			if (data == null) {
				data = mod.makeDataPort(Component.SIDEBAND);
				data.getPeer().setIDLogical(
						getTarget().showIDLogical() + "_RDATA");
				gateway.getMemoryDataReadPort().setBus(data.getPeer());
				// data.getPeer().setSize(65, false); // THIS SHOULD BE BASED ON
				// THE BACKING MEMORY!
			}

			addBus(en);
			addBus(addr);
			addBus(size);
			addPort(ready);
			addPort(data);
		}

		@Override
		public void connect(MemoryGateway gateway, int index) {
			MemoryGateway.ReadSlot slot = gateway.getReadSlots().get(index);
			slot.getEnable().setBus(getBus(0));
			slot.getAddress().setBus(getBus(1));
			slot.getSizePort().setBus(getBus(2));
			getPort(0).setBus(slot.getReady());
			getPort(1).setBus(slot.getData());
		}

		@Override
		public void connect(MemoryReferee.TaskSlot slot) {
			slot.getGoRPort().setBus(getBus(0));
			slot.getAddressPort().setBus(getBus(1));
			slot.getSizePort().setBus(getBus(2));
			getPort(0).setBus(slot.getDoneBus());
			getPort(1).setBus(slot.getDataOutBus());
		}

		@Override
		public MemAccess copy() {
			return new MemReadAccess(getTarget());
		}
	}

	private static class MemWriteAccess extends MemAccess {
		private MemWriteAccess(Resource target) {
			super(false, target);
		}

		public MemWriteAccess(MemoryWrite memWrite) {
			this(memWrite.getMemoryPort());
			MemoryWrite.Physical physical = (MemoryWrite.Physical) memWrite
					.getPhysicalComponent();
			addBus(physical.getSideEnableBus());
			addBus(physical.getSideAddressBus());
			addBus(physical.getSideDataBus());
			addBus(physical.getSideSizeBus());
			addPort(physical.getSideWriteFinishedPort());
		}

		/**
		 * Create a new MemWriteAccess representing the 'global' side of the
		 * MemoryGateway by duplicating the global ports/buses on the containing
		 * module (as provided) and grouping those newly created ports/buses.
		 * Avoids duplication with ports/buses shared with a read access via the
		 * duplicate map.
		 */
		public MemWriteAccess(MemoryGateway gateway, Module mod, Map duplicate) {
			this(gateway.getResource());
			// Create ports/buses on module for each on gateway
			Exit exit = mod.getExit(Exit.SIDEBAND);
			if (exit == null)
				exit = mod.makeExit(0, Exit.SIDEBAND);

			Bus en = (Bus) duplicate.get(gateway.getMemoryWriteEnableBus());
			if (en == null) {
				en = exit.makeDataBus(Component.SIDEBAND);
				en.setIDLogical(getTarget().showIDLogical() + "_WE");
				en.getPeer().setBus(gateway.getMemoryWriteEnableBus());
				// en.setSize(1, false);
			}
			Bus addr = (Bus) duplicate.get(gateway.getMemoryAddressBus());
			if (addr == null) {
				addr = exit.makeDataBus(Component.SIDEBAND);
				addr.setIDLogical(getTarget().showIDLogical() + "_ADDR");
				addr.getPeer().setBus(gateway.getMemoryAddressBus());
				// addr.setSize(32, false); // THIS SHOULD BE BASED ON THE
				// BACKING MEMORY!
			}
			Bus data = (Bus) duplicate.get(gateway.getMemoryDataWriteBus());
			if (data == null) {
				data = exit.makeDataBus(Component.SIDEBAND);
				data.setIDLogical(getTarget().showIDLogical() + "_WDATA");
				data.getPeer().setBus(gateway.getMemoryDataWriteBus());
				// data.setSize(65, false); // THIS SHOULD BE BASED ON THE
				// BACKING MEMORY!
			}
			Bus size = (Bus) duplicate.get(gateway.getMemorySizeBus());
			if (size == null) {
				size = exit.makeDataBus(Component.SIDEBAND);
				size.setIDLogical(getTarget().showIDLogical() + "_SIZE");
				size.getPeer().setBus(gateway.getMemorySizeBus());
				// size.setSize(Memory.SIZE_WIDTH, false);
			}

			Port ready = (Port) duplicate.get(gateway.getMemoryDonePort());
			if (ready == null) {
				ready = mod.makeDataPort(Component.SIDEBAND);
				ready.getPeer().setIDLogical(
						getTarget().showIDLogical() + "_DONE");
				gateway.getMemoryDonePort().setBus(ready.getPeer());
				// ready.getPeer().setSize(1, false);
			}

			addBus(en);
			addBus(addr);
			addBus(data);
			addBus(size);
			addPort(ready);
		}

		@Override
		public void connect(MemoryGateway gateway, int index) {
			MemoryGateway.WriteSlot slot = gateway.getWriteSlots().get(index);
			getPort(0).setBus(slot.getDone());
			slot.getEnable().setBus(getBus(0));
			slot.getAddress().setBus(getBus(1));
			slot.getData().setBus(getBus(2));
			slot.getSizePort().setBus(getBus(3));
		}

		@Override
		public void connect(MemoryReferee.TaskSlot slot) {
			slot.getGoWPort().setBus(getBus(0));
			slot.getAddressPort().setBus(getBus(1));
			slot.getDataInPort().setBus(getBus(2));
			slot.getSizePort().setBus(getBus(3));
			getPort(0).setBus(slot.getDoneBus());
		}

		@Override
		public MemAccess copy() {
			return new MemWriteAccess(getTarget());
		}
	}

}// MemoryConnectionVisitor
