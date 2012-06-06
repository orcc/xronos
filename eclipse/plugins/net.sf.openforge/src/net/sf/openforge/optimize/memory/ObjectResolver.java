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

package net.sf.openforge.optimize.memory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataFlowVisitor;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Register;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.Resource;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.Allocation;
import net.sf.openforge.lim.memory.DefaultMemoryVisitor;
import net.sf.openforge.lim.memory.LValue;
import net.sf.openforge.lim.memory.Location;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.lim.memory.LocationValueSource;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.memory.MemoryVisitable;
import net.sf.openforge.lim.memory.Pointer;
import net.sf.openforge.lim.memory.Record;
import net.sf.openforge.lim.memory.Scalar;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.BinaryOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.OrOpMulti;
import net.sf.openforge.lim.op.PlusOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.UnaryOp;
import net.sf.openforge.lim.op.XorOp;

/**
 * <code>ObjectResolver</code> maps out the potential flow of pointer values
 * through a user's {@link Design}. The purpose is to determine which memory
 * {@link Location Locations} each {@link LValue} operation may access. This
 * information can be used to optimize the number and configuration of the
 * memories in the design.
 * <P>
 * ObjectResolver works by repeatedly visiting the Design and pushing any
 * {@link Pointer} values it finds through the data flows. These values are
 * traced through {@link Port} and {@link Bus} connections as well as storage
 * into and retrieval from global memory {@link Location Locations}. The initial
 * set of {@link Pointer} values are introduced by the contents of global memory
 * (Pointer initial values of global fields) and any {@link LocationConstant
 * LocationConstants} that appear in the design. As the design is traversed,
 * various operations may introduce new {@link Pointer} values based upon
 * existing {@link Pointer} values. Iteration stops when no new accessed
 * {@link Location Locations} are discovered for any {@link LValue} operation.
 * <P>
 * In addition to {@link Location Locations}, ObjectResolver also records for
 * each {@link LValue} which {@link LocationValueSource LocationValueSources},
 * if any, provide a base address {@link Location} that is used by the
 * {@link LValue}, either directly or through a {@link Location} that was
 * derived from the base address. These {@link LocationValueConstant
 * LocationVaulueConstants} are known as <i>address sources</i>, and the
 * ObjectResolver can be queried for the identity of these. Typically each such
 * constant operation is constrained to refer to the same {@link LogicalMemory}
 * as any {@link LValue} that references its {@link Location}.
 * <P>
 * As a side effect of analyzing the design, the ObjectResolver will also re-add
 * each {@link LValue} to its {@link LogicalMemoryPort} with the
 * {@link Location Locations} that were found for it.
 * 
 * <b>NOTE:</b> It may appear that object resolver is storing a lot of redundant
 * information. This is primarily because of the fact that Location objects are
 * not necessarily unique objects. Thus two independent operations (eg an add
 * and a subtract) may actually generate Pointers to the exact same Location
 * object. For this reason, the Location objects cannot be used to trace back to
 * a source operation and thus this class tracks source operations and address
 * sources in addition to accessed Locations.
 * 
 * @version $Id: ObjectResolver.java 70 2005-12-01 17:43:11Z imiller $
 */
public class ObjectResolver extends DataFlowVisitor {

	private static final boolean debug = false;

	/** The design being resolved */
	private Design design;

	/** The set of components at the top level. Calls, pins, etc. */
	private Set<?> topLevelComponents;

	/** True if modified during the current iteration */
	private boolean isModified = false;

	/** Map of Location, Port, or Bus to Set of LogicalValue (mostly Pointers) */
	private Map<Object, Set<LogicalValue>> valueMap = new HashMap<Object, Set<LogicalValue>>();

	/** Map of LValue to Set of Locations accessed by that LValue */
	private Map<LValue, Set<Location>> accessedLocationMap = new HashMap<LValue, Set<Location>>();

	/** Map of LogicalValue to LocationValueSource source */
	private Map addressValueSourceMap = new IdentityHashMap();

	/** Map of LValue to Set of LocationValueSource sources */
	private Map<LValue, Set<LocationValueSource>> addressSourceMap = new HashMap<LValue, Set<LocationValueSource>>();

	/**
	 * A map of Location object to a Set of objects that may produce that
	 * location. THIS MAP ONLY CONTAINS LOCATIONS GENERATED AT RUNTIME. ie, it
	 * contains Locations created by any location.createXXX call in this class.
	 * This map is used to detect feedback and prevent infinite looping.
	 */
	private Map locationSourceMap = new HashMap();

	/**
	 * Creates a new <code>ObjectResolver</code> instance, runs the instance on
	 * a given {@link Design}, and then returns the instance for querying of
	 * results.
	 * 
	 * @param design
	 *            the design to be analyzed
	 * @return the completed resolver, which can be queried for its results
	 * @throws IllegalArgumentException
	 *             is <code>design</code> is null
	 */
	public static ObjectResolver resolve(Design design) {
		if (design == null) {
			throw new IllegalArgumentException("null design");
		}

		if (debug) {
			System.out.println("OBJECT RESOLVING");
			for (Iterator<LogicalMemory> iter = design.getLogicalMemories()
					.iterator(); iter.hasNext();) {
				iter.next().showContents();
			}
		}

		final ObjectResolver resolver = new ObjectResolver(design);
		do {
			resolver.iterate();
		} while (resolver.isModified());

		resolver.finish();

		if (debug) {
			for (Iterator<LogicalMemory> iter = design.getLogicalMemories()
					.iterator(); iter.hasNext();) {
				iter.next().showAccessors();
			}
		}

		return resolver;
	}

