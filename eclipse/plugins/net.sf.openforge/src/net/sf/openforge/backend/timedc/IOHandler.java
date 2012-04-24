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

package net.sf.openforge.backend.timedc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.lim.io.SimplePin;

/**
 * IOHandler is responsible for generating structures and variable names for
 * those variables which represent the I/O of the design.
 * 
 * <p>
 * Created: Fri Apr 15 10:09:07 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: IOHandler.java 131 2006-04-07 15:18:04Z imiller $
 */
public abstract class IOHandler {

	public static final String IN_VAR = "inputs";
	public static final String OUT_VAR = "outputs";

	/** An ordered list of FifoIF (input) objects */
	private final List<FifoIF> inputs;
	/** An ordered list of FifoIF (output) objects */
	private final List<FifoIF> outputs;

	public static IOHandler makeIOHandler(Design design) {
		// Determine the type of IOHandler to use based on the type of
		// the interfaces. Note that for now all the iterfaces must
		// be of the same type.
		int typeID = -1;
		for (FifoIF fifo : design.getFifoInterfaces()) {
			if (typeID < 0)
				typeID = fifo.getType();
			else {
				if (fifo.getType() != typeID)
					throw new IllegalStateException(
							"Cannot generate C simulation model for designs with mixed interface types.  ("
									+ fifo.getType() + "," + typeID + ")");
			}
		}
		switch (typeID) {
		case FifoIF.TYPE_FSL_FIFO:
			return new FSLIOHandler(design);
		case FifoIF.TYPE_ACTOR_QUEUE:
			return new QueueIOHandler(design);
		case -1: // If there are NO interfaces then simply
					// default to the old Queue style.
			return new QueueIOHandler(design);
		}

		throw new IllegalStateException(
				"Unknown type of compilation.  Generation of I/O Handler in C model failed");
	}

	private IOHandler(Design design) {
		// Take each FifoIF in the design and put them into lists,
		// sorting by order. Since they are called 'FSL#' we can do
		// an alphabetic sort.
		final List<FifoIF> ins = new ArrayList<FifoIF>();
		final List<FifoIF> outs = new ArrayList<FifoIF>();
		final Set<SimplePin> fifoPins = new HashSet<SimplePin>();
		for (FifoIF fifo : design.getFifoInterfaces()) {
			if (fifo.isInput())
				ins.add(fifo);
			else
				outs.add(fifo);
			fifoPins.addAll(fifo.getPins());
		}

		this.inputs = sort(ins);
		this.outputs = sort(outs);
	}

	public boolean isHandled(SimplePin pin) {
		for (FifoIF input : inputs) {
			if (input.getPins().contains(pin))
				return true;
		}

		for (FifoIF output : outputs) {
			if (output.getPins().contains(pin))
				return true;
		}

		return false;
	}

	public void declareStructures(PrintStream ps) {
		// The way that these are declared has direct correlation with
		// the way that the structures are accessed by the
		// getAccessString and API functions

		// Declare the inputs first, then the outputs
		ps.println(this.getInputType() + " __inputShadow["
				+ this.getInputCount() + "] = {");
		for (int i = 0; i < this.getInputCount(); i++) {
			ps.println(this.getInputTypeInitString()
					+ (i < (this.getInputCount() - 1) ? "," : ""));
		}
		ps.println("};");

		ps.println(this.getInputDecl() + "= {");
		for (int i = 0; i < this.getInputCount(); i++) {
			ps.print("&__inputShadow[" + i + "]"
					+ ((i < this.getInputCount() - 1) ? "," : ""));
		}
		ps.println("};");

		ps.println(this.getOutputType() + " __outputShadow["
				+ this.getOutputCount() + "] = {");
		for (int i = 0; i < this.getOutputCount(); i++) {
			ps.println(this.getOutputTypeInitString()
					+ (i < (this.getOutputCount() - 1) ? "," : ""));
		}
		ps.println("};");

		ps.println(this.getOutputDecl() + "= {");
		for (int i = 0; i < this.getOutputCount(); i++) {
			ps.print("&__outputShadow[" + i + "]"
					+ ((i < this.getOutputCount() - 1) ? "," : ""));
		}
		ps.println("};");
	}

	/** Returns the list in proper sorted order. */
	protected abstract List<FifoIF> sort(List<FifoIF> list);

	/**
	 * Returns the string identifying the C data structure type for FifoIF
	 * inputs.
	 */
	public abstract String getInputType();

