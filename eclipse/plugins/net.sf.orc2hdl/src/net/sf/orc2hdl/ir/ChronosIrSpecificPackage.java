/**
 */
package net.sf.orc2hdl.ir;

import net.sf.orcc.ir.IrPackage;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see net.sf.orc2hdl.ir.ChronosIrSpecificFactory
 * @model kind="package"
 * @generated
 */
public interface ChronosIrSpecificPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "ir";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://orcc.sf.net/backends/ir";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "net.sf.orc2hdl.ir";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	ChronosIrSpecificPackage eINSTANCE = net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl.init();

	/**
	 * The meta object id for the '{@link net.sf.orc2hdl.ir.impl.InstPortStatusImpl <em>Inst Port Status</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.orc2hdl.ir.impl.InstPortStatusImpl
	 * @see net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl#getInstPortStatus()
	 * @generated
	 */
	int INST_PORT_STATUS = 0;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__ATTRIBUTES = IrPackage.INST_SPECIFIC__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__LINE_NUMBER = IrPackage.INST_SPECIFIC__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__PREDICATE = IrPackage.INST_SPECIFIC__PREDICATE;

	/**
	 * The feature id for the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__PORT = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS__TARGET = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Inst Port Status</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_STATUS_FEATURE_COUNT = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 2;

	/**
	 * The meta object id for the '{@link net.sf.orc2hdl.ir.impl.InstPortReadImpl <em>Inst Port Read</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.orc2hdl.ir.impl.InstPortReadImpl
	 * @see net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl#getInstPortRead()
	 * @generated
	 */
	int INST_PORT_READ = 1;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__ATTRIBUTES = IrPackage.INST_SPECIFIC__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__LINE_NUMBER = IrPackage.INST_SPECIFIC__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__PREDICATE = IrPackage.INST_SPECIFIC__PREDICATE;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__TARGET = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ__PORT = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Inst Port Read</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_READ_FEATURE_COUNT = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 2;

	/**
	 * The meta object id for the '{@link net.sf.orc2hdl.ir.impl.InstPortWriteImpl <em>Inst Port Write</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.orc2hdl.ir.impl.InstPortWriteImpl
	 * @see net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl#getInstPortWrite()
	 * @generated
	 */
	int INST_PORT_WRITE = 2;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__ATTRIBUTES = IrPackage.INST_SPECIFIC__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__LINE_NUMBER = IrPackage.INST_SPECIFIC__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__PREDICATE = IrPackage.INST_SPECIFIC__PREDICATE;

	/**
	 * The feature id for the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__PORT = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE__VALUE = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Inst Port Write</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_WRITE_FEATURE_COUNT = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 2;

	/**
	 * The meta object id for the '{@link net.sf.orc2hdl.ir.impl.InstPortPeekImpl <em>Inst Port Peek</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.orc2hdl.ir.impl.InstPortPeekImpl
	 * @see net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl#getInstPortPeek()
	 * @generated
	 */
	int INST_PORT_PEEK = 3;

	/**
	 * The feature id for the '<em><b>Attributes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__ATTRIBUTES = IrPackage.INST_SPECIFIC__ATTRIBUTES;

	/**
	 * The feature id for the '<em><b>Line Number</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__LINE_NUMBER = IrPackage.INST_SPECIFIC__LINE_NUMBER;

	/**
	 * The feature id for the '<em><b>Predicate</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__PREDICATE = IrPackage.INST_SPECIFIC__PREDICATE;

	/**
	 * The feature id for the '<em><b>Port</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__PORT = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK__TARGET = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Inst Port Peek</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INST_PORT_PEEK_FEATURE_COUNT = IrPackage.INST_SPECIFIC_FEATURE_COUNT + 2;


	/**
	 * Returns the meta object for class '{@link net.sf.orc2hdl.ir.InstPortStatus <em>Inst Port Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Port Status</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortStatus
	 * @generated
	 */
	EClass getInstPortStatus();

	/**
	 * Returns the meta object for the containment reference '{@link net.sf.orc2hdl.ir.InstPortStatus#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortStatus#getTarget()
	 * @see #getInstPortStatus()
	 * @generated
	 */
	EReference getInstPortStatus_Target();

	/**
	 * Returns the meta object for the reference '{@link net.sf.orc2hdl.ir.InstPortStatus#getPort <em>Port</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Port</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortStatus#getPort()
	 * @see #getInstPortStatus()
	 * @generated
	 */
	EReference getInstPortStatus_Port();

	/**
	 * Returns the meta object for class '{@link net.sf.orc2hdl.ir.InstPortRead <em>Inst Port Read</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Port Read</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortRead
	 * @generated
	 */
	EClass getInstPortRead();

	/**
	 * Returns the meta object for the reference '{@link net.sf.orc2hdl.ir.InstPortRead#getPort <em>Port</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Port</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortRead#getPort()
	 * @see #getInstPortRead()
	 * @generated
	 */
	EReference getInstPortRead_Port();

	/**
	 * Returns the meta object for the containment reference '{@link net.sf.orc2hdl.ir.InstPortRead#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortRead#getTarget()
	 * @see #getInstPortRead()
	 * @generated
	 */
	EReference getInstPortRead_Target();

	/**
	 * Returns the meta object for class '{@link net.sf.orc2hdl.ir.InstPortWrite <em>Inst Port Write</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Port Write</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortWrite
	 * @generated
	 */
	EClass getInstPortWrite();

	/**
	 * Returns the meta object for the reference '{@link net.sf.orc2hdl.ir.InstPortWrite#getPort <em>Port</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Port</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortWrite#getPort()
	 * @see #getInstPortWrite()
	 * @generated
	 */
	EReference getInstPortWrite_Port();

	/**
	 * Returns the meta object for the containment reference '{@link net.sf.orc2hdl.ir.InstPortWrite#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Value</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortWrite#getValue()
	 * @see #getInstPortWrite()
	 * @generated
	 */
	EReference getInstPortWrite_Value();

	/**
	 * Returns the meta object for class '{@link net.sf.orc2hdl.ir.InstPortPeek <em>Inst Port Peek</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inst Port Peek</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortPeek
	 * @generated
	 */
	EClass getInstPortPeek();

	/**
	 * Returns the meta object for the reference '{@link net.sf.orc2hdl.ir.InstPortPeek#getPort <em>Port</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Port</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortPeek#getPort()
	 * @see #getInstPortPeek()
	 * @generated
	 */
	EReference getInstPortPeek_Port();

	/**
	 * Returns the meta object for the containment reference '{@link net.sf.orc2hdl.ir.InstPortPeek#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see net.sf.orc2hdl.ir.InstPortPeek#getTarget()
	 * @see #getInstPortPeek()
	 * @generated
	 */
	EReference getInstPortPeek_Target();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	ChronosIrSpecificFactory getChronosIrSpecificFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link net.sf.orc2hdl.ir.impl.InstPortStatusImpl <em>Inst Port Status</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.orc2hdl.ir.impl.InstPortStatusImpl
		 * @see net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl#getInstPortStatus()
		 * @generated
		 */
		EClass INST_PORT_STATUS = eINSTANCE.getInstPortStatus();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_STATUS__TARGET = eINSTANCE.getInstPortStatus_Target();

		/**
		 * The meta object literal for the '<em><b>Port</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_STATUS__PORT = eINSTANCE.getInstPortStatus_Port();

		/**
		 * The meta object literal for the '{@link net.sf.orc2hdl.ir.impl.InstPortReadImpl <em>Inst Port Read</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.orc2hdl.ir.impl.InstPortReadImpl
		 * @see net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl#getInstPortRead()
		 * @generated
		 */
		EClass INST_PORT_READ = eINSTANCE.getInstPortRead();

		/**
		 * The meta object literal for the '<em><b>Port</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_READ__PORT = eINSTANCE.getInstPortRead_Port();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_READ__TARGET = eINSTANCE.getInstPortRead_Target();

		/**
		 * The meta object literal for the '{@link net.sf.orc2hdl.ir.impl.InstPortWriteImpl <em>Inst Port Write</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.orc2hdl.ir.impl.InstPortWriteImpl
		 * @see net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl#getInstPortWrite()
		 * @generated
		 */
		EClass INST_PORT_WRITE = eINSTANCE.getInstPortWrite();

		/**
		 * The meta object literal for the '<em><b>Port</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_WRITE__PORT = eINSTANCE.getInstPortWrite_Port();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_WRITE__VALUE = eINSTANCE.getInstPortWrite_Value();

		/**
		 * The meta object literal for the '{@link net.sf.orc2hdl.ir.impl.InstPortPeekImpl <em>Inst Port Peek</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.orc2hdl.ir.impl.InstPortPeekImpl
		 * @see net.sf.orc2hdl.ir.impl.ChronosIrSpecificPackageImpl#getInstPortPeek()
		 * @generated
		 */
		EClass INST_PORT_PEEK = eINSTANCE.getInstPortPeek();

		/**
		 * The meta object literal for the '<em><b>Port</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_PEEK__PORT = eINSTANCE.getInstPortPeek_Port();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference INST_PORT_PEEK__TARGET = eINSTANCE.getInstPortPeek_Target();

	}

} //ChronosIrSpecificPackage
