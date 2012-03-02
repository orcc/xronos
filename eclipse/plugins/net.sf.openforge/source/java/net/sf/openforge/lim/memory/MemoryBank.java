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

package net.sf.openforge.lim.memory;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;

/**
 * MemoryBank is an atomic memory element and may be single or dual
 * ported.  This class contains methods that aid in the instantiation
 * of this object in a {@link StructuralMemory}
 *
 * <p>Created: Wed Feb 12 13:09:40 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryBank.java 536 2007-11-19 22:10:55Z imiller $
 */
public class MemoryBank extends Component
{
    private static final String _RCS_ = "$Rev: 536 $";

    /** The bit width of this memory bank */
    private final int width;
    /** The number of memory lines in this bank. */
    private final int depth;

    /** Governs the number of bits per addressable location in this bank. */
    private final AddressStridePolicy addrPolicy;
    
    /**
     * List of BankPort objects to identify accessable ports of this
     * MemoryBank
     */
    private List<BankPort> ports = Collections.EMPTY_LIST;

    /** Implementation details for this MemoryBank */
    private MemoryImplementation implementation = null;

    /**
     * List of Number objects representing the initial values of this
     * MemoryBank.
     */
    private AddressableUnit[][] initValues = null;

    /** The bit width of the address lines to this bank. */
    private int addrWidth;

    
    /**
     * Constructs a new MemoryBank with a given width, depth and
     * implementation.
     *
     * @param width, the bit width of this bank
     * @param depth, the number of lines this bank contains
     * @param addrPolicy, the AddressStridePolicy that governs how
     * many bits make up one addressable location in this bank.
     * @param imp, the {@link MemoryImplementation} governing the way
     * this bank is to be implemented.
     * @param addrWidth, the bit width of the address lines to this bank
     */
    public MemoryBank (int width, int depth, AddressStridePolicy addrPolicy,
        MemoryImplementation imp, int addrWidth)
    {
        super();
        getClockPort().setUsed(true);
        this.width = width;
        this.depth = depth;
        this.addrPolicy = addrPolicy;
        this.implementation = imp;

        Exit exit = makeExit(0, Exit.DONE);

        clearInitValues();

        this.addrWidth = addrWidth;
    }

    /**
     * Constructs a new MemoryBank with a given width, depth and
     * implementation, where the address width is the log2 of the
     * depth. 
     *
     * @param width, the bit width of this bank
     * @param depth, the number of lines this bank contains
     * @param addrPolicy, the AddressStridePolicy that governs how
     * many bits make up one addressable location in this bank.
     * @param imp, the {@link MemoryImplementation} governing the way
     * this bank is to be implemented.
     */
    public MemoryBank (int width, int depth, AddressStridePolicy addrPolicy, MemoryImplementation impl)
    {
        this(width, depth, addrPolicy, impl, net.sf.openforge.util.MathStuff.log2(depth));
    }


    /**
     * Creates and records an accessible port to this MemoryBank and
     * records whether the port is read or written although there is
     * no difference in the way the port is created based on
     * read/write.
     */
    public BankPort createPort (boolean read, boolean write, String writeMode)
    {
        // 4 ports. address, data in, enable, write enable.
        Port a = makeDataPort();
        Port d = makeDataPort();
        Port e = makeDataPort();
        Port w = makeDataPort();

        Bus b = getExit(Exit.DONE).makeDataBus();
        
        if (this.ports == Collections.EMPTY_LIST)
            this.ports = new ArrayList(2);
        BankPort port = createBankPort(a, d, e, w, b, read, write, writeMode);
        this.ports.add(port);
        return port;
    }

    /**
     * A utility method used to create a BankPort which is associated
     * with this MemoryBank.  This is needed because BankPort is not a
     * static class, and thus a simple way to create the BankPort is
     * needed when cloning.
     */
    private BankPort createBankPort (Port a, Port d, Port e, Port w, Bus b, boolean rd, boolean wr, String writeMode)
    {
        return new BankPort(a, d, e, w, b, rd, wr, writeMode);
    }

