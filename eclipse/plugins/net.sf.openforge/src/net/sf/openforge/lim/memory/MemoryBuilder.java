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

package net.sf.openforge.lim.memory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionInt;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;

/**
 * MemoryBuilder is responsible for turning {@link Memory} and
 * {@link LogicalMemory} objects in a {@link Design} into their equivalent
 * {@link StructuralMemory}. The determination of what Bank width to use is
 * determined here based on these criteria.
 * <p>
 * The current implementation for selecting bank width of a Memory object simply
 * uses the memories width (normalized to the next larger 2<sup><font
 * size=-1>n</font></sup> x 8)
 * <p>
 * The current implementation for converting {@link LogicalMemory} objects is to
 * identify the largest and smallest accesses (in terms of numbers of bytes
 * accessed). The largest access determines the width of the implemented memory
 * so that the access can be completed in only 1 access. The smallest access
 * determines the width of the banks which make up the memory. Based on these
 * two criteria, the memory is then 'packed' such that each allocation begins at
 * the beginning of a 'line' of the memory (a line is defined by the width of
 * the memory). An allocation which is wider than the memory will thus span
 * multiple lines of the memory. Any extra bytes between the end of an
 * Allocation and the end of the line are padded with 0's. In this way we are
 * guranteed that all accesses to the memory can complete in a single cycle.
 * 
 * <p>
 * Created: Wed Feb 12 14:09:35 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryBuilder.java 538 2007-11-21 06:22:39Z imiller $
 */
public class MemoryBuilder {

	/**
	 * This is a limitation imposed by the fact that we use 'int' in
	 * LogicalMemory.getAddress() and other places to represent an address.
	 */
	private static final int MAX_ADDR_SIZE = 32;

	public MemoryBuilder() {
	}

	/**
	 * Generates the {@link StructuralMemory} for each {@link Memory} and each
	 * {@link LogicalMemory} in the design.
	 */
	public static void buildMemories(Design design) {
		if (design.getLogicalMemories().size() == 0) {
			return;
		}

		// We use this code to dynamically place the memory ID just
		// above the largest address necessary so that we can support
		// both designs with a few large memories, or lots of small
		// memories.
		int memCount = design.getLogicalMemories().size();
		int addrBits = calcAddrBits(design.getLogicalMemories());
		int idBits = net.sf.openforge.util.MathStuff.log2(memCount);
		if ((addrBits + idBits) > MAX_ADDR_SIZE) {
			EngineThread.getEngine().fatalError(
					"Could not encode memory addresses." + "  A "
							+ (addrBits + idBits)
							+ " bit minimum address size would be necessary."
							+ "  Forge is currently limited to "
							+ MAX_ADDR_SIZE);
		}

		// Build/pack memories in 2 seperate phases. We need to do
		// this so that every LogicalMemory has a base address map
		// established before we begin trying to get the contents of
		// any memory. This allows all Pointers in any memory to have
		// a valid address associated with the Location they target.
		Map<LogicalMemory, PackedMemory> packedMemMap = new HashMap<LogicalMemory, PackedMemory>();
		for (LogicalMemory logicalMemory : design.getLogicalMemories()) {
			// packedMemMap.put(logicalMemory, buildMemory(logicalMemory,
			// addrBits, design.getNextMemoryId()));
			// For now, we'll just use '0' as the memory ID for all
			// memories. This is not correct (as 2 allocations in
			// seperate memories may have the same address but are not
			// equal), however, random assignment of memory ID's isn't
			// correct either, because 2 pointers to the same object
			// may point to different memories in which case they need
			// to be numerically equal.
			packedMemMap.put(logicalMemory,
					buildMemory(logicalMemory, addrBits, 0));
		}
		for (LogicalMemory logicalMemory : design.getLogicalMemories()) {
			final PackedMemory packedMem = packedMemMap.get(logicalMemory);
			if (!packMemory(logicalMemory, packedMem)) {
				design.removeMemory(logicalMemory);
			}
		}
		// _memory.d.launchXGraph(design, false);
	}