	/**
	 * Gets the memory {@link Location Locations} that are potentially accessed
	 * by the given access.
	 * 
	 * @param lvalue
	 *            a valid access in the design
	 * @return the set of {@link Location Locations} potentially accessed by the
	 *         given <code>lvalue</code>
	 */
	private Set<Location> getAccessedLocations(LValue lvalue) {
		final Set<Location> set = accessedLocationMap.get(lvalue);
		return set == null ? Collections.<Location> emptySet() : Collections
				.unmodifiableSet(set);
	}

	/**
	 * Gets the address sources for a given memory <code>access</code>. An
	 * address source is an {@link LValue} that provides the base component of
	 * the accessed location, and so must be associated with the same memory as
	 * the <code>access</code>.
	 * 
	 * @param access
	 *            a memory access
	 * @return a set of {@link LocationValueSource LocationValueSources}
	 *         representing the address sources for the <code>access</code>
	 */
	public Set<LocationValueSource> getAddressSources(LValue access) {
		final Set<LocationValueSource> set = addressSourceMap.get(access);
		return set == null ? Collections.<LocationValueSource> emptySet()
				: Collections.unmodifiableSet(set);
	}

	/**
	 * No outside instances, please.
	 */
	protected ObjectResolver(Design design) {
		this.design = design;
		setRunForward(true);

		topLevelComponents = new HashSet<Component>(design.getDesignModule()
				.getComponents());

		/*
		 * Initialize the valueMap with all the known Allocations.
		 */
		for (LogicalMemory logicalMemory : design.getLogicalMemories()) {
			// IDM new. Clear out all accessors. We will re-find
			// them during resolving.
			logicalMemory.clearAccessors();

			for (Allocation allocation : logicalMemory.getAllocations()) {
				allocation.getInitialValue()
						.accept(new Initializer(allocation));
			}

			// Set the 'addressSource' of any Pointer initial value to
			// be that pointer itself. All other Pointers in the LIM
			// come from LocationConstants or are derived from one of
			// these two sources.
			logicalMemory.accept(new PtrFinder());
		}

		for (Register reg : design.getRegisters()) {
			MemoryVisitable init = reg.getInitialValue();
			init.accept(new RegPtrFinder(reg));
		}
	}

	/**
	 * The PtrFinder traverses a LogicalMemory finding all Pointer initial
	 * values and then sets each as its own address source. This is correct
	 * because all initial values are their own address source, all other
	 * Pointers in the LIM come from LocationConstants or are derived from one
	 * of these two sources.
	 */
	private class PtrFinder extends DefaultMemoryVisitor {
		@Override
		public void visit(Pointer ptr) {
			setAddressSource(ptr, ptr);
			// IDM new
			if (ptr.getTarget() != null) {
				ptr.getTarget().getLogicalMemory().addAccessor(ptr);
			} else {
				EngineThread.getGenericJob().warn(
						"null target of pointer.  Continuing.");
			}
		}
	}

	/**
	 * Simply used to identify Pointer initial values and add that pointer as
	 * the initial value of the Register.
	 */
	private class RegPtrFinder extends PtrFinder {
		private Register reg;

		public RegPtrFinder(Register reg) {
			this.reg = reg;
		}

		@Override
		public void visit(Pointer ptr) {
			super.visit(ptr);
			setValues(reg, Collections.singleton(ptr));
			// IDM new
			ptr.getTarget().getLogicalMemory().addAccessor(ptr);
		}
	}

	/**
	 * After iterations have completed, associate each LValue with the Locations
	 * that were found for it.
	 */
	private void finish() {
		/*
		 * Re-add all LValues to their LogicalMemories with the Locations we've
		 * found.
		 */
		for (Map.Entry<LValue, Set<Location>> entry : accessedLocationMap
				.entrySet()) {
			final LValue access = entry.getKey();
			final Set<Location> locations = entry.getValue();

			if (debug) {
				System.out.println("Access " + access + " to " + locations);
			}

			if (!locations.isEmpty()) {
				/*
				 * Remove the access from its LogicalMemoryPort.
				 */
				final LogicalMemoryPort logicalMemoryPort = access
						.getLogicalMemoryPort();
				// No need to remove from logical memory port if we
				// clear out all accesses prior to resolving.
				// if (access.getLogicalMemoryPort() != null)
				// {
				// logicalMemoryPort.removeAccess(access);
				// }

				/*
				 * Add the access back to its LogicalMemoryPort for all
				 * Locations. Verify that the Locations all refer to the correct
				 * LogicalMemory.
				 */
				final LogicalMemory logicalMemory = logicalMemoryPort
						.getLogicalMemory();
				if (debug) {
					System.out.println("\taccess mem " + logicalMemory);
				}
				for (Location location : locations) {
					if (debug) {
						System.out.println("\tlocation mem "
								+ location.getLogicalMemory());
					}
					if (logicalMemory == location.getLogicalMemory()) {
						logicalMemoryPort.addAccess(access, location);
					}
				}
			}
		}

		// Might as well clear it out... we wont need it anymore.
		locationSourceMap.clear();
	}

