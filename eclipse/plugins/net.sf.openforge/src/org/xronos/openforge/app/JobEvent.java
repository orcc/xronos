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
package org.xronos.openforge.app;

import java.util.EventObject;

/**
 * JobEvent represents an event which occurred in relation to the processing of
 * a job.
 * <P>
 * Created: Thu Mar 14 15:34:45 2002
 * 
 * @author <a href="mailto:abk@cubist">Andreas Kollegger</a>
 * @version $Id: JobEvent.java 2 2005-06-09 20:00:48Z imiller $
 */

@SuppressWarnings("serial")
public class JobEvent extends EventObject {

	String message;

	public JobEvent(JobHandler source, String message) {
		super(source);
		this.message = message;
	} // JobEvent()

	public JobHandler getJobHandler() {
		return (JobHandler) getSource();
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return new String("JobEvent[message(" + getMessage() + "), source("
				+ getSource() + ")]");
	}

	public static class Started extends JobEvent {
		public static final String MESSAGE = "started";

		public Started(JobHandler source) {
			super(source, MESSAGE);
		}
	}

	public static class Finished extends JobEvent {
		public static final String MESSAGE = "finished";

		public Finished(JobHandler source) {
			super(source, MESSAGE);
		}
	}

	public static class Error extends JobEvent {
		public static final String MESSAGE = "error";

		public Error(JobHandler source) {
			super(source, MESSAGE);
		}
	}

} // class JobEvent
