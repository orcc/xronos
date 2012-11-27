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

package org.xronos.openforge.forge.api.entry;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.xronos.openforge.forge.api.ForgeApiException;
import org.xronos.openforge.forge.api.internal.EntryMethods;
import org.xronos.openforge.forge.api.pin.ClockDomain;
import org.xronos.openforge.forge.api.pin.ClockPin;
import org.xronos.openforge.forge.api.pin.DonePin;
import org.xronos.openforge.forge.api.pin.GoPin;
import org.xronos.openforge.forge.api.pin.PinIn;
import org.xronos.openforge.forge.api.pin.ResetPin;
import org.xronos.openforge.forge.api.pin.ResultPin;


/**
 * An EntryMethod is an entry point into the user's design. It consists of a
 * target object and a method to be called on that object. EntryMethods are
 * created by calls in the user application to the API method.
 * 
 */
public class EntryMethod {
	/** The clock domain for this method. */
	private ClockDomain clockDomain = ClockDomain.GLOBAL;

	/** The go pin. */
	private final GoPin goPin = new GoPin();

	/** The done pin. */
	private final DonePin donePin = new DonePin();

	/** The argument pins. */
	private final PinIn[] argPins;

	/** The result pin used by this method. */
	private final ResultPin resultPin = new ResultPin();

	/** All pins, excluding clock and reset. */
	// private Collection<Pin> pins;

	/** The object this virtual method is attached to */
	private Object object;

	/** The Class of the method belonging to */
	private final Class clazz;

	/** The entry method to be called on the object */
	private final Method method;

	/** The argument list of this entry method */
	private Class[] arguments = {};

	/** true if the entry method is external, false if auto start */
	private final boolean external;

	/**
	 * Adds a design entry method. If the method signature is not specified, the
	 * class is checked to verify that only one method by the given name is
	 * present. An error is thrown if the method name isn't present in the class
	 * or it is overloaded.
	 * 
	 * @param object
	 *            , the Object on which the entry method is to be invoked, or
	 *            null if entry method is static
	 * @param clazz
	 *            the target Class object of the entry method
	 * @param methodName
	 *            the name of the entry method
	 * @param arguments
	 *            the types of the method arguments
	 * @param external
	 *            true if this is an external method, false if it is an
	 *            AutoStart method (go asserted once on reset)
	 * 
	 * @exception ForgeApiException
	 *                if the given description is erroneous
	 * @exception NoSuchMethodException
	 *                if the class of the given object has no such method
	 */
	protected EntryMethod(Object object, Class clazz, String methodName,
			Class[] arguments, boolean external) throws ForgeApiException,
			NoSuchMethodException {
		this.object = object;
		this.clazz = clazz;
		this.arguments = arguments;
		this.external = external;

		if (arguments == null) {
			Method[] methods = clazz.getDeclaredMethods();

			// Verify the method name exists in the class and that is
			// isn't overloaded.
			boolean foundName = false;
			int whichOne = 0;

			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals(methodName)) {
					if (foundName == true) {
						throw new ForgeApiException(
								"method: "
										+ clazz.getName()
										+ "."
										+ methodName
										+ " is overloaded, cannot add without explicit signature");
					}

					foundName = true;
					whichOne = i;
				}
			}

			if (!foundName) {
				throw new ForgeApiException("method: " + clazz.getName() + "."
						+ methodName + " cannot be found");
			}

