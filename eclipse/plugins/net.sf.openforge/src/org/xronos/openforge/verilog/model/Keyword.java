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
package org.xronos.openforge.verilog.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Keyword provides all the named keywords in Verilog.
 * 
 * <P>
 * 
 * Created: Tue Jan 30 2001
 * 
 * @author abk
 * @version $Id: Keyword.java 2 2005-06-09 20:00:48Z imiller $
 */

public class Keyword extends Token {

	private String token;

	public static final int TYPE = 2;

	public static Keyword ALWAYS = new Keyword("always");
	public static Keyword AND = new Keyword("and");
	public static Keyword ASSIGN = new Keyword("assign");
	public static Keyword AUTOMATIC = new Keyword_2001("automatic");
	public static Keyword BEGIN = new Keyword("begin");
	public static Keyword BUF = new Keyword("buf");
	public static Keyword BUFIF0 = new Keyword("bufif0");
	public static Keyword BUFIF1 = new Keyword("bufif1");
	public static Keyword CASE = new Keyword("case");
	public static Keyword CASEX = new Keyword("casex");
	public static Keyword CASEZ = new Keyword("casez");
	public static Keyword CELL = new Keyword_2001("cell");
	public static Keyword CMOS = new Keyword("cmos");
	public static Keyword CONFIG = new Keyword_2001("config");
	public static Keyword DEASSIGN = new Keyword("deassign");
	public static Keyword DEFAULT = new Keyword("default");
	public static Keyword DEFPARAM = new Keyword("defparam");
	public static Keyword DESIGN = new Keyword_2001("design");
	public static Keyword DISABLE = new Keyword("disable");
	public static Keyword EDGE = new Keyword("edge");
	public static Keyword ELSE = new Keyword("else");
	public static Keyword END = new Keyword("end");
	public static Keyword ENDCASE = new Keyword("endcase");
	public static Keyword ENDCONFIG = new Keyword_2001("endconfig");
	public static Keyword ENDFUNCTION = new Keyword("endfunction");
	public static Keyword ENDGENERATE = new Keyword_2001("endgenerate");
	public static Keyword ENDMODULE = new Keyword("endmodule");
	public static Keyword ENDPRIMITIVE = new Keyword("endprimitive");
	public static Keyword ENDSPECIFY = new Keyword("endspecify");
	public static Keyword ENDTABLE = new Keyword("endtable");
	public static Keyword ENDTASK = new Keyword("endtask");
	public static Keyword EVENT = new Keyword("event");
	public static Keyword FOR = new Keyword("for");
	public static Keyword FORCE = new Keyword("force");
	public static Keyword FOREVER = new Keyword("forever");
	public static Keyword FORK = new Keyword_2001("fork");
	public static Keyword FUNCTION = new Keyword("function");
	public static Keyword GENERATE = new Keyword_2001("generate");
	public static Keyword GENVAR = new Keyword_2001("genvar");
	public static Keyword HIGHZ0 = new Keyword("highz0");
	public static Keyword HIGHZ1 = new Keyword("highz1");
	public static Keyword IF = new Keyword("if");
	public static Keyword IFNONE = new Keyword("ifnone");
	public static Keyword INCDIR = new Keyword_2001("incdir");
	public static Keyword INCLUDE_2001 = new Keyword_2001("include");
	public static Keyword INITIAL = new Keyword("initial");
	public static Keyword INOUT = new Keyword("inout");
	public static Keyword INPUT = new Keyword("input");
	public static Keyword INSTANCE = new Keyword_2001("instance");
	public static Keyword INTEGER = new Keyword("integer");
	public static Keyword JOIN = new Keyword("join");
	public static Keyword LARGE = new Keyword("large");
	public static Keyword LIBLIST = new Keyword_2001("liblist");
	public static Keyword LIBRARY = new Keyword_2001("library");
	public static Keyword LOCALPARAM = new Keyword_2001("localparam");
	public static Keyword MACROMODULE = new Keyword("macromodule");
	public static Keyword MEDIUM = new Keyword("medium");
	public static Keyword MODULE = new Keyword("module");
	public static Keyword NAND = new Keyword("nand");
	public static Keyword NEGEDGE = new Keyword("negedge");
	public static Keyword NMOS = new Keyword("nmos");
	public static Keyword NOR = new Keyword("nor");
	public static Keyword NOSHOWCANCELLED = new Keyword_2001("noshowcancelled");
	public static Keyword NOT = new Keyword("not");
	public static Keyword NOTIF0 = new Keyword("notif0");
	public static Keyword NOTIF1 = new Keyword("notif1");
	public static Keyword OR = new Keyword("or");
	public static Keyword OUTPUT = new Keyword("output");
	public static Keyword PARAMETER = new Keyword("parameter");
	public static Keyword PMOS = new Keyword("pmos");
	public static Keyword POSEDGE = new Keyword("posedge");
	public static Keyword PRIMITIVE = new Keyword("primitive");
	public static Keyword PULL0 = new Keyword("pull0");
	public static Keyword PULL1 = new Keyword("pull1");
	public static Keyword PULLDOWN = new Keyword("pulldown");
	public static Keyword PULLUP = new Keyword("pullup");
	public static Keyword PULSESTYLE_ONEVENT = new Keyword_2001(
			"pulsestyle_onevent");
	public static Keyword PULSESTYLE_ONDETECT = new Keyword_2001(
			"pulsestyle_ondetect");
	public static Keyword RCMOS = new Keyword("rcmos");
	public static Keyword REAL = new Keyword("real");
	public static Keyword REALTIME = new Keyword("realtime");
	public static Keyword REG = new Keyword("reg");
	public static Keyword RELEASE = new Keyword("release");
	public static Keyword REPEAT = new Keyword("repeat");
	public static Keyword RNMOS = new Keyword("rnmos");
	public static Keyword RPMOS = new Keyword("rpmos");
	public static Keyword RTRAN = new Keyword("rtran");
	public static Keyword RTRANIF0 = new Keyword("rtranif0");
	public static Keyword RTRANIF1 = new Keyword("rtranif1");
	public static Keyword SCALARED = new Keyword("scalared");
	public static Keyword SHOWCANCELLED = new Keyword_2001("showcancelled");
	public static Keyword SIGNED = new Keyword_2001("signed");
	public static Keyword SMALL = new Keyword("small");
	public static Keyword SPECIFY = new Keyword("specify");
	public static Keyword SPECPARAM = new Keyword("specparam");
	public static Keyword STRONG0 = new Keyword("strong0");
	public static Keyword STRONG1 = new Keyword("strong1");
	public static Keyword SUPPLY0 = new Keyword("supply0");
	public static Keyword SUPPLY1 = new Keyword("supply1");
	public static Keyword TABLE = new Keyword("table");
	public static Keyword TASK = new Keyword("task");
	public static Keyword TIME = new Keyword("time");
	public static Keyword TRAN = new Keyword("tran");
	public static Keyword TRANIF0 = new Keyword("tranif0");
	public static Keyword TRANIF1 = new Keyword("tranif1");
	public static Keyword TRI = new Keyword("tri");
	public static Keyword TRI0 = new Keyword("tri0");
	public static Keyword TRI1 = new Keyword("tri1");
	public static Keyword TRIAND = new Keyword("triand");
	public static Keyword TRIOR = new Keyword("trior");
	public static Keyword TRIREG = new Keyword("trireg");
	public static Keyword UNSIGNED = new Keyword_2001("unsigned");
	public static Keyword USE = new Keyword_2001("use");
	public static Keyword VECTORED = new Keyword("vectored");
	public static Keyword WAIT = new Keyword("wait");
	public static Keyword WAND = new Keyword("wand");
	public static Keyword WEAK0 = new Keyword("weak0");
	public static Keyword WEAK1 = new Keyword("weak1");
	public static Keyword WHILE = new Keyword("while");
	public static Keyword WIRE = new Keyword("wire");
	public static Keyword WOR = new Keyword("wor");
	public static Keyword XNOR = new Keyword("xnor");
	public static Keyword XOR = new Keyword("xor");

