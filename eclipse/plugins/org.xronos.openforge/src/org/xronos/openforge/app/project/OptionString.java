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
package org.xronos.openforge.app.project;

import java.util.List;

import org.xronos.openforge.app.NewJob;
import org.xronos.openforge.app.OptionKey;


/**
 * An option which allows an arbitrary string to be used for the value.
 * 
 * @see org.xronos.openforge.app.project.Option
 * @author Andreas Kollegger
 */
public class OptionString extends Option {

	/**
	 * Constructs an Option of OptionString type.
	 * 
	 * @param key
	 *            the lookup-key for the option (its name)
	 * @param default_value
	 *            the default value related to the key
	 * @param hidden
	 *            whether this option should be hidden from the user
	 */
	public OptionString(OptionKey key, String default_value, boolean hidden) {
		super(key, default_value, hidden);
	} // OptionString()

	@Override
	public boolean isValid(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (!Character.isLowerCase(c) && !Character.isUpperCase(c)
					&& !Character.isDigit(c) && (c != ':') && (c != '-')
					&& (c != '+') && (c != ' ') && (c != '_') && (c != '.')
					&& (c != '\\') && (c != '/')) {
				System.out.println("FAILED AT " + c);
				return false;
			}
		}
		return true;
	} // isValid()

	public String validatePever(String s) {
		String newVersionString = null;

		// symbol representations:
		// # is a single interger digit
		// $ is a single lower case letter
		if (s.length() == 7) {
			// if s is fully specified using v#_##_$ notation, take it
			// as the new version string.
			if ((s.charAt(0) == 'v') && Character.isDigit(s.charAt(1))
					&& (s.charAt(2) == '_') && Character.isDigit(s.charAt(3))
					&& Character.isDigit(s.charAt(4)) && (s.charAt(5) == '_')
					&& Character.isLowerCase(s.charAt(6))) {
				newVersionString = s;
			}
		} else if (s.length() == 6) {
			// if s is fully specified using #_##_$ notation, prepend the
			// letter 'v', take it as the new version string.
			if (Character.isDigit(s.charAt(0)) && (s.charAt(1) == '_')
					&& Character.isDigit(s.charAt(2))
					&& Character.isDigit(s.charAt(3)) && (s.charAt(4) == '_')
					&& Character.isLowerCase(s.charAt(5))) {
				newVersionString = "v" + s;
			}

		} else if ((s.length() == 1) && Character.isDigit(s.charAt(0))) {
			// if s is major version only #, create v#_00_a, take it
			// as the new version string.
			newVersionString = "v" + s + "_00_a";
		} else if (s.length() == 4) {
			// if s is major and minor version #_##, create v#_##_a,
			// take it as the new version string.
			if (Character.isDigit(s.charAt(0)) && (s.charAt(1) == '_')
					&& Character.isDigit(s.charAt(2))
					&& Character.isDigit(s.charAt(3))) {
				newVersionString = "v" + s + "_a";
			}
		} else if ((s.length() == 1) && Character.isLowerCase(s.charAt(0))) {
			// if s is hardware letter only $, create v1_00_a, take it
			// as the new vesion string.
			newVersionString = "v1_00_" + s;
		}

		return newVersionString;
	}

	/**
	 * The expand method takes in a list of command line arguments and converts
	 * every option to "-opt label-key_pair=<value>" format.
	 * 
	 * @param tokens
	 *            list of command line arguments.
	 */
	@Override
	public void expand(List<String> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			String s = tokens.get(i).toString();
			String key = getOptionKey().getCLASwitch();
			if (s.charAt(0) == '-') {
				if (key.equals(s.substring(1))) {
					// replace the cla switch with -opt and set its value.
					s = "-opt";
					tokens.add(i, s);
					tokens.remove(i + 1);
					try {
						String value = tokens.get(i + 1).toString();
						s = getOptionKey().getKey() + "=" + value;
						tokens.remove(i + 1);
						tokens.add(i + 1, s);
					} catch (Exception e) {
						throw new NewJob.InvalidNumberOfArgsException();
					}
				}
			}
		}
	}

	/**
	 * This method checks to see if the value passed to the option is of the
	 * right type. If true, it sets the option to that value. Otherwise, it
	 * throws and InvalidOptionValue Exception.
	 * 
	 * @param slabel
	 *            SearchLabel that can be used to get the scope.
	 * @param val
	 *            value to the option.
	 */
	@Override
	public void setValue(SearchLabel slabel, Object val) {
		if (!this.isValid(val.toString())) {
			throw new NewJob.InvalidOptionValueException(getOptionKey()
					.getKey(), val.toString());
		} else {
			// do extra checking for pever
			if ((getOptionKey().getCLASwitch()).equals("pever")) {
				String newValue = validatePever(val.toString());
				if (newValue != null) {
					val = newValue;
				} else {
					throw new NewJob.InvalidOptionValueException(getOptionKey()
							.getKey(), val.toString());
				}
			}
			// set its value
			super.setValue(slabel, val);
		}
	}

	/**
	 * This method checks to see if the value passed to the option is of the
	 * right type. If true, it replaces the option value with that new value.
	 * Otherwise, it throws and InvalidOptionValue Exception.
	 * 
	 * @param slabel
	 *            SearchLabel that can be used to get the scope.
	 * @param val
	 *            value to the option.
	 */
	@Override
	public void replaceValue(SearchLabel slabel, Object val) {
		if (!this.isValid(val.toString())) {
			throw new NewJob.InvalidOptionValueException(getOptionKey()
					.getKey(), val.toString());
		} else {
			// do extra checking for pever
			if ((getOptionKey().getCLASwitch()).equals("pever")) {
				String newValue = validatePever(val.toString());
				if (newValue != null) {
					val = newValue;
				} else {
					throw new NewJob.InvalidOptionValueException(getOptionKey()
							.getKey(), val.toString());
				}
			}
			// replace its value
			super.replaceValue(slabel, val);
		}
	}

	/**
	 * Convenience method to pass on setValue method to super, so that this
	 * class should not validate it.
	 */
	public void setValue(SearchLabel slabel, Object val, boolean valid) {
		super.setValue(slabel, val);
	}

	/**
	 * Convenience method to pass on replaceValue method to super, so that this
	 * class should not validate it.
	 */
	public void replaceValue(SearchLabel slabel, Object val, boolean valid) {
		super.replaceValue(slabel, val);
	}

	@Override
	public String getTypeName() {
		return "string";
	}

	/**
	 * Returns "&lt;value&gt;"
	 * 
	 * @return a value of type 'String'
	 */
	@Override
	public String getHelpValueDescription() {
		return "<value>";
	}

} /* end class OptionString */