	/**
	 * Determine the number of address bits needed to cover all addresses to all
	 * memories in the design
	 * 
	 * @param logicalMems
	 *            a 'Collection' of LogicalMemories
	 * @return the minimum number of address bits necessary to cover the address
	 *         space of all memories.
	 */
	protected static int calcAddrBits(Collection<LogicalMemory> logicalMems) {
		// First determine the largest sized memory so that we know
		// the largest address width needed.
		int maxBytes = 0;
		for (LogicalMemory logicalMemory : logicalMems) {
			maxBytes = Math.max(maxBytes, (logicalMemory).getAddressableSize());
		}
		// The number of address bits needed then is Log2 of
		// maxBytes+1 (plus 1 to account for padding bits which are
		// guaranteed to never double the memory size)
		int addrBits = net.sf.openforge.util.MathStuff.log2(maxBytes) + 1;
		return addrBits;
	}

	/**
	 * Constructs a 'packed memory' representation of the given LogicalMemory
	 * which is used to determine the exact layout and initial values needed for
	 * the physical memory.
	 * 
	 * @param mem
	 *            the 'LogicalMemory' to be packed
	 * @param addrBits
	 *            the number of bits necessary to represent any address in any
	 *            memory of this design
	 * @param memoryID
	 *            a unique non-zero non-negative identifier for the generated
	 *            packed memory.
	 * @return a 'PackedMemory', the packed memory that was built.
	 */
	protected static PackedMemory buildMemory(LogicalMemory mem, int addrBits,
			int memoryID) {
		if (_memory.db) {
			_memory.ln(_memory.BUILD, "Building Structural Memory for " + mem);
		}

		// The largest and smallest access in numbers of addresses
		// accessed atomically.
		int smallestAccess = Integer.MAX_VALUE;
		int largestAccess = Integer.MIN_VALUE;
		for (LValue lvalue : mem.getLValues()) {
			final int lvalueAccessCount = lvalue.getAccessLocationCount();
			if (lvalueAccessCount > 0) {
				/*
				 * Ignore byte counts of 0, which are pure addresses.
				 */
				smallestAccess = Math.min(smallestAccess, lvalueAccessCount);
				largestAccess = Math.max(largestAccess, lvalueAccessCount);
			}
		}

		if (_memory.db) {
			_memory.ln(_memory.BUILD, "Largest Access: " + largestAccess
					+ " Smallest Access: " + smallestAccess);
		}

		// Now build a model of the 'packed' memory which identifies
		// how allocations/locations are stored in the memory and what
		// is defined as a 'line'.
		PackedMemory packedMem = new PackedMemory(largestAccess, addrBits,
				memoryID);
		for (Allocation allocation : mem.getAllocations()) {
			packedMem.allocate(allocation);
		}
		/*
		 * Lock the packed memory to allocate addresses for 0 length
		 * Allocations.
		 */
		packedMem.lock();
		if (_memory.db) {
			_memory.ln(_memory.BUILD, "Packed Memory base Addresses: "
					+ packedMem.getBaseAddressMap());
		}

		/*
		 * The address map has to be computed regardless of whether any actual
		 * storage is needed. This is because there may be operations that just
		 * take the addresses of variables without actuall reading or writing
		 * them (e.g., "&var" in C).
		 */
		mem.setBaseAddressMap(packedMem.getBaseAddressMap());
		packedMem.setSmallestAccess(smallestAccess);
		return packedMem;
	}

