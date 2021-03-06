/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */

package org.xronos.orcc.backend.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.llvm.tta.transform.PrintRemoval;
import net.sf.orcc.backends.transform.CastAdder;
import net.sf.orcc.backends.transform.GlobalArrayInitializer;
import net.sf.orcc.backends.transform.Inliner;
import net.sf.orcc.backends.transform.LocalArrayRemoval;
import net.sf.orcc.backends.transform.LoopUnrolling;
import net.sf.orcc.backends.transform.StoreOnceTransformation;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.transform.UnitImporter;
import net.sf.orcc.df.util.DfSwitch;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.CfgNode;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.transform.BlockCombine;
import net.sf.orcc.ir.transform.ControlFlowAnalyzer;
import net.sf.orcc.ir.transform.DeadGlobalElimination;
import net.sf.orcc.ir.transform.DeadVariableRemoval;
import net.sf.orcc.ir.transform.SSAVariableRenamer;
import net.sf.orcc.util.OrccLogger;
import net.sf.orcc.util.Void;

import org.xronos.orcc.analysis.NativeProcedureFinder;
import org.xronos.orcc.backend.transform.pipelining.Pipelining;
import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.visitors.XronosScheduler;
import org.xronos.orcc.forge.transform.analysis.Liveness;
import org.xronos.orcc.forge.transform.analysis.UninitializedVariable;
import org.xronos.orcc.forge.transform.analysis.XronosCFG;
import org.xronos.orcc.forge.transform.io.SinglePortList;
import org.xronos.orcc.forge.transform.io.SinglePortReadWrite;
import org.xronos.orcc.forge.transform.memory.RedundantLoadElimination;
import org.xronos.orcc.forge.transform.memory.VarInitializer;

/**
 * This helper class transforms only a given procedure
 * 
 * @author Endri Bezati
 * 
 */
public class XronosTransform {

	private static boolean debug = false;

	public static void transformActor(Actor actor, Map<String, Object> options, Boolean debugMode) {
		List<DfSwitch<?>> transformations = new ArrayList<DfSwitch<?>>();
		transformations.add(new UnitImporter());
		transformations.add(new VarInitializer());
		transformations.add(new SinglePortReadWrite());
		// transformations.add(new CheckVarSize());
		// transformations.add(new DfVisitor<Void>(new Inliner(false, true,
		// true)));
		transformations.add(new ParameterArrayRemoval());
		transformations.add(new DfVisitor<Object>(new XronosConstantFolding()));
		transformations.add(new DfVisitor<Void>(new DeadVariableRemoval()));
		transformations.add(new DfVisitor<Void>(new XronosConstantPropagation()));
		transformations.add(new DeadActionEliminaton(true));
		transformations.add(new DfVisitor<Void>(new XronosDeadCodeElimination(true, false)));
		transformations.add(new DfVisitor<Void>(new IndexFlattener()));
		// transformations.add(new DfVisitor<Void>(new
		// ScalarRedundancyElimination()));
		transformations.add(new DfVisitor<Void>(new BlockCombine(false)));
		transformations.add(new PrintRemoval());
		// transformations.add(new DfVisitor<Void>(new DeadVariableRemoval()));
		transformations.add(new DfVisitor<Void>(new RedundantLoadElimination()));
		transformations.add(new SinglePortList());
		transformations.add(new XronosDivision());
		transformations.add(new DfVisitor<CfgNode>(new XronosCFG()));
		transformations.add(new DfVisitor<Void>(new Liveness()));
		transformations.add(new DfVisitor<Void>(new UninitializedVariable(true)));
		for (DfSwitch<?> transformation : transformations) {
			try {
				long t0 = System.currentTimeMillis();
				transformation.doSwitch(actor);
				long t1 = System.currentTimeMillis();
				if (debug) {
					System.out.println(("\t -Transformation: " + transformation.toString() + " took: "
							+ (float) (t1 - t0) / 1000 + "s"));
				}
			} catch (NullPointerException ex) {

				OrccLogger.severeln("\t - transformation failed: NullPointerException, " + ex.getMessage());
				break;
			}
		}
	}

