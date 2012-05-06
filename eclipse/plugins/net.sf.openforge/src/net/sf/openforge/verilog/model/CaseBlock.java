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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * CaseBlock is a chunk of case statements bounded by case and endcase.
 * <P>
 * Example:<BR>
 * <CODE>
 * case(case_number)<BR>
 *   2'b00 : out = in1;<BR>
 *   2'b01 : out = in2;<BR>
 *   2'b10 : out = in3;<BR>
 *   2'b11 : out = in4;<BR>
 * endcase<BR>
 * </CODE>
 * <P>
 * Created: Fri June 21 2002
 * 
 * @author cwu
 * @version $Id: CaseBlock.java 2 2005-06-09 20:00:48Z imiller $
 */
public class CaseBlock implements Statement {

	private Expression caseControl;

	private Group controlGroup;

	private List<CaseStatement> body = new ArrayList<CaseStatement>();

	public CaseBlock(Expression caseControl) {
		this.caseControl = caseControl;
		controlGroup = new Group(this.caseControl);
	} // CaseBlock ()

	public void add(String caseString, Statement action) {
		body.add(new CaseStatement(new CaseValue(caseString), action));
	}

	@Override
	public Collection<Net> getNets() {
		HashSet<Net> nets = new HashSet<Net>();

		for (CaseStatement caseStatement : body) {
			nets.addAll(caseStatement.getNets());
		}

		return nets;
	} // getNets()

	public Expression getCaseControl() {
		return caseControl;
	}

	public Group getControlGroup() {
		return controlGroup;
	}

	public Collection<CaseStatement> getCaseBody() {
		return body;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Keyword.CASE);
		lex.append(controlGroup);

		for (CaseStatement statement : body) {
			lex.append(statement);
		}
		lex.append(Keyword.ENDCASE);

		return lex;

	} // lexicalify()

	@Override
	public String toString() {
		return lexicalify().toString();
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//
	public static final class CaseStatement implements Statement {
		private CaseValue caseValue;
		private Statement actionStatement;

		public CaseStatement(CaseValue caseValue, Statement actionStatement) {
			this.caseValue = caseValue;
			this.actionStatement = actionStatement;
		}

		@Override
		public Collection<Net> getNets() {
			return actionStatement.getNets();
		}

		public Statement getActionStatement() {
			return actionStatement;
		}

		public String getCaseValue() {
			return caseValue.getToken();
		}

		@Override
		public Lexicality lexicalify() {
			Lexicality lex = new Lexicality();

			lex.append(caseValue);
			lex.append(Symbol.COLON);
			lex.append(actionStatement);

			return lex;
		} // lexicalify ()

		@Override
		public String toString() {
			return lexicalify().toString();
		}

	} // end of inner class CaseStatement

	public static final class CaseValue extends Token {
		private String id;

		public CaseValue(String caseID) {
			id = caseID.trim();
		}

		// ////////////////////////////
		// VerilogElement interface
		@Override
		public String getToken() {
			return id;
		}
	} // end of inner class CaseValue

} // end of class CaseBlock
