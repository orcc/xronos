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

package net.sf.openforge.verilog.mapping.memory;

import net.sf.openforge.app.ForgeFatalException;
import net.sf.openforge.util.XilinxDevice;

/**
 * A base class of all of the XST supported dual-port ram models.
 * <P>
 * 
 * Created: Tue Jun 18 12:37:06 2002
 * 
 * @author cwu
 * @version $Id: DualPortRam.java 490 2007-06-15 16:37:00Z imiller $
 */

public abstract class DualPortRam extends Ram implements Cloneable {

	public static final int PORTA = 0;
	public static final int PORTB = 1;

	protected String[] clkName = { "", "" };
	protected String[] weName = { "", "" };
	protected String[] oeName = { "", "" };

	protected String[] addrName = { "", "" };
	protected int addrWidth = 0;
	protected int[] addrStartBit = { 0, 0 };
	protected boolean[] addrScalar = { false, false };

	protected String[] dataInName = { "", "" };
	protected int[] dataInStartBit = { 0, 0 };

	protected String[] dataOutName = { "", "" };
	protected int[] dataOutStartBit = { 0, 0 };
	protected String[] dataOutExtraName = { "", "" };

	protected int[] dataAttachWidth = { 0, 0 };
	protected boolean[] datScalar = { false, false };

	protected int[] initValues = null;
	protected int currenInitBit = 0;

	// virtex4 dual port lut based ram primitives
	private static DualPortRam[] v4lut = { new RAM16X1D() };

	// virtex4 dual port block based ram primitives
	private static DualPortRam[] v4block = { new RAMB16_S1_S1(),
			new RAMB16_S2_S2(), new RAMB16_S4_S4(), new RAMB16_S9_S9(),
			new RAMB16_S18_S18(), new RAMB16_S36_S36() };

	// virtex2/pro dual port lut based ram primitives
	private static DualPortRam[] v2lut = { new RAM16X1D(), new RAM32X1D(),
			new RAM64X1D() };

	// virtex2/pro dual port block ram based primitives
	private static DualPortRam[] v2block = { new RAMB16_S1_S1(),
			new RAMB16_S2_S2(), new RAMB16_S4_S4(), new RAMB16_S9_S9(),
			new RAMB16_S18_S18(), new RAMB16_S36_S36() };

	// virtex/spartan2 dual port lut based ram primitives
	private static DualPortRam[] vlut = { new RAM16X1D() };

	// virtex/spartan2 dual port block ram based primitives
	private static DualPortRam[] vblock = { new RAMB4_S1_S1(),
			new RAMB4_S2_S2(), new RAMB4_S4_S4(), new RAMB4_S8_S8(),
			new RAMB4_S16_S16() };

	protected DualPortRam() {
	}

	public static DualPortRam[] getDualPortMappers(XilinxDevice xd,
			boolean lut_map) {
		switch (xd.getFamily()) {
		case XilinxDevice.VIRTEX2P:
		case XilinxDevice.VIRTEX2:
			return lut_map ? v2lut : v2block;

		case XilinxDevice.VIRTEX4SX:
		case XilinxDevice.VIRTEX4FX:
		case XilinxDevice.VIRTEX4LX:
			return lut_map ? v4lut : v4block;

			// XXX: We don't have docs on spartan 3 internal
			// resources, play it really safe with the basics that
			// we'd expect them to support.
		case XilinxDevice.SPARTAN3:
			return lut_map ? vlut : v2block;

		case XilinxDevice.VIRTEX:
		case XilinxDevice.VIRTEXE:
		case XilinxDevice.SPARTAN2:
		case XilinxDevice.SPARTAN2E:
			return lut_map ? vlut : vblock;

		case XilinxDevice.SPARTAN:
		case XilinxDevice.SPARTANXL:
		case XilinxDevice.XC4000EX:
		case XilinxDevice.XC4000E:
		case XilinxDevice.XC4000XLA:
		case XilinxDevice.XC4000XV:
			// These devices only have LUT memory, so if the
			// requester doesn't want luts, give them nothing.
			// They would tell us they don't want LUTs if they are
			// requesting a true dual port with read/write on both
			// sides, which LUTs can't do.
			return null;
		case XilinxDevice.XC4000:
		case XilinxDevice.XC4000L:
		case XilinxDevice.XC5200:
		case XilinxDevice.XC3000:
		case XilinxDevice.XC9500:
		case XilinxDevice.XC9500XL:
		case XilinxDevice.XC9500XV:
			// What are they doing!!! no mappers for these guys!
			return null;
		default:
			// They must not have defined a part in the
			// preferences, no mappers for you!
			return null;
		}
	}

	public abstract boolean isBlockRam16();

	@Override
	public void setClkName(String name) {
		setClkName(name, PORTA);
	}

	public void setClkName(String name, int port) {
		clkName[port] = name;
	}

	@Override
	public void setWeName(String name) {
		setWeName(name, PORTA);
	}

	public void setWeName(String name, int port) {
		weName[port] = name;
	}

	@Override
	public void setOeName(String name) {
		setOeName(name, PORTA);
	}

	public void setOeName(String name, int port) {
		oeName[port] = name;
	}

