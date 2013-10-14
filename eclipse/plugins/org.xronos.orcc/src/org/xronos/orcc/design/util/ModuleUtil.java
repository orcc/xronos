/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
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

package org.xronos.orcc.design.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.ClockDependency;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.LoopBody;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.MutexBlock;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.ResetDependency;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.LValue;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LogicalMemoryPort;
import org.xronos.openforge.lim.op.NoOp;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.preference.Constants;

/**
 * This class contains several methods for building Design Modules
 * 
 * @author Endri Bezati
 * 
 */

public class ModuleUtil {
	public static Component absoluteMemoryRead(Var stateVar, Var readValue,
			ResourceCache resourceCache, Map<Bus, Var> busDependency,
			Map<Bus, Integer> doneBusDependency) {
		Location targetLocation = resourceCache.getLocation(stateVar);

		LogicalMemoryPort memPort = targetLocation.getLogicalMemory()
				.getLogicalMemoryPorts().iterator().next();

		Component absoluteMemRead = new AbsoluteMemoryRead(targetLocation,
				Constants.MAX_ADDR_WIDTH, stateVar.getType().isInt());
		memPort.addAccess((LValue) absoluteMemRead, targetLocation);
		PortUtil.mapOutDataPorts(absoluteMemRead, readValue, busDependency,
				doneBusDependency);
		return absoluteMemRead;
	}

	/**
	 * This method creates an AbsoluteMemoryWrite Component which is an absolute
	 * memory write access.
	 * 
	 * @param stateVar
	 *            the stateVar to be modifier
	 * @param value
	 *            the variable that contains the value
	 * @param resourceCache
	 *            the resource cache
	 * @param portDependency
	 *            the port dependency map
	 * @param portGroupDependency
	 *            the port group dependency map
	 * @param doneBusDependency
	 *            the done bus dependency map
	 * @return
	 */
	public static Component absoluteMemoryWrite(Var stateVar, Var writeValue,
			ResourceCache resourceCache, Map<Port, Var> portDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Location targetLocation = resourceCache.getLocation(stateVar);
		LogicalMemoryPort memPort = targetLocation.getLogicalMemory()
				.getLogicalMemoryPorts().iterator().next();

		Component absoluteMemWrite = new AbsoluteMemoryWrite(targetLocation,
				Constants.MAX_ADDR_WIDTH, stateVar.getType().isInt());
		memPort.addAccess((LValue) absoluteMemWrite, targetLocation);
		PortUtil.mapInDataPorts(absoluteMemWrite, writeValue, portDependency,
				portGroupDependency);
		PortUtil.mapOutControlPort(absoluteMemWrite, 0, doneBusDependency);
		return absoluteMemWrite;
	}

	/**
	 * This method creates an NoOp component used for assign that are not
	 * include in a procedure
	 * 
	 * @param target
	 *            the target variable
	 * @param source
	 *            the source variable
	 * @param portDependency
	 *            the port dependency map
	 * @param busDependency
	 *            the bus dependency map
	 * @param doneBusDependency
	 *            the bus done dependency map
	 * @return
	 */
	public static Component assignComponent(Var target, Var source,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Component component = new NoOp(1, Exit.DONE);

		PortUtil.mapInDataPorts(component, source, portDependency,
				portGroupDependency);

		PortUtil.mapOutDataPorts(component, target, busDependency,
				doneBusDependency);
		return component;
	}

