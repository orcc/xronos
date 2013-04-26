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
 * $Id: LXGraph.java 108 2006-02-23 15:53:48Z imiller $
 *
 * 
 */

package org.xronos.openforge.lim.graph;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.Kicker;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.PinRead;
import org.xronos.openforge.lim.PinReferee;
import org.xronos.openforge.lim.PinWrite;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.PriorityMux;
import org.xronos.openforge.lim.Register;
import org.xronos.openforge.lim.RegisterGateway;
import org.xronos.openforge.lim.RegisterReferee;
import org.xronos.openforge.lim.Switch;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimplePinAccess;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.EndianSwapper;
import org.xronos.openforge.lim.memory.MemoryGateway;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryReferee;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.lim.memory.StructuralMemory;
import org.xronos.openforge.util.graphviz.Graph;
import org.xronos.openforge.util.naming.ID;

/**
 * LXGraph produces an expanded LIM {@link Graph}. It creates a separate
 * subgraph for each {@link Module} in a {@link Visitable} hierarchy. Every
 * {@link Module} and {@link Component} is annotated with its hashcode for
 * cross-referencing. In addition, each {@link Port} and {@link Bus} is
 * annotated with the size in bits of its {@link Value}, if it has one.
 * 
 * @version $Id: LXGraph.java 108 2006-02-23 15:53:48Z imiller $
 */
public class LXGraph extends DefaultVisitor {

