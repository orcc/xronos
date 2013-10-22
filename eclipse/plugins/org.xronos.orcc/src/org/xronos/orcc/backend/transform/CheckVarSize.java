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
				if (!var.isAssignable() || var.isInitialized()) {
					checkAndModifySize(var);
				}
			}
			return super.caseProcedure(procedure);
		}

	}

	public CheckVarSize() {
		CheckProcedureVarTypes checkProcedureVarTypes = new CheckProcedureVarTypes();
		irVisitor = checkProcedureVarTypes;
	}

	@Override
	public Void caseActor(Actor actor) {
		for (Var var : actor.getStateVars()) {
			checkAndModifySize(var);
		}
		return super.caseActor(actor);
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
			} else {
				BigInteger max = BigInteger.ZERO;
				for (int i = 0; i < dimension.get(0); i++) {
					BigInteger value = (BigInteger) ValueUtil.get(type, obj, i);
					if (value.compareTo(max) > 0) {
						max = value;
					}
				}
				size = max.bitLength();
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
					if (innerSize < newInnerSize) {
						OrccLogger.warnln("Variable: " + var.getName()
								+ " has a wrong size, its correct size is: "
								+ newInnerSize + " instead of: " + innerSize);
						typeList.getType().setSize(newInnerSize);
					}
				}
			} else {
				if (type.isInt() || type.isUint()) {
					BigInteger value = (BigInteger) var.getValue();
					int size = type.getSizeInBits();
					int newSize = value.bitLength();
					if (size < newSize) {
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
