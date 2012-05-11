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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A EngineThread is used to interact with a thread or runnable object. It also
 * has static methods to manipulate the mapping of Thread :: Job [1 :: 1]
 * 
 * @author <a href="cschanck@xilinx.com">CRSS</a>
 * @version $Id: EngineThread.java 2 2005-06-09 20:00:48Z imiller $
 */
public class EngineThread {
	private static Map<Thread, Engine> threadToJob = new HashMap<Thread, Engine>();

	// statc class -- don't create any instances!
	private EngineThread() {
	}

	/**
	 * Add the relationship of a thread to an engine. This will overwrite a
	 * previously added relationship for this thread.
	 * 
	 * @param thread
	 *            a value of type 'Thread'
	 * @param engine
	 *            a value of type 'Engine'
	 */
	public static void addThread(Thread thread, Engine engine) {
		synchronized (threadToJob) {
			threadToJob.put(thread, engine);
		}
	}

	/**
	 * Adds a relationship from the current thread to the specified engine
	 * 
	 * @param engine
	 *            a value of type 'Engine'
	 */
	public static void addThread(Engine engine) {
		addThread(Thread.currentThread(), engine);
	}

	/**
	 * Remove the relationship of a thread to an engine.
	 * 
	 * @param thread
	 *            a value of type 'Thread'
	 */
	public static void removeThread(Thread thread) {
		synchronized (threadToJob) {
			threadToJob.remove(thread);
		}
	}

	/**
	 * Remove the relationship of a thread to an engine.
	 * 
	 * @param thread
	 *            a value of type 'Thread'
	 */
	public static void removeJob(Engine engine) {
		synchronized (threadToJob) {
			for (Iterator<Entry<Thread, Engine>> it = threadToJob.entrySet()
					.iterator(); it.hasNext();) {
				Entry<Thread, Engine> entry = it.next();
				Engine e = entry.getValue();
				if (e.equals(engine))
					it.remove();
			}
		}
	}

	/**
	 * check if a thread has a known engine
	 * 
	 * @param thread
	 *            a value of type 'Thread'
	 * @return a value of type 'boolean'
	 */
	public static final boolean isKnownThread(Thread thread) {

		return threadToJob.containsKey(thread);
	}

	/**
	 * Get the Engine for specific engine
	 * 
	 * @param thread
	 *            a value of type 'Thread'
	 * @return a value of type 'Engine'
	 */
	public static final Engine getEngine(Thread thread) {
		return threadToJob.get(thread);
	}

	/**
	 * Get the Engine for the current thread
	 * 
	 */
	public static final Engine getEngine() {
		return getEngine(Thread.currentThread());
	}

	public static final GenericJob getGenericJob(Thread t) {

		Engine e = getEngine(t);

		if (e != null) {

			return e.getGenericJob();
		}

		return null;
	}

	public static final GenericJob getGenericJob() {
		return getGenericJob(Thread.currentThread());
	}

	public static void info(Object token, String s) {
		getGenericJob().getLogger().getRawLogger()
				.log(java.util.logging.Level.INFO, s, token);
	}

	public static void verbose(Object token, String s) {
		getGenericJob().getLogger().getRawLogger()
				.log(java.util.logging.Level.FINE, s, token);
	}
} // class EngineThread
