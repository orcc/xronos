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
package net.sf.openforge.verilog.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Module is a VerilogElement which defines a verilog module.
 * It is a container for the actual components of a module:
 * ModuleDeclaration, { Statement | Block }, <strong>endmodule</strong>
 * <P>
 * Example:<BR>
 * <CODE>
 * module foo(arg0, arg1, result);
 *
 * input [31:0] arg0, arg1; // NetDeclaration of Inputs
 * output [31:0] result;    // NetDeclaration of Outputs
 *
 * result = arg0 + arg1;    // Assign of an expression
 *
 * endmodule
 * </CODE>
 * Created: Fri Jan 26 2001
 *
 * @author abk
 * @version $Id: Module.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Module implements VerilogElement
{

    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    /** This Module's identifier. */
    Identifier identifier;
    
    /** The actual modules declaration, with the identifier and port list */
    ModuleDeclaration module_declaration;
    
    /** Internal port, net, and other declarations. */
    List declarations;

    /** Collection of Identifiers for each declared Net */
    HashMap declared_nets;
    
    /** Statements which manipulate the nets, may be simple or compound. */
    protected List statements;
    
    /**
     * Constructs an unpopulated Module with the specified identifier.
     *
     * @param identifier the Identifier for this module
     */
    public Module(Identifier identifier) 
    {
        this(identifier, null);
    } // Module()
    
    /**
     * Constructs an unpopulated Module with the specified identifier
     * and ports. NetDeclarations are automatically created and added
     * for each of the ports.
     *
     * @param identifier the Identifier for this module
     * @param ports a NetList to be used as ports for the module
     */
    public Module(Identifier identifier, Net[] ports) 
    {
        this.identifier = identifier;
        declarations = new ArrayList();
        declared_nets = new LinkedHashMap();
        statements = new ArrayList();
        
        if (ports != null)
        {
            module_declaration = new ModuleDeclaration(identifier, ports);

            // add declarations for module ports
            for (int i=0; i<ports.length; i++)
            {
                declare(new NetDeclaration(ports[i]));
            }
        }
        else
        {
            module_declaration = new ModuleDeclaration(identifier);
        }
        
    } // Module()
    
    public Module(String identifier) 
    {
        this(new Identifier(identifier));
    }
    
    public Module(String identifier, Net[] ports)
    {
        this(new Identifier(identifier), ports);
    }
    
    public Identifier getIdentifier()
    {
        return identifier;
    }
    
    /**
     * Adds an input port (and the related declartion) to the module.
     */
    public void addPort(Input in)
    {
    	try 
    	{
        	module_declaration.add(in);
    	} catch (VerilogSyntaxException vse)
    	{
    		throw new VerilogSyntaxException("Module \"" + getIdentifier().toString() + "\"" +
    			" failed to add port because: " + vse.getMessage());
    	}
        declarePort(in);
    } // addPort()
    
    /**
     * Adds an output port (and the related declartion) to the module.
     */
    public void addPort(Output out)
    {
        module_declaration.add(out);
        declarePort(out);
    } // addPort()
    
    /**
     * Adds an in/out port (and the related declartion) to the module.
     */
    public void addPort(Inout io)
    {
        module_declaration.add(io);
        declarePort(io);
    } // addPort()

    /**
     * This method allows subclasses to override the way in which the
     * ports (input, output, or inout) are declared in this module.
     * DesignModule, for example, overrides this method in order to
     * reverse the port range declarations.
     *
     * @param portNet a value of type 'Net'
     */
    protected void declarePort (Net portNet)
    {
        declare(portNet);
    }
    

    /**
     * Checks whether the module has a port with a particular name.
     *
     * @return true if the module has the named port
     */
    public boolean hasPort(Identifier id)
    {
        return module_declaration.hasPort(id);
    }
    
    /**
     * Creates and then adds a NetDeclaration for the given net.
     */
    public void declare(Net net)
    {
        if (!isDeclared(net))
        {
            NetDeclaration declaration = null;
            if (net instanceof InitializedMemory)
                declaration = new MemoryDeclaration((InitializedMemory)net);
            else
                declaration = new NetDeclaration(net);
            declare(declaration);
        } 
        else 
        {
            NetDeclaration declaration = (NetDeclaration)declared_nets.get(net.getIdentifier());
            Keyword declared_type = declaration.getType();
            Keyword proposed_type = net.getType();
            
            // only allow a WIRE to be upgraded to an INPUT or OUTPUT
            if ((proposed_type.equals(Keyword.INPUT) || proposed_type.equals(Keyword.OUTPUT)) &&
                (declared_type.equals(Keyword.WIRE)))
            {
                undeclare(net);
                declare(net);
            }
            else
            {
                throw new VerilogSyntaxException("Attempt to re-declare a net: " + net.toString());
            }
        }
    } // declare(Net)

    /**
     * Adds a statement to the declaration block of the module.
     */
    public void declare(NetDeclaration declaration)
    {
        declarations.add(declaration);
        for (Iterator it = declaration.getNets().iterator(); it.hasNext();)
        {
            Net net = (Net)it.next();
            declared_nets.put(net.getIdentifier(), declaration);
        }
    }
    
    /**
     * Removes the Net from the NetDeclaration in which it appears,
     * and possibly removes that declaration if it contains no other Nets.
     */
    public void undeclare(Net net)
    {
        NetDeclaration declaration = (NetDeclaration)declared_nets.get(net.getIdentifier());
        declared_nets.remove(net.getIdentifier());
        declaration.remove(net);
        if (declaration.getNets().size() == 0)
        {
            declarations.remove(declaration);
        }
    }
    
    public boolean isDeclared(Net net)
    {
        return declared_nets.containsKey(net.getIdentifier());   
    }
    
    /**
     * Adds a statement to the statement block of the module,
     * and a declaration for each undeclared Net used in the 
     * statement.
     */
    public void state (Statement statement)
    {
        for (Iterator it = statement.getNets().iterator(); it.hasNext();)
        {
            Net net = (Net)it.next();
            if (net instanceof MemoryElement)
            {
                //System.out.println("Assuming that someone else has declared it");
                continue;
            }
            /*
             * QualfiedNets are global references, so they don't require a declaration
             */
            if (!(net instanceof QualifiedNet))
            {
                // Stick in a patch to prevent declaring a dummy empty
                // named wire which was created during forge memory
                // module creation.  
                if (!isDeclared((Net)net) && !((Net)net).getIdentifier().toString().equals(""))
                {
                    declare((Net)net);
                }
            }
        }
        statements.add(statement);
    }

    public Lexicality lexicalify()
    {
        Lexicality lex = new Lexicality();
        
        lex.append(module_declaration);
        
        // append declarations
        for (Iterator it = declarations.iterator(); it.hasNext();)
        {
            Statement s = (Statement)it.next();
            lex.append(s);
        }
        
        // append statements
        for (Iterator it = statements.iterator(); it.hasNext();)
        {
            Statement s = (Statement)it.next();
            lex.append(s);
        }

        lex.append(Keyword.ENDMODULE);
        lex.append(Control.NEWLINE);

        return lex;
    } // lexicalify()
    
    
    public String toString() 
    {
        StringBuffer reply = new StringBuffer();
        
        reply.append(module_declaration.toString() + "\n");

        // append declarations
        for (Iterator it = declarations.iterator(); it.hasNext();)
        {
            Statement s = (Statement)it.next();
            reply.append(s.toString());
            if (it.hasNext()) 
            {
                reply.append(Control.NEWLINE.toString());
                
            }
        }
        
        if (declarations.size() > 0)
            reply.append(Control.NEWLINE.toString());
        
        // append statements
        for (Iterator it = statements.iterator(); it.hasNext();)
        {
            Statement s = (Statement)it.next();
            reply.append(s.toString());
            if (it.hasNext()) 
            {
                reply.append(Control.NEWLINE.toString());
            }
        }

        if (statements.size() > 0)
            reply.append(Control.NEWLINE.toString());
        
        reply.append(Keyword.ENDMODULE);
        reply.append(Control.NEWLINE.toString());

        return reply.toString();
    } // toString()

} // end of class Module
