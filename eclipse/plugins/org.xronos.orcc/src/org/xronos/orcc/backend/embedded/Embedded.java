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
package org.xronos.orcc.backend.embedded;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Connection;
import net.sf.orcc.df.Entity;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.transform.Instantiator;
import net.sf.orcc.df.transform.NetworkFlattener;
import net.sf.orcc.df.transform.TypeResizer;
import net.sf.orcc.df.transform.UnitImporter;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.transform.RenameTransformation;
import net.sf.orcc.util.FilesManager;
import net.sf.orcc.util.OrccLogger;
import net.sf.orcc.util.Result;
/**
 * The Xronos Embedded C++ backend.
 * 
 * @author Endri Bezati
 * 
 */
public class Embedded extends AbstractBackend {

	private String srcPath;

	@Override
	protected void doInitializeOptions() {
		
		// Source Paths
		srcPath = outputPath + File.separator + "src";
		File srcDir = new File(srcPath);
		if (!srcDir.exists()) {
			srcDir.mkdir();
		}

		// Build Path
		String buildPath = outputPath + File.separator + "build";
		File buildDir = new File(buildPath);
		if (!buildDir.exists()) {
			buildDir.mkdir();
		}

		Map<String, String> replacementMap = new HashMap<String, String>();
		replacementMap.put("abs", "abs_");
		replacementMap.put("getw", "getw_");
		replacementMap.put("index", "index_");
		replacementMap.put("max", "max_");
		replacementMap.put("min", "min_");
		replacementMap.put("select", "select_");
		replacementMap.put("bitand", "bitand_");
		replacementMap.put("bitor", "bitor_");
		replacementMap.put("not", "not_");
		replacementMap.put("and", "and_");
		replacementMap.put("OUT", "OUT_");
		replacementMap.put("IN", "IN_");
		replacementMap.put("DEBUG", "DEBUG_");
		replacementMap.put("INT_MIN", "INT_MIN_");

		networkTransfos.add(new Instantiator(true));
		networkTransfos.add(new NetworkFlattener());
		networkTransfos.add(new UnitImporter());
		
		childrenTransfos.add(new TypeResizer(false, false, false, false));
		childrenTransfos.add(new RenameTransformation(replacementMap));
	}

	@Override
	protected Result doGenerateNetwork(Network network) {

		network.computeTemplateMaps();

		// hack, should be done in the method just above
		for (Vertex vertex : network.getChildren()) {
			Entity entity = vertex.getAdapter(Entity.class);
			Map<Port, List<Connection>> map = entity.getOutgoingPortMap();
			for (List<Connection> connections : map.values()) {
				for (Connection connection : connections) {
					connection.setAttribute("nbReaders", connections.size());
				}
			}
		}

		// print network
		OrccLogger.traceln("Printing network...");
		EmbeddedNetwork printer = new EmbeddedNetwork(network, getOptions());

		printer.printMain(srcPath);
		printer.printNetwork(srcPath);
		printer.printCMakeLists(outputPath);
		return super.doGenerateNetwork(network);
	}
	
	@Override
	protected Result doGenerateInstance(Instance instance) {
		if(!instance.getActor().isNative()){
			new EmbeddedInstance(instance, getOptions()).print(srcPath);
		}
		return super.doGenerateInstance(instance);
	}
	
	protected Result doGenerateActor(Actor actor) {
		new EmbeddedActor(actor, getOptions()).print(srcPath);
		return super.doGenerateActor(actor);
	
	}

	@Override
	protected Result doLibrariesExtraction() {
		String target = outputPath + File.separator + "lib";
		OrccLogger
				.trace("Export libraries sources into " + target + "... ");
		Result result = FilesManager.extract("/bundle/embedded", target);
		return result;
	}
}
