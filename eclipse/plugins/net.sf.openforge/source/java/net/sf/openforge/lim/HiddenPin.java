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

import java.util.Collection;
import java.util.Collections;

/**
 * A pin which is used in the internal logic of a {@link Design} but should not
 * appear as in input or output of the <code>Design</code>.
 * 
 * @author Andreas Kollegger
 * @version $Id: HiddenPin.java 2 2005-06-09 20:00:48Z imiller $
 */
public class HiddenPin extends InputPin {
	private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

	private Port port;
	private int width;

	/**
	 * 
	 * @param width
	 * @deprecated
	 */
	public HiddenPin(int width) {
		this(width, false);
	}

	/**
	 * Constructs a new HiddenPin. The pin may have a different width than the
	 * it's bus, though the signedness must match. Optimization may have
	 * downsized the data path leading from the Pin, but the pin may have an
	 * explicit width requirement, even if all the bits aren't used internally.
	 * 
	 * @param width
	 * @param isSigned
	 */
	public HiddenPin(int width, boolean isSigned) {
		super(width, isSigned);
		this.width = width;
		this.port = makeDataPort();
		this.port.setSize(width, isSigned);
	}

	public Collection getPinBufs() {
		return Collections.EMPTY_LIST;
	}

	public Port getPort() {
		return port;
	}

	public Module getPhysicalComponent() {
		return null;
	}

	public boolean hasPhysicalComponent() {
		return getPhysicalComponent() != null;
	}

	/**
	 * Returns a full copy of this Pin
	 * 
	 * @return an HiddenPin object.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
