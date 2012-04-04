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

package net.sf.orc2hdl.design;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.op.XorOp;
import net.sf.orcc.df.Action;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.NodeIf;
import net.sf.orcc.ir.NodeWhile;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.util.AbstractActorVisitor;

public class DesignActorVisitor extends AbstractActorVisitor<Object> {

	/** List which associates each action with its components **/
	Map<Action, List<Component>> actionComponents = new HashMap<Action, List<Component>>();

	/** Current visited action **/
	Action currentAction = null;

	/** Current Component **/
	Component currentComponent = null;

	/** Current List Component **/
	List<Component> currentListComponent;

	public DesignActorVisitor() {
		super(true);
	}

	@Override
	public Object caseAction(Action action) {
		currentAction = action;
		currentListComponent = new ArrayList<Component>();
		super.doSwitch(action.getBody().getNodes());
		actionComponents.put(currentAction, currentListComponent);
		return null;
	}

	@Override
	public Object caseNodeIf(NodeIf nodeIf) {
		return null;
	}

	@Override
	public Object caseNodeWhile(NodeWhile nodeWhile) {
		return null;
	}

	@Override
	public Object caseInstAssign(InstAssign assign) {
		super.caseInstAssign(assign);
		if (currentComponent != null) {
			currentListComponent.add(currentComponent);
		}
		return null;
	}

	@Override
	public Object caseExprInt(ExprInt expr) {
		final long value = expr.getIntValue();
		currentComponent = new SimpleConstant(value, expr.getType()
				.getSizeInBits(), expr.getType().isInt());
		return null;
	}

	@Override
	public Object caseExprBool(ExprBool expr) {
		final long value = expr.isValue() ? 1 : 0;
		currentComponent = new SimpleConstant(value, 1, true);
		return null;
	}

	@Override
	public Object caseExprBinary(ExprBinary expr) {
		if (expr.getOp() == OpBinary.BITAND) {
			currentComponent = new AndOp();
		} else if (expr.getOp() == OpBinary.BITOR) {
			currentComponent = new OrOp();
		} else if (expr.getOp() == OpBinary.BITXOR) {
			currentComponent = new XorOp();
		} else if (expr.getOp() == OpBinary.DIV) {
			currentComponent = new DivideOp(expr.getType().getSizeInBits());
		} else if (expr.getOp() == OpBinary.GT) {
			currentComponent = new GreaterThanOp();
		}
		return null;
	}
}
