/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
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

package org.xronos.orcc.design.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.ir.Block;
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
import net.sf.orcc.ir.InstSpecific;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.InstPortStatus;

/**
 * 
 * @author Endri Bezati
 * 
 */
public class BlockVars extends AbstractIrVisitor<Set<Var>> {

	private Block blockPhi;

	private List<Block> blocksContainer;

	private Set<Var> blockVars;

	private Block currentBlock;

	private Boolean decisionInputs;

	private Boolean deepSearch;

	private Boolean getDefinedVar;

	private Boolean inputVars;

	private Map<Block, Map<Var, List<Var>>> phi;

	private Boolean phiVisit;

	private Block stmBlock;

	/** Map of a Loop Module Output Variables **/
	private Map<Block, List<Var>> stmOutputs;

	public BlockVars(Block stmBlock, Map<Block, Map<Var, List<Var>>> phi) {
		super(true);
		this.inputVars = true;
		this.deepSearch = false;
		this.phiVisit = true;
		this.stmBlock = stmBlock;
		this.phi = phi;
		this.blocksContainer = new ArrayList<Block>();
		this.blocksContainer.add(stmBlock);
		this.getDefinedVar = false;
		this.decisionInputs = true;
	}

	public BlockVars(Boolean inputVars, Boolean deepSearch,
			List<Block> blocksContainer, Block blockPhi) {
		super(true);
		this.inputVars = inputVars;
		this.deepSearch = deepSearch;
		this.phiVisit = false;
		this.blocksContainer = blocksContainer;
		this.getDefinedVar = false;
		this.blockPhi = blockPhi;
		this.decisionInputs = false;
	}

	public BlockVars(Boolean getDefinedVar, Map<Block, List<Var>> stmOutputs) {
		super(true);
		this.inputVars = false;
		this.deepSearch = false;
		this.phiVisit = false;
		this.getDefinedVar = getDefinedVar;
		this.stmOutputs = stmOutputs;
		blockVars = new HashSet<Var>();
		this.decisionInputs = false;
	}

	@Override
	public Set<Var> caseBlockBasic(BlockBasic block) {
		if (!getDefinedVar) {
			blockVars = new HashSet<Var>();
		}
		currentBlock = block;
		super.caseBlockBasic(block);
		return blockVars;
	}

	@Override
	public Set<Var> caseBlockIf(BlockIf nodeIf) {
		if (getDefinedVar) {
			if (stmOutputs != null) {
				if (stmOutputs.containsKey(nodeIf)) {
					if (blockVars != null) {
						blockVars.addAll(stmOutputs.get(nodeIf));
					}
				}
			}
		} else {
			if (deepSearch) {

				doSwitch(nodeIf.getThenBlocks());
				doSwitch(nodeIf.getElseBlocks());
				return blockVars;
			}
		}
		return blockVars;
	}

	@Override
	public Set<Var> caseBlockWhile(BlockWhile nodeWhile) {
		if (getDefinedVar) {
			if (stmOutputs != null) {
				if (stmOutputs.containsKey(nodeWhile)) {
					if (blockVars != null) {
						blockVars.addAll(stmOutputs.get(nodeWhile));
					}
				}
			}
		} else {
			if (deepSearch) {
				doSwitch(nodeWhile.getBlocks());
				return blockVars;
			}
		}
		return blockVars;
	}

	@Override
	public Set<Var> caseExprBinary(ExprBinary expr) {
		Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();
		if (inputVars) {
			if (definedInOtherBlock(varE1)) {
				blockVars.add(varE1);
			}
			if (definedInOtherBlock(varE2)) {
				blockVars.add(varE2);
			}
		}

		if (phiVisit) {
			if (definedInOtherBlock(varE1)
					|| phi.get(stmBlock).containsKey(varE1)) {
				blockVars.add(varE1);
			}
			if (definedInOtherBlock(varE2)
					|| phi.get(stmBlock).containsKey(varE2)) {
				blockVars.add(varE2);
			}
		}
		return null;
	}

	@Override
	public Set<Var> caseExprVar(ExprVar expr) {
		Var var = expr.getUse().getVariable();
		if (inputVars) {
			if (definedInOtherBlock(var)) {
				blockVars.add(var);
			}
		}

		if (phiVisit) {
			if (definedInOtherBlock(var) || phi.get(stmBlock).containsKey(var)) {
				blockVars.add(var);
			}
		}

		return null;
	}

	@Override
	public Set<Var> caseInstAssign(InstAssign assign) {
		Var target = assign.getTarget().getVariable();
		if (!getDefinedVar) {
			if (!inputVars) {
				if (usedInOtherBlock(target)) {
					blockVars.add(target);
				}
			}
		} else {
			blockVars.add(target);
		}
		super.caseInstAssign(assign);
		return null;
	}

