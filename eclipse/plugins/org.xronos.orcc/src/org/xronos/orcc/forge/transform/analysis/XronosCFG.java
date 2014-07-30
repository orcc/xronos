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
package org.xronos.orcc.forge.transform.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.graph.Edge;
import net.sf.orcc.graph.Graph;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.graph.util.Dota;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Cfg;
import net.sf.orcc.ir.CfgNode;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.FilesManager;
import net.sf.orcc.util.util.EcoreHelper;

/**
 * This visitor builds the {@link Cfg Control Flow Graph} of a {@link Procedure}
 * 
 * @author Endri Bezati
 *
 */
public class XronosCFG extends AbstractIrVisitor<CfgNode> {

	/**
	 * This procedure CFG
	 */
	protected Cfg cfg;

	/**
	 * Type of the edge (True or False)
	 */
	protected boolean flag;

	/**
	 * Last visited CFG Node
	 */
	protected CfgNode last;

	/**
	 * Basic Block index
	 */
	protected int bBasicIndex;

	/**
	 * Block If index
	 */
	protected int bIfIndex;

	/**
	 * Block While index
	 */
	protected int bWhileIndex;

	/**
	 * Creates an edge to this CFG
	 * 
	 * @param node
	 */
	protected void addEdge(CfgNode node) {
		Edge edge = cfg.add(last, node);
		if (flag) {
			edge.setLabel("true");
			// reset flag to false (so it is only set for this edge)
			flag = false;
		}
	}

	/**
	 * Creates a node and adds it to this CFG.
	 * 
	 * @param block
	 *            the related block
	 * @return a newly-created node
	 */
	protected CfgNode addNode(Block block) {
		CfgNode cfgNode = IrFactory.eINSTANCE.createCfgNode(block);
		cfg.add(cfgNode);
		return cfgNode;
	}

	/**
	 * Creates an empty node with a label and adds it to this CFG
	 * 
	 * @param label
	 * @return
	 */
	protected CfgNode addNode(String label) {
		CfgNode node = IrFactory.eINSTANCE.createCfgNode();
		node.setLabel(label);
		cfg.add(node);

		return node;
	}

	@Override
	public CfgNode caseBlockBasic(BlockBasic block) {
		CfgNode cfgNode = addNode(block);
		cfgNode.setLabel("bB" + bBasicIndex);
		bBasicIndex++;
		if (last != null) {
			addEdge(cfgNode);
		}

		return cfgNode;
	}

	@Override
	public CfgNode caseBlockIf(BlockIf block) {
		CfgNode cfgNode = addNode(block);
		cfgNode.setLabel("bIf" + bIfIndex);
		bIfIndex++;
		if (last != null) {
			addEdge(cfgNode);
		}

		CfgNode join = addNode("join");

		last = cfgNode;
		flag = true;
		last = doSwitch(block.getThenBlocks());

		// reset flag (in case there are no nodes in "then" branch)
		flag = false;
		addEdge(join);

		last = cfgNode;
		last = doSwitch(block.getElseBlocks());
		addEdge(join);
		last = join;

		return join;
	}

	public CfgNode caseBlockMutex(BlockMutex blockMutex) {
		last = doSwitch(blockMutex.getBlocks());
		return last;
	}
	
	@Override
	public CfgNode caseBlockWhile(BlockWhile block) {
		CfgNode cfgNode = addNode(block);
		cfgNode.setLabel("bW" + bWhileIndex);
		bWhileIndex++;
		if (last != null) {
			addEdge(cfgNode);
		}
		
		CfgNode join = addNode("join");
		
		flag = true;
		last = cfgNode;
		
		flag = true;
		last = doSwitch(block.getBlocks());
		
		addEdge(cfgNode);
		// reset flag (in case there are no block in "then" branch)
		flag = false;
		addEdge(join);
		last = cfgNode;

		return join;
	}

	@Override
	public CfgNode caseProcedure(Procedure procedure) {
		bBasicIndex = 0;
		bIfIndex = 0;
		bWhileIndex = 0;

		cfg = IrFactory.eINSTANCE.createCfg();
		procedure.setCfg(cfg);

		CfgNode entry = addNode("entry");
		cfg.setEntry(entry);
		last = entry;

		last = super.caseProcedure(procedure);

		CfgNode exit = addNode("exit");
		cfg.setExit(exit);
		addEdge(exit);
		removeJoins(cfg);
		//cfg.computeDominance();

		Action action = EcoreHelper.getContainerOfType(procedure, Action.class);
		if (action != null) {
			if (action.hasAttribute("xronos_cfg")) {
				Dota dota = new Dota();
				FilesManager.writeFile(dota.dot(cfg), "/tmp/",
						((Actor) action.eContainer()).getName() + "_"
								+ procedure.getName() + "_cfg.dot");
			}
		}else{
			if(procedure.hasAttribute("xronos_cfg")){
				Dota dota = new Dota();
				FilesManager.writeFile(dota.dot(cfg), "/tmp/",
						procedure.getName() + "_cfg.dot");
			}
		}
		return last;
	}

	/**
	 * Visits the given block list.
	 * 
	 * @param blocks
	 *            a list of blocks
	 * @return the last block of the block list
	 */
	public CfgNode doSwitch(List<Block> blocks) {
		for (Block block : blocks) {
			last = doSwitch(block);
		}

		return last;
	}

	/**
	 * This method removes empty joins found on Block Ifs and Whiles
	 * 
	 * @param g
	 */
	private void removeJoins(Graph g) {
		List<Vertex> nodesToDelete = new ArrayList<Vertex>();
		for (Vertex vertex : g.getVertices()) {
			List<Edge> edgeToDelete = new ArrayList<Edge>();
			if (vertex.getLabel().equals("join")) {
				for (Edge inEdge : vertex.getIncoming()) {
					for (Edge outEdge : vertex.getOutgoing()) {
						// Create a new Edge
						Vertex source = inEdge.getSource();
						Vertex target = outEdge.getTarget();
						g.add(source, target);
						edgeToDelete.add(inEdge);
						edgeToDelete.add(outEdge);
					}
				}
				g.removeEdges(edgeToDelete);
				nodesToDelete.add(vertex);
			}

		}
		g.removeVertices(nodesToDelete);
	}

	@Override
	public CfgNode defaultCase(EObject object) {
		if(object instanceof BlockMutex){
			return caseBlockMutex((BlockMutex) object);
		}
		return null;
	}

}
