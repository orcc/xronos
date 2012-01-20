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

package net.sf.openforge.verilog.testbench;





import java.io.*;
import java.util.*;
import java.text.*;

import net.sf.openforge.app.*;
import net.sf.openforge.forge.api.internal.Core;
import net.sf.openforge.forge.api.pin.*;
import net.sf.openforge.forge.api.sim.pin.*;
import net.sf.openforge.lim.BidirectionalPin;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InputPin;
import net.sf.openforge.lim.OutputPin;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Task;
import net.sf.openforge.util.naming.*;
import net.sf.openforge.verilog.model.*;
import net.sf.openforge.verilog.pattern.*;
import net.sf.openforge.verilog.translate.PrettyPrinter;

/**
 * CycleSimTestBench creates a testbench framework in which the user can
 * populate exactly what values are applied to each input each clock
 * cycle (including reset) and what values are to be expected from
 * each output on each clock cycle.
 *
 * The basic structures created are:
 * <ul>
 * <li>1 memory for each pin
 * <li>code to apply the value from the memory to each input pin every
 * clock cycle.
 * <li>code to verify the value on each output pin against the value
 * in the memory every clock cycle.
 * </ul>
 *
 *
 * <p>Created: Tue Oct 29 14:10:48 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CycleSimTestBench.java 282 2006-08-14 21:25:33Z imiller $
 */
public class CycleSimTestBench 
{
    private static final String _RCS_ = "$Rev: 282 $";

    // This static is used to uniquely name the test bench modules
    // produced within the same run of forge so they can all be run together
    private static int uniqueID = 1;

    private int cycleCount;
    private ClockPin cycleCountClockPin;
    private VerilogDocument simulationDocument;

