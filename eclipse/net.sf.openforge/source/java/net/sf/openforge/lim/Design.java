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


import java.util.*;

import net.sf.openforge.app.Engine;
import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.project.SearchLabel;
import net.sf.openforge.forge.api.internal.*;
import net.sf.openforge.forge.api.pin.ClockPin;
import net.sf.openforge.forge.api.pin.ResetPin;
import net.sf.openforge.forge.api.sim.pin.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.io.actor.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.util.naming.*;



/**
 * Design is the top level representation of a an implementation
 * in hardware.  It consists of one or more parallel
 * {@link Task Tasks} and zero or more global {@link Resource Resources}
 * that are used by these Tasks.
 *
 * @author  Stephen Edwards
 * @version $Id: Design.java 538 2007-11-21 06:22:39Z imiller $
 */
public class Design extends ID implements Visitable,Cloneable
{
    private static final String _RCS_ = "RCS_REVISION: $Rev: 538 $";

    private List<Task> taskList = Collections.EMPTY_LIST;
    private Collection<Register> registers=Collections.EMPTY_LIST;

    /** Tracks the allocated memory ID's for this design.  Note that
     * memory ID 0 is reserved for the Null object. */
    private int memoryId = 1;

    /** This is a map of String (the fifo ID) to an instance of
     * FifoIF which contains all the necessary pins for the fifo
     * interface.  Uses a linked hashmap as a convenience to the user
     * so that the translated Verilog has the same port ordering each
     * time. */
    private Map<String, FifoIF> fifoInterfaces = new LinkedHashMap();
    
    private Collection inputPins=Collections.EMPTY_LIST;
    private Collection outputPins=Collections.EMPTY_LIST;
//     private Collection bidirectionalPins=Collections.EMPTY_LIST;

    private Collection<LogicalMemory> logicalMemories = Collections.EMPTY_LIST;
    
    /**
     * Holds a reference to the {@link Tester} that is capable of
     * testing this <code>Design</code>.
     */
    private Tester tester = null;

    /** Map of defined clock domains. */
    private Map<String, ClockDomain> clockDomains = new HashMap();
    
    /** map of api clock pin name to input pins (clocks) */
    private HashMap apiClockNameToLIMClockMap=new HashMap();
    
    /** map of api reset pin name to input pins (reset) */
    private HashMap apiResetNameToLIMResetMap=new HashMap();

    /**
     * A mapping between {@link Pin} and {@link Port} where the Port
     * is the port on a Call (entry method) and the Pin is the pin
     * created to connect to that port.  This map is used by the
     * automatic test bench generator to provide correlations.
     */
    private Map pinPortBusMap = new HashMap();
    
    /** The max gate depth */
    private int maxGateDepth = 0;

    /** The max unbreakable gate depth */
    private int unbreakableGateDepth = 0;
    
    /** The include statements */
    private List includeStatements = Collections.EMPTY_LIST;

    private final CodeLabel searchLabel;
    
    public Design ()
    {
        super();
        apiClockNameToLIMClockMap.clear();
        apiResetNameToLIMResetMap.clear();
        this.designModule = new DesignModule();
        this.searchLabel = new CodeLabel("design");
    }

    public void accept(Visitor vis)
    {
        vis.visit(this);
    }    
    
    public void addTask(Task tk)
    {
        if (this.taskList == Collections.EMPTY_LIST)
        {
            this.taskList = new ArrayList(3);
        }
        this.taskList.add(tk);
        addComponentToDesign(tk.getCall());
    }
    
    /**
     * Add a Register in the list of Register for this Design
     * @param reg {@link Register Register}
     */
    public void addRegister(Register reg)
    {
        if(this.registers == Collections.EMPTY_LIST)
        {
            this.registers = new ArrayList(3);
        }
        this.registers.add(reg);
    }

    /**
     * Removes the specified register from this design.
     *
     * @param register true if removed, false if not found
     */
    public boolean removeRegister (Register register)
    {
        return this.registers.remove(register);
    }

    /**
     * Adds the given {@link LogicalMemory} to this design
     */
    public void addMemory (LogicalMemory mem)
    {
        if (this.logicalMemories == Collections.EMPTY_LIST)
        {
            this.logicalMemories = new ArrayList(3);
        }
        this.logicalMemories.add(mem);
        // TBD.  When we have full flow we will need to come back
        // through and ensure that any object/struct reference has a
        // unique identifier regardless of what memory it is in.
        // Also, we'll need to add to the MemoryDispositionReporter
        // support for LogicalMemories.
//         mem.setMemoryId(getNextMemoryId());
    }

