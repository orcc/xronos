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

package org.xronos.openforge.report;

import java.io.FileOutputStream;


import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.IPCoreCall;
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.Task;

/**
 * Prints an XML resource report for all modules in a Task
 * 
 * @author Endri Bezati
 * 
 */

public class XmlResourcePrinter {

	private Design design;
	private Document document;
	private static DOMImplementation impl;
	private static DOMImplementationRegistry registry;

	public XmlResourcePrinter(Design design, FileOutputStream fos) {
		this.design = design;
		createDocument("Design");
		populateResourceFile(document.getDocumentElement());
		writeDocument(fos);
	}

	private void createDocument(String designName) {
		try {
			registry = DOMImplementationRegistry.newInstance();
			impl = registry.getDOMImplementation("Core 3.0 XML 3.0 LS");
			document = impl.createDocument("", designName, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void populateResourceFile(Element designElement) {
		designElement.setAttribute("name", design.showIDLogical());
		writeTasks(designElement);
	}

	private void writeTasks(Element parent) {

		for (Task task : design.getTasks()) {
			final Call topCall = task.getCall();
			if (topCall instanceof IPCoreCall)
				continue;
			final Latency latency = topCall.getLatency();

			Element taskElt = document.createElement("Task");
			parent.appendChild(taskElt);

			taskElt.setAttribute("name", topCall.sourceName);

			Element type = document.createElement("Resource");
			taskElt.appendChild(type);

			type.setAttribute("MaxGateDepth",
					String.valueOf(task.getMaxGateDepth()));
			type.setAttribute("MinLatency",
					getClocksString(latency.getMinClocks()));
			type.setAttribute("MaxLatency",
					getClocksString(latency.getMaxClocks()));
			String spacing = "";
			int space = task.getGoSpacing();
			if (space == Task.INDETERMINATE_GO_SPACING) {
				spacing = "indeterminate";
			} else {
				spacing = String.valueOf(space);
			}
			type.setAttribute("MinGoSpacing", spacing);

		}
	}

	private void writeDocument(FileOutputStream fos) {
		try {
			DOMImplementationLS implLS = (DOMImplementationLS) impl;
			// Serialize the DOM to an XML file
			LSOutput output = implLS.createLSOutput();
			output.setByteStream(fos);
			// serialize the document, close the stream
			LSSerializer serializer = implLS.createLSSerializer();
			serializer.getDomConfig().setParameter("format-pretty-print", true);
			serializer.write(document, output);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets a descriptive string for a number of clocks. {@link Latency#UNKNOWN}
	 * is reported as "*".
	 */
	private static String getClocksString(int clocks) {
		if (clocks == Latency.UNKNOWN) {
			return "unknown";
		}
		return String.valueOf(clocks);
	}
}