	public static void transformActor(Actor actor, Map<String, Object> options, ResourceCache resourceCache,
			Boolean portTransformation, boolean schedulerInformation, Boolean debugMode) {
		if (!actor.hasAttribute("xronos_no_generation")) {
			List<DfSwitch<?>> transformations = new ArrayList<DfSwitch<?>>();

			transformations.add(new UnitImporter());

			// transformations.add(new DfVisitor<Void>(new Liveness()));
			Boolean sizeArrayOfPowerOfTwo = options.get("org.xronos.orcc.arraySizeToPowerOfTwo") != null ? (Boolean) options
					.get("org.xronos.orcc.arraySizeToPowerOfTwo") : false;
			if (sizeArrayOfPowerOfTwo) {
				transformations.add(new ArraySizeToPowerOfTwo());
			}
			transformations.add(new XronosVarInitializer());
			// transformations.add(new CheckVarSize());
			transformations.add(new ParameterArrayRemoval());
			transformations.add(new DfVisitor<Void>(new XronosConstantPropagation()));
			transformations.add(new DfVisitor<Object>(new XronosConstantFolding()));
			transformations.add(new DeadActionEliminaton(true));

			if (!actor.hasAttribute("xronos_no_store_once")) {
				transformations.add(new StoreOnceTransformation());
			}
			transformations.add(new DfVisitor<Void>(new LoopUnrolling()));
			transformations.add(new XronosDivision());

			if (portTransformation) {
				transformations.add(new InputRepeatPattern(resourceCache));
				transformations.add(new OutputRepeatPattern());
				transformations.add(new ScalarPortIO(resourceCache));
			}

			transformations.add(new DfVisitor<Void>(new LocalArrayRemoval()));
			transformations.add(new GlobalArrayInitializer(true));

			transformations.add(new XronosScheduler(resourceCache, schedulerInformation));

			transformations.add(new PrintRemoval());
			transformations.add(new DfVisitor<Void>(new DeadVariableRemoval()));

			transformations.add(new DfVisitor<Void>(new XronosSSA()));
			transformations.add(new DfVisitor<Void>(new PhiFixer()));

			transformations.add(new DfVisitor<Void>(new NativeProcedureFinder()));

			transformations.add(new DfVisitor<Void>(new Inliner(false, true, true)));

			transformations.add(new DfVisitor<Void>(new AssignConstantPropagator()));

			transformations.add(new DfVisitor<Void>(new XronosDeadCodeElimination(true, true)));

			transformations.add(new DfVisitor<Void>(new DeadVariableRemoval()));

			transformations.add(new DfVisitor<Void>(new AssignConstantPropagator()));

			transformations.add(new DeadGlobalElimination());
			transformations.add(new DfVisitor<Void>(new DeadVariableRemoval()));
			transformations.add(new DfVisitor<Expression>(new XronosLiteralIntegersAdder()));
			transformations.add(new DfVisitor<Void>(new IndexFlattener()));
			transformations.add(new DfVisitor<Expression>(new XronosTac()));

			transformations.add(new DfVisitor<Expression>(new XronosLiteralIntegersAdder()));
			// transformations.add(new TypeResizer(false, false, false, false));

			transformations.add(new DfVisitor<Expression>(new XronosCast(false, true)));

			transformations.add(new DfVisitor<Void>(new BlockCombine(false)));
			transformations.add(new Pipelining(options));

			// computes names of local variables
			transformations.add(new DfVisitor<Void>(new SSAVariableRenamer()));

			transformations.add(new DfVisitor<CfgNode>(new ControlFlowAnalyzer()));

			for (DfSwitch<?> transformation : transformations) {
				try {
					transformation.doSwitch(actor);
				} catch (NullPointerException ex) {
					OrccLogger.severeln("\t - transformation failed: NullPointerException, " + ex.getMessage());
					break;
				}
			}
		}

	}

	public static void transformActorNonSSA(Actor actor, Map<String, Object> options, ResourceCache resourceCache,
			Boolean portTransformation, boolean schedulerInformation, Boolean debugMode) {
		List<DfSwitch<?>> transformations = new ArrayList<DfSwitch<?>>();
		transformations.add(new UnitImporter());

		// Multi-Dimensional Indexes to Single dimension index
		transformations.add(new DfVisitor<Void>(new IndexFlattener()));

		// Casting
		transformations.add(new DfVisitor<Expression>(new XronosCast(false, true)));
		// Combine Blocks
		transformations.add(new DfVisitor<Void>(new BlockCombine(false)));

		for (DfSwitch<?> transformation : transformations) {
			try {
				transformation.doSwitch(actor);
			} catch (NullPointerException ex) {
				OrccLogger.severeln("\t - transformation failed: NullPointerException, " + ex.getMessage());
				break;
			}
		}
	}

	public static void transformNetworkActors(Network network, Map<String, Object> options,
			ResourceCache resourceCache, boolean schedulerInformation) {
		for (Actor actor : network.getAllActors()) {
			transformActor(actor, options, resourceCache, false, schedulerInformation, false);
		}
	}

	private final Procedure procedure;

	public XronosTransform(Procedure procedure) {
		this.procedure = procedure;
	}

	@Deprecated
	public Procedure transformProcedure(ResourceCache resourceCache) {
		// Add SSA transformation
		new XronosSSA().doSwitch(procedure);
		// Add missing Phi values assign
		new PhiFixer().doSwitch(procedure);
		// Add Literal Integers
		new XronosLiteralIntegersAdder().doSwitch(procedure);
		// Three address Code
		new XronosTac().doSwitch(procedure);
		// Add Literal Integers
		new XronosLiteralIntegersAdder().doSwitch(procedure);
		// Cast Adder
		new CastAdder(false, false).doSwitch(procedure);
		return procedure;
	}

}
