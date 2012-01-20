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
/* $Rev: 2 $ */

package net.sf.openforge.forge.api.ucf;

/**
 * The <code>UCFAttribute</code> interface marks classes which
 * can provide a legal UCF attribute. The text which the class
 * returns from its toString() procedure is expected to be a
 * snippet of UCF grammar which can be appended directly after
 * the quoted name of a NET and before the terminating semicolon.
 * <P>
 * As an example:
 * <PRE>
 * NET "i<*" IOSTANDARD=LVTTL;
 * </PRE>
 * In this example the text "IOSTANDARD=LVTTL" is considered
 * the UCFAttribute.
 */
public interface UCFAttribute
{
    /**
     * Gets the bit position which this attribute affects.
     * If the attribute affects all bits, this should
     * return a negative number.
     */
    public int getBit();
    
    /**
     * Expected to produce the text of a legitimate UCF attribute.
     */
    public String toString();
}
