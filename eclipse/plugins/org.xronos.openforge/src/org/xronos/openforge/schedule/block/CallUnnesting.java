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

package org.xronos.openforge.schedule.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Referenceable;
import org.xronos.openforge.lim.Referencer;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.optimize.ComponentSwapVisitor;
import org.xronos.openforge.util.naming.ID;


/**
 * CallUnnesting is an engine for flattening the LIM by removing the Call
 * hierarchy. This is accomplished by pulling the called procedures body block
 * out of the procedure and instantiating it into the Block in place of the
 * Call. All connectivity is mimiced on the procedure block from the Call to
 * ensure correct functionality. This class can be used to flatten the entire
 * call hierarchy, or it may be used to only flatten those calls which contain
 * (anywhere in their tree) a {@link Referencer} which targets a state
 * maintaining {@link Referenceable} such as a memory access or a register
 * access.
 * 
 * <p>
 * Created: Fri Sep 3 08:16:40 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CallUnnesting.java 122 2006-03-30 18:05:17Z imiller $
 */
public class CallUnnesting {

	/**
	 * If set to true then all calls are flattened, instead of just those calls
	 * which contain a Referencer.
	 */
	private boolean flattenAll = false;

	/**
	 * Constructs a default CallUnnesting instance which can be used to flatten
	 * any call which contains a reference to a memory element (memory or
	 * register) within its logic tree.
	 */
	public CallUnnesting() {
		this(false);
	}

	/**
	 * Constructs a CallUnnesting instance with selectable behavior. If
	 * flattenAllCalls is set to true then all calls will be unnested, otherwise
	 * only calls which contain a memory element reference will be flattened.
	 * 
	 * @param flattenAllCalls
	 *            a value of type 'boolean'
	 */
	public CallUnnesting(boolean flattenAllCalls) {
		flattenAll = flattenAllCalls;
	}

	/**
	 * Performs call flattening on the specified {@link Visitable}
	 * 
	 * @param vis
	 *            a non-null Visitable
	 * @throws NullPointerException
	 *             if vis is null
	 */
	public void flattenCalls(Visitable vis) {
		final CallFinder finder = new CallFinder();
		vis.accept(finder);

		for (Call call : finder.foundCalls) {
			flattenCall(call);
		}
	}

	/**
	 * Flattens the specified call, or does nothing if the call has no owner or
	 * has more than 1 entry. Call flattening is accomplished by converting the
	 * RETURN exit to a DONE exit, creating the appropriate Dependencies on the
	 * block to mimic those of the call and then finally moving the block to
	 * replace the Call in the containing module.
	 * 
	 * @param call
	 *            a value of type 'Call'
	 */
	private void flattenCall(Call call) {
		// Cannot flatten the top level call.
		// IDM. Ugly hack, but there is no way to be sure we will have
		// traversed the top level design.
		if (call.getOwner() == null
				|| (call.getOwner() instanceof Design.DesignModule))
			return;

		if (call.getEntries().size() > 1) {
			EngineThread
					.getEngine()
					.getGenericJob()
					.warn("Cannot flatten call "
							+ ID.showLogical(call.getProcedure())
							+ " due to multiple independent paths to call");
			return;
		}

		// Should never encounter a call with 0 entries!
		assert call.getEntries().size() != 0;

		// Add the procedure block to the owning block
		Module owner = call.getOwner();
		Block procBody = call.getProcedure().getBody();

		// Switch the 'type' of the exit of the procedure body to be a
		// DONE exit instead of return and also mark the block as NOT
		// being a procedure body.
		procBody.setProcedure(null);
		assert procBody.getExits().size() == 1;
		final Exit returnExit = procBody.getExit(Exit.RETURN);
		final Exit doneExit = procBody.makeExit(returnExit.getDataBuses()
				.size(), Exit.DONE, returnExit.getTag().getLabel());
		// Now that we have a new (DONE) exit, copy the connectivity
		// of the RETURN exit over to the DONE exit (actually the outbufs).
		Map<Port, Port> portMap = new HashMap<Port, Port>();
		Iterator<Port> sourceIter = returnExit.getPeer().getPorts().iterator();
		Iterator<Port> targetIter = doneExit.getPeer().getPorts().iterator();
		while (sourceIter.hasNext()) {
			portMap.put(sourceIter.next(), targetIter.next());
		}
		ComponentSwapVisitor.replaceConnections(portMap,
				Collections.<Bus, Bus> emptyMap(),
				Collections.<Exit, Exit> emptyMap());
		procBody.removeExit(returnExit);

		// Connect procedure block by mimicing the call connections
		ComponentSwapVisitor.mapExact(call, procBody);

		// This takes care of both removing and adding as well as
		// disconnecting the call.
		boolean replaced = owner.replaceComponent(call, procBody);
		if (!replaced) {
			EngineThread
					.getEngine()
					.getGenericJob()
					.fatalError(
							"Could not flatten design.  Component replacement failed.");
		}
	}

	/**
	 * A simple visitor that is used to capture all Call objects that must be
	 * traversed in order to reach every memory referencing node. To do this the
	 * current calling 'stack' is maintained and when a memory access is reached
	 * the entire stack is added to the collection of calls to be flattened.
	 */
	class CallFinder extends DefaultVisitor {
		/** A Set of Call objects that have been identified. */
		private Set<Call> foundCalls = new HashSet<Call>();

		/**
		 * Used to maintain knowledge of what the current calling tree is.
		 */
		private Stack<Call> callTree = new Stack<Call>();

		public CallFinder() {
			super();
		}

		@Override
		public void visit(Call call) {
			if (flattenAll) {
				foundCalls.add(call);
			}

			callTree.push(call);
			super.visit(call);
			callTree.pop();
		}

		@Override
		public void visit(MemoryRead access) {
			foundCalls.addAll(callTree);
		}

		@Override
		public void visit(MemoryWrite access) {
			foundCalls.addAll(callTree);
		}

		@Override
		public void visit(RegisterRead access) {
			foundCalls.addAll(callTree);
		}

		@Override
		public void visit(RegisterWrite access) {
			foundCalls.addAll(callTree);
		}

		@Override
		public void visit(FifoRead access) {
			foundCalls.addAll(callTree);
		}

		@Override
		public void visit(FifoWrite access) {
			foundCalls.addAll(callTree);
		}

		@Override
		public void visit(FifoAccess access) {
			foundCalls.addAll(callTree);
		}

	}

}// CallUnnesting
