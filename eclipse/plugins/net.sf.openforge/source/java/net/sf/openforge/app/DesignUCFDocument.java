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

package net.sf.openforge.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionMultiFile;
import net.sf.openforge.backend.OutputEngine;
import net.sf.openforge.backend.hdl.VerilogTranslateEngine;
import net.sf.openforge.backend.hdl.ucf.UCFArbitraryStatement;
import net.sf.openforge.backend.hdl.ucf.UCFDocument;
import net.sf.openforge.backend.hdl.ucf.UCFStatement;
import net.sf.openforge.forge.api.internal.Core;
import net.sf.openforge.forge.api.pin.Buffer;
import net.sf.openforge.forge.api.pin.ClockPin;
import net.sf.openforge.forge.api.ucf.UCFAttribute;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.util.XilinxDevice;
import net.sf.openforge.util.naming.ID;

public class DesignUCFDocument extends UCFDocument implements OutputEngine {
	private final int family;
	private String targetspeed;

	public static final ForgeFileKey UCF_FILE = new ForgeFileKey(
			"Design UCF File");

	public DesignUCFDocument() {
		GenericJob gj = EngineThread.getGenericJob();
		String partstring = gj.getPart(CodeLabel.UNSCOPED).getFullDeviceName();
		if (partstring == null)
			partstring = "";
		if (!(new XilinxDevice(partstring)).isFullySpecified())
			partstring = "xc2v8000-5-ff1152C";
		this.family = (new XilinxDevice(partstring)).getFamily();

		this.targetspeed = gj.getTargetSpeed();
		if (targetspeed == null)
			targetspeed = "";

	}

	@Override
	public void initEnvironment() {
		final GenericJob gj = EngineThread.getGenericJob();
		final String filename = gj.getOption(OptionRegistry.UCF_FILE)
				.getValue(CodeLabel.UNSCOPED).toString();
		final File testFile = new File(filename);
		if (testFile.isAbsolute())
			gj.getFileHandler().registerFile(UCF_FILE,
					testFile.getParentFile(), testFile.getName());
		else {
			// Follow the HDL source
			File hdlTarget = gj.getFileHandler().getFile(
					VerilogTranslateEngine.VERILOG);
			gj.getFileHandler().registerFile(UCF_FILE,
					hdlTarget.getParentFile(), filename);
		}
	}

	@Override
	public void translate(Design design) throws IOException {
		File ucfFile = EngineThread.getGenericJob().getFileHandler()
				.getFile(UCF_FILE);
		File destination_dir = ucfFile.getAbsoluteFile().getParentFile();

		// Create directories if necessary
		if (!destination_dir.exists()) {
			destination_dir.mkdirs();
		}

		// Test if the ucf file already exists, and if so make
		// backup
		if (ucfFile.exists()) {
			ucfFile.renameTo(new File(ucfFile.getAbsolutePath() + ".old"));
		}

		final FileOutputStream ucfFos = new FileOutputStream(ucfFile);

		this.generateUCFDocument(design);

		this.write(ucfFos);
		ucfFos.flush();
		ucfFos.close();
	}

	/**
	 * Returns a string which uniquely identifies this phase of the compiler
	 * output.
	 * 
	 * @return a non-empty, non-null String
	 */
	@Override
	public String getOutputPhaseId() {
		return "UCF Document";
	}

	/**
	 * 
	 * @param genJob
	 *            the GenericJob from which the design was derived
	 * @param design
	 *            the design
	 */
	private void generateUCFDocument(Design design) {
		includeUserUCF();

		addHeader();

		Collection inputPins = design.getInputPins();
		Collection clockPins = design.getClockPins();

		inputPins.removeAll(clockPins);

		addClockHeader();

		addClocks(clockPins);

		addPinHeader();

		if (clockPins.size() > 0) {
			comment("Clock Pins");
		}
		addPins(clockPins);

		if (design.getInputPins().size() > 0) {
			comment("Input Pins");
		}
		addPins(design.getInputPins());
		if (design.getOutputPins().size() > 0) {
			comment("Output Pins");
		}
		addPins(design.getOutputPins());
		if (design.getBidirectionalPins().size() > 0) {
			comment("Bidirectional Pins");
		}
		addPins(design.getBidirectionalPins());

	}

	private void includeUserUCF() {
		ArrayList ucf_list = new ArrayList();
		Option op = EngineThread.getGenericJob().getOption(
				OptionRegistry.UCF_INCLUDES);
		ucf_list.addAll(((OptionMultiFile) op)
				.getValueAsList(CodeLabel.UNSCOPED));

		// Include each of the specified ucf files first
		// with header comments detailing which file they
		// came from.
		for (Iterator it = ucf_list.iterator(); it.hasNext();) {
			File userUcfFile = new File((String) it.next());
			LineNumberReader bis = null;

			// open the file and copy its contents into
			// our new ucf file.
			try {
				bis = new LineNumberReader(new FileReader(userUcfFile));

				// Put a header message first
				comment("Including user specified ucf file: "
						+ userUcfFile.getAbsolutePath());

				blank();

				String lineRead = bis.readLine();

				while (lineRead != null) {
					state(lineRead);
					lineRead = bis.readLine();
				}
			} catch (IOException ioe) {
			}

			blank();
			comment("End of included ucf file");
			blank();

			if (bis != null) {
				try {
					bis.close();
				} catch (IOException ioed) {
				}
			}
		}

	}

