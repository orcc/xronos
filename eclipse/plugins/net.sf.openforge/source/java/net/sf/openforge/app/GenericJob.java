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

/*
 * Created on Sep 2, 2004
 */
package net.sf.openforge.app;

import java.io.*;
import java.util.*;
import java.util.logging.Handler;

import net.sf.openforge.app.logging.*;
import net.sf.openforge.app.project.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.util.*;
import net.sf.openforge.verilog.mapping.PrimitiveMapper;


/**
 * A GenericJob represents a single compilation task processed under
 * a specific set of user preferences. This is the compiler's
 * source for all user-configurable settings. Also, all access 
 * to the "environment" passes through job, so that interested
 * listeners can monitor what is going on and each job can potentially
 * have its own little world.
 * 
 * @author Srinivas Beeravolu
 */

public class GenericJob implements NewJob {
	
    protected ForgeLogger logger=
        new ForgeLogger("Forge.Default"+System.identityHashCode(this));	
    public HashMap optionsMap;
    private PrimitiveMapper primMapper;
    
    private ForgeFileHandler fileHandler = new ForgeFileHandler(this);
    
    /*
     * The full resolved (absolute) path name to the forge.h file that
     * can be used during gcc compilation of the users source.
     */
    private static String cAPI_IncludeFile = null;
    public static String DEFAULT_FILENAME = "default.forge";
	
    /** Constructor - Builds the optionsMap and loads it with default values*/
    public GenericJob() 
    {		
        optionsMap = new HashMap();	
        optionsMap = OptionRegistry.getDefaults();	
        primMapper = PrimitiveMapper.getInstance();
    }
	
    /**
     * Tokenizes the command line arguments and builds a list of tokens.
     * @param args the command line arguments
     * @return java.util.list form of command line arguments
     */
    private List tokenizeCLA (String []args)
    {
        List tokenized = new ArrayList();
        //tokenize the command line args on white space.
        for(int i = 0; i < args.length; i++)
        {
            String s = args[i].trim();
            tokenized.add(s);
        }		
        return tokenized;
    }
	
    /**
     * This method takes in the command line arguments and sets the 
     * values of the options to Forge. It first checks to see if the 
     * user supplied a forge project file. If yes, it sets the values 
     * to the options from that file. It then overrides those values 
     * with any of them given at command line.   
     * @param args command line arguments to forge
     * @throws ForgeOptionException if there is an error in any option
     * syntax/usage
     */
    public void setOptionValues(String []args) throws ForgeOptionException
    {
        List pfTokens = null;
        boolean usePfile = false;
        String pfile = null;
        String token;
        
        //tokenize the CLAs
        List claTokens = tokenizeCLA(args);
        
        //Quickly check to see if the user supplied a forge project file.
        for(int i = 0; i < claTokens.size(); i++)
        {
            token = claTokens.get(i).toString();
            if(token.charAt(0) == '-')
            {
                if((token.substring(1)).equals("pfile"))
                {
                    usePfile = true;
                    pfile = claTokens.get(i+1).toString();
                }
            }
        }

        //if user supplied a pfile, read the file.
        if(usePfile)
        {
            if (ForgeFileTyper.isForgeProject(pfile))
            {
                File pf = new File(pfile);
                if (pf.exists())
                {
                    try 
                    {
                        pfTokens = ForgeProjectScanner.readProjectFile(pf);
                    } 
                    catch (FileNotFoundException fnfe) 
                    {
                        fnfe.printStackTrace();
                    }
                }
                //We have successfully read from the pfile. Now, set those values.
                setValuesFromTokens(pfTokens, true);
            }
            else
            {
                error("Failed to read project from file -- " + pfile);
            }
        }
        
        //Now override the option values with the ones given at command line.
        expandCommandLine(claTokens);
        setValuesFromTokens(claTokens, false);
        
        //We have set the values to all the options if we reached here.
        //Now, we should walk through the tokens and collect the 
        //source files and assign them to the TARGET option.
        if(claTokens.size() > 0)
            collectSourceFiles(claTokens);
    }
    
