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

package net.sf.openforge.optimize.loop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.LoopBody;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.optimize._optimize;
import net.sf.openforge.util.naming.ID;

/**
 * This is the code that actually unrolls a loop. A Loop consists of an
 * InitBlock and a LoopBody. The Body consists of a Decision, a Body block, and
 * an optional Update block. The order is specific, based on LoopB
 * ody.isDecisionFirst(). </p>
 * 
 * Several things happen here. A new Block object is created to replace the
 * Loop. The initblock is cloned, Using the known number of iterations the loop
 * will execute, N many Blocks are created to replace the LoopBody. within these
 * body blocks, The LoopBody.getBody() is cloned, as is the Update (if needed).
 * The Decision object is a special case, in that the Decision is thrown away
 * but the Decision.getTestBlock() is cloned. These new body blocks are added to
 * the new loop block<\p>
 * 
 * 2 sets of things need to be connected. First the innards of the new body
 * blocks are connected, then the body blocks themsleves are connected, then the
 * new loop block is connected. Then the old Loop object is disconnected from
 * the graph.</p>
 * 
 * In terms of connecting, the approach is like this. Each new component is
 * connected in a manner to mirror an old component, getEquivalentBus() is used.
 * getEquivalentBus() works by starting from the old Entry and the old Port,
 * finding the old Bus it depended on, then finding the equivalent Component in
 * the new structure, and getting the new Bus at the same position as the old
 * bus was. </p>
 * 
 * In theory, performance could be improved by caching tracebus, and bus
 * positional primitives.
 * 
 * @author CSchanck
 * @version $Id: LoopUnrollingEngine.java 558 2008-03-14 14:14:48Z imiller $
 */
public class LoopUnrollingEngine {

	private Loop loop = null;

	private Block loopBlock = null; // loop block (equiv to Loop)
	private Block newInitBlock = null; // new init block

	// these are used for context while connecting them up.
	private Block currentIteration = null; // current iteration body block
	private Block prevIteration = null; // previos iteration body block

	private Map<Block, Module> newBodies = new HashMap<Block, Module>(11); // map
																			// of
																			// iteration
																			// block
																			// ::
																			// body
	private Map<Block, Module> newDecisions = new HashMap<Block, Module>(11); // map
																				// of
																				// iteration
																				// block
																				// ::
	// decision
	private Map<Block, Module> newUpdates = new HashMap<Block, Module>(11); // map
																			// of
																			// iteration
																			// block
																			// ::
	// update
	private int currentIterationIndex;
	private int iterationCount;

	private boolean hasUpdate;

	private void setLoop(Loop l) {
		loop = l;

		// this is IMPORTANT. The getIterations() count is the nuber of times
		// the decision
		// will evaluate TRUE. This means for a While/For loop, there will be an
		// iteration+1
		// consisting of just a decision (for the first false), whicl for an
		// UNTIL, there will
		// be an iteration+1 consisting of everything.
		iterationCount = loop.getIterations() + 1;

		// need the update?
		hasUpdate = loop.getBody().getUpdate() != null;
	}

	private void clear() {
		loop = null;
		loopBlock = null;
		newInitBlock = null;
		currentIteration = null;
		prevIteration = null;
		newBodies.clear();
		newDecisions.clear();
		newUpdates.clear();
	}

	public void unroll(Loop loop) throws IllegalStateException {
		setLoop(loop);

		// check if we have a fixed iteration count
		if (iterationCount == Loop.ITERATIONS_UNKNOWN) {
			throw new IllegalStateException("unroll unknown iterations");
		}
		if (_optimize.db) {
			dbgln("unrolling to " + iterationCount + " iterations");
		}
		ArrayList<Component> blocks = new ArrayList<Component>(
				iterationCount + 2);
		try {
			// create an init block
			newInitBlock = makeNewInitBlock();
			for (int i = 0; i < iterationCount; i++) {
				// make a body block
				Block loopBodyBlock = makeBodyBlock(i);
				blocks.add(loopBodyBlock);
			}
		} catch (CloneNotSupportedException e) {
			assert false : "error cloning: " + e;
		}

		// create the new replacement loop block
		makeLoopBlock(blocks);

		// connect init block & loop block
		// connect up the blocks
		connectIterationBlocks(blocks);
		connectInitBlock();
		connectLoopBlock();

		// get rid of any memory accesses in the old loop....
		RemoveMemoryAccessVisitor rmav = new RemoveMemoryAccessVisitor();
		loop.getBody().accept(rmav);

		// remove the old Loop
		//
		loop.getOwner().replaceComponent(loop, loopBlock);

		// disconnect
		loop.disconnect();

		// clear driving exits in new loop block
		// loopBlock.accept(new Exit.ClearDrivingExitVisitor());

		// all done!
		clear();

	}

