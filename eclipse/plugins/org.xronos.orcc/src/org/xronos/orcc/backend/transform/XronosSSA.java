package org.xronos.orcc.backend.transform;

import net.sf.orcc.ir.transform.SSATransformation;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;

public class XronosSSA extends SSATransformation {

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			doSwitch(((BlockMutex) object).getBlocks());
		}
		return super.defaultCase(object);
	}

}
