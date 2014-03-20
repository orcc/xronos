/**
 */
package org.xronos.orcc.ir;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.xronos.orcc.ir.XronosIrPackage
 * @generated
 */
public interface XronosIrFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	XronosIrFactory eINSTANCE = org.xronos.orcc.ir.impl.XronosIrFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Inst Port Status</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Port Status</em>'.
	 * @generated
	 */
	InstPortStatus createInstPortStatus();

	/**
	 * Returns a new object of class '<em>Inst Port Read</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Port Read</em>'.
	 * @generated
	 */
	InstPortRead createInstPortRead();

	/**
	 * Returns a new object of class '<em>Inst Port Write</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Port Write</em>'.
	 * @generated
	 */
	InstPortWrite createInstPortWrite();

	/**
	 * Returns a new object of class '<em>Inst Port Peek</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Port Peek</em>'.
	 * @generated
	 */
	InstPortPeek createInstPortPeek();

	/**
	 * Returns a new object of class '<em>Inst Simple Port Write</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Inst Simple Port Write</em>'.
	 * @generated
	 */
	InstSimplePortWrite createInstSimplePortWrite();

	/**
	 * Returns a new object of class '<em>Block Mutex</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Block Mutex</em>'.
	 * @generated
	 */
	BlockMutex createBlockMutex();

	/**
	 * Returns a new object of class '<em>Dfg</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Dfg</em>'.
	 * @generated
	 */
	Dfg createDfg();

	/**
	 * Returns a new object of class '<em>Dfg Vertex</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Dfg Vertex</em>'.
	 * @generated
	 */
	DfgVertex createDfgVertex();

	/**
	 * Returns a new object of class '<em>Dfg Edge</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Dfg Edge</em>'.
	 * @generated
	 */
	DfgEdge createDfgEdge();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	XronosIrPackage getXronosIrPackage();

} //XronosIrFactory
