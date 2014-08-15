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

import java.math.BigInteger;
import java.util.ArrayList;
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
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.ir.util.TypeUtil;

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
import org.xronos.openforge.util.Debug;

/**
 * This visitor transforms an Expression to a LIM Component
 * 
 * @author Endri Bezati
 * 
 */
public class ExprToComponent extends AbstractIrVisitor<Component> {

	@Override
	public Component caseExprBinary(ExprBinary expr) {
		Map<Var, Port> inputs = new HashMap<Var, Port>();
		Expression E1 = expr.getE1();
		Expression E2 = expr.getE2();

		Component block = null;
		Component compE1 = null;
		Component compE2 = null;
		CastOp castE1 = null;
		CastOp castE2 = null;
		Component op = null;

		// Get E1 component
		compE1 = new ExprToComponent().doSwitch(E1);

		// Get E2 component
		compE2 = new ExprToComponent().doSwitch(E2);

		// Create List of Block Sequence
		List<Component> sequence = new ArrayList<Component>();

		// Get Least Upper Bound of E1 and E2
		Type castType = null;
		if (expr.getType().isBool()) {
			castType = TypeUtil.getLub(E1.getType(), E2.getType());
		} else {
			castType = IrUtil.copy(expr.getType());
		}
		// Add E1
		sequence.add(compE1);
		if (E1.getType() != castType) {
			// Cast Operator input mix max size of the expression output
			castE1 = new CastOp(castType.getSizeInBits(), castType.isInt());
			sequence.add(castE1);
		}
		// Add E2
		sequence.add(compE2);
		// Cast Operator input mix max size of the expression output
		if (E2.getType() != castType) {
			castE2 = new CastOp(castType.getSizeInBits(), castType.isInt());
			sequence.add(castE2);
		}

		// Get Binary Component
		op = ComponentUtil.createExprBinComponent(expr);
		sequence.add(op);

		CastOp castResult = null;
		if (!expr.getType().isBool()) {
			castResult = new CastOp(expr.getType().getSizeInBits(), expr
					.getType().isInt());
			sequence.add(castResult);
		}
		// Create Block with the given sequence
		block = new Block(sequence);

		@SuppressWarnings("unchecked")
		Map<Var, Port> compInputsE1 = (Map<Var, Port>) E1
				.getAttribute("inputs").getObjectValue();
		for (Var var : compInputsE1.keySet()) {
			Port portBlock = null;
			if (inputs.containsKey(var)) {
				portBlock = inputs.get(var);
			} else {
				Type type = var.getType();
				portBlock = block.makeDataPort(var.getName(),
						type.getSizeInBits(), type.isInt());
				// Add to Expression inputs
				inputs.put(var, portBlock);
			}
			Bus busBlock = portBlock.getPeer();

			ComponentUtil.connectDataDependency(busBlock,
					compInputsE1.get(var), 0);
		}
		// Connect the component inputs
		@SuppressWarnings("unchecked")
		Map<Var, Port> compInputsE2 = (Map<Var, Port>) E2
				.getAttribute("inputs").getObjectValue();
		for (Var var : compInputsE2.keySet()) {
			Port portBlock = null;
			if (inputs.containsKey(var)) {
				portBlock = inputs.get(var);
			} else {
				Type type = var.getType();
				portBlock = block.makeDataPort(var.getName(),
						type.getSizeInBits(), type.isInt());
				// Add to Expression inputs
				inputs.put(var, portBlock);
			}
			Bus busBlock = portBlock.getPeer();
			ComponentUtil.connectDataDependency(busBlock,
					compInputsE2.get(var), 0);
		}

		// Result Bus
		// Connect output
		Type type = expr.getType();
		Exit exit = block.getExit(Exit.DONE);
		Bus resultBus = exit.makeDataBus(type.getSizeInBits(), type.isInt());
		Port resultPort = resultBus.getPeer();

		// Connect Operands and operator components
		// Get Exit
		Exit exitOP = op.getExit(Exit.DONE);
		// Get the result bus
		Bus resultBusOP = exitOP.getDataBuses().iterator().next();

		// Add cast
		if (castResult != null) {
			ComponentUtil.connectDataDependency(resultBusOP,
					castResult.getDataPort());

			ComponentUtil.connectDataDependency(castResult.getResultBus(),
					resultPort, 0);
		} else {
			ComponentUtil.connectDataDependency(resultBusOP, resultPort, 0);
		}

		if (E1.getType() != castType) {
			// Connect the Result Bus of the compE1 to Cast
			Port castDataPort = castE1.getDataPort();
			Bus opResultBus = compE1.getExit(Exit.DONE).getDataBuses().get(0);
			ComponentUtil.connectDataDependency(opResultBus, castDataPort, 0);

			// Connect the casts Result Bus to the Left Data Port
			Bus castResultBus = castE1.getResultBus();
			Port dataPort = ((BinaryOp) op).getLeftDataPort();
			ComponentUtil.connectDataDependency(castResultBus, dataPort, 0);
		} else {
			Bus opResultBus = compE1.getExit(Exit.DONE).getDataBuses().get(0);
			Port dataPort = ((BinaryOp) op).getLeftDataPort();
			ComponentUtil.connectDataDependency(opResultBus, dataPort, 0);
		}

		if (E2.getType() != castType) {
			// Connect the Result Bus of the compE2 to Cast
			Port castDataPort = castE2.getDataPort();
			Bus opResultBus = compE2.getExit(Exit.DONE).getDataBuses().get(0);
			ComponentUtil.connectDataDependency(opResultBus, castDataPort, 0);

			// Connect the casts Result Bus to the Left Data Port
			Bus castResultBus = castE2.getResultBus();
			Port dataPortE2 = ((BinaryOp) op).getRightDataPort();
			ComponentUtil.connectDataDependency(castResultBus, dataPortE2, 0);
		} else {
			Bus opResultBus = compE2.getExit(Exit.DONE).getDataBuses().get(0);
			Port dataPort = ((BinaryOp) op).getRightDataPort();
			ComponentUtil.connectDataDependency(opResultBus, dataPort, 0);
		}
		expr.setAttribute("inputs", inputs);
		return block;
	}

