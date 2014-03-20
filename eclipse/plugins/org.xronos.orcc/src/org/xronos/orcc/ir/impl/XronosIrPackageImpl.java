/**
 */
package org.xronos.orcc.ir.impl;

import net.sf.orcc.graph.GraphPackage;

import net.sf.orcc.ir.IrPackage;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageImpl;

import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.Dfg;
import org.xronos.orcc.ir.DfgEdge;
import org.xronos.orcc.ir.DfgVertex;
import org.xronos.orcc.ir.InstPortPeek;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.ir.InstSimplePortWrite;
import org.xronos.orcc.ir.XronosIrFactory;
import org.xronos.orcc.ir.XronosIrPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class XronosIrPackageImpl extends EPackageImpl implements XronosIrPackage {
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
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass instSimplePortWriteEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass blockMutexEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass dfgEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass dfgVertexEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass dfgEdgeEClass = null;

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
	 * @see org.xronos.orcc.ir.XronosIrPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private XronosIrPackageImpl() {
		super(eNS_URI, XronosIrFactory.eINSTANCE);
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
	 * <p>This method is used to initialize {@link XronosIrPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static XronosIrPackage init() {
		if (isInited) return (XronosIrPackage)EPackage.Registry.INSTANCE.getEPackage(XronosIrPackage.eNS_URI);

		// Obtain or create and register package
		XronosIrPackageImpl theXronosIrPackage = (XronosIrPackageImpl)(EPackage.Registry.INSTANCE.get(eNS_URI) instanceof XronosIrPackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI) : new XronosIrPackageImpl());

		isInited = true;

		// Initialize simple dependencies
		IrPackage.eINSTANCE.eClass();
		EcorePackage.eINSTANCE.eClass();

		// Create package meta-data objects
		theXronosIrPackage.createPackageContents();

		// Initialize created meta-data
		theXronosIrPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theXronosIrPackage.freeze();

  
		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(XronosIrPackage.eNS_URI, theXronosIrPackage);
		return theXronosIrPackage;
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
	public EAttribute getInstPortRead_Blocking() {
		return (EAttribute)instPortReadEClass.getEStructuralFeatures().get(2);
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
	public EAttribute getInstPortWrite_Blocking() {
		return (EAttribute)instPortWriteEClass.getEStructuralFeatures().get(2);
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
	public EClass getInstSimplePortWrite() {
		return instSimplePortWriteEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getInstSimplePortWrite_Name() {
		return (EAttribute)instSimplePortWriteEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInstSimplePortWrite_Value() {
		return (EReference)instSimplePortWriteEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getBlockMutex() {
		return blockMutexEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getBlockMutex_Blocks() {
		return (EReference)blockMutexEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDfg() {
		return dfgEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDfg_Operations() {
		return (EReference)dfgEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDfg_Dependencies() {
		return (EReference)dfgEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDfgVertex() {
		return dfgVertexEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDfgVertex_Node() {
		return (EReference)dfgVertexEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDfgEdge() {
		return dfgEdgeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDfgEdge_Line() {
		return (EReference)dfgEdgeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrFactory getXronosIrFactory() {
		return (XronosIrFactory)getEFactoryInstance();
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
		createEAttribute(instPortReadEClass, INST_PORT_READ__BLOCKING);

		instPortWriteEClass = createEClass(INST_PORT_WRITE);
		createEReference(instPortWriteEClass, INST_PORT_WRITE__PORT);
		createEReference(instPortWriteEClass, INST_PORT_WRITE__VALUE);
		createEAttribute(instPortWriteEClass, INST_PORT_WRITE__BLOCKING);

		instPortPeekEClass = createEClass(INST_PORT_PEEK);
		createEReference(instPortPeekEClass, INST_PORT_PEEK__TARGET);
		createEReference(instPortPeekEClass, INST_PORT_PEEK__PORT);

		instSimplePortWriteEClass = createEClass(INST_SIMPLE_PORT_WRITE);
		createEAttribute(instSimplePortWriteEClass, INST_SIMPLE_PORT_WRITE__NAME);
		createEReference(instSimplePortWriteEClass, INST_SIMPLE_PORT_WRITE__VALUE);

		blockMutexEClass = createEClass(BLOCK_MUTEX);
		createEReference(blockMutexEClass, BLOCK_MUTEX__BLOCKS);

		dfgEClass = createEClass(DFG);
		createEReference(dfgEClass, DFG__OPERATIONS);
		createEReference(dfgEClass, DFG__DEPENDENCIES);

		dfgVertexEClass = createEClass(DFG_VERTEX);
		createEReference(dfgVertexEClass, DFG_VERTEX__NODE);

		dfgEdgeEClass = createEClass(DFG_EDGE);
		createEReference(dfgEdgeEClass, DFG_EDGE__LINE);
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
		EcorePackage theEcorePackage = (EcorePackage)EPackage.Registry.INSTANCE.getEPackage(EcorePackage.eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		instPortStatusEClass.getESuperTypes().add(theIrPackage.getInstruction());
		instPortReadEClass.getESuperTypes().add(theIrPackage.getInstruction());
		instPortWriteEClass.getESuperTypes().add(theIrPackage.getInstruction());
		instPortPeekEClass.getESuperTypes().add(theIrPackage.getInstruction());
		instSimplePortWriteEClass.getESuperTypes().add(theIrPackage.getInstruction());
		blockMutexEClass.getESuperTypes().add(theIrPackage.getBlock());
		dfgEClass.getESuperTypes().add(theGraphPackage.getGraph());
		dfgVertexEClass.getESuperTypes().add(theGraphPackage.getVertex());
		dfgEdgeEClass.getESuperTypes().add(theGraphPackage.getEdge());

		// Initialize classes and features; add operations and parameters
		initEClass(instPortStatusEClass, InstPortStatus.class, "InstPortStatus", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getInstPortStatus_Target(), theIrPackage.getDef(), null, "target", null, 0, 1, InstPortStatus.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstPortStatus_Port(), theGraphPackage.getVertex(), null, "port", null, 0, 1, InstPortStatus.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(instPortReadEClass, InstPortRead.class, "InstPortRead", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getInstPortRead_Target(), theIrPackage.getDef(), null, "target", null, 0, 1, InstPortRead.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstPortRead_Port(), theGraphPackage.getVertex(), null, "port", null, 0, 1, InstPortRead.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getInstPortRead_Blocking(), theEcorePackage.getEBoolean(), "blocking", "false", 0, 1, InstPortRead.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(instPortWriteEClass, InstPortWrite.class, "InstPortWrite", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getInstPortWrite_Port(), theGraphPackage.getVertex(), null, "port", null, 0, 1, InstPortWrite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstPortWrite_Value(), theIrPackage.getExpression(), null, "value", null, 0, 1, InstPortWrite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getInstPortWrite_Blocking(), theEcorePackage.getEBoolean(), "blocking", "false", 0, 1, InstPortWrite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(instPortPeekEClass, InstPortPeek.class, "InstPortPeek", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getInstPortPeek_Target(), theIrPackage.getDef(), null, "target", null, 0, 1, InstPortPeek.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstPortPeek_Port(), theGraphPackage.getVertex(), null, "port", null, 0, 1, InstPortPeek.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(instSimplePortWriteEClass, InstSimplePortWrite.class, "InstSimplePortWrite", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getInstSimplePortWrite_Name(), theEcorePackage.getEString(), "name", null, 0, 1, InstSimplePortWrite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInstSimplePortWrite_Value(), theIrPackage.getExpression(), null, "value", null, 0, 1, InstSimplePortWrite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(blockMutexEClass, BlockMutex.class, "BlockMutex", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getBlockMutex_Blocks(), theIrPackage.getBlock(), null, "blocks", null, 0, -1, BlockMutex.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(dfgEClass, Dfg.class, "Dfg", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getDfg_Operations(), this.getDfgVertex(), null, "operations", null, 0, -1, Dfg.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getDfg_Dependencies(), this.getDfgEdge(), null, "dependencies", null, 0, -1, Dfg.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(dfgVertexEClass, DfgVertex.class, "DfgVertex", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getDfgVertex_Node(), theIrPackage.getInstruction(), null, "node", null, 0, 1, DfgVertex.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(dfgEdgeEClass, DfgEdge.class, "DfgEdge", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getDfgEdge_Line(), theIrPackage.getExpression(), null, "line", null, 0, 1, DfgEdge.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Create resource
		createResource(eNS_URI);
	}

} //XronosIrPackageImpl
