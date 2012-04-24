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

package net.sf.openforge.optimize.constant;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.op.SubtractOp;

/**
 * FullFloatingPointHelper work in conjunction with FullConstantVisitor
 * analyzing each component in the LIM and replaces it with a floating point
 * constant node if all inputs are constant valued. Floating point constant
 * values are represented with the raw long/int bits representation.
 * 
 * @author ysyu
 * @version $Id: FullFloatingPointHelper.java 2 2005-06-09 20:00:48Z imiller $
 */
class FullFloatingPointHelper {

	/**
	 * Propogates constants across method boundary. The Value connected to the
	 * Call port is transferred to the Bus of the InBuf in Procedure. Assumes
	 * that all {@link Procedure Procedures} are already unique.
	 * 
	 * @param call
	 *            a method call
	 * @param fullv
	 *            the visitor that called this method.
	 */
	// static void preProcess (Call call, FullConstantVisitor fullv)
	// {
	// /**
	// * Copy the Values associated with Call data ports to
	// * Procedure InBuf buses.
	// */
	// for(Iterator piter = call.getDataPorts().iterator(); piter.hasNext();)
	// {
	// Port port = (Port)piter.next();
	// Number number = fullv.getPortConstantValue(port);
	// if(number != null)
	// {
	// if(number instanceof Double)
	// {
	// long rawLongBits = Double.doubleToRawLongBits(number.doubleValue());
	// Bus ppeerBus = call.getProcedurePort(port).getPeer();
	// ppeerBus.setValue(Value.getConstantValue(rawLongBits));
	// }
	// else
	// {
	// int rawIntBits = Float.floatToRawIntBits(number.floatValue());
	// Bus ppBus = call.getProcedurePort(port).getPeer();
	// ppBus.setValue(Value.getConstantValue(rawIntBits));
	// }
	// }
	// }
	// }

	/**
	 * Copies any constant value applied to the OutBuf back over to the output
	 * Buses of the Call.
	 * 
	 * @param call
	 *            a method call.
	 * @param fullv
	 *            the visitor that called this method.
	 */
	// static void process (Call call, FullConstantVisitor fullv)
	// {
	// Exit exit = call.getExit(Exit.DONE);
	// for(Iterator iter = exit.getDataBuses().iterator(); iter.hasNext();)
	// {
	// Bus bus = (Bus)iter.next();
	// Port pport = call.getProcedureBus(bus).getPeer();
	// Number number = fullv.getPortConstantValue(pport);
	// if(number != null)
	// {
	// if(number instanceof Double)
	// {
	// long rawLongBits = Double.doubleToRawLongBits(number.doubleValue());
	// bus.setValue(Value.getConstantValue(rawLongBits));
	// call.getProcedureBus(bus).setValue(Value.getConstantValue(rawLongBits));
	// }
	// else
	// {
	// int rawIntBits = Float.floatToRawIntBits(number.floatValue());
	// bus.setValue(Value.getConstantValue(rawIntBits));
	// call.getProcedureBus(bus).setValue(Value.getConstantValue(rawIntBits));
	// }
	// }
	// }
	// }

	/**
	 * All we do here is to replace any constant valued output bus from a call
	 * with a constant of equivalent value. This allows dead component removal
	 * to be responsible for removing the call if/when possible.
	 */
	// static void makeFPConstant (Call call, FullConstantVisitor fullv)
	// {
	// // All we do here is to replace any constant valued output bus
	// // from a call with a constant of equivalent value. This
	// // allows dead component removal to be responsible for
	// // removing the call if/when possible.
	// for (Iterator exitIter = call.getExits().iterator(); exitIter.hasNext();)
	// {
	// Exit exit = (Exit)exitIter.next();
	// for (Iterator busIter = exit.getDataBuses().iterator();
	// busIter.hasNext();)
	// {
	// Bus bus = (Bus)busIter.next();
	// if (bus.getValue().isConstant())
	// {
	// Constant cValue = null;
	// if(bus.getValue().getSize() == 64)
	// {
	// Number dfp = new
	// Double(Double.longBitsToDouble(bus.getValue().getValueMask()));
	// cValue = new
	// SimpleConstant(Double.doubleToRawLongBits(dfp.doubleValue()), 64);
	// }
	// else
	// {
	// Number ffp = new
	// Float(Float.intBitsToFloat((int)bus.getValue().getValueMask()));
	// cValue = new SimpleConstant(Float.floatToRawIntBits(ffp.floatValue()),
	// 32);
	// }
	// cValue.setFloat(true);

	// // Put a constant in the LIM at the same level as the call
	// // and disconnect the bus.
	// Map portCorrelate = new HashMap();
	// portCorrelate.put(call.getClockPort(), cValue.getClockPort());
	// portCorrelate.put(call.getResetPort(), cValue.getResetPort());
	// portCorrelate.put(call.getGoPort(), cValue.getGoPort());

