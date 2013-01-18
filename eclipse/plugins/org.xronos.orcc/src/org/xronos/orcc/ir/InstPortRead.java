/**
 */
package org.xronos.orcc.ir;

import net.sf.orcc.graph.Vertex;

import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.Instruction;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Inst Port Read</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.InstPortRead#getTarget <em>Target</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.InstPortRead#getPort <em>Port</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.InstPortRead#isBlocking <em>Blocking</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortRead()
 * @model
 * @generated
 */
public interface InstPortRead extends Instruction {
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
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortRead_Target()
	 * @model containment="true"
	 * @generated
	 */
	Def getTarget();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.InstPortRead#getTarget <em>Target</em>}' containment reference.
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
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortRead_Port()
	 * @model resolveProxies="false"
	 * @generated
	 */
	Vertex getPort();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.InstPortRead#getPort <em>Port</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Port</em>' reference.
	 * @see #getPort()
	 * @generated
	 */
	void setPort(Vertex value);

	/**
	 * Returns the value of the '<em><b>Blocking</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Blocking</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Blocking</em>' attribute.
	 * @see #setBlocking(boolean)
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortRead_Blocking()
	 * @model default="false"
	 * @generated
	 */
	boolean isBlocking();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.InstPortRead#isBlocking <em>Blocking</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Blocking</em>' attribute.
	 * @see #isBlocking()
	 * @generated
	 */
	void setBlocking(boolean value);

} // InstPortRead
