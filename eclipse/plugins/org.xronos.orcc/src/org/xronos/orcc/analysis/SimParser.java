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
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.util.OrccLogger;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class SimParser {

	private Network network;
	private String path;

	Map<Actor, Map<Action, SummaryStatistics>> statistics;

	public SimParser(Network network, String path) {
		this.network = network;
		this.path = path;
		this.statistics = new HashMap<Actor, Map<Action, SummaryStatistics>>();
	}

	public void createMaps() {

		for (Actor actor : network.getAllActors()) {
			Map<Action, SummaryStatistics> aTimeGoDone = new HashMap<Action, SummaryStatistics>();
			for (Action action : actor.getActions()) {
				OrccLogger.noticeln("Parsing weight: " + actor.getSimpleName()
						+ "_" + action.getName());
				SummaryStatistics tGoDone = new SummaryStatistics();

				File actionFile = new File(path + File.separator
						+ actor.getSimpleName() + "_" + action.getName()
						+ ".txt");
				try {
					FileInputStream iStream = new FileInputStream(actionFile);
					BufferedReader iBuffer = new BufferedReader(
							new InputStreamReader(iStream));
					String str;
					int startTime = 0;
					Boolean fromOneZero = false;
					while ((str = iBuffer.readLine()) != null) {

						int fIdx = str.indexOf(';', 0);
						String stringTime = str.substring(0, fIdx);
						int sIdx = str.indexOf(';', fIdx + 1);
						String stringGo = str.substring(fIdx + 1, sIdx);
						int tIdx = str.indexOf(';', sIdx + 1);
						String stringDone = str.substring(sIdx + 1, tIdx);

						int intTime = Integer.decode(stringTime);
						int intGo = Integer.decode(stringGo);
						int intDone = Integer.decode(stringDone);

						if (intGo == 1 && intDone == 0) {
							startTime = intTime;
							fromOneZero = true;
						} else if (intGo == 1 && intDone == 1) {
							if (fromOneZero) {
								tGoDone.addValue((intTime - startTime) / 100);
								startTime = intTime;
							} else {
								tGoDone.addValue(0);
							}
						} else if (intGo == 0 && intDone == 1) {
							fromOneZero = false;
							tGoDone.addValue((intTime - startTime) / 100);
						}

					}
					iBuffer.close();
					aTimeGoDone.put(action, tGoDone);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			statistics.put(actor, aTimeGoDone);
		}
	}

	public Map<Actor, Map<Action, SummaryStatistics>> getStatisticsMap() {
		return this.statistics;
	}

}
