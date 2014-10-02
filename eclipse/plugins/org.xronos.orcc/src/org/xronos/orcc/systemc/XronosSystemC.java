/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */
package org.xronos.orcc.systemc;

import static net.sf.orcc.backends.BackendsConstants.BXDF_FILE;
import static net.sf.orcc.backends.BackendsConstants.IMPORT_BXDF;

import java.io.File;

import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.backends.util.Validator;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.util.NetworkValidator;
import net.sf.orcc.tools.mapping.XmlBufferSizeConfiguration;
import net.sf.orcc.util.FilesManager;
import net.sf.orcc.util.Result;

/**
 * A synthesizable SystemC backend based on Xronos principles
 * 
 * @author Endri Bezati
 *
 */
public class XronosSystemC extends AbstractBackend {

	/** Printers **/
	private NetworkPrinter nPrinter;
	private InstancePrinter iPrinter;
	private TestbenchPrinter tbPrinter;
	private TclPrinter tclPrinter;
	
	
	/** Path for generated SystemC Actor and Network source file **/
	private String srcPath;

	/** Path for the RTL to be populated by HLS tools **/
	private String rtlPath;

	/** Path that contains the SystemC testbench files **/
	private String tbPath;

	@Override
	protected void doInitializeOptions() {

		// Create Folders

		// -- Source folder
		srcPath = outputPath + File.separator + "rtl";
		File srcDir = new File(srcPath);
		if (!srcDir.exists()) {
			srcDir.mkdir();
		}

		// -- RTL folder
		rtlPath = outputPath + File.separator + "rtl";
		File rtlDir = new File(rtlPath);
		if (!rtlDir.exists()) {
			rtlDir.mkdir();
		}

		// -- Testbench folder
		tbPath = outputPath + File.separator + "testbench";
		File tbDir = new File(tbPath);
		if (!tbDir.exists()) {
			tbDir.mkdir();
		}
	}

	@Override
	protected void doValidate(Network network) {
		Validator.checkMinimalFifoSize(network, fifoSize);

		new NetworkValidator().doSwitch(network);
	}

	@Override
	protected Result doGenerateNetwork(Network network) {
		nPrinter.setNetwork(network);
		return FilesManager.writeFile(nPrinter.getContent(),
				srcPath, network.getSimpleName() + ".h");
	}

	@Override
	protected Result doGenerateInstance(Instance instance) {
		iPrinter.setInstance(instance);
		return FilesManager.writeFile(iPrinter.getContent(), srcPath,
				instance.getSimpleName() + ".h");
	}

	@Override
	protected void beforeGeneration(Network network) {
		network.computeTemplateMaps();

		// if required, load the buffer size from the mapping file
		if (getOption(IMPORT_BXDF, false)) {
			File f = new File(getOption(BXDF_FILE, ""));
			new XmlBufferSizeConfiguration().load(f, network);
		}
	}

	@Override
	protected Result doAdditionalGeneration(Network network) {
		// TODO Auto-generated method stub
		return super.doAdditionalGeneration(network);
	}

	@Override
	protected Result doAdditionalGeneration(Instance instance) {
		// TODO Auto-generated method stub
		return super.doAdditionalGeneration(instance);
	}
	
	

}
