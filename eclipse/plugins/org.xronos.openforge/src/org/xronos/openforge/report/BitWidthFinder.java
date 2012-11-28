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

package org.xronos.openforge.report;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.IPCoreCall;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Kicker;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.PinRead;
import org.xronos.openforge.lim.PinReferee;
import org.xronos.openforge.lim.PinStateChange;
import org.xronos.openforge.lim.PinWrite;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.PriorityMux;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.RegisterGateway;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterReferee;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.Scoreboard;
import org.xronos.openforge.lim.Switch;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.TriBuf;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.lim.io.SimplePinAccess;
import org.xronos.openforge.lim.io.SimplePinRead;
import org.xronos.openforge.lim.io.SimplePinWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.EndianSwapper;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.lim.memory.MemoryGateway;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryReferee;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.ComplementOp;
import org.xronos.openforge.lim.op.ConditionalAndOp;
import org.xronos.openforge.lim.op.ConditionalOrOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.EqualsOp;
import org.xronos.openforge.lim.op.GreaterThanEqualToOp;
import org.xronos.openforge.lim.op.GreaterThanOp;
import org.xronos.openforge.lim.op.LeftShiftOp;
import org.xronos.openforge.lim.op.LessThanEqualToOp;
import org.xronos.openforge.lim.op.LessThanOp;
import org.xronos.openforge.lim.op.MinusOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.NoOp;
import org.xronos.openforge.lim.op.NotEqualsOp;
import org.xronos.openforge.lim.op.NotOp;
import org.xronos.openforge.lim.op.NumericPromotionOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.PlusOp;
import org.xronos.openforge.lim.op.ReductionOrOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.RightShiftUnsignedOp;
import org.xronos.openforge.lim.op.ShortcutIfElseOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.TimingOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Mux;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.lim.primitive.SRL16;


/**
 * Class BitWidthFinder is a utility class used for sorting a set of components
 * according to the 'bit width' of that component. This is not a precise
 * determiniation as a given component may have unbalanced inputs, or certain
 * bits may be constant. However, in general the bit width of an operation is
 * the width of its output, or in the case of comparison operations, the width
 * is the largest of its inputs.
 * 
 * 
 * Created: Mon Apr 10 09:40:18 2006
 * 
 * @author imiller last modified by $Author:$
 * @version $Id: BitWidthFinder.java 132 2006-04-10 15:43:06Z imiller $
 */
public class BitWidthFinder implements Visitor {
	public static final int UNKNOWN = -1;
	public static final int IGNORE = -2;

	private int bitwidth = UNKNOWN;

	/**
	 * Creates a new <code>BitWidthFinder</code> instance.
	 * 
	 */
	private BitWidthFinder() {
	}

	/**
	 * This method returns a Map of Integer objects (whose value is the bit
	 * width) to the Set of object whose value is that bitwidth. If the bitwidth
	 * cannot be determined, or the object is of a type whose bitwidth is
	 * meaningless, the Integer bitwidth value will match either
	 * {@link #UNKNOWN} or {@link #IGNORE}.
	 * 
	 * @param a
	 *            Set of objects to determine the bit width of.
	 * @return a Map of Integer to Set where the Integer stores the value of the
	 *         bitwidth and the Set contains only those objects from comps.
	 */
	public static Map<Integer, Set<?>> sortByBitWidth(Set<?> comps) {
		BitWidthFinder finder = new BitWidthFinder();

		Map<Integer, Set<?>> widthToObject = new HashMap<Integer, Set<?>>();
		for (Object o : comps) {
			finder.bitwidth = UNKNOWN;
			if (o instanceof Visitable) {
				((Visitable) o).accept(finder);
			}
			Set set = widthToObject.get(new Integer(finder.bitwidth));
			if (set == null) {
				set = new HashSet();
				widthToObject.put(new Integer(finder.bitwidth), set);
			}
			set.add(o);
		}

		// Sort the list from greatest to least.
		List<Integer> sortedBitWidths = new LinkedList<Integer>();
		for (Integer width : widthToObject.keySet()) {
			boolean inserted = false;
			for (int i = 0; i < sortedBitWidths.size(); i++) {
				if (width.intValue() > sortedBitWidths.get(i)) {
					sortedBitWidths.add(i, width);
					inserted = true;
					break;
				}
			}
			if (!inserted)
				sortedBitWidths.add(width);
		}

		Map<Integer, Set<?>> sorted = new LinkedHashMap<Integer, Set<?>>();
		for (Integer width : sortedBitWidths) {
			sorted.put(width, widthToObject.get(width));
		}

		return sorted;
	}

	@Override
	public void visit(Design vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Task vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Call vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(IPCoreCall vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Procedure vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Block vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Loop vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(WhileBody vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(UntilBody vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(ForBody vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Branch vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Decision vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Switch vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(InBuf vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(OutBuf vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(TimingOp vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(RegisterGateway vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(RegisterReferee vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(MemoryReferee vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(MemoryGateway vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(MemoryBank vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Kicker vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(PinReferee vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(TaskCall vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(EndianSwapper vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(CastOp vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(ConditionalAndOp vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(ConditionalOrOp vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(ShortcutIfElseOp vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(NumericPromotionOp vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(Scoreboard vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(PinStateChange vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(TriBuf vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(SimplePinAccess vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(SimplePin vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(SimplePinRead vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(SimplePinWrite vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(FifoAccess vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(FifoRead vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(FifoWrite vis) {
		bitwidth = IGNORE;
	}

	@Override
	public void visit(AddOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(AndOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(ComplementOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(Constant vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(DivideOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(EqualsOp vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(GreaterThanEqualToOp vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(GreaterThanOp vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(LeftShiftOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(LessThanEqualToOp vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(LessThanOp vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(LocationConstant vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(MinusOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(ModuloOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(MultiplyOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(NotEqualsOp vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(NotOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(OrOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(PlusOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(ReductionOrOp vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(RightShiftOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(RightShiftUnsignedOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(SubtractOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(XorOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(Reg vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(Mux vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(EncodedMux vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(PriorityMux vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(And vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(Not vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(Or vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(Latch vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(NoOp vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(RegisterRead vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(RegisterWrite vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(MemoryRead vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(MemoryWrite vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(HeapRead vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(ArrayRead vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(HeapWrite vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(ArrayWrite vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(AbsoluteMemoryRead vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(AbsoluteMemoryWrite vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(PinRead vis) {
		getSingleOutputWidth(vis);
	}

	@Override
	public void visit(PinWrite vis) {
		getMaxInputWidth(vis);
	}

	@Override
	public void visit(SRL16 vis) {
		getSingleOutputWidth(vis);
	}

	private void getSingleOutputWidth(Component comp) {
		Exit done = comp.getExit(Exit.DONE);
		Bus data = done.getDataBuses().get(0);
		if (data.getValue() != null)
			bitwidth = data.getValue().getSize();
	}

	private void getMaxInputWidth(Component comp) {
		int max = -1;
		for (Port port : comp.getDataPorts()) {
			if (port.getValue() != null)
				max = Math.max(max, port.getValue().getSize());
		}
		if (max > 0)
			bitwidth = max;
	}

}
