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

package net.sf.openforge.app;

import java.io.*;

import net.sf.openforge.app.logging.*;


public class Drain
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    // shared variables must be volatile!
    volatile boolean alive = true;

    public Drain(InputStream stream, ForgeLogger logger)
    {
        drain(stream, logger);
    }

    
    public boolean isAlive()
    {
        return alive;
    }

    /**
     * Starts a thread to keep a given text InputStream from filling up.
     */
    private void drain (final InputStream stream, final ForgeLogger logger)
    {
        new Thread()
            {
                public void run ()
                {
                    //System.out.println("Starting Drain: " + Thread.currentThread());
                    
                    InputStreamReader reader = new InputStreamReader(stream);
                    BufferedReader buffer = new BufferedReader(reader);
                    
                    try
                    {
                        while (true)
                        {
                            String nextLine = buffer.readLine();
                            if (nextLine == null)
                            {
                                break;
                            }
                            else
                            {
                                if (logger != null)
                                {
                                    logger.info(nextLine);
                                }
                            }
                        }
                    }
                    catch (IOException eIO)
                    {
                    }
                    
                    //System.out.println("Drain Dying: " + Thread.currentThread());
                    alive = false;
                }
                
            }.start();
    }   
}






