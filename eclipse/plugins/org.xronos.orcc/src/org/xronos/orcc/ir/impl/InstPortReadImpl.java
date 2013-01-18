/**
 */
package org.xronos.orcc.ir.impl;

import net.sf.orcc.df.Port;
import net.sf.orcc.graph.Vertex;

import net.sf.orcc.ir.Def;

import net.sf.orcc.ir.impl.InstructionImpl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.XronosIrPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Inst Port Read</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.xronos.orcc.ir.impl.InstPortReadImpl#getTarget <em>Target</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.impl.InstPortReadImpl#getPort <em>Port</em>}</li>
 *   <li>{@link org.xronos.orcc.ir.impl.InstPortReadImpl#isBlocking <em>Blocking</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class InstPortReadImpl extends InstructionImpl implements InstPortRead {
	/**
	 * The cached value of the '{@link #getTarget() <em>Target</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTarget()
	 * @generated
	 * @ordered
	 */
	protected Def target;

	/**
	 * The cached value of the '{@link #getPort() <em>Port</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPort()
	 * @generated
	 * @ordered
	 */
	protected Vertex port;

	/**
	 * The default value of the '{@link #isBlocking() <em>Blocking</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isBlocking()
	 * @generated
	 * @ordered
	 */
	protected static final boolean BLOCKING_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isBlocking() <em>Blocking</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isBlocking()
	 * @generated
	 * @ordered
	 */
	protected boolean blocking = BLOCKING_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected InstPortReadImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return XronosIrPackage.Literals.INST_PORT_READ;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Def getTarget() {
		return target;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetTarget(Def newTarget, NotificationChain msgs) {
		Def oldTarget = target;
		target = newTarget;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, XronosIrPackage.INST_PORT_READ__TARGET, oldTarget, newTarget);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTarget(Def newTarget) {
		if (newTarget != target) {
			NotificationChain msgs = null;
			if (target != null)
				msgs = ((InternalEObject)target).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - XronosIrPackage.INST_PORT_READ__TARGET, null, msgs);
			if (newTarget != null)
				msgs = ((InternalEObject)newTarget).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - XronosIrPackage.INST_PORT_READ__TARGET, null, msgs);
			msgs = basicSetTarget(newTarget, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, XronosIrPackage.INST_PORT_READ__TARGET, newTarget, newTarget));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Vertex getPort() {
		return port;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPort(Vertex newPort) {
		Vertex oldPort = port;
		port = newPort;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, XronosIrPackage.INST_PORT_READ__PORT, oldPort, port));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isBlocking() {
		return blocking;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setBlocking(boolean newBlocking) {
		boolean oldBlocking = blocking;
		blocking = newBlocking;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, XronosIrPackage.INST_PORT_READ__BLOCKING, oldBlocking, blocking));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case XronosIrPackage.INST_PORT_READ__TARGET:
				return basicSetTarget(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case XronosIrPackage.INST_PORT_READ__TARGET:
				return getTarget();
			case XronosIrPackage.INST_PORT_READ__PORT:
				return getPort();
			case XronosIrPackage.INST_PORT_READ__BLOCKING:
				return isBlocking();
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
			case XronosIrPackage.INST_PORT_READ__TARGET:
				setTarget((Def)newValue);
				return;
			case XronosIrPackage.INST_PORT_READ__PORT:
				setPort((Vertex)newValue);
				return;
			case XronosIrPackage.INST_PORT_READ__BLOCKING:
				setBlocking((Boolean)newValue);
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
			case XronosIrPackage.INST_PORT_READ__TARGET:
				setTarget((Def)null);
				return;
			case XronosIrPackage.INST_PORT_READ__PORT:
				setPort((Vertex)null);
				return;
			case XronosIrPackage.INST_PORT_READ__BLOCKING:
				setBlocking(BLOCKING_EDEFAULT);
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
			case XronosIrPackage.INST_PORT_READ__TARGET:
				return target != null;
			case XronosIrPackage.INST_PORT_READ__PORT:
				return port != null;
			case XronosIrPackage.INST_PORT_READ__BLOCKING:
				return blocking != BLOCKING_EDEFAULT;
		}
		return super.eIsSet(featureID);
	}
	
	@Override
	public String toString() {
		return super.toString() + "PortRead("
				+ target.getVariable().getIndexedName() + ", "
				+ ((Port) port).getName() + ")";
	}

} //InstPortReadImpl
