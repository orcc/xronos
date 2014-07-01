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
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */

package org.xronos.orcc.backend.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class LoadOnce extends AbstractIrVisitor<Void> {

	List<Instruction> loadStores;
	List<Instruction> loadProcessed;

	Map<InstLoad, Var> newTargets;

	@Override
	public Void caseInstLoad(InstLoad load) {
		loadStores.add(load);
		return null;
	}

	@Override
	public Void caseInstStore(InstStore store) {
		loadStores.add(store);
		return super.caseInstStore(store);
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		loadStores = new ArrayList<Instruction>();
		loadProcessed = new ArrayList<Instruction>();
		newTargets = new HashMap<InstLoad, Var>();
		super.caseProcedure(procedure);

		// Find Same Loads
		for (int i = 0; i < loadStores.size(); i++) {
			Instruction inst = loadStores.get(i);
			Var stateVar = null;
			Var tempTarget = null;
			EList<Expression> indexes = null;
			if (inst.isInstLoad()) {
				InstLoad currentLoad = (InstLoad) inst;
				if (!loadProcessed.contains(currentLoad)) {
					stateVar = currentLoad.getSource().getVariable();
					tempTarget = currentLoad.getTarget().getVariable();
					indexes = currentLoad.getIndexes();
					for (int j = i + 1; j < loadStores.size(); j++) {
						Instruction otherInst = loadStores.get(j);
						if (otherInst.isInstLoad()) {
							InstLoad load = (InstLoad) otherInst;
							if (!loadProcessed.contains(load)) {
								Var source = load.getSource().getVariable();
								EList<Expression> otherIndex = load
										.getIndexes();
								if (source == stateVar) {
									if (indexes != null) {
										if (!indexes.isEmpty()) {

											if (otherIndex.size() == indexes
													.size()) {
												boolean sameIndex = true;
												for (int e = 0; e < indexes
														.size(); e++) {

													Expression expr = indexes
															.get(e);
													Expression exprOther = otherIndex
															.get(e);

													XronosExprEvaluator exprEvaluator = new XronosExprEvaluator();

													Object value = exprEvaluator
															.doSwitch(expr);

													Object otherValue = exprEvaluator
															.doSwitch(exprOther);

													if (value != otherValue) {
														sameIndex = false;
													}
												}

												if (sameIndex) {
													EList<Use> targetUses = load
															.getTarget()
															.getVariable()
															.getUses();
													while (!targetUses
															.isEmpty()) {
														ExprVar expr = EcoreHelper
																.getContainerOfType(
																		targetUses
																				.get(0),
																		ExprVar.class);
														ExprVar exprValue = IrFactory.eINSTANCE
																.createExprVar(tempTarget);
														EcoreUtil
																.replace(
																		expr,
																		IrUtil.copy(exprValue));
														IrUtil.delete(expr);
													}
													loadProcessed.add(load);
													IrUtil.delete(load);
												}
											}

											// TODO
										} else {
											EList<Use> targetUses = load
													.getTarget().getVariable()
													.getUses();
											while (!targetUses.isEmpty()) {
												ExprVar expr = EcoreHelper
														.getContainerOfType(
																targetUses
																		.get(0),
																ExprVar.class);
												ExprVar exprValue = IrFactory.eINSTANCE
														.createExprVar(tempTarget);
												EcoreUtil.replace(expr,
														IrUtil.copy(exprValue));
												IrUtil.delete(expr);
											}
											loadProcessed.add(load);
											IrUtil.delete(load);
										}
									}
								}
							}
						} else if (otherInst.isInstStore()) {
							InstStore store = (InstStore) otherInst;
							Var target = store.getTarget().getVariable();
							if (target == stateVar) {
								break;
							}
						}
					}

				}
			}
		}
		return null;
	}
}
