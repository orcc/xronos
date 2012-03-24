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
/* $Rev: 2 $ */
package net.sf.openforge.forge.api.sim.pin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.sf.openforge.forge.api.pin.Buffer;
import net.sf.openforge.forge.api.pin.ClockDomain;
import net.sf.openforge.forge.api.pin.RestrictedPin;

/**
 * This is the class used to associate a PinData set with a Pin object.
 * <p>
 * 
 * This sample program illustrates how to use the Test Framework. It contains 2
 * entry methods, one AutoStart and one ExternalStart. Note the use of the
 * return EntryMethod object from Entry.addExternalStartMethod() to gain access
 * to the GO, ARG and RESULT pins for the 'echo()' method.
 * <p>
 * 
 * Also note how multiple types of PinData sets are used to build the needed
 * sequences of values. Multiple types are also combined to build the Test data
 * set for the PinOut 'pin'.
 * <p>
 * 
 * Finally, Calls to PinSimData.setDriveData() and PinSimData.setTestData() are
 * used to associate a given data set with a specific pin.
 * <p>
 * 
 * When test and/or drive data is associate with a pin in the design, a file
 * name <Java file>_atb.v is generated, which will drive the values as described
 * by the Drive data sets associated with pins in the design, and test the with
 * the Test data sets associated with pins in the design.
 * <p>
 */
public class PinSimData {
	private static HashMap<Buffer, SequentialPinData> mapPinToDriveData = new HashMap<Buffer, SequentialPinData>(
			11);
	private static HashMap<Buffer, SequentialPinData> mapPinToTestData = new HashMap<Buffer, SequentialPinData>(
			11);

	private PinSimData() {
	}

	/**
	 * Associate a pin with a set of drive data, referenced to the supplied
	 * clock.
	 * 
	 * @param pin
	 *            a value of type 'Buffer'
	 * @param pinData
	 *            a value of type 'PinData'
	 */
	public static void setDriveData(Buffer pin, PinData pinData) {
		mapPinToDriveData.put(pin, new SequentialPinData(pinData));
	}

	/**
	 * Associate a pin with a set of test data (expected results), referenced to
	 * the supplied clock.
	 * 
	 * @param pin
	 *            a value of type 'Buffer'
	 * @param pinData
	 *            a value of type 'PinData'
	 */
	public static void setTestData(Buffer pin, PinData pinData) {
		mapPinToTestData.put(pin, new SequentialPinData(pinData));
	}

	/**
	 * Associate a pin with a set of drive data, referenced to the supplied
	 * clock.
	 * 
	 * @param rpin
	 *            a Restricted pin (from an ipcore)
	 * @param pinData
	 *            a value of type 'PinData'
	 */
	public static void setDriveData(RestrictedPin rpin, PinData pinData) {
		setDriveData(rpin, pinData, ClockDomain.GLOBAL);
	}

	/**
	 * Associate a pin with a set of test data (expected results), referenced to
	 * the supplied clock.
	 * 
	 * @param rpin
	 *            a Restricted pin (from an ipcore)
	 * @param pinData
	 *            a value of type 'PinData'
	 */
	public static void setTestData(RestrictedPin rpin, PinData pinData) {
		setTestData(rpin, pinData, ClockDomain.GLOBAL);
	}

	/**
	 * Associate a pin with a set of drive data, referenced to the supplied
	 * clock.
	 * 
	 * @param rpin
	 *            a Restricted pin (from an ipcore)
	 * @param pinData
	 *            a value of type 'PinData'
	 * @param domain
	 *            specific domain this vector is associated
	 */
	public static void setDriveData(RestrictedPin rpin, PinData pinData,
			ClockDomain domain) {
		mapPinToDriveData.put(getOriginalBuffer(rpin), new SequentialPinData(
				pinData));
		rpin.setDomain(domain);
	}

	/**
	 * Associate a pin with a set of test data (expected results), referenced to
	 * the supplied clock.
	 * 
	 * @param rpin
	 *            a Restricted pin (from an ipcore)
	 * @param pinData
	 *            a value of type 'PinData'
	 * @param domain
	 *            specific domain this vector is associated
	 */
	public static void setTestData(RestrictedPin rpin, PinData pinData,
			ClockDomain domain) {
		mapPinToTestData.put(getOriginalBuffer(rpin), new SequentialPinData(
				pinData));
		rpin.setDomain(domain);
	}

	/**
	 * Get the sim Drive data for a specific pin. Creates a SequentialPinData
	 * entry with no data if no entry exists.
	 * 
	 * @param pin
	 *            a value of type 'Buffer'
	 * @return a value of type 'PinSimData'
	 */
	public static PinData getDriveData(Buffer pin) {

		PinData pd = mapPinToDriveData.get(pin);

		if (pd == null) {
			pd = new SequentialPinData();
			setDriveData(pin, pd);
		}
		return pd;
	}

	/**
	 * Get the sim Test data for a specific pin. Creates a SequentialPinData
	 * entry with no data if no entry exists.
	 * 
	 * @param pin
	 *            a value of type 'Buffer'
	 * @return a value of type 'PinSimData'
	 */
	public static PinData getTestData(Buffer pin) {
		PinData pd = mapPinToTestData.get(pin);
		if (pd == null) {
			pd = new SequentialPinData();
			setTestData(pin, pd);
		}
		return pd;
	}

	/**
	 * Empty the data base
	 * 
	 */
	public static void clear() {
		mapPinToDriveData.clear();
		mapPinToTestData.clear();
	}

	public static Map<Buffer, SequentialPinData> getDriveMap() {
		return mapPinToDriveData;
	}

	@SuppressWarnings("unchecked")
	public static Map<Buffer, SequentialPinData> cloneDriveMap() {
		return (Map<Buffer, SequentialPinData>) mapPinToDriveData.clone();
	}

	public static Map<Buffer, SequentialPinData> getTestMap() {
		return mapPinToTestData;
	}

	@SuppressWarnings("unchecked")
	public static Map<Buffer, SequentialPinData> cloneTestMap() {
		return (Map<Buffer, SequentialPinData>) mapPinToTestData.clone();
	}

	public static void setDriveData(Map<Buffer, SequentialPinData> m) {
		getDriveMap().clear();
		getDriveMap().putAll(m);
	}

	public static void setTestData(Map<Buffer, SequentialPinData> m) {
		getTestMap().clear();
		getTestMap().putAll(m);
	}

	public static boolean exists() {
		return (getDriveMap().size() > 0) || (getTestMap().size() > 0);
	}

	private static Buffer getOriginalBuffer(RestrictedPin rpin) {
		Class<RestrictedPin> c = RestrictedPin.class;
		try {
			Field f = c.getDeclaredField("original");
			f.setAccessible(true);
			try {
				Buffer b = (Buffer) f.get(rpin);
				return b;
			} catch (IllegalAccessException iae) {
			}
		} catch (NoSuchFieldException nsfe) {
		}
		return null;
	}
}
