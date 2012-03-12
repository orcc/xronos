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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * CycleCTestbench is responsible for writing a <code>main</code> function that
 * will parse the <code>xxxx_input_blocks.vec</code> and the
 * <code>xxxx_output_expected.vec</code> and use those values to verify that the
 * cycle accurate C model generates the correct results. It does NOT verify the
 * cycle accuracy, however it does generate a <code>test_c_sim.results</code>
 * file that matches the format generated by the verilog simulation and which
 * may be used to verify the cycle accuracy.
 * 
 * 
 * <p>
 * Created: Fri Mar 18 17:30:01 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CycleCTestbench.java 424 2007-02-26 22:36:09Z imiller $
 */
public class CycleCTestbench {

	// Give it 20K cycles of non responsiveness before giving up.
	private static final int HANG_TIMER_EXP = 20000;

	private CycleCTestbench() {
	}

	public static void writeVECTestingMain(PrintStream ps, String fxnBaseName,
			String baseName, IOHandler ioHandle) {
		ps.println();
		ps.println("/* Start of testbench code */");

		ps.println("#include <stdio.h>");

		ps.println("");
		ps.println("/*");
		ps.println(" * Parses the xxx_input_blocks.vec file format, counts the number of");
		ps.println(" * data elements, and stores the data elements into 'ptr' iff doSave");
		ps.println(" * is non-zero.  If doSave is zero, then this function only counts the");
		ps.println(" * number of input elements.  If 'supplyValid' is true then the skipped");
		ps.println(" * line defines the 'is valid' flag for the results until the next");
		ps.println(" * 'skipped' line");
		ps.println(" */");
		ps.println("long long validValue;");
		ps.println("long long value;");
		ps.println("char tempBuf[256];");
		ps.println("int getVECData (FILE *fp, long long *ptr, long long *validPtr, int doSave, int skipLine, int supplyValid)");
		ps.println("{");
		ps.println("    int inputValuesCount = 0;");
		ps.println("    validValue = 1;");
		ps.println("    while(1)");
		ps.println("    {");
		ps.println("        if (fscanf(fp, \"%llx\", &value) > 0)");
		ps.println("        {");
		ps.println("            if (doSave)");
		ps.println("            {");
		ps.println("                ptr[inputValuesCount] = value;");
		ps.println("                if (supplyValid) {");
		ps.println("                    validPtr[inputValuesCount] = validValue;");
		ps.println("                }");
		ps.println("                //printf(\"%08x\\n\", value);");
		ps.println("            }");
		ps.println("            inputValuesCount++;");
		ps.println("        }");
		ps.println("        else");
		ps.println("        {");
		ps.println("            int cmp = fscanf(fp, \"%[/blockforiteration0123456789 ]\\n\",tempBuf);");
		ps.println("            // String length will be 2 if all we catch is the double");
		ps.println("            // slash in pad data comment");
		ps.println("            if (cmp <= 0 || (strlen(tempBuf) <= 2))");
		ps.println("            {");
		ps.println("                break;");
		ps.println("            }");
		ps.println("            else if (skipLine)");
		ps.println("            {");
		ps.println("                if (fscanf(fp, \"%llx\", &validValue) > 0)");
		ps.println("                {");
		ps.println("                    /*printf(\"Skipping line\\n\");*/");
		ps.println("                }");
		ps.println("                else");
		ps.println("                {");
		ps.println("                    printf(\"Error trying to skip line! %s\\n\", tempBuf);");
		ps.println("                }");
		ps.println("            }");
		ps.println("        }");
		ps.println("    }");
		ps.println("    return inputValuesCount;");
		ps.println("}");
		ps.println("");
		ps.println("/*");
		ps.println(" * Finds the first input value after //pad data and returns that.  In");
		ps.println(" * the verilog simulation it is this value that is fed to the core");
		ps.println(" * after all the input data has been exhausted.");
		ps.println(" */");
		ps.println("int getLast (FILE *fp, long long *ptr, int count)");
		ps.println("{");
		ps.println("    int cnt = 0;");
		ps.println("    while (1)");
		ps.println("    {");
		ps.println("        if (fscanf(fp, \"%llx\", &value) > 0)");
		ps.println("        {");
		ps.println("            ptr[cnt] = value;");
		ps.println("            cnt++;");
		ps.println("            if (cnt == count)");
		ps.println("            {");
		ps.println("                break;");
		ps.println("            }");
		ps.println("        }");
		ps.println("        else");
		ps.println("        {");
		ps.println("            int cmp = fscanf(fp, \"%[/paddata ]\\n\", tempBuf);");
		ps.println("            if (cmp <= 0)");
		ps.println("            {");
		ps.println("                break;");
		ps.println("            }");
		ps.println("        }");
		ps.println("    }");
		ps.println("    return cnt;");
		ps.println("}");
		ps.println("");
		// ps.println(IOHandler.IN_TYPE + " in = {0,0,0};");
		// ps.println(IOHandler.OUT_TYPE+ " out = {0,0,0};");
		// ps.println(IOHandler.IN_TYPE + " *iptr = {&in};");
		// ps.println(IOHandler.OUT_TYPE+ " *optr = {&out};");
		ps.println("#define VERILOG_EXISTS_DELAY 11");
		ps.println("int main (int argc, char *argv[])");
		ps.println("{");
		ps.println("    char *progname = argv[0];");
		ps.println("    int verbose = 0;");
		ps.println("    if (argc > 1) { verbose = atoi(argv[1]); }");
		// ps.println("    if (argc < 2) ");
		// ps.println("    {");
		// ps.println("        printf(\"%s <baseName>\\n\", argv[0]);");
		// ps.println("        return -1;");
		// ps.println("    }");
		// ps.println("    ");
		// ps.println("    // Build up the names of the input/output files.");
		// ps.println("    char *baseName = argv[1];");
		// ps.println("    int baseNameLength = strlen(baseName);");
		// ps.println("    char *inName = (char *)malloc(baseNameLength + 18);");
		// ps.println("    char *expName = (char *)malloc(baseNameLength + 20);");
		// ps.println("    char *outName = (char *)malloc(baseNameLength + 14);");
		// ps.println("    strcat(inName, baseName);");
		// ps.println("    strcat(inName, \"_input_blocks.vec\");");
		// ps.println("    strcat(expName, baseName);");
		// ps.println("    strcat(expName, \"_output_expected.vec\");");
		// ps.println("    strcat(outName, baseName);");
		// ps.println("    strcat(outName, \"_c_sim.results\");");
		// ps.println("");
		ps.println("    char *inName = \"" + baseName + "_input_blocks.vec\";");
		ps.println("    char *expName = \"" + baseName
				+ "_output_expected.vec\";");
		ps.println("    char *outName = \"" + baseName + "_c_sim.results\";");
		ps.println("    // Count and read in the input data elements");
		ps.println("    FILE *input = fopen(inName, \"r\");");
		ps.println("    input = fopen(inName, \"r\");");
		ps.println("    int inputFinalCount = getVECData(input, NULL, NULL, 0, 0, 0);");
		ps.println("    fclose(input);");
		ps.println("    input = fopen(inName, \"r\");");
		ps.println("    long long *inData = (long long*)malloc((inputFinalCount * 8) + 1);");
		ps.println("    getVECData(input, inData, NULL, 1, 0, 0);");
		ps.println("    int trailerCount = getLast(input, &(inData[inputFinalCount]), 1);");
		ps.println("");
		ps.println("    // Count and read in the expected results");
		ps.println("    FILE *expFile = fopen(expName, \"r\");");
		ps.println("    int expFinalCount = getVECData(expFile, NULL, NULL, 0, 1, 0);");
		ps.println("    fclose(expFile);");
		ps.println("    expFile = fopen(expName, \"r\");");
		ps.println("    long long *expected = (long long*)malloc(expFinalCount * 8);");
		ps.println("    long long *expValid = (long long*)malloc(expFinalCount * 8);");
		ps.println("    getVECData(expFile, expected, expValid, 1, 1, 1);");
		ps.println("");
		ps.println("    // Now get the masking data from the expected file");
		ps.println("    // Assume no more than 8K elements in the output blocks.");
		ps.println("    long long *expMasks = (long long *)malloc(8192 * 8);");
		ps.println("    int expMaskCount = getLast(expFile, expMasks, 8192);");
		ps.println("    // The expected data has 1 extra 'pad' at the end... UGH!");
		ps.println("    expMaskCount--;");
		ps.println("    fclose(expFile);");
		ps.println("");
		ps.println("    // Open the cycle data output file");
		ps.println("    FILE *output = fopen(outName, \"w\");");
		ps.println("");
		ps.println("    // Run the simulation.");
		ps.println("    int inputCount = 0;");
		ps.println("    int cycleCount = 0;");
		ps.println("    int expCount = 0;");
		ps.println("    int expMaskIndex = 0;");
		ps.println("    int errors = 0;");
		ps.println("    int doOneMore = 0;");
		ps.println("    int dataExistsDelay = 0;");
		ps.println("    int hangTimer = 0;");
		ps.println("    while ((expCount < expFinalCount) || doOneMore)");
		ps.println("    {");
		ps.println("        doOneMore = 0;");
		ps.println("        if (hangTimer > " + HANG_TIMER_EXP + ") {");
		ps.println("            fprintf(output, \"HANG TIMER EXPIRED\\n\");");
		ps.println("            errors++;");
		ps.println("        }");
		ps.println("        hangTimer++;");
		ps.println("        if (inputs[0]->read)");
		ps.println("        {");
		ps.println("            inputCount++;");
		ps.println("            hangTimer = 0;");
		ps.println("        }");
		ps.println("        inputs[0]->data = inData[inputCount];");
		ps.println("        inputs[0]->exists = (inputCount < inputFinalCount) ? ((dataExistsDelay > VERILOG_EXISTS_DELAY) ? 1:0):0;");
		ps.println("        outputs[0]->full = 0;");
		ps.println("");
		// ps.println("        "+fxnBaseName+"_update(&iptr, &optr);");
		ps.println("        " + fxnBaseName + "_update();");
		ps.println("        " + fxnBaseName + "_clockEdge();");
		ps.println("");
		ps.println("        // Verify the result");
		ps.println("        long long expectedValid = expValid[expCount];");
		ps.println("        long long mask = expMasks[expMaskIndex];");
		ps.println("        long long maskedResult = outputs[0]->data & mask;");
		ps.println("        long long maskedExpected = expected[expCount] & mask;");
		ps.println("        //printf(\"result %d #%d %llx expected %016llx obtained %016llx\\n\", expMaskIndex, expCount, mask, maskedExpected, maskedResult);");
		ps.println("        if (outputs[0]->write)");
		ps.println("        {");
		ps.println("            if ((expectedValid != 0) && (maskedResult != maskedExpected))");
		ps.println("            {");
		ps.println("                printf(\"ERROR Result error! result #%d expected %016llx obtained %016llx\\n\", expCount, maskedExpected, maskedResult);");
		ps.println("                errors++;");
		ps.println("            }");
		ps.println("            expMaskIndex = (expMaskIndex == (expMaskCount - 1)) ? 0:(expMaskIndex + 1);");
		ps.println("            expCount++;");
		ps.println("            doOneMore = (expCount == expFinalCount);");
		ps.println("            hangTimer = 0;");
		ps.println("        }");
		ps.println("        ");
		ps.println("        if (cycleCount > 0 || inputs[0]->read || outputs[0]->write)");
		ps.println("        {");
		ps.println("            //printf(\"%08x %08x %01x %08x %01x\\n\", cycleCount, inputs[0]->data, inputs[0]->read, outputs[0]->data, outputs[0]->write);");
		if (ioHandle.getInputCount() > 0 && ioHandle.getOutputCount() > 0)
			ps.println("            fprintf(output, \"%08x %08x %01x %08x %01x\\n\", cycleCount, inputs[0]->data, inputs[0]->read, outputs[0]->data, outputs[0]->write);");
		else if (ioHandle.getInputCount() == 0
				&& ioHandle.getOutputCount() != 0)
			ps.println("            fprintf(output, \"%08x xxxxxxxx z %08x %01x\\n\", cycleCount, outputs[0]->data, outputs[0]->write);");
		else if (ioHandle.getInputCount() != 0
				&& ioHandle.getOutputCount() == 0)
			ps.println("            fprintf(output, \"%08x %08x %01x xxxxxxxx z\\n\", cycleCount, inputs[0]->data, inputs[0]->read);");
		else
			ps.println("            fprintf(output, \"%08x %08x %01x %08x %01x\\n\", cycleCount, inputs[0]->data, inputs[0]->read, outputs[0]->data, outputs[0]->write);");
		ps.println("            cycleCount++;");
		ps.println("        }");
		ps.println("        dataExistsDelay++;");
		ps.println("    }");
		ps.println("");
		ps.println("    if (!errors)");
		ps.println("    {");
		ps.println("        if (verbose) { printf(\"PASSED '%s' %d valid writes.\\n\", progname, expFinalCount); }");
		ps.println("    }");
		ps.println("    else");
		ps.println("    {");
		ps.println("        printf(\"ERRORS during '%s': %d errors across %d output writes.\\n\", progname, errors, expFinalCount);");
		ps.println("    }");
		ps.println("    ");
		ps.println("    fclose(output);");
		ps.println("");
		ps.println("    return errors;");
		ps.println("}");
		ps.println("");
		ps.println();
		ps.println("/* End of testbench code */");
	}

