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

import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.optimize.ComponentSwapVisitor;

/**
 * Replaces an {@link ArrayWrite} with a constant index input with a
 * {@link HeapWrite}.
 * <P>
 * <i>Temporarily disabled.</i>
 * 
 * @version $Id: ArrayWriteRule.java 70 2005-12-01 17:43:11Z imiller $
 */
public class ArrayWriteRule {

	public static boolean halfConstant(ArrayWrite op, Number[] consts,
			ComponentSwapVisitor visitor) {
		Number indexConstant = consts[2];
		if (indexConstant == null) {
			return false;
		}

		int index = indexConstant.intValue();
		int offset = index * op.getAccessLocationCount();

		final HeapWrite heapWrite = new HeapWrite(op.getAccessLocationCount(),
				op.getMaxAddressWidth(), offset,
				op.getMemoryWrite().isSigned(), op.getMemoryAccess().getWidth());

		heapWrite.setBlockElement(op.getBlockElement());

		/*
		 * Since the swapComponents() only handles exact one-to-one mapping on
		 * both components, we have to do something on the ArrayWrite. An
		 * ArrayWrite has an OffsetPort, but a HeapWrite does not. The solution
		 * here is trying to remove the offsetPort on an ArrayWrite before
		 * passing it to the swapComponets() method call.
		 */
		op.removeDataPort(op.getOffsetPort());

		visitor.swapComponents(op, heapWrite);

		op.getLogicalMemoryPort().addAccess(heapWrite);
		op.removeFromMemory();

		return true;
	}

}