	// fullv.replaceConnections(portCorrelate,
	// Collections.singletonMap(bus, cValue.getValueBus()),
	// Collections.singletonMap(exit, cValue.getExit(Exit.DONE)));
	// Component owner = call.getOwner();
	// if (owner instanceof Block)
	// {
	// Block block = (Block)owner;
	// int index = block.getSequence().indexOf(call);
	// block.insertComponent(cValue, index);
	// }
	// else
	// {
	// ((Module)owner).addComponent(cValue);
	// }
	// }
	// }
	// }
	// }

	// static boolean isFloat (InBuf inBuf)
	// {
	// boolean flag = false;
	// Set logicalBuses = new HashSet();
	// for(Iterator iter = inBuf.getDataBuses().iterator(); iter.hasNext();)
	// {
	// Bus bus = (Bus)iter.next();
	// if(bus.getPeer() != null)
	// {
	// Port port = bus.getPeer();
	// if(port.getBus() == null)
	// {
	// List entries = new ArrayList(port.getOwner().getEntries());
	// for(Iterator it = entries.iterator(); it.hasNext();)
	// {
	// Entry entry = (Entry)it.next();
	// for(Iterator diter = entry.getDependencies(port).iterator();
	// diter.hasNext();)
	// {
	// Dependency dep = (Dependency)diter.next();
	// logicalBuses.add(dep.getLogicalBus());
	// }
	// }
	// }
	// else
	// {
	// logicalBuses = Collections.singleton(port.getBus());
	// }
	// }
	// }

	// for(Iterator fiter = logicalBuses.iterator(); fiter.hasNext();)
	// {
	// Bus bus = (Bus)fiter.next();
	// if(bus.isFloat()) flag = true;
	// }

	// return flag;
	// }

	// static boolean isFloat (OutBuf outBuf)
	// {
	// boolean flag = false;
	// Set logicalBuses = new HashSet();
	// for(Iterator iter = outBuf.getDataPorts().iterator(); iter.hasNext();)
	// {
	// Port port = (Port)iter.next();
	// if(port.getBus() == null)
	// {
	// List entries = new ArrayList(port.getOwner().getEntries());
	// for(Iterator it = entries.iterator(); it.hasNext();)
	// {
	// Entry entry = (Entry)it.next();
	// for(Iterator diter = entry.getDependencies(port).iterator();
	// diter.hasNext();)
	// {
	// Dependency dep = (Dependency)diter.next();
	// logicalBuses.add(dep.getLogicalBus());
	// }
	// }
	// }
	// else
	// {
	// logicalBuses = Collections.singleton(port.getBus());
	// }
	// }

	// for(Iterator fiter = logicalBuses.iterator(); fiter.hasNext();)
	// {
	// Bus bus = (Bus)fiter.next();

	// if(bus.isFloat())
	// {
	// flag = true;
	// }
	// else if(bus.getOwner().getOwner() instanceof Operation &&
	// ((Operation)bus.getOwner().getOwner()).isFloat())
	// {
	// flag = true;
	// }
	// }

	// return flag;
	// }

	static void makeFPConstant(AddOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[0] instanceof Double || consts[1] instanceof Double) {
			double sum = consts[0].doubleValue() + consts[1].doubleValue();
			fpc = SimpleConstant.getDoubleConstant(sum);
		} else {
			float sum = consts[0].floatValue() + consts[1].floatValue();
			fpc = SimpleConstant.getFloatConstant(sum);
		}

