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

package org.xronos.openforge.optimize.memory;

import java.util.HashSet;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.Option;
import org.xronos.openforge.app.project.OptionInt;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.FailVisitor;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.Register;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.Allocation;
import org.xronos.openforge.lim.memory.LValue;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.optimize.ComponentSwapVisitor;
import org.xronos.openforge.optimize.Optimization;


/**
 * MemoryToRegister analyses each memory in the design, and for any memory that
 * has only a single accessed element (contiguous region of bytes) that memory
 * will be converted to a {@link Register}. The criteria to convert from memory
 * to Register is simply that all Locations in the target memory must be to the
 * same base and have the same offset and size. After converting the memory to a
 * Register, all LValue accessors of that memory are transformed to a
 * {@link RegisterRead} or {@link RegisterWrite} as appropriate. The memory is
 * not deleted because the Allocation that it contains may still be the target
 * of an AddressOf operation, in which case the Allocation and the memory it is
 * in are needed to generate a correct base address. However, it is always true
 * that the remaining memory will have not LValue accesses that target it.
 * 
 * <p>
 * Created: Wed Oct 22 15:35:05 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryToRegister.java 107 2006-02-23 15:46:07Z imiller $
 */
public class MemoryToRegister implements Optimization {

	class ConvertVisitor extends FailVisitor {
		private Register register = null;

		public ConvertVisitor(Register reg) {
			super("conversion of single element memories to registers");
			this.register = reg;
		}

		private void deleteAccessor(LValue lvalue) {
			lvalue.getLogicalMemoryPort().removeAccess(lvalue);
		}

		@Override
		public void visit(AbsoluteMemoryRead lvalue) {
			Component read = this.register.createReadAccess(lvalue
					.getMemoryAccess().isSigned());
			(new ComponentSwapVisitor()).swapComponents(lvalue, read);
			deleteAccessor(lvalue);
		}

		@Override
		public void visit(AbsoluteMemoryWrite lvalue) {
			Component write = this.register.createWriteAccess(lvalue
					.getMemoryAccess().isSigned());
			(new ComponentSwapVisitor()).swapComponents(lvalue, write);
			deleteAccessor(lvalue);
		}

		// Because we preclude 'Index' locations to initialize the
		// Register, it should be impossible for us to ever reach an
		// Array access LValue. So we defer to the super which will
		// fail with an unexpected traversal error.
		@Override
		public void visit(ArrayRead lvalue) {
			super.visit(lvalue);
		}

		@Override
		public void visit(ArrayWrite lvalue) {
			super.visit(lvalue);
		}

		@Override
		public void visit(HeapRead lvalue) {
			Component read = this.register.createReadAccess(lvalue
					.getMemoryAccess().isSigned());
			lvalue.removeDataPort(lvalue.getBaseAddressPort());
			(new ComponentSwapVisitor()).swapComponents(lvalue, read);
			deleteAccessor(lvalue);
		}

		@Override
		public void visit(HeapWrite lvalue) {
			Component write = this.register.createWriteAccess(lvalue
					.getMemoryAccess().isSigned());
			lvalue.removeDataPort(lvalue.getBaseAddressPort());
			(new ComponentSwapVisitor()).swapComponents(lvalue, write);
			deleteAccessor(lvalue);
		}
	}

	/** A count of how many memories were converted into registers. */
	private int convCount = 0;

	public MemoryToRegister() {
	}

	@Override
	public void clear() {
		this.convCount = 0;
	}

	@Override
	public boolean didModify() {
		return this.convCount > 0;
	}

	/**
	 * Determine if the memory can be reduced to a register, and if so:<br>
	 * <ul>
	 * <li>generates the replacement Register
	 * <li>converts the accesses
	 * <li>adds the Register to the design
	 * <li><strike>removes the logical memory from the design</strike> Allow the
	 * memory reducer and/or dead component visitor to remove the memory for us.
	 * There may be things still in the memory that are referenced via only an
	 * address-of operator.
	 * </ul>
	 * 
	 * @param memory
	 *            the non-null {@link LogicalMemory} to be analysed and possibly
	 *            replaced
	 * @param design
	 *            the {@link Design} which contains the given LogicalMemory and
	 *            to which any generated Register will be added.
	 * @throws IllegalArgumentException
	 *             if either memory or design is null.
	 */
	private void optimizeMem(LogicalMemory memory, Design design) {
		LocationSet locations = new LocationSet();
		final Option maxSizeOption = EngineThread.getGenericJob().getOption(
				OptionRegistry.MEM_DECOMPOSE_LIMIT);
		final int maxSize = ((OptionInt) maxSizeOption)
				.getValueAsInt(CodeLabel.UNSCOPED);

		// If the memory is larger than the limit then do not convert it.
		if (memory.getSizeInBytes() > maxSize) {
			return;
		}

		for (LValue element : memory.getLValues()) {
			LValue lvalue = element;
			for (Location location : memory.getAccesses(lvalue)) {
				locations.add(location);
				if (locations.size() > 1) {
					// Too many non-unique locations, cant turn to Register.
					return;
				}
			}
		}
		if (locations.size() != 1) // Handle the 0 location case
		{
			return;
		}

		Location loc = locations.iterator().next();

		// The Location may be an index which we cannot analyze, so
		// detect that via the IllegalInitialValueContextException
		// (see Location.getInitialValue spec) and stop replacement if
		// encountered.
		LogicalValue init = null;
		try {
			init = loc.getInitialValue();
		} catch (Location.IllegalInitialValueContextException e) {
			return;
		}

		// Create the register (non volatile for now) XXX fixme.
		// Take 'volatile' info from the initial declaration of the
		// memory in the users C code.
		Register replacement = new Register(init, init.getBitSize(), false);
		replacement.setSourceName(((Allocation) loc.getAbsoluteBase())
				.getSourceName());
		replacement.setIDLogical(memory.showIDLogical());
		ConvertVisitor converter = new ConvertVisitor(replacement);
		for (LValue lvalue : new HashSet<LValue>(memory.getLValues())) {
			lvalue.accept(converter);
		}

		design.addRegister(replacement);
		// design.removeMemory(memory);

		this.convCount++;
	}

	@Override
	public void postStatus() {
		String end;
		if (this.convCount == 1) {
			end = " memory to a register";
		} else {
			end = " memories to registers";
		}
		EngineThread.getGenericJob().info(
				"Converted " + this.convCount + end + ".");
	}

	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info(
				"Converting single element memories to registers...");
	}

	@Override
	public void run(Visitable vis) {
		if (vis instanceof Design) {
			Design design = (Design) vis;
			org.xronos.openforge.optimize.memory.ObjectResolver.resolve(design);

			HashSet<LogicalMemory> memories = new HashSet<LogicalMemory>(
					design.getLogicalMemories());
			for (LogicalMemory element : memories) {
				this.optimizeMem(element, design);
			}
		}
	}

}// MemoryToRegister
