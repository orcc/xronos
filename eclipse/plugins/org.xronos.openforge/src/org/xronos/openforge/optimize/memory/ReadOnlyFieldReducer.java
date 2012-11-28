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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


import org.eclipse.core.runtime.jobs.Job;
import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.FailVisitor;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.Register;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.LValue;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalMemoryPort;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.optimize.Optimization;

/**
 * ReadOnlyFieldReducer is responsible for converting any memory access (LValue)
 * whose target(s) is/are read only Location(s) to Constants in the LIM.
 * 
 * <p>
 * Created: Thu Oct 17 14:28:39 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ReadOnlyFieldReducer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ReadOnlyFieldReducer implements Optimization {

	/**
	 * A Map of Location to Set of LValues that access that Location.
	 */
	private Map<Location, Collection<LValue>> locationToLValues = new HashMap<Location, Collection<LValue>>();

	/**
	 * A Map of Location to Set of Locations that overlap that Location (as
	 * determined by Location.overlaps().
	 */
	private Map<Location, HashSet<Location>> locationOverlaps = new HashMap<Location, HashSet<Location>>();

	public ReadOnlyFieldReducer() {
	}

	/**
	 * Tests the given Location for being 'read only'. There are 2 criteria for
	 * being a read only field. 1. All accesses to this Location must be reads.
	 * 2. All accesses to any Location that overlaps this Location must also be
	 * reads.
	 * 
	 * <p>
	 * requires: non-null Location
	 * <p>
	 * modifies: none
	 * <p>
	 * effects: returns true if all accesses to the Location are read and all
	 * accesses to all overlapping Locations are reads.
	 * 
	 * @param loc
	 *            a 'Location'
	 * @return true if all accesses to the Location are read and all accesses to
	 *         all overlapping Locations are reads.
	 */
	protected boolean isReadOnly(Location loc) {
		boolean isReadOnly = true;

		for (LValue lv : locationToLValues.get(loc)) {
			if (lv.isWrite()) {
				isReadOnly = false;
				break;
			}
		}
		if (isReadOnly) {
			for (Location overlap : locationOverlaps.get(loc)) {
				for (LValue lv : locationToLValues.get(overlap)) {
					if (lv.isWrite()) {
						isReadOnly = false;
						break;
					}
				}
			}
		}
		return isReadOnly;
	}

	/**
	 * Tests whether the given location was found to have any overlaps in the
	 * most recently analyzed Design. (Mostly for testing purposes)
	 * 
	 * @param loc
	 *            the location to test
	 * @return true if any overlapping Locations were found, false otherwise
	 */
	boolean hasOverlap(Location loc) {
		Collection<Location> overlaps = locationOverlaps.get(loc);
		return ((overlaps != null) && (overlaps.size() > 0));
	}

	/**
	 * The MemoryAnalyzer populates the various analysis maps in preparation for
	 * field reduction.
	 * 
	 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas
	 *         Kollegger</a>
	 * @version $Id: ReadOnlyFieldReducer.java 2 2005-06-09 20:00:48Z imiller $
	 */
	private class MemoryAnalyzer extends DefaultVisitor {
		/** Indicates whether the MemoryAnalyzer found a Design to analyze. */
		private boolean foundDesign = false;

		/**
		 * Reviews all the memories in a design. For each memory, retrieves all
		 * the LValues, then locations accessed by the LValue, building the
		 * locationToLValue mapping. Then iterate over all Locations which have
		 * any access, and find overlapping Locations.
		 * 
		 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Design)
		 */
		@Override
		public void visit(Design design) {
			clear();

			for (LogicalMemory memory : design.getLogicalMemories()) {
				for (LValue lv : memory.getLValues()) {
					for (Location loc : memory.getAccesses(lv)) {
						Collection<LValue> lvSet = locationToLValues.get(loc);
						if (lvSet == null) {
							lvSet = new HashSet<LValue>();
							locationToLValues.put(loc, lvSet);
						}
						lvSet.add(lv);
					}
				}
			}
			for (Location loc : locationToLValues.keySet()) {
				HashSet<Location> overlaps = new HashSet<Location>();
				for (Location otherLoc : locationToLValues.keySet()) {
					if ((otherLoc != loc) && (otherLoc.overlaps(loc))) {
						overlaps.add(otherLoc);
					}
				}
				locationOverlaps.put(loc, overlaps);
			}
			foundDesign = true; // sanity check that the MemoryAnalyzer did
								// something
		}

		/**
		 * Indicates whether the MemoryAnalyzer visited a design.
		 * 
		 * @return true if a Design was visited and analyzed.
		 */
		public boolean foundDesign() {
			return foundDesign;
		}
	}

	/**
	 * Overrides the read LValue visit methods to determine if those accesses
	 * are to read-only Locations. If the target Locations are read-only and all
	 * are the same Constant value then the access is replaced with a Constant
	 * of that value. Makes use of the isReadOnly method to determine if target
	 * Location(s) is/are read only.
	 */
	private class FieldRemover extends DefaultVisitor {

		final HashMap<Component, Constant> readToConstant = new HashMap<Component, Constant>();

		/**
		 * Creates a new FieldRemover, ready for action.
		 */
		public FieldRemover() {
		}

		/**
		 * Visits a visitable, identifying all read-only accesses, and then
		 * replacing them with the related Constants.
		 * 
		 * @param v
		 *            the visitable from which to remove read-only accesses
		 */
		public void removeFrom(Visitable v) {
			// visiting populates the readToConstant mapping
			// with removable read-only accesses and the related constant
			v.accept(this);

			final org.xronos.openforge.optimize.ComponentSwapVisitor swapper = new org.xronos.openforge.optimize.ComponentSwapVisitor();

			RemoveVisitor removeVisitor = new RemoveVisitor();
			for (Map.Entry<Component, Constant> pair : readToConstant
					.entrySet()) {
				Component lv = pair.getKey();
				swapper.replaceComponent(lv, pair.getValue());
				lv.accept(removeVisitor);
			}
		}

		private class RemoveVisitor extends FailVisitor implements Visitor {
			public RemoveVisitor() {
				super("Removal of read only field accesses");
			}

			@Override
			public void visit(RegisterRead comp) {
				comp.removeFromResource();
			}

			@Override
			public void visit(HeapRead comp) {
				comp.getLogicalMemoryPort().removeAccess(comp);
			}

			@Override
			public void visit(AbsoluteMemoryRead comp) {
				comp.getLogicalMemoryPort().removeAccess(comp);
			}
		}

		/**
		 * Evaluate and replace HeapRead LValues if all target Location(s) are
		 * read only and the same constant value.
		 * 
		 * <p>
		 * requires: none
		 * <p>
		 * modifies: LIM, target LogicalMemory
		 * <p>
		 * effects: none
		 * 
		 * @param read
		 *            an HeapRead LValue
		 */
		@Override
		public void visit(HeapRead read) {
			LogicalMemoryPort port = read.getLogicalMemoryPort();
			boolean isUniqueReadOnly = true;
			Constant commonConstant = null;

			for (Location loc : port.getLogicalMemory().getAccesses(read)) {
				if (isReadOnly(loc)) {
					// Constant c = loc.getInitialValue().toConstant();
					Constant c = null;
					try {
						c = loc.getInitialValue().toConstant();
					} catch (Location.IllegalInitialValueContextException e) {
						// Can not determine a constant for the
						// Location, so simply return and dont
						// propagate the access.
						return;
					}
					if (commonConstant == null) {
						commonConstant = c;
					} else if (!commonConstant.isSameValue(c)) {
						isUniqueReadOnly = false;
						break;
					}
				} else {
					isUniqueReadOnly = false;
					break;
				}
			}

			if (isUniqueReadOnly && (commonConstant != null)) {
				readToConstant.put(read, commonConstant);
			}
		}

		/**
		 * Evaluate and replace RegisterRead access if the target Register is
		 * read only.
		 * 
		 * <p>
		 * requires: none
		 * <p>
		 * modifies: LIM, target Register
		 * <p>
		 * effects: none
		 * 
		 * @param read
		 *            a non-null RegisterRead
		 */
		@Override
		public void visit(RegisterRead regRead) {
			Register targetReg = (Register) regRead.getResource();
			if (targetReg.getWriteAccesses().size() == 0) {
				LogicalValue initValue = targetReg.getInitialValue();
				Constant c = initValue.toConstant();
				readToConstant.put(regRead, c);
			}
		}

		/**
		 * Does nothing, array reads with constant index are converted by half
		 * constant to heap reads.
		 * 
		 * @param read
		 *            an ArrayRead LValue
		 */
		@Override
		public void visit(ArrayRead read) {
			// Do nothing here. This is possible because half
			// constant propagation will replace all ArrayRead
			// accesses that have a constant index port with the
			// equivalent HeapRead access. Due to the fact that this
			// optimization can only apply to the array accesses with
			// constant index, we can thus safely ignore array reads
			// here.
			super.visit(read);
		}

		/**
		 * Evaluate and replace AbsolutMemoryRead LValues if the target Location
		 * is a read only location.
		 * 
		 * <p>
		 * requires: none
		 * <p>
		 * modifies: LIM, target LogicalMemory
		 * <p>
		 * effects: none
		 * 
		 * @param read
		 *            an AbsoluteMemoryRead LValue
		 */
		@Override
		public void visit(AbsoluteMemoryRead read) {
			LogicalMemoryPort port = read.getLogicalMemoryPort();
			Collection<Location> locations = port.getLogicalMemory()
					.getAccesses(read);

			assert (locations.size() == 1) : "AbsoluteMemoryRead should only access a sinlge location.";
			Location loc = locations.iterator().next();

			if (isReadOnly(loc)) {
				Constant c = null;
				try {
					c = loc.getInitialValue().toConstant();
				} catch (Location.IllegalInitialValueContextException e) {
					// Can not determine a constant for the
					// Location, so simply return and dont
					// propagate the access.
					return;
				}
				readToConstant.put(read, c);
			}
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
	 * @throws
	 */
	@Override
	public void run(Visitable target) {
		if (!(target instanceof Design))
			return;

		// ObjectResolver resolver =
		ObjectResolver.resolve((Design) target);

		final MemoryAnalyzer analyzer = new MemoryAnalyzer();
		target.accept(analyzer);
		assert analyzer.foundDesign() : "optimization only operates on a Design.";
		final FieldRemover fieldRemover = new FieldRemover();
		fieldRemover.removeFrom(target);
	}

	/**
	 * Sets the object resolve map to null. It will be rebuilt as soon as the
	 * next 'component' is traversed.
	 */
	@Override
	public void clear() {
		locationOverlaps.clear();
		locationToLValues.clear();
	}

	/**
	 * returns true if the current pass of this optimization modified the LIM.
	 */
	@Override
	public boolean didModify() {
		// TBD
		return false;
	}

	/**
	 * Reports, via {@link Job#info}, what optimization is being performed
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info(
				"optimizing read-only memory locations...");
	}

	/**
	 * Reports, via {@link Job#verbose}, the results of <b>this</b> pass of the
	 * optimization.
	 */
	@Override
	public void postStatus() {
		// Job.verbose("reduced " + getReplacedNodeCount() + " expressions");
	}

}// ReadOnlyFieldReducer
