/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.xronos.orcc.design.visitors.io;

import java.util.List;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.xronos.orcc.design.util.XronosMathUtil;

/**
 * This class contains the necessary information of a Circular Buffer
 * 
 * @author Endri Bezati
 * 
 */
public class CircularBuffer {

	/** The buffer Variable **/
	private Var buffer;

	/** The count variable of the circular Buffer Index **/
	private Var count;

	/** The head variable of the circular Buffer Index **/
	private Var head;

	/** The name of the circular buffer **/
	private String name;

	/** The circular buffer size **/
	private Port port;

	/** The circular buffer Request size **/
	private Var requestSize;

	/** The circular buffer size **/
	private Integer size;

	/** The circular buffer size **/
	private Integer sizePowTwo;

	/** The Boolean start Variable **/
	private Var start;

	/** The temporary count variable of the circular Buffer Index **/
	private Var tmpCount;

	/** The temporary head variable of the circular Buffer Index **/
	private Var tmpHead;

	/** The temporary head variable of the circular Buffer Index **/
	private Var tmpRequestSize;

	/** The temporary start variable of the circular Buffer Index **/
	private Var tmpStart;

	public CircularBuffer(Port port, Var buffer, Integer size) {
		this.port = port;
		this.buffer = buffer;
		this.size = size;
		this.sizePowTwo = XronosMathUtil.nearestPowTwo(size);
		createVariables();
	}

	public void addToLocals(Procedure procedure) {
		List<Var> locals = procedure.getLocals();
		locals.add(tmpCount);
		locals.add(tmpHead);
		locals.add(tmpStart);
	}

	public void addToStateVars(Actor actor, boolean addRequestSize) {
		List<Var> stateVars = actor.getStateVars();
		stateVars.add(buffer);
		stateVars.add(head);
		stateVars.add(count);
		stateVars.add(start);
		if (addRequestSize) {
			stateVars.add(requestSize);
		}
	}

	private void createVariables() {
		name = port.getName();
		Type type = IrFactory.eINSTANCE.createTypeInt();

		head = IrFactory.eINSTANCE.createVar(type, "cb_" + name + "_head",
				true, 0);
		tmpHead = IrFactory.eINSTANCE.createVar(type,
				"cb_" + name + "_tmpHead", true, 0);

		count = IrFactory.eINSTANCE.createVar(type, "cb_" + name + "_count",
				true, 0);
		tmpCount = IrFactory.eINSTANCE.createVar(type, "cb_" + name
				+ "_tmpCount", true, 0);

		requestSize = IrFactory.eINSTANCE.createVar(type, "cb_" + name
				+ "_requestSize", true, 0);
		tmpRequestSize = IrFactory.eINSTANCE.createVar(type, "cb_" + name
				+ "_tmpRequestSize", true, 0);

		Type bool = IrFactory.eINSTANCE.createTypeBool();

		start = IrFactory.eINSTANCE.createVar(bool, "cb_" + name + "_start",
				true, 0);
		tmpStart = IrFactory.eINSTANCE.createVar(bool, "cb_" + name
				+ "_tmpStart", true, 0);
	}

	public Var getBuffer() {
		return buffer;
	}

	public Var getCount() {
		return count;
	}

	public Var getHead() {
		return head;
	}

	public Port getPort() {
		return port;
	}

	public Var getRequestSize() {
		return requestSize;
	}

	public Integer getSize() {
		return size;
	}

	public Integer getSizePowTwo() {
		return sizePowTwo;
	}

	public Var getStart() {
		return start;
	}

	public Var getTmpCount() {
		return tmpCount;
	}

	public Var getTmpHead() {
		return tmpHead;
	}

	public Var getTmpRequestSize() {
		return tmpRequestSize;
	}

	public Var getTmpStart() {
		return tmpStart;
	}
}