	// ***********************************************************************
	//
	// Make stuff, not connected
	//
	// ***********************************************************************

	/**
	 * Create a new init block, not connected yet
	 * 
	 * @return a value of type 'Block'
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	private Block makeNewInitBlock() throws CloneNotSupportedException {
		if (_optimize.db) {
			dbgln("Making init block");
		}
		// clone it
		Block oldInit = loop.getInitBlock();
		Block newInit = (Block) oldInit.clone();
		newInit.setIDLogical(ID.showGlobal(newInit) + "unrolledInitBlock");

		assert newInit.getDataPorts().size() == loop.getInitBlock()
				.getDataPorts().size();

		return newInit;
	}

	/**
	 * Create the body block for a given iteration. Handles end-case sentinel
	 * blocks for For/While loops.
	 * 
	 * @param count
	 *            a value of type 'int'
	 * @return a value of type 'Block'
	 */
	private Block makeBodyBlock(int count) throws CloneNotSupportedException {
		Module newDecision = null;
		Module newBody = null;
		Module newUpdate = null;

		if (_optimize.db) {
			dbgln("Making body block " + count + "+ of " + iterationCount);
		}
		if (_optimize.db) {
			dbgln("Old has these components: "
					+ loop.getBody().getComponents().size());
			for (Component comp : loop.getBody().getComponents()) {
				Object o = comp;
				dbgln("\t" + ID.glob(o) + " / " + ID.log(o));
			}
		}

		List<Component> temp = new ArrayList<Component>(2);

		// always need a decision block
		newDecision = (Block) loop.getBody().getDecision().getTestBlock()
				.clone();
		newDecision.setIDLogical("unrolled-decision-"
				+ ID.showGlobal(newDecision));

		// if this is not the last, or we have an untilbody, craete a whole body
		if (((count + 1) < iterationCount)
				|| (loop.getBody() instanceof UntilBody)) {
			newBody = (Module) loop.getBody().getBody().clone();
			newBody.setIDLogical("unrolled-body-" + ID.showGlobal(newBody));
			// get the update if we have it..
			if (hasUpdate) {
				newUpdate = (Module) loop.getBody().getUpdate().clone();
				newUpdate.setIDLogical("unrolled-update-"
						+ ID.showGlobal(newUpdate));
			}
			if (loop.getBody().isDecisionFirst()) {
				temp.add(newDecision);
				temp.add(newBody);
				if (hasUpdate) {
					temp.add(newUpdate);
				}
			} else {
				temp.add(newBody);
				if (hasUpdate) {
					temp.add(newUpdate);
				}
				temp.add(newDecision);
			}
		} else // here it is the last iteration of a For/While
		{
			temp.add(newDecision);
		}

		// create containing block
		Block b = new Block(temp, false);
		b.setIDLogical("unrolled-" + count + "-of-" + iterationCount + "-"
				+ ID.showGlobal(b));

		// record the new blocks relatinship to the parent block
		recordNewBlocks(b, newDecision, newBody, newUpdate);

		// make input ports for the block
		for (Port p : loop.getBody().getDataPorts()) {
			// make new one
			// Port newPort =
			b.makeDataPort(p.getTag());
		}

		// size the inbuf buses & peer ports
		for (Iterator<Bus> oldBusIterator = loop.getBody().getInBuf()
				.getBuses().iterator(), newBusIterator = b.getInBuf()
				.getBuses().iterator(); oldBusIterator.hasNext();) {
			Bus oldBus = oldBusIterator.next();
			Bus newBus = newBusIterator.next();
			newBus.copyAttributes(oldBus);
			newBus.getPeer().copyAttributes(oldBus.getPeer());
			if (_optimize.db) {
				dbgln("\tInbuf: new bus/port: " + ID.showGlobal(newBus) + "/"
						+ ID.showGlobal(newBus.getPeer()));
			}
			if (_optimize.db) {
				dbgln("\t\tInbuf: bus: " + oldBus.getSize() + "/"
						+ newBus.getSize());
			}
			if (_optimize.db) {
				dbgln("\t\tInbuf: port: " + oldBus.getPeer().getSize() + "/"
						+ newBus.getPeer().getSize());
			}
		}

		// make the output buses for the block
		assert b.getExits().size() == 1;
		Exit myExit = b.getExits().iterator().next();
		// we only need the data buses from 1 exit, thank you very much
		Exit bodyExit = loop.getBody().getExit(
				((count + 1) >= iterationCount) ? LoopBody.COMPLETE_TAG
						: LoopBody.FEEDBACK_TAG);

		for (@SuppressWarnings("unused")
		Bus bus : bodyExit.getDataBuses()) {
			// Bus oldBus = (Bus) it.next();
			// Bus newBus = myExit.makeDataBus();
			myExit.makeDataBus();
		}

		// size them
		for (Iterator<Bus> oldBusIterator = bodyExit.getBuses().iterator(), newBusIterator = myExit
				.getBuses().iterator(); oldBusIterator.hasNext();) {
			Bus oldBus = oldBusIterator.next();
			Bus newBus = newBusIterator.next();
			newBus.copyAttributes(oldBus);
			newBus.getPeer().copyAttributes(oldBus.getPeer());
			if (_optimize.db) {
				dbgln("\tOutbuf: new bus/port: " + ID.showGlobal(newBus) + "/"
						+ ID.showGlobal(newBus.getPeer()));
			}
			if (_optimize.db) {
				dbgln("\t\tOutbuf: bus: " + oldBus.getSize() + "/"
						+ newBus.getSize());
			}
			if (_optimize.db) {
				dbgln("\t\tOutbuf: port: " + oldBus.getPeer().getSize() + "/"
						+ newBus.getPeer().getSize());
			}
		}
		assert loop.getBody().getPorts().size() == b.getPorts().size();

		return b;
	}

