/*
 * Copyright (c) 2011, Ecole Polytechnique Fédérale de Lausanne
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
package net.sf.orc2hdl.backend.transformations;

import java.util.ArrayList;
import java.util.List;

import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

/**
 * This class defines an actor transformation that transforms index declaration
 * of dimension 2 and superior multi-dimensional list to a declaration of a
 * single index list.
 * 
 * @author Endri Bezati
 * 
 */
public class IndexFlattener extends AbstractIrVisitor<Void> {

	private void toOneDimIndex(BlockBasic currentBlock,
			List<Expression> indexes, Type listType) {

		// Get last index
		Integer lastIndex = indexes.size() - 1;
		Expression lastExpr = indexes.get(lastIndex);
		// For the rest of the indexes create a binary expression
		// of the index * Dim(index)
		List<Expression> restOfIndex = new ArrayList<Expression>(indexes);
		restOfIndex.remove(lastExpr);
		// Get the Dimension for the rest of Indexes
		List<Integer> listDim = listType.getDimensions();

		int dimCounter = listDim.size() - 1;
		Expression restIndex = null;
		// Index in openForge is represented like a 32bit Integer
		Type exrpType = IrFactory.eINSTANCE.createTypeInt(32);

		for (Expression expr : new ArrayList<Expression>(restOfIndex)) {
			Integer dim = listDim.get(dimCounter);
			ExprInt exprDim = IrFactory.eINSTANCE.createExprInt(dim);
			if (restIndex == null) {
				restIndex = IrFactory.eINSTANCE.createExprBinary(expr,
						OpBinary.TIMES, exprDim, exrpType);
			} else {
				Expression e = IrFactory.eINSTANCE.createExprBinary(expr,
						OpBinary.TIMES, exprDim, exrpType);
				restIndex = IrFactory.eINSTANCE.createExprBinary(restIndex,
						OpBinary.PLUS, e, exrpType);
			}
			dimCounter--;
		}

		Expression finalIndex = IrFactory.eINSTANCE.createExprBinary(restIndex,
				OpBinary.PLUS, lastExpr, exrpType);

		Var indexVar = procedure.newTempLocalVariable(
				IrFactory.eINSTANCE.createTypeInt(), "index");
		// sets indexVar as memory index
		IrUtil.delete(indexes);
		indexes.add(IrFactory.eINSTANCE.createExprVar(indexVar));
		// Add the assign instruction that hold the one-dim index
		InstAssign assign = IrFactory.eINSTANCE.createInstAssign(indexVar,
				finalIndex);
		currentBlock.add(indexInst, assign);
		indexInst++;
	}

	@Override
	public Void caseInstLoad(InstLoad load) {
		List<Expression> indexes = load.getIndexes();

		if (indexes.size() > 1) {
			toOneDimIndex(
					EcoreHelper.getContainerOfType(load, BlockBasic.class),
					indexes,
					IrUtil.copy(load.getSource().getVariable().getType()));
		}

		return null;
	}

	@Override
	public Void caseInstStore(InstStore store) {
		List<Expression> indexes = store.getIndexes();

		if (indexes.size() > 1) {
			toOneDimIndex(
					EcoreHelper.getContainerOfType(store, BlockBasic.class),
					indexes,
					IrUtil.copy(store.getTarget().getVariable().getType()));
		}

		return null;
	}
}
