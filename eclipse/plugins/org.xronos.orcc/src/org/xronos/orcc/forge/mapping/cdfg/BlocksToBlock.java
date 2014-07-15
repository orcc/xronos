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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.eclipse.emf.ecore.EObject;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.MutexBlock;
import org.xronos.openforge.lim.Port;
import org.xronos.orcc.ir.BlockMutex;

/**
 * This visitor takes a list of {@link net.sf.orcc.ir.Block}s and creates a new
 * LIM {@link Block} and resolve its data dependencies
 * 
 * @author Endri Bezati
 * 
 */
public class BlocksToBlock extends AbstractIrVisitor<Component> {

	/**
	 * The Bus variable for each dependency
	 */
	private Map<Bus, Var> busDependecies;

	/**
	 * Set of Block inputs
	 */
	private Map<Var, Port> inputs;

	/**
	 * If this set of blocks is from a procedure body
	 */
	private boolean isActionBody;

	/**
	 * 
	 */
	private boolean isBlockMutex;

	/**
	 * Set of Block outputs
	 */
	private Map<Var, Bus> outputs;

	/**
	 * The port variable for each dependency
	 */
	private Map<Port, Var> portDependecies;

	/**
	 * Target Output data bus
	 */

	private Var target;

	/**
	 * 
	 * @param inputs
	 * @param outputs
	 * @param isActionBody
	 */

	public BlocksToBlock(Map<Var, Port> inputs, Map<Var, Bus> outputs,
			boolean isActionBody) {
		this.inputs = inputs;
		this.outputs = outputs;
		this.isActionBody = isActionBody;
		portDependecies = new HashMap<Port, Var>();
		busDependecies = new HashMap<Bus, Var>();
		this.isBlockMutex = false;
	}

	public BlocksToBlock(Map<Var, Port> inputs, Map<Var, Bus> outputs,
			boolean isActionBody, boolean isBlockMutex) {
		this(inputs, outputs, isActionBody);
		this.isBlockMutex = isBlockMutex;
	}

	public BlocksToBlock(Map<Var, Port> inputs, Map<Var, Bus> outputs,
			Var target) {
		this(inputs, outputs, false);
		this.target = target;
	}

	/**
	 * Add to inputs if the variable has not been already added
	 * 
	 * @param var
	 *            the variable
	 * @param port
	 *            the port
	 */
	private void addToInputs(Var var, Port port) {
		if (!inputs.containsKey(var)) {
			inputs.put(var, port);
		}
	}

	@Override
	public Component caseBlockBasic(BlockBasic block) {
		Component component = new BlockBasicToBlock().doSwitch(block);

		// Set port and bus dependencies
		// -- Inputs
		@SuppressWarnings("unchecked")
		Map<Var, Port> blockInputs = (Map<Var, Port>) block.getAttribute(
				"inputs").getObjectValue();
		for (Var var : blockInputs.keySet()) {
			portDependecies.put(blockInputs.get(var), var);
		}

		// -- Outputs
		@SuppressWarnings("unchecked")
		Map<Var, Bus> blockOutputs = (Map<Var, Bus>) block.getAttribute(
				"outputs").getObjectValue();
		for (Var var : blockOutputs.keySet()) {
			busDependecies.put(blockOutputs.get(var), var);
		}

		return component;
	}

	@Override
	public Component caseBlockIf(BlockIf blockIf) {
		Component component = new BlockIfToBranch().doSwitch(blockIf);

		// Set port and bus dependencies
		// -- Inputs
		@SuppressWarnings("unchecked")
		Map<Var, Port> blockInputs = (Map<Var, Port>) blockIf.getAttribute(
				"inputs").getObjectValue();
		for (Var var : blockInputs.keySet()) {
			portDependecies.put(blockInputs.get(var), var);
		}

		// -- Outputs
		@SuppressWarnings("unchecked")
		Map<Var, Bus> blockOutputs = (Map<Var, Bus>) blockIf.getAttribute(
				"outputs").getObjectValue();
		for (Var var : blockOutputs.keySet()) {
			busDependecies.put(blockOutputs.get(var), var);
		}

		return component;
	}

