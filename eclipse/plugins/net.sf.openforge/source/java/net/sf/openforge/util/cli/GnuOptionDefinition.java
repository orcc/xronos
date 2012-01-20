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

package net.sf.openforge.util.cli;

/**
 * Defines a Gnu-style command line option. 
 * 
 * @author Andreas Kollegger
 */
public class GnuOptionDefinition 
{
    /** The option does not take an argument. */
    public final static int NO_ARGUMENT = 0x01;
    
    /** The option may have an argument. */
    public final static int OPTIONAL_ARGUMENT = 0x02;
    
    /** The option must have an argument. */
    public final static int REQUIRED_ARGUMENT = 0x04;
    
    /** Single character alphanumeric key **/
    private final char shortKey;
    
    /** Long string key. **/
    private final String longKey;
    
    /** The flag for the status of the option's argument. Either NO_ARGUMENT, OPTIONAL_ARGUMENT, or REQUIRED_ARGUMENT  */
    private final int argFlag;
    
    /** Brief description of how to use the option. */
    private final String description;
    
    public GnuOptionDefinition(char shortKey, String longKey, int argFlag, String description)
    {
        this.shortKey = shortKey;
        this.longKey = longKey;
        if ((argFlag < NO_ARGUMENT) || (argFlag>REQUIRED_ARGUMENT))
        {
            throw new IllegalArgumentException("Illegal value of argFlag -- " + argFlag);
        }
        this.argFlag = argFlag;
        this.description = description;
    }
    
    /**
     * @return Returns the shortKey.
     */
    public char getShortKey()
    {
        return shortKey;
    }
    
    /**
     * @return Returns the longKey.
     */
    public String getLongKey()
    {
        return longKey;
    }
    
    /**
     * The flag indicates the expected presence of the option's argument
     * to be either NO_ARGUMENT, OPTIONAL_ARGUMENT, or REQUIRED_ARGUMENT.
     * 
     * @return Returns the argument flag.
     */
    public int getArgFlag()
    {
        return argFlag;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        // TODO Auto-generated method stub
        return super.equals(obj);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        // TODO Auto-generated method stub
        return super.hashCode();
    }
    /**
     * @return Returns the description.
     */
    public String getDescription()
    {
        return description;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("-" + getShortKey() + " ");
        buf.append("--" + getLongKey() + " ");
        buf.append(getDescription());
        return buf.toString();
    }
}
