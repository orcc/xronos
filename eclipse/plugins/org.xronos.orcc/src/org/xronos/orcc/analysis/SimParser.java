/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
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
