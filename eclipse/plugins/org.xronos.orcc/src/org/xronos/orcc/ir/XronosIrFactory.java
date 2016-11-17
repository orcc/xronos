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

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.xronos.orcc.ir.XronosIrPackage
 * @generated
 */
public interface XronosIrFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	XronosIrFactory eINSTANCE = org.xronos.orcc.ir.impl.XronosIrFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Inst Port Status</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Port Status</em>'.
	 * @generated
	 */
	InstPortStatus createInstPortStatus();

	/**
	 * Returns a new object of class '<em>Inst Port Read</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Port Read</em>'.
	 * @generated
	 */
	InstPortRead createInstPortRead();

	/**
	 * Returns a new object of class '<em>Inst Port Write</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Port Write</em>'.
	 * @generated
	 */
	InstPortWrite createInstPortWrite();

	/**
	 * Returns a new object of class '<em>Inst Port Peek</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Port Peek</em>'.
	 * @generated
	 */
	InstPortPeek createInstPortPeek();

	/**
	 * Returns a new object of class '<em>Inst Simple Port Write</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Simple Port Write</em>'.
	 * @generated
	 */
	InstSimplePortWrite createInstSimplePortWrite();

	/**
	 * Returns a new object of class '<em>Block Mutex</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Block Mutex</em>'.
	 * @generated
	 */
	BlockMutex createBlockMutex();

	/**
	 * Returns a new object of class '<em>Dfg</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Dfg</em>'.
	 * @generated
	 */
	Dfg createDfg();

	/**
	 * Returns a new object of class '<em>Dfg Vertex</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Dfg Vertex</em>'.
	 * @generated
	 */
	DfgVertex createDfgVertex();

	/**
	 * Returns a new object of class '<em>Dfg Edge</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Dfg Edge</em>'.
	 * @generated
	 */
	DfgEdge createDfgEdge();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	XronosIrPackage getXronosIrPackage();

} //XronosIrFactory