    /**
     * Removes the given {@link LogicalMemory} from this design
     */
    public void removeMemory (LogicalMemory mem)
    {
        this.logicalMemories.remove(mem);
        if (this.logicalMemories.size() == 0)
        {
            this.logicalMemories = Collections.EMPTY_LIST;
        }
    }

    /**
     * Retrieves the next non-allocated memory ID.
     */
    public int getNextMemoryId ()
    {
        int id = this.memoryId;
        this.memoryId += 1;
        if (id < 0)
        {
            EngineThread.getEngine().fatalError("Too many memory objects allocated in design");
        }
        if (id == 0)
        {
            EngineThread.getEngine().fatalError("Memory id 0 is reserved for the null object");
        }
        return id;
    }

    public void addInputPin(InputPin pin, Port port)
    {
        addInputPin(pin);
        
        this.pinPortBusMap.put(pin, port);
        this.pinPortBusMap.put(port, pin);
    }
    
          
    /**
     * Add an InputPin in the list of InputPins for this Design
     * @param pin {@link InputPin}
     */
    public void addInputPin(InputPin pin)
    {
        if(this.inputPins == Collections.EMPTY_LIST)
        {
            this.inputPins = new ArrayList(3);
        }
        this.inputPins.add(pin);
    }  
     
    /**
     * Add an OutputPin in the list of OutputPins for this Design
     * @param pin {@link OutputPin}
     */
    public void addOutputPin(OutputPin pin, Bus bus)
    {
        this.addOutputPin(pin);
        this.pinPortBusMap.put(pin, bus);
        this.pinPortBusMap.put(bus, pin);
    }

    /**
     * Add an OutputPin in the list of OutputPins for this Design
     * @param pin {@link OutputPin}
     */
    public void addOutputPin(OutputPin pin)
    {
        if(this.outputPins == Collections.EMPTY_LIST)
        {
            this.outputPins = new ArrayList(3);
        }
        this.outputPins.add(pin);
    }

    /**
     * Adds a {@link BidirectionalPin} to this Design.
     */
//     public void addBidirectionalPin (BidirectionalPin pin)
//     {
//         if (this.bidirectionalPins == Collections.EMPTY_LIST)
//         {
//             this.bidirectionalPins = new LinkedList();
//         }
//         this.bidirectionalPins.add(pin);
//     }

    /**
     * Tests whether or not this design is clocked.
     *
     * @return true if this design contains any elements that require a clock
     */
    public boolean consumesClock ()
    {
        for (Iterator iter = getDesignModule().getComponents().iterator(); iter.hasNext();)
        {
            if (((Component)iter.next()).consumesClock())
                return true;
        }
        for(Iterator it=getPins().iterator();it.hasNext();)
        {
            if(((Pin)it.next()).consumesClock())
            {
                return true;
            }
        }
        
        return (!getRegisters().isEmpty()
            || !getLogicalMemories().isEmpty());
    }

    /**
     * Tests whether or not this design is resettable.
     *
     * @return true if this design contains any elements that require a reset
     */
    public boolean consumesReset ()
    {
        for (Iterator iter = getDesignModule().getComponents().iterator(); iter.hasNext();)
        {
            if (((Component)iter.next()).consumesReset())
                return true;
        }

        // this adds logic for pins FIXME for all the other things!
        for(Iterator it=getPins().iterator();it.hasNext();)
        {
            if(((Pin)it.next()).consumesReset())
            {
                return true;
            }
        }
        return !getRegisters().isEmpty() || !getLogicalMemories().isEmpty();
    }
    
    public Collection<Task> getTasks ()
    {
        return taskList;
    }

    public Collection<Register> getRegisters ()
    {
        return registers;
    }

    public Collection<LogicalMemory> getLogicalMemories ()
    {
        return Collections.unmodifiableCollection(this.logicalMemories);
    }

    /**
     * Old way of getting the input pins, still used for clock and
     * reset, and non-blockio pins.
     *
     * @return a value of type 'Collection'
     * @deprecated
     */
    public Collection getInputPins ()
    {
        return inputPins;
    }