    public CycleSimTestBench (Design design, File source)
    {
        String name = source.getName();
        name = name.substring(0, name.length()-2);
        File resFile = new File(source.getParent(), name+".results");
        File clkFile = new File(source.getParent(), name +
                                ".results.clocks");        
        
        ResultFile resultFile = new ResultFile(resFile);
        ResultFile clkCntFile = new ResultFile(clkFile);

        Map pinToClockPinMap = getPinToClockPinMap(design);

        // get the cycle count lenght and clock pin this sim should run for
        CycleCount cc = findCycleCount(design,pinToClockPinMap);
        cycleCount = cc.getCycleCount();
//         cycleCountClockPin = cc.getClockPin();

        // this set is all the clocks the user declared in their test
        // bench (pin sim), they might not actually be used by the
        // design, and if that is the case, then we need to create pin
        // logic for them.
        Set clocksUsed = cc.getClocksUsed();      
        
        if (cycleCount < 0)
        {
            design.getEngine().getGenericJob().warn("No pin found with cycle simulation clock limit.  No testbench written");
            simulationDocument = new DefaultSimDocument("No Cycle Count Limit Specified On Any Pin", Collections.singletonList(resFile));
            return;
        }

        int indexSize = (int)java.lang.Math.ceil(java.lang.Math.log(this.cycleCount + 1)/java.lang.Math.log(2));
        Net index = new Register("testbench_index", indexSize);

        simulationDocument = new VerilogDocument();
        addHeader(simulationDocument);
        
        // add the timescale directive
        simulationDocument.append(new
            Directive.TimeScale(1,Keyword.NS,100,Keyword.PS));
        simulationDocument.append(new Comment(Comment.BLANK));
        
        
        addInclude(simulationDocument, source);

        // add a second timescale directive after the includes to
        // prevent any of them from changing our timescale
        simulationDocument.append(new
            Directive.TimeScale(1,Keyword.NS,100,Keyword.PS));
        simulationDocument.append(new Comment(Comment.BLANK));

        // get a module
        Module testModule = new Module("fixture_" + uniqueID++);

        // Generate a PinLogic instance for each pin.
        Map pinToLogic = buildPinLogic(design, resultFile, clocksUsed);

        // Instantiate the design.  Hook up each input to a register
        // and each output to a wire. Pass the clock logic explicitly
        instantiate(design, testModule, pinToLogic);

        // CHEAT!!!  Install a commented out VCD generation command.
        testModule.state(new InlineComment("initial begin  $dumpfile(\"waves.vcd\");  $dumpvars;end", Comment.SHORT));

        // Remove the clocks so we don't create logic for it.  Need to
        // copy the map to prevent concurrent modification errors.
        Map cpPinToLogic = new HashMap(pinToLogic);
        Map clkPinToLogic = new HashMap();

        // while looping, create clock specific indexes and store in
        // map
        Map clkPinLogicToIndex = new HashMap();
        
        for(Iterator it = cpPinToLogic.keySet().iterator();
            it.hasNext(); )
        {
            Object o = it.next();
            
            PinLogic pl = (PinLogic)pinToLogic.get(o);

            if(pl instanceof ClkPinLogic)
            {
                pinToLogic.remove(o);
                clkPinToLogic.put(o,pl);
                
                Net index_clock = new Register("testbench_index_" + pl.getName(), 32);
                clkPinLogicToIndex.put(pl,index_clock);
            }
        }
        
        for(Iterator it = clkPinToLogic.keySet().iterator(); it.hasNext();)
        {
            Pin clock = (Pin)it.next();
            ClkPinLogic cpl = (ClkPinLogic)clkPinToLogic.get(clock);
            
            // Create an always @(posedge clock) block for all the 'stuff'
            SequentialBlock block = new SequentialBlock();

            // only add the cycle count finish stuff to the correct
            // clock block
            if(clock.getApiPin().equals(cycleCountClockPin))
            {
                block.add(new Assign.NonBlocking(index,
                                                 new net.sf.openforge.verilog.model.Math.Add(index,
                                                                                            new Constant(1, index.getWidth()))));
                
                // Create the ending condition for the 'simulation'
                block.add(getIndexTerminalStatement(resultFile, index,
                                                    this.cycleCount, clkCntFile));
            }

            // add clock specific index increment
            Net tmpindex = (Net)clkPinLogicToIndex.get(cpl);
            block.add(new Assign.NonBlocking(tmpindex,
                                             new net.sf.openforge.verilog.model.Math.Add(tmpindex,
                                                                                        new Constant(1, tmpindex.getWidth()))));
            
            
            
            // Instantiate the logic for asserting inputs/io's and testing
            // outputs/io's
            for (Iterator iter = pinToLogic.keySet().iterator(); iter.hasNext();)
            {
                Pin p = (Pin)iter.next();
                PinLogic logic = (PinLogic)pinToLogic.get(p);

                _testbench.d.ln("Cons: pin: "+p.getApiPin()+" is "+logic);
                // only add if in this clock

                if(pinToClockPinMap.get(p) == null)
                {
                    _testbench.d.ln("\t==null");
                    // catches reset and other stuff that is going
                    // away
                }
                else if(pinToClockPinMap.get(p).equals(clock.getApiPin()))
                {
                    _testbench.d.ln("\t==ok");
                    block.add(logic.stateSequential(index,(Net)clkPinLogicToIndex.get(cpl)));

                    // for continuous we need an index that correlates
                    // to the current clock, not the master index used
                    // to know when simulation is active which the
                    // sequential states use to know when to verify
                    // the outputs or not if the simulation
                    // verification is finished and the simulation is
                    // coasting to completion ($finish).
                    testModule.state(logic.stateContinuous((Net)clkPinLogicToIndex.get(cpl)));
                }
                else
                {
                    _testbench.d.ln("plogmap: "+pinToClockPinMap.get(p)+" and apipin: "+clock.getApiPin());
                }
            }
            
            testModule.state(new Always(new ProceduralTimingBlock(
                new EventControl(new EventExpression.PosEdge(cpl.getNet())), block)));
        }
        
        /**
         * <pre>
         *
         * assign glbl.GSR = GSR;
         * assign glbl.GTS = 1'b0;
         * assign glbl.PRLD = 1'b0;
         *
         * initial begin
         *   GSR <= 1;
         *   #1 GSR <= 0;
         * end
         * </pre>
         */
        Net gsr = new Register("LGSR",1);
        Module m = new Module("glbl");
        Net w = new Wire("GSR",1);
        QualifiedNet qn = new QualifiedNet(m,w);
        testModule.state(new Assign.Continuous(qn,gsr));

        m = new Module("glbl");
        w = new Wire("GTS",1);
        qn = new QualifiedNet(m,w);
        testModule.state(new Assign.Continuous(qn,new Constant(0,1)));

        m = new Module("glbl");
        w = new Wire("PRLD",1);
        qn = new QualifiedNet(m,w);
        testModule.state(new Assign.Continuous(qn,new Constant(0,1)));
        
        InitialBlock ib = new InitialBlock();

        ib.add(new Assign.NonBlocking(gsr, new Constant(1,1)));
        
        ib.add(new DelayStatement(new Assign.NonBlocking(gsr, new
            Constant(0,1)),1));
        
        testModule.state(ib);
        
        ib = new InitialBlock();

        for(Iterator it = clkPinToLogic.keySet().iterator(); it.hasNext();)
        {
            Pin clock = (Pin)it.next();
            ClkPinLogic cpl = (ClkPinLogic)clkPinToLogic.get(clock);

            ib.add(new Assign.NonBlocking(cpl.getNet(), new HexNumber(new
                HexConstant(0, cpl.getNet().getWidth()))));

            // zero out clock specific indexes
            Net tmpindex = (Net)clkPinLogicToIndex.get(cpl);
            ib.add(new Assign.NonBlocking(tmpindex, new HexNumber(new HexConstant(0, tmpindex.getWidth()))));

        }
            
        ib.add(new Assign.NonBlocking(index, new HexNumber(new HexConstant(0, index.getWidth()))));


        ib.add(resultFile.init());
        ib.add(clkCntFile.init());
        
        for (Iterator iter = pinToLogic.values().iterator(); iter.hasNext();)
        {
            PinLogic logic = (PinLogic)iter.next();
            logic.initMemory(ib);
        }
        testModule.state(ib);

        this.simulationDocument.append(testModule);
    }

