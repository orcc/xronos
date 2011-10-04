package net.sf.openforge.report;

import java.io.FileOutputStream;

import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Task;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

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

		for (Task task : this.design.getTasks()) {
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
