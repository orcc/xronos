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

package org.xronos.openforge.verilog.mapping;

import java.util.HashMap;
import java.util.Map;

import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.lim.memory.MemoryImplementation;
import org.xronos.openforge.verilog.mapping.memory.DualPortBlockWriter;
import org.xronos.openforge.verilog.mapping.memory.DualPortInferredWriter;
import org.xronos.openforge.verilog.mapping.memory.DualPortLutWriter;
import org.xronos.openforge.verilog.mapping.memory.SinglePortInferredRamWriter;
import org.xronos.openforge.verilog.mapping.memory.SinglePortInferredRomWriter;
import org.xronos.openforge.verilog.mapping.memory.SinglePortRamWriter;
import org.xronos.openforge.verilog.mapping.memory.SinglePortRomWriter;
import org.xronos.openforge.verilog.mapping.memory.VerilogMemory;


/**
 * A MemoryMapper is a utility class to help on memory mapping decision.
 * <P>
 * 
 * Created: Tue Jun 18 12:37:06 2002
 * 
 * @author cwu
 * @version $Id: MemoryMapper.java 2 2005-06-09 20:00:48Z imiller $
 */

public class MemoryMapper {

	public final static String SIM_INCLUDE_PATH = "$XILINX/verilog/src/unisims/";
	public final static String SIMPRIM_INCLUDE_PATH = "$XILINX/verilog/src/simprims/";
	public final static String SYNTH_INCLUDE_PATH = "$XILINX/verilog/src/iSE/";

	/** A LIM memory to a type of memory being used for mapping */
	private static Map<MemoryBank, VerilogMemory> mapMemoryType = new HashMap<MemoryBank, VerilogMemory>();

	MemoryMapper() {
	}

	/**
	 * Returns the appropriate {@link VerilogMemory} (memory writer) for the
	 * specified memory. This decision is based very closely on the specific
	 * {@link MemoryImplementation}
	 * 
	 * @param memory
	 *            a {@link MemoryBank}
	 * @return a {@link VerilogMemory}
	 */
	// public static VerilogMemory getMemoryType (Memory memory)
	public static VerilogMemory getMemoryType(MemoryBank memory) {
		VerilogMemory allocated = mapMemoryType.get(memory);
		if (allocated == null) {
			MemoryImplementation impl = memory.getImplementation();

			int portCount = memory.getBankPorts().size();

			if (portCount == 2) {
				if (impl.isDefault()) {
					allocated = new DualPortInferredWriter(memory);
				} else {
					if (impl.isLUT())
						allocated = new DualPortLutWriter(memory);
					else
						allocated = new DualPortBlockWriter(memory);
				}
			} else if (portCount == 1) {
				if (impl.isDefault()) {
					if (impl.isROM())
						allocated = new SinglePortInferredRomWriter(memory);
					else
						allocated = new SinglePortInferredRamWriter(memory);
				} else {
					if (impl.isROM())
						allocated = new SinglePortRomWriter(memory);
					else
						allocated = new SinglePortRamWriter(memory);
				}
			} else {
				throw new IllegalArgumentException(
						"All memories must have either 1 or 2 ports. Found: "
								+ portCount);
			}
			mapMemoryType.put(memory, allocated);
		}
		// System.out.println("Allocated memory writer: " + allocated + " for "
		// + memory + " " + memory.getImplementation());
		return allocated;
	}

} // class MemoryMapper
