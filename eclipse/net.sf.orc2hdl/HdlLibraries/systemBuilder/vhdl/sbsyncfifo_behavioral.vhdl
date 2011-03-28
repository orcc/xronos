-- sbfifo_behavioral.vhd
--
-- Copyright (c) 2004,2005 Xilinx Inc.

--   2005-06-23 DBP   Corrected fanout implementation

-----------------------------------------------------------------------
--

architecture behavioral of ram_2p_int is
  constant abits : positive := SystemBuilder.fifo_utilities.address_bits( l );
  constant asize : positive := 2 ** abits;
  type mem_type is array ( asize-1 downto 0 ) of int(w-1 downto 0);
  signal mem: mem_type;
begin
  process( SB_clock )
  begin
    if rising_edge(SB_clock) then
      if we_a = '1' then
        mem(conv_integer (addr_a)) <= din_a;
      end if;
    end if;
  end process;

  process( SB_clock )
  begin
    if rising_edge(SB_clock) then
      if re_b = '1' then
        dout_b <= mem( conv_integer(addr_b) );
      end if;	
    end if;
  end process;

end architecture behavioral;


architecture behavioral_distributed of ram_2p_int is
  constant abits : positive := SystemBuilder.fifo_utilities.address_bits( l );
  constant asize : positive := 2 ** abits;
  type mem_type is array ( asize-1 downto 0 ) of int(w-1 downto 0);
  signal mem: mem_type;
  attribute ram_style: string;
  attribute ram_style of mem : signal is "distributed";
begin
  process( SB_clock )
  begin
    if rising_edge(SB_clock) then
      if we_a = '1' then
        mem(conv_integer (addr_a)) <= din_a;
      end if;
    end if;
  end process;

  process( SB_clock )
  begin
    if rising_edge(SB_clock) then
      if re_b = '1' then
        dout_b <= mem( conv_integer(addr_b) );
      end if;	
    end if;
  end process;

end architecture behavioral_distributed;

architecture behavioral of ram_2p_bool is
  constant abits : positive := SystemBuilder.fifo_utilities.address_bits( l );
  constant asize : positive := 2 ** abits;
  type mem_type is array ( asize-1 downto 0 ) of bool;
  signal mem: mem_type;
begin
  process( SB_clock )
  begin
    if rising_edge(SB_clock) then
      if we_a = '1' then
        mem(conv_integer (addr_a)) <= din_a;
      end if;
    end if;
  end process;

  process( SB_clock )
  begin
    if rising_edge(SB_clock) then
      if re_b = '1' then	
        dout_b <= mem( conv_integer(addr_b) );
      end if;
    end if;
  end process;
  
end architecture behavioral;

architecture behavioral of sync_fifo_controller is
  constant abits : positive := SystemBuilder.fifo_utilities.address_bits( l );
  constant asize : positive := 2 ** abits;

  signal read_addr: std_logic_vector( abits-1 downto 0);
  signal write_addr: std_logic_vector( abits-1 downto 0);
  signal going_empty, going_full, match_means_empty, is_full, is_empty, match: std_logic;
  signal msread, mswrite: std_logic_vector(1 downto 0);
  signal write, read, sending : std_logic;
  constant start_addr : std_logic_vector( abits-1 downto 0 ) := (others => '0' );
