/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */
package org.xronos.orcc.systemc.transform;

import net.sf.orcc.backends.ir.BlockFor;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.eclipse.emf.ecore.EObject;

/**
 * This transformation lables evry loop in an actor
 * 
 * @author Endri Bezati
 *
 */
public class LoopLabeler extends DfVisitor<Void> {

	private class Labeler extends AbstractIrVisitor<Void> {

		@Override
		public Void caseBlockWhile(BlockWhile blockWhile) {
			blockWhile.setAttribute("loopLabel", loopIndex);
			loopIndex++;
			return super.caseBlockWhile(blockWhile);
		}

		public Void caseBlockFor(BlockFor blockFor) {
			blockFor.setAttribute("loopLabel", loopIndex);
			loopIndex++;
			doSwitch(blockFor.getBlocks());
			doSwitch(blockFor.getJoinBlock());
			return null;
		}

		@Override
		public Void defaultCase(EObject object) {
			if (object instanceof BlockFor) {
				return caseBlockFor((BlockFor) object);
			}
			return super.defaultCase(object);
		}

	}
	
	/** Loop Counter **/
	private Integer loopIndex;

	public LoopLabeler(){
		Labeler labeler =  new Labeler();
		this.irVisitor = labeler;
	}

	@Override
	public Void caseActor(Actor actor) {
		loopIndex = 0;
		return super.caseActor(actor);
	}
	
	
}
