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
package net.sf.openforge.verilog.pattern;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.Version;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.VerilogDocument;

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

	private Set<MappedModuleSpecifier> mappedModules = new HashSet<MappedModuleSpecifier>();

	public DesignDocument(Design design) {
		//
		// Put standard header on the file.
		//
		Comment header = new Comment("Open Forge " + Version.versionNumber(),
				Comment.SHORT);
		append(header);
		String runDate = EngineThread.getGenericJob()
				.getOption(OptionRegistry.RUN_DATE)
				.getValue(CodeLabel.UNSCOPED).toString();
		header = new Comment("Run date: " + runDate, Comment.SHORT);
		append(header);

		header = new Comment("");
		append(header);

		// Put 'include statement on the file
		for (Iterator iter = design.getIncludeStatements().iterator(); iter
				.hasNext();) {
			append(new IncludeStatement((String) iter.next()));
		}
	} // DesignDocument

	/**
	 * Appends a verilog module to the end of the document, and gathers any
	 * needed include directives.
	 */
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
	public Set getMappedModules() {
		return mappedModules;
	}

} // class DesignDocument

