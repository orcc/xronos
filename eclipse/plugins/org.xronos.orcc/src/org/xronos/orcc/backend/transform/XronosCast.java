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
