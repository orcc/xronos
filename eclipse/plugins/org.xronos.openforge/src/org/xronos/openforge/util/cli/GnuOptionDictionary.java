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

package org.xronos.openforge.util.cli;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author Andreas Kollegger
 */
public class GnuOptionDictionary {
	/** A map of short keys to GnuOptionDefinitions. */
	Map<Character, GnuOptionDefinition> shortMap;

	/** A map of long keys to GnuOptionDefinitions. */
	Map<String, GnuOptionDefinition> longMap;

	/**
	 * Contructs an empty dictionary. Use @link #add to populate the dictionary
	 * with definitions.
	 */
	public GnuOptionDictionary() {
		shortMap = new HashMap<Character, GnuOptionDefinition>();
		longMap = new HashMap<String, GnuOptionDefinition>();
	}

	/**
	 * Contructs a new parser for a set of GnuOptionDefinitions.
	 * 
	 * @param definitions
	 */
	public GnuOptionDictionary(Set<GnuOptionDefinition> definitions) {
		this();
		for (GnuOptionDefinition definition : definitions) {
			add(definition);
		}
	}

	/**
	 * Adds a definition to the dictionary.
	 * 
	 * @param definition
	 *            the definition to add
	 */
	public void add(GnuOptionDefinition definition) {
		Character shortKey = new Character(definition.getShortKey());
		shortMap.put(shortKey, definition);
		String longKey = definition.getLongKey();
		longMap.put(longKey, definition);
	}

	/**
	 * Retrieves the option definition related to a long key.
	 * 
	 * @param longKey
	 *            the long key
	 * @return the related definition, or null if there is no matching
	 *         definition
	 */
	public GnuOptionDefinition getDefinition(String longKey) {
		return longMap.get(longKey);
	}

	/**
	 * Retrieves the option definition related to a short key.
	 * 
	 * @param shortKey
	 *            the short key
	 * @return the related definition, or null if there is no matching
	 *         definition
	 */
	public GnuOptionDefinition getDefinition(char shortKey) {
		return shortMap.get(new Character(shortKey));
	}

	/**
	 * Retrieves all the definitions.
	 * 
	 * @return the set of GnuOptionDefinitions.
	 */
	public Collection<GnuOptionDefinition> getDefinitions() {
		return shortMap.values();
	}

	/**
	 * Gets the number of entries.
	 */
	public int size() {
		return shortMap.size();
	}

	/**
	 * Checks whether there are no entries.
	 */
	public boolean isEmpty() {
		return shortMap.isEmpty();
	}

	/**
	 * Checks whether the dictionary contains a definition for the given key.
	 * 
	 * @param key
	 *            either the short key (as a Character) or the long key (as a
	 *            String)
	 */
	public boolean containsKey(Object key) {
		return (shortMap.containsKey(key) || longMap.containsKey(key));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		shortMap.clear();
		longMap.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#values()
	 */
	public Collection<GnuOptionDefinition> values() {
		return shortMap.values();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#entrySet()
	 */
	public Set<Entry<Character, GnuOptionDefinition>> entrySet() {
		Set<Entry<Character, GnuOptionDefinition>> entries = new HashSet<Entry<Character, GnuOptionDefinition>>();
		entries.addAll(shortMap.entrySet());
		return entries;
	}

	/**
	 * @param err
	 */
	public void printUsage(PrintStream printer) {
		for (Object element : values()) {
			printer.println("  " + element);
		}
	}

}
