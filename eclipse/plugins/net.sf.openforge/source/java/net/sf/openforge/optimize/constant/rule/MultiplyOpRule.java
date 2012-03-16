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

package net.sf.openforge.optimize.constant.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.OptionInt;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Operation;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.BinaryOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.UnaryOp;
import net.sf.openforge.optimize.ComponentSwapVisitor;
import net.sf.openforge.optimize._optimize;
import net.sf.openforge.optimize.constant.TwoPassPartialConstant;

/**
 * <pre>
 * MultiplyOpRule.java
 * 
 * a * 0 = 0
 * a * 1 = a
 * a * -1 = -a
 * a * 2^n = a << n
 * a * constant = (a << n) + (a << m) + ... + a
 * </pre>
 * <p>
 * Created: Thu Jul 18 09:24:39 2002
 * 
 * @author imiller
 * @version $Id: MultiplyOpRule.java 105 2006-02-15 21:38:08Z imiller $
 */
public class MultiplyOpRule {

	/** the limit for decomposing constant into shifts **/
	// private static final int decompose_limit = 5;

	private static class Container {
		/** the block that will replace this multiplication **/
		private Block container = null;

		public void setContainer(Block b) {
			this.container = b;
		}

		public Block getContainer() {
			return this.container;
		}
	}

