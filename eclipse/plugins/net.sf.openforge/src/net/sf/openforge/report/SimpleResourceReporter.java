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

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.lim.primitive.And;
import net.sf.openforge.lim.primitive.EncodedMux;
import net.sf.openforge.lim.primitive.Mux;
import net.sf.openforge.lim.primitive.Not;
import net.sf.openforge.lim.primitive.Or;
import net.sf.openforge.lim.primitive.Reg;
import net.sf.openforge.lim.primitive.SRL16;

/**
 * Describe class SimpleResourceReporter here.
 *
 *
 * Created: Tue Jun 27 09:36:50 2006
 *
 * @author imiller last modified by $Author:$
 * @version $Id: SimpleResourceReporter.java 149 2006-06-28 17:34:19Z imiller $
 */
public class SimpleResourceReporter extends DefaultVisitor
{
    private ResourceBank currentScope = null;

    /**
     * Creates a new <code>SimpleResourceReporter</code> instance.
     *
     */
    public SimpleResourceReporter ()
    {
        setTraverseComposable(true);
    }

    public ResourceBank getTopResource()
    {
        return currentScope;
    }

    public void visit (Design vis)
    {
        currentScope = new DesignResource(vis);
        super.visit(vis);
    }
    
    public void visit (Task vis)
    {
        ResourceBank outerScope = currentScope;
        currentScope = new TaskResource(vis);
        super.visit(vis);
        outerScope.addResource(currentScope);
        currentScope = outerScope;
    }
    
    public void visit (Call vis)
    {
        ResourceBank outerScope = currentScope;
        currentScope = new ProcedureResource(vis.getProcedure());
        super.visit(vis);
        outerScope.addResource(currentScope);
        ((ProcedureResource)currentScope).generateTotalReport();
        currentScope = outerScope;
    }

    // These are all modules which simply contain other stuff
    public void visit (Procedure vis) { super.visit(vis); }
    public void visit (Block vis) { super.visit(vis); }
    public void visit (Loop vis) { super.visit(vis); }
    public void visit (WhileBody vis) { super.visit(vis); }
    public void visit (UntilBody vis) { super.visit(vis); }
    public void visit (ForBody vis) { super.visit(vis); }
    public void visit (Branch vis) { super.visit(vis); }
    public void visit (Decision vis) { super.visit(vis); }
    public void visit (Switch vis) { super.visit(vis); }
    public void visit (RegisterGateway vis) { super.visit(vis); }
    public void visit (RegisterReferee vis) { super.visit(vis); }
    public void visit (MemoryReferee vis) { super.visit(vis); }
    public void visit (MemoryGateway vis) { super.visit(vis); }
    public void visit (Kicker vis) { super.visit(vis); }
    public void visit (EndianSwapper vis) { super.visit(vis); }
    public void visit (FifoAccess vis) { super.visit(vis); }
    public void visit (FifoRead vis) { super.visit(vis); }
    public void visit (FifoWrite vis) { super.visit(vis); }
    public void visit (HeapRead vis) { super.visit(vis); }
    public void visit (ArrayRead vis) { super.visit(vis); }
    public void visit (HeapWrite vis) { super.visit(vis); }
    public void visit (ArrayWrite vis) { super.visit(vis); }
    public void visit (AbsoluteMemoryRead vis) { super.visit(vis); }
    public void visit (AbsoluteMemoryWrite vis) { super.visit(vis); }
    public void visit (SimplePinAccess vis) { super.visit(vis); }

    // These are 'atomic' elements
    public void visit (AddOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (AndOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (CastOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (ComplementOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (ConditionalAndOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (ConditionalOrOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (Constant vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (DivideOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (EqualsOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (GreaterThanEqualToOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (GreaterThanOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (LeftShiftOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (LessThanEqualToOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (LessThanOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (LocationConstant vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (MinusOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (ModuloOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (MultiplyOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (NotEqualsOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (NotOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (OrOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (PlusOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (ReductionOrOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (RightShiftOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (RightShiftUnsignedOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (SubtractOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (NumericPromotionOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (XorOp vis) { currentScope.addResource(vis); super.visit(vis); }

    public void visit (Reg vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (Mux vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (EncodedMux vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (PriorityMux vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (And vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (Not vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (Or vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (Scoreboard vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (Latch vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (NoOp vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (MemoryBank vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (SRL16 vis) { currentScope.addResource(vis); super.visit(vis); }

    // These are all either referencers or referenceables
    public void visit (RegisterRead vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (RegisterWrite vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (MemoryRead vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (MemoryWrite vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (TaskCall vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (SimplePinRead vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (SimplePinWrite vis) { currentScope.addResource(vis); super.visit(vis); }
    public void visit (SimplePin vis) { currentScope.addResource(vis); super.visit(vis); }

    public void visit (InBuf vis) { super.visit(vis); }
    public void visit (OutBuf vis) { super.visit(vis); }

    // unexpected/unused or old/obsolete
    public void visit (IPCoreCall vis) { super.visit(vis); }
    public void visit (TimingOp vis) { super.visit(vis); }
    public void visit (PinRead vis) { super.visit(vis); }
    public void visit (PinWrite vis) { super.visit(vis); }
    public void visit (PinStateChange vis) { super.visit(vis); }
    public void visit (PinReferee vis) { super.visit(vis); }
    public void visit (ShortcutIfElseOp vis) { super.visit(vis); }
    public void visit (TriBuf vis) { super.visit(vis); }
}