	/**
	 * Constructs the actual StructuralMemory based on the data obtained in the
	 * previous steps. This method uses the initial values (byte[][]) obtained
	 * from the PackedMemory to initialize the StructuralMemory.
	 * 
	 * @return a 'boolean', true if a Structural representation was constructed,
	 *         false otherwise.
	 */
	protected static boolean packMemory(LogicalMemory mem,
			PackedMemory packedMem) {
		/*
		 * The memory may still be empty if the only accessors were those that
		 * computed addresses without actually reading or writing any data.
		 */
		if (packedMem.getAddrsPerLine() > 0) {
			// Now that the base address map has been specified, we can
			// initialize the packed memory.
			AddressableUnit[][] init = packedMem.getInitValues();
			if (_memory.db) {
				_memory.ln(_memory.BUILD,
						"Initial Value map for logical memory " + mem);
				for (int i = 0; i < init.length; i++) {
					printArr(init[i]);
				}
			}

			String name = "logicalMem_" + Integer.toHexString(mem.hashCode());

			// The call to this method also causes the memory to
			// verify that it has a consistent stride policy across
			// all allocations.
			final AddressStridePolicy memoryAddressStridePolicy = mem
					.getAddressStridePolicy();

			final int bitWidth = packedMem.getAddrsPerLine()
					* memoryAddressStridePolicy.getStride();

			// NumLines is the total number of addressable locations
			// divided by # addressable locations per line
			final int numLines = packedMem.getAddressCount()
					/ packedMem.getAddrsPerLine();

			final int addressableBytes = Math.max(mem.getSizeInBytes(),
					(bitWidth / 8) * numLines);

			// Bank width is in bits, access size is in addressable
			// locations. The bank width is the size of the smallest
			// access.
			// We no longer ensure that bank width is 2^n * 8 because
			// we support arbitrary sized memory banks.
			final int bankWidth = bankWidthUserOverride(
					packedMem.getSmallestAccess()
							* memoryAddressStridePolicy.getStride(), false);

			// Determine the implementation based on the final
			// characteristics of the memory.
			MemoryImplementation impl = MemoryImplementation
					.getMemoryImplementation(addressableBytes, isReadOnly(mem),
							mem.getLogicalMemoryPorts().size());
			// Just in case it wasn't set previously, handle it here
			if (mem.getImplementation() == null) {
				mem.setImplementation(impl);
			}

			// Test to see if the previously determined configuration
			// matches the final characteristics. If not that is OK.
			GenericJob gj = EngineThread.getGenericJob();
			if (!impl.equals(mem.getImplementation())) {
				gj.warn("Selected memory implementation differs from final characteristics.  This warning is only for informational purposes");
			}

			_memory.ln(_memory.BUILD, "bitWidth " + bitWidth);
			_memory.ln(_memory.BUILD, "numLines " + numLines);
			_memory.ln(_memory.BUILD, "bankWidth " + bankWidth);
			_memory.ln(_memory.BUILD, "impl " + impl);

			StructuralMemory structMem = new StructuralMemory(bitWidth,
					numLines, bankWidth, new ArrayList<LogicalMemoryPort>(
							mem.getLogicalMemoryPorts()), name, impl,
					memoryAddressStridePolicy, mem.getMaxAddressWidth());

			structMem.setInitialValues(init);

			mem.setStructuralImplementation(structMem);
			mem.setImplementation(impl);
			// Wait until we are connecting up the memory in
			// MemoryConnectionVisitor before adding to the design.
			// design.addComponentToDesign(structMem);

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns true if all {@link LogicalMemoryPort}s are read only.
	 */
	private static boolean isReadOnly(LogicalMemory mem) {
		for (LogicalMemoryPort port : mem.getLogicalMemoryPorts()) {
			if (!port.isReadOnly()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This is a utility class used to pack Allocations into a memory of a set
	 * width. This facilitates determining the necessary depth of the memory as
	 * well as dividing up Allocation initial values into the necessary 2-d
	 * array of bytes.
	 */
	protected static class PackedMemory {
		private int addrsPerLine;
		private int nextAddress = 0;
		private Map<Allocation, Integer> baseAddressMap = new HashMap<Allocation, Integer>();
		private int smallestAccessWidth = 128;

		/**
		 * A set of all the Allocations that have 0 length and have been
		 * allocated in this packed memory. They do not figure into the size of
		 * this memory, or its initial value, but upon calling the 'lock'
		 * method, they will be allocated an address above the last address
		 * allocated to any Allocation with accessed data.
		 */
		private LinkedHashSet<Allocation> zeroLengthAllocations = new LinkedHashSet<Allocation>();

		/**
		 * The locked flag is used to know when all Allocations have been
		 * allocated in this memory. Once that has happened, all the 0 length
		 * allocations can have their addresses allocated as well.
		 */
		private boolean locked = false;

		/** The unique number assigned to this memory in the design. */
		private int memID;

		/**
		 * The number of address bits necessary for all memories. Bits above
		 * this number may be used for memory ID.
		 */
		private int addrBits;

		/**
		 * Constructs a new PackedMemory capable of laying out Allocation
		 * objects into a physical (numerical) representation.
		 * 
		 * @param addrsPerLine
		 *            , the width of this memory in numbers of addressable
		 *            locations.
		 * @param addrBits
		 *            , the number of bits necessary to represent ANY address in
		 *            any memory of the design.
		 * @param memoryID
		 *            a unique non-zero non-negative value.
		 * @throws IllegalArgumentException
		 *             if memoryID <= 0.
		 */
		PackedMemory(int addrsPerLine, int addrBits, int memoryID) {
			// Allow zero for now, though when we acutally have real
			// memory ids we should change this to <= 0 since 0 will
			// be reserved for the null pointer.
			if (memoryID < 0) {
				throw new IllegalArgumentException(
						"Memory ID must be non-zero and non-negative.  Got: "
								+ memoryID);
			}

			this.addrsPerLine = addrsPerLine;
			memID = memoryID;
			this.addrBits = addrBits;
		}

		public void setSmallestAccess(int acc) {
			if (acc <= 0) {
				throw new IllegalArgumentException(
						"illegal minimum access width " + acc);
			}
			smallestAccessWidth = acc;
		}

		public int getSmallestAccess() {
			return smallestAccessWidth;
		}

		/**
		 * Assigns the {@link Allocation} to the next available address,
		 * skipping sufficient locations to ensure that the Allocation starts at
		 * the beginning of a line of memory (as defined by the memory width)
		 */
		public void allocate(Allocation loc) {
			assert !locked : "Physical memory has already been fully allocated.  New allocation not possible.";

			final int locAddresses = loc.getAddressableSize();
			if (locAddresses > 0) {
				if (_memory.db) {
					_memory.ln(_memory.BUILD, "Allocating to packed memory "
							+ loc + " locAddresses " + locAddresses);
				}
				baseAddressMap.put(loc, getAddress(nextAddress));
				nextAddress += locAddresses;

				if (addrsPerLine > 0) {
					// Pad it to the end of the line.
					final int modulo = locAddresses % addrsPerLine;
					final int extra = modulo == 0 ? 0 : addrsPerLine - modulo;

					if (_memory.db) {
						_memory.ln(_memory.BUILD, "\textra: " + extra);
					}

					assert extra >= 0;
					nextAddress += extra;

					if (_memory.db) {
						_memory.ln(_memory.BUILD, "\tnext: " + nextAddress);
					}
				}
			} else {
				zeroLengthAllocations.add(loc);
			}
		}

		/**
		 * Returns the number of addresses per line of memory. This is the
		 * number of addressable locations that must be accessed contiguously.
		 */
		public int getAddrsPerLine() {
			return addrsPerLine;
		}

		/**
		 * Returns the number of allocated addresses in this packed memory.
		 */
		public int getAddressCount() {
			return nextAddress;
		}

		/**
		 * This method must be called after ALL Allocations have been allocated
		 * in this memory in order to 'lock' the base address map. Until the
		 * base address map is locked, no address can be correctly obtained.
		 */
		public void lock() {
			locked = true;

			// Cache the address locally so that we dont modify it,
			// because it is used to determine the physical depth of
			// this memory.
			int addr = nextAddress;
			for (Allocation alloc : zeroLengthAllocations) {
				baseAddressMap.put(alloc, getAddress(addr++));
			}
		}

		/**
		 * Returns a Map of Allocation to Number, where the Number is the
		 * address of the Allocation key in memory. Note that the address has a
		 * memory id in the MSB bits
		 * 
		 * @return a 'Map' of Allocation : Number.
		 */
		public Map getBaseAddressMap() {
			assert locked : "Physical implementation of memory not yet fully populated.  Cannot generate a complete base address map.";
			return Collections.unmodifiableMap(baseAddressMap);
		}

		/**
		 * The first dimension of the array is the number of 'lines' in this
		 * memory, and the second dimesion is the value of the byte in that
		 * offset of a given line. This method operates by starting at location
		 * 0 and incrementing by the width of the memory. For each allocation
		 * found, we then translate that each 'line' that the Allocation
		 * occupies to an array of bytes. This method depends on the
		 * 'nextAddress' variable to be larger than any occupied location of
		 * this memory.
		 * 
		 * @return a value of type 'byte[][]'
		 */
		public AddressableUnit[][] getInitValues() {
			if (_memory.db) {
				_memory.ln(_memory.BUILD, "Building map of initial values");
			}
			// Create a map in which we can look up the allocation
			// based on the base address.
			final Map inverted = new HashMap();

			for (Iterator iter = getBaseAddressMap().entrySet().iterator(); iter
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) iter.next();
				inverted.put(entry.getValue(), entry.getKey());
			}

			final List<AddressableUnit[]> lines = new ArrayList<AddressableUnit[]>();
			for (int i = 0; i < nextAddress; i += addrsPerLine) {// Base
																	// addresses
																	// _will_
																	// fall @
																	// start of
																	// each
																	// line.
				final Object key = getAddress(i);
				if (!inverted.containsKey(key)) {
					continue;
				}
				final Allocation alloc = (Allocation) inverted.get(key);
				final LogicalValue value = alloc.getInitialValue();

				if (_memory.db) {
					_memory.ln(_memory.BUILD, "Alloc: " + alloc);
				}
				AddressableUnit[] values = value.getRep();
				printArr(values);
				final int valLen = values.length;
				int index = 0;
				while (index < valLen) {
					AddressableUnit[] line = new AddressableUnit[addrsPerLine];
					Arrays.fill(line, AddressableUnit.ZERO_UNIT);
					int remain = valLen - index;
					int copyLen = remain < addrsPerLine ? remain : addrsPerLine;
					if (_memory.db) {
						_memory.ln(_memory.BUILD, "Copying from values "
								+ valLen + " index " + index + " to line "
								+ line.length + " length " + copyLen);
					}
					System.arraycopy(values, index, line, 0, copyLen);
					lines.add(line);
					index += addrsPerLine;
				}
			}

			AddressableUnit[][] map = new AddressableUnit[lines.size()][addrsPerLine];
			for (int i = 0; i < lines.size(); i++) {
				AddressableUnit[] line = lines.get(i);
				map[i] = line;
			}
			return map;
		}

		/**
		 * Encode the given address with this memories memory ID.
		 * 
		 * @param addr
		 *            a value of type 'int'
		 * @return a value of type 'Integer'
		 */
		private Integer getAddress(int addr) {
			addr |= memID << addrBits;
			return new Integer(addr);
		}
	}

	/**
	 * Determines if the user has specified an exact value to use for the memory
	 * bank widths. If the user specifies a value, then we will use that value
	 * otherwise we return the argument value.
	 * 
	 * @param calculatedBankWidth
	 *            the int value, calculated as the necessary bank width.
	 * @param normalize
	 *            set to true if the value should be normalized, note that the
	 *            user specified value is always normalized.
	 * @return an int value, the value to use for memory bank widths.
	 */
	private static int bankWidthUserOverride(int calculatedBankWidth,
			boolean normalize) {
		GenericJob gj = EngineThread.getGenericJob();
		Option op = gj.getOption(OptionRegistry.MEMORY_BANK_WIDTH);
		final int targetBankWidth = ((OptionInt) op)
				.getValueAsInt(CodeLabel.UNSCOPED);
		int bankWidth;
		if (targetBankWidth < 0) {
			bankWidth = normalize ? normalize(calculatedBankWidth)
					: calculatedBankWidth;
		} else {
			// bankWidth = normalize(targetBankWidth);
			bankWidth = normalize ? normalize(targetBankWidth)
					: targetBankWidth;
		}

		if (bankWidth != targetBankWidth && targetBankWidth > 0) {
			gj.warn("Specified memory bank width is not a power of 8, "
					+ "using next higher power of 8.  Specified: "
					+ targetBankWidth + " Used: " + bankWidth);
		}

		// System.out.println("USING " + bankWidth + " BIT WIDE BANKS");
		return bankWidth;
	}

	/**
	 * Returns the power of 2 that is greater than or equal to the input value
	 * with a lower bound of 8.
	 * 
	 * @param value
	 *            a value of type 'int'
	 * @return a value of type 'int'
	 */
	private static int normalize(int value) {
		int normalized = 8;
		while (value > normalized) {
			normalized <<= 1;
		}
		return normalized;
	}

	public static void printArr(AddressableUnit[][] arr) {
		if (_memory.db) {
			for (int i = 0; i < arr.length; i++) {
				AddressableUnit[] b = arr[i];
				printArr(b);
			}
		}
	}

	public static void printArr(AddressableUnit[] arr) {
		if (_memory.db) {
			for (int i = 0; i < arr.length; i++) {
				// _memory.o(_memory.BUILD, "0x" + Integer.toHexString(arr[i]) +
				// " ");
				_memory.o(_memory.BUILD, "0x" + arr[i].getValue().toString(16)
						+ " ");
			}
			_memory.ln(_memory.BUILD);
		}
	}

} // MemoryBuilder
