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

package net.sf.openforge.optimize.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Operation;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.BinaryOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ConditionalOrOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.UnaryOp;
import net.sf.openforge.lim.op.XorOp;
import net.sf.openforge.optimize.ComponentSwapVisitor;
import net.sf.openforge.optimize.Optimization;
import net.sf.openforge.optimize._optimize;

import org.eclipse.core.runtime.jobs.Job;

/**
 * FullConstantVisitor analyzes each Component in the LIM and replaces it with a
 * {@link Constant} node if all inputs are constant valued. This optimization
 * works on the Logical dependencies for each port as stored in the Entries of
 * the component. It is acceptible to have multiple entries and multiple
 * dependencies, however, in order to determine a constant value on the port all
 * dependencies for that port must have the <b>same</b> logical bus. The
 * constant value of the {@link Bus} is determined from it's {@link Value}. The
 * rule for each Component is contained within this {@link Visitor} because of
 * their simple nature; each rule is only 1 line of code.
 * 
 * Floating point support has been implemented by creating new Constants with
 * Float.floatToRawIntBits or Double.doubleToRawLongBits in floating point
 * capable operation rules.
 * 
 * Created: Wed Jul 10 09:58:27 2002
 * 
 * @author imiller
 * @version $Id: FullConstantVisitor.java 23 2005-09-09 18:45:32Z imiller $
 */
