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

package net.sf.openforge.lim;


/**
 * Referee is a {@link Module} which control access to a {@link Referent}.
 *
 * @version $Id: Referee.java 13 2005-08-11 19:47:36Z imiller $
 */
public abstract class Referee extends Module
{
    private static final String _RCS_ = "$Rev: 13 $";
    
    /**
     * Constructs a Referee.
     *
     * @param referent the referent which this Referee will manage
     */
    public Referee (Referent referent)
    {
        this();
    }

    protected Referee ()
    {
        super();
    }
    
    /**
     * Throws an exception, replacement in this class not supported.
     */
    public boolean replaceComponent (Component removed, Component inserted)
    {
        throw new UnsupportedOperationException("Cannot replace components in " + getClass());
    }
    
    /**
     * Returns a copy of this Referee in which the referent has
     * <b>not</b> been cloned, so that the cloned referee still points
     * to the same referent.
     *
     * @return a copy of the Referee.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
    
}
