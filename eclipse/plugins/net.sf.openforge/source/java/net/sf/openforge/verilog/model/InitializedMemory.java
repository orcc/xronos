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

package net.sf.openforge.verilog.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * InitializedMemory is a representation of a Verilog array which, when
 * lexicalified will return a collection initialization assignments, one for
 * each location in the memory.
 * 
 * <pre>
 *  arg0_data[0]<=32 'h 80000000;
 *  arg0_data[1]<=32 'h 80000000;
 *  arg0_data[2]<=32 'h 80000000;
 *  arg0_data[3]<=32 'h 80000000;
 *  arg0_data[4]<=32 'h 80000000;
 * </pre>
 * 
 * <p>
 * Created: Fri Aug 23 12:41:43 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: InitializedMemory.java 2 2005-06-09 20:00:48Z imiller $
 */
public class InitializedMemory extends Register implements Statement {

	private List initialValues;

	public InitializedMemory(String name, int width) {
		super(name, width);
		this.initialValues = new ArrayList();
	}

	/**
	 * Adds one more location to the memory and specifies it's initialization
	 * value via the given Expression.
	 */
	public void addInitValue(Expression init) {
		this.initialValues.add(init);
	}

	public Collection getNets() {
		return Collections.singleton(this);
	}

	/**
	 * Returns the number of locations in this memory.
	 */
	public int depth() {
		return initialValues.size();
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		int i = 0;
		for (Iterator iter = initialValues.iterator(); iter.hasNext();) {
			Expression right = (Expression) iter.next();
			lex.append(new Assign.NonBlocking(new MemoryElement(this, i++),
					right));
		}
		return lex;
	}

}// InitializedMemory
