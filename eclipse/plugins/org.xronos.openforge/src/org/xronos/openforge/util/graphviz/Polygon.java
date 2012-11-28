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
package org.xronos.openforge.util.graphviz;

/**
 * A polygon node. (Doesn't seem to be implemented yet...).
 * 
 * @author Stephen Edwards
 * @version $Id: Polygon.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Polygon extends Node {

	/**
	 * Constructs a new Polygon node.
	 * 
	 * @param id
	 *            the identifier of the node
	 */
	public Polygon(String id) {
		super(id, "polygon");
	}

	/**
	 * Sets the polygon's distortion.
	 */
	public void setDistortion(float distortion) {
		setAttribute("distortion", Float.toString(distortion));
	}

	/**
	 * Sets the number of sides in the polygon.
	 */
	public void setSides(int sides) {
		setAttribute("sides", Integer.toString(sides));
	}
}
