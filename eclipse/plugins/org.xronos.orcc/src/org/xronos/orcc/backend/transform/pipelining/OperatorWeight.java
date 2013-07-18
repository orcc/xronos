/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.xronos.orcc.backend.transform.pipelining;

import org.eclipse.emf.common.util.Enumerator;

/**
 * A class that defines two weights for an operator, resource cost and time cost
 * 
 * @author Endri Bezati
 * 
 */
public class OperatorWeight {

	/**
	 * Op Binary or Unary
	 */
	private Enumerator operator;

	/**
	 * Time of the operator
	 */
	private float time;

	/**
	 * Cost of the operator
	 */
	private float cost;

	public OperatorWeight(Enumerator operator, float time, float cost) {
		this.operator = operator;
		this.time = time;
		this.cost = cost;
	}

	/**
	 * Get the cost of the operator
	 * 
	 * @return
	 */
	public float getCost() {
		return cost;
	}

	/**
	 * Get the operator
	 * 
	 * @return
	 */
	public Enumerator getOperator() {
		return operator;
	}

	/**
	 * Get the time of the operator
	 * 
	 * @return
	 */
	public float getTime() {
		return time;
	}

	public String print() {
		return toString();
	}

	@Override
	public String toString() {
		return "Op(" + operator + ", " + time + ", " + cost + ")";
	}
}