    private void addHeader (VerilogDocument doc)
    {
        Comment header = new Comment("OpenForge Test", Comment.SHORT);
        doc.append(header);
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        header = new Comment("Pin Automatic Test Bench.  Generated: " + df.format(new Date()), Comment.SHORT);
        doc.append(header);

        header = new Comment("");
        doc.append(header);
    }

    private void addInclude (VerilogDocument doc, File source)
    {
        if (source != null)
        {
            doc.append(new IncludeStatement(source));
        }
    }
    
    public void write (FileOutputStream fos)
    {
        PrettyPrinter pp = new PrettyPrinter(fos);
        pp.print(this.simulationDocument);
    }

    private Map buildPinLogic (Design design, ResultFile resultFile,
                               Set clocksUsed)
    {
        Map pinToLogic = new HashMap();
        Set unInitedClocks = new HashSet(clocksUsed);        

        for (Iterator iter = design.getClockPins().iterator();
             iter.hasNext();)
        {
            InputPin pin = (InputPin)iter.next();

            unInitedClocks.remove(pin.getApiPin());
            
            pinToLogic.put(pin, new ClkPinLogic(pin));
        }
        
        for (Iterator iter = design.getInputPins().iterator(); iter.hasNext();)
        {
            InputPin pin = (InputPin)iter.next();

            if(pinToLogic.get(pin) != null)
            {
                // skip it, it must be a clock and is already in the map
            }
            else
            {
                if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
                {
                    pinToLogic.put(pin, new InPinLogic(pin));
                }
            }
        }
        
        for (Iterator iter = design.getOutputPins().iterator(); iter.hasNext();)
        {
            OutputPin pin = (OutputPin)iter.next();

            if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
            {
                pinToLogic.put(pin, new OutPinLogic(pin, resultFile));
            }
        }
        
        for (Iterator iter = design.getBidirectionalPins().iterator(); iter.hasNext();)
        {
            BidirectionalPin pin = (BidirectionalPin)iter.next();

            if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
            {
                pinToLogic.put(pin, new InOutPinLogic(pin, resultFile));
            }
        }

        // finish all uninited clocks
        for (Iterator iter = unInitedClocks.iterator();
             iter.hasNext(); )
        {
            ClockPin cp = (ClockPin)iter.next();

            InputPin ip = new InputPin(1,false);
            ip.setApiPin(cp);
            ip.setIDLogical(cp.getName());
            
            pinToLogic.put(ip, new ClkPinLogic(ip));
        }
        
            
        return pinToLogic;
    }

