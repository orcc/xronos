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

package net.sf.openforge.verilog.pattern;

import java.lang.RuntimeException;

/**
 * UnknownRegConfigurationException is an exception that can be
 * thrown to indicate that the configuration of a Reg cannot be
 * recognized and inferred.
 *
 * <p>Created: Wed Dec 10 04:08:40 2003
 *
 * @author imiller
 * @version $Id: UnknownRegConfigurationException.java 2 2005-06-09 20:00:48Z imiller $
 */
public class UnknownRegConfigurationException extends RuntimeException 
{
    private static final String _RCS_ = "$Rev: 2 $";

    public UnknownRegConfigurationException ()
    {
    }
    
}// UnknownRegConfigurationException
