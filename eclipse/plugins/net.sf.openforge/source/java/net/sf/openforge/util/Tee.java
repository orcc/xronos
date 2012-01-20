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

package net.sf.openforge.util;

import java.io.*;
import java.util.*;

/**
 * The <code>Tee</code> class is a <code>PrintWriter</code> that
 * allows other <code>OutputStream</code>s and <code>Writer</code>s to
 * be added to it such that each output call made to this class will
 * be distributed to all the registered output classes.  This class is
 * usefull to split standard error and standard out to the user's
 * terminal and to a file.
 *
 * @author <a href="mailto:Jonathan.Harris@xilinx.com">Jonathan
 * C. Harris</a>
 * @version $Id
 */
public class Tee extends PrintWriter
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    private PrintWriter[] writers = new PrintWriter[0];
    private boolean error = false;
    private boolean verbose = true;
    private HashSet nonVerboseWriters = new HashSet(11);
    private HashMap inputWritersToPrintWriters = new HashMap(11);
    
        
    public Tee(OutputStream os)
    {
        super(os);
        add(os);
    }

    public Tee(OutputStream os, boolean autoflush)
    {
        super(os,autoflush);
        add(os,autoflush);
    }

    public Tee(Writer out)
    {
        super(out);
        add(out);
    }

    public Tee(Writer out, boolean autoflush)
    {
        super(out,autoflush);
        add(out,autoflush);
    }
    
    public void add(PrintWriter pw)
    {
        if(pw == null)
            return;
        
        // Make sure we haven't been given a duplicate
        for(int i=0; i<writers.length; i++)
        {
            if(writers[i].equals(pw))
                return;
        }
        
        PrintWriter[] tmp = new PrintWriter[writers.length + 1];

        for(int i=0; i<writers.length; i++)
        {
            tmp[i] = writers[i];
        }

        tmp[tmp.length - 1] = pw;

        writers = tmp;

        inputWritersToPrintWriters.put(pw,pw);
    }

    public void add(Writer w)
    {
        if(w == null)
            return;

        PrintWriter pw = new PrintWriter(w);

        add(pw);
        inputWritersToPrintWriters.put(w,pw);
    }

    public void add(Writer w, boolean autoflush)
    {
        if(w == null)
            return;

        PrintWriter pw = new PrintWriter(w,autoflush);
        add(pw);
        inputWritersToPrintWriters.put(w,pw);
    }
    
    public void add(OutputStream os)
    {
        if(os == null)
            return;
        
        PrintWriter pw = new PrintWriter(os);
        add(pw);
        inputWritersToPrintWriters.put(os,pw);
    }

    public void add(OutputStream os, boolean autoflush)
    {
        if(os == null)
            return;

        PrintWriter pw = new PrintWriter(os,autoflush);
        add(pw);
        inputWritersToPrintWriters.put(os,pw);
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    public void setWriteOnNotVerbose(Object obj)
    {
        // Lookup the internal object we created for what they gave us
        Object o = inputWritersToPrintWriters.get(obj);
        nonVerboseWriters.add(o);
    }

    private boolean isWriteOnNotVerbose(Object obj)
    {
        return(nonVerboseWriters.contains(obj));
    }
    
    

    public boolean checkError()
    {
        boolean result = error;
        
        for(int i=0; i<writers.length; i++)
            result |= writers[i].checkError();

        return result;
    }
    
    public void close() 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].close();
    }
    
    public void flush() 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].flush();
    }
    
    public void print(boolean b) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(b);
    }
    
    public void print(char c) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(c);
    }
    
    public void print(char[] s) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(s);
    }
    
    public void print(double d) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(d);
    }
    
    public void print(float f) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(f);
    }
    
    public void print(int in) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(in);
    }
    
    public void print(long l) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(l);
    }
    
    public void print(Object obj) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(obj);
    }
    
    public void print(String s) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].print(s);
    }
    
    public void println() 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println();
    }
    
    public void println(boolean x) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    public void println(char x)
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    public void println(char[] x) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    public void println(double x) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    public void println(float x) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    public void println(int x) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    public void println(long x) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    public void println(Object x) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    public void println(String x) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].println(x);
    }
    
    protected void setError() 
    {
        error = true;
    }
    
    public void write(char[] buf) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].write(buf);
    }
    
    public void write(char[] buf, int off, int len) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].write(buf,off,len);
    }
    
    public void write(int c) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].write(c);
    }
    
    public void write(String s) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].write(s);
    }
    
    public void write(String s, int off, int len) 
    {
        for(int i=0; i<writers.length; i++)
            if(verbose || isWriteOnNotVerbose(writers[i]))
                writers[i].write(s,off,len);
    }
}








