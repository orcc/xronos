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
 * A Reference is an {@link Operation} which stands in for a {@link Referent}
 * component that is defined outside the current context. The Reference executes
 * by deferring its behavior to its Reference.
 * 
 * @author Stephen Edwards
 * @version $Id: Reference.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Reference extends Operation {

	/** The referenced entity */
	private Referent referent;

	/**
	 * Gets the Referent referenced by this Reference, may be null.
	 */
	public Referent getReferent() {
		return referent;
	}

	protected Reference() {
	}

	/**
	 * Constructs a new Reference for a given Reference.
	 */
	protected Reference(Referent referent, int dataPortCount) {
		super(dataPortCount);
		this.referent = referent;
	}

	/**
	 * Redirects this reference to the given {@link Referent}, replacing the
	 * existing referent, if any.
	 * 
	 * @param ref
	 *            a value of type 'Referent'
	 */
	public abstract void setReferent(Referent ref);

	/**
	 * Used by concrete classes to replace the referent field and to remove this
	 * Reference from the {@link Referent Referent's} list of accessing
	 * References.
	 * 
	 * @param ref
	 *            a {@link Referent} which can be set to null to remove this
	 *            reference from all references.
	 */
	protected void setRef(Referent ref) {
		if (this.referent != null) {
			this.referent.removeReference(this);
		}
		this.referent = ref;
		if (ref != null) {
			this.referent.addReference(this);
		}
	}

	public void accept(Visitor v) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a copy of this Reference to the <b>same</b> {@link Referent} as
	 * the original node, but does not add the clone to the list of references
	 * stored by the Referent.
	 * 
	 * @return a value of type 'Object'
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	public Object clone() throws CloneNotSupportedException {
		Reference clone = (Reference) super.clone();
		return clone;
	}

}
