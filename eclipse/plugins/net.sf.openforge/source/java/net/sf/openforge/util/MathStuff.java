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

package net.sf.openforge.util;

/**
 * Various math utility methods.
 *
 * @version $Id: MathStuff.java 2 2005-06-09 20:00:48Z imiller $
 */
public class MathStuff
{
    /** Natural log of 2 */
    private static final double LN_2 = java.lang.Math.log(2);

    /**
     * Two, four, six, eight, please do not instantiate.
     */
    private MathStuff ()
    {
    }

    /**
     * Computes the log-base-2 of a an integer.
     * 
     * @param i the operand for the log operation
     * @return log base 2 of the input; 0 for all values <= 0
     */
    public static int log2 (int i)
    {
        return (i == 0) ? 0 : (int)java.lang.Math.ceil(java.lang.Math.log(i)/LN_2);
    }
}
