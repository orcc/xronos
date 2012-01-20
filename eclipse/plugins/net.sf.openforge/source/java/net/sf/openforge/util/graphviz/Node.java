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

import java.io.*;


/**
 * A node in a graph.
 *
 * @author  Stephen Edwards
 * @version $Id: Node.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Node extends GVObject
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    private String id;
    private Graph graph = null;


    /**
     * Constructs a Node.
     *
     * @param id the identifier of the node
     * @param shape the shape of the node (eg, "box")
     */
    Node (String id, String shape)
    {
        this.id = id;
        setAttribute("shape", shape);
    }

    /**
     * Gets the identifier of this node.
     */
    public String getId ()
    {
        return id;
    }

    /**
     * Gets the graph to which this node has been added, or null if
     * this node has not been added to a graph.
     */
    public Graph getGraph ()
    {
        return graph;
    }

    /**
     * Gets the name that should be used to identify this node
     * when used as the source of an edge.  By default, the same
     * as the identifier.
     */
    public String getEdgeSourceId ()
    {
        return getId();
    }

    /**
     * Gets the name that should be used to identify this node
     * as the target of an edge.  By default, the same as the
     * identifier.
     */
    public String getEdgeTargetId ()
    {
        return getId();
    }

    /**
     * Gets the shape of this node.
     */
    public String getShape ()
    {
        return getAttribute("shape");
    }

    /**
     * Gets the displayed label of this node.
     */
    public String getLabel ()
    {
        /*
         * A node's label attribute defaults to its id.
         */
        return getAttribute("label", getId());
    }

    /**
     * Gets the style of this node.
     */
    public String getStyle ()
    {
        return getAttribute("style");
    }

    /**
     * Sets the style of this node.
     */
    public void setStyle (String style)
    {
        setAttribute("style", style);
    }

    public void setFontSize (int size)
    {
        setAttribute("fontsize", Integer.toString(size));
    }

    void print (PrintWriter out)
    {
        out.print(id);
        out.print(" ");
        printAttributes(out);
        out.println(";");
    }
    
    /**
     * Sets the graph to which this node has been added.  Called by Graph.
     */
    void setGraph (Graph graph)
    {
        this.graph = graph;
    }
}
