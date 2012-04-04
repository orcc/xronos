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
package net.sf.orc2hdl.design;

import java.util.ArrayList;
import java.util.List;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.Node;
import net.sf.orcc.ir.NodeBlock;
import net.sf.orcc.ir.NodeIf;

public class DesignOperationFactory extends DesignFactory {
	private final ResourceCache resourceCache;
	private final GenericJob gj;
	private Integer compCounter = 0;

	public DesignOperationFactory(ResourceCache resourceCache) {
		this.resourceCache = resourceCache;
		gj = EngineThread.getEngine().getGenericJob();
	}

	public Component makePinReadOperation(net.sf.orcc.df.Port port,
			PortCache portCache) {
		Component comp = null;
		ActionIOHandler ioHandler = resourceCache.getIOHandler(port);
		comp = ioHandler.getReadAccess();
		setAttributes(
				"pinRead_" + port.getName() + "_"
						+ Integer.toString(compCounter), comp);

		mapIOPorts(port, comp, portCache, true);
		compCounter++;
		return comp;

	}

	public Component makePinWriteOperation(net.sf.orcc.df.Port port,
			PortCache portCache) {
		Component comp = null;
		ActionIOHandler ioHandler = resourceCache.getIOHandler(port);
		comp = ioHandler.getWriteAccess();
		setAttributes(
				"pinWrite_" + port.getName() + "_"
						+ Integer.toString(compCounter), comp);

		mapIOPorts(port, comp, portCache, false);
		compCounter++;
		return comp;

	}

	public List<Component> makeNodeOperations(Node node, PortCache portCache) {
		List<Component> nodeComponets = new ArrayList<Component>();
		if (node.isNodeIf()) {
			// TODO: DesignBranchFactory
			List<Component> thenNodeComponets = new ArrayList<Component>();
			List<Component> elseNodeComponets = new ArrayList<Component>();
			List<Component> joinNodeComponets = new ArrayList<Component>();

			Node thenNode = (Node) ((NodeIf) node).getThenNodes();
			thenNodeComponets = makeNodeOperations(thenNode, portCache);
			nodeComponets.addAll(thenNodeComponets);

			Node elseNode = (Node) ((NodeIf) node).getElseNodes();
			elseNodeComponets = makeNodeOperations(elseNode, portCache);
			nodeComponets.addAll(elseNodeComponets);

			Node joinNode = ((NodeIf) node).getJoinNode();
			joinNodeComponets = makeNodeOperations(joinNode, portCache);
			nodeComponets.addAll(joinNodeComponets);

			nodeComponets.addAll(joinNodeComponets);
		} else if (node.isNodeWhile()) {
			// TODO: DesignLoopFactory
			List<Component> whileNodeComponets = new ArrayList<Component>();
			nodeComponets.addAll(whileNodeComponets);
		} else {
			for (Instruction op : ((NodeBlock) node).getInstructions()) {
				if (op instanceof InstAssign) {
					Expression expr = ((InstAssign) op).getValue();
					if (expr.isExprInt()) {

					}
				}
			}
		}
		return nodeComponets;
	}

	private void mapIOPorts(net.sf.orcc.df.Port port, Component op,
			PortCache portCache, boolean isInput) {

		if (isInput) {
			// pinRead Operation
			for (Bus dataBus : op.getExit(Exit.DONE).getDataBuses()) {
				Bus bus = null;
				// Set the size and the type of the Bus
				// A bus is signed only if the Orcc Type Port is an Integer or a
				// Boolean
				bus = dataBus;
				if (bus.getValue() == null) {
					Boolean isSigned = port.getType().isBool()
							|| port.getType().isInt();
					bus.setSize(port.getType().getSizeInBits(), isSigned);
				}
				portCache.putSource(port, bus);
				// Put Done Bus
				bus = op.getExit(Exit.DONE).getDoneBus();
				portCache.putSource(port, bus);
			}
		} else {
			// pinWrite Operation
			for (Port dataPort : op.getDataPorts()) {
				Port p = dataPort;
				Boolean isSigned = port.getType().isBool()
						|| port.getType().isInt();
				p.setSize(port.getType().getSizeInBits(), isSigned);
				portCache.putTarget(port, p);
				// Put Done Bus
				Bus bus = op.getExit(Exit.DONE).getDoneBus();
				portCache.putSource(port, bus);
			}
		}
	}
}
