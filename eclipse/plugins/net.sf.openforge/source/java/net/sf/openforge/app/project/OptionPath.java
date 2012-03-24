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

import java.util.List;

import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionKey;

/**
 * An option which keeps a list of directories.
 * 
 * @see net.sf.openforge.app.project.Option
 * @author Andreas Kollegger
 */
public class OptionPath extends OptionList {

	/**
	 * Constructs an Option of OptionPath type.
	 * 
	 * @param key
	 *            the lookup-key for the option (its name)
	 * @param default_value
	 *            the default value related to the key
	 * @param hidden
	 *            whether this option should be hidden from the user
	 */
	public OptionPath(OptionKey key, String default_value, boolean hidden) {
		super(key, default_value, hidden);
	} // OptionPath()

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
	 * This method sets the value to the option.
	 * 
	 * @param slabel
	 *            SearchLabel that can be used to get the scope.
	 * @param val
	 *            value to the option.
	 */
	@Override
	public void setValue(SearchLabel slabel, Object val) {
		super.setValue(slabel, val);
	}

	/**
	 * This method replaces the value of the option.
	 * 
	 * @param slabel
	 *            SearchLabel that can be used to get the scope.
	 * @param val
	 *            value to the option.
	 */
	@Override
	public void replaceValue(SearchLabel slabel, Object val) {
		super.replaceValue(slabel, val);
	}

	/**
	 * Gets the value associated with the option.
	 * 
	 * @param slabel
	 *            SearchLabel to search for scope
	 * @return Object - value of the option
	 */
	@Override
	public Object getValue(SearchLabel slabel) {
		return OptionList.toString(super.getValueAsList(slabel));
	}

	@Override
	public String getTypeName() {
		return "path";
	}

} /* end class OptionPath */
