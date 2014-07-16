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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.util.EcoreHelper;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.util.Debug;
import org.xronos.openforge.util.naming.IDSourceInfo;

/**
 * This Visitor transforms a {@link BlockIf} to a LIM {@link Branch}
 * 
 * @author Endri Bezati
 *
 */
public class BlockIfToBranch extends AbstractIrVisitor<Branch> {

	@SuppressWarnings("unchecked")
	@Override
	public Branch caseBlockIf(BlockIf blockIf) {
		Map<Var, Port> inputs = new HashMap<Var, Port>();
		Map<Var, Bus> outputs = new HashMap<Var, Bus>();

		// -- Decision
		// Construct decision from the block while condition
		Block decisionBlock = null;
		Component valueComponent = new ExprToComponent().doSwitch(blockIf
				.getCondition());

		Map<Var, Port> dBlockDataPorts = null;

		if (!(valueComponent instanceof Block)) {
			dBlockDataPorts = new HashMap<Var, Port>();
			Map<Var, Port> valueDataPorts = (Map<Var, Port>) blockIf
					.getCondition().getAttribute("inputs").getObjectValue();
			decisionBlock = new Block(Arrays.asList(valueComponent));
			// Propagate DataPorts
			ComponentUtil.propagateDataPorts(decisionBlock, dBlockDataPorts,
					valueDataPorts);
			// Propagate DataBuses
			for (Bus dataBus : valueComponent.getExit(Exit.DONE).getDataBuses()) {
				Bus blockDataBus = decisionBlock.getExit(Exit.DONE).makeDataBus();
				Port blockDataBuspeer = blockDataBus.getPeer();
				ComponentUtil.connectDataDependency(dataBus, blockDataBuspeer,
						0);
			}
		} else {
			decisionBlock = (Block) valueComponent;
			dBlockDataPorts = (Map<Var, Port>) blockIf.getCondition()
					.getAttribute("inputs").getObjectValue();
		}

		Component decisionComponent = ComponentUtil
				.decisionFindConditionComponent(decisionBlock);

		// Create decision
		Decision decision = new Decision(decisionBlock, decisionComponent);

		Debug.depGraphTo(decision, "decision", "/tmp/decision.dot", 1);
		
		// Propagate decisionBlockInputs to the decision one
		Map<Var, Port> dDataPorts = new HashMap<Var, Port>();
		ComponentUtil.propagateDataPorts(decision, dDataPorts, dBlockDataPorts);

		// -- Then Blocks

		Map<Var, Port> tDataPorts = new HashMap<Var, Port>();
		Map<Var, Bus> tDataBuses = new HashMap<Var, Bus>();
		Module trueBranch = (Module) new BlocksToBlock(tDataPorts, tDataBuses,
				false).doSwitch(blockIf.getThenBlocks());

		// -- Else Blocks
		Map<Var, Port> eDataPorts = new HashMap<Var, Port>();
		Map<Var, Bus> eDataBuses = new HashMap<Var, Bus>();
		Module falseBranch = null;
		if (!blockIf.getElseBlocks().isEmpty()) {
			falseBranch = (Module) new BlocksToBlock(eDataPorts, eDataBuses,
					false).doSwitch(blockIf.getElseBlocks());
		}

		// Create Branch
		Branch branch = falseBranch == null ? new Branch(decision, trueBranch)
				: new Branch(decision, trueBranch, falseBranch);

		// Propagate Inputs from decision, true branch and false branch
		ComponentUtil.propagateDataPorts(branch, inputs, dDataPorts);
		ComponentUtil.propagateDataPorts(branch, inputs, tDataPorts);
		ComponentUtil.propagateDataPorts(branch, inputs, eDataPorts);

		// Create DataBuses for true Branch
		for (Var var : tDataBuses.keySet()) {
			Type type = var.getType();
			Bus bus = tDataBuses.get(var);
			// Connect
			Bus bDataBus = branch.getExit(Exit.DONE).makeDataBus(var.getName(),
					type.getSizeInBits(), type.isInt());
			Port bDataBusPeer = bDataBus.getPeer();
			ComponentUtil.connectDataDependency(bus, bDataBusPeer, 0);
			outputs.put(var, bDataBus);
		}

		// -- True branch control dependencies
		Bus doneBus = trueBranch.getExit(Exit.DONE).getDoneBus();
		Port donePort = branch.getExit(Exit.DONE).getDoneBus().getPeer();
		List<Entry> entries = donePort.getOwner().getEntries();
		Entry entry = entries.get(0);
		Dependency dep = new ControlDependency(doneBus);
		entry.addDependency(donePort, dep);

		// Create DataBuses for false Branch
		for (Var var : eDataBuses.keySet()) {
			Type type = var.getType();
			Bus bus = eDataBuses.get(var);
			Bus bDataBus = null;
			if (outputs.containsKey(var)) {
				bDataBus = outputs.get(var);
			} else {
				// Connect
				bDataBus = branch.getExit(Exit.DONE).makeDataBus(var.getName(),
						type.getSizeInBits(), type.isInt());
				outputs.put(var, bDataBus);
			}
			Port bDataBusPeer = bDataBus.getPeer();
			ComponentUtil.connectDataDependency(bus, bDataBusPeer, 1);
		}

		// -- False branch control dependencies
		if (falseBranch != null) {
			doneBus = falseBranch.getExit(Exit.DONE).getDoneBus();
			donePort = branch.getExit(Exit.DONE).getDoneBus().getPeer();
			entries = donePort.getOwner().getEntries();
			entry = entries.get(1);
			dep = new ControlDependency(doneBus);
			entry.addDependency(donePort, dep);
		}
		
		blockIf.setAttribute("inputs", inputs);
		blockIf.setAttribute("outputs", outputs);

		Procedure procedure = EcoreHelper.getContainerOfType(blockIf, Procedure.class);
		IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(), blockIf.getLineNumber());
		branch.setIDSourceInfo(sinfo);
		return branch;
	}

}
