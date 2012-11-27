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

package org.xronos.openforge.util.naming;

/**
 * IDSourceInfo is used to hold all info relating the the original source from
 * which the object is derived. Any of the fields may be null.
 * 
 * @author C. Schanck
 * @version $Id: IDSourceInfo.java 2 2005-06-09 20:00:48Z imiller $
 */
public class IDSourceInfo implements Cloneable, HasIDSourceInfo {

	private String fname = null;
	private String cname = null;
	private String pname = null;
	private String mname = null;
	private String signature = null;
	private String fieldName = null;
	private int line = -1;
	private int cpos = -1;

	/** optional ending src line for ie: blocks, switches, etc */
	private int endLine = -1;
	/** optional ending column */
	private int endCol = -1;

	/**
	 * Constructor to create an empty SourceInfo object with no knowledge of
	 * source information
	 * 
	 */
	public IDSourceInfo() {
	}

	/**
	 * This creates a partially specified IDSourceInfo with with only high level
	 * source information.
	 * 
	 * @param fname
	 *            filename the source was loaded from
	 * @param pname
	 *            package name from the source
	 * @param cname
	 *            class name the source is related to
	 * @param line
	 *            line number, 0-(N-1). &LT;0 is unknown
	 * @param cpos
	 *            position in the line, 0-(N-1). &LT;0 is unknown
	 */
	public IDSourceInfo(String fname, String pname, String cname, int line,
			int cpos) {
		this(fname, pname, cname, null, null, null, line, cpos);
	}

	/**
	 * This creates a partially specified IDSourceInfo with all recorded info
	 * except the field.
	 * 
	 * @param fname
	 *            the source filename
	 * @param pname
	 *            the source package name
	 * @param cname
	 *            the related source class name
	 * @param mname
	 *            the related source method (procedure, function, whatever) name
	 * @param signature
	 *            the method signature (indicates type of params)
	 * @param line
	 *            line number, 0-(N-1). &LT;0 is unknown
	 * @param cpos
	 *            position in the line, 0-(N-1). &LT;0 is unknown
	 */
	public IDSourceInfo(String fname, String pname, String cname, String mname,
			String signature, int line, int cpos) {
		this(fname, pname, cname, mname, signature, null, line, cpos);
	}

	/**
	 * This creates a fully specified IDSourceInfo with all recorded info. The
	 * method name should include a signature derived from the data types of its
	 * parameters.
	 * 
	 * @param fname
	 *            the source filename
	 * @param pname
	 *            the source package name
	 * @param cname
	 *            the related source class name
	 * @param mname
	 *            the related source method (procedure, function, whatever) name
	 * @param fieldName
	 *            the related field (or other item) name
	 * @param line
	 *            line number, 0-(N-1). &LT;0 is unknown
	 * @param cpos
	 *            position in the line, 0-(N-1). &LT;0 is unknown
	 */
	public IDSourceInfo(String fname, String pname, String cname, String mname,
			String signature, String fieldName, int line, int cpos) {
		this.fname = fname;
		this.cname = cname;
		this.pname = pname;
		this.mname = mname;
		this.signature = signature;
		this.fieldName = fieldName;
		this.line = line;
		this.cpos = cpos;
	}

	/**
	 * Creates SOurce info with bare minimum
	 * 
	 * @param fname
	 *            a value of type 'String'
	 * @param line
	 *            a value of type 'int'
	 * @param cpos
	 *            a value of type 'int'
	 */
	public IDSourceInfo(String fname, int line) {
		this(fname, null, null, null, null, null, line, -1);
	}

	public IDSourceInfo getIDSourceInfo() {
		return this;
	}

	/**
	 * Return the source file name
	 * 
	 * @return source file name
	 */
	public String getSourceFileName() {
		return fname;
	}

	/**
	 * Describe 'getSourcePackageName' method here.
	 * 
	 * @return a value of type 'String'
	 */
	public String getSourcePackageName() {
		return pname;
	}

	/**
	 * Describe 'getSourceClassName' method here.
	 * 
	 * @return a value of type 'String'
	 */
	public String getSourceClassName() {
		return cname;
	}

	/**
	 * Gets the method name (if any) derived from the source.
	 * 
	 * @return the method name, or null
	 */
	public String getMethodName() {
		return mname;
	}

	/**
	 * Gets the method signature (if any) derived from the source.
	 * 
	 * @return the method signature, or null
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * Gets the field name (if any) derived from the source. This is eitherany
	 * variable name: the name of a global field, an instance variable or even
	 * just a parameter.
	 * 
	 * @return the field name, or null
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * Line number, starting with 0
	 * 
	 * @return line number, &LT;0 means unknown
	 */
	public int getSourceLine() {
		return line;
	}

	/**
	 * Horizontal position on the line, 0-(N-1).
	 * 
	 * @return horizontal position; &LT;0 means unknown
	 */
	public int getSourceCharPosition() {
		return cpos;
	}

	/**
	 * Two source infos have the same position if they have the same line
	 * number, and char position
	 * 
	 * @param sinfo
	 *            a value of type 'IDSourceInfo'
	 * @return a value of type 'boolean'
	 */
	public boolean hasSamePos(IDSourceInfo sinfo) {
		return ((sinfo.getSourceCharPosition() == getSourceCharPosition()) && (sinfo
				.getSourceFileName().equals(getSourceFileName())));
	}

