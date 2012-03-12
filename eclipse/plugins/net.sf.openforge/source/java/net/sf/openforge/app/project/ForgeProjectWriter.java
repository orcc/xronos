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
package net.sf.openforge.app.project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.Engine;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionKey;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.SearchLabelExtractor;
import net.sf.openforge.util.IndentWriter;

/**
 * ForgeProjectWriter writes out a Forge project file as an XML document.
 * 
 * Created: Wed Mar 27 10:20:26 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: ForgeProjectWriter.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ForgeProjectWriter {

	Map<OptionKey, Option> option_map;
	File project_file;
	IndentWriter printer;
	GenericJob gj = null;
	Engine engine = null;
	boolean details = false;

	public ForgeProjectWriter(File project_file, GenericJob gj, Engine eng) {
		this.project_file = project_file;
		this.option_map = gj.optionsMap;
		this.gj = gj;
		this.engine = eng;
	} // ForgeProjectWriter()

	public ForgeProjectWriter(File project_file, GenericJob gj, Engine eng,
			boolean details) {
		this(project_file, gj, eng);
		this.details = details;
	} // ForgeProjectWriter()

	public void write() throws IOException {
		FileWriter fw = new FileWriter(project_file);
		printer = new IndentWriter(fw, true);
		printer.setIndentString("    ");

		// header
		printXMLComment("Forge Project File");

		// body
		printer.println("<forgeProject>");

		printer.inc();

		// print unscoped options, using explicit and default values
		printOptions("UNSCOPED", false, details);

		if (this.engine != null) {
			SearchLabelExtractor sle = new SearchLabelExtractor();
			this.engine.getDesign().accept(sle);
			List<String> tagList = sle.getTags();
			for (String str : tagList) {
				printOptions(str, true, false);
			}

		}

		printer.dec();

		printer.println("</forgeProject>");

		printer.flush();
		printer.close();
		fw.close();

	} // write()

	private void printXMLComment(String comment) {
		printer.println("<!-- " + comment + " -->");
	}

	private void printOptions(String tag, boolean explicit, boolean details) {
		printer.println("<options tag=\"" + tag + "\">");
		printer.inc();

		if (!explicit) {
			for (Option definition: option_map.values()){
				if ((definition.getOptionKey() != OptionRegistry.PFILE)
						&& (definition.getOptionKey() != OptionRegistry.PROJ_FILE)
						&& (definition.getOptionKey() != OptionRegistry.PROJ_FILE_DESC)) {
					String value = definition.getValue(CodeLabel.UNSCOPED)
							.toString();

					if (!definition.isHidden()) {
						if (details) {
							printXMLComment(definition.getDescription());
						}
						printOption(definition, value);
					}
				}
			}
		} else {
			for (Option definition: option_map.values()){
				Object obj = definition.getExplicitValue(tag);

				if (obj != null) {
					if (!definition.isHidden()) {
						printOption(definition, obj.toString());
					}
				}
			}
		}

		printer.dec();
		printer.println("</options>");
	}

	private void printOption(Option definition, String value) {
		definition.printXML(printer, value);
	} // printOption()

} // ForgeProjectWriter
