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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


import org.eclipse.core.runtime.jobs.Job;
import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.EmptyVisitor;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.FailVisitor;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.OffsetMemoryAccess;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.Allocation;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.lim.memory.NonRemovableRangeException;
import org.xronos.openforge.lim.memory.Pointer;
import org.xronos.openforge.lim.memory.Variable;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.SimpleConstant;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.optimize.ComponentSwapVisitor;
import org.xronos.openforge.optimize.Optimization;
import org.xronos.openforge.util.naming.ID;

/**
 * MemoryReducer eliminates bytes from the LogicalMemory that are non-accessed
 * and not needed to preserve alignment. Modifies all Locations, MemoryAccesses
 * (LValues), and LogicalValues such that no OffsetLocation can be created such
 * that there is no possible MemoryAccess(LValue) to that Location. Some pad
 * bytes may need to remain to preserve data alignment and spacing for
 * indeterminate offset accesses.
 * 
 * <p>
 * Created: Fri Oct 18 11:31:06 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryReducer.java 107 2006-02-23 15:46:07Z imiller $
 */
public class MemoryReducer implements Optimization {

	/**
	 * Keeps track of the number of bytes removed from any memory
	 */
	private int removedByteCount = 0;

	private boolean isModified = false;

	/**
	 * Eliminates holes/padding in the LogicalMemory by eliminating those bytes
	 * from the underlying LogicalValue(s). This is accomplished by following 2
	 * basic rules: 1. All Locations in the memory must be moved by the same
	 * amount (this guarantees that offset accesses to multiple locations still
	 * work). 2. Locations must be moved by an integral multiple of their size.
	 * 
	 * @param mem
	 *            a value of type 'LogicalMemory'
	 * @return a DeltaMap of old base Location to new base Location which also
	 *         indicates the amount that the new location was moved by.
	 * @throws NonRemovableRangeException
	 *             if the LogicalValue upon which a BaseLocationMap is based
	 *             cannot be modified by delta.
	 */
	DeltaMap reduce(LogicalMemory mem, ObjectResolver resolver)
			throws NonRemovableRangeException {
		Set<BaseLocationMap> baseLocationMaps = generateBaseLocationMaps(mem);
		int delta = findDelta(baseLocationMaps);
		int trimmable = findTrimmable(baseLocationMaps);

		DeltaMap oldBaseToNewBaseMap = new DeltaMap(delta);
		// Do nothing if the memory cannot be reduced.
		if (delta == 0 && trimmable == 0) {
			// Return an empty map
			return oldBaseToNewBaseMap;
		}

		// Each base location map is 'moved' by that delta. This is a
		// non-destructive process. If any of the maps throws a
		// NonRemovableRangeException, then the moving has failed and we
		// are done with this memory (no modification has been made).
		// Otherwise, the collection of LogicalValues returned from
		// the move(int) method becomes the new Allocations for the
		// memory. Once allocated, the mapping of old->new
		// Allocations is passed to the ReductionRetargetVisitor. Once
		// the memory accesses and locations have been retargetted,
		// remove original allocations.

		Map<LogicalValue, Location> newLogicalValueToBaseLocationMap = new HashMap<LogicalValue, Location>();
		for (BaseLocationMap blm : baseLocationMaps) {
			newLogicalValueToBaseLocationMap.put(blm.move(delta),
					blm.getAbsoluteBase());
		}
		// If we make it this far, all the moves have/will succeed so
		// we can assume we will remove 'delta' bytes.
		removedByteCount += delta;
		removedByteCount += trimmable;

		// Allocate the new space in the memory.
		for (LogicalValue newValue : newLogicalValueToBaseLocationMap.keySet()) {
			Allocation oldAlloc = (Allocation) newLogicalValueToBaseLocationMap
					.get(newValue);
			Allocation newAlloc = mem.allocate(newValue);
			ID.copy(oldAlloc, newAlloc);
			newAlloc.copyBlockElements(oldAlloc);
			oldBaseToNewBaseMap.put(oldAlloc, newAlloc);
		}

		// Retarget the LocationConstants and LValues associated with
		// this memory. This is necessary because the reduction
		// process is accomplished by using new Allocations with
		// different structures and reflected in the offset
		// and Locations of the LValues. There must be ONE unique
		// instance of this visitor for each memory because the
		// instance protects against duplicate modification of
		// Locations.
		ReductionRetargetVisitor vis = new ReductionRetargetVisitor(
				oldBaseToNewBaseMap, delta, resolver);
		// Use a new list to group LValues and LocationConstants and
		// protect against concurrent modification
		List retargettable = new ArrayList();
		retargettable.addAll(mem.getLocationConstants());
		retargettable.addAll(mem.getLValues());
		for (Iterator accessIter = retargettable.iterator(); accessIter
				.hasNext();) {
			((Visitable) accessIter.next()).accept(vis);
		}

		// Fix all the Pointers whose target Location resides in the
		// modified memory.
		for (Iterator<Pointer> iter = (new ArrayList<Pointer>(
				mem.getAccessingPointers())).iterator(); iter.hasNext();) {
			fixPointer(iter.next(), oldBaseToNewBaseMap);
		}

		// Remove original allocation
		for (Iterator oldBaseIter = oldBaseToNewBaseMap.keySet().iterator(); oldBaseIter
				.hasNext();) {
			Allocation oldBase = (Allocation) oldBaseIter.next();
			if (mem.getAllocations().contains(oldBase)) {
				mem.delete(oldBase);
			}
		}

		return oldBaseToNewBaseMap;
	}

