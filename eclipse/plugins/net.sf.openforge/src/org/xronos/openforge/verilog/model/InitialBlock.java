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

import java.util.Collection;

/**
 * InitialBlock is a Verilog 'initial' block.
 * 
 * <pre>
 * initial
 *   <statement>
 * </pre>
 * 
 * <p>
 * Created: Thu Aug 22 15:08:25 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: InitialBlock.java 2 2005-06-09 20:00:48Z imiller $
 */
public class InitialBlock implements Statement {

	private SequentialBlock block = new SequentialBlock();

	public InitialBlock() {
	}

	public InitialBlock(Statement statement) {
		add(statement);
	}

	public void add(Statement statement) {
		block.add(statement);
	}

	@Override
	public Collection<Net> getNets() {
		return block.getNets();
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Keyword.INITIAL);
		lex.append(block);
		return lex;

	} // lexicalify()

	@Override
	public String toString() {
		return lexicalify().toString();
	}

}// InitialBlock
