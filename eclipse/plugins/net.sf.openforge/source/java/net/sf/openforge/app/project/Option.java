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

import net.sf.openforge.app.OptionKey;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.util.IndentWriter;


/**
 * Option defines a settable/gettable value.  Options can have an associtated tag
 * which specifies grouping information. An unspecified
 * tag places the Option in the default OptionDB.GLOBAL
 * group. The OptionDB.CONFIG group is a special designation
 * used for Options which relate to configuration information
 * rather than processing constraints (ie, directory locations,
 * search paths, verbosity, etc). Other special groups can be
 * created as needed. 
 *
 * @author Andreas Kollegger
 */
public abstract class Option
{
    private static final String _RCS_ = "$Rev: 2 $";

    private OptionKey optionKey;
    private String default_value;
    private boolean hidden = false;
    protected HashMap localMap = new HashMap();
    
    private String unscoped;
    int brief_end_index = 0;
    boolean isFirstTime = true;

    /**
     * Constructs an Option.
     *
     * @param key the lookup-key for the option (its name)
     * @param default_value the default value related to the key 
     * @param hidden whether this option should be hidden from the user
     */
    public Option(OptionKey key,
            String default_value,
            boolean hidden)
	{    	
		this.optionKey = key;
		this.default_value = default_value;
		this.hidden = hidden;
		unscoped = CodeLabel.UNSCOPED.getSearchList().get(0).toString();
		localMap.put(unscoped, default_value);
		brief_end_index = findFirstSentenceEnd(getDescription(), 0);
	} // Option
   
    /**
     * Gets the default value for this option.
     */
    public String getDefault()
    {
        return default_value;
    }

    /**
     * Gets the first sentence of the description.
     */
    public String getBriefDescription()
    {
        return getOptionKey().getDescription().substring(0, brief_end_index);
    }

    /**
     * Gets the entire description for the option.
     * @return String description
     */
    public String getDescription()
    {
        return getOptionKey().getDescription();
    }

    /**
     * Returns a String indicating the type of value that is valid for
     * this type of option.
     *
     * @return a non-null String, may be zero length if no value is to
     * be supplied.
     */
    public abstract String getHelpValueDescription ();
    
    /**
     * Gets the OptionKey of the option. 
     * @return OptionKey
     */
    public OptionKey getOptionKey()
    {
        return optionKey;
    }
    
    /**
     * Says if the option is a hidden option or not.
     * @return boolean hidden or not.
     */
    public boolean isHidden()
    {
        return hidden;
    }

    private int findFirstSentenceEnd(String s, int from)
    {
        int i = s.indexOf('.', from);
        
        if ((i < (s.length() - 2)) && (i>0))
        {
            if (!Character.isWhitespace(s.charAt(i+1)))
            {
                i = findFirstSentenceEnd(s, i+1);
            } else 
            {
                i = i + 1;
            }
        }
        else
        {
            i = s.length();
        }
        
        return i;
        
    } // findFirstSentenceEnd()

/*    public String toString()
    {
        return new String("Option -- " + getKey() + 
                          " default:{" + getDefault() + "}" +
                          " brief:\"" + getBriefDescription() + "\"" +
                          " long:\"" + getDescription() + "\"" +
                          " cli: " + (hasCLI() ? ("\"" + getCLI() + "\"") : "none") +
                          " hidden? " + (hidden ? "yes" : "no")
                          );
    }
  */
    
    public void printXML(IndentWriter printer, String value)
    {
       printer.println(
            "<option name=\"" + getOptionKey().getKey() + "\" " +
            "type=\"" + getTypeName() + "\">" +
            value +
            "</option>");
    }
    
    
    /**
     * The expand method takes in a list of command line arguments and 
     * converts every option to "-opt label-key_pair=<value>" format.
     * @param tokens list of command line arguments.
     */
    public abstract void expand(List tokens);
    
    /**
     * This method sets the value to the option. The validity check 
     * on the 'value' is performed in the different Option<Type> classes.
     * @param slabel Searchlabel that can be used to get the scope.
     * @param val value to the option.
     */
    public void setValue(SearchLabel slabel, Object value)
    {
    	if(isFirstTime)
    		isFirstTime = false;
    	localMap.put(slabel.getSearchList().get(0).toString(), value);
    }
    
    
    /**
     * Gets the value associated with the option. It uses the SearchLabel 
     * to get a list of scopes to search the value for and returns the 
     * value of the first matching scope. If no matching scope is found, 
     * it returns the default value of the option. This method NEVER 
     * returns null. 
     * @param slabel label to search for scope
     * @return Object - value of the option
     */
    public Object getValue(SearchLabel slabel)
    {
    	List labelList = slabel.getSearchList();
    	String retval = localMap.get(unscoped).toString();
    	for(Iterator it = labelList.iterator(); it.hasNext(); )
    	{
    		String s = it.next().toString();
	    	if(localMap.containsKey(s))
	    	{
	    		retval = localMap.get(s).toString();
	    		break;
	    	}
    	}
    	return retval;
    }
    
    /**
     * Gets the value associated with the option at a particular scope. 
     * It uses the scope to get the value for the option at that 
     * scope. If no value exists at that scope, it returns null.
     * @param scope the particular scope at which the value is to be searched.
     * @return Object - value of the option
     */
    public Object getExplicitValue(String scope)
    {    
    	String retval = null;  	    	
	    if(localMap.containsKey(scope))
	    {	    
	    	retval = localMap.get(scope).toString();	    
	    }
    	return retval;
    }
    
    /**
     * This method replaces the current value in the localMap with 
     * the value passed to it. Note that setValue in OptionList behaves 
     * differently, as it appends the new value to the existing value.
     * Hence, OptionList and OptionMultiFile need replaceValue methods 
     * to overcome this.
     * @param slabel the searchLabel to search for scope
     * @param value - value of the option
     */
    public void replaceValue(SearchLabel slabel, Object value)
    {  
    	if(isFirstTime)
    		isFirstTime = false;
    	localMap.put(slabel.getSearchList().get(0).toString(), value);
    }
    
    /**
     * Validates a value as assignable to this Option.
     *
     * @param s the string version of a possible value
     * @return true if the value is valid; false otherwise
     */
    public abstract boolean isValid(String s);
    
    public abstract String getTypeName();
    
} /* end class Option */
