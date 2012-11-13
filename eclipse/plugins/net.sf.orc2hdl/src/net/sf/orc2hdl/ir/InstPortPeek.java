/**
 */
package net.sf.orc2hdl.ir;

import net.sf.orcc.graph.Vertex;

import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.InstSpecific;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Inst Port Peek</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link net.sf.orc2hdl.ir.InstPortPeek#getPort <em>Port</em>}</li>
 *   <li>{@link net.sf.orc2hdl.ir.InstPortPeek#getTarget <em>Target</em>}</li>
 * </ul>
 * </p>
 *
 * @see net.sf.orc2hdl.ir.ChronosIrSpecificPackage#getInstPortPeek()
 * @model
 * @generated
 */
public interface InstPortPeek extends InstSpecific {
	/**
	 * Returns the value of the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Port</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Port</em>' reference.
	 * @see #setPort(Vertex)
	 * @see net.sf.orc2hdl.ir.ChronosIrSpecificPackage#getInstPortPeek_Port()
	 * @model resolveProxies="false"
	 * @generated
	 */
	Vertex getPort();

	/**
	 * Sets the value of the '{@link net.sf.orc2hdl.ir.InstPortPeek#getPort <em>Port</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Port</em>' reference.
	 * @see #getPort()
	 * @generated
	 */
	void setPort(Vertex value);

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
	 * @see net.sf.orc2hdl.ir.ChronosIrSpecificPackage#getInstPortPeek_Target()
	 * @model containment="true"
	 * @generated
	 */
	Def getTarget();

	/**
	 * Sets the value of the '{@link net.sf.orc2hdl.ir.InstPortPeek#getTarget <em>Target</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' containment reference.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(Def value);

} // InstPortPeek
