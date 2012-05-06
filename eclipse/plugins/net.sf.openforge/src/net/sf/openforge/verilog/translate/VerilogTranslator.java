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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.forge.api.internal.Core;
import net.sf.openforge.forge.api.internal.IPCoreStorage;
import net.sf.openforge.forge.api.ipcore.HDLWriter;
import net.sf.openforge.forge.api.ipcore.IPCore;
import net.sf.openforge.forge.api.pin.Buffer;
import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.InPinBuf;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.OutPinBuf;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.SRL16;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.EndianSwapper;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryWrite;
import net.sf.openforge.lim.op.AddMultiOp;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ConditionalOrOp;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NotOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.OrOpMulti;
import net.sf.openforge.lim.op.PlusOp;
import net.sf.openforge.lim.op.ReductionOrOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.ShortcutIfElseOp;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.XorOp;
import net.sf.openforge.util.IndentWriter;
import net.sf.openforge.util.UserPrintWriter;
import net.sf.openforge.util.VarFilename;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.mapping.MemoryMapper;
import net.sf.openforge.verilog.mapping.memory.VerilogMemory;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Bitwise;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.Replication;
import net.sf.openforge.verilog.model.VerilogDocument;
import net.sf.openforge.verilog.pattern.BitwiseAssignment;
import net.sf.openforge.verilog.pattern.BusOutput;
import net.sf.openforge.verilog.pattern.CallInstance;
import net.sf.openforge.verilog.pattern.CompareOp;
import net.sf.openforge.verilog.pattern.DesignDocument;
import net.sf.openforge.verilog.pattern.DesignModule;
import net.sf.openforge.verilog.pattern.ForgeStatement;
import net.sf.openforge.verilog.pattern.GenericModule;
import net.sf.openforge.verilog.pattern.IPCoreCallInstance;
import net.sf.openforge.verilog.pattern.IncludeStatement;
import net.sf.openforge.verilog.pattern.InferredRegVariant;
import net.sf.openforge.verilog.pattern.LogicalAssignment;
import net.sf.openforge.verilog.pattern.MappedModuleSpecifier;
import net.sf.openforge.verilog.pattern.MathAssignment;
import net.sf.openforge.verilog.pattern.OrManyAssignment;
import net.sf.openforge.verilog.pattern.PortWire;
import net.sf.openforge.verilog.pattern.PrimitiveAssignment;
import net.sf.openforge.verilog.pattern.ProcedureModule;
import net.sf.openforge.verilog.pattern.RegVariant;
import net.sf.openforge.verilog.pattern.SRL16Variant;
import net.sf.openforge.verilog.pattern.TriBufOp;
import net.sf.openforge.verilog.pattern.UnaryOpAssignment;
import net.sf.openforge.verilog.pattern.VEncodedMux;
import net.sf.openforge.verilog.pattern.VMux;

/**
 * VerilogTranslator visits a physically connected LIM. It creates a {link
 * VerilogDocument} from a {@link Design}.
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: VerilogTranslator.java 425 2007-03-07 19:17:39Z imiller $
 */
public class VerilogTranslator extends DefaultVisitor implements Visitor {

	/**
	 * A handle to the VerilogDocument which is where all pieces of the
	 * application will ultimately reside.
	 */
	private VerilogDocument vDoc = null;

	/**
	 * A Map of all the Modules which were defined. Every Module which needs to
	 * get added to the VerilogDocument should be added to this map. Once all
	 * the visiting is done, an Iteration over this map inserts the Modules into
	 * the document in the right order. If modules just inserted themselves
	 * directly into the document (once they were completely defined) then the
	 * document would start with definitions from the lowest level, rather than
	 * from the top.
	 * <P>
	 * Also, this map provides a convenient look-up for existing defintions
	 * related to a procedure (or really any LIM component related to a module).
	 */
	private Map<ID, Module> lim_module_map = new LinkedHashMap<ID, Module>();

	/**
	 * The current Module being populated with calls.
	 */
	private net.sf.openforge.verilog.model.Module current_vmodule;

	/**
	 * A Map of MemoryBank.getSignature() to VerilogMemory used to instantiate
	 * that memory. This eliminates duplicated memory definitions.
	 */
	private Map memoryMap = new HashMap();