	public static void branchDependencies(Branch branch, Decision decision,
			Block thenBlock, Block elseBlock, Map<Port, Var> portDependency,
			Map<Bus, Var> busDependency, Map<Var, List<Var>> phiOuts,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		/** Input Dependencies **/
		InBuf inBuf = branch.getInBuf();
		for (Bus bus : inBuf.getDataBuses()) {
			Var busVar = busDependency.get(bus);
			// Decision
			for (Port port : decision.getDataPorts()) {
				if (portDependency.get(port) == busVar) {
					int group = portGroupDependency.get(port);
					List<Entry> entries = port.getOwner().getEntries();
					Entry entry = entries.get(group);
					Dependency dep = new DataDependency(bus);
					entry.addDependency(port, dep);
				}
			}
			// Then Block
			for (Port port : thenBlock.getDataPorts()) {
				if (portDependency.get(port) == busVar) {
					int group = portGroupDependency.get(port);
					List<Entry> entries = port.getOwner().getEntries();
					Entry entry = entries.get(group);
					Dependency dep = new DataDependency(bus);
					entry.addDependency(port, dep);
				}
			}
			if (elseBlock != null) {
				// Else Block
				for (Port port : elseBlock.getDataPorts()) {
					if (portDependency.get(port) == busVar) {
						int group = portGroupDependency.get(port);
						List<Entry> entries = port.getOwner().getEntries();
						Entry entry = entries.get(group);
						Dependency dep = new DataDependency(bus);
						entry.addDependency(port, dep);
					}
				}
			}

			// Check if there is any dependency from input to output (Group 1)
			for (Bus outBus : branch.getDataBuses()) {
				Port port = outBus.getPeer();
				Var var = portDependency.get(port);
				if (phiOuts.keySet().contains(var)) {
					List<Var> joinVars = phiOuts.get(var);
					if (joinVars.get(1) == busVar) {
						List<Entry> entries = port.getOwner().getEntries();
						Entry entry = entries.get(1);
						Dependency dep = new DataDependency(bus);
						entry.addDependency(port, dep);
					}
				}
			}
		}

		/** Then and Else Output block Dependencies **/
		for (Bus bus : thenBlock.getDataBuses()) {
			Var busVar = busDependency.get(bus);
			for (Bus outBus : branch.getDataBuses()) {
				Port port = outBus.getPeer();
				Var var = portDependency.get(port);
				if (phiOuts.keySet().contains(var)) {
					List<Var> joinVars = phiOuts.get(var);
					if (joinVars.get(0) == busVar) {
						List<Entry> entries = port.getOwner().getEntries();
						Entry entry = entries.get(0);
						Dependency dep = new DataDependency(bus);
						entry.addDependency(port, dep);
					}
				}
			}
		}

		// Control Dependency
		Bus doneBus = thenBlock.getExit(Exit.DONE).getDoneBus();
		Port donePort = branch.getExit(Exit.DONE).getDoneBus().getPeer();
		List<Entry> entries = donePort.getOwner().getEntries();
		Entry entry = entries.get(0);
		Dependency dep = new ControlDependency(doneBus);
		entry.addDependency(donePort, dep);

		if (elseBlock != null) {
			for (Bus bus : elseBlock.getDataBuses()) {
				Var busVar = busDependency.get(bus);
				for (Bus outBus : branch.getDataBuses()) {
					Port port = outBus.getPeer();
					Var var = portDependency.get(port);
					if (phiOuts.keySet().contains(var)) {
						List<Var> joinVars = phiOuts.get(var);
						if (joinVars.get(1) == busVar) {
							entries = port.getOwner().getEntries();
							entry = entries.get(1);
							dep = new DataDependency(bus);
							entry.addDependency(port, dep);
						}
					}
				}
			}

			doneBus = elseBlock.getExit(Exit.DONE).getDoneBus();
			donePort = branch.getExit(Exit.DONE).getDoneBus().getPeer();
			entries = donePort.getOwner().getEntries();
			entry = entries.get(1);
			dep = new ControlDependency(doneBus);
			entry.addDependency(donePort, dep);
		}

	}

