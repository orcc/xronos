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

package net.sf.openforge.schedule;

import java.util.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.OptionString;
import net.sf.openforge.forge.api.entry.*;
import net.sf.openforge.forge.api.pin.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.optimize.constant.*;
import net.sf.openforge.schedule.loop.LoopFlopAnalysis;
import net.sf.openforge.util.naming.*;

/**
 * <code>GlobalConnector</code> is a visitor which is used to traverse
 * the entire LIM {@link Design} hierarchy and create data and control
 * paths from local resource accesses to the global instantiations of
 * those resources. The creation of these paths may include the
 * addition of {@link Port Ports}, {@link Exit Exits},
 * {@link Bus Buses}, and gateways in the {@link Module Modules} that
 * contain global accesses.
 * 
 * <P>
 * GlobaleConnector also tracks the relative priorities of
 * {@link PinWrite PinWrites} and
 * {@link PinStateChange PinStateChanges}, both immediate and
 * synchronous, for each {@link PinBuf}.  This information can then be
 * used when merging the writes at the global level.
 * </p>
 *
 * @version $Id: GlobalConnector.java 562 2008-03-17 21:33:12Z imiller $
 */
public class GlobalConnector extends FilteredVisitor
{
    private static final String _RCS_ = "$Rev: 562 $";

    /** A stack of ConnectionFrames. */
    private Stack frames = new Stack();

    private Design design = null;
    private LatencyCache latCache;
    
    GlobalConnector (LatencyCache cache)
    {
        this.latCache = cache;
    }
    
    /**
     * The Design should be the first Visitable that is visited by the
     * GlobalConnector, indicating the initialization of processing.
     *
     * @param design Description of Parameter
     */
    public void visit (Design design)
    {
        if (_schedule.db)
        {
            _schedule.ln(_schedule.GVISIT, "visiting design " + design);
        }
        final GenericJob job = EngineThread.getGenericJob();

        this.design = design;

        beginFrame();
        traverse(design);
        connectGlobalRegisters(design, getFrame());
        generateKickers();

        if (job.getUnscopedBooleanOptionValue(OptionRegistry.NO_BLOCK_IO))
        {
            connectTasks(design);
        }

        design.accept(new MemoryConnectionVisitor());
        // Connect all SimplePinRead and SimplePinWrite accesses to
        // their targetted pins.
        design.accept(new SimplePinConnector());

        // Connect clock and reset to all design module elements.  Do
        // this last in case any of the other connectors add global
        // level stuff.
        for (Iterator iter = design.getDesignModule().getComponents().iterator(); iter.hasNext();)
        {
            // Find the clock domain for each design level element and
            // connect the clock and/or reset pins.
            final Component designComp = (Component)iter.next();
            final String domainSpec = (String)((OptionString)EngineThread.getGenericJob().getOption(OptionRegistry.CLOCK_DOMAIN)).getValue(designComp.getSearchLabel());
            assert domainSpec != null : "No clock domain specifier found for " + designComp;
            design.getClockDomain(domainSpec).connectComponentToDomain(designComp);
        }
        
    }
    
    /** 
     * set up the pins for clock and reset, if used by this task
     * if used, but not defined, default to the global values
     */
    public void visit (Task task)
    {
        if (_schedule.db)
        {
            _schedule.ln(_schedule.GVISIT, "visiting task "+task);
            _schedule.d.inspect(task,"/tmp/task"+task);
        }
        super.visit(task);
    }
    

