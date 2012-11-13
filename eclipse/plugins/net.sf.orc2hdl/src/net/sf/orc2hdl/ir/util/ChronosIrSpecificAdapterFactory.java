/**
 */
package net.sf.orc2hdl.ir.util;

import net.sf.orc2hdl.ir.*;

import net.sf.orcc.ir.InstSpecific;
import net.sf.orcc.ir.Instruction;

import net.sf.orcc.util.Attributable;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see net.sf.orc2hdl.ir.ChronosIrSpecificPackage
 * @generated
 */
public class ChronosIrSpecificAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static ChronosIrSpecificPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ChronosIrSpecificAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = ChronosIrSpecificPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch that delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ChronosIrSpecificSwitch<Adapter> modelSwitch =
		new ChronosIrSpecificSwitch<Adapter>() {
			@Override
			public Adapter caseInstPortStatus(InstPortStatus object) {
				return createInstPortStatusAdapter();
			}
			@Override
			public Adapter caseInstPortRead(InstPortRead object) {
				return createInstPortReadAdapter();
			}
			@Override
			public Adapter caseInstPortWrite(InstPortWrite object) {
				return createInstPortWriteAdapter();
			}
			@Override
			public Adapter caseInstPortPeek(InstPortPeek object) {
				return createInstPortPeekAdapter();
			}
			@Override
			public Adapter caseAttributable(Attributable object) {
				return createAttributableAdapter();
			}
			@Override
			public Adapter caseInstruction(Instruction object) {
				return createInstructionAdapter();
			}
			@Override
			public Adapter caseInstSpecific(InstSpecific object) {
				return createInstSpecificAdapter();
			}
			@Override
			public Adapter defaultCase(EObject object) {
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target) {
		return modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link net.sf.orc2hdl.ir.InstPortStatus <em>Inst Port Status</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see net.sf.orc2hdl.ir.InstPortStatus
	 * @generated
	 */
	public Adapter createInstPortStatusAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link net.sf.orc2hdl.ir.InstPortRead <em>Inst Port Read</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see net.sf.orc2hdl.ir.InstPortRead
	 * @generated
	 */
	public Adapter createInstPortReadAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link net.sf.orc2hdl.ir.InstPortWrite <em>Inst Port Write</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see net.sf.orc2hdl.ir.InstPortWrite
	 * @generated
	 */
	public Adapter createInstPortWriteAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link net.sf.orc2hdl.ir.InstPortPeek <em>Inst Port Peek</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see net.sf.orc2hdl.ir.InstPortPeek
	 * @generated
	 */
	public Adapter createInstPortPeekAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link net.sf.orcc.util.Attributable <em>Attributable</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see net.sf.orcc.util.Attributable
	 * @generated
	 */
	public Adapter createAttributableAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link net.sf.orcc.ir.Instruction <em>Instruction</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see net.sf.orcc.ir.Instruction
	 * @generated
	 */
	public Adapter createInstructionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link net.sf.orcc.ir.InstSpecific <em>Inst Specific</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see net.sf.orcc.ir.InstSpecific
	 * @generated
	 */
	public Adapter createInstSpecificAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //ChronosIrSpecificAdapterFactory
