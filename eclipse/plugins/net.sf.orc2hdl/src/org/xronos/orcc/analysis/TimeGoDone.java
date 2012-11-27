/*
 * Copyright (c) 2011, Ecole Polytechnique Fédérale de Lausanne
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

package org.xronos.orcc.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map;

/**
 * This class contains the Go and Done signal of a given action at a specific
 * time.
 * 
 * @author Endri Bezati
 * 
 */
public class TimeGoDone implements Iterable<ArrayList<Integer>>{

	private Map<Integer, ArrayList<Integer>> timeGoDoneMap;

	public TimeGoDone() {
		this.timeGoDoneMap = new TreeMap<Integer, ArrayList<Integer>>();
	}

	public ArrayList<Integer> get(Integer Time) {
		ArrayList<Integer> returnList = timeGoDoneMap.get(Time);
		return returnList;
	}

	public void put(Integer Time, Integer Go, Integer Done) {
		ArrayList<Integer> goDone = new ArrayList<Integer>();
		goDone.add(0, Go);
		goDone.add(1, Done);
		timeGoDoneMap.put(Time, goDone);
	}
	
	public Integer size(){
		return timeGoDoneMap.size();
		
	}

	@Override
	public Iterator<ArrayList<Integer>> iterator() {
		return timeGoDoneMap.values().iterator();
	}
}