	/**
	 * The stack of Modules being visited. This is used to keep a history of
	 * partially assembled modules, since each time a call is visited, the
	 * related procedure visit creates a new module. That new module becomes the
	 * current module to which component visits add their verilog. When that
	 * module definition is complete (control has passed back up to the
	 * procedure visit) the current module reverts back to whatever module was
	 * at the top of the stack.
	 */
	private Stack<Module> module_stack = new Stack<Module>();

	/**
	 * Whether an "application" module should be created for the Design.
	 */
	private boolean suppress_application = false;

	/** This is a set of Modules whose outbufs are to be translated. */
	private Set<net.sf.openforge.lim.Module> xlatOutBufs = new HashSet<net.sf.openforge.lim.Module>();

	private Set<Component> topLevelComponents = Collections.emptySet();

	/** Set of user verilog modules for IP Core that has been written already */
	Set<String> userVerilog_names = new HashSet<String>();

	private ArrayList<String> userSimIncludes = new ArrayList<String>();

	private Design design;

	/**
	 * Creates a new <code>VerilogTranslator</code> instance. Which is to say
	 * that it <b>constructs</b> a VerilogTranslator object which is capable of
	 * visiting a Design. After the visitor has been created, ask it to visit a
	 * Design using visit(Design d). During the visit, the visitor will
	 * construct a {@link VerilogDocument} which may be retrieved with
	 * getDocument(). The VerilogDocument has a few write methods which will
	 * output the verilog.
	 * 
	 */
	public VerilogTranslator() {
		current_vmodule = new net.sf.openforge.verilog.model.Module(
				"genericApp");
	}

	/**
	 * Creates a new <code>VerilogTranslator</code> instance, specifying whether
	 * a top-level "application" module should be generated for the
	 * {@link Design}.
	 * 
	 * @param suppress_application
	 *            whether to generate a module for the Design
	 */
	public VerilogTranslator(boolean suppress_application) {
		this.suppress_application = suppress_application;
	}

	/**
	 * Creates a new <code>VerilogTranslator</code> instance, specifying whether
	 * a top-level "application" module should be generated for the
	 * {@link Design}.
	 * 
	 * @param suppress_application
	 *            whether to generate a module for the Design
	 */
	public VerilogTranslator(Design design, boolean suppress_application) {
		this.suppress_application = suppress_application;
		design.accept(this);
	}

	/**
	 * Creates a new VerilogTranslator which automatically translates a Design
	 * into a VerilogDocument, which may be retrieved with getDocument(), or
	 * sent to an output with writeDocument().
	 */
	public VerilogTranslator(Design design) {
		this(design, false);
	}

	/**
	 * A convenience method which visits a Design with a new VerilogTranslator,
	 * and then outputs the verilog.
	 * 
	 * @param design
	 *            the design to translate
	 * @param writer
	 *            output destination for the verilog
	 */
	public static void translate(Design design, Writer writer) {
		VerilogTranslator vt = new VerilogTranslator(design);
		vt.writeDocument(writer);
	}

	/**
	 * A convenience method which visits a Design with a new VerilogTranslator,
	 * and then outputs the verilog.
	 * 
	 * @param design
	 *            the design to translate
	 * @param writer
	 *            output destination for the verilog
	 */
	public static void translate(Design design, OutputStream writer) {
		VerilogTranslator vt = new VerilogTranslator(design);
		vt.writeDocument(writer);
	}

	@Override
	public void visit(Design design) {
		// _translate.d.launchXGraph(design, false);

		// store off the design for IPCore usage during writing of the document
		this.design = design;

		xlatOutBufs = new HashSet<net.sf.openforge.lim.Module>();
		topLevelComponents = new HashSet<Component>(design.getDesignModule()
				.getComponents());

		/*
		 * Compact all the Values in the Design.
		 */
		ValueCompactor.compact(design);

		vDoc = new DesignDocument(design);
		current_vmodule = null;
		module_stack.clear();
		lim_module_map.clear();
		memoryMap.clear();

		if (!suppress_application) {
			net.sf.openforge.verilog.model.Module design_module = defineDesignModule(design);
			vDoc.append(design_module);
		} else {
			current_vmodule = new DesignModule(design);

			for (Task task : design.getTasks()) {
				task.accept(this);
			}
		}

		// the modules list should have been populated with Module definitions,
		// so now add them to the document
		for (Iterator mods = lim_module_map.entrySet().iterator(); mods
				.hasNext();) {
			Map.Entry me = (Map.Entry) mods.next();
			net.sf.openforge.verilog.model.Module m = (net.sf.openforge.verilog.model.Module) me
					.getValue();
			vDoc.append(m);
		}
	}

