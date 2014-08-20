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
import org.xronos.openforge.util.Debug;
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

		Debug.depGraphTo(decision, "deicions", "/tmp/decision1.dot", 1);

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
		Debug.depGraphTo(loopBody, "loopBody", "/tmp/loopBody.dot", 1);
		// Propagate decision and body inputs to the loopBody
		// -- Propagate Decision data ports
		ComponentUtil.propagateDataPorts(loopBody, lbDataPorts, dDataPorts);

		// -- Propagate Body Blocks data ports
		ComponentUtil.propagateDataPorts(loopBody, lbDataPorts, blockDataPorts);

		// -- Complete Exit
		for (Var var : blockDataBuses.keySet()) {
			Type type = var.getType();
			Bus bus = blockDataBuses.get(var);
			// -- Make an complete exit data bus
			Bus cBus = loopBody.getLoopCompleteExit().makeDataBus(
					var.getName() + "_fb", type.getSizeInBits(), type.isInt());
			// -- Connect
			Port cBusPeer = cBus.getPeer();
			ComponentUtil.connectDataDependency(bus, cBusPeer);
			// -- Save it to the feedbackBusVar
			completeBusVar.put(cBus, var);
		}

		// -- FeedBack Exit
		for (Var var : blockDataBuses.keySet()) {
			Type type = var.getType();
			// -- If the input does not exist create one because this is a
			// feedback
			if (!lbDataPorts.containsKey(var)) {
				Port lbPort = loopBody.makeDataPort(var.getName() + "_fb",
						type.getSizeInBits(), type.isInt());
				lbDataPorts.put(var, lbPort);
			}
			Bus bus = blockDataBuses.get(var);
			// -- Make an feedback exit data bus
			Bus fbBus = loopBody.getFeedbackExit().makeDataBus(
					var.getName() + "_fb", type.getSizeInBits(), type.isInt());
			// -- Connect
			Port fbBusPeer = fbBus.getPeer();
			ComponentUtil.connectDataDependency(bus, fbBusPeer);
			// -- Save it to the feedbackBusVar
			feedbackBusVar.put(fbBus, var);
		}

		// -- From input to Complete Exit dependency
		for (Bus bus : completeBusVar.keySet()) {
			Var var = completeBusVar.get(bus);
			Port port = lbDataPorts.get(var);
			// -- Connect it
			Port busPeer = bus.getPeer();
			Bus portpeer = port.getPeer();
			ComponentUtil.connectDataDependency(portpeer, busPeer);
		}

		// Create Loop
		Loop loop = new Loop(loopBody);

		// -- Loop inputs comes from decision and body
		Set<Var> inVars = new HashSet<Var>();
		inVars.addAll(dDataPorts.keySet());
		inVars.addAll(lbDataPorts.keySet());

		for (Var var : inVars) {
			Type type = var.getType();
			Port dataPort = loop.makeDataPort(var.getName(),
					type.getSizeInBits(), type.isInt());
			inputs.put(var, dataPort);
		}

		// -- Init Dependencies
		Entry initEntry = loop.getBodyInitEntry();
		for (Var var : inputs.keySet()) {
			if (feedbackBusVar.containsValue(var)) {
				Port lPort = inputs.get(var);
				Port lbPort = lbDataPorts.get(var);
				Bus lPortPeer = lPort.getPeer();
				Dependency dep = (lbPort == lbPort.getOwner().getGoPort()) ? new ControlDependency(
						lPortPeer) : new DataDependency(lPortPeer);
				initEntry.addDependency(lbPort, dep);
			}
		}

		// -- Feedback Dependencies
		Entry fbEntry = loop.getBodyFeedbackEntry();
		for (Bus bus : feedbackBusVar.keySet()) {
			Var var = feedbackBusVar.get(bus);
			Exit lfbExit = loop.getBody().getFeedbackExit();
			Port lbPort = lbDataPorts.get(var);

			// -- Create a feedback register
			Reg fbReg = loop.createDataRegister();
			fbReg.setIDLogical("fbReg_" + var.getName());
			// fbReg.getDataPort().setIDLogical(var.getName());
			// fbReg.getResultBus().setIDLogical(var.getName());

			// -- Dependencies
			Entry entry = fbReg.makeEntry(lfbExit);
			entry.addDependency(fbReg.getDataPort(), new DataDependency(bus));
			fbEntry.addDependency(lbPort,
					new DataDependency(fbReg.getResultBus()));
		}

		// -- Latch Dependencies
		Collection<Dependency> goInitDeps = initEntry.getDependencies(loop
				.getBody().getGoPort());
		Bus initDoneBus = goInitDeps.iterator().next().getLogicalBus();
		for (Var var : inVars) {
			if (!feedbackBusVar.containsValue(var)) {
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

		// -- Done Dependencies
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

		// -- Set control dependency
		Port lbDonePort = loopBody.getLoopCompleteExit().getDoneBus().getPeer();
		Bus lDoneBus = loop.getExit(Exit.DONE).getDoneBus();
		Dependency dep = new ControlDependency(lDoneBus);
		outbufEntry.addDependency(lbDonePort, dep);

		// -- IDSourceInfo
		Procedure procedure = EcoreHelper.getContainerOfType(blockWhile,
				Procedure.class);
		IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
				blockWhile.getLineNumber());
		loop.setIDSourceInfo(sinfo);
		Debug.depGraphTo(loop, "loopBody", "/tmp/loop_new.dot", 1);

		// -- Set attributes
		blockWhile.setAttribute("inputs", inputs);
		blockWhile.setAttribute("outputs", outputs);
		return loop;
	}
}
