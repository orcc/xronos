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

package net.sf.openforge.lim.memory;

import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.io.BlockElement;

/**
 * LValue is implemented by LIM objects which can represent an <i>lvalue</i>. An
 * lvalue, short for "left hand value," is any expression which can appear on
 * the left hand side of an assignment statement. Such an expression designates
 * a region of memory.
 * <P>
 * This region of memory may be either read from or written to, depending on the
 * context in which the lvalue is used. If it is used as the left hand side of
 * an assignment, it is used to write memory. In every other context it
 * represents a read from memory.
 * <P>
 * An LValue has a size, which is the number of addressable locations in the
 * memory region that it denotes. This amount of data transferred when the
 * LValue is read or written is determined by the number of addressable
 * locations accessed and the stride length for the target location in memory.
 * <P>
 * For instance, in the following code <code>
 *   <pre>
 *   i = 5;
 *   f(i);
 *   </pre>
 *   </code> If <code>i</code> is an integer variable, in the first line
 * <code>i</code> is an lvalue that represents a write to the region of memory
 * named by <code>i</code>, and in the second line it is a read from that same
 * region of memory.
 * <P>
 * An lvalue need not be a simple identifier. Here are some other examples of
 * lvalue expressions: <code>
 * <ul>
 * <li>*p   // pointer indirection
 * <li>a[i] // array subscript
 * <li>(l)  // parenthesized lvalue
 * <li>s.f  // direct struct access
 * <li>s->f // indirect struct access
 * </ul>
 * </code> The thing they all have in common is that they designate a region of
 * memory.
 * 
 * @version $Id: LValue.java 70 2005-12-01 17:43:11Z imiller $
 * @see LogicalMemory
 */
public interface LValue extends Visitable {
	/** Revision */
	static final String _RCS_ = "$Rev: 70 $";

	/**
	 * Gets the size of this LValue. This is the number of addressable locations
	 * read from or written to memory by the lvalue expression.
	 * 
	 * @return the number of addressable units in memory denoted by this lvalue;
	 *         this number is non-negative
	 */
	public int getAccessLocationCount();

	/**
	 * Tests whether the context of the LValue is a write or a read.
	 * 
	 * @return true if this lvalue is used in an assignment to memory, false if
	 *         it is used to read from memory
	 */
	public boolean isWrite();

	/**
	 * Sets the specific {@link BlockElement} that this LValue is associated
	 * with, and further indicates that this LValue is part of the interface
	 * wrapper code.
	 * 
	 * @param element
	 *            a BlockElement
	 */
	public void setBlockElement(BlockElement element);

	/**
	 * Retrieves the {@link BlockElement} associated with this LValue or null if
	 * none has been defined. A non null value means that this LValue is part of
	 * the interface wrapper logic for the design.
	 * 
	 * @return a BlockElement, may be null
	 */
	public BlockElement getBlockElement();

	/**
	 * Gets the {@link LogicalMemoryPort} that is referenced by this
	 * <code>LValue</code>.
	 * 
	 * @return the referenced port, or null if there is none
	 */
	public LogicalMemoryPort getLogicalMemoryPort();

	/**
	 * Sets the {@link LogicalMemoryPort} that is referenced by this
	 * <code>LValue</code>.
	 * 
	 * @param the
	 *            referenced port, or null if there is none
	 */
	public void setLogicalMemoryPort(LogicalMemoryPort port);

}
