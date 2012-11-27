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

package org.xronos.openforge.verilog.testbench;

import java.util.ArrayList;
import java.util.List;

import org.xronos.openforge.lim.Task;


/**
 * TestVector is a utility object that is used to maintain all information
 * necessary for one assert/validate cycle of a design being tested. As such it
 * contains the following information:
 * <ul>
 * <li>Entry method to be tested.
 * <li>List of input arguments (in java.lang wrappers)
 * <li>Result (in java.lang.wrapper)
 * <li>Result qualifier (is result 'valid' or not)
 * </ul>
 * 
 * 
 * <p>
 * Created: Thu Aug 22 12:14:51 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: TestVector.java 2 2005-06-09 20:00:48Z imiller $
 */
public class TestVector {

	private final Task task;
	private final List<Object> argValues;
	private final Object resultValue;
	private final boolean resultValid;

	public TestVector(Task task, List<Object> args, Object result,
			boolean resultValid) {
		this.task = task;
		argValues = new ArrayList<Object>(args);
		resultValue = result;
		this.resultValid = resultValid;
	}

	/**
	 * Retrieves the Task to be tested with the values contained in this vector.
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * Retrieves the input arguments to be applied to the Task for this test
	 * vector.
	 * 
	 * @return a List of java.lang wrappers (Boolean, Byte, Character, Short,
	 *         Integer, Long, Float, or Double).
	 */
	public List<Object> getArgValues() {
		return argValues;
	}

	/**
	 * Retrieves the expected result of the method.
	 * 
	 * @return an Object of one of the java.lang wrappers (Boolean, Byte,
	 *         Character, Short, Integer, Long, Float, or Double).
	 */
	public Object getResultValue() {
		return resultValue;
	}

	/**
	 * Returns true if the result for this set of input vectors is valid or
	 * false otherwise. Result may be invalid if method throws an exception (eg
	 * divide by 0 exception)
	 */
	public boolean resultValid() {
		return resultValid;
	}

}// TestVector
