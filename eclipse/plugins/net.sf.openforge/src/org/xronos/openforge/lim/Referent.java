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
package org.xronos.openforge.lim;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.util.naming.ID;


/**
 * A Referent is an entity that defines the behavior of one or more standin
 * operations called {@link Reference References}. This allows the Referent to
 * be defined once but used in different contexts. Each Reference is created by
 * its Referent, which allows the Referent to keep a record of all its
 * References. A Reference may be discarded with the method
 * {@link Referent#removeReference(Reference)}.
 * 
 * @author Stephen Edwards
 * @version $Id: Referent.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Referent extends ID implements Cloneable {

	/** Set of created References; References removed with remove(Reference) */
	private Set<Reference> references = new HashSet<Reference>(11);

	/**
	 * Constructs a Referent.
	 * 
	 * @param body
	 *            the contents of the referent
	 */
	public Referent() {
		super();
	}

	/**
	 * Gets all references that were created from this referent, except for
	 * those that were removed.
	 * 
	 * @return a collection of References
	 */
	public Collection<Reference> getReferences() {
		return Collections.unmodifiableCollection(references);
	}

	/**
	 * Removes a reference from this referent's list of known references.
	 * 
	 * @param reference
	 *            a reference that was created from this referent
	 */
	public void removeReference(Reference reference) {
		if (!references.remove(reference)) {
			throw new IllegalArgumentException("Removing Unknown reference: "
					+ reference);
		}
	}

	/**
	 * Gets the latency of a reference's exit.
	 */
	public abstract Latency getLatency(Exit exit);

	/**
	 * Adds a reference for this referent.
	 * 
	 * @param reference
	 *            a reference to be added to the list of known references
	 */
	protected void addReference(Reference reference) {
		references.add(reference);
	}

	/**
	 * Clones this referent and clears out the set of references maintained in
	 * the clone.
	 * 
	 * @return a Referent object
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		Referent clone = (Referent) super.clone();
		clone.references = new HashSet<Reference>(11);
		return clone;
	}

}
