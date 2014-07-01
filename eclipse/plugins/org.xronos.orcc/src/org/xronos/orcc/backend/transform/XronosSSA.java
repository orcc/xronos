/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */

package org.xronos.orcc.backend.transform;

import net.sf.orcc.ir.transform.SSATransformation;
import net.sf.orcc.util.Void;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.ir.InstSimplePortWrite;

public class XronosSSA extends SSATransformation {

	public Void caseInstPortWrite(InstPortWrite portWrite) {
		replaceUses(portWrite.getValue());
		return null;
	}

	public Void caseInstSimplePortWrite(InstSimplePortWrite simplePortWrite) {
		replaceUses(simplePortWrite.getValue());
		return null;
	}

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			doSwitch(((BlockMutex) object).getBlocks());
		} else if (object instanceof InstPortWrite) {
			return caseInstPortWrite((InstPortWrite) object);
		} else if (object instanceof InstSimplePortWrite) {
			return caseInstSimplePortWrite((InstSimplePortWrite) object);
		}
		return super.defaultCase(object);
	}

}
