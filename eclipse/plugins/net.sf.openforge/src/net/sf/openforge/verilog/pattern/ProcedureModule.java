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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Statement;

/**
 * A ProcedureModule is Module based upon a LIM {@link Procedure}.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: ProcedureModule.java 2 2005-06-09 20:00:48Z imiller $
 */

public class ProcedureModule extends net.sf.openforge.verilog.model.Module
		implements MappedModuleSpecifier {

	private Procedure proc;

	private Set<MappedModule> mappedModules = new HashSet<MappedModule>();

	/**
	 * Construct a ProcedureModule based on a {@link Procedure}. The output
	 * Buses and peer-Buses of input Ports will be added to the defined Bus-Net
	 * map.
	 * 
	 * @param proc
	 *            the Procedure which is being instantiated
	 */
	public ProcedureModule(Procedure proc) {
		super(ID.toVerilogIdentifier(ID.showLogical(proc)));
		this.proc = proc;
		defineInterface();
	} // ProcedureModule

	/**
	 * Defines the Module ports based on the Ports and Buses of the Procedure's
	 * body Block.
	 */
	public void defineInterface() {
		net.sf.openforge.lim.Module body = proc.getBody();

		// define input ports (based on the body's Ports)
		
		for( Port port : body.getPorts()){
			addInput(port);
		}

		// define output ports (based on the body's buses)
		for (Iterator mod_buses = body.getBuses().iterator(); mod_buses
				.hasNext();) {
			addOutput((Bus) mod_buses.next());
		}
	} // defineInterface

	private void addInput(Port p) {
		if (p.isUsed()) {
			assert (p.getPeer() != null) : "Can't add input for Port with null source.";
			assert (p.getPeer().getValue() != null) : "Can't add input for Port with null value.";
			addPort(new BusInput(p.getPeer()));
		}
	} // addInput()

	private void addOutput(Bus b) {
		if (b.isUsed()) {
			/*
			 * CWU - Since the partial constant prop has resolved the source bus
			 * of each bits stored in Value, all the pass through components has
			 * been removed at this point. we just need to make sure that the
			 * bus's value is not null. // assert (b.getSource() != null) :
			 * "Can't add output for Bus with null source."; // assert
			 * (b.getSource().getValue() != null) :
			 * "Can't add output for Bus with null value.";
			 */
			assert (b.getValue() != null) : "Can't add output for Bus with null value.";

			addPort(new BusOutput(b));
		}
	} // addOutput()

	/**
	 * Adds a statement to the statement block of the module, and a declaration
	 * for each undeclared Net produced by the statement.
	 */
	public void state(Statement statement) {
		assert ((statement instanceof ForgePattern) || (statement instanceof InlineComment)) : "DesignModule only supports stating ForgePatterns.";

		if (statement instanceof ForgePattern) {
			for (Iterator it = ((ForgePattern) statement).getProducedNets()
					.iterator(); it.hasNext();) {
				Net net = (Net) it.next();
				if (!isDeclared(net)) {
					declare(net);
				}
			}
		}
		statements.add(statement);

		if (statement instanceof MappedModuleSpecifier) {
			mappedModules.addAll(((MappedModuleSpecifier) statement)
					.getMappedModules());
		}

	} // state()

	/**
	 * Provides the Set of mapped Modules
	 */
	public Set<MappedModule> getMappedModules() {
		return mappedModules;
	}

} // class ProcedureModule
