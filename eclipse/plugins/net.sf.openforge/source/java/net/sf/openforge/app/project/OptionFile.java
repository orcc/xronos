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

import java.io.*;
import java.util.List;

import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionKey;
import net.sf.openforge.lim.CodeLabel;


/** 
* An {@link Option} which allows a filename to be used for the value.
 *
 * @see net.sf.openforge.app.project.Option
 * @author Andreas Kollegger
 */
public class OptionFile extends OptionRegExp
{

    private static final String _RCS_ = "$Rev: 2 $";

    /** The regexp to use to allow selection of a directory. */
    public final static String DIRECTORY_REGEXP = "\\\\";
    
    boolean is_directory = false;
    
    /**
     * Constructs a new OptionFile definition of a File based Option.
     *
     * @param key the look-up key for the option
     * @param default_value the default value for the option
     * @param regexp the regular expression used
     *      to filter file names 
     * @param hidden true if this option should be secret
     */
    public OptionFile(OptionKey key,
            String default_value,
            String regexp,
            boolean hidden)
    {
    	super(key, default_value, regexp, hidden);
    	if (regexp.equals(DIRECTORY_REGEXP)) is_directory = true;
    } // OptionFile()
    
    public String getTypeName()
    {
        return "file";
    }    
    
    /**
     * Checks if thie OptionFile is meant to select a directory
     * rather than a File.
     */
    public boolean isDirectory()
    {
        return is_directory;
    }
        
    /**
     * The expand method takes in a list of command line arguments and 
     * converts every option to "-opt label-key_pair=<value>" format.
     * @param tokens list of command line arguments.
     */
    public void expand(List tokens){
    	for(int i = 0; i < tokens.size(); i++)
    	{
    		String s = tokens.get(i).toString();
    		String key = getOptionKey().getCLASwitch();
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
     * of the right type. If true, it sets the option to that value.
     * Otherwise, it throws and InvalidOptionValue Exception.
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
    		setValue(slabel, val, true);
    	}
    }
    
    /**
     * This method checks to see if the value passed to the option is 
     * of the right type. If true, it replaces the value with the new 
     * value. Otherwise, it throws and InvalidOptionValue Exception.
     * @param slabel SearchLabel that can be used to get the scope.
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
    		replaceValue(slabel, val, true);
    	}
    }
        
    /**
     * Convenience method to get the value of the option as a File.
     * @param clabel Codelabel that can be used to get the scope.
     * @param cwd current working directory as obtained from CWD option
     * @return File - value of the option as a File.
     */
    public File getValueAsFile(CodeLabel clabel, String cwd)
    {
       	File target = new File(super.getValue(clabel).toString());
    	if (!target.isAbsolute())
        {
            target = new File(cwd, target.getPath());
        }
        return(target);
    }
    
    /**
     * Checks to see if the value is valid!
     * param s String value that is used to check for validity.
     * @return valid boolean value (whether the value is valid or not).
     */
    public boolean isValid(String s)
    {
        if (isDirectory())
        {
            File f = new File(s).getAbsoluteFile();
            return f.isDirectory();
        }
        else
        {
            return super.isValid(s);
        }
    } // isValid()

    /**
     * Returns "&lt;file|path&gt;"
     *
     * @return a value of type 'String'
     */
    public String getHelpValueDescription ()
    {
        if (!isDirectory())
            return "<file>";
        else
            return "<path>";
    }
    
} /* end class OptionFile */