	@Override
	public Component caseExprBool(ExprBool object) {
		Map<Var, Port> inputs = new HashMap<Var, Port>();
		object.setAttribute("inputs", inputs);
		final long value = object.isValue() ? 1 : 0;
		return new SimpleConstant(value, 1, true);
	}

	@Override
	public Component caseExprInt(ExprInt object) {
		Map<Var, Port> inputs = new HashMap<Var, Port>();
		object.setAttribute("inputs", inputs);
		BigInteger value = object.getValue();
		int sizeInBits = object.getType().getSizeInBits();
		boolean isSigned = object.getType().isInt();

		return new SimpleConstant(value, sizeInBits, isSigned);
	}

	@Override
	public Component caseExprUnary(ExprUnary expr) {
		Map<Var, Port> inputs = new HashMap<Var, Port>();

		// Create the sequence of components
		List<Component> sequence = new ArrayList<Component>();
		Component comp = new ExprToComponent().doSwitch(expr.getExpr());
		sequence.add(comp);

		UnaryOp opUnary = ComponentUtil.createExprUnaryComponent(expr);
		sequence.add(opUnary);

		// Create a new Block
		Block block = new Block(sequence);

		// Set Data Dependencies between components
		Exit compExit = comp.getExit(Exit.DONE);
		// Only one output possible
		Bus compResultBus = compExit.getDataBuses().get(0);
		// Get DataPort of UnaryOp
		Port portOpUnary = opUnary.getDataPort();
		ComponentUtil.connectDataDependency(compResultBus, portOpUnary, 0);

		// Connect comp inputs with new Ports of blocks
		@SuppressWarnings("unchecked")
		Map<Var, Port> compInputs = (Map<Var, Port>) expr.getExpr()
				.getAttribute("inputs").getObjectValue();
		for (Var var : compInputs.keySet()) {
			Type type = var.getType();
			Port portBlock = block.makeDataPort(type.getSizeInBits(),
					type.isInt());
			Bus busBlock = portBlock.getPeer();

			ComponentUtil.connectDataDependency(busBlock, compInputs.get(var),
					0);

			// Add to Expression inputs
			inputs.put(var, portBlock);
		}

		// Create Block Exit
		Exit blockExit = block.getExit(Exit.DONE);
		Type type = expr.getType();
		Bus blockResultBus = blockExit.makeDataBus(type.getSizeInBits(),
				type.isInt());
		Port blockResultPort = blockResultBus.getPeer();

		// Connect opUnary ResultBus with Block Result Port
		Bus resultBusOpUnary = ((UnaryOp) opUnary).getResultBus();
		ComponentUtil.connectDataDependency(resultBusOpUnary, blockResultPort,
				0);

		expr.setAttribute("inputs", inputs);
		return block;
	}

	@Override
	public Component caseExprVar(ExprVar object) {
		Map<Var, Port> inputs = new HashMap<Var, Port>();
		Var var = object.getUse().getVariable();
		Component comp = new NoOp(1, Exit.DONE);
		Port dataPort = comp.getDataPorts().get(0);
		dataPort.setIDLogical(var.getName());
		inputs.put(var, dataPort);
		object.setAttribute("inputs", inputs);

		return comp;
	}

}
