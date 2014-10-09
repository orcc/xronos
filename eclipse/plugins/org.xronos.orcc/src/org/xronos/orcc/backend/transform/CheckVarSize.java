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

package org.xronos.orcc.backend.transform;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ValueUtil;
import net.sf.orcc.util.OrccLogger;

/**
 * This class visits all global and local variables that are initialize so that
 * it checks if its type has a correct size with the containing value.
 * 
 * @author Endri Bezati
 * 
 */
public class CheckVarSize extends DfVisitor<Void> {

	public class CheckProcedureVarTypes extends AbstractIrVisitor<Void> {

		@Override
		public Void caseProcedure(Procedure procedure) {
			for (Var var : procedure.getLocals()) {
				if (!var.isAssignable() && var.isInitialized()) {
					checkAndModifySize(var);
				}
			}
			return null;
		}

	}

	public CheckVarSize() {
		CheckProcedureVarTypes checkProcedureVarTypes = new CheckProcedureVarTypes();
		irVisitor = checkProcedureVarTypes;
	}

	@Override
	public Void caseActor(Actor actor) {
		for (Var var : actor.getStateVars()) {
			if (!var.isAssignable() && var.isInitialized()) {
				checkAndModifySize(var);
			}
		}
		return null;
	}

	private Integer checkAndModifySize(Object obj, List<Integer> dimension,
			Type type) {
		Integer size = 0;
		if (dimension.size() > 1) {

			List<Integer> newListDimension = new ArrayList<Integer>(dimension);

			Integer firstDim = dimension.get(0);
			newListDimension.remove(0);

			for (int i = 0; i < firstDim; i++) {
				size = checkAndModifySize(Array.get(obj, i), newListDimension,
						type);
			}
		} else {
			if (dimension.get(0).equals(1)) {
				BigInteger value = BigInteger.valueOf(0);
				size = value.bitLength();
				if(type.isInt()){
					size++;
				}
			} else {
				BigInteger max = BigInteger.ZERO;
				for (int i = 0; i < dimension.get(0); i++) {
					BigInteger value = (BigInteger) ValueUtil.get(type, obj, i);
					if (value.compareTo(max) > 0) {
						max = value;
					}
				}
				size = max.bitLength();
				if(type.isInt()){
					size++;
				}
			}
		}

		return size;
	}

	private void checkAndModifySize(Var var) {
		Type type = var.getType();
		if (var.isInitialized()) {
			if (type.isList()) {
				TypeList typeList = (TypeList) var.getType();
				Type innerType = typeList.getInnermostType();
				if (innerType.isInt() || innerType.isUint()) {
					List<Integer> listDimension = new ArrayList<Integer>(
							typeList.getDimensions());
					Object varValue = var.getValue();
					int innerSize = typeList.getInnermostType().getSizeInBits();
					int newInnerSize = checkAndModifySize(varValue,
							listDimension, innerType);
					if (innerSize != newInnerSize) {
						OrccLogger.warnln("Variable: " + var.getName()
								+ " has a wrong size, its correct size is: "
								+ newInnerSize + " instead of: " + innerSize);
						typeList.getInnermostType().setSize(newInnerSize);
					}
				}
			} else {
				if (type.isInt() || type.isUint()) {
					BigInteger value = (BigInteger) var.getValue();
					int size = type.getSizeInBits();
					int newSize = value.bitLength();
					if (size != newSize) {
						OrccLogger.warnln("Variable: " + var.getName()
								+ " has a wrong size, its correct size is: "
								+ newSize + " instead of: " + newSize);
						type.setSize(newSize);
					}
				}
			}
		}
	}

}