	/**
	 * This method takes the Set of {@link BaseLocationMap} objects created by
	 * {@link BaseLocationMap#buildMaps} and removes from it any of them that
	 * represent non-accessed Allocations as determined by the maxSize method of
	 * the BaseLocationMap. This prevents us from applying the rules for
	 * downsizing a memory to any Allocation that is not used (except perhaps by
	 * an address of operation).
	 * 
	 * @param mem
	 *            a non-null LogicalMemory
	 * @return a Set of BaseLocationMap
	 */
	private Set<BaseLocationMap> generateBaseLocationMaps(LogicalMemory mem) {
		Set<BaseLocationMap> maps = BaseLocationMap.buildMaps(mem);
		ArrayList<BaseLocationMap> copyMaps = new ArrayList<BaseLocationMap>(
				maps);
		for (BaseLocationMap map : copyMaps) {
			if (map.maxSize() == 0) {
				maps.remove(map);
			}
		}
		return maps;
	}

	/**
	 * Once all Locations have been stored in their BaseLocationMap, each map is
	 * queried for how much it is movable by (isMovableBy). The SMALLEST of
	 * these values is then used to be the delta magnitude IFF it is an integral
	 * multiple of the maxSize() of each BaseLocationMap.
	 * 
	 * @param baseLocationMaps
	 *            a list of BaseLocationMap
	 * @return the delta value to be moved
	 */
	private int findDelta(Collection<BaseLocationMap> baseLocationMaps) {
		if (baseLocationMaps.isEmpty()) {
			return 0;
		}

		int maxLocationSize = 0;
		// find the max size of all BaseLocationMap
		for (BaseLocationMap blm : baseLocationMaps) {
			maxLocationSize = Math.max(maxLocationSize, blm.maxSize());
		}

		// determine the number of movable bytes(i.e. delta)
		int minMovableByteCount = baseLocationMaps.iterator().next()
				.isMovableBy();
		for (BaseLocationMap blm : baseLocationMaps) {
			minMovableByteCount = Math.min(minMovableByteCount,
					blm.isMovableBy());
			if ((minMovableByteCount % maxLocationSize) != 0) {
				return 0;
			}
		}

		return minMovableByteCount;
	}

	/**
	 * Returns the maximum number of trimmable bytes on any base location map
	 * associated with this memory.
	 * 
	 * @param baseLocationMaps
	 *            a non-null Collection of BaseLocationMap objects.
	 * @return a non-negative int
	 */
	private int findTrimmable(Collection<BaseLocationMap> baseLocationMaps) {
		if (baseLocationMaps.isEmpty()) {
			return 0;
		}

		int maxTrimmable = 0;
		for (BaseLocationMap blm : baseLocationMaps) {
			maxTrimmable = Math.max(maxTrimmable, blm.trimmableBytes());
		}
		return maxTrimmable;
	}