    private CycleCount findCycleCount (Design design, Map pinToClockPinMap)
    {
        double minTime = Double.MAX_VALUE;
        boolean bounded = false;
        int whichCount = 0;
        ClockPin whichClk = null;
        HashSet uniqueClocks = new HashSet();
        
        for (Iterator iter = design.getPins().iterator(); iter.hasNext();)
        {
            Pin pin = (Pin)iter.next();

            Buffer apiPin = pin.getApiPin();

            // do we have a user pin, not part of a core or a published part of a core
            if ((apiPin != null) && ((!Core.hasThisPin(apiPin)) || (Core.hasPublished(apiPin))))
            {
                int driveLength=PinSimData.getDriveData(apiPin).getCycleCount();
                int testLength=PinSimData.getTestData(apiPin).getCycleCount();

                int length=-1;

                if(driveLength<=0) // if we have no drive, use test
                {
                    length=testLength;
                }
                else if(testLength<=0) // if no test, then drive
                {
                    length=driveLength;
                }
                else
                {
                    length=java.lang.Math.min(driveLength,testLength); // take smallest of two sides
                }

                if (length > 0)
                {
                    // use clock to calculate the actual time required
                    
                    ClockPin cp = (ClockPin)pinToClockPinMap.get(pin);
                    double period;

                    if(cp == null)
                    {
                        // something happened, default to global clock
                        cp = ClockDomain.GLOBAL.getClockPin();
                    }
                    
                    uniqueClocks.add(cp);
                    
                    if(cp.getFrequency() == ClockPin.UNDEFINED_HZ)
                    {
                        // assume 10 Mhz by default
                        period = 1.0 / 10000000.0;
                    }
                    else
                    {
                        period = 1.0 / ((double)cp.getFrequency());
                    }
                    
                    double time = period * (double)length;

                    if(time < minTime)
                    {
                        // we found a smaller one
                        minTime = time;
                        whichCount = length;
                        whichClk = cp;                        
                    }
                    
                    bounded = true;
                }
            }
        }
        
        return( new CycleCount((bounded ? whichCount : -1),whichClk,uniqueClocks));
    }
    
    private Statement getIndexTerminalStatement (ResultFile file,
                                                 Net index,
                                                 int vectorCount,
                                                 ResultFile clockCntFile)
    {
        SequentialBlock block = new SequentialBlock();
        Statement pass = new StringStatement("PASSED\\n");
        block.add(file.write(pass));

        CommaDelimitedStatement cds = new CommaDelimitedStatement();
        cds.append(new StringStatement("%d\\n"));
        cds.append(new
            net.sf.openforge.verilog.model.Math.Add(index,new
                Constant(1,index.getWidth())));
        block.add(clockCntFile.write(cds));
        
        block.add(new FStatement.Finish());
        ConditionalStatement cs = new ConditionalStatement(
            new Compare.GTEQ(index, new HexNumber(new HexConstant(vectorCount, index.getWidth()))), block);
        return cs;
    }

    private Map instantiate (Design design, Module module, Map pinToLogic)
    {
        Map pinToExpr = new HashMap();
        ModuleInstance instance = new ModuleInstance(getVerilogName(design), "test");

        List inPins = new ArrayList(design.getInputPins());

        Set configuredClockPins = new HashSet();
        
        // special handling for clock pins prior to the other input pins
        for (Iterator iter = inPins.iterator(); iter.hasNext();)
        {
            InputPin pin = (InputPin)iter.next();

            PinLogic logic = (PinLogic)pinToLogic.get(pin);

            if((logic != null) && (logic instanceof ClkPinLogic))
            {
                ClkPinLogic cpl = (ClkPinLogic)logic;

                configuredClockPins.add(pin);
                
                // handle as a clock pin
                module.state(new Always(cpl.stateSequential(null,null)));

                if(design.consumesClock())
                {
                    instance.add(new PortConnection(new Input(getVerilogName(pin.getBus()), 1), cpl.getNet()));                
                }    
            }
        }        

        // finish all pins in logic to clock map that were not
        // connected to the design
        for (Iterator iter = pinToLogic.keySet().iterator();
             iter.hasNext();)
        {
            Pin pin = (Pin)iter.next();

            PinLogic logic  = (PinLogic)pinToLogic.get(pin);

            if(logic instanceof ClkPinLogic)
            {
                if(!configuredClockPins.contains(pin))
                {
                    module.state(new Always(logic.stateSequential(null,null)));
                    configuredClockPins.add(pin);
                }
            }
        }
        
        
        for (Iterator iter = inPins.iterator(); iter.hasNext();)
        {
            InputPin pin = (InputPin)iter.next();
            _testbench.d.ln("Instantiate: Input Pin: "+pin+" Api: "+pin.getApiPin());
            if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
            {
                PinLogic logic = (PinLogic)pinToLogic.get(pin);

                _testbench.d.ln("Instantiate: Logic Input Pin: "+logic);
                if(logic instanceof ClkPinLogic)
                {
                    // we already did the clock, skip
                }
                else
                {
                    // normal old input pin
                    String name = getVerilogName(pin.getBus());
                    instance.add(new PortConnection(new Input(name,
                                                        pin.getWidth()), logic.getNet()));
                }
            }
        }
        
        for (Iterator iter = design.getOutputPins().iterator(); iter.hasNext();)
        {
            OutputPin pin = (OutputPin)iter.next();
            _testbench.d.ln("Instantiate: Output Pin: "+pin+" Api: "+pin.getApiPin());

            if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
            {
                PinLogic logic = (PinLogic)pinToLogic.get(pin);
                _testbench.d.ln("Instantiate: Logic Output Pin: "+logic);
                String name = getVerilogName(pin);
                instance.add(new PortConnection(new Output(name, pin.getWidth()), logic.getNet()));
            }
        }
        
        for (Iterator iter = design.getBidirectionalPins().iterator(); iter.hasNext();)
        {
            BidirectionalPin pin = (BidirectionalPin)iter.next();

            _testbench.d.ln("Instantiate: Bidir Pin: "+pin+" Api: "+pin.getApiPin());
            if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
            {
                PinLogic logic = (PinLogic)pinToLogic.get(pin);
                _testbench.d.ln("Instantiate: Logic Bidir Pin: "+logic);
                String name = getVerilogName(pin);
                instance.add(new PortConnection(new Inout(name, pin.getWidth()), logic.getNet()));
            }
        }

        module.state(instance);
        
        return pinToExpr;
    }
    
