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

package net.sf.openforge.lim.op;

import java.util.*;

import net.sf.openforge.lim.*;

/**
 * NoOp is an {@link Operation} which represents a pure flow of control
 * or data in a {@link Module}.  That is, it is a pass-through operation
 * that can stand in for non-computational operators such as break, continue,
 * return, etc.  Functionally, control and data values are passed through
 * from each {@link Port} to its corresponding {@link Bus}.
 *
 * @version $Id: NoOp.java 562 2008-03-17 21:33:12Z imiller $
 */
public class NoOp extends Operation implements Emulatable
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 562 $";

    public static final int LOCAL_READ  = 0x0;
    public static final int LOCAL_WRITE = 0x1;
    public static final int RETURN      = 0x2;
    public static final int PAREN       = 0x4;
    public static final int FILLER      = 0x8;

    /** The single exit */
    private Exit exit;

    private int reason;


    /**
     * Constructs a NoOp.
     *
     * @param dataCount the number of data values
     * @param type the type of the single exit
     * @param label the label of the exit, which together with the type comprises
     *          the Exit Exit.Tag
     * @param reason the purpose of the operation, one of
     * <ul>
     * <li>{@link NoOp#LOCAL_READ}
     * <li>{@link NoOp#LOCAL_WRITE}
     * <li>{@link NoOp#RETURN}
     * <li>{@link NoOp#PAREN}
     * <li>{@link NoOp#FILLER}
     * </ul>
     */
    public NoOp (int dataCount, Exit.Type type, String label, int reason)
    {
        super(dataCount);
        exit = makeExit(dataCount, type, label);

        switch (reason)
        {
            case LOCAL_READ:
            case LOCAL_WRITE:
            case RETURN:
            case PAREN:
            case FILLER:
                this.reason = reason;
                break;
            default:
                throw new IllegalArgumentException("invalid reason: " + reason);
        }
    }

    /**
     * Constructs a NoOp with a reason of {@link NoOp#FILLER}.
     *
     * @param dataCount the number of data values
     * @param type the type of the single exit
     * @param label the label of the exit, which together with the type comprises
     *          the Exit Exit.Tag
     */
    public NoOp (int dataCount, Exit.Type type, String label)
    {
        this(dataCount, type, label, FILLER);
    }

    /**
     * Constructs a NoOp with an unlabled Exit.
     *
     * @param dataCount the number of data values
     * @param type the type of the exit
     * @param reason the purpose of the operation, one of
     * <ul>
     * <li>{@link NoOp#LOCAL_READ}
     * <li>{@link NoOp#LOCAL_WRITE}
     * <li>{@link NoOp#RETURN}
     * <li>{@link NoOp#PAREN}
     * <li>{@link NoOp#FILLER}
     * </ul>
     */
    public NoOp (int dataCount, Exit.Type type, int reason)
    {
        this(dataCount, type, Exit.Tag.NOLABEL, reason);
    }

    /**
     * Constructs a NoOp with an unlabled Exit and a reson of {@link NoOp#FILLER}.
     *
     * @param dataCount the number of data values
     * @param type the type of the exit
     */
    public NoOp (int dataCount, Exit.Type type)
    {
        this(dataCount, type, FILLER);
    }

    /**
     * Constructs a NoOp whose Exit.Tag is the same as that
     * of an existings {@link Exit}.
     *
     * @param exit an existing exit, whose tag will be used as the tag of
     *          this operation's exit
     * @param reason the purpose of the operation, one of
     * <ul>
     * <li>{@link NoOp#LOCAL_READ}
     * <li>{@link NoOp#LOCAL_WRITE}
     * <li>{@link NoOp#RETURN}
     * <li>{@link NoOp#PAREN}
     * <li>{@link NoOp#FILLER}
     * </ul>
     */
    public NoOp (Exit exit, int reason)
    {
        this(exit.getDataBuses().size(), exit.getTag().getType(), exit.getTag().getLabel(),
            reason);
    }

    /**
     * Constructs a NoOp whose Exit.Tag is the same as that
     * of an existings {@link Exit}.  The reason defaults to {@link NoOp#FILLER}.
     *
     * @param exit an existing exit, whose tag will be used as the tag of
     *          this operation's exit
     */
    public NoOp (Exit exit)
    {
        this(exit, FILLER);
    }

    /**
     * Gets the single {@link Exit} of this operation.
     */
    public Exit getExit ()
    {
        return exit;
    }

    public int getReason ()
    {
        return reason;
    }

    public void accept (Visitor visitor)
    {
        visitor.visit(this);
    }

    
    /**
     * Gets the single data bus available on an NoOp, which
     * is the Exit's data bus.
     */
    public Bus getResultBus()
    {
        return (Bus)exit.getDataBuses().iterator().next();
    }

    /**
     * Returns true if all data buses on this NoOp are float
     * types, or false if there are no data buses.
     */
    public boolean isFloat ()
    {
        for (Iterator iter = getExit().getDataBuses().iterator(); iter.hasNext();)
        {
            if (!((Bus)iter.next()).isFloat())
                return false;
        }
        
        return getExit().getDataBuses().size() > 0;
    }

    public Object clone() throws CloneNotSupportedException
    {
        NoOp clone = (NoOp)super.clone();
        clone.exit = clone.getExit(this.exit.getTag());
        return clone;
    }

    /**
     * Performes a high level numerical emulation of this component.
     *
     * @param portValues a map of owner {@link Port} to {@link SizedInteger}
     *          input value
     * @return a map of {@link Bus} to {@link SizedInteger} result value
     */
    public Map emulate (Map portValues)
    {
        final Map resultMap = new HashMap();
        final Iterator portIter = getDataPorts().iterator();
        final Iterator busIter = getExit(Exit.DONE).getDataBuses().iterator();
        while (portIter.hasNext())
        {
            resultMap.put(busIter.next(), portValues.get(portIter.next()));
        }
        return resultMap;
    }


    
    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */
     
    public boolean pushValuesForward ()
    {
        boolean mod = false;
        
        final List<Port> ports = getDataPorts();
        final List<Bus> buses = getExit(Exit.DONE).getDataBuses();
        
        if (ports.size() == buses.size())
        {
            Iterator iter = buses.iterator();
            for (Port p : ports)
            {
                Bus b = (Bus)iter.next();
                boolean m = b.pushValueForward(p.getValue());
                if (m) mod = true;
            }
        }
        
        
        return mod;
    }
    
    public boolean pushValuesBackward ()
    {
        boolean mod = false;
        
        final List<Port> ports = getDataPorts();
        final List<Bus> buses = getExit(Exit.DONE).getDataBuses();
        
        if (ports.size() == buses.size())
        {
            Iterator iter = buses.iterator();
            for (Port p : ports)
            {
                final Bus b = (Bus)iter.next();
                final Value resultBusValue = b.getValue();
                if (!p.getValue().isConstant())
                {
                    Value newPushBackValue = new Value(p.getValue().getSize(), p.getValue().isSigned());
                    for (int i = 0; i < newPushBackValue.getSize(); i++)
                    {
                        Bit bit = resultBusValue.getBit(i);
                        if (!bit.isCare() || bit.isConstant())
                        {
                            newPushBackValue.setBit(i, Bit.DONT_CARE);
                        }
                    }
                    mod |= p.pushValueBackward(newPushBackValue);
                }
            }
        }
        
        return mod;
    }

    /*
     *    End new constant prop rules implementation.
     * =================================================
     */                 
}
