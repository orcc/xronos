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

package net.sf.openforge.backend.timedc;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.memory.AddressStridePolicy;
import net.sf.openforge.lim.memory.AddressableUnit;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.lim.memory.StructuralMemory;
import net.sf.openforge.util.naming.ID;

/**
 * MemoryWriter generates a mapping of {@link LogicalMemory} to
 * {@link MemoryVar}. Encapsulated in this class is the ability to generate the
 * initialization data for the memory in either big or little endian order.
 * 
 * 
 * <p>
 * Created: Wed Mar 2 20:55:10 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryWriter.java 120 2006-03-27 19:57:10Z imiller $
 */
public class MemoryWriter {

	/** No instances please, just the static method contained here. */
	private MemoryWriter() {
	}

	public static Map<LogicalMemory, MemoryVar> generateMemories(Design design) {
		final Map<LogicalMemory, MemoryVar> memoryMap = new HashMap<LogicalMemory, MemoryVar>();
		final GenericJob gj = EngineThread.getGenericJob();
		@SuppressWarnings("unused")
		final boolean isLittleEndian = gj
				.getUnscopedBooleanOptionValue(OptionRegistry.LITTLE_ENDIAN);

		for (LogicalMemory logicalMem : design.getLogicalMemories()) {

			// Add the register to the map
			final String hash = Integer.toHexString(logicalMem.hashCode());
			final String name = ID.showLogical(logicalMem) + hash;
			final String legalName = CNameCache.getLegalIdentifier(name);

			// Get the structural memory and the size of each data in the memory
			final StructuralMemory sm = logicalMem.getStructuralMemory();

			// Get the Initial value of the memory :
			// LogicalMemory->StructuralMemory->List of MemoryBank->InitValues
			final int size = sm.getAddressableLocations();
			final AddressStridePolicy addressPolicy = logicalMem
					.getAddressStridePolicy();
			final int stride = addressPolicy.getStride();

			if (!(stride == 8 || stride == 16 || stride == 32 || stride == 64)) {
				if (stride != sm.getDataWidth()) {
					throw new IllegalArgumentException(
							"Cannot generate C memory for memory with stride of "
									+ stride + " and width of "
									+ sm.getDataWidth());
				}
			}

			// Collect the banks that make up the memory
			final List<MemoryBank> banks = sm.getBanks();
			boolean first = true;
			final int numBanks = banks.size();
			final int mbWidth = banks.get(0).getWidth();
			final int numLines = (int) Math.ceil((double) size
					/ (double) (mbWidth * numBanks));

			String initialization = "";
			final String memType = OpHandle.getTypeDeclaration(mbWidth, true);

			initialization += "{";

			for (int row = 0; row < numLines; row++) {
				for (MemoryBank mb : banks) {
					// final byte [][] values = mb.getInitValues();
					final AddressableUnit[][] values = mb.getInitValues();
					final int numColumns = values[0].length;
					// final byte [] rep = new byte[numColumns];
					final AddressableUnit[] rep = new AddressableUnit[numColumns];
					System.arraycopy(values[row], 0, rep, 0, numColumns);

					long value = constantValue(rep, addressPolicy);
					if (!first)
						initialization += ",";
					else
						first = false;
					initialization += Long.toString(value);
				}
			}
			initialization += "}";
			memoryMap.put(logicalMem, new MemoryVar(legalName, initialization,
					memType, logicalMem.getLogicalMemoryPorts()));
		}

		return memoryMap;
	}

	/**
	 * Returns an endian-correct constant for the given byte rep. Endianness is
	 * determined by the endianness of the compilation as specified by the
	 * command line options.
	 * 
	 * @param rep
	 *            [] an array of bytes, not longer than 8 elements.
	 * @return a value of type 'long'
	 */
	static long constantValue(byte rep[]) {
		assert rep.length <= 8 : "Rep too long " + rep.length;

		final boolean isLittleEndian = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(OptionRegistry.LITTLE_ENDIAN);
		long value = 0;
		if (isLittleEndian) {
			for (int i = 0; i < rep.length; i++) {
				long temp = (((long) rep[i]) & 0xFF);
				value |= (temp << (8 * i));
			}
		} else {
			for (int i = rep.length - 1; i >= 0; i--) {
				long temp = (((long) rep[i]) & 0xFF);
				value |= (temp << (8 * (rep.length - i - 1)));
			}
		}
		return value;
	}

	static long constantValue(AddressableUnit rep[], AddressStridePolicy policy) {
		final boolean isLittleEndian = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(OptionRegistry.LITTLE_ENDIAN);

		AddressableUnit[] theRep = rep;

		if (!isLittleEndian) {
			theRep = new AddressableUnit[rep.length];
			for (int i = 0; i < rep.length; i++) {
				theRep[i] = rep[rep.length - 1 - i];
			}
		}

		BigInteger value = AddressableUnit.getCompositeValue(theRep, policy);
		assert (value.bitLength() + 1) < 64 : "AddressableUnit representation too large for long";
		return value.longValue();
	}

}// MemoryWriter