	/**
	 * Constructs a new DesignModule based on a design, populating its contents
	 * by visiting the physical components for all top-level resources.
	 */
	private net.sf.openforge.verilog.model.Module defineDesignModule(
			Design design) {
		current_vmodule = new DesignModule(design);

		for (Component comp : design.getDesignModule().getComponents()) {
			Visitable vis = comp;

			if (vis instanceof net.sf.openforge.lim.Module) {
				instantiateModule((net.sf.openforge.lim.Module) vis);
			} else {
				vis.accept(this);
			}
		}

		// how about pins?
		for (Pin pin : design.getPins()) {
			if ((pin.getInPinBuf() != null))
				visit(pin.getInPinBuf());
			if ((pin.getOutPinBuf() != null))
				visit(pin.getOutPinBuf());
		}

		return current_vmodule;
	}

	/**
	 * Constructs a new verilog module based on a generic LIM module, places an
	 * instantiation of the module in the current verilog module, then returns
	 * the newly created module.
	 * 
	 * @param module
	 *            any generic LIM module
	 * @param instanceName
	 *            the name to use for the generic instance
	 * @return a fully populated verilog module
	 */
	private GenericModule instantiateModule(net.sf.openforge.lim.Module module) {
		return instantiateModule(module, ID.showLogical(module));
	}

	/**
	 * Constructs a new verilog module based on a generic LIM module, places an
	 * instantiation of the module in the current verilog module, then returns
	 * the newly created module.
	 * 
	 * @param module
	 *            any generic LIM module
	 * @param name
	 *            the name to use for the generic module
	 * @return a fully populated verilog module
	 */
	private GenericModule instantiateModule(net.sf.openforge.lim.Module module,
			String name) {
		module_stack.push(current_vmodule);
		current_vmodule = new GenericModule(module, name);
		xlatOutBufs.add(module);
		for (Component component : module.getComponents()) {
			((Visitable) component).accept(this);
		}
		lim_module_map.put(module, current_vmodule);
		GenericModule generic = (GenericModule) current_vmodule;
		current_vmodule = module_stack.pop();
		current_vmodule.state(generic.makeInstance());
		return generic;
	}

	/**
	 * Visits the {@link net.sf.openforge.lim.Module Modules} components as if
	 * they were instantiated in the current scope, by ignoring its in/out bufs.
	 */
	@SuppressWarnings("unused")
	private void popModuleComponents(net.sf.openforge.lim.Module mod) {
		Collection<Component> components = mod.getComponents();
		components.remove(mod.getInBuf());
		components.removeAll(mod.getOutBufs());
		for (Component component : components) {
			Visitable vis = component;
			vis.accept(this);
		}
	}

	@Override
	public void visit(Task task) {
		if (task.getCall() != null) {
			task.getCall().accept(this);
		}
	}

	/**
	 * only needs a module instantiation for an IPCore call. translation of
	 * module is not needed.
	 * 
	 * @param call
	 *            a value of type 'IPCoreCall'
	 */
	@Override
	public void visit(IPCoreCall call) {
		IPCoreCallInstance ci = new IPCoreCallInstance(call);
		current_vmodule.state(ci);
	}

	/**
	 * Except for top-level calls (identified by Calls without an owner),
	 * creates an instantiation of a Module, then visits the Procedure to
	 * produce the Module definition.
	 * 
	 * @param call
	 *            a value of type 'Call'
	 */
	@Override
	public void visit(Call call) {
		// first, visit the Procedure to create a Module which can be called
		assert (call.getProcedure() != null) : "Call to non-existant procedure";

		if (topLevelComponents.contains(call) || !isEmptyModuleCall(call)) {
			call.getProcedure().accept(this);
			assert (lim_module_map.get(call.getProcedure()) != null) : "Module not created for call's procedure";
		}

		if ((topLevelComponents.contains(call)) && suppress_application) {
			// presumed to be a call to a top-level entry method
			vDoc.append(new Comment("Entry Method Call: "
					+ Integer.toHexString(call.hashCode())));
		} else {
			if (topLevelComponents.contains(call) || !isEmptyModuleCall(call)) {
				//
				// Add the instantiation to this module.
				//

				// current_vmodule.state(new InlineComment("Call to: " +
				// Integer.toHexString(call.hashCode())));

				//
				// Insert the actual 'instantiation' of the module into
				// the current Module here.
				//
				CallInstance ci = new CallInstance(call);

				// Now instantiate the module
				current_vmodule.state(ci);

				//
				// End module instantiation
			}
		}

	} // visit(Call)

