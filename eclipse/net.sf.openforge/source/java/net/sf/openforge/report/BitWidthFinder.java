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

package net.sf.openforge.report;


import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;

/**
 * Class BitWidthFinder is a utility class used for sorting a set of
 * components according to the 'bit width' of that component.  This is
 * not a precise determiniation as a given component may have
 * unbalanced inputs, or certain bits may be constant.  However, in
 * general the bit width of an operation is the width of its output,
 * or in the case of comparison operations, the width is the largest
 * of its inputs.  
 *
 *
 * Created: Mon Apr 10 09:40:18 2006
 *
 * @author imiller last modified by $Author:$
 * @version $Id: BitWidthFinder.java 132 2006-04-10 15:43:06Z imiller $
 */
public class BitWidthFinder implements Visitor
{
    public static final int UNKNOWN = -1;
    public static final int IGNORE = -2;
    
    private int bitwidth = UNKNOWN;
     
    /**
     * Creates a new <code>BitWidthFinder</code> instance.
     *
     */
    private BitWidthFinder()
    {}

    /**
     * This method returns a Map of Integer objects (whose value is
     * the bit width) to the Set of object whose value is that
     * bitwidth.  If the bitwidth cannot be determined, or the object
     * is of a type whose bitwidth is meaningless, the Integer
     * bitwidth value will match either {@link #UNKNOWN} or
     * {@link #IGNORE}.
     *
     * @param a Set of objects to determine the bit width of.
     * @return a Map of Integer to Set where the Integer stores the
     * value of the bitwidth and the Set contains only those objects
     * from comps.
     */
    public static Map<Integer, Set> sortByBitWidth (Set comps)
    {
        BitWidthFinder finder = new BitWidthFinder();

        Map<Integer, Set> widthToObject = new HashMap();
        for (Object o : comps)
        {
            finder.bitwidth = UNKNOWN;
            if (o instanceof Visitable)
            {
                ((Visitable)o).accept(finder);
            } 
            Set set = widthToObject.get(new Integer(finder.bitwidth));
            if (set == null)
            {
                set = new HashSet();
                widthToObject.put(new Integer(finder.bitwidth), set);
            }
            set.add(o);
        }

        // Sort the list from greatest to least.
        List<Integer> sortedBitWidths = new LinkedList();
        for (Integer width : widthToObject.keySet())
        {
            boolean inserted = false;
            for (int i=0; i < sortedBitWidths.size(); i++)
            {
                if (width.intValue() > sortedBitWidths.get(i))
                {
                    sortedBitWidths.add(i,width);
                    inserted = true;
                    break;
                }
            }
            if (!inserted)
                sortedBitWidths.add(width);
        }

        Map<Integer, Set> sorted = new LinkedHashMap();
        for (Integer width : sortedBitWidths)
        {
            sorted.put(width, widthToObject.get(width));
        }
        
        return sorted;
    }

    public void visit (Design vis) { this.bitwidth = IGNORE; }
    public void visit (Task vis) { this.bitwidth = IGNORE; }
    public void visit (Call vis) { this.bitwidth = IGNORE; }
    public void visit (IPCoreCall vis) { this.bitwidth = IGNORE; }
    public void visit (Procedure vis) { this.bitwidth = IGNORE; }
    public void visit (Block vis) { this.bitwidth = IGNORE; }
    public void visit (Loop vis) { this.bitwidth = IGNORE; }
    public void visit (WhileBody vis) { this.bitwidth = IGNORE; }
    public void visit (UntilBody vis) { this.bitwidth = IGNORE; }
    public void visit (ForBody vis) { this.bitwidth = IGNORE; }
    public void visit (Branch vis) { this.bitwidth = IGNORE; }
    public void visit (Decision vis) { this.bitwidth = IGNORE; }
    public void visit (Switch vis) { this.bitwidth = IGNORE; }
    public void visit (InBuf vis) { this.bitwidth = IGNORE; }
    public void visit (OutBuf vis) { this.bitwidth = IGNORE; }
    public void visit (TimingOp vis) { this.bitwidth = IGNORE; }
    public void visit (RegisterGateway vis) { this.bitwidth = IGNORE; }
    public void visit (RegisterReferee vis) { this.bitwidth = IGNORE; }
    public void visit (MemoryReferee vis) { this.bitwidth = IGNORE; }
    public void visit (MemoryGateway vis) { this.bitwidth = IGNORE; }
    public void visit (MemoryBank vis) { this.bitwidth = IGNORE; }
    public void visit (Kicker vis) { this.bitwidth = IGNORE; }
    public void visit (PinReferee vis) { this.bitwidth = IGNORE; }
    public void visit (TaskCall vis) { this.bitwidth = IGNORE; }
    public void visit (EndianSwapper vis) { this.bitwidth = IGNORE; }
    public void visit (CastOp vis) { this.bitwidth = IGNORE; }
    public void visit (ConditionalAndOp vis) { this.bitwidth = IGNORE; }
    public void visit (ConditionalOrOp vis) { this.bitwidth = IGNORE; }
    public void visit (ShortcutIfElseOp vis) { this.bitwidth = IGNORE; }
    public void visit (NumericPromotionOp vis) { this.bitwidth = IGNORE; }
    public void visit (Scoreboard vis) { this.bitwidth = IGNORE; }
    public void visit (PinStateChange vis) { this.bitwidth = IGNORE; }
    public void visit (TriBuf vis) { this.bitwidth = IGNORE; }
    public void visit (SimplePinAccess vis) { this.bitwidth = IGNORE; }
    public void visit (SimplePin vis) { this.bitwidth = IGNORE; }
    public void visit (SimplePinRead vis) { this.bitwidth = IGNORE; }
    public void visit (SimplePinWrite vis) { this.bitwidth = IGNORE; }
    public void visit (FifoAccess vis) { this.bitwidth = IGNORE; }
    public void visit (FifoRead vis) { this.bitwidth = IGNORE; }
    public void visit (FifoWrite vis) { this.bitwidth = IGNORE; }


