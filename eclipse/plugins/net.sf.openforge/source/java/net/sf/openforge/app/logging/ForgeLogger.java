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

import java.util.*;
import java.io.*;
import java.util.logging.*;

/**
 * A ForgeLogger object, typically 1 per Project

 * @author <a href="cschanck@xilinx.com">CRSS</a>
 * @version $Id: ForgeLogger.java 2 2005-06-09 20:00:48Z imiller $
 */

public class ForgeLogger
{
    private static final String _RCS_ = "$Rev: 2 $";
 
    public static final PrintStream consoleOut=System.out;
    public static final PrintStream consoleErr=System.err;
    public static final InputStream consoleIn=System.in;
    
    private static String newline=System.getProperty("line.separator");
    private int indentCount=0;
    private String indentionString="    ";
    private String preface=null;
    private Logger logger;
    private LinkedList prefaces=new LinkedList();
    private static HashMap allocatedHandlers=new HashMap();
    
    public ForgeLogger(String name)
    {
        logger=Logger.getLogger(name);
        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);
    }

    HashSet handlers = new HashSet();
    
    /**
     * Add a loggin handler to this logger
     *
     * @param h a value of type 'Handler'
     */
    public void addHandler(Handler h)
    {
        if (!handlers.contains(h))
        {
            logger.addHandler(h);
            handlers.add(h);
        }
    }
    
    public Logger getRawLogger() { return logger; }

    private static Handler getCachedHandler(Object key)
    {
        return (Handler)allocatedHandlers.get(key);
    }

    private static void cacheHandler(Object key,Handler h)
    {
        allocatedHandlers.put(key,h);
    }
    
    public static Handler getStreamHandler(OutputStream os)
    {
        Handler h=getCachedHandler(os);
        if(h==null)
        {
            h=new ForgeStreamHandler(os,new ForgeLogFormatter());
            cacheHandler(os,h);
        }
        return h;
    }

    public static Handler getFileOutputHandler(String filename)
    {
        Handler h=getCachedHandler(filename);
        if(h==null)
        {
            try
            {
                h=new ForgeStreamHandler(new FileOutputStream(filename),new ForgeLogFormatter());
                cacheHandler(filename,h);
            }
            catch(IOException ioe)
            {
                System.err.println("Unable to create forge.log");
                return getStreamHandler(consoleOut);
            }
        }
        return h;
    }

    /**
     * Set the level at which messages are displayed for this logger
     *
     * @param l a value of type 'Level'
     */
    public void setLevel(Level l)
    {
        logger.setLevel(l);
    }

    /**
     * Set the level at which messages are displayed for this logger
     *
     * @param l a value of type 'Level'
     */
    public Level getLevel()
    {
        return logger.getLevel();
    }

    /**
     * Get the current preface for logging
     *
     * @return current preface or null if there is none
     */
    public String getPreface()
    {
        try
        {
            return (String)prefaces.getFirst();
        }
        catch(NoSuchElementException e)
        {
            return null;
        }
    }

    private int cachedIndentCount=(-1);
    private String cachedIndentString="";
    public String getIndent()
    {
        if(cachedIndentCount!=indentCount)
        {
            cachedIndentString="";
            for(int i=0;i<indentCount;i++)
                cachedIndentString=cachedIndentString+indentionString;
            cachedIndentCount=indentCount;
        }
        return cachedIndentString;
    }

    // this is the basic user visible logging facility
    /**
     * Push a new preface onto th stack. This will be used
     * as the preface until popped or cleared
     *
     * @param p a value of type 'String'
     */
    public void pushPreface(String p)
    {
        // push this preference on top of stack
        prefaces.addFirst(p);
    }

    /**
     * Remove the current preface from the stack
     *
     */
    public void popPreface()
    {
        // pop one prefaces
        try
        {
            prefaces.removeFirst();
        }
        catch(NoSuchElementException e)
        {
        }
    }

    /**
     * Wipe away all prefaces
     *
     */
    public void clearPreface()
    {
        prefaces.clear();
    }    
    
    public void inc()
    {
        indentCount++;
    }

    public void dec()
    {
        indentCount=Math.max(0,indentCount-1);
    }

    public void decAll()
    {
        indentCount=0;
    }
    
    public void info(String s)
    {
        log(Level.INFO,null,s);
    }

    public void verbose(String s)
    {
        log(Level.FINE,null,s);
    }

    public void warn(String s)
    {
        log(Level.WARNING,null,s);
    }

    public void error(String s)
    {
        log(Level.SEVERE,null,s);
    }
    
    public void raw_warn(String s)
    {
        log(ForgeLevel.RAW_WARNING,null,s);
    }

    public void raw_error(String s)
    {
        log(ForgeLevel.RAW_SEVERE,null,s);
    }
    
    public void info(Object token,String s)
    {
        log(Level.INFO,token,s);
    }

    public void verbose(Object token,String s)
    {
        log(Level.FINE,token,s);
    }
        
    public void raw_warn(Object token,String s)
    {
        log(ForgeLevel.RAW_WARNING,token,s);
    }

    public void raw_error(Object token,String s)
    {
        log(ForgeLevel.RAW_SEVERE,token,s);
    }

    public void warn(Object token,String s)
    {
        log(Level.WARNING,token,s);
    }

    public void error(Object token,String s)
    {
        log(Level.SEVERE,token,s);
    }

    public void log(Level lev,String s)
    {
        log(lev,null,s);
    }

    public void log (Level lev,Object token,String s)
    {
        /*
         * We used to break up a multi-line message string into
         * separate log records here.  This functionality has
         * been moved into ForgeLogFormatter. This gives us the
         * ability to omit the prefix before a continuation line. --SGE
         */
        logger.log(lev, s, token);
    }

    // parsish
    // logstring: console|error|<fname>[=error|warn|info|verbose|off],...
    public void processLogString(String logString,String defaultLevel)
    {
        logger.setLevel(Level.FINE);
        // 1st, parse into comma sep parts
        ArrayList directives=new ArrayList();
        StringTokenizer st=new StringTokenizer(logString,"[],");
        while(st.hasMoreTokens())
        {
            String s=st.nextToken();
            directives.add(s);
        }

        // now, for each directive, try to parse into 2 parts
        for(Iterator it=directives.iterator();it.hasNext();)
        {
            String directive=(String)it.next();
            st=new StringTokenizer(directive,"=");

            String dest=st.nextToken().trim();
            String lev=defaultLevel; // default level
            if(st.hasMoreTokens())
                lev=st.nextToken();

            lev=lev.toLowerCase();
            Level level;
            level=getLevelFromString(lev);
            
            // ok, now add it. legal levels are: info, warn, verbose, erro
            // console,error are special filenames
            // else just use the filename
            Handler h;
            if(dest.toLowerCase().equals("console"))
            {
                h=ForgeLogger.getStreamHandler(consoleOut);
            }
            else if(dest.toLowerCase().equals("error"))
            {
                h=ForgeLogger.getStreamHandler(consoleErr);
            }
            else
            {
                h=getFileOutputHandler(dest);
            }
            h.setLevel(level);
            addHandler(h);
        }
    }

    public static Level getLevelFromString(String lev)
    {
        Level level;
        // convert to Level
        if(lev.equals("verbose"))
        {
            level=Level.FINE;
        }
        else if(lev.equals("info"))
        {
            level=Level.INFO;
        }
        else if(lev.equals("warn"))
        {
            level=Level.WARNING;
        }
        else if(lev.equals("error"))
        {
            level=Level.SEVERE;
        }
        else if(lev.equals("off"))
        {
            level=Level.OFF;
        }
        else
            throw new IllegalArgumentException("Legal log levels are: off,verbose,info,warn,error");
        return level;
    }
}







