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
package net.sf.openforge.verilog.translate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.FilteredVisitor;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Operation;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.Primitive;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.UnexpectedVisitationException;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryWrite;

/**
 * PassThroughComponentRemover visits a physically connected LIM. It eliminates
 * pass through components from a {@link Design} since those components buses
 * aren't consumed by any port's value. This will improve the QoR of outputting
 * Verilog.
 * 
 * @author cwu
 * @version $Id: PassThroughComponentRemover.java 23 2005-09-09 18:45:32Z
 *          imiller $
 */
public class PassThroughComponentRemover extends FilteredVisitor {

	private Set<Object> removableComponents = new HashSet<Object>();

	public PassThroughComponentRemover() {
		if (_translate.db)
			_translate.ln("Identifying pass through components...");
	}

	@Override
	public void visit(Design design) {
		// _translate.d.modGraph(design, "./dotDir");

		// super.visit(design);
		// No need to call the super, the task calls will be caught
		// here
		LinkedList<Object> comps = new LinkedList<Object>(design
				.getDesignModule().getComponents());
		while (!comps.isEmpty()) {
			Visitable vis = (Visitable) comps.remove(0);
			try {
				vis.accept(this);
			} catch (UnexpectedVisitationException uve) {
				if (vis instanceof Module) {
					comps.addAll(((Module) vis).getComponents());
				} else {
					// Not rethrowing uve b/c it is OK to leave pass
					// throughs.
				}
			}
		}

		for (Iterator<Object> compIter = removableComponents.iterator(); compIter
				.hasNext();) {
			Component component = (Component) compIter.next();
			if (_translate.db)
				_translate.ln("Removing(Component): " + component.toString());
			component.getOwner().removeComponent(component);
		}
		removableComponents.clear();

		// Visit the components of register's endian swappers.
		/*
		 * The physical and swappers will be visited in the super. for (Iterator
		 * iter = design.getRegisters().iterator(); iter.hasNext();) { Register
		 * reg = (Register)iter.next();
		 * 
		 * Module physical = reg.getPhysicalComponent(); if (physical != null) {
		 * for (Iterator compIter = physical.getComponents().iterator();
		 * compIter.hasNext();) { ((Visitable)compIter.next()).accept(this); } }
		 * for (Iterator compIter = removableComponents.iterator();
		 * compIter.hasNext();) { Component component =
		 * (Component)compIter.next(); if (_translate.db)
		 * _translate.ln("Removing(Component): " + component.toString());
		 * component.getOwner().removeComponent(component); }
		 * removableComponents.clear(); }
		 */

		// Visit the components inside the structural memory.
		/*
		 * The structural memory pieces will get visited by the super. for
		 * (Iterator iter = design.getLogicalMemories().iterator();
		 * iter.hasNext();) { LogicalMemory mem = (LogicalMemory)iter.next();
		 * StructuralMemory structMem = mem.getStructuralMemory(); if (structMem
		 * != null) { for (Iterator compIter =
		 * structMem.getComponents().iterator(); compIter.hasNext();) {
		 * ((Visitable)compIter.next()).accept(this); } } }
		 * 
		 * for (Iterator compIter = removableComponents.iterator();
		 * compIter.hasNext();) { Component component =
		 * (Component)compIter.next(); if (_translate.db)
		 * _translate.ln("Removing(Component): " + component.toString());
		 * component.getOwner().removeComponent(component); }
		 * removableComponents.clear();
		 */
	}

	@Override
	public void visit(Procedure procedure) {
		if (_translate.db)
			_translate.ln("Checking... " + procedure.toString());
		traverse(procedure);
	}

	@Override
	public void visit(InBuf inBuf) {
		preFilterAny(inBuf);
	}

	@Override
	public void visit(OutBuf outBuf) {
		preFilterAny(outBuf);
	}

	@Override
	public void visit(MemoryRead mr) {
		preFilterAny(mr);
		traverse(mr);
	}

	@Override
	public void visit(MemoryWrite mw) {
		preFilterAny(mw);
		traverse(mw);
	}

	@Override
	public void visit(ArrayRead ar) {
		preFilterAny(ar);
		traverse(ar);
	}

	@Override
	public void visit(ArrayWrite aw) {
		preFilterAny(aw);
		traverse(aw);
	}

	/**
	 * A SimplePinRead, by definition, is always just a passthrough. This method
	 * adds the pin read to the collection of components to be removed.
	 */
	@Override
	public void visit(SimplePinRead pinRead) {
		preFilterAny(pinRead);
		removableComponents.add(pinRead);
	}

	@Override
	public void visit(PinRead pinRead) {
		preFilter(pinRead);
	}

	@Override
	public void visit(PinWrite pinWrite) {
		preFilter(pinWrite);
		traverse(pinWrite);
	}

	@Override
	public void visit(Scoreboard scoreboard) {
		preFilter(scoreboard);
		traverse(scoreboard);
	}

	@Override
	public void visit(PinStateChange pinChange) {
		preFilter(pinChange);
		traverse(pinChange);
	}

	@Override
	public void visit(PinReferee pinReferee) {
		preFilter(pinReferee);
		traverse(pinReferee);
	}

	@Override
	public void visit(Latch latch) {
		preFilter(latch);
		traverse(latch);
	}

	@Override
	public void visit(Kicker kicker) {
		preFilter(kicker);
		traverse(kicker);
	}

	@Override
	public void preFilterAny(Component c) {
		if (_translate.db)
			_translate.ln("Checking... " + c.toString());
	}

	@Override
	public void filter(Operation op) {
		if (op.isPassThrough()) {
			if (_translate.db)
				_translate.ln("Found pass through operation: " + op.toString());
			removableComponents.add(op);
		}
	}

	@Override
	public void filter(Primitive p) {
		if (p.isPassThrough()) {
			if (_translate.db)
				_translate.ln("Found pass through primitive: " + p.toString());
			removableComponents.add(p);
		}
	}
}
