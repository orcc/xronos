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
package org.xronos.orcc.ir.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.xronos.orcc.ir.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class XronosIrFactoryImpl extends EFactoryImpl implements XronosIrFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static XronosIrFactory init() {
		try {
			XronosIrFactory theXronosIrFactory = (XronosIrFactory)EPackage.Registry.INSTANCE.getEFactory(XronosIrPackage.eNS_URI);
			if (theXronosIrFactory != null) {
				return theXronosIrFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new XronosIrFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case XronosIrPackage.INST_PORT_STATUS: return createInstPortStatus();
			case XronosIrPackage.INST_PORT_READ: return createInstPortRead();
			case XronosIrPackage.INST_PORT_WRITE: return createInstPortWrite();
			case XronosIrPackage.INST_PORT_PEEK: return createInstPortPeek();
			case XronosIrPackage.INST_SIMPLE_PORT_WRITE: return createInstSimplePortWrite();
			case XronosIrPackage.BLOCK_MUTEX: return createBlockMutex();
			case XronosIrPackage.DFG: return createDfg();
			case XronosIrPackage.DFG_VERTEX: return createDfgVertex();
			case XronosIrPackage.DFG_EDGE: return createDfgEdge();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstPortStatus createInstPortStatus() {
		InstPortStatusImpl instPortStatus = new InstPortStatusImpl();
		return instPortStatus;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstPortRead createInstPortRead() {
		InstPortReadImpl instPortRead = new InstPortReadImpl();
		return instPortRead;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstPortWrite createInstPortWrite() {
		InstPortWriteImpl instPortWrite = new InstPortWriteImpl();
		return instPortWrite;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstPortPeek createInstPortPeek() {
		InstPortPeekImpl instPortPeek = new InstPortPeekImpl();
		return instPortPeek;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstSimplePortWrite createInstSimplePortWrite() {
		InstSimplePortWriteImpl instSimplePortWrite = new InstSimplePortWriteImpl();
		return instSimplePortWrite;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public BlockMutex createBlockMutex() {
		BlockMutexImpl blockMutex = new BlockMutexImpl();
		return blockMutex;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Dfg createDfg() {
		DfgImpl dfg = new DfgImpl();
		return dfg;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public DfgVertex createDfgVertex() {
		DfgVertexImpl dfgVertex = new DfgVertexImpl();
		return dfgVertex;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public DfgEdge createDfgEdge() {
		DfgEdgeImpl dfgEdge = new DfgEdgeImpl();
		return dfgEdge;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrPackage getXronosIrPackage() {
		return (XronosIrPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static XronosIrPackage getPackage() {
		return XronosIrPackage.eINSTANCE;
	}

} //XronosIrFactoryImpl
