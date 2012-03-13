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

package net.sf.openforge.frontend.slim.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.frontend.slim.builder.SLIMBuilder;
import net.sf.openforge.frontend.slim.builder.SLIMBuilderException;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.naming.LIMLogicalNamer;
import net.sf.openforge.optimize.Optimizer;
import net.sf.openforge.schedule.Scheduler;
import net.sf.openforge.verilog.translate.PassThroughComponentRemover;
import net.sf.openforge.verilog.translate.VerilogNaming;
import net.sf.openforge.verilog.translate.VerilogTranslator;

import org.w3c.dom.Document;

/**
 * This class is used for testing/debug only, XLIMCompiler sequences the
 * necessary steps to realize the compilation of XLIM source to HDL.
 */
public class SLIMCompiler {

	private SLIMCompiler() {
	}


	private void compile(GenericJob job) {
		@SuppressWarnings("unused")
		final SLIMEngine engine = new SLIMEngine(job);

		final File[] targetFiles = job.getTargetFiles();
		assert targetFiles.length == 1 : "XLIM Compilation supports exactly one target file.  Found: "
				+ targetFiles.length;

		final File input = targetFiles[0];
		Document document = null;
		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			System.out.println("RUN A SCHEMA ON ME!");
			document = domBuilder.parse(input);
		} catch (ParserConfigurationException pce) {
			throw new SLIMBuilderException("error in configuration " + pce);
		} catch (org.xml.sax.SAXException saxe) {
			throw new SLIMBuilderException("sax error " + saxe);
		} catch (IOException ioe) {
			throw new SLIMBuilderException("io error " + ioe);
		}
		Design design = new SLIMBuilder().build(document);

		System.out.println("Optimizing");
		(new Optimizer()).optimize(design);

		System.out.println("Scheduling");
		Scheduler.schedule(design);

		System.out.println("Naming");
		LIMLogicalNamer.setNames(design);

		System.out.println("PassThroughRemover");
		design.accept(new PassThroughComponentRemover());

		System.out.println("RE-naming");
		VerilogNaming naming = new VerilogNaming();
		naming.visit(design);

		System.out.println("Writing");
		// _parser.d.launchXGraph(design, false);
		// try {System.out.println("Sleeping"); Thread.sleep(1000);}catch
		// (Exception e){}
		try {
			FileOutputStream fos = new FileOutputStream(new File("foo.v"));
			VerilogTranslator.translate(design, fos);
		} catch (IOException ioe) {
			System.out.println("Writing failed " + ioe);
		}
	}

	public static void main(String args[]) {
		GenericJob job = new GenericJob();

		try {
			job.setOptionValues(args);
		} catch (NewJob.ForgeOptionException foe) {
			System.err
					.println("Command line option error: " + foe.getMessage());
			System.err.println("");
			System.err.println(OptionRegistry.usage(false));
			System.exit(-1);
		}
		SLIMCompiler compiler = new SLIMCompiler();
		compiler.compile(job);
	}

}// XLIMCompiler