public class FullConstantVisitor extends ComponentSwapVisitor implements
		Optimization {

	@Override
	public void visit(Design design) {
		int i = 0;
		do {
			if (_optimize.db) {
				_optimize.ln("======================================");
			}
			if (_optimize.db) {
				_optimize.ln("# Starting Full Constant iteration " + (i++));
			}
			if (_optimize.db) {
				_optimize.ln("======================================");
			}
			reset();
			super.visit(design);
		} while (isModified());
	}

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

	/**
	 * Does nothing <strike> Propogates constants across method boundary. The
	 * Value connected to the Call port is transferred to the Bus of the InBuf
	 * in Procedure. Assumes that all {@link Procedure Procedures} are already
	 * unique.</strike>
	 * 
	 * @param call
	 *            a method call
	 */
	@Override
	public void preFilter(Call call) {
	}

	/**
	 * Does nothing <strike>Copies any constant value applied to the OutBuf back
	 * over to the output Buses of the Call.</strike>
	 * 
	 * @param call
	 *            a method call.
	 */
	@Override
	public void filter(Call call) {
	}

	/**
	 * All we do here is to replace any constant valued output bus from a call
	 * with a constant of equivalent value. This allows dead component removal
	 * to be responsible for removing the call if/when possible.
	 */
	@Override
	public void visit(Call call) {
		preFilter(call);
		traverse(call);
		filter(call);

		// Do not replace the Entry level method call
		// if (call.getOwner() == null)
		// IDM. Ugly hack, but there is no way to be sure we will have
		// traversed the top level design.
		if (call.getOwner() instanceof Design.DesignModule) {
			return;
		}

		// All we do here is to replace any constant valued output bus
		// from a call with a constant of equivalent value. This
		// allows dead component removal to be responsible for
		// removing the call if/when possible.
		for (Exit exit : call.getExits()) {
			for (Bus bus : exit.getDataBuses()) {
				if (bus.getValue().isConstant()) {
					Constant cValue;
					if (bus.isFloat()) {
						if (bus.getValue().getSize() == 64) {
							Number dfp = new Double(Double.longBitsToDouble(bus
									.getValue().getValueMask()));
							cValue = SimpleConstant.getDoubleConstant(dfp
									.doubleValue());
						} else {
							Number ffp = new Float(
									Float.intBitsToFloat((int) bus.getValue()
											.getValueMask()));
							cValue = SimpleConstant.getFloatConstant(ffp
									.floatValue());
						}
					} else {
						long con = bus.getValue().getValueMask();
						cValue = new SimpleConstant(con, bus.getSize(), bus
								.getValue().isSigned());
					}

					// Put a constant in the LIM at the same level as the call
					// and disconnect the bus.
					Map<Port, Port> portCorrelate = new HashMap<Port, Port>();
					portCorrelate.put(call.getClockPort(),
							cValue.getClockPort());
					portCorrelate.put(call.getResetPort(),
							cValue.getResetPort());
					portCorrelate.put(call.getGoPort(), cValue.getGoPort());

					replaceConnections(
							portCorrelate,
							Collections.singletonMap(bus, cValue.getValueBus()),
							Collections.singletonMap(exit,
									cValue.getExit(Exit.DONE)));
					Component owner = call.getOwner();
					if (owner instanceof Block) {
						Block block = (Block) owner;
						int index = block.getSequence().indexOf(call);
						block.insertComponent(cValue, index);
					} else {
						((Module) owner).addComponent(cValue);
					}
				}
			}
		}
	}

	@Override
	public void visit(InBuf inbuf) {
		inbuf.propagateValuesForward();
	}

	/**
	 * Calls a custom propagate method to move fully constant values across the
	 * module boundry.
	 */
	@Override
	public void visit(OutBuf outbuf) {
		if (outbuf.propagateValuesForward()) {
			setModified(true);
		}
	}

	@Override
	public void visit(AddOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == comp.getDataPorts().size() : ("AddOp data ports: "
					+ comp.getDataPorts().size() + "; constants: " + consts.length);

			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();

				// long newResult = consts[0].longValue() +
				// consts[1].longValue();
				long newResult = 0;
				for (int i = 0; i < consts.length; i++) {
					newResult += consts[i].longValue();
				}

				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(AndOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "AndOp must have only 2 ports";
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() & consts[1].longValue();
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(CastOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 1 : "NumericPromotionOp must have only 1 port";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				final int compCastSize = comp.getCastSize();
				long castValue;

				// mask to significant bits if size < 64
				if (compCastSize == 64) {
					castValue = consts[0].longValue();
				} else {
					final long constantValueMask = (1L << compCastSize) - 1L;
					castValue = consts[0].longValue() & constantValueMask;
				}
				replaceComponent(
						comp,
						new SimpleConstant(castValue, comp.getCastSize(), comp
								.isCastSigned()));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(ComplementOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 1 : "ComplementOp must have only 1 ports";
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = ~consts[0].longValue();
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(ConditionalAndOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "ConditionalAndOp must have only 2 ports";
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() & consts[1].longValue();
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(ConditionalOrOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "ConditionalOrOp must have only 2 ports";
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() | consts[1].longValue();
				boolean newSign = Component.getDataBus(comp).getValue()
						.isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(DivideOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "DivideOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				if (consts[1].longValue() == 0) {
					EngineThread.getGenericJob().warn(
							"    Found divide by 0 at line "
									+ comp.getIDSourceInfo().getSourceLine()
									+ ", replacing with 0.");
					replaceComponent(comp,
							new SimpleConstant(0, newSize, false));
				} else {
					long newResult = consts[0].longValue()
							/ consts[1].longValue();
					boolean newSign = comp.getResultBus().getValue().isSigned();
					replaceComponent(comp, new SimpleConstant(newResult,
							newSize, newSign));
				}
			}
		}
		filter(comp);
	}

	@Override
	public void visit(EqualsOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "EqualsOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = (consts[0].longValue() == consts[1]
						.longValue()) ? 1 : 0;
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(GreaterThanEqualToOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "GreaterThanEqualToOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = (consts[0].longValue() >= consts[1]
						.longValue()) ? 1 : 0;
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(GreaterThanOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "GreaterThanOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = (consts[0].longValue() > consts[1].longValue()) ? 1
						: 0;
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(LeftShiftOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "LeftShiftOp must have only 2 ports";
			int magnitude = comp.getShiftMagnitude();
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() << magnitude;
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(LessThanEqualToOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "LessThanEqualToOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = (consts[0].longValue() <= consts[1]
						.longValue()) ? 1 : 0;
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(LessThanOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "LessThanOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = (consts[0].longValue() < consts[1].longValue()) ? 1
						: 0;
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(MinusOp comp) {
		preFilter(comp);
		// for a constant, this value is computed by the partial constant
		// rule. Thus, it does not need to be recomputed, just replaced.
		Number[] consts = null;

		Value result = comp.getResultBus().getValue();

		if (result.isConstant()) {
			consts = new Number[1];
			consts[0] = new Long(result.getValueMask());
		}

		if (consts != null) {
			assert consts.length == 1 : "MinusOp must have only 1 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				// don't negate - thats already done by getting the value from
				// the result bus above.
				long newResult = consts[0].longValue();
				boolean newSign = comp.getDataPort().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(ModuloOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "ModuloOp must have only 2 ports";

			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				if (consts[1].longValue() == 0) {
					EngineThread.getGenericJob().warn(
							"    Found mod by 0 at line "
									+ comp.getIDSourceInfo().getSourceLine()
									+ ", replacing with 0.");
					replaceComponent(comp,
							new SimpleConstant(0, newSize, false));
				} else {
					long newResult = consts[0].longValue()
							% consts[1].longValue();
					boolean newSign = comp.getResultBus().getValue().isSigned();
					replaceComponent(comp, new SimpleConstant(newResult,
							newSize, newSign));
				}
			}
		}
		filter(comp);
	}

	@Override
	public void visit(MultiplyOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);

		if (consts != null) {
			assert consts.length == 2 : "MultiplyOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() * consts[1].longValue();
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(NoOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 1 : "NoOp must have only 1 port. "
					+ consts.length;
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue();
				boolean newSign = comp.getDataPorts()
						.get(comp.getDataPorts().size() - 1).getValue()
						.isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(NotEqualsOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "NotEqualsOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = (consts[0].longValue() != consts[1]
						.longValue()) ? 1 : 0;
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(NumericPromotionOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 1 : "NumericPromotionOp must have only 1 port";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				// FIXME
				// wait to be implemented
			}
		}
		filter(comp);
	}

	@Override
	public void visit(OrOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "OrOp must have only 2 ports";
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() | consts[1].longValue();
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(RightShiftOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "RightShiftOp must have only 2 ports";
			// int magnitude = comp.getShiftMagnitude();
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() >> consts[1].longValue();
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(RightShiftUnsignedOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "RightShiftUnsignedOp must have only 2 ports";
			long value = consts[0].longValue();
			int magnitude = comp.getShiftMagnitude();

			// Calculate a mask for the input value based on the
			// number of stages of the shift.
			long mask = 1L;
			for (int i = 0; i < comp.getMaxStages(); i++) {
				mask <<= (1 << i);
			}
			mask = mask | (mask - 1);

			value &= mask;
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = value >>> magnitude;
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(SubtractOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "SubtractOp must have only 2 ports";
			if (comp.isFloat()) {
				FullFloatingPointHelper.makeFPConstant(comp, consts, this);
			} else {
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() - consts[1].longValue();
				boolean newSign = comp.getLeftDataPort().getValue().isSigned()
						&& comp.getRightDataPort().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void visit(XorOp comp) {
		preFilter(comp);
		Number[] consts = getPortConstants(comp);
		if (consts != null) {
			assert consts.length == 2 : "XorOp must have only 2 ports";
			{
				int newSize = comp.getResultBus().getValue().getSize();
				long newResult = consts[0].longValue() ^ consts[1].longValue();
				boolean newSign = comp.getResultBus().getValue().isSigned();
				replaceComponent(comp, new SimpleConstant(newResult, newSize,
						newSign));
			}
		}
		filter(comp);
	}

	@Override
	public void preFilter(Operation op) {
		super.preFilter(op);
		for (Port port : op.getDataPorts()) {
			port.pushValueForward();
		}
	}

	/**
	 * Returns the cumulative count of how many nodes have been replaced by a
	 * {@link Constant}.
	 * 
	 * @return the number of nodes replaced by this visitor.
	 */
	@Override
	public int getReplacedNodeCount() {
		return replacedNodeCount;
	}

	/**
	 * Returns an array of Number objects, one corresponding to each port, of
	 * the constant value of each port, or <code>null</code> if any port is
	 * non-constant/non-analyzable.
	 * 
	 * @param c
	 *            the {@link Component} to be analyzed.
	 * @return an array of Number objects one for the constant value on each
	 *         port, or returns null if any ports are non constant.
	 */
	private Number[] getPortConstants(Component c) {
		if (c.getDataPorts().size() == 0) {
			if (_optimize.db) {
				_optimize.ln(_optimize.FULL_CONST, "\t" + c + " has no ports");
			}
			return null;
		}

		Number[] consts = new Number[c.getDataPorts().size()];
		int index = 0;
		for (Port port : c.getDataPorts()) {
			if (port.isUsed()) {
				Number constant = getPortConstantValue(port);
				if (constant == null) {
					return null;
				}
				consts[index++] = constant;
			} else {
				consts[index++] = new Long(0);
			}
		}
		return consts;
	}

	public void replaceComponent(BinaryOp comp, Constant constant) {
		super.replaceComponent(comp, constant);
	}

	public void replaceComponent(UnaryOp comp, Constant constant) {
		super.replaceComponent(comp, constant);
	}

	/**
	 * Reports, via {@link Job#info}, what optimization is being performed
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info(
				"replacing constant valued expressions...");
	}

	/**
	 * Reports, via {@link Job#verbose}, the results of <b>this</b> pass of the
	 * optimization.
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().verbose(
				"replaced " + getReplacedNodeCount() + " expressions");
	}

}// FullConstantVisitor
