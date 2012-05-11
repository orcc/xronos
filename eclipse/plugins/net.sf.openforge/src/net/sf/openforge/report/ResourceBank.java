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
package net.sf.openforge.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ResourceBank tracks the resources within a graph
 * 
 * @author ysyu
 * @version $Id: ResourceBank.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class ResourceBank {

	/** a list of resources */
	private List<Object> resources = new ArrayList<Object>();

	public ResourceBank() {
	}

	/**
	 * @return a mapping of resources to their counts
	 */
	public abstract Map generateReport();

	/**
	 * Add a resource discovered in a graph
	 * 
	 * @param o
	 *            a resource
	 */
	public void addResource(Object o) {
		resources.add(o);
	}

	/**
	 * @return a list of resources used in a graph
	 */
	public List<Object> getResources() {
		return resources;
	}
}
