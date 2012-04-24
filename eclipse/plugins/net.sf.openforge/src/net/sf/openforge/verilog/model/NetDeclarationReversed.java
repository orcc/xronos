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

package net.sf.openforge.verilog.model;

/**
 * NetDeclarationReversed is a net declaration in which the vector bit notation
 * is in the form of LSB:MSB, ie [0:31].
 * 
 * 
 * <p>
 * Created: Fri Aug 6 12:25:30 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: NetDeclarationReversed.java 2 2005-06-09 20:00:48Z imiller $
 */
public class NetDeclarationReversed extends NetDeclaration {

	public NetDeclarationReversed() {
		super();
	}

	public NetDeclarationReversed(Net net) {
		super(net);
	}

	public NetDeclarationReversed(Net[] nets) {
		super(nets);
	}

	protected Range getRange() {
		return new RangeReversed(msb, lsb);
	}

}// NetDeclarationReversed
