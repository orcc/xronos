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

package net.sf.openforge.backend.hdl.ucf;

/**
 * A UCF comment is a single line comment starting with a '#' symbol.
 */
public class UCFComment implements UCFStatement {

	private final String message;

	public UCFComment(String message) {
		this.message = message;
	}

	public UCFComment(UCFStatement statement) {
		this.message = statement.toString();
	}

	@Override
	public String toString() {
		return "# " + message;
	}
}