	/**
	 * Returns the string identifying the C data structure type for FifoIF
	 * outputs.
	 */
	public abstract String getOutputType();

	/** The variable name for the array of input data structures. */
	public String getInputVar() {
		return IN_VAR;
	}

	/** The variable name for the array of output data structures. */
	public String getOutputVar() {
		return OUT_VAR;
	}

	/**
	 * Returns the C data structure member name for the specific pin.
	 */
	protected abstract String getMemberName(SimplePin pin);

	/**
	 * Writes the C data structure type declarations for both the input and
	 * output types.
	 */
	public abstract void writeTypeDecls(PrintStream ps);

	/**
	 * Returns a string that can be used to initialize one copy of the input
	 * data structure.
	 */
	public abstract String getInputTypeInitString();

	/**
	 * Returns a string that can be used to initialize one copy of the output
	 * data structure.
	 */
	public abstract String getOutputTypeInitString();

	/** Writes the isSending API call. */
	public abstract void writeIsSending(PrintStream ps, boolean declOnly);

	/** Writes the isAcking API call. */
	public abstract void writeIsAcking(PrintStream ps, boolean declOnly);

	/** Writes the getDataValue API call. */
	public abstract void writeGetValue(PrintStream ps, boolean declOnly);

	/** Writes the setDataValue call. */
	public abstract void writeSetValue(PrintStream ps, boolean declOnly);

	protected List<FifoIF> getInputs() {
		return this.inputs;
	}

	protected List<FifoIF> getOutputs() {
		return this.outputs;
	}

	/** Writes the getID API call. */
	public void writeGetId(PrintStream ps, boolean declOnly) {
		ps.print("int getInterfaceID (char *portName)");
		if (declOnly) {
			ps.println(";");
			return;
		}
		ps.println();
		ps.println("{");

		int cnt = 0;
		for (FifoIF fifo : getInputs()) {
			ps.println("\tif (!strcmp(portName, \"" + fifo.getPortBaseName()
					+ "\")) return " + cnt + ";");
			cnt++;
		}
		for (FifoIF fifo : getOutputs()) {
			ps.println("\tif (!strcmp(portName, \"" + fifo.getPortBaseName()
					+ "\")) return " + cnt + ";");
			cnt++;
		}
		ps.println("\treturn -1; // unknown port id");
		ps.println("}");
	}

	/**
	 * Returns the number of input fifo interfaces found.
	 */
	public int getInputCount() {
		return getInputs().size();
	}

	/**
	 * Returns the number of output fifo interfaces found.
	 */
	public int getOutputCount() {
		return getOutputs().size();
	}

	/**
	 * Returns a list of the input port base names.
	 */
	public List<String> getInputPortNames() {
		List<String> names = new ArrayList<String>();
		for (FifoIF fifoIF : getInputs()) {
			names.add(fifoIF.getPortBaseName());
		}
		return names;
	}

	/**
	 * Returns a list of the output port base names.
	 */
	public List<String> getOutputPortNames() {
		List<String> names = new ArrayList<String>();
		for (FifoIF fifoIF : getOutputs()) {
			names.add(fifoIF.getPortBaseName());
		}
		return names;
	}

	/**
	 * Returns a string which is a declaration of a suitably sized array of fifo
	 * input structures.
	 * 
	 * @return a value of type 'String'
	 */
	public String getInputDecl() {
		return getInputType() + " *" + getInputVar() + "[" + getInputCount()
				+ "]";
	}

	/**
	 * Returns a string which is a declaration of a suitably sized array of fifo
	 * output structures.
	 * 
	 * @return a value of type 'String'
	 */
	public String getOutputDecl() {
		return getOutputType() + " *" + getOutputVar() + "[" + getOutputCount()
				+ "]";
	}

	/**
	 * Returns a String of the form fsl_in[index]->member
	 * 
	 * @param access
	 *            a value of type 'Referencer'
	 * @return a value of type 'String'
	 */
	public String getAccessString(Referencer access) {
		SimplePin pin = (SimplePin) access.getReferenceable();
		for (int i = 0; i < inputs.size(); i++) {
			FifoIF fifo = inputs.get(i);
			if (fifo.getPins().contains(pin)) {
				return getInputVar() + "[" + i + "]->" + getMemberName(pin);
			}
		}
		for (int i = 0; i < outputs.size(); i++) {
			FifoIF fifo = outputs.get(i);
			if (fifo.getPins().contains(pin)) {
				return getOutputVar() + "[" + i + "]->" + getMemberName(pin);
			}
		}
		assert false : "Unknown pin " + pin.getName();
		return "UNKNOWN PIN";
	}

