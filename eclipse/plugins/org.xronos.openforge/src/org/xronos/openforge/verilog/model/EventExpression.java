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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * EventExpression is a part of Forge
 * 
 * <P>
 * 
 * Created: Mon Mar 05 2001
 * 
 * @author abk
 * @version $Id: EventExpression.java 2 2005-06-09 20:00:48Z imiller $
 */
public class EventExpression implements Expression {

	List<Expression> events = new ArrayList<Expression>();

	public EventExpression(SimpleExpression event) {
		events.add(event);
	}

	/**
	 * Creates an event expression composed of one or more expressions,
	 * seperated by 'or'.
	 */
	public EventExpression(EventExpression[] events) {
		for (int i = 0; i < events.length; i++) {
			this.events.add(events[i]);
		}
	} // EventExpression()

	public void add(SimpleExpression event) {
		events.add(event);
	}

	@Override
	public int getWidth() {
		return 1;
	}

	@Override
	public Collection<Net> getNets() {
		Set<Net> nets = new HashSet<Net>();

		for (Expression expression : events) {
			nets.addAll(expression.getNets());
		}

		return nets;
	} // getNets()

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		for (Iterator<Expression> it = events.iterator(); it.hasNext();) {
			Expression e = it.next();

			lex.append(e);

			if (it.hasNext()) {
				lex.append(Keyword.OR);
			}
		}

		return lex;
	}

	@Override
	public String toString() {
		return lexicalify().toString();
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class NegEdge extends EventExpression {
		public NegEdge(SimpleExpression e) {
			super(e);
		}

		@Override
		public Lexicality lexicalify() {
			Lexicality lex = super.lexicalify();

			lex.prepend(Keyword.NEGEDGE);

			return lex;

		} // lexicalify()

	} // end of class NegEdge

	public static final class PosEdge extends EventExpression {
		public PosEdge(SimpleExpression e) {
			super(e);
		}

		@Override
		public Lexicality lexicalify() {
			Lexicality lex = super.lexicalify();

			lex.prepend(Keyword.POSEDGE);

			return lex;

		} // lexicalify()

	} // end of class PosEdge

} // end of class EventExpression
