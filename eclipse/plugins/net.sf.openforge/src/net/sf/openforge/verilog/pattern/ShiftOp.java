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
package net.sf.openforge.verilog.pattern;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.op.BinaryOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Concatenation;
import net.sf.openforge.verilog.model.Conditional;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Group;
import net.sf.openforge.verilog.model.HexConstant;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.Replication;
import net.sf.openforge.verilog.model.Wire;

/**
 * A ShiftOp is a shift operation, based on a {@link BinaryOp}, which assigns
 * the result to a wire.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: ShiftOp.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class ShiftOp extends StatementBlock implements ForgePattern {

	Set<Net> produced_nets = new HashSet<Net>();
	Set<Net> consumed_nets = new HashSet<Net>();

	Wire left_operand;
	Value l_value;
	Wire right_operand;
	Value r_value;
	Net result_wire;

	public ShiftOp(BinaryOp bo) {
		result_wire = NetFactory.makeNet(bo.getResultBus());
		produced_nets.add(result_wire);

		Iterator<Port> ports = bo.getDataPorts().iterator();
		Port l_port = ports.next();
		assert (l_port.isUsed()) : "Left operand port in math operation is set to unused.";
		// Bus l_bus = l_port.getBus();
		// assert (l_bus != null) :
		// "Left operand port in math operation not attached to a bus.";
		assert (l_port.getValue() != null) : "Left operand port in math operation does not have a value.";
		l_value = l_port.getValue();
		left_operand = new PortWire(l_port);

		Port r_port = ports.next();
		assert (r_port.isUsed()) : "Right operand port in math operation is set to unused.";
		// Bus r_bus = r_port.getBus();
		// assert (r_bus != null) :
		// "Right operand port in math operation not attached to a bus.";
		assert (r_port.getValue() != null) : "Right operand port in math operation does not have a value.";
		r_value = r_port.getValue();
		right_operand = new PortWire(r_port);
	}

	@Override
	public Collection<Net> getConsumedNets() {
		return consumed_nets;
	}

	/**
	 * Provides the collection of Nets which this statement of verilog produces
	 * as output signals.
	 */
	@Override
	public Collection<Net> getProducedNets() {
		return produced_nets;
	}

	// left shift
	public static final class Left extends ShiftOp {
		/**
		 * XXX FIXME. If any bit of the shift magnitude is a constant value then
		 * it needs to NOT create the Mux for that stage, instead, just wire
		 * through the previous wire or shifted wire based on the constant
		 * value.
		 */
		public Left(LeftShiftOp op) {
			super(op);

			// get the shift width.
			int maxShift = left_operand.getWidth() - 1;

			int totalShiftStage = right_operand.getWidth();

			// must have at least 1 shift stage
			assert (totalShiftStage > 0) : "attempt to shift by zero";

			assert (totalShiftStage <= op.getMaxStages()) : "too many shift stages: "
					+ totalShiftStage + ":" + op.getMaxStages();

			/* Get this operation's result bus and its width. */
			Wire prevStageWire = left_operand;
			Wire currentStageWire = left_operand;

			if (!r_value.isConstant()) {
				/* Compose the Verilog expression for each shifting stage */
				for (int stageCount = 1; stageCount <= totalShiftStage; stageCount++) {
					/* Compose the Verilog expression for each shift stage. */
					Expression conExp = right_operand
							.getBitSelect(totalShiftStage - stageCount);
					int shiftAmount = 1 << (totalShiftStage - stageCount);

					Expression leftExp = null;
					if (shiftAmount <= maxShift) {
						Concatenation concat = new Concatenation();
						concat.add(prevStageWire.getRange(maxShift
								- shiftAmount, 0));
						concat.add(new HexNumber(
								new HexConstant(0, shiftAmount)));
						leftExp = concat;
					} else {
						leftExp = new HexNumber(0, maxShift + 1);
					}
					Expression rightExp = prevStageWire;

					/*
					 * The boolean test expression is grouped in parens because
					 * ModelTech can't parse it when it's a literal value (ie,
					 * "1 'h 1?").
					 */
					Expression stageExp = new Conditional(new Group(conExp),
							leftExp, rightExp);

					if (stageCount != totalShiftStage) {
						currentStageWire = new Wire(ID.toVerilogIdentifier(ID
								.showLogical(op.getResultBus())
								+ "_stage_"
								+ stageCount), maxShift + 1);

						produced_nets.add(currentStageWire);
						prevStageWire = currentStageWire;
						add(new Assign.Continuous(currentStageWire, stageExp));
					} else {
						add(new Assign.Continuous(result_wire, stageExp));
					}
				}
			} else {
				// right_operand is a constant, so just re-wire the left operand
				// by
				// the shift amount indicated by the Constant right_operand
				int shiftAmount = (int) r_value.getValueMask();
				if (maxShift > shiftAmount) {
					Concatenation rewire = new Concatenation();
					rewire.add(prevStageWire
							.getRange(maxShift - shiftAmount, 0));
					rewire.add(new HexNumber(new HexConstant(0, shiftAmount)));
					add(new Assign.Continuous(result_wire, rewire));
				} else {
					add(new Assign.Continuous(result_wire, new HexNumber(0,
							result_wire.getWidth())));
				}
			}

		}
	}

	// right shift
	public static final class Right extends ShiftOp {
		/**
		 * XXX FIXME. If any bit of the shift magnitude is a constant value then
		 * it needs to NOT create the Mux for that stage, instead, just wire
		 * through the previous wire or shifted wire based on the constant
		 * value.
		 */
		public Right(RightShiftOp op) {
			super(op);

			int maxShift = left_operand.getWidth() - 1;
			int totalShiftStage = right_operand.getWidth();

			// must have at least 1 shift stage
			assert (totalShiftStage > 0) : "attempt to shift by zero";

			assert (totalShiftStage <= op.getMaxStages()) : "too many shift stages: "
					+ totalShiftStage + ":" + op.getMaxStages();

			Wire prevStageWire = left_operand;
			Wire currentStageWire = left_operand;
			int leftOperandSize = left_operand.getWidth();
			int resultSize = op.getResultBus().getValue().getSize();

			// add the left and right operands so the declarations will get
			// written out
			produced_nets.add(left_operand);

			if (!r_value.isConstant()) {
				produced_nets.add(right_operand);

				/* Compose the Verilog expression for each shifting stage */
				for (int stageCount = 1; stageCount <= totalShiftStage; stageCount++) {
					/* Compose the Verilog expression for each shift stage. */
					Expression conExp = right_operand
							.getBitSelect(totalShiftStage - stageCount);

					int shiftAmount = 1 << (totalShiftStage - stageCount);

					Expression leftExp = null;
					if (shiftAmount <= maxShift) {
						Concatenation concat = new Concatenation();
						concat.add(new Replication(shiftAmount, prevStageWire
								.getBitSelect(maxShift)));
						concat.add(prevStageWire
								.getRange(maxShift, shiftAmount));
						leftExp = concat;
					} else {
						leftExp = new Replication(maxShift + 1,
								prevStageWire.getBitSelect(maxShift));
					}
					Expression rightExp = prevStageWire;

					/*
					 * The boolean test expression is grouped in parens because
					 * ModelTech can't parse it when it's a literal value (ie,
					 * "1 'h 1?").
					 */
					Expression stageExp = new Conditional(new Group(conExp),
							leftExp, rightExp);

					// is this a simple one or the last ?
					if (stageCount != totalShiftStage) {
						// need to size the intermediate stages larger than the
						// result bus to handle high bits
						// int size=resultSize+(totalShiftStage-stageCount)+1;
						int size = stageExp.getWidth(); // ABK -- the result
														// size of each stage
														// should
														// simply match the size
														// of the stage
														// expression
						if (size > leftOperandSize) {
							size = leftOperandSize;
						}

						currentStageWire = new Wire(ID.toVerilogIdentifier(ID
								.showLogical(op.getResultBus())
								+ "_stage_"
								+ stageCount), size);
						produced_nets.add(currentStageWire);
						prevStageWire = currentStageWire;
						add(new Assign.Continuous(currentStageWire, stageExp));
						// set maxShift to work with current size of
						// prevStageWire
						maxShift = prevStageWire.getWidth() - 1;
					} else {
						if (result_wire.getWidth() != stageExp.getWidth()) {
							currentStageWire = new Wire(
									ID.toVerilogIdentifier(ID.showLogical(op
											.getResultBus())
											+ "_stage_"
											+ stageCount), stageExp.getWidth());
							produced_nets.add(currentStageWire);
							add(new Assign.Continuous(currentStageWire,
									stageExp));
							add(new Assign.Continuous(result_wire,
									currentStageWire.getRange(
											result_wire.getWidth() - 1, 0)));
						} else {
							add(new Assign.Continuous(result_wire, stageExp));
						}
					}
				}
			} else {
				// right_operand is a constant, so just re-wire the left operand
				// by
				// the shift amount indicated by the Constant right_operand
				int shiftAmount = (int) r_value.getValueMask();
				if (maxShift > shiftAmount) {
					int top_bit = maxShift;
					if ((shiftAmount + resultSize) < maxShift)
						top_bit = shiftAmount + resultSize;
					int extra_bits = resultSize - (top_bit - shiftAmount + 1);

					Concatenation rewire = new Concatenation();
					if ((maxShift - shiftAmount) < resultSize)
						if (extra_bits > 0) {
							rewire.add(new Replication(extra_bits,
									prevStageWire.getBitSelect(maxShift)));
						}
					rewire.add(prevStageWire.getRange(top_bit, shiftAmount));
					add(new Assign.Continuous(result_wire, rewire));
				} else {
					assert result_wire.getWidth() == 1;
					add(new Assign.Continuous(result_wire,
							prevStageWire.getBitSelect(maxShift)));
				}
			}

		}
	}

	// right shift unsigned
	public static final class RightUnsigned extends ShiftOp {
		/**
		 * XXX FIXME. If any bit of the shift magnitude is a constant value then
		 * it needs to NOT create the Mux for that stage, instead, just wire
		 * through the previous wire or shifted wire based on the constant
		 * value.
		 */
		public RightUnsigned(RightShiftUnsignedOp op) {
			super(op);

			int maxShift = left_operand.getWidth() - 1;
			int totalShiftStage = right_operand.getWidth();

			// must have at least 1 shift stage
			assert (totalShiftStage > 0) : "attempt to shift by zero";

			assert (totalShiftStage <= op.getMaxStages()) : "too many shift stages: "
					+ totalShiftStage + ":" + op.getMaxStages();

			Wire prevStageWire = left_operand;
			Wire currentStageWire = left_operand;
			int leftOperandSize = left_operand.getWidth();
			int resultSize = op.getResultBus().getValue().getSize();

			// add the left and right operands so the declarations will get
			// written out
			produced_nets.add(left_operand);

			if (!r_value.isConstant()) {
				produced_nets.add(right_operand);

				/* Compose the Verilog expression for each shifting stage */
				for (int stageCount = 1; stageCount <= totalShiftStage; stageCount++) {
					/* Compose the Verilog expression for each shift stage. */
					Expression conExp = right_operand
							.getBitSelect(totalShiftStage - stageCount);

					int shiftAmount = 1 << (totalShiftStage - stageCount);

					Expression leftExp = null;
					if (shiftAmount <= maxShift) {
						Concatenation concat = new Concatenation();
						concat.add(new HexNumber(
								new HexConstant(0, shiftAmount)));
						concat.add(prevStageWire
								.getRange(maxShift, shiftAmount));
						leftExp = concat;
					} else {
						leftExp = new HexNumber(0, maxShift + 1);
					}

					Expression rightExp = prevStageWire;

					/*
					 * The boolean test expression is grouped in parens because
					 * ModelTech can't parse it when it's a literal value (ie,
					 * "1 'h 1?").
					 */
					Expression stageExp = new Conditional(new Group(conExp),
							leftExp, rightExp);

					// is this a simple one or the last ?
					if (stageCount != totalShiftStage) {
						int size = stageExp.getWidth(); // ABK -- the result
														// size of each stage
														// should
														// simply match the size
														// of the stage
														// expression
						if (size > leftOperandSize) {
							size = leftOperandSize;
						}

						currentStageWire = new Wire(ID.toVerilogIdentifier(ID
								.showLogical(op.getResultBus())
								+ "_stage_"
								+ stageCount), size);
						produced_nets.add(currentStageWire);
						prevStageWire = currentStageWire;
						add(new Assign.Continuous(currentStageWire, stageExp));
						// set maxShift to work with current size of
						// prevStageWire
						maxShift = currentStageWire.getWidth() - 1;
					} else {
						if (result_wire.getWidth() != stageExp.getWidth()) {
							currentStageWire = new Wire(
									ID.toVerilogIdentifier(ID.showLogical(op
											.getResultBus())
											+ "_stage_"
											+ stageCount), stageExp.getWidth());
							produced_nets.add(currentStageWire);
							add(new Assign.Continuous(currentStageWire,
									stageExp));
							add(new Assign.Continuous(result_wire,
									currentStageWire.getRange(
											result_wire.getWidth() - 1, 0)));
						} else {
							add(new Assign.Continuous(result_wire, stageExp));
						}
					}
				}
			} else {
				// right_operand is a constant, so just re-wire the left operand
				// by
				// the shift amount indicated by the Constant right_operand
				int shiftAmount = (int) r_value.getValueMask();
				if (maxShift > shiftAmount) {
					int top_bit = maxShift;
					if ((shiftAmount + resultSize) < maxShift)
						top_bit = shiftAmount + resultSize;
					int extra_bits = resultSize - (top_bit - shiftAmount + 1);

					Concatenation rewire = new Concatenation();
					if ((maxShift - shiftAmount) < resultSize)
						if (extra_bits > 0) {
							rewire.add(new HexNumber(new HexConstant(0,
									extra_bits)));
						}
					rewire.add(prevStageWire.getRange(top_bit, shiftAmount));
					add(new Assign.Continuous(result_wire, rewire));
				} else {
					add(new Assign.Continuous(result_wire, new HexNumber(0,
							result_wire.getWidth())));
				}
			}
		}
	}

} // class ShiftOp
