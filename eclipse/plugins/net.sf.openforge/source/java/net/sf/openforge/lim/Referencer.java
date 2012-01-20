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

/**
 * Referencer is an interface that is implemented by any LIM {@link
 * Component} which represents an inferred connection to a {@link
 * Referenceable} target.
 *
 * <p>Created: Tue Jan 20 08:27:55 2004
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: Referencer.java 62 2005-11-17 18:10:30Z imiller $
 */
public interface Referencer 
{
    static final String _RCS_ = "$Rev: 62 $";

    /**
     * Returns the {@link Referenceable} target of this Referencer.
     */
    public Referenceable getReferenceable ();

    /** Returns true if this reference to the {@link Referenceable} is
     * a sequence enforcing point (eg it modifies the state of the
     * resource) false if other non-sequencing {@link Referencer}
     * accesses can occur in parallel with this access */
    public boolean isSequencingPoint ();
    
}// Referencer
