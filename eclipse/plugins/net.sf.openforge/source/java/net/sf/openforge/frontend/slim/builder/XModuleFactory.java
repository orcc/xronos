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

package net.sf.openforge.frontend.slim.builder;


import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.naming.*;

import org.w3c.dom.*;
import java.util.*;

/**
 * XModuleFactory generates structurally correct {@link Block} objects
 * for all the module types in XLIM.  This includes the basic 'module'
 * as well as the implicit blocks implementing the if/test, then, and
 * else blocks.  The XModuleFactory uses document order of the child
 * nodes to define relationships between nodes without specific data
 * flow dependencies.  For example state modifying/consuming nodes
 * have an implicit dependence upon one another.  This factory assumes
 * that this implicit dependence is defined by their document order.
 *
 * <p>Created: Mon Jul 11 14:01:38 2005
 *
 * @author imiller, last modified by $Author: imiller $
 */
public class XModuleFactory extends XFactory
{

    /** The cache of design level resources. */
    private ResourceCache resourceCache;

    /** The cache of node to Port created for that node */
    final PortCache portCache = new PortCache();
    
    /** A simple mapping of Node to the component for that node. */
    private final Map componentMap = new HashMap();
    
    /** The module being constructed. */
    private Module module;
    private Exit.Type exitType;

    /**
     * Constructs an XModuleFactory which will use the specified
     * resources and which will generate a {@link Block} with the
     * default exit type of {@link Exit#DONE}.
     *
     * @param resources a non-null ResourceCache
     */
    XModuleFactory (ResourceCache resources)
    {
        this(resources, false);
    }
    
    /**
     * Constructs an XModuleFactory which will use the specified
     * resources and which will generate a {@link Block} if
     * <i>isMutex</i> is false or a {@link MutexBlock} if
     * <i>isMutex</i>  is true, and with the default exit type of
     * {@link Exit#DONE}. 
     *
     * @param resources a non-null ResourceCache
     * @param isMutex a boolean indicating if the module components
     * are mutually exclusive in their execution.
     */
    XModuleFactory (ResourceCache resources, boolean isMutex)
    {
        this(resources, isMutex ? new MutexBlock(false):new Block(false), Exit.DONE);
    }
    
    /**
     * Constructs an XModuleFactory which will use the specified
     * resources and which will generate a {@link Block} with the
     * specified exit type.  This may be usefull when the block is the
     * top level of a call/procedure implementation and needs a return
     * type exit.
     *
     * @param resources a non-null ResourceCache
     * @param exitType a value of type 'Exit.Type'
     */
    protected XModuleFactory (ResourceCache resources, Exit.Type exitType)
    {
        this(resources, new Block(false), exitType);
    }
    
    private XModuleFactory (ResourceCache resources, Module mod, Exit.Type exitType)
    {
        this.resourceCache = resources;
        this.module = mod;
        if (_parser.db && mod.isMutexModule())
        {
            System.out.println("NOTE: Module " + mod + " is mutex");
        }
        this.exitType = exitType;
    }
    
    /**
     * Returns the {@link PortCache} which contains a mapping for all
     * the ports ({@link Port} and {@link Bus} objects) in the
     * generated Block <b>including</b> the ports of the Block
     * itself. 
     *
     * @return a non-null PortCache
     */
    public PortCache getPortCache ()
    {
        return this.portCache;
    }

    /**
     * Returns a Collection of Port and Bus objects that are visible
     * on the returned Component from buildComponent().
     *
     * @return a Collection of Port and Bus objects
     */
    protected Collection getExternallyVisiblePorts ()
    {
        final Set modulePorts = new HashSet(getModule().getPorts());
        modulePorts.addAll(getModule().getBuses());
        return modulePorts;
    }

