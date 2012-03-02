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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * LADomain. This is the domain or scope of a listener/adapter tuple.
 * 
 * @author C. Schanck
 * @version $Id: LADomain.java 2 2005-06-09 20:00:48Z imiller $
 */
public class LADomain {
	private final Object tag;
	private ArrayList listeners = new ArrayList();
	private static final String nullTag = "";

	/**
	 * Constructor. Pass in a tag object which might be simple (like a string)
	 * or complicated (like an entire preference set). This tag object will be
	 * available on each adaptor callback.
	 * 
	 * @param tag
	 *            Any object which should be made available to the callbacks
	 */
	public LADomain(Object tag) {
		// record the tag
		if (tag == null)
			tag = nullTag;
		this.tag = tag;
	}

	/**
	 * Construtor for a domain with no tag.
	 * 
	 */
	public LADomain() {
		this(null);
	}

	/**
	 * Add a Listener/Adapter handler to the list. The same LAHandleable may be
	 * added more than once; in this case it will fire more than once per event.
	 * 
	 * @param lah
	 *            a value of type 'LAHandleable'
	 */
	public void add(LAHandleable lah) {
		listeners.add(lah);
	}

	/**
	 * Remove a Listener/Adapter handler from the list. Removing a non-existent
	 * handler does not through an error.
	 * 
	 * @param lah
	 *            a value of type 'LAHandleable'
	 */
	public void remove(LAHandleable lah) {
		int i = listeners.indexOf(lah);
		if (i >= 0)
			listeners.remove(i);

	}

	public final Iterator getListenerIterator() {
		return listeners.iterator();
	}

	/**
	 * Fires all the adapters currently listening for this domain.
	 * 
	 * @param passthru
	 *            Object to passthru.
	 */

	public synchronized void fire(Object passthru) {
		// for all listeners
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			// get the handler
			LAHandleable lah = (LAHandleable) it.next();
			// call default
			lah.laListen(tag, passthru);
		}
	}

}
