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
package org.xronos.orcc.backend

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
class TestbenchPrinter {
	
	var Boolean goDone;
	var Boolean generateWeights;
	
	var Boolean globalVerification = false;
	
	var Boolean doubleBuffering = false;
	
	/**
	 * Contains a Map which indicates the index of the given clock
	 */

	var Map<String, Integer> clockDomainsIndex;
	var String DEFAULT_CLOCK_DOMAIN = "CLK";	
	
	def headerComments(Object object, String string){
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
		-- ----------------------------------------------------------------------------
		-- __  ___ __ ___  _ __   ___  ___ 
		-- \ \/ / '__/ _ \| '_ \ / _ \/ __|
		--  >  <| | | (_) | | | | (_) \__ \
		-- /_/\_\_|  \___/|_| |_|\___/|___/
		-- ----------------------------------------------------------------------------
		-- Xronos synthesizer
		«IF object instanceof Network »
		-- «IF string.equals("")»Testbench for Network:«ELSE»«string»«ENDIF» «(object as Network).simpleName» 
		«ELSEIF object instanceof Actor»
		-- Testbench for Instance: «(object as Actor).simpleName» 
		«ENDIF»
		-- Date: «dateFormat.format(date)»
		-- ----------------------------------------------------------------------------
		'''	
	}
	
	def addLibraries(){
		'''
		library ieee, SystemBuilder;
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
		    «IF generateWeights»
		    	«addGoDoneComponentPort((vertex as Network))»
		    «ENDIF»
		    «IF doubleBuffering»
		    	CG_EN : IN std_logic;
		    «ENDIF»
		    «IF vertex instanceof Network»
		    	«FOR string: clockDomainsIndex.keySet»
		    		«string» : in std_logic;
		    	«ENDFOR»
		    «ELSE»
		    CLK: IN std_logic;
		    «ENDIF»
		    RESET: IN std_logic);
		end component «name»;
		'''
	}
	
	def addComponentPort(Port port, String dirA, String dirB){
		'''
		«IF port.type.bool || port.type.sizeInBits == 1»
		«port.name»_data : «dirA» std_logic;
		«ELSE»
		«port.name»_data : «dirA» std_logic_vector(«port.type.sizeInBits - 1» downto 0);
		«ENDIF»
		«port.name»_send : «dirA» std_logic;
		«port.name»_ack : «dirB» std_logic;
		«IF dirA.equals("OUT")»
		«port.name»_rdy : «dirB» std_logic;
		«ENDIF»
		«port.name»_count : «dirA» std_logic_vector(15 downto 0);
		'''
	}
	
	def addGoDoneComponentPort(Network network){
		'''
		«FOR vertex: network.vertices»
				«IF vertex instanceof Actor»
					«FOR action: (vertex as Actor).actions SEPARATOR "\n"»
					«(vertex as Actor).simpleName»_«action.name»_go : OUT std_logic;
					«(vertex as Actor).simpleName»_«action.name»_done : OUT std_logic;
					«ENDFOR»
				«ENDIF»
		«ENDFOR»
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
			constant PERIOD : time := 100 ns;
			constant DUTY_CYCLE : real := 0.5;
			constant OFFSET : time := 100 ns;
		«ENDIF»
		-- Severity level and testbench type types
		type severity_level is (note, warning, error, failure);
		type tb_type is (after_reset, read_file, CheckRead);
		
		-- Component input(s) signals
		«FOR port: inputPorts»
		signal tb_FSM_«port.name» : tb_type;
		file sim_file_«name»_«port.name» : text is "fifoTraces/«traceName»_«port.name».txt";
		«IF port.type.bool || port.type.sizeInBits == 1»
		signal «port.name»_data : std_logic := '0';
		«ELSE»
		signal «port.name»_data : std_logic_vector(«port.type.sizeInBits-1» downto 0) := (others => '0');
		«ENDIF»
		signal «port.name»_send : std_logic := '0';
		signal «port.name»_ack : std_logic;
		signal «port.name»_rdy : std_logic;
		signal «port.name»_count : std_logic_vector(15 downto 0) := (others => '0');
		-- Input component queue
		«IF port.type.bool || port.type.sizeInBits == 1»
		signal q_«port.name»_data : std_logic := '0';
		«ELSE»
		signal q_«port.name»_data : std_logic_vector(«port.type.sizeInBits-1» downto 0) := (others => '0');
		«ENDIF»
		signal q_«port.name»_send : std_logic := '0';
		signal q_«port.name»_ack : std_logic;
		signal q_«port.name»_rdy : std_logic;
		signal q_«port.name»_count : std_logic_vector(15 downto 0) := (others => '0');
		«ENDFOR»
		
