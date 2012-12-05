/**
 */
package org.xronos.orcc.ir;

import net.sf.orcc.ir.Block;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Block Mutex</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.BlockMutex#getBlocks <em>Blocks</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.xronos.orcc.ir.XronosIrPackage#getBlockMutex()
 * @model
 * @generated
 */
public interface BlockMutex extends Block {
	/**
	 * Returns the value of the '<em><b>Blocks</b></em>' containment reference list.
	 * The list contents are of type {@link net.sf.orcc.ir.Block}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Blocks</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Blocks</em>' containment reference list.
	 * @see org.xronos.orcc.ir.XronosIrPackage#getBlockMutex_Blocks()
	 * @model containment="true"
	 * @generated
	 */
	EList<Block> getBlocks();

} // BlockMutex
