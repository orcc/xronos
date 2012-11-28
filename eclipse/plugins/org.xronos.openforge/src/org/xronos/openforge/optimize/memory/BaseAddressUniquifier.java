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
import java.util.List;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.OffsetMemoryAccess;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.memory.LValue;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.optimize.ComponentSwapVisitor;
import org.xronos.openforge.optimize.Optimization;


/**
 * BaseAddressUniquifier is used to find all ({@link LValue}) memory accesses
 * which are {@link OffsetMemoryAccess} and which are based off of a single
 * fixed Location and replaces the expression leading up to the base address
 * port with a deferred Constant whose value is the 'pointer' to the target
 * Location for the LValue. This optimization serves two benefits. First it
 * allows the pointer generation logic to be eliminated. Second it creates
 * unique address sources for each LValue (when possible) thus, potentially,
 * allowing the target Location of those LValues to be split into unique
 * memories.
 * 
 * 
 * <p>
 * Created: Wed Jan 28 06:28:30 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: BaseAddressUniquifier.java 70 2005-12-01 17:43:11Z imiller $
 */
public class BaseAddressUniquifier extends DefaultVisitor implements
		Optimization {

	private ObjectResolver resolver = null;

	private boolean isModified = false;

	private int uniquifiedBaseAddressCount = 0;

	public BaseAddressUniquifier() {
	}

	@Override
	public void visit(HeapRead lvalue) {
		uniquifyBaseAddressPort(lvalue);
	}

	@Override
	public void visit(HeapWrite lvalue) {
		uniquifyBaseAddressPort(lvalue);
	}

	@Override
	public void visit(ArrayRead lvalue) {
		uniquifyBaseAddressPort(lvalue);
	}

	@Override
	public void visit(ArrayWrite lvalue) {
		uniquifyBaseAddressPort(lvalue);
	}

	/**
	 * Replaces the dependency for the
	 * {@link OffsetMemoryAccess#getBaseAddressPort} with one to a
	 * LocationConstant whose value represents the calculated pointer value.
	 * 
	 * @param acc
	 *            a value of type 'OffsetMemoryAccess'
	 */
	private void uniquifyBaseAddressPort(OffsetMemoryAccess acc) {
		// Determine the Location values for the base address port via
		// ObjectResolver.getCurrentValues(port). Note that this
		// returns a Collection of LogicalValue objects, Pointer
		// objects.

		// Iff there is only 1 target location then:
		// 1. Test that there is only 1 logical dependency (and no
		// connections) to the base address port, if not stop and do
		// nothing more with this access.
		// 2. Test that the logical dependency does not come from a
		// Constant already, if so stop and do nothing more with this
		// access.
		// 3. Test that the target location is a fixed location (non
		// index) by calling:
		// <LogicalValue>.toLocation().getInitialValue(). An Index
		// Location will throw an exception. Also check that the
		// returned Location is not Location.INVALID. If the
		// exception is thrown or the Location is INVALID, stop and do
		// nothing more with this access.
		// 4. Create a new Constant by <LogicalValue>.toConstant()
		// 5. add the new Constant to the accesses owner
		// 6. Remove the existing dependency to the base address port
		// 7. Create a new dependency to the base address port from
		// the constant.
		// 8. set the ismodified flag for this pass of the
		// optimization to true.

		final Port addressPort = acc.getBaseAddressPort();
		final Collection logicalValues = resolver.getCurrentValues(addressPort);

		if (logicalValues.size() == 1) {
			final LogicalValue logicalValue = (LogicalValue) logicalValues
					.iterator().next();
			final List<Entry> entries = addressPort.getOwner().getEntries();
			if (entries.size() == 1) {
				Entry entry = entries.iterator().next();
				final Collection<Dependency> dependencies = entry
						.getDependencies(addressPort);
				if ((dependencies.size() == 1) && !addressPort.isConnected()) {
					DataDependency dataDep = (DataDependency) dependencies
							.iterator().next();
					if (!dataDep.getLogicalBus().getOwner().getOwner()
							.isConstant()) {
						try {
							// The call to getInitialValue may throw
							// an IllegalInitialValueContextException
							// which we may catch here. Do not remove
							// this call.
							// final LogicalValue fixedValue =
							logicalValue.toLocation().getInitialValue();
							if (logicalValue.toLocation() != Location.INVALID) {
								LocationConstant locConst = (LocationConstant) logicalValue
										.toConstant();
								class Inserter extends ComponentSwapVisitor {
									public void insertMe(Component orig,
											Component insert) {
										this.moduleInsert(orig, insert);
									}
								}

								Inserter inserter = new Inserter();
								inserter.insertMe(acc, locConst);

								dataDep.zap();
								dataDep = new DataDependency(
										locConst.getValueBus());
								entry.addDependency(addressPort, dataDep);
								isModified = true;
								uniquifiedBaseAddressCount++;
							}
						} catch (Location.IllegalInitialValueContextException e) {
						}
					}
				}
			}
		}
	}

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		// Call the object resolver and save the returned resolver
		// handle.
		assert (target instanceof Design) : "BaseAddressUniquifier.run() only runs on a Design";
		final Design design = (Design) target;
		resolver = ObjectResolver.resolve(design);

		// Visit all OffsetMemoryAccess objects (heap/array
		// read/write)
		for (LogicalMemory memory : design.getLogicalMemories()) {
			for (LValue lvalue : memory.getLValues()) {
				lvalue.accept(this);
			}
		}
	}

	/**
	 * Method called prior to performing the optimization, should use Job (info,
	 * verbose, etc) to report to the user what action is being performed.
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info(
				"uniquifying memory base addresses...");
	}

	/**
	 * Method called after performing the optimization, should use Job (info,
	 * verbose, etc) to report to the user the results (if any) of running the
	 * optimization
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().verbose(
				"uniquified " + uniquifiedBaseAddressCount
						+ " memory base addresses");
	}

	/**
	 * Should return true if the optimization modified the LIM <b>and</b> that
	 * other optimizations in its grouping should be re-run
	 */
	@Override
	public boolean didModify() {
		return isModified;
	}

	/**
	 * The clear method is called after each complete visit to the optimization
	 * and should free up as much memory as possible, and reset any per run
	 * status gathering.
	 */
	@Override
	public void clear() {
		resolver = null;
		isModified = false;
		uniquifiedBaseAddressCount = 0;
	}

}// BaseAddressUniquifier
