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
package org.xronos.openforge.verilog.pattern;

import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.Version;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.verilog.mapping.MappedModule;
import org.xronos.openforge.verilog.model.Comment;
import org.xronos.openforge.verilog.model.Module;
import org.xronos.openforge.verilog.model.VerilogDocument;


/**
 * A DesignDocument is VerilogDocument based upon a LIM {@link Design}.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: DesignDocument.java 113 2006-03-22 18:28:24Z imiller $
 */

public class DesignDocument extends VerilogDocument implements
		MappedModuleSpecifier {

	private Set<MappedModule> mappedModules = new HashSet<MappedModule>();

	public DesignDocument(Design design) {
		//
		// Put standard header on the file.
		//
		Comment logo = new Comment("__  ___ __ ___  _ __   ___  ___ ");
		append(logo);
		logo = new Comment("\\ \\/ / '__/ _ \\| '_ \\ / _ \\/ __|");
		append(logo);
		logo = new Comment(" >  <| | | (_) | | | | (_) \\__ \\");
		append(logo);
		logo = new Comment("/_/\\_\\_|  \\___/|_| |_|\\___/|___/");
		append(logo);
		append(new Comment(""));
		Comment header = new Comment("Xronos synthesizer "
				+ Version.versionNumber(), Comment.SHORT);
		append(header);
		String runDate = EngineThread.getGenericJob()
				.getOption(OptionRegistry.RUN_DATE)
				.getValue(CodeLabel.UNSCOPED).toString();
		header = new Comment("Run date: " + runDate, Comment.SHORT);
		append(header);

		header = new Comment("");
		append(header);

		// Put 'include statement on the file
		for (String string : design.getIncludeStatements()) {
			append(new IncludeStatement(string));
		}
	} // DesignDocument

	/**
	 * Appends a verilog module to the end of the document, and gathers any
	 * needed include directives.
	 */
	@Override
	public void append(Module module) {
		super.append(module);

		if (module instanceof MappedModuleSpecifier) {
			mappedModules.addAll(((MappedModuleSpecifier) module)
					.getMappedModules());
		}
	} // append()

	/**
	 * Provides the Set of MappedModules
	 */
	@Override
	public Set<MappedModule> getMappedModules() {
		return mappedModules;
	}

} // class DesignDocument

