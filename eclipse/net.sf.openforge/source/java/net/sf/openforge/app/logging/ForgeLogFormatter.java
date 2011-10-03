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

import java.text.*;
import java.util.*;
import java.util.logging.*;

import net.sf.openforge.app.*;
import net.sf.openforge.app.project.*;
import net.sf.openforge.lim.CodeLabel;


public class ForgeLogFormatter extends java.util.logging.Formatter
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    Date date = new Date();
    DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    private static String NEWLINE = System.getProperty("line.separator");

    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format (LogRecord record)
    {
        StringBuffer buf = new StringBuffer();
        String message = record.getMessage();

        // here we do some cleverness...
        if (message.indexOf(NEWLINE) < 0)
        {
            if (message.indexOf("\n") < 0)
            {
                buf.append(formatLine(message, record, false));
            }
            else
            {
                boolean isContinuation = false;
                StringTokenizer st = new StringTokenizer(message, "\n");
                while (st.hasMoreElements())
                {
                    if (isContinuation)
                    {
                        buf.append("\n");
                    }
                    buf.append(formatLine(st.nextToken(), record, isContinuation));
                    isContinuation = true;
                }
            }
        }
        else
        {
            boolean isContinuation = false;
            StringTokenizer st = new StringTokenizer(message, NEWLINE);
            while (st.hasMoreElements())
            {
                if (isContinuation)
                {
                    buf.append("\n");
                }
                buf.append(formatLine(st.nextToken(), record, isContinuation));
                isContinuation = true;
            }
        }

        /*
         * If we're dealing with anything more severe than INFO, set it off
         * with a newline for emphasis.
         */
        if (record.getLevel().intValue() > Level.INFO.intValue())
        {
            buf.insert(0, "\n");
        }

        return buf.toString();
    }


    private String formatLine (String line, LogRecord record, boolean isContinuation)
    {
        StringBuffer stringBuffer = new StringBuffer();
                    
        boolean isRaw=((record.getLevel() == ForgeLevel.RAW_SEVERE)||
            (record.getLevel() == ForgeLevel.RAW_WARNING));
        /*
         * Prefix each line with a timestamp, but only if we're autogenerating
         * a testbench.  Not for user consumption.
         */
        final GenericJob gj = EngineThread.getGenericJob();
        boolean shouldATB = gj.getUnscopedBooleanOptionValue(OptionRegistry.AUTO_TEST_BENCH);
        boolean shouldTimeStamp = gj.getUnscopedBooleanOptionValue(OptionRegistry.SHOULD_TIME_STAMP);
        
        if(!isRaw)
        {
            if (gj != null && shouldATB && !isContinuation || shouldTimeStamp)
            {
                date.setTime(record.getMillis());
                stringBuffer.append(dateFormat.format(date));
            }
            
            String level = null;
            if (record.getLevel() == Level.WARNING)
            {
                level = "warning";       
            }
            if (record.getLevel() == Level.SEVERE)
            {
                level = "error";
                Object[] obj = record.getParameters();
                if (obj != null)
                {
                    for (int i = 0; i < obj.length; i++)
                    {
                        if (obj[i] instanceof ForgeFatalException)
                        {
                            level = "fatal error";
                            break;
                        }
                    }
                }
            }
            
            if (level != null && !isContinuation)
            {
                if (stringBuffer.length() > 0)
                {
                    stringBuffer.append(" ");
                }
                stringBuffer.append("*** ");
                stringBuffer.append(level);
            }
            
            //indention & preface        
            if (gj != null)
            {
                String s = gj.getLogger().getPreface();
                if (s != null)
                {
                    if (stringBuffer.length() > 0)
                    {
                        stringBuffer.append(" ");
                    }
                    stringBuffer.append("- ");
                    stringBuffer.append(s);
                }
                
                if (stringBuffer.length() > 0)
                {
                    stringBuffer.append(": ");
                }
                stringBuffer.append(gj.getLogger().getIndent());
            }
            else
            {
                if (stringBuffer.length() > 0)
                {
                    stringBuffer.append(": ");
                }
            }
        }

        // message
        stringBuffer.append(line);

        return stringBuffer.toString();
    }
}