	/**
	 * True if this sourceinfo block has a lesser position then the passed in
	 * source position, assuming they are bothed populated
	 * 
	 * @param sinfo
	 *            a value of type 'IDSourceInfo'
	 * @return a value of type 'boolean'
	 */
	public boolean hasLesserPos(IDSourceInfo sinfo) {
		// if it is a lesser line, we are golden
		if (getSourceLine() < sinfo.getSourceLine())
			return true;
		// if the same line, and the charpos is less, true
		if ((getSourceLine() == sinfo.getSourceLine())
				&& (getSourceCharPosition() < sinfo.getSourceCharPosition()))
			return true;
		return false;
	}

	/**
	 * True if this sourceinfo block has a greater position then the passed in
	 * source position, assuming they are bothed populated
	 * 
	 * @param sinfo
	 *            a value of type 'IDSourceInfo'
	 * @return a value of type 'boolean'
	 */
	public boolean hasGreaterPos(IDSourceInfo sinfo) {
		// if it is a lesser line, we are golden
		if (getSourceLine() > sinfo.getSourceLine())
			return true;
		// if the same line, and the charpos is less, true
		if ((getSourceLine() == sinfo.getSourceLine())
				&& (getSourceCharPosition() > sinfo.getSourceCharPosition()))
			return true;
		return false;
	}

	/**
	 * Method setMethodName.
	 * 
	 * @param string
	 */
	public void setMethodName(String mname) {
		this.mname = mname;
	}

	/**
	 * Method setSignature.
	 * 
	 * @param string
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}

	public void setSourceFileName(String fname) {
		this.fname = fname;
	}

	public void setSourcePackageName(String pname) {
		this.pname = pname;
	}

	public void setSourceClassName(String cname) {
		this.cname = cname;
	}

	public void setSourceLine(int line) {
		this.line = line;
	}

	public void setSourceCharPosition(int cpos) {
		this.cpos = cpos;
	}

	public void setSourceEndLine(int line) {
		endLine = line;
	}

	public int getSourceEndLine() {
		return endLine;
	}

	public void setSourceEndCharPosition(int cpos) {
		endCol = cpos;
	}

	public int getSourceEndCharPosition() {
		return endCol;
	}

	public String getFullyQualifiedName() {
		StringBuffer qName = new StringBuffer();

		if (pname != null)
			qName.append(pname);
		if (cname != null) {
			if (qName.length() > 0)
				qName.append(".");
			qName.append(cname);
		}
		if (mname != null) {
			if (qName.length() > 0)
				qName.append(".");
			qName.append(mname);
			qName.append(((signature != null) ? signature : ""));
		}

		if (fieldName != null) {
			if (qName.length() > 0)
				qName.append("#");
			qName.append(fieldName);
		}
		return qName.toString();
	}

	/**
	 * Constructs a new IDSourceInfo derived from this one, but overriding the
	 * method name information with the name provided and clearing the field
	 * name.
	 * 
	 * @param mname
	 *            the derived method name
	 * @param signature
	 *            the method signature
	 * @param the
	 *            line number
	 * @param the
	 *            char position
	 */
	public IDSourceInfo deriveMethod(String mname, String signature, int line,
			int cpos) {
		return new IDSourceInfo(getSourceFileName(), getSourcePackageName(),
				getSourceClassName(), mname, signature, null, line, cpos);
	}

	/**
	 * Constructs a new IDSourceInfo derived from this one, but overriding the
	 * method and field name information.
	 * 
	 * @param fieldName
	 *            the derived field name
	 * @param the
	 *            line number
	 * @param the
	 *            char position
	 */
	public IDSourceInfo deriveField(String fieldName, int line, int cpos) {
		return new IDSourceInfo(getSourceFileName(), getSourcePackageName(),
				getSourceClassName(), getMethodName(), getSignature(),
				fieldName, line, cpos);
	}

	/**
	 * Constructs a new IDSourceInfo derived from this one, but overriding the
	 * line number and character position information.
	 * 
	 * @param the
	 *            line number
	 * @param the
	 *            char position
	 */
	public IDSourceInfo derivePosition(int line, int cpos) {
		return new IDSourceInfo(getSourceFileName(), getSourcePackageName(),
				getSourceClassName(), getMethodName(), getSignature(),
				getFieldName(), line, cpos);
	}

	/**
	 * Constructs a new IDSourceInfo derived from this one, but overriding the
	 * line number and character position information.
	 * 
	 * @param line
	 *            the line number
	 * @param lineEnd
	 *            the ending line number
	 * @param cpos
	 *            the character position
	 * @param cposEnd
	 *            the ending character position
	 */
	public IDSourceInfo derivePosition(int line, int lineEnd, int cpos,
			int cposEnd) {
		IDSourceInfo newInfo = new IDSourceInfo(getSourceFileName(),
				getSourcePackageName(), getSourceClassName(), getMethodName(),
				getSignature(), getFieldName(), line, cpos);
		newInfo.setSourceEndLine(lineEnd);
		newInfo.setSourceEndCharPosition(cposEnd);
		return newInfo;
	}

	public String toString() {
		return "File: " + fname + ", Class: " + pname + "." + cname
				+ ", method: " + mname + "(" + signature + ")" + ", field: "
				+ fieldName + " begin: " + line + ":" + cpos + "->" + endLine
				+ ":" + endCol;
	}

	/** in case the derived object changes any parameters */
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * Sets the field name.
	 * 
	 * @param fieldName
	 *            the name of the field
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * Return an error string in a GCC formatted way...
	 * 
	 * @param text
	 *            text of error
	 * @return formatted error string
	 */
	public String asGCCError(String text) {
		String name = (fname == null) ? "<unknown file>" : fname;
		return name + ":" + line + ": " + text;
	}
}
