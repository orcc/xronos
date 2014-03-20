/**
 */
package org.xronos.orcc.ir.impl;

import net.sf.orcc.graph.impl.EdgeImpl;

import net.sf.orcc.ir.Expression;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.xronos.orcc.ir.DfgEdge;
import org.xronos.orcc.ir.XronosIrPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Dfg Edge</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.impl.DfgEdgeImpl#getLine <em>Line</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DfgEdgeImpl extends EdgeImpl implements DfgEdge {
	/**
	 * The cached value of the '{@link #getLine() <em>Line</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLine()
	 * @generated
	 * @ordered
	 */
	protected Expression line;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected DfgEdgeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return XronosIrPackage.Literals.DFG_EDGE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Expression getLine() {
		if (line != null && line.eIsProxy()) {
			InternalEObject oldLine = (InternalEObject)line;
			line = (Expression)eResolveProxy(oldLine);
			if (line != oldLine) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, XronosIrPackage.DFG_EDGE__LINE, oldLine, line));
			}
		}
		return line;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Expression basicGetLine() {
		return line;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLine(Expression newLine) {
		Expression oldLine = line;
		line = newLine;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, XronosIrPackage.DFG_EDGE__LINE, oldLine, line));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case XronosIrPackage.DFG_EDGE__LINE:
				if (resolve) return getLine();
				return basicGetLine();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case XronosIrPackage.DFG_EDGE__LINE:
				setLine((Expression)newValue);
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
			case XronosIrPackage.DFG_EDGE__LINE:
				setLine((Expression)null);
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
			case XronosIrPackage.DFG_EDGE__LINE:
				return line != null;
		}
		return super.eIsSet(featureID);
	}

} //DfgEdgeImpl