    /**
     * Publishes the ports of the generated module to the specified
     * cache. 
     *
     * @param cache a value of type 'PortCache'
     */
    public void publishPorts (PortCache cache)
    {
        final PortCache local = getPortCache();
        local.publish(cache, getExternallyVisiblePorts());
    }
    
    /**
     * Retrieves the {@link ResourceCache} for this factory
     *
     * @return a value of type 'ResourceCache'
     */
    protected ResourceCache getResourceCache ()
    {
        return this.resourceCache;
    }

    /**
     * Returns an unmodifiable view of the mapping of Nodes to
     * components. 
     *
     * @return a non-null, unmodifiable Map
     */
    protected Map getComponentMap ()
    {
        return Collections.unmodifiableMap(this.componentMap);
    }
    
    /**
     * A mechanism for sub-classes to override the type of module
     * created in this factory.
     *
     * @param mod a non-null Module
     */
    protected void setModule (Module mod)
    {
        if (mod == null)
            throw new IllegalArgumentException("Cannot set factory to have null module");
        
        this.module = mod;
    }

    /**
     * Retrieve the module that this factory created.
     */
    protected Module getModule ()
    {
        return this.module;
    }
    
    /**
     * Populates the Block being generated by this factory with
     * contents as specified by the XLIM fragment Node.
     *
     * @param moduleNode a Node
     * @return a non-null, fully populated Block (according to structure of
     * the Node)
     */
    public Component buildComponent (Node moduleNode)
    {
        //assert moduleNode.getNodeName().equals(XLIMConstants.MODULE);
        final Element moduleElement = (Element)moduleNode;

        final List componentTags = new ArrayList();
        componentTags.add(SLIMConstants.OPERATION);
        componentTags.add(SLIMConstants.IF);
        componentTags.add(SLIMConstants.MODULE);
        
        final List operations = getChildNodesByTag(moduleElement, componentTags);
        final List components = new ArrayList(operations.size());
        final XOperationFactory opFactory = new XOperationFactory(this.resourceCache);

        for (Iterator iter = operations.iterator(); iter.hasNext();)
        {
            Node node = (Node)iter.next();
            Component comp = opFactory.makeOperation(node, portCache);
            this.componentMap.put(node, comp);
            components.add(comp);
        }

        createInterface((Element)moduleNode, portCache);
        
        populateModule(getModule(), components);

        buildDependencies((Element)moduleNode, portCache);

        getResourceCache().registerConfigurable(moduleNode, getModule());
        
        buildOptionScope(moduleNode, getModule());
            
        return (Block)getModule();
    }

    
    /**
     * Builds the external interface to the Block.
     *
     * @param moduleElement the XLIM module Element
     * @param portCache the PortCache which will be updated with the
     * generated interface.
     */
    protected void createInterface (Element moduleElement, PortCache portCache)
    {
        List ports = getChildNodesByTag(moduleElement, SLIMConstants.PORT);
        // Build ports
        if (_parser.db) System.out.println("Building interface in " + this);
        for (Iterator iter = ports.iterator(); iter.hasNext();)
        {
            Element portNode = (Element)iter.next();

            // <port name="go2" dir="in" size="1" source="internal_action2_go" typeName="bool" tag="idm25" type="go"/>
            Port port;
            if (portNode.getAttribute(SLIMConstants.PORT_TYPE).equals(SLIMConstants.CONTROL_TYPE))
            {
                port = getModule().getGoPort();
            }
            else
            {
                port = getModule().makeDataPort();
            }
            if (_parser.db) System.out.println("Created port " + port + " " + port.getPeer() + " " + portNode.getAttribute("name"));
            portCache.putTarget(portNode, port);
            portCache.putSource(portNode, port.getPeer());
        }

        List exits = getChildNodesByTag(moduleElement, SLIMConstants.EXIT);
        // Build exits
        for (Iterator iter = exits.iterator(); iter.hasNext();)
        {
            Node exitNode = (Node)iter.next();
            makeModuleExit(exitNode, portCache);
        }
    }
    
