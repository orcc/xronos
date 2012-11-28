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
import java.util.Map
import net.sf.orcc.df.Instance
import net.sf.orcc.df.Network
import net.sf.orcc.ir.util.IrSwitch
import net.sf.orcc.graph.Vertex
import net.sf.orcc.df.Port
import java.util.List

/*
 * A VHDL Testbench printer
 * 
 * @author Endri Bezati
 */
class TestbenchPrinter extends IrSwitch {
	var Boolean goDone;
	
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
		«ELSEIF object instanceof Instance»
		-- Testbench for Instance: «(object as Instance).simpleName» 
		«ENDIF»
		-- Date: «dateFormat.format(date)»
		-- ----------------------------------------------------------------------------
		'''	
	}
	
	def addLibraries(){
		'''
		«IF goDone»
			library ieee;
			use ieee.std_logic_1164.all;
			use ieee.std_logic_unsigned.all;
			use ieee.numeric_std.all;

			library std;
			use std.textio.all;

			library work;
		«ELSE»
			library ieee, SystemBuilder;
			use ieee.std_logic_1164.all;
			use ieee.std_logic_unsigned.all;
			use ieee.numeric_std.all;
			use std.textio.all;

			library work;
			use work.sim_package.all;
		«ENDIF»
		'''
	}
	
	def addEntity(Vertex vertex){
		var String name;
		if(vertex instanceof Instance){
			name = (vertex as Instance).simpleName;
		}else if(vertex instanceof Network){
			name = (vertex as Network).simpleName;
		} 
		if(goDone){
			name = name + "_goDone"
		}
		'''
		entity «name»_tb is
		end «name»_tb;
		'''
	}
	
	def addArchitecture(Vertex vertex){
		var String name;
		if(vertex instanceof Instance){
			name = (vertex as Instance).simpleName;
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
	
	def addArchitectureGoDone(Network network){
		'''
		architecture arch_«network.simpleName»_goDone_tb of «network.simpleName»_goDone_tb is
			----------------------------------------------------------------------------
			-- Output Files
			----------------------------------------------------------------------------
			«FOR vertex: network.vertices»
				«IF vertex instanceof Instance»
					«FOR action: (vertex as Instance).actor.actions SEPARATOR "\n"»
					file f_«(vertex as Instance).simpleName»_«action.name»: TEXT;
					«ENDFOR»
				«ENDIF»
			«ENDFOR»
			
			----------------------------------------------------------------------------
			-- Component declaration
			----------------------------------------------------------------------------
			«addArchitectureComponent(network)»
			
			----------------------------------------------------------------------------
			-- Network Output Signlas
			----------------------------------------------------------------------------
			«FOR port: network.outputs»
				«IF port.type.bool || port.type.sizeInBits == 1»
				signal «port.name»_DATA : std_logic;
				«ELSE»
				signal «port.name»_DATA : std_logic_vector(«port.type.sizeInBits-1» downto 0);
				«ENDIF»
				signal «port.name»_SEND : std_logic;
				signal «port.name»_ACK : std_logic;
				signal «port.name»_COUNT : std_logic_vector(15 downto 0);
			«ENDFOR»
			
			----------------------------------------------------------------------------
			-- Actions GO and DONE
			----------------------------------------------------------------------------
			«FOR vertex: network.vertices»
				«IF vertex instanceof Instance»
					«FOR action: (vertex as Instance).actor.actions SEPARATOR "\n"»
					signal «(vertex as Instance).simpleName»_«action.name»_go : std_logic;
					signal «(vertex as Instance).simpleName»_«action.name»_done : std_logic;
					«ENDFOR»
				«ENDIF»
			«ENDFOR»
			---------------------------------------------------------------------------
			-- Constant and Clock, Reset declaration
			--------------------------------------------------------------------------- 
			constant PERIOD : time := 50 ns;
			constant DUTY_CYCLE : real := 0.5;
			constant OFFSET : time := 100 ns;
		
			signal CLK : std_logic := '0';
		 	signal RESET : std_logic := '0';
			
			---------------------------------------------------------------------------
			-- Functions declaration
			--------------------------------------------------------------------------- 
			function chr(sl : std_logic) return character is
				variable c : character;
			begin
				case sl is
					when 'U' => c := 'U';
					when 'X' => c := 'X';
					when '0' => c := '0';
					when '1' => c := '1';
					when 'Z' => c := 'Z';
					when 'W' => c := 'W';
					when 'L' => c := 'L';
					when 'H' => c := 'H';
					when '-' => c := '-';
				end case;
				return c;
			end chr;
			