    /**
     * DOCUMENT ME!
     *
     * @param call DOCUMENT ME!
     */
    public void visit (Call call)
    {
        if (_schedule.db)
        {
            _schedule.ln(_schedule.GVISIT, "visiting call " + call);
        }

        beginFrame();
        call.getProcedure().accept(this);
        ConnectionFrame procedureFrame = endFrame();

        // record duplicates of the procedure's connections
        // Some connections contain the same buses (done & address)
        // which we only want 1 new port for not multiples...
        Map duplicatedMap = new HashMap();
        //for (Iterator resources = procedureFrame.getConnectedResources().iterator(); resources.hasNext();)
//         {
//             Resource resource = (Resource)resources.next();
        for (Resource resource : procedureFrame.getConnectedResources())
        {
            Collection readConnections = procedureFrame.getReadConnections(resource);
            for (Iterator reads = readConnections.iterator(); reads.hasNext();)
            {
                Connection read = (Connection)reads.next();
                Connection duplicate = read.duplicate(call, duplicatedMap);
                mapPortsAndBuses(call, duplicate, read);
                recordRead(duplicate, call);
            }

            Collection writeConnections = procedureFrame.getWriteConnections(resource);
            for (Iterator writes = writeConnections.iterator(); writes.hasNext();)
            {
                Connection write = (Connection)writes.next();
                Connection duplicate = write.duplicate(call, duplicatedMap);
                mapPortsAndBuses(call, duplicate, write);
                recordWrite(duplicate, call);
            }
        }
    }

    /**
     * Create the physical implementation of this PinRead and create a
     * new Connection for it.
     */
    public void visit (PinRead pinRead)
    {
        super.visit(pinRead);
        throw new UnsupportedOperationException("Deprecated LIM element " + pinRead);
    }

    /**
     * Create the physical implementation of this PinWrite and create a
     * new Connection for it.
     */
    public void visit (PinWrite pinWrite)
    {
        super.visit(pinWrite);
        throw new UnsupportedOperationException("Deprecated LIM element " + pinWrite);
    }

    /**
     * Create the physical implementation of this PinStateChange and
     * create a new Connection for it.
     */
    public void visit (PinStateChange pinStateChange)
    {
        super.visit(pinStateChange);
        throw new UnsupportedOperationException("Deprecated LIM element " + pinStateChange);
    }

    /**
     * DOCUMENT ME!
     *
     * @param module DOCUMENT ME!
     */
    public void preFilter (Module module)
    {
        if (_schedule.db)
        {
            _schedule.ln(_schedule.GVISIT, "Entering module " + module);
        }

        super.preFilter(module);
        beginFrame();
    }

    public void filter (Module module)
    {
        if (_schedule.db)
        {
            _schedule.ln(_schedule.GVISIT, "Completing module " + module);
        }

        super.filter(module);

        ConnectionFrame frame = endFrame();
        for (Iterator iter = frame.getConnectedResources().iterator();
                iter.hasNext();)
        {
            Resource resource = (Resource)iter.next();

            if (resource instanceof Register)
            {
                connectRegister(module, (Register)resource, frame);
            }
        }

        copyPinConnections(module, frame);
    }

    public void visit (RegisterRead regRead)
    {
        if (_schedule.db)
        {
            _schedule.ln(_schedule.GVISIT, "visiting RegisterRead " + regRead);
        }

        regRead.makeSidebandConnections();
        recordRead(new RegisterReadConnection(
                       regRead.getResource(),
                       regRead.getSidebandDataPort()));
    }

    public void visit (RegisterWrite regWrite)
    {
        if (_schedule.db)
        {
            _schedule.ln(_schedule.GVISIT, "visiting RegisterWrite " + regWrite);
        }

        regWrite.makeSidebandConnections();
        recordWrite(new RegisterWriteConnection(
                        regWrite.getResource(),
                        regWrite.getSidebandWEBus(),
                        regWrite.getSidebandDataBus()));
    }

    protected Exit getSidebandExit (Component c)
    {
        Exit sideExit = c.getExit(Exit.SIDEBAND);
        if (sideExit == null)
        {
            sideExit = c.makeExit(0, Exit.SIDEBAND);
        }

        return sideExit;
    }

    protected void traverse(Block block)
    {
        block.getInBuf().accept(this);
        traverse(block.getSequence());
        traverse(block.getOutBufs());
    }

    protected void traverse (Loop loop)
    {
        loop.getInBuf().accept(this);
        loop.getInitBlock().accept(this);
        if (loop.getBody() != null)
        {
            loop.getBody().accept(this);
        }

        traverse(loop.getOutBufs());
    }

