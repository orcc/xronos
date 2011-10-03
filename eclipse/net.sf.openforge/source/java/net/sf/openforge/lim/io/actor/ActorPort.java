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

package net.sf.openforge.lim.io.actor;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;

/**
 * ActorPort is an interface which will be implemented by all the
 * various flavors of Actor ports and provides methods which are
 * needed to create the various types of accesses which are possible
 * on an Actor port.
 *
 *
 * <p>Created: Mon Aug 29 10:55:36 2005
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ActorPort.java 236 2006-07-20 16:57:10Z imiller $
 */
public interface ActorPort 
{
    public static final String _RCS_ = "$Rev: 236 $";

    //public static final int ATTR_SIMPLE_PORT_STYLE = 1;
    public static final int COUNT_PORT_WIDTH = 16;

    /**
     * Mechanism for configuring the behavior of the ActorPort based
     * on attribute values.
     *
     * @param type, an int value, one of the fields of the ActorPort
     * class.
     * @param value, any String
     */
    public void setAttribute (int type, String value);
    
    /**
     * Returns true if the Actor Port is an input interface.
     */
    public boolean isInput ();
    
    /**
     * Returns a data production/consumption access to the interface.
     * If isInput returns true the the access will be an data
     * consumption access, otherwise it will be a data production
     * access.
     *
     * @param blocking if set true the returned access is a blocking
     * access, otherwise it is a simple access of the data
     * (non-blocking).
     */
    public Component getAccess (boolean blocking);

    /**
     * Returns a component which tests the current number of tokens in
     * the channel.
     */
    public Component getCountAccess ();

    /**
     * Returns a component which peeks at the value at a depth into
     * the channel queue specified by an index port.
     */
    public Component getPeekAccess ();

    /**
     * Returns a component access to the status flag of the
     * interface.  
     */
    public Component getStatusAccess ();
    
}// ActorPort
