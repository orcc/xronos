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
package net.sf.openforge.verilog.pattern;

import java.util.Set;

import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.mapping.memory.VerilogMemory;
import net.sf.openforge.verilog.model.Module;

/**
 * A MemoryModule is a Verilog Module of a {@link VerilogMemory}.
 * <P>
 * 
 * Created: Thr Jul 27 11:26:43 2002
 * 
 * @author cwu
 * @version $Id: MemoryModule.java 2 2005-06-09 20:00:48Z imiller $
 */

public class MemoryModule extends Module implements MappedModuleSpecifier {

	private Set<MappedModule> mappedModules;

	/**
	 * Composes a <code>MemoryModule</code> based on a module name and a set of
	 * inferred xilinx memory modules.
	 * 
	 * @param memory
	 *            the Memory which is being instantiated
	 */
	public MemoryModule(String moduleName, Set<MappedModule> mappedModules) {
		super(moduleName);
		this.mappedModules = mappedModules;
	} // MemoryModule

	/**
	 * Gets the instantiated memory module name
	 * 
	 * @return A String of memory module name
	 */
	public Set<MappedModule> getMappedModules() {
		return mappedModules;
	}

} // class MemoryModule
