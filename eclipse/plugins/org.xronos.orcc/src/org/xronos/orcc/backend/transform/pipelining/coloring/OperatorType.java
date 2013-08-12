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

package org.xronos.orcc.backend.transform.pipelining.coloring;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * This class defines different parameters for an operator
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class OperatorType {

	/**
	 * The operators name
	 */
	private String name;

	/**
	 * The operators time cost
	 */
	private float time;

	/**
	 * The operators cost
	 */
	private float cost;

	/**
	 * Count the copies
	 */
	private int count;

	public OperatorType() {
		name = "t0";
		time = 0.0f;
		cost = 0.0f;
		count = 0;
	}

	public OperatorType(String name, float time, float cost) {
		this.name = name;
		this.time = time;
		this.cost = cost;
		count = 0;
	}

	/**
	 * Add copy
	 */

	public void addCopy() {
		count++;
	}

	/**
	 * Get this operator name
	 * 
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get this operator time
	 * 
	 * @return
	 */
	public float getTime() {
		return this.time;
	}

	/**
	 * Print this operator type
	 * 
	 * @param out
	 * @return
	 */
	public boolean print(BufferedWriter out) {
		try {
			out.write(name + " - " + time + " - " + cost + " - " + count);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Remove copy
	 */
	public void removeCopy() {
		count--;
	}

	@Override
	public String toString() {
		return name + " - " + time + " - " + cost + " - " + count;
	}

}
