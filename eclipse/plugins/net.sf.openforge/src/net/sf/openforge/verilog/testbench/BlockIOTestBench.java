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

package net.sf.openforge.verilog.testbench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.ForgeFileHandler;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.backend.hdl.TestBenchEngine;
import net.sf.openforge.backend.hdl.VerilogTranslateEngine;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.io.BlockDescriptor;
import net.sf.openforge.lim.io.BlockIOInterface;
import net.sf.openforge.util.naming.ID;

/**
 * <code>TestGenerator</code> uses {@link BlockIOInterface} to determines the
 * entry method and its input variable types, then creates a vector set for each
 * entry method and finally executes the actual method by generating a C
 * program, compiling it with gcc and running it to collect the generated
 * results. This class is usefull for automated testing, where a test program is
 * supplied and most (if not all) of the corner case input values are executed
 * and the results captured for later use. The results of the black box testing
 * are used to generate an expected value file.
 * 
 * @author <a href="mailto:Jim.Jensen@xilinx.com"> Jim Jensen & Ian Miller based
 *         on Jonathan's non-block io version</a>
 * @version $Id: BlockIOTestBench.java 288 2006-08-15 17:31:43Z imiller $
 */
public class BlockIOTestBench {

	/**
	 * FIXME: get from c.design ?
	 */

	// temporary storage of source stuff to be used when init is
	// called on us after naming has been imposed on the graph. These
	// 3 fields get initialized by the constructor, but nulled out
	// after the init() method is called to avoid holding on the large
	// amounts of runtime memory.
	Design design = null;

	// flag which is set when tests are run
	boolean testsRun = false;

	/**
	 * Creates a new <code>TestGenerator</code> instance. Vectors are generated
	 * based on the BlockIOInterface
	 * 
	 * @param design
	 *            a <code>Design</code> value
	 */
	public BlockIOTestBench(Design design) {
		this.design = design;
	}

	/**
	 * <code>runTests</code> generates a C program to generate expected results,
	 * and then builds a verilog testbench based on those results
	 */
	public void runTests() {
		if (!testsRun) {
			// 4. Create black box vector sets for each method based on
			// the input arguments, call the method and store the
			// results. Any exceptions thrown mark the given
			// result as invalid.
			if (!EngineThread.getGenericJob().getUnscopedBooleanOptionValue(
					OptionRegistry.NO_BLOCK_IO)) {
				generateBlocks();
			}
			testsRun = true;
		}
	}

	/**
	 * gcc -E -DFORGE_ATB_INPUT the target file and search for the pattern;
	 * return 0 if found, 1 if not found, -1 if exec error. uses gcc and grep in
	 * current path
	 */
	private int cppGrep(String pattern, File targetFile) {

		String[] cmdArray = {
				"sh",
				"-c",
				"gcc -m32 -E -DFORGE_ATB_INPUT " + targetFile.getName()
						+ "| grep " + pattern };

		Process grep = null; // returns 0 if found, 1 if not found
		int grepResult = -1;
		try {
			grep = Runtime.getRuntime().exec(cmdArray);
			grep.waitFor();
			grepResult = grep.exitValue();
		} catch (IOException ioe) {
			String cmd = "";
			for (int i = 0; i < cmdArray.length; i++) {
				cmd += cmdArray[i] + " ";
			}

			EngineThread.getEngine().fatalError(
					"couldn't process " + cmd + ":" + ioe);
		} catch (InterruptedException ie) {
			grep.destroy();
			EngineThread.getEngine().fatalError("thread interrupted: " + ie);
		}
		return (grepResult);
	}

	/***************************************************************************************/
	/***************************************************************************************/
	/** handle blockio tests *****************************************************************/
	/***************************************************************************************/
	/***************************************************************************************/

	/**
	 * do the "grunt work" of generating a _reflect program to generate the
	 * input vectors and the correct results
	 */
	private void generateBlocks() {
		GenericJob gj = EngineThread.getGenericJob();

		Set<String> functionNames = BlockIOInterface.getFunctionNames();
		// handle each function, currently there is just one...
		for (String functionName : functionNames) {

			// currently we expect an input and an output block descriptor
			BlockDescriptor inputBlock = null;
			BlockDescriptor outputBlock = null;

			for (BlockDescriptor bd : BlockIOInterface
					.getDescriptors(functionName)) {
				if (bd.isSlave()) {
					inputBlock = bd;
				} else {
					outputBlock = bd;
				}
			}
			// sanity check to make sure we have one of each
			if (inputBlock == null || outputBlock == null) {
				EngineThread.getEngine().fatalError(
						"missing an input or output block descriptor");
			}

			// now generate the input vectors:

			// first lets find out if there are user supplied tests
			File srcFile = gj.getTargetFiles()[0];
			int grepResult = cppGrep("ATB_" + inputBlock.getFunctionName()
					+ "_numTests", srcFile);

			boolean userTestInputs;

			if (grepResult != 0) {
				userTestInputs = false;
			} else {
				userTestInputs = true;
			}

			BlockIOReflection bior = new BlockIOReflection(inputBlock,
					outputBlock);
			bior.generateBlockIOReflectFile(userTestInputs);

			// int totalVectors=executeBlockIOReflectFile(bior);
			int totalVectors = bior.executeBlockIOReflectFile();

			gj.info("    generated " + totalVectors + " test vectors");

			ForgeFileHandler fileHandler = EngineThread.getGenericJob()
					.getFileHandler();
			generateVerilogFile(fileHandler, inputBlock, outputBlock,
					totalVectors);
		}
	}

