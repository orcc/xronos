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

package org.xronos.orcc.backend.embedded

import net.sf.orcc.backends.CommonPrinter
import net.sf.orcc.ir.ExprBinary
import net.sf.orcc.ir.ExprBool
import net.sf.orcc.ir.ExprFloat
import net.sf.orcc.ir.ExprInt
import net.sf.orcc.ir.ExprList
import net.sf.orcc.ir.ExprString
import net.sf.orcc.ir.ExprUnary
import net.sf.orcc.ir.ExprVar
import net.sf.orcc.ir.TypeBool
import net.sf.orcc.ir.TypeFloat
import net.sf.orcc.ir.TypeInt
import net.sf.orcc.ir.TypeList
import net.sf.orcc.ir.TypeString
import net.sf.orcc.ir.TypeUint
import net.sf.orcc.ir.TypeVoid
import net.sf.orcc.ir.OpBinary
import net.sf.orcc.util.util.EcoreHelper
import net.sf.orcc.ir.Expression

/**
 * 
 * @author Endri Bezati
 */
class ExprAndTypePrinter extends CommonPrinter {
		
	override caseExprBinary(ExprBinary expr) {
		val op = expr.op
		val container = EcoreHelper.getContainerOfType(expr, typeof(Expression))
		var nextPrec = if (op == OpBinary::SHIFT_LEFT || op == OpBinary::SHIFT_RIGHT) {

				// special case, for shifts always put parentheses because compilers
				// often issue warnings
				Integer::MIN_VALUE;
			} else {
				op.precedence;
			}

		val resultingExpr = '''«expr.e1.printExpr(nextPrec, 0)» «op.stringRepresentation» «expr.e2.printExpr(nextPrec, 1)»'''

		if (op.needsParentheses(precedence, branch) || (container != null && op.logical)) {
			'''(«resultingExpr»)'''
		} else {
			resultingExpr
		}
	}

	override caseExprBool(ExprBool expr) '''«IF expr.value»true«ELSE»false«ENDIF»'''
	
	override caseExprFloat(ExprFloat expr) '''«expr.value»'''

	override caseExprInt(ExprInt expr) '''«expr.value»«IF expr.long»LL«ENDIF»'''

	override caseExprList(ExprList expr) '''
	{
		«FOR value: expr.value SEPARATOR ","» 
			«value.doSwitch»
		«ENDFOR»
	}'''

	override caseExprString(ExprString expr) '''"«expr.value»"'''

	override caseExprUnary(ExprUnary expr) '''«expr.op.text»(«expr.expr.doSwitch»)'''

	override caseExprVar(ExprVar expr) '''«expr.use.variable.name»'''
	
	override caseTypeBool(TypeBool type)  '''bool'''
	
	override caseTypeFloat(TypeFloat type)  '''float'''
	
	override caseTypeInt(TypeInt type) {
		printInt(type.size)
	}

	override caseTypeList(TypeList type) {
		type.type.doSwitch
	}

	override caseTypeString(TypeString type)  '''std::string'''

	override caseTypeUint(TypeUint type) {
		"unsigned " + printInt(type.size);
	}

	override caseTypeVoid(TypeVoid type) {
		"void";
	}
	
	def private printInt(int size) {
		if (size <= 8) {
			return "char";
		} else if (size <= 16) {
			return "short";
		} else if (size <= 32) {
			return "int";
		} else if (size <= 64) {
			return "long long";
		} else {
			return null;
		}
	}
	
	override protected stringRepresentation(OpBinary op) {
		if (op == OpBinary::DIV_INT)
			"/"
		else
			super.stringRepresentation(op)
	}
}
