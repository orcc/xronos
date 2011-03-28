-- sbsyncfifo.vhd
--
-- Copyright (c) 2004-2007 Xilinx Inc.

--  2005-06-23 DBP   Added fanout building blocks 

-----------------------------------------------------------------------
--
library ieee;
use ieee.std_logic_1164.all;

package fifo_utilities is

-- Compute number of address bits needed for a given RAM length
function address_bits( address_length: natural ) return positive;
function bin2gray( bin: std_logic_vector ) return std_logic_vector;

end package fifo_utilities;

package body fifo_utilities is

-- Compute number of address bits needed for a given RAM length
function address_bits( address_length: natural ) return positive is
 variable length : natural := 4;
 variable bits: positive := 2;
begin
  while length < address_length loop
    length := length * 2;
	bits := bits + 1;
  end loop;
  return bits;
end address_bits;

-- Convert binary to gray code
function bin2gray( bin: std_logic_vector ) return std_logic_vector is
 variable gray: std_logic_vector( bin'length-1 downto 0 );
begin
  gray( bin'length-1 ) := bin( bin'length-1 );
  for i in bin'length-2 downto 0 loop
    gray( i ) := bin( i+1 ) xor bin( i );
  end loop;
  return gray;
end bin2gray;

end package body fifo_utilities;

-----------------------------------------------------------------------
---- 2-port RAMs

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity ram_2p_int is
  generic (
    w: positive;
    l: positive );
  port (
    SB_clock : in std_logic;
    din_a : in int( w-1 downto 0 );
    addr_a : in std_logic_vector( address_bits( l )-1 downto 0);
    we_a : in std_logic;
    addr_b : in std_logic_vector( address_bits( l )-1 downto 0);
    re_b : in std_logic;
    dout_b : out int( w-1 downto 0 ) );
end entity ram_2p_int;

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity ram_2p_bool is
  generic ( l: positive );
  port (
    SB_clock : in std_logic;
    din_a : in bool;
    addr_a : in std_logic_vector( address_bits( l )-1 downto 0);
    we_a : in std_logic;
    addr_b : in std_logic_vector( address_bits( l )-1 downto 0);
    re_b : in std_logic;
    dout_b : out bool );
end entity ram_2p_bool;

-----------------------------------------------------------------------
-- fifo controller
-- Everything but the memory and data path

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;   -- need for "+"(std_logic_vector,integer)
use ieee.std_logic_arith.all;      -- need for conv_std_logic_vector()
use SystemBuilder.fifo_utilities.all;

entity sync_fifo_controller is
  generic( l: positive );
  port(
    SB_reset, SB_clock: in std_logic;
    input_clock: in std_logic;
    output_clock: in std_logic;       
    i_send: in  std_logic;
    i_ack:  out std_logic;
    i_mem_addr: out std_logic_vector( address_bits( l ) -1 downto 0 );
    i_mem_enable: out std_logic;
    o_send: out  std_logic;
    o_ack:   in std_logic;
    o_mem_addr: out std_logic_vector( address_bits( l ) -1 downto 0 );
    o_mem_enable: out std_logic;
    full: out std_logic;
    empty: out std_logic);
end entity sync_fifo_controller;

-----------------------------------------------------------------------
-- FIFOs

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity sync_fifo_int is
  generic(
    w: positive;
    l: natural -- ;
    -- synchronous_protocol: std_logic := '0'
     );
  port(
    SB_reset, SB_clock: in std_logic;
    i_data: in int( w-1 downto 0 );
    i_send: in  std_logic;
    i_ack:  out std_logic;
    i_rdy: out std_logic;
    i_count: in std_logic_vector(15 downto 0);  -- ignored for now
    o_data: out int( w-1 downto 0 );
    o_send: out  std_logic;
    o_ack: in std_logic;
    o_count: out std_logic_vector(15 downto 0) -- ;
    );

end entity sync_fifo_int;

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity msync_fifo_int is
  generic(
    w: positive;
    l: natural -- ;
    -- synchronous_protocol: std_logic := '0'
     );
  port(
    SB_reset, SB_clock: in std_logic;
    input_clock: in std_logic;
    output_clock: in std_logic;
    i: in int( w-1 downto 0 );
    i_send: in  std_logic;
    i_ack:  out std_logic;
    o: out int( w-1 downto 0 );
    o_send: out  std_logic;
    o_ack: in std_logic := '1';
    full: out std_logic;
    empty: out std_logic;
    size: out std_logic_vector( SystemBuilder.fifo_utilities.address_bits( l ) downto 0 ));

end entity msync_fifo_int;

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity sync_fifo_bool is
  generic( l: natural --;
           -- synchronous_protocol: std_logic := '0'
            );
  port(
    SB_reset, SB_clock: in std_logic;
    i_data: in bool;
    i_send: in  std_logic;
    i_ack:  out std_logic;
    i_rdy:  out std_logic;
    i_count: in std_logic_vector(15 downto 0);  -- ignored for now
    o_data: out bool;
    o_send: out  std_logic;
    o_ack: in std_logic;
    o_count: out std_logic_vector(15 downto 0) -- ;
    );
end entity sync_fifo_bool;

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity msync_fifo_bool is
  generic( l: natural --;
           -- synchronous_protocol: std_logic := '0'
            );
  port(
    SB_reset, SB_clock: in std_logic;
    input_clock: in std_logic;
    output_clock: in std_logic;
    i: in bool;
    i_send: in  std_logic;
    i_ack:  out std_logic;
    o: out bool;
    o_send: out  std_logic;
    o_ack: in std_logic := '1';
    full: out std_logic;
    empty: out std_logic;
    size: out std_logic_vector( SystemBuilder.fifo_utilities.address_bits( l ) downto 0 ));
end entity msync_fifo_bool;

-----------------------------------------------------------------------
-- Queues (just a port rename from the fifos)
library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use ieee.std_logic_arith.all;      -- need for conv_std_logic_vector()
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity Queue is
  generic (
    width  : INTEGER;
    length : INTEGER
    );

  port (
    In_DATA            : in  std_logic_vector (width-1 downto 0);
    In_SEND            : in  std_logic;
    In_ACK             : out std_logic;
    In_COUNT           : in  std_logic_vector (15 downto 0);
    In_RDY             : out std_logic;
    Out_DATA           : out std_logic_vector (width-1 downto 0);
    Out_SEND           : out std_logic;
    Out_ACK            : in  std_logic;
    Out_COUNT          : out std_logic_vector (15 downto 0);
    clk, reset         : in  std_logic);

end Queue;

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity Queue_bool is
  generic (
    length : INTEGER
    );

  port (
    In_DATA            : in  bool;
    In_SEND            : in  std_logic;
    In_ACK             : out std_logic;
    In_COUNT           : in  std_logic_vector (15 downto 0);
    In_RDY             : out std_logic;
    Out_DATA           : out bool;
    Out_SEND           : out std_logic;
    Out_ACK            : in  std_logic;
    Out_COUNT          : out std_logic_vector (15 downto 0);
    clk, reset         : in  std_logic);

end Queue_bool;

-----------------------------------------------------------------------
-- Fanouts

-- Fanout building blocks
library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

-- Manage the individual output-side send/ack signals
entity fanout_protocol is
  generic( fanout: positive );
  port(
    SB_reset: in  std_logic;
    SB_clock: in  std_logic;
    In_SEND:   in  std_logic;
    In_ACK:    out std_logic;
    Out_SEND:   out std_logic_vector(fanout-1 downto 0);
    Out_ACK:    in  std_logic_vector(fanout-1 downto 0);
    In_RDY: out std_logic;
    Out_RDY: in std_logic_vector(fanout-1 downto 0)
    );
  
end entity fanout_protocol;

-- Fanout implementations using generic for number of outputs
library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity fanout is
  generic( width, fanout: positive );
  port(
    reset: in  std_logic;
    clk: in  std_logic;
    In_DATA:        in  int( width-1 downto 0 );
    In_SEND:   in  std_logic;
    In_ACK:    out std_logic;
    In_COUNT: in std_logic_vector (15 downto 0);
    In_RDY: out std_logic;
    Out_DATA:        out int( width-1 downto 0 );
    Out_SEND:   out std_logic_vector(fanout-1 downto 0);
    Out_ACK:    in  std_logic_vector(fanout-1 downto 0);
    Out_COUNT: out std_logic_vector(15 downto 0);
    Out_RDY: in std_logic_vector(fanout-1 downto 0)
    );  
  
end entity fanout;

library ieee, SystemBuilder;
use ieee.std_logic_1164.all;
use SystemBuilder.sb_types.all;
use SystemBuilder.fifo_utilities.all;

entity fanout_bool is
  generic( fanout: positive );
  port(
    reset: in  std_logic;
    clk: in  std_logic;
    In_DATA:        in  bool;
    In_SEND:   in  std_logic;
    In_ACK:    out std_logic;
    In_COUNT: in std_logic_vector (15 downto 0);
    In_RDY: out std_logic;
    Out_DATA:        out bool;
    Out_SEND:   out std_logic_vector(fanout-1 downto 0);
    Out_ACK:    in  std_logic_vector(fanout-1 downto 0);
    Out_COUNT: out std_logic_vector(15 downto 0);
    Out_RDY: in std_logic_vector(fanout-1 downto 0)
    );
end entity fanout_bool;

--------------------------------------------------------
