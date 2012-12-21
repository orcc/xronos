package org.xronos.orcc.backend.transform;

import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

public class XronosConstantPropagation extends AbstractIrVisitor<Void> {

	public XronosConstantPropagation() {
		super(true);
	}

	@Override
	public Void caseInstLoad(InstLoad load) {
		Use use = load.getSource();
		Var var = use.getVariable();
		if (var.isGlobal()) {
			if (!var.isAssignable()) {
				if (var.getType().isInt() || var.getType().isUint()
						|| var.getType().isFloat() || var.getType().isBool()) {
					if (var.getInitialValue() != null) {
						Var target = load.getTarget().getVariable();
						Expression value = IrUtil.copy(var.getInitialValue());
						InstAssign assign = IrFactory.eINSTANCE
								.createInstAssign(target, value);
						BlockBasic block = load.getBlock();
						int index = block.getInstructions().indexOf(load);
						block.add(index, assign);
						IrUtil.delete(load);
					}
				}
			}
		}
		return null;
	}
}
