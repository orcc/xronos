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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.openforge.forge.api.pin.GoPin;
import net.sf.openforge.forge.api.pin.SuspendPin;
import net.sf.openforge.lim.ApiCallIdentifier.Tag;
import net.sf.openforge.util.naming.ID;

/**
 * A Call is a {@link Reference} to a {@link Procedure}. It represents a call by
 * name, and it is expected that the HDL compilation will resolve the reference.
 * 
 * @author Stephen Edwards
 * @version $Id: Call.java 284 2006-08-15 15:43:34Z imiller $
 */
public class Call extends Reference {

	/** Map of call's Port to referent's body Port */
	private Map<Port, Port> portMap;

	/** Map of call's Exit to referent's body Exit */
	private Map<Exit, Exit> exitMap;

	/** Map of call's Bus to referent's body Bus */
	private Map<Bus, Bus> busMap;

	/*
	 * Map of ApiCallIdentifiers.Tag to ApiCallIdentifier. NOTE: there should be
	 * only one mapping at this point 3/18/03. -ysyu
	 */
	private Map<ApiCallIdentifier.Tag, ApiCallIdentifier> apiIdentifiers = new LinkedHashMap<ApiCallIdentifier.Tag, ApiCallIdentifier>(
			1);

	/* throughput go spacing of this call */
	private int throughput_local = 0;

	/**
	 * Constructs a new Call for a given Procedure.
	 */
	Call(Procedure procedure) {
		super(procedure, (procedure.getBody().getThisPort() == null ? procedure
				.getBody().getDataPorts().size() : procedure.getBody()
				.getDataPorts().size() - 1));

		Block body = procedure.getBody();
		if (body.getThisPort() != null) {
			makeThisPort();
		}
		generateMaps(body);

		/*
		 * Add a return value Bus, if necessary. This won't appear in the
		 * Procedure until the JLIMDataLinker crunches it. Fill in the mapping
		 * at that time.
		 */
		Exit callExit = getExit(Exit.DONE);
		if (procedure.hasReturnValue()) {
			final Exit procExit = body.getExit(Exit.RETURN);
			final Bus returnBus = callExit.getDataBuses().get(0);
			addBusMap(returnBus, procExit.getDataBuses().get(0));
		}

		// Set up some default naming
		setIDSourceInfo(getProcedure().getIDSourceInfo());
		setIDLogical(ID.showLogical(getProcedure()));
	}

	Call() {
	}

	/**
	 * Notifies this call to update the mappings between its {@link Port Ports}
	 * and {@link Bus Buses} and those of its {@link Procedure}.
	 */
	public void updateNotify() {
		final Block body = getProcedure().getBody();
		if (body != null) {
			generateMaps(body);
		}
	}

