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
public class XronosIrSpecificFactoryImpl extends EFactoryImpl implements XronosIrSpecificFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static XronosIrSpecificFactory init() {
		try {
			XronosIrSpecificFactory theXronosIrSpecificFactory = (XronosIrSpecificFactory)EPackage.Registry.INSTANCE.getEFactory("http://www.xronos.org/ir"); 
			if (theXronosIrSpecificFactory != null) {
				return theXronosIrSpecificFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new XronosIrSpecificFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrSpecificFactoryImpl() {
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
			case XronosIrSpecificPackage.INST_PORT_STATUS: return createInstPortStatus();
			case XronosIrSpecificPackage.INST_PORT_READ: return createInstPortRead();
			case XronosIrSpecificPackage.INST_PORT_WRITE: return createInstPortWrite();
			case XronosIrSpecificPackage.INST_PORT_PEEK: return createInstPortPeek();
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
	public XronosIrSpecificPackage getXronosIrSpecificPackage() {
		return (XronosIrSpecificPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static XronosIrSpecificPackage getPackage() {
		return XronosIrSpecificPackage.eINSTANCE;
	}

} //XronosIrSpecificFactoryImpl