	public void writeOutputInits(PrintStream ps) {
		for (int i = 0; i < getInputs().size(); i++) {
			FifoIF fifo = getInputs().get(i);
			for (SimplePin pin : fifo.getOutputPins()) {
				ps.println(getInputVar() + "[" + i + "]->" + getMemberName(pin)
						+ " = 0;");
			}
		}
		for (int i = 0; i < getOutputs().size(); i++) {
			FifoIF fifo = getOutputs().get(i);
			for (SimplePin pin : fifo.getOutputPins()) {
				ps.println(getOutputVar() + "[" + i + "]->"
						+ getMemberName(pin) + " = 0;");
			}
		}
	}

	private static class QueueIOHandler extends IOHandler {
		public final String IN_TYPE = "queue_input";
		public final String OUT_TYPE = "queue_output";

		public final String DATA_MEMBER = "data";
		public final String SEND_MEMBER = "send";
		public final String ACK_MEMBER = "ack";
		public final String RDY_MEMBER = "rdy";
		public final String COUNT_MEMBER = "count";

		private QueueIOHandler(Design design) {
			super(design);
		}

		@Override
		protected List sort(List list) {
			// No sorting.
			return Collections.unmodifiableList(list);
		}

		@Override
		public String getInputType() {
			return this.IN_TYPE;
		}

		@Override
		public String getOutputType() {
			return this.OUT_TYPE;
		}

		@Override
		protected String getMemberName(SimplePin pin) {
			String name = pin.getName().toLowerCase();
			if (name.contains("data"))
				return DATA_MEMBER;
			if (name.contains("send"))
				return SEND_MEMBER;
			if (name.contains("ack"))
				return ACK_MEMBER;
			if (name.contains("count"))
				return COUNT_MEMBER;
			if (name.contains("rdy"))
				return RDY_MEMBER;

			return name;
		}

		@Override
		public void writeTypeDecls(PrintStream ps) {
			ps.println("typedef struct");
			ps.println("{");
			ps.println("\tint " + DATA_MEMBER + "; /* consumed */");
			ps.println("\tint " + SEND_MEMBER + "; /* consumed */");
			ps.println("\tint " + ACK_MEMBER + "; /* produced */");
			ps.println("\tint " + COUNT_MEMBER + "; /* consumed */");
			ps.println("} " + getInputType() + ";");

			ps.println("typedef struct");
			ps.println("{");
			ps.println("\tint " + DATA_MEMBER + "; /* produced */");
			ps.println("\tint " + SEND_MEMBER + "; /* produced */");
			ps.println("\tint " + ACK_MEMBER + "; /* consumed */");
			ps.println("\tint " + RDY_MEMBER + "; /* consumed */");
			ps.println("\tint " + COUNT_MEMBER + "; /* produced */");
			ps.println("}" + getOutputType() + ";");
		}

		@Override
		public String getInputTypeInitString() {
			return "{0,0,0,0}";
		}

		@Override
		public String getOutputTypeInitString() {
			return "{0,0,0,1,0}";
		}

		@Override
		public void writeIsSending(PrintStream ps, boolean declOnly) {
			ps.print("int isSending (int id)");
			if (declOnly) {
				ps.println(";");
				return;
			}
			ps.println();
			ps.println("{");
			ps.println("\tif (id < " + getInputCount() + ")");
			ps.println("\t{");
			ps.println("\t\treturn 0; // inputs never send data");
			ps.println("\t}");
			ps.println("\telse");
			ps.println("\t{");
			ps.println("\t\tint index = id - " + getInputCount() + ";");
			ps.println("\t\t" + getOutputType() + " *output = "
					+ getOutputVar() + "[index];");
			ps.println("\t\treturn output->" + SEND_MEMBER + ";");
			ps.println("\t}");
			ps.println("}");
		}

