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

package net.sf.openforge.verilog.pattern;


import java.util.*;
import java.io.*;

import net.sf.openforge.util.VarFilename;
import net.sf.openforge.verilog.model.*;

/**
 * IncludeStatement is a include statement in Verilog.
 * <pre>`include "pipelined_sim.v"</pre>
 *
 * In order to support xflow under wine in linux, we need to list include files
 * in "windows" format, even though we are running under linux.  Thus, $XILINX
 * is of the form C:\\Xilinx and not /opt/xilinx.  This obviously confuses File
 * to no end (why can't windows use a normal unix file system convention?).  So,
 * until Xilinx supports the xflow chain natively under linux (3Q03?), we have a
 * hack:  constructor IncludeStatement(String fileToInclude, boolean
 * dontMakeMeAFile) and 
 *
 * <p>Created: Mon Aug 26 13:47:38 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: IncludeStatement.java 2 2005-06-09 20:00:48Z imiller $
 */
public class IncludeStatement 
    extends Directive.Include
{

    private static final String _RCS_ = "$Rev: 2 $";

    private File file=null;

    /** to handle windows includes under linux */
    private String stringFile=null;

    public IncludeStatement (File toInclude)
    {
        this.file = toInclude;
    }

    /**
     * hack to keep a file in the "wrong" (windows) format on linux.  Please
     * remove once xflow works natively under linux
     * @param fileToInclude
     * @param dontMakeMeAFile placeholder - to differentiate from 1 String
     * constructor
     * note: this is only to be called if XIL_LINUX_WINE is true!
     */
    public IncludeStatement (String fileToInclude, boolean dontMakeMeAFile)
    {
        stringFile=VarFilename.parse(fileToInclude);
    }
    
            
    public IncludeStatement (String fileToInclude)
    {
        this(new File(VarFilename.parse(fileToInclude)));
    }

    public Collection getNets ()
    {
        return Collections.EMPTY_SET;
    }

    protected Token getFilename()
    {
        if (file == null)
        {
            Token t=new ArbitraryString(stringFile);
            return t;
        }
        else
        {
            assert stringFile == null:"Can't have both stringFile: "+stringFile+" and file: "+file+" in IncludeStatement";
            try
            {
                String path = file.getCanonicalPath();
                path = VarFilename.parse(path);
                
                Token t = new ArbitraryString(path);
                return t;
            }
            catch (IOException e)
            {
                throw new BadIncludeFileException("Could not get path of include file " + file + " " + e.getMessage());
            }
        }
    }

    public String toString ()
    {
        return lexicalify().toString();
    }
    

    public static class BadIncludeFileException extends RuntimeException
    {
        public BadIncludeFileException (String excep)
        {
            super(excep);
        }
    }
    
}// IncludeStatement
