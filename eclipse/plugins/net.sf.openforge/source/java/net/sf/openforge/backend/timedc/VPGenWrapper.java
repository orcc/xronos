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

package net.sf.openforge.backend.timedc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.lim.io.SimplePin;

/**
 * VPGenWrapper generates a header file and C++ source file that makes our cycle
 * C model Virtual Platform compliant. The header file defines the instance of
 * the pcore class and the publicly available member data and functions.
 * 
 * <p>
 * Created: Sat Apr 16 13:32:09 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: VPGenWrapper.java 112 2006-03-21 15:41:57Z imiller $
 */
public class VPGenWrapper {

	/**
	 * The ioHandler that will take care of naming for our models ports.
	 */
	private final IOHandler ioHandle;
	/** The base of all naming for functions, VP structures, etc. */
	private final String fxnName;
	/** The Virtual Platform wrapper header file. */
	private final File headerFile;
	/** The Virtual Platform wrapper source file */
	private final File sourceFile;
	/** The File which is the header file for our simulation model. */
	private final File simHeaderFile;
	/* A List of all FifoIF objects for this design. */
	private final List<FifoIF> fifoIFs;

	private static final String I_PTR = "iptr";
	private static final String O_PTR = "optr";

	/**
	 * Creates a new VPGenWrapper.
	 * 
	 * @param io
	 *            a value of type 'IOHandler'
	 * @param fxnName
	 *            a value of type 'String'
	 * @param simHeaderFile
	 *            a value of type 'File'
	 */
	public VPGenWrapper(IOHandler io, String fxnName, File simHeaderFile) {
		this.ioHandle = io;
		this.fxnName = fxnName;
		this.simHeaderFile = simHeaderFile;
		this.headerFile = EngineThread.getGenericJob().getFileHandler()
				.getFile(CycleCTranslateEngine.VPGEN_HEADER);
		this.sourceFile = EngineThread.getGenericJob().getFileHandler()
				.getFile(CycleCTranslateEngine.VPGEN_WRAPPER);

		this.fifoIFs = new ArrayList<FifoIF>(this.ioHandle.getInputs());
		fifoIFs.addAll(this.ioHandle.getOutputs());
	}

	public void generateVPWrapper(File cModel) throws IOException {
		VarInits inits = writeHeaderFile();
		writeSourceFile(cModel, inits);
	}

	/**
	 * Generates the VP compliant header file.
	 * 
	 * @exception IOException
	 *                if an error occurs
	 */
	private VarInits writeHeaderFile() throws IOException {
		final VarInits varInits = new VarInits();
		final PrintStream ps = new PrintStream(new FileOutputStream(
				this.headerFile), true);

		ps.println("#ifndef _" + this.fxnName + "_H_");
		ps.println("#define _" + this.fxnName + "_H_");
		ps.println();
		ps.println("#include \"pcore.h\"");
		ps.println("#include \"" + this.simHeaderFile.getName() + "\"");
		ps.println();

		// Define the ports struct, one var for each port of the
		// design, including the clock port.
		ps.println("typedef struct ");
		ps.println("{");
		for (FifoIF fifo : this.fifoIFs) {
			for (SimplePin pin : fifo.getPins()) {
				ps.println("    UINT32 *" + getVPName(pin) + ";");
			}
		}
		ps.println("    UINT32 *clk;");
		ps.println("} " + this.fxnName + "_ports;");

		// We dont use any generics
		ps.println();
		ps.println("typedef struct");
		ps.println("{");
		ps.println("} " + this.fxnName + "_generics;");

		// Nor do we publish any of our internal registers
		ps.println();
		ps.println("typedef struct");
		ps.println("{");
		ps.println("} " + this.fxnName + "_regs;");

		// Define the pcore instance for this function
		ps.println();
		ps.println("class " + this.fxnName + " : public pcore");
		ps.println("{");
		ps.println(" private:");

		// Create a private variable for each fsl input and output
		// which is of the type that our simulation uses.
		for (FifoIF fifo : this.ioHandle.getInputs()) {
			// final FifoIF fifo = (FifoIF)iter.next();
			final String name = getVPName(fifo);
			ps.println("    " + ioHandle.getInputType() + " " + name + ";");
			for (SimplePin pin : fifo.getPins()) {
				varInits.structMemberInits.add(name + "."
						+ this.ioHandle.getMemberName(pin) + " = 0;");
			}
			varInits.inputPointers.add(name);
		}
		for (FifoIF fifo : this.ioHandle.getOutputs()) {
			// final FifoIF fifo = (FifoIF)iter.next();
			final String name = getVPName(fifo);
			ps.println("    " + ioHandle.getOutputType() + " " + name + ";");
			for (SimplePin pin : fifo.getPins()) {
				varInits.structMemberInits.add(name + "."
						+ this.ioHandle.getMemberName(pin) + " = 0;");
			}
			varInits.outputPointers.add(name);
		}
		// Create an array of pointers to these private variables.
		// This is what we feed to our update function.
		ps.println("    " + ioHandle.getInputType() + " *" + I_PTR + "["
				+ varInits.inputPointers.size() + "];");
		ps.println("    " + ioHandle.getOutputType() + " *" + O_PTR + "["
				+ varInits.outputPointers.size() + "];");

		// Declare the variables that VP is looking for.
		ps.println();
		ps.println(" public:");

		ps.println("    " + this.fxnName + "_ports *ports;");
		ps.println("    " + this.fxnName + "_generics *generics;");
		ps.println("    " + this.fxnName + "_regs *regs;");

		// Provide prototypes for the API functions that VP uses.
		ps.println("    UINT8 EvalAsyncOutputs();");
		ps.println("    void clk_EvalSyncInputs();");
		ps.println("    void clk_UpdateSyncOutputs();");
		ps.println("    int InitModel();");

		// A constructor for the pcore instance.
		ps.println();
		ps.println("    " + this.fxnName + "() ");
		ps.println("    {");
		ps.println("        InitModel();");
		ps.println("    }");

		// A destructor.
		ps.println();
		ps.println("    ~" + this.fxnName + "()");
		ps.println("    {");
		ps.println("        delete ports;");
		ps.println("        delete generics;");
		ps.println("        delete regs;");
		ps.println("    }");

		ps.println("};");
		ps.println("#endif");

		return varInits;
	}

