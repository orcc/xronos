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

/**
 * Referenceable is an interface that is implemented by any entity which can be
 * accessed by components in the LIM but that is not tied directly to those
 * components via data and control paths (during scheduling). These may include
 * memories, registers, interfaces, pins, etc. This interface provides the
 * necessary information to correctly create dependencies between those accesses
 * and thus preserve their validity. This interface works in conjunction with
 * the {@link Referencer} interface.
 * 
 * <p>
 * Created: Tue Dec 16 11:39:23 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: Referenceable.java 538 2007-11-21 06:22:39Z imiller $
 */
public interface Referenceable {
	static final String _RCS_ = "$Rev: 538 $";

	/**
	 * Returns the minimum number of clock edges that must seperate the given
	 * two Referencer accesses. The determination is specific to the types of
	 * accesses and the implementation of this Referenceable element.
	 */
	public int getSpacing(Referencer from, Referencer to);

	/**
	 * Returns the minimum number of clock edges that must seperate the GO
	 * signals of the two Referencer accesses. Return value of -1 indicates that
	 * the accesses cannot be scheduled on the basis of their GO signals and
	 * must defer to the standard DONE to GO spacing.
	 */
	public int getGoSpacing(Referencer from, Referencer to);

}// Referenceable
