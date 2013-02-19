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

	public void getModelsimWeights() {
		File weightsPath = new File(path + File.separator + "weights");
		if (weightsPath.exists()) {
			SimParser simParser = new SimParser(network, path + File.separator
					+ "weights");
			simParser.createMaps();
			execution = simParser.getExecutionMap();
		}
	}

	public void getMeanWeights(String outputPath) {
		getModelsimWeights();
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
				Map<Action, List<Integer>> actionWeight = execution.get(actor);
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
					writer.writeAttribute("meanWeight", meanWeight.toString());
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
