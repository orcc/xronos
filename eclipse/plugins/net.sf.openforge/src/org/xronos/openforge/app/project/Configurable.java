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

package org.xronos.openforge.app.project;

import org.xronos.openforge.app.GenericJob;

/**
 * An interface which defines the association of an object with Options.
 * 
 * @author Andreas Kollegger
 */
public interface Configurable {

	/**
	 * Returns the Configurable parent. The parent is involved in hierarchical
	 * searches for option values.
	 */
	public Configurable getConfigurableParent();

	public String getOptionLabel();

	/**
	 * Returns the string label associated with this Configurable.
	 * 
	 */
	public SearchLabel getSearchLabel();

	/**
	 * Gets the GenericJob context for the configurable.
	 */
	public GenericJob getGenericJob();

} /* end interface Configurable */
