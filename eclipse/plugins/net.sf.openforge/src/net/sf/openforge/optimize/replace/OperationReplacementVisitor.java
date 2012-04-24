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
import java.io.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.optimize.Optimization;
import net.sf.openforge.util.ForgeResource;

/**
 * OperationReplacementVisitor replaces operations with calls to
 * functionally equivalent methods.  This allows for the generation of
 * synthesizable implementations for nonsynthesizable operations such
 * as div, rem, and floating point operations.  Iterative multipliers
 * and/or bitserial arithmetic may also be implemented.  The
 * implementation libraries are taken from the option
 * {@link OptimizeDefiner#OPERATOR_REPLACEMENT_LIBS} for the given
 * Component.  The library is found according to the rules defined in
 * the {@link LibraryResource} class.
 * <p><b>The replacement depends on correct name of method, sufficient
 * size of args/result as obtained from the JMethod model for each top
 * level call's procedure.  When additional source languages are added
 * the models will need to implement an interface to report these
 * characteristics. </b>
 *
 * <p>Created: Thu Aug 29 09:19:59 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OperationReplacementVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class OperationReplacementVisitor extends ReplacementVisitor implements Optimization
{
    private static final String _RCS_ = "$Rev: 2 $";

    /**
     * The default libs are only created (extracted from jar file) the
     * first time that we encounter a divide or remainder operation.
     */
    private static List defaultLibraries = null;
    
    /**
     * Applies this optimization to a given target.
     *
     * @param target the target on which to run this optimization
     */
    public void run (Visitable target)
    {
        target.accept(this);
    }

    /**
     * Looks for any {@link Operation} which has a
     * {@link ReplacementCorrelation} defined and attempts to replace
     * that {@link Operation} with a library defined implementation
     * according to the context sensitive library search order.
     *
     * @param comp a value of type 'Operation'
     */
    public void filter (Operation op)
    {
        super.filter(op);

        if (ReplacementCorrelation.isReplaceable(op))
        {
            Match match = getImplementation(op);
            
            if (match != null)
            {
                replace(op, match);
            }
        }
    }
    
    /**
     * Ensures that the operation for which an implementation is being
     * retrieved is not more complex (according to
     * {@link ReplacementCorrelation#getComplexityRank}) than the max
     * complexity level specified by the preference
     * {@link OptionDB#OPERATOR_REPLACEMENT_MAX_LEVEL}.  Then, if the
     * complexity is allowable, the libraries to be searched are
     * retrieved.  If the libraries are the default libraries, and the
     * component is not one which we know exists in the default
     * libraries, null is returned.  Otherwise the libraries are
     * searched for an implementation.
     *
     * @param op an 'Operation' which implements the
     * {@link Replaceable} interface
     * @return the Call from the Design to the found method or null if
     * none found.
     */
    private Match getImplementation (Operation op)
    {
        Option option;
        option = op.getGenericJob().getOption(OptionRegistry.OPERATOR_REPLACEMENT_LIBS);
        List libs = ((OptionList)option).getValueAsList(op.getSearchLabel());
        option = op.getGenericJob().getOption(OptionRegistry.OPERATOR_REPLACEMENT_MAX_LEVEL);
        int maxReplaceLevel = ((OptionInt)option).getValueAsInt(op.getSearchLabel());
        ReplacementCorrelation correlation = ReplacementCorrelation.getCorrelation(op);

        // Don't do the replacement if the rank is higher than our
        // allowable level.
        if (correlation.getComplexityRank() > maxReplaceLevel)
        {
            return null;
        }
        
        //
        // Test to see if the user has changed the libraries from
        // their default setting.  If they haven't (most cases) then
        // we know that we can only replace for a DIV, a MOD, or
        // floats so return null if it isn't one of those components.
        // THIS CODE MUST BE CHANGED IF YOU CHANGE THE DEFAULTS IN
        // OptimizeDefiner.
        //
        final ReplacementCorrelation divCorr = ReplacementCorrelation.getCorrelation(DivideOp.class);
        final ReplacementCorrelation remCorr = ReplacementCorrelation.getCorrelation(ModuloOp.class);
        final boolean isDivRem = false;//(divCorr == correlation || remCorr == correlation);
        
        if (libs.isEmpty())
        {
            if (!isDivRem && !op.isFloat())
            {
                return null;
            }
        }

        // Append the default libraries to the end of the list, but
        // only if unique and only if they are applicable to the
        // operation at hand.
        List runLibs = new ArrayList(libs);
        if (isDivRem)
        {
            List defaultLibs = getDefaultLibs();
            for (Iterator iter = defaultLibs.iterator(); iter.hasNext();)
            {
                Object o = iter.next();
                if (!runLibs.contains(o))
                {
                    runLibs.add(o);
                }
            }
        }

        return getImplementationFromLibs(runLibs, op);
    }

    /**
     * Reports, via {@link Job#info}, what optimization is being
     * performed
     */
    public void preStatus ()
    {
        EngineThread.getGenericJob().info("performing operation substitution...");
    }
    
    /**
     * Reports, via {@link Job#verbose}, the results of <b>this</b>
     * pass of the optimization.
     */
    public void postStatus ()
    {
        EngineThread.getGenericJob().verbose("replaced " + getReplacedNodeCount() + " operations");
    }

    private List getDefaultLibs ()
    {
        if (OperationReplacementVisitor.defaultLibraries == null)
        {
            List list = new ArrayList();
            try
            {
                InputStream divIS = ForgeResource.loadForgeResourceStream("OP_REPLACE_DIV");
                File divFile = writeTempFile(divIS, "div");
                list.add(divFile.getAbsolutePath());
            }
            catch (IOException e)
            {
                EngineThread.getGenericJob().warn("Could not create default divide library.  Divide will not be synthesizable.");
            }
            
            try
            {
                InputStream remIS = ForgeResource.loadForgeResourceStream("OP_REPLACE_REM");
                File remFile = writeTempFile(remIS, "rem");
                list.add(remFile.getAbsolutePath());
            }
            catch (IOException e)
            {
                EngineThread.getGenericJob().warn("Could not create default divide library.  Divide will not be synthesizable.");
            }
            
            OperationReplacementVisitor.defaultLibraries = Collections.unmodifiableList(list);
        }
        
        return OperationReplacementVisitor.defaultLibraries;
    }
    
    // JWJFIXME: make this language independent, allow temp files with types other than ".c"

    private File writeTempFile (InputStream stream, String name) throws IOException
    {
        File outFile = File.createTempFile(name, ".c");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        PrintStream ps = new PrintStream(new FileOutputStream(outFile));
        while (reader.ready())
        {
            ps.println(reader.readLine());
        }
        ps.close();
        outFile.deleteOnExit();

        return outFile;
    }
    
    
}// OperationReplacementVisitor








