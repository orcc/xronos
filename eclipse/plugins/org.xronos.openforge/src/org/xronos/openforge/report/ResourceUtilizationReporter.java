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
package org.xronos.openforge.report;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.FilteredVisitor;
import org.xronos.openforge.lim.Procedure;


/**
 * A visitor that travserses through a Design and collect the resource
 * consumsion information for each individual procedure, task, and design.
 * 
 * @author cwu
 * @version $Id: ResourceUtilizationReporter.java 2 2005-06-09 20:00:48Z imiller
 *          $
 */
public class ResourceUtilizationReporter extends FilteredVisitor {

	private PrintWriter writer;
	private String designID;

	public ResourceUtilizationReporter(Design design,
			FPGAResource designResource, Map<Procedure, FPGAResource> procedureResourceUsageMap,
			OutputStream os) {
		super();

		writer = new PrintWriter(os, true);
		designID = new String(design.showIDLogical());

		writer.println("<HTML>");
		writer.println("<HEAD>");
		writer.println("<TITLE>");
		writer.println("Forge Design Resource Utilization Report");
		writer.println("</TITLE>");
		writer.println("</HEAD>");
		writer.println("<BODY BGCOLOR=\"white\">");
		visit(design);
		writer.println("<HR>");
		writer.println("<H2>");
		writer.println("FPGA Resouces Usage Report ");
		writer.println("</H2>");
		printDesignResourceUsage(designResource);
		printTaskResourceUsage(procedureResourceUsageMap);
		for (int i = 0; i < 100; i++) {
			writer.print("<BR>");
		}
		writer.println();
		writer.println("</BODY>");
		writer.println("</HTML>");
	}

	@Override
	public void visit(Design design) {
		writer.println("<CENTER>");
		writer.println("<H2>");
		writer.println(designID + " Design Resource Utilization Report");
		writer.println("</H2>");
		writer.println("</CENTER>");
		writer.println("<HR>");
		writer.println("<H2>");
		writer.println("Design Hierarchy");
		writer.println("</H2>");
		writer.println("<UL>");
		writer.println("<LI TYPE=\"square\"><B><A HREF=\"ResourceUtilizationReport.html#"
				+ designID + "\">" + designID + "</A></B>");
		super.visit(design);
		writer.println("</UL>");
	}

	@Override
	public void visit(Procedure procedure) {
		writer.println("<UL>");
		writer.println("<LI TYPE=\"circle\"><B><A HREF=\"ResourceUtilizationReport.html#"
				+ procedure.showIDLogical()
				+ "\">"
				+ procedure.showIDLogical()
				+ "</A></B>");
		super.visit(procedure);
		writer.println("</UL>");
	}

	private void printDesignResourceUsage(FPGAResource designResource) {
		writer.println("<A NAME=\"" + designID + "\"></A><B>" + designID
				+ "</B> design resource usage: ");
		writer.println("<BR>");
		printResourceUsage(designResource);
		writer.println("<P>");
	}

	private void printTaskResourceUsage(Map<Procedure, FPGAResource> procedureResourceUsageMap) {
		for (Iterator<Procedure> procIter = procedureResourceUsageMap.keySet().iterator(); procIter
				.hasNext();) {
			Procedure proc = (Procedure) procIter.next();
			FPGAResource procResource = (FPGAResource) procedureResourceUsageMap
					.get(proc);

			writer.println("<A NAME=\"" + proc.showIDLogical() + "\"></A><B>"
					+ proc.showIDLogical() + "</B> resource usage: ");
			writer.println("<BR>");
			printResourceUsage(procResource);
			writer.println("<P>");
		}
	}

	private void printResourceUsage(FPGAResource resource) {
		final int outputLengh = 50;
		String outputString;
		int spacing = 0;

		writer.println("<BLOCKQUOTE>");
		writer.println("<DL>");
		if (resource.getFlipFlops() != 0) {
			writer.println("<DT>");
			writer.println("<B>");
			outputString = new String("Number of Slice Flip Flops:");
			String number = Integer.toString(resource.getFlipFlops());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
			writer.println("</B>");
		}
		if (resource.getTotalLUTs() != 0) {
			writer.println("<DT>");
			writer.println("<B>");
			outputString = new String("Total 4 input LUTs:");
			String number = Integer.toString(resource.getTotalLUTs());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
			writer.println("</B>");
		}
		if (resource.getLUTs() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as LUTs:");
			String number = Integer.toString(resource.getLUTs());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getSRL16s() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as SRL16s:");
			String number = Integer.toString(resource.getSRL16s());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getSpLutRams() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as Single Port RAMs:");
			String number = Integer.toString(resource.getSpLutRams());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getDpLutRams() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as Dual Port RAMs:");
			String number = Integer.toString(resource.getDpLutRams());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getRoms() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as Roms:");
			String number = Integer.toString(resource.getRoms());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getBlockRams() != 0) {
			writer.println("<DT>");
			writer.println("<B>");
			outputString = new String("Number of Block Rams:");
			String number = Integer.toString(resource.getBlockRams());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
			writer.println("</B>");
		}
		if (resource.getSpBlockRams() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as Single Port RAMs:");
			String number = Integer.toString(resource.getSpBlockRams());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getDpBlockRams() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as Dual Port RAMs:");
			String number = Integer.toString(resource.getDpBlockRams());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getMULT18X18s() != 0) {
			writer.println("<DT>");
			writer.println("<B>");
			outputString = new String("Number of MULT18x18s:");
			String number = Integer.toString(resource.getMULT18X18s());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
			writer.println("</B>");
		}
		if (resource.getBondedIOBs() != 0) {
			writer.println("<DT>");
			writer.println("<B>");
			outputString = new String("Total bonded IOBs:");
			String number = Integer.toString(resource.getBondedIOBs());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
			writer.println("</B>");
		}
		if (resource.getIBs() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as IBUFs:");
			String number = Integer.toString(resource.getIBs());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getOBs() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as OBUFs:");
			String number = Integer.toString(resource.getOBs());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getIOBs() != 0) {
			writer.println("<DD>");
			outputString = new String("Number used as IOBUFs:");
			String number = Integer.toString(resource.getIOBs());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
		}
		if (resource.getClock() != 0) {
			writer.println("<DT>");
			writer.println("<B>");
			outputString = new String("Number of GCLKs:");
			String number = Integer.toString(resource.getClock());
			spacing = outputLengh - outputString.length() - number.length();
			for (int i = 0; i < spacing; i++) {
				outputString += " ";
			}
			outputString += number;
			writer.println(outputString);
			writer.println("</B>");
		}
		writer.println("</DL>");
		writer.println("</BLOCKQUOTE>");
	}
}