    /**
     * Old way of getting the output pins, still used for done and
     * result, and non-blockio pins.
     *
     * @return a value of type 'Collection'
     * @deprecated
     */
    public Collection getOutputPins ()
    {
        return outputPins;
    }

    /**
     * Old way of getting the inout pins, not used any longer???
     *
     * @return a value of type 'Collection'
     * @deprecated
     */
    public Collection getBidirectionalPins ()
    {
        //return bidirectionalPins;
        return Collections.EMPTY_LIST;
    }

    /**
     * Retrieves the Pin created for the given Port or Bus
     *
     * @param o a Port or Bus
     * @return a value of type 'Pin'
     */
    public Pin getPin (Object o)
    {
        return (Pin)pinPortBusMap.get(o);
    }

    /**
     * method to return an InputPin representing a clock.  If not already
     *    defined, define it first.
     * @param apiClockPin api ClockPin corresponding to the requested lim clock pin
     * @return the InputPin representing clockName
     * calls Job.fatalError() if the name is already defined as a reset pin
     */
    public InputPin getClockPin (ClockPin apiClockPin)
    {
        String name=apiClockPin.getName();

        if (apiResetNameToLIMResetMap.containsKey(name))
        {
            EngineThread.getEngine().fatalError("Can't have clock and reset share a pin name: "+apiClockPin);
        }
        
        InputPin clockPin=(InputPin) apiClockNameToLIMClockMap.get(name);
        // if not defined, then define a clock pin & store it
        if (clockPin==null)
        {
            clockPin=new InputPin(1,false);
            clockPin.setApiPin(apiClockPin);
            clockPin.setIDLogical(name);
            addInputPin(clockPin);
            apiClockNameToLIMClockMap.put(name, clockPin);
        }
        return clockPin;
    }
    /**
     * method to return an InputPin representing a reset.  If not already
     *    defined, define it first.
     * @param apiResetPin api ResetPin corresponding to the requested lim reset pin
     * @return the InputPin representing resetName
     * calls Job.fatalError() if the name is already defined as a clock pin
     */
    public GlobalReset getResetPin (ResetPin apiResetPin)
    {
        String name=apiResetPin.getName();
        if (apiClockNameToLIMClockMap.containsKey(name))
        {
            EngineThread.getEngine().fatalError("Can't have clock and reset share a pin name: "+apiResetPin);
        }
        
        GlobalReset resetPin=(GlobalReset) apiResetNameToLIMResetMap.get(name);
        // if not defined, then define a reset pin & store it
        if (resetPin==null)
        {
            resetPin=new GlobalReset();
            resetPin.setApiPin(apiResetPin);
            resetPin.setIDLogical(name);
            apiResetNameToLIMResetMap.put(name, resetPin);

            // wire the input of the reset to the output of the paired
            // clock
            ClockPin apiClockPin = apiResetPin.getDomain().getClockPin();
            InputPin clockPin = getClockPin(apiClockPin);
            resetPin.getPort().setBus(clockPin.getBus());
        }
        return resetPin;
    }
    /**
     * return the LIM Clock Pins
     */
    public Collection getClockPins()
    {
        return apiClockNameToLIMClockMap.values();
    }
    
    /**
     * return the LIM reset pins
     */
    public Collection getResetPins()
    {
        return apiResetNameToLIMResetMap.values();
    }

    public Collection getPins ()
    {
        Collection pins = new LinkedHashSet();
        pins.addAll(getInputPins());
        pins.addAll(getOutputPins());
//         pins.addAll(getBidirectionalPins());
        return pins;
    }
    

