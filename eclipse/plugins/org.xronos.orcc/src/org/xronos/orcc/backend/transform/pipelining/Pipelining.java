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
