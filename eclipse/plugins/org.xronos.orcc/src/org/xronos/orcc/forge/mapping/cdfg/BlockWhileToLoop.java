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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.util.EcoreHelper;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.LoopBody;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.util.naming.IDSourceInfo;

/**
 * This Visitor transforms a {@link BlockWhile} to a LIM {@link Loop}
 * 
 * @author Endri Bezati
 * 
 */
public class BlockWhileToLoop extends AbstractIrVisitor<Loop> {

	@SuppressWarnings("unchecked")
	@Override
	public Loop caseBlockWhile(BlockWhile blockWhile) {
		// Initialize members
		Map<Var, Port> inputs = new HashMap<Var, Port>();
		Map<Var, Bus> outputs = new HashMap<Var, Bus>();

		Map<Bus, Var> feedbackBusVar = new HashMap<Bus, Var>();
		Map<Bus, Var> completeBusVar = new HashMap<Bus, Var>();
		// -- Decision
		// Construct decision from the block while condition
		Block decisionBlock = null;
		Component valueComponent = new ExprToComponent().doSwitch(blockWhile
				.getCondition());

		Map<Var, Port> dBlockDataPorts = null;

		if (!(valueComponent instanceof Block)) {
			dBlockDataPorts = new HashMap<Var, Port>();
			Map<Var, Port> valueDataPorts = (Map<Var, Port>) blockWhile
					.getCondition().getAttribute("inputs").getObjectValue();
			decisionBlock = new Block(Arrays.asList(valueComponent));
			// Propagate DataPorts
			ComponentUtil.propagateDataPorts(decisionBlock, dBlockDataPorts,
					valueDataPorts);
			// Propagate DataBuses
			for (Bus dataBus : valueComponent.getExit(Exit.DONE).getDataBuses()) {
				Bus blockDataBus = decisionBlock.getExit(Exit.DONE)
						.makeDataBus();
				Port blockDataBuspeer = blockDataBus.getPeer();
				ComponentUtil.connectDataDependency(dataBus, blockDataBuspeer,
						0);
			}
		} else {
			decisionBlock = (Block) valueComponent;
			dBlockDataPorts = (Map<Var, Port>) blockWhile.getCondition()
					.getAttribute("inputs").getObjectValue();
		}

		Component decisionComponent = ComponentUtil
				.decisionFindConditionComponent(decisionBlock);

		// Create decision
		Decision decision = new Decision(decisionBlock, decisionComponent);

		//Debug.depGraphTo(decision, "deicions", "/tmp/decision1.dot", 1);
		
		// Propagate decisionBlockInputs to the decision one
		Map<Var, Port> dDataPorts = new HashMap<Var, Port>();
		ComponentUtil.propagateDataPorts(decision, dDataPorts, dBlockDataPorts);

		// -- Loop Body
		// Construct Loop Body Block from the block while blocks
		Map<Var, Port> blockDataPorts = new HashMap<Var, Port>();
		Map<Var, Bus> blockDataBuses = new HashMap<Var, Bus>();

		Module body = (Module) new BlocksToBlock(blockDataPorts,
				blockDataBuses, false).doSwitch(blockWhile.getBlocks());

		// Loop body (While Body) inputs and outputs
		Map<Var, Port> lbDataPorts = new HashMap<Var, Port>();

		LoopBody loopBody = new WhileBody(decision, body);

		// Propagate decision and body inputs to the loopBody
		// -- Propagate Decision data ports
		ComponentUtil.propagateDataPorts(loopBody, lbDataPorts, dDataPorts);

		// -- Propagate Body Blocks data ports
		ComponentUtil.propagateDataPorts(loopBody, lbDataPorts, blockDataPorts);

		// -- Propagate Body Blocks data Buses

		// Propagate data buses to feedback and completed data buses
		// -- Feedback Exit
		for (Var var : blockDataBuses.keySet()) {
			if (lbDataPorts.containsKey(var)) {
				Type type = var.getType();
				Bus bus = blockDataBuses.get(var);
				// -- Make an feedback exit data bus
				Bus fbBus = loopBody.getFeedbackExit().makeDataBus(
						var.getName(), type.getSizeInBits(), type.isInt());
				// -- Connect
				Port fbBusPeer = fbBus.getPeer();
				ComponentUtil.connectDataDependency(bus, fbBusPeer, 0);
				// -- Save it to the feedbackBusVar
				feedbackBusVar.put(fbBus, var);
			}
		}

		// -- Complete Exit
		for (Var var : blockDataBuses.keySet()) {
			Type type = var.getType();
			Bus bus = blockDataBuses.get(var);
			// -- Make an complete exit data bus
			Bus cBus = loopBody.getLoopCompleteExit().makeDataBus(
					var.getName(), type.getSizeInBits(), type.isInt());
			// -- Connect
			Port cBusPeer = cBus.getPeer();
			ComponentUtil.connectDataDependency(bus, cBusPeer, 0);
			// -- Save it to the feedbackBusVar
			completeBusVar.put(cBus, var);
		}

		// -- Dependency through input to the Done Exit
		for (Var lbVar : lbDataPorts.keySet())
			for (Bus cBus : completeBusVar.keySet()) {
				Var cVar = completeBusVar.get(cBus);
				if (lbVar == cVar) {
					Port lbDataPort = lbDataPorts.get(lbVar);
					Bus lbDataportBus = lbDataPort.getPeer();

					Port cBusPeer = cBus.getPeer();
					ComponentUtil.connectDataDependency(lbDataportBus,
							cBusPeer, 0);
				}
			}

		// Create Loop
		Loop loop = new Loop(loopBody);

		// Create Loop DataPorts

		Set<Var> inVars = new HashSet<Var>();
		inVars.addAll(dDataPorts.keySet());
		inVars.addAll(lbDataPorts.keySet());

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
			if (lbDataPorts.containsKey(var)) {
				Port lbPort = lbDataPorts.get(var);
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
			if (lbDataPorts.containsKey(var)) {
				Exit lfbExit = loop.getBody().getFeedbackExit();
				Port lbPort = lbDataPorts.get(var);

				// -- Create a feedback register
				Reg fbReg = loop.createDataRegister();
				fbReg.setIDLogical("fbReg_" + var.getName());
				// fbReg.getDataPort().setIDLogical(var.getName());
				// fbReg.getResultBus().setIDLogical(var.getName());

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

		for (Var var : blockDataPorts.keySet()) {
			if (!blockDataBuses.containsKey(var)) {
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
				latchResultBus.setIDLogical(var.getName() + "_result");

				Port lbPort = lbDataPorts.get(var);
				fbEntry.addDependency(lbPort,
						new DataDependency(latchResultBus));
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

		// IDSourceInfo
		Procedure procedure = EcoreHelper.getContainerOfType(blockWhile,
				Procedure.class);
		IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
				blockWhile.getLineNumber());
		loop.setIDSourceInfo(sinfo);
		return loop;
	}
}
