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
package org.xronos.orcc.forge.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.IrUtil;

import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * This visitor construct a list of blocks that handles the action selection
 * 
 * @author Endri Bezati
 *
 */
public class ActionSelection extends DfVisitor<List<Block>> {

	/**
	 * The scheduler procedure
	 */
	Procedure scheduler;

	public ActionSelection(Procedure scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public List<Block> caseActor(Actor actor) {
		List<Block> blocks = new ArrayList<Block>();
		
		
		// TODO : Create a Block if for each state
		// FIXME: delete the following code
		for (Action action : actor.getActions()) {
			doSwitch(action);
		}

		return blocks;
	}

	@Override
	public List<Block> caseAction(Action action) {
		List<Block> blocks = new ArrayList<Block>();

		// -- Block Basic with InstCall
		BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();
		InstCall instCall = IrFactory.eINSTANCE.createInstCall();
		instCall.setProcedure(action.getBody());
		block.add(instCall);

		// -- Create portTokenIndex for each port on the output pattern
		if (action.getOutputPattern() != null) {
			for (Port port : action.getOutputPattern().getPorts()) {
				Var portIndex = null;
				if (scheduler.getLocal(port.getName() + "TokenIndex") != null) {
					portIndex = scheduler.getLocal(port.getName()
							+ "TokenIndex");
				} else {
					portIndex = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeInt(), port.getName()
									+ "TokenIndex", true, 0);
					scheduler.addLocal(portIndex);
				}
				ExprInt value = IrFactory.eINSTANCE.createExprInt(0);
				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
						portIndex, value);
				block.add(assign);
			}
		}

		// -- Add block to blocks
		blocks.add(block);

		// -- Build output pattern while Block
		if (action.getOutputPattern() != null) {
			if (action.getOutputPattern().getPorts().size() == 1) {
				blocks.addAll(doSwitch(action.getOutputPattern()));
			} else {
				BlockMutex mutex = XronosIrFactory.eINSTANCE.createBlockMutex();
				mutex.getBlocks().addAll(doSwitch(action.getOutputPattern()));
				blocks.add(mutex);
			}
		}
		return blocks;
	}

	@Override
	public List<Block> casePattern(Pattern pattern) {
		List<Block> blocks = new ArrayList<Block>();
		for (Port port : pattern.getPorts()) {

			if (pattern.getNumTokens(port) > 1) {
				BlockBasic blockBasic = IrFactory.eINSTANCE.createBlockBasic();
				Var target = null;
				if (scheduler.getLocal(port.getName() + "PortStatus") != null) {
					target = scheduler.getLocal(port.getName() + "PortStatus");
				} else {
					target = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeBool(),
							port.getName() + "PortStatus", true, 0);
					scheduler.addLocal(target);
				}
				// -- Create Port Status
				InstPortStatus portStatus = XronosIrFactory.eINSTANCE
						.createInstPortStatus();
				Def def = IrFactory.eINSTANCE.createDef(target);
				portStatus.setTarget(def);
				portStatus.setPort(port);
				blockBasic.add(portStatus);
				blocks.add(blockBasic);

				// -- Create Block If
				Expression condition = IrFactory.eINSTANCE
						.createExprVar(target);
				BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
				blockIf.setCondition(condition);
				// -- Create Block basic for the then Block
				blockBasic = IrFactory.eINSTANCE.createBlockBasic();

				// -- Create Load and then Port Write
				if (scheduler.getLocal(port.getName() + "Portvalue") != null) {
					target = scheduler.getLocal(port.getName() + "Portvalue");
				} else {
					Type type = IrUtil.copy(port.getType());
					target = IrFactory.eINSTANCE.createVar(type, port.getName()
							+ "Portvalue", true, 0);
					scheduler.addLocal(target);
				}

				def = IrFactory.eINSTANCE.createDef(target);
				Var source = pattern.getPortToVarMap().get(port);
				InstLoad load = IrFactory.eINSTANCE.createInstLoad(target,
						source, 0);
				blockBasic.add(load);

				InstPortWrite portWrite = XronosIrFactory.eINSTANCE
						.createInstPortWrite();
				portWrite.setPort(port);
				Expression value = IrFactory.eINSTANCE.createExprVar(target);
				portWrite.setValue(value);
				blockBasic.add(portWrite);

				// -- Add blockBasic to BlockIf then Blocks
				blockIf.getThenBlocks().add(blockBasic);
				// -- Add blockIf to Blocks
				blocks.add(blockIf);
			} else {
				// -- Create While Condition
				Var tokenIndex = scheduler.getLocal(port.getName()
						+ "TokenIndex");
				Expression E1 = IrFactory.eINSTANCE.createExprVar(tokenIndex);
				Expression E2 = IrFactory.eINSTANCE.createExprInt(pattern
						.getNumTokens(port));
				Expression condition = IrFactory.eINSTANCE.createExprBinary(E1,
						OpBinary.LE, E2, IrFactory.eINSTANCE.createTypeBool());

				// -- Create While
				BlockWhile blockWhile = IrFactory.eINSTANCE.createBlockWhile();
				blockWhile.setCondition(condition);

				// Create BlockBasic for PinStatus
				BlockBasic blockBasic = IrFactory.eINSTANCE.createBlockBasic();
				Var target = null;
				if (scheduler.getLocal(port.getName() + "PortStatus") != null) {
					target = scheduler.getLocal(port.getName() + "PortStatus");
				} else {
					target = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeBool(),
							port.getName() + "PortStatus", true, 0);
					scheduler.addLocal(target);
				}
				// -- Create Port Status
				InstPortStatus portStatus = XronosIrFactory.eINSTANCE
						.createInstPortStatus();
				Def def = IrFactory.eINSTANCE.createDef(target);
				portStatus.setTarget(def);
				portStatus.setPort(port);
				blockBasic.add(portStatus);
				blockWhile.getBlocks().add(blockBasic);

				// -- Create Block If
				condition = IrFactory.eINSTANCE.createExprVar(target);
				BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
				blockIf.setCondition(condition);
				// -- Create Block basic for the then Block
				blockBasic = IrFactory.eINSTANCE.createBlockBasic();

				// -- Create Load and then Port Write
				if (scheduler.getLocal(port.getName() + "Portvalue") != null) {
					target = scheduler.getLocal(port.getName() + "Portvalue");
				} else {
					Type type = IrUtil.copy(port.getType());
					target = IrFactory.eINSTANCE.createVar(type, port.getName()
							+ "Portvalue", true, 0);
					scheduler.addLocal(target);
				}

				def = IrFactory.eINSTANCE.createDef(target);
				Var source = pattern.getPortToVarMap().get(port);

				Expression index = IrFactory.eINSTANCE
						.createExprVar(tokenIndex);
				InstLoad load = IrFactory.eINSTANCE.createInstLoad(target,
						source, Arrays.asList(index));
				blockBasic.add(load);

				InstPortWrite portWrite = XronosIrFactory.eINSTANCE
						.createInstPortWrite();
				portWrite.setPort(port);
				Expression value = IrFactory.eINSTANCE.createExprVar(target);
				portWrite.setValue(value);
				blockBasic.add(portWrite);

				// -- Add blockBasic to BlockIf then Blocks
				blockIf.getThenBlocks().add(blockBasic);
				// -- Add to block while blocks
				blockWhile.getBlocks().add(blockIf);
				// -- Add to blocks the while block
				blocks.add(blockWhile);
			}
		}

		return blocks;
	}

}
