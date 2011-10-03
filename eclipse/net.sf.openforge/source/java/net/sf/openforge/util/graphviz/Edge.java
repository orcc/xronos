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
package net.sf.openforge.util.graphviz;


/**
 * An edge between two nodes in a graph.
 */
public class Edge extends GVObject
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    public static final String DIR_FORWARD = "forward";
    public static final String DIR_BACK    = "back";
    public static final String DIR_BOTH    = "both";
    public static final String DIR_NONE    = "none";

    public static final String STYLE_DASHED = "dashed";
    public static final String STYLE_DOTTED = "dotted";
    public static final String STYLE_BOLD   = "bold";
    public static final String STYLE_NONE   = "none";

    /**
     * Constructs a new Edge with a default weight.
     */
    public Edge ()
    {
        super();
    }

    /**
     * Constructs an edge with a given weight.
     */
    public Edge (int weight)
    {
        this();
        setWeight(weight);
    }

    /**
     * Sets the weight of this edge.
     */
    public void setWeight (int weight)
    {
        setAttribute("weight", Integer.toString(weight));
    }
    
    /**
     * Sets the direction of this edge, as indicated by
     * arrows at the ends.
     *
     * @param direction one of DIR_FORWARD, DIR_BACK, DIR_BOTH,
     *          or DIR_NONE
     */
    public void setDirection (String direction)
    {
        assert (direction.equals(DIR_FORWARD)
            || direction.equals(DIR_BACK)
            || direction.equals(DIR_BOTH)
            || direction.equals(DIR_NONE))
        : "Invalid Edge direction: " + direction;

        setAttribute("dir", direction);
    }
    
    /**
     * Sets the style of this edge.
     *
     * @param style one of STYLE_DASHED, STYLE_DOTTED, STYLE_BOLD,
     *          or STYLE_NONE
     */
    public void setStyle (String style)
    {
        assert (style.equals(STYLE_DASHED)
            || style.equals(STYLE_DOTTED)
            || style.equals(STYLE_BOLD)
            || style.equals(STYLE_NONE))
        : "Invalid Edge style: " + style;

        setAttribute("style", style);
    }
//     /**
//      * sets the port of the head and tail of the edge.  Port is the quadrant of the node that the edge connects to - there are _NO_ buses
//      * @param headPort
//      * @param tailPort are each one of n,ne,e,se,s,sw,w,nw
//      */
//     public void setNodePorts (String headPort, String tailPort)
//     {
//         setAttribute("headport",headPort);
//         setAttribute("tailport",tailPort);
//     }
}