    public void setInitValues (AddressableUnit[][] values, int offset)
    {
        assert (values.length > 0) && (values[0].length > offset);
        clearInitValues();

        // Offset in number of locations.  If this is bank 2 and banks are
        // 16 bits wide each, and each addressable location is 8 bits
        // then our offset is 2 * 16/8 = 4.
        final int numLocations = (this.width / getAddressStridePolicy().getStride());
        offset = offset * numLocations;
        
        for (int i=0; i < getDepth(); i++)
        {
            for (int j=0; j < numLocations; j++)
            {
                //System.out.println("Setting row " + i + " col " + j + " from values row: " + i + " col " + (j+offset));
                initValues[i][j] = values[i][j + offset];
            }
        }
    }

    private void clearInitValues()
    {
        int stride = getAddressStridePolicy().getStride();
        assert (this.width % stride) == 0 : "unbalanced stride ("+stride+")and bank width("+this.width+")";
        final int numLocations = (this.width / stride);
        this.initValues = new AddressableUnit[depth][numLocations];
        for (int i=0; i < depth; i++)
        {
            for (int j=0; j < numLocations; j++)
            {
                this.initValues[i][j] = AddressableUnit.ZERO_UNIT;
            }
        }
    }

    /**
     * Gets the List of {@link BankPort} objects for this MemoryBank.
     */
    public List<BankPort> getBankPorts ()
    {
        return Collections.unmodifiableList(ports);
    }

    /**
     * Returns the bit width of this MemoryBank.
     */
    public int getWidth ()
    {
        return this.width;
    }

    /**
     * Returns the number of addressable locations in this MemoryBank.
     */
    public int getDepth ()
    {
        return this.depth;
    }

    /**
     * Returns the {@link AddressStridePolicy} that governs how
     * many bits make up one addressable location in this bank.
     */
    public AddressStridePolicy getAddressStridePolicy ()
    {
        return this.addrPolicy;
    }
    
    /**
     * Returns the width necessary to cover the 'depth' of this memory
     * bank bounded on the low end with 1 bit.
     */
    public int getAddrWidth ()
    {
        // We must always have at least 1 bit for address...
        return Math.max(this.addrWidth, 1);
    }

    /**
     * Returns a 2-D array of bytes, where the first dimension is the
     * depth of this MemoryBank ({@link MemoryBank#getDepth}) and the
     * second dimension is the number of bytes wide this bank is.
     */
    public AddressableUnit[][] getInitValues ()
    {
        return this.initValues;
    }
    
    /**
     * Returns the {@link MemoryImplementation} for this MemoryBank,
     * which identifies the type of memory (lut vs block) and access
     * characteristics (combinational or sequential read).
     */
    public MemoryImplementation getImplementation ()
    {
        return this.implementation;
    }
    
    /**
     * Instantiates and connects the logic that is needed on a
     * per-memory-bank basis that is responsible for taking the n-bit
     * wide select bus and slicing out the specific bit used by this
     * MemoryBank and then qualifying it with the enable and write
     * enable
     * <p><img src=doc-files/ByteSel.png>
     *
     * @param owner a value of type 'Module'
     * @param bankNum a value of type 'int'
     * @param select a 'Bus' containing all the bank select bits, from
     * which to select the 'bankNum' bit for our select
     */
    public void instantiateSelectLogic (StructuralMemory owner, int bankNum,
        Bus select, Bus enable, StructuralMemory.StructuralMemoryPort memPort,
        BankPort bankPort)
    {
        // Create/connect the enables
        final Bus shiftedSelect;
        if (bankNum > 0)
        {
            Constant selectShiftConst = new SimpleConstant(bankNum, 32);
            final int shiftStages = selectShiftConst.getValueBus().getValue().getSize();
            owner.addComponent(selectShiftConst);
            RightShiftUnsignedOp selectShift = new RightShiftUnsignedOp(shiftStages);
            selectShift.setIDLogical("selectShift");
            selectShift.getLeftDataPort().setBus(select);
            selectShift.getRightDataPort().setBus(selectShiftConst.getValueBus());
            owner.addComponent(selectShift);
            shiftedSelect = selectShift.getResultBus();
        }
        else
        {
            // Since we only connect to something that has 1 bit
            // (the ands) we'll let them dictate that only the lsb is
            // used
            shiftedSelect = select;
        }

        // mask the bank enable with the incoming enable
        if (memPort.isRead() || memPort.isWrite())
        {
            /*
             * AND the enable with the LSB from the shifted value.
             */
            final CastOp trunc = new CastOp(1, false);
            trunc.getDataPort().setBus(shiftedSelect);
            owner.addComponent(trunc);

            And enAnd = new And(2);
            ((Port)enAnd.getDataPorts().get(0)).setBus(trunc.getResultBus());
            ((Port)enAnd.getDataPorts().get(1)).setBus(enable);
            enAnd.setIDLogical("enAnd");
            owner.addComponent(enAnd);

            bankPort.getEnablePort().setBus(enAnd.getResultBus());
        }
        else
        {
            // would be odd, but this is safest.
            bankPort.getEnablePort().setBus(owner.getZero().getValueBus());
        }
        

        if (memPort.isWrite())
        {
            /*
             * AND the enable with the LSB from the shifted value.
             */
            final CastOp trunc = new CastOp(1, false);
            trunc.getDataPort().setBus(shiftedSelect);
            owner.addComponent(trunc);

            And wenAnd = new And(2);
            ((Port)wenAnd.getDataPorts().get(0)).setBus(trunc.getResultBus());
            ((Port)wenAnd.getDataPorts().get(1)).setBus(memPort.getWriteEnablePort().getPeer());
            wenAnd.setIDLogical("wenAnd");
            owner.addComponent(wenAnd);

            bankPort.getWriteEnablePort().setBus(wenAnd.getResultBus());
        }
        else
        {
            bankPort.getWriteEnablePort().setBus(owner.getZero().getValueBus());
        }
    }

