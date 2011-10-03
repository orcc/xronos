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
 * A plaintext node.
 *
 * @author  Stephen Edwards
 * @version $Id: PlainText.java 2 2005-06-09 20:00:48Z imiller $
 */
public class PlainText extends Node
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    /**
     * Constructs a new Plaintext node.
     *
     * @param id the identifier of the node
     */
    public PlainText (String id)
    {
        super(id, "plaintext");
    }
}
