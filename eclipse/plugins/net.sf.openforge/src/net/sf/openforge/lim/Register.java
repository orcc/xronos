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
package net.sf.openforge.lim;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.memory.AddressableUnit;
import net.sf.openforge.lim.memory.EndianSwapper;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.primitive.Mux;
import net.sf.openforge.lim.primitive.Or;
import net.sf.openforge.lim.primitive.Reg;
import net.sf.openforge.schedule.GlobalConnector;

/**
 * A single register memory. The Register object is an atomic element accessed
 * by {@link RegisterRead} and {@link RegisterWrite} lim components, and <b>is
 * always considered to be unsigned</b>. It is up to the accessing context to
 * correctly cast the read/write values from/to unsigned as needed. To
 * facilitate this convertion the methods {@link createWriteAccess} and
 * {@link createReadAccess} have been made available. After scheduling, and
 * during GlobalConnection, the Register is implemented in a Physical module by
 * instantiating a Reg object with the correct attributes.
 * 
 * @author Stephen Edwards
 * @version $Id: Register.java 538 2007-11-21 06:22:39Z imiller $
 */
public class Register extends Storage implements StateHolder, Arbitratable {

	/** True if this is a volatile register */
	private boolean isVolatile = false;

	/** Collection of RegisterRead */
	private Collection<RegisterRead> reads = new HashSet<RegisterRead>(11);

	/** Collection of RegisterWrite */
	private Collection<RegisterWrite> writes = new HashSet<RegisterWrite>(11);

	/**
	 * This Component (either a plain Register.Physical or a RegisterReferee)
	 * maintains physical connections for the Register.
	 */
	private Module registerComponent = null;

	/** The width to which this Register was constructed. */
	private final int initWidth;

	/** The initial value of this Register. */
	private LogicalValue initialValue = null;

	/** The endianness swapper modules for Big Endian, comes in a pair */
	private EndianSwapper inputSwapper = null;
	private EndianSwapper outputSwapper = null;

	/**
	 * Constructs a new Register with the specified width
	 * 
	 * @param init
	 *            , the {@link LogicalValue} which specifies the initial value
	 *            to be used for this Register. May be null.
	 * @param width
	 *            the bit width of the register
	 * @param isVolatile
	 *            true if this is a volatile register
	 * @throws IllegalArgumentException
	 *             if width < 1.
	 */
	public Register(LogicalValue init, int width, boolean isVolatile) {
		super();
		initialValue = init;
		this.isVolatile = isVolatile;
		if (width < 1) {
			throw new IllegalArgumentException(
					"Illegal initial width specified for register: " + width);
		}
		initWidth = width;
	}

	public void addEndianSwappers(EndianSwapper front, EndianSwapper back) {
		inputSwapper = front;
		outputSwapper = back;
	}

	public EndianSwapper getInputSwapper() {
		return inputSwapper;
	}

	public EndianSwapper getOutputSwapper() {
		return outputSwapper;
	}

	/**
	 * Returns the LogicalValue which specifies the initial value of this
	 * Register, or null if none has been specified.
	 * 
	 * @return a {@link LogicalValue} or null.
	 */
	public LogicalValue getInitialValue() {
		return initialValue;
	}

	/**
	 * Returns false, the implementation of the Register is ALWAYS unsigned, but
	 * every read and write access contains a CastOp to convert from the
	 * unsigned backing to the correct type Value for the access. {@see
	 * RegisterAccessBlock}. This is a static method because it holds true for
	 * all Register objects.
	 * 
	 * @return a false
	 */
	public static boolean isSigned() {
		return false;
	}

	/**
	 * NOT IMPLEMENTED <strike>Returns true if the initial value of this
	 * register is a primitive value and is floating point</strike>
	 */
	public boolean isFloat() {
		// TBD XXX. Implement me.
		return false;
	}

	/**
	 * Returns the width of this Register that was specified at the time of its
	 * construction.
	 * 
	 * @return a non-negative 'int'
	 */
	public int getInitWidth() {
		return initWidth;
	}

	/**
	 * Tests the referencer types for compatibility and then returns 0 always.
	 * 
	 * @param from
	 *            the prior accessor in source 'document' order.
	 * @param to
	 *            the latter accessor in source 'document' order.
	 */
	@Override
	public int getSpacing(Referencer from, Referencer to) {
		if (from instanceof RegisterWrite) {
			return 1;
		}
		return 0;

		/*
		 * if ((from instanceof RegisterWrite) || (from instanceof
		 * RegisterRead)) return 1; else throw new
		 * IllegalArgumentException("Source access to " + this +
		 * " is of unknown type " + from.getClass());
		 */
	}

