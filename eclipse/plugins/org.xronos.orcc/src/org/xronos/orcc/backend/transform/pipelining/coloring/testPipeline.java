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
package org.xronos.orcc.backend.transform.pipelining.coloring;

import java.io.File;

public class testPipeline {

	private static class testFDCT {
		public final static int NC = 140;
		public final static int MC = 148;
		public final static int IC = 2;
		public final static int OC = 1;

		public final static int T = 15;
		public final static int MODE = 2;
		public final static float StageTime = 2.20f;

		public final static OperatorType[] OpT = {
				new OperatorType("+", 1.0f, 1.0f), // 0
				new OperatorType("-", 1.1f, 1.0f), // 1
				new OperatorType("*", 3.0f, 5.0f), // 2
				new OperatorType("bitand", 0.02f, 0.5f), // 3
				new OperatorType("bitor", 0.02f, 0.5f), // 4
				new OperatorType("rshift", 0.1f, 0.2f), // 5
				new OperatorType("lshift", 0.1f, 0.2f), // 6
				new OperatorType("dummy", 0.0f, 0.0f), // 7
				new OperatorType("PLUS", 1.0f, 1.0f), // 8
				new OperatorType("MINUS", 1.1f, 1.0f), // 9
				new OperatorType("TIMES", 3.0f, 5.0f), // 10
				new OperatorType("SHIFT_RIGHT", 0.1f, 0.2f), // 11
				new OperatorType("SHIFT_LEFT", 0.1f, 0.2f), // 12
				new OperatorType("ASSIGN", 0.0f, 0.0f), // 13
				new OperatorType("CAST", 0.0f, 0.0f), // 14
		};

		public static final String[] OpNN = { "PLUS", "MINUS", "PLUS", "MINUS",
				"PLUS", "MINUS", "PLUS", "MINUS", "ASSIGN", "ASSIGN", "CAST",
				"SHIFT_RIGHT", "CAST", "SHIFT_RIGHT", "MINUS", "ASSIGN",
				"CAST", "SHIFT_RIGHT", "MINUS", "ASSIGN", "CAST",
				"SHIFT_RIGHT", "PLUS", "ASSIGN", "ASSIGN", "CAST",
				"SHIFT_RIGHT", "MINUS", "CAST", "SHIFT_RIGHT", "MINUS",
				"ASSIGN", "ASSIGN", "CAST", "SHIFT_RIGHT", "CAST",
				"SHIFT_RIGHT", "MINUS", "ASSIGN", "CAST", "SHIFT_RIGHT",
				"MINUS", "ASSIGN", "CAST", "SHIFT_RIGHT", "PLUS", "ASSIGN",
				"ASSIGN", "CAST", "SHIFT_RIGHT", "MINUS", "CAST",
				"SHIFT_RIGHT", "MINUS", "PLUS", "MINUS", "ASSIGN", "CAST",
				"SHIFT_RIGHT", "ASSIGN", "CAST", "SHIFT_RIGHT", "MINUS",
				"ASSIGN", "CAST", "SHIFT_RIGHT", "MINUS", "ASSIGN", "CAST",
				"SHIFT_RIGHT", "ASSIGN", "CAST", "SHIFT_RIGHT", "MINUS",
				"ASSIGN", "CAST", "SHIFT_RIGHT", "MINUS", "MINUS", "PLUS",
				"PLUS", "MINUS", "PLUS", "MINUS", "PLUS", "MINUS", "PLUS",
				"MINUS", "PLUS", "MINUS", "PLUS", "MINUS", "ASSIGN", "CAST",
				"SHIFT_RIGHT", "PLUS", "ASSIGN", "CAST", "SHIFT_RIGHT",
				"MINUS", "ASSIGN", "CAST", "SHIFT_RIGHT", "PLUS", "ASSIGN",
				"ASSIGN", "CAST", "SHIFT_RIGHT", "CAST", "SHIFT_RIGHT", "PLUS",
				"ASSIGN", "CAST", "SHIFT_RIGHT", "PLUS", "ASSIGN", "CAST",
				"SHIFT_RIGHT", "MINUS", "ASSIGN", "CAST", "SHIFT_RIGHT",
				"PLUS", "ASSIGN", "ASSIGN", "CAST", "SHIFT_RIGHT", "CAST",
				"SHIFT_RIGHT", "ASSIGN", "PLUS", "PLUS", "MINUS", "ASSIGN",
				"ASSIGN", "ASSIGN", "ASSIGN", "ASSIGN", "ASSIGN", "ASSIGN" };

