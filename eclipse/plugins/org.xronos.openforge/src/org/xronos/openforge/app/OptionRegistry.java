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

/*
 * Created on Aug 31, 2004
 *
 */
package org.xronos.openforge.app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.xronos.openforge.app.project.Option;
import org.xronos.openforge.app.project.OptionBoolean;
import org.xronos.openforge.app.project.OptionFile;
import org.xronos.openforge.app.project.OptionInt;
import org.xronos.openforge.app.project.OptionIntUnit;
import org.xronos.openforge.app.project.OptionList;
import org.xronos.openforge.app.project.OptionMultiFile;
import org.xronos.openforge.app.project.OptionPath;
import org.xronos.openforge.app.project.OptionPickOne;
import org.xronos.openforge.app.project.OptionString;
import org.xronos.openforge.app.project.OptionXilPart;
import org.xronos.openforge.app.project.SearchLabel;
import org.xronos.openforge.optimize.replace.ReplacementCorrelation;
import org.xronos.openforge.schedule.loop.LoopFlopConflictSet;

/**
 * 
 * Option Registry is the centralized database of all the preferences and
 * options to forge.
 * 
 * @author <a href="mailto:sb@xilinx.com">Srinivas Beeravolu</a>
 * @version $Id: OptionRegistry.java 490 2007-06-15 16:37:00Z imiller $
 * 
 */

public class OptionRegistry {

	public static final String PREFIX = "project.";

	public static final OptionKey PROJ_FILE = new OptionKey(PREFIX
			+ "project_file", "p",
			"Generate complete project file to default.forge file.");
	public static final OptionKey PROJ_FILE_DESC = new OptionKey(PREFIX
			+ "project_file_desc", "P",
			"Generate complete project file with descriptions to default.forge file.");
	public static final OptionKey PFILE = new OptionKey(PREFIX + "pfile",
			"pfile", "Use the named project file as a template.");
	public static final OptionKey QUIET = new OptionKey(PREFIX + "quiet", "q",
			"Do not print anything, nothing even to stdout");
	public static final OptionKey VERSION = new OptionKey(PREFIX + "version",
			"version", "Print the version infomation.");
	public static final OptionKey RUN_DATE = new OptionKey(PREFIX + "rundate",
			"rundate", "The date and time of the compilation run.");
	public static final OptionKey NOTES = new OptionKey(PREFIX + "notes",
			"notes", "Print the release notes.");

	public static final List<String> HELP_CLA = new ArrayList<String>();
	static {
		HELP_CLA.add("h");
		HELP_CLA.add("help");
	}
	public static final OptionKey HELP = new OptionKey(PREFIX + "help",
			HELP_CLA, "Show this help message.");
	public static final OptionKey HELP_DETAIL = new OptionKey(PREFIX
			+ "help_detail", "H", "Show detailed help.");
	public static final OptionKey LICENSE = new OptionKey(PREFIX + "license",
			"license", "Print out license information.");
	public static final OptionKey LICENSEKEY = new OptionKey(PREFIX
			+ "licenseKey", "licenseKey", "Specify the license key");

	public static final OptionKey CWD = new OptionKey(PREFIX + "cwd", "",
			"The current working directory. Used internally to coordinate relative paths.");
	public static final OptionKey TARGET = new OptionKey(
			PREFIX + "target",
			"",
			"The source file for a design. The target should directly reference all related classes.");
	public static final String MESSAGES = PREFIX + "messages.";
	public static final OptionKey VERBOSE = new OptionKey(MESSAGES + "verbose",
			"v",
			"Generate verbose messages during compilation. Defaults levels to info");
	public static final OptionKey VERBOSE_VERBOSE = new OptionKey(
			MESSAGES + "detailed",
			"vv",
			"Generate REALLY verbose messages during compilation. Defaults levels to verbose.");
	public static final OptionKey NOLOG = new OptionKey(MESSAGES + "nolog",
			"nolog", "Suppress generation of the default forge.log log file.");
	public static final OptionKey LOG = new OptionKey(
			MESSAGES + "log",
			"log",
			"Control logging. Format is: console|error|<filename>[=verbose|info|warn|error][,...] Default level is warn.");
	public static final String DESTINATION = PREFIX + "destination.";
	public static final OptionKey DESTINATION_DIR = new OptionKey(DESTINATION
			+ "directory", "d",
			"Destination directory for generated design files.");
	public static final String INCLUDES = PREFIX + "includes.";
	public static final OptionKey INCLUDES_DIR = new OptionKey(
			INCLUDES + "directory",
			"I",
			"Includes directory that is to be searched for header files. NOTE: It is dangerous to specify standard system include directory in this option. Refer to gcc man pages for more details.");
	public static final OptionKey DESTINATION_FILE = new OptionKey(
			DESTINATION + "hdl_file",
			"o",
			"Verilog output file to generate.  When left empty this field inherits its name from the source file.  The suffix .v will be appended if not supplied.");
	public static final OptionKey DESTINATION_FOLLOWS_TARGET = new OptionKey(
			DESTINATION + "follows_target",
			"dfs",
			"Destination Directory Follows Source. Always use the target's parent path as the destination directory. Ignore default (current directory) and explicit destination directory settings. ");
	public static final OptionKey SOURCEPATH = new OptionKey(PREFIX
			+ "sourcepath", "sourcepath", "The search path for source files.");
	public static final OptionKey SHOULD_SIMULATE = new OptionKey(PREFIX
			+ "should_simulate", "sim",
			"A simulation of the design should be conducted.");
	public static final OptionKey SUPPRESS_APP_MODULE = new OptionKey(PREFIX
			+ "suppress_app_module", "noapp",
			"The top-level application module should not be produced for the design.");
	public static final OptionKey SHOULD_TIME_STAMP = new OptionKey(PREFIX
			+ "should_time_stamp", "timestamp",
			"should the log messages be time stamped.");
	public static final OptionKey NO_IOB_OPTS = new OptionKey(
			PREFIX + "no_iob_opts",
			"noiob",
			"When set true, no iob related optimizations will be used or attributes set on components in the translated design.");

	public static final OptionKey LITTLE_ENDIAN = new OptionKey(PREFIX
			+ "little_endian", "littleendian",
			"Use little endian memory format for all internal memories.");
	public static final OptionKey MEMORY_ALIGN = new OptionKey(
			PREFIX + "memory_align",
			"align",
			"Determines where the alignment boundaries exist for packing structures.  Specified in bytes, valid choices are 8 (default) or 4.");
	public static final OptionKey MODULE_BUILDER = new OptionKey(PREFIX
			+ "module_builder", "Xmodule_builder",
			"only for acceptance tests, requires -noblockio");
	public static final OptionKey NO_BLOCK_IO = new OptionKey(
			PREFIX + "block_io",
			"noblockio",
			"Do not use block IO interface to application. This reverts the interface back to the simple GO/DONE interface utilizing only primitive data types.");
	public static final OptionKey BLOCK_IO_FIFO_WIDTH = new OptionKey(PREFIX
			+ "block_io_fifo_width", "fifowidth",
			"Specifies the BYTE width of the fifo interface when block io is enabled.");
	public static final OptionKey CHANNEL_DESCRIPTOR = new OptionKey(
			PREFIX + "channel_descriptor",
			"channel_descriptor",
			"Specifies the implementation characteristics of the design ports when the ports are not being inferred (requires -noblockio).  A channel descriptor takes the form of: '"
					+ "id:bitwidth:name:type:direction where type is one of ACTOR or FSL and dir is either I or O"
					+ "'. Multiple channel descriptors are seperated by commas");

