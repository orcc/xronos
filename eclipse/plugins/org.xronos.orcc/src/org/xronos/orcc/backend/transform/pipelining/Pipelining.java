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

package org.xronos.orcc.backend.transform.pipelining;

import static net.sf.orcc.OrccLaunchConstants.PROJECT;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Connection;
import net.sf.orcc.df.DfFactory;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.df.util.XdfWriter;
import net.sf.orcc.ir.Type;
import net.sf.orcc.util.OrccLogger;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.xronos.orcc.backend.cal.ActorPrinter;
import org.xronos.orcc.backend.transform.pipelining.coloring.PipeliningOptimization;
import org.xronos.orcc.backend.transform.pipelining.coloring.TestBench;

/**
 * The pipelining engine transformation
 * 
 * @author Endri Bezati
 * 
 */
public class Pipelining extends DfVisitor<Void> {

	Map<String, Object> options;

	/**
	 * Define the time of a Stage
	 */

	public Pipelining(Map<String, Object> options) {
		this.options = options;
	}

	@Override
	public Void caseAction(Action action) {
		float stageTime = 0f;
		// Apply iff the action has the xronos_pipeline tag
		if (action.hasAttribute("xronos_pipeline")) {
			if (action.getAttribute("xronos_pipeline")
					.hasAttribute("StageTime")) {
				stageTime = Float.parseFloat(action
						.getAttribute("xronos_pipeline")
						.getAttribute("StageTime").getStringValue());

				// Get the Input and Output matrix of the operators found on the
				// BlockBasic of the action
				ExtractOperatorsIO opIO = new ExtractOperatorsIO();
				opIO.doSwitch(action.getBody());
				// opIO.printTablesForCTestbench();

				// Create the TestBench for this action
				TestBench tb = opIO.createTestBench(stageTime);

				// Create and run the PipelineOptimization
				String logPath = System.getProperty("user.home")
						+ File.separator + "Pipeline.txt";
				PipeliningOptimization pOptimization = new PipeliningOptimization(
						tb, logPath);

				pOptimization.run();

				// Create Actors
				int stages = pOptimization.getNbrStages();
				List<Actor> pipeActors = new ArrayList<Actor>();

				IFolder folder = createNewPipelineFolder(action);
				String path = folder.getRawLocation().toOSString();

				String packageName = findActorPackage(path);
				for (int i = 0; i < stages; i++) {
					PipelineActor pipelineActor = new PipelineActor(path,
							packageName, action, opIO, i,
							pOptimization.getStageInputs(i),
							pOptimization.getStageOutputs(i),
							pOptimization.getStageOperators(i));
					Actor pActor = pipelineActor.getActor();
					// Debug print actor
					ActorPrinter actorPrinter = new ActorPrinter(pActor,
							packageName);

					actorPrinter.printActor(path);
					pipeActors.add(pActor);
				}

				// Create Network
				Network network = createNetwork(action, pipeActors, opIO);

				XdfWriter writer = new XdfWriter();
				File file = new File(path);

				writer.write(file, network);

				try {
					folder.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (action.getAttribute("xronos_pipeline").hasAttribute(
					"Debug")) {
				// Get the Input and Output matrix of the operators found on the
				// BlockBasic of the action
				ExtractOperatorsIO opIO = new ExtractOperatorsIO();
				opIO.doSwitch(action.getBody());
				opIO.printTablesForCTestbench();
			} else {
				OrccLogger
				.warnln("PIPELINING: StageTime attribute missing, example: @xronos_pipeline(StageTime=\"1,2\")");
			}
		}
		return null;
	}

	private Network createNetwork(Action action, List<Actor> actors,
			ExtractOperatorsIO opIO) {
		Map<Port, String> portToVarString = new HashMap<Port, String>();
		Actor containementActor = EcoreHelper.getContainerOfType(action,
				Actor.class);
		Network network = DfFactory.eINSTANCE.createNetwork();
		network.setName(containementActor.getSimpleName());

		// Create network inputs
		for (Port port : action.getInputPattern().getPorts()) {
			String name = port.getName();
			Type type = EcoreUtil.copy(port.getType());
			Port nIn = DfFactory.eINSTANCE.createPort(type, name);
			network.addInput(nIn);
			String varName = opIO.getStringPortToVarMap().get(name);
			portToVarString.put(nIn, varName);
		}
		// Create network outputs
		for (Port port : action.getOutputPattern().getPorts()) {
			String name = port.getName();
			Type type = EcoreUtil.copy(port.getType());
			Port nOut = DfFactory.eINSTANCE.createPort(type, name);
			network.addOutput(nOut);
			String varName = opIO.getStringPortToVarMap().get(name);
			portToVarString.put(nOut, varName);
		}

		Map<Actor, Instance> actorInstanceMap = new HashMap<Actor, Instance>();
		// Add actors
		for (Actor actor : actors) {
			Instance instance = DfFactory.eINSTANCE.createInstance(
					getInstanceName(actor.getName()), actor);
			network.getChildren().add(instance);
			actorInstanceMap.put(actor, instance);
		}

		// Interconnect input Port with first stage actor
		for (Port port : network.getInputs()) {
			Actor firstStage = actors.get(0);
			Port actorPort = firstStage.getPort(portToVarString.get(port)
					+ "_pI");

			Connection connection = DfFactory.eINSTANCE.createConnection(port,
					null, actorInstanceMap.get(firstStage), actorPort);
			network.getConnections().add(connection);
		}

		// Interconnect output Port with last stage actor outputs
		for (Port port : network.getOutputs()) {
			Actor lastStage = actors.get(actors.size() - 1);
			Port actorPort = lastStage.getPort(portToVarString.get(port)
					+ "_pO");

			Connection connection = DfFactory.eINSTANCE.createConnection(
					actorInstanceMap.get(lastStage), actorPort, port, null);
			network.getConnections().add(connection);
		}

		// Interconnect the actors
		for (int i = 0; i < actors.size() - 1; i++) {
			Actor currentActor = actors.get(i);
			Actor nextActor = actors.get(i + 1);
			Instance currentInstance = actorInstanceMap.get(currentActor);
			Instance nextInstance = actorInstanceMap.get(nextActor);

			for (Port port : currentActor.getOutputs()) {
				String name = port.getName();
				int postion = name.indexOf("_pO");
				String baseName = name.substring(0, postion);
				Port portIn = nextActor.getInput(baseName + "_pI");

				Connection connection = DfFactory.eINSTANCE.createConnection(
						currentInstance, port, nextInstance, portIn);
				network.getConnections().add(connection);
			}
		}

		return network;
	}

	private IFolder createNewPipelineFolder(Action action) {
		// Get Project
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject((String) options.get(PROJECT));

		// Get the package of the actor
		Actor containementActor = EcoreHelper.getContainerOfType(action,
				Actor.class);
		String actorFileName = containementActor.getFileName();
		int position = actorFileName.indexOf(containementActor.getName(), 0);

		int positionBegin = project.getFullPath().toString().length() + 1;

		String packageFolder = actorFileName.substring(positionBegin, position);

		IFolder folder = project.getFolder(packageFolder + "/"
				+ containementActor.getSimpleName() + "_pipeline");
		try {
			if (!folder.exists()) {
				folder.create(false, true, null);
			} else {
				// Delete old contents
				IResource[] resources = folder.members();
				for (IResource resource : resources) {
					resource.delete(true, null);
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return folder;
	}

	private String findActorPackage(String string) {

		int position = string.indexOf("src/", 0);

		String subString = string.substring(position + 4, string.length());

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < subString.length(); i++) {
			if (subString.charAt(i) == '/') {
				builder.append('.');
			} else {
				builder.append(subString.charAt(i));
			}
		}

		return builder.toString();
	}

	/**
	 * Get the instance Name
	 * 
	 * @param string
	 * @return
	 */
	private String getInstanceName(String string) {

		int position = 0;
		for (int i = string.length() - 1; i >= 0; i--) {
			if (string.charAt(i) == '.') {
				position = i;
				break;
			}
		}
		String name = string.substring(position + 1, string.length());

		return name;
	}

}
