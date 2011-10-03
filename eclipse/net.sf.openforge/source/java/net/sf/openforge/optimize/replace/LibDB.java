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

package net.sf.openforge.optimize.replace;


import java.util.*;
import java.net.*;

import net.sf.openforge.app.*;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.frontend.xlim.app.XLIMEngine;
import net.sf.openforge.lim.*;
import net.sf.openforge.optimize.*;

/**
 * LibDB is a class which builds and stores the Design created for
 * each Library that has been searched.  When an instance of this
 * class is queried for a given library, the map of libraries is
 * searched to return the previously built implementation if it
 * exists.  If it does not, then it is build from its source.  This
 * class also ensures that the implementation is only ever merged with
 * another design one time.
 *
 * <p>Created: Wed Apr  9 12:29:18 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: LibDB.java 2 2005-06-09 20:00:48Z imiller $
 */
public class LibDB
{
    private static final String _RCS_ = "$Rev: 2 $";

    /** A map of String->Status for each library that has been
     * built. */
    private Map nameToStatus = new HashMap();

    /** The Status instance that we use for any library which could
     * not be found. */
    private static Status NULL_STATUS = new Status(null);

    public LibDB ()
    {
    }

    /**
     * Retrieves the {@link Design} which is the implementation of the
     * named (fully qualified) library source.
     *
     * @param libName a fully qualified String name of the library.
     * @return a {@link Design}
     */
    public Design getDesign (String libName)
    {
        Status status = (Status)nameToStatus.get(libName);
        if (status == null)
        {
            Design design = buildDesignFor(libName);
            if (design != null)
            {
                status = new Status(design);
            }
            else
            {
                status = NULL_STATUS;
            }
            this.nameToStatus.put(libName, status);
        }
        return status.getDesign();
    }

    /**
     * Merges the named library Design into the specified design, or
     * does nothing if the library was already merged.
     *
     * @param libName a value of type 'String'
     * @param top a value of type 'Design'
     */
    public void mergeDesigns (String libName, Design top)
    {
        assert this.nameToStatus.containsKey(libName) : "Merge error: Library " + libName + " not previously fetched from database.";
        assert this.nameToStatus.get(libName) != NULL_STATUS : "Merge error: Library " + libName + " could not be instantiated.";
        Status status = (Status)this.nameToStatus.get(libName);
        if (!status.isMerged())
        {
            status.mergeWith(top);
        }
//         else
//         {
//             System.out.println("Library " + libName + " already merged");
//         }
    }


    /**
     * Builds a new and independent Design which represents the given
     * library (source file).
     *
     * @param library a value of type 'String'
     * @return a value of type 'Design'
     */
    private Design buildDesignFor (String library)
    {
        String[] sourcePath = EngineThread.getGenericJob().getLibrarySourcePath();

        LibraryResource lr = new LibraryResource(
            Collections.singletonList(library), sourcePath);
        URL url = lr.getResourceURL(library);

        Design design = null;
        if (url != null)
        {
            EngineThread.getGenericJob().info("Building library from source " + url);

            Engine oldEngine = EngineThread.getEngine();
            GenericJob oldGenJob = EngineThread.getGenericJob(); // save old job

            GenericJob newGenJob = new GenericJob();
            Option option;
            Option newOption;
            //Copy the optionsMap in the old GenericJob to this new GenericJob.
            for(Iterator it = oldGenJob.optionsMap.values().iterator(); it.hasNext(); )
            {
                option = (Option)it.next();
                newOption = newGenJob.getOption(option.getOptionKey());
                newOption.replaceValue(CodeLabel.UNSCOPED, option.getValue(CodeLabel.UNSCOPED));
            }

            // need to turn off blockio for the library
            option = newGenJob.getOption(OptionRegistry.NO_BLOCK_IO);
            option.replaceValue(CodeLabel.UNSCOPED, "true");

            // need to turn off ENTRY for the library
            option = newGenJob.getOption(OptionRegistry.ENTRY);
            option.replaceValue(CodeLabel.UNSCOPED, "");

            // Set the replacement level to 0 so that we do not try to
            // perform operator replacement while compiling the
            // library.  If operation replacement is necessary, then
            // it will happen in the next pass of this visitor.
            option = newGenJob.getOption(OptionRegistry.OPERATOR_REPLACEMENT_MAX_LEVEL);
            option.replaceValue(CodeLabel.UNSCOPED, new Integer(0));

            try
            {
                option = newGenJob.getOption(OptionRegistry.TARGET);
                option.replaceValue(CodeLabel.UNSCOPED, url.getFile());
            }
            catch (NewJob.InvalidOptionValueException e)
            {
                oldGenJob.error("Library files must have the expected source language extension (ie: .c)");
                oldGenJob.error("Library file "+url.getFile()+" does not meet this requirement and can not be used.");
                return null;
            }
            
            // replace the entry to be libfile:- which means all valid
            // entry functions in the library file
            option = newGenJob.getOption(OptionRegistry.ENTRY);
            option.replaceValue(CodeLabel.UNSCOPED, ":-");
            
            
            Engine libEngine = null; // says this thread no belongs to this new job
            /*
              if (url.getFile().endsWith(".java"))
              {
              libJob = new JJob(proj);
              }
              else
            */
            if (url.getFile().endsWith(".xlim"))
            {
                libEngine = new XLIMEngine(newGenJob);
            }
            else
            {
                oldGenJob.error("Unknown library file extension " + url);
            }
            // if you want to turn off logging in this build now, TEMPRORARILY reset the log level to off
            // 1. Level oldLevel=Job.project().getLogger().getLevel();
            // 2. Job.project().getLogger().setLevel(Level.OFF);
            design = libEngine.buildLim(); // builds lim

            // Optimize the lim so that we get sizes propagated
            // throughout it.  Needed for doing the matching.
            Optimizer opts = new Optimizer();
            opts.optimize(design);

            // 3.Job.project().getLogger().setLevel(oldLevel);
            libEngine.kill(); // gets rid of this job
            oldEngine.updateJobThread(); // reassigns old job
        }
        else
        {
            EngineThread.getGenericJob().warn("Library source file " + library + " not found");
        }
        return design;
    }

    /**
     * A convenience class for tracking the library design and whether
     * it has been merged yet.
     */
    private static class Status
    {
        private Design libDesign = null;
        private boolean addedToDesign = false;

        public Status (Design libDesign)
        {
            this.libDesign = libDesign;
        }

        public Design getDesign ()
        {
            return this.libDesign;
        }

        public void mergeWith (Design design)
        {
            assert !isMerged() : "Already merged!";
            if (!isMerged())
            {
                design.mergeResources(this.libDesign);
                this.addedToDesign = true;
            }
        }

        public boolean isMerged ()
        {
            return this.addedToDesign;
        }
    }

}// LibDB
