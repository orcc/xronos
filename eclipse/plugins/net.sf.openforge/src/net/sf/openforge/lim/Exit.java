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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDSourceInfo;

/**
 * An Exit is a group of Buses that represent an exit condition from an
 * Executable. This includes the control signal as well as all the data values
 * that are output at that point. A given {@link Component} may have multiple
 * Exits.
 * <P>
 * Each Exit has an identifier called a {@link Exit.Tag Tag}. A {@link Exit.Tag
 * Tag} consists of an Exit type (one of {@link Exit#DONE}, {@link Exit#BREAK},
 * {@link Exit#RETURN}, {@link Exit#CONTINUE}, or {@link Exit#EXCEPTION}) and an
 * optional label String. A {@link Component} may have only one Exit with a
 * given {@link Exit.Tag Tag}. When added, a new {@link Exit} will replace any
 * existing {@link Exit} with the same {@link Exit.Tag Tag}; if assertions are
 * turned on, this condition will also thrown an {@link AssertionError}.
 * 
 * @author Stephen Edwards
 * @version $Id: Exit.java 109 2006-02-24 18:10:34Z imiller $
 */
public class Exit extends ID implements LatencyKey {

	/** The identifier tag of exit */
	private Tag tag;

	/** The component to which this exit belongs */
	private Component owner;

	/** The internal connector for this exit */
	private OutBuf peer = null;

	/** The output which asserts this exit */
	private Bus doneBus;

	/** Collection of data Buses */
	private List<Bus> dataBuses;

	/** The latency from the start of the owner to this Exit */
	private Latency latency;

	/**
	 * A Set of Entry objects identifying which entries list this Exit as their
	 * 'drivingExit'
	 */
	private Set<Entry> drivenEntries;

	public boolean removeDataBus(Bus bus) {
		boolean success = dataBuses.remove(bus);
		if (success) {
			Port port = bus.getPeer();
			if (port != null) {
				bus.setPeer(null);
				port.setPeer(null);
				port.getOwner().removeDataPort(port);
			}
		}
		return success;
	}

	/**
	 * A type identifier for an Exit.
	 * 
	 * @version $Id: Exit.java 109 2006-02-24 18:10:34Z imiller $
	 */
	public final static class Type {

		static final int ID_DONE = 0;
		static final int ID_RETURN = 1;
		static final int ID_BREAK = 2;
		static final int ID_CONTINUE = 3;
		static final int ID_EXCEPTION = 4;
		static final int ID_SIDEBAND = 5;

		/** The kind of exit */
		private int id;

		private Type(int id) {
			this.id = id;
		}

		private int getId() {
			return id;
		}

		@Override
		public String toString() {
			switch (getId()) {
			case ID_DONE:
				return "DONE";
			case ID_RETURN:
				return "RETURN";
			case ID_BREAK:
				return "BREAK";
			case ID_CONTINUE:
				return "CONTINUE";
			case ID_EXCEPTION:
				return "EXCEPTION";
			case ID_SIDEBAND:
				return "SIDEBAND";
			default:
				assert false : "Unknown id: " + getId();
				return null;
			}
		}
	}

	/**
	 * The type of Exit that represents the normal completion of a module, for
	 * example, falling off the end of a sequence
	 */
	public static final Type DONE = new Type(Type.ID_DONE);

	/** The type of Exit that represents a break out of the current statement */
	public static final Type BREAK = new Type(Type.ID_BREAK);

	/** The type of Exit that represents a return from a procedure */
	public static final Type RETURN = new Type(Type.ID_RETURN);

	/** The type of Exit that represents a continue of the current statement */
	public static final Type CONTINUE = new Type(Type.ID_CONTINUE);

	/** The type of Exit that represents an exceptional condition */
	public static final Type EXCEPTION = new Type(Type.ID_EXCEPTION);

	/** The type of Exit that represents sideband (global connections) data */
	public static final Type SIDEBAND = new Type(Type.ID_SIDEBAND);

	/**
	 * A unique, hashable identifier for an {@link Exit}.
	 */
	public static class Tag {
		/** The exit type */
		private Type type;

		/** The optional exit label */
		private String label;

		public static final String NOLABEL = "";

		public String getLabel() {
			return label;
		}

		public Type getType() {
			return type;
		}

		@Override
		public int hashCode() {
			return type.hashCode() + (label == null ? 0 : label.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Tag) {
				Tag tag = (Tag) obj;
				return (tag.getType() == getType())
						&& tag.getLabel().equals(getLabel());
			}
			return false;
		}

		@Override
		public String toString() {
			return type + (label.equals(NOLABEL) ? "" : (":" + label));
		}

		private Tag(Type type, String label) {
			this.type = type;
			this.label = label;
		}

		private Tag(Type type) {
			this(type, NOLABEL);
		}
	}

	/**
	 * Gets a labeled Exit tag.
	 * 
	 * @param type
	 *            the exit type: {@link Exit#DONE}, {@link Exit#BREAK},
	 *            {@link Exit#RETURN}, {@link Exit#CONTINUE},
	 *            {@link Exit#SIDEBAND}, or {@link Exit#EXCEPTION}
	 * @param label
	 *            the label of the tag
	 * @return a labeled exit tag
	 */
	public static Tag getTag(Type type, String label) {
		return new Tag(type, label);
	}