	/**
	 * Writes the source code for the VP wrapper.
	 * 
	 * @param cModel
	 *            a value of type 'File'
	 * @exception IOException
	 *                if an error occurs
	 */
	private void writeSourceFile(File cModel, VarInits varInits)
			throws IOException {
		final PrintStream ps = new PrintStream(new FileOutputStream(
				this.sourceFile), true);
		ps.println("#include \"" + this.headerFile.getName() + "\"");
		ps.println("#include \"" + cModel.getName() + "\"");

		//
		// Generate the EvalAsyncOutputs function
		//
		ps.println();
		ps.println("UINT8 " + this.fxnName + "::EvalAsyncOutputs()");
		ps.println("{");
		// copy values from VP structures into our structures
		for (FifoIF fifo : this.fifoIFs) {
			final String base = getVPName(fifo);
			for (SimplePin pin : fifo.getPins()) {
				final String member = this.ioHandle.getMemberName(pin);
				ps.println("    " + base + "." + member + " = *ports->"
						+ getVPName(pin) + ";");
			}
		}

		ps.println("    // Call update");
		ps.println("    " + this.fxnName + "_update(" + I_PTR + ", " + O_PTR
				+ ");");

		ps.println("    // Check to see if values changed");
		ps.println("    UINT8 retVal = (");
		// Store the terms in a list so that we get the "||" correct.
		final List<String> compareTerms = new ArrayList();
		final List<String> copyTerms = new ArrayList();
		for (FifoIF fifo : this.fifoIFs) {
			final String base = getVPName(fifo);
			for (SimplePin outPin : fifo.getOutputPins()) {
				compareTerms.add("(" + base + "."
						+ this.ioHandle.getMemberName(outPin) + " != *ports->"
						+ getVPName(outPin) + ")");
				copyTerms.add("*ports->" + getVPName(outPin) + " = " + base
						+ "." + this.ioHandle.getMemberName(outPin) + ";");
			}
		}
		for (Iterator iter = compareTerms.iterator(); iter.hasNext();) {
			String term = (String) iter.next();
			String trail = (iter.hasNext()) ? " ||" : "";
			ps.println("        " + term + trail);
		}
		ps.println("    );");
		ps.println();
		// Write out the copy of our data structures to VP data structures.
		ps.println("    // Copy the outputs from the core back to the VP data structures");
		for (String term : copyTerms) {
			ps.println("    " + term);
		}
		ps.println();
		ps.println("    return retVal;");
		ps.println("}");

		//
		// Generate the EvalSyncInputs function. Is empty. According
		// to the EDK team this function is rarely used. Mostly it is
		// just for cmodelgen.
		//
		ps.println();
		ps.println("void " + this.fxnName + "::clk_EvalSyncInputs()");
		ps.println("{");
		ps.println("    // This function intentionally left empty");
		ps.println("}");

		//
		// Generate the UpdateSyncOutputs function. This is where
		// most of the work is done.
		//
		ps.println();
		ps.println("void " + this.fxnName + "::clk_UpdateSyncOutputs()");
		ps.println("{");
		ps.println("    // Call the clockEdge() function.");
		ps.println("    // Check that the clock is in the '1' state, ie rising edge");
		ps.println("    // and if it is rising edge, call clock edge, but dont propagate our");
		ps.println("    // core outputs to the vp outputs (yet).");
		ps.println("    if (*ports->clk == 1)");
		ps.println("    {");
		ps.println("        " + this.fxnName + "_clockEdge();");
		ps.println("    }");
		/*
		 * I dont think that this needs to be done here. I believe that this
		 * function is the clock edge and EvalAsyncOutputs is the combinational
		 * phase. ps.println(
		 * "    // Now that the clock has advanced, do the combinational stuff too"
		 * ); ps.println("    "+this.fxnName+"_update("+I_PTR+", "+O_PTR+");");
		 */
		ps.println("}");

		//
		// Generate the InitModel function. This initializes class
		// variables. Ultimately it would be good if this could reset
		// all the global variables used in our model too.
		//
		ps.println();
		ps.println("int " + this.fxnName + "::InitModel ()");
		ps.println("{");

		writeVarInits(varInits, ps);

		ps.println("    // Should set the initial values for all vars.  Currently this is built into C model.");
		ps.println("    // Run the first 5 cycles to get us out of reset.");
		ps.println("    int i;");
		ps.println("    for (i=0; i < 5; i++)");
		ps.println("    {");
		ps.println("        // we init exists and full to 0, so no need to reset ports during these 5 cycles");
		ps.println("        " + this.fxnName + "_update(iptr, optr);");
		ps.println("        " + this.fxnName + "_clockEdge();");
		ps.println("    }");
		ps.println("}");
	}

