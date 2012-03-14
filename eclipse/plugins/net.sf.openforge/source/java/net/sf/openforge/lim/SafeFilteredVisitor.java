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
 * SafeFilteredVisitor is an implementation of the FilteredVisitor which creates
 * a new linked list for all Collections before iterating over them. This
 * eliminates the problems of ConcurrentModification exceptions if/when
 * iterating over the components of a module and adding/deleting components
 * to/from that module.
 * 
 * 
 * Created: Thu Jul 11 14:32:46 2002
 * 
 * @author imiller
 * @version $Id: SafeFilteredVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SafeFilteredVisitor extends FilteredVisitor {

	public SafeFilteredVisitor() {
		super();
		this.scanner = new Scanner(this, true);
	}

}// SafeFilteredVisitor
