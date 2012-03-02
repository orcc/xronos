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
package net.sf.openforge.verilog.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Symbol provides all of the allowable Verilog symbols.
 * 
 * <P>
 * 
 * Created: Wed Feb 07 2001
 * 
 * @author abk
 * @version $Id: Symbol.java 280 2006-08-11 17:00:32Z imiller $
 */
public class Symbol extends Token {

	private String token;

	public static final int TYPE = 1;

	public static Symbol PLUS = new Symbol("+");
	public static Symbol MINUS = new Symbol("-");
	public static Symbol MULTIPLY = new Symbol("*");
	public static Symbol DIVIDE = new Symbol("/");
	public static Symbol MODULUS = new Symbol("%");

	public static Symbol LOGICAL_NEGATE = new Symbol("!");
	public static Symbol BANG = new Symbol("!");
	public static Symbol BITWISE_NEGATE = new Symbol("~");
	public static Symbol TILDE = new Symbol("~");
	public static Symbol AND = new Symbol("&");
	public static Symbol NAND = new Symbol("~&");
	public static Symbol XOR = new Symbol("^");
	public static Symbol XNOR = new Symbol("^~");
	public static Symbol OR = new Symbol("|");
	public static Symbol NOR = new Symbol("~|");
	public static Symbol LEFT_SHIFT = new Symbol("<<");
	public static Symbol RIGHT_SHIFT = new Symbol(">>");
	public static Symbol LT = new Symbol("<");
	public static Symbol LESS_THAN = new Symbol("<");
	public static Symbol LTEQ = new Symbol("<=");
	public static Symbol LESS_THAN_OR_EQUAL = new Symbol("<=");
	public static Symbol GT = new Symbol(">");
	public static Symbol GREATER_THAN = new Symbol(">");
	public static Symbol GTEQ = new Symbol(">=");
	public static Symbol GREATER_THAN_OR_EQUAL = new Symbol(">=");
	public static Symbol LOGICAL_EQUALITY = new Symbol("==");
	public static Symbol EQ = new Symbol("==");
	public static Symbol LOGICAL_INEQUALITY = new Symbol("!=");
	public static Symbol NEQ = new Symbol("!=");
	public static Symbol CASE_EQUALITY = new Symbol("===");
	public static Symbol CASE_INEQUALITY = new Symbol("!==");
	public static Symbol LOGICAL_AND = new Symbol("&&");
	public static Symbol LOGICAL_OR = new Symbol("||");
	public static Symbol CONDITION = new Symbol("?");
	public static Symbol CONDITION_ELSE = new Symbol(":");

	public static Symbol BLOCKING_ASSIGN = new Symbol("=");
	public static Symbol CONTINUOUS_ASSIGN = new Symbol("=");
	public static Symbol NONBLOCKING_ASSIGN = new Symbol("<=");

	public static Symbol OPEN_PARENTHESIS = new Symbol("(");
	public static Symbol CLOSE_PARENTHESIS = new Symbol(")");
	public static Symbol OPEN_CURLY = new Symbol("{");
	public static Symbol CLOSE_CURLY = new Symbol("}");
	public static Symbol OPEN_SQUARE = new Symbol("[");
	public static Symbol CLOSE_SQUARE = new Symbol("]");
	public static Symbol COMMA = new Symbol(",");
	public static Symbol SEMICOLON = new Symbol(";");
	public static Symbol COLON = new Symbol(":");
	public static Symbol DOT = new Symbol(".");
	public static Symbol PERIOD = DOT;

	public static Symbol SHORT_COMMENT = new Symbol("//");
	public static Symbol OPEN_COMMENT = new Symbol("/*");
	public static Symbol CONTINUE_COMMENT = new Symbol("*");
	public static Symbol CLOSE_COMMENT = new Symbol("*/");

	public static Symbol EVENT = new Symbol("@");
	public static Symbol DOLLAR = new Symbol("$");
	public static Symbol QUOTE = new Symbol("\"");
	public static Symbol BACKQUOTE = new Symbol("`");
	public static Symbol BACKTICK = BACKQUOTE;