	public static boolean halfConstant(MultiplyOp op, Number[] consts,
			ComponentSwapVisitor visit) {
		assert consts.length == 2 : "Expecting exactly 2 port constants for Multiply Op";
		Number p1 = consts[0];
		Number p2 = consts[1];

		if ((p1 == null && p2 == null) || (p1 != null && p2 != null)) {
			return false;
		}

		Number constant = p1 == null ? p2 : p1;
		final int constantSize;
		if (p1 == null)
			constantSize = op.getRightDataPort().getValue().getSize();
		else
			constantSize = op.getLeftDataPort().getValue().getSize();

		Port nonConstantPort = p1 == null ? (Port) op.getDataPorts().get(0)
				: (Port) op.getDataPorts().get(1);
		Port constantPort = p1 != null ? (Port) op.getDataPorts().get(0)
				: (Port) op.getDataPorts().get(1);

		if (constant.longValue() == 0) {
			if (_optimize.db)
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (a * 0)");
			// a * 0 = 0. Simply delete the component and replace with constant
			// 0
			visit.replaceComponent(op, new SimpleConstant(0, op.getResultBus()
					.getValue().getSize(), op.getResultBus().getValue()
					.isSigned()));

			return true;
		} else if (constant.longValue() == 1) {
			if (_optimize.db)
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (a * 1)");
			// a * 1 = a. Simply delete the component and wire through the
			// non-constant port

			// wire through the control.
			ComponentSwapVisitor.wireControlThrough(op);

			// Wire the non constant port through.
			ComponentSwapVisitor.shortCircuit(nonConstantPort,
					op.getResultBus());

			// Delete the op.
			// op.getOwner().removeComponent(op);
			boolean removed = ComponentSwapVisitor.removeComp(op);

			assert removed : "MultiplyOp was not able to be removed!";

			return true;
		} else if (DivideOpRule.isAllOnes(constant, constantSize)) // constant
																	// is -1
		{
			if (_optimize.db)
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (a * -1)");

			// a * -1 = -a. Simply replace the component with a MinusOp on the
			// non-constant port
			MinusOp mop = new MinusOp();
			Module owner = op.getOwner();
			assert owner != null : "Cannot replace a component which is not contained in a module";

			// map the ports/buses and connects dependencies and push value to
			// result bus
			replaceConnections(op, mop, nonConstantPort);
			mop.propagateValuesForward();

			if (owner instanceof Block) {
				((Block) owner).replaceComponent(op, mop);
			} else {
				owner.removeComponent(op);
				owner.addComponent(mop);
			}
			op.disconnect();

			return true;
		} else if (constant.longValue() != 1 && constant.longValue() > 0) {
			int power = getPowerOfTwo(constant.longValue());
			if (power != -1) {
				Constant newConst = new SimpleConstant(power, nonConstantPort
						.getValue().getSize(), nonConstantPort.getValue()
						.isSigned());

				if (_optimize.db)
					_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
							+ " due to (a * 2^n)");
				LeftShiftOp shift = new LeftShiftOp(
						getLog2(op.getNaturalSize()));
				Module owner = op.getOwner();
				assert owner != null : "Cannot replace a component which is not contained in a module";

				/** map the ports/buses and connects dependencies **/
				replaceConnections(op, shift, nonConstantPort);

				/**
				 * setup the dependency between the shift and the new power of
				 * two constant
				 */
				Entry shift_entry = (Entry) shift.getEntries().get(0);
				Dependency dep = new DataDependency(newConst.getValueBus());
				shift_entry.addDependency((Port) shift.getDataPorts().get(1),
						dep);

				/** set the left shift bus size with this MultiplyOp sizing info **/
				// assert false : "New constant prop: fix these lines. --SGE";
				// ((Port)shift.getDataPorts().get(0)).setValue(nonConstantPort.getValue());
				// Value op_value = op.getResultBus().getValue();
				// shift.getResultBus().setBits(op_value.size());

				/** replace the constant with its power **/
				Entry mult_entry = (Entry) constantPort.getOwner().getEntries()
						.get(0);
				Collection<Dependency> list = mult_entry
						.getDependencies(constantPort);
				Dependency depend = (Dependency) list.iterator().next();
				Component old = depend.getLogicalBus().getOwner().getOwner();
				visit.replaceComponent(old, newConst);

				if (owner instanceof Block) {
					((Block) owner).replaceComponent(op, shift);
				} else {
					owner.removeComponent(op);
					owner.addComponent(shift);
				}
				op.disconnect();

				// Run the partial constant visitor to propagate values info
				// to the newly created shift from the owner.
				TwoPassPartialConstant.forward(owner);

				return true;
			} else {
				if (_optimize.db)
					_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
							+ " due to (a * constant)");

				Value argValue = (nonConstantPort.getValue());

				/**
				 * Replace multiplication with additions or subtractions of left
				 * shifts. Don't replace if requires more than "decompose_limit"
				 * (default == 5) terms to replace the multiplication.
				 */
				Container container = new Container();

				if (!decompose(constant.longValue(), argValue, op, container))
					return false;

				Module owner = op.getOwner();
				assert owner != null : "Cannot replace a component which is not contained in a module";

				// map the dependencies/connections
				Map<Port, Port> portCorrelation = new HashMap<Port, Port>();
				portCorrelation.put(op.getClockPort(), container.getContainer()
						.getClockPort());
				portCorrelation.put(op.getResetPort(), container.getContainer()
						.getResetPort());
				portCorrelation.put(op.getGoPort(), container.getContainer()
						.getGoPort());
				// portCorrelation.put(op.getDataPorts().get(0),
				// container.getContainer().getDataPorts().get(0));
				portCorrelation.put(nonConstantPort, container.getContainer()
						.getDataPorts().get(0));

				assert op.getExits().size() == 1 : "Only expecting one exit on node to be replaced";
				Exit exit = (Exit) op.getExits().iterator().next();
				assert exit.getDataBuses().size() == 1 : "Only expecting one data bus on component to be replaced";

				Exit block_exit = (Exit) container.getContainer().getExits()
						.iterator().next();

				Map<Bus, Bus> busCorrelation = new HashMap<Bus, Bus>();
				busCorrelation.put(exit.getDataBuses().get(0), block_exit
						.getDataBuses().get(0));
				assert container.getContainer().getExits().size() == 1 : "Only expecting one exit on MinusOp";
				busCorrelation.put(exit.getDoneBus(), container.getContainer()
						.getOnlyExit().getDoneBus());
				assert container.getContainer().getExits().size() == 1 : "Only expecting one exit on node to be replaced";
				Map<Exit, Exit> exitCorrelation = new HashMap<Exit, Exit>();
				exitCorrelation.put(exit, container.getContainer()
						.getOnlyExit());

				ComponentSwapVisitor.replaceConnections(portCorrelation,
						busCorrelation, exitCorrelation);

				if (owner instanceof Block) {
					((Block) owner).replaceComponent(op,
							container.getContainer());
				} else {
					owner.removeComponent(op);
					owner.addComponent(container.getContainer());
				}
				op.disconnect();

				// Run the partial constant visitor to propagate values info
				// to the newly created shifts from the owner.
				TwoPassPartialConstant.forward(owner);

				return true;
			}
		} else {
			return false;
		}
	}

	/**
	 * Maps the multiply op ports and buse to a op ports and buses which will
	 * replace the multiply op. Makes a static call to
	 * ComponentSwapVisitor.replaceConnections() to replace the mapped ports and
	 * buses.
	 * 
	 * @param MultiplyOp
	 *            multiplication to be replaced
	 * @param Operation
	 *            new operation to replace multiplication
	 * @param nonConstantPort
	 *            a port connected to a variable
	 */
	private static void replaceConnections(MultiplyOp multi, Operation newOp,
			Port nonConstantPort) {
		// map the dependencies/connections
		Map<Port, Port> portCorrelation = new HashMap<Port, Port>();
		portCorrelation.put(multi.getClockPort(), newOp.getClockPort());
		portCorrelation.put(multi.getResetPort(), newOp.getResetPort());
		portCorrelation.put(multi.getGoPort(), newOp.getGoPort());
		// portCorrelation.put(multi.getDataPorts().get(0),
		// newOp.getDataPorts().get(0));
		portCorrelation.put(nonConstantPort, newOp.getDataPorts().get(0));

		assert multi.getExits().size() == 1 : "Only expecting one exit on node to be replaced";
		Exit exit = (Exit) multi.getExits().iterator().next();
		assert exit.getDataBuses().size() == 1 : "Only expecting one data bus on component to be replaced";

		Map<Bus, Bus> busCorrelation = new HashMap<Bus, Bus>();
		if (newOp instanceof BinaryOp) {
			busCorrelation.put(exit.getDataBuses().get(0),
					((BinaryOp) newOp).getResultBus());
		} else {
			busCorrelation.put(exit.getDataBuses().get(0),
					((UnaryOp) newOp).getResultBus());
		}
		assert newOp.getExits().size() == 1 : "Only expecting one exit on MinusOp";
		busCorrelation.put(exit.getDoneBus(), ((Exit) newOp.getExits()
				.iterator().next()).getDoneBus());

		assert newOp.getExits().size() == 1 : "Only expecting one exit on node to be replaced";
		Map<Exit, Exit> exitCorrelation = new HashMap<Exit, Exit>();
		exitCorrelation.put(exit, newOp.getOnlyExit());

		ComponentSwapVisitor.replaceConnections(portCorrelation,
				busCorrelation, exitCorrelation);
	}

	/**
	 * 
	 * Creates left shift ops and addition/subtraction which combines the left
	 * shifts and copies the bus attributes from the multiply op to the buses of
	 * created shifts and adds/subtracts.
	 * <p>
	 * XXX This needs to be re-implemented as a balanced tree of operations. For
	 * adders this is quite simple, but for a tree of subtracts, anything that
	 * feeds the right hand port needs to be converted to an adder.
	 * 
	 * @param long the constant that needs to be decomposed into left shifts
	 * @param Value
	 *            the Value of the MultiplyOp's non-constant port
	 * @param MultiplyOp
	 *            _the_ multiply op. Will be used to extract sizing info.
	 * @param container
	 *            , the {@link Container} which will hold the resulting block.
	 * 
	 * @return false if additions/subtractions is greater than the
	 *         "decompose_limit"
	 */
	private static boolean decompose(long number, Value argValue,
			MultiplyOp op, Container container) {
		List<Integer> addition = calculateAdditionTerms(number);
		List<Integer> subtraction = calculateSubtractionTerms(number);
		List<Integer> terms;
		boolean isAdd = true;
		if (addition.size() <= subtraction.size()) {
			terms = addition;
		} else {
			terms = subtraction;
			isAdd = false;
		}

		// int decompLimit = decompose_limit;
		final int decompLimit = ((OptionInt) EngineThread.getEngine()
				.getGenericJob()
				.getOption(OptionRegistry.MULTIPLY_DECOMP_LIMIT))
				.getValueAsInt(op.getSearchLabel());

		if (terms.size() > decompLimit) {
			return false;
		}

		final List<Component> newSequence = new ArrayList<Component>();

		// A map of port->dependency for that port which will be set
		// after the Block containing the ports owner has been created
		// to avoid 're-entrant' assertion failure by
		// Block.setControlDependencies()
		final Map<Port, DataDependency> deps = new HashMap<Port, DataDependency>();

		final List<Bus> termBuses = new LinkedList<Bus>();
		final Set<LeftShiftOp> shifts = new HashSet<LeftShiftOp>();
		final int shiftTerms = getLog2(op.getNaturalSize());
		for (Integer term : terms) {
			// Create 1 shift & constant for each term
			Constant constant = new SimpleConstant(term.intValue(),
					argValue.getSize(), argValue.isSigned());
			LeftShiftOp shl = new LeftShiftOp(shiftTerms);
			shl.getResultBus().copyAttributes(op.getResultBus());
			deps.put(shl.getRightDataPort(),
					new DataDependency(constant.getValueBus()));
			newSequence.add(constant);
			newSequence.add(shl);
			shifts.add(shl);
			termBuses.add(shl.getResultBus());
		}

		// Create the tree of adds/subtracts.
		while (termBuses.size() > 1) {
			// This is a daisy chain of operations. We'd prefer a
			// true tree, but all the right handed terms of the
			// subtract must be adders. i.e 8-7-6-5-4-3-2 ==
			// ((8-7) - (6+5)) - ((4+3) + 2)
			Bus b1 = (Bus) termBuses.remove(termBuses.size() - 1);
			Bus b2 = (Bus) termBuses.remove(termBuses.size() - 1);
			BinaryOp combineOp = (isAdd) ? ((BinaryOp) (new AddOp()))
					: ((BinaryOp) (new SubtractOp()));
			combineOp.getResultBus().copyAttributes(op.getResultBus());
			newSequence.add(combineOp);
			deps.put(combineOp.getLeftDataPort(), new DataDependency(b1));
			deps.put(combineOp.getRightDataPort(), new DataDependency(b2));
			termBuses.add(combineOp.getResultBus());
		}

		assert termBuses.size() == 1;
		final Bus result = (Bus) termBuses.get(0);

		// Create the block
		Block implementation = new Block(newSequence, false);
		container.setContainer(implementation);

		// Establish dep from final add/subtract to exit
		Bus implementationResult = implementation.getExit(Exit.DONE)
				.makeDataBus();
		implementationResult.copyAttributes(op.getResultBus());
		deps.put(implementationResult.getPeer(), new DataDependency(result));

		// Establish deps from data input to shifts
		Port implementationData = implementation.makeDataPort();
		implementationData.getPeer().setSize(argValue.getSize(),
				argValue.isSigned());
		for (Iterator<LeftShiftOp> iter = shifts.iterator(); iter.hasNext();) {
			deps.put(((LeftShiftOp) iter.next()).getLeftDataPort(),
					new DataDependency(implementationData.getPeer()));
		}

		// Establish data deps
		for (Port port : deps.keySet()) {
			Dependency dep = (Dependency) deps.get(port);
			((Entry) port.getOwner().getEntries().get(0)).addDependency(port,
					dep);
		}

		return true;
	}

	/**
	 * Creates a list of powers of two which, when added, add up to the
	 * specified number.
	 * 
	 * @param long constant
	 * @return a List of Integers representing the powers of 2 which sum up to
	 *         the given number.
	 */
	private static List<Integer> calculateAdditionTerms(long number) {
		long width = getWidth(number);
		List<Integer> addition = new ArrayList<Integer>();
		for (int i = 0; i < width; i++) {
			int tmp = getPowerOfTwo(number & (1L << i));
			if (tmp != -1) {
				addition.add(new Integer(tmp));
			}
		}
		return addition;
	}

	/**
	 * Creates a list of powers of two which, when subtracted from the larges
	 * term, result in the specified number.
	 * 
	 * @param long constant
	 * @return a List of Integers representing the powers of 2 which total the
	 *         given number (largest - rest).
	 */
	private static List<Integer> calculateSubtractionTerms(long number) {
		long negative = -number;
		List<Integer> subtraction = new ArrayList<Integer>();
		for (int k = 0; k < getWidth(number) + 1; k++) {
			int tmp = getPowerOfTwo(negative & (1L << k));
			if (tmp != -1) {
				subtraction.add(new Integer(tmp));
			}
		}
		return subtraction;
	}

	/**
	 * @param number
	 *            a number
	 * @return if the number is a power of 2, return the power otherwise return
	 *         -1
	 */
	private static int getPowerOfTwo(long number) {
		int power = -1;
		for (int i = 0; i < 64; i++) {
			if (1L << i == number) {
				power = i;
				break;
			}
		}
		return power;
	}

	/**
	 * @param long a constant
	 * 
	 * @return width of the constant + sign bit
	 */
	private static int getWidth(long number) {
		// force minimum width to be 2

		for (int i = 2; i < 64; i++) {
			if ((~(((1L << i) - 1)) & number) == 0) {
				return i;
			}
		}

		return 64;
	}

	private static int getLog2(int number) {
		int stages = 0;
		while ((1 << stages) < number - 1) {
			stages++;
		}
		return stages;
	}

}// MultiplyOpRule