    private static String getVerilogName(Object obj)
    {
        return ID.toVerilogIdentifier(ID.showLogical(obj));
    }

    private Map getPinToClockPinMap(Design design)
    {
        // the goal of this method is to build a map of lim.Pin
        // objects to api.pin.ClockPin objects so the logic in this
        // class can know what clock domain to put the test logic in

        Map pinToClockPinMap = new HashMap();

        // step 1, go through each task in the design, fetch all the
        // ports and busses and map them back to their lim.Pin at the
        // top level, then add an association from lim.Pin to the
        // clock for the task.

        for(Iterator iter=design.getTasks().iterator(); iter.hasNext();)
        {
            Task task = (Task)iter.next();

            if(!task.isAutomatic())
            {
                Call call = task.getCall();
        
                Port goPort = null;

                if(call.consumesGo())
                {
                    goPort = call.getGoPort();
                }

                ArrayList argumentPorts = new ArrayList();
        
                for(Iterator it=call.getDataPorts().iterator(); it.hasNext();)
                {
                    Port p = (Port)it.next();
                    
                    if(p.getTag() == Component.NORMAL)
                    {
                        // this is an argument
                        argumentPorts.add(p);
                    }
                }
        

                Bus doneBus = null;
                Bus resultBus = null;
                
                for(Iterator it=call.getExits().iterator(); it.hasNext();)
                {
                    Exit exit = (Exit)it.next();
                    
                    if(exit.getTag().getType() == Exit.DONE)
                    {
                        // this is the result and done bus exit
                        
                        List l = exit.getDataBuses();
                        if(l.size() > 0)
                        {
                            resultBus = (Bus)l.get(0);
                        }
                        
                        if(call.producesDone())
                        {
                            doneBus = exit.getDoneBus();
                        }
                    }
                }
        
                // Now determine the pins representing the ports and buses we
                // identified

                Procedure procedure = call.getProcedure();
                //ClockPin cp = procedure.getClockPin();
                ClockPin cp = null;
                
                if(goPort != null)
                {
                    Pin goPin = design.getPin(goPort);
                    pinToClockPinMap.put(goPin,cp);
                }

                if(doneBus != null)
                {
                    Pin donePin = design.getPin(doneBus);
                    pinToClockPinMap.put(donePin,cp);                    
                }
                
                if(resultBus != null)
                {
                    Pin resultPin = design.getPin(resultBus);
                    pinToClockPinMap.put(resultPin,cp);
                }
                
                for(Iterator it=argumentPorts.iterator(); it.hasNext();)
                {
                    Object obj = it.next();
                    
                    // make sure there is a pin for the given argument
                    if(design.getPin(obj) != null)
                    {
                        pinToClockPinMap.put(design.getPin(obj),cp);
                    }
                }
            }
        }
        
        
        // Now go through all the design pins and if there is
        // nothing registered in the pinToClockPinMap, then ask
        // the api pin for its clock domain
        Collection clkpins = design.getClockPins();
        
        for (Iterator iter = design.getInputPins().iterator(); iter.hasNext();)
        {
            InputPin pin = (InputPin)iter.next();

            //_testbench.d.ln("Map: Input: "+pin.getApiPin()+" Has: "+pin.getClockPin()+" domain: "+pin.getApiPin().getDomain());
            if(clkpins.contains(pin))
            {
                // skip it, it is a clock
            }
            else
            {
                if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
                {
                    if(pinToClockPinMap.get(pin) == null)
                    {
                        // here we have to flush in the clock pin from the domain of the api pin
                        if(Core.hasPublished(pin.getApiPin()))
                        {
                            if(pin.getApiPin().getDomain()==null)
                            {
                                pin.getApiPin().setDomain(ClockDomain.GLOBAL);
                                EngineThread.getGenericJob().warn("IP Core Pin: "+pin.getApiPin()+" using GOBAL clock as default");
                            }
//                             pinToClockPinMap.put(pin,pin.getApiPin().getDomain().getClockPin());
                        }
//                         else if(pin.getClockPin() != null)
//                         {
//                             pinToClockPinMap.put(pin,pin.getClockPin().getApiPin());
//                         }
                    }
                }
            }
        }
        
        for (Iterator iter = design.getOutputPins().iterator(); iter.hasNext();)
        {
            OutputPin pin = (OutputPin)iter.next();
//             _testbench.d.ln("Map: Output: "+pin.getApiPin()+" Has: "+pin.getClockPin()+" domain: "+pin.getApiPin().getDomain());
            
            if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
            {
                if(pinToClockPinMap.get(pin) == null)
                {
                    // here we have to flush in the clock pin from the domain of the api pin
                    if(Core.hasPublished(pin.getApiPin()))
                    {
                        if(pin.getApiPin().getDomain()==null)
                        {
                            pin.getApiPin().setDomain(ClockDomain.GLOBAL);
                            EngineThread.getGenericJob().warn("IP Core Pin: "+pin.getApiPin()+" using GOBAL clock as default");
                        }
//                         pinToClockPinMap.put(pin,pin.getApiPin().getDomain().getClockPin());
                    }
//                     else if(pin.getClockPin() != null)
//                     {
//                         pinToClockPinMap.put(pin,pin.getClockPin().getApiPin());
//                     }
                }
            }
        }
        
        for (Iterator iter = design.getBidirectionalPins().iterator(); iter.hasNext();)
        {
            BidirectionalPin pin = (BidirectionalPin)iter.next();
//             _testbench.d.ln("Map: Bidir: "+pin.getApiPin()+" Has: "+pin.getClockPin()+" domain: "+pin.getApiPin().getDomain());
            
            if((!Core.hasThisPin(pin.getApiPin())) || (Core.hasPublished(pin.getApiPin())))
            {
                if(pinToClockPinMap.get(pin) == null)
                {
                    // here we have to flush in the clock pin from the domain of the api pin
                    if(Core.hasPublished(pin.getApiPin()))
                    {
                        if(pin.getApiPin().getDomain()==null)
                        {
                            pin.getApiPin().setDomain(ClockDomain.GLOBAL);
                            EngineThread.getGenericJob().warn("IP Core Pin: "+pin.getApiPin()+" using GOBAL clock as default");
                        }
//                         pinToClockPinMap.put(pin,pin.getApiPin().getDomain().getClockPin());
                    }
//                     else if(pin.getClockPin() != null)
//                     {
//                         pinToClockPinMap.put(pin,pin.getClockPin().getApiPin());
//                     }
                }
            }
        }   
        
        return(pinToClockPinMap);
    }
    
    
    public abstract class PinLogic
    {
        private InitializedMemory mem;
        private String name;
        int width;
        
