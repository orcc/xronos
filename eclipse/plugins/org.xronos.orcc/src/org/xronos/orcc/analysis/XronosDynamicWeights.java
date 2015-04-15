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
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */
package org.xronos.orcc.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class XronosDynamicWeights {

	private Network network;
	private String path;

	Map<Actor, Map<Action, SummaryStatistics>> statistics;

	public XronosDynamicWeights(Network network, String path) {
		this.network = network;
		this.path = path;
	}

	public void getMeanWeights(String outputPath) {

		if (getModelsimWeights()) {
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			try {
				XMLStreamWriter writer = factory
						.createXMLStreamWriter(new FileWriter(outputPath
								+ File.separator + network.getSimpleName()
								+ "_dynamicWeights.exdf"));
				writer.writeStartDocument();
				writer.writeStartElement("actors");
				for (Actor actor : statistics.keySet()) {
					writer.writeStartElement("actor");
					writer.writeAttribute("id", actor.getSimpleName());
					Map<Action, SummaryStatistics> actionWeight = statistics
							.get(actor);
					writer.writeStartElement("actions");
					for (Action action : actionWeight.keySet()) {
						writer.writeEmptyElement("action");
						writer.writeAttribute("id", action.getName());

						double min = Double.isNaN(actionWeight.get(action)
								.getMin()) ? 0 : actionWeight.get(action)
								.getMin();
						double mean = Double.isNaN(actionWeight.get(action)
								.getMean()) ? 0 : actionWeight.get(action)
								.getMean();
						double max = Double.isNaN(actionWeight.get(action)
								.getMax()) ? 0 : actionWeight.get(action)
								.getMax();
						double variance = Double.isNaN(actionWeight.get(action)
								.getVariance()) ? 0 : actionWeight.get(action)
								.getVariance();

						long nbrExec = actionWeight.get(action).getN();

						writer.writeAttribute("clockcycles-min", Double.toString(min));
						writer.writeAttribute("clockcycles", Double.toString(mean));
						writer.writeAttribute("clockcycles-max", Double.toString(max));
						writer.writeAttribute("clockcycles-variance",
								Double.toString(variance));
						writer.writeAttribute("executions",
								Long.toString(nbrExec));
					}
					writer.writeEndElement();
					writer.writeEndElement();
				}
				writer.writeEndElement();
				writer.writeEndDocument();

				writer.flush();
				writer.close();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void getMeanWeightsCSV(String outputPath) {
		if (getModelsimWeights()) {
			try {
				FileWriter writer = new FileWriter(outputPath + File.separator
						+ network.getSimpleName() + "_dynamicWeights.csv");

				writer.append("Actor; Action; Execution; Cycles;");
				writer.append("\n");
				for (Actor actor : statistics.keySet()) {
					Map<Action, SummaryStatistics> actionWeight = statistics
							.get(actor);
					for (Action action : actionWeight.keySet()) {

						double mean = Double.isNaN(actionWeight.get(action)
								.getMean()) ? 0 : actionWeight.get(action)
								.getMean();

						long nbrExec = actionWeight.get(action).getN();

						writer.append(actor.getSimpleName() + "; ");
						writer.append(action.getName() + "; ");
						writer.append(nbrExec + "; ");
						writer.append(mean + "; ");
						writer.append('\n');
					}
				}

				writer.flush();
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean getModelsimWeights() {
		File weightsPath = new File(path + File.separator + "weights");
		if (weightsPath.exists()) {
			if (weightsPath.list().length > 0) {
				SimParser simParser = new SimParser(network, path
						+ File.separator + "weights");
				simParser.createMaps();
				statistics = simParser.getStatisticsMap();
				return true;
			} else {
				return false;
			}

		}
		return false;
	}
}
