/**
 */
package net.sf.orc2hdl.ir.impl;

import net.sf.orc2hdl.ir.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class ChronosIrSpecificFactoryImpl extends EFactoryImpl implements ChronosIrSpecificFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static ChronosIrSpecificFactory init() {
		try {
			ChronosIrSpecificFactory theChronosIrSpecificFactory = (ChronosIrSpecificFactory)EPackage.Registry.INSTANCE.getEFactory("http://orcc.sf.net/backends/ir"); 
			if (theChronosIrSpecificFactory != null) {
				return theChronosIrSpecificFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new ChronosIrSpecificFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ChronosIrSpecificFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case ChronosIrSpecificPackage.INST_PORT_STATUS: return createInstPortStatus();
			case ChronosIrSpecificPackage.INST_PORT_READ: return createInstPortRead();
			case ChronosIrSpecificPackage.INST_PORT_WRITE: return createInstPortWrite();
			case ChronosIrSpecificPackage.INST_PORT_PEEK: return createInstPortPeek();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstPortStatus createInstPortStatus() {
		InstPortStatusImpl instPortStatus = new InstPortStatusImpl();
		return instPortStatus;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstPortRead createInstPortRead() {
		InstPortReadImpl instPortRead = new InstPortReadImpl();
		return instPortRead;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstPortWrite createInstPortWrite() {
		InstPortWriteImpl instPortWrite = new InstPortWriteImpl();
		return instPortWrite;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InstPortPeek createInstPortPeek() {
		InstPortPeekImpl instPortPeek = new InstPortPeekImpl();
		return instPortPeek;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ChronosIrSpecificPackage getChronosIrSpecificPackage() {
		return (ChronosIrSpecificPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static ChronosIrSpecificPackage getPackage() {
		return ChronosIrSpecificPackage.eINSTANCE;
	}

} //ChronosIrSpecificFactoryImpl
