/**
 */
package org.xronos.orcc.ir;

import net.sf.orcc.graph.Graph;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Dfg</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.Dfg#getOperations <em>Operations</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.Dfg#getDependencies <em>Dependencies</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.xronos.orcc.ir.XronosIrPackage#getDfg()
 * @model
 * @generated
 */
public interface Dfg extends Graph {
	/**
	 * Returns the value of the '<em><b>Operations</b></em>' reference list.
	 * The list contents are of type {@link org.xronos.orcc.ir.DfgVertex}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Operations</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Operations</em>' reference list.
	 * @see org.xronos.orcc.ir.XronosIrPackage#getDfg_Operations()
	 * @model
	 * @generated
	 */
	EList<DfgVertex> getOperations();

	/**
	 * Returns the value of the '<em><b>Dependencies</b></em>' reference list.
	 * The list contents are of type {@link org.xronos.orcc.ir.DfgEdge}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Dependencies</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Dependencies</em>' reference list.
	 * @see org.xronos.orcc.ir.XronosIrPackage#getDfg_Dependencies()
	 * @model
	 * @generated
	 */
	EList<DfgEdge> getDependencies();

} // Dfg