    protected void traverse (WhileBody whileBody)
    {
        whileBody.getInBuf().accept(this);
        whileBody.getDecision().accept(this);
        if (whileBody.getBody() != null)
        {
            whileBody.getBody().accept(this);
        }

        traverse(whileBody.getOutBufs());
    }

    protected void traverse (UntilBody untilBody)
    {
        untilBody.getInBuf().accept(this);
        if (untilBody.getBody() != null)
        {
            untilBody.getBody().accept(this);
        }
        if (untilBody.getDecision() != null)
        {
            untilBody.getDecision().accept(this);
        }

        traverse(untilBody.getOutBufs());
    }
    
    protected void traverse (ForBody forBody)
    {
        forBody.getInBuf().accept(this);
        forBody.getDecision().accept(this);
        if (forBody.getBody() != null)
        {
            forBody.getBody().accept(this);
        }
        /*
         * Could be null if there's no increment
         */
        if (forBody.getUpdate() != null)
        {    
            forBody.getUpdate().accept(this);
        }

        traverse(forBody.getOutBufs());
    }

    protected void traverse (Branch branch)
    {
        branch.getInBuf().accept(this);
        branch.getDecision().accept(this);
        branch.getTrueBranch().accept(this);
        branch.getFalseBranch().accept(this);
        traverse(branch.getOutBufs());
    }

    protected void traverse (Switch sw)
    {
        traverse((Block)sw);
    }

    private void traverse (Collection collection)
    {
        for (Iterator iter = collection.iterator(); iter.hasNext();)
        {
            ((Visitable)iter.next()).accept(this);
        }
    }

    /**
     * Completes RegisterRead and RegisterWrite connections passed up
     * to the top-level as side-band ports and buses.
     *
     * @param design the Design which has Registers
     * @param frame the ConnectionFrame which describes the read/write
     *        connections
     */
    private void connectGlobalRegisters (Design design, ConnectionFrame frame)
    {
        boolean isLittleEndian = EngineThread.getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.LITTLE_ENDIAN);
        
