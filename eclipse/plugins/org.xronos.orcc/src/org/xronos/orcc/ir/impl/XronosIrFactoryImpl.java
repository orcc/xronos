/**
 */
package org.xronos.orcc.ir.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.xronos.orcc.ir.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class XronosIrFactoryImpl extends EFactoryImpl implements XronosIrFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static XronosIrFactory init() {
		try {
			XronosIrFactory theXronosIrFactory = (XronosIrFactory)EPackage.Registry.INSTANCE.getEFactory("http://www.xronos.org/ir"); 
			if (theXronosIrFactory != null) {
				return theXronosIrFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new XronosIrFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrFactoryImpl() {
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
			case XronosIrPackage.INST_PORT_STATUS: return createInstPortStatus();
			case XronosIrPackage.INST_PORT_READ: return createInstPortRead();
			case XronosIrPackage.INST_PORT_WRITE: return createInstPortWrite();
			case XronosIrPackage.INST_PORT_PEEK: return createInstPortPeek();
			case XronosIrPackage.BLOCK_MUTEX: return createBlockMutex();
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
	public BlockMutex createBlockMutex() {
		BlockMutexImpl blockMutex = new BlockMutexImpl();
		return blockMutex;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrPackage getXronosIrPackage() {
		return (XronosIrPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static XronosIrPackage getPackage() {
		return XronosIrPackage.eINSTANCE;
	}

} //XronosIrFactoryImpl
