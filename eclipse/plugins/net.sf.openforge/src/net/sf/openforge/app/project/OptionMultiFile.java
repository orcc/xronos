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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionKey;
import net.sf.openforge.util.IndentWriter;

/**
 * Allows for multiple files to be selected.
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: OptionMultiFile.java 122 2006-03-30 18:05:17Z imiller $
 */
public class OptionMultiFile extends OptionFile {
	/**
	 * The characters that are used to divide a list into elements when
	 * represented as a string
	 */
	private final String separators = "[]," + java.io.File.pathSeparator;

	/**
	 * Constructs a new OptionMultiFile definition of a File based Option.
	 * 
	 * @param key
	 *            the look-up key for the option
	 * @param default_value
	 *            the default value for the option
	 * @param regexp
	 *            the regular expression used to filter file names
	 * @param hidden
	 *            true if this option should be secret
	 */
	public OptionMultiFile(OptionKey key, String default_value, String regexp,
			boolean hidden) {
		super(key, default_value, regexp, hidden);
	}

	/**
	 * @see net.sf.openforge.app.project.Option#getTypeName()
	 */
	@Override
	public String getTypeName() {
		return "multi-file";
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
		// boolean consumed_token = false;
		// int number_consumed = 0;
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
		if (!isValid(val.toString())) {
			throw new NewJob.InvalidOptionValueException(getOptionKey()
					.getKey(), val.toString());
		} else {
			if (isFirstTime) {
				super.replaceValue(slabel, val, true);
			} else {
				// if the value is already present, then append the new value;
				List<String> oldList = getValueAsList(slabel);
				oldList.add(val.toString().trim());
				String s = OptionMultiFile.toString(oldList);
				super.setValue(slabel, s, true);
			}
		}
	}

	/**
	 * This method replaces the current value of the option with the value
	 * passed to it. Note that setValue in OptionList behaves differently, as it
	 * appends the new value to the existing value. Hence, OptionList and
	 * OptionMultiFile need replaceValue methods to overcome this.
	 * 
	 * @param slabel
	 *            the searchLabel to search for scope
	 * @param value
	 *            - value of the option
	 */
	@Override
	public void replaceValue(SearchLabel slabel, Object value) {
		if (!isValid(value.toString())) {
			throw new NewJob.InvalidOptionValueException(getOptionKey()
					.getKey(), value.toString());
		} else {
			super.replaceValue(slabel, value, true);
		}
	}

	/**
	 * Convenience method to get the value of the option as a List.
	 * 
	 * @param slabel
	 *            SearchLabel that can be used to get the scope.
	 * @return List value of the option as a List.
	 */
	public List<String> getValueAsList(SearchLabel slabel) {
		return toList(super.getValue(slabel).toString());
	}

	/**
	 * @see net.sf.openforge.app.project.Option#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(String s) {
		List<String> files = toList(s);
		for (String string : files) {
			if (!super.isValid(string))
				return false;
		}
		return true;
	}

	public List<String> toList(String s) {
		StringTokenizer st = new StringTokenizer(s, separators);
		List<String> reply = new ArrayList<String>();

		while (st.hasMoreTokens()) {
			reply.add(st.nextToken().trim());
		}
		return reply;
	}

	public String[] toArray(String s) {
		StringTokenizer st = new StringTokenizer(s, separators);
		String[] reply = new String[st.countTokens()];

		int i = 0;
		while (st.hasMoreTokens()) {
			reply[i++] = st.nextToken().trim();
		}

		return reply;
	}

	public static String toString(List<String> l) {
		return l.toString();
	} // toString()

	/**
	 * @see net.sf.openforge.app.project.Option#printXML(net.sf.openforge.util.IndentWriter,
	 *      java.lang.String)
	 */
	@Override
	public void printXML(IndentWriter printer, String value) {
		List<String> files = toList(value);

		printer.println("<option name=\"" + getOptionKey().getKey() + "\" "
				+ "type=\"" + getTypeName() + "\">");
		printer.inc();

		for (String file : files) {
			printer.println("<entry>" + file + "</entry>");
		}
		printer.dec();
		printer.println("</option>");
	}

}
