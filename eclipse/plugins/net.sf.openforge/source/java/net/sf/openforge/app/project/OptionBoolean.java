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

import java.util.List;

import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionKey;


/** 
 * An Option which accepts boolean true/false settings.
 *
 * @author Andreas Kollegger
 */
public class OptionBoolean extends Option
{
    private static final String _RCS_ = "$Rev: 2 $";    
    
    /**
     * Constructs an Option of OptionBoolean type.
     *
     * @param key the lookup-key for the option (its name)
     * @param default_value the default value related to the key 
     * @param hidden whether this option should be hidden from the user
     */
    public OptionBoolean(OptionKey key,
        boolean default_value,
        boolean hidden)
	{    	
    	super(key, Boolean.toString(default_value), hidden);
	} // OptionBoolean()    
    
    
    /**
     * The expand method takes in a list of command line arguments and 
     * converts every option to "-opt label-key_pair=<value>" format.
     * @param tokens list of command line arguments.
     */
    public void expand(List tokens)
    {
    	String key;
    	//some options might have more than one CLA switch! For eg. help has 2 !
    	//So, get the list of all CLAs and iterate over the list
    	List claList = getOptionKey().getCLAList();
    	for(int idx = 0 ; idx < claList.size(); idx++)
    	{
    		key = claList.get(idx).toString();
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
    					s = getOptionKey().getKey() + "=true";    					
    					tokens.add(i+1, s);	
    				}
    			}
    		}
    	}
    }
    
    /**
     * This method checks to see if the value passed to the option is 
     * of the right type. If true, it sets the option to that value.
     * Otherwise, it throws and InvalidOptionValue Exception.
     * @param slabel Searchlabel that can be used to get the scope.
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
    		super.setValue(slabel, val);
    	}    	
    }
    
    /**
     * This method checks to see if the value passed to the option is 
     * of the right type. If true, it replaces the value with the new 
     * value. Otherwise, it throws and InvalidOptionValue Exception.
     * @param slabel Searchlabel that can be used to get the scope.
     * @param val value to the option.
     */
    public void replaceValue(SearchLabel slabel, Object val)
    {
    	if(!this.isValid(val.toString()))
    	{
    		throw new NewJob.InvalidOptionValueException(getOptionKey().getKey(), val.toString());
    	}
    	else
    	{
    		super.replaceValue(slabel, val);
    	}    	
    }
    
    /**
     * Checks to see if the value is valid!
     * param s String value that is used to check for validity.
     * @return valid boolean value (whether the value is valid or not).
     */
    public boolean isValid(String s)
    {
        boolean valid = (s.equalsIgnoreCase("true") ||
                         s.equalsIgnoreCase("false") ||
						 s.equals("T") ||
						 s.equals("F"));

        return valid;
    } // isValid()
    
    /**
     * Convenience method to get the value of the option as a boolean value.
     * @param clabel Codelabel that can be used to get the scope.
     * @return boolean value of the option.
     */
    public boolean getValueAsBoolean(SearchLabel slabel)
    {    	
    	return new Boolean(super.getValue(slabel).toString()).booleanValue();
    }
  
    public String getTypeName()
    {
        return "boolean";
    }
    
    /**
     * Returns a 0 length string.
     *
     * @return a value of type 'String'
     */
    public String getHelpValueDescription ()
    {
        return "";
    }
    
} /* end class OptionBoolean */