			function str(sl : std_logic) return string is
			variable 
				s : string(1 to 1);
			begin
				s(1) := chr(sl);
				return s;
			end str;
		
		begin
			-- Output Files 
			«FOR vertex: network.vertices»
				«IF vertex instanceof Instance»
					«FOR action: (vertex as Instance).actor.actions SEPARATOR "\n"»
					file_open(f_«(vertex as Instance).simpleName»_«action.name», "weights/«(vertex as Instance).simpleName»_«action.name».txt", WRITE_MODE);
					«ENDFOR»
				«ENDIF»
			«ENDFOR»
			
			-- Instantiate Network component with signals
			n_«network.simpleName» : component «network.simpleName»
			port map(
				«FOR port: network.outputs SEPARATOR "\n"»
					«port.name»_DATA => «port.name»_DATA,
					«port.name»_SEND => «port.name»_SEND,
					«port.name»_ACK => «port.name»_ACK,
					«port.name»_RDY => «port.name»_RDY,
					«port.name»_COUNT => «port.name»_COUNT,
				«ENDFOR»
				«FOR vertex: network.vertices»
					«IF vertex instanceof Instance»
						«FOR action: (vertex as Instance).actor.actions SEPARATOR "\n"»
						«(vertex as Instance).simpleName»_«action.name»_go => «(vertex as Instance).simpleName»_«action.name»_go, 
						«(vertex as Instance).simpleName»_«action.name»_done => «(vertex as Instance).simpleName»_«action.name»_done,
						«ENDFOR»
					«ENDIF»
				«ENDFOR»
				CLK => CLK,
				RESET => RESET
			);
			
			-- Make a virtual infinite queue for the Output(s)
			«FOR port: network.outputs»
				«port.name»_ACK <= «port.name»_SEND;
				«port.name»_RDY <='1';
			«ENDFOR»
			
				clKProcess : process
				begin
					wait for OFFSET;
			    clock_LOOP : loop
			      CLK <= '0';
			      wait for (PERIOD - (PERIOD * DUTY_CYCLE));
			      CLK <= '1';
			      wait for (PERIOD * DUTY_CYCLE);
			    end loop clock_LOOP;
				end process;
			
				resetProcess : process
				begin		
					wait for OFFSET;
					-- reset state for 500 ns.
			    	RESET <= '1';
					wait for 500 ns;
					RESET <= '0';	
					wait;
				end process;
				
				writeProcess : process(CLK, RESET)
				variable countClk: integer := 0;
				variable l : line;
				
