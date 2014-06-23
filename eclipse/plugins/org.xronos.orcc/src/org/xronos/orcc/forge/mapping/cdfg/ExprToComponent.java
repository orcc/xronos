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
 */
package org.xronos.orcc.forge.mapping.cdfg;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.op.BinaryOp;
import org.xronos.openforge.lim.op.SimpleConstant;
import org.xronos.openforge.lim.op.UnaryOp;

/**
 * This visitor transforms an Expression to a LIM Block
 * 
 * @author Endri Bezati
 * 
 */
public class ExprToComponent extends AbstractIrVisitor<Component> {

	/**
	 * Set of expression inputs, expression can not have an output, given by the
	 * assign later
	 */
	Map<Var, Port> inputs;

	public ExprToComponent() {
		inputs = new HashMap<Var, Port>();
	}

	@Override
	public Component caseExprBinary(ExprBinary expr) {
		Expression E1 = expr.getE1();
		Expression E2 = expr.getE2();
		Component block = null;

		Var varE1 = null;
		Var varE2 = null;
		Component compE1 = null;
		Component compE2 = null;

		if (!E1.isExprVar()) {
			compE1 = new ExprToComponent().doSwitch(E1);
		} else {
			varE1 = ((ExprVar) E1).getUse().getVariable();
		}

		if (!E2.isExprVar()) {
			compE2 = new ExprToComponent().doSwitch(E2);
		} else {
			varE2 = ((ExprVar) E2).getUse().getVariable();
		}

		Component op = ComponentUtil.createExprBinComponent(expr);

		// Create List of Block Components
		List<Component> components = new ArrayList<Component>();

		if (compE1 != null) {
			components.add(compE1);
		}

		if (compE2 != null) {
			components.add(compE2);
		}
		components.add(op);

		// Create Block of components
		block = new Block(components);

		// Create Data Dependencies between components
		// Left Data Port
		if (varE1 != null) {
			Type type = varE1.getType();
			// Create DataPort and Get the bus
			Port dataPort = block.makeDataPort(varE1.getName(),
					type.getSizeInBits(), type.isInt() || type.isBool());
			Bus dataBus = dataPort.getPeer();
			// Get the left data port of a binary Op
			Port dataPortE1 = ((BinaryOp) op).getLeftDataPort();
			dataPortE1.setIDLogical(varE1.getName());
			// Connect the blocks data Bus with Binary Op data port
			ComponentUtil.connectDataDependency(dataBus, dataPortE1, 0);

			inputs.put(varE1, dataPort);
		} else {
			if (compE1 != null) {
				@SuppressWarnings("unchecked")
				Map<Var, Port> compInputs = (Map<Var, Port>) E1.getAttribute(
						"inputs").getObjectValue();
				for (Var var : compInputs.keySet()) {
					Type type = var.getType();
					Port portBlock = block.makeDataPort(type.getSizeInBits(),
							type.isInt() || type.isBool());
					Bus busBlock = portBlock.getPeer();

					ComponentUtil.connectDataDependency(busBlock,
							compInputs.get(var), 0);
					// Add to Expression inputs
					inputs.put(var, portBlock);
				}
				// Connect the RightDataPort of the input OP
				Bus resultBus = compE1.getExit(Exit.DONE).getDataBuses().get(0);
				Port dataPortE1 = ((BinaryOp) op).getRightDataPort();
				ComponentUtil.connectDataDependency(resultBus, dataPortE1, 0);
			}

		}
		// Right Data Port
		if (varE2 != null) {
			Type type = varE2.getType();
			// Create DataPort and Get the bus
			Port dataPort = block.makeDataPort(varE2.getName(),
					type.getSizeInBits(), type.isInt() || type.isBool());
			Bus dataBus = dataPort.getPeer();
			// Get the left data port of a binary Op
			Port dataPortE2 = ((BinaryOp) op).getRightDataPort();
			dataPortE2.setIDLogical(varE2.getName());
			// Connect the blocks data Bus with Binary Op data port
			ComponentUtil.connectDataDependency(dataBus, dataPortE2, 0);

			inputs.put(varE2, dataPort);
		} else {
			// Connect the component inputs
			if (compE2 != null) {
				@SuppressWarnings("unchecked")
				Map<Var, Port> compInputs = (Map<Var, Port>) E2.getAttribute(
						"inputs").getObjectValue();
				for (Var var : compInputs.keySet()) {
					Type type = var.getType();
					Port portBlock = block.makeDataPort(type.getSizeInBits(),
							type.isInt() || type.isBool());
					Bus busBlock = portBlock.getPeer();

					ComponentUtil.connectDataDependency(busBlock,
							compInputs.get(var), 0);
					// Add to Expression inputs
					inputs.put(var, portBlock);
				}
				// Connect the RightDataPort of the input OP
				Bus resultBus = compE2.getExit(Exit.DONE).getDataBuses().get(0);
				Port dataPortE2 = ((BinaryOp) op).getRightDataPort();
				ComponentUtil.connectDataDependency(resultBus, dataPortE2, 0);
			}
		}

		// Result Bus
		// Connect output
		Type type = expr.getType();
		Exit exit = block.getExit(Exit.DONE);
		Bus resultBus = exit.makeDataBus(type.getSizeInBits(), type.isInt()
				|| type.isBool());
		Port resultPort = resultBus.getPeer();

		// Connect Operands and operator components
		// Get Exit
		Exit exitE1 = op.getExit(Exit.DONE);
		// Get the result bus
		Bus resultBusE1 = exitE1.getDataBuses().iterator().next();
		ComponentUtil.connectDataDependency(resultBusE1, resultPort, 0);

		expr.setAttribute("inputs", inputs);

		return block;
	}