	/**
	 * Populates the 3 maps contained in this class with correlations between
	 * procedure body and this call.
	 * 
	 * @param body
	 *            a value of type 'Block'
	 */
	private void generateMaps(Block body) {
		// Map ports to procedure body's ports
		portMap = new HashMap<Port, Port>(body.getPorts().size());
		addPortMap(getClockPort(), body.getClockPort());
		addPortMap(getResetPort(), body.getResetPort());
		addPortMap(getGoPort(), body.getGoPort());

		for (Iterator<Port> iter = getDataPorts().iterator(), bodyIter = body
				.getDataPorts().iterator(); iter.hasNext();) {
			Port dp = iter.next();
			Port bp = bodyIter.next();
			addPortMap(dp, bp);
		}

		// Map exits to procedure body's exits
		exitMap = new HashMap<Exit, Exit>(body.getExits().size());
		busMap = new HashMap<Bus, Bus>(body.getBuses().size());
		for (Iterator<Exit> exitIter = body.getExits().iterator(); exitIter
				.hasNext();) {
			Exit bodyExit = exitIter.next();
			Exit.Tag tag = bodyExit.getTag();
			// First see if the exit already exists
			// (in the case where we are re-setting the referent), if
			// it doesn't then create a new one.
			Exit exit = getExit(convertProcedureType(tag.getType()),
					tag.getLabel());
			if (exit == null) {
				exit = makeExit(bodyExit.getDataBuses().size(),
						convertProcedureType(tag.getType()), tag.getLabel());
			}

			assert exit.getDataBuses().size() == bodyExit.getDataBuses().size() : "Number of data buses on exit doesn't match procedure";
			// exitMap.put(exit, bodyExit);
			addExitMap(exit, bodyExit);

			// Map buses to procedure body's buses.
			for (Iterator<Bus> bodyIter = bodyExit.getDataBuses().iterator(), iter = exit
					.getDataBuses().iterator(); bodyIter.hasNext();) {
				Bus db = iter.next();
				Bus bb = bodyIter.next();
				addBusMap(db, bb);
			}
			addBusMap(exit.getDoneBus(), bodyExit.getDoneBus());
		}
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	@Override
	public Port makeThisPort() {
		assert getProcedure().getBody().getThisPort() != null : "Cannot make a 'this' port on a call to a procedure without a 'this'";
		// Port bodyPort = getProcedure().getBody().getThisPort();
		Port port = super.makeThisPort();
		return port;
	}

	/**
	 * Resets the target {@link Referent} and regenerates the port, exit, and
	 * bus correlation maps.
	 * 
	 * @param ref
	 *            the new target Referent
	 * @throws ClassCastException
	 *             if the given referent is not an instance of {@link Procedure}
	 */
	@Override
	public void setReferent(Referent ref) {
		super.setRef(ref);
		generateMaps(((Procedure) ref).getBody());
	}

	/**
	 * Gets the resources accessed by or within this component.
	 * 
	 * @return a collection of {@link Resource}
	 */
	@Override
	public Collection getAccessedResources() {
		return getProcedure().getBody().getAccessedResources();
	}

	/**
	 * Gets the Procedure invoked by this Call.
	 */
	public Procedure getProcedure() {
		return (Procedure) getReferent();
	}

	/**
	 * Sets the {@link Procedure} invoked by this call.
	 */
	public void setProcedure(Procedure procedure) {
		setReferent(procedure);
	}

	/**
	 * Tests whether this component requires a connection to its <em>go</em>
	 * {@link Port} in order to commence processing.
	 */
	@Override
	public boolean consumesGo() {
		return getProcedure().getBody().consumesGo();
	}

	/**
	 * Tests whether this component produces a signal on the done {@link Bus} of
	 * each of its {@link Exit Exits}.
	 */
	@Override
	public boolean producesDone() {
		return getProcedure().getBody().producesDone();
	}

	/**
	 * Tests whether this component produces a signal on the done {@link Bus} of
	 * each of its {@link Exit Exits}.
	 * 
	 * @see Component#isDoneSynchronous()
	 * @return true if called procedure's body isdonesynchronous.
	 */
	@Override
	public boolean isDoneSynchronous() {
		return getProcedure().getBody().isDoneSynchronous();
	}

	/**
	 * Tests whether this component requires a connection to its clock
	 * {@link Port}.
	 */
	@Override
	public boolean consumesClock() {
		return getProcedure().getBody().consumesClock();
	}

	/**
	 * Tests whether this component requires a connection to its reset
	 * {@link Port}. By default, returns the value of
	 * {@link Component#consumesClock()}.
	 */
	@Override
	public boolean consumesReset() {
		return getProcedure().getBody().consumesReset();
	}

	/**
	 * Returns the name to use for the suspend. This defers to
	 * {@link Procedure#getSuspendName()}.
	 * 
	 * @return the clock name
	 * @deprecated
	 */
	@Deprecated
	public String getSuspendName() {
		return getProcedure().getSuspendName();
	}

	/**
	 * Returns the name to use for the go. This defers to
	 * {@link Procedure#getGoName()}.
	 * 
	 * @return the clock name
	 * @deprecated
	 */
	@Deprecated
	public String getGoName() {
		return getProcedure().getGoName();
	}

	/**
	 * Returns the api pin to use for the suspend. This defers to
	 * {@link Procedure#getSuspendPin()}.
	 * 
	 * @return the suspend api pin
	 */
	public SuspendPin getSuspendPin() {
		return getProcedure().getSuspendPin();
	}

	/**
	 * Returns the name to use for the go. This defers to
	 * {@link Procedure#getGoPin()}.
	 * 
	 * @return the api go pin
	 */
	public GoPin getGoPin() {
		return getProcedure().getGoPin();
	}

	/**
	 * Tests whether or not the timing of this component can be balanced during
	 * scheduling. That is, can all of the execution paths through the component
	 * be made to complete in the same number of clocks. Note that this property
	 * is based only upon the type of this component and any components that it
	 * may contain.
	 * 
	 * @return true if the called procedures body is balanceable.
	 */
	@Override
	public boolean isBalanceable() {
		return getProcedure().getBody().isBalanceable();
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

	/**
	 * Associates a Procedure side Port with the related Port on the Call.
	 * 
	 * @param local
	 *            the call-side Port
	 * @param proc
	 *            the related proccedure-side Port
	 */
	public void setProcedurePort(Port local, Port proc) {
		addPortMap(local, proc);
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
	 * Returns the Port on this Call object that corresponds to the given port
	 * from the called procedure.
	 * 
	 * @param port
	 *            a 'Port' on this Call.
	 * @return a 'Port' from the called procedure
	 */
	public Port getPortFromProcedurePort(Port port) {
		if (port.getOwner() != ((Procedure) getReferent()).getBody()) {
			throw new IllegalArgumentException("unknown procedure port");
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

	public void setProcedureReturnBus(Bus returnBus) {
		Bus localBus = getExit(Exit.DONE).getDataBuses().get(0);
		addBusMap(localBus, returnBus);
		// propagate the value across the exit. IDM. Let constant
		// prop handle this.
		// localBus.setValue(returnBus.getValue());
	}

	/**
	 * Associates a Procedure side Bus with the related Bus on the Call.
	 * 
	 * @param local
	 *            the call-side bus
	 * @param proc
	 *            the related proccedure-side bus
	 */
	public void setProcedureBus(Bus local, Bus proc) {
		addBusMap(local, proc);
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
	 * Returns the Bus on this Call object that corresponds to the given bus
	 * from the called procedure.
	 * 
	 * @param bus
	 *            a 'Bus' on this Call.
	 * @return a 'Bus' from the called procedure
	 */
	public Bus getBusFromProcedureBus(Bus bus) {
		if (bus.getOwner().getOwner() != ((Procedure) getReferent()).getBody()) {
			throw new IllegalArgumentException("unknown procedure bus");
		}

		return busMap.get(bus);
	}

	/**
	 * Gets the end-to-end Latency of the referent.
	 */
	@Override
	public Latency getLatency() {
		// return getProcedure().getLatency(getMainExit());
		return getProcedure().getLatency(getExit(Exit.DONE));
	}

	/**
	 * Ensures that a {@link Procedure} {@link Exit} type of {@link Exit.RETURN}
	 * maps to a {@link Call} {@link Exit} type of {@link Exit.DONE}. All other
	 * types are unchanged.
	 * 
	 * @param procedureType
	 *            the exit type from the procedure
	 * @return the corresponding exit type for the call
	 */
	private static Exit.Type convertProcedureType(Exit.Type procedureType) {
		assert procedureType != Exit.DONE : "Procedure has Exit type: "
				+ procedureType;
		return procedureType == Exit.RETURN ? Exit.DONE : procedureType;
	}

	@Override
	public String toString() {
		String ret = super.toString();
		// for (Iterator portIter = getPorts().iterator(); portIter.hasNext();)
		// {
		// Port port = (Port)portIter.next();
		// if (port == getClockPort())
		// ret += " ck:" + port;
		// else if (port == getResetPort())
		// ret += " rs:" + port;
		// else if (port == getGoPort())
		// ret += " go:" + port;
		// else if (port == getThisPort())
		// ret += " th:" + port;
		// else
		// ret += " dp:" + port;
		// }
		return ret;
	}

	@Override
	public String cpDebug(boolean verbose) {
		String ret = toString();
		for (Port p : getDataPorts()) {
			Bus procBus = getProcedurePort(p).getPeer();
			if (verbose)
				ret += p.getValue() == null ? " n" : " "
						+ p.getValue().bitSourceDebug() + "("
						+ procBus.getValue().bitSourceDebug() + ")";
			else
				ret += p.getValue() == null ? " n" : " "
						+ p.getValue().getSize() + "("
						+ procBus.getValue().getSize() + ")";
		}
		for (Exit exit : getExits()) {
			ret += " :";
			for (Bus bus : exit.getDataBuses()) {
				Bus procBus = getProcedureBus(bus);
				if (verbose)
					ret += bus.getValue() == null ? " n" : " "
							+ bus.getValue().bitSourceDebug() + "("
							+ procBus.getValue().bitSourceDebug() + ")";
				else
					ret += bus.getValue() == null ? " n" : " "
							+ bus.getValue().getSize() + "("
							+ procBus.getValue().getSize() + ")";
			}
		}
		return ret;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * This method does nothing for a Call. Since the work must be done
	 * recursively be descending into the called {@link Procedure}, the activity
	 * is directed by the constant propagation visitor, which calls the methods
	 * {@link #copyInputValuesToProcedure()} and
	 * {@link #copyOutputValuesFromProcedure()}.
	 * 
	 * @return false
	 */
	@Override
	public boolean pushValuesForward() {
		return false;
	}

	/**
	 * Called by the constant propagation visitor as the first step in forward
	 * propagation of {@link Value Values}. For each {@link Port} {@link Value}
	 * of this Call, every global {@link Bit} is set in the corresponding
	 * {@link Procedure} {@link Port} {@link Value}, if the destination
	 * {@link Bit} is not a don't-care.
	 * <P>
	 * No propagation occurs for Call {@link Port Ports} that do not have a
	 * {@link Value}.
	 * 
	 * @return true if any of the procedure's bits were modified, false
	 *         otherwise
	 */
	public boolean copyInputValuesToProcedure() {
		boolean isModified = false;
		for (Port callPort : getPorts()) {
			final Value callValue = callPort.getValue();
			final Port procPort = getProcedurePort(callPort);

			Value procValue = procPort.getValue();
			if (procValue == null) {
				procPort.setSize(callValue.getSize(), callValue.isSigned());
				procValue = procPort.getValue();
			}

			for (int i = 0; i < callValue.getSize(); i++) {
				final Bit callBit = callValue.getBit(i);
				final Bit procBit = procValue.getBit(i);
				if (callBit.isGlobal() && procBit.isCare()
						&& !callValue.bitEquals(i, procValue, i)) {
					procValue.setBit(i, callBit);
					isModified = true;
				}
			}
		}
		return isModified;
	}

	/**
	 * Called by the constant propagation visitor as the last step in forward
	 * propagation of {@link Value Values} through a Call.
	 * <P>
	 * For each care {@link Bit} in the {@link Value} of every call {@link Bus},
	 * copy the {@link Bit} from the {@link Procedure} if it is constant; else
	 * use the local {@link Bit} from the call's {@link Bus} in that position.
	 * <P>
	 * No propagation occurs from {@link Procedure} {@link Bus Buses} which do
	 * not have a {@link Value}.
	 * 
	 * @return true if any of this call's bits were modified, false otherwise
	 */
	public boolean copyOutputValuesFromProcedure() {
		boolean isModified = false;
		for (Bus callBus : getBuses()) {
			final Bus procBus = getProcedureBus(callBus);
			final Value procValue = procBus.getValue();

			Value callValue = callBus.getValue();
			if (callValue == null) {
				callBus.setSize(procValue.getSize(), procValue.isSigned());
				callValue = callBus.getValue();
			}

			for (int i = 0; i < procValue.getSize(); i++) {
				final Bit procBit = procValue.getBit(i);
				final Bit callBit = callValue.getBit(i);
				if (callBit.isCare()) {
					if (procBit.isConstant()) {
						callValue.setBit(i, procBit);
					} else {
						// if (procBit.isConstant() || !procBit.isCare())
						// {
						// callValue.setBit(i, procBit);
						// }
						// else
						// {
						// callValue.setBit(i, callBus.getLocalBit(i));
						// }
						callValue.setBit(i, callBus.getLocalBit(i));
					}
				}
			}

			if (procValue.getCompactedSize() != callValue.getCompactedSize()) {
				final Bit msb = callValue
						.getBit(procValue.getCompactedSize() - 1);
				for (int i = procValue.getCompactedSize(); i < callValue
						.getSize(); i++) {
					callValue.setBit(i, msb);
				}
			}
		}
		return isModified;
	}

	/**
	 * This method does nothing for a Call. Since the work must be done
	 * recursively be descending into the called {@link Procedure}, the activity
	 * is directed by the constant propagation visitor, which calls the methods
	 * {@link #copyOutputValuesToProcedure()} and
	 * {@link #copyInputValuesFromProcedure()}.
	 * 
	 * @return false
	 */
	@Override
	public boolean pushValuesBackward() {
		return false;
	}

	/**
	 * Called by the constant propagation visitor as the last step in backward
	 * propagation of {@link Value Values}.
	 * <P>
	 * Each don't-care {@link Bit} in the {@link Value} of every procedure
	 * {@link Port} is copied to the {@link Value} of the corresponding call
	 * {@link Port}.
	 * <P>
	 * No propagation occurs from {@link Procedure} {@link Port Ports} which do
	 * not have a {@link Value}.
	 * 
	 * @return true if any of this call's bits were modified, false otherwise
	 */
	public boolean copyInputValuesFromProcedure() {
		boolean isModified = false;
		for (Port callPort : getPorts()) {
			final Port procPort = getProcedurePort(callPort);
			final Value procValue = procPort.getValue();
			final Value callValue = callPort.getValue();
			for (int i = 0; i < procValue.getSize(); i++) {
				final Bit callBit = callValue.getBit(i);
				final Bit procBit = procValue.getBit(i);
				if (!procBit.isCare() && callBit.isCare()) {
					callValue.setBit(i, Bit.DONT_CARE);
					isModified = true;
				}
			}
		}
		return isModified;
	}

	/**
	 * Called by the constant propagation visitor as the first step in backward
	 * propagation of {@link Value Values} through a Call.
	 * <P>
	 * Each don't-care {@link Bit} in the {@link Value} of every Call
	 * {@link Bus} is copied to the {@link Value} of the corresponding procedure
	 * {@link Bus}.
	 * <P>
	 * No propagation occurs from {@link Call} {@link Port Ports} which do not
	 * have a {@link Value}.
	 * 
	 * @return true if any of the called procedure's bits were modified, false
	 *         otherwise
	 */
	public boolean copyOutputValuesToProcedure() {
		boolean isModified = false;
		for (Bus callBus : getBuses()) {
			final Value callValue = callBus.getValue();
			final Bus procBus = getProcedureBus(callBus);

			final Value procValue = procBus.getValue();
			for (int i = 0; i < callValue.getSize(); i++) {
				final Bit callBit = callValue.getBit(i);
				final Bit procBit = procValue.getBit(i);
				if (!callBit.isCare() && procBit.isCare()) {
					procValue.setBit(i, Bit.DONT_CARE);
					isModified = true;
				}
			}
		}
		return isModified;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	/**
	 * Creates a copy of this Call which points to the <b>same</b> procedure as
	 * this node.
	 * 
	 * @return a Call
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		final Procedure procedureClone = (Procedure) getProcedure().clone();
		final Call clone = procedureClone.makeCall();

		copyComponentAttributes(clone);

		/*
		 * Copy ApiCallIdentifier attributes
		 */
		clone.apiIdentifiers = new LinkedHashMap<ApiCallIdentifier.Tag, ApiCallIdentifier>();
		for (Map.Entry<ApiCallIdentifier.Tag, ApiCallIdentifier> mapEntry : apiIdentifiers
				.entrySet()) {
			ApiCallIdentifier api = mapEntry.getValue();
			cloneApiIdentifier(clone, api);
		}

		return clone;
	}

	/**
	 * Copies this ApiCallIdentifier to the clone
	 * 
	 * @param clone
	 *            the clone of this call
	 * @param api
	 *            the ApiCallIdentifier to be copied to the clone
	 */
	protected void cloneApiIdentifier(Call clone, ApiCallIdentifier api) {
		final ApiCallIdentifier cloneApi = clone.makeApiIdentifier(api
				.getName());
		cloneApi.copyAttributes(api);
	}

	private void addPortMap(Port p1, Port p2) {
		portMap.put(p1, p2);
		portMap.put(p2, p1);
	}

	private void addBusMap(Bus b1, Bus b2) {
		busMap.put(b1, b2);
		busMap.put(b2, b1);
	}

	private void addExitMap(Exit e1, Exit e2) {
		exitMap.put(e1, e2);
		exitMap.put(e2, e1);
	}

	@Override
	public String show() {
		String ret = super.show();
		ret = ret + "PortMap: " + portMap;
		ret = ret + "BusMap: " + busMap;
		ret = ret + "ExitMap: " + exitMap;
		return ret;
	}

	/**
	 * Gets the {@link ApiCallIdentifier} with a give
	 * {@link ApiCallIdentifier.Tag Tag}.
	 * 
	 * @param tag
	 *            an api call identifier tag
	 * @return the specified api call identifer, or null if not found.
	 */
	public ApiCallIdentifier getApiIdentifier(ApiCallIdentifier.Tag tag) {
		return apiIdentifiers.get(tag);
	}

	/**
	 * Gets the {@link ApiCallIdentifier} whose {@link ApiCallIdentifier.Tag
	 * Tag} has a specified {@link ApiCallIdentifer.Type Type} and name.
	 * 
	 * @param type
	 *            the api call identifier type
	 * @param name
	 *            the api call identifier name
	 * @return the api call identifier, or null if not found
	 */
	public ApiCallIdentifier getApiIdentifier(ApiCallIdentifier.Type type,
			String name) {
		return getApiIdentifier(ApiCallIdentifier.getTag(type, name));
	}

	/**
	 * Creates an ApiCallIdentifier
	 * 
	 * @param api_name
	 *            api method name
	 * @return an ApiCallIdentifier for this Call
	 */
	public ApiCallIdentifier makeApiIdentifier(String api_name) {
		ApiCallIdentifier aci = new ApiCallIdentifier(this, api_name);
		apiIdentifiers.put(aci.getTag(), aci);
		return aci;
	}

	/**
	 * Tests if this Call is an forged api call.
	 * 
	 * @return true if this Call is a forged api call
	 */
	public boolean isForgeableAPICall() {
		return apiIdentifiers.isEmpty();
	}

	/**
	 * @return a mapping of ApiCallIdentifiers.Tag => ApiCallIdentifier.
	 */
	public Map<Tag, ApiCallIdentifier> getApiIdentifiers() {
		return apiIdentifiers;
	}

	/**
	 * Set the throughput spacing for this call
	 * 
	 * @param tpl
	 *            throughput spacing
	 */
	public void setThroughputLocal(int tpl) {
		throughput_local = tpl;
	}

	/**
	 * Get the throughput spacing for this call
	 * 
	 * @return throughput spacing
	 */
	public int getThroughputLocal() {
		return throughput_local;
	}
}
