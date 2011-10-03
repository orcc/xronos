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

import java.util.*;

import net.sf.openforge.app.project.Configurable;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;

import org.w3c.dom.*;


/**
 * ResourceCache maintains mappings from the Node objects to all the
 * Design level resources in the implementation created for those
 * Nodes.  This includes input/output structures and memory allocated
 * for state variables.
 *
 *
 * <p>Created: Tue Jul 12 16:00:41 2005
 *
 * @author imiller, last modified by $Author: imiller $
 */
public class ResourceCache extends CacheBase
{

    /** Map of Node to ActionIOHandler objects*/
    private final Map ioHandlers = new HashMap();
    /** Map of Node to Location objects*/
    private final Map memLocations = new HashMap();
    /** Map of Node to TaskCall objects */
    private final Map taskCalls = new HashMap();
    /** Map of Node to net.sf.openforge.app.project.Configurable object */
    private final Map configurableElements = new HashMap();
    
    public ResourceCache ()
    {}

    /**
     * Specifies that the given {@link Location} was created as the
     * implementation of the specified {@link Node}.  The specific
     * {@link LogicalMemory} in which the location is allocated may be
     * obtained directly from the Location object.
     *
     * @param node a non-null Node
     * @param loc a non-null Location
     */
    public void addLocation (Node node, Location loc)
    {
        this.memLocations.put(node, loc);
    }

    /**
     * Returns the {@link Location} that was defined for the Node
     * whose key attribute ({@see CacheBase#getNodeForString}) has the
     * specified value.
     *
     * @param nodeName a non-null String
     * @return the non-null Location which was associated with the
     * specified Node.
     */
    public Location getLocation (String nodeName)
    {
        return (Location)this.memLocations.get(getNodeForString(nodeName, this.memLocations));
    }
    
    /**
     * Specifies the specific {@link ActionIOHandler} which was
     * created as the implementation for the given {@link Node}.
     *
     * @param node a non-null Node
     * @param io a non-null ActionIOHandler
     */
    public void addIOHandler (Node node, ActionIOHandler io)
    {
        this.ioHandlers.put(node, io);
    }

    /**
     * Returns the {@link ActionIOHandler} that was defined for the
     * Node whose key attribute ({@see CacheBase#getNodeForString})
     * has the specified value.
     *
     * @param nodeName a non-null String
     * @return the non-null ActionIOHandler which was associated with
     * the specified Node.
     */
    public ActionIOHandler getIOHandler (String nodeName)
    {
        return (ActionIOHandler)this.ioHandlers.get(getNodeForString(nodeName, this.ioHandlers));
    }
    
    public void addTaskCall (Element node, TaskCall call)
    {
        this.taskCalls.put(node, call);
    }
    
    public Set<Element> getTaskCallNodes ()
    {
        return (Set<Element>)this.taskCalls.keySet();
    }
    
    public TaskCall getTaskCall (Element node)
    {
        return (TaskCall)this.taskCalls.get(node);
    }

    /**
     * If needed, the specified Node and Configurable are registered
     * as containing compiler configuration information.  This
     * information is annotated to the options database after the
     * entire design is constructed.
     */
    public void registerConfigurable (Node node, Configurable config)
    {
        NodeList nodeList = node.getChildNodes();
        for (int i=0; i < nodeList.getLength(); i++)
        {
            Node child = nodeList.item(i);
            if ((child instanceof Element) &&
                ((Element)child).getNodeName().equalsIgnoreCase(SLIMConstants.CONFIG_OPTION))
            {
                this.configurableElements.put(config, (Element)node);
            }
        }
    }

    /**
     * Returns a Map of Configurable to Element.  Each of the Elements
     * contains at least one child node of type:
     * {@link SLIMConstants#CONFIG_OPTION}.
     */
    public Map getConfigurableMap ()
    {
        return Collections.unmodifiableMap(this.configurableElements);
    }
    
}// ResourceCache