	public void setAddr(String name, int width, int start, boolean scalar,
			int which) {
		setAddrName(name, which);
		setAddrWidth(width);
		setAddrStartBit(start, which);
		setAddrScalar(scalar, which);
	}

	@Override
	public void setAddrName(String name) {
		setAddrName(name, PORTA);
	}

	public void setAddrName(String name, int port) {
		addrName[port] = name;
	}

	@Override
	public void setAddrWidth(int width) {
		if (width > getLibAddressWidth())
			addrWidth = getLibAddressWidth();
		else
			addrWidth = width;
	}

	@Override
	public void setAddrStartBit(int startBit) {
		setAddrStartBit(startBit, PORTA);
	}

	public void setAddrStartBit(int startBit, int port) {
		addrStartBit[port] = startBit;
	}

	@Override
	public void setAddrScalar(boolean scalar) {
		setAddrScalar(scalar, PORTA);
	}

	public void setAddrScalar(boolean scalar, int port) {
		addrScalar[port] = scalar;
	}

	@Override
	public void setDataInName(String name) {
		setDataInName(name, PORTA);
	}

	public void setDataInName(String name, int port) {
		dataInName[port] = name;
	}

	@Override
	public void setDataAttachWidth(int width) {
		setDataAttachWidth(width, PORTA);
	}

	public void setDataAttachWidth(int width, int port) {
		if (width > getWidth())
			dataAttachWidth[port] = getWidth();
		else
			dataAttachWidth[port] = width;
	}

	@Override
	public void setDataScalar(boolean scalar) {
		setDataScalar(scalar, PORTA);
	}

	public void setDataScalar(boolean scalar, int port) {
		datScalar[port] = scalar;
	}

	@Override
	public void setDataInStartBit(int startBit) {
		setDataInStartBit(startBit, PORTA);
	}

	public void setDataInStartBit(int startBit, int port) {
		dataInStartBit[port] = startBit;
	}

	@Override
	public void setDataOutName(String name) {
		setDataOutName(name, PORTA);
	}

	public void setDataOutName(String name, int port) {
		dataOutName[port] = name;
	}

	@Override
	public void setDataOutExtraName(String name) {
		setDataOutExtraName(name, PORTA);
	}

	public void setDataOutExtraName(String name, int port) {
		dataOutExtraName[port] = name;
	}

	@Override
	public void setDataOutStartBit(int startBit) {
		setDataOutStartBit(startBit, PORTA);
	}

	public void setDataOutStartBit(int startBit, int port) {
		dataOutStartBit[port] = startBit;
	}

	@Override
	public int getLibAddressWidth() {
		int indexSize = 0;

		if (getDepth() == 0) {
			indexSize = 0;
		} else if (getDepth() == 1) {
			indexSize = 1;
		} else {
			// Find the top 1 in the number
			int size = 31;

			while (((1 << size) & (getDepth() - 1)) == 0)
				size--;

			indexSize = size + 1;
		}

		return indexSize;
	}

	@Override
	public void setNextInitBit(int bit) {
		if (initValues == null) {
			// allocate enough storage for all the bits of the memory.
			int size = getWidth() * getDepth();

			if ((size % 32) == 0)
				initValues = new int[(size / 32)];
			else
				initValues = new int[(size / 32) + 1];
		}

		int index = currentInitBit / 32;
		int offset = currentInitBit % 32;

		if (index < initValues.length) {
			// Add the given bit to the location
			initValues[index] |= (bit & 1) << offset;
			currentInitBit++;
		} else {
			throw new ForgeFatalException(this.getClass().getName()
					+ ".setNextInitBit(): array index out of bounds");
		}

	}

	@Override
	public int getInitBit(int addr) {
		if (initValues == null)
			return 0;

		int index = addr / 32;
		int offset = addr % 32;

		if (index < initValues.length)
			return ((initValues[index] >> offset) & 1);
		else {
			throw new ForgeFatalException(this.getClass().getName()
					+ ".getInitBit(" + addr + "): array index out of bounds");
		}
	}

	@Override
	public String toString() {
		return (getName());
	}

	@Override
	public Object clone() {
		DualPortRam dpr = null;

		try {
			dpr = (this.getClass().newInstance());
		} catch (IllegalAccessException iae) {
			return null;
		} catch (InstantiationException ie) {
			return null;
		} catch (ExceptionInInitializerError eiie) {
			return null;
		}

		dpr.clkName = clkName.clone();
		dpr.weName = weName.clone();
		dpr.oeName = oeName.clone();
		dpr.addrName = addrName.clone();
		dpr.addrWidth = addrWidth;
		dpr.addrStartBit = addrStartBit.clone();
		dpr.addrScalar = addrScalar.clone();
		dpr.dataInName = dataInName.clone();
		dpr.dataInStartBit = dataInStartBit.clone();
		dpr.dataOutName = dataOutName.clone();
		dpr.dataOutStartBit = dataOutStartBit.clone();
		dpr.dataOutExtraName = dataOutExtraName.clone();
		dpr.dataAttachWidth = dataAttachWidth.clone();
		dpr.datScalar = datScalar.clone();

		if (initValues != null)
			dpr.initValues = initValues.clone();
		else
			dpr.initValues = null;

		dpr.currentInitBit = currentInitBit;

		return dpr;
	}
}
