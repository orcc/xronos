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
 * @author gandhij, last modified by $Author: imiller $
 * @version $Id: UnexpectedVisitationException.java 23 2005-09-09 18:45:32Z
 *          imiller $
 * 
 *          This expection should be thrown if a a lim object that should not be
 *          visited is visited.
 * 
 */
@SuppressWarnings("serial")
public class UnexpectedVisitationException extends RuntimeException {

	public UnexpectedVisitationException() {
		super();
	}

	public UnexpectedVisitationException(String msg) {
		super(msg);
	}

}
