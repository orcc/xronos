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
import java.util.HashMap
import java.util.List
import java.util.Map
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Network
import net.sf.orcc.df.Port
import net.sf.orcc.graph.Vertex

/*
 * A VHDL Testbench printer
 * 
 * @author Endri Bezati
 */
class VHDLTestbenchPrinter {
	
	var Boolean globalVerification = false;
	
	var Boolean doubleBuffering = false;
	
	/**
	 * Contains a Map which indicates the index of the given clock
	 */

	var Map<String, Integer> clockDomainsIndex;
	var String DEFAULT_CLOCK_DOMAIN = "CLK";	
	
	private var Vertex vertex

	private String name

	private Map<String, Object> options

	def setActor(Actor actor) {
		this.vertex = actor
		this.name = actor.simpleName
	}

	def setNetwork(Network network) {
		this.vertex = network
		this.name = network.simpleName
		
		var Map<String,String> clkDomains = new HashMap<String,String>(); 
		
		if (options.containsKey("clkDomains")) {
			clkDomains = options.get("clkDomains") as Map<String,String>; 
		}
		
		if (options.containsKey("doubleBuffering")) {
			doubleBuffering = options.get("doubleBuffering") as Boolean; 
		}
		
		computeNetworkClockDomains(network,clkDomains);
	}

	def setOptions(Map<String, Object> options) {
		this.options = options
	}
	
	
		
