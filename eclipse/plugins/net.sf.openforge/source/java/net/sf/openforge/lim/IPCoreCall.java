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

import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A Call is a {@link Reference} to a {@link Procedure}. It represents a call by
 * name, and it is expected that the HDL compilation will resolve the reference.
 * 
 * @author ysyu
 * @version $Id: IPCoreCall.java 2 2005-06-09 20:00:48Z imiller $
 */
public class IPCoreCall extends Call {
	private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

	private Writer hdlWriter = null;

	/** Map of call's Port to referent's body Port */
	private Map portMap;

	/** Map of call's Exit to referent's body Exit */
	private Map exitMap;

	/** Map of call's Bus to referent's body Bus */
	private Map busMap;

	private List noConnectPorts = new LinkedList();

	public IPCoreCall(Writer writer) {
		super();
		this.hdlWriter = writer;
	}

	public void accept(Visitor vis) {
		vis.visit(this);
	}

	public void addNoConnectPort(Port port) {
		noConnectPorts.add(port);
	}

	public List getNoConnectPorts() {
		return noConnectPorts;
	}

	public boolean hasWriter() {
		if (hdlWriter == null) {
			return false;
		} else {
			return true;
		}
	}

	public Writer getHDLWriter() {
		return this.hdlWriter;
	}

	public Port getProcedurePort(Port port) {
		if (port.getOwner() != this) {
			throw new IllegalArgumentException("unknown port");
		}
		return (Port) portMap.get(port);
	}

	public Bus getProcedureBus(Bus bus) {
		if (bus.getOwner().getOwner() != this) {
			throw new IllegalArgumentException("unknown bus");
		}
		return (Bus) busMap.get(bus);
	}

	public void setHDLWriter(Writer writer) {
		this.hdlWriter = writer;
	}

	public void setProcedure(Procedure proc) {
		setReferent(proc);
	}

	public void setReferent(Referent ref) {
		setRef(ref);
		generateMaps(((Procedure) ref).getBody());
	}

	private void generateMaps(Component body) {
		// Map ports to procedure body's ports
		portMap = new HashMap(body.getPorts().size());
		addPortMap(getClockPort(), body.getClockPort());
		addPortMap(getResetPort(), body.getResetPort());
		addPortMap(getGoPort(), body.getGoPort());

		for (Iterator iter = getDataPorts().iterator(), bodyIter = body
				.getDataPorts().iterator(); iter.hasNext();) {
			Port dp = (Port) iter.next();
			Port bp = (Port) bodyIter.next();
			addPortMap(dp, bp);
		}

		// Map exits to procedure body's exits
		exitMap = new HashMap(body.getExits().size());
		busMap = new HashMap(body.getBuses().size());
		for (Iterator exitIter = body.getExits().iterator(); exitIter.hasNext();) {
			Exit bodyExit = (Exit) exitIter.next();
			Exit.Tag tag = bodyExit.getTag();

			Exit exit = getExit(convertProcedureType(tag.getType()),
					tag.getLabel());
			if (exit == null) {
				exit = makeExit(bodyExit.getDataBuses().size(),
						convertProcedureType(tag.getType()), tag.getLabel());
			}
			assert exit.getDataBuses().size() == bodyExit.getDataBuses().size() : "Number of data buses on exit doesn't match procedure";
			addExitMap(exit, bodyExit);

			// Map buses to procedure body's buses.
			for (Iterator bodyIter = bodyExit.getDataBuses().iterator(), iter = exit
					.getDataBuses().iterator(); bodyIter.hasNext();) {
				Bus db = (Bus) iter.next();
				Bus bb = (Bus) bodyIter.next();
				addBusMap(db, bb);
			}
			addBusMap(exit.getDoneBus(), bodyExit.getDoneBus());
		}
	}

	private static Exit.Type convertProcedureType(Exit.Type procedureType) {
		assert procedureType != Exit.DONE : "Procedure has Exit type: "
				+ procedureType;
		return procedureType == Exit.RETURN ? Exit.DONE : procedureType;
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
}
