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
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */
package org.xronos.orcc.backend.embedded;

import static net.sf.orcc.OrccLaunchConstants.NO_LIBRARY_EXPORT;

import java.io.File;
import java.util.ArrayList;
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
import net.sf.orcc.df.util.DfSwitch;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.transform.RenameTransformation;
import net.sf.orcc.util.OrccLogger;

import org.eclipse.core.resources.IFile;
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
		srcPath = path + File.separator + "src";
		File srcDir = new File(srcPath);
		if (!srcDir.exists()) {
			srcDir.mkdir();
		}
		
		// Build Path
		String buildPath = path + File.separator + "build";
		File buildDir = new File(buildPath);
		if (!buildDir.exists()) {
			buildDir.mkdir();
		}

	}

	@Override
	protected void doTransformActor(Actor actor) {
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

		List<DfSwitch<?>> transformations = new ArrayList<DfSwitch<?>>();
		transformations.add(new UnitImporter());
		transformations.add(new TypeResizer(false, false, false, false));
		transformations.add(new RenameTransformation(replacementMap));
		
		for (DfSwitch<?> transformation : transformations) {
			transformation.doSwitch(actor);
		}
	}

	private void doTransformNetwork(Network network) {
		OrccLogger.trace("Instantiating... ");
		new Instantiator(false).doSwitch(network);
		OrccLogger.traceRaw("done\n");
		new NetworkFlattener().doSwitch(network);
	}
	
	
	@Override
	protected void doVtlCodeGeneration(List<IFile> files) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doXdfCodeGeneration(Network network) {
		doTransformNetwork(network);
		transformActors(network.getAllActors());

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

		printChildren(network);
		// print network
		OrccLogger.traceln("Printing network...");
		printNetwork(network);

	}
	
	@Override
	public boolean printInstance(Instance instance) {
		if(!instance.getActor().isNative()){
			return new EmbeddedInstance(instance, options).print(srcPath) > 1;
		}
		return false;
	}
	
	public void printNetwork(Network network) {
		EmbeddedNetwork printer = new EmbeddedNetwork(network, options);
		
		printer.printMain(srcPath);
		printer.printNetwork(srcPath);
		printer.printCMakeLists(path);
	}
	
	@Override
	public boolean exportRuntimeLibrary() {
		if (!getAttribute(NO_LIBRARY_EXPORT, false)) {
			String target = path + File.separator + "lib";
			OrccLogger
					.trace("Export libraries sources into " + target + "... ");
			if (copyFolderToFileSystem("/bundle/embedded", target, debug)) {
				OrccLogger.traceRaw("OK" + "\n");
				return true;
			} else {
				OrccLogger.warnRaw("Error" + "\n");
				return false;
			}
		}
		return false;
	}

}
