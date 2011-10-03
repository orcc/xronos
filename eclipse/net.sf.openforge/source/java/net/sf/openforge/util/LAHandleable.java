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

package net.sf.openforge.util;



/**
 * LAHandleable. Interface which must be implemented for the listener.
 *
 * @author C. Schanck
 * @version $Id: LAHandleable.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface LAHandleable
{
    static final String _RCS_ = "$Rev: 2 $";

    /**
     * Every LAHandleable object must implement at least this.
     *
     * @param domainTag domain tag. Will never be null. May be Empty String to represet null.
     * @param arg Any object which was passed in when the event fired. May be null
     */
    public void laListen(Object domainTag,Object arg);
}