	/**
	 * Record a mapping of new ody block to Decision,Body,Update
	 * 
	 * @param parent
	 *            a value of type 'Block'
	 * @param newDecision
	 *            a value of type 'Module'
	 * @param newBody
	 *            a value of type 'Module'
	 * @param newUpdate
	 *            a value of type 'Module'
	 */
	private void recordNewBlocks(Block parent, Module newDecision,
			Module newBody, Module newUpdate) {
		if (newDecision != null) {
			newDecisions.put(parent, newDecision);
		}
		if (newBody != null) {
			newBodies.put(parent, newBody);
		}
		if (newUpdate != null) {
			newUpdates.put(parent, newUpdate);
		}
	}

	/**
	 * Creates a new Block whose contents are the specified initBlock and the
	 * Blocks contained in the List.
	 * 
	 * @param blocks
	 *            List of body Block's
	 */
	private void makeLoopBlock(List<Component> blocks) {
		if (_optimize.db) {
			dbgln("Making loop block with N sub-blocks: " + blocks.size());
		}
		if (_optimize.db) {
			dbgln("Old has these components: " + loop.getComponents().size());
			for (Component comp : loop.getComponents()) {
				Object o = comp;
				dbgln("\t" + ID.glob(o) + " / " + ID.log(o));
			}
		}
		List<Component> sequence = new ArrayList<Component>();
		sequence.add(newInitBlock);
		sequence.addAll(blocks);

		loopBlock = new Block(sequence);
		loopBlock
				.setIDLogical("unrolled-loop-block" + ID.showGlobal(loopBlock));
		// make an entry
		// Entry unrolledEntry = loopBlock.makeEntry();
		loopBlock.makeEntry();

		// make the data ports
		for (Port p : loop.getDataPorts()) {
			// make new one
			// Port newPort = loopBlock.makeDataPort(p.getTag());
			loopBlock.makeDataPort(p.getTag());
		}

		// size the inbuf buses & peer ports
		for (Iterator<Bus> oldBusIterator = loop.getInBuf().getBuses()
				.iterator(), newBusIterator = loopBlock.getInBuf().getBuses()
				.iterator(); oldBusIterator.hasNext();) {
			Bus oldBus = oldBusIterator.next();
			Bus newBus = newBusIterator.next();
			newBus.copyAttributes(oldBus);
			newBus.getPeer().copyAttributes(oldBus.getPeer());
		}

		// now the exits (really the data buses
		assert loop.getExits().size() == 1; // best bo only 1
		assert loopBlock.getExits().size() == 1; // best bo only 1

		Exit oldExit = loop.getExits().iterator().next();
		Exit newExit = loopBlock.getExits().iterator().next();
		for (int i = 0; i < oldExit.getDataBuses().size(); i++) {
			Bus oldBus = oldExit.getDataBuses().get(i);
			Bus newBus = newExit.makeDataBus();
			newBus.copyAttributes(oldBus);
			newBus.getPeer().copyAttributes(oldBus.getPeer());
			if (_optimize.db) {
				dbgln("\tInbuf: new bus/port: " + ID.showGlobal(newBus) + "/"
						+ ID.showGlobal(newBus.getPeer()));
			}
			if (_optimize.db) {
				dbgln("\t\tInbuf: bus: " + oldBus.getSize() + "/"
						+ newBus.getSize());
			}
			if (_optimize.db) {
				dbgln("\t\tInbuf: port: " + oldBus.getPeer().getSize() + "/"
						+ newBus.getPeer().getSize());
			}
		}

		// size them
		for (Iterator<Bus> oldBusIterator = oldExit.getBuses().iterator(), newBusIterator = newExit
				.getBuses().iterator(); oldBusIterator.hasNext();) {
			Bus oldBus = oldBusIterator.next();
			Bus newBus = newBusIterator.next();
			newBus.copyAttributes(oldBus);
			newBus.getPeer().copyAttributes(oldBus.getPeer());
			if (_optimize.db) {
				dbgln("\tOutbuf: new bus/port: " + ID.showGlobal(newBus) + "/"
						+ ID.showGlobal(newBus.getPeer()));
			}
			if (_optimize.db) {
				dbgln("\t\tOutbuf: bus: " + oldBus.getSize() + "/"
						+ newBus.getSize());
			}
			if (_optimize.db) {
				dbgln("\t\tOutbuf: port: " + oldBus.getPeer().getSize() + "/"
						+ newBus.getPeer().getSize());
			}
		}

		assert loop.getExits().size() == 1;
		assert loop.getEntries().size() == 1;
		assert loop.getEntries().size() == loopBlock.getEntries().size();
		assert loop.getExits().size() == loopBlock.getExits().size();
		assert loop.getPorts().size() == loopBlock.getPorts().size();
		assert loop.getBuses().size() == loopBlock.getBuses().size();
		assert loop.getOutBufs().size() == loopBlock.getOutBufs().size();

	}

