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

package net.sf.openforge.forge.api.entry;

import java.lang.reflect.Method;

import net.sf.openforge.forge.api.ForgeApiException;

/**
 * The <code>Entry</code> class supports registration of methods that are entry
 * points to the design and will execute in parallel. Each entry method is
 * analagous to a parallel executing thread in the resulting hardware. Any
 * shared resources such as Arrays and Static fields will not be synchronized,
 * so the user's design must accomodate these situations.
 * 
 */
public final class Entry {
	/**
	 * Constant <code>BOOLEAN</code> is a convenience field for building class
	 * type arrays for use with the addExternalMethod() method.
	 */
	public static final Class<Boolean> BOOLEAN = boolean.class;
	/**
	 * Constant <code>BYTE</code> is a convenience field for building class type
	 * arrays for use with the addExternalMethod() method.
	 */
	public static final Class<Byte> BYTE = byte.class;
	/**
	 * Constant <code>CHAR</code> is a convenience field for building class type
	 * arrays for use with the addExternalMethod() method.
	 */
	public static final Class<Character> CHAR = char.class;
	/**
	 * Constant <code>SHORT</code> is a convenience field for building class
	 * type arrays for use with the addExternalMethod() method.
	 */
	public static final Class<Short> SHORT = short.class;
	/**
	 * Constant <code>INT</code> is a convenience field for building class type
	 * arrays for use with the addExternalMethod() method.
	 */
	public static final Class<Integer> INT = int.class;
	/**
	 * Constant <code>LONG</code> is a convenience field for building class type
	 * arrays for use with the addExternalMethod() method.
	 */
	public static final Class<Long> LONG = long.class;

	/**
	 * Registers a virtual method for execution in parallel with other entry
	 * methods in the design. The method will execute each time its GO input is
	 * asserted.
	 * 
	 * @param object
	 *            an <code>Object</code> reference to invoke the given
	 *            methodName on.
	 * @param methodName
	 *            a <code>String</code> representing the name of the method to
	 *            call. For overloaded methods, use one of the other forms of
	 *            <code>addExternalStartMethod</code>.
	 */
	public static final EntryMethod addExternalStartMethod(Object object,
			String methodName) throws ForgeApiException {
		if (object == null) {
			throw new ForgeApiException("Entry: Object must be non-null");
		}

		try {
			return add(object, object.getClass(), methodName, null, true);
		} catch (ForgeApiException eMethodNotFound) {
			throw eMethodNotFound;
		}
	}

	/**
	 * Registers an overloaded virtual method for execution in parallel with
	 * other entry methods in the design. The method will execute each time its
	 * GO input is asserted.
	 * 
	 * @param object
	 *            an <code>Object</code> reference to invoke the given
	 *            methodName on.
	 * @param methodName
	 *            a <code>String</code> representing the name of the method to
	 *            call.
	 * @param arguments
	 *            an array of <code>Class</code> Objects representing the
	 *            ordered list of each argument type (Class) for the desired
	 *            method.
	 */
	public static final EntryMethod addExternalStartMethod(Object object,
			String methodName, Class[] arguments) throws ForgeApiException {
		if (object == null) {
			throw new ForgeApiException("Entry: Object must be non-null");
		}

		try {
			return add(object, object.getClass(), methodName, arguments, true);
		} catch (ForgeApiException eMethodNotFound) {
			throw eMethodNotFound;
		}
	}

	/**
	 * Registers a static method for execution in parallel with other entry
	 * methods in the design. The method will execute each time its GO input is
	 * asserted.
	 * 
	 * @param clazz
	 *            a <code>java.lang.Class</code> object, indicating the Class in
	 *            which the static entry method exists.
	 * @param methodName
	 *            a <code>String</code> representing the name of the method to
	 *            call. For overloaded methods, use one of the other forms of
	 *            <code>addExternalStartMethod</code>.
	 */
	public static final EntryMethod addExternalStartMethod(Class clazz,
			String methodName) throws ForgeApiException {
		try {
			return add(null, clazz, methodName, null, true);
		} catch (ForgeApiException eMethodNotFound) {
			throw eMethodNotFound;
		}
	}