begin

  msread <= read_addr( abits-1 downto abits-2 );
  mswrite <= write_addr( abits-1 downto abits-2 );
  
  -- Detect the read address just behind the write address
  going_empty  <= '1' when
      ( msread = 0 and mswrite = 1 ) or
      ( msread = 1 and mswrite = 2 ) or
      ( msread = 2 and mswrite = 3 ) or
      ( msread = 3 and mswrite = 0 )
    else '0';

  -- Detect the write address just behind the read address
  going_full  <= '1' when
      ( mswrite = 0 and msread = 1 ) or
      ( mswrite = 1 and msread = 2 ) or
      ( mswrite = 2 and msread = 3 ) or
      ( mswrite = 3 and msread = 0 )
    else '0';

  -- Predict the meaning of a read/write address match
  -- If there is no going_empty or going_full indication, keep the same prediction
  process( SB_clock, SB_reset )
  begin
    if SB_reset = '1' then
      match_means_empty <= '1';
    elsif rising_edge( SB_clock ) then
      if going_empty = '1' then
        match_means_empty <= '1';
      elsif going_full = '1' then
        match_means_empty <= '0';
      end if;
    end if;
  end process; 

  match <= '1' when (read_addr = write_addr) else '0';
  is_full  <= match and not match_means_empty;
  is_empty <= match and match_means_empty;

  -- Input side address and controls
  write <= i_send and not is_full;
  process( input_clock, SB_reset )
  begin
    if SB_reset = '1' then
      write_addr <= start_addr;
    elsif rising_edge( input_clock ) then
      if write = '1' then
        write_addr <= write_addr + 1;
      end if;
    end if;
  end process;
  i_ack <= write;
  i_mem_addr <= write_addr;
  i_mem_enable <= write;
  
  -- Output side address and controls.  NOTE: This 'not sending' clause
  -- converts this queue fifo into a first-word-fall-through (FWFT).
  read <= ( (not sending) or o_ack ) and (not is_empty);
  process( output_clock, SB_reset )
  begin
    if SB_reset = '1' then
      read_addr <= start_addr;
    elsif rising_edge( output_clock ) then
      if read = '1' then
        read_addr <= read_addr + '1';
      end if;
    end if;
  end process;
  process( output_clock, SB_reset )
  begin
    if SB_reset = '1' then
      sending <= '0';
    elsif rising_edge( output_clock ) then
      if read = '1' then
        sending <= '1';
      elsif o_ack = '1' then
        sending <= '0';
      end if;
    end if;
  end process; 
   
  o_mem_addr <= read_addr;
  o_mem_enable <= read;
  o_send <= sending;

  full <= is_full;
  empty <= is_empty;        
end architecture behavioral;


architecture behavioral of msync_fifo_int is

begin
  -- Length 0, 1 are supported as special cases
  -- See not for why length 1 is no longer a special case
  fifo_zero: if l <= 1 generate
    signal reg_dat: int(w-1 downto 0);
    signal reg_valid: std_logic;
    signal write, read: std_logic;
    
  begin
    -- Pass through
    -- o <= i;
    -- o_send <= i_send;
    -- i_ack <= o_ack;
    -- full <= i_send and not o_ack;
    -- empty <= not (i_send and not o_ack);
    -- size <= b"000";
    
    -- Zero length is register with ability to read and write simultaneously
    read <= o_ack;
    write <= (not(reg_valid) or read) and i_send;
    i_ack <= write;
    o <= reg_dat;
    o_send <= reg_valid;
    full <= reg_valid and not(read);
    empty <= not(reg_valid) or read;

    process (SB_clock, SB_reset) is begin
      if SB_reset = '1' then
        reg_dat <= (others => '0');
        reg_valid <= '0';
      elsif rising_edge( SB_clock ) then
        reg_valid <= (reg_valid and not(read)) or write;
        if write = '1' then
          reg_dat <= i;
        end if;
      end if;
    end process;
    
  end generate fifo_zero;

  -- length 1 breaks the combinatorial path
  -- IDM 06.2007.  Breaking the combinatorial path with a register effectively
  -- halves the throughput.  No longer supported as an option.
