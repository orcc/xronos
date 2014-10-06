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

import java.text.SimpleDateFormat
import java.util.Date

class TestBenchUtilityPrinter {
	
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
			// This file is generated automatically by Xronos HLS
			// ----------------------------------------------------------------------------
			// Xronos TestBench Utility Modules
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
			
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
		'''
	}
	
	
	def getKickerModule()'''
	«header»
	
	#ifndef TB_KICKER_H
	#define TB_KICKER_H
	
	#include <systemc.h>
	#include <iostream>
	#include "string"
	using namespace std;
	
	SC_MODULE(tb_kicker) {
		sc_in<bool> clk;
		sc_out<bool> reset;
		sc_out<bool> start;
	
		void prc_reset();
	
	  SC_CTOR(tb_kicker)
		{
	    SC_CTHREAD(prc_reset, clk.pos());
		}
	
		void tb_kicker::prc_reset() {
			reset = true;
			start = false;
			wait();
			wait();
		
			reset = false;
		#ifndef __SYNTHESIS__
			cout << "Reset Off =" << reset <<" : "<< sc_time_stamp() << endl;
		#endif
		
			wait();
			wait();
			wait();
		
		#ifndef __SYNTHESIS__
			cout << "Start On   =" << start << " : " << sc_time_stamp() << endl;
			cout << " " << endl;
		#endif
			start = true;
			wait(2);
			start = false;
		}
	};
	
	#endif // TB_KICKER_H
	'''
	
	def getDriverModule()'''
	«header»
	
	#ifndef TB_DRIVER_H
	#define TB_DRIVER_H
	
	#include <systemc.h>
	#include <iostream>
	#include "string"
	using namespace std;
	
	SC_MODULE(tb_kicker) {
		sc_in<bool> clk;
		sc_out<bool> reset;
		sc_in<bool> start;
		sc_out<bool> done;
	
		void prc_read();
	
	  SC_CTOR(tb_driver)
		{
	    SC_CTHREAD(prc_read, clk.pos());
		}

		void tb_driver::prc_read() {
		}	
	};
	
	#endif // TB_DRIVER_H
	'''
	
	
	def getCompareModule()'''
	«header»
	
	#ifndef TB_CAPTURE_H
	#define TB_CAPTURE_H
	
	#include <systemc.h>
	#include <iostream>
	#include "string"
	using namespace std;
	
	SC_MODULE(tb_compare) {
		sc_in<bool> clk;
		sc_out<bool> reset;
		sc_in<bool> start;
		sc_out<bool> done;
	
		void prc_compare();
	
	  SC_CTOR(tb_compare)
		{
	    SC_CTHREAD(prc_compare, clk.pos());
		}

		void tb_driver::prc_compare() {
		}	
	};
	
	#endif // TB_CAPTURE_H
	'''
}