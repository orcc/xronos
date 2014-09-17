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

package org.xronos.orcc.backend.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Connection;
import net.sf.orcc.df.util.DfVisitor;

public class NetworkBufferSizeImporter extends DfVisitor<Void> {

	private class BufferSizeParser {

		private static final String ELM_FIFOSIZE = "fifosSize";
		private static final String ELM_CONNECTION = "connection";
		private static final String ELM_SOURCE = "source";
		private static final String ELM_TARGET = "target";
		private static final String ELM_SOURCE_PORT = "src-port";
		private static final String ELM_TARGET_PORT = "tgt-port";

		private static final String ELM_SIZE = "size";

		public BufferSizeParser(String bufferSizeFile,
				List<XmlConnection> connections) {

			File inputFile = new File(bufferSizeFile);
			try {
				InputStream inStream = new FileInputStream(inputFile);
				XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
				try {
					XMLStreamReader reader = xmlFactory
							.createXMLStreamReader(inStream);
					while (reader.hasNext()) {
						reader.next();
						if (reader.getEventType() == XMLStreamReader.START_ELEMENT
								&& reader.getLocalName().equals(ELM_CONNECTION)) {
							String source = reader.getAttributeValue("",
									ELM_SOURCE);
							String target = reader.getAttributeValue("",
									ELM_TARGET);
							String sourcePort = reader.getAttributeValue("",
									ELM_SOURCE_PORT);
							String targetPort = reader.getAttributeValue("",
									ELM_TARGET_PORT);
							String strSize = reader.getAttributeValue("",
									ELM_SIZE);
							int size = Integer.valueOf(strSize);
							XmlConnection connection = new XmlConnection(
									source, sourcePort, target, targetPort,
									size);
							connections.add(connection);
						} else if (reader.getEventType() == XMLStreamReader.END_ELEMENT
								&& reader.getLocalName().equals(ELM_FIFOSIZE)) {
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

	}

	private class XmlConnection {
		private String source;
		private String sourcePort;
		private String target;
		private String targetPort;
		private int size;

		public XmlConnection(String source, String sourcePort, String target,
				String targetPort, int size) {
			this.source = source;
			this.target = target;
			this.sourcePort = sourcePort;
			this.targetPort = targetPort;
			this.size = size;
		}

		public int getSize() {
			return size;
		}

		public String getSource() {
			return source;
		}

		public String getSourcePort() {
			return sourcePort;
		}

		public String getTarget() {
			return target;
		}

		public String getTargetPort() {
			return targetPort;
		}
	}

	private List<XmlConnection> xmlConnections;

	public NetworkBufferSizeImporter(String bufferSizePartitioningFile) {
		this.xmlConnections = new ArrayList<XmlConnection>();
		// Parse the Xml File
		new BufferSizeParser(bufferSizePartitioningFile, xmlConnections);
	}

	@Override
	public Void caseConnection(Connection connection) {

		String source = null;
		String target = null;
		String sourcePort = null;
		String targetPort = null;

		if (connection.getSource() instanceof Actor) {
			source = ((Actor) connection.getSource()).getName().toLowerCase();
			sourcePort = connection.getSourcePort().getName().toUpperCase();
			if (connection.getTarget() instanceof Actor) {
				target = ((Actor) connection.getTarget()).getName()
						.toLowerCase();
				targetPort = connection.getTargetPort().getName().toUpperCase();
				int newSize = getConnectionSize(source, target, sourcePort,
						targetPort);
				connection.setSize(newSize);
			}
		}

		return null;
	}

	public int getConnectionSize(String source, String target,
			String sourcePort, String targetPort) {

		for (XmlConnection connection : xmlConnections) {
			if (connection.getSource().equals(source)) {
				if (connection.getTarget().equals(target)) {
					if (connection.getSourcePort().equals(sourcePort)) {
						if (connection.getTargetPort().equals(targetPort)) {
							return connection.getSize();
						}
					}
				}
			}
		}

		return 1;
	}

}