	@Override
	public void visit(Procedure procedure) {
		if (!lim_module_map.containsKey(procedure)) {
			module_stack.push(current_vmodule);
			current_vmodule = new ProcedureModule(procedure);
			lim_module_map.put(procedure, current_vmodule);
			xlatOutBufs.add(procedure.getBody());
			procedure.getBody().accept(this);
			current_vmodule = module_stack.pop();
		}
	} // visit(Procedure)

	@Override
	public void visit(Reg reg) {
		reg.updateResetType();
		if (reg.hardInstantiate())
			current_vmodule.state(new RegVariant(reg));
		else
			current_vmodule.state(new InferredRegVariant(reg));
	}

	@Override
	public void visit(SRL16 srl_16) {
		current_vmodule.state(new SRL16Variant(srl_16));
	}

	@Override
	public void visit(Latch latch) {
		for (Component component : latch.getComponents()) {
			component.accept(this);
		}
	}

	@Override
	public void visit(PinReferee pref) {
		for (Component component : pref.getComponents()) {
			component.accept(this);
		}
	}

	// public void visit (Kicker kicker)
	// {
	// instantiateModule(kicker);
	// assert false : "Kicker should be translated as generic module";

	// for(Iterator it=kicker.getComponents().iterator();it.hasNext();)
	// {
	// Component c = (Component)it.next();
	// current_vmodule.state(new InlineComment("KICKER: "+c, Comment.SHORT));
	// c.accept(this);
	// }
	// }

	@Override
	public void visit(Scoreboard scoreboard) {
		for (Component component : scoreboard.getComponents()) {
			component.accept(this);
		}
	}

	@Override
	public void visit(EndianSwapper endianSwapper) {
		instantiateModule(endianSwapper);
		/*
		 * for(Iterator
		 * it=endianSwapper.getComponents().iterator();it.hasNext();) {
		 * Component c=(Component)it.next(); c.accept(this); }
		 */
	}

	@Override
	public void visit(AddOp add) {
		if (add.hasMulti())
			current_vmodule
					.state(new MathAssignment.AddMulti((AddMultiOp) add));
		else
			current_vmodule.state(new MathAssignment.Add(add));
	}

	@Override
	public void visit(AndOp and) {
		current_vmodule.state(new BitwiseAssignment.And(and));
	}

	@Override
	public void visit(NumericPromotionOp numericPromotion) {
		// current_vmodule.state(new
		// UnaryOpAssignment.SignExtend(numericPromotion));
	}

	@Override
	public void visit(CastOp cast) {
		// current_vmodule.state(new UnaryOpAssignment.SignExtend(cast));
	}

	@Override
	public void visit(ComplementOp complement) {
		current_vmodule.state(new UnaryOpAssignment.Negate(complement));
	}

	@Override
	public void visit(ConditionalAndOp conditionalAnd) {
		current_vmodule.state(new LogicalAssignment.And(conditionalAnd));
	}

	@Override
	public void visit(ConditionalOrOp conditionalOr) {
		current_vmodule.state(new LogicalAssignment.Or(conditionalOr));
	}

	@Override
	public void visit(net.sf.openforge.lim.op.Constant constant) {
		// Constant is a no-op. The bus attached to the constant
		// should have a constant value which will end up being
		// used by a BusWire to represent the bus instead of a name
	}

	@Override
	public void visit(DivideOp divide) {
		current_vmodule.state(new MathAssignment.Divide(divide));
	}

	@Override
	public void visit(EqualsOp equals) {
		current_vmodule.state(new CompareOp.Equals(equals));
	}

	@Override
	public void visit(EncodedMux mux) {
		if (mux.getDataPorts().size() == 2) {
			current_vmodule.state(new VMux(mux));
		} else {
			current_vmodule.state(new VEncodedMux(mux));
		}
	}

