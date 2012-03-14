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

/**
 * Gateway is the superclass for all types of global gateways.
 * 
 * <p>
 * Created: Tue Nov 19 11:00:40 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: Gateway.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Gateway extends Module {
	private static final String _RCS_ = "$Rev: 2 $";

	/** the associated resource for this Gateway */
	private Storage resource;

	public Gateway(Storage resource) {
		this.resource = resource;
	}

	/**
	 * Throws an exception, replacement in this class not supported.
	 */
	public boolean replaceComponent(Component removed, Component inserted) {
		throw new UnsupportedOperationException("Cannot replace components in "
				+ getClass());
	}

	/**
	 * returns true.
	 */
	public boolean isGateway() {
		return true;
	}

	/**
	 * Returns the {@link Storage} object that this instance is a gateway to.
	 */
	public Storage getResource() {
		return resource;
	}

	/**
	 * Throws a CloneNotSupportedExcpetion or asserts false since there is, as
	 * yet, no implementation.
	 * 
	 * @return a value of type 'Object'
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	public Object clone() throws CloneNotSupportedException {
		assert false : "tbd";

		throw new CloneNotSupportedException(
				"Clone not implemented for RegisterGateway yet");
	}

	public boolean removeDataBus(Bus bus) {
		assert false : "remove data bus not supported on " + this;
		return false;
	}

	public boolean removeDataPort(Port port) {
		assert false : "remove data port not supported on " + this;
		return false;
	}

}// Gateway
