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

package org.xronos.openforge.verilog.testbench;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.backend.hdl.TestBenchEngine;
import org.xronos.openforge.lim.io.BlockDescriptor;
import org.xronos.openforge.lim.io.BlockElement;
import org.xronos.openforge.lim.io.DeclarationGenerator;
import org.xronos.openforge.util.exec.Drain;
import org.xronos.openforge.util.exec.ExecutionException;
import org.xronos.openforge.util.exec.GCC;


/**
 * BlockIOReflection.java
 * 
 * 
 * <p>
 * Created: Thu Mar 17 11:25:18 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: BlockIOReflection.java 112 2006-03-21 15:41:57Z imiller $
 */
public class BlockIOReflection {

	private static final int MAX_VECTORS = 1000;
	private static final int NUMBER_OF_NOARG_VECTORS = 25;

	// number of generic input vectors for blockio
	private int numVectors;

	private BlockDescriptor ibd;
	private BlockDescriptor obd;
	private PrintWriter pw;
	/** The users input source file. */
	private File sourceFile;
	/** The generated reflect file. */
	private File targetFile;
	private String baseName;

	public BlockIOReflection(BlockDescriptor ibd, BlockDescriptor obd) {
		this.ibd = ibd;
		this.obd = obd;

		final GenericJob gj = EngineThread.getGenericJob();
		this.baseName = gj.getOutputBaseName();
		this.targetFile = gj.getFileHandler().getFile(
				TestBenchEngine.BlockIOTestBenchEngine.REFLECT);

		try {
			this.pw = new PrintWriter(new FileWriter(targetFile));
		} catch (IOException ioe) {
			gj.fatalError("Could not create writer for reflect file. "
					+ ioe.getMessage());
		}
		this.sourceFile = gj.getTargetFiles()[0];
	}

	public File getReflectFile() {
		return this.targetFile;
	}

	private String getFunctionName() {
		return this.ibd.getFunctionName();
	}

	private DeclarationGenerator getFunctionDeclaredType() {
		return this.ibd.getFunctionDeclaredType();
	}

	private String getInFifoType() {
		return getFIFOType(this.ibd.getByteWidth());
	}

	private String getOutFifoType() {
		return getFIFOType(this.obd.getByteWidth());
	}

