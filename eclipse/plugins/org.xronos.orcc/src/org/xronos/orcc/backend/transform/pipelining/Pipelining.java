package org.xronos.orcc.backend.transform.pipelining;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.util.DfVisitor;

public class Pipelining extends DfVisitor<Void> {

	/**
	 * Define the number of stages
	 */
	private int stages;

	/**
	 * Define the time of a Stage
	 */
	private float stageTime;

	public Pipelining(int stages, float stageTime) {
		this.stages = stages;
		this.stageTime = stageTime;
	}

	@Override
	public Void caseAction(Action action) {
		// Apply iff the action has the xronos_pipeline tag
		if (action.hasAttribute("xronos_pipeline")) {
			OperatorsIO operatorsIO = new OperatorsIO();
			operatorsIO.doSwitch(action.getBody());
		}
		return null;
	}

}
