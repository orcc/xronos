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

package net.sf.openforge.lim.io;

import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.StateAccessor;
import net.sf.openforge.lim.StateHolder;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;

/**
 * FifoAccess is the superclass of all specific types of accesses to a
 * {@link FifoIF}.
 * 
 * 
 * <p>
 * Created: Tue Dec 16 11:22:23 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoAccess.java 20 2005-08-31 20:14:15Z imiller $
 */
public abstract class FifoAccess extends Module implements Referencer,
		StateAccessor, Visitable {

	private final FifoIF targetInterface;

	/**
	 * Constructs a new FifoAccess instance which targets the specified FifoIF.
	 * 
	 * @param targetInterface
	 *            a value of type 'FifoIF'
	 * @throws IllegalArgumentException
	 *             if targetInterface is null
	 */
	public FifoAccess(FifoIF targetInterface) {
		if (targetInterface == null) {
			throw new IllegalArgumentException(
					"Target fifo interface cannot be null");
		}

		this.targetInterface = targetInterface;
	}

	/**
	 * Accept the specified visitor
	 * 
	 * @param visitor
	 *            a Visitor
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		// TBD
		assert false;
		return false;
	}

	/**
	 * Returns the targetted {@link FifoIF}.
	 * 
	 * @return a non null 'FifoIF'.
	 */
	public FifoIF getFifoIF() {
		return targetInterface;
	}

	/**
	 * determines if this component can be scheduled to execute in fixed known
	 * time (all paths through take same time), but because fifoaccesses may
	 * block on an external flag (ef or ff) this overrides the one in component
	 * to return false.
	 * 
	 * @return false
	 */
	@Override
	public boolean isBalanceable() {
		return false;
	}

	/**
	 * Returns the {@link Referenceable} {@link FifoIF} which this access
	 * targets.
	 * 
	 * @return a non-null {@link Referenceable}
	 */
	@Override
	public Referenceable getReferenceable() {
		return getFifoIF();
	}

	/**
	 * Returns the {@link FifoIF} object that this access targets.
	 * 
	 * @return a non-null StateHolder
	 */
	@Override
	public StateHolder getStateHolder() {
		return getFifoIF();
	}

}// FifoAccess
