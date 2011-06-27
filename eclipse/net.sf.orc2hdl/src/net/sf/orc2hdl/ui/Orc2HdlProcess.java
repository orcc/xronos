/* Copyright (c) 2010-2011 - EPFL
 *
 * The use of this software is defined in the consortium agreement of the
 * ACTORS European Project (Adaptivity and Control of Resources in Embedded Systems)
 * funded in part by the European Unions Seventh Framework Programme (FP7).
 * Grant agreement no 216586.
 */
package net.sf.orc2hdl.ui;

import java.io.IOException;

import net.sf.orcc.util.WriteListener;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

public class Orc2HdlProcess extends PlatformObject implements IProcess,
		WriteListener {

	/**
	 * This class defines an implementation of stream monitor.
	 * 
	 * @author Matthieu Wipliez
	 * 
	 */
	private class CodesignMonitor implements IStreamMonitor {

		private String contents;

		private ListenerList list;

		/**
		 * Creates a new monitor.
		 */
		public CodesignMonitor() {
			contents = "";
			list = new ListenerList();
		}

		@Override
		public void addListener(IStreamListener listener) {
			list.add(listener);
		}

		@Override
		public String getContents() {
			synchronized (contents) {
				return contents;
			}
		}

		@Override
		public void removeListener(IStreamListener listener) {
			list.remove(listener);
		}

		/**
		 * Writes the given text to the contents watched by this monitor.
		 * 
		 * @param text
		 *            a string
		 */
		private void write(String text) {
			synchronized (contents) {
				contents += text;
			}

			for (Object listener : list.getListeners()) {
				((IStreamListener) listener).streamAppended(text, this);
			}
		}

	}

	private class CodesignProxy implements IStreamsProxy {

		private IStreamMonitor errorMonitor;

		private IStreamMonitor outputMonitor;

		public CodesignProxy() {
			errorMonitor = new CodesignMonitor();
			outputMonitor = new CodesignMonitor();
		}

		@Override
		public IStreamMonitor getErrorStreamMonitor() {
			return errorMonitor;
		}

		@Override
		public IStreamMonitor getOutputStreamMonitor() {
			return outputMonitor;
		}

		@Override
		public void write(String input) throws IOException {
			// nothing to do
		}

	}

	private ILaunchConfiguration configuration;

	private ILaunch launch;

	private IProgressMonitor monitor;

	private IStreamsProxy proxy;

	private boolean terminated;

	public Orc2HdlProcess(ILaunch launch, ILaunchConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		this.configuration = configuration;
		this.launch = launch;
		this.monitor = monitor;
		proxy = new CodesignProxy();
	}

	@Override
	public boolean canTerminate() {
		return !terminated;
	}

	@Override
	public String getAttribute(String key) {
		return null;
	}

	@Override
	public int getExitValue() throws DebugException {
		return 0;
	}

	@Override
	public String getLabel() {
		return configuration.getName();
	}

	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	public IProgressMonitor getProgressMonitor() {
		return monitor;
	}

	@Override
	public IStreamsProxy getStreamsProxy() {
		return proxy;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public void setAttribute(String key, String value) {
	}

	@Override
	public void terminate() throws DebugException {
		terminated = true;
		DebugEvent event = new DebugEvent(this, DebugEvent.TERMINATE);
		DebugEvent[] events = { event };
		DebugPlugin.getDefault().fireDebugEventSet(events);
	}

	@Override
	public void writeText(String text) {
		((CodesignMonitor) proxy.getOutputStreamMonitor()).write(text);
	}

}
