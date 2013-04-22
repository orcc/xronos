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
				connection.setAttribute("bufferSize", Integer.valueOf(newSize));
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