	def private getHeader() {
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
		-- ----------------------------------------------------------------------------
		-- __  ___ __ ___  _ __   ___  ___ 
		-- \ \/ / '__/ _ \| '_ \ / _ \/ __|
		--  >  <| | | (_) | | | | (_) \__ \
		-- /_/\_\_|  \___/|_| |_|\___/|___/
		-- ----------------------------------------------------------------------------
		-- Xronos SystemC, VHDL TestBench
			«IF vertex instanceof Network»
				-- TestBench file for Network: «this.name» 
			«ELSE»
				-- TestBench file for Actor: «this.name» 
			«ENDIF»
		-- Date: «dateFormat.format(date)»
		-- ----------------------------------------------------------------------------
		'''
	}
	
	def addLibraries(){
		'''
		library ieee;
		use ieee.std_logic_1164.all;
		use ieee.std_logic_unsigned.all;
		use ieee.numeric_std.all;
		use std.textio.all;

		library work;
		use work.sim_package.all;
		'''
	}
	
	def addEntity(Vertex vertex){
		var String name;
		if(vertex instanceof Actor){
			name = (vertex as Actor).simpleName;
		}else if(vertex instanceof Network){
			name = (vertex as Network).simpleName;
		}
		'''
		entity «name»_tb is
		end «name»_tb;
		'''
	}
	
	def addArchitecture(Vertex vertex){
		var String name;
		if(vertex instanceof Actor){
			name = (vertex as Actor).simpleName;
		}else if(vertex instanceof Network){
			name = (vertex as Network).simpleName;
		}
		'''
		architecture arch_«name»_tb of «name»_tb is
			«addArchitectureComponent(vertex)»
			
			«addArchitectureSignals(vertex)»
		begin
		«addBeginBody(vertex)»
		end architecture arch_«name»_tb; 
		'''
	}
	
	def addArchitectureComponent(Vertex vertex){
		var String name;
		var List<Port> inputPorts;
		var List<Port> outputPorts;
		if(vertex instanceof Actor){
			name = (vertex as Actor).simpleName;
			inputPorts = (vertex as Actor).inputs;
			outputPorts = (vertex as Actor).outputs;
		}else if(vertex instanceof Network){
			name = (vertex as Network).simpleName;
			inputPorts = (vertex as Network).inputs;
			outputPorts = (vertex as Network).outputs;
		} 
		'''
		-----------------------------------------------------------------------
		-- Component declaration
		-----------------------------------------------------------------------
		component «name»
		port(
		    «FOR port: inputPorts»
		    «addComponentPort(port,"IN","OUT")»
		    «ENDFOR»
		    «FOR port: outputPorts»
		    «addComponentPort(port,"OUT","IN")»
		    «ENDFOR»
	
		    clk: IN std_logic;
		    reset: IN std_logic;
		    start: IN std_logic;
		    done: OUT std_logic);
		end component «name»;
		'''
	}
	
	def addComponentPort(Port port, String dirA, String dirB){
		'''
		«IF port.type.bool || port.type.sizeInBits == 1»
			«getPortName(port)»_«IF dirA.equals("IN")»dout«ELSE»din«ENDIF» : «dirA» std_logic;
		«ELSE»
			«getPortName(port)»_«IF dirA.equals("IN")»dout«ELSE»din«ENDIF» : «dirA» std_logic_vector(«port.type.sizeInBits - 1» downto 0);
		«ENDIF»
		«getPortName(port)»_«IF dirA.equals("IN")»empty_n«ELSE»full_n«ENDIF» : IN std_logic;
		«getPortName(port)»_«IF dirA.equals("IN")»read«ELSE»write«ENDIF» : OUT std_logic;
		'''
	}
	
	def addArchitectureSignals(Vertex vertex){
		var String name;
		var String traceName;
		var List<Port> inputPorts;
		var List<Port> outputPorts;
		if(vertex instanceof Actor){
			name = (vertex as Actor).simpleName;
			traceName = (vertex as Actor).name;
			inputPorts = (vertex as Actor).inputs;
			outputPorts = (vertex as Actor).outputs;
		}else if(vertex instanceof Network){
			name = (vertex as Network).simpleName;
			traceName = (vertex as Network).simpleName;
			inputPorts = (vertex as Network).inputs;
			outputPorts = (vertex as Network).outputs;
		}
		'''
		-----------------------------------------------------------------------
		-- Achitecure signals & constants
		-----------------------------------------------------------------------
		«IF vertex instanceof Network»
			«FOR string: clockDomainsIndex.keySet SEPARATOR "\n"»
				constant «string»_PERIOD : time := 100 ns;
				constant «string»_DUTY_CYCLE : real := 0.5;
			«ENDFOR»
			constant OFFSET : time := 100 ns;
		«ELSE»
			constant CLK_PERIOD : time := 100 ns;
			constant CLK_DUTY_CYCLE : real := 0.5;
			constant OFFSET : time := 100 ns;
		«ENDIF»
		-- Severity level and testbench type types
		type severity_level is (note, warning, error, failure);
		type tb_type is (after_reset, read_file, CheckRead);
		
		-- Component input(s) signals
		«FOR port: inputPorts»
			signal tb_FSM_«getPortName(port)» : tb_type;
			file sim_file_«name»_«getPortName(port)» : text is "../../testbench/traces/«traceName»_«port.name».txt";
			«IF port.type.bool || port.type.sizeInBits == 1»
				signal «getPortName(port)»_dout : std_logic := '0';
			«ELSE»
				signal «getPortName(port)»_dout : std_logic_vector(«port.type.sizeInBits-1» downto 0) := (others => '0');
			«ENDIF»
			signal «getPortName(port)»_empty_n : std_logic := '0';
			signal «getPortName(port)»_read : std_logic;
		«ENDFOR»
		
		-- Component Output(s) signals
		«FOR port: outputPorts»
			signal tb_FSM_«getPortName(port)» : tb_type;
			file sim_file_«name»_«getPortName(port)» : text is "../../testbench/traces/«traceName»_«port.name».txt";
			«IF port.type.bool || port.type.sizeInBits == 1»
				signal «getPortName(port)»_din : std_logic := '0';
			«ELSE»
				signal «getPortName(port)»_din : std_logic_vector(«port.type.sizeInBits-1» downto 0) := (others => '0');
			«ENDIF»
			signal «getPortName(port)»_full_n : std_logic := '0';
			signal «getPortName(port)»_write : std_logic;
		«ENDFOR»
		
		«IF globalVerification»
		-- Actors output file
			«IF vertex instanceof Network»
				«FOR v : (vertex as Network).vertices»
					«IF v instanceof Actor»
						«FOR port : (v as Actor).outputs»
							file sim_file_«name»_«getPortName(port)» : text is "fifoTraces/«(v as Actor).simpleName»_«getPortName(port)».txt";
						«ENDFOR»
					«ENDIF»
				«ENDFOR»
			«ENDIF»
		«ENDIF»
		
		signal count : integer range 255 downto 0 := 0;
		«IF vertex instanceof Network»
			«FOR string: clockDomainsIndex.keySet»
				signal «string» : std_logic := '0';
			«ENDFOR»
		«ELSE»
			signal clk : std_logic := '0';
		«ENDIF»
		signal reset : std_logic := '0';
		signal start : std_logic := '0';
		
		«IF doubleBuffering»
			signal CG_EN : std_logic := '1';
		«ENDIF»
		'''
	}
	
	def addBeginBody(Vertex vertex){
		var String name;
		var List<Port> inputPorts;
		var List<Port> outputPorts;
		if(vertex instanceof Actor){
			name = (vertex as Actor).simpleName;
			inputPorts = (vertex as Actor).inputs;
			outputPorts = (vertex as Actor).outputs;
		}else if(vertex instanceof Network){
			name = (vertex as Network).simpleName;
			inputPorts = (vertex as Network).inputs;
			outputPorts = (vertex as Network).outputs;
		}
		'''
		
		i_«name» : «name» 
		port map(
			«FOR port: inputPorts SEPARATOR "\n"»
				«getPortName(port)»_dout => «getPortName(port)»_dout,
				«getPortName(port)»_empty_n => «getPortName(port)»_empty_n,
				«getPortName(port)»_read => «getPortName(port)»_read,
			«ENDFOR»
			
			«FOR port: outputPorts SEPARATOR "\n"»
				«getPortName(port)»_din => «getPortName(port)»_din,
				«getPortName(port)»_full_n => «getPortName(port)»_full_n,
				«getPortName(port)»_write => «getPortName(port)»_write,
			«ENDFOR»
			
			«IF doubleBuffering»
				CG_EN => CG_EN,
			«ENDIF»
			start => start,
			«IF vertex instanceof Network»
				«FOR string: clockDomainsIndex.keySet»
					«string» => «string»,
				«ENDFOR»
		    «ELSE»
				CLK => CLK,
			«ENDIF»
			reset => reset);
		
		
	
		-- Clock process
		
		
		clockProcess : process
		begin
		wait for OFFSET;
			clockLOOP : loop
				CLK <= '0';
				wait for (CLK_PERIOD - (CLK_PERIOD * CLK_DUTY_CYCLE));
				CLK <= '1';
				wait for (CLK_PERIOD * CLK_DUTY_CYCLE);
			end loop clockLOOP;
		end process;
	
		
		-- Reset process
		resetProcess : process
		begin
			wait for OFFSET;
			-- reset state for 100 ns.
			reset <= '1';
			start <= '1';
			wait for 100 ns;
			reset <= '0';
			wait;
		end process;
	
		
		-- Input(s) Waveform Generation
		WaveGen_Proc_In : process (CLK)
			variable Input_bit : integer range 2147483647 downto - 2147483648;
			variable line_number : line;
		begin
			if rising_edge(CLK) then
			«FOR port: inputPorts SEPARATOR "\n"»
			«IF (!port.native)»
				-- Input port: «getPortName(port)» Waveform Generation
					case tb_FSM_«getPortName(port)» is
						when after_reset =>
							count <= count + 1;
							if (count = 15) then
								tb_FSM_«getPortName(port)» <= read_file;
								count <= 0;
							end if;
						when read_file =>
							if (not endfile (sim_file_«name»_«getPortName(port)»)) then
								readline(sim_file_«name»_«getPortName(port)», line_number);
								if (line_number'length > 0 and line_number(1) /= '/') then
									read(line_number, input_bit);
									«IF port.type.bool || port.type.sizeInBits == 1»
									if (input_bit = 1) then
										«getPortName(port)»_data <= '1';
									else
										«getPortName(port)»_data <= '0';
									end if;
									«ELSE»
										«IF (port.type.uint)»
											«getPortName(port)»_dout <= std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»));
										«ELSE»
											«getPortName(port)»_dout <= std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»));
										«ENDIF»
									«ENDIF»
									«getPortName(port)»_empty_n <= '1';
									tb_FSM_«getPortName(port)» <= CheckRead;
								end if;
							end if;
						when CheckRead =>
							if (not endfile (sim_file_«name»_«getPortName(port)»)) and «getPortName(port)»_read = '1' then
								readline(sim_file_«name»_«getPortName(port)», line_number);
								if (line_number'length > 0 and line_number(1) /= '/') then
									read(line_number, input_bit);
									«IF port.type.bool || port.type.sizeInBits == 1»
									if (input_bit = 1) then
										«getPortName(port)»_dout <= '1';
									else
										«getPortName(port)»_dout <= '0';
									end if;
									«ELSE»
										«IF (port.type.uint)»
											«getPortName(port)»_dout <= std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»));
										«ELSE»
											«getPortName(port)»_dout <= std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»));
										«ENDIF»
									«ENDIF»
									«getPortName(port)»_empty_n <= '1';
								end if;
							elsif (endfile (sim_file_«name»_«getPortName(port)»)) then
								«getPortName(port)»_empty_n <= '0';
							end if;
						when others => null;
					end case;
			«ENDIF»
			«ENDFOR»
			end if;
		end process WaveGen_Proc_In;
		
		«IF !outputPorts.empty»
		-- Output(s) waveform Generation
		«ENDIF»
		«FOR port: outputPorts SEPARATOR "\n"»
			«IF (!port.native)»
				«getPortName(port)»_full_n <= '1';
			«ENDIF»
		«ENDFOR»
		
		WaveGen_Proc_Out : process (CLK)
			variable Input_bit   : integer range 2147483647 downto - 2147483648;
			variable line_number : line;
			«FOR port_out: outputPorts SEPARATOR "\n"»
			variable sequence_«getPortName(port_out)» : integer := 0;
			«ENDFOR»
		begin
			if (rising_edge(CLK)) then
			«FOR port: outputPorts SEPARATOR "\n"»
			«IF (!port.native)»
			-- Output port: «getPortName(port)» Waveform Generation
				if (not endfile (sim_file_«name»_«getPortName(port)») and «getPortName(port)»_write = '1') then
					readline(sim_file_«name»_«getPortName(port)», line_number);
						if (line_number'length > 0 and line_number(1) /= '/') then
							read(line_number, input_bit);
							«IF port.type.bool || port.type.sizeInBits == 1»
							if (input_bit = 1) then
								assert («getPortName(port)»_din = '1')
								report "on port «getPortName(port)» incorrect value computed : '0' instead of : '1' sequence " & str(sequence_«getPortName(port)»)
								severity failure;
								
								assert («getPortName(port)»_din = '0')
								report "on port «getPortName(port)» correct value computed : '1' instead of : '1' sequence " & str(sequence_«getPortName(port)»)
								severity note;
							else
								assert («getPortName(port)»_din = '0')
								report "on port «getPortName(port)» incorrect value computed : '1' instead of : '0' sequence " & str(sequence_«getPortName(port)»)
								severity failure;
								
								assert («getPortName(port)»_din = '1')
								report "on port «getPortName(port)» correct value computed : '0' instead of : '0' sequence " & str(sequence_«getPortName(port)»)
								severity note;
							end if;
							sequence_«getPortName(port)» := sequence_«getPortName(port)» + 1;
							«ELSEIF port.type.int»
							assert («getPortName(port)»_din  = std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»)))
							report "on port «getPortName(port)» incorrect value computed : " & str(to_integer(signed(«getPortName(port)»_din))) & " instead of : " & str(input_bit) & " sequence " & str(sequence_«getPortName(port)»)
							severity failure;
							
							assert («getPortName(port)»_din /= std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»)))
							report "on port «getPortName(port)» correct value computed : " & str(to_integer(signed(«getPortName(port)»_din))) & " equals : " & str(input_bit) & " sequence " & str(sequence_«getPortName(port)»)
							severity note;
							sequence_«getPortName(port)» := sequence_«getPortName(port)» + 1;
							«ELSEIF port.type.uint»
							assert («getPortName(port)»_din  = std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»)))
							report "on port «getPortName(port)» incorrect value computed : " & str(to_integer(unsigned(«getPortName(port)»_din))) & " instead of : " & str(input_bit) & " sequence " & str(sequence_«getPortName(port)»)
							severity failure;
							
							assert («getPortName(port)»_din /= std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»)))
							report "on port «getPortName(port)» correct value computed : " & str(to_integer(unsigned(«getPortName(port)»_din))) & " equals : " & str(input_bit) & " sequence " & str(sequence_«getPortName(port)»)
							severity note;
							sequence_«getPortName(port)» := sequence_«getPortName(port)» + 1;
							«ENDIF»
						end if;
				end if;
			«ENDIF»
			«ENDFOR»
			end if;			
		end process WaveGen_Proc_Out;
		'''
	}
	
	def getContent(){
		'''
		«header»
		
		«addLibraries()»
		
		«addEntity(vertex)»
		
		«addArchitecture(vertex)»
		'''
	}
	
	
	def getPortName(Port port){
		var String name = port.name
		if(name.equals("In") || name.equals("Out") || name.equals("IN")|| name.equals("OUT")|| name.equals("Bit")|| name.equals("BIT")){
			name = port.name + "_r"
		}
		'''«name»'''
	}
	

	
	def void computeNetworkClockDomains(Network network,
			Map<String, String> clockDomains) {
		clockDomainsIndex = new HashMap<String, Integer>();
		// For each instance on the network give the clock domain specified by
		// the mapping configuration tab or if not give the default clock domain
		var int clkIndex = 0;
		clockDomainsIndex.put(DEFAULT_CLOCK_DOMAIN, clkIndex);
		clkIndex = clkIndex + 1;

		for (String string : clockDomains.values()) {
			if (!string.isEmpty() && !clockDomainsIndex.containsKey(string)) {
				clockDomainsIndex.put(string, clkIndex);
				clkIndex = clkIndex + 1;
			}
		}
	}
}


