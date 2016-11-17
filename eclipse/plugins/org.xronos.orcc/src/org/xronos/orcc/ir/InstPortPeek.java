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

import net.sf.orcc.graph.Vertex;

import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.Instruction;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Inst Port Peek</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.InstPortPeek#getTarget <em>Target</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.InstPortPeek#getPort <em>Port</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortPeek()
 * @model
 * @generated
 */
public interface InstPortPeek extends Instruction {
	/**
	 * Returns the value of the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Target</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target</em>' containment reference.
	 * @see #setTarget(Def)
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortPeek_Target()
	 * @model containment="true"
	 * @generated
	 */
	Def getTarget();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.InstPortPeek#getTarget <em>Target</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' containment reference.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(Def value);

	/**
	 * Returns the value of the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Port</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Port</em>' reference.
	 * @see #setPort(Vertex)
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortPeek_Port()
	 * @model resolveProxies="false"
	 * @generated
	 */
	Vertex getPort();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.InstPortPeek#getPort <em>Port</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Port</em>' reference.
	 * @see #getPort()
	 * @generated
	 */
	void setPort(Vertex value);

} // InstPortPeek
