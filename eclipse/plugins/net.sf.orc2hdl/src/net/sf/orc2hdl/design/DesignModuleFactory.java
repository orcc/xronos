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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.MutexBlock;
import net.sf.openforge.lim.Port;
import net.sf.openforge.util.naming.ID;
import net.sf.orcc.df.Action;

public class DesignModuleFactory extends DesignFactory {
	/** The cache of design level resources. */
	private final ResourceCache resourceCache;

	/** The cache of Instance Port to Lim Port created for that Port */
	PortCache portCache = new PortCache();

	/** The module being constructed. */
	private Module module;
	private final Exit.Type exitType;

	/**
	 * Constructs an DesignModuleFactory which will use the specified resources
	 * and which will generate a {@link Block} with the default exit type of
	 * {@link Exit#DONE}.
	 * 
	 * @param resources
	 *            a non-null ResourceCache
	 */
	DesignModuleFactory(ResourceCache resources) {
		this(resources, false);
	}

	/**
	 * Constructs an DesignModuleFactory which will use the specified resources
	 * and which will generate a {@link Block} if <i>isMutex</i> is false or a
	 * {@link MutexBlock} if <i>isMutex</i> is true, and with the default exit
	 * type of {@link Exit#DONE}.
	 * 
	 * @param resources
	 *            a non-null ResourceCache
	 * @param isMutex
	 *            a boolean indicating if the module components are mutually
	 *            exclusive in their execution.
	 */

	DesignModuleFactory(ResourceCache resources, boolean isMutex) {
		this(resources, isMutex ? new MutexBlock(false) : new Block(false),
				Exit.DONE);
	}

	/**
	 * Constructs an DesignModuleFactory which will use the specified resources
	 * and which will generate a {@link Block} with the specified exit type.
	 * This may be usefull when the block is the top level of a call/procedure
	 * implementation and needs a return type exit.
	 * 
	 * @param resources
	 *            a non-null ResourceCache
	 * @param exitType
	 *            a value of type 'Exit.Type'
	 */
	protected DesignModuleFactory(ResourceCache resources, Exit.Type exitType) {
		this(resources, new Block(false), exitType);
	}

	/**
	 * Constructs an DesignModuleFactory which will use the specified resources
	 * and which will generate a {@link Block} with the specified exit type.
	 * This may be usefull when the block is the top level of a call/procedure
	 * implementation and needs a return type exit.
	 * 
	 * @param resources
	 *            a non-null ResourceCache
	 * @param exitType
	 *            a value of type 'Exit.Type'
	 */
	public DesignModuleFactory(ResourceCache resources, Module mod,
			Exit.Type exitType) {
		resourceCache = resources;
		module = mod;
		if (mod.isMutexModule()) {
			System.out.println("NOTE: Module " + mod + " is mutex");
		}
		this.exitType = exitType;
	}

	/**
	 * Returns the {@link PortCache} which contains a mapping for all the ports
	 * ({@link Port} and {@link Bus} objects) in the generated Block
	 * <b>including</b> the ports of the Block itself.
	 * 
	 * @return a non-null PortCache
	 */
	public PortCache getPortCache() {
		return portCache;
	}

	/**
	 * Returns a Collection of Port and Bus objects that are visible on the
	 * returned Component from buildComponent().
	 * 
	 * @return a Collection of Port and Bus objects
	 */
	protected Collection<ID> getExternallyVisiblePorts() {
		final Set<ID> modulePorts = new HashSet<ID>(getModule().getPorts());
		modulePorts.addAll(getModule().getBuses());
		return modulePorts;
	}

	/**
	 * Publishes the ports of the generated module to the specified cache.
	 * 
	 * @param cache
	 *            a value of type 'PortCache'
	 */
	public void publishPorts(PortCache cache) {
		final PortCache local = getPortCache();
		local.publish(cache, getExternallyVisiblePorts());
	}

	/**
	 * Retrieves the {@link ResourceCache} for this factory
	 * 
	 * @return a value of type 'ResourceCache'
	 */
	protected ResourceCache getResourceCache() {
		return resourceCache;
	}

	/**
	 * A mechanism for sub-classes to override the type of module created in
	 * this factory.
	 * 
	 * @param mod
	 *            a non-null Module
	 */
	protected void setModule(Module mod) {
		if (mod == null)
			throw new IllegalArgumentException(
					"Cannot set factory to have null module");

		module = mod;
	}

	/**
	 * Retrieve the module that this factory created.
	 */
	protected Module getModule() {
		return module;
	}

	public Component buildComponent(Action action) {
		List<Component> components = new ArrayList<Component>();
		for (net.sf.orcc.df.Port port : action.getInputPattern().getPorts()) {

		}
		return getModule();
	}

}
