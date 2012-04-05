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
 * Created on Sep 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.sf.openforge.app;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.lim.Design;
import net.sf.openforge.util.Stoppable;

/**
 * @author sb
 * 
 *         An Engine represents a single compilation task processed under a
 *         specific set of user preferences. This is the compiler's source for
 *         all user-configurable settings. Also, all access to the "environment"
 *         passes through the Engine, so that interested listeners can monitor
 *         what is going on and each engine can potentially have its own little
 *         world.
 * 
 *         Based on Andy's Job.java
 */
public abstract class Engine extends Stoppable.Adapter implements Runnable {
	/**
     * 
     */
	public static final int IN_PROGRESS = 0;
	public static final int STOPPED = 1;
	public static final int ERROR = 2;

	/**
	 * The LIM design produced by this job. The design may be null, or the
	 * result of any stage of compilation.
	 */
	private Design design;

	private final Set<JobListener> listeners = new HashSet<JobListener>();
	private final GenericJob gj;
	private JobHandler jobHandler;
	protected volatile int status = STOPPED;

	public Engine(GenericJob genJob) {
		gj = genJob;
		EngineThread.addThread(this);
		if (gj.getTargetFiles().length > 0) {
			File targetFile = gj.getTargetFiles()[0];
			jobHandler = new JobHandlerAdapter("Forging: " + targetFile);
		}
	}

	/**
	 * Get the Generic Job being processed by this Engine.
	 * 
	 * @return the GenericJob being processed
	 */
	public GenericJob getGenericJob() {
		return gj;
	}

	/**
	 * Gets the design produced by this Engine. May be null, or the result of
	 * any stage of compilation.
	 */
	public Design getDesign() {
		return design;
	}

	/**
	 * If this returns a non-null Design object, the lim will be processed.
	 * 
	 * @return a value of type 'Design'
	 */
	public abstract Design buildLim();

	/**
	 * Tell the Engine to start processing. The Engine status should only change
	 * to IN_PROGRESS when the current handler fires an event to indicate that
	 * processing is underway.
	 * 
	 */
	public final void begin() {
		try {
			fireHandlerStart(getCurrentHandler());

			// just to be sure the Engine :: thread relationship is correct
			updateJobThread();

			breathe();

			design = buildLim();

			breathe();

			if (design != null) {
				long t0 = System.currentTimeMillis();
				new LIMCompiler().processLim(design);
				long t1 = System.currentTimeMillis();
				System.out.println("LIM Compiled in: "
						+ ((float) (t1 - t0) / (float) 1000) + "s\n");
			} else {
				gj.info("Skipping Lim Processing -- no Lim produced.");
			}

			fireHandlerFinish(getCurrentHandler());
		} catch (Stoppable.InterruptException se) {
			fireHandlerFinish(getCurrentHandler());
			gj.decAll();
			gj.error("Lim Compilation Terminated.");
		}
	}

	/**
	 * Stop the processing of the current job by passing on the stop request to
	 * the current job handler. This returns immediately, regardless of whether
	 * the handler has actually stopped or not. Use status() to determine if the
	 * job has actually stopped.
	 */
	public void end() {
		requestStop();
	}

	/**
	 * Runs this Job by calling begin(). Allows this Job to be attached to a
	 * Thread.
	 */
	@Override
	public void run() {
		begin();
	}

	/**
	 * update the Engine :: thread relationship
	 * 
	 */
	public void updateJobThread() {
		EngineThread.addThread(this);
	}

	public void kill() {
		status = STOPPED;
		EngineThread.removeJob(this);
	}

	/**
	 * Return the current GenericJob of the current Engine for the current
	 * thread.
	 * 
	 * @return Current Project
	 */
	public static GenericJob genericJob() {
		return EngineThread.getGenericJob();
	}

	/**
	 * gets the current JobHandler.
	 * 
	 */
	public JobHandler getCurrentHandler() {
		return jobHandler;
	}

	/**
	 * Get the current status of job handling.
	 */
	public int status() {
		return status;
	}

	/**
	 * Distributes a JobEvent to all registered JobListeners.
	 * 
	 * @param event
	 *            the JobEvent to distribute
	 */
	public void fireEvent(JobEvent event) {
		for (JobListener listener : listeners) {
			listener.jobNotice(event);
		}
	} // fireEvent()

	/**
	 * Fires a new JobEvent.started for a JobHandler.
	 * 
	 * @param handler
	 *            the JobHandler which is starting
	 */
	public void fireHandlerStart(JobHandler hand) {
		status = IN_PROGRESS;
		fireEvent(new JobEvent.Started(hand));
	}

	/**
	 * Fires a new JobEvent finished for a JobHandler.
	 * 
	 * @param handler
	 *            the JobHandler which has finished
	 */
	public void fireHandlerFinish(JobHandler hand) {
		status = STOPPED;
		fireEvent(new JobEvent.Finished(hand));
		// ABK: why remove the job? There are multiple handlers per job
		// so this shouldn't be done until the entire job is finished, not
		// just a single handler.
		// EngineThread.removeJob(this);
	}

	public void fireHandlerError(JobHandler hand) {
		status = ERROR;
		fireEvent(new JobEvent.Error(hand));
	}

	/**
	 * Allow for graceful termination of the task
	 * 
	 */
	public static void breathe() {
		engine().takeBreath();
	}

	/**
	 * Return the current engine of the currently executing thread.
	 * 
	 * @return current engine
	 */
	public static Engine engine() {
		return EngineThread.getEngine();
	}

	/**
	 * Log an error level message, then cease processing and exit the engine.
	 * This is a duplicate of GenericJob.fatalError(String s); so call that
	 * 
	 * @param s
	 *            a value of type 'String'
	 */
	public void fatalError(String s) {
		gj.fatalError(s);
	}

	public void addJobListener(JobListener listener) {
		listeners.add(listener);
	}

	public void removeJobListener(JobListener listener) {
		listeners.remove(listener);
	}
}
