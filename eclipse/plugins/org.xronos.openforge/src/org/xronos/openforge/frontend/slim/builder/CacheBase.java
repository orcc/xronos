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

package org.xronos.openforge.frontend.slim.builder;

import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * CacheBase is the common superclass for all cache classes which are mapping
 * document {@link Node}s to LIM classes.
 * 
 * 
 * <p>
 * Created: Tue Jul 12 16:12:08 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public abstract class CacheBase {

	protected CacheBase() {
	}

	/**
	 * Returns the {@link Node} object which is a key to this cache whose value
	 * for the attribute defined by {@link SLIMConstants#NAME} is equal to the
	 * value of 'nameId'.
	 * 
	 * @param nameId
	 *            a non-null String
	 * @param cache
	 *            the Map, containing keys of type {@link Node} to be searched.
	 * @return a value of type 'Node'
	 * @throws IllegalStateException
	 *             if no matching Node is found
	 */
	protected Node getNodeForString(String nameId, Map<Node, ?> cache) {
		if (nameId == null)
			throw new IllegalArgumentException("key id cannot be null");

		if (_parser.db)
			System.out.println("Looking for " + nameId + " in " + this);
		// Find the Node which goes along with the name
		Node key = null;
		for (Node node : cache.keySet()) {
			NamedNodeMap attrs = node.getAttributes();
			String name = attrs.getNamedItem(SLIMConstants.NAME).getNodeValue();
			if (_parser.db)
				System.out.println("\tTesting " + name);
			if (name.equals(nameId)) {
				key = node;
				break;
			}
		}

		if (key == null)
			throw new UnknownKeyException("Unknown name '" + nameId + "' in "
					+ cache.keySet());

		return key;
	}

	@SuppressWarnings("serial")
	public static class UnknownKeyException extends RuntimeException {
		public UnknownKeyException(String msg) {
			super(msg);
		}
	}

}// CacheBase
