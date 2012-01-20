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
package net.sf.openforge.lim.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Value;
import net.sf.openforge.util.SizedInteger;


/**
 * @author gandhij
 *
 * Helper class to Emulate a Loop
 */
public class LoopEmulator extends Emulator{

    /**
     *  loop to be emulated
     */
    private Loop loop = null;

    /**
     * The feedbackMap contains (loopBody port, feedback entry bus
     * that feeds the port) pairs.
     */
    private Map feedbackMap = null;

    /**
     * The feedbackRegMap contains (loopBody port, feedback register
     * providing data to this port)
     */
    private Map feedbackRegMap = null;

    /**
     * Map of (Reg feedbackReg, LinkedList valuelist) pairs.
     * The linked list holds the value that the feedback register
     * takes through each iteration of the loop.
     */
    private Map feedbackRegisterValues = null;

    /**
     * The latchMap contains (loopBody port, latch output bus
     * that provides data to the port) pairs. If the loopBody
     * port is not connected to a latch, the bus will be null.
     */

    private Map latchMap = null;

    /**
     * The latchValuesMap contains (loopBody port, latch bus value)
     * pairs.
     */
    private Map latchValues = null;

    /**
     * Emulator for the loopBody
     */
    private LoopBodyEmulator loopBodyEmulator = null;

    /**
     * collection of unEmulatable components cached
     * here to speed up the emulation process
     */
    private Collection unEmulatable = new ArrayList();

    /**
     * flag to say if the loop iterations are done
     */
    private boolean isDone = false;

    /**
     * LoopEmulator Constructor
     * @param loop - Loop to be emulated
     */
    public LoopEmulator(Loop loop) {
        super();

        this.loop = loop;
        componentList = new ArrayList();
        componentList.add(loop.getBody());

        /* initialize the feedbackValues list */
        feedbackRegisterValues = new HashMap();
        Iterator iter = loop.getFeedbackPoints().iterator();
        while(iter.hasNext()){
            Reg register = (Reg)iter.next();
            feedbackRegisterValues.put(register, new LinkedList());
        }

        loopBodyEmulator = new LoopBodyEmulator(loop);
    }

    /**
     * Record the starting port values for the loopBody. The value
     * is obtained by looking at the {@link Dependency} for the
     * loop body's initialization {@link Entry}.
     */
    private HashMap recordStartingPorts (){
        HashMap startPorts = new HashMap();
        Iterator iter = loop.getBody().getDataPorts().iterator();
        while(iter.hasNext()){
            Port port = (Port)iter.next();

            final Collection initDeps = loop.getBodyInitEntry().getDependencies(port);
            if (initDeps.size() != 1)
            {
                continue;
            }

            final Dependency initDep = (Dependency)initDeps.iterator().next();
            final Bus initBus = initDep.getLogicalBus();
            final Value initValue = initBus.getValue();

            if (initValue == null){
                continue;
            }

            if (initValue.isConstant()){
                startPorts.put(port, SizedInteger.valueOf(initValue.getValueMask() & initValue.getCareMask(),
                        initValue.getSize(), initValue.isSigned()));
            }

        }
        return startPorts;
    }

    /**
     * Get the loop body input Reg that provides data to the
     * feedback entry of the loopBody for the given loopBody port.
     *
     * @param loop - loop under consideration
     * @param port - loopBody port whose input Reg is required
     * @return Reg - Reg that provides data to the feedback entry
     *                 of the loopBody for the input port.
     */
    private Reg getLoopBodyInputReg(Loop loop, Port port){
        Entry feedbackEntry = loop.getBodyFeedbackEntry();
        Reg reg = null;
        if(!feedbackEntry.getDependencies(port).isEmpty()){
            Dependency dep = (Dependency)feedbackEntry.getDependencies(port).iterator().next();
            reg = (Reg)dep.getLogicalBus().getOwner().getOwner();
        }
        return reg;
    }