	/**
	 * This method adds an entry to a component
	 * 
	 * @param component
	 *            the component
	 * @param drivingExit
	 *            the driveing Exit
	 * @param clockBus
	 *            the clock bus attached to the component
	 * @param resetBus
	 *            the reset bus attached to the component
	 * @param goBus
	 *            the go bus control of the component
	 */
	public static void componentAddEntry(Component component, Exit drivingExit,
			Bus clockBus, Bus resetBus, Bus goBus) {

		Entry entry = component.makeEntry(drivingExit);
		// Even though most components do not use the clock, reset and
		// go ports we set up the dependencies for consistency.
		entry.addDependency(component.getClockPort(), new ClockDependency(
				clockBus));
		entry.addDependency(component.getResetPort(), new ResetDependency(
				resetBus));
		entry.addDependency(component.getGoPort(), new ControlDependency(goBus));
	}

	public static Component createBranch(Decision decision, Block thenBlock,
			Block elseBlock, List<Var> inVars, List<Var> outVars,
			Map<Var, List<Var>> phiOuts, String searchScope,
			Exit.Type exitType, Map<Port, Var> portDependency,
			Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Branch branch = null;
		List<Component> branchComponents = new ArrayList<Component>();
		if (elseBlock == null) {
			branch = new Branch(decision, thenBlock);
			branchComponents.add(decision);
			branchComponents.add(thenBlock);
		} else {
			branch = new Branch(decision, thenBlock, elseBlock);
			branchComponents.add(decision);
			branchComponents.add(thenBlock);
			branchComponents.add(elseBlock);
		}

		createModuleInterface(branch, inVars, outVars, exitType,
				portDependency, portGroupDependency, busDependency);

		// Map In/Out port of branch, Branch done Group 0
		PortUtil.mapInDataPorts(branch, inVars, portDependency,
				portGroupDependency);
		PortUtil.mapOutControlPort(branch, 0, doneBusDependency);

		// moduleDependencies(branch, branchComponents,
		// branch.getExit(Exit.DONE),
		// portDependency, busDependency, portGroupDependency,
		// doneBusDependency);

		branchDependencies(branch, decision, thenBlock, elseBlock,
				portDependency, busDependency, phiOuts, portGroupDependency,
				doneBusDependency);

		// Give the name of the searchScope
		branch.specifySearchScope(searchScope);
		return branch;
	}

