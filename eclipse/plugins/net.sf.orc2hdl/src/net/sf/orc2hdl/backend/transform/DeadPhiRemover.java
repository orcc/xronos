package net.sf.orc2hdl.backend.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orc2hdl.design.visitors.BlockVars;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

import org.eclipse.emf.ecore.EObject;

public class DeadPhiRemover extends AbstractIrVisitor<Void> {

	Block currentBlock;
	Map<Block, List<Var>> usedVariables;
	Map<Block, List<InstPhi>> phiToBeRemoved;
	private LinkedList<Block> nestedBlock;

	@Override
	public Void caseProcedure(Procedure procedure) {
		phiToBeRemoved = new HashMap<Block, List<InstPhi>>();
		usedVariables = new HashMap<Block, List<Var>>();
		nestedBlock = new LinkedList<Block>();
		return super.caseProcedure(procedure);
	}

	@Override
	public Void caseBlockWhile(BlockWhile nodeWhile) {
		// Initialize Variables
		currentBlock = nodeWhile;
		List<InstPhi> rPhi = new ArrayList<InstPhi>();
		phiToBeRemoved.put(nodeWhile, rPhi);
		List<Block> parentBlocks = new ArrayList<Block>();

		if (nestedBlock.isEmpty()) {
			nestedBlock.addFirst(nodeWhile);
		} else {
			nestedBlock.add(nodeWhile);
		}

		// Test if the container is a procedure
		if (nodeWhile.eContainer() instanceof Procedure) {
			parentBlocks = new ArrayList<Block>(procedure.getBlocks());

		} else if (nodeWhile.eContainer() instanceof BlockWhile) {
			BlockWhile parent = (BlockWhile) nodeWhile.eContainer();
			parentBlocks = new ArrayList<Block>(parent.getBlocks());
		} else if (nodeWhile.eContainer() instanceof BlockIf) {
			BlockIf parent = (BlockIf) nodeWhile.eContainer();
			List<Block> thenBlocks = parent.getThenBlocks();
			List<Block> elseBlocks = parent.getElseBlocks();
			if (thenBlocks.contains(nodeWhile)) {
				parentBlocks = thenBlocks;
			} else if (elseBlocks.contains(nodeWhile)) {
				parentBlocks = elseBlocks;
			}

		}

		// Get the last index of nodeWhile
		int lastIndexOf = parentBlocks.lastIndexOf(nodeWhile);

		// Remove all blocks before the nodeWhile
		List<Block> usedBlocks = parentBlocks.subList(lastIndexOf + 1,
				parentBlocks.size());

		usedVariables.put(nodeWhile, getVars(true, false, usedBlocks, null));

		super.caseBlockWhile(nodeWhile);

		// Delete the unused Phis
		for (InstPhi phi : phiToBeRemoved.get(nodeWhile)) {
			IrUtil.delete(phi);
		}

		// Fix currentBlock
		int indexOfLastBlock = nestedBlock.lastIndexOf(nodeWhile);
		if (indexOfLastBlock != 0) {
			if (nestedBlock.get(indexOfLastBlock - 1) == nodeWhile.eContainer()) {
				currentBlock = nestedBlock.get(indexOfLastBlock - 1);
			}
		}
		nestedBlock.remove(nodeWhile);
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		Var target = phi.getTarget().getVariable();
		if (target.getUses().isEmpty()) {
			// This target is not used anywhere it should be deleted
			phiToBeRemoved.get(currentBlock).add(phi);
		} else {
			if (!usedVariables.get(currentBlock).contains(target)) {
				if (usedOnlyInPhi(target)) {
					phiToBeRemoved.get(currentBlock).add(phi);
				}
			}
		}
		super.caseInstPhi(phi);
		return null;
	}

	private List<Var> getVars(Boolean input, Boolean deepSearch,
			List<Block> blocks, Block phiBlock) {
		List<Var> vars = new ArrayList<Var>();
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				Set<Var> blkVars = new BlockVars(input, deepSearch, blocks,
						phiBlock).doSwitch(block);
				for (Var var : blkVars) {
					if (!vars.contains(var)) {
						vars.add(var);
					}
				}
			}
		}
		return vars;
	}

	private Boolean usedOnlyInPhi(Var var) {
		Map<Use, Boolean> useMap = new HashMap<Use, Boolean>();
		for (Use use : var.getUses()) {
			EObject container = use.eContainer();
			// Get the BlockBasic container
			while (!(container instanceof BlockBasic)) {
				container = container.eContainer();
				if (container instanceof InstPhi) {
					useMap.put(use, true);
					break;
				}
			}
			if (container instanceof BlockBasic) {
				useMap.put(use, false);
			}
		}

		if (!useMap.containsValue(false)) {
			return true;
		} else {
			return false;
		}

	}

}
