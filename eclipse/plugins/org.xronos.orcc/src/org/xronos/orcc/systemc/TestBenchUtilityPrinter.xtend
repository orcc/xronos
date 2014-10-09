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
	
	using namespace std;
	
	SC_MODULE(tb_kicker) {
		sc_in<bool> clk;
		sc_out<bool> reset;
		sc_out<bool> start;
	
	  SC_CTOR(tb_kicker)
		{
	    SC_CTHREAD(prc_reset, clk.pos());
			//reset_signal_is(reset,true);
		}
	
		void prc_reset() {
			reset = true;
			start = false;
			wait();
			wait();
		
			reset = false;
		#ifndef __SYNTHESIS__
			cout << "INFO: @" << sc_time_stamp() << ", " << "Reset Off = " << reset << endl;
		#endif
		
			wait();
			wait();
			wait();
		
		#ifndef __SYNTHESIS__
			cout << "INFO: @" << sc_time_stamp() << ", " << "Start On  = " << start << endl;
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
	#include <fstream>
	#include <sstream>
	
	using namespace std;
	
	template<class T>
	SC_MODULE(tb_driver) {
		sc_in<bool> clk;
		sc_in<bool> reset;
		sc_in<bool> start;
		sc_out<bool> done;
	
		sc_fifo_out< T > dout;
		
		string file_name;
		
		SC_CTOR(tb_driver):
			file_name("")
		{
			SC_CTHREAD(prc_read, clk.pos());
			reset_signal_is(reset,true);
		}
	
		void set_file_name(string file_name){
			this->file_name = file_name;
		}	
	
		void prc_read() {
			done = false;
			
			do { wait(); } while ( !start.read() );
			wait();
			
			ifstream in_file;
			in_file.open(file_name.c_str());
			if (in_file.fail()) {
				cerr << "unable to open file " << file_name <<" for reading" << endl;
				exit(1);
			}
			
			T n;
			while(!in_file.eof()){
				string line;
				getline(in_file, line);
				if(line != ""){
					istringstream iss(line);
					iss >> n;
					dout.write(n);
				}
			}
	
			in_file.close();
	
			done = true;
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
	#include <fstream>
	#include <sstream>
	
	using namespace std;
	
	template<class T>
	SC_MODULE(tb_compare) {
		sc_in<bool> clk;
		sc_in<bool> reset;
		sc_in<bool> start;
		sc_out<bool> done;
	
		sc_fifo_in< T > din;
	
		string file_name;
	
		string port_name;
	
		SC_CTOR(tb_compare)
		{
	    	SC_CTHREAD(prc_compare, clk.pos());
	 		reset_signal_is(reset,true);
		}
	
		void set_file_name(string file_name){
			this->file_name = file_name;
		}
	
		void set_port_name(string port_name){
			this->port_name = port_name;
		}
	
		void prc_compare() {
			done = false;

			do { wait(); } while ( !start.read() );
	
			ifstream in_file;
			in_file.open(file_name.c_str());
		  	if (in_file.fail()) {
	    		cerr << "Unable to open file " << file_name <<" for reading" << endl;
	    		exit(1);
			}
	
			T golden;
			T sim;
			int line_counter = 0;
			while(!in_file.eof()){
				string line;
				getline(in_file, line);
				if(line != ""){
					// -- Read golden reference
					istringstream iss(line);
					iss >> golden;	
					
					// -- Read from the input
					sim = din.read();
					if( sim != golden){
						cout  << "ERROR: @" << sc_time_stamp() << ", " << "On port " << port_name << " incorrect value computed " << sim << " instead of " << golden << ", sequence " << line_counter << endl; 
						sc_stop();
					}else{
						cout  << "INFO: @" << sc_time_stamp() << ", " << "On port " << port_name << " correct value computed (" << sim << "), sequence " << line_counter << endl;
					}
					line_counter++;	
				}	
			}
	
	 		in_file.close();
			done = true;
		}
	};
	
	#endif // TB_CAPTURE_H
	'''
	
}