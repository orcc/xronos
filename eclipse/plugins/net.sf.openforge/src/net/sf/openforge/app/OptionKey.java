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
 * Created on Sep 3, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.openforge.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author sb
 * 
 *         This class holds information about the key, command-line argument,
 *         and description for every Option of forge.
 * 
 */
public class OptionKey {

	private final String key;
	private final List<String> claList;
	private final String description;

	/**
	 * Constructs an OptionKey.
	 * 
	 * @param key
	 *            the lookup-key for the OptionKey (its name)
	 * @param cli_switch
	 *            the switch to use (if any) for the command line equivalent of
	 *            this option. May be null or an empty string to indicate that
	 *            no command line should be provided.
	 * @param description
	 *            a possibly multi-sentence summary of this Option's purpose.
	 *            The first sentence (identified by the first occurence of a
	 *            period followed by whitespace) is used for the brief
	 *            description.
	 * 
	 */
	public OptionKey(String key, String CLA, String description) {
		this.claList = new ArrayList<String>();
		this.key = key;
		this.claList.add(CLA);
		this.description = description;
	}

	/**
	 * Constructs an OptionKey.
	 * 
	 * @param key
	 *            the lookup-key for the OptionKey (its name)
	 * @param clas
	 *            the list of switches that can be used for the command line
	 *            equivalent of this option.
	 * @param description
	 *            a possibly multi-sentence summary of this Option's purpose.
	 *            The first sentence (identified by the first occurence of a
	 *            period followed by whitespace) is used for the brief
	 *            description.
	 * 
	 */
	public OptionKey(String key, List<String> clas, String description) {
		this.claList = new ArrayList<String>();
		this.key = key;
		this.claList.addAll(clas);
		this.description = description;
	}

	/**
	 * The CLAlist should be accessed only through this method. It returns a
	 * List of all the possible CLAs for an option. For example: HELP has two
	 * CLA's -h, -help.
	 * 
	 */
	public List<String> getCLAList() {
		return this.claList;
	}

	/**
	 * For options that have just one CLA Switch, this method can be used to get
	 * the CLA Switch directly.
	 * 
	 * @return String the CLA switch (if any) associated with this option.
	 */
	public String getCLASwitch() {
		return this.claList.get(0).toString();
	}

	/**
	 * This method is used to get the Description of the option. This
	 * Description is a possibly multi-sentence summary that describes the
	 * purpose of this option.
	 * 
	 * @return String Description of the option.
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Get the key (name) associated with this OptionKey.
	 * 
	 * @return String Name of the OptionKey.
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Returns a String of the command line args for this key, formatted as:
	 * -key, -key2, -keyN.
	 * 
	 * @return a formatted String, may be 0 length if no command line args exist
	 *         for this key.
	 */
	public String getHelpFormattedKeyList() {
		if (getCLAList().size() == 1) {
			String key = getCLAList().get(0).toString();
			if (key.length() == 0)
				return "";
			else
				return "-" + key;
		}

		String list = "";
		for (Iterator<String> iter = getCLAList().iterator(); iter.hasNext();) {
			String key = iter.next().toString();
			if (key.length() == 0) {
				continue;
			}
			list += "-" + key;
			if (iter.hasNext()) {
				list += ", ";
			}
		}

		return list;
	}
}
