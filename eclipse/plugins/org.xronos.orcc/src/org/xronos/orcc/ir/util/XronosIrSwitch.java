/**
 */
package org.xronos.orcc.ir.util;

import net.sf.orcc.graph.Edge;
import net.sf.orcc.graph.Graph;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.Instruction;

import net.sf.orcc.util.Adaptable;
import net.sf.orcc.util.Attributable;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.Switch;

import org.xronos.orcc.ir.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see org.xronos.orcc.ir.XronosIrPackage
 * @generated
 */
public class XronosIrSwitch<T> extends Switch<T> {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static XronosIrPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrSwitch() {
		if (modelPackage == null) {
			modelPackage = XronosIrPackage.eINSTANCE;
		}
	}

	/**
	 * Checks whether this is a switch for the given package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @parameter ePackage the package in question.
	 * @return whether this is a switch for the given package.
	 * @generated
	 */
	@Override
	protected boolean isSwitchFor(EPackage ePackage) {
		return ePackage == modelPackage;
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	@Override
	protected T doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case XronosIrPackage.INST_PORT_STATUS: {
				InstPortStatus instPortStatus = (InstPortStatus)theEObject;
				T result = caseInstPortStatus(instPortStatus);
				if (result == null) result = caseInstruction(instPortStatus);
				if (result == null) result = caseAttributable(instPortStatus);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrPackage.INST_PORT_READ: {
				InstPortRead instPortRead = (InstPortRead)theEObject;
				T result = caseInstPortRead(instPortRead);
				if (result == null) result = caseInstruction(instPortRead);
				if (result == null) result = caseAttributable(instPortRead);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrPackage.INST_PORT_WRITE: {
				InstPortWrite instPortWrite = (InstPortWrite)theEObject;
				T result = caseInstPortWrite(instPortWrite);
				if (result == null) result = caseInstruction(instPortWrite);
				if (result == null) result = caseAttributable(instPortWrite);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrPackage.INST_PORT_PEEK: {
				InstPortPeek instPortPeek = (InstPortPeek)theEObject;
				T result = caseInstPortPeek(instPortPeek);
				if (result == null) result = caseInstruction(instPortPeek);
				if (result == null) result = caseAttributable(instPortPeek);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrPackage.INST_SIMPLE_PORT_WRITE: {
				InstSimplePortWrite instSimplePortWrite = (InstSimplePortWrite)theEObject;
				T result = caseInstSimplePortWrite(instSimplePortWrite);
				if (result == null) result = caseInstruction(instSimplePortWrite);
				if (result == null) result = caseAttributable(instSimplePortWrite);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrPackage.BLOCK_MUTEX: {
				BlockMutex blockMutex = (BlockMutex)theEObject;
				T result = caseBlockMutex(blockMutex);
				if (result == null) result = caseBlock(blockMutex);
				if (result == null) result = caseAttributable(blockMutex);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrPackage.DFG: {
				Dfg dfg = (Dfg)theEObject;
				T result = caseDfg(dfg);
				if (result == null) result = caseGraph(dfg);
				if (result == null) result = caseVertex(dfg);
				if (result == null) result = caseAttributable(dfg);
				if (result == null) result = caseAdaptable(dfg);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrPackage.DFG_VERTEX: {
				DfgVertex dfgVertex = (DfgVertex)theEObject;
				T result = caseDfgVertex(dfgVertex);
				if (result == null) result = caseVertex(dfgVertex);
				if (result == null) result = caseAttributable(dfgVertex);
				if (result == null) result = caseAdaptable(dfgVertex);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrPackage.DFG_EDGE: {
				DfgEdge dfgEdge = (DfgEdge)theEObject;
				T result = caseDfgEdge(dfgEdge);
				if (result == null) result = caseEdge(dfgEdge);
				if (result == null) result = caseAttributable(dfgEdge);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Port Status</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Port Status</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstPortStatus(InstPortStatus object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Port Read</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Port Read</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstPortRead(InstPortRead object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Port Write</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Port Write</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstPortWrite(InstPortWrite object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Port Peek</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Port Peek</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstPortPeek(InstPortPeek object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Simple Port Write</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Simple Port Write</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstSimplePortWrite(InstSimplePortWrite object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Block Mutex</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Block Mutex</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseBlockMutex(BlockMutex object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Dfg</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Dfg</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDfg(Dfg object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Dfg Vertex</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Dfg Vertex</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDfgVertex(DfgVertex object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Dfg Edge</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Dfg Edge</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDfgEdge(DfgEdge object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Attributable</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Attributable</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAttributable(Attributable object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Instruction</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Instruction</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstruction(Instruction object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Block</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Block</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseBlock(Block object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Adaptable</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Adaptable</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAdaptable(Adaptable object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Vertex</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Vertex</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseVertex(Vertex object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Graph</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Graph</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseGraph(Graph object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Edge</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Edge</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseEdge(Edge object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	@Override
	public T defaultCase(EObject object) {
		return null;
	}

} //XronosIrSwitch
