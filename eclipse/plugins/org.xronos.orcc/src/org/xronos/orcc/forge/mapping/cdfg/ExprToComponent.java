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
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.NoOp;
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

		Var varE1 = null;
		Var varE2 = null;

		Component block = null;
		Component compE1 = null;
		Component compE2 = null;
		NoOp noopE1 = null;
		NoOp noopE2 = null;
		CastOp castE1 = null;
		CastOp castE2 = null;
		Component op = null;

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

		op = ComponentUtil.createExprBinComponent(expr);

		// Create List of Block Sequence
		List<Component> sequence = new ArrayList<Component>();

		// Add NoOp and Cast or only Cast
		if (compE1 != null) {
			sequence.add(compE1);
			// Cast Operator input mix max size of the expression output
			Type type = expr.getType();
			castE1 = new CastOp(type.getSizeInBits(), type.isInt()
					|| type.isBool());
			sequence.add(castE1);
		} else {
			noopE1 = new NoOp(1, Exit.DONE);
			sequence.add(noopE1);
			// Cast Operator input mix max size of the expression output
			Type type = expr.getType();
			castE1 = new CastOp(type.getSizeInBits(), type.isInt()
					|| type.isBool());
			sequence.add(castE1);
		}

		if (compE2 != null) {
			sequence.add(compE2);
			// Cast Operator input mix max size of the expression output
			Type type = expr.getType();
			castE2 = new CastOp(type.getSizeInBits(), type.isInt()
					|| type.isBool());
			sequence.add(castE2);
		} else {
			noopE2 = new NoOp(1, Exit.DONE);
			sequence.add(noopE2);
			// Cast Operator input mix max size of the expression output
			Type type = expr.getType();
			castE2 = new CastOp(type.getSizeInBits(), type.isInt()
					|| type.isBool());
			sequence.add(castE2);
		}
		sequence.add(op);

		// Create Block with the given sequence
		block = new Block(sequence);

		// Create Data Dependencies between components
		// Left Data Port
		if (varE1 != null) {
			Type type = varE1.getType();
			// Create DataPort and Get the bus
			Port dataPort = block.makeDataPort(varE1.getName(),
					type.getSizeInBits(), type.isInt() || type.isBool());
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
			}
		}
		// Right Data Port
		if (varE2 != null) {
			Type type = varE2.getType();
			// Create DataPort and Get the bus
			Port dataPort = block.makeDataPort(varE2.getName(),
					type.getSizeInBits(), type.isInt() || type.isBool());
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
		Exit exitOP = op.getExit(Exit.DONE);
		// Get the result bus
		Bus resultBusOP = exitOP.getDataBuses().iterator().next();
		ComponentUtil.connectDataDependency(resultBusOP, resultPort, 0);

		// Connect Everything
		if (varE1 != null) {
			// Connect Block data Port with NoOp Data Port
			Port blkDataport = inputs.get(varE1);
			Bus blkDataBus = blkDataport.getPeer();
			Port noopDataPort = noopE1.getDataPorts().get(0);
			ComponentUtil.connectDataDependency(blkDataBus, noopDataPort, 0);

			// Connect Cast with NoOp result Bus
			Bus noopResultBus = noopE1.getResultBus();
			Port castDataPort = castE1.getDataPort();
			ComponentUtil.connectDataDependency(noopResultBus, castDataPort, 0);

			// Connect the casts Result Bus to the Left Data Port
			Bus castResultBus = castE1.getResultBus();
			Port dataPortE1 = ((BinaryOp) op).getLeftDataPort();
			ComponentUtil.connectDataDependency(castResultBus, dataPortE1, 0);

		} else {
			// Connect the Result Bus of the compE1 to Cast
			Port castDataPort = castE1.getDataPort();
			Bus opResultBus = compE1.getExit(Exit.DONE).getDataBuses().get(0);
			ComponentUtil.connectDataDependency(opResultBus, castDataPort, 0);

			// Connect the casts Result Bus to the Left Data Port
			Bus castResultBus = castE1.getResultBus();
			Port dataPort = ((BinaryOp) op).getLeftDataPort();
			ComponentUtil.connectDataDependency(castResultBus, dataPort, 0);
		}

		if (varE2 != null) {
			// Connect Block data Port with NoOp Data Port
			Port blkDataport = inputs.get(varE2);
			Bus blkDataBus = blkDataport.getPeer();
			Port noopDataPort = noopE2.getDataPorts().get(0);
			ComponentUtil.connectDataDependency(blkDataBus, noopDataPort, 0);

			// Connect Cast with NoOp result Bus
			Bus noopResultBus = noopE2.getResultBus();
			Port castDataPort = castE2.getDataPort();
			ComponentUtil.connectDataDependency(noopResultBus, castDataPort, 0);

			// Connect the casts Result Bus to the Left Data Port
			Bus castResultBus = castE2.getResultBus();
			Port dataPort = ((BinaryOp) op).getLeftDataPort();
			ComponentUtil.connectDataDependency(castResultBus, dataPort, 0);
		} else {
			// Connect the Result Bus of the compE1 to Cast
			Port castDataPort = castE2.getDataPort();
			Bus opResultBus = compE2.getExit(Exit.DONE).getDataBuses().get(0);
			ComponentUtil.connectDataDependency(opResultBus, castDataPort, 0);

			// Connect the casts Result Bus to the Left Data Port
			Bus castResultBus = castE2.getResultBus();
			Port dataPortE2 = ((BinaryOp) op).getRightDataPort();
			ComponentUtil.connectDataDependency(castResultBus, dataPortE2, 0);
		}

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
