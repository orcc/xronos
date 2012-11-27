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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xronos.openforge.app.logging.ForgeLogger;


public class Drain {

	// shared variables must be volatile!
	volatile boolean alive = true;

	public Drain(InputStream stream, ForgeLogger logger) {
		drain(stream, logger);
	}

	public boolean isAlive() {
		return alive;
	}

	/**
	 * Starts a thread to keep a given text InputStream from filling up.
	 */
	private void drain(final InputStream stream, final ForgeLogger logger) {
		new Thread() {
			@Override
			public void run() {
				// System.out.println("Starting Drain: " +
				// Thread.currentThread());

				InputStreamReader reader = new InputStreamReader(stream);
				BufferedReader buffer = new BufferedReader(reader);

				try {
					while (true) {
						String nextLine = buffer.readLine();
						if (nextLine == null) {
							break;
						} else {
							if (logger != null) {
								logger.info(nextLine);
							}
						}
					}
				} catch (IOException eIO) {
				}

				// System.out.println("Drain Dying: " + Thread.currentThread());
				alive = false;
			}

		}.start();
	}
}
