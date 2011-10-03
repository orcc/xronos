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

package net.sf.openforge.util.stats;


import java.util.*;

import net.sf.openforge.util.*;


/**
 * Root class for stats
 */
public abstract class Stat
{

    /** DOCUMENT ME! */
    static final String rcs_id = "RCS_REVISION: $Rev: 2 $";
    private String name;
    private String typeName;
    private SoftCollection.LL tokens = new SoftCollection.LL();

    /**
     * Creates a new Stat object. DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     * @param typeName DOCUMENT ME!
     */
    public Stat (String name, String typeName)
    {
        this.name = name;
        this.typeName = typeName;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public abstract String toString ();

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getName ()
    {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getTypeName ()
    {
        return typeName;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Collection getTokens ()
    {
        return tokens.getCollection();
    }

    /**
     * DOCUMENT ME!
     *
     * @param o DOCUMENT ME!
     */
    public void addToken (Object o)
    {
        tokens.getLinkedList().addLast(o);
    }
}
