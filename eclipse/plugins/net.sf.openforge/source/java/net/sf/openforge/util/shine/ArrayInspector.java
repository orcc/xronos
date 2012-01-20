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

import java.lang.reflect.*;
import java.io.*;

/**
 * Goal here is to return a DB of all data elements
 */
public class ArrayInspector
{
    static final String rcs_id = "RCS_REVISION: $Rev: 2 $";
    private Object myArray;
    private Class compType;
    
    public ArrayInspector(Object o)
    {
        reuse(o);
    }

    public ArrayInspector()
    { ; }
    
    public void reuse(Object o)
    {
        if(!o.getClass().isArray())
            throw new IllegalArgumentException("Must be array!");
        myArray=o;
        compType=o.getClass().getComponentType();
    }

    public int getElementCount()
    {
        return Array.getLength(myArray);
    }

    public Object getMyArray()
    {
        return myArray;
    }

    public boolean isElementRef(int eIndex)
    {
        return !compType.isPrimitive();
    }

    public boolean isElementArray(int eIndex)
    {
        Class type=getElementType(eIndex);
        if(type==null)
            return false;
        return type.isArray();
    }
    
    public Object getElementName(int eIndex)
    {
        return "#"+eIndex;
    }
    
    public Class getElementType(int eIndex)
    {
        if(compType.isPrimitive())
            return compType;
        Object obj=Array.get(myArray,eIndex);
        return obj==null?null:obj.getClass();
    }

    public Object getElementValue(int eIndex)
    {
        if(Array.get(myArray,eIndex)==null)
            return "<null>";
        if(compType.isPrimitive())
        {
            if (compType.equals(byte.class))
            {
                return Array.getByte(myArray,eIndex)+"";
            }
            else if (compType.equals(short.class))
            {
                return Array.getShort(myArray,eIndex)+"";
            }
            else if (compType.equals(int.class))
            {
                return Array.getInt(myArray,eIndex)+"";
            }
            else if (compType.equals(long.class))
            {
                return Array.getLong(myArray,eIndex)+"";
            }
            else if (compType.equals(float.class))
            {
                return Array.getFloat(myArray,eIndex)+"";
            }
            else if (compType.equals(double.class))
            {
                return Array.getDouble(myArray,eIndex)+"";
            }
            else if (compType.equals(char.class))
            {
                return Array.getChar(myArray,eIndex)+"";
            }
            else if (compType.equals(boolean.class))
            {
                return Array.getBoolean(myArray,eIndex)+"";
            }
        }
        return Array.get(myArray,eIndex);
    }

    public String toString()
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);

        pw.println("ArrayInspector: " + myArray.getClass().getName());

        for(int i=0;i<getElementCount();i++)
        {
            pw.println("\t"+i+": " + getElementType(i)+ " == " + getElementValue(i));
        }

        pw.flush();
        return bos.toString();
    }
    
    public static void main(String args[])
    {
        int[] test1 = { 2,4,6,8,10 };
        ArrayInspector ii1=new ArrayInspector(test1);
        System.out.println(ii1);
    }

}



