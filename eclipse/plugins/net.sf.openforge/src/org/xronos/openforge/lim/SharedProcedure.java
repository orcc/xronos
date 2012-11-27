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

/**
 * A SharedProcedure allows a {@link Procedure} definition to be shared by
 * multiple {@link SharedProcedureCall SharedProcedureCalls}. All references to
 * the same SharedProcedure access the same hardware instantiated by that
 * procedure.
 * 
 * @author Stephen Edwards
 * @version $Id: SharedProcedure.java 538 2007-11-21 06:22:39Z imiller $
 */
public class SharedProcedure extends Resource {

	/** The procedure to be shared by all calls */
	private Procedure procedure;

	public SharedProcedure(Procedure procedure) {
		super();
		this.procedure = procedure;
	}

	public Procedure getProcedure() {
		return procedure;
	}

	/**
	 * Gets the latency of a reference's exit.
	 */
	@Override
	public Latency getLatency(Exit exit) {
		/*
		 * tbd.
		 */
		return Latency.ZERO;
	}

	/**
	 * Gets the pipeline latency for a particular reference to this referent.
	 */
	public Latency getPipelineLatency(Reference reference) {
		/*
		 * tbd.
		 */
		return Latency.ONE;
	}

	public SharedProcedureCall makeCall() {
		SharedProcedureCall call = new SharedProcedureCall(this);
		addReference(call);
		return call;
	}

	/**
	 * Gets the calls to this procedure.
	 * 
	 * @return a collection of SharedProcedureCalls
	 */
	public Collection<Reference> getCalls() {
		return getReferences();
	}

	/**
	 * Creates a copy of this shared procedure (including a copy of the backing
	 * {@link Procedure}).
	 * 
	 * @return a SharedProcedure object.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@Override
	public int getSpacing(Referencer from, Referencer to) {
		throw new UnsupportedOperationException(
				"Shared procedure unsupported.  Call to unsupported method");
	}

	@Override
	public int getGoSpacing(Referencer from, Referencer to) {
		throw new UnsupportedOperationException(
				"Shared procedure unsupported.  Call to unsupported method");
	}

}
