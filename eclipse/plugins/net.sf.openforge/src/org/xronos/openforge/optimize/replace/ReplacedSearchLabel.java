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

package org.xronos.openforge.optimize.replace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.app.project.Configurable;
import org.xronos.openforge.app.project.SearchLabel;


/**
 * ReplacedSearchLabel is a SearchLabel which merges preference hierarchy (order
 * of preference string tags) from 2 scopable nodes (Configurable nodes). This
 * is used when a node has been replaced by another node (or a Call) to preserve
 * user settings for both the replaced node and the replacing node.
 * 
 * <p>
 * Created: Mon Mar 31 12:15:26 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ReplacedSearchLabel.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ReplacedSearchLabel implements SearchLabel {

	private SearchLabel replacement;
	private SearchLabel replaced;
	private String label;

	/**
	 * Generates a new ReplacedSearchLabel from the given replacement
	 * searchLabel, and the {@link Configurable} node being replaced.
	 * 
	 * @param replacement
	 *            a value of type 'SearchLabel'
	 * @param replacedConfig
	 *            a value of type 'Configurable'
	 */
	public ReplacedSearchLabel(SearchLabel replacement,
			Configurable replacedConfig) {
		this.replacement = replacement;
		label = replacedConfig.getOptionLabel();
		replaced = findLabel(replacedConfig);
	}

	private static SearchLabel findLabel(Configurable start) {
		SearchLabel sl = null;
		Configurable config = start;
		while (sl == null && config != null) {
			sl = config.getSearchLabel();
			config = config.getConfigurableParent();
		}
		return sl;
	}

	/**
	 * Returns a List which produces an increasingly general sequence of label
	 * strings.
	 * 
	 */
	@Override
	public List<String> getSearchList() {
		List<String> searchOrderList = new ArrayList<String>();
		// Using a linkedHashSet will eliminate redundant values..
		Set<String> searchOrderSet = new LinkedHashSet<String>();
		List<String> iterList;

		iterList = replacement.getSearchList(label);
		for (int idx = 0; idx < iterList.size(); idx++) {
			searchOrderSet.add((iterList.get(idx)));
		}

		iterList = replaced.getSearchList(label);
		for (int idx = 0; idx < iterList.size(); idx++) {
			searchOrderSet.add((iterList.get(idx)));
		}

		// Convert the set to a list and return the list.
		for (Iterator<String> it = searchOrderSet.iterator(); it.hasNext();)
			searchOrderList.add(it.next());

		return searchOrderList;
	}

	/**
	 * Returns a list which produces an increasingly general sequence of label
	 * strings.
	 * 
	 * <pre>
	 * If postfix is not null the ordering is:
	 * replacement procedure#postfix
	 * replacement procedure#replaced component label
	 * replaced component#replaced component label 
	 * 
	 * If postfix is null the ordering is:
	 * replacement procedure#replaced component label
	 * replacement procedure
	 * replaced component#replaced component label
	 * </pre>
	 * 
	 * @see SearchLabel
	 */

	@Override
	public List<String> getSearchList(String postfix) {
		List<String> searchOrderList = new ArrayList<String>();
		List<String> iterList;
		// Using a linkedHashSet will eliminate redundant values..
		Set<String> searchOrderSet = new LinkedHashSet<String>();

		if (postfix == null) {
			// No need to use the postfix, since the searchList
			// will postpend the non-qualified search path if label is
			// not null
			iterList = replacement.getSearchList(label);
			for (int idx = 0; idx < iterList.size(); idx++) {
				searchOrderSet.add((iterList.get(idx)));
			}
		} else {
			iterList = replacement.getSearchList(postfix);
			for (int idx = 0; idx < iterList.size(); idx++) {
				searchOrderSet.add((iterList.get(idx)));
			}
			iterList = replacement.getSearchList(label);
			for (int idx = 0; idx < iterList.size(); idx++) {
				searchOrderSet.add((iterList.get(idx)));
			}
		}

		// The replaced component will have no knowledge of the
		// postfix being sent to us, so we'll ignore it.
		iterList = replaced.getSearchList(label);
		for (int idx = 0; idx < iterList.size(); idx++) {
			searchOrderSet.add((iterList.get(idx)));
		}

		// Convert the set to a list and return the list.
		for (Iterator<String> it = searchOrderSet.iterator(); it.hasNext();)
			searchOrderList.add(it.next());

		return searchOrderList;
	}

	/**
	 * Retrieves the label from the Configurable that this was created from.
	 */
	@Override
	public String getLabel() {
		return label;
	}

	@SuppressWarnings("unused")
	private void debug(Collection<String> col) {
		System.out.println("RETRIEVED RSL: " + this);
		for (Iterator<String> iter = col.iterator(); iter.hasNext();) {
			System.out.println("\t" + iter.next());
		}

	}

}// ReplacedSearchLabel