    public void visit (AddOp vis) { getSingleOutputWidth(vis); }
    public void visit (AndOp vis) { getSingleOutputWidth(vis); }
    public void visit (ComplementOp vis) { getSingleOutputWidth(vis); }
    public void visit (Constant vis) { getSingleOutputWidth(vis); }
    public void visit (DivideOp vis) { getSingleOutputWidth(vis); }
    public void visit (EqualsOp vis) { getMaxInputWidth(vis); }
    public void visit (GreaterThanEqualToOp vis) { getMaxInputWidth(vis); }
    public void visit (GreaterThanOp vis) { getMaxInputWidth(vis); }
    public void visit (LeftShiftOp vis) { getSingleOutputWidth(vis); }
    public void visit (LessThanEqualToOp vis) { getMaxInputWidth(vis); }
    public void visit (LessThanOp vis) { getMaxInputWidth(vis); }
    public void visit (LocationConstant vis) { getSingleOutputWidth(vis); }
    public void visit (MinusOp vis) { getSingleOutputWidth(vis); }
    public void visit (ModuloOp vis) { getSingleOutputWidth(vis); }
    public void visit (MultiplyOp vis) { getSingleOutputWidth(vis); }
    public void visit (NotEqualsOp vis) { getMaxInputWidth(vis); }
    public void visit (NotOp vis) { getSingleOutputWidth(vis); }
    public void visit (OrOp vis) { getSingleOutputWidth(vis); }
    public void visit (PlusOp vis) { getSingleOutputWidth(vis); }
    public void visit (ReductionOrOp vis) { getMaxInputWidth(vis); }
    public void visit (RightShiftOp vis) { getSingleOutputWidth(vis); }
    public void visit (RightShiftUnsignedOp vis) { getSingleOutputWidth(vis); }
    public void visit (SubtractOp vis) { getSingleOutputWidth(vis); }
    public void visit (XorOp vis) { getSingleOutputWidth(vis); }
    public void visit (Reg vis) { getSingleOutputWidth(vis); }
    public void visit (Mux vis) { getSingleOutputWidth(vis); }
    public void visit (EncodedMux vis) { getSingleOutputWidth(vis); }
    public void visit (PriorityMux vis) { getSingleOutputWidth(vis); }
    public void visit (And vis) { getSingleOutputWidth(vis); }
    public void visit (Not vis) { getSingleOutputWidth(vis); }
    public void visit (Or vis) { getSingleOutputWidth(vis); }
    public void visit (Latch vis) { getSingleOutputWidth(vis); }
    public void visit (NoOp vis) { getSingleOutputWidth(vis); }
    public void visit (RegisterRead vis) { getSingleOutputWidth(vis); }
    public void visit (RegisterWrite vis) { getMaxInputWidth(vis); }
    public void visit (MemoryRead vis) { getSingleOutputWidth(vis); }
    public void visit (MemoryWrite vis) { getMaxInputWidth(vis); }
    public void visit (HeapRead vis) { getSingleOutputWidth(vis); }
    public void visit (ArrayRead vis) { getSingleOutputWidth(vis); }
    public void visit (HeapWrite vis) { getMaxInputWidth(vis); }
    public void visit (ArrayWrite vis) { getMaxInputWidth(vis); }
    public void visit (AbsoluteMemoryRead vis) { getSingleOutputWidth(vis); }
    public void visit (AbsoluteMemoryWrite vis) { getMaxInputWidth(vis); }
    public void visit (PinRead vis) { getSingleOutputWidth(vis); }
    public void visit (PinWrite vis) { getMaxInputWidth(vis); }
    public void visit (SRL16 vis) { getSingleOutputWidth(vis); }

    private void getSingleOutputWidth (Component comp)
    {
        Exit done = comp.getExit(Exit.DONE);
        Bus data = done.getDataBuses().get(0);
        if (data.getValue() != null)
            this.bitwidth = data.getValue().getSize();
    }
    
    private void getMaxInputWidth (Component comp)
    {
        int max = -1;
        for (Port port : comp.getDataPorts())
        {
            if (port.getValue() != null)
                max = Math.max(max, port.getValue().getSize());
        }
        if (max > 0)
            this.bitwidth = max;
    }
    
}
