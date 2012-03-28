/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package net.sf.orc2hdl.design;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler.FifoIOHandler;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler.NativeIOHandler;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.memory.AddressStridePolicy;
import net.sf.openforge.lim.memory.AddressableUnit;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.memory.Scalar;
import net.sf.orcc.df.Instance;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Var;

import org.eclipse.emf.common.util.EList;

/**
 * This class transforms an Orcc {@link Instance} Object to an OpenForge
 * {@link Design} Object
 * 
 * @author Endri Bezati
 */
public class InstanceToDesign {
	Design design;
	Instance instance;

	public InstanceToDesign(Instance instance) {
		this.instance = instance;
		this.design = new Design();
	}

	public Design buildDesign() {
		final ResourceCache resources = new ResourceCache();

		// Get Instance name
		String designName = instance.getName();
		design.setIDLogical(designName);
		// GenericJob job = EngineThread.getGenericJob();
		// job.getOption(OptionRegistry.TOP_MODULE_NAME).setValue(
		// design.getSearchLabel(), designName);

		// Get Instance Input Ports
		getInstancePorts(instance.getActor().getInputs(), "in", resources);

		// Get Instance Output Ports
		getInstancePorts(instance.getActor().getOutputs(), "out", resources);

		// Get Instance State Variables
		Map<LogicalValue, Var> stateVars = new HashMap<LogicalValue, Var>();
		getInstanceStateVars(stateVars);

		return design;
	}

	/**
	 * This method get the I/O ports of the actor and it adds in {@link Design}
	 * the actors ports
	 * 
	 * @param ports
	 *            the list of the Ports
	 * @param direction
	 *            the direction of the port, "in" for input / "out" for output
	 * @param resources
	 *            the cache resource
	 */
	private void getInstancePorts(EList<net.sf.orcc.df.Port> ports,
			String direction, ResourceCache resources) {
		for (net.sf.orcc.df.Port port : ports) {
			if (port.isNative()) {
				NativeIOHandler ioHandler = new ActionIOHandler.NativeIOHandler(
						direction, port.getName(), Integer.toString(port
								.getType().getSizeInBits()));
				ioHandler.build(design);
				resources.addIOHandler(port, ioHandler);
			} else {
				FifoIOHandler ioHandler = new ActionIOHandler.FifoIOHandler(
						direction, port.getName(), Integer.toString(port
								.getType().getSizeInBits()));
				ioHandler.build(design);
				resources.addIOHandler(port, ioHandler);
			}
		}
	}

	private void getInstanceStateVars(Map<LogicalValue, Var> stateVars) {
		for (Var var : instance.getActor().getStateVars()) {
			stateVars.put(makeLogicalValue(var), var);
		}
	}

	private LogicalValue makeLogicalValue(Var var) {
		LogicalValue logicalValue = null;
		if (var.getType().isList()) {

		} else {
			final int bitSize = var.getType().getSizeInBits();
			String valueString = "0";
			if (var.isInitialized()) {
				valueString = Integer
						.toString(((ExprInt) var.getInitialValue())
								.getIntValue());

			}
			final BigInteger value;
			if (valueString.trim().toUpperCase().startsWith("0X")) {
				value = new BigInteger(valueString.trim().substring(2), 16);
			} else {
				value = new BigInteger(valueString);
			}
			AddressStridePolicy addrPolicy = new AddressStridePolicy(bitSize);
			logicalValue = new Scalar(new AddressableUnit(value), addrPolicy);
		}

		return logicalValue;
	}
}