	private void addHeader() {
		comment("AutoGenerated by Forge for xflow run.");
		comment("Any existing .ucf file was renamed to .ucf.old");
		comment("");
	}

	private void addClockHeader() {
		comment("Timing constraints for clocks.");
	}

	private void addClocks(Collection pins) {
		String dsp48 = "";
		String mults = "";

		if ((family == XilinxDevice.VIRTEX2)
				|| (family == XilinxDevice.VIRTEX2P)
				|| (family == XilinxDevice.SPARTAN3)) {
			mults = " MULTS";
		}
		if ((family == XilinxDevice.VIRTEX4SX)
				|| (family == XilinxDevice.VIRTEX4FX)
				|| (family == XilinxDevice.VIRTEX4LX)) {
			dsp48 = " DSP48";
		}

		if (targetspeed.length() == 0) {

			long frequency = 0;

			for (Iterator it = pins.iterator(); it.hasNext();) {
				Pin pin = (Pin) it.next();
				ClockPin apiPin = (ClockPin) pin.getApiPin();

				if (apiPin != null) {
					long currentFrequency = apiPin.getFrequency();
					if (currentFrequency > frequency) {
						frequency = currentFrequency;
					}
				}
			}

			if (frequency > 0) {
				// the UCF file format can only handle the following
				// units in the TIMESPEC line: ms, us, ps, ns, mhz,
				// khz. We'll convert the user supplied frequency in
				// hertz to nanoseconds using floating point
				// arithmentic and create the correct string. Decimal
				// notation like 1.234 khz is also supported.

				double period = 1.0 / frequency;
				// convert to nanoseconds
				// convert to nanoseconds, * 1x10^9
				double periodNs = period * 1000000000.0;

				targetspeed = Double.toString(periodNs) + " ns";
			}
		}

		UCFStatement timeGroup = new UCFArbitraryStatement("TIMEGRP \"ALL\" = "
				+ "PADS LATCHES FFS RAMS" + mults + dsp48);
		UCFStatement timeSpec = new UCFArbitraryStatement(
				"TIMESPEC \"TS_All_to_All\" = " + "FROM \"ALL\" "
						+ "TO \"ALL\" " + targetspeed);

		if (targetspeed.length() > 0) {
			state(timeGroup);
			state(timeSpec);
		} else {
			comment(timeGroup);
			comment(timeSpec);
		}
	}

	private void addPinHeader() {
		comment("Below are all the design pins enumerated with IOSTANDARD constraints.");
		comment("");
		comment("For a full description of the IOSTANDARD constraints, please see");
		comment("the Constraints Guide section of the online documentation located at:");
		comment("<path-to-ISE>/doc/usenglish/manuals.pdf");
		comment("");
		comment("Possible values for speed settings are: FAST | SLOW.");
		comment("Possible values for pull settings are: PULLUP | PULLDOWN | KEEPER.");
		comment("Values for the LOC settings are package dependent.");
	}

	private void addPins(Collection pins) {
		for (Iterator it = pins.iterator(); it.hasNext();) {
			Pin pin = (Pin) it.next();

			if (!Core.hasThisPin(pin.getApiPin())
					|| Core.hasPublished(pin.getApiPin())) {
				String netName = ID.toVerilogIdentifier(ID.showLogical(pin));
				Buffer apiPin = pin.getApiPin();

				addApiPin(apiPin, pin.getWidth(), netName);
			}

		}

	}

	private void addApiPin(Buffer apiPin, int size, String netName) {
		if (size == 1) {
			if ((apiPin != null) && (apiPin.getUCFAttributes().size() > 0)) {
				List attrList = apiPin.getUCFAttributes();

				for (Iterator attrs = attrList.iterator(); attrs.hasNext();) {
					state(new UCFArbitraryStatement("NET \"" + netName + "\""
							+ "\t" + attrs.next().toString()));
				}
			} else {
				comment(new UCFArbitraryStatement("NET \"" + netName + "\""
						+ "\t" + "IOSTANDARD=LVTTL"));
				comment(new UCFArbitraryStatement("NET \"" + netName + "\""
						+ "\t" + "LOC = \"\""));
			}
		} else {
			if ((apiPin != null) && (apiPin.getUCFAttributes().size() > 0)) {
				for (Iterator attrs = apiPin.getUCFAttributes().iterator(); attrs
						.hasNext();) {
					UCFAttribute attr = (UCFAttribute) attrs.next();

					String bitSpec = "<*";

					if (attr.getBit() >= 0) {
						bitSpec = "<" + attr.getBit() + ">";
					}

					state(new UCFArbitraryStatement("NET \"" + netName
							+ bitSpec + "\"" + "\t" + attr.toString()));
				}
			} else {

				comment(new UCFArbitraryStatement("NET \"" + netName + "<*\""
						+ "\t" + "IOSTANDARD=LVTTL"));

				for (int i = (size - 1); i >= 0; i--) {
					comment(new UCFArbitraryStatement("NET \"" + netName + "<"
							+ i + ">\"" + "\t" + "LOC = \"\""));
				}
			}
		}
	}
}
