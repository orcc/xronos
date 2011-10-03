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

package net.sf.openforge.backend.timedc;


import java.util.*;
import java.io.*;

import net.sf.openforge.lim.*;

/**
 * OpHandle is a class which handles maintaining unique String
 * identifiers for each Bus associated with a given {@link Component}
 * as well as ensuring that each identifier is only declared one
 * time. 
 *
 * <p>Created: Wed Mar  2 21:21:30 2005
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OpHandle.java 103 2006-02-15 21:35:17Z imiller $
 */
public class OpHandle 
{
    private static final String _RCS_ = "$Rev: 103 $";

    /** If set to true an ifdef will be written out with a printf for
     * each assignment that is handled here. */
    private static final boolean DBG = false;
    
    private Component comp;
    private Map busNames = new HashMap();
    private Set declaredBuses = new HashSet();
    
    public OpHandle (Component comp, CNameCache cache)
    {
        if (comp == null) { throw new IllegalArgumentException("Cannot construct a handle with a null component"); }
        
        this.comp = comp;

        String baseName = cache.getName(comp);
        int exitCount = 0;

        // Create a name for EVERY bus on the component, even though
        // we will only use some.
        for (Iterator iter = comp.getExits().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            List dataBuses = exit.getDataBuses();
            busNames.put(exit.getDoneBus(), baseName + "_" + exitCount + "_done");
            for (int i=0; i < dataBuses.size(); i++)
            {
                String name = baseName + "_" + exitCount + "_" + i;
                if (_timedc.db) _timedc.ln("\t" + name);
                busNames.put(dataBuses.get(i), name);
            }
            exitCount++;
        }
    }

    /**
     * Returns true if the bus has already been declared.
     */
    boolean isDeclared (Bus b, String postFix)
    {
        /* Debug of BusTuple.  Used to ensure that the bus tuple works
         * as a hashmap key
        BusTuple t = new BusTuple(b, postFix);
        System.out.println("Testing " + b + " and -" + postFix + "-" + t + " " + this.declaredBuses.contains(t));
        for (Iterator iter = this.declaredBuses.iterator(); iter.hasNext();)
        {
            Object o = iter.next();
            System.out.println(t + " " + o);
            System.out.println(t.equals(o) + " " + o.equals(t));
        }
        */
        return this.declaredBuses.contains(new BusTuple(b, postFix));
    }

    /**
     * Assigns the specified value to the bus and has the side-effect
     * of declaring the bus if it is not already declared.
     */
    public String assign (Bus b, String rightHandSide)
    {
        return assign(b, rightHandSide, "");
    }

    /**
     * Assigns the specified value to the bus and has the side-effect
     * of declaring the bus if it is not already declared, the bus
     * name is postpended with the postfix.
     */
    public String assign (Bus b, String rightHandSide, String postFix)
    {
        final String assignment = getBusDecl(b, postFix) + " = " + rightHandSide + ";";
        this.declaredBuses.add(new BusTuple(b, postFix));

        if (DBG)
        {
            String bname = getBusName(b, postFix);
            return assignment + "\n#ifdef DBG\nprintf(\"%x -> " + bname + "\\n\"," + bname + ");\n#endif";
        }
        return assignment;
    }

    /**
     * Gets a correct declaration for the specified bus with initial
     * value of 0.
     */
    public String declare (Bus b)
    {
        return declare(b, "");
    }
    
    /**
     * Gets a correct declaration for the specified bus, whose name is
     * postpended with the postfix, with initial value of 0.
     */
    public String declare (Bus b, String postFix)
    {
        return declare(b, "0", postFix);
    }
    
    /**
     * Gets a correct declaration for the specified bus with the
     * specified initial value.
     */
    public String declare (Bus b, String initValue, String postFix)
    {
        // This method is really just a lightweight cover for assign,
        // but it does check that the bus is not already declared.
        assert !isDeclared(b, postFix) : "Cannot double declare bus " + b + postFix;
        return assign(b, initValue, postFix);
    }
    
    protected String getBusDecl (Bus b)
    {
        return getBusDecl(b, "");
    }
    
    protected String getBusDecl (Bus b, String postFix)
    {
        final String prefix;
        if (isDeclared(b, postFix))
        {
            prefix = "";
        }
        else
        {
            prefix = getTypeDeclaration(b.getValue()) + " ";
        }
            
        return prefix + getBusName(b, postFix);
    }
    
    public String getBusName (Bus b)
    {
        return getBusName(b, "");
    }
    
    public String getBusName (Bus b, String postFix)
    {
        assert busNames.containsKey(b);
        String value = (String)busNames.get(b);
        return value + postFix;
    }

    /**
     * Irrevesibly replaces the generated name for the given bus with
     * the supplied value, as expected this will not affect any value
     * retrieved prior to the override.
     *
     * @param b a non-null Bus
     * @param name a non-null String
     * @throws IllegalArgumentException if b or name are null
     */
    public void overrideName (Bus b, String name)
    {
        if (b == null || name == null) { throw new IllegalArgumentException("Illegal use of null value in name override"); }
        
        assert busNames.containsKey(b);
        this.busNames.put(b, name);
    }

    public static String getTypeDeclaration (Value value)
    {
        return getTypeDeclaration(value.getSize(), value.isSigned());
    }
    
    /**
     * Returns the C data type necessary to cover the specified bit
     * width, ie char, short, int, or long long for 8, 16, 32, or 64
     * bits respectively.
     *
     * @param size a value of type 'int'
     * @return a value of type 'String'
     */
    public static String getTypeDeclaration (int size, boolean signed)
    {
        String ret;
        if (size <= 8) ret = "char";
        else if (size <= 16) ret = "short";
        else if (size <= 32) ret = "int";
        else ret = "long long";

        if (!signed)
        {
            ret = "unsigned " + ret;
        }
        
        return ret;
    }

    /**
     * This class is used as a key into the declaredbuses set.  MODIFY
     * WITH GREAT CAUTION!  The intent here is that any instance of
     * this class with the same combination of bus plus postfix string
     * will cause a match in declaredbuses.contains() method.
     */
    private static class BusTuple
    {
        private Bus b;
        private String postfix;

        public BusTuple (Bus b, String post)
        {
            this.b = b;
            this.postfix = post;
        }

        public int hashCode ()
        {
            return b.hashCode() + postfix.hashCode();
        }

        public boolean equals (Object o)
        {
            if (o instanceof BusTuple)
            {
                if (!this.b.equals(((BusTuple)o).b))
                    return false;
                if (!this.postfix.equals(((BusTuple)o).postfix))
                    return false;
                
                return true;
            }
            return false;
        }

        public String toString ()
        {
            return b.toString() + ":" + postfix;
        }
        
    }
    
}// OpHandle
