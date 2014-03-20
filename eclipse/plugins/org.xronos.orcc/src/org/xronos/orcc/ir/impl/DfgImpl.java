/**
 */
package org.xronos.orcc.ir.impl;

import java.util.Collection;

import net.sf.orcc.graph.impl.GraphImpl;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.util.EObjectResolvingEList;

import org.xronos.orcc.ir.Dfg;
import org.xronos.orcc.ir.DfgEdge;
import org.xronos.orcc.ir.DfgVertex;
import org.xronos.orcc.ir.XronosIrPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Dfg</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.impl.DfgImpl#getOperations <em>Operations</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.impl.DfgImpl#getDependencies <em>Dependencies</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DfgImpl extends GraphImpl implements Dfg {
	/**
	 * The cached value of the '{@link #getOperations() <em>Operations</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOperations()
	 * @generated
	 * @ordered
	 */
	protected EList<DfgVertex> operations;

	/**
	 * The cached value of the '{@link #getDependencies() <em>Dependencies</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDependencies()
	 * @generated
	 * @ordered
	 */
	protected EList<DfgEdge> dependencies;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected DfgImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return XronosIrPackage.Literals.DFG;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<DfgVertex> getOperations() {
		if (operations == null) {
			operations = new EObjectResolvingEList<DfgVertex>(DfgVertex.class, this, XronosIrPackage.DFG__OPERATIONS);
		}
		return operations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<DfgEdge> getDependencies() {
		if (dependencies == null) {
			dependencies = new EObjectResolvingEList<DfgEdge>(DfgEdge.class, this, XronosIrPackage.DFG__DEPENDENCIES);
		}
		return dependencies;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case XronosIrPackage.DFG__OPERATIONS:
				return getOperations();
			case XronosIrPackage.DFG__DEPENDENCIES:
				return getDependencies();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case XronosIrPackage.DFG__OPERATIONS:
				getOperations().clear();
				getOperations().addAll((Collection<? extends DfgVertex>)newValue);
				return;
			case XronosIrPackage.DFG__DEPENDENCIES:
				getDependencies().clear();
				getDependencies().addAll((Collection<? extends DfgEdge>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case XronosIrPackage.DFG__OPERATIONS:
				getOperations().clear();
				return;
			case XronosIrPackage.DFG__DEPENDENCIES:
				getDependencies().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case XronosIrPackage.DFG__OPERATIONS:
				return operations != null && !operations.isEmpty();
			case XronosIrPackage.DFG__DEPENDENCIES:
				return dependencies != null && !dependencies.isEmpty();
		}
		return super.eIsSet(featureID);
	}

} //DfgImpl
