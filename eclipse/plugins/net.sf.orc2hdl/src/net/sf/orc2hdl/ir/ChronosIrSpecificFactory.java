/**
 */
package net.sf.orc2hdl.ir;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see net.sf.orc2hdl.ir.ChronosIrSpecificPackage
 * @generated
 */
public interface ChronosIrSpecificFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	ChronosIrSpecificFactory eINSTANCE = net.sf.orc2hdl.ir.impl.ChronosIrSpecificFactoryImpl.init();

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
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	ChronosIrSpecificPackage getChronosIrSpecificPackage();

} //ChronosIrSpecificFactory
