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

package org.xronos.openforge.app;

import org.xronos.openforge.frontend.xlim.app.XLIMEngine;

/**
 * A convenience class for setting up whatever is needed to run parts of Forge.
 * (A hack, really. But I prefer to have these dependencies wrapped up here.)
 * 
 * @author Andreas Kollegger
 * 
 * @version $Id: ForgeContext.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ForgeContext {
	private static GenericJob gj;

	@SuppressWarnings("unused")
	private static Engine engine;

	/**
	 * Constructs whatever instances various pieces of Forge expect to exist.
	 */
	public static void initialize() {
		gj = new GenericJob();

		gj.setOptionValues(new String[] { "-q", "-nolog" });

		engine = new XLIMEngine(gj);

		/*
		 * gj.updateLoggers();
		 * 
		 * Handler[] h = gj.getLogger().getRawLogger().getHandlers(); for (int
		 * i=0; ((h!=null) && (i < h.length)); i++) { h[i].setLevel(Level.OFF);
		 * }
		 */
	}

	/**
	 * Gets the GenericJob which has been initialized for the Forge context. If
	 * initialization hasn't been performed, this has the side-effect of first
	 * initializing the context.
	 * 
	 * @return
	 */
	public static GenericJob getJob() {
		if (gj == null)
			initialize();
		return gj;
	}
}
