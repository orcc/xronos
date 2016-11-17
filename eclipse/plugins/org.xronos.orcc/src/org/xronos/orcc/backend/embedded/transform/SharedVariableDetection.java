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

package org.xronos.orcc.backend.embedded.transform;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.util.EcoreUtil;

import net.sf.orcc.OrccRuntimeException;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.util.OrccLogger;

/**
 * 
 * @author Endri Bezati
 */
public class SharedVariableDetection extends DfVisitor<Void> {

	Set<Var> sharedVars;

	@Override
	public Void caseNetwork(Network network) {
		sharedVars = new HashSet<Var>();
		for(Actor actor: network.getAllActors()){
			Set<Var> actorSharedVars = new HashSet<Var>();
			for(Var var : actor.getStateVars()){
				if (var.hasAttribute("shared")){
					String id = var.getAttribute("shared").getAttribute("id").getStringValue();
					var.setName(id);
					Type type = EcoreUtil.copy(var.getType());
					Var sharedVar = getNetworkSharedVar(id);
					if (sharedVar !=null){
						// -- Check if it has the same type
						Type sharedVarType = sharedVar.getType();
						if(!EcoreUtil.equals(type, sharedVarType)){
							OrccLogger.severeRaw("Shared variable with id: " + id +" has not the same type");
							throw new OrccRuntimeException("Shared variable with id: " + id +" has not the same type");
						}
					}else{
						sharedVar = IrFactory.eINSTANCE.createVar();
						sharedVar.setName(id);
						sharedVar.setType(type);
						sharedVars.add(sharedVar);
					}
					actorSharedVars.add(sharedVar);
				}
				actor.setAttribute("actor_shared_variables", actorSharedVars);
			}
		}
		
		if(!sharedVars.isEmpty()){
			network.setAttribute("network_shared_variables", sharedVars);
		}
		
		return null;
	}

	
	public Var getNetworkSharedVar(String id){
		
		for(Var var: sharedVars){
			if (var.getName().equals(id)){
				return var;
			}
		}
		
		return null;
	}
	
}
