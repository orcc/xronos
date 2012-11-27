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

package org.xronos.openforge.lim.io;

/**
 * DeclarationGenerator is an interface which provides a method capable of
 * generating a C syntax declaration String. This is used to abstract the
 * specific C type from the ATB/report/etc classes that simply need to declare a
 * variable of the type without wanting to know the details of the type.
 * <p>
 * This interface was made necessary so that the automatic testbench generation
 * could create variables in a reflected C file of the correct type for each
 * parameter. The problem being solved here is that the type of the variable
 * determines the way that the variable is declared. If the variable is
 * <code>foo</code> then we need declarations of type:
 * <ul>
 * <li><code>int <i>foo</i></code>
 * <li><code>int <i>foo</i>[4]</code>
 * <li><code>struct *<i>foo</i>[4]</code>
 * </ul>
 * 
 * <p>
 * Created: Tue Feb 10 10:54:54 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DeclarationGenerator.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface DeclarationGenerator {

	/**
	 * return a correctly formatted String declaring (in C syntax) the
	 * identifier where the type is defined by the specific implementation of
	 * this interface.
	 */
	public String getDeclaration(String identifier);

}// DeclarationGenerator