		public static final String[] VaNN = { "in0", "in1", "in2", "in3",
				"in4", "in5", "in6", "in7", "x0_2", "x1_2", "x4_2", "x5_2",
				"x2_2", "x3_2", "x6_2", "x7_2", "inlined_tmp1_1",
				"inlined_tmp2_1", "xa_2", "x3_3", "inlined_tmp10_1",
				"inlined_tmp20_1", "xb_2", "x5_3", "x3_4", "x5_4", "xa_3",
				"inlined_tmp_1", "x1_3", "xb_3", "inlined_tmp0_1", "x7_3",
				"x1_4", "x7_4", "xa_4", "x3_5", "xb_4", "x5_5", "x1_5", "x7_5",
				"xa_5", "x6_3", "xb_5", "x2_3", "x0_3", "x4_3",
				"inlined_tmp3_1", "xa_6", "inlined_tmp4_1", "x2_4",
				"inlined_tmp5_1", "xb_6", "inlined_tmp6_1", "x6_4", "x2_5",
				"x6_5", "out0_1", "out1_1", "out2_1", "out3_1", "out4_1",
				"out5_1", "out6_1", "out7_1", "literal", "literal0",
				"literal1", "literal2", "literal3", "literal4", "literal5",
				"literal6", "literal7", "literal8", "literal9", "literal10",
				"literal11", "literal12", "literal13", "literal14",
				"literal15", "literal16", "literal17", "literal18",
				"literal19", "literal20", "literal21", "literal22",
				"literal23", "literal24", "literal25", "literal26",
				"splitted_expr", "splitted_expr0", "splitted_expr1",
				"splitted_expr2", "splitted_expr3", "splitted_expr4",
				"splitted_expr5", "splitted_expr6", "splitted_expr7",
				"splitted_expr8", "splitted_expr9", "splitted_expr10",
				"splitted_expr11", "splitted_expr12", "splitted_expr13",
				"splitted_expr14", "splitted_expr15", "splitted_expr16",
				"splitted_expr17", "splitted_expr18", "splitted_expr19",
				"splitted_expr20", "splitted_expr21", "splitted_expr22",
				"splitted_expr23", "splitted_expr24", "splitted_expr25",
				"splitted_expr26", "castedExpr_calculate",
				"castedExpr_calculate0", "castedExpr_calculate1",
				"castedExpr_calculate2", "castedExpr_calculate3",
				"castedExpr_calculate4", "castedExpr_calculate5",
				"castedExpr_calculate6", "castedExpr_calculate7",
				"castedExpr_calculate8", "castedExpr_calculate9",
				"castedExpr_calculate10", "castedExpr_calculate11",
				"castedExpr_calculate12", "castedExpr_calculate13",
				"castedExpr_calculate14", "castedExpr_calculate15",
				"castedExpr_calculate16", "castedExpr_calculate17",
				"castedExpr_calculate18", "castedExpr_calculate19",
				"castedExpr_calculate20", "castedExpr_calculate21",
				"castedExpr_calculate22", "castedExpr_calculate23",
				"castedExpr_calculate24", "castedExpr_calculate25",
				"castedExpr_calculate26" };

