/*
 * Copyright (c) 2011, EPFL
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the EPFL nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package net.sf.orc2hdl;

/**
 * Constants associated with an Orcc OpenForge launch configuration.
 * 
 * @author Endri Bezati
 * 
 */

public interface LaunchConstants {
	public static final String PROJECT = "net.sf.openforge.cal2hdl.project";

	public static final String OUTPUT_FOLDER = "net.sf.openforge.cal2hdl.outputFolder";

	public static final String XDF_FILE = "net.sf.openforge.cal2hdl.xdfFile";

	public static final String FPGA_TYPE = "net.sf.openforge.cal2hdl.fpgaType";
	
	public static final String FORGE_VERBOSE = "net.sf.openforge.cal2hdl.forgeVerbose";

	public static final String FORGE_PIPELINE = "net.sf.openforge.cal2hdl.forgePipeline";

	public static final String FORGE_NO_BLOCK = "net.sf.openforge.cal2hdl.forgeNoBlock";

	public static final String FORGE_NO_BLOCK_SCHED = "net.sf.openforge.cal2hdl.forgeNoBlockSched";

	public static final String FORGE_SIM_ARB = "net.sf.openforge.cal2hdl.forgeSimArb";

	public static final String FORGE_EDK = "net.sf.openforge.cal2hdl.forgeEdk";

	public static final String FORGE_LOOP_BAL = "net.sf.openforge.cal2hdl.forgeLoopBal";

	public static final String FORGE_MUL_DECOMP_LIMIT = "net.sf.openforge.cal2hdl.forgeMulDecompLimit";

	public static final String FORGE_COMB_LUT_MEM_READ = "net.sf.openforge.cal2hdl.forgeCombLutMemRead";

	public static final String FORGE_DP_LUT = "net.sf.openforge.cal2hdl.forgeDpLut";

	public static final String FORGE_NO_LOG = "net.sf.openforge.cal2hdl.forgeNoLog";

	public static final String FORGE_NO_INCLUDE = "net.sf.openforge.cal2hdl.forgeNoInclude";

	public static final String SYNC_FIFO = "net.sf.openforge.cal2hdl.syncFifo";
}
