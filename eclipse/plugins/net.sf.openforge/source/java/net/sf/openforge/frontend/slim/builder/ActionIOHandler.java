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

package net.sf.openforge.frontend.slim.builder;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.io.FifoID;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.lim.io.SimpleInternalPin;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinStall;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.io.actor.ActorPort;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * ActionIOHandler is the base class for a series of classes which are
 * responsible for managing the way that I/O is handled at the ports of an
 * Action. Each Action port may be implemented using a different electrical
 * protocol (eg fifo I/O, bus I/O, or simple wires). This class provides a
 * consistent interface to the parser regardless of the type of underlying
 * protocol.
 * 
 * 
 * <p>
 * Created: Wed Jul 13 11:57:55 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */

public abstract class ActionIOHandler {

	protected ActionIOHandler() {
	}

	/**
	 * The build method causes the appropriate structures to implement the
	 * Action input/output to be instantiated and added to the specified
	 * {@link Design} object.
	 * 
	 * @param design
	 *            a value of type 'Design'
	 */
	public abstract void build(Design design);

	/**
	 * A unique read access to the input/output resource for this IO handler is
	 * created and returned. Behavior of this method is undefined if the 'build'
	 * method has not been called.
	 * 
	 * @return a non-null Component
	 */
	public abstract Component getReadAccess(Element element);

	/**
	 * A unique stall access on the specified pin for this IO handler. Behavior
	 * of this method is undefined if the 'build' method has not been called.
	 */
	public abstract Component getStallAccess();

	/**
	 * A unique write access to the input/output resource for this IO handler is
	 * created and returned. Behavior of this method is undefined if the 'build'
	 * method has not been called.
	 * 
	 * @return a non-null Component
	 */
	public abstract Component getWriteAccess(Element element);

	/**
	 * A unique access to the token count for the resource for this IO handler
	 * is created and returned. Behavior of this method is undefined if the
	 * 'build' method has not been called.
	 * 
	 * @return a non-null Component
	 */
	public abstract Component getTokenCountAccess();

	/**
	 * Returns a unique peek access to the resource backing this IO handler. The
	 * peek access provides a token offset (and potentially a field offset) port
	 * and returns the scalar value at that offset. The token peek functionality
	 * does not affect the number of tokens in the queue. The token peek
	 * functionality is valid for both input and output queues.
	 */
	public abstract Component getTokenPeekAccess();

	/**
	 * Returns a unique access to the status flag of the resource backing this
	 * I/O handler.
	 */
	public abstract Component getStatusAccess();

	/**
	 * A specific implementation of the ActionIOHandler class which is
	 * implemented by a {@link FifoIF} resource. All input/output using this
	 * type of handler is accopmlished through a standard Fifo interface. The
	 * Fifo interface will be named according the the name of the specified port
	 * node.
	 */
	public static class FifoIOHandler extends ActionIOHandler {
		private final Element portNode;
		private ActorPort resource;

		public FifoIOHandler(Node portNode) {
			super();
			assert portNode.getNodeType() == Node.ELEMENT_NODE;
			this.portNode = (Element) portNode;
		}

		/**
		 * Creates a new FifoIF for the Design.
		 * 
		 * @param design
		 *            a value of type 'Design'
		 */
		@Override
		public void build(Design design) {
			final String direction = portNode
					.getAttribute(SLIMConstants.PORT_DIRECTION);
			final String portName = portNode
					.getAttribute(SLIMConstants.PORT_NAME);
			final String portSize = portNode
					.getAttribute(SLIMConstants.PORT_SIZE);
			if (portSize.length() == 0) {
				EngineThread.getGenericJob().warn(
						"Port " + portName + " has no size specified!");
			}
			FifoID fifoId = new NamedFifoID();
			fifoId.setBitWidth(Integer.parseInt(portSize));
			fifoId.setID(portName);
			fifoId.setDirection(direction.startsWith("in"));
			fifoId.setType(FifoID.TYPE_ACTION_SCALAR);

			resource = (ActorPort) design.getFifoIF(fifoId);
		}

		@Override
		public Component getReadAccess(Element accElement) {
			if (!resource.isInput())
				throw new UnsupportedOperationException(
						"Cannot read from an output interface");
			return resource.getAccess(getBlockingState(accElement));
		}

		@Override
		public Component getStallAccess() {
			// This could be supported if we wanted to stall on the
			// availability of input data, or not-full status of the
			// output channel.
			throw new UnsupportedOperationException(
					"Cannot stall on a FIFO interface");
		}

		@Override
		public Component getWriteAccess(Element accElement) {
			if (resource.isInput())
				throw new UnsupportedOperationException(
						"Cannot write to an input interface");
			return resource.getAccess(getBlockingState(accElement));
		}