	@Override
	public void visit(GreaterThanEqualToOp greaterThanEqualTo) {
		current_vmodule.state(new CompareOp.GreaterThanEqualTo(
				greaterThanEqualTo));
	}

	@Override
	public void visit(GreaterThanOp greaterThan) {
		current_vmodule.state(new CompareOp.GreaterThan(greaterThan));
	}

	@Override
	public void visit(LeftShiftOp leftShift) {
		current_vmodule
				.state(new net.sf.openforge.verilog.pattern.ShiftOp.Left(
						leftShift));
	}

	@Override
	public void visit(LessThanEqualToOp lessThanEqualTo) {
		current_vmodule.state(new CompareOp.LessThanEqualTo(lessThanEqualTo));
	}

	@Override
	public void visit(LessThanOp lessThan) {
		current_vmodule.state(new CompareOp.LessThan(lessThan));
	}

	@Override
	public void visit(MinusOp minus) {
		current_vmodule.state(new UnaryOpAssignment.Minus(minus));
	}

	@Override
	public void visit(ModuloOp modulo) {
		current_vmodule.state(new MathAssignment.Modulo(modulo));
	}

	@Override
	public void visit(MultiplyOp multiply) {
		current_vmodule.state(new MathAssignment.Multiply(multiply));
	}

	@Override
	public void visit(NoOp nop) {
	}

	@Override
	public void visit(NotEqualsOp notEquals) {
		current_vmodule.state(new CompareOp.NotEquals(notEquals));
	}

	@Override
	public void visit(NotOp not) {
		current_vmodule.state(new UnaryOpAssignment.Not(not));
	}

	@Override
	public void visit(TriBuf tbuf) {
		current_vmodule.state(new TriBufOp(tbuf));
	}

	@Override
	public void visit(OrOp or) {
		if (or instanceof OrOpMulti) {
			current_vmodule.state(new OrManyAssignment((OrOpMulti) or));
		} else {
			current_vmodule.state(new BitwiseAssignment.Or(or));
		}
	}

	@Override
	public void visit(PlusOp plus) {
		current_vmodule.state(new UnaryOpAssignment.SignExtend(plus));
	}

	@Override
	public void visit(ReductionOrOp reductionOrOp) {
		current_vmodule.state(new UnaryOpAssignment.Or(reductionOrOp));
	}

	@Override
	public void visit(RightShiftOp rightShift) {
		current_vmodule
				.state(new net.sf.openforge.verilog.pattern.ShiftOp.Right(
						rightShift));
	}

