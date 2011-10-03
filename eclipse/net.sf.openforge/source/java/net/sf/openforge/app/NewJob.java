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
 * Created on Sep 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.sf.openforge.app;

import net.sf.openforge.app.project.Option;

/**
 * NewJob is the main interface and it maintains a single map with all 
 * the preferences and options in it.
 * 
 * @author Srinivas Beeravolu
 */
public interface NewJob {
	
	/**
	 * This method is used to the set the values of the Options
	 * of Forge, from the command line arguments.
	 * 
	 */
    public void setOptionValues(String []args) throws ForgeOptionException;
		
	/** 
	 *  This method is used to get an option based on the key 
	 *  passed to it.
	 * 
	 */	
	public Option getOption(OptionKey key);

	/**
     * Convenience method for determining the value of various command
     * line switches.
     */
    public boolean getUnscopedBooleanOptionValue(OptionKey key);
	
	/**
	 *  This method is used to add an option to the Map.
	 * 
	 */
	public void addOption(OptionKey key, Option opt);
	
	/**
     * Log an informatational level message.
     * By default this goes to forge.log
     *
     * @param s message to log
     */
    public void info(String s);

    /**
     * Log a verbose level message.
     *
     * @param s a value of type 'String'
     */
    public void verbose(String s);
    
    /**
     * Log a warning level message
     *
     * @param s a value of type 'String'
     */
    public void warn(String s);
    
    /**
     * Log an error level message
     *
     * @param s a value of type 'String'
     */
    public void error(String s);

    public static abstract class ForgeOptionException extends RuntimeException
    {
        public ForgeOptionException (String msg)
        {
            super(msg);
        }
    }
        
    /* 
     *  Exceptions. 
     */
    public static class NoSuchOptionException extends ForgeOptionException
    {
        public NoSuchOptionException(String key)
        {
            super("Option hasn't been defined for key: \"" + key + "\"");
        }
    } // inner class NoSuchOptionException
    
    public static class InvalidOptionValueException extends ForgeOptionException
    {
        public InvalidOptionValueException(String key, String value)
        {
            super("Option named \"" + key + "\" can't be set to \"" + value + "\"");
        }
        
        public InvalidOptionValueException(String key)
        {
            super("Invalid value to Option named \"" + key + "\"");
        }
    } // inner class InvalidOptionValueException

    public static class InvalidNumberOfArgsException extends ForgeOptionException
    {
        public InvalidNumberOfArgsException()
        {
            super("Invalid number of arguments to forge. Type \"forge -h\" for help.");
        }
        
        public InvalidNumberOfArgsException(String s)
        {
            super("Invalid number of arguments to forge. Type \"forge -h\" for help. " + s);
        }
    } // inner class InvalidNumberOfArgsException
    
    public static class InvalidEntryValueException extends Exception
    {
        public InvalidEntryValueException(String key, String value)
        {
            super("Option named \"" + key + "\" can't be set to \"" + value + "\", " + 
                  "function \"" + value + "\" is not present in the provided source files");
        }
    } // inner class InvalidOptionValueException
	
}
