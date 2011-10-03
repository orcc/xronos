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

package net.sf.openforge.backend.hdl.ucf;

import java.util.*;
import java.io.*;

public class UCFDocument
{

    List statements = new ArrayList();
    
    public UCFDocument()
    {
    }

    public void blank()
    {
        statements.add("");
    }

    protected void state(String statement)
    {
        statements.add(statement);
    }
    
    public void state(UCFStatement statement)
    {
        statements.add(statement);
    }
    
    public void comment(String comment)
    {
        statements.add(new UCFComment(comment));
    }

    public void comment(UCFStatement comment)
    {
        statements.add(new UCFComment(comment));
    }
    
    public void write(OutputStream os)
    {
        write(new PrintWriter(os));
    }
    
    public void write(Writer writer)
    {
        write(new PrintWriter(writer));
    }
    
    public void write(PrintWriter printer)
    {
        for (Iterator it=statements.iterator(); it.hasNext();)
        {
            printer.println(it.next().toString());
        }
        printer.flush();
    }
    
    public String toString()
    {
        StringWriter stringer = new StringWriter();
        write(stringer);
        return stringer.toString();
    }
}
