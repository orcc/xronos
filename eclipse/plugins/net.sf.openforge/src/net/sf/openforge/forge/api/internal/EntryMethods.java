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

package net.sf.openforge.forge.api.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.forge.api.entry.EntryMethod;

/**
 * An EntryMethod is an entry point into the user's design. This class keeps a
 * simple set of all declared entry methods to the user's design
 * 
 */
public class EntryMethods {

	public static volatile boolean runAutoStarts = true;

	/** Set of entry methods */
	private static final HashSet<EntryMethod> entrySet = new HashSet<EntryMethod>();

	// only we call this
	private EntryMethods() {
	}

	/**
	 * Clears the Collection of EntryMethods maintained by this class.
	 */
	public static void clearEntryMethods() {
		entrySet.clear();
	}

	/**
	 * Gets all the EntryMethods that have been defined.
	 * 
	 * @return a 'Set' of EntryMethod objects.
	 */
	public static Set<EntryMethod> getEntryMethods() {
		return entrySet;
	}

	/**
	 * Return a shallow clone of the entry Methods
	 * 
	 * @return a value of type 'Set'
	 */
	@SuppressWarnings("unchecked")
	public static Set<EntryMethod> cloneEntryMethods() {
		return (Set<EntryMethod>) entrySet.clone();
	}

	/**
	 * Sets all the EntryMethods per the incoming set
	 * 
	 * @param s
	 *            a value of type 'Set'
	 * 
	 */
	public static void setEntryMethods(Set<EntryMethod> s) {
		entrySet.addAll(s);
	}

	/**
	 * Determines if the given EntryMethod has been added to this Class.
	 * 
	 * @param em
	 *            the EntryMethod to look for
	 * @return true if the EntryMethod is found
	 */
	public static boolean contains(EntryMethod em) {
		return entrySet.contains(em);
	}

	/**
	 * Adds the given EntryMethod to this class.
	 * 
	 * @param em
	 *            the EntryMethod to add.
	 * @return true if the EntryMethod was successfully added.
	 */
	public static boolean add(EntryMethod em) {
		boolean ret = false;
		synchronized (entrySet) {
			ret = entrySet.add(em);
		}

		if (ret) {
			// If the entry is an autostart, then we create a thread and
			// actually run the method, so a user's program has a hope of
			// working when run outside of forge.
			//
			// XXX: when the simulator is added, and if simulating, then
			// don't launch the RunThread since it will interfere with the
			// simulation of the design.
			//
			if (!em.isExternal() && runAutoStarts) {
				RunThread rt = new RunThread(em.getObject(),
						em.getObjectClass(), em.getMethod());
				// Mark the thread as a daemon, so the user doesn't need
				// to call System.exit() to exit their program, if the
				// AutoStart method never returns.

				rt.setDaemon(true);
				rt.start();
			}
		}
		return ret;
	}
}

class RunThread extends java.lang.Thread {
	Object obj;
	Method method;
	@SuppressWarnings("rawtypes")
	Class clazz;

	@SuppressWarnings("rawtypes")
	public RunThread(Object obj, Class clazz, Method method) {
		super(method.getName());
		this.obj = obj;
		this.clazz = clazz;
		this.method = method;
	}

	@Override
	public void run() {
		// Call the given method, it should have no arguments since it
		// is an autostart method, and this fact was verified for us.
		if (method == null)
			return;

		if (clazz == null)
			return;

		String className;

		if (obj == null)
			className = clazz.toString();
		else
			className = obj.getClass().toString();

		String methodName = method.getName();

		try {
			// Make accessable, incase its private or package protected, then
			// call
			method.setAccessible(true);
			method.invoke(obj, new Object[0]);
		} catch (IllegalAccessException iae) {
			System.err
					.println("ERROR: IllegalAccessException when creating Thread for Entry Method: "
							+ className + "." + methodName);
		} catch (IllegalArgumentException iae) {
			System.err
					.println("ERROR: IllegalArgumentException when creating Thread for Entry Method: "
							+ className + "." + methodName);
		} catch (InvocationTargetException ite) {
			System.err
					.println("ERROR: InvocationTargetException when creating Thread for Entry Method: "
							+ className + "." + methodName);
		} catch (NullPointerException npe) {
			System.err
					.println("ERROR: NullPointerException when creating Thread for Entry Method: "
							+ className + "." + methodName);
		} catch (ExceptionInInitializerError eiie) {
			System.err
					.println("ERROR: ExceptionInInitializerError when creating Thread for Entry Method: "
							+ className + "." + methodName);
		} catch (Throwable e) {
			System.err.println("ERROR: Exception: " + e.getMessage()
					+ " when creating Thread for Entry Method: " + className
					+ "." + methodName);
		}
	}

}