	public static Keyword FWRITE = new Keyword("fwrite");
	public static Keyword FINISH = new Keyword("finish");
	public static Keyword FOPEN = new Keyword("fopen");
	public static Keyword DISPLAY = new Keyword("display");

	/** Standard compiler directives */
	public static Keyword DEFINE = new Keyword("`define");
	public static Keyword UNDEF = new Keyword("`undef");
	public static Keyword IFDEF = new Keyword("`ifdef");
	public static Keyword ELSE_DIRECTIVE = new Keyword("`else");
	public static Keyword ENDIF = new Keyword("`endif");
	public static Keyword DEFAULT_NETTYPE = new Keyword("`default_nettype");
	public static Keyword INCLUDE = new Keyword("`include");
	public static Keyword RESETALL = new Keyword("`resetall");
	public static Keyword TIMESCALE = new Keyword("`timescale");
	public static Keyword UNCONNECTED_DRIVE = new Keyword("`unconnected_drive");
	public static Keyword NOUNCONNECTED_DRIVE = new Keyword(
			"`nounconnected_drive");
	public static Keyword CELLDEFINE = new Keyword("`celldefine");
	public static Keyword ENDCELLDEFINE = new Keyword("`endcelldefine");

	/** timescale units */
	public static Keyword SECONDS = new Keyword("s");
	public static Keyword S = SECONDS;
	public static Keyword MILLISECONDS = new Keyword("ms");
	public static Keyword MS = MILLISECONDS;
	public static Keyword MICROSECONDS = new Keyword("us");
	public static Keyword US = MICROSECONDS;
	public static Keyword NANOSECONDS = new Keyword("ns");
	public static Keyword NS = NANOSECONDS;
	public static Keyword PICOSECONDS = new Keyword("ps");
	public static Keyword PS = PICOSECONDS;
	public static Keyword FEMTOSECONDS = new Keyword("fs");
	public static Keyword FS = FEMTOSECONDS;

