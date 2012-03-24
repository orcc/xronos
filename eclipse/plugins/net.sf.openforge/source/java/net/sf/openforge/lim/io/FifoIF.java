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

package net.sf.openforge.lim.io;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.StateHolder;

/**
 * FifoIF is a class representing an atomic FIFO interface to a Forged design.
 * The class ties together all the pins necessary to interact with fifo
 * interface as well as maintaining important characteristics of the interface
 * such as data path width.
 * 
 * 
 * <p>
 * Created: Mon Dec 15 12:40:24 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoIF.java 538 2007-11-21 06:22:39Z imiller $
 */
public abstract class FifoIF implements Referenceable, StateHolder {

	public static final int TYPE_FSL_FIFO = 1;
	public static final int TYPE_ACTOR_QUEUE = 2;

	private int interfaceWidth = -1;

	/**
	 * The set of defined SimplePin objects for this interface. We use a linked
	 * hashset just as a convenience to the user so that the interface will
	 * always be generated in the same order.
	 */
	private Set<SimplePin> pins = new LinkedHashSet<SimplePin>();

	/**
	 * Constructs a new FifoIF (fifo interface) with the specified byte width
	 * for the data path.
	 * 
	 * @param width
	 *            a positive int, the byte width of the data path.
	 * @throws IllegalArgumentException
	 *             if width is not greater than zero
	 */
	protected FifoIF(int width) {
		if (width <= 0) {
			throw new IllegalArgumentException(
					"Illegal byte width for fifo IF " + width);
		}

		interfaceWidth = width;
	}

	/**
	 * Returns a value which is one of the types represented by the type fields
	 * of this class.
	 */
	public abstract int getType();

	/**
	 * Returns a positive integer value indicating the BIT width of the data
	 * path for this FIFO interface in bytes.
	 * 
	 * @return a non zero, positive int.
	 */
	public int getWidth() {
		return interfaceWidth;
	}

	/**
	 * Returns a Collection of {@link SimplePin} objects representing the
	 * complete set of pins necessary to implement this fifo inteface.
	 * 
	 * @return a 'Collection' of {@link SimplePin} objects.
	 */
	public Collection<SimplePin> getPins() {
		return pins;
	}

	/**
	 * Returns a subset of {@link #getPins} that are the output pins of the
	 * interface.
	 */
	public abstract Collection<SimplePin> getOutputPins();

	/**
	 * Adds the specified {@link SimplePin} to this interface.
	 */
	protected void addPin(SimplePin pin) {
		pins.add(pin);
	}

	/**
	 * Returns a {@link FifoAccess} object that is used to either obtain data
	 * from this FifoIF (if this is an input FIFO interface) or to write data to
	 * this FifiIF (if this is an output FIFO interface)
	 */
	public abstract FifoAccess getAccess();

	/**
	 * Returns true if the data port of this fifo interface is an input to the
	 * generated core.
	 */
	public abstract boolean isInput();

	/**
	 * Returns a String, based upon the supplied port name, that is to be used
	 * as the base name for all pins comprising this fifo interface.
	 */
	protected abstract String buildPortBaseName(String portName);

	/**
	 * Returns the String that is the base name of this interface.
	 */
	public abstract String getPortBaseName();

	// ////////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////////
	//
	// Implementation of the Referenceable interface
	//
	// ////////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////////

	/**
	 * Tests the referencer types and then returns 1 or 0 depending on the types
	 * of accesses.
	 * 
	 * @param from
	 *            the prior accessor in source document order.
	 * @param to
	 *            the latter accessor in source document order.
	 */
	@Override
	public int getSpacing(Referencer from, Referencer to) {
		if (from instanceof FifoRead)
			return 1;
		else if (from instanceof FifoWrite)
			return 1;
		else
			throw new IllegalArgumentException("Source access to " + this
					+ " is of unknown type " + from.getClass());
	}

	/**
	 * Returns -1 indicating that the referencers must be scheduled using the
	 * default DONE to GO spacing.
	 */
	@Override
	public int getGoSpacing(Referencer from, Referencer to) {
		return -1;
	}

}// FifoIF