    /**
     * Instantiates the logic necessary to manage data into and out of
     * this MemoryBank as instantiated in a {@link StructuralMemory}
     * <p><img src=doc-files/StructuralDataFlow.png>
     *
     * @return the data output {@link Bus}
     */
    public Bus instantiateDataLogic (Module owner, Bus dataIn, Bus bankMask,
        int bankNum, int memWidth, BankPort bankPort)
    {
        //
        // Instantiate the logic to move the data output into the
        // correct byte lane
        //
        // Logically 'and' the output with the mask.
        AndOp and = new AndOp();
        owner.addComponent(and);
        and.getLeftDataPort().setBus(bankPort.getDataOutBus());
        and.getRightDataPort().setBus(bankMask);
        and.setIDLogical("datMask" + bankNum);

        Bus dataInput = dataIn;
        Bus bankResult = and.getResultBus();

        /*
         * If necessary, cast the output value to the same size
         * as the memory prior to shifting.
         */
        if (getWidth() != memWidth)
        {
            final CastOp outputCastOp = new CastOp(memWidth, false);
            owner.addComponent(outputCastOp);
            outputCastOp.setIDLogical("doutCast_" + bankNum);
            outputCastOp.getDataPort().setBus(bankResult);
            bankResult = outputCastOp.getResultBus();
        }

        if (bankNum > 0)
        {
            Constant shiftMag = new SimpleConstant(bankNum * getWidth(), 32);
            owner.addComponent(shiftMag);
            final int dataStages = shiftMag.getValueBus().getValue().getSize();

            RightShiftUnsignedOp inShift = new RightShiftUnsignedOp(dataStages);
            owner.addComponent(inShift);
            inShift.setIDLogical("dinShift" + bankNum);
            inShift.getLeftDataPort().setBus(dataIn);
            inShift.getRightDataPort().setBus(shiftMag.getValueBus());
            dataInput = inShift.getResultBus();

            LeftShiftOp outShift = new LeftShiftOp(dataStages);
            owner.addComponent(outShift);
            outShift.setIDLogical("doutShift" + bankNum);
            outShift.getLeftDataPort().setBus(bankResult);
            outShift.getRightDataPort().setBus(shiftMag.getValueBus());
            
            bankResult = outShift.getResultBus();
        }

        /*
         * If necessary, cast the input value to the same width
         * as this bank after shifting.
         */
        if (getWidth() != memWidth)
        {
            final CastOp inputCastOp = new CastOp(getWidth(), false);
            owner.addComponent(inputCastOp);
            inputCastOp.setIDLogical("dinCast_" + bankNum);
            inputCastOp.getDataPort().setBus(dataInput);
            dataInput = inputCastOp.getResultBus();
        }

        bankPort.getDataInPort().setBus(dataInput);

        return bankResult;
    }

