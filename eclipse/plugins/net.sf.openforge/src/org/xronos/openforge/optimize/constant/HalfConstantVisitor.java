/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package org.xronos.openforge.optimize.constant;


import org.eclipse.core.runtime.jobs.Job;
import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.ConditionalAndOp;
import org.xronos.openforge.lim.op.ConditionalOrOp;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.LeftShiftOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.RightShiftUnsignedOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.optimize.ComponentSwapVisitor;
import org.xronos.openforge.optimize.Optimization;
import org.xronos.openforge.optimize._optimize;
import org.xronos.openforge.optimize.constant.rule.AddOpRule;
import org.xronos.openforge.optimize.constant.rule.AndOpRule;
import org.xronos.openforge.optimize.constant.rule.ArrayReadRule;
import org.xronos.openforge.optimize.constant.rule.ArrayWriteRule;
import org.xronos.openforge.optimize.constant.rule.ConditionalAndOpRule;
import org.xronos.openforge.optimize.constant.rule.ConditionalOrOpRule;
import org.xronos.openforge.optimize.constant.rule.DivideOpRule;
import org.xronos.openforge.optimize.constant.rule.ModuloOpRule;
import org.xronos.openforge.optimize.constant.rule.MultiplyOpRule;
import org.xronos.openforge.optimize.constant.rule.OrOpRule;
import org.xronos.openforge.optimize.constant.rule.ShiftOpRule;
import org.xronos.openforge.optimize.constant.rule.SubtractOpRule;

/**
 * HalfConstantVisitor.java
 * 
 * 
 * <p>
 * Created: Thu Jul 18 09:04:14 2002
 * 
 * @author imiller
 * @version $Id: HalfConstantVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class HalfConstantVisitor extends ComponentSwapVisitor implements
		Optimization {

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		target.accept(this);
	}

	@Override
	public void visit(Design design) {
		int i = 0;
		do {
			if (_optimize.db) {
				_optimize.ln("======================================");
			}
			if (_optimize.db) {
				_optimize.ln("# Starting Half Constant iteration " + (i++));
			}
			if (_optimize.db) {
				_optimize.ln("======================================");
			}
			reset();
			super.visit(design);
		} while (isModified());
	}

	@Override
	public void visit(AddOp op) {
		super.visit(op);
		if (op.isFloat()) // Skip float for now
		{
			return;
		}

		Number[] consts = getPortConstants(op);

		if (AddOpRule.halfConstant(op, consts)) {
			setModified(true);
		}
	}

	@Override
	public void visit(AndOp op) {
		super.visit(op);
		Number[] consts = getPortConstants(op);
		if (AndOpRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(ArrayRead op) {
		Number[] consts = getPortConstants(op);
		if (ArrayReadRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(ArrayWrite op) {
		Number[] consts = getPortConstants(op);
		if (ArrayWriteRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	/**
	 * Push any constants across the module boundries. Full constant prop would
	 * do this for us too, but why wait and potentially force a re-run of all
	 * the optimizations as a result.
	 */
	@Override
	public void visit(InBuf ib) {
		{
			if (ib.propagateValuesForward()) {
				setModified(true);
			}
		}
	}

	@Override
	public void visit(ConditionalAndOp op) {
		super.visit(op);
		Number[] consts = getPortConstants(op);
		if (ConditionalAndOpRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(ConditionalOrOp op) {
		super.visit(op);
		Number[] consts = getPortConstants(op);
		if (ConditionalOrOpRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(DivideOp op) {
		super.visit(op);
		if (op.isFloat()) // Skip float for now
		{
			return;
		}
		Number[] consts = getPortConstants(op);
		if (DivideOpRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(LeftShiftOp op) {
		super.visit(op);
		Number[] consts = getPortConstants(op);
		if (ShiftOpRule.halfConstant(op, consts, this)
				|| ShiftOpRule.leftHalfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(ModuloOp op) {
		super.visit(op);
		if (op.isFloat()) // Skip float for now
		{
			return;
		}
		Number[] consts = getPortConstants(op);
		if (ModuloOpRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(MultiplyOp op) {
		super.visit(op);
		if (op.isFloat()) // Skip float for now
		{
			return;
		}
		Number[] consts = getPortConstants(op);
		if (MultiplyOpRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(OrOp op) {
		super.visit(op);
		Number[] consts = getPortConstants(op);
		if (OrOpRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	/**
	 * Push any constants across the module boundries. Full constant prop would
	 * do this for us too, but why wait and potentially force a re-run of all
	 * the optimizations as a result.
	 */
	@Override
	public void visit(OutBuf outbuf) {
		if (outbuf.propagateValuesForward()) {
			setModified(true);
		}
	}

	@Override
	public void visit(RightShiftOp op) {
		super.visit(op);
		Number[] consts = getPortConstants(op);
		if (ShiftOpRule.halfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(RightShiftUnsignedOp op) {
		super.visit(op);
		Number[] consts = getPortConstants(op);
		if (ShiftOpRule.halfConstant(op, consts, this)
				|| ShiftOpRule.unsignedRightHalfConstant(op, consts, this)) {
			setModified(true);
		}
	}

	@Override
	public void visit(SubtractOp op) {
		super.visit(op);
		if (op.isFloat()) // Skip float for now
		{
			return;
		}
		Number[] consts = getPortConstants(op);
		if (SubtractOpRule.halfConstant(op, consts)) {
			setModified(true);
		}
	}

	private static Number[] getPortConstants(Component c) {
		Number[] consts = new Number[c.getDataPorts().size()];
		if (c.getDataPorts().size() == 0) {
			if (_optimize.db) {
				_optimize.ln(_optimize.HALF_CONST, "\t" + c + " has no ports");
			}
			return consts;
		}

		int index = 0;
		for (Port port : c.getDataPorts()) {
			if (port.isUsed()) {
				Number constant = getPortConstantValue(port);
				consts[index++] = constant;
			} else {
				consts[index++] = new Long(0);
			}
		}
		return consts;
	}

	/**
	 * Reports, via {@link Job#info}, what optimization is being performed
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info(
				"reducing expressions with constants...");
	}

	/**
	 * Reports, via {@link Job#verbose}, the results of <b>this</b> pass of the
	 * optimization.
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().verbose(
				"reduced " + getReplacedNodeCount() + " expressions");
	}

}// HalfConstantVisitor
