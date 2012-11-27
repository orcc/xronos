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

package org.xronos.openforge.verilog.translate;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;

import org.xronos.openforge.util.IndentWriter;
import org.xronos.openforge.verilog.model.Constant;
import org.xronos.openforge.verilog.model.Control;
import org.xronos.openforge.verilog.model.Identifier;
import org.xronos.openforge.verilog.model.InlineComment;
import org.xronos.openforge.verilog.model.Keyword;
import org.xronos.openforge.verilog.model.Lexicality;
import org.xronos.openforge.verilog.model.StringStatement;
import org.xronos.openforge.verilog.model.Symbol;
import org.xronos.openforge.verilog.model.Token;
import org.xronos.openforge.verilog.model.VerilogDocument;
import org.xronos.openforge.verilog.model.VerilogElement;


/**
 * PrettyPrinter applies formatting rules to a stream of Verilog tokens to
 * produce pretty-printed verilog output.
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 *         <p>
 *         created July 31, 2002
 * @version $Id: PrettyPrinter.java 2 2005-06-09 20:00:48Z imiller $
 */
public class PrettyPrinter {

	IndentWriter writer;

	Iterator<Object> tokens = null;

	/**
	 * Constructs a new PrettyPrinter.
	 * 
	 * @param os
	 *            the OutputStream to which output should be sent
	 */
	public PrettyPrinter(OutputStream os) {
		writer = new IndentWriter(os);
	}

	/**
	 * Constructs a new PrettyPrinter.
	 * 
	 * @param w
	 *            the Writer to which output should be sent
	 */
	public PrettyPrinter(Writer w) {
		writer = new IndentWriter(w);
	}

	/**
	 * Pretty prints the {@link VerilogDocument}.
	 * 
	 * @param doc
	 *            the VerilogDocument to be printed
	 */
	public void print(VerilogDocument doc) {
		for (Iterator<Object> it = doc.elements().iterator(); it.hasNext();) {
			Object element = it.next();

			if (element instanceof VerilogElement) {
				pretty((VerilogElement) element);
			} else {
				print(element.toString());
			}
		}
		writer.flush();
	}

	/**
	 * Pretty prints the {@link VerilogElement}.
	 * 
	 * @param element
	 *            the VerilogElement to be printed
	 */
	public void pretty(VerilogElement element) {
		Lexicality lex = element.lexicalify();

		tokens = lex.iterator();

		while (tokens.hasNext()) {
			Token t = (Token) tokens.next();
			pretty(t);
		}
		writer.flush();
	}

	/**
	 * Prints an arbitrary string to the output.
	 * 
	 * @param s
	 *            the string to output
	 */
	public void print(String s) {
		writer.print(s);
	}

	/**
	 * Prints an arbitrary string to the output, followed with a newline.
	 * 
	 * @param s
	 *            the string to output
	 */
	public void println(String s) {
		writer.println(s);
	}

	/** The most recently printed Token. */
	private Token previous = Control.NEWLINE;

	/**
	 * Prints a token to the output.
	 */
	public void print(Token t) {
		print(t.toString());
		previous = t;
	}

	/**
	 * Pretties a run of tokens until a particular token is encountered.
	 */
	private void prettyUntil(Token last) {
		while (tokens.hasNext()) {
			Token t = (Token) tokens.next();
			pretty(t);
			if (t.equals(last))
				break;
		}

	}

	/**
	 * Pretty-prints the token.
	 * 
	 */
	private void pretty(Token t) {
		switch (t.getType()) {
		case Constant.TYPE:
			pretty((Constant) t);
			break;
		case Control.TYPE:
			pretty((Control) t);
			break;
		case Identifier.TYPE:
			pretty((Identifier) t);
			break;
		case InlineComment.TYPE:
			print(t.toString());
			previous = Control.NEWLINE;
			break;
		case Keyword.TYPE:
			pretty((Keyword) t);
			break;
		case Symbol.TYPE:
			pretty((Symbol) t);
			break;
		default:
			print(t.toString());
		}
	}

	private void pretty(Control control) {
		previous = control;
		if (control.equals(Control.WHITESPACE)) {
			print(" ");
		} else if (control.equals(Control.NEWLINE)) {
			print(control.toString());
		} else if (control.equals(Control.TAB)) {
			print("\t");
		} else if (control.equals(Control.INDENT)) {
			writer.inc();
		} else if (control.equals(Control.OUTDENT)) {
			writer.dec();
		} else if (control.equals(Control.SETDENT)) {
		}
	}

	private void pretty(Keyword keyword) {
		delimit();
		if (keyword.equals(Keyword.MODULE)) {
			pretty(Control.NEWLINE);
		}
		print(keyword);
		if (Keyword.isWireword(keyword)) {
			formatWireDeclaration();
		} else if (keyword.equals(Keyword.ENDMODULE)) {
			pretty(Control.NEWLINE);
			pretty(Control.NEWLINE);
		} else if (keyword.equals(Keyword.BEGIN)) {
			pretty(Control.NEWLINE);
		} else if (keyword.equals(Keyword.END)) {
			pretty(Control.NEWLINE);
		} else if (keyword.equals(Keyword.ALWAYS) || keyword.equals(Keyword.IF)) {
			pretty(Control.WHITESPACE);
			while (tokens.hasNext()) {
				Token t = (Token) tokens.next();
				pretty(t);
				if (t.equals(Symbol.CLOSE_PARENTHESIS)) {
					pretty(Control.NEWLINE);
					break;
				}
			}
		} else {
			pretty(Control.WHITESPACE);
		}
	}

	/**
	 * A "wire" keyword was just processed, so expect a series of tokens
	 * representing a wire declaration. Two possible forms...
	 * <P>
	 * <CODE>
	 * wire [31:0] arg0;<BR>
	 * wire        arg1;
	 * </CODE>
	 */
	private void formatWireDeclaration() {
		pretty(Control.TAB);
		Token id_or_range = (Token) tokens.next();
		if (id_or_range.getType() == Identifier.TYPE) {
			pretty(Control.TAB);
			pretty(id_or_range);
		} else {
			pretty(id_or_range);
			prettyUntil(Symbol.CLOSE_SQUARE);
			pretty(Control.TAB);
		}
	}

	private void pretty(Constant constant) {
		print(constant);
	}

	private void pretty(Identifier identifier) {
		delimit();
		print(identifier);
	}

	@SuppressWarnings("unused")
	private void pretty(StringStatement.StringToken string) {
		delimit();
		print(string);
	}

	private void pretty(Symbol symbol) {
		print(symbol);
		if (symbol.equals(Symbol.SEMICOLON)) {
			pretty(Control.NEWLINE);
		} else if (symbol.equals(Symbol.COMMA)) {
			pretty(Control.WHITESPACE);
		}
	}

	private void delimit() {
		if (!Control.isWhitespace(previous) && !Symbol.isDelimiter(previous)
				&& !Symbol.isOperator(previous)
				&& !(previous.getType() == Constant.TYPE)) {
			pretty(Control.WHITESPACE);
		}
	}
}
