package org.xronos.orcc.backend.transform;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.State;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

public class ActorFSMStates extends DfVisitor<Void> {

	@Override
	public Void caseActor(Actor actor) {
		if (actor.getFsm() != null) {
			// Add state Vars
			for (State state : actor.getFsm().getStates()) {
				Type typeBool = IrFactory.eINSTANCE.createTypeBool();
				Var fsmState = IrFactory.eINSTANCE.createVar(typeBool, "state_"
						+ state.getName(), true, 0);
				if (state == actor.getFsm().getInitialState()) {
					fsmState.setValue(true);
					fsmState.setInitialValue(IrFactory.eINSTANCE.createExprBool(true));
				} else {
					fsmState.setValue(false);
					fsmState.setInitialValue(IrFactory.eINSTANCE.createExprBool(false));
				}
				actor.getStateVars().add(fsmState);
			}
		}
		return null;
	}

}