    /**
     * Performs forward constant propagation through this component.  This
     * component will fetch the incoming {@link Value} from each {@link Port}
     * using {@link Port#_getValue()}.  It will then compute a new outgoing
     * {@link Value} for each {@link Bus} and set it with
     * {@link Bus#pushValueForward(Value)}.
     *
     * @return true if any of the bus values was modified, false otherwise
     */
    protected boolean pushValuesForward ()
    {
        /*
         * Ensure that the data output bus is the width of the memory.
         */
        for (Iterator iter = getBankPorts().iterator(); iter.hasNext();)
        {
            BankPort port = (BankPort)iter.next();
            port.getDataOutBus().pushValueForward(new Value(getWidth(), false));
        }
        return false;
    }
    
    /**
     * Performs reverse constant propagation inside through component.  This
     * component will fetch the incoming {@link Value} from each {@link Bus}
     * using {@link Bus#_getValue()}.  It will then compute a new outgoing
     * {@link Value} for each {@link Port} and set it with
     * {@link Port#pushValueBackward(Value)}.
     *
     * @return true if any of the port values was modified, false otherwise
     */
    protected boolean pushValuesBackward ()
    {
        return false;
    }
    
    public void accept (Visitor vis)
    {
        vis.visit(this);
    }

    public Object clone () throws CloneNotSupportedException
    {
        MemoryBank clone = (MemoryBank)super.clone();
        if (this.ports == Collections.EMPTY_LIST)
        {
            clone.ports = Collections.EMPTY_LIST;
        }
        else
        {
            clone.ports = new ArrayList(this.ports.size());
            List cPorts = clone.getDataPorts();
            List oPorts = this.getDataPorts();
            List cBus = clone.getExit(Exit.DONE).getDataBuses();
            List oBus = this.getExit(Exit.DONE).getDataBuses();
            for (Iterator iter = this.ports.iterator(); iter.hasNext();)
            {
                BankPort orig = (BankPort)iter.next();
                BankPort clonePort = clone.createBankPort(
                    (Port)cPorts.get(oPorts.indexOf(orig.addr)),
                    (Port)cPorts.get(oPorts.indexOf(orig.din)),
                    (Port)cPorts.get(oPorts.indexOf(orig.en)),
                    (Port)cPorts.get(oPorts.indexOf(orig.we)),
                    (Bus)cBus.get(oBus.indexOf(orig.dout)),
                    orig.isRead(), orig.isWrite(), new String(orig.getWriteMode())
                    );
                clone.ports.add(clonePort);
            }
        }
        return clone;
    }
    

    /**
     * A simple class to tie together the ports and buses associated
     * with one access port of this MemoryBank.
     */
    public class BankPort
    {
        Port addr;
        Port din;
        Port en;
        Port we;
        Bus dout;

        boolean read;
        boolean write;

        String writeMode;
        
        public BankPort (Port a, Port d, Port e, Port w, Bus b, boolean rd, boolean wr, String writeMode)
        {
            this.addr = a;
            this.din = d;
            this.en = e;
            this.we = w;
            this.dout = b;
            this.read = rd;
            this.write = wr;
            this.writeMode = writeMode;
        }

        public Port getAddressPort () { return this.addr; }
        public Port getDataInPort () { return this.din; }
        public Port getEnablePort () { return this.en; }
        public Port getWriteEnablePort () { return this.we; }
        public Bus getDataOutBus () { return this.dout; }

        public boolean isRead () { return this.read; }
        public boolean isWrite () { return this.write; }

        public String getWriteMode ()
        {
            return this.writeMode;
        }
        
        public void remove ()
        {
            assert MemoryBank.this.ports.contains(this);

            MemoryBank.this.ports.remove(this);
            
            MemoryBank.this.removeDataPort(this.addr);
            MemoryBank.this.removeDataPort(this.din);
            MemoryBank.this.removeDataPort(this.en);
            MemoryBank.this.removeDataPort(this.we);
            MemoryBank.this.getExit(Exit.DONE).removeDataBus(this.dout);
        }
    }

    /**
     * Returns a {@link Signature} object suitable for determining if
     * two MemoryBank's are going to result in indentical
     * implementations.  This is used in the VerilogTranslator to
     * eliminate redundant modules for memories.
     */
    public Signature getSignature ()
    {
        return new Signature(this);
    }