    /**
     * Retrieves the specific FifoIF object that exists in this design
     * for the attributes contained in the specified {@link FifoID}
     * object. If a matching {@link FifoIF} object has not yet been
     * allocated on this design, then one will be created and
     * returned.  Subsequent calls to getFifoIF with a fifoID with the
     * same criteria will return the same FifoIF object.
     *
     * @param fifoID a non null {@link FifoID}
     * @return a value of type 'FifoIF'
     * @throws IllegalArgumentException if the fifoID contains
     * criteria that conflict with an already allocated fifo
     * interface.  Including, for example, requesting a fifo interface
     * with the same ID but a different data path width.
     */
    public FifoIF getFifoIF (FifoID fifoID)
    {
        // This method must look at the ID number specified in the id class
        // and return the FifoIF that has been allocated for that ID number.
        // Thus all fifoID instances with ID with matching number and
        // direction must return the same FifoIF.  Some rudimentary checking
        // should be performed to try to catch configuration errors early on,
        // such as returning a FifoIF for a given ID number when the fifoID
        // instances data width does not match the FifoIF, direction does not
        // match, etc.  If a FifoIF has not yet been created for the given ID
        // number, then create a new FifoIF (of the right direction input or
        // output), add all of its pins to this design, and then return the
        // FifoIF object.

        String key = fifoID.getName()+""+fifoID.isInputFifo();
        FifoIF fifoIF = (FifoIF)this.fifoInterfaces.get(key);

        if (fifoIF == null)
        {
            String id = fifoID.getName();

            switch (fifoID.getType())
            {
                case FifoID.TYPE_FSL :
                    if (fifoID.isInputFifo())
                        fifoIF = new FSLFifoInput(id, fifoID.getBitWidth());
                    else
                        fifoIF = new FSLFifoOutput(id, fifoID.getBitWidth());
                    break;
                case FifoID.TYPE_ACTION_SCALAR :
                    if (fifoID.isInputFifo())
                        fifoIF = new ActorScalarInput(fifoID);
                    else
                        fifoIF = new ActorScalarOutput(fifoID);
                    break;
                case FifoID.TYPE_ACTION_OBJECT :
                    throw new UnsupportedOperationException("Object fifos not yet supported");
                    //break;
            }
            
            this.fifoInterfaces.put(key, fifoIF);
        }
        else
        {
            // Rule checking
            if (fifoIF.getWidth() != fifoID.getBitWidth())
                throw new IllegalArgumentException("Attempt to redefine fifo interaface " + fifoID.getName() + " width from " + fifoIF.getWidth() + " to " + fifoID.getBitWidth());
        }

        addComponentToDesign(fifoIF.getPins());
        
        return fifoIF;
    }

    /**
     * Returns a Collection of the defined FifoIF objects for this design.
     *
     * @return a Collection of {@link FifoIF} objects
     */
    public Collection<FifoIF> getFifoInterfaces ()
    {
        // Jump through these hoops so that the fifo interfaces come
        // back in the same order each time.
        List<FifoIF> interfaces = new LinkedList();
        for (Iterator iter = this.fifoInterfaces.entrySet().iterator(); iter.hasNext();)
        {
            interfaces.add((FifoIF)((Map.Entry)iter.next()).getValue());
        }
        
        return interfaces;
    }

    /**
     * <code>getClockDomain</code> returns the appropriate
     * {@link ClockDomain} for the specified clk/reset string.  The
     * String takes the form of clkname:resetname or simply clkname if
     * there is no (published) reset for that clock domain.
     *
     * @param domainSpec a <code>String</code> value
     * @return a <code>ClockDomain</code> value
     */
    public ClockDomain getClockDomain (String domainSpec)
    {
        ClockDomain domain = this.clockDomains.get(domainSpec);
        if (domain == null)
        {
            domain = new ClockDomain(domainSpec);
            addComponentToDesign(domain.getClockPin());
            if (domain.getResetPin() != null)
            {
                
                addComponentToDesign(domain.getResetPin());
            }
            addComponentToDesign(domain.getGSR());
            this.clockDomains.put(domainSpec, domain);
        }
        return domain;
    }

    /**
     * Retrieves a collection of all the allocated clock domains for
     * this design.
     */
    public Collection<ClockDomain> getAllocatedClockDomains ()
    {
        return Collections.unmodifiableCollection(this.clockDomains.values());
    }
    
    /**
     * Sets the {@link Tester} for this <code>Design</code>.
     *
     * @param tester the <code>Tester</code> that can generate
     * arguments and expected results for this <code>Design</code>
     */
    public void setTester(Tester tester)
    {
        this.tester = tester;
    }


    /**
     * @return The {@link Tester} for this <code>Design</code>.  From
     * the {@link Tester} all the information necessary for generating
     * a self verifying automatic test bench can be gleaned.  A return
     * value of <code>null</code> is valid if this <code>Design</code>
     * doesn't have a {@link Tester}.
     */
    public Tester getTester()
    {
        return(this.tester);
    }

    public int getMaxGateDepth ()
    {
        return this.maxGateDepth;
    }
    
