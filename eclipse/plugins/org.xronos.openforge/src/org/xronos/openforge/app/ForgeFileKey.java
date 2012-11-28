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

package org.xronos.openforge.app;

/**
 * The ForgeFileKey is a lightweight class that is used to refer to a particular
 * handled file in the {@link ForgeFileHandler}
 * 
 * @author imiller Created on 03.17.2006
 */

public class ForgeFileKey {
	private String reason = "unknown";

	public ForgeFileKey(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return super.toString() + "<" + this.reason + ">";
	}

}