		-- Component Output(s) signals
		«FOR port: outputPorts»
		signal tb_FSM_«port.name» : tb_type;
		file sim_file_«name»_«port.name» : text is "fifoTraces/«traceName»_«port.name».txt";
		«IF port.type.bool || port.type.sizeInBits == 1»
		signal «port.name»_data : std_logic := '0';
		«ELSE»
		signal «port.name»_data : std_logic_vector(«port.type.sizeInBits-1» downto 0) := (others => '0');
		«ENDIF»
		signal «port.name»_send : std_logic;
		signal «port.name»_ack : std_logic := '0';
		signal «port.name»_rdy : std_logic := '0';
		signal «port.name»_count : std_logic_vector(15 downto 0) := (others => '0');
		«ENDFOR»
		
		«IF globalVerification»
		-- Actors output file
			«IF vertex instanceof Network»
				«FOR v : (vertex as Network).vertices»
					«IF v instanceof Actor»
						«FOR port : (v as Actor).outputs»
							file sim_file_«name»_«port.name» : text is "fifoTraces/«(v as Actor).simpleName»_«port.name».txt";
						«ENDFOR»
					«ENDIF»
				«ENDFOR»
			«ENDIF»
		«ENDIF»
	
		«IF generateWeights»
		-- Actions Go Done signals
		«FOR actor: (vertex as Network).allActors»
					«FOR action: actor.actions SEPARATOR "\n"»
						signal «actor.simpleName»_«action.name»_go : std_logic;
						signal «actor.simpleName»_«action.name»_done : std_logic;
					«ENDFOR»
				«ENDFOR»
		«ENDIF»
		-- GoDone Weights Output Files
		«IF generateWeights»
			«FOR actor: (vertex as Network).allActors»
				«FOR action: actor.actions SEPARATOR "\n"»
					file f_«actor.simpleName»_«action.name»: TEXT;
				«ENDFOR»
			«ENDFOR»
		«ENDIF»
		
		signal count : integer range 255 downto 0 := 0;
		«IF vertex instanceof Network»
			«FOR string: clockDomainsIndex.keySet»
				signal «string» : std_logic := '0';
			«ENDFOR»
		«ELSE»
			signal CLK : std_logic := '0';
		«ENDIF»
		signal reset : std_logic := '0';
		
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
		«IF generateWeights»
			«FOR actor: (vertex as Network).allActors»
				«FOR action: actor.actions SEPARATOR "\n"»
					file_open(f_«actor.simpleName»_«action.name», "weights/«actor.simpleName»_«action.name».txt", WRITE_MODE);
				«ENDFOR»
			«ENDFOR»
		«ENDIF»
		
		i_«name» : «name» 
		port map(
			«FOR port: inputPorts SEPARATOR "\n"»
				«port.name»_data => q_«port.name»_data,
				«port.name»_send => q_«port.name»_send,
				«port.name»_ack => q_«port.name»_ack,
				«port.name»_count => q_«port.name»_count,
			«ENDFOR»
			
			«FOR port: outputPorts SEPARATOR "\n"»
				«port.name»_data => «port.name»_data,
				«port.name»_send => «port.name»_send,
				«port.name»_ack => «port.name»_ack,
				«port.name»_rdy => «port.name»_rdy,
				«port.name»_count => «port.name»_count,
			«ENDFOR»
			«IF generateWeights»
				«FOR actor: (vertex as Network).allActors»
					«FOR action: actor.actions SEPARATOR "\n"»
						«actor.simpleName»_«action.name»_go => «actor.simpleName»_«action.name»_go, 
						«actor.simpleName»_«action.name»_done => «actor.simpleName»_«action.name»_done,
					«ENDFOR»
				«ENDFOR»
			«ENDIF»
			«IF doubleBuffering»
				CG_EN => CG_EN,
			«ENDIF»
			«IF vertex instanceof Network»
				«FOR string: clockDomainsIndex.keySet»
					«string» => «string»,
				«ENDFOR»
		    «ELSE»
				CLK => CLK,
			«ENDIF»
			reset => reset);
		
