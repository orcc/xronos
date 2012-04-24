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

package net.sf.openforge.optimize.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.lim.io.FifoOutput;
import net.sf.openforge.lim.io.NativeOutput;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;

/**
 * FifoDataOutputOptimizer is a class which sets the status of consumesGo on
 * {@link SimplePinWrite} objects based on the number of accesses to that pin.
 * In specific, a data pin of a FifoOutput needs to be qualified with the GO
 * only when the target pin has multiple writers. A fifo output data pin with
 * only one writer can allow the data value to 'float' as there will not be a
 * wired-or structure between the writer and the pin.
 * 
 * <p>
 * Created: Fri Mar 02 10:37:09 2007
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoDataOutputOptimizer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class FifoDataOutputOptimizer extends DefaultVisitor {

	public FifoDataOutputOptimizer() {
	}

	/**
	 * Builds a correlation of SimplePin to all accesses to that SimplePin and
	 * then marks any data pin of output fifo interfaces as not needing the GO
	 * iff that pin has fewer than 2 accesses.
	 */
	@Override
	public void visit(Design design) {
		final PinAccessCorrelator correlator = new PinAccessCorrelator();
		design.accept(correlator);

		for (FifoIF fifoIF : design.getFifoInterfaces()) {
			if (fifoIF.isInput()) {
				continue;
			}

			FifoOutput fifoOutput;
			NativeOutput nativeOutput;
			SimplePin dataPin = null;
			if (fifoIF instanceof FifoOutput) {
				fifoOutput = (FifoOutput) fifoIF;
				dataPin = fifoOutput.getDataPin();
			} else if (fifoIF instanceof NativeOutput) {
				nativeOutput = (NativeOutput) fifoIF;
				dataPin = nativeOutput.getDataPin();
			}

			final Set<Referencer> refs = correlator.getRefs(dataPin);
			if (refs.size() < 2) {
				for (Referencer ref : refs) {
					assert ref instanceof SimplePinWrite : "Unknown access type to fifo output data pin";
					((SimplePinWrite) ref).setGoNeeded(false);
				}
			}
		}
	}

	/**
	 * A simple visitor class to correlate SimplePin and SimplePin
	 * Read/Write/Access nodes.
	 */
	private static class PinAccessCorrelator extends DefaultVisitor {
		Map<SimplePin, Set<Referencer>> correlation = new HashMap();

		public Set<Referencer> getRefs(SimplePin pin) {
			Set<Referencer> refs = this.correlation.get(pin);

			// Ensure that we never return null
			if (refs == null) {
				return Collections.EMPTY_SET;
			}

			return refs;
		}

		@Override
		public void visit(SimplePinWrite acc) {
			putAccess(acc, acc.getReferenceable());
		}

		@Override
		public void visit(SimplePinRead acc) {
			putAccess(acc, acc.getReferenceable());
		}

		@Override
		public void visit(SimplePinAccess acc) {
			putAccess(acc, acc.getReferenceable());
		}

		private void putAccess(Referencer ref, Referenceable refable) {
			assert refable instanceof SimplePin : "Only expecting simple pins in correlation of pin accesses";
			SimplePin pin = (SimplePin) refable;
			Set<Referencer> refs = this.correlation.get(pin);
			if (refs == null) {
				refs = new HashSet();
				this.correlation.put(pin, refs);
			}
			refs.add(ref);
		}

	}

}// FifoDataOutputOptimizer
