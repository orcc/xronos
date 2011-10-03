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

package net.sf.openforge.lim.graph;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.util.graphviz.*;

/**
 * RegNode.java
 */
public class RegNode extends ComponentNode 
{
    private static final String _RCS_ = "$Rev: 280 $";

    private int stages = -1;
    
    RegNode (Reg component, String id,int fontSize)
    {
        super(component, id, fontSize);
    }

    protected String getBodyLabel ()
    {
        String label = super.getBodyLabel();
        final int type = ((Reg)getComponent()).getType();
        final String syncLabel = (((type & Reg.CLEAR) != 0) || ((type & Reg.PRESET) != 0)) ? "async":"sync";
        
        label += "\\n" + syncLabel;
        return label;
    }
    /**
     * Creates sub-nodes in a given bounding box for each port of the component.
     */
    protected void graphPorts (Record.Port boundingBox)
    {
        Reg reg = (Reg)getComponent();
        if (needPortGraph())
        {
            Record.Port entryBox = boundingBox.getPort(ENTRY);
            entryBox.setSeparated(false);
            graphPort(reg.getGoPort(), entryBox, "go", "G");
            net.sf.openforge.lim.Port reset = reg.getResetPort();
            if (reset.isConnected())
            {
                graphPort(reset, entryBox, "reset", "R");
            }
            net.sf.openforge.lim.Port clock = reg.getClockPort();
            if (clock.isConnected())
            {
                graphPort(clock, entryBox, "clock", "C");
            }

            int index = 0;
            for (Iterator iter = reg.getDataPorts().iterator(); iter.hasNext();)
            {
                net.sf.openforge.lim.Port port = (net.sf.openforge.lim.Port)iter.next();
                String pid = "din" + index;
                String plabel = "d" + index;
                if (port == reg.getDataPort())
                {
                    pid = "din";
                    plabel = "D";
                }
                else if (port == reg.getSetPort())
                {
                    pid = "set";
                    plabel = "set";
                }
                else if (port == reg.getInternalResetPort())
                {
                    pid = "ir";
                    plabel = "ir";
                }
                else if (port == reg.getEnablePort())
                {
                    pid = "en";
                    plabel = "en";
                }
//                 else if (port == reg.getPresetPort())
//                 {
//                     pid = "pre";
//                     plabel = "pre";
//                 }
//                 else if (port == reg.getClearPort())
//                 {
//                     pid = "clr";
//                     plabel = "clr";
//                 }
                else if (port == reg.getEnablePort())
                {
                    pid = "en";
                    plabel = "en";
                }

                if (port.isConnected())
                {
                    graphPort(port, entryBox, pid, plabel);
                }
                index++;
            }
        }
    }

    
}// RegNode