	public void fixPointer(Pointer ptr, DeltaMap correlations) {
		Location target = ptr.getTarget();
		// Try modifying the target location based on every moved
		// Allocation. The chopStart method will take care of
		// knowing when to modify the Location and when not to.
		if (!correlations.containsValue(ptr.getTarget())) {
			for (Iterator baseIter = correlations.keySet().iterator(); baseIter
					.hasNext();) {
				Location base = (Location) baseIter.next();
				target.chopStart(base, correlations.getDelta());
			}
		}
		// target location has modified offset now (if needed) but
		// now correlate it over to the new allocation.
		Location newTarget = (Location) correlations.get(target);
		if (newTarget != null) {
			ptr.setTarget(newTarget);
		}
	}

	/**
	 * This visitor is used to process LocationConstants and LValues associated
	 * with a single memory. This visitor takes the correlation map (old to new
	 * Allocation) delta and the ObjectResolver to modify the internal structure
	 * of all LValues. Absolute Lvalues and LocationConstants are modified by
	 * changing the Location that they target. Heap Lvalues are modified by
	 * updating their offsets and rebuilding adders/subtractors along each data
	 * path from Heap LValue's absolute base to heap LValue's base address port.
	 * The rebuilding process has the advantage of reflecting the offset
	 * embadded in each heap access on the LIM graph. It makes a clear picture
	 * of seeing the reduced memory structure and is extremely helpful on
	 * debugging because it correlates the LIM graph to the complicated process
	 * of figuring out the bases and locations of memories by
	 * {@link ObjectResolver}. Array Lvalues are not modified at all(they are
	 * unexpected). LocationConstants are modified by calling chopStart with
	 * every modified Allocation from the memory. This causes the
	 * LocationConstant's target location to modify itself internally by
	 * modifying its offset as needed. The modified Location is re-set as the
	 * target of LocationConstants(which Absolute accesses refer to). This
	 * visitor also protects against a Location which LocationConstant is
	 * referring to being modified more than one time.
	 */
	private class ReductionRetargetVisitor extends FailVisitor {
		/** ObjectResolver */
		private ObjectResolver resolver;

		/** Map of old Allocation to new Allocation */
		private Map<Location, Location> old2NewAlloc;

		/** The amount by which the memory was moved */
		private int delta;

		/**
		 * A set of LocationConstants. Keeps us from modifying one more than
		 * once.
		 */
		private Set<LocationConstant> processed;

		/**
		 * The visitor to rebuild adders/subtractors along the data path from
		 * each LocationConstant to its heap access
		 */
		private Swaper swaper;

		public ReductionRetargetVisitor(Map allocMap, int delta,
				ObjectResolver resolver) {
			super("Memory reduction location retarget");
			this.resolver = resolver;
			old2NewAlloc = allocMap;
			this.delta = delta;
			processed = new HashSet();
			swaper = new Swaper(this.delta);
		}

		// We should not see array accesses in a moved memory
		@Override
		public void visit(ArrayRead comp) {
			super.visit(comp);
		}

		@Override
		public void visit(ArrayWrite comp) {
			super.visit(comp);
		}

		// For absolute accesses, simply modify to the contained
		// LocationConstant.
		@Override
		public void visit(AbsoluteMemoryRead comp) {
			comp.getAddressConstant().accept(this);
		}

		@Override
		public void visit(AbsoluteMemoryWrite comp) {
			comp.getAddressConstant().accept(this);
		}

