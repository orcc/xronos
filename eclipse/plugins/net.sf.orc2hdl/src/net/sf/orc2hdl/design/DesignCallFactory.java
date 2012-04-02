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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDSourceInfo;
import net.sf.orcc.df.Action;

/**
 * DesignCallFactory is an extension of the DesignModuleFactory which wraps the
 * module in a {@link Procedure} and {@link Call} and returns the built Call.
 * The publishing of Ports to the PortCache is also handled such that the
 * externally visible ports of the module are the call ports.
 * 
 * @author Endri Bezati
 * 
 */
public class DesignCallFactory extends DesignModuleFactory {

	private Call generatedCall = null;
	private String className = null;

	public DesignCallFactory(ResourceCache resources, String className) {
		super(resources, Exit.RETURN);
		this.className = className;

	}

	@Override
	public Component buildComponent(Action action) {
		Block procedureBlock = (Block) super.buildComponent(action);
		// Create a procedure out of the block and use it to
		// populate a new 'task' in the design
		Procedure proc = new Procedure(procedureBlock);
		Call call = proc.makeCall();
		// Give the procedure and call some meaningful IDSourceInfo so
		// that it will have a reasonable searchLabel for assigning
		// attributes to.
		proc.setIDSourceInfo(deriveIDSourceInfo(action));

		// Fix the port cache by substituting the call ports for the
		// block ports
		PortCache cache = getPortCache();
		for (Port blockPort : procedureBlock.getPorts()) {
			Port callPort = call.getPortFromProcedurePort(blockPort);
			assert blockPort != null;
			assert callPort != null;
			cache.replaceTarget(blockPort, callPort);
		}
		for (Exit exit : procedureBlock.getExits()) {
			for (Bus blockBus : exit.getBuses()) {
				Bus callBus = call.getBusFromProcedureBus(blockBus);
				cache.replaceSource(blockBus, callBus);
			}
		}

		generatedCall = call;
		// getResourceCache().registerConfigurable(moduleNode, proc);

		return call;
	}

	/**
	 * This method is used by the {@link #publishPorts} method to identify which
	 * ports are to be published. We must override the super in order to specify
	 * the call ports, otherwise the ports of the procedure module would be
	 * used.
	 * 
	 * @return a Collection of Port and Bus objects.
	 */
	@Override
	protected Collection<ID> getExternallyVisiblePorts() {
		final Set<ID> ports = new HashSet<ID>(generatedCall.getPorts());
		ports.addAll(generatedCall.getBuses());
		return ports;
	}

	protected IDSourceInfo deriveIDSourceInfo(Action action) {
		String fileName = null;
		String packageName = null;
		String className = this.className;
		String methodName = action.getName();
		String signature = null;
		int line = 0;
		int cpos = 0;
		return new IDSourceInfo(fileName, packageName, className, methodName,
				signature, line, cpos);
	}
}