				begin
					if (RESET = '1' ) then			
						countClk := 0;
					elsif (rising_edge(CLK)) then
						«FOR vertex: network.vertices»
							«IF vertex instanceof Instance»
								«FOR action: (vertex as Instance).actor.actions SEPARATOR "\n"»
									write(l, string'(integer'image(countClk)&";"&str(«(vertex as Instance).simpleName»_«action.name»_go)&";"&str(«(vertex as Instance).simpleName»_«action.name»_done)&";"));
									writeline(f_«(vertex as Instance).simpleName»_«action.name», l );
								«ENDFOR»
							«ENDIF»
						«ENDFOR»
						countClk := countClk + 100;			
					end if;
				end process;   
		end architecture arch_«network.simpleName»_goDone_tb; 
		'''
	}
	
	def addArchitectureComponent(Vertex vertex){
		var String name;
		var List<Port> inputPorts;
		var List<Port> outputPorts;
		if(vertex instanceof Instance){
			name = (vertex as Instance).simpleName;
			inputPorts = (vertex as Instance).actor.inputs;
			outputPorts = (vertex as Instance).actor.outputs;
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
		    «IF goDone»
		    	«addGoDoneComponentPort((vertex as Network))»
		    «ENDIF»
		    CLK: IN std_logic;
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
				«IF vertex instanceof Instance»
					«FOR action: (vertex as Instance).actor.actions SEPARATOR "\n"»
					«(vertex as Instance).simpleName»_«action.name»_go : OUT std_logic;
					«(vertex as Instance).simpleName»_«action.name»_done : OUT std_logic;
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
		if(vertex instanceof Instance){
			name = (vertex as Instance).simpleName;
			traceName = (vertex as Instance).name;
			inputPorts = (vertex as Instance).actor.inputs;
			outputPorts = (vertex as Instance).actor.outputs;
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
		constant PERIOD : time := 20 ns;
		constant DUTY_CYCLE : real := 0.5;
		constant OFFSET : time := 100 ns;
		
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
		
		signal count : integer range 255 downto 0 := 0;
		signal clk : std_logic := '0';
		signal reset : std_logic := '0';
		'''
	}
	
	def addBeginBody(Vertex vertex){
		var String name;
		var List<Port> inputPorts;
		var List<Port> outputPorts;
		if(vertex instanceof Instance){
			name = (vertex as Instance).simpleName;
			inputPorts = (vertex as Instance).actor.inputs;
			outputPorts = (vertex as Instance).actor.outputs;
		}else if(vertex instanceof Network){
			name = (vertex as Network).simpleName;
			inputPorts = (vertex as Network).inputs;
			outputPorts = (vertex as Network).outputs;
		}
		'''
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
			clk => clk,
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

			clk => clk,
			reset => reset);
		«ENDFOR»
	
		-- Clock process
		clockProcess : process
		begin
		wait for OFFSET;
			clock_LOOP : loop
				clk <= '0';
				wait for (PERIOD - (PERIOD * DUTY_CYCLE));
				clk <= '1';
				wait for (PERIOD * DUTY_CYCLE);
			end loop clock_LOOP;
		end process;
	
		-- Reset process
		resetProcess : process
		begin
			wait for OFFSET;
			-- reset state for 100 ns.
			reset <= '1';
			wait for 100 ns;
			reset <= '0';
			wait;
		end process;
	
		
		-- Input(s) Waveform Generation
		WaveGen_Proc_In : process (clk)
			variable Input_bit : integer range 2147483647 downto - 2147483648;
			variable line_number : line;
		begin
			if rising_edge(clk) then
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
										«port.name»_data <= '1';
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
										«port.name»_data <= '1';
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
		
		WaveGen_Proc_Out : process (clk)
			variable Input_bit   : integer range 2147483647 downto - 2147483648;
			variable line_number : line;
		begin
			if (rising_edge(clk)) then
			«FOR port: outputPorts SEPARATOR "\n"»
			«IF (!port.native)»
			-- Output port: «port.name» Waveform Generation
				if (not endfile (sim_file_«name»_«port.name») and «port.name»_send = '1') then
					readline(sim_file_«name»_«port.name», line_number);
						if (line_number'length > 0 and line_number(1) /= '/') then
							read(line_number, input_bit);
							«IF port.type.bool»
							if (input_bit = 1) then
								assert («port.name»_data = '1')
								report "on port «port.name» incorrectly value computed : '0' instead of : '1'"
								severity failure;
								
								assert («port.name»_data = '0')
								report "on port «port.name» correctly value computed : '1' instead of : '1'"
								severity note;
							else
								assert («port.name»_data = '0')
								report "on port «port.name» incorrectly value computed : '1' instead of : '0'"
								severity failure;
								
								assert («port.name»_data = '1')
								report "on port «port.name» correctly value computed : '0' instead of : '0'"
								severity note;
							end if;
							«ELSEIF port.type.int»
							assert («port.name»_data  = std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»)))
							report "on port «port.name» incorrectly value computed : " & str(to_integer(signed(«port.name»_data))) & " instead of :" & str(input_bit)
							severity failure;
							
							assert («port.name»_data /= std_logic_vector(to_signed(input_bit, «port.type.sizeInBits»)))
							report "on port «port.name» correct value computed : " & str(to_integer(signed(«port.name»_data))) & " equals :" & str(input_bit)
							severity note;
							«ELSEIF port.type.uint»
							assert («port.name»_data  = std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»)))
							report "on port «port.name» incorrectly value computed : " & str(to_integer(signed(«port.name»_data))) & " instead of :" & str(input_bit)
							severity failure;
							
							assert («port.name»_data /= std_logic_vector(to_unsigned(input_bit, «port.type.sizeInBits»)))
							report "on port «port.name» correct value computed : " & str(to_integer(signed(«port.name»_data))) & " equals :" & str(input_bit)
							severity note;
							«ENDIF»
						end if;
				end if;
			«ENDIF»
			«ENDFOR»
			end if;
		end process WaveGen_Proc_Out;
		'''
	}
	
	def printGoDone(Network network, Map<String,Object> options){
		goDone = true;
		'''
		«headerComments(network,"Go and Done Generator :")»
		
		«addLibraries()»
		
		«addEntity(network)»
		'''
	}
	
	def printInstance(Instance instance, Map<String,Object> options){
		goDone = false;
		'''
		«headerComments(instance,"")»
		
		«addLibraries()»
		
		«addEntity(instance)»
		
		«addArchitecture(instance)»
		'''
	}
	
	def printNetwork(Network network, Map<String,Object> options){
		goDone = false;
		'''
		«headerComments(network,"")»
		
		«addLibraries()»
		
		«addEntity(network)»
		
		«addArchitecture(network)»
		'''
	}
	
}