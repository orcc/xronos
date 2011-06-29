/*
 * Copyright (c) 2011, EPFL
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the EPFL nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.orc2hdl.ui;

import static net.sf.orc2hdl.LaunchConstants.FORGE_COMB_LUT_MEM_READ;
import static net.sf.orc2hdl.LaunchConstants.FORGE_DP_LUT;
import static net.sf.orc2hdl.LaunchConstants.FORGE_EDK;
import static net.sf.orc2hdl.LaunchConstants.FORGE_LOOP_BAL;
import static net.sf.orc2hdl.LaunchConstants.FORGE_MUL_DECOMP_LIMIT;
import static net.sf.orc2hdl.LaunchConstants.FORGE_NO_BLOCK;
import static net.sf.orc2hdl.LaunchConstants.FORGE_NO_BLOCK_SCHED;
import static net.sf.orc2hdl.LaunchConstants.FORGE_NO_INCLUDE;
import static net.sf.orc2hdl.LaunchConstants.FORGE_NO_LOG;
import static net.sf.orc2hdl.LaunchConstants.FORGE_PIPELINE;
import static net.sf.orc2hdl.LaunchConstants.FORGE_SIM_ARB;
import static net.sf.orc2hdl.LaunchConstants.FORGE_VERBOSE;
import static net.sf.orc2hdl.LaunchConstants.FPGA_TYPE;
import static net.sf.orc2hdl.LaunchConstants.OUTPUT_FOLDER;
import static net.sf.orc2hdl.LaunchConstants.PROJECT;
import static net.sf.orc2hdl.LaunchConstants.SYNC_FIFO;
import static net.sf.orc2hdl.LaunchConstants.XDF_FILE;

import java.io.File;

import net.sf.orcc.OrccActivator;
import net.sf.orcc.util.OrccUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @author Endri Bezati
 * 
 */

