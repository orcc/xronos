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

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.backends.ir.IrSpecificFactory;
import net.sf.orcc.backends.transform.CastAdder;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortPeek;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortWrite;

/**
 * Adding Cast for Xronos port extension
 * 
 * @author endrix
 * 
 */
public class XronosCast extends CastAdder {

	public XronosCast(boolean castToUnsigned, boolean createEmptyBlockBasic) {
		super(castToUnsigned, createEmptyBlockBasic);
	}

	@Override
	public Expression defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			doSwitch(((BlockMutex) object).getBlocks());

		} else if (object instanceof InstPortRead) {

			InstPortRead instPortRead = (InstPortRead) object;

			Type uncastedType = ((Port) instPortRead.getPort()).getType();

			Var target = instPortRead.getTarget().getVariable();

			if (needCast(target.getType(), uncastedType)) {
				Var castedTarget = procedure.newTempLocalVariable(
						target.getType(), "casted_" + target.getName());
				castedTarget.setIndex(1);

				target.setType(uncastedType);

				InstCast cast = IrSpecificFactory.eINSTANCE.createInstCast(
						target, castedTarget);
				instPortRead.getBlock().add(indexInst + 1, cast);
			}
		} else if (object instanceof InstPortPeek) {

			InstPortPeek instPortPeek = (InstPortPeek) object;

			Type uncastedType = ((Port) instPortPeek.getPort()).getType();

			Var target = instPortPeek.getTarget().getVariable();

			if (needCast(target.getType(), uncastedType)) {
				Var castedTarget = procedure.newTempLocalVariable(
						target.getType(), "casted_" + target.getName());
				castedTarget.setIndex(1);

				target.setType(uncastedType);

				InstCast cast = IrSpecificFactory.eINSTANCE.createInstCast(
						target, castedTarget);
				instPortPeek.getBlock().add(indexInst + 1, cast);
			}
		} else if (object instanceof InstPortWrite) {
			InstPortWrite instPortWrite = (InstPortWrite) object;
			Type oldParentType = parentType;
			parentType = ((Port) instPortWrite.getPort()).getType();
			instPortWrite.setValue(doSwitch(instPortWrite.getValue()));
			parentType = oldParentType;
		}
		return null;
	}

}
