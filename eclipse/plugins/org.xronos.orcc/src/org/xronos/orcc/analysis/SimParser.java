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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.graph.Vertex;

public class SimParser {

	private Network network;
	private String path;

	private Map<Instance, Map<Action, TimeGoDone>> execution;

	public SimParser(Network network, String path) {
		this.network = network;
		this.path = path;
		this.execution = new HashMap<Instance, Map<Action, TimeGoDone>>();
	}

	public void createMaps() {

		for (Vertex vertex : network.getVertices()) {
			if (vertex instanceof Instance) {
				Instance instance = (Instance) vertex;
				Map<Action, TimeGoDone> actionsGoDone = new HashMap<Action, TimeGoDone>();

				for (Action action : instance.getActor().getActions()) {
					TimeGoDone timeGoDone = new TimeGoDone();

					File actionFile = new File(path + File.separator
							+ instance.getSimpleName() + "_" + action.getName()
							+ ".txt");
					try {
						FileInputStream iStream = new FileInputStream(
								actionFile);
						BufferedReader iBuffer = new BufferedReader(
								new InputStreamReader(iStream));
						String str;
						while ((str = iBuffer.readLine()) != null) {
							String Time;
							String Go;
							String Done;
							int fIdx = str.indexOf(';', 0);
							Time = str.substring(0, fIdx);
							int sIdx = str.indexOf(';', fIdx + 1);
							Go = str.substring(fIdx + 1, sIdx);
							int tIdx = str.indexOf(';', sIdx + 1);
							Done = str.substring(sIdx + 1, tIdx);
							timeGoDone.put(Integer.decode(Time),
									Integer.decode(Go), Integer.decode(Done));
						}
						iBuffer.close();
						actionsGoDone.put(action, timeGoDone);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				execution.put(instance, actionsGoDone);
			}
		}
	}

	public Map<Instance, Map<Action, TimeGoDone>> getExecutionMap() {
		return this.execution;
	}

}