	/**
	 * Return all ports in deterministic order
	 * 
	 * @param c
	 *            a value of type 'Component'
	 * @return a value of type 'List'
	 */
	private static List<Port> getPortsInOrder(Component c) {
		List<Port> l = c.getDataPorts();
		List<Port> ret = new ArrayList<Port>(l.size() + 4);
		ret.add(c.getClockPort());
		ret.add(c.getResetPort());
		ret.add(c.getGoPort());
		ret.addAll(l);
		return ret;
	}

	// ***********************************************************************
	//
	// Connect up loop block and init block,
	//
	// ***********************************************************************

	/**
	 * Connect the replacement block for oop up
	 * 
	 */
	private void connectLoopBlock() {
		// ok, now do the connectsions
		// all entries, exits, ports and buses must be connected just like the
		// Loop was.
		Entry oldEntry = loop.getEntries().get(0);
		Entry newEntry = loopBlock.getEntries().get(0);

		// now the ports (special inorder traversal)
		{
			Iterator<Port> itOld = getPortsInOrder(loop).iterator();
			Iterator<Port> itNew = getPortsInOrder(loopBlock).iterator();
			while (itOld.hasNext()) {
				// get the two ports
				Port oldPort = itOld.next();
				Port newPort = itNew.next();
				connectAs(oldPort, newPort, oldEntry, newEntry);
			}
		}

		// now the buses
		{
			Iterator<Bus> itOld = loop.getBuses().iterator();
			Iterator<Bus> itNew = loopBlock.getBuses().iterator();
			while (itOld.hasNext()) {
				// get the two ports
				Bus oldBus = itOld.next();
				Bus newBus = itNew.next();
				connectAs(oldBus, newBus);
			}
		}

		assert loopBlock.getOutBufs().size() == 1;
		assert loop.getOutBufs().size() == 1;
		// now make sure it's outbuf is connected properly ....
		OutBuf newOB = loopBlock.getOutBufs().iterator().next();
		OutBuf oldOB = loop.getOutBufs().iterator().next();
		newEntry = newOB.getEntries().get(0);
		oldEntry = oldOB.getEntries().get(0);
		for (Iterator<Port> itOldPorts = oldOB.getDataPorts().iterator(), itNewPorts = newOB
				.getDataPorts().iterator(); itOldPorts.hasNext();) {
			Port oldPort = itOldPorts.next();
			Port newPort = itNewPorts.next();
			// connect
			connectSinglePort(oldEntry, oldPort, newEntry, newPort);
		}
	}