	/**
	 * generate the verilog test fixture
	 * 
	 * @param numBlocks
	 *            is the number of input and output blocks in the testbench
	 */
	private void generateVerilogFile(ForgeFileHandler fileHandler,
			BlockDescriptor ibd, BlockDescriptor obd, int numBlocks) {
		final String taskName = ibd.getFunctionName();
		// size of an input block in words
		final int inBlockSize = ibd.getBlockOrganization().length;
		// size of an output block in words
		final int outBlockSize = obd.getBlockOrganization().length;
		// size of an output block including the valid word in words
		final int outBlockSizeValid = outBlockSize + 1;
		// number of blocks including the pad block
		final int numBlocksPad = numBlocks + 1;
		// size of array to map pad data to pad mask
		// final int outputPadSize = 1 << obd.getByteWidth();
		// size in bits of an input word (0 base)
		final int inFifoBits = ibd.getByteWidth() * 8 - 1;
		// size in bits of an output word (0 base)
		final int outFifoBits = obd.getByteWidth() * 8 - 1;
		// number of words in input vector, excluding pad data
		final int inBlocksWords = inBlockSize * numBlocks;
		// number of words in input vector, including pad data
		final int inBlocksWordsPad = inBlockSize * numBlocksPad;
		// number of words in output vector, excluding pad data
		// final int outBlocksWords = outBlockSize * numBlocks;
		// number of words in output vector, excluding pad data and valid words
		final int outBlocksWordsValid = outBlockSizeValid * numBlocks;
		// number of words in output vector, including pad data
		// final int outBlocksWordsPad = outBlockSize * numBlocksPad;
		// number of words in output vector, including pad data and valid words
		final int outBlocksWordsPadValid = outBlockSizeValid * numBlocksPad;
		// true if there is input to the dut
		final boolean isInputFifo = (inBlockSize > 0);
		final boolean isOutputFifo = (outBlockSize > 0);
		String designVerilogIdentifier = ID.toVerilogIdentifier(ID
				.showLogical(design));

		/* Benchmarking stuff */
		boolean atbGenerateBMReport = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(
						OptionRegistry.ATB_BENCHMARK_REPORT);
		boolean dumpCycles = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(OptionRegistry.WRITE_CYCLE_C);
		/* END Benchmarking stuff */

		try {
			int space = 0;

			// PrintWriter pw=new PrintWriter(new FileWriter(srcRoot+"_atb.v"));
			PrintWriter pw = new PrintWriter(new FileWriter(
					fileHandler.getFile(TestBenchEngine.ATB)));

			pw.println("`timescale 1ns/1ps");
			pw.println("`define legacy_model // Some simulators cannot handle the syntax of the new memory models.  This define uses a simpler syntax for the memory models in the unisims library");
			final String simFile = fileHandler.getFile(
					VerilogTranslateEngine.SIMINCL).getAbsolutePath();
			pw.println("`include \"" + simFile + "\"");
			pw.println("`timescale 1ns/1ps");
			pw.println("");
			pw.println("module fixture();");
			space += 3;
			pw.println("");
			pw.println(pad(space) + "//io");
			pw.println(pad(space) + "integer        resultFile;");
			pw.println(pad(space) + "integer        clkCntFile;");
			if (dumpCycles) {
				pw.println(pad(space) + "integer        cycleFile;");
			}

			/* BENCHMARKING Stuff */
			if (atbGenerateBMReport) {
				pw.println(pad(space) + "/* benchmarking .. */");
				pw.println(pad(space) + "integer    benchmarkReportFile;");
				pw.println(pad(space) + "integer    benchNumReads;");
				pw.println(pad(space) + "integer    benchConsumedBytes;");
				pw.println(pad(space) + "integer        benchNumWrites;");
				pw.println(pad(space) + "integer    benchProducedBytes;");
				pw.println(pad(space) + "integer        benchFirstReadCycle;");
				pw.println(pad(space) + "integer        benchLastWriteCycle;");
				pw.println(pad(space) + "integer    benchWorkInProgress;");
				pw.println(pad(space) + "real       benchThroughput;");
				pw.println(pad(space) + "integer    benchTotalCycles;");
				pw.println(pad(space) + "real       benchOverallInputUtil;");
				pw.println(pad(space) + "real       benchOverallOutputUtil;");
				pw.println(pad(space) + "real       benchOverallCoreUtil;");
				pw.println(pad(space) + "real       benchZoneInputUtil;");
				pw.println(pad(space) + "real       benchZoneOutputUtil;");
				pw.println(pad(space) + "real       benchZoneCoreUtil;");
				pw.println(pad(space)
						+ "integer        benchCoreCyclesCounter;");
				pw.println(pad(space) + "integer        benchCoreCycles;");
				pw.println(pad(space)
						+ "integer        benchIdleCyclesCounter;");
				pw.println(pad(space) + "integer        benchIdleCycles;");
				pw.println(pad(space) + "integer        benchIdleFlag;");
				pw.println(pad(space) + "real       benchIdlePercentage;");
				pw.println(pad(space) + "integer        benchInReadZone;");
				pw.println(pad(space) + "integer        benchInWriteZone;");
				pw.println(pad(space) + "integer        benchReadZoneCycles;");
				pw.println(pad(space)
						+ "integer        benchReadZoneCyclesCounter;");
				pw.println(pad(space)
						+ "integer        benchReadZoneCyclesCounterSave;");
				pw.println(pad(space) + "integer        benchWriteZoneCycles;");
				pw.println(pad(space)
						+ "integer        benchWriteZoneCyclesCounter;");
				pw.println(pad(space)
						+ "integer        benchWriteZoneCyclesCounterSave;");
				pw.println(pad(space) + "integer        benchCoreZoneCycles;");
				pw.println(pad(space)
						+ "integer        benchCoreZoneCyclesCounter;");
				pw.println(pad(space) + "/* End benchmarking .. */");
			}
			/* END BENCHMARKING Stuff */

			pw.println(pad(space) + "//clock");
			pw.println(pad(space) + "reg            clk;");
			pw.println(pad(space) + "// set/reset");
			pw.println(pad(space) + "reg            LGSR;");

			final String results = fileHandler.getFile(TestBenchEngine.RESULTS)
					.getAbsolutePath();
			final String resultsClocks = fileHandler.getFile(
					TestBenchEngine.DefaultTestBenchEngine.CLKS)
					.getAbsolutePath();
			final String cycleResults = fileHandler.getFile(
					TestBenchEngine.BlockIOTestBenchEngine.CYCLES)
					.getAbsolutePath();
			if (!isInputFifo && !isOutputFifo) {
				// this is a no arg/no output design. instantiate, delay 500,
				// and exit
				pw.println(pad(space)
						+ "//initial begin  $dumpfile(\"waves.vcd\");  $dumpvars;end   ");
				pw.println(pad(space) + "assign glbl.GSR=LGSR;");
				pw.println(pad(space));
				pw.println(pad(space) + "initial");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space) + "resultFile <= $fopen (\"" + results
						+ "\");");
				pw.println(pad(space) + "clkCntFile <= $fopen (\""
						+ resultsClocks + "\");");
				if (dumpCycles) {
					pw.println(pad(space) + "cycleFile <= $fopen (\""
							+ cycleResults + "\");");
				}

				if (atbGenerateBMReport) {
					writeBenchmarkInit(pw, fileHandler, space);
				}

				pw.println(pad(space) + "clk <= 0;");
				pw.println(pad(space));
				pw.println(pad(space) + "#1 LGSR <= 0;");
				pw.println(pad(space));
				pw.println(pad(space)
						+ "#1000 $fwrite (resultFile, \"PASSED\");");
				pw.println(pad(space));
				pw.println(pad(space) + "#1000 $finish(1);");
				pw.println(pad(space));
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println("");
				pw.print(pad(space) + "" + designVerilogIdentifier + " dut();");
				pw.println("");
				pw.println(pad(space) + "always #25 clk <= ~clk;");
				pw.println("");
				space -= 3;
			} else {
				pw.println(pad(space)
						+ "// input data[block * cycle + 1 cycle for good/pad bytes]");
				pw.println(pad(space) + "// inBlockSize: " + inBlockSize
						+ " outBlockSize: " + numBlocksPad);
				pw.println(pad(space) + "reg  [" + inFifoBits
						+ ":0]    FSL_input[0:" + (inBlocksWordsPad)
						+ "]; // sized to hold " + numBlocks
						+ " input blocks of data and one block of pad");
				pw.println(pad(space)
						+ "reg  ["
						+ outFifoBits
						+ ":0]    FSL_output[0:"
						+ (outBlocksWordsPadValid)
						+ "]; // sized to hold "
						+ numBlocks
						+ " output blocks of expected data, each including one word of valid, and one block of pad");
				pw.println(pad(space));
				pw.println(pad(space) + "reg  [" + outFifoBits
						+ ":0]       out[0:" + (outBlockSize)
						+ "];    // sized to hold one output block");
				pw.println(pad(space)
						+ "reg  [31:0]    din_index;    // hold the index into FSL_input");
				pw.println(pad(space)
						+ "reg  [31:0]    dout_index;   // hold the index into FSL_output");
				pw.println(pad(space)
						+ "reg  [31:0]    dout_in_block_index; // holds the index within the current output block");
				pw.println(pad(space) + "reg            task_" + taskName
						+ "_fail;");
				pw.println(pad(space) + "reg            task_" + taskName
						+ "_finished;");
				pw.println(pad(space) + "reg  [" + outFifoBits
						+ ":0]       dout_valid_index;   ");
				pw.println(pad(space)
						+ "wire  ["
						+ outFifoBits
						+ ":0]      dout_valid;   // stays the same for an entire output block  ");
				pw.println(pad(space) + "reg  [31:0]    hangTimer;");
				pw.println(pad(space) + "reg  [31:0]    clockCount;");
				pw.println(pad(space) + "wire           read;");
				pw.println(pad(space) + "wire           write;");
				pw.println(pad(space) + "reg            inDataExists;");
				pw.println(pad(space) + "reg            outDataFull;");
				pw.println(pad(space) + "wire           fsl1_m_control;");
				pw.println(pad(space) + "wire [" + inFifoBits + ":0]    din;");
				pw.println(pad(space) + "wire [" + outFifoBits + ":0]   dout;");
				pw.println(pad(space)
						+ "wire ["
						+ outFifoBits
						+ ":0]   expected; // expected value for current FSL1_M fifo data");
				pw.println(pad(space)
						+ "wire ["
						+ outFifoBits
						+ ":0]   current_pad;  // a mask with ff for each data byte, and 0 for each pad byte");
				pw.println(pad(space) + "reg             startSimulation;");
				if (dumpCycles) {
					pw.println(pad(space)
							+ "reg  [31:0]     relativeCycleCount;");
				}
				pw.println(pad(space) + "");
				pw.println(pad(space) + "");
				pw.println(pad(space));
				pw.println(pad(space)
						+ "//initial begin  $dumpfile(\"waves.vcd\");  $dumpvars;end   ");
				pw.println(pad(space) + "assign glbl.GSR=LGSR;");
				pw.println(pad(space));
				pw.println(pad(space) + "initial");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				final String inVecs = fileHandler.getFile(
						TestBenchEngine.BlockIOTestBenchEngine.INVECS)
						.getAbsolutePath();
				final String outVecs = fileHandler.getFile(
						TestBenchEngine.BlockIOTestBenchEngine.OUTVECS)
						.getAbsolutePath();
				pw.println(pad(space) + "$readmemh(\"" + inVecs
						+ "\", FSL_input);");
				pw.println(pad(space) + "$readmemh(\"" + outVecs
						+ "\", FSL_output);");
				pw.println(pad(space) + "LGSR <= 1;");
				pw.println(pad(space) + "hangTimer <= 0;");
				pw.println(pad(space) + "");
				pw.println(pad(space) + "resultFile <= $fopen (\"" + results
						+ "\");");
				pw.println(pad(space) + "clkCntFile <= $fopen (\""
						+ resultsClocks + "\");");
				if (dumpCycles) {
					pw.println(pad(space) + "cycleFile <= $fopen (\""
							+ cycleResults + "\");");
				}

				if (atbGenerateBMReport) {
					writeBenchmarkInit(pw, fileHandler, space);
				}

				pw.println(pad(space) + "clk <= 0;");
				pw.println(pad(space) + "inDataExists <= 0;");
				pw.println(pad(space)
						+ "dout_index <= 1;      // first element is valid data flag ");
				pw.println(pad(space) + "dout_in_block_index <= 0;");
				pw.println(pad(space) + "task_" + taskName + "_fail <= 0;");
				pw.println(pad(space) + "task_" + taskName + "_finished <= 0;");
				pw.println("");
				pw.println(pad(space) + "");
				pw.println(pad(space) + "outDataFull<=0;");
				pw.println(pad(space) + "clockCount <= 0;");
				pw.println(pad(space) + "din_index <= 0;");
				pw.println(pad(space) + "startSimulation <= 0;");
				pw.println(pad(space) + "");
				pw.println(pad(space) + "dout_valid_index <= 0;");
				if (dumpCycles) {
					pw.println(pad(space) + "relativeCycleCount <= 0;");
				}
				pw.println("");
				pw.println(pad(space) + "#1 LGSR <= 0;");
				pw.println(pad(space) + "");
				pw.println(pad(space) + "#500 startSimulation <= 1;");
				pw.println(pad(space)
						+ "#25 startSimulation <= 0; // stay high for half a clock");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println("");

				pw.print(pad(space) + "" + designVerilogIdentifier + " dut(");
				if (isInputFifo) {
					pw.println(".FSL0_S_READ(read),   .FSL0_S_DATA(din),  .FSL0_S_EXISTS(inDataExists), ");
					pw.println(pad(space)
							+ pad(designVerilogIdentifier.length() + 5)
							+ ".FSL0_S_CONTROL(1'b0), .FSL0_S_CLK(clk),");
					pw.print(pad(space + designVerilogIdentifier.length() + 5));
				}
				pw.println(".FSL1_M_WRITE(write), .FSL1_M_DATA(dout), .FSL1_M_FULL(outDataFull),");
				pw.println(pad(space + designVerilogIdentifier.length() + 5)
						+ ".FSL1_M_CONTROL(fsl1_m_control), .FSL1_M_CLK(clk),");

				pw.println(pad(space + designVerilogIdentifier.length() + 5)
						+ ".RESET(1'b0), // GSR will hit internal reset at start");

				pw.println(pad(space + designVerilogIdentifier.length() + 5)
						+ ".CLK(clk));");
				pw.println("");
				pw.println(pad(space) + "always #25 clk <= ~clk;");
				pw.println("");
				pw.println(pad(space) + "assign din=FSL_input[din_index];");
				pw.println(pad(space)
						+ "assign expected=FSL_output[dout_index];");
				pw.println(pad(space)
						+ "assign current_pad=FSL_output[dout_in_block_index+("
						+ (outBlockSizeValid * numBlocks) + ")];");
				pw.println(pad(space)
						+ "assign dout_valid=FSL_output[dout_valid_index];");
				pw.println("");
				pw.println("   //send to input fifo and read from output fifo");
				pw.println(pad(space) + "always @(posedge clk)");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;

				if (dumpCycles) {
					pw.println(pad(space)
							+ "if ((relativeCycleCount > 0) || read || write) begin");
					space += 2;
					pw.println(pad(space)
							+ "$fwrite(cycleFile, \"%x %x %x %x %x\\n\", relativeCycleCount, din, read, dout, write);");
					pw.println(pad(space)
							+ "relativeCycleCount <= relativeCycleCount + 1;");
					space -= 2;
					pw.println(pad(space) + "end");
				}

				if (atbGenerateBMReport) {
					writeBenchmarkAtClockEdge(pw, space, outBlocksWordsValid,
							numBlocks);
				}

				pw.println(pad(space) + "if (startSimulation === 1)");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space) + "inDataExists <= 1;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space) + "else if (read && din_index === "
						+ (inBlocksWords - 1)
						+ ") // reached the end of the input data");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;

