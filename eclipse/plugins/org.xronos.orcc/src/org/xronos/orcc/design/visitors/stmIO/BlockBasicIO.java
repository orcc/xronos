/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
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
package org.xronos.orcc.design.visitors.stmIO;

import static net.sf.orcc.ir.util.IrUtil.getNameSSA;

import java.util.ArrayList;
import java.util.List;

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.Attribute;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.InstPortPeek;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;

/**
 * 
 * This visitor add the input and the output of variables used and defined in
 * this block. This visitor can be used iff the IR has been transformes with SSA
 * and TAC.
 * 
 * @author Endri Bezati
 * 
 */
public class BlockBasicIO extends AbstractIrVisitor<Void> {

	private BlockBasic block;
	private List<Var> inputs;

	private List<Var> outputs;

	public BlockBasicIO(BlockBasic block) {
		super(true);
		this.block = block;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void caseBlockBasic(BlockBasic block) {
		if (!block.hasAttribute("inputs") && !block.hasAttribute("outputs")) {
			inputs = new ArrayList<Var>();
			outputs = new ArrayList<Var>();
			super.caseBlockBasic(block);

			// Add attributes
			block.setAttribute("inputs", inputs);
			block.setAttribute("outputs", outputs);
		} else {
			Attribute input = block.getAttribute("inputs");
			inputs = (List<Var>) input.getObjectValue();

			Attribute output = block.getAttribute("outputs");
			outputs = (List<Var>) output.getObjectValue();
		}

		return null;
	}

	@Override
	public Void caseExprBinary(ExprBinary expr) {
		Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();

		if (!definedOnlyInThisBlock(varE1)) {
			if (!inputs.contains(varE1)) {
				inputs.add(varE1);
			}
		}

		if (!definedOnlyInThisBlock(varE2)) {
			if (!inputs.contains(varE2)) {
				inputs.add(varE2);
			}
		}

		return null;
	}

	@Override
	public Void caseExprVar(ExprVar object) {
		// Take Var
		Var var = object.getUse().getVariable();
		if (!definedOnlyInThisBlock(var)) {
			if (!inputs.contains(var)) {
				inputs.add(var);
			}
		}
		return null;
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		// Get Target
		Var target = assign.getTarget().getVariable();
		if (!usedOnlyInThisBlock(target)) {
			if (!outputs.contains(target)) {
				outputs.add(target);
			}
		}
		// Visit Value
		doSwitch(assign.getValue());

		return null;
	}

	public Void caseInstCast(InstCast cast) {
		// Get target
		Var target = cast.getTarget().getVariable();
		if (!usedOnlyInThisBlock(target)) {
			if (!outputs.contains(target)) {
				outputs.add(target);
			}
		}

		// Get source
		Var source = cast.getSource().getVariable();
		if (!definedOnlyInThisBlock(source)) {
			if (!inputs.contains(source)) {
				inputs.add(source);
			}
		}

		return null;
	}

	@Override
	public Void caseInstLoad(InstLoad load) {
		// Get Target
		Var target = load.getTarget().getVariable();
		if (!usedOnlyInThisBlock(target)) {
			if (!outputs.contains(target)) {
				outputs.add(target);
			}
		}

		// Visit indexes
		Var loadIndexVar = null;
		List<Expression> indexes = load.getIndexes();
		for (Expression expr : new ArrayList<Expression>(indexes)) {
			loadIndexVar = ((ExprVar) expr).getUse().getVariable();
			if (!definedOnlyInThisBlock(loadIndexVar)) {
				if (!inputs.contains(loadIndexVar)) {
					inputs.add(loadIndexVar);
				}
			}
		}
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		// Do nothing
		return null;
	}

	public Void caseInstPortPeek(InstPortPeek portPeek) {
		Var target = portPeek.getTarget().getVariable();
		if (!usedOnlyInThisBlock(target)) {
			if (!outputs.contains(target)) {
				outputs.add(target);
			}
		}
		return null;
	}

	public Void caseInstPortRead(InstPortRead portRead) {
		Var target = portRead.getTarget().getVariable();
		if (!usedOnlyInThisBlock(target)) {
			if (!outputs.contains(target)) {
				outputs.add(target);
			}
		}
		return null;
	}

	public Void caseInstPortStatus(InstPortStatus portStatus) {
		Var target = portStatus.getTarget().getVariable();
		if (!usedOnlyInThisBlock(target)) {
			if (!outputs.contains(target)) {
				outputs.add(target);
			}
		}
		return null;
	}

	public Void caseInstPortWrite(InstPortWrite portWrite) {
		Var source = ((ExprVar) portWrite.getValue()).getUse().getVariable();
		if (!definedOnlyInThisBlock(source)) {
			if (!inputs.contains(source)) {
				inputs.add(source);
			}
		}
		return null;
	}

	@Override
	public Void caseInstStore(InstStore store) {
		// Visit source
		doSwitch(store.getValue());

		// Visit indexes
		Var loadIndexVar = null;
		List<Expression> indexes = store.getIndexes();
		for (Expression expr : new ArrayList<Expression>(indexes)) {
			loadIndexVar = ((ExprVar) expr).getUse().getVariable();
			if (!definedOnlyInThisBlock(loadIndexVar)) {
				if (!inputs.contains(loadIndexVar)) {
					inputs.add(loadIndexVar);
				}
			}
		}
		return null;
	}

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof InstPortStatus) {
			return caseInstPortStatus((InstPortStatus) object);
		} else if (object instanceof InstPortPeek) {
			return caseInstPortPeek((InstPortPeek) object);
		} else if (object instanceof InstPortRead) {
			return caseInstPortRead((InstPortRead) object);
		} else if (object instanceof InstPortWrite) {
			return caseInstPortWrite((InstPortWrite) object);
		} else if (object instanceof InstCast) {
			return caseInstCast((InstCast) object);
		}
		return null;
	}

	private boolean definedOnlyInThisBlock(Var var) {
		boolean defined = true;
		for (Def def : var.getDefs()) {
			BlockBasic blockBasic = EcoreHelper.getContainerOfType(def,
					BlockBasic.class);
			if (blockBasic != null) {
				if (blockBasic != block) {
					return false;
				} else {
					// It is defined in the same Block Basic but is defined in
					// Phi, this is an input for the loop decision
					InstPhi instPhi = EcoreHelper.getContainerOfType(def,
							InstPhi.class);
					if (instPhi != null) {
						if (instPhi.getTarget().getVariable() == var) {
							return false;
						}
					}
				}
			}
		}
		return defined;
	}

	public List<Var> getInputs() {
		doSwitch(block);
		return inputs;
	}

	public List<Var> getOutputs() {
		doSwitch(block);
		return outputs;
	}

	private boolean usedOnlyInThisBlock(Var var) {
		boolean used = true;
		for (Use use : var.getUses()) {
			BlockBasic blockBasic = EcoreHelper.getContainerOfType(use,
					BlockBasic.class);
			if (blockBasic != null) {
				if (blockBasic != block) {
					return false;
				}
			} else {
				BlockWhile blockWhile = EcoreHelper.getContainerOfType(use,
						BlockWhile.class);
				if (blockWhile != null) {
					// It is the blockWhile condition variable
					return false;
				} else {
					BlockIf blockIf = EcoreHelper.getContainerOfType(use,
							BlockIf.class);
					if (blockIf != null) {
						// It is the blockIf condition variable
						return false;
					} else {
						System.out.println("Var(" + getNameSSA(var)
								+ ") not contained in a BlockBasic");
					}
				}
			}
		}
		return used;
	}
}