	private static final boolean DO_CYCLES = false;

	public static void writeQueueTestingMain(PrintStream ps,
			String fxnBaseName, String baseName, IOHandler ioHandle,
			Set<String> goVars) {
		ps.println("#include \"testbenchUtil.h\"");
		ps.println("#include <stdio.h>");
		ps.println("int DO_DEBUG = 0;");
		ps.println("int main (int argc, char *argv[])");
		ps.println("{");
		if (DO_CYCLES)
			ps.println("    FILE *cycleResults = fopen(\"" + baseName
					+ "_csim.cycles\", \"w\");");

		int ioCount = ioHandle.getInputCount() + ioHandle.getOutputCount();
		String corrInit = "";
		for (int i = 0; i < ioCount; i++)
			corrInit += "-1" + (i < (ioCount - 1) ? "," : "");
		ps.println("    int corr[" + ioCount + "] = {" + corrInit + "};");
		ps.println("");
		ps.println("    int queueId, simId;");
		List<String> inPorts = ioHandle.getInputPortNames();
		List<String> outPorts = ioHandle.getOutputPortNames();
		List<String> ports = new ArrayList<String>();
		ports.addAll(inPorts);
		ports.addAll(outPorts);
		for (String portName : ports) {
			ps.println("    queueId = registerVectorFile(\"" + baseName + "_"
					+ portName + ".vec\");");
			ps.println("    simId = getInterfaceID(\"" + portName + "\");");
			ps.println("    if (DO_DEBUG) printf(\"" + portName
					+ " qid %d simid %d\\n\", queueId, simId);");
			ps.println("    corr[simId] = queueId;");
		}
		ps.println("    ");
		ps.println("    int dataValue, validValue;");
		ps.println("    unsigned int errors=0;");
		ps.println("    unsigned int timer=0;");
		ps.println("    unsigned int hangTimer=0;");
		ps.println("    while (actionFiringsRemain())");
		ps.println("    {");
		ps.println("        timer++;");
		ps.println("");
		// ps.println("        if (DO_DEBUG) printf(\"\tpre \t%d:\tMOT %08x %x %x\tTEX %08x %x %x\tBTYPE %08x %x %x\tVID %08x %x %x\\n\", timer, inputs[0]->data, inputs[0]->send, inputs[0]->ack, inputs[1]->data, inputs[1]->send, inputs[1]->ack, inputs[2]->data, inputs[2]->send, inputs[2]->ack, outputs[0]->data, outputs[0]->send,  outputs[0]->ack);");
		ps.println("        ");
		ps.println("        // Update the values of all input ports");
		ps.println("        // run the update function");
		ps.println("        // run the tick fucntion");
		ps.println("        // test outputs");
		for (String portName : inPorts) {
			ps.println("        simId = getInterfaceID(\"" + portName + "\");");
			ps.println("        currentData(corr[simId], &dataValue, &validValue);");
			ps.println("        setDataValue(simId, dataValue, validValue);");
		}
		ps.println("");
		ps.println("        " + fxnBaseName + "_update();");
		ps.println("");
		// ps.println("        if (DO_DEBUG) printf(\"\tpost\t%d:\tMOT %08x %x %x\tTEX %08x %x %x\tBTYPE %08x %x %x\tVID %08x %x %x\\n\", timer, inputs[0]->data, inputs[0]->send, inputs[0]->ack, inputs[1]->data, inputs[1]->send, inputs[1]->ack, inputs[2]->data, inputs[2]->send, inputs[2]->ack, outputs[0]->data, outputs[0]->send,  outputs[0]->ack);");
		ps.println("");
		ps.println("        int isAcked;");
		ps.println("        ");
		for (String portName : inPorts) {
			ps.println("        simId = getInterfaceID(\"" + portName + "\");");
			ps.println("        isAcked = isAcking(simId);");
			ps.println("        if (isAcked) { popData(corr[simId]); }");
		}
		ps.println("");
		for (String portName : outPorts) {
			ps.println("        simId = getInterfaceID(\"" + portName + "\");");
			ps.println("        errors += checkResult(simId, corr[simId]);");
		}
		ps.println("        ");
		ps.println("        int firing = isFiring();");

		String dbg1 = "";
		String dbg2 = "";
		assert !goVars.isEmpty();
		for (Iterator<String> iter = goVars.iterator(); iter.hasNext();) {
			String goVar = (String) iter.next();
			dbg1 += "%d ";
			dbg2 += goVar;
			if (iter.hasNext())
				dbg2 += " ,";
		}
		ps.println("        if (DO_DEBUG && firing)");
		ps.println("            printf(\"" + dbg1 + "\\n\"," + dbg2 + ");");
		ps.println("        ");

		if (DO_CYCLES) {
			// Code for supporting a cycle by cycle dump. Difficult to
			// correlate with the string writers in HDL however
			String cycleString1 = "%d: ";
			String cycleString2 = "timer, ";
			for (int i = 0; i < ports.size(); i++) {
				cycleString1 += "%x %x %x ";
				String id;
				if (i < inPorts.size())
					id = "inputs[" + i + "]";
				else
					id = "outputs[" + (i - inPorts.size()) + "]";
				String send = id + "->send";
				String ack = id + "->ack";
				cycleString2 += "(" + send + " || " + ack + ") ? " + id
						+ "->data : 0, " + id + "->send, " + id + "->ack";
				if (i < (ports.size() - 1))
					cycleString2 += ", ";
			}
			ps.println("        fprintf(cycleResults, \"" + cycleString1
					+ "\\n\", " + cycleString2 + ");");
		}

		ps.println("        " + fxnBaseName + "_clockEdge();");
		ps.println("");
		ps.println("        if (firing)");
		ps.println("        {");
		ps.println("            if (DO_DEBUG) printf(\"actionFired\\n\");");
		ps.println("            actionFire();");
		ps.println("        }");
		ps.println("        if (DO_DEBUG) printf(\"---------------------------------\\n\");");
		ps.println("    }");
		ps.println("        ");
		ps.println("");
		if (DO_CYCLES)
			ps.println("    fclose(cycleResults);");
		ps.println("    printf(\"%d errors found in %d cycles\\n\", errors, timer);");
		ps.println("    ");
		ps.println("    return errors;");
		ps.println("}");
		ps.println("");
		ps.println("int checkResult (int simId, int queueId)");
		ps.println("{");
		ps.println("    if (isSending(simId))");
		ps.println("    {");
		ps.println("        int result = getDataValue(simId, 1);");
		ps.println("        int expected, expValid;");
		ps.println("        currentData(queueId, &expected, &expValid);");
		ps.println("        popData(queueId);");
		ps.println("        if (DO_DEBUG) printf(\"%d Checking %d vs expected %d\\n\",simId, result, expected);");
		ps.println("        if (result != expected)");
		ps.println("        {");
		ps.println("            printf(\"ERROR Result error! result #XXX expected %016llx obtained %016llx\\n\", expected, result);");
		ps.println("            return 1;");
		ps.println("        }");
		ps.println("    }");
		ps.println("    else");
		ps.println("    {");
		ps.println("        // Ensure the ack is set low");
		ps.println("        getDataValue(simId, 0);");
		ps.println("    }");
		ps.println("    return 0;");
		ps.println("}");
		ps.println("");
		ps.println("int isFiring ()");
		ps.println("{");
		ps.println("    return");
		assert !goVars.isEmpty();
		for (Iterator<String> iter = goVars.iterator(); iter.hasNext();) {
			String goVar = (String) iter.next();
			ps.print(" " + goVar);
			if (iter.hasNext())
				ps.println(" ||");
			else
				ps.println(";");
		}
		ps.println("    ");
		ps.println("");
		ps.println("}");
	}

}// CycleCTestbench
