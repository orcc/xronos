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

package org.xronos.openforge.lim;

import java.util.Collection;
import java.util.Iterator;


import org.eclipse.core.runtime.jobs.Job;
import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.optimize.ComponentSwapVisitor;
import org.xronos.openforge.optimize.Optimization;
import org.xronos.openforge.util.naming.ID;

/**
 * ApiCallVisitor annotates information passed in by api method calls
 * <p>
 * <b>NOTE: More {@link ApiCallIdentifier} types shall be added if there are
 * more.</b>
 * 
 * @author ysyu
 * @version $Id: ApiCallVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ApiCallVisitor extends FilteredVisitor implements Optimization {

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		target.accept(this);
	}

	@Override
	public void visit(Design design) {
		super.visit(design);
	}

	@Override
	public void filter(Call call) {
		/* quit if call is not an API call */
		if (call.isForgeableAPICall())
			return;

		final Collection<Reference> enclosings = ((Block) call.getLineage()
				.iterator().next()).getProcedure().getCalls();

		/*
		 * Annotate the constant specifications to api call identifier
		 */
		for (Port port : call.getDataPorts()) {
			final Number number = ComponentSwapVisitor
					.getPortConstantValue(port);
			if (number != null) {
				for (ApiCallIdentifier aci : call.getApiIdentifiers().values()) {
					aci.setSpecification(number);
				}
			} else {
				EngineThread.getGenericJob().warn(
						"API value is not a constant.");
				return;
			}
		}

		/*
		 * Go through the api call identifier and identify the type and set the
		 * respective information to the enclosing method(s).
		 */
		for (ApiCallIdentifier apiId : call.getApiIdentifiers().values()) {

			/*
			 * NOTE: Add here if there are more api identifier types
			 */
			if (apiId.getTag().equals(
					call.getApiIdentifier(ApiCallIdentifier.THROUGHPUT_LOCAL,
							ID.showLogical(call)).getTag())) {
				setThroughputLocal(enclosings, call);
			} else {
				assert false : "Unknown API call identifier type "
						+ apiId.getTag();
			}
		}
	}

	/**
	 * Set the Throughput Local spacing of the enclosing methods of the api
	 * method call
	 * 
	 * @param enclosings
	 *            enclosing methods of the api call
	 * @param call
	 *            api call
	 */
	private void setThroughputLocal(Collection<Reference> enclosings, Call call) {
		for (Iterator<Reference> iter = enclosings.iterator(); iter.hasNext();) {
			final Call enclosingCall = (Call) iter.next();

			final ApiCallIdentifier aci = call.getApiIdentifier(
					ApiCallIdentifier.THROUGHPUT_LOCAL, ID.showLogical(call));
			enclosingCall.setThroughputLocal(aci.getSpecifications().iterator()
					.next().intValue());
		}
	}

	//
	// Optimization interface.
	//

	/**
	 * Reports, via {@link Job#info}, what optimization is being performed
	 */
	@Override
	public void preStatus() {
	}

	/**
	 * Reports, via {@link Job#verbose}, the results of <b>this</b> pass of the
	 * optimization.
	 */
	@Override
	public void postStatus() {
	}

	/**
	 * Returns false, the didModify is used to determine if this optimization
	 * caused a change which necessitates other optimizations to re-run.
	 */
	@Override
	public boolean didModify() {
		return false;
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void clear() {
	}

}
