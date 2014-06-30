/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.xronos.orcc.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * An utility class for OpenForge flags
 * 
 * @author Endri Bezati
 * 
 */
public class XronosFlags {

	/**
	 * Generate REALLY verbose messages during compilation. Defaults levels to
	 * verbose.
	 */
	private Boolean verboseVerboseFlag = false;

	/**
	 * Pipeline to a specified gate depth.
	 */
	private Boolean pipelineFlag = false;

	/**
	 * Auto pipelining level. The amount of pipelining to perform. This
	 * preference controls how many pipelining registers are inserted into the
	 * design in order to improve the max frequency the design can run at. The
	 * number of registers inserted is determined by finding the longest
	 * (deepest) combinational path in the design. The length of that path is
	 * divided by the value specified in this preference to determine the
	 * maximum allowable combinational depth for the design. The entire design
	 * is then pipelined so that no combinational path exceeds this metric (if
	 * possible). A zero value disables pipeline register insertion.
	 */
	private Boolean autoPipelineLevelFlag = false;

	/**
	 * Auto pipelining level value.
	 */

	private Integer autoPipelineLevelValue = 0;

	/**
	 * Target gate depth for pipelining.
	 */
	private Boolean gateDepthPipelineFlag = false;

	/**
	 * Value of the target gate depth for pipelining.
	 */
	private Integer gateDepthPipelineValue = 0;

	/**
	 * Do not use block IO interface to application. This reverts the interface
	 * back to the simple GO/DONE interface utilizing only primitive data types.
	 */
	private Boolean noBlockIOFlag = false;

	/**
	 * Disables block scheduling.
	 */
	private Boolean noBlockSchedulingFlag = false;

	/**
	 * The arbitration logic for memories and/or registers should be a simple
	 * merging of accesses without regard to potential collisions.
	 */
	private Boolean simpleArbitrationFlag = false;

	/**
	 * Do not generate edk peripheral specification hierarchy on forge output.
	 */
	private Boolean noEDKFlag = false;

	/**
	 * When set to true, causes any branch statements in a loop to have its true
	 * and false branches balanced such that both are combinational or both are
	 * latency > 0.
	 */
	private Boolean loopBranchBalanceFlag = false;

	/**
	 * Maximum number of operations to which a half constant multiply can be
	 * decomposed.
	 */
	private Boolean multiplyDecompositionLimitFlag = false;

	/**
	 * Multiply decomposition value.
	 */
	private Integer multiplyDecompositionLimitValue = 0;

	/**
	 * LUT based memories should provide combinational read timing.
	 */
	private Boolean combinationalLUTMemReadFlag = false;

	/**
	 * Allow the creation of dual port LUT based memories.
	 */
	private Boolean dualortLUTFlag = false;

	/**
	 * Suppress generation of the default log file
	 */
	private Boolean noLogFlag = false;

	/**
	 * Suppresses generation of the standard _sim and _synth HDL include files.
	 */
	private boolean noIncludeFlag = false;

	/**
	 * Generate a report file.
	 */
	private Boolean reportFlag = false;

	/**
	 * Adds extra sizing and constant information to resource report.
	 */
	private Boolean detailedReportFlag = false;

	/**
	 * The output folder
	 */
	private String outputFolder = "";

	/**
	 * The name of the design
	 */
	private String outputName = "";

	public XronosFlags(String outputFolder, String outputName) {
		this.outputFolder = outputFolder;
		this.outputName = outputName;
		setDefaultValues();
	}

	/**
	 * Activate the pipelining with a given depth
	 * 
	 * @param gateDepth
	 */

	public void activatePipelining(Integer gateDepth) {
		pipelineFlag = true;
		gateDepthPipelineFlag = true;
		gateDepthPipelineValue = gateDepth;
	}

	/**
	 * Return the String Array with the Forges options
	 * 
	 * @return
	 */
	public String[] getStringFlag() {
		List<String> xronosFlags = new ArrayList<String>();

		if (verboseVerboseFlag) {
			xronosFlags.add("-vv");
		}

		if (pipelineFlag) {
			xronosFlags.add("-pipeline");
			xronosFlags.add("-apl");
			xronosFlags.add("5");
			if (autoPipelineLevelFlag) {
				xronosFlags.add(autoPipelineLevelValue.toString());
			}
			if (gateDepthPipelineFlag) {
				xronosFlags.add("-gd");
				xronosFlags.add(gateDepthPipelineValue.toString());
			}

		}

		if (noBlockIOFlag) {
			xronosFlags.add("-noblockio");
		}

		if (noBlockSchedulingFlag) {
			xronosFlags.add("-no_block_sched");
		}

		if (simpleArbitrationFlag) {
			xronosFlags.add("-simple_arbitration");
		}

		if (noEDKFlag) {
			xronosFlags.add("-noedk");
		}

		if (loopBranchBalanceFlag) {
			xronosFlags.add("-loopbal");
		}

		if (multiplyDecompositionLimitFlag) {
			xronosFlags.add("-multdecomplimit");
			multiplyDecompositionLimitValue = 8;
			xronosFlags.add(multiplyDecompositionLimitValue.toString());
		}

		if (combinationalLUTMemReadFlag) {
			xronosFlags.add("-comb_lut_mem_read");
		}

		if (dualortLUTFlag) {
			xronosFlags.add("-dplut");
		}

		if (noLogFlag) {
			xronosFlags.add("-nolog");
		}

		if (noIncludeFlag) {
			xronosFlags.add("-noinclude");
		}

		if (reportFlag) {
			xronosFlags.add("-report");
		}

		if (detailedReportFlag) {
			xronosFlags.add("-Xdetailed_report");
		}

		// Add output folder and give a new name to the design
		xronosFlags.add("-d");
		xronosFlags.add(outputFolder);
		xronosFlags.add("-o");
		xronosFlags.add(outputName);

		String[] flags = xronosFlags.toArray(new String[0]);
		return flags;
	}

	/**
	 * Set the Default Forge options
	 */
	public void setDefaultValues() {
		verboseVerboseFlag = true;
		noBlockIOFlag = true;
		noBlockSchedulingFlag = true;
		simpleArbitrationFlag = true;
		noEDKFlag = true;
		loopBranchBalanceFlag = true;
		multiplyDecompositionLimitFlag = true;
		multiplyDecompositionLimitValue = 8;
		combinationalLUTMemReadFlag = true;
		dualortLUTFlag = true;
		noLogFlag = true;
		noIncludeFlag = true;
		reportFlag = true;
	}

}