	public static void graphTo(Design design, String filename) {
		try {
			LXGraph lxg = new LXGraph(design, 10);
			lxg.print(new PrintWriter(new FileOutputStream(filename)));
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public static void graphTo(Visitable module, String filename, int layers) {
		try {
			LXGraph lxg = new LXGraph(module.toString(), module, 10, layers);
			lxg.print(new PrintWriter(new FileOutputStream(filename)));
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	/** The top level graph */
	private Graph graph;

	/** The number of nodes so far, used to generate unique identifiers */
	private int nodeCount = 0;

	/** Map of LIM object to graph Node - for physical connections */
	// private Map nodeMap = new HashMap();

	private int fontSize;

	private int layersToGraph = -1;

	/**
	 * Designed to graph a design
	 * 
	 * @param design
	 *            a value of type 'Design'
	 */
	public LXGraph(Design design, int fontSize) {
		super();
		this.fontSize = fontSize;
		graph = new Graph(ID.showLogical(design));
		graph.setLabel(ID.showLogical(design));

		/*
		 * Some attributes to make the graph smaller, and to print to several
		 * pages when printed via: dot -Tps file.dot | lpr
		 */
		graph.setGVAttribute("ratio", "auto");
		graph.setGVAttribute("ranksep", ".5");
		graph.setGVAttribute("nodesep", ".12");
		graph.setGVAttribute("fontsize", "" + fontSize);
		// this.graph.setGVAttribute("fontname","Courier");
		// this.graph.setGVAttribute("page","8.5,11.0");

		design.accept(this);
	}

	public LXGraph(String name, Visitable topModule, int fontSize) {
		// graph all layers
		this(name, topModule, fontSize, -1);
	}

	// public static void graphTo (Module module, String filename)
	// {
	// graphTo(module, filename, -1); // all layers
	// }

	// public static void graphTo (Module module, String filename, int layers)
	// {
	// graphTo((Visitable)module, filename, layers);
	// }

	/**
	 * Constructs a new LXGraph.
	 * 
	 * @param name
	 *            a title for the graph
	 * @param topModule
	 *            the top level module to be graphed
	 */
	public LXGraph(String name, Visitable topModule, int fontSize,
			int layersToGraph) {
		super();
		this.fontSize = fontSize;
		graph = new Graph(name);
		graph.setLabel(name);
		this.layersToGraph = layersToGraph;

		/*
		 * Some attributes to make the graph smaller, and to print to several
		 * pages when printed via: dot -Tps file.dot | lpr
		 */
		graph.setGVAttribute("ratio", "auto");
		graph.setGVAttribute("ranksep", ".5");
		graph.setGVAttribute("nodesep", ".12");
		graph.setGVAttribute("fontsize", "" + fontSize);
		// this.graph.setGVAttribute("fontname","Helvetica");
		// this.graph.setGVAttribute("page","8.5,11.0");

		if (topModule instanceof StructuralMemory
				|| topModule instanceof Register.Physical) {
			doSubgraph((Module) topModule);
		} else {
			topModule.accept(this);
		}
	}

	// ////////////////////////////////
	// ////////////////////////////////
	//
	// Visitor methods
	//
	// ////////////////////////////////
	// ////////////////////////////////

	private void doSubgraph(Module mod) {
		BlackBoxGraph physGraph = new BlackBoxGraph(ID.showLogical(mod), mod,
				nodeCount++, fontSize);
		for (Component comp : mod.getComponents()) {
			physGraph.addComponent(comp);
			nodeCount++;
		}
		physGraph.graphAddedComponents();
		graph.addSubgraph(physGraph);
	}

	/**
	 * Prints a text representation of this graph readable by the <i>dotty</i>
	 * viewer.
	 * 
	 * @param writer
	 *            the writer to receive the text
	 */
	public void print(PrintWriter writer) {
		graph.print(writer);
		writer.flush();
	}

	/**
	 * 
	 * Adds the given graph as a subgraph of the current graph, but allows for a
	 * hierarchical limit. The trigger is set to true on the first pass through
	 * this method (when we've graphed one module) which has the effect of
	 * hiding any sub-modules
	 */
	private void pushSubgraph(Graph graph) {
		if (layersToGraph != 0) {
			layersToGraph -= 1;
			this.graph.addSubgraph(graph);
		}
	}

	@Override
	public void visit(AbsoluteMemoryRead block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(AbsoluteMemoryWrite block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(ArrayRead block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(ArrayWrite block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(Block block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(Branch branch) {
		_graph.ln("Visit: " + branch);
		ModuleGraph subgraph = new ModuleGraph(branch, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(branch);
	}

	@Override
	public void visit(Call block) {
		_graph.ln("Visit: " + block);
		BlackBoxGraph subgraph = new BlackBoxGraph(
				ComponentNode.getShortClassName(block) + " @"
						+ Integer.toHexString(block.hashCode()) + ": "
						+ ID.showLogical(block), nodeCount++, fontSize);

		subgraph.addComponent(block.getProcedure().getBody());
		subgraph.graphAddedComponents();
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(Decision decision) {
		_graph.ln("Visit: " + decision);
		ModuleGraph subgraph = new ModuleGraph(decision, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(decision);
	}

	@Override
	public void visit(Design design) {
		_graph.ln("Visit: " + design);

		Module designMod = design.getDesignModule();
		BlackBoxGraph designModuleGraph = new BlackBoxGraph(
				ID.showLogical(design) + "_designModule", designMod,
				nodeCount++, fontSize);
		Set<Component> moduleComps = new HashSet<Component>();
		for (Component comp : designMod.getComponents()) {
			designModuleGraph.addComponent(comp);
			if (comp instanceof Module) {
				moduleComps.add(comp);
			}
			nodeCount++;
		}
		designModuleGraph.graphAddedComponents();
		pushSubgraph(designModuleGraph);
		nodeCount = designModuleGraph.getNodeCount();

		for (Component comp : moduleComps) {
			Module mod = (Module) comp;
			doSubgraph(mod);
		}
		super.visit(design);
	}

	@Override
	public void visit(EndianSwapper endianSwapper) {
		_graph.ln("Visit: " + endianSwapper);
		ModuleGraph subgraph = new ModuleGraph(endianSwapper, nodeCount++,
				fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(endianSwapper);
	}

	@Override
	public void visit(FifoAccess block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(FifoRead block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(FifoWrite block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(ForBody body) {
		_graph.ln("Visit: " + body);
		ModuleGraph subgraph = new ModuleGraph(body, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(body);
	}

	@Override
	public void visit(HeapRead block) {
		_graph.ln("Visit: " + block);

		// ModuleGraph subgraph = new ModuleGraph(block, nodeCount++,fontSize);
		BlackBoxGraph subgraph = new BlackBoxGraph(
				ComponentNode.getShortClassName(block) + " @"
						+ Integer.toHexString(block.hashCode()) + ": "
						+ ID.showLogical(block), nodeCount++, fontSize);
		// System.out.println("HR COMPS: " + block.getComponents());
		subgraph.addComponents(block.getComponents());
		MemoryRead mr = block.getMemoryRead();
		if (mr.getPhysicalComponent() != null) {
			// System.out.println("Adding " + mr.getPhysicalComponent());
			subgraph.addComponent(mr.getPhysicalComponent());
		}
		subgraph.graphAddedComponents();

		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();

		super.visit(block);
	}

	@Override
	public void visit(HeapWrite block) {
		_graph.ln("Visit: " + block);
		// ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		// pushSubgraph(subgraph);
		// nodeCount = subgraph.getNodeCount();
		// visit(block.getMemoryWrite());

		BlackBoxGraph subgraph = new BlackBoxGraph(
				ComponentNode.getShortClassName(block) + " @"
						+ Integer.toHexString(block.hashCode()) + ": "
						+ ID.showLogical(block), nodeCount++, fontSize);
		subgraph.addComponents(block.getComponents());
		MemoryWrite mw = block.getMemoryWrite();
		if (mw.getPhysicalComponent() != null) {
			subgraph.addComponent(mw.getPhysicalComponent());
			for (Port p : mw.getPhysicalComponent().getDataPorts()) {
				Bus b = p.getBus();
				@SuppressWarnings("unused")
				Component owner = b == null ? null : b.getOwner().getOwner();
			}
		}
		subgraph.graphAddedComponents();

		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();

		super.visit(block);
	}

	@Override
	public void visit(Kicker block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(Latch block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(Loop loop) {
		_graph.ln("Visit: " + loop);
		ModuleGraph subgraph = new LoopGraph(loop, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(loop);
	}

	@Override
	public void visit(MemoryGateway block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(MemoryRead mr) {
		_graph.ln("Visit: " + mr);
		if (mr.getPhysicalComponent() != null) {
			ModuleGraph subgraph = new ModuleGraph(mr.getPhysicalComponent(),
					nodeCount++, fontSize);
			pushSubgraph(subgraph);
			nodeCount = subgraph.getNodeCount();
		}
	}

	@Override
	public void visit(MemoryReferee block) {
		_graph.ln("Visit: " + block);
		String id = ComponentNode.getShortClassName(block) + " @"
				+ Integer.toHexString(block.hashCode());
		id += ": " + block.showIDLogical();
		BlackBoxGraph subgraph = new BlackBoxGraph(id, nodeCount++, fontSize);

		for (Component comp : block.getComponents()) {
			subgraph.addComponent(comp);
		}

		subgraph.graphAddedComponents();
		graph.addSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(MemoryWrite mw) {
		_graph.ln("Visit: " + mw);
		if (mw.getPhysicalComponent() != null) {
			ModuleGraph subgraph = new ModuleGraph(mw.getPhysicalComponent(),
					nodeCount++, fontSize);
			pushSubgraph(subgraph);
			nodeCount = subgraph.getNodeCount();
		}
	}

	@Override
	public void visit(PinRead pread) {
		_graph.ln("Visit: " + pread);
		if (pread.getPhysicalComponent() != null) {
			ModuleGraph subgraph = new ModuleGraph(
					pread.getPhysicalComponent(), nodeCount++, fontSize);
			pushSubgraph(subgraph);
			nodeCount = subgraph.getNodeCount();
		}
		super.visit(pread);
	}

	@Override
	public void visit(PinReferee block) {
		_graph.ln("Visit: " + block);

		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		graph.addSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();

		super.visit(block);
	}

	@Override
	public void visit(PinWrite pwrite) {
		_graph.ln("Visit: " + pwrite);
		if (pwrite.getPhysicalComponent() != null) {
			ModuleGraph subgraph = new ModuleGraph(
					pwrite.getPhysicalComponent(), nodeCount++, fontSize);
			pushSubgraph(subgraph);
			nodeCount = subgraph.getNodeCount();
		}
		super.visit(pwrite);
	}

	@Override
	public void visit(PriorityMux pmux) {
		_graph.ln("Visit: " + pmux);
		ModuleGraph subgraph = new ModuleGraph(pmux, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(pmux);
	}

	@Override
	public void visit(RegisterGateway block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(RegisterReferee block) {
		_graph.ln("Visit: " + block);

		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		graph.addSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();

		super.visit(block);
	}

	@Override
	public void visit(SimplePinAccess block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(Switch swich) {
		_graph.ln("Visit: " + swich);
		ModuleGraph subgraph = new ModuleGraph(swich, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(swich);
	}

	@Override
	public void visit(TaskCall block) {
		_graph.ln("Visit: " + block);
		ModuleGraph subgraph = new ModuleGraph(block, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(block);
	}

	@Override
	public void visit(UntilBody body) {
		_graph.ln("Visit: " + body);
		ModuleGraph subgraph = new ModuleGraph(body, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(body);
	}

	@Override
	public void visit(WhileBody body) {
		_graph.ln("Visit: " + body);
		ModuleGraph subgraph = new ModuleGraph(body, nodeCount++, fontSize);
		pushSubgraph(subgraph);
		nodeCount = subgraph.getNodeCount();
		super.visit(body);
	}

}
