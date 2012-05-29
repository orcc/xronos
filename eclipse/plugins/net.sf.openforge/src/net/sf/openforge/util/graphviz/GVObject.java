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
package net.sf.openforge.util.graphviz;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The base class for graphs, nodes, and edges that specifies their common
 * attributes.
 * 
 * @author Stephen Edwards
 * @version $Id: GVObject.java 2 2005-06-09 20:00:48Z imiller $
 */
abstract class GVObject {

	/** Attributes stored as a property list */
	protected Properties properties;

	/**
	 * Constructs a new GVObject with no attributes specified.
	 */
	GVObject() {
		properties = new Properties();
	}

	/**
	 * Gets the color of this object.
	 */
	public String getColor() {
		return getAttribute("color", "black");
	}

	/**
	 * Sets the color of this object.
	 */
	public void setColor(String color) {
		setAttribute("color", color);
	}

	/**
	 * Gets the displayed label of this object.
	 */
	public String getLabel() {
		return getAttribute("label");
	}

	/**
	 * Sets the displayed label of this object.
	 */
	public void setLabel(String label) {
		setAttribute("label", label);
	}

	/**
	 * set arbitrary attributes
	 */
	public void setGVAttribute(String attr, String value) {
		// System.err.println("Using advanced option setGVAttribute!");
		properties.setProperty(attr, "\"" + value + "\"");
	}

	protected void setAttribute(String attr, String value) {
		properties.setProperty(attr, value);
	}

	protected String getAttribute(String attr) {
		return properties.getProperty(attr);
	}

	protected String getAttribute(String attr, String defaultVal) {
		return properties.getProperty(attr, defaultVal);
	}

	void printAttributes(PrintWriter out) {
		if (!properties.isEmpty()) {
			out.print("[");
			for (Enumeration<?> enumeration = properties.propertyNames(); enumeration
					.hasMoreElements();) {
				String attr = (String) enumeration.nextElement();
				out.print(attr);
				out.print("=");
				if (attr.equals("label")) {
					out.print("\"");
				}
				out.print(getAttribute(attr));

				if (attr.equals("label")) {
					out.print("\"");
				}
				if (enumeration.hasMoreElements()) {
					out.print(", ");
				}
			}
			out.print("]");
		}
	}
}
