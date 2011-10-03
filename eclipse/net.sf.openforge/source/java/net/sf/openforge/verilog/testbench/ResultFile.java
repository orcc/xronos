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

package net.sf.openforge.verilog.testbench;


import java.io.*;

import net.sf.openforge.util.VarFilename;
import net.sf.openforge.verilog.model.*;

/**
 * ResultFile.java
 *
 *
 * <p>Created: Wed Oct 30 11:26:39 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ResultFile.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ResultFile
{
    private File file;
    private IntegerWire handle;
    private static int uniqueID = 0;
    
    public ResultFile (File file)
    {
        this.file = file;
        this.handle = new IntegerWire("FileHandle" + uniqueID++, 1);
    }

    public Statement init ()
    {
        String path;
        try 
        {
            path = this.file.getCanonicalPath();
        }
        catch (IOException e)
        {
            System.err.println("Internal error. Could not get path name for result file " + file + " using ''");
            path = "";
        }
        
        Statement state = new FStatement.FOpen(this.handle,
            new StringStatement(VarFilename.parse(path)));
        return state;
    }

    public Statement write (Statement toWrite)
    {
        return new FStatement.FWrite(this.handle, toWrite);
    }
    
}// ResultFile