	/**
	 * Registers an overloaded static method for execution in parallel with
	 * other entry methods in the design. The method will execute each time its
	 * GO input is asserted.
	 * 
	 * @param clazz
	 *            a <code>java.lang.Class</code> object, indicating the Class in
	 *            which the static entry method exists.
	 * @param methodName
	 *            a <code>String</code> representing the name of the method to
	 *            call.
	 * @param arguments
	 *            an array of <code>Class</code> Objects representing the
	 *            ordered list of each argument type (Class) for the desired
	 *            method.
	 */
	public static final EntryMethod addExternalStartMethod(Class clazz,
			String methodName, Class[] arguments) throws ForgeApiException {
		try {
			return add(null, clazz, methodName, arguments, true);
		} catch (ForgeApiException eMethodNotFound) {
			throw eMethodNotFound;
		}
	}

	//
	// Now the auto starts
	//

	/**
	 * Registers a virtual method for execution in parallel with other entry
	 * methods in the design. The Forge will insert circuitry to automatically
	 * invoke the method after reset.
	 * 
	 * Auto start methods must have no arguments and return void.
	 * 
	 * @param object
	 *            an <code>Object</code> reference to invoke the given
	 *            methodName on.
	 * @param methodName
	 *            a <code>String</code> representing the name of the method to
	 *            call.
	 */
	public static final EntryMethod addAutoStartMethod(Object object,
			String methodName) throws ForgeApiException {
		// AutoStart methods must have no arguments
		java.lang.Class[] arguments = {};

		if (object == null) {
			throw new ForgeApiException("Entry: Object must be non-null");
		}

		try {
			return add(object, object.getClass(), methodName, arguments, false);
		} catch (ForgeApiException eMethodNotFound) {
			throw eMethodNotFound;
		}
	}

	/**
	 * Registers a static method for execution in parallel with other entry
	 * methods in the design. The Forge will insert circuitry to invoke the
	 * method after reset.
	 * 
	 * Auto start methods must have no arguments and return void.
	 * 
	 * @param clazz
	 *            a <code>java.lang.Class</code> object, indicating the Class in
	 *            which the static entry method exists.
	 * @param methodName
	 *            a <code>String</code> representing the name of the method to
	 *            call.
	 */
	public static final EntryMethod addAutoStartMethod(Class clazz,
			String methodName) throws ForgeApiException {
		// AutoStart methods must have no arguments
		Class[] arguments = {};

		try {
			return add(null, clazz, methodName, arguments, false);
		} catch (ForgeApiException eMethodNotFound) {
			throw eMethodNotFound;
		}
	}

	private static EntryMethod add(Object object, Class clazz,
			String methodName, Class[] arguments, boolean external)
			throws ForgeApiException {

		if (clazz == null) {
			throw new ForgeApiException("Entry: Class must be non-null");
		}

		if (methodName == null) {
			throw new ForgeApiException("Entry: Methodname must be non-null");
		}

		try {
			return new EntryMethod(object, clazz, methodName, arguments,
					external);
		} catch (NoSuchMethodException eMethodNotFound) {
			StringBuffer buf = new StringBuffer();

			buf.append(clazz.getName());
			buf.append(".");
			buf.append(methodName);
			buf.append("(");
			for (int i = 0; i < arguments.length; i++) {
				buf.append(arguments[i].getName());
				if (i < arguments.length - 2) {
					buf.append(",");
				}
			}
			buf.append(")");

			if (external) {
				buf.insert(0,
						"Entry.addExternalStartMethod(): Method not found: ");
			} else {
				buf.insert(0, "Entry.addAutoStartMethod(): Method not found: ");

				// make an extra effort to give more informative error message
				// on AutoStartMethod
				boolean moreWarning = false;
				for (int i = 0; i < clazz.getDeclaredMethods().length; i++) {
					Method m = clazz.getDeclaredMethods()[i];
					if (m.getName().equals(methodName)
							&& (m.getParameterTypes().length > 0)) {
						moreWarning = true;
					}
				}
				if (moreWarning) {
					buf.append(". AutoStartMethod must have no arguments and return void.");
				}
			}

			throw new ForgeApiException(buf.toString());
		}
	}

	private Entry() {
	}

}
