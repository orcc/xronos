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

package net.sf.openforge.util;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * This is a wrapper for an collection. The underlying object my be emptied at
 * any time when no one holds a reference for it. Be careful to keep only a
 * persistant reference to this object, not to the GETlIST() OBJECT...
 * 
 */
public class SoftCollection {
	private SoftReference softHeader = new SoftReference(new LinkedList());
	private Class<?> refClass;

	/**
	 * Constructs an empty list.
	 */
	public SoftCollection(Class refClass) {
		if (!Collection.class.isAssignableFrom(refClass)) {
			throw new IllegalArgumentException("Must be a collection");
		}

		this.refClass = refClass;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param c
	 *            DOCUMENT ME!
	 */
	public void addAll(Collection c) {
		getCollection().addAll(c);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public final Collection getCollection() {
		Object o = softHeader.get();

		if (o == null) {
			try {
				Collection c = (Collection) refClass.newInstance();
				softHeader = new SoftReference(c);
				return c;
			} catch (Exception e) {
				throw new IllegalStateException(
						"Unable to new reference class instance");
			}
		} else {
			return (Collection) o;
		}
	}

	/**
	 * Simple class, has constructors, and the getLinkedList() call which is the
	 * crux of the mattter
	 */
	public static class LL extends SoftCollection {
		public LL() {
			super(LinkedList.class);
		}

		public LL(Collection c) {
			this();
			addAll(c);
		}

		public final LinkedList getLinkedList() {
			return (LinkedList) getCollection();
		}
	}

	/**
	 * ArrayList version
	 */
	public static class AL extends SoftCollection {
		public AL() {
			super(ArrayList.class);
		}

		public AL(Collection c) {
			this();
			addAll(c);
		}

		public final ArrayList getArrayList() {
			return (ArrayList) getCollection();
		}
	}
}
