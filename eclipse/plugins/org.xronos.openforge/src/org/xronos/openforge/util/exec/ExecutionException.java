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

package org.xronos.openforge.util.exec;

/**
 * Thrown when an external program executed by one of the classes in this
 * package results in an error.
 * 
 * @version $Id: ExecutionException.java 2 2005-06-09 20:00:48Z imiller $
 */
@SuppressWarnings("serial")
public class ExecutionException extends Exception {

	/**
	 * Constructs an ExecutionException with no error message.
	 */
	public ExecutionException() {
		this(null);
	}

	/**
	 * Constructs an ExecutionException with a given error message.
	 */
	public ExecutionException(String msg) {
		super(msg);
	}
}