    public void setMaxGateDepth (int maxGateDepth)
    {
        this.maxGateDepth = maxGateDepth;
    }

    public int getUnbreakableGateDepth ()
    {
        return this.unbreakableGateDepth;
    }
    
    public void setUnbreakableGateDepth (int unbreakableGateDepth)
    {
        this.unbreakableGateDepth = unbreakableGateDepth;
    }
    
    /**
     *
     * Creates a new and fully independent copy of the design.  The
     * cloning process works in two phases.  In the first phase, the
     * clone in accomplished by simply cloning each task in the
     * design.  Once cloned the new copy of the task is visited by the
     * Design.DesignCloneVisitor.  The job of this visitor is to find
     * each Call in the design and point it to the clone of the
     * original procedure.  This process ensures that each procedure
     * is only cloned once, and that any call pointing to a given
     * procedure is re-targetted to the corresponding clone.  Once the
     * process of cloning each Task is completed, all global resources
     * are cloned.  This includes memories, registers, pins, etc.  As
     * each one is cloned we point each access in the cloned LIM
     * (cloned task hierarchies) to the corresponding new resource.
     * This is accomplished by iterating over the collection of
     * references stored by each original resource.  Each reference is
     * contained in a module (by LIM definition) which has a clone
     * correlation map.  This map provides a correlation between that
     * original reference and the clone reference.  The clone
     * reference then has it's {@link Referent} set to the cloned
     * resource via the {@link Reference#setReferent} method.
     *
     * @return a deeply cloned copy of the Design
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();

        /*
         * Leave this out and untested until it is really needed.
         */


//         Design clone = (Design)super.clone();

//         // Clone each task.
//         //        DesignCloneVisitor cloneVisitor = new DesignCloneVisitor();
//         Map thisToClone = new HashMap();
//         if (this.taskList.size() > 0)
//         {
//             clone.taskList = new ArrayList(this.taskList.size());
//             for (Iterator iter = this.taskList.iterator(); iter.hasNext();)
//             {
//                 Task origTask = (Task)iter.next();
//                 Task cloneTask = (Task)origTask.clone();
//                 //
//                 // Do a 'deep' clone, cloning the procedure attached
//                 // to each call, unless we've already cloned that
//                 // procedure
//                 // ABK: not anymore. Call.clone() now makes a freshly
//                 // cloned Procedure.
//                 //  cloneTask.accept(cloneVisitor);
//                 clone.taskList.add(cloneTask);
//                 correlate(origTask, cloneTask, thisToClone);
//             }
//         }
//         else
//         {
//             clone.taskList = Collections.EMPTY_LIST;
//         }
        
//         // Now clone each accessed resource
        
//         clone.registers = cloneResources(this.registers);

//         // Clone any memories.
//         if (this.memories.size() > 0)
//         {
//             clone.memories = new ArrayList(this.memories.size());
//             for (Iterator iter = this.memories.iterator(); iter.hasNext();)
//             {
//                 Memory origMem = (Memory)iter.next();
//                 Memory cloneMem = (Memory)origMem.clone();
//                 for (Iterator oMemPortIter = origMem.getMemoryPorts().iterator(),
//                      cMemPortIter = cloneMem.getMemoryPorts().iterator();
//                      oMemPortIter.hasNext();)
//                 {
//                     resetReferent((Resource)oMemPortIter.next(), (Resource)cMemPortIter.next());
//                 }
//             }
//         }
//         else
//         {
//             clone.memories = Collections.EMPTY_LIST;
//         }
        

//         clone.sharedProcedures = cloneResources(this.sharedProcedures);

//         // Create a port/bus correlation map from this to clone.
//         clone.pinPortBusMap = new HashMap();
//         clone.inputPins = clonePins(this.inputPins, clone, thisToClone);
//         clone.outputPins = clonePins(this.outputPins, clone, thisToClone);
//         clone.bidirectionalPins = clonePins(this.bidirectionalPins, clone, thisToClone);

//         clone.resources = cloneResources(this.resources);
        
//         return clone;
    }

//     private List clonePins(Collection pinsToBeCloned, Design clone, Map correlation) throws CloneNotSupportedException
//     {
//         List cloneList = Collections.EMPTY_LIST;

