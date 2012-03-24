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

package net.sf.openforge.app.project;

import net.sf.openforge.app.OptionKey;
import net.sf.openforge.util.XilinxDevice;

/**
 * OptionXilPart is a lightweight extension to OptionString that does type
 * checking on the String specified by the user to ensure that it represents a
 * valid part, as recognized by the {@link XilinxDevice} class.
 * 
 * <p>
 * Created: Tue Feb 1 11:53:46 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OptionXilPart.java 2 2005-06-09 20:00:48Z imiller $
 */
public class OptionXilPart extends OptionString {

	public OptionXilPart(OptionKey key, String defaultValue, boolean hidden) {
		super(key, defaultValue, hidden);
	}

	/**
	 * Returns false if the string does not parse to a known Xilinx Device, or
	 * if the super class reports that it is an invalid string.
	 * 
	 * @param s
	 *            a value of type 'String'
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean isValid(String s) {
		final XilinxDevice testDevice = new XilinxDevice(s);
		if (!testDevice.isXilinxDevice()) {
			return false;
		}

		return super.isValid(s);
	}

	@Override
	public String getTypeName() {
		return "xilinxPartSelect";
	}

}// OptionXilPart
