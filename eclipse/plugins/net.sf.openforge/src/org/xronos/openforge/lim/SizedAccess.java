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

import java.util.List;

/**
 * SizedAccess is any Access which also identifies the size of the access at
 * runtime by providing an encoded size signal as one parameter. The value of
 * this size port is defined by the following encoding:
 * <table border=1>
 * <tr>
 * <th>encoded value</th>
 * <th>number of bytes of access</th>
 * </tr>
 * <tr>
 * <td>0</td>
 * <td>4</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>8</td>
 * </tr>
 * </table>
 * 
 * <p>
 * Created: Mon Feb 10 15:52:57 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SizedAccess.java 70 2005-12-01 17:43:11Z imiller $
 */
public abstract class SizedAccess extends Access {

	private Port sizePort;

	public SizedAccess(Resource resource, int dataPortCount, boolean isVolatile) {
		super(resource, dataPortCount, isVolatile);
		sizePort = makeDataPort();
	}

	/**
	 * Returns the size port, used to provide the encoded number of bytes to
	 * this access.
	 */
	public Port getSizePort() {
		return sizePort;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		SizedAccess clone = (SizedAccess) super.clone();
		List<Port> ports = getPorts();
		List<Port> clonePorts = clone.getPorts();
		clone.sizePort = clonePorts.get(ports.indexOf(sizePort));
		return clone;
	}

}// SizedAccess
