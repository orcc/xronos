/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.orc2hdl.design.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.sf.orcc.ir.Var;

/**
 * This class is container of a Variable and its associated group
 * 
 * @author Endri Bezati
 * 
 */
public class GroupedVar {

	private Var var;
	private Integer group;

	public GroupedVar(Var var, Integer group) {
		super();
		this.var = var;
		this.group = group;
	}

	public Var getVar() {
		return var;
	}

	public Integer getGroup() {
		return group;
	}

	public List<GroupedVar> getAsList() {
		return Arrays.asList(this);
	}

	public static List<GroupedVar> ListGroupedVar(List<Var> vars, Integer group) {
		List<GroupedVar> listGroupVar = new ArrayList<GroupedVar>();

		for (Var var : vars) {
			listGroupVar.add(new GroupedVar(var, group));
		}
		return listGroupVar;
	}

	public static List<GroupedVar> ListGroupedVar(Set<Var> vars, Integer group) {
		List<GroupedVar> listGroupVar = new ArrayList<GroupedVar>();

		for (Var var : vars) {
			listGroupVar.add(new GroupedVar(var, group));
		}
		return listGroupVar;
	}

	public static Boolean VarContainedInList(List<GroupedVar> groupedVarList,
			Var var) {
		for (GroupedVar groupedVar : groupedVarList) {
			Var gVar = groupedVar.getVar();
			if (gVar == var) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "[" + var.getIndexedName() + ", " + group + "]";
	}

}