			this.arguments = arguments = methods[whichOne].getParameterTypes();
		}

		method = clazz.getDeclaredMethod(methodName, arguments);
		int modifier = method.getModifiers();

		// Check if the method is virtual, but the user gave us a null
		// object, i.e. they wanted a static method
		if (!Modifier.isStatic(modifier) && (object == null)) {
			throw new ForgeApiException("EntryMethod(" + methodName
					+ "): method is virtual, can't use static version of add");
		}

		if (Modifier.isStatic(modifier)) {
			this.object = null;
		}

		if (Modifier.isAbstract(modifier) || Modifier.isNative(modifier)) {
			throw new ForgeApiException("EntryMethod(" + methodName
					+ "): Can't add methods that are abstract or native");
		}

		// if its not external then it can not have a result or arguments
		if (!external
				&& ((arguments.length > 0) || !(method.getReturnType()
						.equals(java.lang.Void.TYPE)))) {
			throw new ForgeApiException(
					"EntryMethod("
							+ methodName
							+ "): Can't add an Auto Start method with arguments or non-void return");
		}

		if (!EntryMethods.add(this)) {
			throw new ForgeApiException("EntryMethod(" + methodName
					+ "): Identical method already added.");
		}

		// args
		argPins = new PinIn[arguments.length];
		for (int i = 0; i < argPins.length; i++) {
			argPins[i] = new PinIn("ENTRY_" + System.identityHashCode(this)
					+ "_ARG" + i, 64);
		}

	}

	/**
	 * Hashcode is constructed of class+method for statics, class
	 * +method+system.identityHashcode(object) for virtuals
	 * 
	 * @return an int hash code value for this EntryMethod
	 */
	@Override
	public int hashCode() {
		if (isStatic()) {
			return getObjectClass().hashCode() + getMethod().hashCode();
		} else { // also use the target object if it is not static
			return System.identityHashCode(object)
					+ getObjectClass().hashCode() + getMethod().hashCode();
		}
	}

	/**
	 * Returns true if this EntryMethod object is equivalent to the given
	 * Object. Equivalence is determined by identical entry method name, class
	 * and signature being invoked on the same target (if non static).
	 * 
	 * @param obj
	 *            an Object to test for equality with this EntryMethod.
	 * 
	 * @return true if the Object is equivalent to this EntryMethod.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EntryMethod) {
			EntryMethod entryMethod = (EntryMethod) obj;
			return ((System.identityHashCode(getObject()) == System
					.identityHashCode(entryMethod.getObject()))
					&& (entryMethod.getMethod().equals(getMethod())) && (entryMethod
						.getObjectClass() == getObjectClass()));
		}

		return false;
	}

	/**
	 * Returns the Class of the target of this entry method.
	 * 
	 * @return a java.lang.Class object.
	 */
	public Class getObjectClass() {
		return clazz;
	}

	/**
	 * Returns the Object on which this entry method is to be invoked, or null
	 * if this entry method is static.
	 * 
	 * @return the target Object for this entry method.
	 */
	public Object getObject() {
		return object;
	}

	/**
	 * Gets an array of Class objects identifying the types of each parameter to
	 * this entry method.
	 * 
	 * @return an array of Class objects, primitives represented by the Class
	 *         fields in EntryMethod.
	 */
	public Class[] getArguments() {
		return arguments;
	}

	/**
	 * The entry method to be called on the target object.
	 * 
	 * @return the reflected Method to be invoked.
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * Is the method external
	 * 
	 * @return true if the method is an external start method, false if
	 *         autostart (go asserted once on reset)
	 */
	public boolean isExternal() {
		return (external);
	}

	/**
	 * Is the method static.
	 * 
	 * @return true if this entry method is static.
	 */
	public boolean isStatic() {
		return Modifier.isStatic(method.getModifiers());
	}

	/**
	 * Convenience string for informational purposes only.
	 * 
	 * @return a string.
	 */
	@Override
	public String toString() {
		return (external ? "External" : "Auto") + "EntryMethod: " + method;
	}

	/**
	 * Sets the clock domain to use for this method.
	 * 
	 * @param domain
	 *            the clock domain to use
	 */
	public void setDomain(ClockDomain domain) {
		if (domain == org.xronos.openforge.forge.api.ipcore.IPCore.AUTOCONNECT) {
			throw new ForgeApiException(
					"Cannot use the 'stand in' clock domain on entry methods.  It is only valid for setting the clock domain of an IPCore.");
		}

		clockDomain = domain;

		getGoPin().setDomain(domain);
		getDonePin().setDomain(domain);
		for (int i = 0; i < argPins.length; i++) {
			argPins[i].setDomain(domain);
		}
		getResultPin().setDomain(domain);
	}

	/**
	 * Gets the clock domain assigned to this method.
	 * 
	 * @return the clock domain
	 */
	public ClockDomain getDomain() {
		return clockDomain;
	}

	/**
	 * Gets the clock pin used by this method's clock domain.
	 * 
	 * @return the ClockPin for this EntryMethod.
	 */
	public ClockPin getClockPin() {
		return clockDomain.getClockPin();
	}

	/**
	 * Gets the reset pin used by this method's clock domain.
	 * 
	 * @return the ResetPin for this EntryMethod.
	 */
	public ResetPin getResetPin() {
		return clockDomain.getResetPin();
	}

	/**
	 * Gets the go pin.
	 * 
	 * @return the GoPin for this EntryMethod
	 */
	public GoPin getGoPin() {
		return goPin;
	}

	/**
	 * Gets the done pin used by this method's clock domain.
	 * 
	 * @return the DonePin for this EntryMethod
	 */
	public DonePin getDonePin() {
		return donePin;
	}

	/**
	 * Gets the result pin used by this method.
	 * 
	 * @return the ResultPin used by this EntryMethod.
	 */
	public ResultPin getResultPin() {
		return resultPin;
	}

	/**
	 * Gets an argument pin.
	 * 
	 * @param arg
	 *            the zero-indexed argument position
	 * 
	 * @return the related argument Pin for this EntryMethod.
	 */
	public PinIn getArgPin(int arg) {
		return argPins[arg];
	}

}
