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
package org.xronos.openforge.verilog.model;

import java.util.Collection;

/**
 * EventControl is a verilog element.
 * <P>
 * Example:<BR>
 * <CODE>
 * &#64; (posedge CLK)
 * </CODE>
 * 
 * <P>
 * 
 * Created: Fri Mar 02 2001
 * 
 * @author abk
 * @version $Id: EventControl.java 2 2005-06-09 20:00:48Z imiller $
 */
public class EventControl implements Expression {

	EventExpression ee;

	public EventControl(EventExpression ee) {
		this.ee = ee;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.EVENT);
		lex.append(Symbol.OPEN_PARENTHESIS);
		lex.append(ee);
		lex.append(Symbol.CLOSE_PARENTHESIS);

		return lex;
	} // EventControl()

	@Override
	public Collection<Net> getNets() {
		return ee.getNets();
	}

	@Override
	public int getWidth() {
		return ee.getWidth();
	}

	@Override
	public String toString() {
		return lexicalify().toString();
	}

} // end of class EventControl
