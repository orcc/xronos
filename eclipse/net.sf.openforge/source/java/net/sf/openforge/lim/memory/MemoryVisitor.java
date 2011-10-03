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

/**
 * MemoryVisitor is a visitor interface that is used to traverse
 * {@link LogicalMemory LogicalMemories} and their contents.  By
 * traversing the LogicalMemory, {@link Allocation Allocations}, and
 * all implementations of {@link LogicalValue} this visitor can be
 * used to visit all classes responsible for the physical contents of
 * any memory.  The other types of {@link Location Locations} are not
 * necessary in this visitor as they do not define actual contents of
 * memory but rather they define the nature of accesses to the
 * memory.
 *
 * <p>Created: Tue Sep 16 15:24:07 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface MemoryVisitor 
{
    static final String _RCS_ = "$Rev: 2 $";

    void visit (LogicalMemory mem);
    void visit (Allocation alloc);
    void visit (Pointer ptr);
    void visit (Record rec);
    void visit (Scalar sclr);
    void visit (Slice slice);
    
}// MemoryVisitor