//         if (pinsToBeCloned.size() > 0)
//         {
//             cloneList = new ArrayList(pinsToBeCloned.size());
//             for (Iterator iter = pinsToBeCloned.iterator(); iter.hasNext();)
//             {
//                 Pin origPin = (Pin)iter.next();
//                 Pin clonePin = (Pin)origPin.clone();
//                 for (Iterator origBufIter = origPin.getPinBufs().iterator(),
//                      cloneBufIter = clonePin.getPinBufs().iterator();
//                      origBufIter.hasNext();)
//                 {
//                     resetReferent((PinBuf)origBufIter.next(),
//                         (PinBuf)cloneBufIter.next());
//                 }
//                 Object portOrBus = this.pinPortBusMap.get(origPin);
//                 if (portOrBus != null)
//                 {
//                     Object clonePortOrBus = correlation.get(portOrBus);
//                     clone.pinPortBusMap.put(clonePortOrBus, clonePin);
//                     clone.pinPortBusMap.put(clonePin, clonePortOrBus);
//                 }
//             }
//         }

//         return cloneList;
//     }

//     /**
//      * Correlate all ports and buses from the call of each task.
//      */
//     private static void correlate(Task local, Task clone, Map map)
//     {
//         Call localCall = local.getCall();
//         Call cloneCall = clone.getCall();
//         for (Iterator localIter = localCall.getPorts().iterator(),
//              cloneIter = cloneCall.getPorts().iterator(); localIter.hasNext();)
//         {
//             Port lp = (Port)localIter.next();
//             Port cp = (Port)cloneIter.next();
//             map.put(lp, cp);
//             map.put(cp, lp);
//         }
//         for (Iterator localIter = localCall.getBuses().iterator(),
//              cloneIter = cloneCall.getBuses().iterator(); localIter.hasNext();)
//         {
//             Bus lp = (Bus)localIter.next();
//             Bus cp = (Bus)cloneIter.next();
//             map.put(lp, cp);
//             map.put(cp, lp);
//         }
//     }
    
    
    
//     /**
//      * Returns a List of cloned Resources based on the input
//      * collection of Resource objects.  This method depends on the
//      * tasks already being cloned so as to populate the clone
//      * correlation maps at each module level.
//      *
//      * @param toBeCloned a Collection of {@link Resource} objects.
//      * @return a List of cloned {@link Resource} objects.
//      */
//     private static List cloneResources(Collection toBeCloned) throws CloneNotSupportedException
//     {
//         List cloneList = Collections.EMPTY_LIST;

//         if (toBeCloned.size() > 0)
//         {
//             cloneList = new ArrayList(toBeCloned.size());

//             for (Iterator iter = toBeCloned.iterator(); iter.hasNext();)
//             {
//                 Resource orig = (Resource)iter.next();
//                 Resource clone = (Resource)orig.clone();
//                 cloneList.add(clone);

//                 // Now set up the references to this resource.  Call
//                 // the 'setReferent' on each Reference
//                 resetReferent(orig, clone);
//             }
//         }
        
//         return cloneList;
//     }

//     private static void resetReferent(Resource orig, Resource clone)
//     {
//         assert false : "FIXME";
// //          for (Iterator refIter = orig.getReferences().iterator(); refIter.hasNext();)
// //          {
// //              Reference origRef = (Reference)refIter.next();
// //              assert origRef.getOwner() != null : "Reference cannot exist outside of a module";
// //              assert origRef.getOwner().getCloneCorrelationMap() != null : "The modules must all be cloned first before cloning global resources. Ref: " + origRef + " in " + origRef.getOwner();
// //              assert origRef.getOwner().getCloneCorrelationMap().containsKey(origRef) : "The clone correlation map doesn't contain the resource!";
// //              Reference cloneRef = (Reference)origRef.getOwner().getCloneCorrelationMap().get(origRef);
// //              cloneRef.setReferent(clone);
// //          }
//     }

//     /**
//      * ABK: this is no longer needed because Calls also clone the procedure
//      * which they are calling.
//      * @deprecated 
//      */
//     private static class DesignCloneVisitor extends FilteredVisitor
//     {
//         // Map of original procedure -> clone procedure to ensure that
//         // we keep the proper uniqueness (or lack thereof) for each call.
//         private Map clonedProcedures = new HashMap();
        
//         public void preFilter(Call call)
//         {
//             super.preFilter(call);
            