		// For heap accesses, use the delta to adjust and rebuild
		// adders/subtractors along the data path from this heap
		// access to its absolute base, also modified the offset of
		// each heap access if necessary. Since those
		// adders/subtractors has been properly adjusted/rebuilt, each heap
		// access's new offset which reflects the new location of
		// reduced memory will always be greater than or equals to ZERO.
		@Override
		public void visit(HeapRead comp) {
			if (delta != 0) {
				Set sources = resolver.getAddressSources(comp);
				for (Iterator sourceIter = sources.iterator(); sourceIter
						.hasNext();) {
					LocationConstant locConst = (LocationConstant) sourceIter
							.next();
					swaper.resetDelta(delta);
					adjustOffsetConstantOnDataPath(locConst, comp, swaper);
				}

				int oldOffset = comp.getOffset();
				int newOffset = Math.max(0, (oldOffset - swaper.getDelta()));
				if (oldOffset != newOffset) {
					comp.setOffset(newOffset);
					isModified = true;
				}
			}
		}

		@Override
		public void visit(HeapWrite comp) {
			if (delta != 0) {
				Set sources = resolver.getAddressSources(comp);
				for (Iterator sourceIter = sources.iterator(); sourceIter
						.hasNext();) {
					LocationConstant locConst = (LocationConstant) sourceIter
							.next();
					swaper.resetDelta(delta);
					adjustOffsetConstantOnDataPath(locConst, comp, swaper);
				}

				int oldOffset = comp.getOffset();
				int newOffset = Math.max(0, (oldOffset - swaper.getDelta()));
				if (oldOffset != newOffset) {
					comp.setOffset(newOffset);
					isModified = true;
				}
			}
		}

		// For LocationConstants, the original target location needs
		// to be shifted and corelated to the new target
		// location. The processed LocationConstants are stored in a set
		// to ensure none of them is taken care of more than once.
		@Override
		public void visit(LocationConstant comp) {
			Location target = comp.getTarget();
			if (!old2NewAlloc.keySet().contains(target.getAbsoluteBase())) {
				if (target.getAbsoluteBase().getAddressableSize() != 0) {
					throw new IllegalArgumentException(
							"A non zero length allocation cannot be correlated after size reduction");
				}
				return;
			}
			if (!processed.contains(comp)) {
				int removableDelta = Math.min(target.getAbsoluteMinDelta(),
						delta);
				Location newLoc = shiftAndCorrelate(target, removableDelta);
				comp.setTarget(newLoc);
				processed.add(comp);
			}
		}

		/**
		 * To adjust offset constant and rebuild the adder/subtractor as
		 * necessary along the data flow path from a specified LocationConstant
		 * to a specified heap access.
		 * 
		 * @param LocConst
		 *            a LocationConst
		 * @param heapAccess
		 *            a HeaapRead or HeapWrite
		 */
		private void adjustOffsetConstantOnDataPath(LocationConstant locConst,
				OffsetMemoryAccess heapAccess, Swaper swaper) {
			// Search the components in the dath flow path which
			// drive the base address port of a heap access and get
			// the result.
			Port addressPort = heapAccess.getBaseAddressPort();
			DataPathSearcher pathFinder = new DataPathSearcher(locConst,
					heapAccess, addressPort);
			List dataPathComponents = pathFinder.getDataPathComponents();

			// adjust the offset constant and rebuild the
			// adders/subtractors as necessary.
			for (Iterator compIter = dataPathComponents.iterator(); compIter
					.hasNext();) {
				((Component) compIter.next()).accept(swaper);
			}
		}

		/**
		 * Returns a Location that has been shifted by delta (as necessary) and
		 * then re-targetted by changing its base hierarchy.
		 * 
		 * @param loc
		 *            a value of type 'Location'
		 * @return a value of type 'Location'
		 */
		private Location shiftAndCorrelate(Location loc, int delta) {
			// First, inform the Locaiton that the underlying
			// allocation may have been moved. All Allocations
			// potentially apply to this location.
			for (Iterator it = old2NewAlloc.keySet().iterator(); it.hasNext();) {
				loc.chopStart((Location) it.next(), delta);
			}
			// Now correlate it back to the new allocation.
			return Variable.getCorrelatedLocation(old2NewAlloc, loc);
		}
	}