        public PinLogic (String prefix, Pin pin, List data)
        {
            this.name = ID.toVerilogIdentifier(ID.showLogical(pin));

            width = pin.getWidth();
            this.mem = initMem(prefix + "_" + this.name + "_values", pin, data, width);
        }

        protected InitializedMemory initMem (String name, Pin pin, List data, int width)
        {
            InitializedMemory im = new InitializedMemory(name, width);
            //for (int i=0; i < CycleSimTestBench.this.cycleCount; i++)
            for (int i=0; i < (data.size() + 2); i++)
            {
                SignalValue sv= (i < data.size()) ? ((SignalValue)data.get(i)):SignalValue.X;
                im.addInitValue(getExpr(sv, width, pin));
            }

            return im;
        }

        private Expression getExpr(SignalValue s, int width, Pin pin)
        {
            Expression expr = null;
            if (s.isX())
            {
                expr = new HexNumber(new BinaryConstant("x", width));
            }
            else if (s.isZ())
            {
                expr = new HexNumber(new BinaryConstant("z", width));
            }
            else
            {
                expr = new HexNumber(new HexConstant(s.getValue(), width));
            }
            return expr;
        }
        
        protected InitializedMemory getMemory ()
        {
            return this.mem;
        }

        public void initMemory (InitialBlock ib)
        {
            ib.add(getMemory());
        }

