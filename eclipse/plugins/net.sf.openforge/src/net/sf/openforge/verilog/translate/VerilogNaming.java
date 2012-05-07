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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.BidirectionalPin;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.FilteredVisitor;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.Primitive;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.UnexpectedVisitationException;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDSourceInfo;
import net.sf.openforge.verilog.model.Keyword;

/**
 * VerilogNaming resolves any naming problems (scope conflicts and illegal
 * names) in a LIM model prior to translation into verilog.
 * <P>
 * 
 * @author abk
 * @version $Id: VerilogNaming.java 23 2005-09-09 18:45:32Z imiller $
 * @see Visitable
 */
public class VerilogNaming extends FilteredVisitor {

	private final Stack scope_stack = new Stack();

	private Set<String> current_scope = null;

	private final Set<ID> recorded = new HashSet<ID>();

	@SuppressWarnings("unused")
	private Design current_design = null;

	private boolean doLongNames = false;

	/**
	 * stores unique Procedure names, uses all UPERCASE to catch same spelling
	 * but different capitlization which drives XST nuts and causes it to fail!
	 **/
	private final Set<String> uniquified = new HashSet<String>();

	public VerilogNaming() {
		doLongNames = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(
						OptionRegistry.LONG_VERILOG_NAMES);
	}

	/**
	 * Starts a new namespace scope.
	 */
	private void startScope() {
		scope_stack.push(current_scope);
		current_scope = new HashSet<String>();
	}

	/**
	 * Ends the current scope, dropping back to the previous scope.
	 */
	private void endScope() {
		current_scope = (Set) scope_stack.pop();
	}

	private void recordName(ID id) {
		recordName(id, null);
	}

	/**
	 * Records the uniquefied verilog name for an ID within the current scope.
	 * In addition to renaming ID's which have duplicate names in the current
	 * scope, names which are verilog keywords are changed to be non-keywords.
	 * 
	 * @param id
	 *            the ID to be recorded
	 * @param prefix
	 *            a possible prefix for more explicitly naming the ID
	 */
	private void recordName(ID id, String prefix) {
		if (prefix != null && prefix.length() > 0) {
			prefix = ID.toVerilogIdentifier(prefix);
		}

		if (!recorded.contains(id)) {
			String name = toVerilogName(id);
			if (current_scope.contains(name) || Keyword.isKeyword(name)) {
				if ((prefix != null) && !name.startsWith(prefix)) {
					name = prefix + "_" + name;
					id.setIDLogical(name);
					if (current_scope.contains(name)) {
						rename(id);
					}
				} else {
					rename(id);
				}
			}
			current_scope.add(toVerilogName(id));
			recorded.add(id);
		}
		// System.out.println("recordName(" + ID.showDebug(id) + ") named " +
		// ID.showLogical(id));
	}

	/**
	 * Special case for uniquifying a Procedure name
	 * 
	 * @param proc
	 *            the procedure to be recorded
	 */
	private void recordModuleName(ID id) {
		/*
		 * We need to check every procedure for name clashes. XXX: Scoping
		 * doesn't apply (?)
		 */
		String procName = toVerilogName(id);
		if (uniquified.contains(procName.toUpperCase())
				|| Keyword.isKeyword(procName)) {
			rename(id);
		}
		uniquified.add(toVerilogName(id).toUpperCase());
		// System.out.println("recordName(" + ID.showDebug(proc) + ")" +
		// " started as \"" + procName + "\" became \"" + toVerilogName(proc) +
		// "\"");
	}

	/**
	 * Special handling for design level input/output pin name uniquifying.
	 * Uniquify by following this format: name_design_method
	 * 
	 * @param name_map
	 *            "pin name" => "pins"
	 * @param design
	 *            current design
	 */
	private void recordName(HashMap<String, List<Pin>> name_map, Design design) {
		Set<String> key_set = name_map.keySet();

		for (String name : key_set) {
			List<Pin> pin_list = name_map.get(name);
			// if((pin_list.size() > 1) || (design.getTasks().size() > 1))
			if (pin_list.size() > 1) {
				for (Iterator<Pin> pinIter = pin_list.iterator(); pinIter
						.hasNext();) {
					Pin pin = pinIter.next();

					if (!recorded.contains(pin)) {
						String verilog_name = toVerilogName(pin);

						if (verilog_name.equals("CLK")
								|| verilog_name.equals("RESET")) {
							break;
						}

						IDSourceInfo info = pin.getIDSourceInfo();
						String namePrefix = (info != null) ? info
								.getFullyQualifiedName() : "";

						recordName(pin, namePrefix);
					}

					// System.out.println("RENAMED PIN " + pin + " to " +
					// ID.showLogical(pin));
				}
			} else {
				Pin pin = pin_list.get(0);
				IDSourceInfo prefix = pin.getIDSourceInfo();
				recordName(pin,
						(prefix != null) ? prefix.getFullyQualifiedName() : "");
			}
		}
	}

