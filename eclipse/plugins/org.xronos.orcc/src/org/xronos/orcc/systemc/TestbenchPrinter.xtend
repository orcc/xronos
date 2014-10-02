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
 
package org.xronos.orcc.systemc

import net.sf.orcc.df.util.DfVisitor
import net.sf.orcc.df.Network
import java.text.SimpleDateFormat
import java.util.Date
import net.sf.orcc.df.Instance

/**
 * SystemC Testbench Printer for Network and Actor
 * 
 * @author Endri Bezati
 */
class TestbenchPrinter extends DfVisitor<Void> {

	private var Network network
	
	private var Instance instance
	
	def getHeader() {
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
			// ----------------------------------------------------------------------------
			// __  ___ __ ___  _ __   ___  ___ 
			// \ \/ / '__/ _ \| '_ \ / _ \/ __|
			//  >  <| | | (_) | | | | (_) \__ \
			// /_/\_\_|  \___/|_| |_|\___/|___/
			// ----------------------------------------------------------------------------
			// Xronos SystemC Generator
			// Top level Network: «network.simpleName» 
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
		'''
	}
	
	
	def private setNetwork(){
		this.network = network
	}


	def private getContent()'''
		«header»
	'''	
}