    /**
     * Defines the data and control flow relationships between the
     * components of the block according to the
     * {@link SLIMConstants#DEPENDENCY} elements of the XLIM module.
     * This method creates only dependencies and not any physical
     * connectivity.  The implemented connectivity is currently
     * handled by the Forge scheduling code.
     * <p>This functionality could be extended by defining explicit
     * connectivity and not scheduling the block.
     *
     * @param moduleElement a value of type 'Element'
     * @param portCache a value of type 'PortCache'
     */
    protected void buildDependencies (Element moduleElement, PortCache portCache)
    {
        //List dependencies = getChildNodesByTag(moduleElement,XLIMConstants.DEPENDENCY);
        List dependencies = getDependencies(moduleElement);

        // Build dependencies
        for (Iterator iter = dependencies.iterator(); iter.hasNext();)
        {
            Element depNode = (Element)iter.next();
            if (_parser.db) System.out.println("Building dependency " + ((Element)depNode).getAttribute("tag"));
            String sourceId = depNode.getAttribute(SLIMConstants.DEP_SOURCE);
            String targetId = depNode.getAttribute(SLIMConstants.DEP_TARGET);
            String groupId = depNode.getAttribute(SLIMConstants.DEP_GROUP);
            int group = Integer.parseInt(groupId);
            
            Bus sourceBus = portCache.getSource(sourceId);
            Port targetPort = portCache.getTarget(targetId);
            // Due to the 'populateModule' everything WILL have an entry now.
            List entries = targetPort.getOwner().getEntries();
            // Create sufficient entries.  In most cases this does nothing
            for (int i=entries.size(); i <= group; i++)
            {
                targetPort.getOwner().makeEntry(null);
            }
            
            Entry entry = (Entry)entries.get(group);
            Dependency dep;
            if (depNode.getAttribute("kind").equals("resource"))
            {
                int clocks = Integer.parseInt(depNode.getAttribute("cycles"));
                dep = new ResourceDependency(sourceBus, clocks);
            }
            else
            {
                dep = (targetPort == targetPort.getOwner().getGoPort()) ? new ControlDependency(sourceBus):new DataDependency(sourceBus);
            }
            entry.addDependency(targetPort, dep);
        }
    }
    
    /**
     * Takes care of the busy work of putting the components into the
     * module (in order) and ensuring appropriate clock, reset, and go
     * dependencies (which ALL components must have)
     *
     * @param components a List of {@link Component} objects
     */
    public static void populateModule (Module module, List components)
    {
        final InBuf inBuf = module.getInBuf();
        final Bus clockBus = inBuf.getClockBus();
        final Bus resetBus = inBuf.getResetBus();
        final Bus goBus = inBuf.getGoBus();

        // I believe that the drivingExit no longer relevant
        Exit drivingExit = inBuf.getExit(Exit.DONE);

        int index = 0;
        for (Iterator iter = components.iterator(); iter.hasNext();)
        {
            final Component comp = (Component)iter.next();
            if (module instanceof Block)
                ((Block)module).insertComponent(comp, index++);
            else
                module.addComponent(comp);

            addEntry(comp, drivingExit, clockBus, resetBus, goBus);
            
            drivingExit = comp.getExit(Exit.DONE);
        }
        
        // Ensure that the outbufs of the module have an entry
        for (Iterator iter = module.getOutBufs().iterator(); iter.hasNext();)
        {
            addEntry((OutBuf)iter.next(), drivingExit, clockBus, resetBus, goBus);
        }
    }

