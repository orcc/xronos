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

/**
 * An output pin which a {@link Design} both write and tri-state.
 * 
 * @author Stephen Edwards
 * @version $Id: TriOutputPin.java 2 2005-06-09 20:00:48Z imiller $
 */
public class TriOutputPin extends OutputPin {

	/**
	 * Constructs a new TriOutputPin based on a Bus.
	 * 
	 * @param bus
	 *            the bus upon which to base the pin
	 */
	public TriOutputPin(Bus bus) {
		super(bus);
	}
}
