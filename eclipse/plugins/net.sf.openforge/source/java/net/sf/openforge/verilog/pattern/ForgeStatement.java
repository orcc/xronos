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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.verilog.model.Statement;

/**
 * A ForgeStatement is a wrapper for any verilog statement. It provides support
 * for the ForgePattern interface.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: ForgeStatement.java 2 2005-06-09 20:00:48Z imiller $
 */

public class ForgeStatement extends StatementBlock implements ForgePattern {

	Set produced_nets;

	public ForgeStatement() {
		super();
		this.produced_nets = new HashSet();
	}

	/**
	 * Construct a ForgeStatement based on a Set of produced nets and a
	 * Statement.
	 * 
	 * @param produced_nets
	 *            the Set of Nets which are produced by the statement
	 * @param s
	 *            the generic statement
	 */
	public ForgeStatement(Set produced_nets, Statement s) {
		this.produced_nets = produced_nets;

		add(s);
	}

	public Collection getConsumedNets() {
		Collection consumed_nets = getNets();
		consumed_nets.removeAll(getProducedNets());
		return consumed_nets;
	}

	public Collection getProducedNets() {
		return produced_nets;
	}

} // class ForgeStatement
