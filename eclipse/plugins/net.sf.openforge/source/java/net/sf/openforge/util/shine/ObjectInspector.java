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

package net.sf.openforge.util.shine;

import java.util.*;

/**
 */
public class ObjectInspector
{
    static final String rcs_id = "RCS_REVISION: $Rev: 2 $";
    private ArrayInspector ai=new ArrayInspector();
    private InstanceInspector ii=new InstanceInspector();
    private boolean amArray=false;
    private Object myObject;
    
    // Abstract, Final, Interface, Native, Private, pRotected,
    // pUblic, Static, sYnchro, Transient, Volatile
    private String sortOrder="";
    
    public ObjectInspector(Object o)
    {
        reuse(o);
    }

    public void clear()
    {
        myObject=null;
    }
    
    public void reuse(Object o)
    {
        clear();
        myObject=o;
        amArray=o.getClass().isArray();
        if(amArray)
            ai.reuse(myObject);
        else
        {
            ii.reuse(myObject);
            ii.setSortOrder(sortOrder);
        }
    }

    public void setSortOrder(String order)
    throws IllegalArgumentException
    {
        ii.setSortOrder(order);
    }

    public int getCount()
    {
        if(amArray)
            return ai.getElementCount();
        else
            return ii.getFieldCount();
    }

    public Object getMyObject()
    {
        return myObject;
    }

    public boolean isArray()
    {
        return amArray;
    }

    public boolean isArray(int eIndex)
    {
        if(amArray)
            return ai.isElementArray(eIndex);
        else
            return ii.isFieldArray(eIndex);
    }
    
    public boolean isRef(int eIndex)
    {
        if(amArray)
            return ai.isElementRef(eIndex);
        else
            return ii.isFieldRef(eIndex);
    }
    
    public String getModifierString(int eIndex)
    {
        String pat=isRef(eIndex)?"*":"_";
        if(amArray)
            return pat;
        else
            return pat+ii.getModifierString(eIndex);
    }

    public Object getType(int eIndex)
    {
        Class type;
        if(amArray)
            type=ai.getElementType(eIndex);
        else
            type=ii.getFieldType(eIndex);
        return type==null?"<no type>":type.getName();
    }
    
    public Object getName(int eIndex)
    {
        if(amArray)
            return ai.getElementName(eIndex);
        else
            return ii.getFieldName(eIndex);
    }

    public Object getValue(int eIndex)
    {
        if(amArray)
            return ai.getElementValue(eIndex);
        else
            return ii.getFieldValue(eIndex);
    }

    public Object getValueString(int eIndex)
    {
        if(amArray)
            return ai.getElementValue(eIndex)+"";
        else
            return ii.getFieldValue(eIndex)+"";
    }

    
    public String toString()
    {
        if(amArray)
            return ai.toString();
        else
            return ii.toString();
    }

    public static void main(String args[])
    {
        HashMap hm=new HashMap(11);
        hm.put("Hello!","Goodbye");

        ObjectInspector ii1=new ObjectInspector(hm);
        System.out.println(ii1);
        int[] test1 = { 2,4,6,8,10 };
        ObjectInspector ii2=new ObjectInspector(test1);
        System.out.println(ii2);
    }
}