    /**
     * The Signature class simply captures the relevent information
     * for determining if two memories are identical.  The
     * implementation is designed such that the signature can be used
     * as a key in a map, or added to a set so that 2 identical (in
     * implementation) memories will generate only 1 value in the map.
     */
    private static class Signature
    {
        private List bankPorts;
        private AddressableUnit[][] initValues;
        private int width;
        private int depth;
        private MemoryImplementation impl;

        public Signature (MemoryBank bank)
        {
            this.bankPorts = new ArrayList(bank.getBankPorts());
            this.initValues = bank.getInitValues();
            this.width = bank.getWidth();
            this.depth = bank.getDepth();
            this.impl = bank.getImplementation();
        }
        
        public boolean equals (Object o)
        {
            if (!(o instanceof Signature)) return false;
            Signature comp = (Signature)o;
            if (comp.bankPorts.size() != this.bankPorts.size()) return false;
            if (comp.width != this.width) return false;
            if (comp.depth != this.depth) return false;
            if (initValues.length != comp.initValues.length)    return false;

            // Test implementation char.
            if (!comp.impl.getReadLatency().equals(this.impl.getReadLatency())) return false;
            if (comp.impl.isROM() != this.impl.isROM()) return false;
            if (!comp.impl.isROM() && !this.impl.isROM())
                if (!comp.impl.getWriteLatency().equals(this.impl.getWriteLatency())) return false;
            if (comp.impl.isLUT() != this.impl.isLUT()) return false;
            if (comp.impl.isDefault() != this.impl.isDefault()) return false;
            if (comp.impl.isDPReadFirst() != this.impl.isDPReadFirst()) return false;
            
            for (int i=0; i < initValues.length; i++)
            {
                if (!Arrays.equals(initValues[i], comp.initValues[i])) return false;
            }
            
            Iterator thisIter = bankPorts.iterator();
            Iterator compIter = comp.bankPorts.iterator();
            while(thisIter.hasNext())
            {
                MemoryBank.BankPort local = (MemoryBank.BankPort)thisIter.next();
                MemoryBank.BankPort test = (MemoryBank.BankPort)compIter.next();
                if (local.isRead() != test.isRead())   return false;
                if (local.isWrite() != test.isWrite()) return false;
                if (!testSize(local.getAddressPort(), test.getAddressPort())) return false;
                if (!testSize(local.getDataInPort(), test.getDataInPort()))   return false;
                if (!testSize(local.getDataOutBus(), test.getDataOutBus()))   return false;
                if (!local.getWriteMode().equals(test.getWriteMode()))        return false;
            }
            return true;
        }
        
        private boolean testSize (Port p1, Port p2)
        {
            return p1.getValue().getSize() == p2.getValue().getSize();
        }
        
        private boolean testSize (Bus b1, Bus b2)
        {
            return b1.getValue().getSize() == b2.getValue().getSize();
        }
        
        public int hashCode ()
        {
            // Generate the hash.  Any multipliers are to help space
            // out the values.
            int hash = (this.width + this.depth) * 1000;
            for (int i=0; i < initValues.length; i++)
                for (int j=0; j < initValues[i].length; j++)
                    hash += initValues[i][j].hashCode();

            hash += this.impl.getReadLatency().getMinClocks();
            if (!this.impl.isROM())
                hash += this.impl.getWriteLatency().getMinClocks();
            hash += this.impl.isLUT()     ?  250 : 0;
            hash += this.impl.isROM()     ?  500 : 0;
            hash += this.impl.isDefault() ? 1000 : 0;
            hash += this.impl.isDPReadFirst() ? 2000 : 0;
            
            hash += bankPorts.size();
            for (Iterator iter = bankPorts.iterator(); iter.hasNext();)
            {
                MemoryBank.BankPort bp = (MemoryBank.BankPort)iter.next();
                hash += bp.isRead() ?   1000000 : 0;
                hash += bp.isWrite() ? 10000000 : 0;
                if (bp.getAddressPort().getValue() != null)
                    hash += bp.getAddressPort().getValue().getSize();
                if (bp.getDataInPort().getValue() != null)
                    hash += bp.getDataInPort().getValue().getSize();
            }
            return hash;
        }
    }
    
}// MemoryBank

