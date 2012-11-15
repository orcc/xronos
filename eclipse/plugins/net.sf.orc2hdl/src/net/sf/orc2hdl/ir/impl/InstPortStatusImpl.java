/**
 */
package net.sf.orc2hdl.ir.impl;

import net.sf.orc2hdl.ir.InstPortStatus;
import net.sf.orc2hdl.ir.XronosIrSpecificPackage;
import net.sf.orcc.df.Port;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.impl.InstSpecificImpl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Inst Port Status</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.sf.orc2hdl.ir.impl.InstPortStatusImpl#getTarget <em>Target
 * </em>}</li>
 * <li>{@link net.sf.orc2hdl.ir.impl.InstPortStatusImpl#getPort <em>Port</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class InstPortStatusImpl extends InstSpecificImpl implements
		InstPortStatus {
	/**
	 * The cached value of the '{@link #getTarget() <em>Target</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getTarget()
	 * @generated
	 * @ordered
	 */
	protected Def target;

	/**
	 * The cached value of the '{@link #getPort() <em>Port</em>}' reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getPort()
	 * @generated
	 * @ordered
	 */
	protected Vertex port;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected InstPortStatusImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return XronosIrSpecificPackage.Literals.INST_PORT_STATUS;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Def getTarget() {
		return target;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public NotificationChain basicSetTarget(Def newTarget,
			NotificationChain msgs) {
		Def oldTarget = target;
		target = newTarget;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this,
					Notification.SET,
					XronosIrSpecificPackage.INST_PORT_STATUS__TARGET,
					oldTarget, newTarget);
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
	public void setTarget(Def newTarget) {
		if (newTarget != target) {
			NotificationChain msgs = null;
			if (target != null) {
				msgs = ((InternalEObject) target)
						.eInverseRemove(
								this,
								EOPPOSITE_FEATURE_BASE
										- XronosIrSpecificPackage.INST_PORT_STATUS__TARGET,
								null, msgs);
			}
			if (newTarget != null) {
				msgs = ((InternalEObject) newTarget)
						.eInverseAdd(
								this,
								EOPPOSITE_FEATURE_BASE
										- XronosIrSpecificPackage.INST_PORT_STATUS__TARGET,
								null, msgs);
			}
			msgs = basicSetTarget(newTarget, msgs);
			if (msgs != null) {
				msgs.dispatch();
			}
		} else if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET,
					XronosIrSpecificPackage.INST_PORT_STATUS__TARGET,
					newTarget, newTarget));
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Vertex getPort() {
		return port;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void setPort(Vertex newPort) {
		Vertex oldPort = port;
		port = newPort;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET,
					XronosIrSpecificPackage.INST_PORT_STATUS__PORT, oldPort,
					port));
		}
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
		case XronosIrSpecificPackage.INST_PORT_STATUS__TARGET:
			return basicSetTarget(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_STATUS__TARGET:
			return getTarget();
		case XronosIrSpecificPackage.INST_PORT_STATUS__PORT:
			return getPort();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_STATUS__TARGET:
			setTarget((Def) newValue);
			return;
		case XronosIrSpecificPackage.INST_PORT_STATUS__PORT:
			setPort((Vertex) newValue);
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
	public void eUnset(int featureID) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_STATUS__TARGET:
			setTarget((Def) null);
			return;
		case XronosIrSpecificPackage.INST_PORT_STATUS__PORT:
			setPort((Vertex) null);
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
	public boolean eIsSet(int featureID) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_STATUS__TARGET:
			return target != null;
		case XronosIrSpecificPackage.INST_PORT_STATUS__PORT:
			return port != null;
		}
		return super.eIsSet(featureID);
	}

	@Override
	public String toString() {
		return super.toString() + "PortStatus("
				+ target.getVariable().getIndexedName() + ", "
				+ ((Port) port).getName() + ")";
	}

} // InstPortStatusImpl