	/**
	 * generate the input and result vectors via "reflection". Includes the
	 * srcFile, and creates targetFile
	 * 
	 * @param userTestResults
	 *            is true if the user has supplied input parameter generators
	 * @param rootFile
	 *            is the root of the destination file
	 * @param targetFile
	 *            is the .c file to generate
	 */
	public void generateBlockIOReflectFile(boolean userTestResults) {
		int space = 0;
		int iBlockSize = this.ibd.getBlockOrganization().length;
		BlockElement[] ielements = this.ibd.getBlockElements();

		int oBlockWidth = this.obd.getByteWidth();
		int oBlockSize = this.obd.getBlockOrganization().length;
		BlockElement[] oelements = this.obd.getBlockElements();
		StringBuffer inputPadInfo = new StringBuffer();
		StringBuffer outputPadInfo = new StringBuffer();

		// find the index of return in the output block if it exists....
		int returnIndex = -1;
		for (int i = 0; i < oelements.length; i++) {
			if (oelements[i].getFormalName().equals("return")) {
				returnIndex = i;
			}
		}

		printHeader(pw);

		int numPrimitiveVectors = printPrimitiveGenerators(pw, ielements);
		pw.println(pad(space));
		pw.println(pad(space));
		// pw.println("/* \ninput block format: \n"+ibd+"\n\noutput format: "+obd+"\n*/");
		pw.println(pad(space) + "/* space for the input block*/");
		pw.println(pad(space) + getInFifoType() + " ATB_iBlockArray["
				+ iBlockSize + "];");
		pw.println(pad(space));
		pw.println(pad(space)
				+ "/* space for the output block + 1 word for the data valid flag */");
		pw.println(pad(space) + getOutFifoType() + " ATB_oBlockArray["
				+ (oBlockSize + 1) + "];");
		pw.println(pad(space));
		pw.println(pad(space) + "int main(int argc, char * argv[])");
		pw.println(pad(space) + "{");
		space += 4;
		pw.println(pad(space));
		File inputVec = new File(getReflectFile().getParent(), this.baseName
				+ "_input_blocks.vec");
		File outputVec = new File(getReflectFile().getParent(), this.baseName
				+ "_output_expected.vec");
		pw.println(pad(space) + "FILE *ATBinput=fopen(\""
				+ escapeWindowsSlash(inputVec.getAbsolutePath()) + "\",\"w\");");
		pw.println(pad(space) + "FILE *ATBoutput=fopen(\""
				+ escapeWindowsSlash(outputVec.getAbsolutePath())
				+ "\",\"w\");");
		pw.println(pad(space));
		pw.println(pad(space) + "int ATB_i, ATB_j, ATB_num_vectors=0;");
		pw.println(pad(space)
				+ "int ATB_skip;              /* used to handle */");
		pw.println(pad(space)
				+ "int ATB_longjump_return;   /* math exceptions */");
		pw.println(pad(space) + "" + getOutFifoType() + " ATB_valid;");
		pw.println(pad(space) + "");
		pw.println(pad(space) + "(void) signal(SIGFPE, signal_handler);");
		pw.println(pad(space) + "");

		//
		// first do the user specified tests, if desired
		//
		if (userTestResults) {
			pw.println(pad(space) + "for (ATB_i=0; ATB_i<ATB_"
					+ getFunctionName() + "_numTests(); ATB_i++)");
			pw.println(pad(space) + "{");
			space += 4;
			// declare variables for each parameter in the input block
			for (int i = 0; i < ielements.length; i++) {
				String declaration = ielements[i]
						.getDeclaredType()
						.getDeclaration(
								"ATB_arg_" + ielements[i].getFormalName())
						.trim();
				// change array declaration to pointer
				if (declaration.endsWith("]")) {
					declaration = ielements[i].getDeclaredType()
							.getDeclaration("").trim().split("\\[")[0].trim()
							+ " * " + "ATB_arg_" + ielements[i].getFormalName();
				}
				pw.println(pad(space) + declaration + "=ATB_"
						+ getFunctionName() + "_arg_"
						+ ielements[i].getFormalName() + "(ATB_i);");
			}
			if (returnIndex != -1) {
				pw.println(pad(space)
						+ "/* declare the function return for use later */");
				pw.println(pad(space)
						+ getFunctionDeclaredType()
								.getDeclaration("ATB_return") + ";");
			}

			pw.println(pad(space));

			// first declare a char array for each parameter so we can arrange
			// the bytes according to the BlockIOInterface
			pw.println(pad(space)
					+ "/* declare char * for each parameter so we can extract arbitrary bytes */");
			for (int i = 0; i < ielements.length; i++) { // if we are
															// referencing a
															// pointer then we
															// don't wan't to
															// dereference
				boolean pointer = (ielements[i].getDeclaredType()
						.getDeclaration("").trim().endsWith("*") || ielements[i]
						.getDeclaredType().getDeclaration("").trim()
						.endsWith("]"));

				pw.println(pad(space) + "char * ATB_"
						+ ielements[i].getFormalName()
						+ "Ptr=(char *)"
						// if the type isn't a pointer then we need to use
						// the address
						+ (pointer ? " " : " &") + "ATB_arg_"
						+ ielements[i].getFormalName() + ";");
			}
			pw.println(pad(space)
					+ "char * ATB_blockArrayPtr=(char *) ATB_iBlockArray;");
			if (returnIndex != -1) {
				pw.println(pad(space)
						+ "/* declare char * for the result so we can extract arbitrary bytes */");
				pw.println(pad(space)
						+ "char * ATB_returnPtr=(char *)"
						+ (oelements[returnIndex].getDeclaredType()
								.getDeclaration("").trim().endsWith("*") ? " "
								: " &") + "ATB_return;");
			}
			// now assemble the blockArray byte by byte according to the
			// BlockDescriptor
			// now write out the input block into iblockArray

			inputPadInfo.append(writeBlockMapping(pw, ibd, false, space));

			pw.println(pad(space) + "");
			pw.println(pad(space) + "/* now execute the dut */");
			pw.println(pad(space)
					+ "/* first save the current stack in case of an exception */");
			pw.println(pad(space) + "ATB_skip=0;");
			pw.println(pad(space)
					+ "if ((ATB_longjump_return = setjmp(ATB_returnStack)) != 0)");
			pw.println(pad(space) + "{");
			space += 4;
			pw.println(pad(space) + "ATB_valid=0;");
			pw.println(pad(space) + "ATB_skip=1;");
			pw.println(pad(space) + "(void) signal(SIGFPE, signal_handler);\n"
					+ pad(space - 4) + "}");
			space -= 4;
			pw.println(pad(space) + "if (!ATB_skip)\n" + pad(space) + "{");
			space += 4;
			if (returnIndex == -1) // don't expect a return value for void
			// functions
			{
				pw.print(pad(space) + getFunctionName() + "(");
			} else {
				pw.print(pad(space) + "ATB_return=" + getFunctionName() + "(");
			}

			for (int i = 0; i < ielements.length; i++) {
				pw.print("ATB_arg_" + ielements[i].getFormalName());
				if (i < ielements.length - 1) {
					pw.print(", ");
				}
			}
			pw.println(");");
			pw.println(pad(space) + "ATB_valid=1;");
			space -= 4;
			pw.println(pad(space) + "}");
			// now write out the output block into oBlockArray

			pw.println(pad(space)
					+ "ATB_blockArrayPtr=(char *) ATB_oBlockArray;");
			// now assemble the blockArray byte by byte according to the
			// BlockDescriptor
			outputPadInfo.append(writeBlockMapping(pw, obd, true, space));
			// add a word of 00 to match the data_valid string in data
			// blocks
			for (int i = 0; i < oBlockWidth; i++) {
				outputPadInfo.append("00");
			}
			outputPadInfo.append("\\n");
			pw.println(pad(space) + "ATB_num_vectors++;");
			space -= 4;
			pw.println(pad(space) + "}");
		}

		//
		// Next write the code for the generic tests
		//
		// 3 possible cases:
		// case 1 > MAX_VECTORS
		// case 2 <= MAX_VECTORS
		// case 3 no input args
		//
		if (numPrimitiveVectors > 0) {
			if (userTestResults) {
				pw.println(pad(space) + "if (ATB_" + getFunctionName()
						+ "_doGeneric())");
				pw.println(pad(space) + "{");
				space += 4;
			}
			// generic tests case 1: > MAX_VECTORS and case 3 - no input args
			if (numPrimitiveVectors > MAX_VECTORS
					|| (numPrimitiveVectors > 0 && ielements.length == 0)) {
				numPrimitiveVectors = MAX_VECTORS;
				pw.println(pad(space) + "for (ATB_i=0; ATB_i<"
						+ numPrimitiveVectors + "; ATB_i++)\n" + pad(space)
						+ "{");
				space += 4;
				// declare variables for each parameter in the input block
				for (int i = 0; i < ielements.length; i++) {
					pw.println(pad(space)
							+ ielements[i].getDeclaredType().getDeclaration(
									"ATB_arg_" + ielements[i].getFormalName())
							+ "=ATB_" + getFunctionName() + "_generate_"
							+ ielements[i].getFormalName() + "(ATB_i);");
				}
			}
			// generic tests case 2: < MAX_VECTORS but has input parameters
			else if (numPrimitiveVectors < MAX_VECTORS && ielements.length > 0) {
				String function = getFunctionName();
				String[] variableToElement = new String[ielements.length];
				variableToElement[0] = "ATB_i";
				BlockElement e = ielements[0];
				for (int i = 1; i <= ielements.length; i++) {
					pw.println(pad(space) + "for (" + variableToElement[i - 1]
							+ "=0; " + variableToElement[i - 1] + "< ATB_"
							+ function + "_generate_" + e.getFormalName()
							+ "_length; " + variableToElement[i - 1] + "++)");
					pw.println(pad(space) + "{");
					space += 4;
					pw.println(pad(space)
							+ e.getDeclaredType().getDeclaration(
									"ATB_arg_" + e.getFormalName()) + "=ATB_"
							+ function + "_generate_" + e.getFormalName()
							+ "(ATB_i);");

					if (i < ielements.length) {
						e = ielements[i];
						variableToElement[i] = "ATB_i_" + e.getFormalName();
						pw.println(pad(space) + "int " + variableToElement[i]
								+ ";");
					}
				}
			}
			//
			// write the input, call the dut, and write the output
			//
			if (returnIndex != -1) {
				pw.println(pad(space)
						+ "/* declare the function return for use later */");
				pw.println(pad(space)
						+ getFunctionDeclaredType()
								.getDeclaration("ATB_return") + ";");
			}

			pw.println("");

			// first declare a char array for each parameter so we can arrange
			// the bytes according to the BlockIOInterface
			pw.println(pad(space)
					+ "/* declare char * for each parameter so we can extract arbitrary bytes */");
			for (int i = 0; i < ielements.length; i++) {
				// if we are referencing a pointer then we don't wan't to
				// dereference
				boolean pointer = (ielements[i].getDeclaredType()
						.getDeclaration("").trim().endsWith("*") || ielements[i]
						.getDeclaredType().getDeclaration("").trim()
						.endsWith("]"));

				pw.println(pad(space) + "char * ATB_"
						+ ielements[i].getFormalName()
						+ "Ptr=(char *)"
						// if the type isn't a pointer then we need to use
						// the address
						+ (pointer ? " " : " &") + "ATB_arg_"
						+ ielements[i].getFormalName() + ";");
			}
			pw.println(pad(space)
					+ "char * ATB_blockArrayPtr=(char *) ATB_iBlockArray;");
			if (returnIndex != -1) {
				pw.println(pad(space)
						+ "/* declare char * for the result so we can extract arbitrary bytes */");
				pw.println(pad(space)
						+ "char * ATB_returnPtr=(char *)"
						+ (oelements[returnIndex].getDeclaredType()
								.getDeclaration("").trim().endsWith("*") ? " "
								: " &") + "ATB_return;");
			}
			// now assemble the blockArray byte by byte according to the
			// BlockDescriptor
			// now write out the input block into iblockArray
			inputPadInfo.append(writeBlockMapping(pw, ibd, false, space));

			pw.println("");
			pw.println(pad(space) + "/* now execute the dut */");
			pw.println(pad(space)
					+ "/* first save the current stack in case of an exception */");
			pw.println(pad(space) + "ATB_skip=0;");
			pw.println(pad(space)
					+ "if ((ATB_longjump_return = setjmp(ATB_returnStack)) != 0)\n"
					+ pad(space) + "{");
			space += 4;
			pw.println(pad(space) + "ATB_valid=0;");
			pw.println(pad(space) + "ATB_skip=1;");
			pw.println(pad(space) + "(void) signal(SIGFPE, signal_handler);");
			space -= 4;
			pw.println(pad(space) + "}");
			pw.println(pad(space) + "if (!ATB_skip)\n" + pad(space) + "{");
			space += 4;
			if (returnIndex == -1) // don't expect a return value for void
			// functions
			{
				pw.print(pad(space) + getFunctionName() + "(");
			} else {
				pw.print(pad(space) + "ATB_return=" + getFunctionName() + "(");
			}

			for (int i = 0; i < ielements.length; i++) {
				pw.print("ATB_arg_" + ielements[i].getFormalName());
				if (i < ielements.length - 1) {
					pw.print(", ");
				}
			}
			pw.println(");");
			pw.println(pad(space) + "ATB_valid=1;\n" + pad(space - 4) + "}");
			space -= 4;
			// now write out the output block into oBlockArray

			pw.println(pad(space)
					+ "ATB_blockArrayPtr=(char *) ATB_oBlockArray;");
			// now assemble the blockArray byte by byte according to the
			// BlockDescriptor

			outputPadInfo.append(writeBlockMapping(pw, obd, true, space));
			pw.println(pad(space) + "ATB_num_vectors++;");

			// now close the loops
			if (numPrimitiveVectors < MAX_VECTORS && ielements.length > 0)// case
																			// 2
			{
				for (int i = 0; i < ielements.length; i++) {
					space -= 4;
					pw.println(pad(space) + "}");
				}
			} else {
				space -= 4;
				pw.println(pad(space) + "}");
			}

			// add a word of 00 to match the data_valid string in data
			// blocks
			for (int i = 0; i < oBlockWidth; i++) {
				outputPadInfo.append("00");
			}
			outputPadInfo.append("\\n");

			if (userTestResults) {
				pw.println(pad(space) + "}");
				space -= 4;
			}
		}

		pw.println(pad(space)
				+ "/* data/pad flags - ff for each valid byte or 00 for each pad byte in the block */");
		pw.println(pad(space) + "fprintf(ATBinput, \"//pad data\\n"
				+ inputPadInfo.toString() + "\");");
		pw.println(pad(space) + "fprintf(ATBoutput, \"//pad data\\n"
				+ outputPadInfo.toString() + "\");");

		pw.println(pad(space) + "fclose(ATBinput);");
		pw.println(pad(space) + "fclose(ATBoutput);");
		pw.println(pad(space) + "fprintf(stdout,\"%d\\n\", ATB_num_vectors);");
		pw.println(pad(space) + "return(0);");
		space -= 4;
		pw.println(pad(space) + "}");
		pw.close();
	}