	private void writeVarInits(VarInits varInits, PrintStream ps) {
		final String tab = "    ";
		ps.println("// Initialization of the member vars");
		ps.println(tab + "ports = new " + this.fxnName + "_ports();");
		ps.println(tab + "generics = new " + this.fxnName + "_generics();");
		ps.println(tab + "regs = new " + this.fxnName + "_regs();");
		for (int i = 0; i < varInits.inputPointers.size(); i++) {
			ps.println(tab + "" + I_PTR + "[" + i + "] = &"
					+ varInits.inputPointers.get(i) + ";");
		}
		for (int i = 0; i < varInits.outputPointers.size(); i++) {
			ps.println(tab + "" + O_PTR + "[" + i + "] = &"
					+ varInits.outputPointers.get(i) + ";");
		}

		for (Iterator iter = varInits.structMemberInits.iterator(); iter
				.hasNext();) {
			ps.println(tab + "" + iter.next());
		}
	}

	/**
	 * Converts the specified pin to the VP name for that port.
	 * 
	 * @param pin
	 *            a value of type 'SimplePin'
	 * @return a value of type 'String'
	 */
	private String getVPName(SimplePin pin) {
		return pin.getName().toLowerCase();
	}

	private final Map nameMap = new HashMap();

	/**
	 * Generates/caches a name for the given fifo interface. This is the name
	 * used for our private variable which interacts with our simulation model.
	 * 
	 * @param pin
	 *            a value of type 'SimplePin'
	 * @return a value of type 'String'
	 */
	private String getVPName(FifoIF fifo) {
		if (nameMap.containsKey(fifo))
			return (String) nameMap.get(fifo);

		String prefix = fifo.getPins().iterator().next().getName();
		prefix = prefix.substring(0, prefix.indexOf("_"));
		prefix = prefix.toLowerCase();
		prefix = (fifo.isInput()) ? (prefix + "_in") : (prefix + "_out");
		nameMap.put(fifo, prefix);
		return prefix;
	}

	private static class VarInits {
		List inputPointers = new ArrayList();
		List outputPointers = new ArrayList();
		List structMemberInits = new ArrayList();
	}

}// VPGenWrapper
