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

package net.sf.openforge.lim;


import java.util.*;

import net.sf.openforge.app.*;
import net.sf.openforge.optimize.*;
import net.sf.openforge.util.naming.*;

/**
 * ApiCallVisitor annotates information passed in by api method calls
 * <p><b>NOTE: More {@link ApiCallIdentifier} types shall be
 * added if there are more.</b>
 * 
 * @author ysyu
 * @version $Id: ApiCallVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ApiCallVisitor extends FilteredVisitor implements Optimization
{
    /** Revision */
    private static final String _RCS_ = "$Rev: 2 $";


    /**
     * Applies this optimization to a given target.
     *
     * @param target the target on which to run this optimization
     */
    public void run (Visitable target)
    {
        target.accept(this);
    }

    public void visit (Design design)
    {
        super.visit(design);
    }

    public void filter (Call call)
    {
        /* quit if call is not an api call */
        if(call.isForgeableAPICall()) return;

        final Collection enclosings = ((Block)call.getLineage().iterator().next()).getProcedure().getCalls();

        /*
         * Annotate the constant specifications to api call identifier
         */
        for(Iterator piter = call.getDataPorts().iterator(); piter.hasNext();)
        {
            final Port port = (Port)piter.next();
            final Number number = ComponentSwapVisitor.getPortConstantValue(port);
            if(number != null)
            {
                for(Iterator iter = call.getApiIdentifiers().values().iterator(); iter.hasNext();)
                {
                    final ApiCallIdentifier aci = (ApiCallIdentifier)iter.next();
                    aci.setSpecification(number);
                }
            }
            else
            {
            	EngineThread.getGenericJob().warn("API value is not a constant.");
                return;
            }
        }

        /*
         * Go through the api call identifier and identify the type and set the
         * respective information to the enclosing method(s).
         */
        for(Iterator it = call.getApiIdentifiers().values().iterator(); it.hasNext();)
        {
            final ApiCallIdentifier apiId = (ApiCallIdentifier)it.next();

            /*
             * NOTE: Add here if there are more api identifier types
             */
            if(apiId.getTag().equals(call.getApiIdentifier(ApiCallIdentifier.THROUGHPUT_LOCAL, 
                                                           ID.showLogical(call)).getTag()))
            {
                setThroughputLocal(enclosings, call);
            }
            else
            {
                assert false : "Unknown API call identifier type " + apiId.getTag();
            }
        }
    }
    
    /**
     * Set the Throughput Local spacing of the enclosing methods of the api method call
     *
     * @param enclosings enclosing methods of the api call
     * @param call api call
     */
    private void setThroughputLocal(Collection enclosings, Call call)
    {
        for(Iterator iter = enclosings.iterator(); iter.hasNext();)
        {
            final Call enclosingCall = (Call)iter.next();
            
            final ApiCallIdentifier aci = call.getApiIdentifier(ApiCallIdentifier.THROUGHPUT_LOCAL, 
                                                                ID.showLogical(call));
            enclosingCall.setThroughputLocal(((Number)aci.getSpecifications().iterator().next()).intValue());
        }
    }

    //
    // Optimization interface.
    //
    
    /**
     * Reports, via {@link Job#info}, what optimization is being
     * performed
     */
    public void preStatus ()
    {
    }
    
    /**
     * Reports, via {@link Job#verbose}, the results of <b>this</b>
     * pass of the optimization.
     */
    public void postStatus ()
    {
    }
    
    /**
     * Returns false, the didModify is used to determine if this
     * optimization caused a change which necessitates other
     * optimizations to re-run.
     */
    public boolean didModify ()
    {
        return false;
    }
    /**
     * Does nothing.
     */
    public void clear () {;}
    
}


