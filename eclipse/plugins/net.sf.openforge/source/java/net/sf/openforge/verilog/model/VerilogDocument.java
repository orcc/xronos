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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * VerilogDocument is used to construct Verilog HDL files.
 * <P>
 * Created: Fri Jan 26 2001
 * 
 * @author abk
 * @version $Id: VerilogDocument.java 2 2005-06-09 20:00:48Z imiller $
 */
public class VerilogDocument {

	List<Object> elements = new ArrayList<Object>();

	public VerilogDocument() {
	} // VerilogDocument()

	/**
	 * Appends a verilog module to the end of the document.
	 */
	public void append(Module module) {
		elements.add(module);
	} // append()

	/**
	 * Appends a verilog comment to the end of the document.
	 */
	public void append(Comment comment) {
		elements.add(comment);
	} // append()

	/**
	 * Appends a compiler directive to the end of this document.
	 */
	public void append(Directive d) {
		elements.add(d);
	}

	public Collection<Object> elements() {
		return elements;
	}

	/**
	 * Writes the document out to a stream. The stream supplied as input is not
	 * closed in case the caller wants to add additional information.
	 * <P>
	 * <strong>Warning</Strong><BR>
	 * Currently, this routine relies on the poorly formatted and inefficient
	 * toString() to generate and then print the text of the elements to the
	 * outputstream. A better mechanism would iterate over the tokens provided
	 * by each Lexicality, applying formatting rules along the way.
	 */
	public void write(OutputStream os) {
		PrintWriter printer = new PrintWriter(os);

		for (Object object : elements) {
			printer.print(object.toString());
		}

		printer.flush();
	} // write()

	/**
	 * Writes the document out to a Writer. The Writer supplied as input is not
	 * closed in case the caller wants to add additional information.
	 * <P>
	 * <strong>Warning</Strong><BR>
	 * Currently, this routine relies on the poorly formatted and inefficient
	 * toString() to generate and then print the text of the elements to the
	 * outputstream. A better mechanism would iterate over the tokens provided
	 * by each Lexicality, applying formatting rules along the way.
	 */
	public void write(Writer out) {
		PrintWriter printer = new PrintWriter(out);

		for (Object object : elements) {
			printer.print(object.toString());
		}

		printer.flush();
	} // write()

} // end of class VerilogDocument
