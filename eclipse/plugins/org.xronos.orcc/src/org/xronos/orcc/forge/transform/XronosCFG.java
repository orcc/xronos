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
package org.xronos.orcc.forge.transform;

import java.util.Iterator;
import java.util.List;

import net.sf.orcc.graph.Edge;
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
	private Cfg cfg;

	/**
	 * The last CFG Node
	 */
	private CfgNode last;

	private int bBIndex;

	private int bIfIndex;

	private int bWIndex;

	private CfgNode addNode(String label) {
		CfgNode node = IrFactory.eINSTANCE.createCfgNode();
		node.setLabel(label);
		cfg.add(node);
		return node;
	}

	protected CfgNode addNode(Block block) {
		CfgNode cfgNode = IrFactory.eINSTANCE.createCfgNode(block);
		cfg.add(cfgNode);
		return cfgNode;
	}

	@Override
	public CfgNode caseBlockBasic(BlockBasic block) {
		CfgNode node = addNode(block);
		node.setLabel("bb" + bBIndex);
		bBIndex++;
		last = node;
		return node;
	}

	@Override
	public CfgNode caseBlockIf(BlockIf blockIf) {
		CfgNode node = addNode(blockIf);
		node.setLabel("bIf" + bIfIndex);
		bIfIndex++;
		last = node;

		if (!blockIf.getThenBlocks().isEmpty()) {
			CfgNode lastThenNode = doSwitch(blockIf.getThenBlocks());
			blockIf.setAttribute("lastThenNode", lastThenNode);
		}

		if (!blockIf.getElseBlocks().isEmpty()) {
			CfgNode lastElseNode = doSwitch(blockIf.getElseBlocks());
			blockIf.setAttribute("lastElseNode", lastElseNode);
		}
		last = node;
		return node;
	}

	@Override
	public CfgNode caseBlockWhile(BlockWhile blockWhile) {
		CfgNode node = addNode(blockWhile);
		node.setLabel("bW" + bWIndex);
		bWIndex++;
		last = node;

		if (!blockWhile.getBlocks().isEmpty()) {
			CfgNode lastBlockNode = doSwitch(blockWhile.getBlocks());
			blockWhile.setAttribute("lastBlockNode", lastBlockNode);
		}
		last = node;
		return node;
	}

	@Override
	public CfgNode caseProcedure(Procedure procedure) {
		this.procedure = procedure;
		bBIndex = 0;
		bIfIndex = 0;
		bWIndex = 0;

		cfg = IrFactory.eINSTANCE.createCfg();
		procedure.setCfg(cfg);

		CfgNode entry = addNode("entry");
		cfg.setEntry(entry);

		last = entry;

		CfgNode lastNode = doSwitch(procedure.getBlocks());
 
		CfgNode exit = addNode("exit");
		cfg.setEntry(exit);

		addEdge(lastNode, exit);
		Dota dota = new Dota();
		FilesManager.writeFile(dota.dot(cfg), "/tmp/", procedure.getName()
				+ "_cfg.dot");

		return null;
	}

	public CfgNode doSwitch(List<Block> blocks) {
		CfgNode lastNode = last;

		Iterator<Block> iter = blocks.listIterator();

		while (iter.hasNext()) {
			Block next = iter.next();
			CfgNode node = doSwitch(next);
			if (!lastNode.getLabel().equals("entry")) {
				Block lastBlock = lastNode.getNode();
				if (lastBlock instanceof BlockBasic) {
					addEdge(lastNode, node);
				} else if (lastBlock instanceof BlockIf) {
					if (next.eContainer() != lastBlock) {
						if (!((BlockIf) lastBlock).getThenBlocks().isEmpty()) {
							CfgNode lastThenNode = (CfgNode) lastBlock
									.getAttribute("lastThenNode")
									.getReferencedValue();
							Edge trueEdge = addEdge(lastThenNode, node);
							trueEdge.setLabel("true");
						} else {
							Edge trueEdge = addEdge(lastNode, node);
							trueEdge.setLabel("true");
						}

						if (!((BlockIf) lastBlock).getElseBlocks().isEmpty()) {
							CfgNode lastElseNode = (CfgNode) lastBlock
									.getAttribute("lastElseNode")
									.getReferencedValue();
							Edge falseEdge = addEdge(lastElseNode, node);
							falseEdge.setLabel("false");
						} else {
							Edge falseEdge = addEdge(lastNode, node);
							falseEdge.setLabel("false");
						}
					} else {
						Edge trueEdge = addEdge(lastNode, node);
						trueEdge.setLabel("true");
					}
				} else if (lastBlock instanceof BlockWhile) {
					if (next.eContainer() != lastBlock) {
						if (!((BlockWhile) lastBlock).getBlocks().isEmpty()) {
							CfgNode lasteBlockNode = (CfgNode) lastBlock
									.getAttribute("lastBlockNode")
									.getReferencedValue();
							Edge trueEdge = addEdge(lasteBlockNode, node);
							trueEdge.setLabel("true");
						}
						Edge falseEdge = addEdge(lastNode, node);
						falseEdge.setLabel("false");
					}
				} else {
					Edge trueEdge = addEdge(lastNode, node);
					trueEdge.setLabel("true");
				}
			} else {
				addEdge(lastNode, node);
			}
			lastNode = node;
		}
		last = lastNode;
		return lastNode;
	}

	private Edge addEdge(CfgNode source, CfgNode target) {
		Edge edge = cfg.add(source, target);
		return edge;
	}

}