    /**
     * The expandCommandLine method takes in a list of CLA tokens and 
     * expands every CLA to "-opt label-key_pair=<value>" format, and 
     * replaces the CLA with those two tokens.
     * @param tokens list of command line argument tokens.
     * @throws ForgeOptionException if a command line token could not
     * be expanded to the -opt syntax
     */
    private void expandCommandLine(List expTokens) throws ForgeOptionException
    {
        Option option;
        
        //Iterate through the optionMap and call Expand on each of the options.
        for(Iterator it = optionsMap.values().iterator(); it.hasNext(); )
        {
            option = (Option)it.next();
            option.expand(expTokens);
        }
        
        //Now all options should have the -opt syntax. If there is any 
        //option(switch) that was not expanded, it is an Illegal option.
        //Iterate over the tokens and check for illegal options.
        for(Iterator it = expTokens.iterator(); it.hasNext(); )
        {
            String s = it.next().toString();
            if(s.charAt(0) == '-')
            {
                if (!((s.substring(1)).equals("opt")))
                {
                    throw new NewJob.NoSuchOptionException(s);
                }
            }
        }
    }

    /**
     * This method takes in a list of expanded tokens and sets the 
     * values to the options to forge from those tokens.
     * @param tokens list of expanded tokens
     */    
    private void setValuesFromTokens(List tokens, boolean frompfile)
    {
        Option option;
        boolean found_option;
        //Go through the tokens list and for each token (CLA option) iterate over
        // optionMap and set the value for that option. 
        for(int i = 0; i < tokens.size(); i++)
        {
            String s = tokens.get(i).toString();
            if(s.charAt(0) == '-')
            {
                if ((s.substring(1)).equals("opt"))
                {
                    //if the syntax of the value to opt is wrong, throw an exception.
                    // ie.. if it does not have an "=" sign.
                    // If all is well, set the value of the option.
                    String newToken = null;
                    try
                    {
                        newToken = tokens.get(i+1).toString();
                        StringTokenizer st = new StringTokenizer(newToken, "=");
                        String labelKey_pair = st.nextToken();
                        String p_value = "";
                        if(st.hasMoreTokens())
                        {
                            p_value = st.nextToken();
                        }                      
                        
                        StringTokenizer token = new StringTokenizer(labelKey_pair, "@");
                        String p_key = token.nextToken();
                        String label = null;
                        if(token.hasMoreTokens())
                        {
                            label = token.nextToken();
                        }
                        
                        found_option = false;
                        
                    	SearchLabel slabel;
                    	if(label == null)
                    	{
                            slabel = CodeLabel.UNSCOPED;
                    	}
                    	else
                    	{
                            slabel = new CodeLabel(label);
                    	}
                    	//Iterate over the optionsMap to find the right option and set its value.
                    	for(Iterator it = optionsMap.values().iterator(); it.hasNext();)
                        {
                            option = (Option)it.next();
                            if((option.getOptionKey().getKey()).equals(p_key))
                            {							
                                if(frompfile)
                                    option.replaceValue(slabel, p_value);
                                else
                                    option.setValue(slabel, p_value);
                                found_option = true;
                            }
                        }
                    	//If the option is not found, complain and exit.
                        if(!found_option)
                        {
                            this.fatalError("Failed to set option specified by \"" + newToken + "\", option not found");
                        }
                    }
                    catch (IllegalCLASyntaxException ex)
                    {
                        throw new IllegalCLASyntaxException(newToken);
                    }					
                }
            }
        }
    }  

