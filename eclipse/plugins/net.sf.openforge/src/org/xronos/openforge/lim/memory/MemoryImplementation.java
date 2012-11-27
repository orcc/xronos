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

package org.xronos.openforge.lim.memory;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.Option;
import org.xronos.openforge.app.project.OptionInt;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.util.XilinxDevice;
import org.xronos.openforge.verilog.mapping.memory.DualPortRam;
import org.xronos.openforge.verilog.mapping.memory.Ram;

/**
 * MemoryImplementation is a class that looks at the specific memory to be
 * implemented and determines the optimal implementation type (lut vs block and
 * RAM vs ROM).
 * 
 * <p>
 * Created: Mon Dec 2 16:12:19 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryImplementation.java 538 2007-11-21 06:22:39Z imiller $
 */
public abstract class MemoryImplementation {

	public MemoryImplementation() {
	}

	public abstract Latency getReadLatency();

	public abstract Latency getWriteLatency();

	public abstract boolean isLUT();

	public abstract boolean isROM();

	/**
	 * Returns true if this memory, in dual port mode, resolves address
	 * conflicts by a read-first strategy. Returns false if the strategy is
	 * write-first. NOTE that this is the behavior across the two ports, NOT
	 * single port behavior.
	 */
	public abstract boolean isDPReadFirst();

	public abstract boolean isDefault();

	public static void setMemoryImplementation(LogicalMemory mem) {
		boolean readOnly = true;
		for (LogicalMemoryPort port : mem.getLogicalMemoryPorts()) {
			readOnly = readOnly & port.isReadOnly();
		}

		// Note that the way in which the memory is packed may affect
		// the final number of bytes allocated. If structures need to
		// fall on specific byte alignments then there may be bytes
		// allocated only for address alignment, effectively
		// increasing the size of the memory over the actual bytes
		// stored. However, the type of implementation needs to be
		// known for some optimizations prior to fixating the
		// structure and packing of the memory.
		mem.setImplementation(getMemoryImplementation(mem.getSizeInBytes(),
				readOnly, mem.getLogicalMemoryPorts().size()));
	}

	/**
	 * Returns the implementation style for the memory.
	 * 
	 * @param bitWidth
	 *            the width of the memory in bits
	 * @param depth
	 *            the depth of the memory in lines
	 * @param readOnly
	 *            , true if the memory is a read only memory
	 * @param portCount
	 *            , the number of access ports to the memory.
	 */
	public static MemoryImplementation getMemoryImplementation(int bytes,
			boolean readOnly, int portCount) {
		GenericJob gj = EngineThread.getGenericJob();
		Option op;
		XilinxDevice xd = gj.getPart(CodeLabel.UNSCOPED);

		// op = gj.getOption(OptionRegistry.MAX_LUT_WIDTH);
		// int lut_width = new
		// Integer(op.getValue(CodeLabel.UNSCOPED).toString()).intValue();
		// op = gj.getOption(OptionRegistry.MAX_LUT_DEPTH);
		// int lut_depth = new
		// Integer(op.getValue(CodeLabel.UNSCOPED).toString()).intValue();
		op = gj.getOption(OptionRegistry.MAX_LUT_BYTES);
		final int lut_bytes = ((OptionInt) op)
				.getValueAsInt(CodeLabel.UNSCOPED);
		/*
		 * If set to true, our memory writer will add a register between the RAM
		 * primitive DOUT and the output of our memory.
		 */
		boolean lutsAreCombRead = gj
				.getUnscopedBooleanOptionValue(OptionRegistry.COMBINATIONAL_LUT_MEM);

		// boolean mapToLut = bitWidth <= lut_width && depth <= lut_depth;
		boolean mapToLut = bytes <= lut_bytes;
		boolean isROM = readOnly;

		if (portCount == 0 || portCount == 1) {
			Ram[] availableMaps = Ram.getMappers(xd, mapToLut);
			if (availableMaps != null && availableMaps.length > 0) {
				return getType(isROM, mapToLut, lutsAreCombRead);
			}

			// Try the other type of memory since no match found
			mapToLut = !mapToLut;
			availableMaps = Ram.getMappers(xd, mapToLut);
			if (availableMaps != null && availableMaps.length > 0) {
				return getType(isROM, mapToLut, lutsAreCombRead);
			}
		} else if (portCount == 2) {
			Ram[] availableMaps = DualPortRam.getMappers(xd, mapToLut);
			if (availableMaps != null && availableMaps.length > 0) {
				return getType(isROM, mapToLut, lutsAreCombRead);
			}

			// Try the other type of memory since no match found
			mapToLut = !mapToLut;
			availableMaps = DualPortRam.getMappers(xd, mapToLut);
			if (availableMaps != null && availableMaps.length > 0) {
				return getType(isROM, mapToLut, lutsAreCombRead);
			}
		} else {
			throw new IllegalArgumentException(
					"All memories must be either single or dual ported. "
							+ portCount);
		}

		return (isROM ? ((MemoryImplementation) new DefaultROM())
				: ((MemoryImplementation) new DefaultRAM()));
	}

	private static MemoryImplementation getType(boolean isROM, boolean isLUT,
			boolean combLutRead) {
		if (isLUT) {
			if (combLutRead) {
				return (isROM ? ((MemoryImplementation) new LutROM())
						: ((MemoryImplementation) new LutRAM()));
			} else {
				return (isROM ? ((MemoryImplementation) new RegisteredLutROM())
						: ((MemoryImplementation) new RegisteredLutRAM()));
			}
		} else {
			return (isROM ? ((MemoryImplementation) new BlockROM())
					: ((MemoryImplementation) new BlockRAM()));
		}
	}

