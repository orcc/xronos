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
								+ "_dynamicWeights.xml"));
				writer.writeStartDocument();
				writer.writeStartElement("actors");
				for (Actor actor : statistics.keySet()) {
					writer.writeStartElement("actor");
					writer.writeAttribute("name", actor.getSimpleName()
							.toLowerCase());
					Map<Action, SummaryStatistics> actionWeight = statistics
							.get(actor);
					writer.writeStartElement("actions");
					for (Action action : actionWeight.keySet()) {
						writer.writeStartElement("action");
						writer.writeAttribute("name", action.getName()
								.toLowerCase());

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

						writer.writeAttribute("min", Double.toString(min));
						writer.writeAttribute("mean", Double.toString(mean));
						writer.writeAttribute("max", Double.toString(max));
						writer.writeAttribute("variance",
								Double.toString(variance));

						writer.writeEndElement();
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
