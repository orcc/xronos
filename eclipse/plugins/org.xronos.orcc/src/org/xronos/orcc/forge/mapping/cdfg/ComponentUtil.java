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
package org.xronos.orcc.forge.mapping.cdfg;

import java.util.List;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.ClockDependency;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.ResetDependency;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.ComplementOp;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.EqualsOp;
import org.xronos.openforge.lim.op.GreaterThanEqualToOp;
import org.xronos.openforge.lim.op.GreaterThanOp;
import org.xronos.openforge.lim.op.LeftShiftOp;
import org.xronos.openforge.lim.op.LessThanEqualToOp;
import org.xronos.openforge.lim.op.LessThanOp;
import org.xronos.openforge.lim.op.MinusOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.NotEqualsOp;
import org.xronos.openforge.lim.op.NotOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.util.MathStuff;

import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.OpUnary;

/**
 * An utility class that contains static methods that helps creating components
 * @author Endri Bezati
 *
 */
public class ComponentUtil {

	public static Component createExprBinComponent(ExprBinary expr){
		int sizeInBits = expr.getType().getSizeInBits();
		Component op = null;
		if (expr.getOp() == OpBinary.BITAND) {
			op = new AndOp();
		} else if (expr.getOp() == OpBinary.BITOR) {
			op = new OrOp();
		} else if (expr.getOp() == OpBinary.BITXOR) {
			op = new XorOp();
		} else if (expr.getOp() == OpBinary.DIV) {
			op = new DivideOp(sizeInBits);
		} else if (expr.getOp() == OpBinary.DIV_INT) {
			op = new DivideOp(sizeInBits);
		} else if (expr.getOp() == OpBinary.EQ) {
			op = new EqualsOp();
		} else if (expr.getOp() == OpBinary.GE) {
			op = new GreaterThanEqualToOp();
		} else if (expr.getOp() == OpBinary.GT) {
			op = new GreaterThanOp();
		} else if (expr.getOp() == OpBinary.LE) {
			op = new LessThanEqualToOp();
		} else if (expr.getOp() == OpBinary.LOGIC_AND) {
			op = new And(2);
		} else if (expr.getOp() == OpBinary.LOGIC_OR) {
			op = new Or(2);
		} else if (expr.getOp() == OpBinary.LT) {
			op = new LessThanOp();
		} else if (expr.getOp() == OpBinary.MINUS) {
			op = new SubtractOp();
		} else if (expr.getOp() == OpBinary.MOD) {
			op = new ModuloOp();
		} else if (expr.getOp() == OpBinary.NE) {
			op = new NotEqualsOp();
		} else if (expr.getOp() == OpBinary.PLUS) {
			op = new AddOp();
		} else if (expr.getOp() == OpBinary.SHIFT_LEFT) {
			int log2N = MathStuff.log2(sizeInBits);
			op = new LeftShiftOp(log2N);
		} else if (expr.getOp() == OpBinary.SHIFT_RIGHT) {
			int log2N = MathStuff.log2(sizeInBits);
			op = new RightShiftOp(log2N);
		} else if (expr.getOp() == OpBinary.TIMES) {
			op = new MultiplyOp(sizeInBits);
		}
		return op;			
	}
	
	public static Component createExprUnaryComponent(ExprUnary expr){
		Component op = null;
		if (expr.getOp() == OpUnary.BITNOT){
			op = new ComplementOp();
		}else if (expr.getOp() == OpUnary.LOGIC_NOT){
			op = new NotOp();
		}else if (expr.getOp() == OpUnary.MINUS){
			op = new MinusOp();
		}
		
		return op;
	}
	
	
	/**
	 * This method connects the done bus port of a component to its owner done
	 * bus
	 * 
	 * @param component
	 * @param owner
	 */
	public static void connectControlDependency(Component component, Component owner) {
		Bus doneBus = component.getExit(Exit.DONE).getDoneBus();
		Port donePort = owner.getExit(Exit.DONE).getDoneBus().getPeer();
		List<Entry> entries = donePort.getOwner().getEntries();
		Entry entry = entries.get(0);
		Dependency dep = new ControlDependency(doneBus);
		entry.addDependency(donePort, dep);
	}

	/**
	 * This method connects a bus with a target port
	 * 
	 * @param bus
	 *            the result bus
	 * @param port
	 *            the target port
	 * @param group
	 *            the entry group
	 */
	public static void connectDataDependency(Bus bus, Port port, int group) {
		List<Entry> entries = port.getOwner().getEntries();
		Entry entry = entries.get(0);
		Dependency dep = new DataDependency(bus);
		entry.addDependency(port, dep);
	}
	
	/**
	 * This method adds an entry to a component
	 * 
	 * @param component
	 *            the component
	 * @param drivingExit
	 *            the driving Exit
	 * @param clockBus
	 *            the clock bus attached to the component
	 * @param resetBus
	 *            the reset bus attached to the component
	 * @param goBus
	 *            the go bus control of the component
	 */
	public static void componentAddEntry(Component component, Exit drivingExit,
			Bus clockBus, Bus resetBus, Bus goBus) {

		Entry entry = component.makeEntry(drivingExit);
		// Even though most components do not use the clock, reset and
		// go ports we set up the dependencies for consistency.
		entry.addDependency(component.getClockPort(), new ClockDependency(
				clockBus));
		entry.addDependency(component.getResetPort(), new ResetDependency(
				resetBus));
		entry.addDependency(component.getGoPort(), new ControlDependency(goBus));
	}
	
}
