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
package net.sf.openforge.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inspect Java objects. The class <code>ObjectInspector</code> uses the Java
 * Reflection API to output the internal state (fields) of any object without
 * requiring changes or access rights to that object. Its primary purpose is to
 * be used as a debugging tool to examine an object after performing operations
 * on it in order to verify its correctness, but it can also be used as a
 * testing tool or for reverse engineering.
 * <p>
 * 
 * <code>ObjectInspector</code> uses HTML to format its output. Each object is
 * printed as a table. The first row contains the object's type and ID (see
 * below) and the others contain its contents. The exact format of the contents
 * depends on the object's type and whether or not it is filtered (see below),
 * as follows:
 * 
 * <ul>
 * <li>if the object is an array, <code>ObjectInspector</code> prints the index
 * of each element and its value</li>
 * <li>if the object is not filtered, <code>ObjectInspector</code> prints a list
 * of all fields (name, type and value) declared by the object (except any
 * excluded fields), followed by the type and fields of its superclass, and so
 * on until it reaches <code>java.lang.Object</code> or a filtered class; notice
 * that classes that does not contain any fields to be printed are skipped</li>
 * <li>if the object is filtered and implements the <code>java.util.List</code>
 * interface, it is printed like an array</li>
 * <li>if the object is filtered and implements the
 * <code>java.util.Collection</code> interface, <code>ObjectInspector</code>
 * just prints a list of its elements</li>
 * <li>if the object is filtered and implements the <code>java.util.Map</code>
 * interface, <code>ObjectInspector</code> prints a list of its entries (key and
 * value)</li>
 * </ul>
 * 
 * The format used to print a value (field value, array element, map key, etc)
 * depends on its type and the filters set, as follows:
 * 
 * <ul>
 * <li>if the value is of a primitive type, prints a string representation of it
 * </li>
 * <li>if the value is <code>null</code>, prints the string <code>null</code></li>
 * <li>if the value is an instance of a class that is not filtered, prints its
 * type and ID as a link to the object, which is printed separately, as
 * explained above</li>
 * <li>if the value is an instance of a class that is filtered and implements
 * one of the interfaces <code>java.util.Collection</code>,
 * <code>java.util.List</code> or <code>java.util.Map</code>, prints its type
 * and ID as a link to the object, which is printed separately, as explained
 * above</li>
 * <li>otherwise, prints a string representation of the object, as returned by
 * the method <code>toString()</code></li>
 * </ul>
 * 
 * Every object printed by <code>ObjectInspector</code> is labeled with a unique
 * ID, that is just a numeric value, and is always printed with the object in
 * the format <i>class name</i><code>#</code><i>ID</i>. The ID works as an
 * identification and as a visual representation of the relationships between
 * objects.
 * <p>
 * 
 * <code>ObjectInspector</code> provides filters in order to limit the amount of
 * the data printed, restricting the output to just the relevant classes. A
 * filter is just a pattern that matches class names. The pattern can use the
 * character '<code>*</code>' to match anything, but just as the first or last
 * character. Filters can be of two types: exclusion and inclusion filters.
 * Exclusion filters exclude from the output any class that matches the pattern,
 * while inclusion filters include in the output any class that matches the
 * pattern.
 * <p>
 * 
 * Any number of filters in any combination can be added to an
 * <code>ObjectInspector</code> instance. Filters added last have precedence
 * over the first ones. If no filters are specified, all classes are printed.
 * Additionally, it is also possible to exclude fields from a given class from
 * the output in order to further restrict the data.
 * <p>
 * 
 * Notice that the fully qualified name must always be used when providing class
 * names. When providing names of inner classes, the name generated by the Java
 * Compiler must be used, that is, use the character '<code>$</code>' to
 * separate the name of the inner class from the outer class, e.g., use
 * <code>java.util.Map$Entry</code> instead of <code>java.util.Map.Entry</code>.
 * <p>
 * 
 * In order to use <code>ObjectInspector</code>, you must call one of the
 * methods {@link #startOutput(Writer)} or {@link #startOutput(OutputStream)} to
 * initialize the output. Once the output is initialized, the method
 * {@link #inspect(Object)} can be called as many times as you want to add the
 * objects to be inspected. After adding at least one object, the method
 * {@link #output()} outputs the objects added as well as any objects they
 * reference, according the rules stated above. These two later steps can be
 * repeated as many times as necessary. Finally, call the method
 * {@link #endOutput()} to finalize the output and release resources.
 */
public class ObjectInspector {
	private List<Filter> filters; // list of filters
	private Map<String, Set<String>> excludedFields; // list of fields to
														// exclude

	private Map<ObjectReference, Integer> IDMap; // map object references to
													// their IDs
	private List<Object> objList; // list of objects to print
	private int nextID; // next ID to use
	private PrintWriter out; // output character stream
	private boolean printRule; // controls outputs delimiters
	private Map<String, List<Field>> fieldsCache; // cache for declared fields

	// ---------------------------- inner classes ------------------------------
	/**
	 * The class <code>Filter</code> represents an inclusion/exclusion filter.
	 */
	private class Filter {
		private String pattern; // pattern to match
		private boolean prefix; // if true, matches any class that starts with
								// pattern
		private boolean suffix; // if true, matches any class that ends with
								// pattern
		private boolean inclusion; // true = inclusion filter; false = exclusion

		/**
		 * Constructs a new <code>Filter</code> with a given pattern.
		 * 
		 * @param pattern
		 *            pattern to match
		 * @param inclusion
		 *            <code>true</code> to create an inclusion filter,
		 *            <code>false</code> to create an exclusion filter
		 */
		public Filter(String pattern, boolean inclusion) {
			this.inclusion = inclusion;
			if (pattern.startsWith("*")) {
				this.pattern = pattern.substring(1);
				suffix = true;
			} else if (pattern.endsWith("*")) {
				this.pattern = pattern.substring(0, pattern.length() - 1);
				prefix = true;
			} else
				this.pattern = pattern;
		}

		/**
		 * Returns whether this object is an inclusion or exclusion filter.
		 * 
		 * @return <code>true</code> if this object represents an inclusion
		 *         filter, <code>false</code> if exclusion
		 */
		public boolean isInclusion() {
			return inclusion;
		}

		/**
		 * Tests whether the pattern represented by this filter matches a given
		 * class.
		 * 
		 * @param clazz
		 *            class to test
		 * 
		 * @return <code>true</code> if the pattern matches the class,
		 *         <code>false</code> otherwise
		 */
		public boolean match(Class<?> clazz) {
			String className = clazz.getName();
			if (prefix)
				return className.startsWith(pattern);
			else if (suffix)
				return className.endsWith(pattern);
			else
				return className.equals(pattern);
		}
	}

	/**
	 * The class <code>ObjectReference</code> stores a reference to an object.
	 */
	private class ObjectReference {
		private Object obj; // referenced object

		/**
		 * Constructs a new <code>ObjectReference</code> to reference a given
		 * object.
		 * 
		 * @param obj
		 *            object to reference
		 */
		public ObjectReference(Object obj) {
			this.obj = obj;
		}

		/**
		 * Compares this object with another. Returns <code>true</code> if the
		 * other object is an <code>ObjectReferemce</code> and refers to the
		 * same object as this one.
		 * 
		 * @param obj
		 *            object to compare to
		 * 
		 * @return <code>true</code> if both refer to the same object,
		 *         <code>false</code> otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ObjectReference)
				return (this.obj == ((ObjectReference) obj).obj);
			return false;
		}

		/**
		 * Returns a hash code value for this object.
		 * 
		 * @return a hash code for this object
		 */
		@Override
		public int hashCode() {
			return obj.hashCode();
		}
	}

	// --------------------------- internal methods ----------------------------
	/**
	 * Closes the object table. Outputs the table's close tag.
	 */
	private void closeTable() {
		out.println("</TABLE>");
	}

	/**
	 * Adds an object to the list of objects to output. If the object has an ID,
	 * it was already found before, so the method does nothing. Otherwise it
	 * assigns an ID to it and adds the object to the list, but only if it
	 * should be inspected as determined by the method
	 * {@link #shouldInspect(Object)}.
	 * 
	 * @param obj
	 *            object to add
	 * 
	 * @return the ID of the object or <code>null</code> if the object should
	 *         not be inspected
	 * 
	 * @see #shouldInspect(Object)
	 */
	private Integer enqueue(Object obj) {
		ObjectReference objRef = new ObjectReference(obj);
		Integer ID = IDMap.get(objRef);
		if ((ID == null) && (shouldInspect(obj))) {
			ID = new Integer(nextID++);
			IDMap.put(objRef, ID);
			objList.add(obj);
		}
		return ID;
	}

	/**
	 * Gets the declared fields of a given class, excluding any fields excluded
	 * from the output. Notice that the method already sets the accessible flag
	 * of the fields. The method uses a cache to improve performance, so that it
	 * just needs to filter the fields the first time the class is found.
	 * 
	 * @param clazz
	 *            class to get the fields
	 * 
	 * @return the declared fields of the class
	 */
	private List<Field> getDeclaredFields(Class<?> clazz) {
		String className = clazz.getName();
		List<Field> filteredFields = fieldsCache.get(className);
		if (filteredFields != null)
			return filteredFields;
		Field[] fields = clazz.getDeclaredFields();
		Field.setAccessible(fields, true);
		Set<String> fieldsSet = excludedFields.get(className);
		if (fieldsSet == null)
			filteredFields = Arrays.asList(fields);
		else {
			filteredFields = new ArrayList<Field>();
			for (int i = 0; i < fields.length; i++)
				if (!fieldsSet.contains(fields[i].getName()))
					filteredFields.add(fields[i]);
		}
		fieldsCache.put(className, filteredFields);
		return filteredFields;
	}

	/**
	 * Gets the name of a given class in a user friendly format. If the class is
	 * not an array, returns just the class name, otherwise returns the name of
	 * its component type followed by a number of square bracket pairs
	 * reflecting its depth.
	 * 
	 * @param clazz
	 *            class to get the name
	 * 
	 * @return the user friendly name of the class
	 */
	public String getFriendlyName(Class<?> clazz) {
		StringBuffer buffer = new StringBuffer();
		while (clazz.isArray()) {
			buffer.insert(0, "[]");
			clazz = clazz.getComponentType();
		}
		buffer.insert(0, clazz.getName());
		return buffer.toString();
	}

	/**
	 * Gets a given field from a given object.
	 * 
	 * @param obj
	 *            object from which to get the field
	 * @param field
	 *            field to get
	 * 
	 * @return the value of the field
	 */
	private Object getFieldValue(Object obj, Field field) {
		Object value = null;
		try {
			value = field.get(obj);
		} catch (IllegalAccessException e) {
		}
		return value;
	}

	/**
	 * Gets the ID of a given object.
	 * 
	 * @param obj
	 *            object from which to get the ID
	 * 
	 * @return the ID of the object or <code>null</code> if it does not have an
	 *         ID
	 */
	private Integer getObjectID(Object obj) {
		return new Integer(obj.hashCode());
		// return (Integer)IDMap.get(new ObjectReference(obj) );
	}

	/**
	 * Encodes a piece of text according HTML conventions. Specifically, encodes
	 * the characters <code>&lt;</code>, <code>&gt;</code>, <code>'</code> and
	 * <code>"</code> as a numeric character reference.
	 * 
	 * @param text
	 *            text to encode
	 * 
	 * @return the text encoded according HTML conventions
	 */
	private String HTMLEncode(String text) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if ((c == '<') || (c == '>') || (c == '\'') || (c == '"'))
				buffer.append("&#" + (int) c + ";");
			else
				buffer.append(c);
		}
		return buffer.toString();
	}

	/**
	 * Tests whether a given class is <i>filtered</i>. Compares the class with
	 * each filter in turn, from the last to the first, until a match is found.
	 * If the class matched an inclusion filter, returns <code>false</code>,
	 * otherwise returns <code>true</code>. If the class does not match any
	 * filter, returns <code>false</code> if the first filter is an exclusion
	 * filter, otherwise returns <code>true</code>. This logic means that
	 * filters must be added from the most permissive to the most restrictive.
	 * 
	 * @param clazz
	 *            class to test
	 * 
	 * @return <code>true</code> if the class is filtered, <code>false</code>
	 *         otherwise
	 */
	private boolean isFiltered(Class<?> clazz) {
		if (filters.isEmpty())
			return false;
		Filter filter = null;
		for (int i = filters.size() - 1; i >= 0; i--) {
			filter = filters.get(i);
			if (filter.match(clazz))
				return !filter.isInclusion();
		}
		return filter.isInclusion();
	}

	/**
	 * Opens a table to output a given object. Outputs a named anchor, the table
	 * open tag and a table row with the class name of the object.
	 * 
	 * @param obj
	 *            object
	 * @param cols
	 *            number of columns of the table
	 * 
	 * @throws IOException
	 *             if an error occurs while outputing the table header
	 */
	private void openTable(Object obj, int cols) throws IOException {
		String tag = getTagFor(obj);
		String target = getTargetFor(obj);
		// Integer ID = getObjectID(obj);
		// out.println(
		// "<A NAME=\""+Integer.toHexString(ID.intValue())+"\"></A>" );
		out.println("<A NAME=\"" + target + "\"></A>");
		out.println("<TABLE BORDER=\"1\" CELLPADDING=\"0\" CELLSPACING=\"0\" WIDTH=\"100%\">");
		out.println("<TR BGCOLOR=\"#C0C0C0\">");
		// out.println(
		// "<TD COLSPAN=\""+cols+"\"><FONT SIZE=\"+2\"><B>"+getFriendlyName(obj.getClass())+"#"+Integer.toHexString(ID.intValue())+"</B></FONT></TD>"
		// );
		out.println("<TD COLSPAN=\"" + cols + "\"><FONT SIZE=\"+2\"><B>" + tag
				+ "</B></FONT></TD>");
		out.println("</TR>");
	}

	private String getTagFor(Object obj) {
		Integer ID = getObjectID(obj);
		String name = getFriendlyName(obj.getClass());
		return name + "#" + Integer.toHexString(ID.intValue());
	}

	private String getTargetFor(Object obj) {
		Integer ID = getObjectID(obj);
		String name = getFriendlyName(obj.getClass());
		String target = name + "_" + Integer.toHexString(ID.intValue());
		// target = target.replace('.','_');
		return target;
	}

	/**
	 * Outputs a given array. Prints each element of the array, as well as its
	 * index.
	 * 
	 * @param obj
	 *            array to output
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the array
	 */
	private void outputArray(Object obj) throws IOException {
		openTable(obj, 2);
		boolean primitive = obj.getClass().getComponentType().isPrimitive();
		for (int i = 0; i < Array.getLength(obj); i++) {
			out.println("<TR>");
			out.println("<TD>[" + i + "]</TD>");
			Object value = Array.get(obj, i);
			outputValue(value, primitive);
			out.println("</TR>");
		}
		closeTable();
	}

	/**
	 * Outputs a given object that implements the
	 * <code>java.util.Collection</code> interface. The method just prints each
	 * element of the collection.
	 * 
	 * @param obj
	 *            collection to output
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the collection
	 */
	private void outputCollection(Collection<Object> obj) throws IOException {
		openTable(obj, 1);
		Iterator<Object> iterator = obj.iterator();
		while (iterator.hasNext()) {
			out.println("<TR>");
			outputValue(iterator.next(), false);
			out.println("</TR>");
		}
		closeTable();
	}

	/**
	 * Outputs a given object that implements the <code>java.util.List</code>
	 * interface. Prints each element of the list, as well as its index.
	 * 
	 * @param obj
	 *            list to output
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the list
	 */
	private void outputList(List<Object> obj) throws IOException {
		openTable(obj, 2);
		for (int i = 0; i < obj.size(); i++) {
			out.println("<TR>");
			out.println("<TD>[" + i + "]</TD>");
			outputValue(obj.get(i), false);
			out.println("</TR>");
		}
		closeTable();
	}

	/**
	 * Outputs a given object that implements the <code>java.util.Map</code>
	 * interface. The method just prints each key/value pair of the map.
	 * 
	 * @param obj
	 *            map to output
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the map
	 */
	private void outputMap(Map obj) throws IOException {
		openTable(obj, 2);
		Iterator iterator = obj.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = (Map.Entry) iterator.next();
			out.println("<TR>");
			outputValue(entry.getKey(), false);
			outputValue(entry.getValue(), false);
			out.println("</TR>");
		}
		closeTable();
	}

	/**
	 * Outputs a given object. Prints all fields, including package, protected
	 * and private fields. First outputs the fields declared by the object's
	 * class and then the fields of each superclass, in turn, until it reaches
	 * the class <code>Object</code> or a filtered class.
	 * 
	 * @param obj
	 *            object to output
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the object
	 */
	private void outputObject(Object obj) throws IOException {
		openTable(obj, 3);
		boolean printHeader = false;
		Class<?> clazz = obj.getClass();
		while (true) {
			List<Field> fields = getDeclaredFields(clazz);
			if ((fields.size() > 0) && (printHeader)) {
				out.println("<TR BGCOLOR=\"#C0C0C0\">");
				out.println("<TD COLSPAN=\"3\"><FONT SIZE=\"+1\"><B>fields inherited from "
						+ clazz.getName() + "</B></FONT></TD>");
				out.println("</TR>");
			}
			printHeader = true;
			for (int i = 0; i < fields.size(); i++) {
				Field field = fields.get(i);
				Class<?> type = field.getType();
				Object value = getFieldValue(obj, field);
				out.println("<TR>");
				out.println("<TD>" + field.getName() + "</TD>");
				out.println("<TD>" + getFriendlyName(type) + "</TD>");
				outputValue(value, type.isPrimitive());
				out.println("</TR>");
			}
			clazz = clazz.getSuperclass();
			if ((clazz == null) || (isFiltered(clazz)))
				break;
		}
		closeTable();
	}

	/**
	 * Outputs a table cell with the value of a given object.
	 * 
	 * @param obj
	 *            object to output
	 * @param primitive
	 *            tells to the method whether the object is a wrapper for a
	 *            primitive
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the value
	 */
	private void outputValue(Object value, boolean primitive)
			throws IOException {
		out.print("<TD>");
		if ((value == null) || (primitive) || (enqueue(value) == null))
			out.print(HTMLEncode(String.valueOf(value)));
		else {
			// Integer ID = getObjectID(value);
			// out.print(
			// "<A HREF=\"#"+Integer.toHexString(ID.intValue())+"\">"+getFriendlyName(value.getClass())+"#"+Integer.toHexString(ID.intValue())+"</A>"
			// );
			String tag = getTagFor(value);
			String target = getTargetFor(value);
			out.print("<A HREF=\"#" + target + "\">" + tag + "</A>");
		}
		out.println("</TD>");
	}

	/**
	 * Tests whether a given object should be inspected according its type and
	 * the filters. Specifically, the object should be inspected if it is not
	 * filtered, if it is an array or if it implements the interface
	 * <code>java.util.Collection</code> or <code>java.util.Map</code>.
	 * 
	 * @param obj
	 *            object to test
	 * 
	 * @return <code>true</code> if the object should be inspected,
	 *         <code>false</code> otherwise
	 */
	private boolean shouldInspect(Object obj) {
		Class<?> clazz = obj.getClass();
		if ((clazz.isArray()) || (!isFiltered(clazz))
				|| (obj instanceof Collection) || (obj instanceof Map))
			return true;
		return false;
	}

	// ------------------------------ class API --------------------------------
	/**
	 * Constructs a new <code>ObjectInspector</code>. The constructor also
	 * automatically adds the exclusion filters <code>java.*</code> and
	 * <code>javax.*</code>.
	 */
	public ObjectInspector() {
		filters = new ArrayList<Filter>();
		excludedFields = new HashMap<String, Set<String>>();
		IDMap = new HashMap<ObjectReference, Integer>();
		objList = new LinkedList<Object>();
		fieldsCache = new HashMap<String, List<Field>>();
		addExclusionFilter("java.*");
		addExclusionFilter("javax.*");
	}

	/**
	 * Adds a filter to exclude classes that matches a pattern from the output.
	 * 
	 * @param pattern
	 *            pattern to match
	 */
	public void addExclusionFilter(String pattern) {
		filters.add(new Filter(pattern, false));
	}

	/**
	 * Adds a filter to include just the classes that matches a pattern in the
	 * output.
	 * 
	 * @param pattern
	 *            pattern to match
	 */
	public void addInclusionFilter(String pattern) {
		filters.add(new Filter(pattern, true));
	}

	/**
	 * Clear the list of excluded fields.
	 */
	public void clearExcludedFields() {
		excludedFields.clear();
	}

	/**
	 * Clear all filters added.
	 */
	public void clearFilters() {
		filters.clear();
	}

	/**
	 * Writes the final part of the HTML output and releases any resources held.
	 * 
	 * @throws IOException
	 *             if an exception occurs while writing to the stream
	 */
	public void endOutput() throws IOException {
		out.println("</BODY>");
		out.println("</HTML>");
		out.flush();
		out = null;
	}

	/**
	 * Adds a given field to the list of fields to exclude from the output.
	 * 
	 * @param field
	 *            field to exclude
	 */
	public void excludeField(String field) {
		int index = field.lastIndexOf('.');
		String clazz = field.substring(0, index);
		field = field.substring(index + 1);
		Set<String> fields = excludedFields.get(clazz);
		if (fields == null)
			fields = new HashSet<String>();
		fields.add(field);
		excludedFields.put(clazz, fields);
	}

	/**
	 * Adds a given object to this <code>ObjectInspector</code> to be inspected.
	 * 
	 * @param obj
	 *            object to inspect
	 */
	public void inspect(Object obj) {
		enqueue(obj);
	}

	/**
	 * Outputs all objects added to this <code>ObjectInspector</code>.
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing an object
	 */
	public void output() throws IOException {
		if (printRule)
			out.println("<HR><BR>");
		printRule = true;
		while (!objList.isEmpty()) {
			Object obj = objList.remove(0);
			Class<?> clazz = obj.getClass();
			if (clazz.isArray())
				outputArray(obj);
			else if (!isFiltered(clazz))
				outputObject(obj);
			else if (obj instanceof List)
				outputList((List) obj);
			else if (obj instanceof Collection)
				outputCollection((Collection) obj);
			else if (obj instanceof Map)
				outputMap((Map) obj);
			out.println("<BR>");
		}
		IDMap.clear();
		fieldsCache.clear();
	}

	/**
	 * Outputs a given object. Same as calling {@link #inspect(Object)} and
	 * {@link #output()}.
	 * 
	 * @param obj
	 *            object to output
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the object
	 */
	public void output(Object obj) throws IOException {
		inspect(obj);
		output();
	}

	/**
	 * Outputs a given object to a given character stream. Same as calling
	 * {@link #startOutput(Writer)}, {@link #output(Object)} and
	 * {@link #endOutput()}.
	 * 
	 * @param obj
	 *            object to output
	 * @param writer
	 *            character stream
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the object
	 */
	public void output(Object obj, Writer writer) throws IOException {
		startOutput(writer);
		output(obj);
		endOutput();
	}

	/**
	 * Outputs a given object to a given byte stream. Same as calling
	 * {@link #startOutput(OutputStream)}, {@link #output(Object)} and
	 * {@link #endOutput()}.
	 * 
	 * @param obj
	 *            object to output
	 * @param stream
	 *            byte stream
	 * 
	 * @throws IOException
	 *             if an exception occurs while outputing the object
	 */
	public void output(Object obj, OutputStream stream) throws IOException {
		startOutput(stream);
		output(obj);
		endOutput();
	}

	/**
	 * Starts the output to a given character stream. Initializes the internal
	 * state of this <code>ObjectInspector</code> and outputs the initial part
	 * of the HTML output to the stream.
	 * 
	 * @param writer
	 *            character stream
	 * 
	 * @throws IOException
	 *             if an exception occurs while writing to the stream
	 */
	public void startOutput(Writer writer) throws IOException {
		nextID = 0;
		printRule = false;

		out = new PrintWriter(new BufferedWriter(writer));
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
		out.println("<HTML>");
		out.println("<BODY>");
		SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		out.println("<b>Generated on: " + df.format(new Date())
				+ "</b><br><br>");
	}

	/**
	 * Starts the output to a given byte stream. Wraps the byte stream in a
	 * character stream, using the default character encoding and calls
	 * {@link #startOutput(Writer)}.
	 * 
	 * @param stream
	 *            byte stream
	 * 
	 * @throws IOException
	 *             if an exception occurs while writing to the stream
	 */
	public void startOutput(OutputStream stream) throws IOException {
		startOutput(new OutputStreamWriter(stream));
	}
}