        public String getName ()
        {
            return this.name;
        }

        public abstract Net getNet ();

        public abstract Statement stateSequential (Net index, Net clkIndex);
        public abstract Statement stateContinuous (Net index);
    }

    public class ClkPinLogic extends PinLogic
    {
        Register reg;
        int periodNs;
        double period = 0.0;
        
        public ClkPinLogic (InputPin pin)
        {
            super("arg", pin, pin.getApiPin() == null ? Collections.EMPTY_LIST:PinSimData.getDriveData(pin.getApiPin()).asList());
            reg = new Register(getVerilogName(pin.getBus()), pin.getWidth());

            ClockPin cp = (ClockPin)pin.getApiPin();
            
            if(cp.getFrequency() == ClockPin.UNDEFINED_HZ)
            {
                // assume 10 Mhz by default
                period = 1.0 / 10000000.0;
            }
            else
            {
                period = 1.0 / ((double)cp.getFrequency());
            }

            // convert to nanoseconds, * 1x10^9
            period *= 1000000000.0;

            periodNs = (int)period;
        }

        public Net getNet ()
        {
            return this.reg;
        }

        public Statement stateSequential (Net index, Net clkIndex)
        {
            Assign assign = new Assign.NonBlocking(getNet(), new Unary.Negate(getNet()));
            // delay for 1/2 the period to create the correct clock
            return new DelayStatement(assign, (periodNs / 2));
        }

        public Statement stateContinuous (Net index)
        {
            return new InlineComment("");
        }

        public InitializedMemory getMemory ()
        {
            assert false : "Should not use memory for clock";
            return super.getMemory();
        }
    }

    public class InPinLogic extends PinLogic
    {
        Wire reg;
        
        public InPinLogic (InputPin pin)
        {
            super("arg", pin, pin.getApiPin() == null ? Collections.EMPTY_LIST:PinSimData.getDriveData(pin.getApiPin()).asList());
            reg = new Wire(getVerilogName(pin.getBus()), pin.getWidth());
        }

        public Net getNet ()
        {
            return this.reg;
        }

        public Statement stateSequential (Net index, Net clkIndex)
        {
            return new InlineComment("");
        }
        
        public Statement stateContinuous (Net index)
        {
            return new Assign.Continuous(reg, new MemoryElement(getMemory(), index));
        }
    }


    public class OutPinLogic extends PinLogic
    {
        Wire wire;
        ResultFile file;
        Wire reswire;

        InitializedMemory driveMem;
        
        public OutPinLogic (OutputPin pin, ResultFile file)
        {
            super("res", pin, pin.getApiPin() == null ? Collections.EMPTY_LIST:PinSimData.getTestData(pin.getApiPin()).asList());

            wire = new Wire(getVerilogName(pin), pin.getWidth());            
            reswire = new Wire(getVerilogName(pin) + "_expected",pin.getWidth());
            this.file = file;

            List driveValues =
            PinSimData.getDriveData(pin.getApiPin()).asList();
        
            if(!driveValues.isEmpty())
            {
                driveMem = initMem("drive_" + getName() + "_values", pin, driveValues, pin.getWidth());
            }
            else
            {
                driveMem = null;
            }            
        }
        
        public Net getNet ()
        {
            return this.wire;
        }

        public void initMemory (InitialBlock ib)
        {
            super.initMemory(ib);

            if(driveMem != null)
                ib.add(driveMem);
        }

        public Statement stateContinuous (Net index)
        {
            StatementBlock sb = new StatementBlock();

            // drive any drive data if it exists
            if(driveMem != null)
                sb.add(new Assign.Continuous(getNet(),new
                    MemoryElement(driveMem, index)));
            
            sb.add(new Assign.Continuous(reswire, new
                MemoryElement(getMemory(), index)));

            return sb;
        }
        
