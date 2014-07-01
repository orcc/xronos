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
