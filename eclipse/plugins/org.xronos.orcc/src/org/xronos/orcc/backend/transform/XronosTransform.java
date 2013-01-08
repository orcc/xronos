/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.xronos.orcc.backend.transform;

import java.util.ArrayList;
import java.util.List;

import net.sf.orcc.backends.transform.CastAdder;
import net.sf.orcc.backends.transform.DivisionSubstitution;
import net.sf.orcc.backends.transform.GlobalArrayInitializer;
import net.sf.orcc.backends.transform.Inliner;
import net.sf.orcc.backends.transform.LocalArrayRemoval;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.transform.UnitImporter;
import net.sf.orcc.df.util.DfSwitch;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.CfgNode;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.transform.ControlFlowAnalyzer;

import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.visitors.StmtIO;

/**
 * This helper class transforms only a given procedure
 * 
 * @author Endri Bezati
 * 
 */
public class XronosTransform {

	private final Procedure procedure;

	public XronosTransform(Procedure procedure) {
		this.procedure = procedure;
	}

	public Procedure transformProcedure(ResourceCache resourceCache) {
		// new XronosConstantPropagation();
		// new ConstantPropagator();
		// SSA
		new XronosSSA().doSwitch(procedure);
		// Add Literal Integers
		new XronosLiteralIntegersAdder().doSwitch(procedure);
		// Three address Code
		new XronosTac().doSwitch(procedure);
		// Add Literal Integers
		new XronosLiteralIntegersAdder().doSwitch(procedure);
		// Cast Adder
		new CastAdder(false, false).doSwitch(procedure);
		// Dead Phi Removal
		new DeadPhiRemover().doSwitch(procedure);
		// StmIO
		new StmtIO(resourceCache).doSwitch(procedure);
		return procedure;
	}

	public static void transformActor(Actor actor, ResourceCache resourceCache,
			boolean debugMode) {
		if (!actor.hasAttribute("no_generation")) {
			List<DfSwitch<?>> transformations = new ArrayList<DfSwitch<?>>();
			transformations.add(new UnitImporter());
			// transformations.add(new DfVisitor<Void>(new
			// LocalVarInitializer()));
			// transformations.add(new StoreOnceTransformation());
			transformations.add(new XronosParameterPropagation());
			// transformations.add(new DfVisitor<Void>(
			// new XronosConstantPropagation()));
			// transformations.add(new DfVisitor<Void>(new
			// ConstantPropagator()));
			transformations.add(new DivisionSubstitution());
			transformations.add(new DfVisitor<Void>(new LocalArrayRemoval()));
			transformations.add(new RepeatPattern(resourceCache));
			transformations.add(new DfVisitor<Void>(new XronosSSA()));
			transformations.add(new GlobalArrayInitializer(true));
			transformations.add(new DfVisitor<Void>(new Inliner(true, true)));
			transformations.add(new ScalarPortIO(resourceCache));
			transformations.add(new DfVisitor<Expression>(
					new XronosLiteralIntegersAdder()));
			transformations.add(new DfVisitor<Void>(new IndexFlattener()));
			transformations.add(new DfVisitor<Expression>(new XronosTac()));
			transformations.add(new DfVisitor<CfgNode>(
					new ControlFlowAnalyzer()));
			transformations.add(new DfVisitor<Expression>(
					new XronosLiteralIntegersAdder()));
			transformations.add(new DfVisitor<Expression>(new XronosCast(false,
					false)));
			transformations.add(new DfVisitor<Void>(new DeadPhiRemover()));

			for (DfSwitch<?> transformation : transformations) {
				transformation.doSwitch(actor);
				if (debugMode) {
					System.out.println("oops " + transformation + " "
							+ actor.getName());
				}
			}
		}

	}

}