	/**
	 * Returns -1 indicating that the referencers must be scheduled using the
	 * default DONE to GO spacing.
	 */
	@Override
	public int getGoSpacing(Referencer from, Referencer to) {
		return -1;
	}

	public String cpDebug(boolean verbose) {
		String ret = "initial width: " + getInitWidth();
		return ret;
	}

	/**
	 * Constructs the physial implementation of this Register, including the
	 * backing register (flops) and any write-side arbitration logic that is
	 * needed. The returned {@link Component} is of type Register.Physical
	 * 
	 * @param readers
	 *            a list of the readers of this register, null indicates a
	 *            'slot' which is not a reader (but may line up with a write
	 *            access in the writers list).
	 * @param writers
	 *            a list of the (arbitratable) writers of this register, null
	 *            indicates a 'slot' which is not a writer but may be a reader.
	 */
	public Component makePhysicalComponent(
			List<GlobalConnector.Connection> readers,
			List<GlobalConnector.Connection> writers) {
		String logicalId = showIDLogical();
		registerComponent = new Physical(readers, writers, logicalId);
		registerComponent.setIDLogical(logicalId);
		return registerComponent;
	}

	/**
	 * Gets the {@link Module} that maintains this Register connections. May
	 * either be a {@link RegisterReferee} or a {@link Register.Physical}.
	 */
	public Module getPhysicalComponent() {
		return registerComponent;
	}

	/**
	 * Returns a fixed latency of ZERO because all accesses are guaranteed to
	 * succeed and the resource dependencies will ensure that multiple accesses
	 * are seperated by a cycle.
	 */
	@Override
	public Latency getLatency(Exit exit) {
		return Latency.ZERO;
	}

	@Override
	public Collection<Reference> getReferences() {
		Collection<Reference> list = new ArrayList<Reference>(reads.size()
				+ writes.size());
		list.addAll(reads);
		list.addAll(writes);
		return list;
	}

	/**
	 * Makes a new {@link RegisterAccessBlock#RegisterReadBlock} that can be
	 * used to correctly access this Register and preserves the signedness of
	 * the access through the register. This method is the preferred method of
	 * generating accesses to this Register.
	 * 
	 * @param signed
	 *            a 'boolean', true if the access is a signed access
	 * @return a value of type 'RegisterAccessBlock.RegisterReadBlock'
	 */
	public RegisterAccessBlock.RegisterReadBlock createReadAccess(boolean signed) {
		return new RegisterAccessBlock.RegisterReadBlock(makeReadAccess(signed));
	}

	/**
	 * Makes a new {@link RegisterAccessBlock#RegisterWriteBlock} that can be
	 * used to correctly access this Register and preserves the signedness of
	 * the access through the register. This method is the preferred method of
	 * generating accesses to this Register.
	 * 
	 * @param signed
	 *            a 'boolean', true if the access is a signed access
	 * @return a value of type 'RegisterAccessBlock.RegisterWriteBlock'
	 */
	public RegisterAccessBlock.RegisterWriteBlock createWriteAccess(
			boolean signed) {
		return new RegisterAccessBlock.RegisterWriteBlock(
				makeWriteAccess(signed));
	}

	/**
	 * makes a new atomic register read to access this Register, this method
	 * should not be generally used {@see createReadAccess}
	 * 
	 * @param isSigned
	 *            a boolean, true if this read is a signed access.
	 * @return a value of type 'RegisterRead'
	 */
	RegisterRead makeReadAccess(boolean isSigned) {
		RegisterRead read = new RegisterRead(this, isSigned);
		reads.add(read);
		return read;
	}

	/**
	 * makes a new atomic register write to access this Register, this method
	 * should not be generally used {@see createWriteAccess}
	 * 
	 * @param isSigned
	 *            a boolean, true if this write is a signed access.
	 * @return a value of type 'RegisterWrite'
	 */
	RegisterWrite makeWriteAccess(boolean isSigned) {
		RegisterWrite write = new RegisterWrite(this, isSigned);
		writes.add(write);
		return write;
	}

	public Collection<RegisterRead> getReadAccesses() {
		return Collections.unmodifiableCollection(reads);
	}

	public Collection<RegisterWrite> getWriteAccesses() {
		return Collections.unmodifiableCollection(writes);
	}

