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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.Procedure;

/**
 * @author ysyu
 * @version $Id: ProcedureResource.java 132 2006-04-10 15:43:06Z imiller $
 */
public class ProcedureResource extends ResourceBank {

	/** the procedure that owns these resources */
	private Procedure procedure = null;

	/** all resouces to counts including sub method resources */
	private Map total_report = new HashMap();

	public ProcedureResource(Procedure proc) {
		super();
		procedure = proc;
	}

	/**
	 * Produces a mapping of resource to counts only for the resources in this
	 * Procedure.
	 * 
	 * @return a mapping of resources counts.
	 */
	@Override
	public Map generateReport() {
		Map resourceTypeToInstances = new HashMap();

		for (Iterator iter = getResources().iterator(); iter.hasNext();) {
			Object o = iter.next();
			if (o instanceof ProcedureResource) {
			} else {
				Class klass = o.getClass();
				if (resourceTypeToInstances.containsKey(klass)) {
					((Set) resourceTypeToInstances.get(klass)).add(o);
				} else {
					Set set = new HashSet();
					set.add(o);
					resourceTypeToInstances.put(klass, set);
				}
			}
		}
		return resourceTypeToInstances;
	}

	/**
	 * NOTE: should be only called once per Procedure
	 * 
	 * Produces a mapping of all resouce counts including sub method resources
	 * 
	 * @return a mapping of resources to its counts
	 */
	public Map generateTotalReport() {
		Map sub_report = null;
		Map current_report = generateReport();
		total_report = current_report;
		for (Object o : getResources()) {
			if (o instanceof ProcedureResource) {
				sub_report = ((ProcedureResource) o).getTotalReport();
				for (Iterator aiter = sub_report.keySet().iterator(); aiter
						.hasNext();) {
					Class klass = (Class) aiter.next();
					Set sub_set = (Set) sub_report.get(klass);
					if (total_report.containsKey(klass)) {
						((Set) total_report.get(klass)).addAll(sub_set);
					} else {
						total_report.put(klass, new HashSet(sub_set));
					}
				}
			}
		}
		return total_report;
	}

	/**
	 * @return a mapping of all resource counts, including sub methods
	 */
	public Map getTotalReport() {
		return total_report;
	}

	/**
	 * @return the Procedure that owns these resources
	 */
	public Procedure getProcedure() {
		return procedure;
	}
}
