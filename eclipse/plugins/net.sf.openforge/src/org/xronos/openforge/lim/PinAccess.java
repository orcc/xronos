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

package org.xronos.openforge.lim;

/**
 * PinAccess factors out functionality that is common among all accesses to pins
 * such as address (pin object reference) port, done bus and methods that
 * identify whether the node uses go, done, etc.
 * <p>
 * <b>Note, the targetted Resource (getResource() method) will be NULL until the
 * PinAllocator runs after scheduling. This is because we have the restriction
 * that ALL pins accesses must resolve down to a single pin, however this
 * resolution may depend on the running of several passes of several
 * optimizations. </b>
 * 
 * <p>
 * Created: Mon Sep 23 16:13:23 2002
 * 
 * @author abk, last modified by $Author: imiller $
 * @version $Id: PinAccess.java 88 2006-01-11 22:39:52Z imiller $
 */
public abstract class PinAccess extends Access {

	private boolean isFloatType = false;
	private boolean isDouble = false;

	public PinAccess(int portCount) {
		super(null, portCount, false);
	}

	public Port getAddressPort() {
		return getDataPorts().get(0);
	}

	public Bus getDoneBus() {
		return getExit(Exit.DONE).getDoneBus();
	}

	@Override
	public boolean isSequencingPoint() {
		throw new UnsupportedOperationException(
				"PinAccess is obsolete.  Access to unsupported method");
	}

	public abstract Module getPhysicalComponent();

	public boolean hasPhysicalComponent() {
		return getPhysicalComponent() != null;
	}

	public void setFloat(boolean isFloat) {
		isFloatType = isFloat;
	}

	/**
	 * Returns true if this PinRead was declared a floating point read.
	 */
	@Override
	public boolean isFloat() {
		return isFloatType;
	}

	/**
	 * Used to identify this PinAccess as a double precision floating point
	 * access.
	 */
	public void setDoublePrecisionFloat(boolean value) {
		isDouble = value;
	}

	/**
	 * returns true if this PinAccess has been annotated as isFloat AND is
	 * double precision.
	 */
	public boolean isDoublePrecisionFloat() {
		return isFloat() && isDouble;
	}

}// PinAccess
