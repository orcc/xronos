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
package net.sf.openforge.report;

import java.util.*;

import net.sf.openforge.lim.*;


/**
 * DesignResource is responsible to report all resources in a Design
 *
 * @author ysyu
 * @version $Id: DesignResource.java 132 2006-04-10 15:43:06Z imiller $
 */
public class DesignResource extends ResourceBank
{
    private final static String _RCS_ = "$Rev: 132 $";
    
    /** The design that owns the resources */
    private Design design = null;

    public DesignResource(Design design)
    {
        super();
        this.design = design;
    }

    /**
     * Proceduces a mapping of resource counts by total up all the resources
     * in task(s) of this design.
     *
     * @return a mapping of resources to their counts in this design
     */
    public Map generateReport()
    {
        Map total = new HashMap();
        for(Iterator iter = getResources().iterator(); iter.hasNext();)
        {
            TaskResource tResource = (TaskResource)iter.next();
            Map tReport = tResource.generateReport();
            for(Iterator riter = tReport.keySet().iterator(); riter.hasNext();)
            {
                Class klass = (Class)riter.next();
                if(total.containsKey(klass))
                {
                    Set left = (Set)total.get(klass);
                    Set right = (Set)tReport.get(klass);
                    Set combined = new HashSet();
                    combined.addAll(left);
                    combined.addAll(right);
                    total.put(klass, combined);
                }
                else
                {
                    total.put(klass, tReport.get(klass));
                }
            }
        }
        return total;
    }

    /**
     * @return the design that owns these resources.
     */
    public Design getDesign()
    {
        return this.design;
    }
}

        
