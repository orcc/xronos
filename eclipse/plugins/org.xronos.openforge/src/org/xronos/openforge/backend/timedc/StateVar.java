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

package org.xronos.openforge.backend.timedc;

import java.io.PrintStream;

/**
 * StateVar is an interface implemented by those 'special' OpHandles that have
 * explicit declaration needs and have the ability to update their state at a
 * clock edge.
 * 
 * 
 * <p>
 * Created: Wed Mar 9 13:57:57 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: StateVar.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface StateVar {

	/** The C storage class that is used for all global declarations. */
	static final String STORAGE_CLASS = "static ";

	/**
	 * Writes declarations for any stateful variables necessary for this
	 * {@link StateVar} to the specified stream.
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	public void declareGlobal(PrintStream ps);

	/**
	 * Writes update statements for the stateful variables of this
	 * {@link StateVar} to the specified stream.
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	public void writeTick(PrintStream ps);

}// StateVar
