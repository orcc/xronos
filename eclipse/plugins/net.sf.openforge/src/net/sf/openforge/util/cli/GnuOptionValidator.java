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
 */
package net.sf.openforge.util.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Andreas Kollegger
 */
public class GnuOptionValidator {
	GnuOptionDictionary dict;

	/** Options which were expected. */
	GnuOptionDictionary goodOptions;

	/** Invalid (unknown or badly formed) options. */
	List<String> badOptions;

	/** Arguments to the command (instead of option commands). */
	List<String> cmdArguments;

	/** Maps definitions to their associated value. */
	Map<GnuOptionDefinition, List<String>> argMap;

	public GnuOptionValidator(GnuOptionDictionary dictionary) {
		dict = dictionary;
		goodOptions = new GnuOptionDictionary();
		badOptions = new ArrayList<String>();
		cmdArguments = new ArrayList<String>();
		argMap = new HashMap<GnuOptionDefinition, List<String>>();
	}

	public GnuOptionDictionary getDictionary() {
		return dict;
	}

	public boolean validate(String[] args) {
		goodOptions.clear();
		badOptions.clear();
		cmdArguments.clear();

		int i = 0;
		for (i = 0; i < args.length; i++) {
			String currentToken = args[i];
			if (currentToken.charAt(0) == '-') {
				// found a switch
				if (currentToken.charAt(1) == '-') {
					// handle a long switch
					int argIndex = currentToken.indexOf("=");
					String longKey = null;
					String arg = null;
					if (argIndex > 0) {
						arg = currentToken.substring(argIndex + 1);
						longKey = currentToken.substring(2, argIndex);
					} else {
						longKey = currentToken.substring(2);
					}
					if (longKey.equals("")) {
						i++;
						break; // no longkey means just "--" was found, end of
								// options
					}

					GnuOptionDefinition def = dict.getDefinition(longKey);
					if (def != null) {
						final int argFlag = def.getArgFlag();
						if ((argFlag & GnuOptionDefinition.NO_ARGUMENT) != 0) {
							if ((arg == null) || arg.equals("")) {
								goodOptions.add(def);
								putArg(def, arg);
							} else {
								badOptions.add(currentToken);
							}
						} else if ((argFlag & GnuOptionDefinition.OPTIONAL_ARGUMENT) != 0) {
							goodOptions.add(def);
							putArg(def, arg);
						} else if ((arg != null)
								&& ((argFlag & GnuOptionDefinition.REQUIRED_ARGUMENT) != 0)) {
							goodOptions.add(def);
							putArg(def, arg);
						} else
							badOptions.add(currentToken);
					} else
						badOptions.add(currentToken);
				} else {
					// handle the short switch(es)
					for (int j = 1; j < currentToken.length(); j++) {
						GnuOptionDefinition def = dict
								.getDefinition(currentToken.charAt(j));
						if (def != null) {
							int argFlag = def.getArgFlag();
							if ((argFlag & (GnuOptionDefinition.OPTIONAL_ARGUMENT | GnuOptionDefinition.REQUIRED_ARGUMENT)) != 0) {
								String arg = null;
								if ((j + 1) < currentToken.length()) {
									arg = currentToken.substring(j + 1);
								} else {
									try {
										i++;
										arg = args[i];
										if (arg.charAt(0) == '-') {
											// next token is another option, not
											// this
											// option's argument
											arg = null;
											i--;
										}
									} catch (ArrayIndexOutOfBoundsException e) {
									}
								}
								if (((argFlag & GnuOptionDefinition.REQUIRED_ARGUMENT) != 0)
										&& (arg == null)) {
									badOptions.add(currentToken);
								} else {
									goodOptions.add(def);
									putArg(def, arg);
									break;
								}
							} else {
								goodOptions.add(def);
							}
						} else
							badOptions.add(currentToken);
					}
				}
			} else {
				cmdArguments.add(currentToken);
			}
		}

		// collect any remaining tokens as command arguments
		for (; i < args.length; i++) {
			cmdArguments.add(args[i]);
		}

		return isValid();
	}

	public boolean isSet(char shortKey) {
		return (goodOptions.containsKey(new Character(shortKey)));
	}

	public boolean isSet(String longKey) {
		return (goodOptions.containsKey(longKey));
	}

	public List<String> getArguments(char shortKey) {
		return get(goodOptions.getDefinition(shortKey));
	}

	/**
	 * @param longKey
	 * @return
	 */
	public List<String> getArguments(String longKey) {
		return get(goodOptions.getDefinition(longKey));
	}

	public String getArgument(char shortKey) {
		List<String> l = get(goodOptions.getDefinition(shortKey));
		if (l == null)
			return null;
		return (l.get(l.size() - 1));
	}

	/**
	 * @param longKey
	 * @return
	 */
	public String getArgument(String longKey) {
		List<String> l = get(goodOptions.getDefinition(longKey));
		if (l == null)
			return null;
		return (l.get(l.size() - 1));
	}

	private List<String> get(GnuOptionDefinition def) {
		List<String> argument = null;
		if (def != null) {
			argument = argMap.get(def);
		}
		return argument;
	}

	/**
	 * Sets the value associated with a definition.
	 */
	private void putArg(GnuOptionDefinition definition, String arg) {
		List<String> argument = get(definition);
		if (argument == null)
			argument = new ArrayList<String>();
		argument.add(arg);
		argMap.put(definition, argument);
	}

	/**
	 * @return
	 */
	public boolean isValid() {
		return badOptions.isEmpty();
	}

	/**
	 * @return
	 */
	public List<String> getBadOptions() {
		return badOptions;
	}

	public Set getGoodOptions() {
		return goodOptions.entrySet();
	}

	/**
	 * Returns the list of command arguments which have been collected for the
	 * most recently validated command line.
	 * 
	 * @return a non-null list of command arguments (which may be empty)
	 */
	public List<String> getCmdArguments() {
		return cmdArguments;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("GnuOptionValidator: {");
		if (isValid())
			buf.append("valid");
		else
			buf.append("invalid");
		buf.append("; ");
		buf.append("good: ");
		for (Iterator it = getGoodOptions().iterator(); it.hasNext();) {
			buf.append(it.next());
			if (it.hasNext())
				buf.append(", ");
		}
		buf.append("; ");
		buf.append("bad: ");
		for (Iterator<String> it = getBadOptions().iterator(); it.hasNext();) {
			buf.append(it.next());
			if (it.hasNext())
				buf.append(", ");
		}
		buf.append("; ");
		buf.append("args: ");
		for (Iterator<String> it = getCmdArguments().iterator(); it.hasNext();) {
			buf.append(it.next());
			if (it.hasNext())
				buf.append(", ");
		}
		buf.append("}");
		return buf.toString();
	}
}