	/**
	 * Modifies the IDNamed object to give it a unique name within the current
	 * scope. The uniqification suffix must not produce variants of a name with
	 * only _# syntax since XST ends up with name conflicts if you have NAME and
	 * NAME_# defined, since it expands the bits in NAME into individual signals
	 * by adding the suffix _bit# to each bit of NAME and thereby conflicts with
	 * the user defined signal NAME_#. The use of "__" here doesn't work either,
	 * I suspect someone is pruning multiple underscores, so we use _u# to
	 * identify the name as a unique version.
	 * 
	 * @param renamed
	 *            the object to be renamed
	 */
	private void rename(ID id) {
		String previous_name = toVerilogName(id);
		id.setIDLogical(previous_name + "_u" + ID.getNextID(previous_name));
		// String new_name = toVerilogName(id);
		// EngineThread.getGenericJob().verbose("Renamed \"" + previous_name +
		// "\" to \"" + new_name + "\"");
	}

	/**
	 * Returns the verilog name of an ID, based on the ID's logical name.
	 */
	private String toVerilogName(ID id) {
		return ID.toVerilogIdentifier(ID.showLogical(id));
	}

	/**
	 * Create a map with mapping of "name => pins"
	 * 
	 * @param design
	 *            current design
	 * @return a hash map with mapping of "Pin Name" =>
	 *         "A List of Pins That Share The Same Pin Name"
	 */
	private HashMap<String, List<Pin>> iteratePins(Design design) {
		Map<String, List<Pin>> name_map = new HashMap<String, List<Pin>>();

		for (Pin pin : design.getPins()) {
			String name = toVerilogName(pin);
			if (name_map.containsKey(name)) {
				((ArrayList<Pin>) name_map.get(name)).add(pin);
			} else {
				List<Pin> list = new ArrayList<Pin>();
				list.add(pin);
				name_map.put(name, list);
			}
		}

		return (HashMap<String, List<Pin>>) name_map;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void visit(Design design) {
		startScope();
		current_design = design;

		// check the design itself
		recordModuleName(design);

		// check the pins
		recordName(iteratePins(design), design);
		// for (Iterator it = design.getInputPins().iterator(); it.hasNext();)
		// {
		// // Since RegisterReferee and other stuff @ the top level
		// // will use the input pin's bus for naming internally we
		// // need to be sure that we name it only once. To do that
		// // we have to use the pin's bus here and also in
		// // InputPinPort
		// InputPin pin = (InputPin)it.next();
		// //recordName(pin, pin.getIDPrefix());
		// recordName(pin.getBus(), pin.getIDPrefix());
		// }
		// for (Iterator it = design.getOutputPins().iterator(); it.hasNext();)
		// {
		// OutputPin pin = (OutputPin)it.next();
		// recordName(pin);
		// }
		for (Pin p : design.getBidirectionalPins()) {
			BidirectionalPin pin = (BidirectionalPin) p;
			recordName(pin);
		}

		// visit the global components' physical implementations
		LinkedList<Component> comps = new LinkedList<Component>(design
				.getDesignModule().getComponents());
		while (!comps.isEmpty()) {
			Visitable vis = comps.remove(0);
			try {
				vis.accept(this);
			} catch (UnexpectedVisitationException uve) {
				if (vis instanceof net.sf.openforge.lim.Module) {
					comps.addAll(((net.sf.openforge.lim.Module) vis)
							.getComponents());
				} else {
					System.out.println("(UVE) Verilog naming " + vis);
					throw uve;
				}
			}
		}

		// The visit of the design module components is equivalent to
		// a traversal
		// traverse(design);
		current_design = null;
		endScope();
	}

	@Override
	public void visit(Procedure proc) {
		startScope();
		recordModuleName(proc);
		super.visit(proc);
		endScope();
	}

	@Override
	public void preFilter(Call call) {
		recordName(call);
	}

	/**
	 * Handle any kind of {@link Primitive}. All visit(Primitive subclass)
	 * methods should call this as part of the visit.
	 */
	@Override
	public void filter(Primitive p) {
		// first make sure the Primitive itself has a name that you like,
		// and that it is unique in this scope
		if (doLongNames) {
			p.setIDLogical(ID.showLogical(p) + "_"
					+ Integer.toHexString(System.identityHashCode(p)));
		}

		recordName(p);

		for (Exit exit : p.getExits()) {
			for (Bus bus : exit.getBuses()) {
				if (bus.isUsed()) {
					bus.setIDLogical(ID.showLogical(p));
					recordName(bus);
				}
			}
		}
	}

	@Override
	public void filterAny(Component c) {
		String base = ID.showLogical(c) + "_"
				+ Integer.toHexString(System.identityHashCode(c));
		for (Exit exit : c.getExits()) {
			for (Bus bus : exit.getBuses()) {
				if (bus.isUsed()) {
					// recordName(bus.getSource());
					// Changed to name EVERY bus due to a problem with
					// the way that ProcedureModule builds it's
					// interface. ProcedureModule uses the procedure
					// body's output buses directly to name it's
					// ports. This caused a naming conflict when
					// accessing 2 fields with the same name in
					// different classes (which has since been fixed
					// in RegisterBuilder by uniquifying register
					// names) but could creep up in other cases as
					// well. Now we name all buses.
					if (doLongNames) {
						bus.setIDLogical(ID.showLogical(bus) + "_" + base);
					}
					recordName(bus);
				}
			}
		}
	}
}
