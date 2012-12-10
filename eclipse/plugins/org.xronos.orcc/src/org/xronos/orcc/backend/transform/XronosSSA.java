package org.xronos.orcc.backend.transform;

import net.sf.orcc.ir.transform.SSATransformation;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortWrite;

public class XronosSSA extends SSATransformation {

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			doSwitch(((BlockMutex) object).getBlocks());
		} else if (object instanceof InstPortWrite) {
			return caseInstPortWrite((InstPortWrite) object);
		}
		return super.defaultCase(object);
	}

	public Void caseInstPortWrite(InstPortWrite portWrite) {
		replaceUses(portWrite.getValue());
		return null;
	}

}
