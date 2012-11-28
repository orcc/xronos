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
 * An option which allows a string to be used for the value, picked from a
 * pre-defined set of possible strings.
 * 
 * @see org.xronos.openforge.app.project.Option
 * @author Andreas Kollegger
 */
public class OptionPickOne extends OptionString {

	/** The list of valid option values. */
	String[] possible_values;

	/** If the Option accepts values outside the pre-defined list. */
	boolean editable;

	public OptionPickOne(OptionKey key, String default_value,
			String[] possible_values, boolean hidden, boolean editable) {
		super(key, default_value, hidden);
		this.possible_values = possible_values;
		this.editable = editable;
	} // OptionPickOne()

	public OptionPickOne(OptionKey key, String default_value,
			String[] possible_values, boolean hidden) {
		super(key, default_value, hidden);
		this.possible_values = possible_values;
	} // OptionPickOne()

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
			super.setValue(slabel, val, true);
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
			super.replaceValue(slabel, val, true);
		}
	}

	/**
	 * Gets the value associated with the option.
	 * 
	 * @param slabel
	 *            SearchLabel to find the scope
	 * @return Object - value of the option
	 */
	@Override
	public Object getValue(SearchLabel slabel) {
		return super.getValue(slabel);
	}

	/**
	 * Gets the valid values for this Option.
	 * 
	 * @return a Set of valid String values
	 */
	public String[] getPossibleValues() {
		return possible_values;
	}

	/**
	 * Will the option accept values which are not on the pre-defined list?
	 * 
	 * @param true if the Option accepts arbitrary options
	 */
	public boolean isEditable() {
		return editable;
	}

	@Override
	public boolean isValid(String s) {
		boolean valid = false;

		if (isEditable()) {
			valid = true;
		} else {
			for (int i = 0; i < possible_values.length; i++) {
				if (possible_values[i].equals(s)) {
					valid = true;
					break;
				}
			}
		}
		return valid;
	} // isValid()

	@Override
	public String getTypeName() {
		return "pick";
	}

} /* end class OptionPickOne */
