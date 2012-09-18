package net.sf.orc2hdl.backend.transform;

import java.util.ArrayList;

import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * This visitor initialize all non-initialize local variables that are not Lists
 * 
 * @author Endri Bezati
 * 
 */
public class LocalVarInitializer extends AbstractIrVisitor<Void> {
	@Override
	public Void caseProcedure(Procedure procedure) {
		for (Var var : new ArrayList<Var>(procedure.getLocals())) {
			if (!var.getType().isList()) {
				if (!var.isInitialized()) {
					initializeVar(var);
				}
			}
		}
		return null;
	}

	private void initializeVar(Var variable) {
		Type type = variable.getType();
		Expression initConst = variable.getInitialValue();
		if (initConst == null) {
			if (type.isBool()) {
				variable.setValue(false);
			} else if (type.isInt() || type.isUint()) {
				variable.setValue(0);
			}
		}

	}
}
