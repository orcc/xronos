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


import java.util.*;

/**
 * IDDb. Keeps small ids allocated.
 *
 * @author C. Schanck
 * @version $Id: IDDb.java 2 2005-06-09 20:00:48Z imiller $
 */
public class IDDb
{
    private static final String _RCS_ = "$Rev: 2 $";

    private HashMap idMap=new HashMap();
    private HashMap typeMap=new HashMap();

    /**
     * Return the next id
     *
     * @return allocated id
     */
    public long getNextID(String type)
    {
        Entry e=(Entry)idMap.get(type);
        if(e==null)
        {
            e=new Entry();
            idMap.put(type,e);
        }
        
        return e.getNextID();
    }

    /**
     * Get the default idType for the object in question
     *
     * @return idType
     */
    private static String discernDefaultIdType (Class c)
    {
        // get the FQN
        String temp=c.getName();
        // peel off just the class name
        int i=temp.lastIndexOf('.');
        if(i>=0)
            temp=temp.substring(i+1);
        return temp.toLowerCase();
    }

    public String getTypeName(Class c)
    {
        String s=(String)typeMap.get(c);
        if(s==null)
        {
            s=discernDefaultIdType(c);
            typeMap.put(c,s);            
        }
        return s;
    }
    
    static class Entry
    {
        private long nextID=0;
        
        public final long getNextID() { return nextID++; }
    }
    
}

