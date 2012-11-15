/**
 */
package net.sf.orc2hdl.ir;

import net.sf.orcc.graph.Vertex;

import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstSpecific;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Inst Port Write</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link net.sf.orc2hdl.ir.InstPortWrite#getPort <em>Port</em>}</li>
 *   <li>{@link net.sf.orc2hdl.ir.InstPortWrite#getValue <em>Value</em>}</li>
 * </ul>
 * </p>
 *
 * @see net.sf.orc2hdl.ir.XronosIrSpecificPackage#getInstPortWrite()
 * @model
 * @generated
 */
public interface InstPortWrite extends InstSpecific {
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
	 * @see net.sf.orc2hdl.ir.XronosIrSpecificPackage#getInstPortWrite_Port()
	 * @model resolveProxies="false"
	 * @generated
	 */
	Vertex getPort();

	/**
	 * Sets the value of the '{@link net.sf.orc2hdl.ir.InstPortWrite#getPort <em>Port</em>}' reference.
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
	 * @see net.sf.orc2hdl.ir.XronosIrSpecificPackage#getInstPortWrite_Value()
	 * @model containment="true"
	 * @generated
	 */
	Expression getValue();

	/**
	 * Sets the value of the '{@link net.sf.orc2hdl.ir.InstPortWrite#getValue <em>Value</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Value</em>' containment reference.
	 * @see #getValue()
	 * @generated
	 */
	void setValue(Expression value);

} // InstPortWrite
