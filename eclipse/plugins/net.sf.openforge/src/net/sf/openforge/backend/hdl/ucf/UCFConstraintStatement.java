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

public class UCFConstraintStatement implements UCFStatement {

	private final UCFType type;
	private final String name;
	private final UCFConstraint constraint;

	public UCFConstraintStatement(UCFType type, String name,
			UCFConstraint constraint) {
		this.type = type;
		this.name = name;
		this.constraint = constraint;
	}

	public UCFType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public UCFConstraint getConstraint() {
		return constraint;
	}

	@Override
	public String toString() {
		return getType().toString() + " " + getName() + " "
				+ getConstraint().toString() + ";";
	}
}
