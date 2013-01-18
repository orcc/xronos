/**
 */
package org.xronos.orcc.ir;

import net.sf.orcc.graph.Vertex;

import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Instruction;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Inst Port Write</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.InstPortWrite#getPort <em>Port</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.InstPortWrite#getValue <em>Value</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.InstPortWrite#isBlocking <em>Blocking</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortWrite()
 * @model
 * @generated
 */
public interface InstPortWrite extends Instruction {
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
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortWrite_Port()
	 * @model resolveProxies="false"
	 * @generated
	 */
	Vertex getPort();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.InstPortWrite#getPort <em>Port</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Port</em>' reference.
	 * @see #getPort()
	 * @generated
	 */
	void setPort(Vertex value);

	/**
	 * Returns the value of the '<em><b>Value</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Value</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Value</em>' containment reference.
	 * @see #setValue(Expression)
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortWrite_Value()
	 * @model containment="true"
	 * @generated
	 */
	Expression getValue();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.InstPortWrite#getValue <em>Value</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Value</em>' containment reference.
	 * @see #getValue()
	 * @generated
	 */
	void setValue(Expression value);

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
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstPortWrite_Blocking()
	 * @model default="false"
	 * @generated
	 */
	boolean isBlocking();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.InstPortWrite#isBlocking <em>Blocking</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Blocking</em>' attribute.
	 * @see #isBlocking()
	 * @generated
	 */
	void setBlocking(boolean value);

} // InstPortWrite