		@Override
		public void writeIsAcking(PrintStream ps, boolean declOnly) {
			ps.print("int isAcking (int id)");
			if (declOnly) {
				ps.println(";");
				return;
			}
			ps.println();
			ps.println("{");
			ps.println("\tif (id < " + getInputCount() + ")");
			ps.println("\t{");
			ps.println("\t\t" + getInputType() + " *input = " + getInputVar()
					+ "[id];");
			ps.println("\t\treturn input->" + ACK_MEMBER + ";");
			ps.println("\t}");
			ps.println("\telse");
			ps.println("\t{");
			ps.println("\t\treturn 0; // outputs never ack their data");
			ps.println("\t}");
			ps.println("}");
		}

		@Override
		public void writeGetValue(PrintStream ps, boolean declOnly) {
			ps.print("int getDataValue (int id, int ackValue)");
			if (declOnly) {
				ps.println(";");
				return;
			}
			ps.println();
			ps.println("{");
			ps.println("\tif (id < " + getInputCount() + ")");
			ps.println("\t{");
			ps.println("\t\treturn 0; // there is no data to retrieve from an input");
			ps.println("\t}");
			ps.println("\telse");
			ps.println("\t{");
			ps.println("\t\tint index = id - " + getInputCount() + ";");
			ps.println("\t\t" + getOutputType() + " *output = "
					+ getOutputVar() + "[index];");
			ps.println("\t\toutput->" + ACK_MEMBER + " = ackValue;");
			ps.println("\t\toutput->" + RDY_MEMBER + " = 1;");
			ps.println("\t\treturn output->" + DATA_MEMBER + ";");
			ps.println("\t}");
			ps.println("}");
		}

		@Override
		public void writeSetValue(PrintStream ps, boolean declOnly) {
			ps.print("void setDataValue (int id, int dataValue, int sendValue)");
			if (declOnly) {
				ps.println(";");
				return;
			}
			ps.println();
			ps.println("{");
			ps.println("\tif (id < " + getInputCount() + ")");
			ps.println("\t{");
			ps.println("\t\t" + getInputType() + " *input = " + getInputVar()
					+ "[id];");
			ps.println("\t\tinput->" + DATA_MEMBER + " = dataValue;");
			ps.println("\t\tinput->" + SEND_MEMBER + " = sendValue;");
			ps.println("\t}");
			ps.println("\telse");
			ps.println("\t{");
			ps.println("\t\t// there is no data to set on an output");
			ps.println("\t}");
			ps.println("}");
		}
	}

	private static class FSLIOHandler extends IOHandler {
		public final String IN_TYPE = "fsl_input";
		public final String OUT_TYPE = "fsl_output";

		public final String DATA_MEMBER = "data";
		public final String EXISTS_MEMBER = "exists";
		public final String READ_MEMBER = "read";
		public final String FULL_MEMBER = "full";
		public final String WRITE_MEMBER = "write";
		public final String CTRL_MEMBER = "ctrl";
		public final String CLK_MEMBER = "clk";

		private FSLIOHandler(Design design) {
			super(design);
		}

		@Override
		public String getInputType() {
			return this.IN_TYPE;
		}

		@Override
		public String getOutputType() {
			return this.OUT_TYPE;
		}

		@Override
		protected List<FifoIF> sort(List<FifoIF> values) {
			// Sort alphabetically.
			List<FifoIF> result = new ArrayList<FifoIF>();

			for (FifoIF fifo : values) {
				final String name = ((SimplePin) fifo.getPins()).getName();
				boolean inserted = false;
				for (int i = 0; i < result.size(); i++) {
					final FifoIF cmp = result.get(i);
					final String cmpName = cmp.getPins().iterator().next()
							.getName();
					final int cmpValue = name.compareToIgnoreCase(cmpName);
					assert cmpValue != 0 : "Two interfaces share the same name "
							+ name + " " + cmpName;
					if (cmpValue < 0) {
						result.add(i, fifo);
						inserted = true;
						break;
					}
				}
				if (!inserted) {
					result.add(fifo);
				}
			}

			return result;
		}

		@Override
		public void writeTypeDecls(PrintStream ps) {
			ps.println("typedef struct");
			ps.println("{");
			ps.println("\tint " + DATA_MEMBER + "; /* consumed */");
			ps.println("\tint " + EXISTS_MEMBER + "; /* consumed */");
			ps.println("\tint " + READ_MEMBER + "; /* produced */");
			ps.println("\tint " + CTRL_MEMBER + "; /* consumed */");
			ps.println("\tint " + CLK_MEMBER + "; /* not used */");
			ps.println("} " + getInputType() + ";");

			ps.println("typedef struct");
			ps.println("{");
			ps.println("\tint " + DATA_MEMBER + "; /* produced */");
			ps.println("\tint " + FULL_MEMBER + "; /* consumed */");
			ps.println("\tint " + WRITE_MEMBER + "; /* produced */");
			ps.println("\tint " + CTRL_MEMBER + "; /* produced */");
			ps.println("\tint " + CLK_MEMBER + "; /* not used */");
			ps.println("}" + getOutputType() + ";");
		}

