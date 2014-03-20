/**
 */
package org.xronos.orcc.ir;

import net.sf.orcc.graph.Vertex;

import net.sf.orcc.ir.Instruction;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Dfg Vertex</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.DfgVertex#getNode <em>Node</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.xronos.orcc.ir.XronosIrPackage#getDfgVertex()
 * @model
 * @generated
 */
public interface DfgVertex extends Vertex {
	/**
	 * Returns the value of the '<em><b>Node</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Node</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Node</em>' reference.
	 * @see #setNode(Instruction)
	 * @see org.xronos.orcc.ir.XronosIrPackage#getDfgVertex_Node()
	 * @model
	 * @generated
	 */
	Instruction getNode();

	/**
	 * Sets the value of the '{@link org.xronos.orcc.ir.DfgVertex#getNode <em>Node</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Node</em>' reference.
	 * @see #getNode()
	 * @generated
	 */
	void setNode(Instruction value);

} // DfgVertex
