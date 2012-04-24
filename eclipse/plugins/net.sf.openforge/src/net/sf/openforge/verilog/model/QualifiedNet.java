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
import java.util.Iterator;
import java.util.List;

/**
 * QualifiedNet is a fully qualfied Net expression, with explicit Module and
 * ModuleInstance components in the name.
 * <P>
 * Example...<BR>
 * foo.bar.RESULT
 * <P>
 * Where foo is a Module identifier, bar is a ModuleInstance identifier, and
 * RESULT is a wire name.
 * <P>
 * Created: Thu Feb 08 2001
 * 
 * @author abk
 * @version $Id: QualifiedNet.java 2 2005-06-09 20:00:48Z imiller $
 */
public class QualifiedNet extends Net {

	List qualified_identifiers = new ArrayList();

	/**
	 * Constructs a QualifiedNet.
	 */
	public QualifiedNet(Module module, ModuleInstance[] instances, Net net) {
		super(net.getType(), net.getIdentifier(), net.getMSB(), net.getLSB());

		qualified_identifiers.add(module.getIdentifier());

		for (int i = 0; i < instances.length; i++) {
			qualified_identifiers.add(instances[i].getIdentifier());
		}

		qualified_identifiers.add(net.getIdentifier());

	} // QualifiedNet()

	public QualifiedNet(Module module, Net net) {
		this(module, new ModuleInstance[0], net);
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		for (Iterator it = qualified_identifiers.iterator(); it.hasNext();) {
			lex.append((Identifier) it.next());
			if (it.hasNext())
				lex.append(Symbol.DOT);
		}
		return lex;
	}

} // end of class QualifiedNet
