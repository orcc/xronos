/*
 * Copyright (c) 2014, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
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