		-- Input(s) queues
		«FOR port: inputPorts SEPARATOR "\n"»
		q_«port.name» : entity systemBuilder.Queue(behavioral)
		generic map(length => 512, width => «IF port.type.bool || port.type.sizeInBits == 1»1«ELSE»«port.type.sizeInBits»«ENDIF»)
		port map(
			«IF port.type.bool || port.type.sizeInBits == 1»
			OUT_DATA(0) => q_«port.name»_data,
			«ELSE»
			OUT_DATA => q_«port.name»_data,
			«ENDIF»
			OUT_SEND => q_«port.name»_send,
			OUT_ACK => q_«port.name»_ack,
			OUT_COUNT => q_«port.name»_count,
		
			«IF port.type.bool || port.type.sizeInBits == 1»
			IN_DATA(0)  => «port.name»_data,
			«ELSE»
			IN_DATA => «port.name»_data,
			«ENDIF»
			IN_SEND => «port.name»_send,
			IN_ACK => «port.name»_ack,
			IN_RDY => «port.name»_rdy,
			IN_COUNT => «port.name»_count,

			CLK => CLK,
			reset => reset);
		«ENDFOR»
	
		-- Clock process
		
		«IF vertex instanceof Network»
			«FOR string: clockDomainsIndex.keySet SEPARATOR "\n"»
				«string»_clockProcess : process
					begin
					wait for OFFSET;
						clockLOOP : loop
							«string» <= '0';
							wait for («string»_PERIOD - («string»_PERIOD * «string»_DUTY_CYCLE));
							«string» <= '1';
							wait for («string»_PERIOD * «string»_DUTY_CYCLE);
						end loop clockLOOP;
				end process;
			«ENDFOR»
		«ELSE»
			clockProcess : process
			begin
			wait for OFFSET;
				clockLOOP : loop
					CLK <= '0';
					wait for (PERIOD - (PERIOD * DUTY_CYCLE));
					CLK <= '1';
					wait for (PERIOD * DUTY_CYCLE);
				end loop clockLOOP;
			end process;
		«ENDIF»
		
