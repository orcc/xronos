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
 * XCallFactory is an extension of the XModuleFactory which wraps the
 * module in a {@link Procedure} and {@link Call} and returns the
 * built Call.  The publishing of Ports to the PortCache is also
 * handled such that the externally visible ports of the module are
 * the call ports.
 *
 * <p>Created: Wed Sep  7 15:26:24 2005
 *
 * @author imiller, last modified by $Author: imiller $
 */
public class XCallFactory extends XModuleFactory
{

    private Call generatedCall = null;
    
    public XCallFactory (ResourceCache cache)
    {
        super(cache, Exit.RETURN);
    }

    public Component buildComponent (Node moduleNode)
    {
        Block procedureBlock = (Block)super.buildComponent(moduleNode);

        // Create a procedure out of the block and use it to
        // populate a new 'task' in the design
        Procedure proc = new Procedure(procedureBlock);
        Call call = proc.makeCall();
        // Give the procedure and call some meaningful IDSourceInfo so
        // that it will have a reasonable searchLabel for assigning
        // attributes to.
        proc.setIDSourceInfo(deriveIDSourceInfo(moduleNode));

        // Fix the port cache by substituting the call ports for the
        // block ports
        PortCache cache = getPortCache();
        for (Iterator iter = procedureBlock.getPorts().iterator(); iter.hasNext();)
        {
            Port blockPort = (Port)iter.next();
            Port callPort = call.getPortFromProcedurePort(blockPort);
            assert blockPort != null;
            assert callPort != null;
            cache.replaceTarget(blockPort, callPort);
        }
        for (Iterator iter = procedureBlock.getExits().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            for (Iterator busIter = exit.getBuses().iterator(); busIter.hasNext();)
            {
                Bus blockBus = (Bus)busIter.next();
                Bus callBus = call.getBusFromProcedureBus(blockBus);
                cache.replaceSource(blockBus, callBus);
            }
        }

        this.generatedCall = call;

        //getResourceCache().registerConfigurable(moduleNode, call);
        getResourceCache().registerConfigurable(moduleNode, proc);
        
        return call;
    }

    /**
     * This method is used by the {@link #publishPorts} method to
     * identify which ports are to be published.  We must override the
     * super in order to specify the call ports, otherwise the ports
     * of the procedure module would be used.
     *
     * @return a Collection of Port and Bus objects.
     */
    protected Collection getExternallyVisiblePorts ()
    {
        final Set ports = new HashSet(this.generatedCall.getPorts());
        ports.addAll(this.generatedCall.getBuses());
        return ports;
    }

    protected static IDSourceInfo deriveIDSourceInfo (Node moduleNode)
    {
        // First get the 'class' name from the design element
        Node designNode = null;
        Node current = moduleNode;
        while (current != null)
        {
            if (current.getNodeName().equalsIgnoreCase("design"))
            {
                designNode = current;
                break;
            }
            current = current.getParentNode();
        }
        String fileName = null;
        String packageName = null;
        String className = ((Element)designNode).getAttribute(SLIMConstants.NAME);
        String methodName = ((Element)moduleNode).getAttribute(SLIMConstants.NAME);
        String signature = null;
        int line = 0;
        int cpos = 0;
        return new IDSourceInfo(fileName, packageName, className, methodName, signature, line, cpos);
    }
    
}// XCallFactory