    /**
     * Collects the source files from the command line arguments 
     * to forge. 
     * @param tokens The list of command line arguments.
     */
    private void collectSourceFiles(List tokens)
    {
        boolean removed_token = false;
        //first walk through the tokens and remove all "-opt <value>" tokens
        //-opt and <value> are two tokens. Remove both of them.
        for(int i = 0; i < tokens.size(); i++)
        {
            if(removed_token)
            {
                i = 0;
                removed_token = false;
            }
            String s = tokens.get(i).toString();
            if(s.charAt(0) == '-')
            {
                //Remove the first token.
                tokens.remove(i);
                //Remove the second token. Note that in the prev step, elements moved up by one.
                tokens.remove(i);
                removed_token = true;
            }
        }
        
        /*
         * Whatever tokens that are remaining are source files.
         * Assign them to the TARGET option in optionsMap if they
         * are of the CSource type.
         */
        if(tokens.size() > 0)
        {
            Option option = getOption(OptionRegistry.TARGET);
            boolean isC = false;
            boolean isXLIM = false;
            for(Iterator it = tokens.iterator(); it.hasNext(); )
            {
                String filename = it.next().toString();
                if (ForgeFileTyper.isCSource(filename))
                {
                    if (isXLIM)
                        throw new IllegalMixedSourcesException();
                    
                    isC = true;
                    option.setValue(CodeLabel.UNSCOPED, filename);
                }
                else if (ForgeFileTyper.isXLIMSource(filename))
                {            
                    if (isC)
                        throw new IllegalMixedSourcesException();
                    
                    isXLIM = true;
                    option.setValue(CodeLabel.UNSCOPED, filename);
                }
                else
                {
                    File f = this.getAbsoluteSourceFile(filename);
                    if (f.exists())
                    {
                        error("Can't handle files of this type: " + filename);
                    }
                    else
                    {
                        fatalError("Filename doesn't exist: " + filename);
                    }
                }
            }
        }
        
        /*
         * Now we have collected the source files. Let us quickly validate them.
         * ie...check if the file exists and it is not empty.
         */
        File[] srcFiles = getTargetFiles();
        if(srcFiles.length > 0)
        {
            for(int i = 0 ; i < srcFiles.length; i++)
            {
                File f = srcFiles[i];
                if(!f.exists())  //if the file does not exist
                {
                    if(i == 0)  //and if it is the first file (Target), just quit.
                        fatalError( f.getName() + ": No such file");
                    else		//otherwise, you dont care much.
                        error("Skipping file " + f.getName() + " : No such file");
                }
                else if(f.length() == 0)	//if the file is empty
                {
                    if(i == 0)		//if it is the Target, just quit.
                        fatalError( f.getName() + " : file has a length of 0");
                    else			//otherwise, dont care.
                        error("Skipping " + f.getName() + ": file has length of 0");
                }
            }
        }
    }
	
    /**
     * Gets an option that is mapped to the OptionKey 'key' from the optionsMap.
     * @param key OptionKey that is to be looked up.
     * @return the Option that is mapped to 'key' in the optionsMap. 
     */
    public Option getOption(OptionKey key) {
        return (Option)optionsMap.get(key);
    }

    /**
     * This is a convenience method for accessing the value of
     * 'global' options whose type is {@link OptionBoolean}
     *
     * @param key a value of type 'OptionKey'
     * @return a value of type 'boolean'
     */
    public boolean getUnscopedBooleanOptionValue(OptionKey key)
    {
        assert key != null;
        Option opt = getOption(key);
        assert opt != null;
        return ((OptionBoolean)opt).getValueAsBoolean(CodeLabel.UNSCOPED);
    }
	
    /**
     * Adds an option to the optionsMap of the GenericJob.
     * @param key OptionKey that is to be added (look-up key)
     * @param opt Option that is mapped to the look-up key
     */
    public void addOption(OptionKey key, Option opt) {
        optionsMap.put(key, opt);
    }
	
    public void updateLoggers ()
    {
        // delete current handlers
        Handler[] h=logger.getRawLogger().getHandlers();
        for(int i=0;((h!=null)&&(i<h.length));i++)
        {
            logger.getRawLogger().removeHandler(h[i]);
        }
    
        // create the logger for this Job
        // quiet + verbose means fine, only to log
        // quiet means nothing
        // verbose means fine to log and stdout
        // default is info to log
        String lev="warn";
        if (getUnscopedBooleanOptionValue(OptionRegistry.VERBOSE_VERBOSE))
            lev = "verbose";
        else if (getUnscopedBooleanOptionValue(OptionRegistry.VERBOSE))
            lev = "info";

        if (getUnscopedBooleanOptionValue(OptionRegistry.QUIET))
            lev = "off";
        
        final Option option = getOption(OptionRegistry.LOG);
        logger.processLogString(option.getValue(CodeLabel.UNSCOPED).toString(), lev);
     
        // log appropriately
        System.setOut(new SystemLogger(logger,"info"));
        System.setErr(new SystemLogger(logger,"warn"));        
    } //updateLoggers()
	