		-- Reset process
		resetProcess : process
		begin
			wait for OFFSET;
			-- reset state for 100 ns.
			RESET <= '1';
			wait for 100 ns;
			RESET <= '0';
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
				-- Input port: «port.name» Waveform Generation
					case tb_FSM_«port.name» is
						when after_reset =>
							count <= count + 1;
							if (count = 15) then
								tb_FSM_«port.name» <= read_file;
								count <= 0;
							end if;
						when read_file =>
							if (not endfile (sim_file_«name»_«port.name»)) then
								readline(sim_file_«name»_«port.name», line_number);
								if (line_number'length > 0 and line_number(1) /= '/') then
									read(line_number, input_bit);
									«IF port.type.bool || port.type.sizeInBits == 1»
									if (input_bit = 1) then
										«port.name»_data <= '1';
									else
										«port.name»_data <= '0';
									end if;
									«ELSE»
										«IF (port.type.uint)»
											«port.name»_data <= std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»));
										«ELSE»
											«port.name»_data <= std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»));
										«ENDIF»
									«ENDIF»
									«port.name»_send <= '1';
									tb_FSM_«port.name» <= CheckRead;
								end if;
							end if;
						when CheckRead =>
							if (not endfile (sim_file_«name»_«port.name»)) and «port.name»_ack = '1' then
								readline(sim_file_«name»_«port.name», line_number);
								if (line_number'length > 0 and line_number(1) /= '/') then
									read(line_number, input_bit);
									«IF port.type.bool || port.type.sizeInBits == 1»
									if (input_bit = 1) then
										«port.name»_data <= '1';
									else
										«port.name»_data <= '0';
									end if;
									«ELSE»
										«IF (port.type.uint)»
											«port.name»_data <= std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»));
										«ELSE»
											«port.name»_data <= std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»));
										«ENDIF»
									«ENDIF»
									«port.name»_send <= '1';
								end if;
							elsif (endfile (sim_file_«name»_«port.name»)) then
								«port.name»_send <= '0';
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
				«port.name»_ack <= «port.name»_send;
				«port.name»_rdy <= '1';
			«ENDIF»
		«ENDFOR»
		
		WaveGen_Proc_Out : process (CLK)
			variable Input_bit   : integer range 2147483647 downto - 2147483648;
			variable line_number : line;
			«FOR port_out: outputPorts SEPARATOR "\n"»
			variable sequence_«port_out.name» : integer := 0;
			«ENDFOR»
		begin
			if (rising_edge(CLK)) then
			«FOR port: outputPorts SEPARATOR "\n"»
			«IF (!port.native)»
			-- Output port: «port.name» Waveform Generation
				if (not endfile (sim_file_«name»_«port.name») and «port.name»_send = '1') then
					readline(sim_file_«name»_«port.name», line_number);
						if (line_number'length > 0 and line_number(1) /= '/') then
							read(line_number, input_bit);
							«IF port.type.bool || port.type.sizeInBits == 1»
							if (input_bit = 1) then
								assert («port.name»_data = '1')
								report "on port «port.name» incorrect value computed : '0' instead of : '1' sequence " & str(sequence_«port.name»)
								severity failure;
								
								assert («port.name»_data = '0')
								report "on port «port.name» correct value computed : '1' instead of : '1' sequence " & str(sequence_«port.name»)
								severity note;
							else
								assert («port.name»_data = '0')
								report "on port «port.name» incorrect value computed : '1' instead of : '0' sequence " & str(sequence_«port.name»)
								severity failure;
								
								assert («port.name»_data = '1')
								report "on port «port.name» correct value computed : '0' instead of : '0' sequence " & str(sequence_«port.name»)
								severity note;
							end if;
							sequence_«port.name» := sequence_«port.name» + 1;
							«ELSEIF port.type.int»
							assert («port.name»_data  = std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»)))
							report "on port «port.name» incorrect value computed : " & str(to_integer(signed(«port.name»_data))) & " instead of : " & str(input_bit) & " sequence " & str(sequence_«port.name»)
							severity failure;
							
							assert («port.name»_data /= std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»)))
							report "on port «port.name» correct value computed : " & str(to_integer(signed(«port.name»_data))) & " equals : " & str(input_bit) & " sequence " & str(sequence_«port.name»)
							severity note;
							sequence_«port.name» := sequence_«port.name» + 1;
							«ELSEIF port.type.uint»
							assert («port.name»_data  = std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»)))
							report "on port «port.name» incorrect value computed : " & str(to_integer(unsigned(«port.name»_data))) & " instead of : " & str(input_bit) & " sequence " & str(sequence_«port.name»)
							severity failure;
							
							assert («port.name»_data /= std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»)))
							report "on port «port.name» correct value computed : " & str(to_integer(unsigned(«port.name»_data))) & " equals : " & str(input_bit) & " sequence " & str(sequence_«port.name»)
							severity note;
							sequence_«port.name» := sequence_«port.name» + 1;
							«ENDIF»
						end if;
				end if;
			«ENDIF»
			«ENDFOR»
			end if;			
		end process WaveGen_Proc_Out;
		
		«IF generateWeights»
			«IF vertex instanceof Network»
				GoDoneWriteProcess : process(CLK, RESET)
					variable countClk: integer := 0;
					variable l : line;
				begin
					if (RESET = '1' ) then			
						countClk := 0;
					elsif (rising_edge(CLK)) then
						«FOR actor: (vertex as Network).allActors»
							«FOR action: actor.actions SEPARATOR "\n"»
								write(l, string'(integer'image(countClk)&";"&str(«actor.simpleName»_«action.name»_go)&";"&str(«actor.simpleName»_«action.name»_done)&";"));
								writeline(f_«actor.simpleName»_«action.name», l );
							«ENDFOR»
						«ENDFOR»
						countClk := countClk + 100;			
					end if;
				end process GoDoneWriteProcess;
			«ENDIF»   
		«ENDIF»
		'''
	}
		
	def printInstance(Actor actor, Map<String,Object> options){
		goDone = false
		generateWeights = false
		'''
		«headerComments(actor,"")»
		
		«addLibraries()»
		
		«addEntity(actor)»
		
		«addArchitecture(actor)»
		'''
	}
	
	def printNetwork(Network network, Map<String,Object> options){
		goDone = false
		generateWeights = false
		if (options.containsKey("generateWeights")) {
			generateWeights = options.get("generateWeights") as Boolean
			goDone = true
		}
		var Map<String,String> clkDomains = new HashMap<String,String>(); 
		
		if (options.containsKey("clkDomains")) {
			clkDomains = options.get("clkDomains") as Map<String,String>; 
		}
		
		if (options.containsKey("doubleBuffering")) {
			doubleBuffering = options.get("doubleBuffering") as Boolean; 
		}
		
		computeNetworkClockDomains(network,clkDomains);
		
		'''
		«headerComments(network,"")»
		
		«addLibraries()»
		
		«addEntity(network)»
		
		«addArchitecture(network)»
		'''
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