	public static final String ATB = PREFIX + "auto_test_bench.";
	public static final OptionKey C_AUTO_TEST_BENCH = new OptionKey(
			ATB + "c_test_bench",
			"catb",
			"A self verifying C Model Automatic Test Bench should be generated for the design.");
	public static final OptionKey AUTO_TEST_BENCH = new OptionKey(
			ATB + "auto_test_bench",
			"atb",
			"A self verifying Verilog Automatic Test Bench should be generated for the design.");
	public static final OptionKey HANG_TIMER = new OptionKey(
			ATB + "hang_timer", "",
			"The hang timer of simulating acceptance tests.");
	public static final OptionKey ATB_GO_SPACING = new OptionKey(
			ATB + "go_spacing",
			"atb_gs",
			"Specifies the number of clock cycles between each assertion of GO/DATA to the design under test.  Set to any negative value to force waiting on the DONE.");
	public static final OptionKey ATB_BENCHMARK_REPORT = new OptionKey(PREFIX
			+ "atbBenchmarkReport", "atbBenchmarkReport",
			"Generate a BenchMark Report.");

	// compilation settings for gcc & stuff
	public static final String COMPILE = PREFIX + "compile.";
	public static final String CCOMPILE = COMPILE + "c.";
	public static final OptionKey CCOMPILECFLAGS = new OptionKey(CCOMPILE
			+ "cflags", "cflags", "CFlags to apply when compiling with GCC.");
	public static final OptionKey XGCCRAW = new OptionKey(CCOMPILE + "Xgccraw",
			"Xgccraw",
			"Tells Forge to use 'raw' gcc - ie: don't pass -ansi, don't include forge.h");
	public static final OptionKey XGCCANSI = new OptionKey(CCOMPILE
			+ "Xgccansi", "Xgccansi",
			"Tells Forge to use 'ansi' gcc - ie: pass -ansi, include forge.h");
	public static final OptionKey CCOMPILECPPFLAGS = new OptionKey(CCOMPILE
			+ "cppflags", "cppflags",
			"CPPFlags to apply when preprocessing with GCC.");

	// Global memory settings
	public static final String MEMORY = PREFIX + "memory.";
	public static final OptionKey MAX_LUT_BYTES = new OptionKey(
			MEMORY + "max_lut_bytes",
			"max_lut_bytes",
			"Maximum number of bytes for a memory to be implemented as LUTS when Forge directly instantiates memory blocks.");
	// public static final OptionKey MAX_LUT_DEPTH = new OptionKey(MEMORY +
	// "max_lut_depth", "max_lut_depth",
	// "Maximum depth of memory to map to LUT based memories when Forge directly instantiates memory blocks.");
	// public static final OptionKey MAX_LUT_WIDTH = new OptionKey(MEMORY +
	// "max_lut_width", "max_lut_width",
	// "Maximum width of memory to map to LUT based memories when Forge directly instantiates memory blocks.");
	public static final OptionKey MEMORY_BANK_WIDTH = new OptionKey(
			MEMORY + "bank_width",
			"memBankWidth",
			"Specifies the desired memory bank width to use in implementing memories.  Set to -1 to allow forge to calculate the optimal width based on access characteristics.");
	public static final OptionKey COMBINATIONAL_LUT_MEM = new OptionKey(MEMORY
			+ "combinational_lut_mems", "comb_lut_mem_read",
			"LUT based memories should provide combinational read timing.");
	public static final OptionKey SIMPLE_STATE_ARBITRATION = new OptionKey(
			MEMORY + "simple_arbitration",
			"simple_arbitration",
			"The arbitration logic for memories and/or registers should be a simple merging of accesses without regard to potential collisions.");
	public static final OptionKey ALLOW_DUAL_PORT_LUT = new OptionKey(MEMORY
			+ "allow_dual_port_lut_mems", "dplut",
			"Allow the creation of dual port LUT based memories.");
	public static final OptionKey SUPPRESS_DUAL_PORT_RAM = new OptionKey(MEMORY
			+ "suppress_dual_port_ram", "nodpram",
			"Prevent the creation of dual port read-write memories.");

	// Translation settings
	public static final String TRANSLATE = PREFIX + "translation.";
	public static final OptionKey SHOULD_NOT_TRANSLATE = new OptionKey(
			TRANSLATE + "disable", "notrans",
			"The design should not be translated into verilog.");
	public static final OptionKey FORCE_GO_DONE = new OptionKey(
			TRANSLATE + "force_go_done",
			"fgd",
			"Force GO and DONE signals on the top level verilog module.  Forge (by default) only adds a GO to the top level if one is required to validate inputs.  If a GO is present then a DONE is only added to the top level if the design doesn't take a fixed number of clocks to execute.  Using this switch forces Forge to put a GO and DONE at the top level for each entry method, even if they are simply wired together in the design.");
	public static final OptionKey TOP_MODULE_NAME = new OptionKey(
			TRANSLATE + "top_module_name",
			"topmod",
			"Top module name to use in the output Verilog.  When empty, this field inherits its name from the destination file name.");
	public static final OptionKey INVERT_DESIGN_PORT_RANGE = new OptionKey(
			TRANSLATE + "invert_design_port_ranges",
			"invert_port_ranges",
			"Causes Forge to write out the port declarations of the top level Verilog module in inverted bit order, ie [0:31] instead of [31:0].  This preference does NOT affect the bus notation in any other part of the design.");
	public static final OptionKey LONG_VERILOG_NAMES = new OptionKey(TRANSLATE
			+ "long_verilog_names", "long_verilog_names",
			"Causes Forge to use longer style names in the output Verilog HDL.");
	public static final OptionKey NO_INCLUDE_FILES = new OptionKey(TRANSLATE
			+ "no_include_files", "noinclude",
			"Suppresses generation of the standard _sim and _synth HDL include files.");

	// Output SysGen core settings
	public static final OptionKey SYSGEN = new OptionKey(PREFIX + "do_sysgen",
			"sysgen", "Write out a System Generator(tm) compliant core.");

	// Output EDK peripheral settings
	public static final OptionKey NO_EDK = new OptionKey(PREFIX + "no_edk",
			"noedk",
			"Do not generate edk peripheral specification hierarchy on forge output.");
	public static final OptionKey ENTRY = new OptionKey(
			PREFIX + "entry",
			"entry",
			"Specifies the entry level function. The entry can be specified either as 'file:function' or 'function'. In the latter case, the first file with the matching function name is used.");
	public static final OptionKey PE_NAME = new OptionKey(PREFIX + "pe_name",
			"", "Handle for storing the name of the design.");
	public static final OptionKey PE_VERSION = new OptionKey(
			PREFIX + "pe_version",
			"pever",
			"Specifies the major, minor, hardware version string for generating processing elements.");