	public static Symbol DELAY = new Symbol("#");
	public static Symbol PARAM_HASH = new Symbol("#");
	public static Symbol SENSITIVE_ALL = new Symbol("*");

	/** Number bases */
	public static Symbol DECIMAL_BASE = new Symbol("'d");
	public static Symbol BINARY_BASE = new Symbol("'b");
	public static Symbol OCTAL_BASE = new Symbol("'o");
	public static Symbol HEX_BASE = new Symbol("'h");

	private static Set<Symbol> all_words;
	private static Set<Symbol> operators;
	private static Set<Symbol> delimiters;

	static {
		operators = new HashSet<Symbol>();

		operators.add(PLUS);
		operators.add(MINUS);
		operators.add(MULTIPLY);
		operators.add(DIVIDE);
		operators.add(MODULUS);
		operators.add(LOGICAL_NEGATE);
		operators.add(BANG);
		operators.add(BITWISE_NEGATE);
		operators.add(TILDE);
		operators.add(AND);
		operators.add(NAND);
		operators.add(XOR);
		operators.add(XNOR);
		operators.add(OR);
		operators.add(NOR);
		operators.add(LEFT_SHIFT);
		operators.add(RIGHT_SHIFT);
		operators.add(LT);
		operators.add(LESS_THAN);
		operators.add(LTEQ);
		operators.add(LESS_THAN_OR_EQUAL);
		operators.add(GT);
		operators.add(GREATER_THAN);
		operators.add(GTEQ);
		operators.add(GREATER_THAN_OR_EQUAL);
		operators.add(LOGICAL_EQUALITY);
		operators.add(EQ);
		operators.add(LOGICAL_INEQUALITY);
		operators.add(NEQ);
		operators.add(CASE_EQUALITY);
		operators.add(CASE_INEQUALITY);
		operators.add(LOGICAL_AND);
		operators.add(LOGICAL_OR);
		operators.add(CONDITION);
		operators.add(CONDITION_ELSE);
		operators.add(BLOCKING_ASSIGN);
		operators.add(CONTINUOUS_ASSIGN);
		operators.add(NONBLOCKING_ASSIGN);

		delimiters = new HashSet<Symbol>();

		delimiters.add(OPEN_PARENTHESIS);
		delimiters.add(CLOSE_PARENTHESIS);
		delimiters.add(OPEN_SQUARE);
		delimiters.add(CLOSE_SQUARE);
		delimiters.add(COMMA);
		delimiters.add(SEMICOLON);
		delimiters.add(COLON);
		delimiters.add(DOT);
		delimiters.add(PERIOD);
		delimiters.add(DOLLAR);
		delimiters.add(QUOTE);
		delimiters.add(EVENT);
		delimiters.add(DELAY);
		delimiters.add(PARAM_HASH);
		delimiters.add(OPEN_CURLY);
		delimiters.add(CLOSE_CURLY);
		delimiters.add(BACKTICK);

		all_words = new HashSet<Symbol>();
		all_words.addAll(operators);
		all_words.addAll(delimiters);

		// and the base numbers
		all_words.add(DECIMAL_BASE);
		all_words.add(HEX_BASE);
		all_words.add(BINARY_BASE);
		all_words.add(OCTAL_BASE);
	}

	private Symbol(String token) {
		this.token = token;
	} // Symbol()

	// ////////////////////////////
	// VerilogElement interface

	public String getToken() {
		return token;
	}

	public int getType() {
		return TYPE;
	}

	// ////////////////////////////
	// public static methods

	public static boolean isSymbol(String token) {
		return all_words.contains(new Symbol(token));
	}

	public static boolean isOperator(String token) {
		return operators.contains(new Symbol(token));
	}

	public static boolean isDelimiter(String token) {
		return delimiters.contains(new Symbol(token));
	}

	public static boolean isSymbol(Token token) {
		return all_words.contains(token);
	}

	public static boolean isOperator(Token token) {
		return operators.contains(token);
	}

	public static boolean isDelimiter(Token token) {
		return delimiters.contains(token);
	}

} // end of class Symbol