	/**
	 * Convenience method to a port to mirror another port
	 * 
	 * @param oldPort
	 *            old port connect to mirror
	 * @param newPort
	 *            new port connection
	 * @param oldEntry
	 *            entry for the old port
	 * @param newEntry
	 *            entry for the new port
	 */
	private static void connectAs(Port oldPort, Port newPort, Entry oldEntry,
			Entry newEntry)

	{
		// get the entries

		// first the dependencies
		Collection<Dependency> deps = oldEntry.getDependencies(oldPort);
		// for each dependency
		for (Dependency dep : deps) {
			Dependency newDep = dep.createSameType(dep.getLogicalBus());
			newEntry.addDependency(newPort, newDep);
			assert (oldPort.getBus() == null);
		}
	}

	/**
	 * Mirror a new bus as an old bus
	 * 
	 * @param oldBus
	 *            a value of type 'Bus'
	 * @param newBus
	 *            a value of type 'Bus'
	 * @param oldExit
	 *            a value of type 'Exit'
	 * @param newExit
	 *            a value of type 'Exit'
	 */
	private static void connectAs(Bus oldBus, Bus newBus) {
		// the bus's dependencies ....
		for (Dependency dep : oldBus.getLogicalDependents()) {
			Dependency newDep = dep.createSameType(newBus);
			dep.getEntry().addDependency(dep.getPort(), newDep);
			assert oldBus.getPorts().size() == 0;
		}
	}

	/**
	 * Connect the init block
	 * 
	 */
	private void connectInitBlock() {
		// inputs
		// get the right entry
		Entry newEntry = newInitBlock.getEntries().get(0);
		Entry oldEntry = loop.getInitBlock().getEntries().get(0);
		for (Iterator<Port> itOldPorts = loop.getInitBlock().getDataPorts()
				.iterator(), itNewPorts = newInitBlock.getDataPorts()
				.iterator(); itOldPorts.hasNext();) {
			Port oldPort = itOldPorts.next();
			Port newPort = itNewPorts.next();
			// connect
			connectSinglePort(oldEntry, oldPort, newEntry, newPort);
		}

	}

	// ***********************************************************************
	//
	// Connect up iteration blocks
	//
	// ***********************************************************************

	/**
	 * Connect up the iteration bocks. Manages current/prevIteration objects and
	 * the currentIterationIndex counter
	 * 
	 * @param blocks
	 *            a value of type 'List'
	 */
	private void connectIterationBlocks(List<Component> blocks) {
		prevIteration = null;
		// for each
		currentIterationIndex = 0;
		for (Iterator<Component> itBlocks = blocks.iterator(); itBlocks
				.hasNext(); currentIterationIndex++) {
			currentIteration = (Block) itBlocks.next();
			connectIterationBlock();
			prevIteration = currentIteration;
		}
	}