//             // Clone the procedure since the cloning of the call by
//             // default points to the _same_ procedure as the original.
//             try
//             {
//                 Procedure original = call.getProcedure();
//                 Procedure clone;
//                 if (clonedProcedures.containsKey(original))
//                 {
//                     clone = (Procedure)clonedProcedures.get(original);
//                 }
//                 else
//                 {
//                     clone = (Procedure)original.clone();
//                 }
//                 call.setReferent(clone);
//             }
//             catch (CloneNotSupportedException e)
//             {
//                 e.printStackTrace();
//                 System.exit(1);
//             }
//         }
//     }
    
    public Engine getEngine()
    {
        return EngineThread.getEngine();
    }

    public SearchLabel getSearchLabel()
    {               
        //return CodeLabel.UNSCOPED;
        return this.searchLabel;
    }
    
    /**
     * Mainly for use with IPCore. Sets the include statement
     * in a Design header.
     *
     * @param include the HDL source to be included
     */
    public void addIncludeStatement (String include)
    {
        if(includeStatements.isEmpty())
        {
            includeStatements = new ArrayList(1);
        }

        if (!includeStatements.contains(include))
        {
            includeStatements.add(include);
        }
    }

    /**
     * @return a list of included source file paths
     */
    public List getIncludeStatements()
    {
        return (List)includeStatements;
    }

    /**
     * Adds any 'global' resources, such as Memories, Registers, Pins,
     * etc from the source design to this design.  Note that this is,
     * essentially, destructive to the source design as any and all
     * accesses to the resources should (eventually) be moved over to
     * this design.
     *
     * @param source a value of type 'Design'
     */
    public void mergeResources (Design source)
    {
        /* These things do not have to be merged:
         * sharedProcedures - we dont have any yet!
         * kickers - we aren't bringing over tasks, so we dont need kickers
         * flipflopschain - again, no tasks brought over
         * apiClockNameToLIMClockMap - merging should be done pre-clock pins
         * apiResetNameToLIMResetMap - ditto
         * pinPortBusMap - ditto
         */
        assert apiClockNameToLIMClockMap.isEmpty();
        assert apiResetNameToLIMResetMap.isEmpty();
        assert pinPortBusMap.isEmpty();

        Module sourceMod = source.getDesignModule();
        Set sourceComps = new HashSet(sourceMod.getComponents());
        // We want the top level infrastructure, but NOT the entry
        // methods 
        for (Iterator iter = source.getTasks().iterator(); iter.hasNext();)
        {
            Task task = (Task)iter.next();
            sourceComps.remove(task.getCall());
        }
        //this.addComponentToDesign(source.getDesignModule().getComponents());
        this.addComponentToDesign(sourceComps);
        
        // registers
        for (Iterator iter = source.getRegisters().iterator(); iter.hasNext();)
        {
            addRegister((Register)iter.next());
        }
//         // memories
//         for (Iterator iter = source.getMemories().iterator(); iter.hasNext();)
//         {
//             addMemory((Memory)iter.next());
//         }
        // logicalMemories
        for (Iterator iter = source.getLogicalMemories().iterator(); iter.hasNext();)
        {
            addMemory((LogicalMemory)iter.next());
        }
        // inputPins
        for (Iterator iter = source.getInputPins().iterator(); iter.hasNext();)
        {
            addInputPin((InputPin)iter.next());
        }
        // outputPins
        for (Iterator iter = source.getOutputPins().iterator(); iter.hasNext();)
        {
            addOutputPin((OutputPin)iter.next());
        }
        // bidirectionalPins
//         for (Iterator iter = source.getBidirectionalPins().iterator(); iter.hasNext();)
//         {
//             addBidirectionalPin((BidirectionalPin)iter.next());
//         }
        // include statements
        for (Iterator iter = source.getIncludeStatements().iterator(); iter.hasNext();)
        {
            addIncludeStatement((String)iter.next());
        }
    }

    private Set entryData;
    private Map pinSimDriveMap;
    private Map pinSimTestMap;
    
    /**
     * This will save off the static info in forge.api.ipcore.Core,
     * forge.api.entry.Entry, and forge.api.sim.pin.PinSimData
     *
     */
    public void saveAPIContext()
    {
        // Don't clear the IPCore map since it keeps unique 
        // ipcore references and when bubbling up replaced operations
        // that use IPCores, what they put in the map needs to not get
        // cleared out.  This puts the requirement on the translator
        // to only call the HDLWriters for IPCores that are included
        // in the design.

        // entry
        entryData=EntryMethods.cloneEntryMethods();

        // pinsim        
        pinSimDriveMap=PinSimData.cloneDriveMap();
        pinSimTestMap=PinSimData.cloneTestMap();
    }

    /**
     * Clear Core, Entry, PinSim 
     *
     */
    public void clearAPIContext()
    {
        // ipcore - see saveAPIContext comments above.

        // entry
        EntryMethods.clearEntryMethods();

        // pinsim
        PinSimData.clear();
    }

    /**
     * Restore Core, Entry, PinSim
     *
     */
    public void restoreAPIContext()
    {
        // ipcore - see saveAPIContext comments above.

        // entry
        EntryMethods.setEntryMethods(entryData);

        // pinsim data
        PinSimData.setDriveData(pinSimDriveMap);
        PinSimData.setTestData(pinSimTestMap);
    }

    /////////////////////
    //
    // Implementation of a design module to contain the resources.
    //
    /////////////////////

    private DesignModule designModule;

    public DesignModule getDesignModule ()
    {
        return this.designModule;
    }

    /**
     * Adds the specified component to the design module.  This method
     * does NOT update any of the Design 'get' methods.
     *
     * @param comp a value of type 'Component'
     */
    public void addComponentToDesign (Component comp)
    {
        getDesignModule().addComponent(comp);
    }
    
    /**
     * Adds the specified components to the design module.  This
     * method does NOT update any of the Design 'get' methods.
     *
     * @param comp a value of type 'Component'
     */
    public void addComponentToDesign (Collection comps)
    {
        getDesignModule().addComponents(comps);
    }
    
    /**
     * This is a specific module type used to hold all the top level
     * components for the design.
     */
    public static class DesignModule extends Module
    {
        public boolean replaceComponent (Component removed, Component inserted)
        {
            if (!removeComponent(removed))
                return false;
            addComponent(inserted);
            return true;
        }

        public void accept (Visitor vis)
        {
            throw new UnsupportedOperationException("Cannot directly visit a design Module");
        }

        public void addComponent (Component comp)
        {
            super.addComponent(comp);
        }

        public Collection<Component> getComponents ()
        {
            LinkedHashSet comps = new LinkedHashSet(super.getComponents());
            comps.remove(getInBuf());
            comps.remove(getOutBufs());
            return comps;
        }
        
    }

    public static class ClockDomain
    {
        private SimplePin clock;
        private SimplePin reset;
        private GlobalReset.Physical gsr;
        private String domainSpec;

        private ClockDomain (String domainSpec)
        {
            this.domainSpec = domainSpec;
            String[] parsed = parse(domainSpec);
            String clk = parsed[0];
            String rst = parsed[1];
            this.clock = new ControlPin(clk);
            if (rst != null && rst.length() > 0)
            {
                this.reset = new ControlPin(rst);
            }
            gsr = new GlobalReset.Physical(this.reset != null);
            this.clock.connectBus(Collections.singleton(gsr.getClockInput()));
            if (this.reset != null)
            {
                this.reset.connectBus(Collections.singleton(gsr.getResetInput()));
            }
        }

        // Private methods are accessible to Design
        public SimplePin getClockPin () { return this.clock; }
        public SimplePin getResetPin () { return this.reset; }
        private GlobalReset.Physical getGSR () { return this.gsr; }
        public String getDomainKeyString () { return this.domainSpec; }

        public void connectComponentToDomain (Component comp)
        {
            if (comp.getClockPort().isUsed())
                this.clock.connectBus(Collections.singleton(comp.getClockPort()));
            if (comp.getResetPort().isUsed())
                comp.getResetPort().setBus(this.gsr.getResetOutput());
        }
        
        public static String[] parse (String spec)
        {
            String[] result = {"",""};
            String[] split = spec.split(":");
            if (split.length > 0) result[0] = split[0];
            else throw new IllegalArgumentException("Cannot parse clock domain");
            if (split.length > 1) result[1] = split[1];
            return result;
        }
        
        
        private static class ControlPin extends SimplePin
        {
            private ControlPin (String name)
            {
                super(1,name);
            }
        }
    }
    
}