	/**
	 * This simple visitor is used to find all the Pointers contained in memory
	 * and update the initial value that they contain according to the Set of
	 * 'DeltaMaps' that were generated during the moving process. We cant use
	 * the {@link PointerRetargetVis} because the correlated Location must be
	 * shifted by the delta.
	 */
	// private class PointerRetarget extends DefaultMemoryVisitor
	// {
	// /** A Set of DeltaMap objects. The maps contains old Location
	// * to new Location correlations as well as storing the amount
	// * that each new Location was moved by (compared to the old
	// * Location) */
	// Set correlations;

	// PointerRetarget (Set correlate)
	// {
	// this.correlations = correlate;
	// }

	// public void visit (Pointer ptr)
	// {
	// Location target = ptr.getTarget();
	// // Try modifying the target location based on every moved
	// // Allocation. The chopStart method will take care of
	// // knowing when to modify the Location and when not to.
	// for (Iterator iter = this.correlations.iterator(); iter.hasNext();)
	// {
	// DeltaMap map = (DeltaMap)iter.next();
	// if (!map.containsValue(ptr.getTarget()))
	// {
	// for (Iterator baseIter = map.keySet().iterator(); baseIter.hasNext();)
	// {
	// Location base = (Location)baseIter.next();
	// target.chopStart(base, map.getDelta());
	// }
	// }
	// }
	// // target location has modified offset now (if needed) but
	// // now correlate it over to the new allocation.
	// for (Iterator iter = this.correlations.iterator(); iter.hasNext();)
	// {
	// DeltaMap map = (DeltaMap)iter.next();
	// if (!map.containsValue(ptr.getTarget()) || !map.containsValue(ptr))
	// {
	// Location newTarget = (Location)map.get(target);
	// if (newTarget != null)
	// {
	// ptr.setTarget(newTarget);
	// break;
	// }
	// }
	// }
	// }
	// }

	/**
	 * Simple extension of HashMap to include the 'delta' relationship between
	 * the old and new locations.
	 */
	private class DeltaMap extends HashMap {
		private int delta;

		DeltaMap(int delta) {
			super();
			this.delta = delta;
		}

		public int getDelta() {
			return delta;
		}
	}

	/**
	 * A utility visitor class to serach components and build a ordered list
	 * along the data flow path by specifying the origin {@link component} and
	 * the desitination {@link Component}. There is also a option of seach the
	 * data path ending with a specific data port on the destination
	 * {@link Component}.
	 */
	private class DataPathSearcher extends EmptyVisitor {
		/** The storage of keeping nodes during the searching process */
		private Stack stack;

		/** The list of ordered data flow path components */
		private LinkedList components;

		/* The origin component */
		private Component origin;

		/** The destination component */
		private Component destination;

		/** The specified data port on the destination component */
		private Port destinationPort;

		/**
		 * A boolean flag of checking whether the data path has been found or
		 * not
		 */
		private boolean foundDataPath = false;

		DataPathSearcher(Component fromComponent, Component toComponent) {
			this(fromComponent, toComponent, null);
		}

		DataPathSearcher(Component fromComponent, Component toComponent,
				Port port) {
			stack = new Stack();
			components = new LinkedList();
			origin = fromComponent;
			destination = toComponent;
			destinationPort = port;
		}

		/**
		 * Search the LIM graph and return a list of components along the
		 * designated search path.
		 * 
		 * @return a list of compoennts in order.
		 */
		public LinkedList getDataPathComponents() {
			stack.push(origin);
			searchPath();
			return components;
		}