public class Orc2HdlSettingsTab extends AbstractLaunchConfigurationTab
		implements ModifyListener {

	private String project;

	private Text textXDF;

	private Text textOutput;

	private Combo fpgaCombo;

	private Button syncFIFO;

	private Button openForgeVerbose;

	private Button openForgePipeline;

	private Button openForgeNoBlockIO;

	private Button openForgeNoEDK;

	private Button openForgeNoBlockSched;

	private Button openForgeSimArb;

	private Button openForgeLoopBal;

	private Button openForgeMulDecompLimit;

	private Button openForgeCombLutMemRead;

	private Button openForgeDpLUT;

	private Button openForgeNoLog;

	private Button openForgeNoInclude;

	private boolean updateLaunchConfiguration;

	private Text textProject;

	private void browseDirectory(Shell shell, Text textOutput) {
		DirectoryDialog dialog = new DirectoryDialog(shell, SWT.NONE);
		if (getFolderFromText(textOutput)) {
			dialog.setFilterPath(textOutput.getText());
		}

		String dir = dialog.open();
		if (dir != null) {
			textOutput.setText(dir);
		}
	}

	/**
	 * Method called when the "Browse..." button is clicked. Shows a selection
	 * dialog with projects available in the workspace.
	 * 
	 * @param shell
	 *            a shell
	 */
	private void browseProject(Shell shell) {
		ElementTreeSelectionDialog tree = new ElementTreeSelectionDialog(shell,
				WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(),
				new WorkbenchContentProvider());
		tree.setAllowMultiple(false);
		tree.setInput(ResourcesPlugin.getWorkspace().getRoot());

		IProject project = getProjectFromText();
		if (project != null) {
			tree.setInitialSelection(project);
		}

		tree.setMessage("Please select an existing project:");
		tree.setTitle("Choose an existing project");

		tree.setValidator(new ISelectionStatusValidator() {

			@Override
			public IStatus validate(Object[] selection) {
				if (selection.length == 1) {
					if (selection[0] instanceof IProject) {
						return new Status(IStatus.OK, OrccActivator.PLUGIN_ID,
								"");
					} else {
						return new Status(IStatus.ERROR,
								OrccActivator.PLUGIN_ID,
								"Only projects can be selected");
					}
				}

				return new Status(IStatus.ERROR, OrccActivator.PLUGIN_ID,
						"No project selected.");
			}

		});

		// opens the dialog
		if (tree.open() == Window.OK) {
			project = (IProject) tree.getFirstResult();
			textProject.setText(project.getName());
		}
	}

	private void browseWorkspace(Shell shell) {
		ElementTreeSelectionDialog tree = new ElementTreeSelectionDialog(shell,
				WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(),
				new WorkbenchContentProvider());
		tree.setAllowMultiple(false);
		tree.setInput(ResourcesPlugin.getWorkspace().getRoot());

		IResource resource = getFileFromText();
		if (resource != null) {
			tree.setInitialSelection(resource);
		}

		tree.setMessage("Please select an existing xdf file:");
		tree.setTitle("Choose an existing XDF file");

		tree.setValidator(new ISelectionStatusValidator() {

			@Override
			public IStatus validate(Object[] selection) {
				if (selection.length == 1) {
					if (selection[0] instanceof IFile) {
						return new Status(IStatus.OK, OrccActivator.PLUGIN_ID,
								"");
					} else {
						return new Status(IStatus.ERROR,
								OrccActivator.PLUGIN_ID,
								"Only files can be selected");
					}
				}

				return new Status(IStatus.ERROR, OrccActivator.PLUGIN_ID,
						"No file selected.");
			}

		});

		// opens the dialog
		if (tree.open() == Window.OK) {
			resource = (IResource) tree.getFirstResult();
			textXDF.setText(OrccUtil.getQualifiedName((IFile) resource));
			project = resource.getProject().getName();
		}
	}

	private IFile getFileFromText() {
		String value = textXDF.getText();
		if (value.isEmpty()) {
			return null;
		}

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return root.getFileForLocation(new Path(value));
	}

	public IProject getProjectFromText() {
		String projectName = textProject.getText();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		if (root.getFullPath().isValidSegment(projectName)) {
			return root.getProject(projectName);
		}
		return null;
	}

	private boolean getFolderFromText(Text textOutput) {
		String value = textOutput.getText();
		File file = new File(value);
		if (file.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		setControl(composite);

		Font font = parent.getFont();
		composite.setFont(font);

		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 0;
		composite.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(data);

		createControlProject(font, composite);
		createControlTopXDF(font, composite);
		createControlOutputFolder(font, composite);
		createControlFpgaType(font, composite);
		createControlOpenForgeFlags(font, composite);
		createControlSystemBuilder(font, composite);

	}

	private void createControlProject(Font font, Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setFont(font);
		group.setText("&Project:");
		group.setLayout(new GridLayout(3, false));
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		group.setLayoutData(data);

		// createControlBrowseProject(font, group);
		textProject = new Text(group, SWT.BORDER | SWT.SINGLE);
		textProject.setFont(font);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textProject.setLayoutData(data);
		textProject.addModifyListener(this);

		Button buttonBrowse = new Button(group, SWT.PUSH);
		buttonBrowse.setFont(font);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		buttonBrowse.setLayoutData(data);
		buttonBrowse.setText("&Browse...");
		buttonBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseProject(group.getShell());
			}
		});

	}

	public void createControlTopXDF(Font font, Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setFont(font);
		group.setText("&Top XDF Application:");
		group.setLayout(new GridLayout(3, false));
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		group.setLayoutData(data);

		Label lbl = new Label(group, SWT.NONE);
		lbl.setFont(font);
		lbl.setText("XDF file:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		lbl.setLayoutData(data);

		textXDF = new Text(group, SWT.BORDER | SWT.SINGLE);
		textXDF.setFont(font);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textXDF.setLayoutData(data);
		textXDF.addModifyListener(this);

		Button buttonBrowse = new Button(group, SWT.PUSH);
		buttonBrowse.setFont(font);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		buttonBrowse.setLayoutData(data);
		buttonBrowse.setText("&Browse...");
		buttonBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseWorkspace(group.getShell());
			}
		});
	}

	public void createControlOutputFolder(Font font, Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setFont(font);
		group.setText("&Ouput:");
		group.setLayout(new GridLayout(3, false));
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		group.setLayoutData(data);

		Label lbl = new Label(group, SWT.NONE);
		lbl.setFont(font);
		lbl.setText("Output folder:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		lbl.setLayoutData(data);

		textOutput = new Text(group, SWT.BORDER | SWT.SINGLE);
		textOutput.setFont(font);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textOutput.setLayoutData(data);
		textOutput.addModifyListener(this);

		Button buttonBrowse = new Button(group, SWT.PUSH);
		buttonBrowse.setFont(font);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		buttonBrowse.setLayoutData(data);
		buttonBrowse.setText("&Browse...");
		buttonBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseDirectory(group.getShell(), textOutput);
			}
		});
	}

	public void createControlFpgaType(Font font, Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setFont(font);
		group.setText("&Select an FPGA:");
		group.setLayout(new GridLayout(3, false));
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		group.setLayoutData(data);

		Label lbl = new Label(group, SWT.NONE);
		lbl.setFont(font);
		lbl.setText("FPGA Type:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		lbl.setLayoutData(data);

		fpgaCombo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		fpgaCombo
				.setItems(new String[] { "Spartan 3", "Virtex 2", "Virtex 4" });
		fpgaCombo.select(1);
		fpgaCombo.setFont(font);
		fpgaCombo.addModifyListener(this);
	}

	public void createControlOpenForgeFlags(Font font, Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setFont(font);
		group.setText("&OpenForge Options:");
		group.setLayout(new GridLayout(3, false));
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		group.setLayoutData(data);

		Label lblVerbose = new Label(group, SWT.NONE);
		lblVerbose.setFont(font);
		lblVerbose.setText("Verbose:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblVerbose.setLayoutData(data);

		openForgeVerbose = new Button(group, SWT.CHECK);
		openForgeVerbose.setToolTipText("Print OpenForge compilation Output");
		openForgeVerbose.setSelection(true);

		Label lbl = new Label(group, SWT.NONE);
		lbl.setFont(font);
		lbl.setText("Pipelining:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lbl.setLayoutData(data);

		openForgePipeline = new Button(group, SWT.CHECK);
		openForgePipeline
				.setToolTipText("Allow auto-insertion of registers based on max-gate-depth spec in the XLIM.");
		openForgePipeline.setSelection(true);

		Label lblNoBlockIO = new Label(group, SWT.NONE);
		lblNoBlockIO.setFont(font);
		lblNoBlockIO.setText("No Block IO:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblNoBlockIO.setLayoutData(data);

		openForgeNoBlockIO = new Button(group, SWT.CHECK);
		openForgeNoBlockIO
				.setToolTipText("Do not automatically infer fifo interface from top level function signature.");
		openForgeNoBlockIO.setSelection(false);

		Label lblNoBlockSched = new Label(group, SWT.NONE);
		lblNoBlockSched.setFont(font);
		lblNoBlockSched.setText("No Block Based Scheduling:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblNoBlockSched.setLayoutData(data);

		openForgeNoBlockSched = new Button(group, SWT.CHECK);
		openForgeNoBlockSched
				.setToolTipText("Do not perform block-based scheduling(auto s/w pipelining of top level tasks).");
		openForgeNoBlockSched.setSelection(true);

		Label lblSimArb = new Label(group, SWT.NONE);
		lblSimArb.setFont(font);
		lblSimArb.setText("Simple shared memory arbitration:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblSimArb.setLayoutData(data);

		openForgeSimArb = new Button(group, SWT.CHECK);
		openForgeSimArb
				.setToolTipText("Use simple arbitration of shared memories (assumes logic handles contention.");
		openForgeSimArb.setSelection(true);

		Label lbleEDK = new Label(group, SWT.NONE);
		lbleEDK.setFont(font);
		lbleEDK.setText("No EDK Generation:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lbleEDK.setLayoutData(data);

		openForgeNoEDK = new Button(group, SWT.CHECK);
		openForgeNoEDK
				.setToolTipText("Do not generate EDK pcore compliant directory output structure.");
		openForgeNoEDK.setSelection(true);

		Label lblLoopBal = new Label(group, SWT.NONE);
		lblLoopBal.setFont(font);
		lblLoopBal.setText("Balance Loop Latency:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblLoopBal.setLayoutData(data);

		openForgeLoopBal = new Button(group, SWT.CHECK);
		openForgeLoopBal
				.setToolTipText("Balance loop latency. Ensures that all paths take at least 1 cycle if any path does so that loop iteration flop can be removed.");
		openForgeLoopBal.setSelection(true);

		Label lblMulDecompLimit = new Label(group, SWT.NONE);
		lblMulDecompLimit.setFont(font);
		lblMulDecompLimit.setText("Multiplier Decomposition:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblMulDecompLimit.setLayoutData(data);

		openForgeMulDecompLimit = new Button(group, SWT.CHECK);
		openForgeMulDecompLimit
				.setToolTipText("Any multiplier which can be decomposed into 2 or fewer add/subtract + shift stages is.");
		openForgeMulDecompLimit.setSelection(true);

		Label lblCombLutMemRead = new Label(group, SWT.NONE);
		lblCombLutMemRead.setFont(font);
		lblCombLutMemRead.setText("Combinationally LUT reads:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblCombLutMemRead.setLayoutData(data);

		openForgeCombLutMemRead = new Button(group, SWT.CHECK);
		openForgeCombLutMemRead
				.setToolTipText("Reads of LUT based memories are performed combinationally.");
		openForgeCombLutMemRead.setSelection(true);

		Label lblDpLUT = new Label(group, SWT.NONE);
		lblDpLUT.setFont(font);
		lblDpLUT.setText("Allow Dual Port LUT:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblDpLUT.setLayoutData(data);

		openForgeDpLUT = new Button(group, SWT.CHECK);
		openForgeDpLUT
				.setToolTipText("Allow generation of dual ported LUT memories (default is to only use dual port BRAMs).");
		openForgeDpLUT.setSelection(true);

		Label lblNoLog = new Label(group, SWT.NONE);
		lblNoLog.setFont(font);
		lblNoLog.setText("No Log:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblNoLog.setLayoutData(data);

		openForgeNoLog = new Button(group, SWT.CHECK);
		openForgeNoLog.setToolTipText("No log file generation.");
		openForgeNoLog.setSelection(true);

		Label lblNoInclude = new Label(group, SWT.NONE);
		lblNoInclude.setFont(font);
		lblNoInclude.setText("No Include:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lblNoInclude.setLayoutData(data);

		openForgeNoInclude = new Button(group, SWT.CHECK);
		openForgeNoInclude
				.setToolTipText("Suppress generation of _sim and _synth files");
		openForgeNoInclude.setSelection(true);

	}

	public void createControlSystemBuilder(Font font, Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setFont(font);
		group.setText("&SystemBuilder Options:");
		group.setLayout(new GridLayout(3, false));
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		group.setLayoutData(data);

		Label lbl = new Label(group, SWT.NONE);
		lbl.setFont(font);
		lbl.setText("Synchronous FIFO:");
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		lbl.setLayoutData(data);

		syncFIFO = new Button(group, SWT.CHECK);
		syncFIFO.setToolTipText("Use the systemBuilder synchronous FIFO model.");
	}

	@Override
	public String getName() {
		return "Settings";
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {

		String sValue;
		int iValue;
		Boolean bValue;
		try {
			updateLaunchConfiguration = false;
			project = configuration.getAttribute(PROJECT, "");

			sValue = configuration.getAttribute(XDF_FILE, "");
			textXDF.setText(sValue);

			sValue = configuration.getAttribute(OUTPUT_FOLDER, "");
			textOutput.setText(sValue);

			iValue = configuration.getAttribute(FPGA_TYPE, -1);
			fpgaCombo.select(iValue);

			bValue = configuration.getAttribute(FORGE_VERBOSE, false);
			openForgeVerbose.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_PIPELINE, true);
			openForgePipeline.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_NO_BLOCK, true);
			openForgeNoBlockIO.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_NO_BLOCK_SCHED, true);
			openForgeNoBlockSched.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_SIM_ARB, true);
			openForgeSimArb.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_EDK, true);
			openForgeNoEDK.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_LOOP_BAL, true);
			openForgeLoopBal.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_MUL_DECOMP_LIMIT, true);
			openForgeMulDecompLimit.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_COMB_LUT_MEM_READ, true);
			openForgeCombLutMemRead.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_DP_LUT, true);
			openForgeDpLUT.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_NO_LOG, true);
			openForgeNoLog.setSelection(bValue);

			bValue = configuration.getAttribute(FORGE_NO_INCLUDE, true);
			openForgeNoInclude.setSelection(bValue);

			bValue = configuration.getAttribute(SYNC_FIFO, false);
			syncFIFO.setSelection(bValue);

		} catch (CoreException e) {
			e.printStackTrace();
			updateLaunchConfiguration = true;
		}
		updateLaunchConfiguration = true;
	}

	@Override
	public void modifyText(ModifyEvent e) {
		if (updateLaunchConfiguration) {
			updateLaunchConfigurationDialog();
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(PROJECT, project);
		configuration.setAttribute(XDF_FILE, textXDF.getText());
		configuration.setAttribute(OUTPUT_FOLDER, textOutput.getText());
		configuration.setAttribute(FPGA_TYPE, fpgaCombo.getSelectionIndex());
		configuration.setAttribute(FORGE_VERBOSE,
				openForgeVerbose.getSelection());
		configuration.setAttribute(FORGE_PIPELINE,
				openForgePipeline.getSelection());
		configuration.setAttribute(FORGE_NO_BLOCK,
				openForgeNoBlockIO.getSelection());
		configuration.setAttribute(FORGE_NO_BLOCK_SCHED,
				openForgeNoBlockSched.getSelection());
		configuration.setAttribute(FORGE_SIM_ARB,
				openForgeSimArb.getSelection());
		configuration.setAttribute(FORGE_EDK, openForgeNoEDK.getSelection());
		configuration.setAttribute(FORGE_LOOP_BAL,
				openForgeLoopBal.getSelection());
		configuration.setAttribute(FORGE_MUL_DECOMP_LIMIT,
				openForgeMulDecompLimit.getSelection());
		configuration.setAttribute(FORGE_COMB_LUT_MEM_READ,
				openForgeCombLutMemRead.getSelection());
		configuration.setAttribute(FORGE_DP_LUT, openForgeDpLUT.getSelection());
		configuration.setAttribute(FORGE_NO_LOG, openForgeNoLog.getSelection());
		configuration.setAttribute(FORGE_NO_INCLUDE,
				openForgeNoInclude.getSelection());
		configuration.setAttribute(SYNC_FIFO, syncFIFO.getSelection());
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(PROJECT, "");
		configuration.setAttribute(XDF_FILE, "");
		configuration.setAttribute(OUTPUT_FOLDER, "");
		configuration.setAttribute(FPGA_TYPE, -1);
		configuration.setAttribute(FORGE_VERBOSE, false);
		configuration.setAttribute(FORGE_PIPELINE, true);
		configuration.setAttribute(FORGE_NO_BLOCK, true);
		configuration.setAttribute(FORGE_NO_BLOCK_SCHED, true);
		configuration.setAttribute(FORGE_SIM_ARB, true);
		configuration.setAttribute(FORGE_EDK, true);
		configuration.setAttribute(FORGE_LOOP_BAL, true);
		configuration.setAttribute(FORGE_MUL_DECOMP_LIMIT, true);
		configuration.setAttribute(FORGE_COMB_LUT_MEM_READ, true);
		configuration.setAttribute(FORGE_DP_LUT, true);
		configuration.setAttribute(FORGE_NO_LOG, true);
		configuration.setAttribute(FORGE_NO_INCLUDE, true);
		configuration.setAttribute(SYNC_FIFO, false);
	}

	@Override
	public void setErrorMessage(String errorMessage) {
		super.setErrorMessage(errorMessage);
	}

	@Override
	public void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}

}