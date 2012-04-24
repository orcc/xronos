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

import net.sf.openforge.app.*;
import net.sf.openforge.app.project.*;
import net.sf.openforge.forge.api.ipcore.IPCore;
import net.sf.openforge.forge.api.pin.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.optimize.*;
import net.sf.openforge.util.naming.ID;

/**
 * ReplacementVisitor is the parent class of any visitor which will be
 * replacing operations based on Java implementations found in a
 * library (either Xilinx supplied or user supplied).  Methods are
 * included for finding the source file, compiling it into a design,
 * and then finding within that design an entry method that matches
 * naming/signature/type requirements.  An additional method is
 * supplied to perform the operator replacement.
 *
 * <p>Created: Thu Aug 29 09:19:59 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ReplacementVisitor.java 157 2006-06-28 19:34:48Z imiller $
 */
public class ReplacementVisitor extends ComponentSwapVisitor
{
    private static final String _RCS_ = "$Rev: 157 $";

    /** the top level design. */
    private Design topDesign = null;

    /** The DB which stores built library designs. */
    private LibDB libraryDB = new LibDB();

    /** A map of String IPCore name to Integer, used to assign a
     * unique instance name to each instance of an IPCore. */
    private Map nameUniquifier = new HashMap();

    /**
     * Iterates over the design, continually replacing replaceable
     * components until no new modifications are made.
     */
    public void visit (Design design)
    {
        this.topDesign = design;
        design.saveAPIContext(); // save off current statics
        design.clearAPIContext(); // clear them out...
        
        do
        {
            setModified(false);
            super.visit(design);
        } while (isModified());
        design.restoreAPIContext(); // restore the original api data
    }

    /**
     * Searches each library, looking at each task within each
     * library, in order to find a method named according to the
     * specified components replacement Method Name (as defined by
     * {@link ReplacementCorrelation#getReplacementMethodName} .  The
     * method may be overloaded in the library so we look for the
     * implementation whose result width is the closest to the width
     * of the component (but still over).  The additional constraint
     * is added that if the component returns true from
     * {@link Operation#isFloat} then the implementation must also be
     * floating point.
     * 
     * @param libs a List of String objects identifying fully
     * qualified library names (java classes).
     * @param op the Operation for which an implementation is to be
     * retrieved.
     * @return a Match object which contains a {@link Call} to the
     * uniquified implementation fetched from the library for the
     * given operation as well as other data necessary for handling
     * the replacement, or returns null if no suitable implementation
     * was found in the libraries.
     */
    protected Match getImplementationFromLibs (List libs, Operation op)
    {
        for (Iterator libIter = libs.iterator(); libIter.hasNext();)
        {
            String libname = (String)libIter.next();
            if (_optimize.db)_optimize.ln(_optimize.REPLACE,"Looking for library " + libname);
            
            Design libDesign = libraryDB.getDesign(libname);
            
            if (libDesign == null)// In case the library wasn't found.
            {
            	System.out.println("LIBRARY NOT FOUND !");
                continue;
            }

            ReplacementCorrelation correlation = ReplacementCorrelation.getCorrelation(op);
            String replaceMethodName = correlation.getReplacementMethodName();
            Task match = null;
            int matchWidth = 0;

            // Search each entry method for a match.
            for (Iterator iter = libDesign.getTasks().iterator(); iter.hasNext();)
            {
                Task task = (Task)iter.next();
                Call call = task.getCall();
                
                if (_optimize.db) _optimize.ln(_optimize.REPLACE, "Looking at call " + call.getProcedure().getName());

                Bus result = call.getBusFromProcedureBus((Bus)call.getProcedure().getBody().getExit(Exit.RETURN).getDataBuses().get(0));
                if (result == null)
                {// Allow the user to have a void method in their library.
                    if (_optimize.db) _optimize.ln(_optimize.REPLACE,
                                                   "  result bus is null, must be void return, skipping");
                    continue; 
                }
                
                int callResultWidth = result.getValue().getSize();

                // Look for entry methods whose name matches the name
                // reported by the component to be replaced AND whose
                // size is large enough to cover the ins/outs AND
                // which are the right type (float vs non float).

                if(call.getProcedure().getName().toUpperCase().startsWith(replaceMethodName.toUpperCase()))
                    //if (call.getProcedure().getName().equalsIgnoreCase(replaceMethodName))
                {
                    if (_optimize.db) _optimize.ln(_optimize.REPLACE,
                                                   "  name matches, checking sizes");
                    
                    //if (ensureSizes(op, call) && (call.isFloat() == op.isFloat()))
                    if (ensureSizes(op, call) && ensureFloat(op, call))
                    {
                        if (_optimize.db)
                            _optimize.ln(_optimize.REPLACE,"  sizes match, checking result width for best match");
                        
                        if (match == null || callResultWidth < matchWidth)
                        {
                            if (_optimize.db) _optimize.ln(_optimize.REPLACE, "\tBest Match");
                            match = task;
                            matchWidth = callResultWidth;
                        }
                    }
                    else
                    {
                        if (_optimize.db)
                            _optimize.ln(_optimize.REPLACE,"  sizes were not right");

                    }
                }else
                {
                    if (_optimize.db)
                        _optimize.ln(_optimize.REPLACE,"  call doesn't match expected name");
                }
            }
            
            if (match != null)
            {
                // Uniquify the procedure and create a new call to
                // return.  This should fix any naming issues.
                try
                {
                    Call cloneCall = (Call)match.getCall().clone();
                    Procedure clone = cloneCall.getProcedure();
                    
                    String name = ID.showLogical(clone);
                    clone.setIDLogical(name + "_" + ID.getNextID(name));
                    
                    return new Match(libname, libDesign, cloneCall, match.getMemoryKey());
                }
                catch (CloneNotSupportedException e)
                {
                    throw new RuntimeException("Could not clone procedure during operation replacement. " + e);
                }
            }
        } 
        // No match found in any lib.
        //if (libs.size() > 0) Job.warn("No match found for " + ID.showLogical(comp));
        return null;
    }    