    public PrimitiveMapper getPrimitiveMapper()
    {
        return primMapper;
    }
    
    /**
     * Indent the logger stream one level
     *
     */
    public void inc()
    {
        getLogger().inc();
    }

    /**
     * Un indent the logging stream one level
     *
     */
    public void dec()
    {
        getLogger().dec();
    }

    /**
     * Un indent all active levels
     *
     */
    public void decAll()
    {
        getLogger().decAll();
    }
	
    /**
     * Log an informatational level message.
     * By default this goes to forge.log
     *
     * @param s message to log
     */
    public void info(String s)
    {
        getLogger().info(s);
    }

    /**
     * Log a verbose level message.
     *
     * @param s a value of type 'String'
     */
    public void verbose(String s)
    {
        getLogger().verbose(s);
    }

    /**
     * Log a warning level message
     *
     * @param s a value of type 'String'
     */
    public void warn(String s)
    {
        getLogger().warn(s);
    }

    /**
     * Log an error level message
     *
     * @param s a value of type 'String'
     */
    public void error(String s)
    {
        getLogger().error(s);
    }    
    
    /**
     * Log a warning level message -- no error: prefix
     *
     * @param s a value of type 'String'
     */
    public void raw_warn(String s)
    {
        getLogger().raw_warn(s);
    }

    /**
     * Log an error level message
     *
     * @param s a value of type 'String'
     */
    public void raw_error(String s)
    {
        getLogger().raw_error(s);
    }    
    
    public ForgeLogger getLogger()
    {
        return logger;
    }
    
    /**
     * Log an error level message, then cease processing and exit the
     * engine
     *
     * @param s a value of type 'String'
     */
    public void fatalError(String s)
    {   
        ForgeFatalException ffe=new ForgeFatalException(s);
        getLogger().error(ffe,s);
        throw(ffe);
    }
	
    /**
     * Takes in a String and returns a File represented 
     * by that string. Gets the absolute path to file 
     * with respect to the SOURCEPATH.
     * 
     * @param s string whose file representation is needed
     * @return file file represented by the string.
     */
    private File getAbsoluteSourceFile(String s)
    {		
        File file = new File(s);
        if (!file.isAbsolute())
        {
            Option op = getOption(OptionRegistry.SOURCEPATH);
            String srcPath[] = ((OptionList)op).toArray(op.getValue(CodeLabel.UNSCOPED).toString());
            assert srcPath.length > 0;
            file = new File(srcPath[0], file.getPath());
            int i = 1;
            while(!file.exists() && i < srcPath.length)
            {
                file = new File(srcPath[i++], file.getPath());
            }
        }
        return(file);
    } //getAbsoluteFile()
	
    /**
     * This is a convenience method to get the target files 
     * that need to be forged. 
     * 
     * @return an array of target files.
     */
    public File[] getTargetFiles()
    {
        Option op = getOption(OptionRegistry.TARGET);
        List srcList = ((OptionMultiFile)op).getValueAsList(CodeLabel.UNSCOPED);
        File[] f = new File[srcList.size()];
        int i=0;
        for(Iterator it=srcList.iterator(); it.hasNext(); )
        {
            String s = (String)it.next();
            f[i++] = this.getAbsoluteSourceFile(s);
        }        
        return f;	    	
    }  //getTargetFiles()	
	
    /**
     * This is a convenience method to get the destination 
     * directory for the files produced by forge. It checks 
     * for the -dfs (destination follows target) switch and 
     * if that is not set, it gets the value from the destination 
     * directory specified by the user.  
     * 
     * @return Destination directory for files produced by forge.
     */	
    private File getDestination()
    {
        Option op;
        CodeLabel cl;
		
        if (getUnscopedBooleanOptionValue(OptionRegistry.DESTINATION_FOLLOWS_TARGET))
        {			
            File f = getTargetFiles()[0];
            return f.getParentFile();
        }
        else
        {
            op = getOption(OptionRegistry.DESTINATION_DIR);
            String destName = op.getValue(CodeLabel.UNSCOPED).toString();
            File f = new File(destName);
            if(!f.isAbsolute())
            {
                op = getOption(OptionRegistry.CWD);
                String cwd = op.getValue(CodeLabel.UNSCOPED).toString();
                f = new File(cwd, f.getPath());
            }
            return f;
        }
    } //getDestination()

