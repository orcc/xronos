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

package org.xronos.openforge.lim.io;

/**
 * SimpleFifoPin is a concrete instance of the {@link SimplePin} but provides no
 * additional functionality. The original intent was to have it know what
 * 'group' of pins it belonged to for purposes of adding scheduling contsraints.
 * However this functionality was achieved by adding scheduling constraints on
 * the FifoRead and FifoWrite modules instead. The former documentation is saved
 * for reference. <strike> SimpleFifoPin is an extension of the
 * {@link SimplePin} and adds an additional scheduling contraint by adding the
 * {@link Refernceable} {@link FifoIF} to the set of referenceable targets held
 * in the pin. This way, dependencies can be created for the pin (allowing the
 * scheduler to ensure that no two accesses to the same pin occur in the same
 * cycle) and for the grouping of pins (ie the interface) ensuring that the
 * ordering of all pin accesses to a given interface remains consistent.
 * </strike>
 * 
 * <p>
 * Created: Thu Jan 15 10:48:46 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimpleFifoPin.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SimpleFifoPin extends SimplePin {

	public SimpleFifoPin(FifoIF fifoInterface, int width, String pinName) {
		super(width, pinName);
	}

}// SimpleFifoPin
