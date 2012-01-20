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

/**
 * An Access is a {@link Reference} to a {@link Resource}.  It
 * represents a call by reference; the HDL that is generated must
 * include explicit connections between the Access and its
 * Resource.
 *
 * @author  Stephen Edwards
 * @version $Id: Access.java 88 2006-01-11 22:39:52Z imiller $
 */
public abstract class Access extends Reference implements Referencer
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 88 $";

    /** True if this is an access to a volatile memory location */
    private boolean isVolatile = false;

    /**
     * Constructs a new Access for a given Resource.
     * @param isVolatile true if this is an access to a volatile memory location
     */
    public Access (Resource resource, int dataPortCount, boolean isVolatile)
    {
        super(resource, dataPortCount);
        this.isVolatile = isVolatile;
    }
    
    /**
     * Gets the {@link Resource} that is targetted by this Access, may
     * be null.
     */
    public Resource getResource ()
    {
        return (Resource)getReferent();
    }

    /**
     * Returns the targetted resource as a {@link Referenceable}
     * object. 
     *
     * @return a value of type 'Referenceable'
     */
    public Referenceable getReferenceable ()
    {
        return this.getResource();
    }
    
    /**
     * Removes this Access from the targetted Resource, or does
     * nothing if the targetted resource is null.
     */
    public void removeFromResource ()
    {
        if (getResource() != null)
        {
            getResource().removeReference(this);
        }
    }

    /**
     * Returns the latency reported by the {@link Referent} of this
     * Reference.
     *
     * @return the {@link Latency} of the accessed referent or ZERO if
     * the resource is null.
     */
    public Latency getLatency ()
    {
        assert getResource() != null : "Intenal state error.  Access has nothing to reference";
        return getResource().getLatency(null);
    }

    /**
     * Tests whether this is an access to a volatile memory location.
     */
    public boolean isVolatile ()
    {
        return isVolatile;
    }

    /**
     * Simply re-sets the {@link Referent} of this Reference.
     *
     * @param ref the new target Referent
     */
    public void setReferent(Referent ref)
    {
        super.setRef(ref);
    }
    

    public void setResource (Resource resource)
    {
        setReferent(resource);
    }

    /**
     * Gets the resources accessed by or within this component.
     *
     * @return a collection of {@link Resource} or the empty
     * collection if the targetted resource is null.
     */
    public Collection getAccessedResources ()
    {
        assert getResource() != null : "Intenal state error.  Access has nothing to reference";
        return Collections.singletonList(getResource());
    }
}
