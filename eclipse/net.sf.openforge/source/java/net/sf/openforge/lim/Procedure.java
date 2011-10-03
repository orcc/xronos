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

import net.sf.openforge.app.*;
import net.sf.openforge.app.project.*;
import net.sf.openforge.forge.api.entry.*;
import net.sf.openforge.forge.api.pin.*;


/**
 * A Procedure is a {@link Component} wrapper that allows the
 * Component to be invoked via the execution of a {@link Call}.
 * Each Call is created with the {@link Procedure#makeCall()}
 * method, which allows the Procedure to keep a record of all
 * Calls.  A Call may be discarded with the method
 * {@link Procedure#removeCall(Call)}.
 * <P>
 * A Procedure represents the definition of a call-by-name.  It
 * is expected that the Procedure will be defined once in the
 * output HDL and that the HDL compiler will link each call
 * to that definition.  Procedures are not in themselves shared;
 * each Call represents a new instantiation of the hardware
 * defined by the Procedure.
 * 
 * @author  Stephen Edwards
 * @version $Id: Procedure.java 284 2006-08-15 15:43:34Z imiller $
 */
public class Procedure extends Referent 
    implements Visitable, Cloneable, Configurable
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 284 $";

    /** The contents of this procedure */
    private Block body;

    /** True if this procedure returns a value */
    private boolean hasReturnValue;

    /** DOCUMENT ME */
    private EntryMethod entryMethod=null;
    
    
    /** The label used for OptionDB look-ups. */
    SearchLabel odbLabel;
    
    /**
     * Constructs a Procedure with a null EntryMethod.
     *
     * @param body the contents of the procedure
     * @param hasReturnValue if true, ensures that the RETURN {@link Exit}
     *          of <i>body</i> has at least one data {@link Bus}
     */
    public Procedure (Block body, boolean hasReturnValue)
    {
        this(body,hasReturnValue,null);
    }

    /**
     * Constructs a Procedure with the specified {@link EntryMethod}
     *
     * @param body the contents of the procedure
     * @param hasReturnValue if true, ensures that the RETURN {@link Exit}
     *          of <i>body</i> has at least one data {@link Bus}
     */
    public Procedure (Block body, boolean hasReturnValue, EntryMethod em)
    {
        super();
        odbLabel = new CodeLabel(this, null);
        this.body = body;
        this.entryMethod=em;
        body.setProcedure(this);
        if (hasReturnValue)
        {
            body.getExit(Exit.RETURN).makeDataBus();
        }
    }

    /**
     * Constructs a Procedure with a return value.
     *
     * @param body the contents of the procedure
     */
    public Procedure (Block body)
    {
        this(body, false, null);
        final Exit returnExit = body.getExit(Exit.RETURN);
        assert (returnExit == null) || (returnExit.getDataBuses().size() == 0);
    }

    public void accept(Visitor vis)
    {
        vis.visit(this);
    }

    /**
     * Gets the body of this procedure.
     */
    public Block getBody ()
    {
        return body;
    }

    /**
     * Creates and returns a new call to this procedure.
     */
    public Call makeCall ()
    {
        Call call = new Call(this);
        addReference(call);
        return call;
    }

    /**
     * Gets all calls that were created from this procedure,
     * except for those that were removed.
     *
     * @return a collection of Calls
     */
    public Collection getCalls ()
    {
        return getReferences();
    }

    /**
     * Removes a call from this procedure's list of known calls.
     *
     * @param call a call that was created from this procedure
     */
    public void removeCall (Call call)
    {
        removeReference(call);
    }

    /**
     * Tests whether this procedure returns a value.
     */
    public boolean hasReturnValue ()
    {
        return !body.getExit(Exit.RETURN).getDataBuses().isEmpty();
    }

    /**
     * Gets the latency of a reference's exit.
     */
    public Latency getLatency (Exit exit)
    {
        Call call = (Call)exit.getOwner();
        return call.getProcedureExit(exit).getLatency();
    }

    public GenericJob getGenericJob()
    {
        return EngineThread.getGenericJob();
    }
    
   
     /**
     * Returns the Configurable parent. The parent is involved
     * in hierarchical searches for option values.
     * <P>
     * <B>ABK NOTE:</B>For now this always returns null, since
     * there is no apparent way to know which call chain to
     * follow.
     *
     * @return null 
     */
    public Configurable getConfigurableParent()
    {
        return null;
    }

    public String getOptionLabel()
    {
        if (odbLabel.getLabel() == null)
            return getClass().getName() + "@" + Integer.toHexString(hashCode());
        else
            return odbLabel.getLabel();
        //return odbLabel.toString();
    }

    public void setSearchLabel (SearchLabel sl)
    {
        this.odbLabel = sl;
    }
    
    /**
     * Returns the string label associated with this Configurable.
     * 
     */
    public SearchLabel getSearchLabel()
    {
        return odbLabel;
    }
    
    /**
     * @return The name of this procedure, usually what the user typed in
     *  their source code minus any qualification information such as
     *  package and class for java.
     */
    public String getName()
    {
        net.sf.openforge.util.naming.IDSourceInfo info = getIDSourceInfo();
        
        return (info != null) ? info.getMethodName() : "";
    }
    
    /**
     * Returns a complete and fully connected (internally) copy of
     * this procedure.  The model object, however, is the same as in
     * the original Procedure.
     * note that the localOptions are not cloned 
     *
     * @return a Procedure object.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        Procedure clone = (Procedure)super.clone();
        clone.body = (Block)this.body.clone();
        clone.body.setProcedure(clone);
        copy(this, clone);

        clone.odbLabel = new CodeLabel(clone, null);
        
        clone.setEntryMethod(entryMethod);        
        
        return clone;
    }

    public EntryMethod getEntryMethod() { return entryMethod; }

    public void setEntryMethod(EntryMethod em) { entryMethod=em; }
    
    public String toString ()
    {
        String ret = super.toString();
//         for (Iterator portIter = body.getPorts().iterator(); portIter.hasNext();)
//         {
//             Port port = (Port)portIter.next();
//             if (port == body.getClockPort())
//                 ret += " ck:" + port;
//             else if (port == body.getResetPort())
//                 ret += " rs:" + port;
//             else if (port == body.getGoPort())
//                 ret += " go:" + port;
//             else if (port == body.getThisPort())
//                 ret += " th:" + port;
//             else 
//                 ret += " dp:" + port;
//         }
        return ret;
    }

    /**
     * Returns the api {@link ClockDomain} in which this Procedure
     * operates.
     */
    public ClockDomain getClockDomain()
    {
        if (entryMethod == null)
        {
            return net.sf.openforge.forge.api.pin.ClockDomain.GLOBAL;
        }
        else
        {
            return entryMethod.getDomain();
        }
    }

    /**
     * Returns the name to use for the reset signal
     * @return the name of the reset signal
     * @deprecated
     */
    public String getSuspendName()
    {
        /* FIXME: commented out until the halt signal
           is fully supported. The user API should have
           no mention of suspend until then.
        if (entryMethod == null)
        {
            return net.sf.openforge.forge.api.pin.SuspendPin.GLOBAL.getName();
        }
        else
        {
            return entryMethod.getSuspendPin().getName();
        }
        */
        return "";
    }

    /**
     * Returns the name to use for the go signal
     * @return the name of the go signal
     * @deprecated
     */
    public String getGoName()
    {
        return "GO";
//         if (entryMethod == null)
//         {
//             return net.sf.openforge.forge.api.pin.GoPin.GLOBAL.getName();
//         }
//         else
//         {
//             return entryMethod.getGoPin().getName();
//         }
    }
    
    /**
     * Returns the name to use for the done signal
     * @return the name of the done signal
     * @deprecated
     */
    public String getDoneName()
    {
        return "DONE";
//         if (entryMethod == null)
//         {
//             return net.sf.openforge.forge.api.pin.DonePin.GLOBAL.getName();
//         }
//         else
//         {
//             return entryMethod.getDonePin().getName();
//         }
    }
    
    /**
     * Returns the name to use for the result signal
     * @return the name of the result signal
     * @deprecated
     */
    public String getResultName()
    {
        if (entryMethod == null)
        {
            return net.sf.openforge.forge.api.pin.ResultPin.GLOBAL.getName();
        }
        else
        {
            return entryMethod.getResultPin().getName();
        }
    }

    /**
     * Returns the api pin to use for the reset signal
     * @return the api pin
     */
    public SuspendPin getSuspendPin()
    {
        /* FIXME: commented out until the halt signal
           is fully supported. The user API should have
           no mention of suspend until then.
        if (entryMethod == null)
        {
            return net.sf.openforge.forge.api.pin.SuspendPin.GLOBAL;
        }
        else
        {
            return entryMethod.getSuspendPin();
        }
        */
        return null;
    }

    /**
     * Returns the api pin to use for the go signal
     * @return the api pin
     */
    public GoPin getGoPin()
    {
        if (entryMethod == null)
        {
            return net.sf.openforge.forge.api.pin.GoPin.GLOBAL;
        }
        else
        {
            return entryMethod.getGoPin();
        }
    }
    
    /**
     * Returns the api pin to use for the done signal
     * @return the api pin
     */
    public DonePin getDonePin()
    {
        if (entryMethod == null)
        {
            return net.sf.openforge.forge.api.pin.DonePin.GLOBAL;
        }
        else
        {
            return entryMethod.getDonePin();
        }
    }
    
    /**
     * Returns the api pin to use for the result signal
     * @return the api pin
     */
    public ResultPin getResultPin()
    {
        if (entryMethod == null)
        {
            return net.sf.openforge.forge.api.pin.ResultPin.GLOBAL;
        }
        else
        {
            return entryMethod.getResultPin();
        }
    }
    
}