	/**
	 * Connect up an iteration block, based on currentIteration
	 * 
	 */
	private void connectIterationBlock() {
		if (_optimize.db) {
			dbgln("Connecting innards:" + currentIterationIndex + " is "
					+ ID.showGlobal(currentIteration));
		}
		_optimize.d.inc();
		connectIterationInnards();
		_optimize.d.dec();
		if (_optimize.db) {
			dbgln("Connecting " + currentIterationIndex + " is "
					+ ID.showGlobal(currentIteration));
		}

		// first or last?
		boolean isFirst = (currentIterationIndex == 0);
		boolean isLast = ((currentIterationIndex + 1) >= iterationCount);

		//
		// first connect this block to the things outside of it....
		//
		Entry newEntry = currentIteration.getEntries().get(0);
		// here for the first we use the entry at 0, else use 1
		Entry oldEntry = loop.getBody().getEntries().get(isFirst ? 0 : 1);
		for (Iterator<Port> itOldPorts = loop.getBody().getDataPorts()
				.iterator(), itNewPorts = currentIteration.getDataPorts()
				.iterator(); itOldPorts.hasNext();) {
			Port oldPort = itOldPorts.next();
			Port newPort = itNewPorts.next();
			// connect
			connectSinglePort(oldEntry, oldPort, newEntry, newPort);
		}
		// now make sure it's outbuf is connected properly ....
		assert currentIteration.getOutBufs().size() == 1;
		assert loop.getBody().getOutBufs().size() == 2;
		OutBuf newOB = currentIteration.getOutBufs().iterator().next();

		// here you have to choose the right outbuf; based on whether this is
		// the lastone ...
		// if it is the last one, use exit 1, else use exit 0 (feedback exit)
		OutBuf oldOB = loop
				.getBody()
				.getExit(isLast ? LoopBody.COMPLETE_TAG : LoopBody.FEEDBACK_TAG)
				.getPeer();

		newEntry = newOB.getEntries().get(0);
		oldEntry = oldOB.getEntries().get(0);
		for (Iterator<Port> itOldPorts = oldOB.getDataPorts().iterator(), itNewPorts = newOB
				.getDataPorts().iterator(); itOldPorts.hasNext();) {
			Port oldPort = itOldPorts.next();
			Port newPort = itNewPorts.next();
			// connect
			connectSinglePort(oldEntry, oldPort, newEntry, newPort);
		}
	}

	/**
	 * Connect upthe inside of the blocks (body,update,decision)
	 * 
	 */
	private void connectIterationInnards() {
		// connect the inputs of the components inside out newly created block

		// get the (up to) three components in this block
		Module currentBody = getNewBody(currentIteration);
		Module currentDecision = getNewDecision(currentIteration);
		Module currentUpdate = getNewUpdate(currentIteration);

		for (Iterator<Component> it = currentIteration.getSequence().iterator(); it
				.hasNext();) {
			Component newComp = it.next();

			// which component is this? get appropriate old component
			Component oldComp = null;
			if (newComp.equals(currentDecision)) {
				oldComp = loop.getBody().getDecision();
			} else if (newComp.equals(currentBody)) {
				oldComp = loop.getBody().getBody();
			} else if (newComp.equals(currentUpdate)) {
				oldComp = loop.getBody().getUpdate();
			}
			assert oldComp != null;

			Entry newEntry = newComp.getEntries().get(0);
			Entry oldEntry = oldComp.getEntries().get(0);

			for (Iterator<Port> itOldPorts = oldComp.getDataPorts().iterator(), itNewPorts = newComp
					.getDataPorts().iterator(); itOldPorts.hasNext();) {
				Port oldPort = itOldPorts.next();
				Port newPort = itNewPorts.next();

				// connect
				connectSinglePort(oldEntry, oldPort, newEntry, newPort);
			}
		}
	}

	private Block getNewDecision(Block b) {
		return (Block) newDecisions.get(b);
	}

	private Module getNewBody(Block b) {
		return newBodies.get(b);
	}

	private Block getNewUpdate(Block b) {
		return (Block) newUpdates.get(b);
	}

	/**
	 * Here we actually connect a single port up to it's new bus
	 * 
	 * @param oldEntry
	 *            Entry in the old structure the old port comes from
	 * @param oldPort
	 *            Port in the old structure we are interested in
	 * @param newEntry
	 *            Entry in the new structure the new port comes from
	 * @param newPort
	 *            Port in the new structure to connet the new bus to
	 */
	private void connectSinglePort(Entry oldEntry, Port oldPort,
			Entry newEntry, Port newPort) {
		// get the old bus it was connecte too
		Bus newBus = getEquivalentBus(oldEntry, oldPort);
		assert newBus != null;

		// add the dependency
		newEntry.addDependency(newPort, new DataDependency(newBus));

		if (_optimize.db) {
			dbgln("OldPort: " + oldPort + " size: " + oldPort.getSize()
					+ "NewPort: " + newPort + " size: " + newPort.getSize());
		}

		assert oldPort.getBus() == null;
		assert newPort.getSize() == oldPort.getSize();
	}

