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

package org.xronos.openforge.optimize.replace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.eclipse.core.runtime.jobs.Job;
import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.Option;
import org.xronos.openforge.app.project.OptionInt;
import org.xronos.openforge.app.project.OptionList;
import org.xronos.openforge.lim.Operation;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.optimize.Optimization;
import org.xronos.openforge.util.ForgeResource;

/**
 * OperationReplacementVisitor replaces operations with calls to functionally
 * equivalent methods. This allows for the generation of synthesizable
 * implementations for nonsynthesizable operations such as div, rem, and
 * floating point operations. Iterative multipliers and/or bitserial arithmetic
 * may also be implemented. The implementation libraries are taken from the
 * option {@link OptimizeDefiner#OPERATOR_REPLACEMENT_LIBS} for the given
 * Component. The library is found according to the rules defined in the
 * {@link LibraryResource} class.
 * <p>
 * <b>The replacement depends on correct name of method, sufficient size of
 * args/result as obtained from the JMethod model for each top level call's
 * procedure. When additional source languages are added the models will need to
 * implement an interface to report these characteristics. </b>
 * 
 * <p>
 * Created: Thu Aug 29 09:19:59 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OperationReplacementVisitor.java 2 2005-06-09 20:00:48Z imiller
 *          $
 */
public class OperationReplacementVisitor extends ReplacementVisitor implements
		Optimization {

	/**
	 * The default libs are only created (extracted from jar file) the first
	 * time that we encounter a divide or remainder operation.
	 */
	private static List<String> defaultLibraries = null;

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		target.accept(this);
	}

	/**
	 * Looks for any {@link Operation} which has a
	 * {@link ReplacementCorrelation} defined and attempts to replace that
	 * {@link Operation} with a library defined implementation according to the
	 * context sensitive library search order.
	 * 
	 * @param comp
	 *            a value of type 'Operation'
	 */
	@Override
	public void filter(Operation op) {
		super.filter(op);

		if (ReplacementCorrelation.isReplaceable(op)) {
			Match match = getImplementation(op);

			if (match != null) {
				replace(op, match);
			}
		}
	}

	/**
	 * Ensures that the operation for which an implementation is being retrieved
	 * is not more complex (according to
	 * {@link ReplacementCorrelation#getComplexityRank}) than the max complexity
	 * level specified by the preference
	 * {@link OptionDB#OPERATOR_REPLACEMENT_MAX_LEVEL}. Then, if the complexity
	 * is allowable, the libraries to be searched are retrieved. If the
	 * libraries are the default libraries, and the component is not one which
	 * we know exists in the default libraries, null is returned. Otherwise the
	 * libraries are searched for an implementation.
	 * 
	 * @param op
	 *            an 'Operation' which implements the {@link Replaceable}
	 *            interface
	 * @return the Call from the Design to the found method or null if none
	 *         found.
	 */
	private Match getImplementation(Operation op) {
		Option option;
		option = op.getGenericJob().getOption(
				OptionRegistry.OPERATOR_REPLACEMENT_LIBS);
		List<String> libs = ((OptionList) option).getValueAsList(op
				.getSearchLabel());
		option = op.getGenericJob().getOption(
				OptionRegistry.OPERATOR_REPLACEMENT_MAX_LEVEL);
		int maxReplaceLevel = ((OptionInt) option).getValueAsInt(op
				.getSearchLabel());
		ReplacementCorrelation correlation = ReplacementCorrelation
				.getCorrelation(op);

		// Don't do the replacement if the rank is higher than our
		// allowable level.
		if (correlation.getComplexityRank() > maxReplaceLevel) {
			return null;
		}

		//
		// Test to see if the user has changed the libraries from
		// their default setting. If they haven't (most cases) then
		// we know that we can only replace for a DIV, a MOD, or
		// floats so return null if it isn't one of those components.
		// THIS CODE MUST BE CHANGED IF YOU CHANGE THE DEFAULTS IN
		// OptimizeDefiner.
		//
		@SuppressWarnings("unused")
		final ReplacementCorrelation divCorr = ReplacementCorrelation
				.getCorrelation(DivideOp.class);
		@SuppressWarnings("unused")
		final ReplacementCorrelation remCorr = ReplacementCorrelation
				.getCorrelation(ModuloOp.class);
		final boolean isDivRem = false;// (divCorr == correlation || remCorr ==
										// correlation);

		if (libs.isEmpty()) {
			if (!isDivRem && !op.isFloat()) {
				return null;
			}
		}

		// Append the default libraries to the end of the list, but
		// only if unique and only if they are applicable to the
		// operation at hand.
		List<String> runLibs = new ArrayList<String>(libs);
		if (isDivRem) {
			List<String> defaultLibs = getDefaultLibs();
			for (String string : defaultLibs) {
				if (!runLibs.contains(string)) {
					runLibs.add(string);
				}
			}
		}

		return getImplementationFromLibs(runLibs, op);
	}

	/**
	 * Reports, via {@link Job#info}, what optimization is being performed
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info(
				"performing operation substitution...");
	}

	/**
	 * Reports, via {@link Job#verbose}, the results of <b>this</b> pass of the
	 * optimization.
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().verbose(
				"replaced " + getReplacedNodeCount() + " operations");
	}

	private List<String> getDefaultLibs() {
		if (OperationReplacementVisitor.defaultLibraries == null) {
			List<String> list = new ArrayList<String>();
			try {
				InputStream divIS = ForgeResource
						.loadForgeResourceStream("OP_REPLACE_DIV");
				File divFile = writeTempFile(divIS, "div");
				list.add(divFile.getAbsolutePath());
			} catch (IOException e) {
				EngineThread
						.getGenericJob()
						.warn("Could not create default divide library.  Divide will not be synthesizable.");
			}

			try {
				InputStream remIS = ForgeResource
						.loadForgeResourceStream("OP_REPLACE_REM");
				File remFile = writeTempFile(remIS, "rem");
				list.add(remFile.getAbsolutePath());
			} catch (IOException e) {
				EngineThread
						.getGenericJob()
						.warn("Could not create default divide library.  Divide will not be synthesizable.");
			}

			OperationReplacementVisitor.defaultLibraries = Collections
					.unmodifiableList(list);
		}

		return OperationReplacementVisitor.defaultLibraries;
	}

	// JWJFIXME: make this language independent, allow temp files with types
	// other than ".c"

	private File writeTempFile(InputStream stream, String name)
			throws IOException {
		File outFile = File.createTempFile(name, ".c");

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		PrintStream ps = new PrintStream(new FileOutputStream(outFile));
		while (reader.ready()) {
			ps.println(reader.readLine());
		}
		ps.close();
		outFile.deleteOnExit();

		return outFile;
	}

}// OperationReplacementVisitor

