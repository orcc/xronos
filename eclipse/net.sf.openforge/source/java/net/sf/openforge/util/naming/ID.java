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

package net.sf.openforge.util.naming;

import java.io.*;

import net.sf.openforge.util.HF;

/**
 * ID clasfile. Top level class for the backend of naming. This class is extended
 * by things that need consistent naming...
 *
 * @author C. Schanck
 * @version $Id: ID.java 107 2006-02-23 15:46:07Z imiller $
 */
public class ID implements HasIDSourceInfo
{
    private static final String _RCS_ = "$Rev: 107 $";

    private static final IDDb db=new IDDb();

    /**
     *
     * This is experimentally turned on as of 8/20/2002. It
     * is cleverer in that it attempts to keep a rolling id tab
     * per type. So the first created Bus is bus0, the second is
     * bus1, etc..
     */
    private static final boolean USE_SMALL_IDS=false;

    private String idType=null;
    private final long idNum;
    private String idLogical=null;
    private IDSourceInfo isSourceInfo=null;
    private ID original=null;

    // A String which is derived from the users source code, helpful
    // in constructing a name which is identifiable with the source.
    public String sourceName = null;
        
    private boolean hasExplicitName = false;
    
    /**
     * Copies all information except the global identifier from a source ID
     * to a target ID.  Useful when cloning IDs.
     */
    public static void copy (ID source, ID target)
    {
        target.idType = source.idType;
        target.idLogical = source.idLogical;
        target.original = source.originalID();
        target.sourceName = source.sourceName;
        
        try
        {
            target.isSourceInfo = (source.isSourceInfo == null
                ? null
                : (IDSourceInfo)source.isSourceInfo.clone());
        }
        catch (CloneNotSupportedException eClone)
        {
            throw new IllegalStateException(eClone.toString());
        }
    }

    /**
     * Default we like to use
     *
     */
    public ID()
    {
        idType=getDefaultIDType(this.getClass());
        
        if(USE_SMALL_IDS)
            idNum=ID.getNextID(idType);
        else
            idNum=System.identityHashCode(this);

        if(_naming.db) { _naming.ln("Allocated (1) object idType: "+ID.showGlobal(this)); }
    }
    
    /**
     * Returns the original ID from which this ID was derived (usually through
     * cloning).
     * 
     * @return ID
     */
    public ID originalID()
    {
        return (original == null) ? this : original;
    }
    
    /**
     * Return the string, no spaces 'type' for this object
     *
     * @return type for this class
     */
    public final String getIDGlobalType()
    {
        return idType;    
    }
    
    /**
     * Set the logical id for this object
     *
     * @param s new logical id
     */
    public void setIDLogical(String s)
    {
        this.idLogical=s;
        this.hasExplicitName = (s != null);
    }

    public void setSourceName (String s)
    {
        if (this.sourceName != null && s != null)
            throw new IllegalStateException("Cannot override the source name attribute of an ID old:" + this.sourceName + " new: " + s + " on " + this);
        this.sourceName = s;
    }

    public final String getSourceName ()
    {
        return this.sourceName;
    }
    
    /**
     * append the logical id to the showLogical of the container
     * @param container object to append the id to
     * @param id string to append
     */
    public void setIDLogical(Object container, String s)
    {
        setIDLogical(showLogical(container)+"_"+s);
    }
    
    /**
     * Get the IDSourceInfo block for this object. WIll not return null;
     * uses lazy allocation.
     *
     * @return source info block
     */
    public IDSourceInfo getIDSourceInfo()
    {
        if(isSourceInfo==null)
            isSourceInfo=new IDSourceInfo();
        return isSourceInfo;
    }
    
    /**
     * Set the source info for thisobject
     *
     * @param sinfo a value of type 'IDSourceInfo'
     */
    public void setIDSourceInfo(IDSourceInfo sinfo)
    {
        if(sinfo==null)
        {
            isSourceInfo=new IDSourceInfo();
        }
        else
        {
            isSourceInfo=sinfo;
        }
    }
    
    /**
     * Use this to get the global id for this object. 
     *
     * @return a value of type 'String'
     */
    public String showIDGlobal()
    {
        String s;
        if(USE_SMALL_IDS)
            s=Long.toString(idNum);
        else
            s=HF.hex(System.identityHashCode(this),8);
        return getIDGlobalType()+"["+s+"]";
    }
    
    /**
     * Use this to get the logical id for this object. If there is not
     * a logical id, it returns the global id.
     *
     * @return a value of type 'String'
     */
    public String showIDLogical()
    {
        String theId;
        if(this.idLogical!=null)
        {
            theId = this.idLogical;
        }
        else
        {
            theId = showIDGlobal();
        }

        if (this.sourceName != null)
            return theId + "_" + this.sourceName;
        return theId;
    }
    
    /**
     * Dump the debug info to the specified PrintStream. Handles nulls.
     *
     * @param ps a value of type 'PrintStream'
     */
    public void showIDDebug(PrintStream ps) 
    {
        ps.print(this.toString());
    }
    
