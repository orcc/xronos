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

package net.sf.openforge.app.logging;

import java.util.logging.*;
import java.io.*;


public class ForgeStreamHandler extends Handler
{
    private static final String _RCS_ = "$Rev: 424 $";

    private PrintWriter writer;
    private OutputStream out;
    
    /**
     * Create a <tt>StreamHandler</tt> with a given <tt>Formatter</tt>
     * and output stream.
     * <p>
     * @param output 	the target output stream
     * @param formatter   Formatter to be used to format output
     */
    public ForgeStreamHandler(OutputStream out, Formatter formatter)
    {
        this.out=out;
	setFormatter(formatter);
	writer=new PrintWriter(out,true);
    }

    /**
     * Flush any buffered messages.
     */
    public synchronized void flush() {
	try
	{
	    writer.flush();
            out.flush();
	}
	catch (Exception ex) {}
    }

    public synchronized void close() throws SecurityException {
	flush();
	writer.close();
    }

    public synchronized void publish(LogRecord record)
    {
	if (!isLoggable(record))
	{
	    return;
	}
	// we only log it if it has no parameters implementing interface
	// NoDefaultLogging. May be empty.
	// get parms
	Object[] parms=record.getParameters();
	if(parms!=null)
	{
	    // check each one. Short Circuit immediately
	    for(int i=0;i<parms.length;i++)
	    {
		if(parms[i] instanceof ForgeDefaultLoggable)
		{
		    // found one. skip out.
		    return;
		}
	    }
	}
	// if we get here, we have passed the no logging test
	try
	{
	    String msg = getFormatter().format(record);
	    writer.println(msg);
            writer.flush();
	} catch (Exception ex)
	{
	    // We don't want to throw an exception here, but we
	    // report the exception to any registered ErrorManager.
	    reportError(null, ex, ErrorManager.WRITE_FAILURE);
	}
    }
}





