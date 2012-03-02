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

package net.sf.openforge.util;

/**
 * Stoppable describes an interface of an object representing a task which can
 * be stopped via outside request. The Task is required to check for stoppage by
 * calling the breathe() routine throughout its course of execution, and by
 * trapping for Stoppable.InterruptExeption.
 * 
 */
public interface Stoppable {
	/**
	 * This method is used to request the running Stoppable object discontinue
	 * execution ASAP
	 * 
	 */
	public void requestStop();

	/**
	 * This will allow a task to be reset.
	 * 
	 */
	public void requestStart();

	/**
	 * Inside the thread of a task, call this periodically to check if execution
	 * should continue. If not, a Stoppable.InterruptException will be thrown.
	 */
	public void takeBreath();

	/**
	 * This is a simple default adapter class. You can use this as public class
	 * foo extends Stoppable.Adapter
	 */
	class Adapter implements Stoppable {
		private boolean die = false;

		public void requestStop() {
			die = true;
		}

		public void requestStart() {
			die = false;
		}

		public void takeBreath() {
			if (die) {
				throw new Stoppable.InterruptException("Requested Stop!");
			}
		}

	}

	/**
	 * This is the exception that the Stoppable task should catch to indicate
	 * execution has stopped.
	 */
	class InterruptException extends RuntimeException {
		public InterruptException(String msg) {
			super(msg);
		}
	}
}