		@Override
		public Component getTokenCountAccess() {
			if (!resource.isInput())
				throw new UnsupportedOperationException(
						"Cannot get the token count from an output interface");
			return resource.getCountAccess();
		}

		@Override
		public Component getTokenPeekAccess() {
			return resource.getPeekAccess();
		}

		@Override
		public Component getStatusAccess() {
			return resource.getStatusAccess();
		}

		private static final boolean getBlockingState(Element element) {
			String accType = element
					.getAttribute(SLIMConstants.PORT_ACCESS_STYLE);
			return accType
					.equalsIgnoreCase(SLIMConstants.PORT_ACCESS_BLOCKING_STYLE);
		}
	}

	/**
	 * A specific implementation of the ActionIOHandler class which is
	 * implemented by a {@link SimplePin} resource. All input/output using this
	 * type of handler is accomplished through a standard SimplePin interface.
	 * The Simple Pin interface will be named according the the name of the
	 * specified port node.
	 * 
	 * @author Endri Bezati
	 */
	public static class NativeIOHandler extends ActionIOHandler {
		private final Element portNode;
		private ActorPort resource;

		public NativeIOHandler(Node portNode) {
			super();
			assert portNode.getNodeType() == Node.ELEMENT_NODE;
			this.portNode = (Element) portNode;
		}

		@Override
		public void build(Design design) {
			final String direction = portNode
					.getAttribute(SLIMConstants.PORT_DIRECTION);
			final String portName = portNode
					.getAttribute(SLIMConstants.PORT_NAME);
			final String portSize = portNode
					.getAttribute(SLIMConstants.PORT_SIZE);
			if (portSize.length() == 0) {
				EngineThread.getGenericJob().warn(
						"Port " + portName + " has no size specified!");
			}
			FifoID fifoId = new NamedFifoID();
			fifoId.setBitWidth(Integer.parseInt(portSize));
			fifoId.setID(portName);
			fifoId.setDirection(direction.startsWith("in"));
			fifoId.setType(FifoID.TYPE_ACTION_NATIVE_SCALAR);

			resource = (ActorPort) design.getFifoIF(fifoId);

		}

		@Override
		public Component getReadAccess(Element element) {
			if (!resource.isInput())
				throw new UnsupportedOperationException(
						"Cannot read from an output interface");
			return resource.getAccess(getBlockingState(element));
		}

		@Override
		public Component getStallAccess() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Component getWriteAccess(Element element) {
			if (resource.isInput())
				throw new UnsupportedOperationException(
						"Cannot write to an input interface");
			return resource.getAccess(getBlockingState(element));
		}

		@Override
		public Component getTokenCountAccess() {
			throw new UnsupportedOperationException(
					"Cannot get the token count from an native port");
		}

		@Override
		public Component getTokenPeekAccess() {
			return resource.getPeekAccess();
		}

		@Override
		public Component getStatusAccess() {
			return resource.getStatusAccess();
		}

		private static final boolean getBlockingState(Element element) {
			String accType = element
					.getAttribute(SLIMConstants.PORT_ACCESS_STYLE);
			return accType
					.equalsIgnoreCase(SLIMConstants.PORT_ACCESS_BLOCKING_STYLE);
		}

	}

	public static class InternalPinHandler extends ActionIOHandler {
		private final Element portNode;
		private SimplePin pin = null;

		public InternalPinHandler(Node internalPortNode) {
			super();
			portNode = (Element) internalPortNode;
		}

		@Override
		public void build(Design design) {
			final String portName = portNode.getAttribute("name");
			final String portSize = portNode.getAttribute("size");

			SimplePin pin = new SimpleInternalPin(Integer.parseInt(portSize),
					portName);
			design.addComponentToDesign(pin);
			this.pin = pin;
		}

		@Override
		public Component getReadAccess(Element element) {
			return new SimplePinRead(pin);
		}

		@Override
		public Component getStallAccess() {
			return new SimplePinStall(pin);
		}

		@Override
		public Component getWriteAccess(Element element) {
			return new SimplePinWrite(pin);
		}

		@Override
		public Component getTokenCountAccess() {
			throw new UnsupportedOperationException(
					"Cannot get the token count from an internal pin");
		}

		@Override
		public Component getTokenPeekAccess() {
			throw new UnsupportedOperationException(
					"Cannot get the token count from an internal pin");
		}

		@Override
		public Component getStatusAccess() {
			throw new UnsupportedOperationException(
					"Cannot get the status from an internal pin");
		}
	}

}// ActionIOHandler
