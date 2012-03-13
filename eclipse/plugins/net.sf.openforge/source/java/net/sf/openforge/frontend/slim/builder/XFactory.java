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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sf.openforge.lim.Component;
import net.sf.openforge.util.naming.ID;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XFactory is the common superclass of all the factory classes used in
 * converting SLIM to LIM structures. There are several methods here that are of
 * common use to the factories.
 * 
 * 
 * <p>
 * Created: Wed Jul 13 15:16:14 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public abstract class XFactory {

	protected XFactory() {
	}

	/**
	 * Returns a List of all the children of the specified Node which have a
	 * node name (ie are the element type) specified by the given String tag.
	 * 
	 * @param node
	 *            the Parent Node
	 * @param tag
	 *            the String tag (element type) to be used as a filter on the
	 *            children
	 * @return a non-null List of Node objects, all of which have the specified
	 *         Node as a parent and whose node name (element type) are the
	 *         specified string tag.
	 */
	protected List<Node> getChildNodesByTag(Node node, String tag) {
		return getChildNodesByTag(node, Collections.singleton(tag));
	}

	/**
	 * Returns a List of all the children of the specified Node which have a
	 * node name (ie are the element type) specified by one of the String tags
	 * in the collection.
	 * 
	 * @param node
	 *            the Parent Node
	 * @param tags
	 *            a Collection of String tags (element type) to be used as a
	 *            filter on the children
	 * @return a non-null List of Node objects, all of which have the specified
	 *         Node as a parent and whose node name (element type) is one of the
	 *         specified string tags
	 */
	protected List<Node> getChildNodesByTag(Node node, Collection<String> tags) {
		List<Node> match = new ArrayList<Node>();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (tags.contains(child.getNodeName())) {
				match.add(child);
			}
		}
		return match;
	}

	/**
	 * Returns true of the {@link SLIMConstants#TYPENAME} attribute of the
	 * specified Node is recognized as a signed data type.
	 * 
	 * @param portNode
	 *            a non-null Node which has an attribute
	 *            {@link SLIMConstants#TYPENAME}
	 * @return true for signed types, false otherwise
	 */
	protected static final boolean isSignedPort(Node portNode) {
		String type = ((Element) portNode).getAttribute("typeName");

		if (type.equals("int")) {
			return true;
		} else if (type.equals("bool")) {
			return false;
		} else if (type.equals("uint")) {
			return false;
		} else {
			String tag = "unknown tag";
			try {
				tag = ((Element) portNode).getAttribute("tag");
			} catch (Exception e) {
			}
			assert false : "Illegal port node typeName " + type + " of " + tag;
			return true;
		}
	}

	/**
	 * Returns the bitwidth of the type specified in the
	 * {@link SLIMConstants#TYPENAME} attribute of the specified Node.
	 * 
	 * @param portNode
	 *            a non-null Node which has an attribute
	 *            {@link SLIMConstants#TYPENAME}
	 * @return a positive int
	 */
	protected static final int getPortSize(Node portNode) {
		String tag = "unknown tag";
		try {
			tag = ((Element) portNode).getAttribute("tag");
		} catch (Exception e) {
		}

		String type = ((Element) portNode).getAttribute(SLIMConstants.TYPENAME);
		String size = ((Element) portNode)
				.getAttribute(SLIMConstants.PORT_SIZE);

		int width = -1;
		try {
			width = Integer.parseInt(size);
		} catch (NumberFormatException nfe) {
			System.out.println("Warning.  Element with tag '" + tag
					+ "' has no size attribute.  Deriving size from typeName");

			if (type.equalsIgnoreCase("int")) {
				width = 32;
			} else if (type.equalsIgnoreCase("uint")) {
				width = 1;
			} else if (type.equalsIgnoreCase("bool")) {
				width = 1;
			}
		}
		if (width < 0) {
			assert false : "Illegal port node size '" + size
					+ "' or typeName '" + type + "' of " + tag;
		}

		return width;

		/*
		 * if (type.equals("int")) { return 32; } else if (type.equals("bool"))
		 * { return 1; } else { String tag = "unknown tag"; try { tag =
		 * ((Element)portNode).getAttribute("tag"); } catch (Exception e){}
		 * assert false : "Illegal port node typeName " + type + " of " + tag;
		 * return -1; }
		 */
	}

	protected final void setAttributes(Node node, ID comp) {
		if (!(node instanceof Element))
			return;

		Element element = (Element) node;

		if (element.getAttribute("sourceName").length() > 0) {
			comp.setSourceName(element.getAttribute("sourceName"));
		} else if (element.getAttribute(SLIMConstants.NAME).length() > 0) {
			comp.setSourceName(element.getAttribute(SLIMConstants.NAME));
		} else if (element.getAttribute("tag").length() > 0) {
			comp.setSourceName(element.getAttribute("tag"));
		}
	}

	protected final void setAttributes(Node node, Component comp) {
		setAttributes(node, (ID) comp);

		if (!(node instanceof Element))
			return;

		Element element = (Element) node;

		if (element.getAttribute(SLIMConstants.REMOVABLE).toUpperCase()
				.equals("NO")) {
			comp.setNonRemovable();
		}
	}

	@SuppressWarnings("serial")
	protected static class IllegalNodeConfigurationException extends
			RuntimeException {
		public IllegalNodeConfigurationException(String msg) {
			super(msg);
		}
	}

}// XFactory
