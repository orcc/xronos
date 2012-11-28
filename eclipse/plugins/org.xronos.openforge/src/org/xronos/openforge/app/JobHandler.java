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
 * JobHandler is an object which processes a Job, and which provides methods for
 * external control and monitoring. The JobHandler is responsible for posting
 * events for start/finish, and using the Job for all messages (stderr &
 * stdout).
 * <P>
 * 
 * Created: Thu Mar 14 15:31:14 2002
 * 
 * @author <a href="mailto:abk@cubist">Andreas Kollegger</a>
 * @version $Id: JobHandler.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface JobHandler {

	public void start(Engine eng);

	public void stop();

	public String getStageName();

} // class JobHandler
