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

/**
 * JobHandlerAdapter implements the basic funtionality of a JobHandler.
 * <P>
 * 
 * Created: Thu Mar 14 15:31:14 2002
 * 
 * @author <a href="mailto:abk@cubist">Andreas Kollegger</a>
 * @version $Id: JobHandlerAdapter.java 2 2005-06-09 20:00:48Z imiller $
 */
public class JobHandlerAdapter implements JobHandler {

	Engine engine;
	String stageName;

	/**
	 * The name of the stage which this adapter will handle.
	 */
	public JobHandlerAdapter(String name) {
		this.stageName = name;
	}

	@Override
	public void start(Engine engine) {
		this.engine = engine;
		engine.fireHandlerStart(this);
	}

	@Override
	public void stop() {
		engine.fireHandlerFinish(this);
		this.engine = null;
	}

	@Override
	public String getStageName() {
		return stageName;
	}

} // class JobHandlerAdapter
