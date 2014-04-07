/**
 */
package org.xronos.orcc.ir;

import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Instruction;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Inst Simple Port Write</b></em>'. <!-- end-user-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.xronos.orcc.ir.InstSimplePortWrite#getName <em>Name</em>}</li>
 * <li>{@link org.xronos.orcc.ir.InstSimplePortWrite#getValue <em>Value</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.xronos.orcc.ir.XronosIrPackage#getInstSimplePortWrite()
 * @model
 * @generated
 */
public interface InstSimplePortWrite extends Instruction {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute. <!--
	 * begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear, there really
	 * should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstSimplePortWrite_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Returns the value of the '<em><b>Value</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Value</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Value</em>' containment reference.
	 * @see #setValue(Expression)
	 * @see org.xronos.orcc.ir.XronosIrPackage#getInstSimplePortWrite_Value()
	 * @model containment="true"
	 * @generated
	 */
	Expression getValue();

	/**
	 * Sets the value of the '
	 * {@link org.xronos.orcc.ir.InstSimplePortWrite#getName <em>Name</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Sets the value of the '
	 * {@link org.xronos.orcc.ir.InstSimplePortWrite#getValue <em>Value</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Value</em>' containment reference.
	 * @see #getValue()
	 * @generated
	 */
	void setValue(Expression value);

} // InstSimplePortWrite
