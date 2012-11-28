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

package org.xronos.openforge.optimize;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.OptionBoolean;
import org.xronos.openforge.app.project.OptionInt;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.lim.primitive.SRL16;


/**
 * CompactSRL16Visitor identifies the Reg chains in the entire design and
 * compacts each chain as either SRL16s or SRL16Es. Only the Regs without reset
 * will be compacted and replaced.
 * 
 * <p>
 * Created: Thu Oct 22 09:35:36 2002
 * <p>
 * Rewritten: July 06, 2006
 * 
 * @author imiller, cwu
 * @version $Id: CompactSRL16Visitor.java 204 2006-07-06 21:33:03Z imiller $
 */
public class CompactSRL16Visitor extends DefaultVisitor {

	private Map<Integer, Set<Reg>> groupableRegs = new HashMap<Integer, Set<Reg>>();

	/**
	 * Creates a new CompactSRL16Visitor object. DOCUMENT ME!
	 */
	public CompactSRL16Visitor() {
		// Prime the Map
		groupableRegs.put(new Integer(Reg.REG), new HashSet<Reg>());
		groupableRegs.put(new Integer(Reg.REGE), new HashSet<Reg>());
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param design
	 *            DOCUMENT ME!
	 */
	@Override
	public void visit(Design design) {
		super.visit(design);

		// Only REG and REGE might be groupable, separate out two
		// types of regs.
		Set<List<Reg>> regChains = getChains(groupableRegs.get(new Integer(
				Reg.REG)));
		Set<List<Reg>> regEChains = getChains(groupableRegs.get(new Integer(
				Reg.REGE)));
		compactRegChain(regChains, SRL16.SRL16);
		compactRegChain(regEChains, SRL16.SRL16E);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param reg
	 *            DOCUMENT ME!
	 */
	@Override
	public void visit(Reg reg) {
		// If reg has only 1 consumer of its result
		// and if reg 'is groupable', then put it into the map
		final Set<Component> regOutputTargets = new HashSet<Component>();
		for (Port port : reg.getResultBus().getPorts()) {
			regOutputTargets.add(port.getOwner());
		}

		if (reg.isGroupable() && regOutputTargets.size() == 1
				&& (reg.getType() == Reg.REG || reg.getType() == Reg.REGE)) {
			groupableRegs.get(new Integer(reg.getType())).add(reg);
		}
	}

	/**
	 * Takes a Set of Reg objects and constructs a Set of Lists of Reg objects.
	 * Each List represents a single contiguous chain of registers. The Lists
	 * may contain a single reg, but will never be empty. All Reg objects in the
	 * parameter Set will be contained in exactly one List in the returned Set
	 * of Lists.
	 */
	private Set<List<Reg>> getChains(Set<Reg> regSet) {
		Set<List<Reg>> chains = new HashSet<List<Reg>>();
		List<Reg> allRegs = new LinkedList<Reg>(regSet);
		while (!allRegs.isEmpty()) {
			Reg reg = allRegs.remove(0);
			List<Reg> chain = new LinkedList<Reg>();
			chain.add(reg);
			boolean added = true;
			while (added) {
				added = false;
				Component source = chain.get(0).getDataPort().getBus()
						.getOwner().getOwner();
				if (allRegs.contains(source)) {
					chain.add(0, (Reg) source);
					added = true;
				}
				for (Port port : chain.get(chain.size() - 1).getResultBus()
						.getPorts()) {
					Component sink = port.getOwner();
					if (allRegs.contains(sink)) {
						chain.add((Reg) sink);
						added = true;
					}
				}
			}
			allRegs.removeAll(chain);
			chains.add(chain);
		}
		return chains;
	}

	/**
	 * <code>compactRegChain</code> Consumes a Collection of Lists of Reg
	 * objects, converting each List to a series of SRL16 instantiations based
	 * on the type specified (the type matches SRL16.getType()).
	 * 
	 * @param type
	 *            an <code>int</code> value
	 */
	private void compactRegChain(Collection<List<Reg>> chainList, int type) {
		for (List<Reg> regList : chainList) {
			OptionInt minLengthOption = (OptionInt) EngineThread
					.getGenericJob().getOption(
							OptionRegistry.SRL_COMPACT_LENGTH);
			final int MIN_COMPACT_LENGTH = minLengthOption
					.getValueAsInt(regList.get(0).getSearchLabel());
			final boolean REGISTER_OUTPUT = !((OptionBoolean) EngineThread
					.getGenericJob()
					.getOption(OptionRegistry.SRL_NO_OUTPUT_REG))
					.getValueAsBoolean(regList.get(0).getSearchLabel());

			// Sanity checking. All the registers in a chain must be
			// in the same module.
			for (Reg reg : regList) {
				if (reg.getOwner() != regList.get(0).getOwner()) {
					throw new IllegalStateException(
							"Unexpected register chain configuration.  All registers in chain are not in the same hierarchy");
				}
			}

			// Limit replacement to chains of MIN_COMPACT_LENGTH or longer
			if (regList.size() < MIN_COMPACT_LENGTH) {
				continue;
			}
			// If the chain is only 2 long and we are leaving one in a
			// register, don't bother putting the extra 1 in an SRL
			if (regList.size() <= 2 && REGISTER_OUTPUT) {
				continue;
			}

			// So long as we have at least 2 registers remaining in
			// the chain, replace the first chunk of them with an
			// SRL16. This may leave a trailing register if there are
			// (N*16)+1 registers in the chain. If REGISTER_OUTPUT is
			// set to true, then we will always ENSURE that we are
			// putting the extra register outside the SRL. The
			// functional description docs suggest doing exactly this
			// for better system performance.
			while (regList.size() > 1) {
				final int terminal = regList.size() > 16 ? 16
						: (REGISTER_OUTPUT ? (regList.size() - 1) : regList
								.size());
				// List.subList() is <inclusive, exclusive>
				final List<Reg> subList = new LinkedList<Reg>(regList.subList(
						0, terminal));
				regList.removeAll(subList);
				final SRL16 srl;
				if (type == SRL16.SRL16) {
					srl = SRL16.createSRL16(subList);
				} else {
					srl = SRL16.createSRL16E(subList);
					srl.getEnablePort().setBus(
							subList.get(0).getEnablePort().getBus());
				}
				srl.getClockPort().setBus(
						subList.get(0).getClockPort().getBus());
				srl.getResetPort().setBus(
						subList.get(0).getResetPort().getBus());
				srl.getGoPort().setBus(subList.get(0).getGoPort().getBus());
				// Connect the input port
				srl.getInDataPort().setBus(
						subList.get(0).getDataPort().getBus());
				// Connect the SRL result bus to all targets of the
				// original (tail) register.
				for (Port port : subList.get(subList.size() - 1).getResultBus()
						.getPorts()) {
					port.setBus(srl.getResultBus());
				}
				// Add the SRL16 and remove the replaced registers.
				subList.get(0).getOwner().addComponent(srl);
				for (Reg reg : subList) {
					reg.getOwner().removeComponent(reg);
				}
			}
		}
	}

}