	@Override
	public void visit(AbsoluteMemoryRead read) {
		final Location location = read.getAddressConstant().getTarget();
		// location.getLogicalMemory().addLocationConstant(read.getAddressConstant());
		read.getAddressConstant().accept(this);
		if (getAccessedLocations(read).isEmpty()) {
			addAccessedLocations(read, Collections.singleton(location));
		}
		final Set values = getValues(location);
		setValues(read.getResultBus(), values);
	}

	@Override
	public void visit(AbsoluteMemoryWrite write) {
		final Location location = write.getAddressConstant().getTarget();
		// location.getLogicalMemory().addLocationConstant(write.getAddressConstant());
		write.getAddressConstant().accept(this);
		if (getAccessedLocations(write).isEmpty()) {
			addAccessedLocations(write, Collections.singleton(location));
		}
		addValues(location, getValues(write.getValuePort()));
	}

	@Override
	public void visit(AddOp addOp) {
		/*
		 * If neither input has an associated Location, then the output will
		 * have no Location; likewise, if both inputs have associated Locations,
		 * then we have a possible attempt to add two pointers, which will also
		 * result in no Locations on the output.
		 */
		final Set leftValues = getNewValues(addOp.getLeftDataPort());
		final Set rightValues = getNewValues(addOp.getRightDataPort());

		final Value leftValue = addOp.getLeftDataPort().getValue();
		final Value rightValue = addOp.getRightDataPort().getValue();

		final Map<LogicalValue, Location> leftLocations = toLocationMap(leftValues);
		final Map<LogicalValue, Location> rightLocations = toLocationMap(rightValues);

		if (leftLocations.isEmpty() || rightLocations.isEmpty()) {
			final Map<LogicalValue, Location> inputLocations = leftLocations
					.isEmpty() ? rightLocations : leftLocations;
			final Value testValue = leftLocations.isEmpty() ? leftValue
					: rightValue;

			// Determine if one port is a constant value.
			final boolean isConst = testValue != null && testValue.isConstant();

			final Set<Pointer> outputValues = new HashSet<Pointer>();
			for (Map.Entry<LogicalValue, Location> entry : inputLocations
					.entrySet()) {
				final LogicalValue inputValue = entry.getKey();
				final Location inputLocation = entry.getValue();

				// If one port is a constant value, then create an
				// offset location, otherwise, punt and go with the
				// index location. If we are in a feedback loop then
				// use the index as well.
				Location outputLocation = null;
				boolean isLoop = isFeedbackLocation(addOp, inputLocation);

				/*
				 * Create the output location with a size of 0, since it is a
				 * pure pointer -- in this context, the number of bytes being
				 * accessed is unknown.
				 */
				if (isConst && !isLoop) {
					final int offset = (int) testValue.getValueMask();
					outputLocation = inputLocation.createOffset(0, offset);
				} else {
					outputLocation = inputLocation.createIndex(0);
				}

				final Pointer newValue = new Pointer(outputLocation);

				deriveAddressSource(newValue, inputValue);
				outputValues.add(newValue);
				defineLocationSource(outputLocation, addOp);
			}

			addValues(addOp.getResultBus(), outputValues);
		} else {
			visitBinaryOp(addOp);
		}
	}

	@Override
	public void visit(AndOp op) {
		visitBinaryOp(op);
	}

	@Override
	public void visit(ArrayRead arrayRead) {
		final Set addressValues = getNewValues(arrayRead.getBaseAddressPort());
		addAddressSources(arrayRead, addressValues);

		final Set<Location> baseLocations = toLocations(addressValues);
		final Set<Location> readLocations = new HashSet<Location>();
		for (Location baseLocation : baseLocations) {
			final Location readLocation = baseLocation.createIndex(arrayRead
					.getAccessLocationCount());
			defineLocationSource(readLocation, arrayRead);
			readLocations.add(readLocation);
		}
		addAccessedLocations(arrayRead, readLocations);

		final Set readValues = new HashSet();
		for (Location readLocation : getAccessedLocations(arrayRead)) {
			final Set values = getValues(readLocation);
			readValues.addAll(values);
		}
		setValues(arrayRead.getResultBus(), readValues);
	}