    /**
     * Get the output bus of the Reg that provides data
     * to the given port. If the data provider is not a
     * Reg returns null.
     *
     * @param loop - loop whose loopbody data port's data providing
     *               bus is required
     * @param port - port whose input bus from a Reg is required
     * @return Bus - Reg output bus connected to this port
     */
    private Bus getLoopBodyInputRegBus(Loop loop, Port port){
        Entry feedbackEntry = loop.getBodyFeedbackEntry();
        if(feedbackEntry.getDependencies(port).isEmpty()){
            return null;
        }
        Dependency dep = (Dependency)feedbackEntry.getDependencies(port).iterator().next();
        Object component = dep.getLogicalBus().getOwner().getOwner();

        Reg register = null;
        try{
            register = (Reg)component;
        }catch(ClassCastException ex){
            return null;
        }

        Dependency regDep = (Dependency)((Entry)register.getEntries().iterator().next())
        .getDependencies((Port)register.getDataPorts().iterator().next())
        .iterator().next();

        return regDep.getLogicalBus();
    }

    /**
     * Get the loop body input latch that provides data to the
     * feedback entry of the loopBody for the given loopBody port.
     *
     * @param loop - loop under consideration
     * @param port - loopBody port whose input latch is required
     * @return Latch - latch that provides data to the feedback entry
     *                 of the loopBody for the input port.
     */
    private Latch getLoopBodyInputLatch(Loop loop, Port port){
        Entry feedbackEntry = loop.getBodyFeedbackEntry();
        Latch latch = null;
        if(!feedbackEntry.getDependencies(port).isEmpty()){
            Dependency dep = (Dependency)feedbackEntry.getDependencies(port).iterator().next();
            latch = (Latch)dep.getLogicalBus().getOwner().getOwner();
        }
        return latch;
    }

    /**
     * Get the output bus of the Latch that provides data
     * to the given port. If the data provider is not a
     * Latch returns null.
     *
     * @param loop - loop whose loopbody data port's data providing
     *               bus is required
     * @param port - port whose input bus from a Larch is required
     * @return Bus - Latch output bus connected to this port
     */
    private Bus getLoopBodyInputLatchBus(Loop loop, Port port){

        Entry feedbackEntry = loop.getBodyFeedbackEntry();
        if(feedbackEntry.getDependencies(port).isEmpty()){
            return null;
        }
        Dependency dep = (Dependency)feedbackEntry.getDependencies(port).iterator().next();
        Object component = dep.getLogicalBus().getOwner().getOwner();

        Latch latch = null;
        try{
            latch = (Latch)component;
        }catch(ClassCastException ex){
            return null;
        }
        return latch.getResultBus();
    }

    /**
     * returns the component that is connected to the given port of
     * the loop for the feedback entry. The returned component can be
     * a Reg or a Latch.
     *
     * @param loop
     * @param port
     * @return component which connects to the loops given port for the
     *          feedback entry. the component can be a Reg or a Latch.
     */
    private Component getLoopBodyInputComponent(Loop loop, Port port){

        Entry feedbackEntry = loop.getBodyFeedbackEntry();
        Component component = null;
        if(!feedbackEntry.getDependencies(port).isEmpty()){
            Dependency dep = (Dependency)feedbackEntry.getDependencies(port).iterator().next();
            component = (Component)dep.getLogicalBus().getOwner().getOwner();
        }

        return component;
    }

    /**
     * Update the feedbackMap and the feedbackRegMap.
     *
     * The feedbackMap contains (loopBody port, feedback entry bus)
     * pairs.
     *
     * The feedbackRegMap contains (loopBody port, feedback register
     * providing data to this port)
     *
     */
    private void updateFeedbackMap(){
        feedbackMap = new HashMap();
        feedbackRegMap = new HashMap();
        Iterator iter = loop.getBody().getDataPorts().iterator();
        while(iter.hasNext()){
            Port port = (Port)iter.next();
            Bus bus = getLoopBodyInputRegBus(loop, port);
            if(bus != null){
                feedbackMap.put(port,bus );
                feedbackRegMap.put(port, getLoopBodyInputReg(loop, port));
            }
        }
    }

    /**
     * Update the latchMap and LatchValues map
     *
     * The latchMap contains (loopBody port, latch output bus
     * that provides data to the port) pairs. If the loopBody
     * port is not connected to a latch, the bus will be null.
     *
     * The latchValuesMap contains (loopBody port, latch bus value)
     * pairs.
     */
    private void updateLatchMap(){
        latchMap = new HashMap();
        latchValues = new HashMap();
        Iterator iter = loop.getBody().getDataPorts().iterator();
        while(iter.hasNext()){
            Port port = (Port)iter.next();
            if(getLoopBodyInputComponent(loop, port) instanceof Latch){
                Latch latch = getLoopBodyInputLatch(loop, port);
                if(latch!=null){
                    Bus bus = getLoopBodyInputLatchBus(loop, port);
                    if(bus != null){
                        if(bus.getValue().isConstant()){
                            latchMap.put(port,bus);
                            latchValues.put(port, bus.getValue().toNumber());
                        }
                    }
                }
            }
        }
    }

