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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.util.XilinxDevice;

/**
 * Base brokering class for getting mapping information for primitives. One
 * instance is created for the entire JVM.
 * <P>
 * For a specific primitive/part pair, a number of data points are captured.
 * Simulation and Synthesis Include paths, and the Module name are fairly
 * obvious.
 * 
 * @author "C. Schanck" <cschanck@cschanck>
 * @version 1.0
 * @since 1.0
 */
public class PrimitiveMapper {

	public static final String UNKNOWN_PART = "Unknown Part";
	public static final String UNKNOWN_XILINX_PART = "Xilinx Part";

	/** The one and only instance */
	private static PrimitiveMapper instance = null;

	// Map of primitive+"/"+part :: MappedModule
	private Map<String, PrimitiveMappedModule> mappingStore = new HashMap<String, PrimitiveMappedModule>();

	/**
	 * Gets the single global instance of PrimitiveMapper.
	 * 
	 * @return the single instance, creating it if necessary
	 * @exception IOException
	 *                XXX: ???
	 * @exception InvalidPreferencesFormatException
	 *                XXX: ???
	 * @exception BackingStoreException
	 *                XXX: ???
	 */
	public static PrimitiveMapper getInstance() {
		if (instance == null) {
			instance = new PrimitiveMapper();
		}
		return instance;
	}

	/**
	 * Create an instance based on the info in the filename given.
	 * 
	 * @exception IOException
	 *                if an error occurs
	 * @exception InvalidPreferencesFormatException
	 *                if an error occurs
	 * @exception BackingStoreException
	 *                if an error occurs
	 */
	private PrimitiveMapper() {
		mappingStore.clear();
		new BootstrapMapper(mappingStore);
	}

	public final PrimitiveMappedModule getMappedModule(String primitiveName,
			XilinxDevice xd) {
		String id;
		PrimitiveMappedModule pmm = null;
		// if a xilinx part
		if (xd.isXilinxDevice()) {
			// check full device
			id = primitiveName + "/" + xd.getFullDeviceName();
			pmm = makeMappedModule(id);
			if (pmm != null) {
				mappingStore.put(primitiveName, pmm);
				return pmm;
			}

			// check device with no temp
			id = primitiveName + "/" + xd.getFullDeviceNameNoTemp();
			pmm = makeMappedModule(id);
			if (pmm != null) {
				mappingStore.put(primitiveName, pmm);
				return pmm;
			}

			// check just the family
			id = primitiveName + "/" + xd.getFamilyAsString();
			pmm = makeMappedModule(id);
			if (pmm != null) {
				mappingStore.put(primitiveName, pmm);
				return pmm;
			}

			// give up -- any xilinx part
			id = primitiveName + "/" + UNKNOWN_XILINX_PART;
			pmm = makeMappedModule(id);
			if (pmm != null) {
				mappingStore.put(primitiveName, pmm);
				return pmm;
			}
		}

		// check plain verilog
		id = primitiveName + "/" + UNKNOWN_PART;
		pmm = makeMappedModule(id);
		if (pmm != null) {
			mappingStore.put(primitiveName, pmm);
			return pmm;
		}

		// nope!
		return null;
	}

	public final PrimitiveMappedModule getMappedModule(String primitiveName) {
		PrimitiveMappedModule pmm;
		if (mappingStore.containsKey(primitiveName)) {
			return mappingStore.get(primitiveName);
		}
		XilinxDevice xd = EngineThread.getGenericJob().getPart(
				CodeLabel.UNSCOPED);
		pmm = getMappedModule(primitiveName, xd);
		if (pmm != null) {
			return pmm;
		}

		XilinxDevice defaultDevice = EngineThread.getGenericJob()
				.getDefaultPart();
		pmm = getMappedModule(primitiveName, defaultDevice);
		if (pmm != null) {
			Option op = EngineThread.getGenericJob().getOption(
					OptionRegistry.XILINX_PART);
			EngineThread.getGenericJob().warn(
					"No implementation found for " + primitiveName
							+ " for device name: "
							+ op.getValue(CodeLabel.UNSCOPED).toString()
							+ " Using device " + defaultDevice);
			return pmm;
		}

		// nope!
		return null;
	}

	public boolean exists(String partString) {
		return mappingStore.containsKey(partString);
	}

	private PrimitiveMappedModule makeMappedModule(String id) {
		// Job.verbose("Looking for primitive mapping for: "+id);
		if (exists(id)) {
			EngineThread.getGenericJob().verbose(
					"Primitive mapping found: " + id);
			return mappingStore.get(id);
		}
		return null;
	}

}
