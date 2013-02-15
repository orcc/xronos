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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.orcc.util.OrccLogger;

public class XronosStaticWeight {

	public class GenericExtFilter implements FilenameFilter {

		private String ext;

		public GenericExtFilter(String ext) {
			this.ext = ext;
		}

		@Override
		public boolean accept(File dir, String name) {
			return (name.endsWith(ext));
		}
	}

	// private static final String ATT_RESOURCE_MAXGATEDEPTH = "MaxGateDepth";
	// private static final String ATT_RESOURCE_MAXLATENCY = "MaxLatency";
	// private static final String ATT_RESOURCE_MINGOSPACING = "MinGoSpacing";
	private static final String ATT_RESOURCE_MINLATENCY = "MinLatency";
	private static final String ELM_DESIGN = "Design";
	private static final String ELM_RESOURCE = "Resource";
	private static final String ELM_TASK = "Task";

	private static final String FILE_TEXT_EXT = ".xml";

	public static String getCharacters(XMLStreamReader reader)
			throws XMLStreamException {
		String value = null;
		while (reader.hasNext()) {
			reader.next();
			if (reader.getEventType() == XMLStreamReader.CHARACTERS) {
				value = reader.getText();
			} else if (reader.getEventType() == XMLStreamReader.END_ELEMENT) {
				break;
			}
		}
		return value;
	}

	String name;
	String path;

	Map<String, Map<String, Integer>> weights;

	public XronosStaticWeight(String name, String path) {
		this.name = name;
		this.path = path;
		this.weights = new HashMap<String, Map<String, Integer>>();
	}

	public boolean createStaticWeight() {
		// Get all XML weights
		getWeights();
		// Create network weights
		XMLOutputFactory factory = XMLOutputFactory.newInstance();

		try {
			XMLStreamWriter writer = factory
					.createXMLStreamWriter(new FileWriter(path + File.separator
							+ name + ".xml"));
			writer.writeStartDocument();
			writer.writeStartElement("actors");
			for (String actor : weights.keySet()) {
				writer.writeStartElement("actor");
				writer.writeAttribute("name", actor.toLowerCase());
				Map<String, Integer> actionWeight = weights.get(actor);
				writer.writeStartElement("actions");
				for (String action : actionWeight.keySet()) {
					writer.writeStartElement("action");
					writer.writeAttribute("name", action.toLowerCase());
					writer.writeAttribute("meanWeight", actionWeight
							.get(action).toString());
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

		return false;
	}

	public boolean getWeights() {
		// Get Folder
		File dir = new File(path);
		if (dir.isDirectory() == false) {
			OrccLogger.severeln("Directory does not exists : " + path);
			return false;
		}

		// Create the xml filter
		GenericExtFilter filter = new GenericExtFilter(FILE_TEXT_EXT);

		// list out all the file name and filter by the extension
		String[] list = dir.list(filter);
		for (String fileName : list) {
			File inputFile = new File(path + File.separator + fileName);
			try {
				InputStream inStream = new FileInputStream(inputFile);
				XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
				try {
					XMLStreamReader reader = xmlFactory
							.createXMLStreamReader(inStream);
					while (reader.hasNext()) {
						reader.next();
						if (reader.getEventType() == XMLStreamReader.START_ELEMENT
								&& reader.getLocalName().equals(ELM_DESIGN)) {
							String designName = reader.getAttributeValue("",
									"name");
							Map<String, Integer> actions = new HashMap<String, Integer>();
							parseTasks(reader, actions);
							weights.put(designName, actions);

						} else if (reader.getEventType() == XMLStreamReader.END_ELEMENT
								&& reader.getLocalName().equals(ELM_DESIGN)) {
							break;
						}
					}
				} catch (XMLStreamException e) {
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		}
		return false;
	}

	private Integer parseResource(XMLStreamReader reader)
			throws XMLStreamException {
		Integer latency = -1;
		while (reader.hasNext()) {
			reader.next();
			if (reader.getEventType() == XMLStreamReader.START_ELEMENT
					&& reader.getLocalName().equals(ELM_RESOURCE)) {
				// String maxGateDepth = reader.getAttributeValue("",
				// ATT_RESOURCE_MAXGATEDEPTH);
				// String maxLatency = reader.getAttributeValue("",
				// ATT_RESOURCE_MAXLATENCY);
				// String minGoSpacing = reader.getAttributeValue("",
				// ATT_RESOURCE_MINGOSPACING);
				String minLatency = reader.getAttributeValue("",
						ATT_RESOURCE_MINLATENCY);
				latency = Integer.valueOf(minLatency);
			} else if (reader.getEventType() == XMLStreamReader.END_ELEMENT
					&& reader.getLocalName().equals(ELM_RESOURCE)) {
				break;
			}

		}
		return latency;
	}

	private void parseTasks(XMLStreamReader reader, Map<String, Integer> weight)
			throws XMLStreamException {
		while (reader.hasNext()) {
			reader.next();
			if (reader.getEventType() == XMLStreamReader.START_ELEMENT
					&& reader.getLocalName().equals(ELM_TASK)) {
				String taskName = reader.getAttributeValue("", "name");
				Integer latency = parseResource(reader);
				weight.put(taskName, latency);
			} else if (reader.getEventType() == XMLStreamReader.END_ELEMENT
					&& reader.getLocalName().equals(ELM_DESIGN)) {
				break;
			}
		}
	}
}