	/**
	 * Gets an unlabeled Exit tag.
	 * 
	 * @param type
	 *            the exit type: {@link Exit#DONE}, {@link Exit#BREAK},
	 *            {@link Exit#RETURN}, {@link Exit#CONTINUE},
	 *            {@link Exit#SIDEBAND}, or {@link Exit#EXCEPTION}
	 * @return an unlabeled Exit tag
	 */
	public static Exit.Tag getTag(Exit.Type type) {
		return getTag(type, Exit.Tag.NOLABEL);
	}

	/**
	 * Overrides IDNameAdaptor.getIDSourceInfo()
	 * 
	 * @return this exit owner id source info
	 */
	@Override
	public IDSourceInfo getIDSourceInfo() {
		return getOwner().getIDSourceInfo();
	}

	/**
	 * Gets the done output.
	 * 
	 * @return an output which is asserted when this exit is activated
	 */
	public Bus getDoneBus() {
		return doneBus;
	}

	/**
	 * Gets the data outputs.
	 * 
	 * @return the data Buses which are valid when the done Bus is asserted
	 */
	public List<Bus> getDataBuses() {
		return dataBuses;
	}

	/**
	 * Gets all outputs. These are NOT is any order;
	 * 
	 * @return a collection of Buses.
	 */
	public Collection<Bus> getBuses() {
		List<Bus> list = new ArrayList<Bus>(getDataBuses().size() + 1);
		list.add(getDoneBus());
		list.addAll(getDataBuses());
		return list;
	}

	/**
	 * Gets the latency from the start of the Executable until the activation of
	 * this exit.
	 */
	public Latency getLatency() {
		return latency;
	}

	/**
	 * Sets the latency from the start of the Executable until the activation of
	 * this exit.
	 * 
	 * @param latency
	 *            the latency value
	 */
	public void setLatency(Latency latency) {
		this.latency = latency;
	}

	/**
	 * Gets the owner of this exit's done bus, which is considered to be the
	 * owner of this exit.
	 */
	public Component getOwner() {
		return owner;
	}

	/**
	 * Gets the Tag of this exit.
	 * 
	 * @return the identifying tag of this exit
	 */
	public Tag getTag() {
		return tag;
	}

	/**
	 * Gets the label, if any, that further describes the {@link Type} of this
	 * Exit. For example, a {@link Exit#BREAK} Exit may have an associated label
	 * that is dervied from a labeled break statement in Java.
	 * 
	 * @return the {@link Type} label
	 */
	public String getLabel() {
		return getTag().getLabel();
	}

	/**
	 * Gets the Component inside this Exit's owner which connects it internally.
	 * 
	 * @return the peer component, or null if there is none
	 */
	public OutBuf getPeer() {
		return peer;
	}

	/**
	 * Makes a one-bit wide unsigned bus.
	 */
	public Bus makeOneBitBus() {
		Bus b = makeDataBus();
		b.setSize(1, false);
		return b;
	}

	/**
	 * Makes and returns a new data bus; if there is an internal peer component,
	 * it receives a corresponding data port.
	 * 
	 * @return the new data Bus
	 */
	public Bus makeDataBus() {
		return makeDataBus(Component.NORMAL);
	}

	/**
	 * makes and returns a new data bus, and tags the bus (and port if there is
	 * an internal peer)
	 * 
	 * @param tag
	 *            type of bus
	 */
	public Bus makeDataBus(Component.Type type) {
		Bus bus = new Bus(this);
		bus.setUsed(true);
		dataBuses.add(bus);
		if (peer != null) {
			Port port = peer.makeDataPort(type);
			port.setUsed(true);
			port.setPeer(bus);
			bus.setPeer(port);
		}
		bus.tag(type);
		return bus;
	}

	void moveDataBusLocation(Bus bus, int index) {
		assert dataBuses.contains(bus) : "Exit doesn't know bus";
		dataBuses.remove(bus);
		dataBuses.add(index, bus);
	}

	/**
	 * Tests whether this a main exit of the owner.
	 */
	public boolean isMain() {
		// return getOwner().getMainExit() == this;
		return getOwner().getExit(Exit.DONE) == this;
	}

	/**
	 * Specifies that the given entry is one which lists this exit as it's
	 * 'drivingExit'.
	 * 
	 * @param entry
	 *            a value of type 'Entry'
	 */
	void drives(Entry entry) {
		drivenEntries.add(entry);
	}

	/**
	 * Removes the given entry from the set of drivenEntries.
	 * 
	 * @param entry
	 *            a value of type 'Entry'
	 */
	void removeEntry(Entry entry) {
		assert drivenEntries.contains(entry) : "unknown entry";
		drivenEntries.remove(entry);
	}

