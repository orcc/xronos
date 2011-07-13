-- ---------------------------------------------------------------------------------
-- Copyright (c) 2011, EPFL
-- All rights reserved.
-- 
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are met:
-- 
--   * Redistributions of source code must retain the above copyright notice,
--     this list of conditions and the following disclaimer.
--   * Redistributions in binary form must reproduce the above copyright notice,
--     this list of conditions and the following disclaimer in the documentation
--     and/or other materials provided with the distribution.
--   * Neither the name of the IETR/INSA of Rennes nor the names of its
--     contributors may be used to endorse or promote products derived from this
--     software without specific prior written permission.
-- 
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
-- AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
-- IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
-- ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
-- LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
-- CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
-- SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
-- INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
-- STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
-- WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
-- SUCH DAMAGE.
-- ---------------------------------------------------------------------------------

-- ---------------------------------------------------------------------------------
-- Author: Endri BEZATI <endri.bezati@epfl.ch> 
-- systemActors Library
-- Input/Output Actors, Source v. 0.1
-- ---------------------------------------------------------------------------------

LIBRARY ieee;
USE ieee.std_logic_1164.ALL;
USE ieee.std_logic_unsigned.all;
USE ieee.numeric_std.ALL;
LIBRARY SystemActors;
USE SystemActors.sa_types.all;

-- ---------------------------------------------------------------------------------
-- Source Entity

entity Source is
	generic (fileName : String);	
	port (
		Out_DATA	: out std_logic_vector(8 downto 0);
		Out_SEND	: out std_logic;
		Out_ACK		: in  std_logic;
		Out_COUNT	: out std_logic_vector(15 downto 0);
		Out_RDY		: in  std_logic;
		CLK		: in  std_logic;
		RESET		: in  std_logic);
end Source;

-- ---------------------------------------------------------------------------------
-- Source behavioral

architecture behavioral of Source is
	file sourceData : sa_ByteFileType open read_mode is fileName;
	signal internal_SEND : std_logic;
begin
  
	p_read: process(CLK, RESET)
		
		variable v_data : sa_Byte;
		
	begin
		if RESET = '1' then
			internal_SEND <= '0';			
			Out_DATA <= "000000000";
		elsif (CLK='1' and CLK'event)  then
			internal_SEND <= '1';				
			if Out_ACK = '1' then 		
				if not endfile (sourceData) then			
					read(sourceData,v_data);
					Out_DATA <= "0"&To_Stdlogicvector(int2bit_vec(sa_ByteT'pos(v_data),8));
				else
				   Out_DATA <= "000000000";
				end if;
			end if;
		end if;
	end process p_read;
	Out_SEND <= internal_SEND and Out_RDY;
	Out_COUNT <= "0000000000000000";
end architecture behavioral;
