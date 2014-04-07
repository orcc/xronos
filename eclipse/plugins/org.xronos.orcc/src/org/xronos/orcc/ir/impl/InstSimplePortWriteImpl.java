/**
 */
package org.xronos.orcc.ir.impl;

import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.impl.InstructionImpl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.xronos.orcc.ir.InstSimplePortWrite;
import org.xronos.orcc.ir.XronosIrPackage;

/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Inst Simple Port Write</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.xronos.orcc.ir.impl.InstSimplePortWriteImpl#getName <em>Name
 * </em>}</li>
 * <li>{@link org.xronos.orcc.ir.impl.InstSimplePortWriteImpl#getValue <em>Value
 * </em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class InstSimplePortWriteImpl extends InstructionImpl implements
		InstSimplePortWrite {
	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The cached value of the '{@link #getValue() <em>Value</em>}' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getValue()
	 * @generated
	 * @ordered
	 */
	protected Expression value;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected InstSimplePortWriteImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public NotificationChain basicSetValue(Expression newValue,
			NotificationChain msgs) {
		Expression oldValue = value;
		value = newValue;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this,
					Notification.SET,
					XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE, oldValue,
					newValue);
			if (msgs == null) {
				msgs = notification;
			} else {
				msgs.add(notification);
			}
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__NAME:
			return getName();
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE:
			return getValue();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd,
			int featureID, NotificationChain msgs) {
		switch (featureID) {
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE:
			return basicSetValue(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__NAME:
			return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT
					.equals(name);
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE:
			return value != null;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__NAME:
			setName((String) newValue);
			return;
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE:
			setValue((Expression) newValue);
			return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return XronosIrPackage.Literals.INST_SIMPLE_PORT_WRITE;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__NAME:
			setName(NAME_EDEFAULT);
			return;
		case XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE:
			setValue((Expression) null);
			return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Expression getValue() {
		return value;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET,
					XronosIrPackage.INST_SIMPLE_PORT_WRITE__NAME, oldName, name));
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void setValue(Expression newValue) {
		if (newValue != value) {
			NotificationChain msgs = null;
			if (value != null) {
				msgs = ((InternalEObject) value)
						.eInverseRemove(
								this,
								EOPPOSITE_FEATURE_BASE
										- XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE,
								null, msgs);
			}
			if (newValue != null) {
				msgs = ((InternalEObject) newValue)
						.eInverseAdd(
								this,
								EOPPOSITE_FEATURE_BASE
										- XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE,
								null, msgs);
			}
			msgs = basicSetValue(newValue, msgs);
			if (msgs != null) {
				msgs.dispatch();
			}
		} else if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET,
					XronosIrPackage.INST_SIMPLE_PORT_WRITE__VALUE, newValue,
					newValue));
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) {
			return super.toString();
		}

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

} // InstSimplePortWriteImpl
