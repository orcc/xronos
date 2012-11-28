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
package org.xronos.openforge.lim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A SharedProcedureCall invokes a {@link SharedProcedure}.
 * 
 * @author Stephen Edwards
 * @version $Id: SharedProcedureCall.java 88 2006-01-11 22:39:52Z imiller $
 */
public class SharedProcedureCall extends Access {

	/** Map of call's Port to referent's body Port */
	private Map<Port, Port> portMap;

	/** Map of call's Exit to referent's body Exit */
	private Map<Exit, Exit> exitMap;

	/** Map of call's Bus to referent's body Bus */
	private Map<Bus, Bus> busMap;

	/**
	 * Constructs a new SharedProcedureCall.
	 */
	SharedProcedureCall(SharedProcedure procedure) {
		super(procedure, procedure.getProcedure().getBody().getDataPorts()
				.size(), false);
		Component body = procedure.getProcedure().getBody();

		generateMaps(body);
	}

	@Override
	public boolean isSequencingPoint() {
		throw new UnsupportedOperationException(
				"Shared procedure unsupported.  Call to unsupported method");
	}

	/**
	 * Calls the super then removes the port from the port mapping.
	 */
	@Override
	public boolean removeDataPort(Port port) {
		boolean ret = super.removeDataPort(port);
		portMap.remove(port);
		return ret;
	}

	/**
	 * Calls the super then removes the bus from the bus mapping.
	 */
	@Override
	public boolean removeDataBus(Bus bus) {
		boolean ret = super.removeDataBus(bus);
		busMap.remove(bus);
		return ret;
	}

	private void generateMaps(Component body) {
		/*
		 * Map ports to procedure body's ports.
		 */
		portMap = new HashMap<Port, Port>(body.getPorts().size());
		portMap.put(getClockPort(), body.getClockPort());
		portMap.put(getResetPort(), body.getResetPort());
		portMap.put(getGoPort(), body.getGoPort());
		for (Iterator<Port> iter = getDataPorts().iterator(), bodyIter = body
				.getDataPorts().iterator(); iter.hasNext();) {
			portMap.put(iter.next(), bodyIter.next());
		}

		/*
		 * Map exits to procedure body's exits.
		 */
		exitMap = new HashMap<Exit, Exit>(body.getExits().size());
		busMap = new HashMap<Bus, Bus>(body.getBuses().size());
		for (Iterator<Exit> exitIter = body.getExits().iterator(); exitIter
				.hasNext();) {
			Exit bodyExit = exitIter.next();
			Exit exit = getExit(Exit.DONE);
			if (exit == null) {
				exit = makeExit(bodyExit.getDataBuses().size());
			}
			exitMap.put(exit, bodyExit);

			/*
			 * Map buses to procedure body's buses.
			 */
			for (Iterator<Bus> bodyIter = bodyExit.getDataBuses().iterator(), iter = exit
					.getDataBuses().iterator(); bodyIter.hasNext();) {
				busMap.put(iter.next(), bodyIter.next());
			}

			// if (bodyExit.isMain())
			// {
			// setMainExit(exit);
			// }
		}
	}

	/**
	 * Resets the target {@link Referent} and regenerates the port, exit, and
	 * bus correlation maps.
	 * 
	 * @param ref
	 *            the new target Referent
	 */
	@Override
	public void setReferent(Referent ref) {
		super.setRef(ref);
		assert ref instanceof Procedure : "Referent of shared procedure call is NOT a procedure!";
		generateMaps(((Procedure) ref).getBody());
	}

	/**
	 * Gets the SharedProcedure invoked by this Call.
	 */
	public SharedProcedure getSharedProcedure() {
		return (SharedProcedure) getResource();
	}

	/**
	 * Gets the procedure body port that corresponds to a given call port.
	 * 
	 * @param port
	 *            a port of this call
	 * @return the corresponding procedure's port
	 */
	public Port getProcedurePort(Port port) {
		if (port.getOwner() != this) {
			throw new IllegalArgumentException("unknown port");
		}
		return portMap.get(port);
	}