		@Override
		public String getInputTypeInitString() {
			return "{0,0,0,0,0}";
		}

		@Override
		public String getOutputTypeInitString() {
			return "{0,0,0,0,0}";
		}

		@Override
		protected String getMemberName(SimplePin pin) {
			String name = pin.getName().toLowerCase();
			if (name.contains("data"))
				return DATA_MEMBER;
			if (name.contains("exists"))
				return EXISTS_MEMBER;
			if (name.contains("read"))
				return READ_MEMBER;
			if (name.contains("full"))
				return FULL_MEMBER;
			if (name.contains("write"))
				return WRITE_MEMBER;
			if (name.contains("control"))
				return CTRL_MEMBER;
			if (name.contains("clk"))
				return CLK_MEMBER;
			return name;
		}

		@Override
		public void writeIsSending(PrintStream ps, boolean declOnly) {
			ps.print("int isSending (int id)");
			if (declOnly) {
				ps.println(";");
				return;
			}
			ps.println();
			ps.println("{");
			ps.println("\tif (id < " + getInputCount() + ")");
			ps.println("\t{");
			ps.println("\t\treturn 0; // inputs never send data");
			ps.println("\t}");
			ps.println("\telse");
			ps.println("\t{");
			ps.println("\t\tint index = id - " + getInputCount() + ";");
			ps.println("\t\tfsl_output *output = " + getOutputVar()
					+ "[index];");
			ps.println("\t\treturn output->" + WRITE_MEMBER + ";");
			ps.println("\t}");
			ps.println("}");
		}

		@Override
		public void writeIsAcking(PrintStream ps, boolean declOnly) {
			ps.print("int isAcking (int id)");
			if (declOnly) {
				ps.println(";");
				return;
			}
			ps.println();
			ps.println("{");
			ps.println("\tif (id < " + getInputCount() + ")");
			ps.println("\t{");
			ps.println("\t\tfsl_input *input = " + getInputVar() + "[id];");
			ps.println("\t\treturn input->" + READ_MEMBER + ";");
			ps.println("\t}");
			ps.println("\telse");
			ps.println("\t{");
			ps.println("\t\treturn 0; // outputs never ack their data");
			ps.println("\t}");
			ps.println("}");
		}

		@Override
		public void writeGetValue(PrintStream ps, boolean declOnly) {
			ps.print("int getDataValue (int id, int ackValue)");
			if (declOnly) {
				ps.println(";");
				return;
			}
			ps.println();
			ps.println("{");
			ps.println("\tif (id < " + getInputCount() + ")");
			ps.println("\t{");
			ps.println("\t\treturn 0; // there is no data to retrieve from an input");
			ps.println("\t}");
			ps.println("\telse");
			ps.println("\t{");
			ps.println("\t\tint index = id - " + getInputCount() + ";");
			ps.println("\t\tfsl_output *output = " + getOutputVar()
					+ "[index];");
			ps.println("\t\toutput->" + FULL_MEMBER + " = ackValue;");
			ps.println("\t\treturn output->" + DATA_MEMBER + ";");
			ps.println("\t}");
			ps.println("}");
		}

		@Override
		public void writeSetValue(PrintStream ps, boolean declOnly) {
			ps.print("void setDataValue (int id, int dataValue, int sendValue)");
			if (declOnly) {
				ps.println(";");
				return;
			}
			ps.println();
			ps.println("{");
			ps.println("\tif (id < " + getInputCount() + ")");
			ps.println("\t{");
			ps.println("\t\tfsl_input *input = " + getInputVar() + "[id];");
			ps.println("\t\tinput->" + DATA_MEMBER + " = dataValue;");
			ps.println("\t\tinput->" + EXISTS_MEMBER + " = sendValue;");
			ps.println("\t}");
			ps.println("\telse");
			ps.println("\t{");
			ps.println("\t\t// there is no data to set on an output");
			ps.println("\t}");
			ps.println("}");
		}
	}

}// IOHandler
