/**
 */
package net.sf.orc2hdl.ir.impl;

import net.sf.orc2hdl.ir.InstPortPeek;
import net.sf.orc2hdl.ir.InstPortRead;
import net.sf.orc2hdl.ir.InstPortStatus;
import net.sf.orc2hdl.ir.InstPortWrite;
import net.sf.orc2hdl.ir.XronosIrSpecificFactory;
import net.sf.orc2hdl.ir.XronosIrSpecificPackage;

import net.sf.orcc.graph.GraphPackage;

import net.sf.orcc.ir.IrPackage;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class XronosIrSpecificPackageImpl extends EPackageImpl implements XronosIrSpecificPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass instPortStatusEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass instPortReadEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass instPortWriteEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass instPortPeekEClass = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see net.sf.orc2hdl.ir.XronosIrSpecificPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private XronosIrSpecificPackageImpl() {
		super(eNS_URI, XronosIrSpecificFactory.eINSTANCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 * 
	 * <p>This method is used to initialize {@link XronosIrSpecificPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static XronosIrSpecificPackage init() {
		if (isInited) return (XronosIrSpecificPackage)EPackage.Registry.INSTANCE.getEPackage(XronosIrSpecificPackage.eNS_URI);

		// Obtain or create and register package
		XronosIrSpecificPackageImpl theXronosIrSpecificPackage = (XronosIrSpecificPackageImpl)(EPackage.Registry.INSTANCE.get(eNS_URI) instanceof XronosIrSpecificPackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI) : new XronosIrSpecificPackageImpl());

		isInited = true;

		// Initialize simple dependencies
		IrPackage.eINSTANCE.eClass();

		// Create package meta-data objects
		theXronosIrSpecificPackage.createPackageContents();

		// Initialize created meta-data
		theXronosIrSpecificPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theXronosIrSpecificPackage.freeze();

  
		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(XronosIrSpecificPackage.eNS_URI, theXronosIrSpecificPackage);
		return theXronosIrSpecificPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getInstPortStatus() {
		return instPortStatusEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstPortStatus_Target() {
		return (EReference)instPortStatusEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstPortStatus_Port() {
		return (EReference)instPortStatusEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getInstPortRead() {
		return instPortReadEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstPortRead_Target() {
		return (EReference)instPortReadEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstPortRead_Port() {
		return (EReference)instPortReadEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getInstPortWrite() {
		return instPortWriteEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstPortWrite_Port() {
		return (EReference)instPortWriteEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstPortWrite_Value() {
		return (EReference)instPortWriteEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getInstPortPeek() {
		return instPortPeekEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstPortPeek_Target() {
		return (EReference)instPortPeekEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstPortPeek_Port() {
		return (EReference)instPortPeekEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrSpecificFactory getXronosIrSpecificFactory() {
		return (XronosIrSpecificFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		instPortStatusEClass = createEClass(INST_PORT_STATUS);
		createEReference(instPortStatusEClass, INST_PORT_STATUS__TARGET);
		createEReference(instPortStatusEClass, INST_PORT_STATUS__PORT);

		instPortReadEClass = createEClass(INST_PORT_READ);
		createEReference(instPortReadEClass, INST_PORT_READ__TARGET);
		createEReference(instPortReadEClass, INST_PORT_READ__PORT);

		instPortWriteEClass = createEClass(INST_PORT_WRITE);
		createEReference(instPortWriteEClass, INST_PORT_WRITE__PORT);
		createEReference(instPortWriteEClass, INST_PORT_WRITE__VALUE);

		instPortPeekEClass = createEClass(INST_PORT_PEEK);
		createEReference(instPortPeekEClass, INST_PORT_PEEK__TARGET);
		createEReference(instPortPeekEClass, INST_PORT_PEEK__PORT);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Obtain other dependent packages
		IrPackage theIrPackage = (IrPackage)EPackage.Registry.INSTANCE.getEPackage(IrPackage.eNS_URI);
		GraphPackage theGraphPackage = (GraphPackage)EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		instPortStatusEClass.getESuperTypes().add(theIrPackage.getInstSpecific());
		instPortReadEClass.getESuperTypes().add(theIrPackage.getInstSpecific());
		instPortWriteEClass.getESuperTypes().add(theIrPackage.getInstSpecific());
		instPortPeekEClass.getESuperTypes().add(theIrPackage.getInstSpecific());

		// Initialize classes and features; add operations and parameters
		initEClass(instPortStatusEClass, InstPortStatus.class, "InstPortStatus", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getInstPortStatus_Target(), theIrPackage.getDef(), null, "target", null, 0, 1, InstPortStatus.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstPortStatus_Port(), theGraphPackage.getVertex(), null, "port", null, 0, 1, InstPortStatus.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(instPortReadEClass, InstPortRead.class, "InstPortRead", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getInstPortRead_Target(), theIrPackage.getDef(), null, "target", null, 0, 1, InstPortRead.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstPortRead_Port(), theGraphPackage.getVertex(), null, "port", null, 0, 1, InstPortRead.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(instPortWriteEClass, InstPortWrite.class, "InstPortWrite", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getInstPortWrite_Port(), theGraphPackage.getVertex(), null, "port", null, 0, 1, InstPortWrite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstPortWrite_Value(), theIrPackage.getExpression(), null, "value", null, 0, 1, InstPortWrite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(instPortPeekEClass, InstPortPeek.class, "InstPortPeek", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getInstPortPeek_Target(), theIrPackage.getDef(), null, "target", null, 0, 1, InstPortPeek.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstPortPeek_Port(), theGraphPackage.getVertex(), null, "port", null, 0, 1, InstPortPeek.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Create resource
		createResource(eNS_URI);
	}

} //XronosIrSpecificPackageImpl
