/**
 */
package org.xronos.orcc.ir;

import net.sf.orcc.graph.Edge;

import net.sf.orcc.ir.Expression;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Dfg Edge</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.DfgEdge#getLine <em>Line</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.xronos.orcc.ir.XronosIrPackage#getDfgEdge()
 * @model
 * @generated
 */
public interface DfgEdge extends Edge {
	/**
	 * Returns the value of the '<em><b>Line</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Line</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Line</em>' reference.
	 * @see #setLine(Expression)
	 * @see org.xronos.orcc.ir.XronosIrPackage#getDfgEdge_Line()
	 * @model
	 * @generated
	 */
	Expression getLine();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.DfgEdge#getLine <em>Line</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Line</em>' reference.
	 * @see #getLine()
	 * @generated
	 */
	void setLine(Expression value);

} // DfgEdge
