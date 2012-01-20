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


import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.forge.api.internal.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.*;


/**
* A DesignModule is Module based upon a LIM {@link Procedure}.
 * <P>
 *
 * Created: Tue Mar 12 09:46:58 2002
 *
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: DesignModule.java 23 2005-09-09 18:45:32Z imiller $
 */

public class DesignModule extends net.sf.openforge.verilog.model.Module 
    implements MappedModuleSpecifier
{
    private static final String _RCS_ = "RCS_REVISION: $Rev: 23 $";

    private static int call_instanceCount = 0;
    
    private Design design;
    
    private Set mappedModules = new HashSet();

    /** When set to true the Ports of this module will be reversed in
     * their Range notation. */
    private boolean REVERSE_PORTS;
    
    /**
     * Construct a DesignModule based on a {@link Design}.
     *
     * @param design the Design which is being instantiated
     */
    public DesignModule (Design design)
    {
        super (modifyNameForEDK(ID.toVerilogIdentifier(ID.showLogical(design)), design));
                
        this.REVERSE_PORTS = design.getEngine().getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.INVERT_DESIGN_PORT_RANGE);
        
        this.design = design;
        defineSimplePins(design);

        // still define the 'old' interface for now.  It should just
        // contain the CLK and RESET ports.
        defineInterface();
        
    } // DesignModule

    /**
     * determine if module name needs to be modified to be compatable with
     * EDK and return the modified name if so.  otherwise return the
     * unmodified name
     *
     * @param name
     */
    private static String modifyNameForEDK (String name, Design design)
    {
        boolean noEDK = design.getEngine().getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.NO_EDK);

        if (noEDK)
        {
            return name;
        }
        else
        {
            return name.toLowerCase();
        }
    }
    

    /**
     * For each simple pin defined in this design, create a
     * corresponding port on the top level application (regardless of
     * whether its connected to anything).  For output ports connect
     * the design port to the bus that drives it.  Input ports are
     * taken care of by name (ie the name of the source bus of the pin
     * matches the name of the port and so no explicit assign is
     * needed)
     */
    private void defineSimplePins (Design des)
    {
        //for (Iterator iter = des.getSimplePins().iterator(); iter.hasNext();)
        for (Iterator iter = des.getDesignModule().getComponents().iterator(); iter.hasNext();)
        {
            Visitable vis = (Visitable)iter.next();
            if (vis instanceof SimplePin)
            {
                //SimplePin pin = (SimplePin)iter.next();
                SimplePin pin = (SimplePin)vis;
                if (pin.isPublished())
                {
                    if (pin.getXLatData().isInput())
                    {
                        addPort(new Input(pin.getName(), pin.getWidth()));
                    }
                    else
                    {
                        Output out = new Output(pin.getName(), pin.getWidth());
                        addPort(out);
                        explicitlyConnect(out, pin.getXLatData().getSink());
                    }
                }
                else
                {
                    // Assign the source bus to be the value of the
                    // connection to the sink port
                    if (pin.getXLatData().getSource() != null && pin.getXLatData().getSink() != null)
                    {
                        Net output = NetFactory.makeNet(pin.getXLatData().getSource());
                        PortWire input = new PortWire(pin.getXLatData().getSink());
                        state(new ForgeStatement(Collections.singleton(output), new Assign.Continuous(output, input)));
                    }
                    else
                    {
                        System.out.println("Error in construction!  internal pins must have both source and sink (" + pin.getName() + ")");
                    }
                }
            }
        }
    }
    
    
    /**
     * Defines the Module ports based on the Design's InputPins,
     * OutputPins, and BidirectionalPins.
     */
    private void defineInterface()
    {
        // define input ports (based on the InputPins)
        for (Iterator pins = design.getInputPins().iterator(); pins.hasNext();)
        {
            InputPin pin = (InputPin)pins.next();
            
            if(!Core.hasThisPin(pin.getApiPin()) || Core.hasPublished(pin.getApiPin()))
            {
                // XXX: FIXME - for some reason - designs that do not
                // neet a clock have the clock show up in the input
                // pins list anyway.
                if(!design.consumesClock())
                {
                    // check if this pin is a clock, and if so skip it
                    // since the design doesn't need it
                    if(!design.getClockPins().contains(pin))
                    {
                        addPort(new InputPinPort(pin));
                    }
                }
                else
                {
                    addPort(new InputPinPort(pin));
                }
                
            }
            //addPort(new InputPinPort((InputPin)pins.next()));
        }
        
        // define output ports (based on OutputPins)
        for (Iterator pins = design.getOutputPins().iterator(); pins.hasNext();)
        {
            OutputPin pin = (OutputPin)pins.next();

            if(!Core.hasThisPin(pin.getApiPin()) || Core.hasPublished(pin.getApiPin()))
            {
                OutputPinPort pin_port = new OutputPinPort(pin); 
                addPort(pin_port);
                explicitlyConnect(pin_port, pin.getPort());
            }
        }
        
        // define in-out ports (based on BidirectionalPins)
        for (Iterator pins = design.getBidirectionalPins().iterator(); pins.hasNext();)
        {
            BidirectionalPin biPin = (BidirectionalPin)pins.next();

            if(!Core.hasThisPin(biPin.getApiPin()) || Core.hasPublished(biPin.getApiPin()))
            {
                BidirectionalPinPort pinPort = new BidirectionalPinPort(biPin);
                addPort(pinPort);
                explicitlyConnect(pinPort, biPin.getPort());
            }
        }
        
    } // defineInterface

    private void explicitlyConnect (Net pinPort, Port port)
    {
        // add explicit assignment to OutputPin
        PortWire pinPortWire = new PortWire(port);
        Expression portWire = pinPortWire;
        // In the case of Characters the wire feeding the Port
        // will be wider than the pin (but not the pin's port).
        // To catch this case, truncate the wire to the pin's width.
        if (pinPort.getWidth() < pinPortWire.getWidth())
        {
            portWire = pinPortWire.getRange(pinPort.getWidth()-1,0);
        }

        state(new ForgeStatement(Collections.EMPTY_SET,
                  new Assign.Continuous(pinPort, portWire)));
    }

    /**
     * Depending on the state of the preference {@link net.sf.openforge.app.project.ProjectDefiner#INVERT_DESIGN_PORT_RANGES}
     * this method will create a custom declaration for the ports
     * which uses the [0:n] notation instead of the standard [n:0]
     * notation 
     *
     * @param net a value of type 'Net'
     */
    protected void declarePort (Net net)
    {
        if (REVERSE_PORTS)
        {
            declare(new NetDeclarationReversed(net));
        }
        else
        {
            super.declarePort(net);
        }
    }
    
    /**
     * Adds a statement to the statement block of the module,
     * and a declaration for each undeclared Net produced by
     * the statement.
     */
    public void state(Statement statement)
    {
        assert (
            (statement instanceof ForgePattern) ||
            (statement instanceof InlineComment)
            ) : "DesignModule only supports stating ForgePatterns.";
        
        if (statement instanceof ForgePattern)
        {
            for (Iterator it = ((ForgePattern)statement).getProducedNets().iterator(); it.hasNext();)
            {
                Net net = (Net)it.next();
                if (!isDeclared(net))
                {
                    declare(net);
                }
            }
        }
        statements.add(statement);

    // bubble bath
        if (statement instanceof MappedModuleSpecifier)
        {
            mappedModules.addAll(((MappedModuleSpecifier)statement).getMappedModules());
        }
        
    } // state()

    /**
     * Provides the Set of MappedModules
     */
    public Set getMappedModules()
    {
        return mappedModules;
    }
} // class DesignModule