	public void disconnect() {
		for (Bus bus : getBuses()) {
			bus.clearDependents();
			bus.disconnect();
		}

		/*
		 * FIXME: The next line doesn't make sense. What does it mean?
		 */
		// assert this.drivenEntries.size() == 0 :
		// "Disconnecting an exit that is still listed as the driving exit of something. "
		// + this + " drives " + drivenEntries;

		for (Iterator<Entry> entryIter = (new LinkedList<Entry>(drivenEntries))
				.iterator(); entryIter.hasNext();) {
			entryIter.next().setDrivingExit(null);
		}
	}

	public Set<Entry> getDrivenEntries() {
		return Collections.unmodifiableSet(drivenEntries);
	}

	/**
	 * Constructs a new Exit with a done bus that is marked as not used.
	 * 
	 * @param owner
	 *            the owner of the exit
	 * @param dataCount
	 *            the number of data buses to create
	 * @param type
	 *            the type of exit
	 * @param label
	 *            the label of the exit
	 */
	public Exit(Component owner, int dataCount, Type type, String label) {
		this.owner = owner;
		doneBus = new Bus(this);
		doneBus.setSize(1, false);
		doneBus.setUsed(false);
		dataBuses = new ArrayList<Bus>(dataCount);
		drivenEntries = new HashSet<Entry>();
		for (int i = 0; i < dataCount; i++) {
			Bus b = new Bus(this);
			b.setUsed(true); // ABK - data buses should always be used
			dataBuses.add(b);
		}

		tag = new Tag(type, label);
	}

	/**
	 * Constructs a new Exit.
	 * 
	 * @param owner
	 *            the owner of the exit
	 * @param dataCount
	 *            the number of data buses to create
	 * @param type
	 *            the type of exit
	 */
	Exit(Component owner, int dataCount, Type type) {
		this(owner, dataCount, type, Tag.NOLABEL);
	}

	/**
	 * Constructs a new {@link Exit#DONE} Exit.
	 * 
	 * @param owner
	 *            the owner of the exit
	 * @param dataCount
	 *            the number of data buses to create
	 */
	Exit(Component owner, int dataCount) {
		this(owner, dataCount, DONE);
	}

	/**
	 * Constructs a new {@link Module} exit. A peer {@link OutBuf} will also be
	 * created inside the {@link Module}.
	 * 
	 * @param owner
	 *            the owner of the exit
	 * @param dataCount
	 *            the number of data buses to create
	 * @param type
	 *            the type of exit
	 * @param label
	 *            the label of the exit
	 */
	Exit(Module owner, int dataCount, Type type, String label) {
		this((Component) owner, dataCount);
		peer = new OutBuf(owner, this);
		owner.addComponent(peer);

		doneBus.setPeer(peer.getGoPort());
		peer.getGoPort().setPeer(doneBus);
		Iterator<Bus> busIter = dataBuses.iterator();
		Iterator<Port> portIter = peer.getDataPorts().iterator();
		while (busIter.hasNext()) {
			Bus bus = busIter.next();
			Port port = portIter.next();
			bus.setPeer(port);
			port.setPeer(bus);
		}

		tag = new Tag(type, label);
	}

	/**
	 * Constructs a new {@link Module} exit. A peer {@link OutBuf} will also be
	 * created inside the {@link Module}.
	 * 
	 * @param owner
	 *            the owner of the exit
	 * @param dataCount
	 *            the number of data buses to create
	 * @param type
	 *            the type of exit
	 */
	Exit(Module owner, int dataCount, Type type) {
		this(owner, dataCount, type, Tag.NOLABEL);
	}

	/**
	 * Constructs a new {@link Exit#DONE} {@link Module} exit. A peer
	 * {@link OutBuf} will also be created inside the {@link Module}.
	 * 
	 * @param owner
	 *            the owner of the exit
	 * @param dataCount
	 *            the number of data buses to create
	 */
	Exit(Module owner, int dataCount) {
		this(owner, dataCount, DONE);
	}

	@Override
	public String toString() {
		String ret = super.toString();
		ret = ret.replaceAll("net.sf.openforge.", "");
		ret += "<" + getTag() + ">";
		return ret;
	}

	/**
	 * Copies the primitive attributes of a given Exit and its {@link Bus Buses}
	 * to this Exit and its {@link Bus Buses}.
	 */
	void copyAttributes(Exit exit) {
		setLatency(exit.getLatency());

		getDoneBus().copyAttributes(exit.getDoneBus());
		Iterator<Bus> exitIter = exit.getDataBuses().iterator();
		Iterator<Bus> thisIter = getDataBuses().iterator();
		while (thisIter.hasNext()) {
			assert exitIter.hasNext();
			final Bus thisBus = thisIter.next();
			final Bus exitBus = exitIter.next();
			thisBus.copyAttributes(exitBus);
		}

		ID.copy(exit, this);
	}

	public static class ClearDrivingExitVisitor extends FilteredVisitor {
		@Override
		public void filterAny(Component c) {
			for (Iterator<Entry> it = c.getEntries().iterator(); it.hasNext();) {
				Entry e = it.next();
				e.setDrivingExit(null);
			}
			super.filterAny(c);
		}
	}
}
