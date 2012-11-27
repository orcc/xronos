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
 * An Option which accepts integer values.
 * 
 * @author Andreas Kollegger
 */
public class OptionInt extends Option {

	/**
	 * Constructs an Option of OptionInt type.
	 * 
	 * @param key
	 *            the lookup-key for the option (its name)
	 * @param default_value
	 *            the default value related to the key
	 * @param hidden
	 *            whether this option should be hidden from the user
	 */
	public OptionInt(OptionKey key, int default_value, boolean hidden) {
		super(key, Integer.toString(default_value), hidden);
	} // OptionInt()

	/**
	 * The expand method takes in a list of command line arguments and converts
	 * every option to "-opt label-key_pair=<value>" format.
	 * 
	 * @param tokens
	 *            list of command line arguments.
	 */
	@Override
	public void expand(List<String> tokens) {
		String key = getOptionKey().getCLASwitch();
		for (int i = 0; i < tokens.size(); i++) {
			String s = tokens.get(i).toString();
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
			super.setValue(slabel, val);
		}
	}

	/**
	 * This method checks to see if the value passed to the option is of the
	 * right type. If true, it replaces the value with the new value. Otherwise,
	 * it throws and InvalidOptionValue Exception.
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
			super.replaceValue(slabel, val);
		}
	}

	/**
	 * Convenience method to get the value of the option as an int value.
	 * 
	 * @param slabel
	 *            SearchLabel that can be used to get the scope.
	 * @return int value of the option.
	 */
	public int getValueAsInt(SearchLabel slabel) {
		return Integer.parseInt(super.getValue(slabel).toString());
	}

	@Override
	public boolean isValid(String s) {
		try {
			// Integer valid = Integer.valueOf(s);
		} catch (NumberFormatException nfe) {
			return false;
		}

		return true;

	} // isValid()

	@Override
	public String getTypeName() {
		return "int";
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

} /* end class OptionInt */
