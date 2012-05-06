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

package net.sf.openforge.verilog.translate;

import java.util.Collection;
import java.util.LinkedList;

import net.sf.openforge.lim.BidirectionalPin;
import net.sf.openforge.lim.Bit;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.FilteredVisitor;
import net.sf.openforge.lim.InputPin;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutputPin;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Register;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.UnexpectedVisitationException;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.lim.op.Constant;

/**
 * ValueCompactor visits every {@link Port} and {@link Bus} in a given
 * {@link Design} and compacts the {@link Value}, if any, that it finds there
 * (using {@link Port#_getValue()} or {@link Bus#_getValue()}.. By "compact," we
 * mean that if the value contains any don't-care bits at its high end, a new
 * value is created that does not have these bits; this value is then forcibly
 * set using {@link Port#forceValue(Value)} or {@link Bus#forceValue(Value)}.
 * This is done to make the new constant propagation representation more
 * palatable to the translator.
 * 
 * @version $Id: ValueCompactor.java 23 2005-09-09 18:45:32Z imiller $
 */
class ValueCompactor {

	/**
	 * Compacts the {@link Value Values} of a given {@link Design}.
	 * 
	 * @param design
	 *            the design to be compacted
	 */
	public static void compact(Design design) {
		design.accept(new CompactingVisitor());
	}

	/**
	 * No instances, please.
	 */
	private ValueCompactor() {
		super();
	}

