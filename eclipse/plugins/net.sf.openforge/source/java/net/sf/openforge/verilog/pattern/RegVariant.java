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
import java.util.Iterator;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.lim.Attribute;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.mapping.MappingName;
import net.sf.openforge.verilog.mapping.PrimitiveMappedModule;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Constant;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.ParameterSetting;
import net.sf.openforge.verilog.model.Wire;

/**
 * There are 8 possible Reg varaints (see lim.reg), all of which are used via
 * the primitive mapper
 * 
 * 
 * @version $Id: RegVariant.java 280 2006-08-11 17:00:32Z imiller $
 */

public class RegVariant extends StatementBlock implements ForgePattern,
		MappedModuleSpecifier {

	private Set<Object> consumedNets = new HashSet<Object>();

	private BusWire resultWire;

	private PortWire enableWire;
	private PortWire resetWire;
	private PortWire setWire;
	private PortWire dataWire;
	private PortWire clockWire;

	private Set<MappedModule> mappedModules = new HashSet<MappedModule>();

	public RegVariant(Reg reg) {
		GenericJob gj = EngineThread.getGenericJob();
		resultWire = new BusWire(reg.getResultBus());
		if (reg.getDataPort().isUsed())
			dataWire = new PortWire(reg.getDataPort());
		if (reg.getClockPort().isUsed())
			clockWire = new PortWire(reg.getClockPort());
		if (reg.getEnablePort().isUsed())
			enableWire = new PortWire(reg.getEnablePort());
		if (reg.getInternalResetPort().isUsed())
			resetWire = new PortWire(reg.getInternalResetPort());
		if (reg.getSetPort().isUsed())
			setWire = new PortWire(reg.getSetPort());

		// Preset and clear aren't supported here... the
		// getPrimitiveComponent below will assert false...

		// create as many module instantiations as you have widths
		int top_bit = 0;
		for (int i = 0; i < resultWire.getWidth(); i++) {
			// get the component we need
			MappingName mappingName = getPrimitiveComponent(reg, i);
			if (mappingName == null) {
				gj.info("Unable to map: " + ID.showLogical(reg));
				return;
			}

			PrimitiveMappedModule pmm = gj.getPrimitiveMapper()
					.getMappedModule(mappingName.getName());
			if (pmm == null) {
				gj.info("No part found for: " + mappingName);
				return;
			}

			mappedModules.add(pmm);

			boolean isOneBit = (dataWire.getWidth() == 1);

			// create this instance
			ModuleInstance mi;
			if (isOneBit) {
				mi = new ModuleInstance(pmm.getModuleName(),
						ID.toVerilogIdentifier(ID.showLogical(reg)));
				// first connect the input and out -- they must be there
				mi.connect(new Wire("D", 1), dataWire);
				mi.connect(new Wire("Q", 1), resultWire);
			} else {
				mi = new ModuleInstance(pmm.getModuleName(),
						ID.toVerilogIdentifier(ID.showLogical(reg) + "_slice"
								+ i));
				// first connect the input and out -- they must be there
				//
				// the result wire size can be larger than the data wire
				// when this reg is create by a PinOutBuf with a reset value.
				if (i >= dataWire.getWidth() - 1) {
					top_bit = top_bit == 0 ? i : top_bit;
					mi.connect(new Wire("D", 1), dataWire.getBitSelect(top_bit));
				} else {
					mi.connect(new Wire("D", 1), dataWire.getBitSelect(i));
				}
				mi.connect(new Wire("Q", 1), resultWire.getBitSelect(i));
			}
			consumedNets.add(dataWire);
			consumedNets.add(resultWire);

			// Use these strings so that the initial value will also
			// reflect in any specified reset/set behavior. RESET is
			// to go to the specified initial value. SET is to go to
			// the inverse of that.
			String resetPort = "R";
			String setPort = "S";
			int initValue = 0;
			if (reg.getInitialValue() != null) {
				// The initial value is captured in the INIT parameter
				// of the instantiated primitive.
				initValue = reg.getInitialValue().getBit(i).isOn() ? 1 : 0;
				ParameterSetting psetting = new ParameterSetting("INIT",
						new Constant(initValue, 1));
				mi.addParameterValue(psetting);
			}
			if (initValue != 0) {
				resetPort = "S";
				setPort = "R";
			}

			// now the singletons ...
			switch (reg.getType()) // this is the raw type
			{
			case Reg.REGRE:
				mi.connect(new Wire("C", 1), clockWire); // clock
				consumedNets.add(clockWire);
				mi.connect(new Wire("CE", 1), enableWire); // enable
				consumedNets.add(enableWire);
				mi.connect(new Wire(resetPort, 1), resetWire); // reset
				consumedNets.add(resetWire);
				break;
			case Reg.REGSE:
				mi.connect(new Wire("C", 1), clockWire); // clock
				consumedNets.add(clockWire);
				mi.connect(new Wire("CE", 1), enableWire); // enable
				consumedNets.add(enableWire);
				mi.connect(new Wire(setPort, 1), setWire); // set
				consumedNets.add(setWire);
				break;
			case Reg.REGR:
				mi.connect(new Wire("C", 1), clockWire); // clock
				consumedNets.add(clockWire);
				mi.connect(new Wire(resetPort, 1), resetWire); // reset
				consumedNets.add(resetWire);
				break;
			case Reg.REGE:
				mi.connect(new Wire("C", 1), clockWire); // clock
				consumedNets.add(clockWire);
				mi.connect(new Wire("CE", 1), enableWire); // enable
				consumedNets.add(enableWire);
				break;
			case Reg.REGRS:
				mi.connect(new Wire("C", 1), clockWire); // clock
				consumedNets.add(clockWire);
				mi.connect(new Wire(resetPort, 1), resetWire); // reset
				consumedNets.add(resetWire);
				mi.connect(new Wire(setPort, 1), setWire); // set
				consumedNets.add(setWire);
				break;
			case Reg.REG:
				mi.connect(new Wire("C", 1), clockWire); // clock
				consumedNets.add(clockWire);
				break;
			}

			add(mi);

			for (Attribute att : reg.getAttributes()) {
				add(new InlineComment(att.getVerilogAttribute(mi
						.getIdentifier().getToken()), Comment.SHORT));
			}

			/*
			 * if(reg.getInitialValue() != null) { // The initial value is
			 * captured for synthesis in a // comment based attribute. For
			 * simulation it is // captured in the INIT parameter of the
			 * instantiated // primitive. String value =
			 * reg.getInitialValue().getBit(i).isOn() ? "1":"0"; String
			 * instanceName = mi.getIdentifier().getToken(); // in this case,
			 * connect the set/reset to the design reset SynopsysBlock initBlock
			 * = new SynopsysBlock(); initBlock.append("defparam " +
			 * instanceName + ".INIT = 1'b" + value + ";"); add(initBlock);
			 * add(new InlineComment("synthesis attribute INIT of " +
			 * instanceName + " is \"1\"")); }
			 */
		}
	}

	private MappingName getPrimitiveComponent(Reg reg, int bit) {
		int type = reg.getType();
		// if it has a reset value, add eith the set or reset port to
		// the type. IDM. Initial values are handled through the
		// programming (INIT) data now. If an initial value is
		// specified then the RESET behavior will return the register
		// to the specified initial value. SET will return the
		// register to the inverse of that.
		switch (type) {
		case Reg.REGRE:
			return MappingName.FLOP_SYNC_ENABLE_RESET;
		case Reg.REGSE:
			return MappingName.FLOP_SYNC_ENABLE_SET;
		case Reg.REGR:
			return MappingName.FLOP_SYNC_RESET;
		case Reg.REGRS:
			return MappingName.FLOP_SYNC_SET_RESET;
		case Reg.REGE:
			return MappingName.FLOP_SYNC_ENABLE;
		case Reg.REG:
			return MappingName.FLOP_SYNC;
		default:
			assert (false) : "No support for Reg " + reg.toString()
					+ " of type " + reg.getType() + " owned by "
					+ reg.getOwner();
			return null;
		}
	}

	/**
	 * Provides the collection of Nets which this statement of verilog uses as
	 * input signals.
	 */
	public Collection getConsumedNets() {
		return consumedNets;
	}

	/**
	 * Provides the collection of Nets which this statement of verilog produces
	 * as output signals.
	 */
	public Collection getProducedNets() {
		return Collections.singleton(resultWire);
	}

	public Set<MappedModule> getMappedModules() {
		return mappedModules;
	}

} // class ShiftOp

