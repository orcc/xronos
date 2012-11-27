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

import java.util.Set;

import org.xronos.openforge.verilog.mapping.MappedModule;


/**
 * An IncludeSpecifier specifies external Verilog which must be included for
 * synthesis or simulation.
 */
public interface MappedModuleSpecifier {

	/**
	 * Provides the Set of MappedModule objects need.
	 */
	public Set<MappedModule> getMappedModules();

} // interface MappedSpecifier
