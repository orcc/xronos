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
 * A Resource is a {@link Component} wrapper that allows the Component to be
 * accessed by the execution of one or more {@link Access Accesses} that
 * reference the Resource. All Accesses to the same Resource share that
 * Resource.
 * <P>
 * A Resource represents the target of a call-by-reference. During the
 * generation of HDL, explicit connections must be made from each Access to its
 * Resource. The Resource is responsible for arbitrating accesses to its
 * component.
 * <p>
 * The functionality of this class has been largely subsumed into the Referent
 * class and the Referenceable interface.
 * 
 * @author Stephen Edwards
 * @version $Id: Resource.java 282 2006-08-14 21:25:33Z imiller $
 */
public abstract class Resource extends Referent implements Referenceable {
	private static final String rcs_id = "RCS_REVISION: $Rev: 282 $";

	/**
	 * Constructs a new Resource.
	 */
	public Resource() {
		super();
	}

	/**
	 * interface to push clock and reset to shared resources
	 */
}
