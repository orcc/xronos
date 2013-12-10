package org.xronos.orcc.backend.transform;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;

import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

public class AssignConstantPropagator extends AbstractIrVisitor<Void> {

	Boolean isModified;

	@Override
	public Void caseInstAssign(InstAssign assign) {
		Boolean usedInPhi = false;
		Expression value = assign.getValue();
		if (value.isExprBool() || value.isExprFloat() || value.isExprInt()
				|| value.isExprString()) {
			EList<Use> targetUses = assign.getTarget().getVariable().getUses();
			while (!targetUses.isEmpty() && !usedInPhi) {
				InstPhi phi = EcoreHelper.getContainerOfType(targetUses.get(0),
						InstPhi.class);
				if (phi == null) {
					ExprVar expr = EcoreHelper.getContainerOfType(
							targetUses.get(0), ExprVar.class);
					EcoreUtil.replace(expr, IrUtil.copy(value));
					IrUtil.delete(expr);
				} else {
					usedInPhi = true;
				}
			}
			if (!usedInPhi) {
				isModified = true;
				IrUtil.delete(assign);
				indexInst--;
			}
		}
		return null;
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		do {
			isModified = false;
			super.caseProcedure(procedure);
		} while (isModified);
		return null;
	}

}
