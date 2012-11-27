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

package org.xronos.openforge.schedule.block;

import java.util.Set;

import org.xronos.openforge.lim.Component;


/**
 * ModuleStallSource is a lightweight interface that is used to supply
 * Scheduling the source of a stall signal. This interface is used to abstract
 * away the details of obtaining the source of the stall signal.
 * 
 * 
 * <p>
 * Created: Mon Nov 15 11:58:30 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ModuleStallSource.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface ModuleStallSource {

	/**
	 * Returns a Set of {@link Component} objects that need to stall a Module.
	 * 
	 * @return a non-null Set of {@link Component} objects.
	 */
	public Set<Component> getStallingComponents();

}// ModuleStallSource