    private static void addEntry (Component comp, Exit drivingExit, Bus clockBus, Bus resetBus, Bus goBus)
    {
        assert comp.getEntries().size() == 0 : "Component "+comp+ " of " + comp.showOwners() + " already has entry";
        
        Entry entry = comp.makeEntry(drivingExit);
        // Even though most components do not use the clock, reset and
        // go ports we set up the dependencies for consistancy.
        entry.addDependency(comp.getClockPort(), new ClockDependency(clockBus));
        entry.addDependency(comp.getResetPort(), new ResetDependency(resetBus));
        entry.addDependency(comp.getGoPort(), new ControlDependency(goBus));
    }
    
    
    /**
     * Generates an exit for the module and populates it with the
     * buses as defined by the exit Node.
     *
     * @param node a Node, of type exit
     * @param portCache the PortCache which will be updated with the
     * constructed output ports.
     */
    protected void makeModuleExit (Node node, PortCache portCache)
    {
        final String exitType = node.getAttributes().getNamedItem(SLIMConstants.EXIT_KIND).getNodeValue();
        assert exitType.equals("done") : "Only DONE type exits are supported";

        final Exit exit;
        if (getModule().getExit(Exit.DONE) == null)
        {
            if (this.exitType != Exit.DONE)
            {
                exit = getModule().makeExit(0, this.exitType);
            }
            else
            {
                exit = getModule().makeExit(0);
            }
        }
        else
        {
            exit = getModule().getExit(Exit.DONE);
        }
        
        populateExit(exit, node, portCache);
    }

    /**
     * Populates the specified exit with the buses specified by the
     * exit Node 'node'
     *
     * @param exit an Exit
     * @param node a Node, of type exit
     * @param portCache the PortCache which will be updated with the
     * constructed output ports.
     */
    protected void populateExit (Exit exit, Node node, PortCache portCache)
    {
        final List exitPorts = getChildNodesByTag(node, SLIMConstants.PORT);
        for (Iterator iter = exitPorts.iterator(); iter.hasNext();)
        {
            Element portNode = (Element)iter.next();
            Bus bus;
            if (portNode.getAttribute(SLIMConstants.ELEMENT_KIND).equals(SLIMConstants.CONTROL_TYPE))
            {
                bus = exit.getDoneBus();
                assert bus != null;
            }
            else
            {
                // For now we are ignoring the size attribute
                //String size = portNode.getAttribute("size").getNodeValue();
                bus = exit.makeDataBus();
                bus.setSize(getPortSize(portNode), isSignedPort(portNode));
            }
            portCache.putTarget(portNode, bus.getPeer());
            portCache.putSource(portNode, bus);
        }
    }

    protected List getDependencies (Element moduleElement)
    {
        List dependencies = getChildNodesByTag(moduleElement,SLIMConstants.DEPENDENCY);
        checkDependencies(dependencies);
        return dependencies;
    }
    
    protected void checkDependencies (Collection dependencies)
    {
        for (Iterator iter = dependencies.iterator(); iter.hasNext();)
        {
            Element depNode = (Element)iter.next();
            String tag = depNode.getAttribute("tag");
            String sourceId = depNode.getAttribute(SLIMConstants.DEP_SOURCE);
            String targetId = depNode.getAttribute(SLIMConstants.DEP_TARGET);
            String groupId = depNode.getAttribute(SLIMConstants.DEP_GROUP);
            int group = Integer.parseInt(groupId);
            if (sourceId == null || sourceId.length() == 0)
                throw new IllegalNodeConfigurationException("Source id of " + tag + " is empty");
            if (targetId == null || targetId.length() == 0)
                throw new IllegalNodeConfigurationException("Target id of " + tag + " is empty");
            if (groupId == null || groupId.length() == 0)
                throw new IllegalNodeConfigurationException("Group id of " + tag + " is empty");
        }
    }

    protected void buildOptionScope (Node moduleNode, Module module)
    {
        assert moduleNode instanceof Element : "Module node is not an element";

        Element moduleElement = (Element)moduleNode;

        String name = moduleElement.getAttribute(SLIMConstants.NAME);
        if (name.length() > 0)
        {
            module.specifySearchScope(name);
        }
    }
    
}// XModuleFactory