	/**
	 * Gets the procedure body exit that corresponds to a given call exit.
	 * 
	 * @param exit
	 *            an exit of this call
	 * @return the corresponding procedure's exit
	 */
	public Exit getProcedureExit(Exit exit) {
		if (exit.getOwner() != this) {
			throw new IllegalArgumentException("unknown exit");
		}
		return exitMap.get(exit);
	}

	/**
	 * Gets the procedure body bus that corresponds to a given call bus.
	 * 
	 * @param bus
	 *            a bus of this call
	 * @return the corresponding procedure's bus
	 */
	public Bus getProcedureBus(Bus bus) {
		if (bus.getOwner().getOwner() != this) {
			throw new IllegalArgumentException("unknown bus");
		}
		return busMap.get(bus);
	}

	/**
	 * Gets the end-to-end Latency of the referent.
	 */
	@Override
	public Latency getLatency() {
		/*
		 * tbd.
		 */
		return Latency.ZERO;
	}

	/**
	 * Gets the pipelined input Latency of the referent.
	 */
	public Latency getPipelineLatency() {
		/*
		 * tbd.
		 */
		return Latency.ONE;
	}

	/**
	 * Creates a copy of this SharedProcedureCall which points to the
	 * <b>same</b> procedure as this node.
	 * 
	 * @return a SharedProcedureCall
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		SharedProcedureCall clone = (SharedProcedureCall) super.clone();

		clone.portMap = new HashMap<Port, Port>();
		Iterator<Port> origPortIter = getPorts().iterator();
		Iterator<Port> clonePortIter = clone.getPorts().iterator();
		while (origPortIter.hasNext()) {
			// Map the clone's ports to the _same_ procedure ports as
			// the original
			Port bodyPort = portMap.get(origPortIter.next());
			clone.portMap.put(clonePortIter.next(), bodyPort);
		}

		clone.exitMap = new HashMap<Exit, Exit>();
		clone.busMap = new HashMap<Bus, Bus>();
		Iterator<Exit> origExitIter = getExits().iterator();
		Iterator<Exit> cloneExitIter = clone.getExits().iterator();
		while (origExitIter.hasNext()) {
			Exit origExit = origExitIter.next();
			Exit cloneExit = cloneExitIter.next();
			Exit bodyExit = exitMap.get(origExit);
			clone.exitMap.put(cloneExit, bodyExit);

			Iterator<Bus> origBusIter = origExit.getDataBuses().iterator();
			Iterator<Bus> cloneBusIter = cloneExit.getDataBuses().iterator();
			while (origBusIter.hasNext()) {
				Bus bodyBus = busMap.get(origBusIter.next());
				clone.busMap.put(cloneBusIter.next(), bodyBus);
			}
		}

		return clone;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Propagate the Procedures data bus {@link Value} to the corresponding Bus
	 * on this Call.
	 */
	@Override
	public boolean pushValuesForward() {
		// All we need to do here is update the output bus(es) with
		// the outputs from the Procedure.
		boolean mod = false;

		for (Exit exit : getExits()) {
			for (Bus callBus : exit.getDataBuses()) {
				Bus procBus = getProcedureBus(callBus);
				Value procValue = procBus.getValue();
				mod |= callBus.pushValueForward(procValue);
			}
		}

		return mod;
	}

	/**
	 * Back propagate procedure Port {@link Value Values} to the corresponding
	 * Port on the Call.
	 */
	@Override
	public boolean pushValuesBackward() {
		// All we need to do here is back propagate the procedure Port
		// values to the Call's ports.
		boolean mod = false;

		for (Port callPort : getDataPorts()) {
			Port procPort = getProcedurePort(callPort);
			mod |= callPort.pushValueBackward(procPort.getValue());
		}

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

}