	// XFlow settings
	public static final String XFLOW = PREFIX + "xflow.";
	public static final OptionKey ENABLE_XFLOW = new OptionKey(
			XFLOW + "enable", "xflow",
			"The Design should invoke the ISE implementation tool after Forge compilation.");
	public static final OptionKey XILINX_PART = new OptionKey(XFLOW
			+ "xilinx_part", "xlpart", "Target part for this compilation.");
	public static final OptionKey TSIM_OPTION = new OptionKey(XFLOW
			+ "tsim_option", "",
			"The flow type generates a file that can be used for timing simulation.");
	public static final OptionKey SYNTH_OPTION = new OptionKey(XFLOW
			+ "synth_option", "",
			"The flow type synthesizes the design for implementation.");
	public static final OptionKey ENABLE_SPEED = new OptionKey(XFLOW
			+ "target_speed.enable", "",
			"Enable the explicit setting of a target speed.");
	public static final OptionKey TARGET_SPEED = new OptionKey(XFLOW
			+ "target_speed.speed", "speed",
			"Specify target frequency for xflow. e.g. \"100 Mhz\".");
	public static final OptionKey IMPLEMENT_OPTION = new OptionKey(XFLOW
			+ "implement_option", "",
			"The flow type implements the design, trading off between speed and area.");
	public static final OptionKey BITGEN_OPTIONS = new OptionKey(
			XFLOW + "bitgen_options",
			"",
			"Specify the command line options for bitgen as the last step of the xflow process, set to NO_RUN to not run bitgen.");

	// UCF config
	public static final String UCF = XFLOW + "ucf.";
	public static final OptionKey UCF_FILE = new OptionKey(
			UCF + "UCF_output",
			"ucffile",
			"The user constraint file to generate.  When running with the -xflow option, a copy of the final ucf file generated in the xflow directory will be put in the specified file name.");
	public static final OptionKey UCF_INCLUDES = new OptionKey(
			UCF + "UCF_includes",
			"ucf",
			"Additional UCF files to be included. The generated design UCF file will have include directives for each file.");
	public static final OptionKey UCF_ONLY = new OptionKey(UCF + "UCF_only",
			"ucf", "Generate a ucf file then stop prior to xflow run.");

	// Report settings
	public static final OptionKey REPORT = new OptionKey(PREFIX + "report",
			"report", "Generate report.");

	// naming related options
	public static final String NAMING = PREFIX + "naming.";
	public static final OptionKey SIMPLE_MODULE_NAMES = new OptionKey(
			NAMING + "simple_module_names",
			"",
			"Use unique but very simple names for modules. Otherwise, fully qualified names including package, class and method signature will be used to name modules.");
	public static final OptionKey SIGNATURE_IN_NAMES = new OptionKey(NAMING
			+ "signature_in_names", "",
			"Include signature (based on parameter type) in module names.");

	// Random other (debug) stuff
	public static final OptionKey DEBUG_MEM_LOCATIONS = new OptionKey(PREFIX
			+ "debug_memory_locations", "debugmemlocations",
			"Add memory location debug information to Verilog output.");

	// Scheduler Options
	public static final OptionKey SCHEDULE_BALANCE = new OptionKey(
			PREFIX + "balance",
			"balance",
			"Apply balanced scheduling.  This preference configures the scheduler to attempt to schedule the design such that new data can be asserted every clock cylce.  The success or failure of the scheduler will be reported to the user.");
	public static final String SCHEDULE_PIPELINE = PREFIX + "pipeline.";
	public static final OptionKey SCHEDULE_PIPELINE_ENABLE = new OptionKey(
			SCHEDULE_PIPELINE + "enable",
			"pipeline",
			"Pipeline to a specified gate depth (see -gd), or to a specified pipeline auto level (see -pal).");
	public static final OptionKey SCHEDULE_PIPELINE_GATE_DEPTH = new OptionKey(
			SCHEDULE_PIPELINE + "gate_depth", "gd",
			"Target gate depth for pipelining (see -pipeline).");
	public static final OptionKey SCHEDULE_PIPELINE_AUTO_LEVEL = new OptionKey(
			SCHEDULE_PIPELINE + "auto_level",
			"apl",
			"Auto pipelining level.  The amount of pipelining to perform.  This preference controls how many pipelining registers are inserted into the design in order to improve the max frequency the design can run at.  The number of registers inserted is determined by finding the longest (deepest) combinational path in the design.  The length of that path is divided by the value specified in this preference to determine the maximum allowable combinational depth for the design.  The entire design is then pipelined so that no combinational path exceeds this metric (if possible).  A zero value disables pipeline register insertion.");
	public static final OptionKey SCHEDULE_PIPELINE_NO_BOUNDRY_DEPTH = new OptionKey(
			SCHEDULE_PIPELINE + "no_boundry_depth", "pipe_nbd",
			"Set to true to suppress assigning gate depths to I/O's at the top level.");
	public static final OptionKey SCHEDULE_FIFO_IO = new OptionKey(
			PREFIX + "fifo_io",
			"fifoio",
			"Use FIFO based IO for all external entry methods.  The associated option: options.fifo_io can be scoped to individual entry methods, allowing a mix of FIFO and parallel based entry methods (i.e. forge -opt options.fifo_io@class.method=true).  When an entry is configured for FIFO IO all synchronous elements for the entry will have a global halt signal added for the entry to support reverse flow control when the output FIFO is full.  When mixing FIFO and parallel IO in a single design, inter task communication must be considered carefully since any FIFO IO task could halt at any time.  If any external entry method is configured for FIFO IO, then a separate output file is generated {design}_fifo.v with the requested IO configuration.");

	public static final OptionKey SCHEDULE_MULTIPLY_STAGES = new OptionKey(
			PREFIX + "multiplier_stages",
			"multstages",
			"Multiplier pipeline stages.  The number of pipeline stages applied to each multiplier.");
	public static final OptionKey SCHEDULE_NO_BLOCK_SCHEDULING = new OptionKey(
			PREFIX + "no_block_scheduling", "no_block_sched",
			"When set to true, disables block scheduling.");

	public static final OptionKey LOOP_BRANCH_BALANCE = new OptionKey(
			PREFIX + "loop_branch_balance",
			"loopbal",
			"When set to true, causes any branch statements in a loop to have its true and false branches balanced such that both are combinational or both are latency > 0.");
	public static final OptionKey LOOP_RESOURCE_FIX_POLICY = new OptionKey(
			PREFIX + "loop_resource_fix_policy",
			"loopresfixpolicy",
			"Integer value determining the policy to be used in breaking resource dependence problems in optimized loops.  Fix None: "
					+ LoopFlopConflictSet.FIX_NONE
					+ " Fix fewest accesses: "
					+ LoopFlopConflictSet.FIX_FEWEST
					+ " Fix most accesses: "
					+ LoopFlopConflictSet.FIX_MOST
					+ " fix first accesses: "
					+ LoopFlopConflictSet.FIRST_ALWAYS
					+ " fix last accesses "
					+ LoopFlopConflictSet.LAST_ALWAYS);

	// Optimize options
	public static final String LOOP_UNROLLING = PREFIX + "loop_unrolling.";
	public static final OptionKey LOOP_UNROLLING_ENABLE = new OptionKey(
			LOOP_UNROLLING + "enable", "unroll", "Enable all loop unrolling.");
	public static final OptionKey LOOP_UNROLLING_LIMIT = new OptionKey(
			LOOP_UNROLLING + "limit", "loopcount",
			"Maximum iteration count considered for unrolling.");
	public static final OptionKey MULTIPLY_DECOMP_LIMIT = new OptionKey(
			PREFIX + "multiply_decomp_limit",
			"multdecomplimit",
			"Maximum number of operations to which a half constant multiply can be decomposed.");
	public static final OptionKey SRL_COMPACT_LENGTH = new OptionKey(PREFIX
			+ "srl_compact_length", "minsrllength",
			"Minimum number of consecutive registers that will be compacted to an SRL.");
	public static final OptionKey SRL_NO_OUTPUT_REG = new OptionKey(
			PREFIX + "srl_no_output_register",
			"srlnoreg",
			"If set to true then all registers in a register chain will be merged into an SRL.  If set to false then the final (trailing) register will be left in a flop for performance. Default is false.");