		/**
		 * Recursively searching the nodes by applying the depth-first tree
		 * searching technique.
		 */
		private void searchPath() {
			if (foundDataPath) {
				return;
			}

			Component comp = (Component) stack.peek();
			for (Iterator dataBusIter = comp.getDataBuses().iterator(); dataBusIter
					.hasNext();) {
				Collection depComps = Component
						.getDependentComponents((Bus) dataBusIter.next());
				for (Iterator depCompIter = depComps.iterator(); depCompIter
						.hasNext();) {
					Component depComp = (Component) depCompIter.next();
					if (!foundDataPath) {
						stack.push(depComp);
						depComp.accept(this);

						// the destination node is hit
						if (!foundDataPath && (stack.peek() == destination)) {
							// If the destination data port is
							// specified, the componet on the stack
							// must be the one who drives the
							// destination data port. Otherwise, the
							// searching continues.
							if (destinationPort != null) {

								Component endPathComponent = (Component) stack
										.pop();
								for (Iterator entryIter = endPathComponent
										.getEntries().iterator(); entryIter
										.hasNext();) {
									Entry entry = (Entry) entryIter.next();
									for (Iterator depIter = entry
											.getDependencies(destinationPort)
											.iterator(); depIter.hasNext();) {
										Dependency dep = (Dependency) depIter
												.next();
										if (dep.getLogicalBus().getOwner()
												.getOwner() == stack.peek()) {
											foundDataPath = true;
											stack.push(endPathComponent);
										}
									}
								}
							}
							// Without specifying the destination data
							// port, the seaching is completed.
							else {
								foundDataPath = true;
							}

							if (foundDataPath) {
								// Once the data flow path has been
								// identified, copy the components in the
								// stack to build the list of components
								// in data connection order.
								while (!stack.isEmpty()) {
									components.addFirst(stack.pop());
								}
							}
						} else {
							if (!foundDataPath && (stack.peek() != destination)) {
								searchPath();
							}

							if (!stack.isEmpty()) {
								// finsh searching the node on top of
								// the stack and pop it out from the
								// stack.
								stack.pop();
							}
						}
					}
				}
			}
		}

		@Override
		public void visit(Call call) {
			call.getProcedure().accept(this);
		}

		@Override
		public void visit(Procedure proc) {
			stack.push(proc.getBody());
			proc.getBody().accept(this);
		}

		@Override
		public void visit(Block block) {
			block.getInBuf().accept(this);
		}

		@Override
		public void visit(InBuf inbuf) {
			stack.push(inbuf);
		}

		@Override
		public void visit(Loop loop) {
			loop.getInBuf().accept(this);
		}

		@Override
		public void visit(ForBody forBody) {
			forBody.getInBuf().accept(this);
		}

		@Override
		public void visit(WhileBody whileBody) {
			whileBody.getInBuf().accept(this);
		}

		@Override
		public void visit(UntilBody untilBody) {
			untilBody.getInBuf().accept(this);
		}

		@Override
		public void visit(Branch branch) {
			branch.getInBuf().accept(this);
		}

		@Override
		public void visit(Decision decision) {
			decision.getInBuf().accept(this);
		}

		@Override
		public void visit(OutBuf outbuf) {
			if (origin.getCommonAncestor(destination) != outbuf.getOwner()) {
				stack.push(outbuf.getOwner());
			}
		}
	}

	/**
	 * A visitor to adjust constants and rebuild adders/subtractors
	 */
	private class Swaper extends EmptyVisitor {
		/**
		 * To keep track of the offset being adjusted on each adder/subtractor
		 */
		private int delta = 0;

		/**
		 * A map of a rebuilt adder/subtractor to its original offset constant
		 */
		private Map processedMap;

		/**
		 * A ComponentSwapVistor to replace the original adder/subtractor with
		 * the rebuilt one
		 */
		private ComponentSwapVisitor swapVisitor;

		Swaper(int delta) {
			this.delta = delta;
			processedMap = new HashMap();
			swapVisitor = new ComponentSwapVisitor();
		}

		@Override
		public void visit(AddOp addOp) {
			Dependency dataDep = addOp.getMainEntry()
					.getDependencies(addOp.getRightDataPort()).iterator()
					.next();
			Bus logicalBus = dataDep.getLogicalBus();
			int origAddrOffset = 0;
			if (!processedMap.keySet().contains(addOp)) {
				origAddrOffset = (int) logicalBus.getValue().getValueMask();
			} else {
				origAddrOffset = (int) ((Constant) processedMap.get(addOp))
						.getValueBus().getValue().getValueMask();
			}
			int newAddrOffset = Math.max(0, origAddrOffset - delta);
			delta = Math.max(0, delta - origAddrOffset);
			if ((origAddrOffset != newAddrOffset)
					&& (!processedMap.keySet().contains(addOp))) {
				Component origConst = logicalBus.getOwner().getOwner();
				Constant newConst = new SimpleConstant(newAddrOffset,
						logicalBus.getSize(), true);
				swapVisitor.swapComponents(origConst, newConst);
				AddOp replacement = new AddOp();
				swapVisitor.swapComponents(addOp, replacement);
				processedMap.put(replacement, origConst);
			}
		}