    /**
     * Check that the component and call have exactly the same number
     * of data ports, exits, and data buses and that in each of the
     * corresponding pairs that the call's half is larger or same size
     * as the components half.
     * @return true if all call ports and buses are greater than or
     * equal to the components size.
     */
    private static boolean ensureSizes (Component comp, Call call)
    {
        List callDataPorts = new ArrayList(call.getDataPorts());
        // Skip the 'this' port if it exists on the call.
        callDataPorts.remove(call.getThisPort());

        // Same number of ports ?
        if (comp.getDataPorts().size() != callDataPorts.size())
        {
            if (_optimize.db) _optimize.ln(_optimize.REPLACE, "Not same number of data ports");
            return false;
        }

        // Same number of exits ?
        if (comp.getExits().size() != call.getExits().size())
        {
            if (_optimize.db) _optimize.ln(_optimize.REPLACE, "Not same number of exits");
            return false;
        }

        // Each port of call is large enough to handle data size ?
        for (Iterator compIter = comp.getDataPorts().iterator(),
             callIter = callDataPorts.iterator(); compIter.hasNext();)
        {
            Port compPort = (Port)compIter.next();
            Port callPort = (Port)callIter.next();
            if (compPort.getValue().isSigned() != callPort.getValue().isSigned())
            {
                if (_optimize.db) _optimize.ln(_optimize.REPLACE, "Call port and op port do not share signed attribute.  Call: " + call.cpDebug(false) + " comp: " + comp.cpDebug(false));
                return false;
            }
            if (callPort.getValue().getSize() < compPort.getValue().getSize())
            {
                if (_optimize.db) _optimize.ln(_optimize.REPLACE, "Call port smaller than comp port.  Call: " + call.cpDebug(false) + " comp: " + comp.cpDebug(false));
                return false;
            }
        }
        
        for (Iterator compIter = comp.getExits().iterator(),
             callIter = call.getExits().iterator(); compIter.hasNext();)
        {
            Exit compExit = (Exit)compIter.next();
            Exit callExit = (Exit)callIter.next();
            // Same number of buses ?
            if (compExit.getDataBuses().size() != callExit.getDataBuses().size())
            {
                if (_optimize.db) _optimize.ln(_optimize.REPLACE, "Call exit and comp exit have different number of buses");
                return false;
            }
            // Each bus of call is large enough to handle data size ?
            for (Iterator compExitIter = compExit.getDataBuses().iterator(),
                 callExitIter = callExit.getDataBuses().iterator(); compExitIter.hasNext();)
            {
                Bus compBus = (Bus)compExitIter.next();
                Bus callBus = (Bus)callExitIter.next();
                if (compBus.getValue().isSigned() != callBus.getValue().isSigned())
                {
                    if (_optimize.db) _optimize.ln(_optimize.REPLACE, "Call result bus sign not the same as comp bus result sign");
                    return false;
                }
                if (callBus.getValue().getSize() < compBus.getValue().getSize())
                {
                    if (_optimize.db) _optimize.ln(_optimize.REPLACE, "Call result bus smaller than comp bus result");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * This method ensures that the 'float' state of the operation
     * matches that of the call.  Call's are never marked as
     * 'isFloat', so we base it off of the interface.  If any port or
     * bus is float, then the op is a float.  Ditto for the call.
     *
     * @param comp a value of type 'Operation'
     * @param call a value of type 'Call'
     * @return a value of type 'boolean'
     */
    private boolean ensureFloat (Operation comp, Call call)
    {
        // If any data input or the result is a float, then we call it
        // a float.
        // Ensure that the inputs match because the
        // comparisons take in floats but produce an int!
        boolean compIsFloat = false;
        for (Iterator iter = comp.getDataPorts().iterator(); iter.hasNext();)
        {
            Port port = (Port)iter.next();
            if (port.getBus() != null)
            {
                compIsFloat |= port.getBus().isFloat();
            }
            else
            {
                for (Iterator eIter = comp.getEntries().iterator(); eIter.hasNext();)
                {
                    Entry entry = (Entry)eIter.next();
                    for (Iterator depIter = entry.getDependencies(port).iterator(); depIter.hasNext();)
                    {
                        Dependency dep = (Dependency)depIter.next();
                        compIsFloat |= dep.getLogicalBus().isFloat();
                    }
                }
            }
        }
        // Let the bus have a say in it too.
        compIsFloat |= ((Bus)comp.getExit(Exit.DONE).getDataBuses().get(0)).isFloat();
        
        boolean callIsFloat = false;
        InBuf ib = call.getProcedure().getBody().getInBuf();
        for (Iterator iter = ib.getDataBuses().iterator(); iter.hasNext();)
        {
            Bus b = (Bus)iter.next();
            callIsFloat |= b.isFloat();
        }
        // Let the bus have a say in it too.
        callIsFloat |= ((Bus)call.getExit(Exit.DONE).getDataBuses().get(0)).isFloat();
        
        return callIsFloat == compIsFloat;
    }
    

    /**
     *
     * Replaces the given component with the call contained in the
     * library Match by performing the following steps:
     * <ul>
     * <li>Set a preference on the replacement call such that nothing
     * equally or more complex than the component being replaced can
     * be replaced on subsequent optimizations.
     * <li>Initialize the values of the ports and buses of the
     * replacement call.
     * <li>Swap the call for the component to be replaced in its
     * owner, duplicating the dependencies, etc.
     * <li>Merge the library Design with the top level design to
     * associate the libraries resources with the top level design.
     * </ul>
     *
     * @param comp a value of type 'Component'
     * @param match a value of type 'Match'
     */
    protected void replace (Component comp, Match match)
    {
    	Option option;
        Call replaceCall = match.getReplacement();
        EngineThread.getGenericJob().info("Replacing " + comp.showIDLogical() + " from library " + match.getLibName());
        
        // Set a preference on the substituted call such that we
        // will not replace any _more_ complex components.  This
        // prevents recursion.
        // Retrieve the setting for the component just in case it
        // is more restrictive and take the most restrictive of
        // that and 1 less than the ops rank (lower bound at 0).
        ReplacementCorrelation correlation = ReplacementCorrelation.getCorrelation(comp);
        int opLevel = Math.max(correlation.getComplexityRank() - 1, 0);
        option = comp.getGenericJob().getOption(OptionRegistry.OPERATOR_REPLACEMENT_MAX_LEVEL);
        int prefLevel = ((OptionInt)option).getValueAsInt(comp.getSearchLabel());        
        int opReplaceLevel = Math.min(opLevel, prefLevel);
        replaceCall.accept(new CallScopePreferenceVisitor(option.getOptionKey(),
                               opReplaceLevel, comp));
        
        // Put the graph in place
        if (_optimize.db)_optimize.ln(_optimize.REPLACE,"Replacing " + comp + " with " + replaceCall);
        final Block block = wrapComponent(comp, replaceCall);
        swapLibComponent(comp, block);
        
        // Copy the target of the call
        if (match.getTarget() != null)
        {
            System.err.println("Fix ReplacementVisitor");
            throw new UnsupportedOperationException("No support for instance methods");
        }
        
        // Merge the designs.
        libraryDB.mergeDesigns(match.getLibName(), topDesign);
    }

    
    /**
     * Wraps the 'rep' component in a block containing casts anywhere
     * that the size of the ports and/or buses do not match the
     * original 'oldComp'
     *
     * @param oldComp a <code>Component</code> value
     * @param rep a <code>Call</code> value
     * @return a <code>Block</code> containing the rep call and the
     * casts. 
     */
    private Block wrapComponent (Component oldComp, Call rep)
    {
        // Create casts for each input port that is mismatched
        Map portToCast = new HashMap();
        ArrayList callPorts = new ArrayList(rep.getPorts());
        callPorts.remove(rep.getThisPort());
        assert callPorts.size() == oldComp.getPorts().size();
        for (Iterator cit = oldComp.getPorts().iterator(),
             rit = callPorts.iterator(); cit.hasNext();)
        {
            Port orig = (Port)cit.next();
            Port repPort = (Port)rit.next();
            if (orig.getValue().getSize() != repPort.getValue().getSize())
            {
                portToCast.put(repPort, new CastOp(repPort.getValue().getSize(), repPort.getValue().isSigned()));
            }
        }

        // Create casts for each output bus that is mismatched
        Map busToCast = new HashMap();
        assert rep.getExits().size() == 1 : "Only supporting one exit in replaceable components";
        for (Iterator cit = oldComp.getExits().iterator(),
             rit = rep.getExits().iterator(); cit.hasNext();)
        {
            Exit compExit = (Exit)cit.next();
            Exit repExit = (Exit)rit.next();
            for (Iterator cbit = compExit.getDataBuses().iterator(),
                 rbit = repExit.getDataBuses().iterator(); cbit.hasNext();)
            {
                Bus orig = (Bus)cbit.next();
                Bus repBus = (Bus)rbit.next();
                if (orig.getValue().getSize() != repBus.getValue().getSize())
                {
                    busToCast.put(repBus, new CastOp(orig.getValue().getSize(), orig.getValue().isSigned()));
                }
            }
        }

        // Create a block and put the cast ops and call into it.
        List comps = new ArrayList();
        comps.addAll(portToCast.values());
        comps.add(rep);
        comps.addAll(busToCast.values());
        Block block = new Block(comps, false);

        for (Port port : rep.getDataPorts())
        {
            // Create the input port on the block
            Port blockPort = block.makeDataPort();
            // Connect the block port to either the cast or the call
            Bus source = blockPort.getPeer();
            if (portToCast.containsKey(port))
            {
                // connect to cast
                CastOp cast = (CastOp)portToCast.get(port);
                Entry entry = (Entry)cast.getEntries().get(0);
                entry.addDependency(cast.getDataPort(), new DataDependency(source));
                source = cast.getResultBus();
            }

            Entry entry = (Entry)rep.getEntries().get(0);
            entry.addDependency(port, new DataDependency(source));
        }

        for (Exit exit : rep.getExits())
        {
            Exit blockExit = block.getExit(exit.getTag());
            // Create the exit on the block (if needed)
            if (blockExit == null)
                blockExit = block.makeExit(0, exit.getTag().getType(), exit.getTag().getLabel());
            for (Bus bus : exit.getDataBuses())
            {
                // Create the output bus on the block
                Bus blockBus = blockExit.makeDataBus();
                Port target = blockBus.getPeer();
                Bus source = bus;
                if (busToCast.containsKey(bus))
                {
                    CastOp cast = (CastOp)busToCast.get(bus);
                    Entry entry = (Entry)cast.getEntries().get(0);
                    entry.addDependency(cast.getDataPort(), new DataDependency(source));
                    source = cast.getResultBus();
                }
                Entry entry = (Entry)target.getOwner().getEntries().get(0);
                entry.addDependency(target, new DataDependency(source));
            }
        }

        return block;
    }
    

    
    /**
     * Removes the {@link Component} 'comp' from its owner and inserts
     * the replacement {@link Call} 'rep' into that owner at the same
     * location.  The dependencies from the replaced component are
     * copied onto the Call via a 1:1 correlation of ports, buses and
     * exits.  If the Call has a 'this' port (indicating that it is a
     * non-static call) the port is ignored in copying of dependencies.
     *
     * @param comp the {@link Component} to be replaced, also the
     * source of all dependencies to be copied over.
     * @param rep the {@link Call} to be substituted for the
     * component.
     */
    private void swapLibComponent (Component comp, Block rep)
    {
        Map portCorrelation = new HashMap();
        Map busCorrelation = new HashMap();
        Map exitCorrelation = new HashMap();

        ArrayList callPorts = new ArrayList(rep.getPorts());
        //callPorts.remove(rep.getThisPort());
        assert callPorts.size() == comp.getPorts().size();
        for (Iterator cit = comp.getPorts().iterator(),
             rit = callPorts.iterator(); cit.hasNext();)
        {
            portCorrelation.put(cit.next(), rit.next());
        }
        
        for (Iterator compExitIter = comp.getExits().iterator(),
             repExitIter = rep.getExits().iterator();
             compExitIter.hasNext();)
        {
            Exit compExit = (Exit)compExitIter.next();
            Exit repExit = (Exit)repExitIter.next();
            exitCorrelation.put(compExit, repExit);

            for (Iterator compIter = compExit.getBuses().iterator(),
                 repIter = repExit.getBuses().iterator();
                 compIter.hasNext();)
            {
                busCorrelation.put(compIter.next(), repIter.next());
            }
        }

        replaceConnections(portCorrelation, busCorrelation, exitCorrelation);
        moduleReplace(comp, rep);
        comp.disconnect();
        
        replacedNodeCount++;
        replacedNodeCountTotal++;
    }

    /**
     * By creating a new collection to iterate over we protect against
     * concurrent modification exceptions.
     */
    protected Iterator getIterator(Collection collection)
    {
        return (new LinkedList(collection)).iterator();        
    }

    /**
     * Performs a class specific copying of the given buffer.
     */
    private static Buffer copyBuffer (Buffer buf, IPCore target)
    {
        if (buf instanceof PinIn)
            return copyPinIn((PinIn)buf, target);
        else if (buf instanceof PinInOutTS)
            return copyPinInOutTS((PinInOutTS)buf, target);
        else if (buf instanceof PinOut)
            return copyPinOut((PinOut)buf, target);
        else if (buf instanceof PinOutTS)
            return copyPinOutTS((PinOutTS)buf, target);
        
        EngineThread.getEngine().fatalError("Unknown pin type associated with IPCore: " + buf.getClass());
        return null;
    }
    
    private static Buffer copyPinIn (PinIn buf, IPCore target)
    {
        return new PinIn(target, buf.getName(), buf.getSize(), buf.getInputPipelineDepth());
    }
    
    private static Buffer copyPinInOutTS (PinInOutTS buf, IPCore target)
    {
        if (buf.getDriveOnReset())
            return new PinInOutTS(target, buf.getName(), buf.getSize(),
                buf.getResetValue(), buf.getInputPipelineDepth());
        else
            return new PinInOutTS(target, buf.getName(), buf.getSize());
    }
    
    private static Buffer copyPinOut (PinOut buf, IPCore target)
    {
        if (buf.getDriveOnReset())
            return new PinOut(target, buf.getName(), buf.getSize(), buf.getResetValue());
        else
            return new PinOut(target, buf.getName(), buf.getSize());
    }
    
    private static Buffer copyPinOutTS (PinOutTS buf, IPCore target)
    {
        if (buf.getDriveOnReset())
            return new PinOutTS(target, buf.getName(), buf.getSize(), buf.getResetValue());
        else
            return new PinOutTS(target, buf.getName(), buf.getSize());
    }
    

    protected class Match
    {
        private String libName;
        private Call replacement;
        private Allocation target;
        private Design libDes;

        public Match (String lib, Design libDesign, Call call, Allocation tgt)
        {
            this.libName = lib;
            this.libDes = libDesign;
            this.replacement = call;
            this.target = tgt;
        }

        public String getLibName ()
        {
            return this.libName;
        }

        public Design getLibDesign ()
        {
            return this.libDes;
        }

        public Call getReplacement ()
        {
            return this.replacement;
        }

        public Allocation getTarget ()
        {
            return this.target;
        }
    }

}// ReplacementVisitor

