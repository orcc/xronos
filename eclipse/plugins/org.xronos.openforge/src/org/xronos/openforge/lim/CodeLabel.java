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

package org.xronos.openforge.lim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xronos.openforge.app.project.SearchLabel;
import org.xronos.openforge.util.naming.IDSourceInfo;


/**
 * A label which can be applied to any structural piece of code, common in most
 * languages.
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: CodeLabel.java 188 2006-07-05 20:13:17Z imiller $
 */
public class CodeLabel implements SearchLabel {
	/** The label that is used when the user does not specify a scope */
	public static final CodeLabel UNSCOPED = new CodeLabel() {
		@Override
		public List<String> getSearchList() {
			return Collections.singletonList("UNSCOPED");
		}
	};

	/** The enclosing procedure, for the scoped search. */
	Procedure enclosingProcedure;

	/** The label. */
	String label;

	/**
	 * No-arg Constructor.
	 * 
	 */
	private CodeLabel() {
	}

	/**
	 * Constructor for CodeLabel.
	 * 
	 * @param enclosing
	 *            the Procedure within which the labeled block appears
	 * @param label
	 *            the label itself
	 */
	public CodeLabel(Procedure enclosingProcedure, String label) {
		this.enclosingProcedure = enclosingProcedure;
		this.label = label;
	}

	public CodeLabel(String s) {
		label = s;
	}

	/**
	 * Returns the label.
	 * 
	 * @return String
	 */
	@Override
	public String getLabel() {
		return label;
	}

	/**
	 * Returns a List which produces an increasingly general sequence of label
	 * strings, substituting the provided label for the CodeLabel's own.
	 * 
	 */
	@Override
	public List<String> getSearchList() {
		List<String> sList = getSearchList(label);
		return sList;
	}

	/**
	 * Returns a List which produces an increasingly general sequence of label
	 * strings, substituting the provided label for the CodeLabel's own.
	 * 
	 */
	@Override
	public List<String> getSearchList(String label) {
		List<String> searchList = new ArrayList<String>();
		if (label != null) {
			addVariations(label, searchList);
		}
		addVariations(null, searchList);

		return searchList;
	}

	@Override
	public String toString() {
		if (enclosingProcedure != null) {
			return new String(
					org.xronos.openforge.util.naming.ID
							.showDebug(enclosingProcedure)
							+ " with label "
							+ label);
		} else {
			return "null enclosing procedure with label " + label;
		}
	}

	/**
	 * A List which produces a sequence of increasingly general labels. The
	 * iterator build search strings based on the enclosing procedure and the
	 * label (any component of which may not be present).
	 * <P>
	 * The sequence follows this pattern...
	 * <OL>
	 * <LI>package.class.method(signature)#label
	 * <LI>package.class.method#label
	 * <LI>package.class#label
	 * <LI>package#label
	 * <LI>label
	 * <OL>
	 */
	private void addVariations(String label, List<String> searchList) {
		if (enclosingProcedure != null) {
			IDSourceInfo sourceInfo = enclosingProcedure.getIDSourceInfo();
			String packageName = sourceInfo.getSourcePackageName();
			String className = sourceInfo.getSourceClassName();
			String methodName = sourceInfo.getMethodName();
			String signature = sourceInfo.getSignature();

			if ((packageName != null) && (packageName.equals(""))) {
				packageName = null;
			}
			if ((className != null) && (className.equals(""))) {
				className = null;
			}
			if ((methodName != null) && (methodName.equals(""))) {
				methodName = null;
			}
			if ((signature != null) && (signature.equals(""))) {
				signature = null;
			}

			// package.class.method(signature)#label
			searchList.add(((packageName != null) ? packageName + "." : "")
					+ ((className != null) ? className + "." : "")
					+ ((methodName != null) ? methodName : "") + "("
					+ ((signature != null) ? signature : "") + ")"
					+ ((label != null) ? "#" + label : ""));

			// package.class.method#label
			if (methodName != null) {
				searchList.add(((packageName != null) ? packageName + "." : "")
						+ ((className != null) ? className + "." : "")
						+ methodName + ((label != null) ? "#" + label : ""));
			}
			// package.class#label
			if (className != null) {
				searchList.add(((packageName != null) ? packageName + "." : "")
						+ className + ((label != null) ? "#" + label : ""));
			}
			// package#label
			if (packageName != null) {
				searchList.add(packageName
						+ ((label != null) ? "#" + label : ""));
			}
		}
		// label
		if (label != null) {
			searchList.add(label);
		}
	}

}
