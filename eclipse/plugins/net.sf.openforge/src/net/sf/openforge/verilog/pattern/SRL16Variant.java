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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.lim.SRL16;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.mapping.MappingName;
import net.sf.openforge.verilog.mapping.PrimitiveMappedModule;
import net.sf.openforge.verilog.model.HexConstant;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.Wire;

/**
 * There are 3 possible SRL16 variants, all of which are used via the primitive
 * mapper
 * 
 * 
 * @version $Id: SRL16Variant.java 2 2005-06-09 20:00:48Z imiller $
 */

public class SRL16Variant extends StatementBlock implements ForgePattern,
		MappedModuleSpecifier {

	private Set<Net> consumed_nets = new HashSet<Net>();

	private Net result_wire;

	private PortWire enable_wire;
	private PortWire data_wire;
	private PortWire clock_wire;

	private Set<MappedModule> mappedModules = new HashSet<MappedModule>();

	public SRL16Variant(SRL16 srl_16) {
		result_wire = NetFactory.makeNet(srl_16.getResultBus());

		GenericJob gj = EngineThread.getGenericJob();

		// get the component we need
		MappingName mappingName = getPrimitiveComponent(srl_16);
		assert (mappingName != null);

		if (mappingName == null) {
			gj.info("Unable to map: " + ID.showLogical(srl_16));
			return;
		}

		// get the project
		assert (gj != null);

		PrimitiveMappedModule pmm = gj.getPrimitiveMapper().getMappedModule(
				mappingName.getName());
		if (pmm == null) {
			gj.info("No part found for: " + mappingName);
			return;
		}

		mappedModules.add(pmm);

		unslice(srl_16, pmm);
	}

	private void unslice(SRL16 srl_16, PrimitiveMappedModule pmm) {
		boolean isOneBit = (data_wire.getWidth() == 1);
		// create as many module instantiations as you have widths
		for (int i = 0; i < data_wire.getWidth(); i++) {
			// create this instance
			ModuleInstance mi;
			if (isOneBit) {
				mi = new ModuleInstance(pmm.getModuleName(),
						ID.toVerilogIdentifier(ID.showLogical(srl_16)));
				// first connect the input and out -- they must be there
				mi.connect(new Wire("D", 1), data_wire);
				mi.connect(new Wire("Q", 1), result_wire);
			} else {
				mi = new ModuleInstance(pmm.getModuleName(),
						ID.toVerilogIdentifier(ID.showLogical(srl_16)
								+ "_slice" + i));
				// first connect the input and out -- they must be there
				mi.connect(new Wire("D", 1), data_wire.getBitSelect(i));
				mi.connect(new Wire("Q", 1), result_wire.getBitSelect(i));
			}

			// now the singletons ...
			mi.connect(new Wire("CLK", 1), clock_wire); // clock
			final int stages = srl_16.getStages() - 1;
			assert stages >= 0 && stages <= 15 : "Illegal number of stages set for SRL16 "
					+ (stages + 1);
			mi.connect(new Wire("A0", 1), new HexNumber(new HexConstant(
					stages & 0x1, 1)));
			mi.connect(new Wire("A1", 1), new HexNumber(new HexConstant(
					(stages >>> 1) & 0x1, 1)));
			mi.connect(new Wire("A2", 1), new HexNumber(new HexConstant(
					(stages >>> 2) & 0x1, 1)));
			mi.connect(new Wire("A3", 1), new HexNumber(new HexConstant(
					(stages >>> 3) & 0x1, 1)));

			switch (srl_16.getType()) {
			case SRL16.SRL16E:
				mi.connect(new Wire("CE", 1), enable_wire); // enable
				break;
			default:
				break;
			}
			add(mi);
		}
	}

	private MappingName getPrimitiveComponent(SRL16 srl_16) {
		// ok, let's determine type... and handle the nets
		// do some asserts for fun ...
		// Build all the wires here, but don't add them to 'consumed'
		// unless they are actually used by the SRL16 type.
		if (srl_16.getInDataPort().isUsed())
			data_wire = new PortWire(srl_16.getInDataPort());
		if (srl_16.getClockPort().isUsed())
			clock_wire = new PortWire(srl_16.getClockPort());
		if (srl_16.getEnablePort().isUsed())
			enable_wire = new PortWire(srl_16.getEnablePort());

		assert (srl_16.getInDataPort().isUsed());
		assert (srl_16.getInDataPort().getValue() != null) : "unresolved data port value of SRL16 "
				+ srl_16.toString() + " owned by " + srl_16.getOwner();
		consumed_nets.add(data_wire);

		assert (srl_16.getClockPort().isUsed());
		assert (srl_16.getClockPort().getValue() != null) : "unresolved clock port value of SRL16 "
				+ srl_16.toString() + " owned by " + srl_16.getOwner();
		consumed_nets.add(clock_wire);

		switch (srl_16.getType()) {
		case SRL16.SRL16E:

			assert (srl_16.getEnablePort().isUsed());
			assert (srl_16.getEnablePort().getValue() != null) : "unresolved enable port value of SRL16 "
					+ srl_16.toString() + " owned by " + srl_16.getOwner();
			consumed_nets.add(enable_wire);

			return MappingName.SHIFT_REGISTER_LUT_ENABLE;

		case SRL16.SRL16:

			return MappingName.SHIFT_REGISTER_LUT;

		case SRL16.SRL16_1:

			return MappingName.SHIFT_REGISTER_LUT_NEG_EDGE;

		default:
			assert (false) : "No support for SRL16 " + srl_16.toString()
					+ " of type " + srl_16.getType() + " owned by "
					+ srl_16.getOwner();
			return null;
		}
	}

	/**
	 * Provides the collection of Nets which this statement of verilog uses as
	 * input signals.
	 */
	@Override
	public Collection<Net> getConsumedNets() {
		return consumed_nets;
	}

	/**
	 * Provides the collection of Nets which this statement of verilog produces
	 * as output signals.
	 */
	@Override
	public Collection<Net> getProducedNets() {
		return Collections.singleton(result_wire);
	}

	@Override
	public Set<MappedModule> getMappedModules() {
		return mappedModules;
	}
}
