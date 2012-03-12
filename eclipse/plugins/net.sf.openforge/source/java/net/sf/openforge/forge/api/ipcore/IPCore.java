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

package net.sf.openforge.forge.api.ipcore;

import net.sf.openforge.forge.api.internal.Core;
import net.sf.openforge.forge.api.internal.IPCoreStorage;
import net.sf.openforge.forge.api.pin.ClockDomain;
import net.sf.openforge.forge.api.pin.ClockPin;
import net.sf.openforge.forge.api.pin.PinIn;
import net.sf.openforge.forge.api.pin.PinInOutTS;
import net.sf.openforge.forge.api.pin.PinOut;
import net.sf.openforge.forge.api.pin.ResetPin;
import net.sf.openforge.forge.api.pin.RestrictedPin;

/**
 * IPCore is an API class that allows the inclusion of existing or legacy cores
 * of Intellectual Property into a Forge generated design. These cores may exist
 * as stand alone elements of HDL, or may be dynamically generated via the
 * <code>HDLWriter</code> interface. A Forge design interacts with an IPCore via
 * instances of the subclasses of <code>Buffer</code>. At first, the Pin
 * directions may seem reversed, however if each Pin is observed from the
 * perspective of the Forge design, the meaning becomes more clear. To supply a
 * value to an IPCore, a PinOut is instantiated in the design, and a value
 * written to the PinOut in forgeable code. This value is driven from the design
 * and into the IPCore. Similarly, to retrieve a value from an IPCore output, a
 * PinIn is instantiated and the value from the IPCore is captured using a
 * <code>get</code> method.
 * <p>
 * IPCore objects also contain methods for publishing pins, meaning a direct
 * connection is made from the IPCore to the top level of the Forge design.
 * These published pins may be used to interact with elements outside the Forge
 * generated design. Similarly, methods exist in IPCore to generate 'no-connect'
 * pins, or ports on the IPCore that have no connection made in the instantiated
 * HDL.
 * <p>
 * Each IPCore instance may have 1 <code>HDLWriter</code> instance associated
 * with it. If an <code>HDLWriter</code> has been attached to an IPCore, that
 * writer is used to insert customized HDL code into the instantiated HDL. See
 * <code>HDLWriter</code> for more details.
 * <p>
 * Each IPCore instance may have 1 source file associated with it. The String
 * passed in via the <code>setHDLSource</code> method will be used to generate
 * an <i>include</i> statement in the instantiated HDL. This may be used to tie
 * together the Forge generated HDL with legacy IP for synthesis and simulation.
 * 
 */
public final class IPCore {

	/**
	 * A special clock domain that may be used with IPCore objects to serve as a
	 * 'stand in' clock domain. During compilation, the clock port associated
	 * with this domain will be connected to the clock which controls the
	 * accessing entry method(s). This means that all accessing entry methods
	 * <b>must</b> be in the same clock domain if the stand in clock domain is
	 * used, otherwise a fatal error will occur during Forge compilation.
	 */
	public final static ClockDomain AUTOCONNECT = new ClockDomain("StandIn",
			ClockPin.UNDEFINED_HZ);

	/**
	 * Creates a new <code>IPCore</code> instance with the given name.
	 * 
	 * @param name
	 *            a <code>String</code> value
	 */
	public IPCore(String name) {
		Core.addIPCore(this, name);
	}

	/**
	 * Creates a published PinOut for this IPCore.
	 * 
	 * @param portName
	 *            a <code>String</code> value
	 * @param topLevelName
	 *            a <code>String</code> value
	 * @param size
	 *            an <code>int</code> value
	 */
	public RestrictedPin publishPinOut(String portName, String topLevelName,
			int size) {
		PinIn in = new PinIn(this, portName, size);
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.addPublishPin(in, topLevelName);
		return new RestrictedPin(in);
	}

	/**
	 * Creates a published PinIn for this IPCore.
	 * 
	 * @param portName
	 *            a <code>String</code> value
	 * @param topLevelName
	 *            a <code>String</code> value
	 * @param size
	 *            an <code>int</code> value
	 */
	public RestrictedPin publishPinIn(String portName, String topLevelName,
			int size) {
		PinOut out = new PinOut(this, portName, size);
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.addPublishPin(out, topLevelName);
		return new RestrictedPin(out);
	}

	/**
	 * Creates a published PinOutTS for this <code>IPCore</code>.
	 * 
	 * @param portName
	 *            a <code>String</code> value
	 * @param topLevelName
	 *            a <code>String</code> value
	 * @param size
	 *            an <code>int</code> value
	 */
	public RestrictedPin publishPinOutTS(String portName, String topLevelName,
			int size) {
		PinIn in = new PinIn(this, portName, size);
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.addPublishPin(in, topLevelName);
		return new RestrictedPin(in);
	}

	/**
	 * Creates a published PinInOutTS for this <code>IPCore</code>.
	 * 
	 * @param portName
	 *            a <code>String</code> value
	 * @param topLevelName
	 *            a <code>String</code> value
	 * @param size
	 *            an <code>int</code> value
	 */
	public RestrictedPin publishPinInOutTS(String portName,
			String topLevelName, int size) {
		PinInOutTS inoutTS = new PinInOutTS(this, portName, size);
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.addPublishPin(inoutTS, topLevelName);
		return new RestrictedPin(inoutTS);
	}

	/**
	 * Creates a no connect PinIn for this <code>IPCore</code>.
	 * 
	 * @param portName
	 *            a <code>String</code> value
	 */
	public void addNoConnectPin(String portName) {
		PinIn in = new PinIn(this, portName);
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.addNoConnectPin(in);
	}

	/**
	 * <code>setWriter</code> allows user to specify the supplied IP Core
	 * Verilog code being integrated into a single Verilog output for the whole
	 * design.
	 * 
	 * @param hdlWriter
	 *            a <code>HDLWriter</code> object
	 */
	public void setWriter(HDLWriter hdlwriter) {
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.setHDLWriter(hdlwriter);
	}

	/**
	 * <code>setHDLInstanceName</code> allows the user to specify the instance
	 * name of the IPCore in the resulting hardware. This is usefull when user
	 * constraints are set targetting internals of the IPCore and the instance
	 * name needs to be deterministic.
	 * 
	 * @param instanceName
	 *            a <code>String</code> value
	 */
	public void setHDLInstanceName(String instanceName) {
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.setHDLInstanceName(instanceName);
	}

	/**
	 * <code>setHDLSource</code> allows the user to specify the source HDL file.
	 * 
	 * @param source
	 *            a <code>String</code> value
	 */
	public void setHDLSource(String source) {
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.setHDLSource(source);
	}

	/**
	 * Allows the user to connect a clock pin to the IPCore
	 * 
	 * @param clockPin
	 *            pin to connect
	 * @param portName
	 *            HDL name of the clock port
	 */
	public void connectClock(ClockPin clockPin, String portName) {
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.connect(clockPin, portName);
	}

	/**
	 * Allows the user to connect a reset pin to the IPCore. The reset pin
	 * should be obtained from the associated clock, and active high or low
	 * should be defined then.
	 * 
	 * @param resetPin
	 *            pin to connect
	 * @param portName
	 *            HDL name of the reset port
	 * @param activeHigh
	 *            true if the reset signal is active high
	 */
	public void connectReset(ResetPin resetPin, String portName) {
		IPCoreStorage ipcs = Core.getIPCoreStorage(this);
		ipcs.connect(resetPin, portName);
	}

}
