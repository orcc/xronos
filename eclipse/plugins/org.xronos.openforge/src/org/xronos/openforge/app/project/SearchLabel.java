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

package org.xronos.openforge.app.project;

import java.util.List;

/**
 * A label which can be applied to any structural piece of code, common in most
 * languages.
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: SearchLabel.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface SearchLabel {

	/**
	 * Returns a List which produces an increasingly general sequence of label
	 * strings.
	 * 
	 * @see SearchIterator
	 */
	public List<String> getSearchList();

	/**
	 * Returns a List which produces an increasingly general sequence of label
	 * strings.
	 * 
	 * @see SearchIterator
	 */
	public List<String> getSearchList(String postfix);

	/**
	 * Returns the String label associated with this SearchLabel if applicable,
	 * null otherwise.
	 */
	public String getLabel();
}