	/**
	 * CompactingVisitor visits and compacts the values of each component in the
	 * design.
	 */
	private static class CompactingVisitor extends FilteredVisitor {
		CompactingVisitor() {
			super(true);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void visit(Design design) {
			// visit(design.getTasks());
			for (Task task : design.getTasks()) {
				// The design module causes us to miss the
				// tasks... handle that here.
				final Constant thisConstant = task.getThisConstant();
				if (thisConstant != null) {
					thisConstant.accept(this);
				}
			}

			LinkedList<Component> comps = new LinkedList<Component>(design
					.getDesignModule().getComponents());
			while (!comps.isEmpty()) {
				Visitable vis = comps.remove(0);
				try {
					vis.accept(this);
				} catch (UnexpectedVisitationException uve) {
					if (vis instanceof net.sf.openforge.lim.Module) {
						comps.addAll(((net.sf.openforge.lim.Module) vis)
								.getComponents());
					} else {
						throw uve;
					}
				}
			}

			for (Register reg : design.getRegisters()) {
				visit(reg);
			}

			for (LogicalMemory logicalMemory : design.getLogicalMemories()) {
				visit(logicalMemory);
			}

			// for (Iterator iter = design.getPinReferees().iterator();
			// iter.hasNext();)
			// {
			// ((PinReferee)iter.next()).accept(this);
			// }

			for (Pin pin : design.getInputPins()) {
				visit((InputPin) pin);
			}

			for (Pin pin : design.getOutputPins()) {
				visit((OutputPin) pin);
			}

			for (Pin pin : design.getBidirectionalPins()) {
				visit((BidirectionalPin) pin);
			}

			// visit(design.getKickers());
			// visit(design.getFlipFlopsChain());
		}

		/**
		 * <b>NOTE: We may be able to reduce the data bus and MemoryBank width
		 * for MemoryBank to an absolute minimum. But for now, don't compact
		 * MemoryBank data port/bus values and leave then to their natural
		 * size</b>
		 * 
		 * @param mb
		 *            the MemoryBank to be visited
		 */
		@Override
		public void visit(MemoryBank mb) {
			// left blank to skip compacting

			// NOTE:
			// ValueCompactor skips compacting Values within MemoryBanks.
			// This is because presently the width for a MemoryBank is always
			// the size of the initial value type. However, MemoryBank's
			// data-in port and data-out bus will be resized. This creates a
			// size mismatch problem when instantiating a SinglePortRam.
			//
			// I believe this issue should be addressed when we are doing memory
			// optimization. We can dynamically update the needed bank width by
			// querying all the accesses.
		}

		/**
		 * Truncates the {@link Value Values} of a component's ports and buses.
		 * 
		 * @param component
		 *            the next component visited
		 */
		@Override
		public void preFilterAny(Component component) {
			truncateValues(component);
		}

		@Override
		protected void traverse(PinRead pinRead) {
			if (pinRead.getPhysicalComponent() != null) {
				truncateValues(pinRead.getPhysicalComponent());
			}
			super.traverse(pinRead);
		}

		@Override
		protected void traverse(PinWrite pinWrite) {
			if (pinWrite.getPhysicalComponent() != null) {
				truncateValues(pinWrite.getPhysicalComponent());
			}
			super.traverse(pinWrite);
		}

		@Override
		protected void traverse(PinStateChange pinStateChange) {
			if (pinStateChange.getPhysicalComponent() != null) {
				truncateValues(pinStateChange.getPhysicalComponent());
			}
			super.traverse(pinStateChange);
		}

		/**
		 * Visits the implementation of a {@link Register}.
		 * 
		 * @param register
		 *            a value of type 'Register'
		 */
		private void visit(Register register) {
			if (register.getInputSwapper() != null)
				visit((Module) register.getInputSwapper());

			visit(register.getPhysicalComponent());

			if (register.getOutputSwapper() != null)
				visit((Module) register.getOutputSwapper());
		}

		private void visit(LogicalMemory memory) {
			for (LogicalMemoryPort memoryPort : memory.getLogicalMemoryPorts()) {
				memoryPort.getReferee().accept(this);
			}
			visit(memory.getStructuralMemory());
		}

		/**
		 * Visits the components assocated with an {@link InputPin}.
		 * 
		 * @param inputPin
		 *            the pin to be visited
		 */
		private void visit(InputPin inputPin) {
			truncateValues(inputPin);
			if (inputPin.getInPinBuf().getPhysicalComponent() != null) {
				visit(inputPin.getInPinBuf().getPhysicalComponent());
			}
		}

		/**
		 * Visits the components assocated with an {@link OutputPin}.
		 * 
		 * @param outputPin
		 *            the pin to be visited
		 */
		private void visit(OutputPin outputPin) {
			truncateValues(outputPin);
			if (outputPin.getOutPinBuf().getPhysicalComponent() != null) {
				visit(outputPin.getOutPinBuf().getPhysicalComponent());
			}
		}

		/**
		 * Visits the components associated with a {@link BidirectionalPin}.
		 * 
		 * @param biPin
		 *            the pin to be visited
		 */
		private void visit(BidirectionalPin biPin) {
			truncateValues(biPin);
			if (biPin.getInPinBuf().getPhysicalComponent() != null) {
				visit(biPin.getInPinBuf().getPhysicalComponent());
			}
			if (biPin.getOutPinBuf().getPhysicalComponent() != null) {
				visit(biPin.getOutPinBuf().getPhysicalComponent());
			}
		}

		/**
		 * Visits the elements of a collection.
		 * 
		 * @param collection
		 *            a collection of {@link Visitable} objects
		 */
		private void visit(Collection<Component> collection) {
			for (Component component : collection) {
				((Visitable) component).accept(this);
			}
		}

		/**
		 * Visits a non-{@link Visitable} {@link Module}.
		 * 
		 * @param module
		 *            a module that does not properly implement
		 *            {@link Visitable}
		 */
		private void visit(Module module) {
			truncateValues(module);
			visit(module.getComponents());
		}

		/**
		 * Gets the {@link Value} from each {@link Port} of a given
		 * {@link Component}, and, if the value is not null, creates a new
		 * {@link Value} with the high order don't care bits truncated; this new
		 * value is then forced back into the port with
		 * {@link Port#forceValue(Value)}. The same operation is then performed
		 * on each {@link Bus} of the component.
		 * 
		 * @param component
		 *            the next component to be visited
		 */
		private static void truncateValues(Component component) {
			for (Port port : component.getPorts()) {
				final Value value = port.getValue();
				if (value != null) {
					port.forceValue(getTruncatedValue(value));
				}
			}

			for (Bus bus : component.getBuses()) {
				final Value value = bus.getValue();
				if (value != null) {
					bus.forceValue(getTruncatedValue(value));
				}
			}
		}

		/**
		 * Gets a truncated value.
		 * 
		 * @param value
		 *            the value to be truncated
		 * @return returns a new value whose bits are those of the given value,
		 *         except that the high order don't-care bits have been removed
		 */
		private static Value getTruncatedValue(Value value) {
			/*
			 * Find the index of the most significant care bit, or 0 if there
			 * are none.
			 */
			int msbIndex = value.getSize() - 1;
			while ((msbIndex > 0) && !value.getBit(msbIndex).isCare()) {
				msbIndex--;
			}

			/*
			 * If the value is entirely don't-care, leave it the same size and
			 * change all bits to zeroes for the sake of the translator.
			 * 
			 * The reason we do this is that even though the wire is dead, the
			 * optimization is not yet there to rip it out. The translator is
			 * expecting something of the same size, so we need to provide it.
			 */
			if ((msbIndex == 0) && !value.getBit(0).isCare()) {
				final Value truncatedValue = new Value(value.getSize(),
						value.isSigned());
				for (int i = 0; i < truncatedValue.getSize(); i++) {
					truncatedValue.setBit(i, Bit.ZERO);
				}
				return truncatedValue;
			}

			/*
			 * Create a truncated value that contains only the bits up to and
			 * including the MSB computed above. If there are any embedded
			 * don't-cares, replace them with zeroes, for the sake of the
			 * translator.
			 */
			final Value truncatedValue = new Value((msbIndex + 1),
					value.isSigned());
			for (int i = 0; i < truncatedValue.getSize(); i++) {
				final Bit bit = value.getBit(i);
				truncatedValue.setBit(i, (bit.isCare() ? bit : Bit.ZERO));
			}
			return truncatedValue;
		}

	} // CompactingVisitor

}
