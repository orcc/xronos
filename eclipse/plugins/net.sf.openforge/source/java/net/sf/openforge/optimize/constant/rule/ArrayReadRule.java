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

package net.sf.openforge.optimize.constant.rule;

import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.optimize.ComponentSwapVisitor;

/**
 * Replaces an {@link ArrayRead} with a constant index input with a
 * {@link HeapRead}.
 * <P>
 * <i>Temporarily disabled.</i>
 * 
 * @version $Id: ArrayReadRule.java 70 2005-12-01 17:43:11Z imiller $
 */
public class ArrayReadRule {

	public static boolean halfConstant(ArrayRead op, Number[] consts,
			ComponentSwapVisitor visitor) {
		Number indexConstant = consts[1];
		if (indexConstant == null) {
			return false;
		}

		int index = indexConstant.intValue();
		int offset = index * op.getAccessLocationCount();

		final HeapRead heapRead = new HeapRead(op.getAccessLocationCount(),
				op.getMaxAddressWidth(), offset, op.getMemoryRead().isSigned(),
				op.getMemoryAccess().getWidth());

		heapRead.setBlockElement(op.getBlockElement());

		/*
		 * Since the swapComponents() only handles exact one-to-one mapping on
		 * both components, we have to do something on the ArrayRead. An
		 * ArrayRead has an OffsetPort, but a HeapRead does not. The solution
		 * here is trying to remove the offsetPort on an ArrayRead before
		 * passing it to the swapComponents() method call.
		 */
		op.removeDataPort(op.getOffsetPort());

		visitor.swapComponents(op, heapRead);

		op.getLogicalMemoryPort().addAccess(heapRead);
		op.removeFromMemory();
		return true;
	}
}
