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

package net.sf.openforge.verilog.mapping;

public class MappedModule {

	private String moduleName;
	private String simInclude;
	private String synthInclude;

	public MappedModule(String moduleName, String simInclude,
			String synthInclude) {
		this.moduleName = moduleName;
		this.simInclude = simInclude;
		this.synthInclude = synthInclude;

		assert moduleName != null;
		assert simInclude != null;
		assert synthInclude != null;
	}

	public String getSimInclude() {
		return simInclude;
	}

	public String getSynthInclude() {
		return synthInclude;
	}

	public String getModuleName() {
		return moduleName;
	}

	@Override
	public int hashCode() {
		return moduleName.hashCode();
	}

	public boolean equals(MappedModule im) {
		return im.getModuleName().equals(moduleName);
	}
}
