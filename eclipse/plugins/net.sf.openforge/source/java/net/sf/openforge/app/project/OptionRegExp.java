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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.openforge.app.OptionKey;

/**
 * An {@link OptionString} which validates by matching against a regular
 * expression.
 * 
 * @see net.sf.openforge.app.project.Option
 * @author Andreas Kollegger
 */
public class OptionRegExp extends OptionString {

	Pattern pattern;

	/**
	 * Constructs a new OptionRegExp definition of a File based Option.
	 * 
	 * @param key
	 *            the look-up key for the option
	 * @param default_value
	 *            the default value for the option
	 * @param regexp
	 *            the default value for the option is not used as a literal
	 *            value, but is used as a regular expression to validate string
	 *            values
	 * @param hidden
	 *            true if this option should be secret
	 */
	public OptionRegExp(OptionKey key, String default_value, String regexp,
			boolean hidden) {
		super(key, default_value, hidden);
		setPattern(regexp);
	} // OptionRegExp()

	private void setPattern(String p) {
		if ((p != null) && (!p.equals(""))) {
			pattern = Pattern.compile(p);
		} else {
			pattern = Pattern.compile(".*");
		}
	}

	@Override
	public boolean isValid(String s) {
		Matcher m = pattern.matcher(s);
		return m.matches();
	} // isValid()

	@Override
	public String getTypeName() {
		return "regexp";
	}

} /* end class OptionRegExp */
