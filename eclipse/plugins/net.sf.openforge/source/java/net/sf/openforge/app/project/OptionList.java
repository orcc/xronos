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
package net.sf.openforge.app.project;


import java.util.*;

import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionKey;
import net.sf.openforge.util.IndentWriter;

/** 
 * An option which allows an arbitrary string to be used for the value.
 *
 * @see net.sf.openforge.app.project.Option
 * @author Andreas Kollegger
 */
public class OptionList extends Option
{

    private static final String _RCS_ = "$Rev: 122 $";
    /** The characters that are used to divide a list into elements
     * when represented as a string */
    private String separators = "[]," + java.io.File.pathSeparator;
    
    /**
     * Constructs a new OptionList of a list based Option.
     *
     * @param key the lookup-key for the option (its name)
     * @param default_value the default value related to the key 
     * @param hidden whether this option should be hidden from the user
     */
    public OptionList(OptionKey key,
        String default_value,
        boolean hidden)
	{
    	super(key, default_value, hidden);
	} // OptionList()

    /** Returns the String of characters that are used to divide the
     * String representation of this list into elements. */
    public String getSeparators ()
    {
        return this.separators;
    }

    /** Allows the setting of the String of characters that are used
     * to divide the String representation of this list into
     * elements. */ 
    public void setSeparators (String sep)
    {
        this.separators = sep;
    }
    
    /**
     * The expand method takes in a list of command line arguments and 
     * converts every option to "-opt label-key_pair=<value>" format.
     * @param tokens list of command line arguments.
     */
    public void expand(List tokens)
    {
    	String key = getOptionKey().getCLASwitch();
    	for(int i = 0; i < tokens.size(); i++)
    	{
    		String s = tokens.get(i).toString();
    		if(s.charAt(0) == '-')
    		{
    			if(key.equals(s.substring(1)))
    			{
    				//replace the cla switch with -opt and set its value.
    				s = "-opt";
    				tokens.add(i, s);
    				tokens.remove(i+1);
    				try
    				{
	    				String value = tokens.get(i+1).toString();
	    				s = getOptionKey().getKey() + "=" + value;
	    				tokens.remove(i+1);
	    				tokens.add(i+1, s);
    				}
    				catch (Exception e)
    				{
    					throw new NewJob.InvalidNumberOfArgsException();
    				}
    			}
    		}
    	}
    }    
        
    
    /**
     * This method checks to see if the value passed to the option is 
     * of the right type. If true, it then checks to see if it has a 
     * value assigned to it. If it already has a value, then it appends 
     * the new value to the end of the List. Otherwise, it just 
     * sets the option to that value. If the value passed to it 
     * is invalid, it throws and InvalidOptionValue Exception.
     * @param slabel SearchLabel that can be used to get the scope.
     * @param val value to the option.
     */
    public void setValue(SearchLabel slabel, Object val)
    {
    	if(!this.isValid(val.toString()))
    	{
    		throw new NewJob.InvalidOptionValueException(getOptionKey().getKey(), val.toString());
    	}
    	else
    	{
    		if(isFirstTime)
    		{
    			super.replaceValue(slabel, val);	
    		}
    		else
    		{
    			//if the value is already present, then append the new value;
    			List oldList = this.getValueAsList(slabel);
    			List newList = this.toList(val.toString());
    			oldList.addAll(newList);
    			String s = this.toString(oldList);
    			super.setValue(slabel, s);
    		}
    	}
    }
    
    /**
     * This method replaces the current value of the option with 
     * the value passed to it. Note that setValue in OptionList behaves 
     * differently, as it appends the new value to the existing value.
     * Hence, OptionList and OptionMultiFile need replaceValue methods 
     * to overcome this.
     * @param slabel the searchLabel to search for scope
     * @param value - value of the option
     */
    public void replaceValue(SearchLabel slabel, Object value)
    {
    	if(!this.isValid(value.toString()))
    	{
    		throw new NewJob.InvalidOptionValueException(getOptionKey().getKey(), value.toString());
    	}
    	else
    	{
    		super.replaceValue(slabel, value);
    	}
    }
    
    /**
     * Convenience method to get the value of the option as a List.
     * @param slabel SearchLabel that can be used to get the scope.
     * @return List value of the option as a List.
     */
    public List getValueAsList(SearchLabel slabel)
    {
    	return this.toList(super.getValue(slabel).toString());
    }
    
    
    public boolean isValid(String s)
    {
        return true;
    } // isValid()

     
    /**
     * Converts a list of elements to a single string
     * representation.
     */
    public static String toString(java.util.List l)
    {
        return l.toString();
    } // toString()
    
    public java.util.List toList(String s)
    {        
        //StringTokenizer st = new StringTokenizer(s, "[]," + java.io.File.pathSeparator);
        StringTokenizer st = new StringTokenizer(s, separators);
        java.util.List reply = new ArrayList();
        
        while (st.hasMoreTokens())
        {
            reply.add(st.nextToken().trim());
        }
        return reply;
    }

    public String[] toArray(String s)
    {
        StringTokenizer st = new StringTokenizer(s, separators);
        String[] reply = new String[st.countTokens()];

        int i=0;
        while (st.hasMoreTokens())
        {
            reply[i++] = st.nextToken().trim();
        }

        return reply;
    }
    
    public void printXML(IndentWriter printer, String value)
    {
        java.util.List entries = this.toList(value);
        
        printer.println("<option name=\"" + getOptionKey().getKey() + "\" " +
                        "type=\"" + getTypeName() + "\">");
        printer.inc();
        
        for (Iterator eit = entries.iterator(); eit.hasNext();)
        {
            String entry = (String)eit.next();
            printer.println("<entry>" + entry + "</entry>");
        }
        printer.dec();
        printer.println("</option>");
    }
    
    public String getTypeName()
    {
        return "list";
    }
    
    /**
     * Returns "&lt;value&gt;"
     *
     * @return a value of type 'String'
     */
    public String getHelpValueDescription ()
    {
        return "<value>";
    }
    
} /* end class OptionList */
