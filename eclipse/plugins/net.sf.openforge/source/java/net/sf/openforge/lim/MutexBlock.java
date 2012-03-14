/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package net.sf.openforge.lim;

import java.util.List;

/**
 * If set to true this is an indication that this module contains components
 * whose 'execution' are mutually exclusive. In reality, there can be some
 * components that are not mutually exclusive so long as their execution is not
 * visible outside this module. Non-visibility means also that operation outside
 * this module cannot affect those components behavior. eg state var reads would
 * violate the non-visibility. The effect of this constraint is that there is no
 * need to insert resource dependencies between the components of this module
 * because it is guaranteed that only one of the components will execute. {@see
 * GlobalResourceSequencer}
 * 
 * @author imiller
 * @version $Id: MutexBlock.java 100 2006-02-03 22:49:08Z imiller $
 */
public class MutexBlock extends Block {
	private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

	/**
	 * Constructs a mutex block whose population is deferred.
	 */
	public MutexBlock(boolean isProcedureBody) {
		super(isProcedureBody);
	}

	public MutexBlock(List sequence, boolean isProcedureBody) {
		super(sequence, isProcedureBody);
	}

	public boolean isMutexModule() {
		return true;
	}

}