	@Override
	public Component caseExprBool(ExprBool object) {
		object.setAttribute("inputs", inputs);
		final long value = object.isValue() ? 1 : 0;
		return new SimpleConstant(value, 1, true);
	}

	@Override
	public Component caseExprInt(ExprInt object) {
		object.setAttribute("inputs", inputs);
		BigInteger value = object.getValue();
		int sizeInBits = object.getType().getSizeInBits();
		boolean isSigned = object.getType().isInt();

		return new SimpleConstant(value, sizeInBits, isSigned);
	}

	@Override
	public Component caseExprUnary(ExprUnary expr) {
		if (expr.isExprVar()) {
			Component opUnary = ComponentUtil.createExprUnaryComponent(expr);
			Var var = ((ExprVar) expr).getUse().getVariable();
			Port dataPort = ((UnaryOp) opUnary).getDataPort();

			inputs.put(var, dataPort);
			expr.setAttribute("inputs", inputs);
			return opUnary;
		} else {
			Component comp = new ExprToComponent().doSwitch(expr.getExpr());

			Component opUnary = ComponentUtil.createExprUnaryComponent(expr);

			Block block = new Block(Arrays.asList(comp, opUnary));

			// Set Data Dependencies between components
			Exit compExit = comp.getExit(Exit.DONE);
			// Only one output possible
			Bus compResultBus = compExit.getDataBuses().get(0);
			// Get DataPort of UnaryOp
			Port portOpUnary = ((UnaryOp) opUnary).getDataPort();
			ComponentUtil.connectDataDependency(compResultBus, portOpUnary, 0);

			// Connect comp inputs with new Ports of blocks
			@SuppressWarnings("unchecked")
			Map<Var, Port> compInputs = (Map<Var, Port>) expr.getExpr()
					.getAttribute("inputs").getObjectValue();
			for (Var var : compInputs.keySet()) {
				Type type = var.getType();
				Port portBlock = block.makeDataPort(type.getSizeInBits(),
						type.isInt() || type.isBool());
				Bus busBlock = portBlock.getPeer();

				ComponentUtil.connectDataDependency(busBlock,
						compInputs.get(var), 0);

				// Add to Expression inputs
				inputs.put(var, portBlock);
			}

			// Create Block Exit
			Exit blockExit = block.getExit(Exit.DONE);
			Type type = expr.getType();
			Bus blockResultBus = blockExit.makeDataBus(type.getSizeInBits(),
					type.isInt() || type.isBool());
			Port blockResultPort = blockResultBus.getPeer();

			// Connect opUnary ResultBus with Block Result Port
			Bus resultBusOpUnary = ((UnaryOp) opUnary).getResultBus();
			ComponentUtil.connectDataDependency(resultBusOpUnary,
					blockResultPort, 0);

			expr.setAttribute("inputs", inputs);
			return block;
		}

	}

	@Override
	public Component caseExprVar(ExprVar object) {
		// Do nothing
		return null;
	}
}