	@Override
	public String toString() {
		return super.toString().replaceAll("net.sf.openforge.lim.memory.", "");
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MemoryImplementation))
			return false;
		MemoryImplementation test = (MemoryImplementation) o;

		if (!getReadLatency().equals(test.getReadLatency()))
			return false;
		if (isROM() != test.isROM())
			return false;
		if (!isROM()) {
			if (!getWriteLatency().equals(test.getWriteLatency()))
				return false;
		}
		if (isLUT() != test.isLUT())
			return false;
		if (isDPReadFirst() != test.isDPReadFirst())
			return false;
		if (isDefault() != test.isDefault())
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int base = 103598574;
		base += getReadLatency().hashCode();
		if (!isROM())
			base += getWriteLatency().hashCode();
		if (isLUT())
			base += 285;
		if (isROM())
			base += 385;
		if (isDPReadFirst())
			base += 485;
		if (isDefault())
			base += 585;
		return base;
	}

	// These classes are closely tied to the
	// verilog.mapping.memory.MemoryMapper class which uses the
	// information from these classes to determine which writer to
	// use. A better approach would be to create a Visitor that would
	// visit each type of implementation and create the appropriate
	// writer from that.

	private static class LutRAM extends MemoryImplementation {
		@Override
		public Latency getReadLatency() {
			return Latency.ZERO;
		}

		@Override
		public Latency getWriteLatency() {
			return Latency.ONE;
		}

		@Override
		public boolean isLUT() {
			return true;
		}

		@Override
		public boolean isROM() {
			return false;
		}

		@Override
		public boolean isDPReadFirst() {
			return true;
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	}

	private static class RegisteredLutRAM extends MemoryImplementation {
		@Override
		public Latency getReadLatency() {
			return Latency.ONE;
		}

		@Override
		public Latency getWriteLatency() {
			return Latency.ONE;
		}

		@Override
		public boolean isLUT() {
			return true;
		}

		@Override
		public boolean isROM() {
			return false;
		}

		@Override
		public boolean isDPReadFirst() {
			return true;
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	}

	private static class LutROM extends MemoryImplementation {
		@Override
		public Latency getReadLatency() {
			return Latency.ZERO;
		}

		@Override
		public Latency getWriteLatency() {
			throw new UnsupportedOperationException("Cannot write to a ROM");
		}

		@Override
		public boolean isLUT() {
			return true;
		}

		@Override
		public boolean isROM() {
			return true;
		}

		@Override
		public boolean isDPReadFirst() {
			return true;
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	}

	private static class RegisteredLutROM extends MemoryImplementation {
		@Override
		public Latency getReadLatency() {
			return Latency.ONE;
		}

		@Override
		public Latency getWriteLatency() {
			throw new UnsupportedOperationException("Cannot write to a ROM");
		}

		@Override
		public boolean isLUT() {
			return true;
		}

		@Override
		public boolean isROM() {
			return true;
		}

		@Override
		public boolean isDPReadFirst() {
			return true;
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	}

	private static class BlockRAM extends MemoryImplementation {
		@Override
		public Latency getReadLatency() {
			return Latency.ONE;
		}

		@Override
		public Latency getWriteLatency() {
			return Latency.ONE;
		}

		@Override
		public boolean isLUT() {
			return false;
		}

		@Override
		public boolean isROM() {
			return false;
		}

		// Intentionally set to always read first. Otherwise caution
		// must be exercised b/c the write-first mode returns corrupt
		// data on the read port, as per V4 user guide. If this is
		// changed, then the parameter setting in DualPortBlockRam
		// must also be changed.
		@Override
		public boolean isDPReadFirst() {
			return true;
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	}

	private static class BlockROM extends MemoryImplementation {
		@Override
		public Latency getReadLatency() {
			return Latency.ONE;
		}

		@Override
		public Latency getWriteLatency() {
			throw new UnsupportedOperationException("Cannot write to a ROM");
		}

		@Override
		public boolean isLUT() {
			return false;
		}

		@Override
		public boolean isROM() {
			return true;
		}

		// Intentionally set to always read first. Otherwise caution
		// must be exercised b/c the write-first mode returns corrupt
		// data on the read port, as per V4 user guide. If this is
		// changed, then the parameter setting in DualPortBlockRam
		// must also be changed.
		@Override
		public boolean isDPReadFirst() {
			return true;
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	}

	// Default implementations are simply non-part specific memory
	// structures with combinational reads and single cycle writes.
	// These structures are only implementable in distributed RAM.
	private static class DefaultRAM extends MemoryImplementation {
		@Override
		public Latency getReadLatency() {
			return Latency.ZERO;
		}

		@Override
		public Latency getWriteLatency() {
			return Latency.ONE;
		}

		@Override
		public boolean isLUT() {
			return false;
		}

		@Override
		public boolean isROM() {
			return false;
		}

		@Override
		public boolean isDPReadFirst() {
			return true;
		}

		@Override
		public boolean isDefault() {
			return true;
		}
	}

	private static class DefaultROM extends MemoryImplementation {
		@Override
		public Latency getReadLatency() {
			return Latency.ZERO;
		}

		@Override
		public Latency getWriteLatency() {
			throw new UnsupportedOperationException("Cannot write to a ROM");
		}

		@Override
		public boolean isLUT() {
			return false;
		}

		@Override
		public boolean isROM() {
			return true;
		}

		@Override
		public boolean isDPReadFirst() {
			return true;
		}

		@Override
		public boolean isDefault() {
			return true;
		}
	}

	public static final MemoryImplementation __getTestImplementation(String type) {
		if (type.equals("lutram"))
			return new LutRAM();

		return new DefaultRAM();
	}

}// MemoryImplementation
