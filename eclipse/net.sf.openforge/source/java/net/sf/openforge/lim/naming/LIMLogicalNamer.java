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
 package net.sf.openforge.lim.naming;


import java.util.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionBoolean;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDDb;
import net.sf.openforge.util.naming.IDSourceInfo;

/**
 * Sets unique logical names to every object in a LIM.
 *
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: LIMLogicalNamer.java 286 2006-08-15 17:03:16Z imiller $
 */
public class LIMLogicalNamer extends FilteredVisitor
    implements Visitor
{
    /** RCS ID */
    private static final String _RCS_ = "$Rev: 286 $";

    /** Used to turn on debug output. */
    private static final boolean DEBUG = false;

    private boolean fifoIO = false;

    /** The top module name.  Used to uniquify global modules and
     * such. */
    private String topModuleName = "";
    

    /**
     *  Create a LIMNamer and make it name the given design.
     *
     * @param design
     */
    public static void setNames (Design design, boolean fifoIO)
    {
        LIMLogicalNamer namer = new LIMLogicalNamer(fifoIO);
        design.accept(namer);
    }

    public static void setNames (Design design)
    {
        setNames(design,false);
    }


    /**
     * Construct a new LIMNamer.
     */
    private LIMLogicalNamer ()
    {
        this(false);
    }

    private LIMLogicalNamer (boolean fifoIO)
    {
        this.fifoIO = fifoIO;
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Design)
     */
    public void visit(Design design)
    {
        // Make the TOP_MODULE_NAME setting the absolute name
        Option op = EngineThread.getGenericJob().getOption(OptionRegistry.TOP_MODULE_NAME); 
        topModuleName = op.getValue(design.getSearchLabel()).toString();

        super.visit(design);

        String designName = topModuleName;
        
        if (designName.length() == 0)
        {
            String baseName = "";
            if(EngineThread.getGenericJob().getTargetFiles().length > 0)
            {
                baseName = EngineThread.getGenericJob().getTargetFiles()[0].getName();
                if(baseName.lastIndexOf(".") >= 0)
                {
                    // prune out the file type suffix
                    baseName = baseName.substring(0,baseName.lastIndexOf("."));
                }
                baseName += "_";
            }

            String derivedModuleName = "";
            // Check if the entry function is specified, and if so
            // the name becomes the entry function name
            op = EngineThread.getGenericJob().getOption(OptionRegistry.NO_BLOCK_IO);
            if (!((OptionBoolean)op).getValueAsBoolean(CodeLabel.UNSCOPED)) //If Do BlockIO
            {
                derivedModuleName = (String)BlockIOInterface.getFunctionNames().iterator().next();
            }
            else
            {
                if (design.getTasks().iterator().hasNext())
                {
                    derivedModuleName = ((Task)design.getTasks().iterator().next()).getCall().getProcedure().showIDLogical();
                }
            }
            
            if(derivedModuleName.length() == 0)
            {
                // the top module name is not supplied, and no entry
                // function is specified, default to the non-static
                // function name.
                derivedModuleName = design.getIDSourceInfo().getFullyQualifiedName();
            }

            if(fifoIO)
            {
                derivedModuleName += "_core_impl";
            }

            designName = baseName + derivedModuleName;
        }
        
        design.setIDLogical(designName);

        if (DEBUG) debugln(ID.showDebug(design) + " given logical name \"" +
            ID.showLogical(design) + "\"");
        namePins(design.getInputPins(), "IN");
        namePins(design.getOutputPins(), "OUT");
        namePins(design.getBidirectionalPins(), "INOUT");

        design.accept(new DesignElementNamingVisitor());
    }

    /** Maps simple pin names to a list of pins with that name. */
    private HashMap pinNameMap = new HashMap();

    /**
     * Because this code uses the IDSourceInfo to generate a unique
     * name, all pins should have the fieldName in the IDSourceInfo
     * set.
     */
    private void namePins(Collection pins, String baseName)
    {
        HashSet newNames = new HashSet();
        for (Iterator it = pins.iterator(); it.hasNext();)
        {
            Pin pin = (Pin)it.next();
            IDSourceInfo info = pin.getIDSourceInfo();
            String simplePinName = baseName;
            if (pin.hasExplicitName())
            {
                simplePinName = ID.showLogical(pin);
            }
            else if (info.getFieldName() != null)
            {
                simplePinName = info.getFieldName();
            }
            List commonlyNamedPins = (List)pinNameMap.get(simplePinName);
            if (commonlyNamedPins == null)
            {
                commonlyNamedPins = new ArrayList();
                pinNameMap.put(simplePinName, commonlyNamedPins);
            }
            newNames.add(simplePinName);
            commonlyNamedPins.add(pin);
        }

        for (Iterator it = newNames.iterator(); it.hasNext();)
        {
            String simplePinName = (String)it.next();
            List commonlyNamedPins = (List)pinNameMap.get(simplePinName);
            if (commonlyNamedPins.size() > 1)
            {
                int pIndex = 0;
                for (Iterator commonPins = commonlyNamedPins.iterator(); commonPins.hasNext();)
                {
                    Pin pin = (Pin)commonPins.next();
                    if (simplePinName == baseName)
                    {
                        pin.setIDLogical(simplePinName + pIndex++);
                    }
                    else
                    {
                        pin.setIDLogical(pin.getIDSourceInfo().getFullyQualifiedName());
                    }
                }
            }
            else
            {
                Pin pin = (Pin)commonlyNamedPins.get(0);
                pin.setIDLogical(simplePinName);
            }
        }
    }

    private IDDb taskDb = new IDDb();

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Task)
     */
    public void visit(Task task)
    {
        Procedure procedure = task.getCall().getProcedure();
        IDSourceInfo info = procedure.getIDSourceInfo();
        String taskName = "task_" + info.getMethodName() + info.getSignature();
        long nextID = taskDb.getNextID(taskName);
        if (nextID>0) taskName += nextID;
        task.setIDLogical(taskName);

        if (DEBUG) debugln(ID.showDebug(task) + " given logical name \"" +
            ID.showLogical(task) + "\"");
        
        super.visit(task);
    }

    /**
     * Makes an appropriate logical name for a Procedure. This can be
     * controlled in a limited way with {@link
     * Procedure#shouldUseSignatureInName()} and
     * {@link Procedure#shouldUseSimpleNaming()}.
     *
     * @param procedure the Procedure to be named.
     * @return a logical name for the procedure
     */
    private String makeMethodName(Procedure procedure)
    {
    	Option op;
        IDSourceInfo info = procedure.getIDSourceInfo();

        String methodName = info.getMethodName();
        if (methodName == null)
        {
            methodName = procedure.showIDLogical();
            if (procedure.getSourceName() != null)
            {
                methodName = methodName.replaceAll("_" + procedure.getSourceName(), "");
            }
        }
        
        String signature = info.getSignature();
        if (signature == null) signature = "";

        op = procedure.getGenericJob().getOption(OptionRegistry.SIGNATURE_IN_NAMES);        
        if (((OptionBoolean)op).getValueAsBoolean(procedure.getSearchLabel()))
        {
            methodName = new String(methodName + signature);
        }

        op = procedure.getGenericJob().getOption(OptionRegistry.SIMPLE_MODULE_NAMES);
        String returnedMethodName = "";
        if (((OptionBoolean)op).getValueAsBoolean(procedure.getSearchLabel()))
        {
            returnedMethodName = methodName;
        }
        else
        {
            StringBuffer fullName = new StringBuffer();
            String packageName = info.getSourcePackageName();
            String className = info.getSourceClassName();
            if (packageName != null) fullName.append(packageName);
            if (className != null)
            {
                if (fullName.length() > 0) fullName.append(".");
                fullName.append(className);
            }
            fullName.append(methodName + signature);
            returnedMethodName = fullName.toString();
        }
        
        return uniquifyCoreName(returnedMethodName);
    }

    private IDDb callDb = new IDDb();

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Call)
     */
    public void visit(Call call)
    {
        // all call naming is based on the procedure, so name that first.
        super.visit(call);

        Procedure procedure = call.getProcedure();

        String callName = ID.showLogical(procedure) + "_instance";
        long nextID = callDb.getNextID(callName);
        if (nextID>0) callName += nextID;
        call.setIDLogical(callName);

        /** set unique id logical for all call DONE exit databus */
        int nameCounter = 0;
        Exit exit = call.getExit(Exit.DONE);
        Bus done = exit.getDoneBus();
        done.setIDLogical(ID.showLogical(call) +
                          "_" + procedure.getDoneName());

        for(Iterator exiter = exit.getDataBuses().iterator(); exiter.hasNext();)
        {
            Bus exitBus = (Bus)exiter.next();
            exitBus.setIDLogical(ID.showLogical(call) +
                                 "_" + procedure.getResultName() +
                                 ((nameCounter > 0) ? Integer.toString(nameCounter) : ""));
            nameCounter++;
        }
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Procedure)
     */
    public void visit(Procedure procedure)
    {

        procedure.setIDLogical(makeMethodName(procedure));

        if (DEBUG) debugln(ID.showDebug(procedure) + " given logical name \"" +
            ID.showLogical(procedure) + "\"");

        Block block = (Block)procedure.getBody();
        block.accept(this);

        String domainSpec = (String)EngineThread.getGenericJob().getOption(OptionRegistry.CLOCK_DOMAIN).getValue(procedure.getSearchLabel());        
        //Design.ClockDomain domain = design.getClockDomain(domainSpec);
        String[] clkrst = Design.ClockDomain.parse(domainSpec);
        // Naming the procedure block's Clock, reset, and go ports
        Port clockPort = block.getClockPort();
        //clockPort.getPeer().setIDLogical(procedure.getClockName());
        clockPort.getPeer().setIDLogical(clkrst[0]);
        Port resetPort = block.getResetPort();
        //resetPort.getPeer().setIDLogical(procedure.getResetName());
        if (clkrst.length > 1 && clkrst[1].length() > 0)
            resetPort.getPeer().setIDLogical(clkrst[1]);
        else
            resetPort.getPeer().setIDLogical("RESET");
        Port goPort = block.getGoPort();
        goPort.getIDSourceInfo().setFieldName(procedure.getGoName());
        goPort.getPeer().setIDLogical(procedure.getGoName());

        // Naming the parameter ports
        for(Iterator dataPortIter = block.getDataPorts().iterator(); dataPortIter.hasNext();)
        {
            Port dataPort = (Port)dataPortIter.next();
            if (DEBUG) debugln("\tport[" + ID.showDebug(dataPort) + "] " +
                "deriving logical name from " + dataPort.getIDSourceInfo());
            dataPort.setIDLogical(dataPort.getIDSourceInfo().getFieldName());

            /** set the InBuf data bus id logical */
            dataPort.getPeer().setIDLogical(ID.showLogical(dataPort));
        }

        /*
         * Naming the output Buses of a procedure block. The Procedure translator requires
         * them.
         */
        int i = 0;
        int j = 0;
        for (Iterator ex_iter = block.getExits().iterator(); ex_iter.hasNext();)
        {
            Exit exit = (Exit)ex_iter.next();
            exit.getDoneBus().setIDLogical(procedure.getDoneName());
            for (Iterator db_iter = exit.getDataBuses().iterator(); db_iter.hasNext();)
            {
                ((Bus)db_iter.next()).setIDLogical(procedure.getResultName());
            }
        }
        
        super.visit(procedure);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Kicker)
     */
    public void visit(Kicker kicker)
    {
        kicker.setIDLogical("kicker");

        if (DEBUG) debugln(ID.showDebug(kicker) + " given logical name \"" +
            ID.showLogical(kicker) + "\"");
        
        super.visit(kicker);
    }

    public void visit (MemoryBank memBank)
    {
        memBank.setIDLogical("memBank");
        
        if (DEBUG) debugln(ID.showDebug(memBank) + " given logical name \"" +
            ID.showLogical(memBank) + "\"");
        
        super.visit(memBank);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Block)
     */
    public void visit(Block block)
    {
        block.setIDLogical("block");

        if (DEBUG) debugln(ID.showDebug(block) + " given logical name \"" +
            ID.showLogical(block) + "\"");

        /*
         * Name the inBuf's buses of a 'not-procedure' block.
         */
        if (!block.isProcedureBody())
        {
            InBuf ib = block.getInBuf();
            ib.getClockBus().setIDLogical(ID.showLogical(block)+"_CLK");
            ib.getResetBus().setIDLogical(ID.showLogical(block)+"_RESET");
            ib.getGoBus().setIDLogical(ID.showLogical(block)+"_GO");
        }
        
        super.traverse(block);

        /*
         * Naming the output Buses of a 'not-procedure' block.
         */
        if (!block.isProcedureBody())
        {
            int i = 0;
            int j = 0;
            for (Iterator ex_iter = block.getExits().iterator(); ex_iter.hasNext();)
            {
                Exit exit = (Exit)ex_iter.next();
                exit.getDoneBus().setIDLogical(ID.showLogical(block)+"_DONE" + i++);
                for (Iterator db_iter = exit.getDataBuses().iterator(); db_iter.hasNext();)
                {
                    ((Bus)db_iter.next()).setIDLogical(ID.showLogical(block)+"_OUT" + j++);
                }
            }
        }
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Loop)
     */
    public void visit(Loop loop)
    {
        loop.setIDLogical("loop");

        if (DEBUG) debugln(ID.showDebug(loop) + " given logical name \"" +
            ID.showLogical(loop) + "\"");
        
        super.visit(loop);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.WhileBody)
     */
    public void visit(WhileBody whileBody)
    {
        whileBody.setIDLogical("whileBody");

        if (DEBUG) debugln(ID.showDebug(whileBody) + " given logical name \"" +
            ID.showLogical(whileBody) + "\"");
        
        super.visit(whileBody);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.UntilBody)
     */
    public void visit(UntilBody untilBody)
    {
        untilBody.setIDLogical("untilBody");

        if (DEBUG) debugln(ID.showDebug(untilBody) + " given logical name \"" +
            ID.showLogical(untilBody) + "\"");
        
        super.visit(untilBody);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.ForBody)
     */
    public void visit(ForBody forBody)
    {
        forBody.setIDLogical("forBody");

        if (DEBUG) debugln(ID.showDebug(forBody) + " given logical name \"" +
            ID.showLogical(forBody) + "\"");
        
        super.visit(forBody);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.AddOp)
     */
    public void visit(AddOp add)
    {
        add.setIDLogical("add");

        if (DEBUG) debugln(ID.showDebug(add) + " given logical name \"" +
            ID.showLogical(add) + "\"");
        
        super.visit(add);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.AndOp)
     */
    public void visit(AndOp andOp)
    {
        andOp.setIDLogical("andOp");

        if (DEBUG) debugln(ID.showDebug(andOp) + " given logical name \"" +
            ID.showLogical(andOp) + "\"");
        
        super.visit(andOp);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.CastOp)
     */
    public void visit(CastOp cast)
    {
        cast.setIDLogical("cast");

        if (DEBUG) debugln(ID.showDebug(cast) + " given logical name \"" +
            ID.showLogical(cast) + "\"");
        
        super.visit(cast);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.ComplementOp)
     */
    public void visit(ComplementOp complement)
    {
        complement.setIDLogical("complement");

        if (DEBUG) debugln(ID.showDebug(complement) + " given logical name \"" +
            ID.showLogical(complement) + "\"");
        
        super.visit(complement);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.ConditionalAndOp)
     */
    public void visit(ConditionalAndOp conditionalAnd)
    {
        conditionalAnd.setIDLogical("conditionalAnd");

        if (DEBUG) debugln(ID.showDebug(conditionalAnd) + " given logical name \"" +
            ID.showLogical(conditionalAnd) + "\"");
        
        super.visit(conditionalAnd);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.ConditionalOrOp)
     */
    public void visit(ConditionalOrOp conditionalOr)
    {
        conditionalOr.setIDLogical("conditionalOr");

        if (DEBUG) debugln(ID.showDebug(conditionalOr) + " given logical name \"" +
            ID.showLogical(conditionalOr) + "\"");
        
        super.visit(conditionalOr);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.Constant)
     */
    public void visit(Constant constant)
    {
        constant.setIDLogical("constant");

        if (DEBUG) debugln(ID.showDebug(constant) + " given logical name \"" +
            ID.showLogical(constant) + "\"");
        
        super.visit(constant);
    }


    /**
     * @see visit(Constant)
     */
    public void visit (LocationConstant loc)
    {
        visit((Constant) loc);
    }
    
    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.DivideOp)
     */
    public void visit(DivideOp divide)
    {
        divide.setIDLogical("divide");

        if (DEBUG) debugln(ID.showDebug(divide) + " given logical name \"" +
            ID.showLogical(divide) + "\"");
        
        super.visit(divide);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.EqualsOp)
     */
    public void visit(EqualsOp equals)
    {
        equals.setIDLogical("equals");

        if (DEBUG) debugln(ID.showDebug(equals) + " given logical name \"" +
            ID.showLogical(equals) + "\"");
        
        super.visit(equals);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.GreaterThanEqualToOp)
     */
    public void visit(GreaterThanEqualToOp greaterThanEqualTo)
    {
        greaterThanEqualTo.setIDLogical("greaterThanEqualTo");

        if (DEBUG) debugln(ID.showDebug(greaterThanEqualTo) + " given logical name \"" +
            ID.showLogical(greaterThanEqualTo) + "\"");
        
        super.visit(greaterThanEqualTo);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.GreaterThanOp)
     */
    public void visit(GreaterThanOp greaterThan)
    {
        greaterThan.setIDLogical("greaterThan");

        if (DEBUG) debugln(ID.showDebug(greaterThan) + " given logical name \"" +
            ID.showLogical(greaterThan) + "\"");
        
        super.visit(greaterThan);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.LeftShiftOp)
     */
    public void visit(LeftShiftOp leftShift)
    {
        leftShift.setIDLogical("leftShift");

        if (DEBUG) debugln(ID.showDebug(leftShift) + " given logical name \"" +
            ID.showLogical(leftShift) + "\"");
        
        super.visit(leftShift);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.LessThanEqualToOp)
     */
    public void visit(LessThanEqualToOp lessThanEqualTo)
    {
        lessThanEqualTo.setIDLogical("lessThanEqualTo");

        if (DEBUG) debugln(ID.showDebug(lessThanEqualTo) + " given logical name \"" +
            ID.showLogical(lessThanEqualTo) + "\"");
        
        super.visit(lessThanEqualTo);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.LessThanOp)
     */
    public void visit(LessThanOp lessThan)
    {
        lessThan.setIDLogical("lessThan");

        if (DEBUG) debugln(ID.showDebug(lessThan) + " given logical name \"" +
            ID.showLogical(lessThan) + "\"");
        
        super.visit(lessThan);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.MinusOp)
     */
    public void visit(MinusOp minus)
    {
        minus.setIDLogical("minus");

        if (DEBUG) debugln(ID.showDebug(minus) + " given logical name \"" +
            ID.showLogical(minus) + "\"");
        
        super.visit(minus);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.ModuloOp)
     */
    public void visit(ModuloOp modulo)
    {
        modulo.setIDLogical("modulo");

        if (DEBUG) debugln(ID.showDebug(modulo) + " given logical name \"" +
            ID.showLogical(modulo) + "\"");
        
        super.visit(modulo);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.MultiplyOp)
     */
    public void visit(MultiplyOp multiply)
    {
        multiply.setIDLogical("multiply");

        if (DEBUG) debugln(ID.showDebug(multiply) + " given logical name \"" +
            ID.showLogical(multiply) + "\"");
        
        super.visit(multiply);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.NotEqualsOp)
     */
    public void visit(NotEqualsOp notEquals)
    {
        notEquals.setIDLogical("notEquals");

        if (DEBUG) debugln(ID.showDebug(notEquals) + " given logical name \"" +
            ID.showLogical(notEquals) + "\"");
        
        super.visit(notEquals);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.NotOp)
     */
    public void visit(NotOp not)
    {
        not.setIDLogical("not");

        if (DEBUG) debugln(ID.showDebug(not) + " given logical name \"" +
            ID.showLogical(not) + "\"");
        
        super.visit(not);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.OrOp)
     */
    public void visit(OrOp or)
    {
        // The SimplePinConnector sets the name of the OrOpMulti and
        // we dont want to override that here.
        if (!or.hasExplicitName())
        {
            or.setIDLogical("or");
        }

        if (DEBUG) debugln(ID.showDebug(or) + " given logical name \"" +
            ID.showLogical(or) + "\"");
        
        super.visit(or);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.PlusOp)
     */
    public void visit(PlusOp plus)
    {
        plus.setIDLogical("plus");

        if (DEBUG) debugln(ID.showDebug(plus) + " given logical name \"" +
            ID.showLogical(plus) + "\"");
        
        super.visit(plus);
    }

    /**
     *      * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.ReductionOrOp)
     *      */
    public void visit(ReductionOrOp reductionOr)
    {
        reductionOr.setIDLogical("reductionOr");
        
        if (DEBUG) debugln(ID.showDebug(reductionOr) + " given logical name \"" + 
            ID.showLogical(reductionOr) + "\"");
    
        super.visit(reductionOr);
    }
    
    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.RightShiftOp)
     */
    public void visit(RightShiftOp rightShift)
    {
        rightShift.setIDLogical("rightShift");

        if (DEBUG) debugln(ID.showDebug(rightShift) + " given logical name \"" +
            ID.showLogical(rightShift) + "\"");
        
        super.visit(rightShift);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.RightShiftUnsignedOp)
     */
    public void visit(RightShiftUnsignedOp rightShiftUnsigned)
    {
        rightShiftUnsigned.setIDLogical("rightShiftUnsigned");

        if (DEBUG) debugln(ID.showDebug(rightShiftUnsigned) + " given logical name \"" +
            ID.showLogical(rightShiftUnsigned) + "\"");
        
        super.visit(rightShiftUnsigned);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.ShortcutIfElseOp)
     */
    public void visit(ShortcutIfElseOp shortcutIfElse)
    {
        shortcutIfElse.setIDLogical("shortcutIfElse");

        if (DEBUG) debugln(ID.showDebug(shortcutIfElse) + " given logical name \"" +
            ID.showLogical(shortcutIfElse) + "\"");
        
        super.visit(shortcutIfElse);
    }

    public void visit(TaskCall comp)
    {
        comp.setIDLogical("taskCall");
        
        if (DEBUG) debugln(ID.showDebug(comp) + " given logical name \"" +
            ID.showLogical(comp) + "\"");
        
        super.visit(comp);
    }

    public void visit(SimplePin comp)
    {
        // Do nothing.... leave the pins alone, their names are
        // handled elsewhere (they have a setName method).
    }
    
    public void visit(SimplePinAccess comp)
    {
        comp.setIDLogical("simplePinAccess");
        
        if (DEBUG) debugln(ID.showDebug(comp) + " given logical name \"" +
            ID.showLogical(comp) + "\"");
        
        super.visit(comp);
    }

    public void visit(SimplePinRead comp)
    {
        comp.setIDLogical("simplePinRead");
        
        if (DEBUG) debugln(ID.showDebug(comp) + " given logical name \"" +
            ID.showLogical(comp) + "\"");
        
        super.visit(comp);
    }

    public void visit(SimplePinWrite comp)
    {
        comp.setIDLogical("simplePinWrite");
        
        if (DEBUG) debugln(ID.showDebug(comp) + " given logical name \"" +
            ID.showLogical(comp) + "\"");
        
        super.visit(comp);
    }


    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.SubtractOp)
     */
    public void visit(SubtractOp subtract)
    {
        subtract.setIDLogical("subtract");

        if (DEBUG) debugln(ID.showDebug(subtract) + " given logical name \"" +
            ID.showLogical(subtract) + "\"");
        
        super.visit(subtract);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.NumericPromotionOp)
     */
    public void visit(NumericPromotionOp numericPromotion)
    {
        numericPromotion.setIDLogical("numericPromotion");

        if (DEBUG) debugln(ID.showDebug(numericPromotion) + " given logical name \"" +
            ID.showLogical(numericPromotion) + "\"");
        
        super.visit(numericPromotion);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.XorOp)
     */
    public void visit(XorOp xor)
    {
        xor.setIDLogical("xor");

        if (DEBUG) debugln(ID.showDebug(xor) + " given logical name \"" +
            ID.showLogical(xor) + "\"");
        
        super.visit(xor);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Branch)
     */
    public void visit(Branch branch)
    {
        branch.setIDLogical("branch");

        if (DEBUG) debugln(ID.showDebug(branch) + " given logical name \"" +
            ID.showLogical(branch) + "\"");
        
        super.visit(branch);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Decision)
     */
    public void visit(Decision decision)
    {
        decision.setIDLogical("decision");

        if (DEBUG) debugln(ID.showDebug(decision) + " given logical name \"" +
            ID.showLogical(decision) + "\"");
        
        super.visit(decision);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Switch)
     */
    public void visit(Switch sw)
    {
        sw.setIDLogical("switch");

        if (DEBUG) debugln(ID.showDebug(sw) + " given logical name \"" +
            ID.showLogical(sw) + "\"");
        
        super.visit(sw);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.InBuf)
     */
    public void visit(InBuf ib)
    {
        ib.setIDLogical("ib");

        if (DEBUG) debugln(ID.showDebug(ib) + " given logical name \"" +
            ID.showLogical(ib) + "\"");
        
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.OutBuf)
     */
    public void visit(OutBuf ob)
    {
        ob.setIDLogical("ob");

        if (DEBUG) debugln(ID.showDebug(ob) + " given logical name \"" +
            ID.showLogical(ob) + "\"");
        
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Reg)
     */
    public void visit(Reg reg)
    {
        String regName = ID.showLogical(reg);
        
        if (!reg.hasExplicitName())
        {
            Port dataport = reg.getDataPort();
            Bus sourceBus = dataport.getBus();
            if (sourceBus.hasExplicitName())
            {
                regName = ID.showLogical(sourceBus) + "_delayed";
            }
        }

        reg.setIDLogical((regName != null) ? regName : "reg");

        reg.getResultBus().setIDLogical(ID.showLogical(reg)+"_result");
        
        if (DEBUG) debugln(ID.showDebug(reg) + " given logical name \"" +
            ID.showLogical(reg) + "\"");
        
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Mux)
     */
    public void visit(Mux m)
    {
        m.setIDLogical("mux");

        if (DEBUG) debugln(ID.showDebug(m) + " given logical name \"" +
            ID.showLogical(m) + "\"");
        
        super.visit(m);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.EncodedMux)
     */
    public void visit(EncodedMux m)
    {
        m.setIDLogical("emux");

        if (DEBUG) debugln(ID.showDebug(m) + " given logical name \"" +
            ID.showLogical(m) + "\"");
        
        super.visit(m);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.PriorityMux)
     */
    public void visit(PriorityMux pmux)
    {
        pmux.setIDLogical("pmux");

        if (DEBUG) debugln(ID.showDebug(pmux) + " given logical name \"" +
            ID.showLogical(pmux) + "\"");
        
        super.visit(pmux);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.And)
     */
    public void visit(And a)
    {
        a.setIDLogical("and");

        if (DEBUG) debugln(ID.showDebug(a) + " given logical name \"" +
            ID.showLogical(a) + "\"");
        
        super.visit(a);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Not)
     */
    public void visit(Not n)
    {
        n.setIDLogical("not");

        if (DEBUG) debugln(ID.showDebug(n) + " given logical name \"" +
            ID.showLogical(n) + "\"");
        
        super.visit(n);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Or)
     */
    public void visit(Or o)
    {
        o.setIDLogical("or");

        if (DEBUG) debugln(ID.showDebug(o) + " given logical name \"" +
            ID.showLogical(o) + "\"");
        
        super.visit(o);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Scoreboard)
     */
    public void visit(Scoreboard scoreboard)
    {
        scoreboard.setIDLogical("scoreboard");

        if (DEBUG) debugln(ID.showDebug(scoreboard) + " given logical name \"" +
            ID.showLogical(scoreboard) + "\"");
        
        super.visit(scoreboard);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Latch)
     */
    public void visit(Latch latch)
    {
        latch.setIDLogical("latch");

        if (DEBUG) debugln(ID.showDebug(latch) + " given logical name \"" +
            ID.showLogical(latch) + "\"");
        
        super.visit(latch);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.NoOp)
     */
    public void visit(NoOp nop)
    {
        nop.setIDLogical("nop");

        if (DEBUG) debugln(ID.showDebug(nop) + " given logical name \"" +
            ID.showLogical(nop) + "\"");
        
        super.visit(nop);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.op.TimingOp)
     */
    public void visit(TimingOp nop)
    {
        nop.setIDLogical("tnop");

        if (DEBUG) debugln(ID.showDebug(nop) + " given logical name \"" +
            ID.showLogical(nop) + "\"");
        
        super.visit(nop);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.RegisterRead)
     */
    public void visit(RegisterRead regRead)
    {
        regRead.setIDLogical("regRead");

        if (DEBUG) debugln(ID.showDebug(regRead) + " given logical name \"" +
            ID.showLogical(regRead) + "\"");
        
        super.visit(regRead);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.RegisterWrite)
     */
    public void visit(RegisterWrite regWrite)
    {
        regWrite.setIDLogical("regWrite");

        if (DEBUG) debugln(ID.showDebug(regWrite) + " given logical name \"" +
            ID.showLogical(regWrite) + "\"");
        
        super.visit(regWrite);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.RegisterGateway)
     */
    public void visit(RegisterGateway regGateway)
    {
        regGateway.setIDLogical("regGateway");

        if (DEBUG) debugln(ID.showDebug(regGateway) + " given logical name \"" +
            ID.showLogical(regGateway) + "\"");
        
        super.visit(regGateway);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.RegisterReferee)
     */
    public void visit(RegisterReferee regReferee)
    {
        regReferee.setIDLogical("regReferee");

        if (DEBUG) debugln(ID.showDebug(regReferee) + " given logical name \"" +
            ID.showLogical(regReferee) + "\"");
        
        super.visit(regReferee);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.memory.MemoryRead)
     */
    public void visit(MemoryRead memRead)
    {
        memRead.setIDLogical("memRead");

        if (DEBUG) debugln(ID.showDebug(memRead) + " given logical name \"" +
            ID.showLogical(memRead) + "\"");
        
        super.visit(memRead);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.memory.MemoryWrite)
     */
    public void visit(MemoryWrite memWrite)
    {
        memWrite.setIDLogical("memWrite");

        if (DEBUG) debugln(ID.showDebug(memWrite) + " given logical name \"" +
            ID.showLogical(memWrite) + "\"");
        
        super.visit(memWrite);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.memory.MemoryReferee)
     */
    public void visit(MemoryReferee memReferee)
    {
        memReferee.setIDLogical("memReferee");

        if (DEBUG) debugln(ID.showDebug(memReferee) + " given logical name \"" +
            ID.showLogical(memReferee) + "\"");
        
        super.visit(memReferee);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.memory.MemoryGateway)
     */
    public void visit(MemoryGateway memGateway)
    {
        memGateway.setIDLogical("memGateway");

        if (DEBUG) debugln(ID.showDebug(memGateway) + " given logical name \"" +
            ID.showLogical(memGateway) + "\"");
        
        super.visit(memGateway);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.HeapRead)
     */
    public void visit(HeapRead heapRead)
    {
        heapRead.setIDLogical("heapRead");

        if (DEBUG) debugln(ID.showDebug(heapRead) + " given logical name \"" +
            ID.showLogical(heapRead) + "\"");
        
        super.visit(heapRead);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.ArrayRead)
     */
    public void visit(ArrayRead arrayRead)
    {
        arrayRead.setIDLogical("arrayRead");

        if (DEBUG) debugln(ID.showDebug(arrayRead) + " given logical name \"" +
            ID.showLogical(arrayRead) + "\"");
        
        super.visit(arrayRead);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.ArrayRead)
     */
    public void visit(AbsoluteMemoryRead read)
    {
        read.setIDLogical("absoluteRead");

        if (DEBUG) debugln(ID.showDebug(read) + " given logical name \"" +
            ID.showLogical(read) + "\"");
        
        super.visit(read);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.HeapWrite)
     */
    public void visit(HeapWrite heapWrite)
    {
        heapWrite.setIDLogical("heapWrite");

        if (DEBUG) debugln(ID.showDebug(heapWrite) + " given logical name \"" +
            ID.showLogical(heapWrite) + "\"");
        
        super.visit(heapWrite);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.ArrayWrite)
     */
    public void visit(ArrayWrite arrayWrite)
    {
        arrayWrite.setIDLogical("arrayWrite");

        if (DEBUG) debugln(ID.showDebug(arrayWrite) + " given logical name \"" +
            ID.showLogical(arrayWrite) + "\"");
        
        super.visit(arrayWrite);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.ArrayWrite)
     */
    public void visit(AbsoluteMemoryWrite write)
    {
        write.setIDLogical("absoluteWrite");

        if (DEBUG) debugln(ID.showDebug(write) + " given logical name \"" +
            ID.showLogical(write) + "\"");
        
        super.visit(write);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.PinRead)
     */
    public void visit(PinRead pinRead)
    {
        pinRead.setIDLogical("pinRead");

        if (DEBUG) debugln(ID.showDebug(pinRead) + " given logical name \"" +
            ID.showLogical(pinRead) + "\"");
        
        super.visit(pinRead);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.PinWrite)
     */
    public void visit(PinWrite pinWrite)
    {
        pinWrite.setIDLogical("pinWrite");

        if (DEBUG) debugln(ID.showDebug(pinWrite) + " given logical name \"" +
            ID.showLogical(pinWrite) + "\"");
        
        super.visit(pinWrite);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.PinStateChange)
     */
    public void visit(PinStateChange pinChange)
    {
        pinChange.setIDLogical("pinChange");

        if (DEBUG) debugln(ID.showDebug(pinChange) + " given logical name \"" +
            ID.showLogical(pinChange) + "\"");
        
        super.visit(pinChange);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.SRL16)
     */
    public void visit(SRL16 srl16)
    {
        srl16.setIDLogical("srl16");

        if (DEBUG) debugln(ID.showDebug(srl16) + " given logical name \"" +
            ID.showLogical(srl16) + "\"");
        
        super.visit(srl16);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.PinReferee)
     */
    public void visit(PinReferee pinReferee)
    {
        pinReferee.setIDLogical("pinReferee");

        if (DEBUG) debugln(ID.showDebug(pinReferee) + " given logical name \"" +
            ID.showLogical(pinReferee) + "\"");
        
        super.visit(pinReferee);
    }

    /**
     * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.TriBuf)
     */
    public void visit(TriBuf tbuf)
    {
        tbuf.setIDLogical("tbuf");

        if (DEBUG) debugln(ID.showDebug(tbuf) + " given logical name \"" +
            ID.showLogical(tbuf) + "\"");
        
        super.visit(tbuf);
    }
    
    public void visit(FifoAccess comp)
    {
        comp.setIDLogical("fifoAccess");
        
        if (DEBUG) debugln(ID.showDebug(comp) + " given logical name \"" +
            ID.showLogical(comp) + "\"");
        
        super.visit(comp);
    }

    public void visit(FifoRead comp)
    {
        comp.setIDLogical("fifoRead");
        
        if (DEBUG) debugln(ID.showDebug(comp) + " given logical name \"" +
            ID.showLogical(comp) + "\"");
        
        super.visit(comp);
    }

    public void visit(FifoWrite comp)
    {
        comp.setIDLogical("fifoWrite");
        
        if (DEBUG) debugln(ID.showDebug(comp) + " given logical name \"" +
            ID.showLogical(comp) + "\"");
        
        super.visit(comp);
    }

    /**
     * @see net.sf.openforge.lim.FilteredVisitor#filterAny(net.sf.openforge.lim.Component)
     */
    public void filterAny(Component c) 
    {
        String baseName = c.showIDLogical();
        for (Iterator exits = c.getExits().iterator(); exits.hasNext();)
        {
            Exit exit = (Exit)exits.next();
            int instanceCount=0;
            for (Iterator buses = exit.getDataBuses().iterator(); buses.hasNext();)
            {
                Bus bus = (Bus)buses.next();
                bus.setIDLogical((instanceCount > 0) ? 
                    new String(baseName + instanceCount) : baseName);
            }
        }
    }

    private String uniquifyCoreName (String name)
    {
        if (this.topModuleName.length() > 0)
        {
            return this.topModuleName + "_" + name;
        }
        return name;
    }
    
    
    /**
     * Private convenience method for printing debug.
     * 
     * @param string
     */
    private void debugln(String string)
    {
        System.out.println(string);
    }

    private class DesignElementNamingVisitor extends FilteredVisitor
    {

        public void visit (Design design)
        {
            // Avoid all task calls...
            Set taskCalls = new HashSet();
            for (Task task : design.getTasks())
            {
                taskCalls.add(task.getCall());
            }

            List<Component> elements = new ArrayList(design.getDesignModule().getComponents());

            while (!elements.isEmpty())
            {
                Visitable vis = elements.remove(0);
                if (!taskCalls.contains(vis))
                {
                    try
                    {
                        vis.accept(this);
                    }
                    catch (UnexpectedVisitationException uve)
                    {
                        // Register.Physical and other physical
                        // implementation modules may not be expecting
                        // visitation, which is OK.
                        if (vis instanceof Register.Physical)
                        {
                            filter((Module)vis);
                            elements.addAll(((Module)vis).getComponents());
                        }
                        else if (vis instanceof StructuralMemory)
                        {
                            filter((Module)vis);
                            elements.addAll(((Module)vis).getComponents());
                        }
                    }
                }
            }
        }

        public void filter (Module mod)
        {
            // It will likely be instantiated as an hdl module.
            // Give it a name that is unique to this compilation
            String idLogical = mod.showIDLogical();
            if (mod.getSourceName() != null)
            {
                idLogical = idLogical.replaceAll("_" + mod.getSourceName(),"");
            }
            mod.setIDLogical(LIMLogicalNamer.this.uniquifyCoreName(idLogical));
        }

        public void visit (MemoryBank vis)
        {
            String moduleName = "forge_memory_" + vis.getDepth() + "x" + vis.getWidth();
            vis.setIDLogical(LIMLogicalNamer.this.uniquifyCoreName(moduleName));
        }
    }
    
}