        public Statement stateSequential (Net index, Net clkIndex)
        {
            SequentialBlock trueBlock = new SequentialBlock();
            CommaDelimitedStatement cds = new CommaDelimitedStatement();
            cds.append(new StringStatement("FAIL: Result does not match expected at clock cycle %d for pin " + getName() + " expected %x received %x \\n"));
            cds.append(clkIndex);
            cds.append(reswire);
            cds.append(getNet());
            trueBlock.add(this.file.write(cds));
            trueBlock.add(new DelayStatement(new FStatement.Finish(), 500));
            ConditionalStatement cs = new ConditionalStatement(
                new Compare.CASE_NEQ(reswire, getNet()), trueBlock);
            ConditionalStatement cs2 = new ConditionalStatement(
                new Compare.CASE_NEQ(reswire,new HexNumber(new BinaryConstant("x", width))),
                new SequentialBlock(cs));            
            ConditionalStatement cs3 = new ConditionalStatement(
                new Compare.LT(index,new Decimal(new Constant(CycleSimTestBench.this.cycleCount,index.getWidth()))),new SequentialBlock(cs2));

            return cs3;
        }
    }

    public class InOutPinLogic extends PinLogic
    {
        Wire net;
        ResultFile file;
        Wire reswire;

        InitializedMemory driveMem;
        
        public InOutPinLogic (BidirectionalPin pin, ResultFile file)
        {
            super("arg", pin, pin.getApiPin() == null ? Collections.EMPTY_LIST:PinSimData.getTestData(pin.getApiPin()).asList());
            this.net = new Wire(getVerilogName(pin), pin.getWidth());
            this.file = file;

            reswire = new Wire(getVerilogName(pin) + "_expected",pin.getWidth());

            List driveValues =
            PinSimData.getDriveData(pin.getApiPin()).asList();
        
            if(!driveValues.isEmpty())
            {
                driveMem = initMem("drive_" + getName() + "_values", pin, driveValues, pin.getWidth());
            }
            else
            {
                driveMem = null;
            }
        }

        public void initMemory (InitialBlock ib)
        {
            super.initMemory(ib);

            if(driveMem != null)
                ib.add(driveMem);
        }

        public Net getNet ()
        {
            return this.net;
        }
        
        public Statement stateSequential (Net index, Net clkIndex)
        {
            SequentialBlock trueBlock = new SequentialBlock();
            CommaDelimitedStatement cds = new CommaDelimitedStatement();
            cds.append(new StringStatement("FAIL: Result does not match expected at clock cycle %d for I/O pin " + getName() + " expected %x received %x \\n"));
            cds.append(clkIndex);
            cds.append(reswire);
            cds.append(getNet());
            trueBlock.add(this.file.write(cds));
            trueBlock.add(new DelayStatement(new FStatement.Finish(), 500));
            ConditionalStatement cs = new ConditionalStatement(
                new Compare.CASE_NEQ(reswire, getNet()), trueBlock);
            ConditionalStatement cs2 = new ConditionalStatement(
                new Compare.CASE_NEQ(reswire,new HexNumber(new BinaryConstant("x", width))),
                new SequentialBlock(cs));            
            ConditionalStatement cs3 = new ConditionalStatement(
                new Compare.LT(index,new Decimal(new Constant(CycleSimTestBench.this.cycleCount,index.getWidth()))),new SequentialBlock(cs2));

            return cs3;
        }
        
        public Statement stateContinuous (Net index)
        {
            StatementBlock sb = new StatementBlock();

            // drive any drive data if it exists
            if(driveMem != null)
                sb.add(new Assign.Continuous(getNet(),new
                    MemoryElement(driveMem, index)));
            
            sb.add(new Assign.Continuous(reswire, new
                MemoryElement(getMemory(), index)));

            return sb;
        }        
    }

    class CycleCount
    {
        private int cycleCount;
        private ClockPin clk;
        private Set clocksUsed;
        
        CycleCount(int count, ClockPin clk, Set clocks)
        {
            this.cycleCount = count;
            this.clk = clk;
            this.clocksUsed = clocks;
        }


        public int getCycleCount()
        {
            return(this.cycleCount);
        }

        public ClockPin getClockPin()
        {
            return(this.clk);
        }

        public Set getClocksUsed()
        {
            return clocksUsed;
        }
        
    }
    
}// CycleSimTestBench



