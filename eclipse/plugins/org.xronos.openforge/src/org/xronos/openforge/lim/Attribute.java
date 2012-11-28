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

import org.xronos.openforge.lim.primitive.Reg;

/**
 * Attribute is a class which ties together the correct syntax for Attributes to
 * be applied to components directly instantiated in the output Verilog.
 * Currently {@link Reg} objects are the only ones that take advantage of this,
 * and the attribute is written out in the RegVariant class.
 * 
 * <p>
 * Created: Tue Jan 28 11:57:38 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: Attribute.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Attribute {

	public static final String IOB = "iob";

	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public static final String AUTO = "auto";

	private String name = null;
	private String value = null;

	public Attribute(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getVerilogAttribute(String instanceName) {
		return "synthesis attribute " + name + " of " + instanceName + " is "
				+ value;
	}

	public static class IOB_True extends Attribute {
		public IOB_True() {
			super(IOB, TRUE);
		}
	}

	public static class IOB_False extends Attribute {
		public IOB_False() {
			super(IOB, FALSE);
		}
	}

	public static class IOB_Auto extends Attribute {
		public IOB_Auto() {
			super(IOB, AUTO);
		}
	}

}// Attribute