	/**
	 * This maps the relationship between signals. if something referenced
	 * component x in the old structure, it references what comp in the new
	 * structure? Unfortunately, this cannot be easily cached because it changes
	 * depending on the iteration we are in
	 * 
	 * @param oldComp
	 *            a value of type 'Component'
	 * @return a value of type 'Component'
	 */
	private Component getEquivalentComponent(Component oldComp) {
		if (_optimize.db) {
			dbgln("Getting equiv component for: " + ID.showGlobal(oldComp)
					+ " with owner: " + oldComp.getOwner());
		}
		Component newComp = null;
		if (oldComp instanceof InBuf) {
			// connect this port to the same positional bus in the new
			// block's inbuf as the posotion of the old bus in the old
			// inbuf
			if (oldComp.getOwner().equals(loop)) {
				if (_optimize.db) {
					dbgln("\tloopBLock inbuf");
				}
				newComp = loopBlock.getInBuf();
			} else if (oldComp.getOwner().equals(loop.getBody())) {
				if (_optimize.db) {
					dbgln("\tcurrent iteration inbuf");
				}
				newComp = currentIteration.getInBuf();
			} else if (oldComp.getOwner().equals(loop.getBody().getDecision())) {
				if (_optimize.db) {
					dbgln("\tnew decision inbuf");
				}
				newComp = getNewDecision(currentIteration).getInBuf();
			} else if (oldComp.getOwner().equals(loop.getBody().getUpdate())) {
				if (_optimize.db) {
					dbgln("\tnew update inbuf");
				}
				newComp = getNewUpdate(currentIteration).getInBuf();
			} else if (oldComp.getOwner().equals(loop.getBody().getBody())) {
				if (_optimize.db) {
					dbgln("\tnew body inbuf");
				}
				newComp = getNewBody(currentIteration).getInBuf();
			}
		} else if (oldComp.equals(loop.getInitBlock())) {
			newComp = newInitBlock;
			if (_optimize.db) {
				dbgln("\tinit block");
			}
		} else if (oldComp.equals(loop.getBody())) {
			assert prevIteration != null;
			newComp = prevIteration;
			if (_optimize.db) {
				dbgln("\tprev iteration");
			}
		} else if (oldComp.equals(loop.getBody().getBody())) {
			newComp = getNewBody(currentIteration);
			if (_optimize.db) {
				dbgln("\tcurrent iteration -- body");
			}
		} else if (oldComp.equals(loop.getBody().getDecision().getTestBlock())) {
			// clever, clever ...
			newComp = getNewDecision(currentIteration);
			if (_optimize.db) {
				dbgln("\tcurrent iteration -- decision");
			}
		} else if (oldComp.equals(loop.getBody().getUpdate())) {
			newComp = getNewUpdate(currentIteration);
			if (_optimize.db) {
				dbgln("\tcurrent iteration -- update");
			}
		}
		if (newComp != null) {
			if (_optimize.db) {
				dbgln("\tExchanged for: " + ID.showGlobal(newComp));
			}
			return newComp;
		} else {
			throw new IllegalStateException();
		}
	}

	/**
	 * This finds the destination bus in the old loop for a given port/entry
	 * pair. Then it finds the equivalent component, and then the same
	 * positional bus.
	 * 
	 * @param e
	 *            Old Entry
	 * @param p
	 *            Old Port
	 * @return new bus to conect to
	 */
	private Bus getEquivalentBus(Entry e, Port p) {
		if (_optimize.db) {
			dbgln("Finding equiv bus for: " + e.getOwner() + "/" + p);
		}

		// find the old bus this port connected to
		Bus oldBus = traceBus(e, p);
		// get old exit
		Exit oldExit = oldBus.getOwner();

		if (_optimize.db) {
			dbgln("\tgave oldbus: " + oldBus + " size: " + oldBus.getSize());
		}

		Bus newBus = getEquivalentBus(oldBus, oldExit);

		assert newBus.getSize() == oldBus.getSize();
		if (_optimize.db) {
			dbgln("\tgave newbus: " + newBus + " size: " + oldBus.getSize());
		}
		return newBus;
	}