	@Override
	public void visit(ArrayWrite arrayWrite) {
		final Set addressValues = getNewValues(arrayWrite.getBaseAddressPort());
		addAddressSources(arrayWrite, addressValues);

		final Set baseLocations = toLocations(addressValues);
		final Set writeLocations = new HashSet();
		for (Iterator iter = baseLocations.iterator(); iter.hasNext();) {
			final Location baseLocation = (Location) iter.next();
			final Location writeLocation = baseLocation.createIndex(arrayWrite
					.getAccessLocationCount());
			defineLocationSource(writeLocation, arrayWrite);
			writeLocations.add(writeLocation);
		}
		addAccessedLocations(arrayWrite, writeLocations);

		final Set writeValues = getNewValues(arrayWrite.getValuePort());
		for (Location writeLocation : getAccessedLocations(arrayWrite)) {
			addValues(writeLocation, writeValues);
		}
	}

	@Override
	public void visit(Call call) {
		final boolean isTopLevel = topLevelComponents.contains(call);

		if (!isTopLevel) {
			for (Port callPort : call.getDataPorts()) {
				/*
				 * Explicitly set the locations of each procedure port to be the
				 * set of locations from the corresponding call port.
				 */
				final Set values = getValues(callPort);
				final Bus b = call.getProcedurePort(callPort).getPeer();
				setValues(b, values);
			}
		}

		super.visit(call);

		if (!isTopLevel) {
			for (Bus callBus : call.getBuses()) {
				/*
				 * Set the locations of each call bus to be the set of locations
				 * from the corresponding procedure bus.
				 */
				final Set values = getValues(call.getProcedureBus(callBus));
				setValues(callBus, values);
			}
		}
	}

	@Override
	public void visit(CastOp op) {
		setValues(op.getResultBus(), getValues(op.getDataPort()));
	}

	@Override
	public void visit(ComplementOp op) {
		visitUnaryOp(op);
	}

	@Override
	public void visit(DivideOp op) {
		visitBinaryOp(op);
	}

	/**
	 * Propagate any values stored in the register to the output bus of the
	 * register read.
	 */
	@Override
	public void visit(RegisterRead regRead) {
		setValues(regRead.getResultBus(), getValues(regRead.getResource()));
	}

	/**
	 * Propagte any values on the write port to the backing Register.
	 */
	@Override
	public void visit(RegisterWrite regWrite) {
		addValues(regWrite.getResource(), getNewValues(regWrite.getDataPort()));
	}

	@Override
	public void visit(HeapRead heapRead) {
		final Set addressValues = getNewValues(heapRead.getBaseAddressPort());
		addAddressSources(heapRead, addressValues);

		final Set<Location> baseLocations = toLocations(addressValues);
		final Set<Location> readLocations = new HashSet<Location>();
		for (Location baseLocation : baseLocations) {
			final Location readLocation = baseLocation.createOffset(
					heapRead.getAccessLocationCount(), heapRead.getOffset());
			defineLocationSource(readLocation, heapRead);
			readLocations.add(readLocation);
		}
		addAccessedLocations(heapRead, readLocations);

		final Set readValues = new HashSet();
		for (Location readLocation : getAccessedLocations(heapRead)) {
			final Set values = getValues(readLocation);
			readValues.addAll(values);
		}

		setValues(heapRead.getResultBus(), readValues);
	}

	@Override
	public void visit(HeapWrite heapWrite) {
		final Set addressValues = getNewValues(heapWrite.getBaseAddressPort());
		addAddressSources(heapWrite, addressValues);

		final Set<Location> baseLocations = toLocations(addressValues);
		final Set<Location> writeLocations = new HashSet<Location>();
		for (Location baseLocation : baseLocations) {
			final Location writeLocation = baseLocation.createOffset(
					heapWrite.getAccessLocationCount(), heapWrite.getOffset());
			defineLocationSource(writeLocation, heapWrite);
			writeLocations.add(writeLocation);
		}
		addAccessedLocations(heapWrite, writeLocations);

		final Set writeValues = getNewValues(heapWrite.getValuePort());
		for (Location writeLocation : getAccessedLocations(heapWrite)) {
			addValues(writeLocation, writeValues);
		}
	}

	@Override
	public void visit(InBuf inbuf) {
		for (Port port : inbuf.getOwner().getDataPorts()) {
			final Set values = new HashSet();

			for (Entry entry : inbuf.getOwner().getEntries()) {
				final Collection<Dependency> dependencies = entry
						.getDependencies(port);

				/*
				 * We have to allow for Entries that have no dependencies for a
				 * given Port; for instance, a Port of a LoopBody may have an
				 * initial value but not a feedback value.
				 */
				assert dependencies.size() <= 1 : ("data port dependencies for a single entry: expected 0 or 1, but was "
						+ dependencies.size() + " port=" + port + " of " + port
							.getOwner());

				if (!dependencies.isEmpty()) {
					final Dependency dependency = dependencies.iterator()
							.next();
					values.addAll(getValues(dependency.getLogicalBus()));
				}
			}
			// Use 'addValues' here instead of setValues because the
			// InBuf buses are set by the visit to the Call when the
			// inbuf is to a procedure body, however, the buses are
			// correctly set by this method for all other module
			// boundries. The addValues ensures that this method does
			// not overwrite the data obtained by the visit(Call)
			addValues(port.getPeer(), values);
		}
	}