	/** Composite 'keywords'. Needed to declare a signed wire */
	public static Keyword SIGNED_WIRE = new CompositeKeyword(WIRE, SIGNED);

	public static Set<Keyword> all_words;
	public static Set<Keyword> wire_words;
	public static Set<Keyword> directive_words;
	public static Set<Keyword> unit_words;
	public static Set<Keyword> f_words;
	public static Set<Keyword> v2001_words;

	static {
		all_words = new HashSet<Keyword>();

		all_words.add(ALWAYS);
		all_words.add(AND);
		all_words.add(ASSIGN);
		all_words.add(BEGIN);
		all_words.add(BUF);
		all_words.add(BUFIF0);
		all_words.add(BUFIF1);
		all_words.add(CASE);
		all_words.add(CASEX);
		all_words.add(CASEZ);
		all_words.add(CMOS);
		all_words.add(DEASSIGN);
		all_words.add(DEFAULT);
		all_words.add(DEFPARAM);
		all_words.add(DISABLE);
		all_words.add(EDGE);
		all_words.add(ELSE);
		all_words.add(END);
		all_words.add(ENDCASE);
		all_words.add(ENDFUNCTION);
		all_words.add(ENDMODULE);
		all_words.add(ENDPRIMITIVE);
		all_words.add(ENDSPECIFY);
		all_words.add(ENDTABLE);
		all_words.add(ENDTASK);
		all_words.add(EVENT);
		all_words.add(FOR);
		all_words.add(FORCE);
		all_words.add(FOREVER);
		all_words.add(FUNCTION);
		all_words.add(HIGHZ0);
		all_words.add(HIGHZ1);
		all_words.add(IF);
		all_words.add(IFNONE);
		all_words.add(INITIAL);
		all_words.add(INTEGER);
		all_words.add(JOIN);
		all_words.add(LARGE);
		all_words.add(MACROMODULE);
		all_words.add(MEDIUM);
		all_words.add(MODULE);
		all_words.add(NAND);
		all_words.add(NEGEDGE);
		all_words.add(NMOS);
		all_words.add(NOR);
		all_words.add(NOT);
		all_words.add(NOTIF0);
		all_words.add(NOTIF1);
		all_words.add(OR);
		all_words.add(PARAMETER);
		all_words.add(PMOS);
		all_words.add(POSEDGE);
		all_words.add(PRIMITIVE);
		all_words.add(PULL0);
		all_words.add(PULL1);
		all_words.add(PULLUP);
		all_words.add(PULLDOWN);
		all_words.add(RCMOS);
		all_words.add(REAL);
		all_words.add(REALTIME);
		all_words.add(RELEASE);
		all_words.add(REPEAT);
		all_words.add(RNMOS);
		all_words.add(RPMOS);
		all_words.add(RTRAN);
		all_words.add(RTRANIF0);
		all_words.add(RTRANIF1);
		all_words.add(SCALARED);
		all_words.add(SMALL);
		all_words.add(SPECIFY);
		all_words.add(SPECPARAM);
		all_words.add(STRONG0);
		all_words.add(STRONG1);
		all_words.add(SUPPLY0);
		all_words.add(SUPPLY1);
		all_words.add(TABLE);
		all_words.add(TASK);
		all_words.add(TIME);
		all_words.add(TRAN);
		all_words.add(TRANIF0);
		all_words.add(TRANIF1);
		all_words.add(TRI);
		all_words.add(TRI0);
		all_words.add(TRI1);
		all_words.add(TRIAND);
		all_words.add(TRIOR);
		all_words.add(TRIREG);
		all_words.add(VECTORED);
		all_words.add(WAIT);
		all_words.add(WAND);
		all_words.add(WEAK0);
		all_words.add(WEAK1);
		all_words.add(WHILE);
		all_words.add(WOR);
		all_words.add(XNOR);
		all_words.add(XOR);

		wire_words = new HashSet<Keyword>();
		wire_words.add(INOUT);
		wire_words.add(INPUT);
		wire_words.add(OUTPUT);
		wire_words.add(REG);
		wire_words.add(WIRE);
		wire_words.add(SIGNED_WIRE);
		all_words.addAll(wire_words);

		directive_words = new HashSet<Keyword>();
		directive_words.add(DEFINE);
		directive_words.add(UNDEF);
		directive_words.add(IFDEF);
		directive_words.add(ELSE_DIRECTIVE);
		directive_words.add(ENDIF);
		directive_words.add(DEFAULT_NETTYPE);
		directive_words.add(INCLUDE);
		directive_words.add(RESETALL);
		directive_words.add(TIMESCALE);
		directive_words.add(UNCONNECTED_DRIVE);
		directive_words.add(NOUNCONNECTED_DRIVE);
		directive_words.add(CELLDEFINE);
		directive_words.add(ENDCELLDEFINE);
		all_words.addAll(directive_words);

		unit_words = new HashSet<Keyword>();
		unit_words.add(SECONDS);
		unit_words.add(MILLISECONDS);
		unit_words.add(MICROSECONDS);
		unit_words.add(NANOSECONDS);
		unit_words.add(PICOSECONDS);
		unit_words.add(FEMTOSECONDS);
		all_words.addAll(unit_words);

		f_words = new HashSet<Keyword>();
		f_words.add(FWRITE);
		f_words.add(FOPEN);
		f_words.add(FINISH);
		f_words.add(DISPLAY);
		all_words.addAll(f_words);

		v2001_words = new HashSet<Keyword>();
		v2001_words.add(AUTOMATIC);
		v2001_words.add(CELL);
		v2001_words.add(CONFIG);
		v2001_words.add(DESIGN);
		v2001_words.add(ENDCONFIG);
		v2001_words.add(ENDGENERATE);
		v2001_words.add(FORK);
		v2001_words.add(GENERATE);
		v2001_words.add(GENVAR);
		v2001_words.add(INCDIR);
		v2001_words.add(INCLUDE_2001);
		v2001_words.add(INSTANCE);
		v2001_words.add(LIBLIST);
		v2001_words.add(LIBRARY);
		v2001_words.add(LOCALPARAM);
		v2001_words.add(NOSHOWCANCELLED);
		v2001_words.add(PULSESTYLE_ONEVENT);
		v2001_words.add(PULSESTYLE_ONDETECT);
		v2001_words.add(SHOWCANCELLED);
		v2001_words.add(SIGNED);
		v2001_words.add(UNSIGNED);
		v2001_words.add(USE);
		all_words.addAll(v2001_words);
	}

