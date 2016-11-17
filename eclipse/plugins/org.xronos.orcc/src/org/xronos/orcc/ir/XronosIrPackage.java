/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */
package org.xronos.orcc.ir;

import net.sf.orcc.graph.GraphPackage;
import net.sf.orcc.ir.IrPackage;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see org.xronos.orcc.ir.XronosIrFactory
 * @model kind="package"
 * @generated
 */
public interface XronosIrPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "ir";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://www.xronos.org/ir";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "org.xronos.orcc.ir";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	XronosIrPackage eINSTANCE = org.xronos.orcc.ir.impl.XronosIrPackageImpl.init();

	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.InstPortStatusImpl <em>Inst Port Status</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.InstPortStatusImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstPortStatus()
	 * @generated
	 */
	int INST_PORT_STATUS = 0;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__ATTRIBUTES = IrPackage.INSTRUCTION__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__LINE_NUMBER = IrPackage.INSTRUCTION__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__PREDICATE = IrPackage.INSTRUCTION__PREDICATE;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__TARGET = IrPackage.INSTRUCTION_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__PORT = IrPackage.INSTRUCTION_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Inst Port Status</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS_FEATURE_COUNT = IrPackage.INSTRUCTION_FEATURE_COUNT + 2;

	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.InstPortReadImpl <em>Inst Port Read</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.InstPortReadImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstPortRead()
	 * @generated
	 */
	int INST_PORT_READ = 1;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__ATTRIBUTES = IrPackage.INSTRUCTION__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__LINE_NUMBER = IrPackage.INSTRUCTION__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__PREDICATE = IrPackage.INSTRUCTION__PREDICATE;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__TARGET = IrPackage.INSTRUCTION_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__PORT = IrPackage.INSTRUCTION_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Blocking</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__BLOCKING = IrPackage.INSTRUCTION_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Inst Port Read</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ_FEATURE_COUNT = IrPackage.INSTRUCTION_FEATURE_COUNT + 3;

	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.InstPortWriteImpl <em>Inst Port Write</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.InstPortWriteImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstPortWrite()
	 * @generated
	 */
	int INST_PORT_WRITE = 2;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__ATTRIBUTES = IrPackage.INSTRUCTION__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__LINE_NUMBER = IrPackage.INSTRUCTION__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__PREDICATE = IrPackage.INSTRUCTION__PREDICATE;

	/**
	 * The feature id for the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__PORT = IrPackage.INSTRUCTION_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__VALUE = IrPackage.INSTRUCTION_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Blocking</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__BLOCKING = IrPackage.INSTRUCTION_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Inst Port Write</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE_FEATURE_COUNT = IrPackage.INSTRUCTION_FEATURE_COUNT + 3;

	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.InstPortPeekImpl <em>Inst Port Peek</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.InstPortPeekImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstPortPeek()
	 * @generated
	 */
	int INST_PORT_PEEK = 3;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__ATTRIBUTES = IrPackage.INSTRUCTION__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__LINE_NUMBER = IrPackage.INSTRUCTION__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__PREDICATE = IrPackage.INSTRUCTION__PREDICATE;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__TARGET = IrPackage.INSTRUCTION_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__PORT = IrPackage.INSTRUCTION_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Inst Port Peek</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK_FEATURE_COUNT = IrPackage.INSTRUCTION_FEATURE_COUNT + 2;

	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.InstSimplePortWriteImpl <em>Inst Simple Port Write</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.InstSimplePortWriteImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstSimplePortWrite()
	 * @generated
	 */
	int INST_SIMPLE_PORT_WRITE = 4;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_SIMPLE_PORT_WRITE__ATTRIBUTES = IrPackage.INSTRUCTION__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_SIMPLE_PORT_WRITE__LINE_NUMBER = IrPackage.INSTRUCTION__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_SIMPLE_PORT_WRITE__PREDICATE = IrPackage.INSTRUCTION__PREDICATE;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_SIMPLE_PORT_WRITE__NAME = IrPackage.INSTRUCTION_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_SIMPLE_PORT_WRITE__VALUE = IrPackage.INSTRUCTION_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Inst Simple Port Write</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_SIMPLE_PORT_WRITE_FEATURE_COUNT = IrPackage.INSTRUCTION_FEATURE_COUNT + 2;

	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.BlockMutexImpl <em>Block Mutex</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.BlockMutexImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getBlockMutex()
	 * @generated
	 */
	int BLOCK_MUTEX = 5;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BLOCK_MUTEX__ATTRIBUTES = IrPackage.BLOCK__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Cfg Node</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BLOCK_MUTEX__CFG_NODE = IrPackage.BLOCK__CFG_NODE;

	/**
	 * The feature id for the '<em><b>Blocks</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BLOCK_MUTEX__BLOCKS = IrPackage.BLOCK_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Block Mutex</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BLOCK_MUTEX_FEATURE_COUNT = IrPackage.BLOCK_FEATURE_COUNT + 1;


	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.DfgImpl <em>Dfg</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.DfgImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getDfg()
	 * @generated
	 */
	int DFG = 6;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__ATTRIBUTES = GraphPackage.GRAPH__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Label</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__LABEL = GraphPackage.GRAPH__LABEL;

	/**
	 * The feature id for the '<em><b>Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__NUMBER = GraphPackage.GRAPH__NUMBER;

	/**
	 * The feature id for the '<em><b>Incoming</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__INCOMING = GraphPackage.GRAPH__INCOMING;

	/**
	 * The feature id for the '<em><b>Outgoing</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__OUTGOING = GraphPackage.GRAPH__OUTGOING;

	/**
	 * The feature id for the '<em><b>Connecting</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__CONNECTING = GraphPackage.GRAPH__CONNECTING;

	/**
	 * The feature id for the '<em><b>Predecessors</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__PREDECESSORS = GraphPackage.GRAPH__PREDECESSORS;

	/**
	 * The feature id for the '<em><b>Successors</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__SUCCESSORS = GraphPackage.GRAPH__SUCCESSORS;

	/**
	 * The feature id for the '<em><b>Neighbors</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__NEIGHBORS = GraphPackage.GRAPH__NEIGHBORS;

	/**
	 * The feature id for the '<em><b>Edges</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__EDGES = GraphPackage.GRAPH__EDGES;

	/**
	 * The feature id for the '<em><b>Vertices</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__VERTICES = GraphPackage.GRAPH__VERTICES;

	/**
	 * The feature id for the '<em><b>Operations</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__OPERATIONS = GraphPackage.GRAPH_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Dependencies</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG__DEPENDENCIES = GraphPackage.GRAPH_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Dfg</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_FEATURE_COUNT = GraphPackage.GRAPH_FEATURE_COUNT + 2;

	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.DfgVertexImpl <em>Dfg Vertex</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.DfgVertexImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getDfgVertex()
	 * @generated
	 */
	int DFG_VERTEX = 7;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__ATTRIBUTES = GraphPackage.VERTEX__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Label</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__LABEL = GraphPackage.VERTEX__LABEL;

	/**
	 * The feature id for the '<em><b>Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__NUMBER = GraphPackage.VERTEX__NUMBER;

	/**
	 * The feature id for the '<em><b>Incoming</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__INCOMING = GraphPackage.VERTEX__INCOMING;

	/**
	 * The feature id for the '<em><b>Outgoing</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__OUTGOING = GraphPackage.VERTEX__OUTGOING;

	/**
	 * The feature id for the '<em><b>Connecting</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__CONNECTING = GraphPackage.VERTEX__CONNECTING;

	/**
	 * The feature id for the '<em><b>Predecessors</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__PREDECESSORS = GraphPackage.VERTEX__PREDECESSORS;

	/**
	 * The feature id for the '<em><b>Successors</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__SUCCESSORS = GraphPackage.VERTEX__SUCCESSORS;

	/**
	 * The feature id for the '<em><b>Neighbors</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__NEIGHBORS = GraphPackage.VERTEX__NEIGHBORS;

	/**
	 * The feature id for the '<em><b>Node</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX__NODE = GraphPackage.VERTEX_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Dfg Vertex</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_VERTEX_FEATURE_COUNT = GraphPackage.VERTEX_FEATURE_COUNT + 1;

	/**
	 * The meta object id for the '{@link org.xronos.orcc.ir.impl.DfgEdgeImpl <em>Dfg Edge</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.xronos.orcc.ir.impl.DfgEdgeImpl
	 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getDfgEdge()
	 * @generated
	 */
	int DFG_EDGE = 8;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_EDGE__ATTRIBUTES = GraphPackage.EDGE__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Label</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_EDGE__LABEL = GraphPackage.EDGE__LABEL;

	/**
	 * The feature id for the '<em><b>Source</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_EDGE__SOURCE = GraphPackage.EDGE__SOURCE;

	/**
	 * The feature id for the '<em><b>Target</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_EDGE__TARGET = GraphPackage.EDGE__TARGET;

	/**
	 * The feature id for the '<em><b>Line</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_EDGE__LINE = GraphPackage.EDGE_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Dfg Edge</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DFG_EDGE_FEATURE_COUNT = GraphPackage.EDGE_FEATURE_COUNT + 1;


	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.InstPortStatus <em>Inst Port Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Port Status</em>'.
	 * @see org.xronos.orcc.ir.InstPortStatus
	 * @generated
	 */
	EClass getInstPortStatus();

	/**
	 * Returns the meta object for the containment reference '{@link org.xronos.orcc.ir.InstPortStatus#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see org.xronos.orcc.ir.InstPortStatus#getTarget()
	 * @see #getInstPortStatus()
	 * @generated
	 */
	EReference getInstPortStatus_Target();

	/**
	 * Returns the meta object for the reference '{@link org.xronos.orcc.ir.InstPortStatus#getPort <em>Port</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Port</em>'.
	 * @see org.xronos.orcc.ir.InstPortStatus#getPort()
	 * @see #getInstPortStatus()
	 * @generated
	 */
	EReference getInstPortStatus_Port();

	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.InstPortRead <em>Inst Port Read</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Port Read</em>'.
	 * @see org.xronos.orcc.ir.InstPortRead
	 * @generated
	 */
	EClass getInstPortRead();

	/**
	 * Returns the meta object for the containment reference '{@link org.xronos.orcc.ir.InstPortRead#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see org.xronos.orcc.ir.InstPortRead#getTarget()
	 * @see #getInstPortRead()
	 * @generated
	 */
	EReference getInstPortRead_Target();

	/**
	 * Returns the meta object for the reference '{@link org.xronos.orcc.ir.InstPortRead#getPort <em>Port</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Port</em>'.
	 * @see org.xronos.orcc.ir.InstPortRead#getPort()
	 * @see #getInstPortRead()
	 * @generated
	 */
	EReference getInstPortRead_Port();

	/**
	 * Returns the meta object for the attribute '{@link org.xronos.orcc.ir.InstPortRead#isBlocking <em>Blocking</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Blocking</em>'.
	 * @see org.xronos.orcc.ir.InstPortRead#isBlocking()
	 * @see #getInstPortRead()
	 * @generated
	 */
	EAttribute getInstPortRead_Blocking();

	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.InstPortWrite <em>Inst Port Write</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Port Write</em>'.
	 * @see org.xronos.orcc.ir.InstPortWrite
	 * @generated
	 */
	EClass getInstPortWrite();

	/**
	 * Returns the meta object for the reference '{@link org.xronos.orcc.ir.InstPortWrite#getPort <em>Port</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Port</em>'.
	 * @see org.xronos.orcc.ir.InstPortWrite#getPort()
	 * @see #getInstPortWrite()
	 * @generated
	 */
	EReference getInstPortWrite_Port();

	/**
	 * Returns the meta object for the containment reference '{@link org.xronos.orcc.ir.InstPortWrite#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Value</em>'.
	 * @see org.xronos.orcc.ir.InstPortWrite#getValue()
	 * @see #getInstPortWrite()
	 * @generated
	 */
	EReference getInstPortWrite_Value();

	/**
	 * Returns the meta object for the attribute '{@link org.xronos.orcc.ir.InstPortWrite#isBlocking <em>Blocking</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Blocking</em>'.
	 * @see org.xronos.orcc.ir.InstPortWrite#isBlocking()
	 * @see #getInstPortWrite()
	 * @generated
	 */
	EAttribute getInstPortWrite_Blocking();

	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.InstPortPeek <em>Inst Port Peek</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Port Peek</em>'.
	 * @see org.xronos.orcc.ir.InstPortPeek
	 * @generated
	 */
	EClass getInstPortPeek();

	/**
	 * Returns the meta object for the containment reference '{@link org.xronos.orcc.ir.InstPortPeek#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see org.xronos.orcc.ir.InstPortPeek#getTarget()
	 * @see #getInstPortPeek()
	 * @generated
	 */
	EReference getInstPortPeek_Target();

	/**
	 * Returns the meta object for the reference '{@link org.xronos.orcc.ir.InstPortPeek#getPort <em>Port</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Port</em>'.
	 * @see org.xronos.orcc.ir.InstPortPeek#getPort()
	 * @see #getInstPortPeek()
	 * @generated
	 */
	EReference getInstPortPeek_Port();

	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.InstSimplePortWrite <em>Inst Simple Port Write</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Simple Port Write</em>'.
	 * @see org.xronos.orcc.ir.InstSimplePortWrite
	 * @generated
	 */
	EClass getInstSimplePortWrite();

	/**
	 * Returns the meta object for the attribute '{@link org.xronos.orcc.ir.InstSimplePortWrite#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.xronos.orcc.ir.InstSimplePortWrite#getName()
	 * @see #getInstSimplePortWrite()
	 * @generated
	 */
	EAttribute getInstSimplePortWrite_Name();

	/**
	 * Returns the meta object for the containment reference '{@link org.xronos.orcc.ir.InstSimplePortWrite#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Value</em>'.
	 * @see org.xronos.orcc.ir.InstSimplePortWrite#getValue()
	 * @see #getInstSimplePortWrite()
	 * @generated
	 */
	EReference getInstSimplePortWrite_Value();

	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.BlockMutex <em>Block Mutex</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Block Mutex</em>'.
	 * @see org.xronos.orcc.ir.BlockMutex
	 * @generated
	 */
	EClass getBlockMutex();

	/**
	 * Returns the meta object for the containment reference list '{@link org.xronos.orcc.ir.BlockMutex#getBlocks <em>Blocks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Blocks</em>'.
	 * @see org.xronos.orcc.ir.BlockMutex#getBlocks()
	 * @see #getBlockMutex()
	 * @generated
	 */
	EReference getBlockMutex_Blocks();

	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.Dfg <em>Dfg</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Dfg</em>'.
	 * @see org.xronos.orcc.ir.Dfg
	 * @generated
	 */
	EClass getDfg();

	/**
	 * Returns the meta object for the reference list '{@link org.xronos.orcc.ir.Dfg#getOperations <em>Operations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Operations</em>'.
	 * @see org.xronos.orcc.ir.Dfg#getOperations()
	 * @see #getDfg()
	 * @generated
	 */
	EReference getDfg_Operations();

	/**
	 * Returns the meta object for the reference list '{@link org.xronos.orcc.ir.Dfg#getDependencies <em>Dependencies</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Dependencies</em>'.
	 * @see org.xronos.orcc.ir.Dfg#getDependencies()
	 * @see #getDfg()
	 * @generated
	 */
	EReference getDfg_Dependencies();

	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.DfgVertex <em>Dfg Vertex</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Dfg Vertex</em>'.
	 * @see org.xronos.orcc.ir.DfgVertex
	 * @generated
	 */
	EClass getDfgVertex();

	/**
	 * Returns the meta object for the reference '{@link org.xronos.orcc.ir.DfgVertex#getNode <em>Node</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Node</em>'.
	 * @see org.xronos.orcc.ir.DfgVertex#getNode()
	 * @see #getDfgVertex()
	 * @generated
	 */
	EReference getDfgVertex_Node();

	/**
	 * Returns the meta object for class '{@link org.xronos.orcc.ir.DfgEdge <em>Dfg Edge</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Dfg Edge</em>'.
	 * @see org.xronos.orcc.ir.DfgEdge
	 * @generated
	 */
	EClass getDfgEdge();

	/**
	 * Returns the meta object for the reference '{@link org.xronos.orcc.ir.DfgEdge#getLine <em>Line</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Line</em>'.
	 * @see org.xronos.orcc.ir.DfgEdge#getLine()
	 * @see #getDfgEdge()
	 * @generated
	 */
	EReference getDfgEdge_Line();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	XronosIrFactory getXronosIrFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.InstPortStatusImpl <em>Inst Port Status</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.InstPortStatusImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstPortStatus()
		 * @generated
		 */
		EClass INST_PORT_STATUS = eINSTANCE.getInstPortStatus();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_STATUS__TARGET = eINSTANCE.getInstPortStatus_Target();

		/**
		 * The meta object literal for the '<em><b>Port</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_STATUS__PORT = eINSTANCE.getInstPortStatus_Port();

		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.InstPortReadImpl <em>Inst Port Read</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.InstPortReadImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstPortRead()
		 * @generated
		 */
		EClass INST_PORT_READ = eINSTANCE.getInstPortRead();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_READ__TARGET = eINSTANCE.getInstPortRead_Target();

		/**
		 * The meta object literal for the '<em><b>Port</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_READ__PORT = eINSTANCE.getInstPortRead_Port();

		/**
		 * The meta object literal for the '<em><b>Blocking</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute INST_PORT_READ__BLOCKING = eINSTANCE.getInstPortRead_Blocking();

		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.InstPortWriteImpl <em>Inst Port Write</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.InstPortWriteImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstPortWrite()
		 * @generated
		 */
		EClass INST_PORT_WRITE = eINSTANCE.getInstPortWrite();

		/**
		 * The meta object literal for the '<em><b>Port</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_WRITE__PORT = eINSTANCE.getInstPortWrite_Port();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_WRITE__VALUE = eINSTANCE.getInstPortWrite_Value();

		/**
		 * The meta object literal for the '<em><b>Blocking</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute INST_PORT_WRITE__BLOCKING = eINSTANCE.getInstPortWrite_Blocking();

		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.InstPortPeekImpl <em>Inst Port Peek</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.InstPortPeekImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstPortPeek()
		 * @generated
		 */
		EClass INST_PORT_PEEK = eINSTANCE.getInstPortPeek();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_PEEK__TARGET = eINSTANCE.getInstPortPeek_Target();

		/**
		 * The meta object literal for the '<em><b>Port</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_PEEK__PORT = eINSTANCE.getInstPortPeek_Port();

		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.InstSimplePortWriteImpl <em>Inst Simple Port Write</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.InstSimplePortWriteImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getInstSimplePortWrite()
		 * @generated
		 */
		EClass INST_SIMPLE_PORT_WRITE = eINSTANCE.getInstSimplePortWrite();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute INST_SIMPLE_PORT_WRITE__NAME = eINSTANCE.getInstSimplePortWrite_Name();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_SIMPLE_PORT_WRITE__VALUE = eINSTANCE.getInstSimplePortWrite_Value();

		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.BlockMutexImpl <em>Block Mutex</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.BlockMutexImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getBlockMutex()
		 * @generated
		 */
		EClass BLOCK_MUTEX = eINSTANCE.getBlockMutex();

		/**
		 * The meta object literal for the '<em><b>Blocks</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference BLOCK_MUTEX__BLOCKS = eINSTANCE.getBlockMutex_Blocks();

		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.DfgImpl <em>Dfg</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.DfgImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getDfg()
		 * @generated
		 */
		EClass DFG = eINSTANCE.getDfg();

		/**
		 * The meta object literal for the '<em><b>Operations</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DFG__OPERATIONS = eINSTANCE.getDfg_Operations();

		/**
		 * The meta object literal for the '<em><b>Dependencies</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DFG__DEPENDENCIES = eINSTANCE.getDfg_Dependencies();

		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.DfgVertexImpl <em>Dfg Vertex</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.DfgVertexImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getDfgVertex()
		 * @generated
		 */
		EClass DFG_VERTEX = eINSTANCE.getDfgVertex();

		/**
		 * The meta object literal for the '<em><b>Node</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DFG_VERTEX__NODE = eINSTANCE.getDfgVertex_Node();

		/**
		 * The meta object literal for the '{@link org.xronos.orcc.ir.impl.DfgEdgeImpl <em>Dfg Edge</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.xronos.orcc.ir.impl.DfgEdgeImpl
		 * @see org.xronos.orcc.ir.impl.XronosIrPackageImpl#getDfgEdge()
		 * @generated
		 */
		EClass DFG_EDGE = eINSTANCE.getDfgEdge();

		/**
		 * The meta object literal for the '<em><b>Line</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DFG_EDGE__LINE = eINSTANCE.getDfgEdge_Line();

	}

} //XronosIrPackage