	/**
	 * A latch can be a feedback point in the LIM, therefore it has the ability
	 * to accumulate LogicalValues on its result bus on successive passes
	 * through object resolving. To account for this, it must be able to set the
	 * isModified flag and cause object resolving to iterate again.
	 */
	@Override
	public void visit(Latch latch) {
		/*
		 * A Latch does not create any new values, so just pass through what
		 * comes in.
		 */
		setValues(latch.getResultBus(), getValues(latch.getDataPort()));
	}

	@Override
	public void visit(LeftShiftOp op) {
		addDefaultValues(getNewValues(op.getLeftDataPort()), op.getResultBus());
	}

	@Override
	public void visit(LocationConstant locationConstant) {
		final Location location = locationConstant.getTarget();
		if (getValues(locationConstant.getValueBus()).isEmpty()) {
			final Pointer outputValue = new Pointer(location);
			setAddressSource(outputValue, locationConstant);
			setValues(locationConstant.getValueBus(),
					Collections.singleton(outputValue));
		}
		locationConstant.getTarget().getLogicalMemory()
				.addLocationConstant(locationConstant);
	}

	@Override
	public void visit(Constant constant) {
		// Some constants are 'built up' of other constants, so make
		// sure that we visit all the backing constants as well.
		Set contents = new HashSet(constant.getContents());
		contents.remove(constant);
		for (Iterator iter = contents.iterator(); iter.hasNext();) {
			((Constant) iter.next()).accept(this);
		}
	}

	@Override
	public void visit(MinusOp op) {
		visitUnaryOp(op);
	}

	@Override
	public void visit(ModuloOp op) {
		addDefaultValues(getNewValues(op.getLeftDataPort()), op.getResultBus());
	}

	@Override
	public void visit(MultiplyOp op) {
		visitBinaryOp(op);
	}

	@Override
	public void visit(Mux mux) {
		final Set inputValues = new HashSet();
		for (Iterator iter = mux.getGoPorts().iterator(); iter.hasNext();) {
			final Port goPort = (Port) iter.next();
			inputValues.addAll(getValues(mux.getDataPort(goPort)));
		}
		setValues(mux.getResultBus(), inputValues);
	}

	@Override
	public void visit(NoOp comp) {
		Exit exit = comp.getOnlyExit();
		assert exit.getDataBuses().size() == comp.getDataPorts().size();
		for (Iterator dbIter = exit.getDataBuses().iterator(), portIter = comp
				.getDataPorts().iterator(); dbIter.hasNext();) {
			final Bus db = (Bus) dbIter.next();
			final Port port = (Port) portIter.next();
			final Set values = getValues(port);
			setValues(db, values);
		}
	}

	@Override
	public void visit(NumericPromotionOp op) {
		setValues(op.getResultBus(), getValues(op.getDataPort()));
	}

	@Override
	public void visit(OrOp op) {
		visitBinaryOp(op);
	}

	public void visit(OrOpMulti op) {
		final Set inputValues = new HashSet();
		for (Iterator iter = op.getDataPorts().iterator(); iter.hasNext();) {
			final Port port = (Port) iter.next();
			inputValues.addAll(getNewValues(port));
		}
		addDefaultValues(inputValues, op.getResultBus());
	}

	@Override
	public void visit(OutBuf outbuf) {
		for (Iterator iter = outbuf.getDataPorts().iterator(); iter.hasNext();) {
			final Port port = (Port) iter.next();
			final Set values = new HashSet();

			if (port.isConnected()) {
				values.addAll(getValues(port.getBus()));
			} else {
				for (Iterator eiter = outbuf.getEntries().iterator(); eiter
						.hasNext();) {
					final Entry entry = (Entry) eiter.next();
					final Collection dependencies = entry.getDependencies(port);

					/*
					 * Allow for Entries with no Dependencies here, because it
					 * is possible for control flow to skip an Entry under
					 * certain condition. For example, in a while(1) loop.
					 */
					if (!dependencies.isEmpty()) {
						assert dependencies.size() == 1 : ("dependency count for Port/Entry tuple: expected 1, was "
								+ dependencies.size()
								+ "\n"
								+ outbuf
								+ " lineage="
								+ outbuf.getLineage()
								+ "\npeer="
								+ outbuf.getPeer() + "\nowner exits=" + outbuf
								.getOwner().getExits());

						final Dependency dependency = (Dependency) dependencies
								.iterator().next();
						values.addAll(getValues(dependency.getLogicalBus()));
					}
				}
			}

			setValues(port.getPeer(), values);
		}
	}

	@Override
	public void visit(PlusOp op) {
		setValues(op.getResultBus(), getValues(op.getDataPort()));
	}

	@Override
	public void visit(Reg reg) {
		/*
		 * A Reg does not create any new values, so just pass through what comes
		 * in.
		 */
		setValues(reg.getResultBus(), getValues(reg.getDataPort()));
	}

	@Override
	public void visit(RightShiftOp op) {
		addDefaultValues(getNewValues(op.getLeftDataPort()), op.getResultBus());
	}

	@Override
	public void visit(RightShiftUnsignedOp op) {
		addDefaultValues(getNewValues(op.getLeftDataPort()), op.getResultBus());
	}

