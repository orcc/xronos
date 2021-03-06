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
package org.xronos.openforge.verilog.pattern;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.verilog.model.ModuleInstance;
import org.xronos.openforge.verilog.model.Net;


/**
 * The MemoryInstance is a Verilog statement of a Forge memory instantiation.
 * <P>
 * 
 * Created: Tue Jul 27 15:01:08 2002
 * 
 * @author cwu
 * @version $Id: MemoryInstance.java 2 2005-06-09 20:00:48Z imiller $
 */

public class MemoryInstance extends ModuleInstance implements ForgePattern {

	private Set<Net> producedNets = new HashSet<Net>();

	/**
	 * Composes a MemoryInstance based on the name of instantiated memory module
	 * and the name of instantiation.
	 * 
	 * @param moduleName
	 *            the name of a memory module which is being instantiated
	 * @param instanceName
	 *            the name of a memory module instantiation
	 */
	public MemoryInstance(String moduleName, String instanceName) {
		super(moduleName, instanceName);
	} // MemoryInstance

	/**
	 * Provides the collection of Nets which this statement of verilog uses as
	 * input signals.
	 */
	@Override
	public Collection<Net> getConsumedNets() {
		return Collections.emptyList();
	};

	/**
	 * Provides the collection of Nets which this statement of verilog produces
	 * as output signals. These are any signals which need to be declared, even
	 * if the statement itself also consumes them.
	 */
	@Override
	public Collection<Net> getProducedNets() {
		// return Collections.EMPTY_LIST;
		return producedNets;
	}

	public void addProducedNet(Net net) {
		producedNets.add(net);
	}

} // class MemoryInstance