		public static final int[] VaWW = { 32, 32, 32, 32, 32, 32, 32, 32, 32,
				32, // 0
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 10
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 20
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 30
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 40
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 50
				32, 32, 32, 32, 2, 3, 4, 1, 2, 3, // 60
				2, 3, 4, 1, 2, 3, 1, 4, 2, 1, // 70
				4, 2, 3, 2, 3, 2, 3, 3, 2, 3, // 80
				2, 3, 32, 32, 32, 32, 32, 32, 32, 32, // 90
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 100
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 110
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 120
				32, 32, 32, 32, 32, 32, 32, 32, 32, 32, // 130
				32, 32, 32, 32, 32, 32, 32, 32 // 140
		};

		public static final String[][] chFF = {
				{ "in0", "in7" }, // 0
				{ "in0", "in7" },
				{ "in1", "in6" },
				{ "in1", "in6" },
				{ "in2", "in5" },
				{ "in2", "in5" },
				{ "in3", "in4" },
				{ "in3", "in4" },
				{ "", "" },
				{ "", "" },
				{ "literal", "" }, // 10
				{ "x3_2", "castedExpr_calculate" },
				{ "literal0", "" },
				{ "x3_2", "castedExpr_calculate0" },
				{ "splitted_expr", "splitted_expr0" },
				{ "", "" },
				{ "literal1", "" },
				{ "x3_2", "castedExpr_calculate1" },
				{ "inlined_tmp1_1", "splitted_expr1" },
				{ "", "" },
				{ "literal2", "" }, // 20
				{ "inlined_tmp2_1", "castedExpr_calculate2" },
				{ "inlined_tmp1_1", "splitted_expr2" },
				{ "", "" },
				{ "", "" },
				{ "literal3", "" },
				{ "x3_2", "castedExpr_calculate3" },
				{ "x3_2", "splitted_expr3" },
				{ "literal4", "" },
				{ "x3_2", "castedExpr_calculate4" },
				{ "splitted_expr4", "splitted_expr5" }, // 30
				{ "", "" },
				{ "", "" },
				{ "literal5", "" },
				{ "x5_2", "castedExpr_calculate5" },
				{ "literal6", "" },
				{ "x5_2", "castedExpr_calculate6" },
				{ "splitted_expr6", "splitted_expr7" },
				{ "", "" },
				{ "literal7", "" },
				{ "x5_2", "castedExpr_calculate7" }, // 40
				{ "inlined_tmp10_1", "splitted_expr8" },
				{ "", "" },
				{ "literal8", "" },
				{ "inlined_tmp20_1", "castedExpr_calculate8" },
				{ "inlined_tmp10_1", "splitted_expr9" },
				{ "", "" },
				{ "", "" },
				{ "literal9", "" },
				{ "x5_2", "castedExpr_calculate9" },
				{ "x5_2", "splitted_expr10" }, // 50
				{ "literal10", "" },
				{ "x5_2", "castedExpr_calculate10" },
				{ "splitted_expr11", "splitted_expr12" },
				{ "x3_3", "xb_2" },
				{ "x5_3", "xa_2" },
				{ "", "" },
				{ "literal11", "" },
				{ "x1_2", "castedExpr_calculate11" },
				{ "", "" },
				{ "literal12", "" }, // 60
				{ "x1_2", "castedExpr_calculate12" },
				{ "splitted_expr13", "x1_2" },
				{ "", "" },
				{ "literal13", "" },
				{ "inlined_tmp_1", "castedExpr_calculate13" },
				{ "splitted_expr14", "inlined_tmp_1" },
				{ "", "" },
				{ "literal14", "" },
				{ "x7_2", "castedExpr_calculate14" },
				{ "", "" }, // 70
				{ "literal15", "" },
				{ "x7_2", "castedExpr_calculate15" },
				{ "splitted_expr15", "x7_2" },
				{ "", "" },
				{ "literal16", "" },
				{ "inlined_tmp0_1", "castedExpr_calculate16" },
				{ "splitted_expr16", "inlined_tmp0_1" },
				{ "x1_3", "xb_3" },
				{ "x7_3", "xa_3" },
				{ "x1_4", "x3_4" }, // 80
				{ "x1_4", "x3_4" },
				{ "x7_4", "x5_4" },
				{ "x7_4", "x5_4" },
				{ "xa_4", "xb_4" },
				{ "xa_4", "xb_4" },
				{ "x0_2", "x6_2" },
				{ "x0_2", "x6_2" },
				{ "x4_2", "x2_2" },
				{ "x4_2", "x2_2" },
				{ "xa_5", "xb_5" }, // 90
				{ "xa_5", "xb_5" },
				{ "", "" },
				{ "literal17", "" },
				{ "x2_3", "castedExpr_calculate17" },
				{ "x2_3", "splitted_expr17" },
				{ "", "" },
				{ "literal18", "" },
				{ "inlined_tmp3_1", "castedExpr_calculate18" },
				{ "inlined_tmp3_1", "splitted_expr18" },
				{ "", "" }, // 100
				{ "literal19", "" },
				{ "x2_3", "castedExpr_calculate19" },
				{ "x2_3", "splitted_expr19" },
				{ "", "" },
				{ "", "" },
				{ "literal20", "" },
				{ "inlined_tmp4_1", "castedExpr_calculate20" },
				{ "literal21", "" },
				{ "x2_3", "castedExpr_calculate21" },
				{ "splitted_expr20", "splitted_expr21" }, // 110
				{ "", "" },
				{ "literal22", "" },
				{ "x6_3", "castedExpr_calculate22" },
				{ "x6_3", "splitted_expr22" },
				{ "", "" },
				{ "literal23", "" },
				{ "inlined_tmp5_1", "castedExpr_calculate23" },
				{ "inlined_tmp5_1", "splitted_expr23" },
				{ "", "" },
				{ "literal24", "" }, // 120
				{ "x6_3", "castedExpr_calculate24" },
				{ "x6_3", "splitted_expr24" }, { "", "" }, { "", "" },
				{ "literal25", "" },
				{ "inlined_tmp6_1", "castedExpr_calculate25" },
				{ "literal26", "" },
				{ "x6_3", "castedExpr_calculate26" },
				{ "splitted_expr25", "splitted_expr26" },
				{ "xb_6", "x2_4" }, // 130
				{ "x6_4", "xa_6" }, { "x0_3", "" }, { "x1_5", "" },
				{ "x2_5", "" }, { "x3_5", "" }, { "x4_3", "" }, { "x5_5", "" },
				{ "x6_5", "" }, { "x7_5", "" } };