    /**
     * Emulate the loop
     *
     * @param inputValues a map of input ports to inital values
     * @return A map of output busses to their values.
     * @throws UnEmulatableLoopException is thrown if the loop is not emulatable
     */
    public Map emulate(Map inputValues) throws UnEmulatableLoopException{
        // System.out.println("EMULATING LOOP " + loop);
        HashMap outputValues = new HashMap();

        updateInputMap();
        updateFeedbackMap();
        updateLatchMap();

        int NumIters = loop.getIterations();
        HashMap portValues = recordStartingPorts();

        int count=0;
        boolean done = false;
        while(!done){
            count++;
            done = iterate(portValues);
            if(count > loop.getIterations() + 1){
                return null;
            }
        }
        componentList.removeAll(unEmulatable);

        return outputValues;
    }

    /**
     * Emulate one iteration of the loop. Essentially emulate the loopBody once
     * This function can be called repeatedly to do subsequent iterations until
     * the return value is true;
     *
     * @param inputValues Map of input ports to their values
     * @return boolean value indicating if all iterations are done.
     * @throws UnEmulatableLoopException
     */
    private boolean iterate(Map inputValues) throws UnEmulatableLoopException{

        Map outputValues = new HashMap();
        /*
         * Prime the input bus value map with the given port values.
         */
        boolean isDone = false;

        Map busValues = portToBusValues(inputValues);
        Component loopBody = loop.getBody();
        Map portValues = busToPortValues(loopBody, busValues);

        outputValues = loopBodyEmulator.emulate(portValues);
        isDone = loopBodyEmulator.getDone();

        if(outputValues!=null){
            busValues.putAll(outputValues);
        }

        /*
         * Save the output values as the input values for the next iteration.
         */
        for (Iterator iterate = inputValues.keySet().iterator(); iterate.hasNext();)
        {
            final Port port = (Port)iterate.next();
            Component inputComponent = getLoopBodyInputComponent(loop, port);
            if(inputComponent instanceof Reg){
                inputValues.put(port, busValues.get(feedbackMap.get(port)));
            }else if(inputComponent instanceof Latch){
                inputValues.put(port, latchValues.get(port));
            }

            /* Save the feedbackRegister values observed during this iteration */
            Component comp = getLoopBodyInputComponent(loop, port);
            /* we dont care about latches */
            if(comp instanceof Reg){
                Reg loopbodyReg = getLoopBodyInputReg(loop, port);
                ((LinkedList)feedbackRegisterValues.get(loopbodyReg))
                .add(busValues.get(feedbackMap.get(port)));
            }
        }

        // System.out.println("INPUT VALUES FOR NEXT ITERATION = " + inputValues);
        return isDone;
    }

    /**
     * Get the bus from the inputBlock of the loop that provides
     * data for the first iteration of the loop for this loopBody port.
     *
     * @param port - LoopBody port whose initblock bus is required
     * @return bus - bus from the init block of the loop that
     *               provides data to the port.
     */
    protected Bus getInitBlockInputBus (Port port)
    {
        final Component entryOwner =  port.getOwner();
        entryOwner.getEntries().iterator().next();
        final Entry initEntry = (Entry)entryOwner.getEntries().iterator().next();
        final Collection deps = initEntry.getDependencies(port);

        /*
         * Handle connections that were established manually rather than through
         * dependency resolution.
         */
        if (deps.isEmpty() && port.isConnected()){
            return port.getBus();
        }

        if (deps.size() != 1)
        {
            return null;
        }

        Bus inputBus = null;
        if(deps.size() != 0){
            final Dependency dep = (Dependency)deps.iterator().next();
            inputBus = dep.getLogicalBus();
        }

        return inputBus;
    }

    /**
     * get the feedbackRegisterValue Map - this map contains
     * (Reg - feedback register, LinkedList (SizedInteger iter1 value, iter2 value...))
     * pairs.
     *
     * @return feedbackRegisterValues Map
     */
    public Map getFeedbackRegisterValues() {
        return feedbackRegisterValues;
    }
}
