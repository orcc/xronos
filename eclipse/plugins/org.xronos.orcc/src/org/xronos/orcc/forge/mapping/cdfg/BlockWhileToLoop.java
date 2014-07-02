/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */

package org.xronos.orcc.forge.mapping.cdfg;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.LoopBody;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.primitive.Reg;

/**
 * This Visitor transforms a {@link BlockWhile} to a LIM {@link Loop}
 * 
 * @author Endri Bezati
 * 
 */
public class BlockWhileToLoop extends AbstractIrVisitor<Loop> {

	@Override
	public Loop caseBlockWhile(BlockWhile blockWhile) {
		// Initialize members
		Map<Var, Port> inputs = new HashMap<Var, Port>();
		Map<Var, Bus> outputs = new HashMap<Var, Bus>();
		Map<Bus, Var> feedbackBusVar = new HashMap<Bus, Var>();
		Map<Bus, Var> completeBusVar = new HashMap<Bus, Var>();
		// -- Decision
		// Construct decision from the block while condition
		Block decisionBlock = (Block) new ExprToComponent().doSwitch(blockWhile
				.getCondition());
		@SuppressWarnings("unchecked")
		Map<Var, Port> dInputs = (Map<Var, Port>) blockWhile.getCondition()
				.getAttribute("inputs").getObjectValue();

		Component decisionComponent = decisionFindConditionComponent(decisionBlock);

		// Create decision
		Decision decision = new Decision(decisionBlock, decisionComponent);

		// Propagate decisionBlockInputs to the decision one
		decisionPropagateInputs(decision, decisionBlock);

		// -- Loop Body
		// Construct Loop Body Block from the block while blocks
		Map<Var, Port> blocksInputs = new HashMap<Var, Port>();
		Map<Var, Bus> blocksOutputs = new HashMap<Var, Bus>();
		Module body = (Module) new BlocksToBlock(blocksInputs, blocksOutputs,
				false).doSwitch(blockWhile.getBlocks());

		// Loop body (While Body) inputs and outputs
		Map<Var, Port> lbInputs = new HashMap<Var, Port>();

		LoopBody loopBody = new WhileBody(decision, body);

		// Propagate decision and body inputs to the loopBody
		// -- Propagate Decision data ports
		ComponentUtil.propagateDataPorts(loopBody, lbInputs, dInputs);

		// -- Propagate Body Blocks data ports
		ComponentUtil.propagateDataPorts(loopBody, lbInputs, blocksInputs);

		// -- Propagate Body Blocks data Buses

		// Propagate data buses to feedback and completed data buses
		// -- Feedback Exit
		Exit fExit = loopBody.getFeedbackExit();
		for (Var var : blocksOutputs.keySet()) {
			if (blocksInputs.containsKey(var)) {
				Type type = var.getType();
				Bus bus = blocksOutputs.get(var);
				// -- Make an feedback exit data bus
				Bus fbBus = fExit.makeDataBus(var.getName(),
						type.getSizeInBits(), type.isInt());
				// -- Connect
				Port fbBusPeer = fbBus.getPeer();
				ComponentUtil.connectDataDependency(bus, fbBusPeer, 0);
				// -- Save it to the feedbackBusVar
				feedbackBusVar.put(fbBus, var);
			}
		}

		// -- Complete Exit
		Exit cExit = loopBody.getLoopCompleteExit();
		for (Var var : blocksOutputs.keySet()) {
			Type type = var.getType();
			Bus bus = blocksOutputs.get(var);
			// -- Make an feedback exit data bus
			Bus cBus = cExit.makeDataBus(var.getName(), type.getSizeInBits(),
					type.isInt());
			// -- Connect
			Port cBusPeer = cBus.getPeer();
			ComponentUtil.connectDataDependency(bus, cBusPeer, 0);
			// -- Save it to the feedbackBusVar
			completeBusVar.put(cBus, var);
		}

		// Create Loop
		Loop loop = new Loop(loopBody);

		// Create Loop DataPorts

		Set<Var> inVars = new HashSet<Var>();
		inVars.addAll(dInputs.keySet());
		inVars.addAll(blocksInputs.keySet());

		for (Var var : inVars) {
			if (!inputs.containsKey(var)) {
				Type type = var.getType();
				Port dataPort = loop.makeDataPort(var.getName(),
						type.getSizeInBits(), type.isInt());
				inputs.put(var, dataPort);
			}
		}

		// Create Loop inner dependencies

		// -- Init dependencies
		Entry initEntry = loop.getBodyInitEntry();
		for (Var var : inputs.keySet()) {
			Port lPort = inputs.get(var);
			if (lbInputs.containsKey(var)) {
				Port lbPort = lbInputs.get(var);
				Bus lPortPeer = lPort.getPeer();
				Dependency dep = (lbPort == lbPort.getOwner().getGoPort()) ? new ControlDependency(
						lPortPeer) : new DataDependency(lPortPeer);
				initEntry.addDependency(lbPort, dep);
			}
		}

		// -- Feedback dependencies
		Entry fbEntry = loop.getBodyFeedbackEntry();
		for (Bus fbBus : loopBody.getFeedbackExit().getDataBuses()) {
			Var var = feedbackBusVar.get(fbBus);
			if (lbInputs.containsKey(var)) {
				Exit lfbExit = loop.getBody().getFeedbackExit();
				Port lbPort = lbInputs.get(var);

				// -- Create a feedback register
				Reg fbReg = loop.createDataRegister();
				fbReg.setIDLogical("fbReg_" + var.getName());
				fbReg.getDataPort().setIDLogical(var.getName());
				fbReg.getResultBus().setIDLogical(var.getName());

				// -- Dependencies
				Entry entry = fbReg.makeEntry(lfbExit);
				entry.addDependency(fbReg.getDataPort(), new DataDependency(
						fbBus));
				fbEntry.addDependency(lbPort,
						new DataDependency(fbReg.getResultBus()));
			}
		}

		// -- Latch dependencies
		Collection<Dependency> goInitDeps = initEntry.getDependencies(loop
				.getBody().getGoPort());
		Bus initDoneBus = goInitDeps.iterator().next().getLogicalBus();

		for (Var var : blocksInputs.keySet()) {
			if (!blocksOutputs.containsKey(var)) {
				Port lPort = inputs.get(var);
				Bus lPortPeer = lPort.getPeer();

				// -- Create a latch
				Latch latch = loop.createDataLatch();
				latch.setIDLogical("latched_" + var.getName());

				// -- Dependencies
				Entry latchEntry = latch.makeEntry(initDoneBus.getOwner());
				latch.getDataPort().setIDLogical(var.getName());
				// -- Control dependency
				latchEntry.addDependency(latch.getEnablePort(),
						new ControlDependency(initDoneBus));
				// -- Data dependency in latch
				latchEntry.addDependency(latch.getDataPort(),
						new DataDependency(lPortPeer));
				// -- Data dependency out latch
				Bus latchResultBus = latch.getResultBus();
				latchResultBus.setIDLogical(var.getName()+"_result");

				Port lbPort = lbInputs.get(var);
				fbEntry.addDependency(lbPort, new DataDependency(lPortPeer));
			}
		}

		// Create Loop Data buses
		Entry outbufEntry = loop.getExit(Exit.DONE).getPeer().getEntries()
				.get(0);

		for (Bus bus : loopBody.getLoopCompleteExit().getDataBuses()) {
			Var var = completeBusVar.get(bus);
			Type type = var.getType();
			Bus dataBus = loop.getExit(Exit.DONE).makeDataBus(var.getName(),
					type.getSizeInBits(), type.isInt());
			Port dataBusPeer = dataBus.getPeer();
			Dependency dep = new DataDependency(bus);
			outbufEntry.addDependency(dataBusPeer, dep);
			outputs.put(var, dataBus);
		}

		// Set control dependency
		Port lbDonePort = loopBody.getLoopCompleteExit().getDoneBus().getPeer();
		Bus lDoneBus = loop.getExit(Exit.DONE).getDoneBus();
		Dependency dep = new ControlDependency(lDoneBus);
		outbufEntry.addDependency(lbDonePort, dep);

		blockWhile.setAttribute("inputs", inputs);
		blockWhile.setAttribute("outputs", outputs);
		
		return loop;
	}