				pw.println(pad(space) + "inDataExists <= 0;");
				pw.println(pad(space) + "din_index <= din_index + 1;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space)
						+ "else if (read && din_index < "
						+ (inBlocksWords)
						+ ") // only look at the data, the last block is pad which can be ignored for the input");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space) + "inDataExists <= 1;");
				pw.println(pad(space) + "din_index <= din_index + 1;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space) + "");
				pw.println(pad(space)
						+ "// check that the output is the same as the expected, masking over the pad bytes in the current word");
				pw.println(pad(space)
						+ "if (write && dout_index < "
						+ (outBlocksWordsValid)
						+ ") // output size * num blocks excluding the pad block.  output size includes the data valid word");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space) + "//out[dout_in_block_index] <= dout;");
				pw.println(pad(space)
						+ "if (dout_valid && (dout & current_pad) !== (expected & current_pad))");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space)
						+ "$fwrite(resultFile, \"FAIL: Incorrect result.  output block index %d (offset in block %d) expected %x found %x\\n\",");
				pw.println(pad(space + 8)
						+ "(dout_index-1), dout_in_block_index, FSL_output[dout_index] & current_pad, dout & current_pad);");
				pw.println(pad(space) + "task_" + taskName + "_fail <= 1;");
				pw.println(pad(space) + "$finish;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space));
				pw.println(pad(space) + "dout_index <= dout_index+1;");
				pw.println(pad(space) + "");
				pw.println(pad(space)
						+ "if (dout_in_block_index === ("
						+ outBlockSize
						+ "-1)) // last word in block, increment valid_index and dout_index over dout_valid");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space) + "dout_in_block_index <= 0;");
				pw.println(pad(space) + "dout_valid_index <= dout_valid_index+"
						+ outBlockSizeValid + ";");
				pw.println(pad(space) + "dout_index <= dout_index+2;");
				pw.println(pad(space)
						+ "$fwrite(clkCntFile,\"%d\\n\",clockCount);");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space) + "else");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space)
						+ "dout_in_block_index <= dout_in_block_index + 1;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 5;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space) + "");
				pw.println(pad(space) + "if (write && dout_index >= "
						+ (outBlocksWordsValid) + ")");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space)
						+ "$fwrite(resultFile, \"FAIL: extraneous output after last expected block finished\");");
				pw.println(pad(space) + "task_" + taskName + "_fail <= 1;");
				pw.println(pad(space) + "$finish;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space) + "");
				pw.println(pad(space) + "if (dout_index === "
						+ (outBlocksWordsValid) + "+1)");
				space += 2;
				{
					pw.println(pad(space) + "begin");
					space += 3;
					pw.println(pad(space) + "$fwrite (resultFile, \"PASSED\");");
					pw.println(pad(space) + "$finish(1);");
				}
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space) + "");
				pw.println(pad(space) + "clockCount <= clockCount + 1;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space) + "");
				pw.println(pad(space)
						+ "// run a hang timer to make sure no one block takes too long");
				pw.println(pad(space) + "always @(posedge clk)");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space) + "if (write)");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space) + "hangTimer <= 0;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 2;
				pw.println(pad(space) + "else");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space) + "hangTimer <= hangTimer + 1;");

				// Just in case we run into a problem with what the
				// user specified, this will default to 1500.
				String hangExpire = "1500";
				try {
					final Option op = EngineThread.getGenericJob().getOption(
							OptionRegistry.HANG_TIMER);
					int HANGTIMER = Integer.parseInt(
							op.getValue(CodeLabel.UNSCOPED).toString(), 10);
					hangExpire = Integer.toString(HANGTIMER);
				} catch (Exception e) {
					hangExpire = "1500";
				}

				pw.println(pad(space) + "if (hangTimer > " + hangExpire + ")");
				space += 2;
				pw.println(pad(space) + "begin");
				space += 3;
				pw.println(pad(space)
						+ "$fwrite (resultFile, \"FAIL: Hang Timer expired at input block offset %d\\n\", din_index);");
				pw.println(pad(space) + "$finish;");
				space -= 3;
				pw.println(pad(space) + "end");
				space -= 5;
				pw.println(pad(space) + "end // else: !if(write)");
				space -= 2;
				space -= 3;
				pw.println(pad(space) + "end // always @ (posedge clk)");
				space -= 5;
				pw.println("");
			}
			pw.println(pad(space) + "");
			pw.println(pad(space) + "endmodule // fixture");
			pw.close();
		} catch (IOException ioe) {
			final String name = fileHandler.getFile(TestBenchEngine.ATB)
					.getAbsolutePath();
			EngineThread.getEngine().fatalError(
					"Couldn't produce " + name + ": " + ioe);
		}
	}

	private void writeBenchmarkAtClockEdge(PrintWriter pw, int space,
			int outBlocksWordsValid, int numBlocks) {
		pw.println(pad(space) + "/* Benchmarking */");

		pw.println(pad(space) + "if(read === 1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space) + "benchNumReads = benchNumReads + 1;");
		pw.println(pad(space) + "benchInReadZone = 1;");
		pw.println(pad(space) + "if(benchWorkInProgress === 0)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space) + "benchWorkInProgress = 1;");
		pw.println(pad(space) + "benchFirstReadCycle = clockCount;");
		space -= 2;
		pw.println(pad(space) + "end");
		pw.println(pad(space)
				+ "benchReadZoneCyclesCounterSave = benchReadZoneCyclesCounter;");
		pw.println(pad(space) + "benchCoreCyclesCounter = 0;");
		pw.println(pad(space) + "if(benchInWriteZone == 1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space)
				+ "benchWriteZoneCycles = benchWriteZoneCycles + benchWriteZoneCyclesCounterSave + 1;");
		pw.println(pad(space) + "benchWriteZoneCyclesCounter = 0;");
		pw.println(pad(space) + "benchInWriteZone = 0;");
		space -= 2;
		pw.println(pad(space) + "end");
		pw.println(pad(space) + "if(benchIdleFlag == 1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space)
				+ "benchIdleCycles = benchIdleCycles + benchIdleCyclesCounter - 1; //(-1 ignores the increment in the write cycle)");
		pw.println(pad(space) + "benchIdleFlag = 0;");
		space -= 2;
		pw.println(pad(space) + "end");
		space -= 2;
		pw.println(pad(space) + "end");
		pw.println("");

		pw.println(pad(space) + "if(write === 1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space) + "benchNumWrites = benchNumWrites + 1;");
		pw.println(pad(space) + "benchInWriteZone = 1;");
		pw.println(pad(space) + "if(benchInReadZone == 1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space)
				+ "benchReadZoneCycles = benchReadZoneCycles + benchReadZoneCyclesCounterSave + 1;");
		pw.println(pad(space) + "benchReadZoneCyclesCounter = 0;");
		pw.println(pad(space) + "benchInReadZone = 0;");
		pw.println(pad(space)
				+ "benchCoreCycles = benchCoreCycles + benchCoreCyclesCounter;");
		pw.println(pad(space) + "benchWriteZoneCyclesCounter = 0;");
		space -= 2;
		pw.println(pad(space) + "end");
		pw.println(pad(space)
				+ "benchWriteZoneCyclesCounterSave = benchWriteZoneCyclesCounter;");
		pw.println(pad(space) + "benchLastWriteCycle = clockCount;");
		pw.println(pad(space) + "benchIdleFlag = 1;");
		pw.println(pad(space) + "benchIdleCyclesCounter = 0;");
		space -= 2;
		pw.println(pad(space) + "end");

		pw.println(pad(space) + "if (dout_index === " + (outBlocksWordsValid)
				+ "+1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space)
				+ "benchWriteZoneCycles = benchWriteZoneCycles + benchWriteZoneCyclesCounterSave + 1;");
		pw.println(pad(space)
				+ "benchIdleCycles = benchIdleCycles + benchIdleCyclesCounter;");
		space -= 2;
		pw.println(pad(space) + "end");

		pw.println(pad(space)
				+ "benchIdleCyclesCounter = benchIdleCyclesCounter + 1;");
		pw.println(pad(space)
				+ "benchCoreCyclesCounter = benchCoreCyclesCounter + 1;");
		pw.println(pad(space) + "if(benchInReadZone == 1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space)
				+ "benchReadZoneCyclesCounter = benchReadZoneCyclesCounter + 1;");
		space -= 2;
		pw.println(pad(space) + "end");
		pw.println(pad(space) + "if(benchInWriteZone == 1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space)
				+ "benchWriteZoneCyclesCounter = benchWriteZoneCyclesCounter + 1;");
		space -= 2;
		pw.println(pad(space) + "end");

		pw.println(pad(space) + "if (dout_index === " + (outBlocksWordsValid)
				+ "+1)");
		pw.println(pad(space) + "begin");
		space += 2;
		pw.println(pad(space)
				+ "benchTotalCycles = benchLastWriteCycle - benchFirstReadCycle;");
		pw.println(pad(space)
				+ "benchThroughput = (benchNumReads *4.0)/(benchTotalCycles);");
		pw.println(pad(space)
				+ "benchOverallInputUtil = (benchNumReads *100) / benchTotalCycles;");
		pw.println(pad(space)
				+ "benchOverallOutputUtil = (benchNumWrites *100)/ benchTotalCycles;");
		pw.println(pad(space)
				+ "benchOverallCoreUtil = (benchCoreCycles * 100.0 )/ benchTotalCycles ;");
		pw.println(pad(space)
				+ "benchIdlePercentage = (benchIdleCycles*100)/ benchTotalCycles;");
		pw.println(pad(space)
				+ "benchZoneInputUtil = (benchNumReads *100)/ benchReadZoneCycles;");
		pw.println(pad(space)
				+ "benchZoneOutputUtil = (benchNumWrites *100)/ benchWriteZoneCycles;");
		pw.println(pad(space)
				+ "benchZoneCoreUtil = (benchCoreCycles *100)/ benchCoreCycles;");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile,\"**********************************************************\\n\");");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"******************** BENCHMARK REPORT ********************\\n\");");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"**********************************************************\\n\");");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Number of Runs                       : %0d\\n\", "
				+ numBlocks + ");");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Total Number of Cycles               : %0d\\n\", clockCount);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Data Crunching Cycles                : %0d\\n\", benchTotalCycles);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Number of Idle Cycles(Turn around)   : %0d\\n\", benchIdleCycles);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Number of Reads                      : %0d\\n\", benchNumReads);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Bytes Consumed                       : %0d\\n\", benchNumReads * 4);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Number of Writes                     : %0d\\n\", benchNumWrites);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Bytes Produced                       : %0d\\n\", benchNumWrites * 4);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"First Read Cycle                     : %0d\\n\", benchFirstReadCycle);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Last Write Cycle                     : %0d\\n\", benchLastWriteCycle);");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Throughput(Bytes Consumed/Cycle)     : %0g\\n\", benchThroughput); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Throughput(Bytes Produced/Cycle)     : %0g\\n\", (benchNumWrites *4.0)/benchTotalCycles); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"*************Overall**************************************\\n\");");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Overall Input Link Utilization       : %0g\\n\", benchOverallInputUtil); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Overall Core Utilization             : %0g\\n\", benchOverallCoreUtil); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Overall Output Link Utilization      : %0g\\n\", benchOverallOutputUtil); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Percentage of Idle Cycles            : %0g\\n\", benchIdlePercentage); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"*************Zonal****************************************\\n\");");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Zone Input Link Utilization          : %0g\\n\", benchZoneInputUtil); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Zone Core Utilization                : %0g\\n\", benchZoneCoreUtil); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile, \"Zone Output Link Utilization         : %0g\\n\", benchZoneOutputUtil); ");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile,\"**********************************************************\\n\");");
		pw.println(pad(space)
				+ "$fwrite (benchmarkReportFile,\"%d %d %d %d %d %d %d %d %d %d %g %g %g %g %g %g %g %g %g\\n\","
				+ numBlocks
				+ ", clockCount, benchTotalCycles, benchIdleCycles, benchNumReads, benchNumReads * 4, benchNumWrites, benchNumWrites * 4, benchFirstReadCycle"
				+ ", benchLastWriteCycle , benchThroughput, (benchNumWrites *4.0)/benchTotalCycles  , benchOverallInputUtil, benchOverallCoreUtil, benchOverallOutputUtil"
				+ ", benchIdlePercentage , benchZoneInputUtil, benchZoneCoreUtil, benchZoneOutputUtil);");

		space -= 2;
		pw.println(pad(space) + "end");
		pw.println(pad(space) + "/* End benchmarking .. */");
	}

	private void writeBenchmarkInit(PrintWriter pw,
			ForgeFileHandler fileHandler, int space) {
		pw.println(pad(space) + "/* Benchmarking init */");
		final String reportFile = fileHandler.getFile(
				TestBenchEngine.DefaultTestBenchEngine.REPORT)
				.getAbsolutePath();
		pw.println(pad(space) + "benchmarkReportFile <= $fopen (\""
				+ reportFile + "\");");
		pw.println(pad(space) + "benchNumReads = 0 ;");
		pw.println(pad(space) + "benchConsumedBytes = 0;");
		pw.println(pad(space) + "benchNumWrites = 0;");
		pw.println(pad(space) + "benchProducedBytes = 0;");
		pw.println(pad(space) + "benchFirstReadCycle = 0;");
		pw.println(pad(space) + "benchLastWriteCycle = 0;");
		pw.println(pad(space) + "benchWorkInProgress = 0;");
		pw.println(pad(space) + "benchThroughput = 0;");
		pw.println(pad(space) + "benchOverallInputUtil = 0;");
		pw.println(pad(space) + "benchOverallOutputUtil = 0;");
		pw.println(pad(space) + "benchOverallCoreUtil = 0;");
		pw.println(pad(space) + "benchZoneInputUtil = 0;");
		pw.println(pad(space) + "benchZoneOutputUtil = 0;");
		pw.println(pad(space) + "benchZoneCoreUtil = 0;");
		pw.println(pad(space) + "benchTotalCycles = 0;");
		pw.println(pad(space) + "benchCoreCyclesCounter = 0;");
		pw.println(pad(space) + "benchCoreCycles = 0;");
		pw.println(pad(space) + "benchIdleCyclesCounter = 0;");
		pw.println(pad(space) + "benchIdleCycles = 0;");
		pw.println(pad(space) + "benchIdleFlag = 0;");
		pw.println(pad(space) + "benchIdlePercentage = 0;");
		pw.println(pad(space) + "benchInReadZone = 0;");
		pw.println(pad(space) + "benchInWriteZone = 0;");
		pw.println(pad(space) + "benchReadZoneCycles = 0;");
		pw.println(pad(space) + "benchReadZoneCyclesCounter = 0;");
		pw.println(pad(space) + "benchReadZoneCyclesCounterSave = 0;");
		pw.println(pad(space) + "benchWriteZoneCycles = 0;");
		pw.println(pad(space) + "benchWriteZoneCyclesCounter = 0;");
		pw.println(pad(space) + "benchWriteZoneCyclesCounterSave = 0;");
		pw.println(pad(space) + "benchCoreZoneCycles = 0;");
		pw.println(pad(space) + "benchCoreZoneCyclesCounter = 0;");
		pw.println(pad(space));
		pw.println(pad(space) + "/* End benchmarking init */");
	}

	/**
	 * produce a string of 00 or ff depending on which bits are set in the mask
	 * parameter. string is width*2 chars long
	 */
	@SuppressWarnings("unused")
	private String padMask(int mask, int width) {
		String result = "";

		for (int i = width - 1; i > -1; i--) {
			if (((1 << i) & mask) != 0) {
				result += "ff";
			} else {
				result += "00";
			}
		}
		return result;
	}

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

}
