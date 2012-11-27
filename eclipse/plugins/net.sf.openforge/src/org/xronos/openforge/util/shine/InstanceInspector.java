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

package org.xronos.openforge.util.shine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Goal here is to return a DB of all data elements
 */
public class InstanceInspector implements Comparator<Object> {
	private ArrayList<Field> _flist = new ArrayList<Field>(10);
	private Field[] myFields;
	// private String modString[];
	private Object myObject;

	// abstract, final, interface, native, private, protected,
	// public, static, synchro, transient, volatile
	private final static String _possibleOrders = "AFINPRUSYTV";
	private String sortOrder = "TVNFSPRU";

	public InstanceInspector() {
	}

	public InstanceInspector(Object o) {
		reuse(o);
	}

	public void clear() {
		myFields = null;
		myObject = null;
	}

	public void reuse(Object o) {
		clear();
		myObject = o;
		createFieldList(o.getClass());
		setSortOrder(sortOrder);
	}

	public Field[] getFieldObjects() {
		return myFields;
	}

	public Object getMyObject() {
		return myObject;
	}

	public boolean isFieldRef(int fIndex) {
		return !myFields[fIndex].getType().isPrimitive();
	}

	public boolean isFieldArray(int fIndex) {
		return getFieldType(fIndex).isArray();
	}

	public Field getField(int fIndex) {
		return myFields[fIndex];
	}

	public int getFieldCount() {
		return myFields.length;
	}

	public Class<?> getFieldType(int fIndex) {
		return myFields[fIndex].getType();
	}

	public Object getFieldName(int fIndex) {
		return myFields[fIndex].getName();
	}

	public Object getFieldValue(int fIndex) {
		try {
			Field f = getField(fIndex);
			f.setAccessible(true);
			return f.get(myObject);
		} catch (IllegalAccessException iae) {
			return "<Inaccessible>";
		}
	}

	public String getModifierString(int fIndex) {
		String pat = "";
		for (int i = 0; i < _possibleOrders.length(); i++) {
			char c = _possibleOrders.charAt(i);
			int bit = getModifierBit(c);
			if ((myFields[fIndex].getModifiers() & bit) == bit)
				pat = pat + c;
			else
				pat = pat + " ";
		}
		return pat;
	}

	@Override
	public String toString() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(bos);

		pw.println("InstanceInspector" + myObject.getClass().getName());

		for (int i = 0; i < getFieldObjects().length; i++) {
			pw.println("\t" + getModifierString(i) + getField(i) + " == "
					+ getFieldValue(i));
		}

		pw.flush();
		return bos.toString();
	}

	public void setSortOrder(String order) throws IllegalArgumentException {
		order = order.toUpperCase();
		for (int i = 0; i < order.length(); i++) {
			if (_possibleOrders.indexOf(order.charAt(i)) < 0)
				throw new IllegalArgumentException("Illegal Sort option: "
						+ order.charAt(i));
		}
		sortOrder = order;
		Arrays.sort(myFields, this);
	}

	/**
	 * Method declaration
	 * 
	 * 
	 * @param topClass
	 * 
	 * @see
	 */
	private void createFieldList(Class<?> topClass) {
		_flist.clear();

		for (Class<?> c = topClass; c != null;) {
			Field[] f = c.getDeclaredFields();
			for (int i = 0; i < f.length; i++) {
				if ((f[i].getModifiers() & Modifier.ABSTRACT) == 0)
					_flist.add(f[i]);
			}
			c = c.getSuperclass();
		}

		myFields = new Field[_flist.size()];
		_flist.toArray(myFields);

		_flist.clear();

	}

	private int getModifierBit(char c) {
		int bit = 0;
		switch (c) {
		case 'A':
			// abstract
			bit = Modifier.ABSTRACT;
			break;
		case 'F':
			// final
			bit = Modifier.FINAL;
			break;
		case 'I':
			// interface
			bit = Modifier.INTERFACE;
			break;
		case 'N':
			// native
			bit = Modifier.NATIVE;
			break;
		case 'P':
			// private
			bit = Modifier.PRIVATE;
			break;
		case 'R':
			// protected
			bit = Modifier.PROTECTED;
			break;
		case 'U':
			// public
			bit = Modifier.PUBLIC;
			break;
		case 'S':
			// static
			bit = Modifier.STATIC;
			break;
		case 'Y':
			// sych
			bit = Modifier.SYNCHRONIZED;
			break;
		case 'T':
			// transient
			bit = Modifier.TRANSIENT;
			break;
		case 'V':
			// volatile
			bit = Modifier.VOLATILE;
			break;
		}
		return bit;
	}

	private int bitCompare(Field f1, Field f2, int bit) {
		if (((f1.getModifiers() & bit) == 0)
				&& ((f2.getModifiers() & bit) == bit))
			return 1;
		if (((f1.getModifiers() & bit) == bit)
				&& ((f2.getModifiers() & bit) == 0))
			return -1;
		return 0;
	}

	/**
	 * Comparator for the field ordering. -1 if o1 comes before o2, 0 if the are
	 * equal, +1 if o1 comes after o2
	 * 
	 * @param o1
	 *            a value of type 'Object'
	 * @param o2
	 *            a value of type 'Object'
	 * @return a value of type 'int'
	 */
	@Override
	public int compare(Object o1, Object o2) {
		Field f1 = (Field) o1;
		Field f2 = (Field) o2;

		// for each entry in sort order
		for (int i = 0; i < sortOrder.length(); i++) {
			int bit = getModifierBit(sortOrder.charAt(i));

			// ok, compare on bit value first
			int test = bitCompare(f1, f2, bit);
			if (test != 0)
				return test;
		}
		// else compare on name...
		return f1.getName().compareTo(f2.getName());
	}

	public static void main(String args[]) {
		HashMap<String, String> hm = new HashMap<String, String>(11);
		hm.put("Hello!", "Goodbye");

		InstanceInspector ii1 = new InstanceInspector(hm);
		System.out.println(ii1);
		InstanceInspector ii2 = new InstanceInspector(ii1);
		System.out.println(ii2);
	}
}
