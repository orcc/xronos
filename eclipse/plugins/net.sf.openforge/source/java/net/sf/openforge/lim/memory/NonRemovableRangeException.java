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

package net.sf.openforge.lim.memory;


/**
 * NonRemovableRangeException is an exception thrown when attempting to remove a
 * range from a context in which that range cannot be removed.
 * <p>
 * Example: Removing a range of bytes from a pointer.
 * 
 * <p>
 * Created: Fri Aug 29 12:00:39 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: NonRemovableRangeException.java 2 2005-06-09 20:00:48Z imiller
 *          $
 */
@SuppressWarnings("serial")
public class NonRemovableRangeException extends Exception {

	public NonRemovableRangeException(String msg) {
		super(msg);
	}

}// NonRemovableRangeException