		assert fpc != null : "Error: Floating point constant not created.";
		fullv.replaceComponent(comp, fpc);
	}

	static void makeFPConstant(CastOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[0] instanceof Double) {
			fpc = SimpleConstant.getDoubleConstant(consts[0].doubleValue());
		} else {
			fpc = SimpleConstant.getFloatConstant(consts[0].floatValue());
		}

		assert fpc != null : "Error: Floating point constant not created.";
		fullv.replaceComponent(comp, fpc);
	}

	static void makeFPConstant(DivideOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[1].doubleValue() == 0.0) {
			EngineThread.getGenericJob().warn(
					"    Found divide by 0 at line "
							+ comp.getIDSourceInfo().getSourceLine()
							+ ", replacing with 0.");
			if (consts[1] instanceof Double)
				fpc = new SimpleConstant(0, 64, false);
			else
				fpc = new SimpleConstant(0, 32, false);
		} else if (consts[0] instanceof Double || consts[1] instanceof Double) {
			double div = consts[0].doubleValue() / consts[1].doubleValue();
			fpc = SimpleConstant.getDoubleConstant(div);
		} else {
			float div = consts[0].floatValue() / consts[1].floatValue();
			fpc = SimpleConstant.getFloatConstant(div);
		}

		assert fpc != null : "Error: Floating point constant not created.";
		fullv.replaceComponent(comp, fpc);
	}

	static void makeFPConstant(EqualsOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		if (consts[0].doubleValue() == consts[1].doubleValue()) {
			fullv.replaceComponent(comp, new SimpleConstant(1, 1, false));
		} else {
			fullv.replaceComponent(comp, new SimpleConstant(0, 1, false));
		}
	}

	static void makeFPConstant(GreaterThanEqualToOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		if (consts[0].doubleValue() >= consts[1].doubleValue()) {
			fullv.replaceComponent(comp, new SimpleConstant(1, 1, false));
		} else {
			fullv.replaceComponent(comp, new SimpleConstant(0, 1, false));
		}
	}

	static void makeFPConstant(GreaterThanOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		if (consts[0].doubleValue() > consts[1].doubleValue()) {
			fullv.replaceComponent(comp, new SimpleConstant(1, 1, false));
		} else {
			fullv.replaceComponent(comp, new SimpleConstant(0, 1, false));
		}
	}

	static void makeFPConstant(LessThanEqualToOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		if (consts[0].doubleValue() <= consts[1].doubleValue()) {
			fullv.replaceComponent(comp, new SimpleConstant(1, 1, false));
		} else {
			fullv.replaceComponent(comp, new SimpleConstant(0, 1, false));
		}
	}

	static void makeFPConstant(LessThanOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		if (consts[0].doubleValue() < consts[1].doubleValue()) {
			fullv.replaceComponent(comp, new SimpleConstant(1, 1, false));
		} else {
			fullv.replaceComponent(comp, new SimpleConstant(0, 1, false));
		}
	}

	static void makeFPConstant(MinusOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[0] instanceof Double) {
			double min = -consts[0].doubleValue();
			fpc = SimpleConstant.getDoubleConstant(min);
		} else {
			float min = -consts[0].floatValue();
			fpc = SimpleConstant.getFloatConstant(min);
		}

		assert fpc != null : "Error: Floating point constant not created.";
		fullv.replaceComponent(comp, fpc);
	}

	static void makeFPConstant(ModuloOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[0].doubleValue() == 0.0) {
			EngineThread.getGenericJob().warn(
					"    Found devide by 0 at line "
							+ comp.getIDSourceInfo().getSourceLine()
							+ ", replacing with 0.");
			if (consts[0] instanceof Double)
				fpc = new SimpleConstant(0, 64, false);
			else
				fpc = new SimpleConstant(0, 32, false);
		} else if (consts[0] instanceof Double || consts[1] instanceof Double) {
			double rem = consts[0].doubleValue() % consts[1].doubleValue();
			fpc = SimpleConstant.getDoubleConstant(rem);
		} else {
			float rem = consts[0].floatValue() % consts[1].floatValue();
			fpc = SimpleConstant.getFloatConstant(rem);
		}

		assert fpc != null : "Error: Floating point constant not created.";
		fullv.replaceComponent(comp, fpc);
	}

	static void makeFPConstant(MultiplyOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[0] instanceof Double || consts[1] instanceof Double) {
			double mul = consts[0].doubleValue() * consts[1].doubleValue();
			fpc = SimpleConstant.getDoubleConstant(mul);
		} else {
			float mul = consts[0].floatValue() * consts[1].floatValue();
			fpc = SimpleConstant.getFloatConstant(mul);
		}

		assert fpc != null : "Error: Floating point constant not created.";
		fullv.replaceComponent(comp, fpc);
	}

	static void makeFPConstant(NoOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[0] instanceof Double) {
			fpc = new SimpleConstant(Double.doubleToRawLongBits(consts[0]
					.doubleValue()), comp.getResultBus().getValue().getSize(),
					false);
		} else {
			fpc = new SimpleConstant(Float.floatToRawIntBits(consts[0]
					.floatValue()), comp.getResultBus().getValue().getSize(),
					false);
		}

		assert fpc != null : "Error: Floating point constant not created.";
		// fpc.setFloat(true);
		fpc.getValueBus().setFloat(true);
		fullv.replaceComponent(comp, fpc);
	}

	static void makeFPConstant(NotEqualsOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		if (consts[0].doubleValue() != consts[1].doubleValue()) {
			fullv.replaceComponent(comp, new SimpleConstant(1, 1, false));
		} else {
			fullv.replaceComponent(comp, new SimpleConstant(0, 1, false));
		}
	}

	static void makeFPConstant(NumericPromotionOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[0] instanceof Double) {
			fpc = SimpleConstant.getDoubleConstant(consts[0].doubleValue());
		} else {
			fpc = SimpleConstant.getFloatConstant(consts[0].floatValue());
		}

		assert fpc != null : "Error: Floating point constant not created.";
		fullv.replaceComponent(comp, fpc);
	}

	static void makeFPConstant(SubtractOp comp, Number[] consts,
			FullConstantVisitor fullv) {
		Constant fpc;
		if (consts[0] instanceof Double || consts[1] instanceof Double) {
			double sub = consts[0].doubleValue() - consts[1].doubleValue();
			fpc = SimpleConstant.getDoubleConstant(sub);
		} else {
			float sub = consts[0].floatValue() - consts[1].floatValue();
			fpc = SimpleConstant.getFloatConstant(sub);
		}

		assert fpc != null : "Error: Floating point constant not created.";
		fullv.replaceComponent(comp, fpc);
	}
}
