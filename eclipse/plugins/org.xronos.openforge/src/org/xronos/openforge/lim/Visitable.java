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

package org.xronos.openforge.lim;

/**
 * Implemented by all classes which can be traversed by a {@link Visitor}.
 * 
 * @author imiller
 * @version $Id: Visitable.java 2 2005-06-09 20:00:48Z imiller $
 * @see Visitor
 */
public interface Visitable {

	void accept(Visitor visitor);

}// Visitable