		@Override
		public void visit(SubtractOp subOp) {
			Dependency dataDep = subOp.getMainEntry()
					.getDependencies(subOp.getRightDataPort()).iterator()
					.next();
			Bus logicalBus = dataDep.getLogicalBus();
			int origAddrOffset = 0;
			if (!processedMap.keySet().contains(subOp)) {
				origAddrOffset = (int) logicalBus.getValue().getValueMask();
			} else {
				origAddrOffset = (int) ((Constant) processedMap.get(subOp))
						.getValueBus().getValue().getValueMask();
			}
			long newAddrOffset = Math.max(0, origAddrOffset - delta);
			delta = Math.max(0, delta - origAddrOffset);
			if ((origAddrOffset != newAddrOffset)
					&& (!processedMap.keySet().contains(subOp))) {
				Component origConst = logicalBus.getOwner().getOwner();
				Constant newConst = new SimpleConstant(newAddrOffset,
						logicalBus.getSize(), true);
				swapVisitor.swapComponents(origConst, newConst);
				SubtractOp replacement = new SubtractOp();
				swapVisitor.swapComponents(subOp, replacement);
				processedMap.put(replacement, origConst);
			}
		}

		public int getDelta() {
			return delta;
		}

		public void resetDelta(int delta) {
			this.delta = delta;
		}
	}

	//
	// Optimization interface.
	//
	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		if (!(target instanceof Design)) {
			return;
		}

		Design design = (Design) target;

		// Only applicable to LogicalMemories.
		final ObjectResolver resolver = ObjectResolver.resolve(design);

		Set<DeltaMap> baseMaps = new HashSet<DeltaMap>();
		for (LogicalMemory mem : design.getLogicalMemories()) {
			if (!mem.getLValues().isEmpty()) {
				try {
					DeltaMap correlation = reduce(mem, resolver);

					if (correlation.size() > 0) {
						baseMaps.add(correlation);
					}
				} catch (NonRemovableRangeException e) {
					EngineThread
							.getGenericJob()
							.warn("Could not reduce memory "
									+ mem
									+ " due to internal analysis error. Memory will not be reduced.");
				}
			}
		}

		/*
		 * Now that all memories have been reduced, we need to fix any Pointers
		 * that are stored in ANY memory. This is because the Pointer may be
		 * initialized to a Location that has been 'redefined' by the reduction
		 * process. This visitor uses the correlation data generated when each
		 * memory was moved to figure out the new target for the Pointer.
		 */
		// if (baseMaps.size() > 0)
		// {
		// PointerRetarget retarget = new PointerRetarget(baseMaps);
		// for (Iterator iter = design.getLogicalMemories().iterator();
		// iter.hasNext();)
		// {
		// LogicalMemory mem = (LogicalMemory)iter.next();
		// for (Iterator locConstIter = mem.getLocationConstants().iterator();
		// locConstIter.hasNext();)
		// {
		// ((LocationConstant)locConstIter.next()).getTarget().getInitialValue().accept(retarget);
		// }
		// }
		// }
	}

	/**
	 * Returns true if any memory access was modified as a result of changing
	 * the size of an allocation in any memory.
	 */
	@Override
	public boolean didModify() {
		return isModified;
	}

	/**
	 * Clears the count of removed locations
	 */
	@Override
	public void clear() {
		removedByteCount = 0;
		isModified = false;
	}

	/**
	 * Reports, via {@link Job#info}, what optimization is being performed
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info(
				"eliminating unused memory locations...");
	}

	/**
	 * Reports, via {@link Job#verbose}, the results of <b>this</b> pass of the
	 * optimization.
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().verbose(
				"removed " + removedByteCount + " memory locations");
	}

}// MemoryReducer
