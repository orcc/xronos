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
package net.sf.openforge.forge.library.ops;


/**
 * DIV.java
 *
 *
 * <p>Created: Thu Aug 29 09:26:40 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DIV.java 2 2005-06-09 20:00:48Z imiller $
 */

public class DIV{
     
    /**
     * Implements a functionally correct signed divide according to the
     * JVM specification.  The only deviation is in the handling of divide
     * by 0.  No exception is thrown in that case, instead the result of
     * -1 is returned.  When forging this code constant propagation must be
     * turned on and, optionally, loop unrolling.  If loop unrolling is turned
     * on, then this method may be balance pipeline scheduled.
     *
     * @param a a value of type 'int'
     * @param b a value of type 'int'
     * @return a value of type 'int'
     */
    public static int div(int num, int den)
    {
        int result = 0;
        
        // If true, then the result must be negative.
        boolean flipResult = false;
        
        if (num < 0)
        {
            num = -num;
            flipResult = !flipResult;
        }
        
        if (den < 0)
        {
            den = -den;
            flipResult = !flipResult;
        }
        
        int remainder = num;
        
        // Cast the denominator to a long so that MIN_INT looks like
        // a positive number.(We need 33 bits to represent -(MIN_INT)).
        long denom = den & 0xFFFFFFFFL;
        
        int mask = 0x80000000;
        
        for (int i=0; i < 32; i++)
        {
            // Cast the numerator to a long so that -(MIN_INT) appears as
            // a positive value (we need 33 bits to represent it).
            long numer = (remainder >>> (31 - i)) & 0xFFFFFFFFL;
            
            if (numer >= denom)
            {
                result |= mask;
                
                remainder = (remainder - (den << (31 - i)));
            }
            
            mask = mask >>> 1;
        }
        
        // If the signs of the inputs did not agree, then make the result negative.
        if (flipResult)
        {
            result = -result;
        }
        
        return result;
    }
    
    /**
     * A fully functional impelentation of a 'long' (64 bit) signed divider.
     * This implementation conforms to the JVM specification for long division
     * except for divide by 0, in which case the result -1 is returned.  
     *
     * @param a a 'long'
     * @param b a 'long'
     * @return a 'long'
     */
    public static long div(long num, long den)
    {
        long result = 0;
        
        // If true, the result is to be negative.
        boolean flipResult = false;
        
        if (num < 0)
        {
            num = -num;
            flipResult = !flipResult;
        }
        
        if (den < 0)
        {
            den = -den;
            flipResult = !flipResult;
        }
        
        long remainder = num;
        
        // Break the denominator into 2 halves for comparison.  This allows us
        // to represent the value -(MIN_LONG) as a positive value and get it
        // to compare correctly.
        long upperDen = den >>> 32;
        long lowerDen = den & 0x00000000FFFFFFFFL;
        
        long mask = 0x8000000000000000L;
        
        for (int i=0; i < 64; i++)
        {
            long numer = remainder >>> (63 - i);
            
            // Split the numerator into 2 halves for comparison.  This allows us
            // to represent the value -(MIN_LONG) as a positive value.
            long upperNum = numer >>> 32;
            long lowerNum = numer & 0x00000000FFFFFFFFL;
            
            
            if ((upperNum > upperDen) ||
                ((upperNum == upperDen) && (lowerNum >= lowerDen)))
            {
                result |= mask;
                
                remainder = (remainder - (den << (63 - i)));
            }
            
            mask = mask >>> 1;
        }
        
        // If the signs do not agree then negate the result.
        if (flipResult)
        {
            result = -result;
        }
        
        return result;
    }
    

}// DIV