    private File getHDLDestination (boolean createNonExisting)
    {
        final String peName = getOption(OptionRegistry.PE_NAME).getValue(CodeLabel.UNSCOPED).toString();
        final String peVersion = getOption(OptionRegistry.PE_VERSION).getValue(CodeLabel.UNSCOPED).toString();

        final File destBaseDir = new File(getDestination().getAbsolutePath());
        File destDir = destBaseDir;
        
        if (!getUnscopedBooleanOptionValue(OptionRegistry.NO_EDK))
        {
            // Specify all forge output directory
            File designOutputDir = new File(destBaseDir, peName + "_" + peVersion);
            File hdlDir = new File(designOutputDir, "hdl");
            destDir = new File(hdlDir, "verilog");
        }

        if (createNonExisting && !destDir.exists())
        {
            destDir.mkdirs();
        }
        
        return destDir.getAbsoluteFile();
    }

    /**
     * Returns the {@link ForgeFileHandler} that manages the files
     * generated by the compiler.
     */
    public ForgeFileHandler getFileHandler ()
    {
        return this.fileHandler;
    }
    
    /**
     * Returns the base name that is to be used for output files of
     * this compilation.  By default this is taken from the name of
     * the first input source file, however if the user specifies a
     * different output file name, then the base name is derived from
     * that. 
     *
     * @return a non-null, non zero length String
     */
    public String getOutputBaseName ()
    {
        String base = getOutputFileName();

        if (base.length() == 0)
        {
            final String srcFileName = getTargetFiles()[0].getName();
            base = srcFileName.substring(0,srcFileName.lastIndexOf("."));
        }
        
        assert base.length() > 0 : "Malformed output file name";
        return base;
    }
    
    /**
     * This is a convenience method to get the list of 
     * all the directories where the included files are to 
     * be searched. It compiles a list of the absolute paths 
     * to all the include file directories.
     * 
     * @return list of absolute paths to Include directories.
     */	
    public List getIncludeDirList()
    {
        Option op;
        CodeLabel cl;
		
        op = getOption(OptionRegistry.INCLUDES_DIR);
        List l = ((OptionMultiFile)op).getValueAsList(CodeLabel.UNSCOPED);
        List filePathList = new ArrayList();
        String s;
        File f;
        op = getOption(OptionRegistry.CWD);
        String cwd = op.getValue(CodeLabel.UNSCOPED).toString();
        for(int i = 0; i < l.size(); i++)
        {
            s = l.get(i).toString();
            f = new File(s);
            if(!f.isAbsolute())
            {
                f = new File(cwd, f.getPath());
            }			
            filePathList.add(f.getAbsolutePath());
        }
        return filePathList;
    }  //getIncludeDirList()
	
    private XilinxDevice myDevice=null;
    private String myPartName = null;
    
    public XilinxDevice getPart (SearchLabel slabel)
    {
    	Option op = getOption(OptionRegistry.XILINX_PART);    	
    	
        String currentPartName = op.getValue(CodeLabel.UNSCOPED).toString();
        if((myDevice==null) || !(currentPartName.equals(myPartName)))
        {
            myPartName = currentPartName;
            myDevice=new XilinxDevice(myPartName);
        }
        return myDevice;
    }
    
    public XilinxDevice getDefaultPart ()
    {
    	Option op = getOption(OptionRegistry.XILINX_PART);
    	myDevice = new XilinxDevice(op.getDefault());
    	return myDevice;
    }    
	
    public String getTargetSpeed()
    {
        if (getUnscopedBooleanOptionValue(OptionRegistry.ENABLE_SPEED))
        {
            Option op = getOption(OptionRegistry.TARGET_SPEED);
            String value = op.getValue(CodeLabel.UNSCOPED).toString();
            // return the empty string if the speed is set to empty
            if(value.equals(""))
                return(value);
            
            return(OptionIntUnit.getIntValueString(value) + " " + 
                ((OptionIntUnit)op).getUnits(value));
        }
        else
        {
            return "";
        }
    }
    