	private static final List<String> defaultLibs;
	private static final String supportedReplacements;

	static {
		// Our default libraries are now automatically generated by
		// the OperationReplacmentVisitor class as temporary files
		// extracted from the Jar file.
		defaultLibs = Collections.emptyList();
		supportedReplacements = ReplacementCorrelation.getHelpString() + ".";
	}

	public static final OptionKey OPERATOR_REPLACEMENT_LIBS = new OptionKey(
			PREFIX + "implementation_library",
			"implib",
			"List of source code libraries for operator replacement.  Include the name of the c source file (i.e. DIV.c).  If the supplied file is an absolute path, then forge uses it directly, otherwise the given file name will be appended to each element of the sourcepath until a valid file is found.  The c source is dynamically compiled and querried for methods that match the supported replacement names along with the closest matching argument types to determine which method will be used for replacement.  The operation to method name associations are: "
					+ supportedReplacements);
	public static final OptionKey OPERATOR_REPLACEMENT_MAX_LEVEL = new OptionKey(
			PREFIX + "op_replacement_max_level",
			"max_op_rep",
			"Integer encoded limit on the most complex operation allowed to be replaced.  Generally the least complex operations are bitwise operators, and the most complex are rem/modulo and divide.");

	public static final OptionKey ROM_REPLICATION_LIMIT = new OptionKey(PREFIX
			+ "rom_replication_max_kbits", "romlimit",
			"Limit when replicating roms in kbits of total memory allocated to design.");
	public static final OptionKey FORCE_SINGLE_PORT_ROMS = new OptionKey(PREFIX
			+ "force_singleport_roms", "sproms",
			"Set to true to limit ROMs to being single ported.");
	public static final OptionKey MEM_DECOMPOSE_LIMIT = new OptionKey(
			PREFIX + "memory_decompose_limit",
			"decomplimit",
			"Specifies the maximum number of bytes in a memory that can be decomposed into registers.");

	public static final OptionKey BLOCK_NOUNNESTING = new OptionKey(PREFIX
			+ "block_nounnesting", "nounnest", "Don't unnest blocks.");

	public static final OptionKey WRITE_ONLY_INPUT_PARAM_OPT = new OptionKey(
			PREFIX + "write_only_parameter_optimize",
			"woparamopt",
			"Eliminates write only parameters of the entry function from being transferred across the input interface of a block I/O implementation if set to true. Set to true only if any passed in data in entry function parameters which are write-only in the algorithm can be ignored/lost.  This is safe if every location in the input parameter is written.");

	public static final OptionKey SYNC_RESET = new OptionKey(
			PREFIX + "sync_reset",
			"sync_reset",
			"When set to true, causes all configurable registers that use reset to implement a synchronous reset.");
	public static final OptionKey CLOCK_DOMAIN = new OptionKey(
			PREFIX + "clock_domain",
			"",
			"Used to specify the clock domain which is either a clock name or clock name/reset pair (eg CLK:RESET).");

	public static final OptionKey XENABLE_MPRINTER = new OptionKey(PREFIX
			+ "Xenable_mprinter", "Xmprinter",
			"Enable MPrinter to spit out the c source for a translation unit.");
	public static final OptionKey XMPRINTER_ONLY = new OptionKey(PREFIX
			+ "Xmprinter_only", "Xmprinter_only",
			"Stop Forge flow after MPrinter spits out the c source for a translation unit");
	public static final OptionKey WRITE_CYCLE_C = new OptionKey(PREFIX
			+ "xlat_cyclec", "xlat_c",
			"Write out a cycle accurate C model of the generated hardware implementation.");
	// public static final OptionKey X_WRITE_CYCLE_C = WRITE_CYCLE_C;
	public static final OptionKey X_WRITE_CYCLE_C_VPGEN = new OptionKey(
			PREFIX + "Xxlat_cyclec_vpgen",
			"Xxlat_c_vpgen",
			"Write out a Virtual Platform compliant wrapper for the cycle accurate C model.  This option depends on the generation of the cycle c model.");

	public static final OptionKey XNOGCC = new OptionKey(PREFIX + "Xnogcc",
			"Xnogcc", "skip compilation and preprocessing");
	public static final OptionKey XNOGCC_PRE = new OptionKey(PREFIX
			+ "Xnogcc_pre", "Xnogcc_pre", "skip gcc preprocessing");
	public static final OptionKey XNOGCC_COMP = new OptionKey(PREFIX
			+ "Xnogcc_comp", "Xnogcc_comp", "skip gcc compilation");

	public static final OptionKey XDETAILED_REPORT = new OptionKey(PREFIX
			+ "Xdetailed_report", "Xdetailed_report",
			"Adds extra sizing and constant information to resource report");
	//
	// Any 'extra' search labels that are needed are defined here.
	//
	public static final SearchLabel ORIGINAL_USER_ENTRY_FXN = new SearchLabel() {
		@Override
		public String getLabel() {
			return null;
		}

		@Override
		public List<String> getSearchList() {
			return Collections.singletonList("ORIGINAL_USER_ENTRY_FXN");
		}

		@Override
		public List<String> getSearchList(String postFix) {
			return Collections.singletonList("ORIGINAL_USER_ENTRY_FXN");
		}
	};

	public static final List<OptionKey> OPTION_KEYS = new ArrayList<OptionKey>();