	@Override
	public void visit(Task task) {
		/*
		 * Initialize the top level 'this' port, if any.
		 */
		Block body = task.getCall().getProcedure().getBody();
		final Port thisPort = task.getCall().getThisPort();
		if (thisPort != null) {
			final Port procPort = task.getCall().getProcedurePort(thisPort);
			setValues(procPort.getPeer(),
					Collections.singleton(new Pointer(task.getMemoryKey())));
		}
		super.visit(task);
	}

	@Override
	public void visit(XorOp op) {
		visitBinaryOp(op);
	}

	/**
	 * Handle pointer subtraction;
	 * 
	 * @param subtractOp
	 *            a subtract of two pointers, or of a pointer and an integer
	 */
	@Override
	public void visit(SubtractOp subtractOp) {
		/*
		 * If neither input has an associated Location, then the output will
		 * have no Location; likewise, if both inputs have associated Locations,
		 * then we have a possible attempt to subtract two pointers, which will
		 * also result in no Locations on the output.
		 */
		final Set leftValues = getNewValues(subtractOp.getLeftDataPort());
		final Set rightValues = getNewValues(subtractOp.getRightDataPort());

		final Value rightValue = subtractOp.getRightDataPort().getValue();

		final Map leftLocations = toLocationMap(leftValues);
		final Map rightLocations = toLocationMap(rightValues);
		if (leftLocations.isEmpty() || rightLocations.isEmpty()) {
			final Map inputLocations = leftLocations.isEmpty() ? rightLocations
					: leftLocations;
			// Only the right value may be a constant
			final Value testValue = leftLocations.isEmpty() ? null : rightValue;

			// Determine if one port is a constant value.
			final boolean isConst = testValue != null && testValue.isConstant();

			final Set outputValues = new HashSet();
			for (Iterator iter = inputLocations.entrySet().iterator(); iter
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) iter.next();
				final LogicalValue inputValue = (LogicalValue) entry.getKey();
				final Location inputLocation = (Location) entry.getValue();

				// If one port is a constant value, then create an
				// offset location, otherwise, punt and go with the
				// index location. If we are in a feedback loop then
				// use the index as well.
				Location outputLocation = null;
				boolean isLoop = isFeedbackLocation(subtractOp, inputLocation);

				/*
				 * Create the output location with a size of 0, since it is a
				 * pure pointer -- in this context, the number of bytes being
				 * accessed is unknown.
				 */
				if (isConst && !isLoop) {
					final int offset = (int) testValue.getValueMask();
					outputLocation = inputLocation.createOffset(0, -offset);
				} else {
					outputLocation = inputLocation.createIndex(0);
				}

				final Pointer newValue = new Pointer(outputLocation);

				deriveAddressSource(newValue, inputValue);
				outputValues.add(newValue);
				defineLocationSource(outputLocation, subtractOp);
			}

			addValues(subtractOp.getResultBus(), outputValues);
		} else {
			visitBinaryOp(subtractOp);
		}
	}

	/**
	 * Sets the output values for a given {@link Bus} based upon a set of input
	 * locations. For each input location, a value is produced which represents
	 * a pointer to the absolute base of the input location. This is our way of
	 * throwing up our hands when some unknown arithmetic is being applied to
	 * the input location.
	 * 
	 * @param inputValues
	 *            a set of {@link LogicalValue}, representing the input
	 *            addresses that are being munged somehow
	 * @param outputBus
	 *            the bus to which the munged address values are being added
	 */
	private void addDefaultValues(Set<LogicalValue> inputValues, Bus outputBus) {
		final Set outputValues = new HashSet();
		for (Iterator iter = inputValues.iterator(); iter.hasNext();) {
			final LogicalValue inputValue = (LogicalValue) iter.next();
			final Location inputLocation = inputValue.toLocation();
			if (inputLocation != Location.INVALID) {
				final Pointer pointer = new Pointer(
						inputLocation.getAbsoluteBase());
				deriveAddressSource(pointer, inputValue);
				outputValues.add(pointer);
			}
		}
		addValues(outputBus, outputValues);
	}

	private void visitBinaryOp(BinaryOp op) {
		final Set inputValues = new HashSet(getNewValues(op.getLeftDataPort()));
		inputValues.addAll(getNewValues(op.getRightDataPort()));
		addDefaultValues(inputValues, op.getResultBus());
	}

	private void visitUnaryOp(UnaryOp op) {
		addDefaultValues(getNewValues(op.getDataPort()), op.getResultBus());
	}

	private void iterate() {
		isModified = false;
		design.accept(this);
	}

	private boolean isModified() {
		return isModified;
	}

	/**
	 * Gets all non-INVALID Locations from a given set of LogicalValues.
	 */
	private static Set<Location> toLocations(
			Collection<LogicalValue> logicalValues) {
		final Set<Location> locations = new HashSet<Location>();
		for (LogicalValue logicalValue : logicalValues) {
			final Location location = logicalValue.toLocation();
			if (location != Location.INVALID) {
				locations.add(location);
			}
		}
		return locations;
	}

	/**
	 * Gets all non-INVALID Locations from a given set of LogicalValues and
	 * returns the results as a Map of LogicalValue to Location.
	 */
	private static Map<LogicalValue, Location> toLocationMap(
			Collection<LogicalValue> logicalValues) {
		final Map<LogicalValue, Location> locations = new HashMap<LogicalValue, Location>();
		for (LogicalValue logicalValue : logicalValues) {
			final Location location = logicalValue.toLocation();
			if (location != Location.INVALID) {
				locations.put(logicalValue, location);
			}
		}
		return locations;
	}

	/**
	 * Gets the input Bus for a given Port. If the Port is connected, its
	 * connected Bus is returned. Otherwise, it is expected that the Port's
	 * owner will have a single Entry with a single Dependency for the Port from
	 * which the Bus can be derived.
	 */
	private Bus getInputBus(Port port) {
		Bus bus = null;

		if (port.isConnected()) {
			bus = port.getBus();
		} else {
			final Collection<Entry> entries = port.getOwner().getEntries();
			assert entries.size() == 1 : "attempt to get single input Bus for Port with multiple Entries";
			final Entry entry = entries.iterator().next();

			final Collection<Dependency> dependencies = entry
					.getDependencies(port);

			assert dependencies.size() == 1 : ("dependencies for data Port of "
					+ port.getOwner() + " (lineage: "
					+ port.getOwner().getLineage() + "); expected 1, was " + dependencies
						.size());

			final Dependency dependency = dependencies.iterator().next();
			bus = dependency.getLogicalBus();
		}

		return bus;
	}

	private Set getNewValues(Port port) {
		final Set currentValues = getCurrentValues(port);
		Set newValues = new HashSet(getValues(getInputBus(port)));
		newValues.removeAll(currentValues);
		addValues(port, newValues);
		return newValues;
	}

	public Set getCurrentValues(Port port) {
		Set set = valueMap.get(port);
		if (set == null) {
			set = new HashSet();
			valueMap.put(port, set);
		}
		return set;
	}

	private void addValues(Port port, Set values) {
		getCurrentValues(port).addAll(values);
	}

	/**
	 * Returns the Set of {@link LogicalValue} objects that have been identified
	 * for the specified port, mostly {@link Pointer} objects.
	 * 
	 * @param port
	 *            a non-null Port
	 * @return a Set of {@link LogicalValue} objects
	 */
	private Set getValues(Port port) {
		final Set set = valueMap.get(port);
		return set == null ? getValues(getInputBus(port)) : set;
	}

	private Set getValues(Bus bus) {
		return getValuesForObject(bus);
	}

	private Set getValues(Location location) {
		return getValuesForObject(location);
	}

	private Set getValues(Resource reg) {
		return getValuesForObject(reg);
	}

	private Set getValuesForObject(Object key) {
		final Set set = valueMap.get(key);
		return set == null ? Collections.EMPTY_SET : set;
	}

	private void setValues(Bus bus, Set values) {
		setValuesForObject(bus, values);
	}

	private void addValues(Bus bus, Set newValues) {
		final Set values = new HashSet(getValues(bus));
		values.addAll(newValues);
		setValues(bus, values);
	}

	private void setValues(Location location, Set values) {
		setValuesForObject(location, values);
	}

	private void addValues(Location location, Set newValues) {
		final Set values = new HashSet(getValues(location));
		values.addAll(newValues);
		setValues(location, values);
	}

	private void setValues(Resource reg, Set values) {
		setValuesForObject(reg, values);
	}

	private void addValues(Resource reg, Set newValues) {
		final Set values = new HashSet(getValues(reg));
		values.addAll(newValues);
		setValues(reg, values);
	}

	private void setValuesForObject(Object key, Set values) {
		if (debug) {
			Set oldSet = valueMap.get(key);
			if (oldSet == null) {
				oldSet = new HashSet();
			} else {
				oldSet = new HashSet(oldSet);
			}
			Set newSet = new HashSet(values);
			newSet.removeAll(oldSet);
		}
		valueMap.put(key, values);
	}

	/**
	 * Records the address source for a given pointer value.
	 * 
	 * @param value
	 *            a pointer value
	 * @param source
	 *            the operation that produces the pointer value
	 */
	private void setAddressSource(LogicalValue value,
			LocationValueSource/* LValue */source) {
		addressValueSourceMap.put(value, source);
	}

	/**
	 * Records the source of a LogicalValue to be the same as that of another
	 * LogicalValue.
	 * 
	 * @param value
	 *            the value whose source is to be derived
	 * @param oldValue
	 *            the value whose source is copied
	 */
	private void deriveAddressSource(LogicalValue value, LogicalValue oldValue) {
		final LocationValueSource source = getAddressSource(oldValue);
		if (source != null) {
			setAddressSource(value, source);
		} else {
			// All Pointers in the LIM must ultimately derive from one
			// of 2 sources. Those are initial values of global
			// fields (Pointer initializers) or from
			// LocationConstants.
			throw new RuntimeException(
					"Illegal internal state.  Location has no address source");
		}
	}

	/**
	 * Gets the address source for a given pointer value.
	 * 
	 * @param value
	 *            a pointer value
	 * @return the {@link LocationValueSource} that produced the base address of
	 *         the given LogicalValue.
	 */
	private LocationValueSource getAddressSource(LogicalValue value) {
		return (LocationValueSource) addressValueSourceMap.get(value);
	}

	/**
	 * Records for a given access the address sources that provide any of a
	 * given set of pointer values.
	 * 
	 * @param access
	 *            a memory access
	 * @param addressValues
	 *            the set of address LogicalValues accessed by the memory access
	 */
	private void addAddressSources(LValue access, Set addressValues) {
		Set sources = addressSourceMap.get(access);
		if (sources == null) {
			sources = new HashSet();
			addressSourceMap.put(access, sources);
		}

		for (Iterator iter = addressValues.iterator(); iter.hasNext();) {
			final LocationValueSource source = getAddressSource((LogicalValue) iter
					.next());
			if (source != null) {
				sources.add(source);
			}
		}
	}

	/**
	 * Adds a given set of Locations to the set of those that can be accessed by
	 * a given LValue.
	 * 
	 * @param access
	 *            a memory access
	 * @param addressValues
	 *            the set of additional address Locations accessed by the memory
	 *            access
	 */
	private void addAccessedLocations(LValue access, Set newLocations) {
		Set locations = accessedLocationMap.get(access);
		if (locations == null) {
			locations = new HashSet();
			accessedLocationMap.put(access, locations);
		}

		if (!locations.containsAll(newLocations)) {
			locations.addAll(newLocations);
			isModified = true;
		}
	}

	/**
	 * Adds an objects as one source of a given Location. There may be multiple
	 * sources because a Location refers only to a region in memory and may thus
	 * be created by multiple operations and the way that Locations are built we
	 * may use a 'cached' version.
	 * 
	 * @param loc
	 *            a value of type 'Location'
	 * @param source
	 *            a value of type 'Object'
	 */
	private void defineLocationSource(Location loc, Object source) {
		Set sources = (Set) locationSourceMap.get(loc);
		if (sources == null) {
			sources = new HashSet();
			locationSourceMap.put(loc, sources);
		}
		sources.add(source);
	}

	/**
	 * This method analyses the given Location (test) to determine if it is
	 * possible that the specified oject (op) may have been responsible for the
	 * creation of test or any of its bases. This is an indication that the
	 * object in question may be part of a feedback loop generating pointers.
	 * This is not, however, a fully definative test for the looping case. It is
	 * guaranteed to return true for all feedback conditions, but it may be
	 * possible for it to return a false positive if the test location is one
	 * which can also be generated by the operation.
	 * <p>
	 * It is likely that the first iteration of the ObjectResolver will not
	 * identify any feedback locations, but subsequent iterations should find
	 * them.
	 * <p>
	 * This method is used to ensure that we dont end up creating an infinite
	 * number of Locations because of constructs like ptr++ in a loop.
	 * 
	 * <p>
	 * Testcase: c/language/pointer/VarInitViaSmallerPointer.c
	 * 
	 * @param op
	 *            an Object which may be responsible for the generation of the
	 *            test location (eg an AddOp, SubtractOp, etc)
	 * @param test
	 *            a Location to test for a feedback condition.
	 * @return a false if there is no way that the given operation can generate
	 *         the test Location or any of its bases.
	 */
	private boolean isFeedbackLocation(Object op, Location test) {
		boolean isLoop = false;
		Location nextLocation = test;
		Location testLocation = test;
		do {
			testLocation = nextLocation;
			nextLocation = nextLocation.getBaseLocation();

			if (locationSourceMap.containsKey(testLocation)) {
				Set sources = (Set) locationSourceMap.get(testLocation);
				for (Iterator iter = sources.iterator(); iter.hasNext();) {
					Object testObj = iter.next();
					isLoop |= (testObj == op);
				}
			} else {
				// Because we dont prime the locationSourceMap with
				// all address sources any Allocation that we reach
				// will probably not exist in the map. This is OK
				// since the address sources will not ever be feedback
				// points (they dont consume Locations and thus cant
				// be part of a loop)
			}

		} while (testLocation.getBaseLocation() != testLocation);

		return isLoop;
	}

	private class Initializer extends DefaultMemoryVisitor {
		final Location baseLocation;

		Initializer(Location baseLocation) {
			this.baseLocation = baseLocation;
		}

		@Override
		public void visit(Scalar scalar) {
			setValues(baseLocation, Collections.singleton(scalar));
		}

		@Override
		public void visit(Pointer pointer) {
			setValues(baseLocation, Collections.singleton(pointer));
		}

		@Override
		public void visit(Record record) {
			int delta = 0;
			for (Iterator iter = record.getComponentValues().iterator(); iter
					.hasNext();) {
				final LogicalValue nextValue = (LogicalValue) iter.next();
				final Location location = baseLocation.createOffset(
						nextValue.getSize(), delta);
				nextValue.accept(new Initializer(location));
				delta += nextValue.getSize();
			}
		}
	}

} // ObjectResolver

