/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */

package org.xronos.orcc.backend.transform;

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
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;

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

		List<Expression> restOfIndex = new ArrayList<Expression>(indexes);
		// Get the Dimension for the rest of Indexes
		List<Integer> listDim = listType.getDimensions();

		Expression restIndex = null;
		// Index in OpenForge is represented like a 32bit Integer
		Type exrpType = IrFactory.eINSTANCE.createTypeInt(32);
		restIndex = restOfIndex.get(0);
		for (int i = 1; i < listDim.size(); i++) {
			ExprInt exprDim = IrFactory.eINSTANCE.createExprInt(listDim.get(i));
			restIndex = IrFactory.eINSTANCE.createExprBinary(restIndex,
					OpBinary.TIMES, exprDim, exrpType);
			restIndex = IrFactory.eINSTANCE.createExprBinary(restIndex,
					OpBinary.PLUS, restOfIndex.get(i), exrpType);
		}

		Var indexVar = procedure.newTempLocalVariable(
				IrFactory.eINSTANCE.createTypeInt(), "index");
		// sets indexVar as memory index
		IrUtil.delete(indexes);
		indexes.add(IrFactory.eINSTANCE.createExprVar(indexVar));
		// Add the assign instruction that hold the one-dim index
		InstAssign assign = IrFactory.eINSTANCE.createInstAssign(indexVar,
				restIndex);
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

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			doSwitch(((BlockMutex) object).getBlocks());
		}
		return super.defaultCase(object);
	}
}
