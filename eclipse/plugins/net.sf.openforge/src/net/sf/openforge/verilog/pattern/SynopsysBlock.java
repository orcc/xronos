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
package net.sf.openforge.verilog.pattern;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sf.openforge.verilog.model.Lexicality;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Statement;
import net.sf.openforge.verilog.model.Token;

/**
 * SynopsysBlock represents a special form verilog comment which uses synopsys
 * directives to turn off translation at the beginning of the block, and turn
 * translation back on at the end. In between the directives, and arbitrary text
 * may appear, including standard verilog statements.
 * 
 * <P>
 * 
 * Created: Mon Feb 12 2001
 * 
 * @author abk
 * @version $Id: SynopsysBlock.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SynopsysBlock extends Token implements ForgePattern {

	List<String> body = new ArrayList<String>();

	public static final int TYPE = 6;

	public SynopsysBlock() {
	}

	public void append(String s) {
		body.add(s);
	}

	public void append(Statement s) {
		body.add(s.toString());
	}

	@Override
	public String getToken() {
		StringWriter sw = new StringWriter();
		PrintWriter printer = new PrintWriter(sw);
		printer.println("// synopsys translate_off");
		for (String str : body) {
			printer.println(str);
		}
		printer.println("// synopsys translate_on");

		try {
			printer.close();
			sw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return sw.toString();
	}

	@Override
	public int getType() {
		return TYPE;
	}

	@Override
	public Collection<Net> getNets() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Net> getConsumedNets() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Net> getProducedNets() {
		return Collections.emptyList();
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		lex.append(this);
		return lex;
	}

} // class SynopsysBlock