		public static final String[][] chHH = {
				{ "x0_2" }, // 0
				{ "x1_2" },
				{ "x4_2" },
				{ "x5_2" },
				{ "x2_2" },
				{ "x3_2" },
				{ "x6_2" },
				{ "x7_2" },
				{ "literal" },
				{ "literal0" },
				{ "castedExpr_calculate" }, // 10
				{ "splitted_expr" },
				{ "castedExpr_calculate0" },
				{ "splitted_expr0" },
				{ "inlined_tmp1_1" },
				{ "literal1" },
				{ "castedExpr_calculate1" },
				{ "splitted_expr1" },
				{ "inlined_tmp2_1" },
				{ "literal2" },
				{ "castedExpr_calculate2" }, // 20
				{ "splitted_expr2" },
				{ "xa_2" },
				{ "literal3" },
				{ "literal4" },
				{ "castedExpr_calculate3" },
				{ "splitted_expr3" },
				{ "splitted_expr4" },
				{ "castedExpr_calculate4" },
				{ "splitted_expr5" },
				{ "x3_3" }, // 30
				{ "literal5" },
				{ "literal6" },
				{ "castedExpr_calculate5" },
				{ "splitted_expr6" },
				{ "castedExpr_calculate6" },
				{ "splitted_expr7" },
				{ "inlined_tmp10_1" },
				{ "literal7" },
				{ "castedExpr_calculate7" },
				{ "splitted_expr8" }, // 40
				{ "inlined_tmp20_1" },
				{ "literal8" },
				{ "castedExpr_calculate8" },
				{ "splitted_expr9" },
				{ "xb_2" },
				{ "literal9" },
				{ "literal10" },
				{ "castedExpr_calculate9" },
				{ "splitted_expr10" },
				{ "splitted_expr11" }, // 50
				{ "castedExpr_calculate10" },
				{ "splitted_expr12" },
				{ "x5_3" },
				{ "x3_4" },
				{ "x5_4" },
				{ "literal11" },
				{ "castedExpr_calculate11" },
				{ "xa_3" },
				{ "literal12" },
				{ "castedExpr_calculate12" }, // 60
				{ "splitted_expr13" },
				{ "inlined_tmp_1" },
				{ "literal13" },
				{ "castedExpr_calculate13" },
				{ "splitted_expr14" },
				{ "x1_3" },
				{ "literal14" },
				{ "castedExpr_calculate14" },
				{ "xb_3" },
				{ "literal15" }, // 70
				{ "castedExpr_calculate15" },
				{ "splitted_expr15" },
				{ "inlined_tmp0_1" },
				{ "literal16" },
				{ "castedExpr_calculate16" },
				{ "splitted_expr16" },
				{ "x7_3" },
				{ "x1_4" },
				{ "x7_4" },
				{ "xa_4" }, // 80
				{ "x3_5" },
				{ "xb_4" },
				{ "x5_5" },
				{ "x1_5" },
				{ "x7_5" },
				{ "xa_5" },
				{ "x6_3" },
				{ "xb_5" },
				{ "x2_3" },
				{ "x0_3" }, // 90
				{ "x4_3" },
				{ "literal17" },
				{ "castedExpr_calculate17" },
				{ "splitted_expr17" },
				{ "inlined_tmp3_1" },
				{ "literal18" },
				{ "castedExpr_calculate18" },
				{ "splitted_expr18" },
				{ "xa_6" },
				{ "literal19" }, // 100
				{ "castedExpr_calculate19" },
				{ "splitted_expr19" },
				{ "inlined_tmp4_1" },
				{ "literal20" },
				{ "literal21" },
				{ "castedExpr_calculate20" },
				{ "splitted_expr20" },
				{ "castedExpr_calculate21" },
				{ "splitted_expr21" },
				{ "x2_4" }, // 110
				{ "literal22" }, { "castedExpr_calculate22" },
				{ "splitted_expr22" },
				{ "inlined_tmp5_1" },
				{ "literal23" },
				{ "castedExpr_calculate23" },
				{ "splitted_expr23" },
				{ "xb_6" },
				{ "literal24" },
				{ "castedExpr_calculate24" }, // 120
				{ "splitted_expr24" }, { "inlined_tmp6_1" }, { "literal25" },
				{ "literal26" }, { "castedExpr_calculate25" },
				{ "splitted_expr25" }, { "castedExpr_calculate26" },
				{ "splitted_expr26" },
				{ "x6_4" },
				{ "x2_5" }, // 130
				{ "x6_5" }, { "out0_1" }, { "out1_1" }, { "out2_1" },
				{ "out3_1" }, { "out4_1" }, { "out5_1" }, { "out6_1" },
				{ "out7_1" } };

	}

	public static void main(String[] args) {
		// Construct the TestBench
		TestBench tB = new TestBench(testFDCT.T, testFDCT.NC, testFDCT.MC,
				testFDCT.MODE, testFDCT.StageTime);
		tB.setData(testFDCT.OpT, testFDCT.IC, testFDCT.OC, testFDCT.OpNN,
				testFDCT.VaNN, testFDCT.VaWW, testFDCT.chFF, testFDCT.chHH);

		// Run Pipeline
		String logPath = System.getProperty("user.home") + File.separator
				+ "Pipeline_W.txt";

		PipeliningOptimization pipe = new PipeliningOptimization(tB, logPath);
		pipe.run();
	}
}