--   fifo_one: if l = 1 generate
--     signal is_full, write : std_logic;
--     signal reg_dat: int(w-1 downto 0);
--   begin
--     write <= i_send and (not is_full);
--     process( SB_clock, SB_reset ) is
--     begin
--       if SB_reset = '1' then
--         is_full <= '0';
--       elsif rising_edge( SB_clock ) then
--         if write = '1' then
--           is_full <= '1';
--         elsif is_full = '1' and o_ack = '1' then
--           is_full <= '0';
--         end if;
--       end if;
--     end process;
--     process( SB_clock ) is
--     begin
--       if rising_edge( SB_clock ) then
--         if write = '1' then
--           reg_dat <= i;
--         end if;
--       end if;
--     end process;
--     i_ack <= write;
--     o <= reg_dat;
--     o_send <= is_full;
--     full <= is_full;
--     empty <= not is_full;
--     size <= b"001" when is_full = '1' else b"000";
--    end generate fifo_one;
   
  -- For requested FIFO length greater than 1, implement length at least 4 (needed for FIFO
  -- controller code to work, and round the length up to a power of 2.
  fifo_many: if l > 1 generate
    constant abits : positive := SystemBuilder.fifo_utilities.address_bits( l );
    constant asize : positive := 2 ** abits;
    signal read_addr    : std_logic_vector( SystemBuilder.fifo_utilities.address_bits( l ) - 1 downto 0 );
    signal read_enable  : std_logic;
    signal write_addr   : std_logic_vector( SystemBuilder.fifo_utilities.address_bits( l ) - 1 downto 0 );
    signal write_enable : std_logic;
  begin
    size <= std_logic_vector(resize(unsigned(write_addr) - unsigned(read_addr),
                   abits + 1))
          when write_addr >= read_addr else
            std_logic_vector(to_unsigned(asize, abits + 1) - resize(unsigned(read_addr) - unsigned(write_addr), abits + 1));
    
    ctl: entity SystemBuilder.sync_fifo_controller( behavioral )
      generic map ( l => l )
      port map (
        SB_reset => SB_reset,
        SB_clock => SB_clock,
        input_clock => input_clock,
        output_clock => output_clock,
        i_send => i_send,
        i_ack => i_ack,
        i_mem_addr => write_addr,
        i_mem_enable => write_enable,
        o_send => o_send,
        o_ack => o_ack,
        o_mem_addr => read_addr,
        o_mem_enable => read_enable,
        full => full,
        empty => empty);
    ram: entity SystemBuilder.ram_2p_int( behavioral )
      generic map ( w => w, l => l )
      port map (
        din_a => i,
        addr_a => write_addr,
        we_a => write_enable,
        addr_b => read_addr,
        re_b => read_enable,
        dout_b => o,
        SB_clock => SB_clock );
  end generate fifo_many;

end architecture behavioral;

architecture behavioral of sync_fifo_int is
  signal msync_full : std_logic;
  signal msync_o_send : std_logic;
  
begin
  i_rdy <= not (msync_full);
  o_count <= (15 downto 1=>'0', 0=>msync_o_send);
  o_send <= msync_o_send;
  
  fifo: entity SystemBuilder.msync_fifo_int(behavioral) generic map(
    l => l, w => w)
  port map(
      SB_clock => SB_clock,
      input_clock => SB_clock,
      output_clock => SB_clock,
      SB_reset => SB_reset,
      full => msync_full,

      i => i_data,
      i_send => i_send,
      i_ack => i_ack,

      o => o_data,
      o_send => msync_o_send,
      o_ack => o_ack
);
end architecture behavioral;


architecture behavioral of msync_fifo_bool is
begin
  -- Length 0, 1 are supported as special cases
  fifo_zero: if l = 0 generate
  begin
    -- Zero length is a pass-through
    o <= i;
    o_send <= i_send;
    i_ack <= o_ack;
    full <= i_send and not o_ack;
    empty <= not (i_send and not o_ack);
    size <= b"000";
  end generate fifo_zero;

  -- length 1 breaks the combinatorial path
  fifo_one: if l = 1 generate
    signal is_full, write : std_logic;
    signal reg_dat: bool;
  begin
    write <= i_send and (not is_full);
    process( SB_clock, SB_reset ) is
    begin
      if SB_reset = '1' then
        is_full <= '0';
      elsif rising_edge( SB_clock ) then
        if write = '1' then
          is_full <= '1';
        elsif is_full = '1' and o_ack = '1' then
          is_full <= '0';
        end if;
      end if;
    end process;
    process( SB_clock ) is
    begin
      if rising_edge( SB_clock ) then
        if write = '1' then
          reg_dat <= i;
        end if;
      end if;
    end process;
    i_ack <= write;
    o <= reg_dat;
    o_send <= is_full;
    full <= is_full;
    empty <= not is_full;
    size <= b"001" when is_full = '1' else b"000";
  end generate fifo_one;

  -- For requested FIFO length greater than 1, implement length at least 4 (needed for FIFO
  -- controller code to work, and round the length up to a power of 2.
  fifo_many: if l > 1 generate 
    constant abits : positive := SystemBuilder.fifo_utilities.address_bits( l );
    constant asize : positive := 2 ** abits;

    signal read_addr    : std_logic_vector( SystemBuilder.fifo_utilities.address_bits( l ) - 1 downto 0 );
    signal read_enable  : std_logic;
    signal write_addr   : std_logic_vector( (abits - 1) downto 0 );
    signal write_enable : std_logic;
  begin
    size <= std_logic_vector(resize(unsigned(write_addr) - unsigned(read_addr),
                   abits + 1))
          when write_addr >= read_addr else
            std_logic_vector(to_unsigned(asize, abits + 1) - resize(unsigned(read_addr) - unsigned(write_addr), abits + 1));

    ctl: entity SystemBuilder.sync_fifo_controller( behavioral )
      generic map ( l => l )
      port map (
        SB_reset => SB_reset,
        SB_clock => SB_clock,
        input_clock => input_clock,
        output_clock => output_clock,
        i_send => i_send,
        i_ack => i_ack,
        i_mem_addr => write_addr,
        i_mem_enable => write_enable,
        o_send => o_send,
        o_ack => o_ack,
        o_mem_addr => read_addr,
        o_mem_enable => read_enable,
        full => full,
        empty => empty);

    ram: entity SystemBuilder.ram_2p_bool( behavioral )
      generic map (l => l )
      port map (
        din_a => i,
        addr_a => write_addr,
        we_a => write_enable,
        addr_b => read_addr,
        re_b => read_enable,
        dout_b => o,
        SB_clock => SB_clock );
  end generate fifo_many;
  
end architecture behavioral;

architecture behavioral of sync_fifo_bool is
  signal msync_full : std_logic;
  signal msync_o_send : std_logic;
  
begin
  i_rdy <= not(msync_full);
  o_count <= (15 downto 1=>'0', 0=>msync_o_send);
  o_send <= msync_o_send;
  
  fifo: entity SystemBuilder.msync_fifo_bool(behavioral) generic map(
    l => l)
  port map(
      SB_clock => SB_clock,
      input_clock => SB_clock,
      output_clock => SB_clock,
      SB_reset => SB_reset,

      i => i_data,
      i_send => i_send,
      i_ack => i_ack,
      full => msync_full,
      
      o => o_data,
      o_send => msync_o_send,
      o_ack => o_ack
);
end architecture behavioral;

-----------------------------------------------------------------------
-- Queues (just a port rename from the fifos)
architecture behavioral of Queue is
    signal o_data: int(width-1 downto 0);
    
begin  -- behavioral

    fifo: entity SystemBuilder.sync_fifo_int( behavioral )
      generic map ( w => width, l => length )
      port map (
        SB_reset => reset,
        SB_clock => clk,
        i_data   => In_DATA,
        i_send   => In_SEND,
        i_ack    => In_ACK,
        i_rdy    => In_RDY,
        i_count  => In_COUNT,
        o_data   => Out_DATA,
        o_send   => Out_SEND,
        o_ack    => Out_ACK,
        o_count  => Out_COUNT);
    
end behavioral;


architecture behavioral of Queue_bool is

begin  -- behavioral

  fifo: entity SystemBuilder.sync_fifo_bool( behavioral )
    generic map ( l => length )
    port map (
      SB_reset => reset,
      SB_clock => clk,
      i_data   => In_DATA,
      i_send   => In_SEND,
      i_ack    => In_ACK,
      i_rdy    => In_RDY,
      i_count  => In_COUNT,
      o_data   => Out_DATA,
      o_send   => Out_SEND,
      o_ack    => Out_ACK,
      o_count  => Out_COUNT);

end behavioral;
-----------------------------------------------------------------------
-- Fanouts

-- Manage the individual output-side send/ack signals
architecture behavioral of fanout_protocol is
  signal o_ack_captured : std_logic_vector(fanout-1 downto 0);
  signal o_send_local : std_logic_vector(fanout-1 downto 0);
  signal i_ack_local : std_logic;
begin

  In_ACK <= i_ack_local;
  Out_SEND <= o_send_local;
  -- Leave all buffering/registering in the queue.
  -- Generate the send to each consumer 
  -- generate the ack back to the producer when all consumers have acked
  process(In_SEND, o_ack_captured, Out_ACK, i_ack_local) is
  begin
    i_ack_local <= '1';
    for i in 0 to fanout-1 loop
      o_send_local(i) <= In_SEND and not(o_ack_captured(i));
      if Out_ACK(i) = '0' and o_ack_captured(i) = '0' then
        i_ack_local <= '0';
      end if;
    end loop;
  end process;

  -- In_RDY is the logical AND of all Out_RDY bits
  process(Out_RDY) is
  begin
    In_RDY <= '1';
    for i in 0 to fanout-1 loop
      if Out_RDY(i) = '0' then
        In_RDY <= '0';
      end if;
    end loop;  -- i
  end process;
  
  -- Capture any o_ack that we see
  process( SB_clock, SB_reset ) is
  begin
    if SB_reset = '1' then
      o_ack_captured <= (others => '0');
    elsif rising_edge( SB_clock ) then
      if i_ack_local = '1' then o_ack_captured <= (others => '0');
      else
        for i in 0 to fanout-1 loop
          if o_send_local(i) = '1' and Out_ACK(i) = '1' then o_ack_captured(i) <= '1'; end if;
        end loop;
      end if;
    end if;
  end process;

end architecture behavioral;

architecture behavioral of fanout is
begin

  fanout_one: if fanout = 1 generate
  begin
    -- simple pass through
    Out_SEND(0) <= In_SEND;
    In_ACK <= Out_ACK(0);
    Out_DATA <= In_DATA;
    Out_COUNT <= In_COUNT;
    In_RDY <= Out_RDY(0);
  end generate fanout_one;
    
  fanout_many: if fanout > 1 generate
  begin
    -- data is a pass through.  Manage the sends and acks.
    protocol: entity SystemBuilder.fanout_protocol( behavioral ) 
      generic map( fanout => fanout )
      port map (
        SB_reset => reset,
        SB_clock => clk,
        In_SEND => In_SEND,
        In_ACK => In_ACK,
        In_RDY => In_RDY,
        Out_SEND => Out_SEND,
        Out_ACK => Out_ACK,
        Out_RDY => Out_RDY
        );
    
    Out_DATA <= In_DATA;
    Out_COUNT <= In_COUNT;
  end generate fanout_many;

end architecture behavioral;

architecture behavioral of fanout_bool is
begin
  fanout_one: if fanout = 1 generate
  begin
    -- simple pass through
    Out_SEND(0) <= In_SEND;
    In_ACK <= Out_ACK(0);
    Out_DATA <= In_DATA;
    Out_COUNT <= In_COUNT;
    In_RDY <= Out_RDY(0);
  end generate fanout_one;
  
  fanout_many: if fanout > 1 generate
  begin
    protocol: entity SystemBuilder.fanout_protocol( behavioral ) 
      generic map( fanout => fanout )
      port map (
        SB_reset => reset,
        SB_clock => clk,
        In_SEND => In_SEND,
        In_ACK => In_ACK,
        In_RDY => In_RDY,
        Out_SEND => Out_SEND,
        Out_ACK => Out_ACK,
        Out_RDY => Out_RDY
        );

    Out_DATA <= In_DATA;
    Out_COUNT <= In_COUNT;
  end generate fanout_many;

end architecture behavioral;