	static {
		// Options that will be displayed in the help.
		// They are arranged in alphabetical order of CLA switch.
		// Please put in any new option in the right place below.
		OPTION_KEYS.add(MEMORY_ALIGN); // align
		OPTION_KEYS.add(SCHEDULE_PIPELINE_AUTO_LEVEL); // apl
		OPTION_KEYS.add(SCHEDULE_BALANCE); // balance
		OPTION_KEYS.add(DESTINATION_DIR); // d (destination)
		OPTION_KEYS.add(MEM_DECOMPOSE_LIMIT); // decompose
		OPTION_KEYS.add(DESTINATION_FOLLOWS_TARGET); // dfs
		OPTION_KEYS.add(ENTRY); // entry
		OPTION_KEYS.add(HELP); // help
		OPTION_KEYS.add(HELP_DETAIL); // help_detail
		OPTION_KEYS.add(INCLUDES_DIR); // I (includes dir)
		OPTION_KEYS.add(OPERATOR_REPLACEMENT_LIBS); // implib
		OPTION_KEYS.add(INVERT_DESIGN_PORT_RANGE); // invert_port_ranges
		OPTION_KEYS.add(LICENSE); // license
		OPTION_KEYS.add(LICENSEKEY); // license_key
		OPTION_KEYS.add(LITTLE_ENDIAN); // little_endian
		OPTION_KEYS.add(LOG); // log
		OPTION_KEYS.add(LOOP_UNROLLING_LIMIT); // loopcount
		OPTION_KEYS.add(MULTIPLY_DECOMP_LIMIT); // multdecomplimit
		OPTION_KEYS.add(SRL_COMPACT_LENGTH); // minsrllength
		OPTION_KEYS.add(MAX_LUT_BYTES); // max_lut_bytes
		// OPTION_KEYS.add(MAX_LUT_DEPTH); // max_lut_depth
		// OPTION_KEYS.add(MAX_LUT_WIDTH); // max_lut_width
		OPTION_KEYS.add(SCHEDULE_MULTIPLY_STAGES); // mult_stages
		OPTION_KEYS.add(SCHEDULE_NO_BLOCK_SCHEDULING); // no_block_sched
		OPTION_KEYS.add(LOOP_BRANCH_BALANCE); // loopbal
		OPTION_KEYS.add(LOOP_RESOURCE_FIX_POLICY); // loopresfixpolicy
		OPTION_KEYS.add(MODULE_BUILDER); // Xmodule_builder
		OPTION_KEYS.add(NO_BLOCK_IO); // noblockio
		OPTION_KEYS.add(NO_EDK); // noedk
		OPTION_KEYS.add(SYSGEN); // sysgen
		OPTION_KEYS.add(NOLOG); // nolog
		OPTION_KEYS.add(NOTES); // notes
		OPTION_KEYS.add(DESTINATION_FILE); // o (output file to generate)
		OPTION_KEYS.add(PROJ_FILE); // p (proj_file)
		OPTION_KEYS.add(PROJ_FILE_DESC); // P (proj_file with desc.)
		OPTION_KEYS.add(PFILE); // pfile
		OPTION_KEYS.add(PE_VERSION); // pever
		OPTION_KEYS.add(QUIET); // q (quiet)
		OPTION_KEYS.add(ROM_REPLICATION_LIMIT); // romlimit
		OPTION_KEYS.add(SOURCEPATH); // sourcepath
		OPTION_KEYS.add(TARGET_SPEED); // speed
		OPTION_KEYS.add(TOP_MODULE_NAME); // topmod
		OPTION_KEYS.add(UCF_FILE); // ucffile (to generate)
		OPTION_KEYS.add(UCF_INCLUDES); // ucf (to include)
		OPTION_KEYS.add(LOOP_UNROLLING_ENABLE); // unroll
		OPTION_KEYS.add(VERBOSE); // v
		OPTION_KEYS.add(VERBOSE_VERBOSE); // vv
		OPTION_KEYS.add(VERSION); // version
		OPTION_KEYS.add(ENABLE_XFLOW); // xflow
		OPTION_KEYS.add(XILINX_PART); // xlpart
		OPTION_KEYS.add(WRITE_CYCLE_C); // xlat_c
		OPTION_KEYS.add(SYNC_RESET); // sync_reset

		// Hidden options and other options that do not have a CLA switch.
		OPTION_KEYS.add(SRL_NO_OUTPUT_REG);
		OPTION_KEYS.add(FORCE_GO_DONE);
		OPTION_KEYS.add(ENABLE_SPEED);
		OPTION_KEYS.add(TARGET);
		OPTION_KEYS.add(SHOULD_SIMULATE);
		OPTION_KEYS.add(SUPPRESS_APP_MODULE);
		OPTION_KEYS.add(SHOULD_TIME_STAMP);
		OPTION_KEYS.add(NO_IOB_OPTS);
		OPTION_KEYS.add(BLOCK_IO_FIFO_WIDTH);
		OPTION_KEYS.add(CHANNEL_DESCRIPTOR);
		OPTION_KEYS.add(AUTO_TEST_BENCH);
		OPTION_KEYS.add(C_AUTO_TEST_BENCH);
		OPTION_KEYS.add(HANG_TIMER);
		OPTION_KEYS.add(ATB_GO_SPACING);
		OPTION_KEYS.add(ATB_BENCHMARK_REPORT);
		OPTION_KEYS.add(CCOMPILECFLAGS);
		OPTION_KEYS.add(XGCCRAW);
		OPTION_KEYS.add(XNOGCC_COMP);
		OPTION_KEYS.add(XDETAILED_REPORT);
		OPTION_KEYS.add(XNOGCC_PRE);
		OPTION_KEYS.add(XGCCANSI);
		OPTION_KEYS.add(CCOMPILECPPFLAGS);
		OPTION_KEYS.add(MEMORY_BANK_WIDTH);
		OPTION_KEYS.add(COMBINATIONAL_LUT_MEM);
		OPTION_KEYS.add(ALLOW_DUAL_PORT_LUT);
		OPTION_KEYS.add(SUPPRESS_DUAL_PORT_RAM);
		OPTION_KEYS.add(SIMPLE_STATE_ARBITRATION);
		OPTION_KEYS.add(SHOULD_NOT_TRANSLATE);
		OPTION_KEYS.add(TSIM_OPTION);
		OPTION_KEYS.add(SYNTH_OPTION);
		OPTION_KEYS.add(IMPLEMENT_OPTION);
		OPTION_KEYS.add(BITGEN_OPTIONS);
		OPTION_KEYS.add(UCF_ONLY);
		OPTION_KEYS.add(REPORT);
		OPTION_KEYS.add(RUN_DATE); // rundate
		OPTION_KEYS.add(SIMPLE_MODULE_NAMES);
		OPTION_KEYS.add(SIGNATURE_IN_NAMES);
		OPTION_KEYS.add(DEBUG_MEM_LOCATIONS);
		OPTION_KEYS.add(SCHEDULE_PIPELINE_ENABLE);
		OPTION_KEYS.add(SCHEDULE_PIPELINE_GATE_DEPTH);
		OPTION_KEYS.add(SCHEDULE_PIPELINE_NO_BOUNDRY_DEPTH);
		OPTION_KEYS.add(SCHEDULE_FIFO_IO);
		OPTION_KEYS.add(OPERATOR_REPLACEMENT_MAX_LEVEL);
		OPTION_KEYS.add(FORCE_SINGLE_PORT_ROMS);
		OPTION_KEYS.add(BLOCK_NOUNNESTING);
		OPTION_KEYS.add(WRITE_ONLY_INPUT_PARAM_OPT);
		OPTION_KEYS.add(LONG_VERILOG_NAMES);
		OPTION_KEYS.add(NO_INCLUDE_FILES);
		OPTION_KEYS.add(XENABLE_MPRINTER); // Xmprinter
		OPTION_KEYS.add(XMPRINTER_ONLY); // Xmprinter_only
		OPTION_KEYS.add(X_WRITE_CYCLE_C_VPGEN); // Xxlat_c_vpgen
		OPTION_KEYS.add(CLOCK_DOMAIN);
	}

	private static final int MAX_COLUMN_WIDTH = 80;

	private static final String usageDescription = "Passing in c source files will cause them to be compiled. If a .forge "
			+ "project file is passed in using the '-pfile' switch, forge will compile "
			+ "using the settings of the project, any of which may be overridden by "
			+ "command line options.  Any files of type .ucf that need to be used for "
			+ "xflow should be given using the -ucf switch. Multiple ucf files, if needed, "
			+ "should each be given by a separate -ucf switch. ";

	private static final String optDetail = "This option may be repeated on the command line as many times as necessary.  "
			+ "-opt overrides a named option.  The Forge option of the name supplied is "
			+ "replaced with the given value (i.e. optimize.loop_unrolling.enable=true).  "
			+ "-opt used without the scope or label qualifiers sets the option at the global level (applicable to the entire design.  "
			+ "The option can be scoped by appending the '@' symbol followed by the scope to the option "
			+ "(i.e. optimize.loop_unrolling.enable@test_source.test_function=true).  "
			+ "A scope can also be further qualified by including a label (i.e. source.function#label).  "
			+ "Labels may be specified for different levels of scope (function, source file, global). "
			+ "When using a scope that includes a function, you can simply include the function name without a "
			+ "signature as a shortcut or include the full function specification. Here are some examples of "
			+ "various scoped labels...\n"
			+ "\toptimize.loop_unrolling.enable@test_source.function(ZCIL)#LABEL=true\n"
			+ "\toptimize.loop_unrolling.enable@test_source.function#LABEL=true\n"
			+ "\toptimize.loop_unrolling.enable@test_source#LABEL=true\n"
			+ "\toptimize.loop_unrolling.enable@LABEL=true\n";