	public Component defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			return caseBlockMutex((BlockMutex) object);
		}

		return super.defaultCase(object);
	}

	public Component caseBlockMutex(BlockMutex blockMutex) {
		Component component = new BlockMutexToBlock().doSwitch(blockMutex);

		// Set port and bus dependencies
		// -- Inputs
		@SuppressWarnings("unchecked")
		Map<Var, Port> blockInputs = (Map<Var, Port>) blockMutex.getAttribute(
				"inputs").getObjectValue();
		for (Var var : blockInputs.keySet()) {
			portDependecies.put(blockInputs.get(var), var);
		}

		// -- Outputs
		@SuppressWarnings("unchecked")
		Map<Var, Bus> blockOutputs = (Map<Var, Bus>) blockMutex.getAttribute(
				"outputs").getObjectValue();
		for (Var var : blockOutputs.keySet()) {
			busDependecies.put(blockOutputs.get(var), var);
		}
		return component;
	}

	@Override
	public Component caseBlockWhile(BlockWhile blockWhile) {
		Component component = new BlockWhileToLoop().doSwitch(blockWhile);

		// Set port and bus dependencies
		// -- Inputs
		@SuppressWarnings("unchecked")
		Map<Var, Port> blockInputs = (Map<Var, Port>) blockWhile.getAttribute(
				"inputs").getObjectValue();
		for (Var var : blockInputs.keySet()) {
			portDependecies.put(blockInputs.get(var), var);
		}

		// -- Outputs
		@SuppressWarnings("unchecked")
		Map<Var, Bus> blockOutputs = (Map<Var, Bus>) blockWhile.getAttribute(
				"outputs").getObjectValue();
		for (Var var : blockOutputs.keySet()) {
			busDependecies.put(blockOutputs.get(var), var);
		}
		return component;
	}

	@Override
	public Component visitBlocks(List<net.sf.orcc.ir.Block> blocks) {
		int oldIndexBlock = indexBlock;
		Component result = null;
		List<Component> sequence = new ArrayList<Component>();
		for (indexBlock = 0; indexBlock < blocks.size(); indexBlock++) {
			net.sf.orcc.ir.Block block = blocks.get(indexBlock);
			result = doSwitch(block);
			if (result != null) {
				sequence.add(result);
			}
		}

		// Create a new Block with the sequence of the components
		Block block = isBlockMutex ? new MutexBlock(sequence, isActionBody)
				: new Block(sequence, isActionBody);

		// A map that contains the last defined variable associated with a Bus

		Map<Var, Bus> lastDefinedVarBus = new HashMap<Var, Bus>();

		// Dependencies

		// -- Inputs
		for (Component component : sequence) {
			// Data ports
			List<Port> dataPorts = component.getDataPorts();
			for (Port port : dataPorts) {
				Var var = portDependecies.get(port);
				if(var == null){
					throw new NullPointerException("Var is Null");
				}
				if (lastDefinedVarBus.containsKey(var)) {
					Bus bus = lastDefinedVarBus.get(var);
					ComponentUtil.connectDataDependency(bus, port);
				} else {
					Type type = var.getType();

					Port blkDataPort = block.makeDataPort(var.getName(),
							type.getSizeInBits(), type.isInt());
					// This is Port is an input, add to the list
					addToInputs(var, blkDataPort);

					Bus blkDataPortPeer = blkDataPort.getPeer();
					ComponentUtil.connectDataDependency(blkDataPortPeer, port,
							0);
				}
			}

			// Data buses
			List<Bus> dataBuses = component.getExit(Exit.DONE).getDataBuses();
			for (Bus bus : dataBuses) {
				Var var = busDependecies.get(bus);
				lastDefinedVarBus.put(var, bus);
			}
		}

		// -- Outputs

		// Build the current block outputs
		// Do not create outputs if this block list comes from an action body
		if (!isActionBody) {
			if (target == null) {
				ListIterator<Component> iter = sequence.listIterator(sequence
						.size());

				while (iter.hasPrevious()) {
					Component component = iter.previous();
					List<Bus> dataBuses = component.getExit(Exit.DONE)
							.getDataBuses();
					for (Bus bus : dataBuses) {
						Var var = busDependecies.get(bus);
						if (!outputs.containsKey(var)) {
							Type type = var.getType();
							Bus blkOutputBus = block.getExit(Exit.DONE)
									.makeDataBus(var.getName(),
											type.getSizeInBits(), type.isInt());
							Port blkOutputPort = blkOutputBus.getPeer();
							// Add dependency
							ComponentUtil.connectDataDependency(bus,
									blkOutputPort, 0);

							outputs.put(var, blkOutputBus);
						}
					}
				}
			} else {
				// Create only one output, function in-lining
				ListIterator<Component> iter = sequence.listIterator(sequence
						.size());
				// Get last Component, it has only one data bus
				Component component = iter.previous();
				Bus resultBus = component.getExit(Exit.DONE).getDataBuses()
						.get(0);

				Type type = target.getType();
				Bus blkOutputBus = block.getExit(Exit.DONE).makeDataBus(
						target.getName(), type.getSizeInBits(), type.isInt());
				Port blkOutputPort = blkOutputBus.getPeer();
				// Add dependency
				ComponentUtil
						.connectDataDependency(resultBus, blkOutputPort);

				outputs.put(target, blkOutputBus);
			}
		}

		indexBlock = oldIndexBlock;
		return block;
	}

}