	private Bus getEquivalentBus(Bus oldBus, Exit oldExit) {
		// get the new component it matches to
		Component newComp = getEquivalentComponent(oldExit.getOwner());
		assert newComp != null;

		// get the new exit
		Exit newExit = newComp.getExit(oldExit.getTag());
		if (newExit == null) {
			newExit = newComp.getOnlyExit();
		}
		assert oldExit.getBuses().size() == newExit.getBuses().size();

		// make steve happy. is it the donebus?
		if (oldBus.equals(oldExit.getDoneBus())) {
			return newExit.getDoneBus();
		} else {
			// else it is a data bus; match positions
			int pos = oldExit.getDataBuses().indexOf(oldBus);
			return newExit.getDataBuses().get(pos);
		}
	}

	/**
	 * Chase dependencies 1 level, port to bus
	 * 
	 * @param port
	 *            a value of type 'Port'
	 * @return a value of type 'Bus'
	 */
	private Bus getSingleBus(Port port) {
		assert port.getOwner().getEntries().size() == 1;
		Entry entry = port.getOwner().getEntries().get(0);
		return getSingleBus(entry, port);
	}

	/**
	 * Chase dependencies from an entry
	 * 
	 * @param entry
	 *            a value of type 'Entry'
	 * @param port
	 *            a value of type 'Port'
	 * @return a value of type 'Bus'
	 */
	private Bus getSingleBus(Entry entry, Port port) {
		Collection<Dependency> deps = entry.getDependencies(port);
		assert deps.size() == 1 : "Expecting single dependency into port "
				+ port + ", found " + deps.size() + " on "
				+ entry.getOwner().show();
		return deps.iterator().next().getLogicalBus();
	}

	/**
	 * This is where the hard work takes place. We return the bus in the old
	 * structure which which is the corresponding bus in the old loop where this
	 * will be connected in the new struture. Special cases: - if the bus is
	 * from a data latch, trace through to the Loop's inbuf bus - if the bus is
	 * from a register, trace to the loop body's outbuf's bus - if from a
	 * decision, run into the testblock for the decision
	 * 
	 * @param e
	 *            a value of type 'Entry'
	 * @param p
	 *            a value of type 'Port'
	 * @return a value of type 'Bus'
	 */
	private Bus traceBus(Entry e, Port p) {
		Bus b = getSingleBus(e, p);
		Component owner = b.getOwner().getOwner();
		if (owner.equals(loop.getBody().getDecision())) {
			// here we really want what it is connected to in the testblokc.
			// follow further
			// get the peer port
			p = b.getPeer();
			b = getSingleBus(p);
			owner = b.getOwner().getOwner();
			// as test block data bus 0 is the true false evaluation
			assert owner.equals(loop.getBody().getDecision().getTestBlock());
		} else if (loop.getDataRegisters().contains(owner)) {
			// connects to a register
			// get the outbuf reg the input of the register is connected to.
			b = getSingleBus(owner.getDataPorts().get(0));
		} else if (loop.getDataLatches().contains(owner)) {
			// connects to a latch
			// in this case, we want the bus that is connected to the input of
			// the data latch!
			b = getSingleBus(owner.getDataPorts().get(0));
		}
		return b;
	}

	private static final void dbgln(Object o) {
		_optimize.d.ln(_optimize.LOOP_UNROLLING, o);
	}

	@SuppressWarnings("unused")
	private static final void dbg(Object o) {
		_optimize.d.o(_optimize.LOOP_UNROLLING, o);
	}

	@SuppressWarnings("unused")
	private static final void dbgln() {
		_optimize.d.ln(_optimize.LOOP_UNROLLING);
	}

}

class RemoveMemoryAccessVisitor extends DefaultVisitor {
	@Override
	public void visit(ArrayRead ar) {
		ar.removeFromMemory();
	}

	@Override
	public void visit(ArrayWrite aw) {
		aw.removeFromMemory();
	}

	@Override
	public void visit(LocationConstant lc) {
		lc.removeFromMemory();
	}

	@Override
	public void visit(HeapRead hr) {
		hr.removeFromMemory();
	}

	@Override
	public void visit(HeapWrite hw) {
		hw.removeFromMemory();
	}

	@Override
	public void visit(AbsoluteMemoryRead comp) {
		comp.removeFromMemory();
	}

	@Override
	public void visit(AbsoluteMemoryWrite comp) {
		comp.removeFromMemory();
	}

	@Override
	public void visit(RegisterRead rr) {
		rr.getReferent().removeReference(rr);
	}

	@Override
	public void visit(RegisterWrite rw) {
		rw.getReferent().removeReference(rw);
	}
}