	@Override
	public void removeReference(Reference ref) {
		if (!(reads.remove(ref) || writes.remove(ref))) {
			throw new IllegalArgumentException("unknown access");
		}
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	/**
	 * Clones this register, clearing out the sets of stored reads and writes as
	 * well as cloning the underlying register component.
	 * 
	 * @return a Register object.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@Override
	public String toString() {
		String ret = super.toString();
		ret = ret.replaceAll("net.sf.openforge.lim.", "");
		return ret + "=<" + getInitialValue() + ">";
	}

	// //////////////////////////////////////////////////
	//
	// Implementation of the Arbitratable interface
	//
	// //////////////////////////////////////////////////

	@Override
	public int getDataPathWidth() {
		return getInitWidth();
	}

	@Override
	public int getAddrPathWidth() {
		// Default to something reasonable even though it is not used.
		return 32;
	}

	@Override
	public boolean isAddressable() {
		return false;
	}

	@Override
	public boolean allowsCombinationalReads() {
		return true;
	}

	// //////////////////////////////////////////////////
	//
	// End of the Arbitratable interface
	//
	// //////////////////////////////////////////////////

	/**
	 * Encapsulates the physical implementation of the Register. This class is a
	 * Module that contains a {@link Reg} object, initialized to the correct
	 * value and maps the ports/bus of the {@link Reg} to the connections
	 * necessary for connecting to the entry function.
	 */
	public class Physical extends PhysicalImplementationModule {
		Port enable;
		Port data;
		Bus registerOutput;

		/**
		 * Builds the physical implementation of a Register. NOTE: If/when sized
		 * accesses are allowed to Registers we need to create logic similar to
		 * the logic in StructuralMemory for managing the input data (write
		 * data). READ LOGIC must be implemented at each accessor, NOT the
		 * global level, so that we can still allow multiple parallel reads.
		 */
		private Physical(List<GlobalConnector.Connection> readers,
				List<GlobalConnector.Connection> writers, String logicalId) {
			super(0);

			final boolean isLittleEndian = EngineThread
					.getGenericJob()
					.getUnscopedBooleanOptionValue(OptionRegistry.LITTLE_ENDIAN);
			final boolean doSimpleMerge = EngineThread.getGenericJob()
					.getUnscopedBooleanOptionValue(
							OptionRegistry.SIMPLE_STATE_ARBITRATION);

			setIDLogical(logicalId);

			getResetPort().setUsed(true);
			setConsumesReset(true);
			getClockPort().setUsed(true);
			setConsumesClock(true);

			for (Iterator<GlobalConnector.Connection> iter = writers.iterator(); iter
					.hasNext();) {
				if (iter.next() != null) {
					Port enablePort = makeDataPort();
					enablePort.setUsed(true);
					enablePort.getPeer().setSize(1, false);
					Port dataPort = makeDataPort();
					dataPort.setUsed(true);
					dataPort.getPeer().setSize(getInitWidth(), isSigned());
				}
			}

			/*
			 * Use the byte representation because we want the register to have
			 * exactly the same byte representation as the memory (endian wise)
			 * and the toConstant() method will always return a little endian
			 * representation.
			 */
			// byte initRep[] = getInitialValue().getRep();
			// Constant initConstant = SimpleConstant.createConstant(initRep);
			BigInteger initValue = AddressableUnit.getCompositeValue(
					getInitialValue().getRep(), getInitialValue()
							.getAddressStridePolicy());
			Constant initConstant = new SimpleConstant(initValue,
					getInitWidth(), isSigned());

			// A Constant object representing the register's initial value
			// Constant initConstant = getInitialValue().toConstant();
			// initConstant.lock(); // at this point we MUST be able to resolve
			// the constant.

			Bus dataSource = null;
			EndianSwapper inSwapper = null;
			EndianSwapper outSwapper = null;
			if (writers.size() > 0) {
				// Needs a RESET b/c it contains an initial value.
				// Synplicity will not pick up the initial value from
				// the declaration which is the only place it exists
				// if the register has no RESET. IDM Aug 10, 2006
				Reg reg = Reg.getConfigurableReg(Reg.REGRE, logicalId);
				reg.getResultBus().setSize(getInitWidth(), false);
				Value init = initConstant.getValueBus().getValue();
				reg.setInitialValue(init);
				reg.getClockPort().setBus(getClockPort().getPeer());
				reg.getResetPort().setBus(getResetPort().getPeer());
				reg.getInternalResetPort().setBus(getResetPort().getPeer());

				if (writers.size() == 1) {
					assert getDataPorts().size() == 2;
					reg.getEnablePort().setBus(getDataPorts().get(0).getPeer());
					reg.getDataPort().setBus(getDataPorts().get(1).getPeer());
				} else {
					if (doSimpleMerge) {
						// Merge the data by a Mux. Merge the enable
						// by an Or.
						Mux mux = new Mux(writers.size());
						Or or = new Or(writers.size());
						Iterator<Port> dataMuxPorts = mux.getGoPorts()
								.iterator();
						Iterator<Port> writeOrPorts = or.getDataPorts()
								.iterator();
						Iterator<Port> physicalPorts = getDataPorts()
								.iterator();
						for (int i = 0; i < writers.size(); i++) {
							Bus enable = physicalPorts.next().getPeer();
							Bus data = physicalPorts.next().getPeer();

							Port dataMuxGoPort = dataMuxPorts.next();
							dataMuxGoPort.setBus(enable);

							Port dataMuxDataPort = mux
									.getDataPort(dataMuxGoPort);
							// dataMuxDataPort.setBus(castOp.getResultBus());
							dataMuxDataPort.setBus(data);

							writeOrPorts.next().setBus(enable);
						}
						addComponent(mux);
						addComponent(or);

						reg.getDataPort().setBus(mux.getResultBus());
						reg.getEnablePort().setBus(or.getResultBus());
					} else {
						/*
						 * Ok... the support is really close... but there are
						 * problems with constant prop. The problems stem from
						 * the fact that I tie off the address ports to a one
						 * bit constant. Should be easy to fix (when needed). To
						 * get working this will also need to ensure that there
						 * are equal sized writer/reader lists. Code in
						 * GlobalConnector exists but is commented out (see
						 * visit(Task))
						 */
						assert false : "Not supporting multiple writers in arbitrated register case.  See comment";
						RegisterReferee referee = new RegisterReferee(
								Register.this, readers, writers);
						addComponent(referee);
						referee.connectImplementation(reg, getDataPorts());
						// The referee ends up being a feedback point b/c
						// it manages both the read and write sides of the
						// reg.
						addFeedbackPoint(reg);
						addFeedbackPoint(referee);
					}
				}

				if (!isLittleEndian && (getInitWidth() > 8)) {
					inSwapper = new EndianSwapper(getInitWidth(),
							getInitialValue().getAddressStridePolicy()
									.getStride());
					// Insert between the data port and whatever is
					// driving that data port
					inSwapper.getInputPort().setBus(reg.getDataPort().getBus());
					reg.getDataPort().setBus(inSwapper.getOutputBus());
					addComponent(inSwapper);
				}
				addComponent(reg);
				dataSource = reg.getResultBus();
			} else // Read-only, use the constant
			{
				addComponent(initConstant);
				dataSource = initConstant.getValueBus();
			}

			if (!isLittleEndian && (getInitWidth() > 8)) {
				outSwapper = new EndianSwapper(getInitWidth(),
						getInitialValue().getAddressStridePolicy().getStride());
				outSwapper.getInputPort().setBus(dataSource);
				dataSource = outSwapper.getOutputBus();
				addComponent(outSwapper);
			}

			registerOutput = makeExit(0).makeDataBus();
			registerOutput.setSize(getInitWidth(), isSigned());
			// Regardless of whether it is arbitrated, a read from a
			// register is just a wire from the reg output.
			registerOutput.getPeer().setBus(dataSource);
		}

		@Override
		public String toString() {
			// String oneBits = (oneBitRegs == null ?
			// "null":Integer.toString(oneBitRegs.length));
			String busWidth = (registerOutput == null ? "null" : Integer
					.toString(registerOutput.getWidth()));
			String r = "Register.Physical " + Integer.toHexString(hashCode())
					+ " result bus width: " + busWidth;
			return r;
		}

		public Bus getRegisterOutput() {
			return registerOutput;
		}

		@Override
		public boolean isOpaque() {
			return true;
		}

		@Override
		public void accept(Visitor v) {
			// assert false : "Nobody should visit this directly. " +
			// getClass(); // nobody should be visiting this component directly
			throw new UnexpectedVisitationException();
		}

		@Override
		public boolean removeDataBus(Bus bus) {
			assert false : "remove data bus not supported on " + this;
			return false;
		}

		@Override
		public boolean removeDataPort(Port port) {
			assert false : "remove data port not supported on " + this;
			return false;
		}
	}

} // class Register()