	@Override
	public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
		current_vmodule
				.state(new net.sf.openforge.verilog.pattern.ShiftOp.RightUnsigned(
						rightShiftUnsigned));
	}

	@Override
	public void visit(SimplePinRead comp) {
		// We translate the simple pin read as a simple assignment of
		// its result bus from its sideband pin. This should never be
		// necessary however because there is never any logic
		// associated with this, constant prop should pass the bits
		// straight through.
		current_vmodule.state(new ForgeStatement(Collections.<Net> emptySet(),
				new Assign.Continuous(NetFactory.makeNet(comp.getResultBus()),
						new PortWire(comp.getDataPorts().get(0)))));

		super.visit(comp);
	}

	/**
	 * Translates the {@link SimplePinWrite} according to the structure
	 * specified by the javadoc which is:
	 * <code>assign bus = {width{compGO}} &amp; compData;</code>
	 * 
	 * @param comp
	 *            a value of type 'SimplePinWrite'
	 */
	@Override
	public void visit(SimplePinWrite comp) {
		// According to the JavaDoc for SimplePinWrite the behavior is:
		// assign bus = {width{compGO}} & compData;
		// or, if the node does not consume GO:
		// assign bus = compData;

		// current_vmodule.state(new
		// InlineComment("SIMPLEPINWRITE",Comment.SHORT));
		final Net busWire = NetFactory.makeNet(comp.getExit(Exit.DONE)
				.getDataBuses().get(0));
		final PortWire portWire = new PortWire(comp.getDataPort());
		final Expression rightHandSide;
		if (comp.consumesGo()) {
			final Replication mask = new Replication(portWire.getWidth(),
					new PortWire(comp.getGoPort()));
			rightHandSide = new Bitwise.And(portWire, mask);
		} else {
			rightHandSide = portWire;
		}

		current_vmodule.state(new ForgeStatement(
				Collections.singleton(busWire), new Assign.Continuous(busWire,
						rightHandSide)));
		super.visit(comp);
	}

	@Override
	public void visit(ShortcutIfElseOp shortcutIfElse) {
		current_vmodule.state(new VMux(shortcutIfElse));
	}

	@Override
	public void visit(SubtractOp subtract) {
		current_vmodule.state(new MathAssignment.Subtract(subtract));
	}

	@Override
	public void visit(XorOp xor) {
		current_vmodule.state(new BitwiseAssignment.Xor(xor));
	}

	@Override
	public void visit(Mux mux) {
		current_vmodule.state(new VMux(mux));
	}

	@Override
	public void visit(Or or) {
		current_vmodule.state(new PrimitiveAssignment.Or(or));
	}

	@Override
	public void visit(And and) {
		current_vmodule.state(new PrimitiveAssignment.And(and));
	}

	@Override
	public void visit(Not not) {

		current_vmodule.state(new PrimitiveAssignment.Not(not));
	}

	public void visit(InPinBuf inPinBuf) {
		InPinBuf.Physical physical = inPinBuf.getPhysicalComponent();
		if (physical != null) {
			Collection<Component> components = physical.getComponents();
			components.remove(physical.getInBuf());
			components.removeAll(physical.getOutBufs());
			for (Component component : components) {
				((Visitable) component).accept(this);
			}
		}
	}

	public void visit(OutPinBuf outPinBuf) {
		OutPinBuf.Physical physical = outPinBuf.getPhysicalComponent();
		if (physical != null) {
			Collection<Component> components = physical.getComponents();
			components.remove(physical.getInBuf());
			components.removeAll(physical.getOutBufs());
			for (Component component : components) {
				Visitable v = component;
				v.accept(this);
			}
		}
	}

	@Override
	public void visit(MemoryBank memBank) {
		// Object sig = memBank.getSignature();
		VerilogMemory vm = (VerilogMemory) memoryMap
				.get(memBank.getSignature());
		if (vm == null) {
			vm = MemoryMapper.getMemoryType(memBank);
			net.sf.openforge.verilog.model.Module memoryModule = vm
					.defineModule();
			lim_module_map.put(memBank, memoryModule);
			memoryMap.put(memBank.getSignature(), vm);
		}

		current_vmodule.state(vm.instantiate(memBank));
	}

	@Override
	public void visit(MemoryRead memoryRead) {
		MemoryRead.Physical physical = (MemoryRead.Physical) memoryRead
				.getPhysicalComponent();
		Collection<Component> components = physical.getComponents();
		components.remove(physical.getInBuf());
		components.removeAll(physical.getOutBufs());
		for (Component component : components) {
			((Visitable) component).accept(this);
		}
	}

	@Override
	public void visit(MemoryWrite memoryWrite) {
		MemoryWrite.Physical physical = (MemoryWrite.Physical) memoryWrite
				.getPhysicalComponent();
		Collection<Component> components = physical.getComponents();
		components.remove(physical.getInBuf());
		components.removeAll(physical.getOutBufs());
		for (Component component : components) {
			((Visitable) component).accept(this);
		}
	}

	@Override
	public void visit(OutBuf ob) {
		// ABK FIXME - This is ugly, but checking for instanceof is needed
		// here because some ownerless modules are in the graph which are
		// not a procedure body. Previously checked for getOwner().getOwner()
		// being null. Perhaps isProcedureBody could get promoted to Module,
		// and any Module (rather than just a block) can be a procedure's body?
		// Also, fabricated Module (Register.Physical and such) should have a
		// legitmate owner set.
		net.sf.openforge.lim.Module owner = ob.getOwner();
		// if ((owner instanceof Block) && (((Block)owner).isProcedureBody()))
		if (xlatOutBufs.contains(owner)) {
			for (Port ob_port : ob.getPorts()) {
				if (ob_port.isUsed()) {
					current_vmodule.state(new ForgeStatement(Collections
							.<Net> emptySet(), new Assign.Continuous(
							new BusOutput(ob_port.getPeer()), new PortWire(
									ob_port))));
				}
			}
		}
	}

	/**
	 * Gets the VerilogDocument generated from the most recent visit of a
	 * Design.
	 * 
	 * @return the translated Design as a VerilogDocument
	 */
	public VerilogDocument getDocument() {
		if (vDoc == null) {
			vDoc = new VerilogDocument();
			vDoc.append(new Comment(
					"Verilog Document NOT generated from a Design"));
		}
		return vDoc;
	}

	/**
	 * Writes the generated VerilogDocument to an output.
	 * 
	 * @param writer
	 *            the output for the verilog
	 */
	public void writeDocument(Writer writer) {
		PrettyPrinter pp = new PrettyPrinter(writer);
		pp.print(getDocument());
		writeIPCore(pp.writer);
	}

	/**
	 * Writes the generated VerilogDocument to an output.
	 * 
	 * @param writer
	 *            the output for the verilog
	 */
	public void writeDocument(OutputStream writer) {
		PrettyPrinter pp = new PrettyPrinter(writer);
		pp.print(getDocument());
		writeIPCore(pp.writer);
	}

	public void outputSimInclude(File vFile, OutputStream simOS) {
		writeIncludeDocument(simOS, new IncludeStatement(vFile.getPath()), true);
	}

	public void outputSynthInclude(File vFile, OutputStream simOS) {
		IncludeStatement inclStatement = (vFile == null) ? null
				: new IncludeStatement(vFile.getPath());
		writeIncludeDocument(simOS, inclStatement, false);
	}

	private void writeIncludeDocument(OutputStream os,
			IncludeStatement hdlFile, boolean sim) {
		// create verilog doc
		VerilogDocument includeDoc = new VerilogDocument();
		// VerilogDocument synthdoc = new VerilogDocument();

		if (hdlFile != null) {
			// include forge generated file intself
			includeDoc.append(new Comment("Forge-generated Verilog",
					Comment.SHORT));
			// includeDoc.append(new IncludeStatement(vFile.getPath()));
			includeDoc.append(hdlFile);
		}
		includeDoc.append(new Comment(Comment.BLANK));

		Map<String, ArrayList<MappedModule>> incls = getIncludes(sim);

		// generate the include statements
		for (Iterator<String> it = incls.keySet().iterator(); it.hasNext();) {
			ArrayList<MappedModule> mmList = incls.get(it.next());
			if (mmList.size() > 0) {
				String comment = "primitive mapping for ";
				MappedModule mm = null;
				for (MappedModule mappedModule : mmList) {
					mm = mappedModule;
					includeDoc.append(new Comment(comment + mm.getModuleName(),
							Comment.SHORT));
					comment = " and ";
				}
				if (sim)
					includeDoc.append(new IncludeStatement(mm.getSimInclude()));
				else
					includeDoc
							.append(new IncludeStatement(mm.getSynthInclude()));

				includeDoc.append(new Comment(Comment.BLANK));
			}
		}

		if (sim) {
			// Always write out glbl.v include for _sim file so
			// automatic test bench doesn't need to conditionally add
			// logic to assert GSR.
			includeDoc
					.append(new Comment("global declarations", Comment.SHORT));
			includeDoc.append(new IncludeStatement(VarFilename
					.parse("$XILINX/verilog/src/glbl.v")));
			includeDoc.append(new Comment(Comment.BLANK));
		}

		try {
			PrintWriter pwSim = new PrintWriter(os);
			(new PrettyPrinter(pwSim)).print(includeDoc);
			pwSim.close();
		} catch (Exception e) {
			EngineThread.getEngine().fatalError(e.getMessage());
		}
	}

	private Map<String, ArrayList<MappedModule>> getIncludes(boolean sim) {
		Map<String, ArrayList<MappedModule>> incls = new HashMap<String, ArrayList<MappedModule>>();

		MappedModuleSpecifier mms = (MappedModuleSpecifier) getDocument();

		// this gets a set of sim and synth includes, uniquyfying them
		for (MappedModule mm : mms.getMappedModules()) {

			// keep a list of everyone who used this
			String key = sim ? mm.getSimInclude() : mm.getSynthInclude();
			ArrayList<MappedModule> mmList = incls.get(key);
			if (mmList == null) {
				mmList = new ArrayList<MappedModule>(2);
			}
			mmList.add(mm);
			incls.put(key, mmList);
		}

		// Deprecated???
		for (String usersim : userSimIncludes) {

			String sim_include_file = MemoryMapper.SIM_INCLUDE_PATH + usersim
					+ ".v";
			// doesn't hurt to include the unisim synth stuff for all
			// the above cases.
			String synth_include_file = MemoryMapper.SYNTH_INCLUDE_PATH
					+ "unisim_comp.v";

			if (usersim.startsWith("$")) {
				// assume environment variable, leave as a string so
				// it will get expanded, also have it included for
				// synthesis since it is a file
				sim_include_file = usersim;
				synth_include_file = usersim;
			} else {

				File f = new File(usersim);
				if (!f.getName().equals(usersim)) {
					// the supplied name has directory prefixes, use it
					// directly, don't pre-pend the sim include stuff
					// and also set the synth to include it.
					sim_include_file = f.getAbsolutePath();
					synth_include_file = f.getAbsolutePath();
				} else if (usersim.startsWith("X_")) {
					// handle as a simprim, not a unisim
					sim_include_file = MemoryMapper.SIMPRIM_INCLUDE_PATH
							+ usersim + ".v";
				}
			}

			MappedModule mm = new MappedModule("HDLWriter", sim_include_file,
					synth_include_file);

			// add our new mapped module to the list
			String key = sim ? mm.getSimInclude() : mm.getSynthInclude();
			ArrayList<MappedModule> mmList = incls.get(key);
			if (mmList == null) {
				mmList = new ArrayList<MappedModule>(2);
			}
			mmList.add(mm);
			incls.put(key, mmList);
		}

		return incls;
	}

	public void writeIPCore(IndentWriter printer) {
		// List of IPCores that has HDLWriters registered that were
		// used in the design. It is possible for the user's code to
		// have instantiated many IPCores, but only reference a few,
		// so we need to only call the writers for the cores that they
		// did reference.
		final ArrayList<IPCore> userVerilog_list = new ArrayList<IPCore>();

		// all the IPCoreStorage that we created with user's IPCores
		for (Pin pin : design.getPins()) {
			Buffer buf = pin.getApiPin();

			if (Core.hasThisPin(buf)) {
				IPCoreStorage ipcs = Core.getIPCoreStorage(buf);
				IPCore ipcore = Core.getIPCore(ipcs);

				// If IPCore's name exists in the user Verilog name list,
				// it means that multiple IPCore instantiation occurs so
				// we just need to register the writer once.

				// if(Core.getFromHDLWriterMap(ipcs) != null &&
				// userVerilog_names.add(ipcs.getModuleName()))
				if (ipcs.getHDLWriter() != null
						&& userVerilog_names.add(ipcs.getModuleName())) {
					// save those IPCores that has registered writers only
					userVerilog_list.add(ipcore);
				}
			}
		}

		if (!userVerilog_list.isEmpty()) {
			printer.println("// ========= User Verilog Module(s) ========= \n");
		}

		// Iterate thru each IPCore that has registered HDLWriter
		for (IPCore ipcore : userVerilog_list) {

			// Get the IPCoreStorage that is associated with this IPCore
			IPCoreStorage ipcs = Core.getIPCoreStorage(ipcore);

			// Now yank out the HDLWriter
			// HDLWriter hdlWriter = (HDLWriter)Core.getFromHDLWriterMap(ipcs);
			HDLWriter hdlWriter = ipcs.getHDLWriter();

			// If there's a HDLWriter registered, write it
			if (hdlWriter != null) {
				UserPrintWriter upw = new UserPrintWriter(printer);

				List<String> unisimlist = hdlWriter.writeVerilog(ipcore, upw);

				if (unisimlist != null) {

					// add the unisims if proper Strings to the includes
					// of the simulation Verilog file.

					for (Iterator<String> unisims = unisimlist.iterator(); unisims
							.hasNext();) {
						Object o = null;

						try {
							o = unisims.next();

							String s = (String) o;

							userSimIncludes.add(s);
						} catch (Throwable t) {
							EngineThread.getGenericJob().error(
									"HDLWriter returned bad unisim: " + o);
						}
					}
				}

				printer.println("");
			}
		}
	}

	/**
	 * Checks whether a call references a empty module or not.
	 * 
	 * @return true if there is at least one data port or one data bus, else
	 *         false
	 */
	private boolean isEmptyModuleCall(Call call) {
		return call.getDataPorts().isEmpty() && call.getDataBuses().isEmpty()
				&& !call.producesDone();
	}

} // class VerilogTranslator