	public Set<Var> caseInstCast(InstCast cast) {
		Var target = cast.getTarget().getVariable();
		Var source = cast.getSource().getVariable();
		if (!getDefinedVar) {
			if (inputVars) {
				if (definedInOtherBlock(source)) {
					blockVars.add(source);
				}
			} else {
				if (usedInOtherBlock(target)) {
					blockVars.add(target);
				}
			}
		} else {
			blockVars.add(target);
		}
		return null;
	}

	@Override
	public Set<Var> caseInstLoad(InstLoad load) {
		Var target = load.getTarget().getVariable();
		if (!getDefinedVar) {
			if (!inputVars) {
				if (usedInOtherBlock(target)) {
					blockVars.add(target);
				}
			} else {
				Var loadIndexVar = null;
				List<Expression> indexes = load.getIndexes();
				for (Expression expr : new ArrayList<Expression>(indexes)) {

					loadIndexVar = ((ExprVar) expr).getUse().getVariable();
					if (definedInOtherBlock(loadIndexVar)) {
						blockVars.add(loadIndexVar);
					}
				}
			}
		} else {
			blockVars.add(target);
		}
		return null;
	}

	@Override
	public Set<Var> caseInstPhi(InstPhi phi) {
		// Do not visit PHI
		return null;
	}

	public Set<Var> caseInstPortStatus(InstPortStatus portStatus) {
		Var target = portStatus.getTarget().getVariable();
		if (!getDefinedVar) {
			if (!inputVars) {
				if (usedInOtherBlock(target)) {
					blockVars.add(target);
				}
			}
		} else {
			blockVars.add(target);
		}

		return null;
	}

	@Override
	public Set<Var> caseInstSpecific(InstSpecific object) {
		if (object instanceof InstCast) {
			return caseInstCast((InstCast) object);
		}
		return super.defaultCase(object);
	}

	@Override
	public Set<Var> caseInstStore(InstStore store) {
		Var target = store.getTarget().getVariable();
		if (!inputVars) {
			if (!target.getType().isList()) {
				if (usedInOtherBlock(target)) {
					blockVars.add(target);
				}
			}
		} else {
			Var value = ((ExprVar) store.getValue()).getUse().getVariable();
			if (definedInOtherBlock(value)) {
				blockVars.add(value);
			}

			Var storeIndexVar = null;
			List<Expression> indexes = store.getIndexes();
			for (Expression expr : new ArrayList<Expression>(indexes)) {
				storeIndexVar = ((ExprVar) expr).getUse().getVariable();
				if (definedInOtherBlock(storeIndexVar)) {
					blockVars.add(storeIndexVar);
				}
			}
		}
		return null;
	}

	@Override
	public Set<Var> defaultCase(EObject object) {
		if (object instanceof InstPortStatus) {
			return caseInstPortStatus((InstPortStatus) object);
		}
		return null;
	}

	private Boolean definedInOtherBlock(Var var) {
		Map<Def, Boolean> defMap = new HashMap<Def, Boolean>();
		for (Def def : var.getDefs()) {
			EObject container = def.eContainer();

			// Test if the container is from InstPhi
			InstPhi instPhi = null;
			if (container instanceof InstPhi) {
				instPhi = (InstPhi) container;
			}

			// Get the BlockBasic container
			while (!(container instanceof BlockBasic)) {
				container = container.eContainer();
				if (container == null) {
					return false;
				}
			}

			if (decisionInputs) {
				if (instPhi != null) {
					Var targetVar = instPhi.getTarget().getVariable();
					if (targetVar == var) {
						defMap.put(def, true);
					}
				}
			}
			if (blockPhi != null) {
				if (container == blockPhi) {
					defMap.put(def, false);
				} else if (!blocksContainer.contains(container)
						&& (container != currentBlock)) {
					defMap.put(def, true);
				}
			} else {
				if (!blocksContainer.contains(container)
						&& (container != currentBlock)) {
					defMap.put(def, true);
				}
			}
		}

		if (defMap.containsValue(true)) {
			return true;
		} else {
			return false;
		}
	}

	private Boolean usedInOtherBlock(Var var) {
		Map<Use, Boolean> useMap = new HashMap<Use, Boolean>();
		for (Use use : var.getUses()) {
			EObject container = use.eContainer();
			// Get the BlockBasic container
			while (!(container instanceof BlockBasic)) {
				container = container.eContainer();
				if (container == null) {
					return false;
				}
			}
			if (blockPhi != null) {
				if (container == blockPhi) {
					if (container.eContainer() instanceof BlockIf) {
						useMap.put(use, true);
					} else {
						useMap.put(use, false);
					}
				} else if (!blocksContainer.contains(container)
						&& (container != currentBlock)) {
					useMap.put(use, true);
				} else {
					useMap.put(use, false);
				}
			}
		}

		if (useMap.containsValue(true)) {
			return true;
		} else {
			return false;
		}
	}
}