    private boolean warned = false;
    /**
     * Returns the setting of -o from the command line, an empty
     * string if the user didn't supply
     */
    private String getOutputFileName ()
    {
    	Option op;
    	op = getOption(OptionRegistry.DESTINATION_FILE);
        String of = op.getValue(CodeLabel.UNSCOPED).toString();
        
        // if empty string just return, no need for further sanity checks
        if(of.length() == 0)
            return(of);
        
        // use the supplied destination file name as the output.
        // Check if .v was appended and remove if present.
        if(of.endsWith(".v") || of.endsWith(".V"))
        {
            of = of.substring(0,of.lastIndexOf("."));
        }

        // detect any file paths in the supplied name, warn the
        // user that they are being tossed
        String baredf = (new File(of)).getName();

        if(!baredf.equals(of))
        {
            if(!warned)
            {
                warned = true;
                warn("output file: " + of + " can't contain directories, truncating to: " + baredf);
            }
            
            of = baredf;
        }

        return(of);
    }
    
    
    /**
     * Generates a temporary directory in the system specified
     * location and writes our default forge header file (as found via
     * the ForgeResource loader).  The absolute location of this
     * header file is returned as a string.  This method caches its
     * result so that we only generate the file one time.
     *
     * @return a non-null String
     */
    // JWJFIXME: Figure out whether we still need this.
    public static String getCApiIncludeFile()
    {
        if (cAPI_IncludeFile == null)
        {
            try
            {
                InputStream stream = ForgeResource.loadForgeResourceStream("FORGE_C_HEADER");
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                
                File outDir = FileUtils.getTemporaryDirectory("forgehdrs");
                
                /*
                 * Create the forge.h file in the temp dir we just created
                 * and copy the cached version to it.
                 */
                File outFile = new File(outDir, "forge.h");
                PrintStream ps = new PrintStream(new FileOutputStream(outFile));
                while (reader.ready())
                {
                    ps.println(reader.readLine());
                }
                ps.close();
                outFile.deleteOnExit();
                
                cAPI_IncludeFile = outFile.getAbsolutePath();
            }
            catch (IOException e)
            {
                EngineThread.getGenericJob().warn("Error when creating a temporary file for cached forge.h " + e);
            }
            catch (FileUtils.FileUtilException fue)
            {
            	EngineThread.getGenericJob().warn("Error when creating a temporary file for cached forge.h " + fue);
            }
            
            if (cAPI_IncludeFile == null)
            {
            	EngineThread.getGenericJob().warn("Could not create temporary forge.h, using unqualified forge.h for include");
                // blindly punt and try just 'forge.h'.  This will allow a
                // user to supply it as a workaround in case something
                // goes wrong
                cAPI_IncludeFile = "forge.h";
            }
        }
        
        return cAPI_IncludeFile;
    }
    
    /**
     * return the source path prepended by $FORGE/c/lib which holds 
     * the default operation replacement implementations
     */    
    public String[] getLibrarySourcePath ()
    {
        // need to prepend api dir to sourcepath so linker find our
        // api classes
    	Option op = getOption(OptionRegistry.SOURCEPATH);
    	String[] tmp = ((OptionList)op).toArray(op.getValue(CodeLabel.UNSCOPED).toString());        
    	
        String[] result = new String[tmp.length + 1];
        String sep=System.getProperty("file.separator");
        
        result[0]=System.getProperty("forge.home")+sep+"c"+sep+"lib";

        System.arraycopy(tmp,0,result,1,tmp.length);
        
        return result;
    }    
    
    public void printTokens(String s, List tok){
    	System.out.println("Printing tokens of ");
    	for(Iterator it = tok.iterator(); it.hasNext();)
            System.out.println("\t" + it.next().toString());
    }

    private static class IllegalMixedSourcesException extends NewJob.ForgeOptionException
    {
        private IllegalMixedSourcesException()
        {
            super("Cannot mix xlim and C sources");
        }
    }
    
}