	/**
	 * Find the condition component on the decision Block
	 * 
	 * @param decisionBlock
	 * @return
	 */
	private Component decisionFindConditionComponent(Block decisionBlock) {
		// Decision block contains olny one result bus
		Bus resultBus = decisionBlock.getExit(Exit.DONE).getDataBuses().get(0);
		Port resultBusPeer = resultBus.getPeer();

		for (Component component : decisionBlock.getComponents()) {
			if (!(component instanceof InBuf) && !(component instanceof OutBuf)) {
				for (Bus bus : component.getExit(Exit.DONE).getDataBuses()) {
					Collection<Dependency> deps = bus.getLogicalDependents();
					for (Dependency dep : deps) {
						Port port = dep.getPort();
						if (port == resultBusPeer) {
							return component;
						}
					}
				}
			}
		}

		return null;
	}

	/**
	 * This method propagates the input of the testBlock of the decision to its
	 * container. Any data inputs to the decision need to be propagated from the
	 * block to the decision. There should be no output ports to propagate. They
	 * are inferred true/false.
	 * 
	 * @param decision
	 * @param testBlock
	 */
	private void decisionPropagateInputs(Decision decision, Block decisionBlock) {
		for (Port port : decisionBlock.getDataPorts()) {
			Port decisionPort = decision.makeDataPort();
			Entry entry = port.getOwner().getEntries().get(0);
			entry.addDependency(port,
					new DataDependency(decisionPort.getPeer()));
		}
	}

}