    /**
     * Return the debug block as a String. Handles nulls
     *
     * @return a value of type 'String'
     */
    public String showIDDebug()
    {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        PrintStream ps=new PrintStream(baos);
        
        showIDDebug(ps);
        
        ps.flush();
        return baos.toString();
    }

    /**
     * Generates a string representation of where this ID comes from,
     * of the form:
     * <code><i>Name</i> at line <i>-1</i> in file <i>filename</i></code>
     *
     * @return a value of type 'String'
     */
    public String showIDLocation ()
    {
        IDSourceInfo info = getIDSourceInfo();
        String location = showIDLogical();
        if (info.getSourceLine() >= 0)
            location += " at line " + info.getSourceLine();
        else
            location += " at line <unknown>";

        if (info.getSourceFileName() != null)
            location += " in file " + info.getSourceFileName();
        else
            location += " in file <unknown>";
        
        return location.trim();
    }
    
    /**
     * Checks if this ID has been explicitly given a logical name
     * using setIDLogical().
     * 
     * @return true if a name has been set with setIDLogical(), false otherwise
     */
    public boolean hasExplicitName()
    {
        return hasExplicitName;
    }

    //
    // static versions, use on anything ....
    //
    
    /**
     * Use this to get the global id for any object. Handles nulls
     *
     * @param o Object
     * @return global identifier
     */
    public static String showGlobal (Object o)
    {
        if(o==null)
            return "[null]";
        try
        {
            ID id=(ID)o;
            return id.showIDGlobal();
        }
        catch(ClassCastException cce)
        {
            return getDefaultIDType(o.getClass())+"["+HF.hex(System.identityHashCode(o))+"]";
        }
    }

    /**
     * Helper -- same as showGlobal
     *
     * @param o a value of type 'Object'
     * @return a value of type 'String'
     */
    public static String glob(Object o) { return showGlobal(o); }
    public static String global(Object o) { return showGlobal(o); }
    
    /**
     * Use this to get the idLogical id for any object. If there is not
     * set idLogical id, it returns the global id. Handles nulls
     *
     * @param o a value of type 'Object'
     * @return a value of type 'String'
     */
    public static String showLogical (Object o)
    {
        if(o==null)
            return "[null]";
        try
        {
            ID id=(ID)o;
            return id.showIDLogical();
        }
        catch(ClassCastException cce)
        {
            return ID.showGlobal(o);
        }
    }

    /**
     * Helper -- same as showLogical
     *
     * @param o a value of type 'Object'
     * @return a value of type 'String'
     */
    public static String log(Object o) { return showLogical(o); }
    
    /**
     * Dump the debug info for any object to the specified PrintStream. Handles nulls.
     *
     * @param o a value of type 'Object'
     * @param ps a value of type 'PrintStream'
     */
    public static void showDebug (Object o,PrintStream ps)
    {
        if(o==null)
            ps.print("[null]");
        try
        {
            ID id=(ID)o;
            id.showIDDebug(ps);
        }
        catch(ClassCastException cce)
        {
            ps.print(o.toString());
        }
    }
    
    /**
     * Return the debug block as a String for any object. Handles nulls
     *
     * @param o a value of type 'Object'
     * @return a value of type 'String'
     */
    public static String showDebug (Object o)
    {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        PrintStream ps=new PrintStream(baos);
        
        showDebug(o,ps);
        
        ps.flush();
        return baos.toString();
    }

    // ------------------------------------------------
    // generic static methods
    // ------------------------------------------------
    
    public static long getNextID(String idType)
    {
        return db.getNextID(idType);
    }    
    
    private static String getDefaultIDType(Class c)
    {
        return db.getTypeName(c);
    }
    
    /**
     * This should push any arbitrary string into a Verilog legal identifier
     *
     * @param s a value of type 'String'
     * @return a value of type 'String'
     */
    public static String toVerilogIdentifier (String s)
    {
        // Only allow alphabets, digits, or underscores to name a Verilog
        // identifier. Any character besides previously mentioned is
        // eliminated or substitued by an underscore.
        
        char underscore = '_';
        String verilogId = "";
        char last_char = ' ';
        
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            
            if (Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isDigit(c))
            {
                verilogId += c;
                last_char = c;
            }
            else if (c == underscore)
            {
                if (last_char != underscore)
                {
                    verilogId += c;
                    last_char = c;
                }
            }
            else
            {
                verilogId += underscore;
                last_char = underscore;
            }
        }
        // make sure the first character is an alpha
        char c=s.charAt(0);
        if (Character.isDigit(c))
        {
            return "const"+verilogId;
        }

        // XST can't handle names that begin with _ in verilog even
        // though it is legal.
        if(verilogId.length() > 0)
        {
            if(verilogId.charAt(0) == '_')
            {
                if(verilogId.length() == 1)
                {
                    return("underscore");
                }
                else
                    return(toVerilogIdentifier(verilogId.substring(1)));
            }
        }
        
        return verilogId;
    }

}




