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
package org.xronos.openforge.report;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;

/**
 * DesignResource is responsible to report all resources in a Design
 * 
 * @author ysyu
 * @version $Id: DesignResource.java 132 2006-04-10 15:43:06Z imiller $
 */
public class DesignResource extends ResourceBank {

	/** The design that owns the resources */
	private Design design = null;

	public DesignResource(Design design) {
		super();
		this.design = design;
	}

	/**
	 * Proceduces a mapping of resource counts by total up all the resources in
	 * task(s) of this design.
	 * 
	 * @return a mapurces to their counts in this design
	 */
	@Override
	public Map<Class<Object>, Set<Component>> generateReport() {
		Map<Class<Object>, Set<Component>> total = new HashMap<Class<Object>, Set<Component>>();
		for (Object element : getResources()) {
			TaskResource tResource = (TaskResource) element;
			Map<Class<Object>, Set<Component>> tReport = tResource
					.generateReport();
			for (Class<Object> element2 : tReport.keySet()) {
				Class<Object> klass = element2;
				if (total.containsKey(klass)) {
					Set<Component> left = total.get(klass);
					Set<Component> right = tReport.get(klass);
					Set<Component> combined = new HashSet<Component>();
					combined.addAll(left);
					combined.addAll(right);
					total.put(klass, combined);
				} else {
					total.put(klass, tReport.get(klass));
				}
			}
		}
		return total;
	}

	/**
	 * @return the design that owns these resources.
	 */
	public Design getDesign() {
		return design;
	}
}