        for (Iterator registers = design.getRegisters().iterator();
                registers.hasNext();)
        {
            Register register = (Register)registers.next();
            List readList = frame.getReadConnections(register);
            List writeList = frame.getWriteConnections(register);

            if (readList.isEmpty() && writeList.isEmpty())
            {
                continue;
            }
            
            Component regPhys = register.makePhysicalComponent(readList, writeList);
            if (register.getInputSwapper() != null)
                design.getDesignModule().addComponent(register.getInputSwapper());
            design.getDesignModule().addComponent(regPhys);
            if (register.getOutputSwapper() != null)
                design.getDesignModule().addComponent(register.getOutputSwapper());
            
            assert regPhys != null;
            
            if (!writeList.isEmpty())
            {
                final Iterator writePortIter = regPhys.getDataPorts().iterator();
                for (Iterator writeListIter = writeList.iterator(); writeListIter.hasNext();)
                {
                    final RegisterWriteConnection writeConn = (RegisterWriteConnection)writeListIter.next();
                    if (writeConn != null)
                    {
                        assert writePortIter.hasNext() : "Too few ports on register physical (enable)";
                        final Port enablePort = (Port)writePortIter.next();
                        assert writePortIter.hasNext() : "Too few ports on register physical (data)";
                        final Port dataPort = (Port)writePortIter.next();
                        enablePort.setBus(writeConn.getEnable());
                        dataPort.setBus(writeConn.getData());
                    }
                }
            }

            if (!readList.isEmpty())
            {
                Bus registerResultBus = null;
                Exit physicalExit = regPhys.getExit(Exit.DONE);
                registerResultBus = (Bus)physicalExit.getDataBuses().get(0);

                for (Iterator rpIter = readList.iterator(); rpIter.hasNext();)
                {
                    RegisterReadConnection rp = (RegisterReadConnection)rpIter.next();
                    // The read connetion may be null because we had
                    // to pad out the pairs in the connection list for
                    // the referee. 
                    if (rp != null)
                    {
                        rp.getDataPort().setBus(registerResultBus);
                    }
                }
            }
        }
    }

    /**
     * Creates Pins on the design to connect each task's I/O.
     *
     * @param design the Design to be connected
     */
    private void connectTasks (Design design)
    {
        Map kickers = new HashMap();
        
        for (Iterator tasks = design.getTasks().iterator(); tasks.hasNext();)
        {
            Task task = (Task)tasks.next();
            Call call = task.getCall();

            // Note: when creating the InputPins, they are based on the related
            // procedure port instead of the call port, because the InputPin needs
            // the Port's peer bus to get sizing information
            
            // might have a this port
            if (task.getCall().getThisPort() != null)
            {
                int width = task.getCall().getThisPort().getValue().getSize();
                
                /*
                 * The base address of the top level object reference is always
                 * 0, since it isn't actually stored in memory.
                 */
                Constant constant = new SimpleConstant(0, width);
                task.setHiddenConstant(constant);
            }
            
            // If module_builder, then publish the ports, and don't use a kicker
            if (EngineThread.getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.MODULE_BUILDER))
            {
                // get the entry method
                EntryMethod em = call.getProcedure().getEntryMethod();

                Port go = call.getGoPort();
                if (go.isUsed())
                {
                    Pin p = connectInputPin(design, call, go, call.getGoName());
                    if (em != null)
                    {
                        p.setApiPin(em.getGoPin());
                    }
                }

                ArrayList dataPorts = new ArrayList(call.getDataPorts());

                // 'this' port is handled specially.
                dataPorts.remove(call.getThisPort());
                int index = 0;
                for (Iterator dports = dataPorts.iterator(); dports.hasNext();)
                {
                    Port port = (Port)dports.next();
                    if (port.getTag().equals(Component.NORMAL))
                    {
                        if (port.isUsed())
                        {
                            Pin p = connectInputPin(design, call, port, null);
                            if (em != null)
                            {
                                p.setApiPin(em.getArgPin(index));
                            }
                        }
                    }

                    index++;
                }

                Exit exit = call.getExit(Exit.DONE);
                Bus done = exit.getDoneBus();
                if (done.isUsed())
                {
                    OutputPin pin = connectOutputPin(design, call, done,
                                                     call.getProcedure().getDoneName());
                    if (em != null)
                    {
                        pin.setApiPin(em.getDonePin());
                    }
                }

                for (Iterator dbuses = exit.getDataBuses().iterator();
                        dbuses.hasNext();)
                {
                    Bus bus = (Bus)dbuses.next();
                    if (bus.getTag().equals(Component.NORMAL))
                    {
                        if (bus.isUsed())
                        {
                            OutputPin pin = connectOutputPin(design, call, bus,
                                                             call.getProcedure().getResultName());
                            if (em != null)
                            {
                                pin.setApiPin(em.getResultPin());
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateKickers ()
    {
        final GenericJob job = EngineThread.getGenericJob();
        final boolean blockIO = !job.getUnscopedBooleanOptionValue(OptionRegistry.NO_BLOCK_IO); //If Do BlockIO
        final boolean doBlockSched = !job.getUnscopedBooleanOptionValue(OptionRegistry.SCHEDULE_NO_BLOCK_SCHEDULING);
        final boolean moduleBuilder = EngineThread.getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.MODULE_BUILDER);

        final Map<String,Kicker> kickers = new HashMap();
        for (Task task : design.getTasks())
        {
            final Call call = task.getCall();
            final String domainSpec = (String)((OptionString)EngineThread.getGenericJob().getOption(OptionRegistry.CLOCK_DOMAIN)).getValue(call.getSearchLabel());

            if (!task.isKickerRequired())
            {
                // Ensure that the Go port is validly connected
                if (!call.getGoPort().isConnected())
                {
                    job.warn("Task " + call.showIDLogical() + " is not autostart and has no enabling signal");
                    Constant constant = new SimpleConstant(0, 1);
                    constant.pushValuesForward();
                    call.getGoPort().setBus(constant.getValueBus());
                    call.getGoPort().pushValueForward();
                }
                continue;
            }
            
            Kicker kicker;
            final String kickerKey;
            if (doBlockSched && !moduleBuilder)
            {
                // Connect up a KickerContinuous that will keep us running at
                // max rate indefinately.  We expect only one task in block
                // io, but if we have more than one, we need individual
                // kickers so that we use the right clock/reset for each.
                kicker = new Kicker.KickerContinuous();
                // Use the kickers string b/c there will be a (unique)
                // feedback from the task done
                kickerKey = kicker.toString();
            }
            else if (blockIO)
            {
                boolean fbRequired = checkFeedback(call.getProcedure().getBody());
                //System.out.println("Generated perpetual kicker withT/withoutF " + fbRequired + " feedback flop required");
                // Connect up a KickerPerpetual that will keep us running
                // indefinately.  We expect only one task in block io, but
                // if we have more than one, each needs its own kicker
                // anyway (since we OR back in the done).  We could use
                // just one kicker for all and put the OR external but
                // there is no Design level module to put this stuff in.
                // Argh!
                kicker = new Kicker.KickerPerpetual(fbRequired);
                // Use the kickers string b/c there will be a (unique)
                // feedback from the task done
                kickerKey = kicker.toString();
            }
            else
            {
                if (moduleBuilder)
                {
                    continue;
                }
                
                // find a kicker that matches this calls clk and reset
                //kickerKey = call.getClockName() + call.getResetName();
                kickerKey = domainSpec;
                kicker = (Kicker)kickers.get(kickerKey);
                // if we didn't find it, construct a new one
                if (kicker == null)
                {
                    kicker=new Kicker();
                }
            }
            if (!kickers.containsKey(kickerKey))
            {
                kickers.put(kickerKey, kicker);
                design.addComponentToDesign(kicker);
                design.getClockDomain(domainSpec).connectComponentToDomain(kicker);
            }

            if (call.getGoPort().isUsed())
            {
                call.getGoPort().setBus(kicker.getDoneBus());
            }

            if (kicker.getRepeatPort() != null)
            {
                Bus callDone = call.getExit(Exit.DONE).getDoneBus();
                if (!callDone.isUsed())
                {
                    // there is the off-chance that it has fixed timing
                    // and thus needs no done.  Create a sequence of flops
                    // to bypass.
                    if (!call.getLatency().isFixed())
                    {
                        EngineThread.getEngine().fatalError("Unexpected internal state.  Design does not have a control structure and has variable timing");
                    }
                    Bus done = kicker.getDoneBus();
                    for (int i=0; i < call.getLatency().getMinClocks(); i++)
                    {
                        // Needs RESET b/c it is in the control path
                        Reg flop = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(call) + "_go_done_" + i);
                        design.getClockDomain(domainSpec).connectComponentToDomain(flop);
                        flop.getInternalResetPort().setBus(flop.getResetPort().getBus());
                        flop.getDataPort().setBus(done);
                        // Ensure that we've constant propagated the flop
                        flop.propagateValuesForward();
                        done = flop.getResultBus();
                        design.addComponentToDesign(flop);
                    }
                    callDone = done;
                }
                kicker.getRepeatPort().setBus(callDone);
            }
        }
    }
    
    
    private InputPin connectInputPin (Design design, Call call, Port port, String name)
    {
        Port procedurePort = call.getProcedurePort(port);
        InputPin pin = new InputPin(procedurePort);
        port.setBus(pin.getBus());
        IDSourceInfo info = (name != null) ? call.getProcedure().getIDSourceInfo().deriveField(name, -1, -1) :
            procedurePort.getIDSourceInfo();
        pin.setIDSourceInfo(info);
        Bus pinBus = pin.getBus();
        pinBus.setUsed(true);
        port.setBus(pinBus);
        design.addInputPin(pin, port);
        return pin;
    }

    private OutputPin connectOutputPin (Design design, Call call, Bus bus, String name)
    {
        OutputPin pin = new OutputPin(bus);
        pin.setIDSourceInfo(call.getIDSourceInfo().deriveField(name, -1, -1));
        pin.getPort().setBus(bus);
        design.addOutputPin(pin, bus);
        return pin;
    }

    private void mapPortsAndBuses (Call call, Connection callSide,
        Connection procSide)
    {
        Iterator callPorts = callSide.getPorts().iterator();
        Iterator procPorts = procSide.getPorts().iterator();
        while (callPorts.hasNext() && procPorts.hasNext())
        {
            call.setProcedurePort((Port)callPorts.next(), (Port)procPorts.next());
        }

        Iterator callBuses = callSide.getBuses().iterator();
        Iterator procBuses = procSide.getBuses().iterator();
        while (callBuses.hasNext() && procBuses.hasNext())
        {
            Bus callBus = (Bus)callBuses.next();
            Bus procBus = (Bus)procBuses.next();
            //call.setProcedureBus((Bus)callBuses.next(), (Bus)procBuses.next());
            call.setProcedureBus(callBus, procBus);
            // Dont forget the done bus.  We may not use it but the
            // relationship should still exist.
            call.setProcedureBus(callBus.getOwner().getDoneBus(), procBus.getOwner().getDoneBus());
        }
    }

    /**
     * Utility method for wiring up the reads and writes captured in a
     * frame to appropriate ports and buses on a procedure.
     *
     * @param body DOCUMENT ME!
     * @param register DOCUMENT ME!
     * @param subFrame DOCUMENT ME!
     */
    private void connectRegister (Module body, Register register,
        ConnectionFrame subFrame)
    {
        // wire all the reads to a new port on the procedure's body
        List readList = subFrame.getReadConnections(register);
        if (!readList.isEmpty())
        {
            Port resourceRead = body.makeDataPort(Component.SIDEBAND);
            Bus resourceReadBus = resourceRead.getPeer();
            resourceReadBus.setIDLogical(register.showIDLogical() + "_read");
            resourceReadBus.setSize(register.getInitWidth(), register.isSigned());

            recordRead(new RegisterReadConnection(register, resourceRead));

            for (Iterator readConnections = readList.iterator();
                    readConnections.hasNext();)
            {
                RegisterReadConnection rp = (RegisterReadConnection)readConnections.next();
                rp.getDataPort().setBus(resourceReadBus);
            }
        }

        // wire the writes. a single write goes directly, multiple go through a gateway
        List writeList = subFrame.getWriteConnections(register);
        if (!writeList.isEmpty())
        {
            Exit bodyExit = getSidebandExit(body);
            Bus writeEnable = bodyExit.makeDataBus(Component.SIDEBAND);
            writeEnable.setIDLogical(register.showIDLogical() + "_enable");
            writeEnable.setSize(1, false);
            Port writeEnablePort = writeEnable.getPeer();
            Bus writeData = bodyExit.makeDataBus(Component.SIDEBAND);
            writeData.setIDLogical(register.showIDLogical() + "_write");
            writeData.setSize(register.getInitWidth(), register.isSigned());

            recordWrite(new RegisterWriteConnection(register, writeEnable, writeData));

            if (writeList.size() == 1)
            {
                RegisterWriteConnection edp = (RegisterWriteConnection)writeList.get(0);
                writeEnable.getPeer().setBus(edp.getEnable());
                writeData.getPeer().setBus(edp.getData());
            }
            else
            {
                RegisterGateway regGateway = new RegisterGateway(writeList.size(),
                        register);
                writeEnable.getPeer().setBus(regGateway.getGlobalEnableBus());
                writeData.getPeer().setBus(regGateway.getGlobalDataBus());
                Iterator gatewayLocalEnablePortIter = regGateway.getLocalEnablePorts().iterator();
                Iterator gatewayLocalDataPortIter = regGateway.getLocalDataPorts().iterator();

                // Wires up RegisterGateway's enable ports paired with data ports.
                int pairCount = 0;
                for (Iterator pairBusIter = writeList.iterator();
                        pairBusIter.hasNext();)
                {
                    RegisterWriteConnection edPair = (RegisterWriteConnection)pairBusIter.next();
                    edPair.getEnable().setIDLogical(register.showIDLogical() + "_we_" + pairCount);
                    ((Port)gatewayLocalEnablePortIter.next()).setBus(edPair.getEnable());
                    edPair.getData().setIDLogical(register.showIDLogical() + "_data_" + pairCount);
                    ((Port)gatewayLocalDataPortIter.next()).setBus(edPair.getData());
//                     edPair.getSize().setIDLogical(register.showIDLogical() + "_size_" + pairCount);
//                     ((Port)gatewayLocalSizePortIter.next()).setBus(edPair.getSize());
                    pairCount++;
                }

                body.addComponent(regGateway);
            }
        }
    }

    /**
     * Utility method for wiring up the reads captured in a frame to
     * appropriate ports and buses on a procedure.
     *
     * @param body DOCUMENT ME!
     * @param subFrame DOCUMENT ME!
     */
    private void copyPinConnections (Module body, ConnectionFrame subFrame)
    {
        // Was used for only API pins.  The handling of which has now
        // been removed.
    }

    /**
     * Gets the current ConnectionFrame (the one at the top of the stack).
     *
     * @return DOCUMENT ME!
     */
    private ConnectionFrame getFrame ()
    {
        return (ConnectionFrame)frames.peek();
    }

    /**
     * Creates a new ConnectionFrame and pushes it onto the stack.
     *
     * @return the new frame
     */
    private ConnectionFrame beginFrame ()
    {
        frames.push(new ConnectionFrame());
        return getFrame();
    }

    /**
     * Pops and returns the current ConnectionFrame.
     *
     * @return DOCUMENT ME!
     */
    private ConnectionFrame endFrame ()
    {
        return (ConnectionFrame)frames.pop();
    }

    /**
     * Records a read connection to a resource in the current frame.
     *
     * @param connection the Connection occuring in the current frame
     */
    private void recordRead (Connection connection)
    {
        getFrame().recordRead(connection);
    }

    /**
     * Records a read connection at the call level with a link to the call
     *
     * @param connection
     * @param call the call where the connection is made
     */
    private void recordRead (Connection connection, Call call)
    {
        getFrame().recordRead(connection, call);
    }

    /**
     * Records a write connection to a resource in the current frame.
     *
     * @param connection the Connection occuring in the current frame
     */
    private void recordWrite (Connection connection)
    {
        getFrame().recordWrite(connection);
    }

    /**
     * Records a write connection at the call level, with a link to
     * the call
     *
     * @param connection
     * @param call the call where the connection is made
     */
    private void recordWrite (Connection connection, Call call)
    {
        getFrame().recordWrite(connection, call);
    }

    /**
     * A connection is a group of ports and buses which are related to a
     * particular {@link Resource}.
     */
    private abstract class Connection
    {
        Resource resource;
        protected List ports = new ArrayList();
        protected List buses = new ArrayList();

        public Connection (Resource resource)
        {
            this.resource = resource;
        }

        public Resource getResource ()
        {
            return resource;
        }

        public List getPorts ()
        {
            return ports;
        }

        public List getBuses ()
        {
            return buses;
        }

        protected Bus duplicateBus (Component c, Bus bus)
        {
            Exit exit = getSidebandExit(c);
            Bus dup = exit.makeDataBus(Component.SIDEBAND);
            dup.setIDLogical(ID.showLogical(bus));
            dup.copyAttributes(bus);
            return dup;
        }

        public abstract Connection duplicate (Call c, Map dupMap);

        protected void addPort (Port p)
        {
            ports.add(p);
        }

        protected void addBus (Bus b)
        {
            buses.add(b);
        }

        protected Bus getBus (Call c, Map dupMap, Bus orig)
        {
            Bus bus = (Bus)dupMap.get(orig);
            if (bus == null)
            {
                bus = duplicateBus(c, orig);
                dupMap.put(orig, bus);
            }

            return bus;
        }

        protected Port getPort (Call c, Map dupMap, Port orig)
        {
            Port port = (Port)dupMap.get(orig);
            if (port == null)
            {
                port = c.makeDataPort(Component.SIDEBAND);
                dupMap.put(orig, port);
            }

            return port;
        }

    } // inner class Connection


    private class RegisterWriteConnection extends Connection
    {
        Bus enable;
        Bus data;

        public RegisterWriteConnection (Resource resource, Bus enable, Bus data)
        {
            super(resource);
            this.enable = enable;
            this.data = data;
            addBus(enable);
            addBus(data);
        }

        public Bus getEnable ()
        {
            return enable;
        }

        public Bus getData ()
        {
            return data;
        }

        public Connection duplicate (Call c, Map dupMap)
        {
            Bus enable = getBus(c, dupMap, getEnable());
            Bus data = getBus(c, dupMap, getData());

            return new RegisterWriteConnection(getResource(), enable, data);
        }
    } // inner class RegisterWriteConnection


    private class RegisterReadConnection extends Connection
    {
        Port data;

        public RegisterReadConnection (Resource resource, Port data)
        {
            super(resource);
            if (data == null){try {throw new Exception();}catch (Exception e){e.printStackTrace();}}
            this.data = data;
            addPort(data);
        }

        public Port getDataPort ()
        {
            return data;
        }

        public Connection duplicate (Call c, Map dupMap)
        {
            Port dp = getPort(c, dupMap, getDataPort());

            return new RegisterReadConnection(getResource(), dp);
        }
    } // inner class RegisterReadConnection


    /**
     * A ConnectionFrame maintains state for Connections which are
     * related to the same Resource.
     */
    private class ConnectionFrame
    {
        LinkedHashSet<Resource> accessedResources = new LinkedHashSet();
        Map readMap = new LinkedHashMap();
        Map writeMap = new LinkedHashMap();

        public void recordRead (Connection connection, Call call)
        {
            recordRead(connection);
        }

        public void recordRead (Connection connection)
        {
            accessedResources.add(connection.getResource());
            addRead(connection.getResource(), connection);
        }
        
        void addRead (Resource res, Connection connection)
        {
            List connectionList = (List)readMap.get(res);
            if (connectionList == null)
            {
                connectionList = new ArrayList();
                readMap.put(res, connectionList);
            }

            connectionList.add(connection);
        }

        public void recordWrite (Connection connection, Call call)
        {
            recordWrite(connection);
        }

        public void recordWrite (Connection connection)
        {
            accessedResources.add(connection.getResource());
            addWrite(connection.getResource(), connection);
        }
        
        void addWrite (Resource res, Connection connection)
        {
            List connectionList = (List)writeMap.get(res);
            if (connectionList == null)
            {
                connectionList = new ArrayList();
                writeMap.put(res, connectionList);
            }

            connectionList.add(connection);
        }

        /**
         * return a copy of the read list
         *
         * @param resource DOCUMENT ME!
         *
         * @return DOCUMENT ME!
         */
        public List getReadConnections (Resource resource)
        {
            final List list = (List)readMap.get(resource);
            return (list == null)
            ? Collections.EMPTY_LIST : new ArrayList(list);
        }

        /**
         * return a copy of the write list
         *
         * @param resource DOCUMENT ME!
         *
         * @return DOCUMENT ME!
         */
        public List getWriteConnections (Resource resource)
        {
            final List list = (List)writeMap.get(resource);
            return (list == null)
            ? Collections.EMPTY_LIST : new ArrayList(list);
        }

        public Collection<Resource> getConnectedResources ()
        {
            return accessedResources;
        }

        public String toString ()
        {
            String ret = "";
            ret += ("Resources: " + accessedResources);
            ret += (" readMap: " + readMap);
            ret += (" writeMap: " + writeMap);
            return ret;
        }
    } // inner class ConnectionFrame

    /**
     * Returns true if a delay is required in the feedback path from
     * DONE to GO of the specified module.
     */
    private boolean checkFeedback (Module module)
    {
        LoopFlopAnalysis lfa = new LoopFlopAnalysis(module, this.latCache, true);
        
        return !lfa.isRemovable();
    }
    
} // GlobalConnector
