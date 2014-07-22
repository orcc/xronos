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
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */
package org.xronos.orcc.forge.transform;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.impl.PatternImpl;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.ir.XronosIrFactory;

public class SinglePortReadWrite extends DfVisitor<Void> {

	public class ReplaceLoadStore extends AbstractIrVisitor<Void> {
		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();

			PatternImpl pattern = EcoreHelper.getContainerOfType(source,
					PatternImpl.class);
			if (pattern != null) {
				Port port = pattern.getVarToPortMap().get(source);
				if (pattern.getNumTokens(port) == 1) {
					Def def = load.getTarget();
					InstPortRead read = XronosIrFactory.eINSTANCE
							.createInstPortRead();
					read.setPort(port);
					read.setTarget(def);

					BlockBasic block = EcoreHelper.getContainerOfType(load,
							BlockBasic.class);
					int idx = block.getInstructions().indexOf(load);
					IrUtil.delete(load);

					block.add(idx, read);
				}
			}
			return null;
		}

		@Override
		public Void caseInstStore(InstStore store) {
			Var target = store.getTarget().getVariable();

			PatternImpl pattern = EcoreHelper.getContainerOfType(target,
					PatternImpl.class);
			if (pattern != null) {
				Port port = pattern.getVarToPortMap().get(target);
				if (pattern.getNumTokens(port) == 1) {
					Expression value = store.getValue();
					InstPortWrite write = XronosIrFactory.eINSTANCE
							.createInstPortWrite();
					write.setPort(port);
					write.setValue(value);

					BlockBasic block = EcoreHelper.getContainerOfType(store,
							BlockBasic.class);
					int idx = block.getInstructions().indexOf(store);
					IrUtil.delete(store);

					block.add(idx, write);
				}
			}
			return null;
		}
	}

	@Override
	public Void caseActor(Actor actor) {
		for (Action action : actor.getActions()) {
			new ReplaceLoadStore().doSwitch(action.getBody());
		}
		return null;
	}

}