	public static Decision createDecision(List<Component> bodyComponents,
			Component decisionComponent, List<Var> inVars,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Decision decision = null;

		Module decisionModule = (Module) createModule(bodyComponents, inVars,
				Collections.<Var> emptyList(), "decisionBlock", false,
				Exit.DONE, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		// Create the decision
		decision = new Decision((Block) decisionModule, decisionComponent);

		// Propagate Inputs
		decisionPropagateInputs(decision, (Block) decisionModule);

		// Map in parts
		PortUtil.mapInDataPorts(decision, inVars, portDependency,
				portGroupDependency);
		return decision;
	}

	/**
	 * This method creates a Design decision
	 * 
	 * @param inputDecision
	 *            the input decision variable
	 * @param resultName
	 *            the final name of the inputDecision for the testBlock
	 * @param portDependency
	 *            the port dependency map
	 * @param busDependency
	 *            the bus dependency map
	 * @param doneBusDependency
	 *            the done bus dependency map
	 * @return
	 */
	public static Decision createDecision(Var inputDecision, String resultName,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Decision decision = null;
		// Create the decision variable and assign the inputDecision to it
		Type type = IrFactory.eINSTANCE.createTypeBool();
		Var decisionVar = IrFactory.eINSTANCE.createVar(0, type, resultName,
				false, 0);

		Component assignComp = assignComponent(decisionVar, inputDecision,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		Module decisionModule = (Module) createModule(
				Arrays.asList(assignComp), Arrays.asList(inputDecision),
				Collections.<Var> emptyList(), "decisionBlock", false,
				Exit.DONE, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		// Add done dependency, Decision group 0
		PortUtil.mapOutControlPort(decisionModule, 0, doneBusDependency);

		// Create the decision
		decision = new Decision((Block) decisionModule, assignComp);

		// Propagate Inputs
		decisionPropagateInputs(decision, (Block) decisionModule);

		// Add to dependency, This type of decision has only one Input at
		// group 0
		Port port = decision.getDataPorts().get(0);
		port.setIDLogical(inputDecision.getIndexedName());
		portDependency.put(port, inputDecision);
		portGroupDependency.put(port, 0);

		return decision;
	}

	public static Component createLoop(Component decisionComponent,
			List<Component> decisionComponents, List<Var> decisionInVars,
			List<Component> bodyComponents, Map<Var, List<Var>> loopPhi,
			List<Var> loopInVars, List<Var> loopOutVars,
			List<Var> loopBodyInVars, List<Var> loopBodyOutVars,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		Decision decision = createDecision(decisionComponents,
				decisionComponent, decisionInVars, portDependency,
				busDependency, portGroupDependency, doneBusDependency);

		Module body = (Module) createModule(bodyComponents, loopBodyInVars,
				loopBodyOutVars, "loopBody", false, Exit.DONE, 0,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		/** Create a While Loop Body **/
		LoopBody loopBody = new WhileBody(decision, body);

		createLoopBodyInterface(loopBody, loopBodyInVars, loopBodyOutVars,
				loopOutVars, portDependency, portGroupDependency,
				busDependency, doneBusDependency);

		/** LoopBody to body module dependency **/
		for (Bus bus : loopBody.getInBuf().getDataBuses()) {
			/** Decision Input Dependency **/
			Var busVar = busDependency.get(bus);
			// Decision
			for (Port port : decision.getDataPorts()) {
				if (portDependency.get(port) == busVar) {
					dataDependencies(port, bus, portGroupDependency);
					break;
				}
			}
			// Body Module Input
			for (Port port : body.getDataPorts()) {
				if (portDependency.get(port) == busVar) {
					dataDependencies(port, bus, portGroupDependency);
					break;
				}
			}
			// Add a dependency through the input to the Done Exit
			for (Bus outBus : loopBody.getLoopCompleteExit().getDataBuses()) {
				Var src = busDependency.get(bus);
				Var tgt = busDependency.get(outBus);
				if (src == tgt) {
					// Create a Data Dependency
					Port port = outBus.getPeer();
					dataDependencies(port, bus, portGroupDependency);
					break;
				}
			}
		}

		/** Body Module Feedback dependency **/
		for (Bus bus : body.getExit(Exit.DONE).getDataBuses()) {
			for (Bus fbBus : loopBody.getFeedbackExit().getDataBuses()) {
				Var src = busDependency.get(bus);
				Var tgt = busDependency.get(fbBus);
				if (src == tgt) {
					// Create a Data Dependency
					Port port = fbBus.getPeer();
					dataDependencies(port, bus, portGroupDependency);
					break;
				}
			}
		}

		/** Create Loop **/
		Loop loop = new Loop(loopBody);

		/** Create Loop Interface **/
		createModuleInterface(loop, loopInVars, loopOutVars, null,
				portDependency, portGroupDependency, busDependency);

		/** Initialization and Feedback Entry **/
		Entry initEntry = loop.getBodyInitEntry();
		Entry fbEntry = loop.getBodyFeedbackEntry();
		Collection<Dependency> goInitDeps = initEntry.getDependencies(loop
				.getBody().getGoPort());
		Bus initDoneBus = goInitDeps.iterator().next().getLogicalBus();

		for (Bus lBus : loop.getInBuf().getDataBuses()) {
			for (Bus lbBus : loopBody.getInBuf().getDataBuses()) {
				Var src = busDependency.get(lBus);
				Var tgt = busDependency.get(lbBus);
				if (loopPhi.keySet().contains(tgt)) {
					if (src == loopPhi.get(tgt).get(0)) {
						// Phi Dependencies
						if (loopPhi.keySet().contains(tgt)) {
							// Group 0 - Init Dependency
							Port targetPort = lbBus.getPeer();
							Bus sourceBus = lBus;
							Dependency dep = (targetPort == targetPort
									.getOwner().getGoPort()) ? new ControlDependency(
									sourceBus) : new DataDependency(sourceBus);
							initEntry.addDependency(targetPort, dep);
							// Group 1 - Feedback Dependency
							for (Bus outBus : loopBody.getFeedbackExit()
									.getDataBuses()) {
								Var fbVar = busDependency.get(outBus);
								if (fbVar == loopPhi.get(tgt).get(1)) {
									Exit fbExit = loop.getBody()
											.getFeedbackExit();
									targetPort = lbBus.getPeer();
									sourceBus = outBus;
									Reg fbReg = loop.createDataRegister();
									fbReg.setIDLogical("fbReg_"
											+ src.getIndexedName());
									fbReg.getDataPort().setIDLogical(
											src.getIndexedName());
									fbReg.getResultBus().setIDLogical(
											tgt.getIndexedName());
									Entry entry = fbReg.makeEntry(fbExit);
									entry.addDependency(fbReg.getDataPort(),
											new DataDependency(sourceBus));
									fbEntry.addDependency(
											targetPort,
											new DataDependency(fbReg
													.getResultBus()));
								}
							}
						}
					}
				} else if (src == tgt) {
					Port port = lbBus.getPeer();
					Bus sourceBus = lBus;
					Latch latch = loop.createDataLatch();
					latch.setIDLogical("latchedInput_" + src.getIndexedName());
					Entry latchEntry = latch.makeEntry(initDoneBus.getOwner());
					latch.getDataPort().setIDLogical(src.getIndexedName());
					latchEntry.addDependency(latch.getEnablePort(),
							new ControlDependency(initDoneBus));
					latchEntry.addDependency(latch.getDataPort(),
							new DataDependency(sourceBus));
					sourceBus = latch.getResultBus();
					sourceBus.setIDLogical(src.getIndexedName());

					Dependency dep = new DataDependency(sourceBus);
					fbEntry.addDependency(port, dep);
				}

			}
		}

		Entry outbufEntry = loop.getExit(Exit.DONE).getPeer().getEntries()
				.get(0);

		for (Bus lbBus : loopBody.getLoopCompleteExit().getDataBuses()) {
			for (Bus lBus : loop.getExit(Exit.DONE).getDataBuses()) {
				Var src = busDependency.get(lbBus);
				Var tgt = busDependency.get(lBus);
				if (src == tgt) {
					Port port = lBus.getPeer();
					Dependency dep = new DataDependency(lbBus);
					outbufEntry.addDependency(port, dep);
				}
			}
		}

		/** Done Dependency **/

		Port loopBodyDonePort = loopBody.getLoopCompleteExit().getDoneBus()
				.getPeer();
		Bus loopDoneBus = loop.getExit(Exit.DONE).getDoneBus();
		doneBusDependency.put(loopDoneBus, 0);
		Dependency dep = new ControlDependency(loopDoneBus);
		outbufEntry.addDependency(loopBodyDonePort, dep);

		return loop;
	}

	public static void createLoopBodyInterface(LoopBody loopBody,
			List<Var> inVars, List<Var> feedbackVars, List<Var> doneVars,
			Map<Port, Var> portDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Var> busDependency, Map<Bus, Integer> doneBusDependency) {
		/** Create LoopBody Inputs **/
		if (!inVars.isEmpty()) {
			for (Var var : inVars) {
				Port port = loopBody.makeDataPort();
				port.setIDLogical(var.getIndexedName());
				portDependency.put(port, var);
				portGroupDependency.put(port, 0);
				busDependency.put(port.getPeer(), var);
			}
		}

		/** Create LoopBody Feedback Exit **/
		Exit fExit = loopBody.getFeedbackExit();
		populateExit(fExit, feedbackVars, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		/** Create LoopBody Complete-Done Exit **/
		Exit cExit = loopBody.getLoopCompleteExit();
		populateExit(cExit, doneVars, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

	}

	/**
	 * This method creates a module
	 * 
	 * @param components
	 *            the list of component
	 * @param inVars
	 *            the module inputs
	 * @param outVars
	 *            the module outputs
	 * @param searchScope
	 *            A string which indicates the search scope
	 * @param exitType
	 *            the Exit Type Tag
	 * @param group
	 *            the group of the module (0 or 1)
	 * @param portDependency
	 *            the port dependency map
	 * @param busDependency
	 *            the bus dependency map
	 * @param doneBusDependency
	 *            the done bus dependency map
	 * @return
	 */
	public static Component createModule(List<Component> components,
			List<Var> inVars, List<Var> outVars, String searchScope,
			Boolean isMutex, Exit.Type exitType, Integer group,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		// Create an Empty Block
		Module module = isMutex ? new MutexBlock(false) : new Block(false);

		// Create the modules IO interface
		createModuleInterface(module, inVars, outVars, exitType,
				portDependency, portGroupDependency, busDependency);

		// Populate Module
		modulePopulate(module, components);

		// Resolve all dependencies
		moduleDependencies(module, components, module.getExit(exitType),
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		// Give the name of the searchScope
		module.specifySearchScope(searchScope);

		// The condition of emptiness on components means that the DONE exit
		// signal should not be connected with the other dependencies of its
		// container
		// if (!components.isEmpty()) {
		if (exitType != Exit.RETURN) {
			// Add done dependency on module, Exit group 0 of a "normal"
			// module
			PortUtil.mapOutControlPort(module, group, doneBusDependency);
		}

		return module;
	}

	/**
	 * This method creates the Input DataPorts and the Output DataBuses of a
	 * module
	 * 
	 * @param module
	 *            the module to add the I/O
	 * @param inVars
	 *            the module inputs
	 * @param outVars
	 *            the module outputs
	 * @param exitType
	 *            the Exit Type tag
	 * @param portDependency
	 *            the port dependency map
	 * @param busDependency
	 *            the bus dependency map
	 */
	public static void createModuleInterface(Module module, List<Var> inVars,
			List<Var> outVars, Exit.Type exitType,
			Map<Port, Var> portDependency,
			Map<Port, Integer> portGroupDependency, Map<Bus, Var> busDependency) {
		// Create Module Input(s) if any
		if (!inVars.isEmpty()) {
			for (Var var : inVars) {
				Port port = module.makeDataPort();
				port.setIDLogical(var.getIndexedName());
				port.getPeer().setIDLogical(var.getIndexedName());
				portDependency.put(port, var);
				portGroupDependency.put(port, 0);
				busDependency.put(port.getPeer(), var);
			}
		}
		// Create module Exit
		moduleExit(module, exitType);

		// Create module Output(s) if any
		if (!outVars.isEmpty()) {
			Exit exit = module.getExit(Exit.DONE);
			for (Var var : outVars) {
				Bus dataBus = exit.makeDataBus();
				Integer busSize = var.getType().isString() ? 1 : var.getType()
						.getSizeInBits();
				boolean isSigned = var.getType().isInt()
						|| var.getType().isBool();
				dataBus.setSize(busSize, isSigned);
				dataBus.setIDLogical(var.getIndexedName());

				portDependency.put(dataBus.getPeer(), var);
				portGroupDependency.put(dataBus.getPeer(), 0);
				busDependency.put(dataBus, var);
			}
		}

	}

	public static Component createTaskCall(Task task, Var outDecision,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		String varName = "decision_outputPattern_" + task.showIDGlobal();
		Decision decision = createDecision(outDecision, varName,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		// Create the the TaskCall component and add the Task
		TaskCall taskCall = new TaskCall();
		taskCall.setTarget(task);

		// Map out TaskCall Done port
		PortUtil.mapOutControlPort(taskCall, 0, doneBusDependency);
		Block thenBlock = (Block) createModule(
				Arrays.asList((Component) taskCall),
				Collections.<Var> emptyList(), Collections.<Var> emptyList(),
				"taskCallThenBlock", false, Exit.DONE, 0, portDependency,
				busDependency, portGroupDependency, doneBusDependency);
		Component branch = createBranch(decision, thenBlock, null,
				Arrays.asList(outDecision), Collections.<Var> emptyList(),
				Collections.<Var, List<Var>> emptyMap(),
				"callTask_" + task.getIDGlobalType(), Exit.DONE,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		Module module = (Module) createModule(Arrays.asList(branch),
				Arrays.asList(outDecision), Collections.<Var> emptyList(),
				"taskCallBlock", false, Exit.DONE, 0, portDependency,
				busDependency, portGroupDependency, doneBusDependency);

		return module;
	}

	public static Component createTaskCallSaveState(Task task, Var outDecision,
			Var stateVar, Var value, ResourceCache resourceCache,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		String varName = "decision_outputPattern_" + task.showIDGlobal();
		Decision decision = createDecision(outDecision, varName,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		List<Component> moduleComponents = new ArrayList<Component>();
		// Create the the TaskCall component and add the Task
		TaskCall taskCall = new TaskCall();
		taskCall.setTarget(task);
		moduleComponents.add(taskCall);

		Component saveState = absoluteMemoryWrite(stateVar, value,
				resourceCache, portDependency, portGroupDependency,
				doneBusDependency);
		moduleComponents.add(saveState);

		// Map out TaskCall Done port
		PortUtil.mapOutControlPort(taskCall, 0, doneBusDependency);
		List<Var> inVars = new ArrayList<Var>();
		inVars.add(value);
		Block thenBlock = (Block) createModule(moduleComponents, inVars,
				Collections.<Var> emptyList(), "taskCallThenBlock", false,
				Exit.DONE, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		inVars.add(outDecision);

		Component branch = createBranch(decision, thenBlock, null, inVars,
				Collections.<Var> emptyList(),
				Collections.<Var, List<Var>> emptyMap(),
				"callTask_" + task.getIDGlobalType(), Exit.DONE,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		Module module = (Module) createModule(Arrays.asList(branch), inVars,
				Collections.<Var> emptyList(), "taskCallBlock", false,
				Exit.DONE, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		return module;
	}

	public static void dataDependencies(Port port, Bus bus,
			Map<Port, Integer> portGroupDependency) {
		int group = portGroupDependency.get(port);
		List<Entry> entries = port.getOwner().getEntries();
		Entry entry = entries.get(group);
		Dependency dep = new DataDependency(bus);
		entry.addDependency(port, dep);
	}

	/**
	 * This method propagates the input of the testBlock of the decision to its
	 * container. Any data inputs to the decision need to be propagated from the
	 * block to the decision. There should be no output ports to propagate. They
	 * are inferred true/false.
	 * 
	 * @param decision
	 * @param testBlock
	 */
	public static void decisionPropagateInputs(Decision decision,
			Block testBlock) {
		for (Port port : testBlock.getDataPorts()) {
			Port decisionPort = decision.makeDataPort();
			Entry entry = port.getOwner().getEntries().get(0);
			entry.addDependency(port,
					new DataDependency(decisionPort.getPeer()));
		}
	}

	public static Component findDecisionComponent(List<Component> components,
			Var decisionVar, Map<Bus, Var> busDependency) {
		Component decisionComponent = null;
		for (Component component : components) {
			Exit exit = component.getExit(Exit.DONE);
			for (Bus bus : exit.getBuses()) {
				Var var = busDependency.get(bus);
				if (var == decisionVar) {
					decisionComponent = component;
				}
			}
		}
		return decisionComponent;
	}

	public static List<Port> getDependencyTargetPorts(
			Collection<Component> components, Var var,
			Map<Port, Var> portDependency) {
		List<Port> targetPorts = new ArrayList<Port>();

		for (Component component : components) {
			for (Port port : component.getDataPorts()) {
				if (portDependency.get(port) == var) {
					targetPorts.add(port);
				}
			}
		}

		return targetPorts;
	}

	public static void moduleDependencies(Module module,
			List<Component> components, Exit exit,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		for (Component component : module.getComponents()) {
			// Build Data Dependencies
			for (Bus bus : component.getDataBuses()) {
				Var busVar = busDependency.get(bus);
				List<Port> targetPorts = getDependencyTargetPorts(
						module.getComponents(), busVar, portDependency);
				for (Port port : targetPorts) {
					int group = portGroupDependency.get(port);
					List<Entry> entries = port.getOwner().getEntries();
					Entry entry = entries.get(group);
					Dependency dep = new DataDependency(bus);
					entry.addDependency(port, dep);
				}
			}

			// Build control Dependencies
			if (!(component instanceof InBuf) && !(component instanceof OutBuf)) {
				Bus doneBus = component.getExit(Exit.DONE).getDoneBus();
				Port donePort = exit.getDoneBus().getPeer();
				List<Entry> entries = donePort.getOwner().getEntries();
				Entry entry = entries.get(doneBusDependency.get(doneBus));
				Dependency dep = new ControlDependency(doneBus);
				entry.addDependency(donePort, dep);
			}
		}

	}

	/**
	 * This method creates the Exit of a module
	 * 
	 * @param module
	 *            a module witch has not an exit
	 * @param exitType
	 *            the Exit Type Tag
	 */
	public static void moduleExit(Module module, Exit.Type exitType) {
		// Add an Exit iff a module has not
		if (module.getExit(Exit.DONE) == null) {
			if ((exitType != null) && (exitType != Exit.DONE)) {
				// Add a latency with an exitType Label
				module.makeExit(0, exitType);
			} else {
				// Add a latency zero Exit
				module.makeExit(0);
			}
		}
	}

	/**
	 * This method populates a module with a given list of components
	 * 
	 * @param module
	 *            an empty module
	 * @param components
	 *            the list of components to add to the module
	 */
	public static void modulePopulate(Module module, List<Component> components) {
		final InBuf inBuf = module.getInBuf();
		final Bus clockBus = inBuf.getClockBus();
		final Bus resetBus = inBuf.getResetBus();
		final Bus goBus = inBuf.getGoBus();

		// I believe that the drivingExit no longer relevant
		Exit drivingExit = inBuf.getExit(Exit.DONE);

		int index = 0;
		for (Component comp : components) {
			if (module instanceof Block) {
				((Block) module).insertComponent(comp, index++);
			} else {
				module.addComponent(comp);
			}

			componentAddEntry(comp, drivingExit, clockBus, resetBus, goBus);

			drivingExit = comp.getExit(Exit.DONE);
		}

		// Ensure that the OutBufs of the module have an entry
		for (OutBuf outbuf : module.getOutBufs()) {
			componentAddEntry(outbuf, drivingExit, clockBus, resetBus, goBus);
		}
	}

	public static void populateExit(Exit exit, List<Var> outVars,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		if (!outVars.isEmpty()) {
			for (Var var : outVars) {
				Bus dataBus = exit.makeDataBus();
				Integer busSize = var.getType().isString() ? 1 : var.getType()
						.getSizeInBits();
				boolean isSigned = var.getType().isInt()
						|| var.getType().isBool();
				dataBus.setSize(busSize, isSigned);
				dataBus.setIDLogical(var.getIndexedName());

				portDependency.put(dataBus.getPeer(), var);
				portGroupDependency.put(dataBus.getPeer(), 0);
				busDependency.put(dataBus, var);
			}
		}

		/** Get the Done Bus **/
		Bus bus = exit.getDoneBus();
		doneBusDependency.put(bus, 0);
	}

}
