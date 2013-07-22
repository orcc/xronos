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
package org.xronos.orcc.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;

public class XronosDynamicWeights {

	private Network network;
	private String path;

	Map<Actor, Map<Action, List<Integer>>> execution;

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
				for (Actor actor : execution.keySet()) {
					writer.writeStartElement("actor");
					writer.writeAttribute("name", actor.getSimpleName()
							.toLowerCase());
					Map<Action, List<Integer>> actionWeight = execution
							.get(actor);
					writer.writeStartElement("actions");
					for (Action action : actionWeight.keySet()) {
						writer.writeStartElement("action");
						writer.writeAttribute("name", action.getName()
								.toLowerCase());
						// get MeanWeight
						int sum = 0;
						for (Integer i : actionWeight.get(action)) {
							sum += i;
						}

						Integer meanWeight = 0;
						if (actionWeight.get(action).size() != 0) {
							meanWeight = sum / actionWeight.get(action).size();
						}
						writer.writeAttribute("meanWeight",
								meanWeight.toString());
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
				execution = simParser.getExecutionMap();
				return true;
			} else {
				return false;
			}

		}
		return false;
	}
}