	public static HashMap<OptionKey, Option> getDefaults() {
		HashMap<OptionKey, Option> defaults = new HashMap<OptionKey, Option>();

		defaults.put(PROJ_FILE, new OptionBoolean(PROJ_FILE, // key
				false, // default value
				false // hidden ?
				));
		defaults.put(PROJ_FILE_DESC, new OptionBoolean(PROJ_FILE_DESC, // key
				false, // default value
				false // hidden ?
				));
		defaults.put(PFILE, new OptionFile(PFILE, // key
				"", // default value
				"(.+\\.forge)??", // regexp filter -- something with pref
									// extension, or nothing at all
				false // hidden?
				));
		defaults.put(QUIET, new OptionBoolean(QUIET, // key
				false, // default value
				false // hidden ?
				));
		defaults.put(VERSION, new OptionBoolean(VERSION, // key
				false, // default value
				false // hidden ?
				));
		defaults.put(NOTES, new OptionBoolean(NOTES, // key
				false, // default value
				false // hidden ?
				));
		defaults.put(HELP, new OptionBoolean(HELP, // key
				false, // default value
				false // hidden ?
				));
		defaults.put(HELP_DETAIL, new OptionBoolean(HELP_DETAIL, // key
				false, // default value
				false // hidden ?
				));
		defaults.put(LICENSE, new OptionBoolean(LICENSE, // key
				false, // default value
				false // hidden ?
				));
		defaults.put(LICENSEKEY, new OptionString(LICENSEKEY, // key
				"", // default value
				false // hidden?
				));
		defaults.put(CWD, new OptionFile(CWD, // key
				System.getProperty("user.dir"), // default value
				OptionFile.DIRECTORY_REGEXP, true // hidden?
				));
		defaults.put(TARGET, new OptionMultiFile(TARGET, // key
				"", // default value
				".*\\.((c)|(C)|(xlim)|(XLIM)|(sxlim)|(SXLIM))$", // regexp - gcc
																	// allows .c
																	// or
				// .C on solaris and windows, but javac only allows
				// .java - .JAVA fails on solaris and windows.
				false // hidden?
				));
		defaults.put(DESTINATION_FILE, new OptionFile(DESTINATION_FILE, // key
				"", // default value empty indicates to inherit from TARGET
				// "(.*\\.(v))?", // regexp
				"((\\S+)|(^$))", // regexp, 1 or more non-whitespace chars, or
									// the empty string
				false // hidden?
				));
		defaults.put(DESTINATION_DIR, new OptionFile(DESTINATION_DIR, // key
				".", // default value
				OptionFile.DIRECTORY_REGEXP, // regular expression
				false // hidden?
				));
		defaults.put(INCLUDES_DIR, new OptionMultiFile(INCLUDES_DIR, // key
				".", // default value
				OptionFile.DIRECTORY_REGEXP, // regular expression
				false // hidden?
				));
		defaults.put(VERBOSE, new OptionBoolean(VERBOSE, false, // default value
				false // is completely hidden?
				));
		defaults.put(VERBOSE_VERBOSE, new OptionBoolean(VERBOSE_VERBOSE, false,
				false // is completely hidden?
				));
		defaults.put(NOLOG, new OptionBoolean(NOLOG, false, false // is
																	// completely
																	// hidden?
				));
		defaults.put(NO_IOB_OPTS, new OptionBoolean(NO_IOB_OPTS, false, true // is
																				// completely
																				// hidden?
				));
		defaults.put(LOG, new OptionList(LOG, // key
				"[console, forge.log=info]", false // hidden?
				));
		defaults.put(DESTINATION_FOLLOWS_TARGET, new OptionBoolean(
				DESTINATION_FOLLOWS_TARGET, false, // default value
				false // hidden ?
				));
		defaults.put(SOURCEPATH,
				new OptionPath(SOURCEPATH, "[" + System.getProperty("user.dir")
						+ "]", false));
		defaults.put(SHOULD_SIMULATE, new OptionBoolean(SHOULD_SIMULATE, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SHOULD_NOT_TRANSLATE, new OptionBoolean(
				SHOULD_NOT_TRANSLATE, // key
				false, // default value
				true // hidden?
				));
		defaults.put(FORCE_GO_DONE, new OptionBoolean(FORCE_GO_DONE, // key
				false, // default value
				true // hidden?
				));
		defaults.put(TOP_MODULE_NAME, new OptionString(TOP_MODULE_NAME, // key
				"", // default value, empty indicates inherit from target
				false // hidden?
				));
		defaults.put(INVERT_DESIGN_PORT_RANGE, new OptionBoolean(
				INVERT_DESIGN_PORT_RANGE, // key
				false, // default value
				false // hidden?
				));
		defaults.put(LONG_VERILOG_NAMES, new OptionBoolean(LONG_VERILOG_NAMES, // key
				false, // default value
				true // hidden?
				));
		defaults.put(NO_INCLUDE_FILES, new OptionBoolean(NO_INCLUDE_FILES, // key
				false, // default value
				false // hidden?
				));

		defaults.put(SUPPRESS_APP_MODULE, new OptionBoolean(
				SUPPRESS_APP_MODULE, // key
				false, // default value
				true // hidden?
				));
		// defaults.put(MAX_LUT_DEPTH, new OptionString(
		// MAX_LUT_DEPTH, // key
		// "128", // default value
		// false // hidden?
		// ));
		// defaults.put(MAX_LUT_WIDTH, new OptionString(
		// MAX_LUT_WIDTH, // key
		// "64", // default value
		// false // hidden?
		// ));
		defaults.put(MAX_LUT_BYTES, new OptionInt(MAX_LUT_BYTES, // key
				1024, // default value
				false // hidden?
				));
		defaults.put(MEMORY_BANK_WIDTH, new OptionInt(MEMORY_BANK_WIDTH, // key
				-1, // default value
				true // hidden?
				));
		defaults.put(SIMPLE_STATE_ARBITRATION, new OptionBoolean(
				SIMPLE_STATE_ARBITRATION, // key
				false, // default value
				false // hidden?
				));
		defaults.put(COMBINATIONAL_LUT_MEM, new OptionBoolean(
				COMBINATIONAL_LUT_MEM, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SUPPRESS_DUAL_PORT_RAM, new OptionBoolean(
				SUPPRESS_DUAL_PORT_RAM, // key
				false, // default value
				false // hidden?
				));
		defaults.put(ALLOW_DUAL_PORT_LUT, new OptionBoolean(
				ALLOW_DUAL_PORT_LUT, // key
				false, // default value
				false // hidden?
				));
		defaults.put(AUTO_TEST_BENCH, new OptionBoolean(AUTO_TEST_BENCH, // key
				false, // default value
				true // hidden?
				));
		defaults.put(C_AUTO_TEST_BENCH, new OptionBoolean(C_AUTO_TEST_BENCH, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SHOULD_TIME_STAMP, new OptionBoolean(SHOULD_TIME_STAMP, // key
				false, // default value
				true // hidden?
				));
		defaults.put(HANG_TIMER, new OptionString(HANG_TIMER, // key
				"1500", // default value
				true // hidden?
				));
		defaults.put(ATB_GO_SPACING, new OptionInt(ATB_GO_SPACING, // key
				-1, // default value
				true // hidden?
				));
		defaults.put(ENABLE_XFLOW, new OptionBoolean(ENABLE_XFLOW, // key
				false, // default value
				false // hidden?
				));
		defaults.put(XILINX_PART, new OptionXilPart(XILINX_PART, // key
				"xc2v8000-5-ff1152C", // per ian
				false // hidden
				));
		defaults.put(UCF_ONLY, new OptionBoolean(UCF_ONLY, false, // default
																	// value
				true // hidden?
				));
		defaults.put(TSIM_OPTION, new OptionPickOne(TSIM_OPTION, // key
				"modelsim_verilog.opt", // default value
				new String[] { "modelsim_verilog.opt" }, // possible values to
															// pick from
				true, // hidden?
				true // editable ?
				));
		defaults.put(IMPLEMENT_OPTION, new OptionPickOne(IMPLEMENT_OPTION, // key
				"balanced.opt", // default value
				new String[] { "fast_runtime.opt", "balanced.opt",
						"high_effort.opt" }, // possible values to pick from
				false, // hidden?
				true // editable ?
				));
		defaults.put(SYNTH_OPTION, new OptionPickOne(SYNTH_OPTION, // key
				"xst_verilog.opt", // default value
				new String[] { "xst_verilog.opt" }, // possible values to pick
													// from
				true, // hidden?
				true // editable ??
				));
		defaults.put(
				BITGEN_OPTIONS,
				new OptionString(
						BITGEN_OPTIONS, // key
						"-l -m -b -w -g ReadBack -g StartupClk:JtagClk -g UserID:0x12345678",
						false // hidden?
				));
		defaults.put(ENABLE_SPEED, new OptionBoolean(ENABLE_SPEED, true, // default
																			// value
				false // hidden?
				));
		defaults.put(TARGET_SPEED, new OptionIntUnit(TARGET_SPEED, // key
				"", // default value
				new String[] { "MHz", "KHz", "ns" }, // possible units
				false // hidden?
				));
		defaults.put(UCF_FILE, new OptionFile(UCF_FILE, // key
				"", // default value
				"(.+\\.ucf)??", // regexp filter -- something with ucf
								// extension, or nothing at all
				false // hidden?
				));
		defaults.put(UCF_INCLUDES, new OptionMultiFile(UCF_INCLUDES, // key
				"", // default value
				"(.*\\.ucf)+", // regexp
				false // hidden?
				));
		defaults.put(REPORT, new OptionBoolean(REPORT, // key
				false, // default value
				true // hidden?
				));
		defaults.put(RUN_DATE, new OptionString(RUN_DATE, // key
				"", // default value
				true // hidden?
				));
		defaults.put(DEBUG_MEM_LOCATIONS, new OptionBoolean(
				DEBUG_MEM_LOCATIONS, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SIMPLE_MODULE_NAMES, new OptionBoolean(
				SIMPLE_MODULE_NAMES, true, false // is completely hidden?
				));
		defaults.put(SIGNATURE_IN_NAMES, new OptionBoolean(SIGNATURE_IN_NAMES,
				false, false // is completely hidden?
				));
		defaults.put(CCOMPILECFLAGS, new OptionString(CCOMPILECFLAGS, // key
				" -m32 -Wmissing-braces -Werror", true // hidden?
				));
		defaults.put(XGCCRAW, new OptionBoolean(XGCCRAW, // key
				true, // default value
				true // hidden?
				));
		defaults.put(XNOGCC_COMP, new OptionBoolean(XNOGCC_COMP, // key
				false, // default value
				true // hidden?
				));
		defaults.put(XDETAILED_REPORT, new OptionBoolean(XDETAILED_REPORT, // key
				false, // default value
				true // hidden?
				));
		defaults.put(XNOGCC_PRE, new OptionBoolean(XNOGCC_PRE, // key
				false, // default value
				true // hidden?
				));
		defaults.put(XGCCANSI, new OptionBoolean(XGCCANSI, // key
				false, // default value
				true // hidden?
				));
		defaults.put(CCOMPILECPPFLAGS, new OptionString(CCOMPILECPPFLAGS, // key
				" -m32", true // hidden?
				));
		defaults.put(MODULE_BUILDER, new OptionBoolean(MODULE_BUILDER, // key
				false, // default value
				true // hidden?
				));
		defaults.put(NO_BLOCK_IO, new OptionBoolean(NO_BLOCK_IO, // key
				false, // default value
				false // hidden?
				));
		defaults.put(BLOCK_IO_FIFO_WIDTH, new OptionInt(BLOCK_IO_FIFO_WIDTH, // key
				4, // default value
				true // hidden?
				));
		defaults.put(CHANNEL_DESCRIPTOR, new OptionList(CHANNEL_DESCRIPTOR, // key
				"null", // default value
				false // hidden?
				));
		// The channel descriptors use : to separate the fields of
		// each descriptor.
		OptionList cdOL = (OptionList) defaults.get(CHANNEL_DESCRIPTOR);
		cdOL.setSeparators(cdOL.getSeparators().replaceAll(":", ""));
		defaults.put(LITTLE_ENDIAN, new OptionBoolean(LITTLE_ENDIAN, // key
				false, // default value
				false // hidden?
				));
		defaults.put(MEMORY_ALIGN, new OptionInt(MEMORY_ALIGN, // key
				8, // default value
				false // hidden?
				));
		defaults.put(NO_EDK, new OptionBoolean(NO_EDK, // key
				false, // default value
				false // hidden?
				));
		defaults.put(SYSGEN, new OptionBoolean(SYSGEN, // key
				false, // default value
				false // hidden?
				));
		defaults.put(ENTRY, new OptionString(ENTRY, // key
				"", false // hidden?
				));
		defaults.put(PE_NAME, new OptionString(PE_NAME, // key
				"unnamed", true // hidden?
				));
		defaults.put(PE_VERSION, new OptionString(PE_VERSION, // key
				"v1_00_a", false // hidden?
				));
		defaults.put(ATB_BENCHMARK_REPORT, new OptionBoolean(
				ATB_BENCHMARK_REPORT, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SCHEDULE_BALANCE, new OptionBoolean(SCHEDULE_BALANCE, // key
				false, // default value
				false // hidden?
				));
		defaults.put(SCHEDULE_PIPELINE_ENABLE, new OptionBoolean(
				SCHEDULE_PIPELINE_ENABLE, // key
				true, // default value
				true // hidden?
				));
		defaults.put(SCHEDULE_PIPELINE_GATE_DEPTH, new OptionInt(
				SCHEDULE_PIPELINE_GATE_DEPTH, // key
				0, // default value
				true // hidden?
				));
		defaults.put(SCHEDULE_PIPELINE_AUTO_LEVEL, new OptionInt(
				SCHEDULE_PIPELINE_AUTO_LEVEL, // key
				0, // default value
				false // hidden?
				));
		defaults.put(SCHEDULE_PIPELINE_NO_BOUNDRY_DEPTH, new OptionBoolean(
				SCHEDULE_PIPELINE_NO_BOUNDRY_DEPTH, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SCHEDULE_FIFO_IO, new OptionBoolean(SCHEDULE_FIFO_IO, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SCHEDULE_MULTIPLY_STAGES, new OptionInt(
				SCHEDULE_MULTIPLY_STAGES, // key
				0, // default value
				false // hidden?
				));
		defaults.put(LOOP_BRANCH_BALANCE, new OptionBoolean(
				LOOP_BRANCH_BALANCE, // key
				false, // default value
				true // hidden?
				));
		defaults.put(LOOP_RESOURCE_FIX_POLICY, new OptionInt(
				LOOP_RESOURCE_FIX_POLICY, // key
				LoopFlopConflictSet.FIX_FEWEST, // default value
				false // hidden?
				));
		defaults.put(SCHEDULE_NO_BLOCK_SCHEDULING, new OptionBoolean(
				SCHEDULE_NO_BLOCK_SCHEDULING, // key
				false, // default value
				true // hidden?
				));
		defaults.put(LOOP_UNROLLING_ENABLE, new OptionBoolean(
				LOOP_UNROLLING_ENABLE, // key
				true, // default value
				false // hidden?
				));
		defaults.put(LOOP_UNROLLING_LIMIT, new OptionInt(LOOP_UNROLLING_LIMIT, // key
				1, // default value
				false // hidden?
				));
		defaults.put(MULTIPLY_DECOMP_LIMIT, new OptionInt(
				MULTIPLY_DECOMP_LIMIT, // key
				5, // default value
				false // hidden?
				));
		defaults.put(SRL_NO_OUTPUT_REG, new OptionBoolean(SRL_NO_OUTPUT_REG, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SRL_COMPACT_LENGTH, new OptionInt(SRL_COMPACT_LENGTH, // key
				2, // default value
				false // hidden?
				));
		defaults.put(OPERATOR_REPLACEMENT_LIBS, new OptionList(
				OPERATOR_REPLACEMENT_LIBS, OptionList.toString(defaultLibs),
				false));
		defaults.put(OPERATOR_REPLACEMENT_MAX_LEVEL, new OptionInt(
				OPERATOR_REPLACEMENT_MAX_LEVEL, Integer.MAX_VALUE, // default
																	// value
				true // hidden?
				));
		defaults.put(ROM_REPLICATION_LIMIT, new OptionInt(
				ROM_REPLICATION_LIMIT, // key
				3024, // default value
				false // hidden?
				));
		defaults.put(FORCE_SINGLE_PORT_ROMS, new OptionBoolean(
				FORCE_SINGLE_PORT_ROMS, // key
				false, // default value
				true // hidden?
				));
		defaults.put(MEM_DECOMPOSE_LIMIT, new OptionInt(MEM_DECOMPOSE_LIMIT, // key
				100, // default value
				false // hidden?
				));
		defaults.put(BLOCK_NOUNNESTING, new OptionBoolean(BLOCK_NOUNNESTING, // key
				false, // default value
				true // hidden?
				));
		defaults.put(WRITE_ONLY_INPUT_PARAM_OPT, new OptionBoolean(
				WRITE_ONLY_INPUT_PARAM_OPT, // key
				false, // default value
				true // hidden?
				));
		defaults.put(XENABLE_MPRINTER, new OptionBoolean(XENABLE_MPRINTER, // key
				false, // default value
				true // hidden?
				));
		defaults.put(XMPRINTER_ONLY, new OptionBoolean(XMPRINTER_ONLY, // key
				false, // default value
				true // hidden?
				));

		defaults.put(WRITE_CYCLE_C, new OptionBoolean(WRITE_CYCLE_C, // key
				false, // default value
				false // hidden?
				));
		defaults.put(X_WRITE_CYCLE_C_VPGEN, new OptionBoolean(
				X_WRITE_CYCLE_C_VPGEN, // key
				false, // default value
				true // hidden?
				));
		defaults.put(SYNC_RESET, new OptionBoolean(SYNC_RESET, // key
				false, // default value
				false // hidden?
				));

		defaults.put(CLOCK_DOMAIN, new OptionString(CLOCK_DOMAIN, // key
				"CLK:RESET", false // hidden?
				));

		return defaults;
	}

	private static String msgFormat(String msg, String linePrefix, int maxLen) {
		if (msg.length() <= maxLen) {
			return msg;
		}

		// If the message already has line breaks, then preserve those
		StringTokenizer lineTokenizer = new StringTokenizer(msg, "\n");
		if (lineTokenizer.countTokens() > 1) {
			String formatted = "";
			while (lineTokenizer.hasMoreTokens()) {
				formatted += msgFormat((String) lineTokenizer.nextElement(),
						linePrefix, maxLen) + "\n";
			}
			return formatted;
		}

		// If there is only one toke (no whitespace) return it unbroken
		StringTokenizer st = new StringTokenizer(msg, " \t", true);
		if (st.countTokens() < 1) {
			return msg;
		}

		// Break a the first whitespace before the max length.
		String formatted = (String) st.nextElement();
		int lineCount = formatted.length();
		while (st.hasMoreElements()) {
			String token = (String) st.nextElement();
			if (lineCount + token.length() > maxLen) {
				lineCount = token.length() + linePrefix.length();
				formatted += "\n" + linePrefix + token.trim();
			} else {
				lineCount += token.length();
				formatted += token;
			}
		}
		return formatted;
	}

	private static String pad(String f1, int f1size) {
		int pad = f1size - f1.length();

		if (pad < 0) {
			pad = 0;
		}

		String result = "";

		for (int i = 0; i < pad; i++) {
			result += " ";
		}

		return f1 + result;
	} // pad()

	/**
	 * Method used to print the usage of Forge. This is called when invalid CLAs
	 * are given to forge or when forge is called with one of the help options
	 * (-h , -help, -?).
	 */
	public static String usage(boolean detailed) {
		final StringWriter usage = new StringWriter();
		final PrintWriter ps = new PrintWriter(usage);

		ps.println("Usage is: forge [option...] [input1.c ... inputN.c]");
		ps.println();
		ps.println("Where options are:");

		final Map<String, String> items = new LinkedHashMap<String, String>();
		final Map<String, String> itemsDetail = new LinkedHashMap<String, String>();
		final Map<OptionKey, Option> defaultOptions = OptionRegistry
				.getDefaults();
		int maxLength = 0;
		for (OptionKey opt : OptionRegistry.OPTION_KEYS) {
			final Option p = defaultOptions.get(opt);

			if (!p.isHidden()) {
				final String key = p.getOptionKey().getHelpFormattedKeyList();
				final String value = p.getHelpValueDescription();
				// Even if the value is empty we'll just add one more
				// space which is OK
				final String qualifiedKey = key + " " + value;

				if (key.length() > 0) {
					maxLength = Math.max(maxLength, qualifiedKey.length());
					items.put(qualifiedKey, p.getBriefDescription());
					itemsDetail.put(qualifiedKey, p.getDescription().trim());
				}
			}
		}
		maxLength = Math.max(maxLength, 19);
		items.put("-opt <option=value>",
				"Override a named option, scope using <option@scope{#label}=value>.");
		itemsDetail.put("-opt <option{@scope{#label}}=value>", optDetail);

		// Add 4 spaces to the max key length so that we print out
		// nicely formatted text.
		maxLength += 4;
		String secondLinePad = "";
		for (int i = 0; i < maxLength; i++) {
			secondLinePad += " ";
		}

		for (Entry<String, String> entry : items.entrySet()) {
			String msg = pad(entry.getKey().toString(), maxLength)
					+ entry.getValue().toString();
			ps.println("    "
					+ msgFormat(msg, "    " + secondLinePad, MAX_COLUMN_WIDTH));
		}

		ps.println();
		ps.println(msgFormat(usageDescription, "", MAX_COLUMN_WIDTH));
		ps.println();

		if (detailed) {
			ps.println("Details...");
			for (Entry<String, String> entry : itemsDetail.entrySet()) {
				ps.println(entry.getKey().toString());
				ps.println("     "
						+ msgFormat(entry.getValue().toString(), "     ",
								MAX_COLUMN_WIDTH));
				ps.println();
			}
		}

		ps.flush();

		return usage.toString();
	} // usage()
}