	private Keyword(String token) {
		this.token = token;
	}

	@Override
	public String getToken() {
		return token;
	}

	@Override
	public int getType() {
		return TYPE;
	}

	// ////////////////////////////
	// public static methods

	/**
	 * Tests the given String to see if it is a verilog Keyword.
	 */
	public static boolean isKeyword(String token) {
		return all_words.contains(new Keyword(token));
	}

	/**
	 * Tests the given Token to see if it is a verilog Keyword.
	 */
	public static boolean isKeyword(Token token) {
		return all_words.contains(token);
	}

	/**
	 * Tests the given String to see if it is a valid verilog Net type (wire,
	 * reg, input, etc).
	 */
	public static boolean isWireword(String token) {
		return isWireword(new Keyword(token));
	}

	/**
	 * Tests the given Token to see if it is a valid verilog Net type (wire,
	 * reg, input, etc).
	 */
	public static boolean isWireword(Token token) {
		return wire_words.contains(token);
	}

	/**
	 * Tests the given Token to see if it is a standard verilog compiler
	 * directive.
	 */
	public static boolean isDirectiveword(Token token) {
		return directive_words.contains(token);
	}

	/**
	 * Tests the given String to see if it is a standard verilog compiler
	 * directive.
	 */
	public static boolean isDirectiveword(String token) {
		return isWireword(new Keyword(token));
	}

	/**
	 * Tests the given String to see if it is a recognized time unit designation
	 * which may be used with the timescale directive.
	 */
	public static boolean isUnitword(String token) {
		return isUnitword(new Keyword(token));
	}

	/**
	 * Tests the given Token to see if it is a recognized time unit designation
	 * which may be used with the timescale directive.
	 */
	public static boolean isUnitword(Token token) {
		return unit_words.contains(token);
	}

	public static boolean isFword(String token) {
		return isFword(new Keyword(token));
	}

	public static boolean isFword(Token token) {
		return f_words.contains(token);
	}

	public static boolean isVerilog2001Word(Token token) {
		return v2001_words.contains(token);
	}

	public static boolean isVerilog2001Word(String token) {
		return v2001_words.contains(new Keyword_2001(token));
	}

	private static class Keyword_2001 extends Keyword {
		private Keyword_2001(String token) {
			super(token);
		}
	}

	private static class CompositeKeyword extends Keyword {
		public CompositeKeyword(Keyword first, Keyword second) {
			super(first.getToken() + " " + second.getToken());
		}
	}

} // end of class Keyword
