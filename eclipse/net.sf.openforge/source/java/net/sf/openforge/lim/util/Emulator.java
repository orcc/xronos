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
package net.sf.openforge.lim.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Emulatable;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.LoopBody;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;


/**
 * @author gandhij
 *
 * This is a helper class to emulate any composite component
 *
 */
public abstract class Emulator{
	
	/**
	 * List of subcomponents 
	 */
	protected List componentList = null;
	
	/* 
	 * Collection of unEmulatable components 
	 * that the module has is cached here for
	 */
	protected Collection unEmulatable = new ArrayList();
	
	
	/**
	 *  Map of each port to its input bus 
	 **/
	protected HashMap inputMap = new HashMap();
	
	/**
	 * Emulator Constructor 
	 */
	public Emulator() {
		super();
	}
	
	/**
     * Gets the {@link Bus} that drives a given {@link Port}.
     * null is returned if the component has 0 entries or more than
     * 1 entry except LoopBody for which the feedback entry is chosen.
     * If the component has more than 1 dependency, null is returned.
     *
     * @param port the port whose driving bus is to be determined
     * @return the bus that provides the input value to <code>port</code>; if the port
     *           is already connected, the bus will be the connected one; otherwise the
     *           port's dependency will be used
     * 
     */
    protected Bus getInputBus (Port port)
    {

    	final Component entryOwner = isInBuf(port.getOwner()) ? port.getOwner().getOwner() : port.getOwner();
    	if(entryOwner.getEntries().size() < 1){
    		return null;
    	}
    	if(entryOwner.getEntries().size() > 2 ||
    		(entryOwner.getEntries().size() == 2 && !(port.getOwner() instanceof LoopBody))){
//    		Job.info("wrong entries count " + entryOwner.getEntries().size()
//    			+ " for " + port.getOwner()
//                + " in " + port.getOwner().getOwner());
    		return null;
    	}
    	
        final Collection deps = ((Entry)entryOwner.getEntries().iterator().next()).getDependencies(port);
		
        /*
         * Handle connections that were established manually rather than through
         * dependency resolution.
         */
        if (deps.isEmpty() && port.isConnected())
        {
            return port.getBus();
        }

        if (deps.size() > 1)
        {
//            Job.info("wrong dependency count: " + deps.size()
//                + " for " + port.getOwner()
//                + " in " + port.getOwner().getOwner());
            return null;
        }
        
        Bus inputBus = null;
        if(deps.size() != 0){
        	final Dependency dep = (Dependency)deps.iterator().next();
        	inputBus = dep.getLogicalBus();
        }

        return inputBus;
    }
 
    /**
     * Tests whether a given component is its owner's {@link InBuf}.
     */
    protected static boolean isInBuf (Component component)
    {
        final Module owner = component.getOwner();
        return (owner != null) && (owner.getInBuf() == component);
    }

    /**
     * Tests whether a given component is one of its owner's {@link OutBuf}.
     */
    protected static boolean isOutBuf (Component component)
    {
        final Module owner = component.getOwner();
        return (owner != null) && (owner.getOutBufs().contains(component));
    }
    
    /**
     * Update the inputMap object. For each component in the
     * componentList, pairs of (input port, data providing bus) are
     * saved in the inputMap object. This helps later when looking up
     * the bus that provides the data for a subcomponents port.
     */
	protected void updateInputMap(){
		Iterator iter = componentList.iterator();
		while(iter.hasNext()){
			Component nextComponent = (Component)iter.next();
			Iterator ports = nextComponent.getDataPorts().iterator();
			while(ports.hasNext()){
				Port port = (Port)ports.next();
				inputMap.put(port, getInputBus(port));
			}
		}
	}
	
	/**
	 * Get a map of (input bus, value of bus) pairs from 
	 * a map of (port, value) pairs 
	 * 
	 * @param portMap (port, value) pairs
	 * @return busValues (bus, value) pairs
	 */
	protected Map portToBusValues(Map portMap){
		final Map busValues = new HashMap();
        for (Iterator iter = portMap.keySet().iterator(); iter.hasNext();){
            final Port port = (Port)iter.next();
            final Bus inputBus = (Bus)inputMap.get(port);
            busValues.put(inputBus, portMap.get(port));
        }
        return busValues;
	}
	
	/**
	 * Get a map of (Port port of component, SizedInteger value) for the given component
	 * from a map of (Bus bus , SizedInteger value) pairs. The busValues map may contain
	 * other busses that do not provide data to any of the components ports
	 * also.
	 * 
	 * SideEffect: If the component is not emulatable, it gets added to the unEmulatable
	 * Collection.
	 * 
	 * @param component component whose input ports have to be mapped to values
	 * @param busValues map of (Bus bus,SizedInteger value) pairs
	 * @return map of the components ports to SizedInteger values.
	 */
	protected HashMap busToPortValues(Component component, Map busValues){
		/*
         * Collect the input values for the component from its input buses.
         */
        final HashMap portValues = new HashMap();
        List dataPorts = isInBuf(component) ? component.getOwner().getDataPorts() : component.getDataPorts();
        for (Iterator piter = dataPorts.iterator(); piter.hasNext();){
            final Port dataPort = (Port)piter.next();
            final Bus dataBus = (Bus)inputMap.get(dataPort);
            if (dataBus != null){
            	
            	boolean isModule = component instanceof Module;
            	boolean isIOBuf = component instanceof InBuf || component instanceof OutBuf;
            	boolean inputUnknown = busValues.get(dataBus)==null;
            	boolean emulateable = (component instanceof Emulatable) || isModule;
            
            	if(!emulateable || (inputUnknown && !(isModule || isIOBuf))){
              		//System.out.println("Component " + component + "is not emulatable ");
              		unEmulatable.add(component);
            	}
                if(busValues.get(dataBus)!=null){
                	portValues.put(dataPort, busValues.get(dataBus));	
                }
            }
        }
        return portValues;
	}
	
	/**
	 * Get the unEmulatable collection
	 * @return the unEmulatable collection 
	 */
	protected Collection getUnEmulatable() {
		return unEmulatable;
	}
	
	/**
	 * Set/Reset the unEmulatable Collection
	 * @param unEmulatable the new unEmulatable collection
	 */
	protected void setUnEmulatable(Collection unEmulatable) {
		this.unEmulatable = unEmulatable;
	}
	
	/** 
	 * get the list of subcomponents  
	 * @return list of subcomponents
	 */
	public List getComponentList() {
		return componentList;
	}
	
	/**
	 * Set the list of subcomponents
	 * @param componentList new list of subcomponents
	 */
	public void setComponentList(List componentList) {
		this.componentList = componentList;
	}
}