	/**
	 * compile & execute the block io reflect file which was pre-generated
	 * returns the number of vectors generated which was output to stdout
	 */
	public int executeBlockIOReflectFile() {
		File inFile = this.getReflectFile();
		String sourceName = inFile.getName();
		File outFile = new File(inFile.getParent(), sourceName.substring(0,
				sourceName.lastIndexOf(".")));
		File[] inFiles = { inFile };

		gcc(outFile, inFiles);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);

		if (exec(outFile.getAbsolutePath(), pw) != 0) {
			EngineThread.getEngine().fatalError(
					"Reflect execution return with non zero exit code");
		}
		int count = -1;

		try {
			count = Integer.valueOf(baos.toString().trim()).intValue();
		} catch (NumberFormatException nfe) {
			EngineThread.getEngine().fatalError(
					"Could not parse the number of vector returned by "
							+ inFile + ": " + baos.toString() + ": " + nfe);
		}

		return count;

	}

	private void printHeader(PrintWriter pw) {
		final String iType = getInFifoType();
		final String oType = getOutFifoType();

		pw.println("#include<stdio.h>");
		pw.println("#include<setjmp.h>");
		pw.println("#include<signal.h>");
		pw.println("");
		pw.println("/* ATB_jmp_buf holds the stack set by longjmp */");
		pw.println("jmp_buf ATB_returnStack;");
		pw.println("");
		pw.println("static void signal_handler(int sig)\n{");
		pw.println("    longjmp(ATB_returnStack, sig);\n}");
		pw.println("");
		pw.println("#ifdef __sparc__");
		pw.println("#    define ATB_bigEndian_platform (1==1)");
		pw.println("#else");
		pw.println("#    define ATB_bigEndian_platform (1==0)");
		pw.println("#endif\n");
		pw.println(iType + " forceToLittleEndianInput (" + iType + " x)");
		pw.println("{");
		// pw.println("#ifdef __sparc__");
		// pw.println("    char * xPtr=(char *)&x;");
		// pw.println("    int i;");
		// pw.println("    "+iType+" result;");
		// pw.println("    char * resultPtr=(char *)&result;");
		// pw.println("    ");
		// pw.println("    for (i=0; i<"+iBlockWidth+"; i++)");
		// pw.println("    {");
		// pw.println("        resultPtr[i]=xPtr["+(iBlockWidth-1)+"-i];");
		// pw.println("    }");
		// pw.println("    return result;");
		// pw.println("#endif");
		// pw.println("#if defined __i386__ || defined __x86_64__");
		pw.println("    return x;");
		// pw.println("#endif");
		pw.println("}");
		pw.println("");
		pw.println(oType + " forceToLittleEndianOutput (" + oType + " x)");
		pw.println("{");
		// pw.println("#ifdef __sparc__");
		// pw.println("    char * xPtr=(char *)&x;");
		// pw.println("    int i;");
		// pw.println("    "+oType+" result;");
		// pw.println("    char * resultPtr=(char *)&result;");
		// pw.println("    ");
		// pw.println("    for (i=0; i<"+oBlockWidth+"; i++)");
		// pw.println("    {");
		// pw.println("        resultPtr[i]=xPtr["+(oBlockWidth-1)+"-i];");
		// pw.println("    }");
		// pw.println("    return result;");
		// pw.println("#endif");
		// pw.println("#if defined __i386__ || defined __x86_64__");
		pw.println("    return x;");
		// pw.println("#endif");
		pw.println("}");
		pw.println("");

		pw.println("/* include the source file under test to avoid having to prototype all the methods we will be testing */");
		pw.println("#define FORGE_ATB_INPUT");
		String fixedPath = escapeWindowsSlash(this.sourceFile.getAbsolutePath());
		pw.println("#include \"" + fixedPath + "\"");
		pw.println("");
		pw.println("");
	}

	/**
	 * looks at the block elements to see if all are primitives. if so, returns
	 * the number of vectors & prints generators for each arg. else returns 0
	 */
	private int printPrimitiveGenerators(PrintWriter pw,
			BlockElement[] ielements) {
		// see if we can generate auto test vectors (only if all params are
		// primitives)
		int space = 0;

		//boolean allPrimitive = true;
		//HashMap types = new HashMap();
		numVectors = 1;
		//int[] argVectors = new int[ielements.length];
		Number[][] vectors = new Number[ielements.length][];

		for (int i = 0; i < ielements.length; i++) {
			String type = ielements[i].getDeclaredType().getDeclaration("")
					.trim();
			String[] typeElements = type.split("\\s");
			String baseType = typeElements[typeElements.length - 1];

			if (baseType.equals("int") || baseType.equals("long")
					|| baseType.equals("short") || baseType.equals("char")
					|| baseType.equals("float") || baseType.equals("double")) {
				vectors[i] = testVector(ielements[i].getAllocatedSize(), false /*
																				 * don
																				 * 't
																				 * support
																				 * floats
																				 * yet
																				 */);
			} else {
				EngineThread.getGenericJob().info(
						"can't generate auto vectors for parameter type: "
								+ type);
				numVectors = 0;
				return numVectors;
			}
		}

		// define the vectors - code taken from non block io case
		// boolean linearVectors = false;
		numVectors = 1;

		for (int i = 0; i < ielements.length; i++) {
			numVectors *= vectors[i].length;
		}
		if (ielements.length == 0) {
			return NUMBER_OF_NOARG_VECTORS;
		}

		String function = ielements[0].getBlockDescriptor().getFunctionName();

		for (int i = 0; i < ielements.length; i++) {
			BlockElement e = ielements[i];

			pw.println("\n" + pad(space) + "/* auto generate values of type "
					+ e.getDeclaredType().getDeclaration("") + " */");

			pw.println(pad(space) + "#define ATB_" + function + "_generate_"
					+ e.getFormalName() + "_length " + vectors[i].length);
			pw.println(pad(space)
					+ ielements[i].getDeclaredType().getDeclaration("")
					+ " ATB_" + function + "_generate_" + e.getFormalName()
					+ " (int sequence)\n" + pad(space) + "{");
			space += 4;
			pw.println(pad(space)
					+ ielements[i].getDeclaredType().getDeclaration("")
					+ " r[]=\n" + pad(space) + "{");
			space += 4;
			for (int j = 0; j < vectors[i].length; j++) {
				switch (ielements[i].getAllocatedSize()) {
				case 1:
					pw.println(pad(space)
							+ "0x"
							+ Integer.toHexString(0xff & vectors[i][j]
									.byteValue()) + ",");
					break;
				case 2:
					pw.println(pad(space)
							+ "0x"
							+ Integer.toHexString(0xffff & vectors[i][j]
									.shortValue()) + ",");
					break;
				case 4:
					pw.println(pad(space) + "0x"
							+ Integer.toHexString(vectors[i][j].intValue())
							+ ",");
					break;
				case 8:
					pw.println(pad(space) + "0x"
							+ Long.toHexString(vectors[i][j].longValue())
							+ "LL,");
					break;
				}
			}
			space -= 4;
			pw.println(pad(space) + "};");
			pw.println("\n" + pad(space) + "return(r[sequence % ATB_"
					+ function + "_generate_" + e.getFormalName()
					+ "_length]);");
			space -= 4;
			pw.println(pad(space) + "}");
		}

		return numVectors;
	}

	/**
	 * return the length of the test vectors for the given bit width floating is
	 * true for floating point, false for integer
	 */
	private Number[] testVector(int size, boolean floating) {
		Number[] result;
		if (floating) {
			return new Number[0];
		}

		switch (size) {
		case 1:
			result = new Number[BYTE_TV.length];
			for (int i = 0; i < BYTE_TV.length; i++) {
				result[i] = new Byte(BYTE_TV[i]);
			}
			return result;
		case 2:
			result = new Number[SHORT_TV.length];
			for (int i = 0; i < SHORT_TV.length; i++) {
				result[i] = new Short(SHORT_TV[i]);
			}
			return result;
		case 4:
			result = new Number[INT_TV.length];
			for (int i = 0; i < INT_TV.length; i++) {
				result[i] = new Integer(INT_TV[i]);
			}
			return result;
		case 8:
			result = new Number[LONG_TV.length];
			for (int i = 0; i < LONG_TV.length; i++) {
				result[i] = new Long(LONG_TV[i]);
			}
			return result;
		default:
			result = new Number[0];
			return result;
		}
	}

	/**
	 * @return type that corresponds to a given blockWidth
	 */
	private String getFIFOType(int blockWidth) {
		String result;
		switch (blockWidth) {
		case 1:
			result = "char";
			break;
		case 2:
			result = "short";
			break;
		case 4:
			result = "int";
			break;
		case 8:
			result = "long long";
			break;
		default:
			result = "UNKNOWN";
			EngineThread.getEngine().fatalError(
					"Non supported fifo width in generateBlockIOReflectFile: "
							+ blockWidth);
		}
		return result;
	}

	/**
	 * write c code to map parameters into a block. returns a String of pad
	 * information by byte: ff if the byte is valid, 00 if the byte is padding.
	 * 
	 * @param dataValid
	 *            true if dataValid to be written, and false if not
	 * @param space
	 *            is the number of leading spaces to print
	 */
	private String writeBlockMapping(PrintWriter pw, BlockDescriptor bd,
			boolean dataValid, int space) {
		BlockElement[] elements = bd.getBlockElements();
		// String padInfo="";
		StringBuffer padInfo = new StringBuffer();

		int[] streamOffsets = new int[elements.length];
		int[] ptrOffsets = new int[elements.length];

		int[] blockOrganization = bd.getBlockOrganization();
		int blockSize = blockOrganization.length;
		int blockWidth = bd.getByteWidth();
		int index = 0;

		for (int i = 0; i < blockSize; i++) {
			@SuppressWarnings("unused")
			int padBits = 0;
			int padIndex = 0;
			StringBuffer blockPadInfo = new StringBuffer();
			blockPadInfo.append("\\n");

			int element = blockOrganization[i];
			byte[] streamFormat = elements[element].getStreamFormat();
			pw.println(pad(space) + "/* begin block element " + i + " ("
					+ elements[element].getFormalName() + ") */");
			/*
			 * this makes the code really ugly... the stream format (data or
			 * pad) is endian agnostic, but the method of choosing data elements
			 * with char * is endian dependent.... so if the target that we are
			 * running the _reflect file is big endian, we need to reverse the
			 * offsets to the ATB_*Ptr[] within the block.
			 * 
			 * The following code really supports only full blocks and single
			 * byte per block blocks as that is all the forge currently
			 * produces. It also doesn't support interleaved blocks (where
			 * different parameters are interleaved)
			 */
			boolean allData = true;
			int streamOffsetsElement = streamOffsets[element];
			for (int j = 0; j < blockWidth; j++) {
				if (streamFormat[streamOffsetsElement + j] == BlockElement.PAD) {
					allData = false;
				}
			}
			if (allData) {
				for (int j = 0; j < blockWidth; j++) {
					if (streamFormat[streamOffsets[element]] == BlockElement.DATA) {
						pw.println(pad(space) + "ATB_blockArrayPtr[" + (index)
								+ "]=ATB_" + elements[element].getFormalName()
								+ "Ptr[" + ptrOffsets[element] + "];");

						blockPadInfo.insert(0, "ff");
						padBits |= 1 << padIndex;
						index++;
						ptrOffsets[element]++;
					} else { // if its a PAD then we don't need to write
								// anything out
						index++;
						blockPadInfo.insert(0, "00");
						throw new org.xronos.openforge.app.ForgeFatalException(
								"found pad where only data should be in ATB generation");
					}
					padIndex++;
					streamOffsets[element]++;
				}
			} else {
				// we only have one byte of valid data - make sure its the low
				// order byte
				for (int j = 0; j < blockWidth; j++) {
					if (streamFormat[streamOffsets[element]] == BlockElement.DATA) {
						int base = (index / blockWidth) * blockWidth;
						int offset = index % blockWidth;

						pw.println(pad(space) + "if (ATB_bigEndian_platform)\n"
								+ pad(space) + "{");
						space += 4;

						pw.println(pad(space) + "ATB_blockArrayPtr["
								+ (base + (blockWidth - offset - 1)) + "]=ATB_"
								+ elements[element].getFormalName() + "Ptr["
								+ ptrOffsets[element] + "];");

						space -= 4;
						pw.println(pad(space) + "}\n" + pad(space) + "else\n"
								+ pad(space) + "{");
						space += 4;

						pw.println(pad(space) + "ATB_blockArrayPtr[" + (index)
								+ "]=ATB_" + elements[element].getFormalName()
								+ "Ptr[" + ptrOffsets[element] + "];");
						space -= 4;
						pw.println(pad(space) + "}");

						blockPadInfo.insert(0, "ff");
						padBits |= 1 << padIndex;
						index++;
						ptrOffsets[element]++;
					} else { // if its a PAD then we don't need to write
								// anything out
						index++;
						blockPadInfo.insert(0, "00");
					}
					padIndex++;
					streamOffsets[element]++;
				}

			}
			padInfo.append(blockPadInfo.toString());
			// padInfo+=blockPadInfo;

			pw.flush();
		}

		String[] printfType = { "ERR", "02hhx", "04hx", "ERR", "08x", "ERR",
				"ERR", "ERR", "016llx" };
		String fileStar = (bd.isSlave() ? "ATBinput" : "ATBoutput");

		pw.println(pad(space) + "fprintf(" + fileStar
				+ ", \"// block for iteration %d\\n\",ATB_i);");
		if (dataValid) {
			pw.println(pad(space) + "fprintf(" + fileStar + ", \"%"
					+ printfType[blockWidth] + "\\n\", ATB_valid);");
		}
		pw.println(pad(space) + "for (ATB_j=0; ATB_j<" + blockSize
				+ "; ATB_j++)");
		pw.println(pad(space) + "{");
		space += 4;
		// print to either input or output FILE*
		pw.println(pad(space) + "fprintf(" + fileStar + ", \"%"
				+ printfType[blockWidth] + "\\n\",forceToLittleEndian"
				+ (bd.isSlave() ? "Input(ATB_i" : "Output(ATB_o")
				+ "BlockArray[ATB_j]));");
		space -= 4;
		pw.println(pad(space) + "}");
		return padInfo.toString();
	}

	private void gcc(File outputFile, File[] srcfiles) {
		GenericJob gj = EngineThread.getGenericJob();
		List<String> incList = gj.getIncludeDirList();

		String[] args = new String[6 + srcfiles.length + (2 * incList.size())];
		int index = 0;
		args[index++] = "-m32";
		args[index++] = "-Wno-long-long";
		args[index++] = "-Wall";
		args[index++] = "-Werror";
		args[index++] = "-o";
		args[index++] = outputFile.getAbsolutePath();

		for (String string: incList) {
			args[index++] = "-I";
			args[index++] = string;
		}

		for (int i = 0; i < srcfiles.length; i++) {
			args[index++] = srcfiles[i].getAbsolutePath();
		}

		GCC gcc = new GCC();
		try {
			if (gcc.exec(args, System.out, System.err) != 0) {
				StringBuffer buf = new StringBuffer();
				for (int i = 0; i < args.length; i++) {
					buf.append(args[i]);
					if ((i + 1) < args.length)
						buf.append(" ");
				}
				EngineThread.getEngine().fatalError(
						"GCC returned non zero exit value for: gcc "
								+ buf.toString()
								+ "; can't complete test bench generation: ");
			}
		} catch (ExecutionException ioe) {
			EngineThread.getEngine().fatalError(
					"IOError during gcc execution: " + ioe.getMessage());
		} catch (IOException ioe) {
			EngineThread.getEngine().fatalError(
					"IOError during gcc execution: " + ioe.getMessage());
		}
	}

	private int exec(String commandLine, PrintWriter out) {
		Process process = null;

		try {
			process = Runtime.getRuntime().exec(commandLine);
		} catch (java.io.IOException ioe) {
			EngineThread.getEngine().fatalError(
					"IOError during reflect execution: " + ioe.getMessage());
		}

		final Drain outConduit = new Drain(process.getInputStream(), out);
		final Drain errConduit = new Drain(process.getErrorStream());

		try {
			process.getOutputStream().close();
			process.waitFor();
			outConduit.waitFor();
			errConduit.waitFor();
		} catch (InterruptedException eInterrupted) {
			process.destroy();
			EngineThread.getEngine().fatalError(
					"Thread interrupted during refelct execution: ");
		} catch (IOException ioe) {
			EngineThread.getEngine().fatalError(
					"IOError during reflect execution: " + ioe.getMessage());
		}

		return process.exitValue();
	}

	/**
	 * FIXME: get from c.design.TestGenerator
	 */

	// TV stands for test vectors, i.e. the numbers we will
	// use when testing primitive types of the given type.
	public static final boolean[] BOOLEAN_TV = { true, false };

	public static final byte[] BYTE_TV = { Byte.MIN_VALUE, Byte.MIN_VALUE + 1,
			Byte.MAX_VALUE, Byte.MAX_VALUE - 1, -2, -1, 0, 1, 2, -62, -63, -64,
			62, 63, 64, (byte) 0x12, (byte) 0xab, (byte) 0xa5, (byte) 0x5a };

	public static final char[] CHAR_TV = { Character.MIN_VALUE,
			(char) (Character.MIN_VALUE + 1), Character.MAX_VALUE,
			(char) (Character.MAX_VALUE - 1), 0, 1, 2, 32767, 32768, 32769,
			0x1234, 0xabcd, 0xa5a5, 0x5a5a };

	public static final short[] SHORT_TV = { Short.MIN_VALUE,
			(short) (Short.MIN_VALUE + 1), Short.MAX_VALUE,
			(short) (Short.MAX_VALUE - 1), -2, -1, 0, 1, 2, -16383, -16384,
			-16385, 16383, 16384, 16385, (short) 0x1234, (short) 0xabcd,
			(short) 0xa5a5, (short) 0x5a5a };

	public static final int[] INT_TV = { Integer.MIN_VALUE,
			Integer.MIN_VALUE + 1, Integer.MAX_VALUE, Integer.MAX_VALUE - 1,
			-2, -1, 0, 1, 2, 0xc0000001, 0xc0000000, 0xbfffffff, 0x3fffffff,
			0x40000000, 0x40000001, 0x12345678, 0xabcdef01, 0xa5a5a5a5,
			0x5a5a5a5a };

	public static final long[] LONG_TV = { Long.MIN_VALUE, Long.MIN_VALUE + 1,
			Long.MAX_VALUE, Long.MAX_VALUE - 1, -2, -1, 0, 1, 2,
			0xc000000000000001L, 0xc000000000000000L, 0xbfffffffffffffffL,
			0x123456789abcdef0L, 0xabcdef0123456789L, 0xa5a5a5a5a5a5a5a5L,
			0x5a5a5a5a5a5a5a5aL };

	public static final float[] FLOAT_TV = { Float.MIN_VALUE,
			(float) (Float.MIN_VALUE + (float) 1.0), Float.MAX_VALUE,
			(float) (Float.MAX_VALUE - (float) 1.0), (float) -2.0,
			(float) -1.0, (float) 0.0, (float) 1.0, (float) 2.0, Float.NaN,
			Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY };

	public static final double[] DOUBLE_TV = { Double.MIN_VALUE,
			Double.MIN_VALUE + 1.0, Double.MAX_VALUE, Double.MAX_VALUE - 1.0,
			-2.0, -1.0, 0.0, 1.0, 2.0, Double.NaN, Double.NEGATIVE_INFINITY,
			Double.POSITIVE_INFINITY };

	/**
	 * return spaces spaces
	 */
	private final String pad(int spaces) {
		if (spaces < 0) {
			return "/*ERROR*/";
		}

		final String[] s = { "", " ", "  ", "   ", "    ", "     ", "      ",
				"       ", "        ", "         ", "          ",
				"           ", "            ", "             ",
				"              ", "               ", "                ",
				"                 ", "                  ",
				"                   ", "                    ",
				"                     ", "                      ",
				"                       ", "                        ",
				"                         ", "                          ",
				"                           ", "                            ",
				"                             ",
				"                              ",
				"                               " };
		if (spaces < s.length) {
			return s[spaces];
		}
		String result = s[s.length - 1];
		while (result.length() < spaces) {
			result += " ";
		}
		return result;
	}

	private static String escapeWindowsSlash(String in) {
		String ret = "";
		for (int i = 0; i < in.length(); i++) {
			if (in.substring(i, i + 1).equals("\\"))
				ret = ret + "\\";
			ret = ret + in.charAt(i);
		}
		return ret;
	}

}// BlockIOReflection
