// __  ___ __ ___  _ __   ___  ___ 
// \ \/ / '__/ _ \| '_ \ / _ \/ __|
//  >  <| | | (_) | | | | (_) \__ \
// /_/\_\_|  \___/|_| |_|\___/|___/
// 

// Author: Endri Bezati

module controller(almost_full, full, enable, clk, reset);

input clk;
input reset;
input almost_full;
input full;
output reg enable;


parameter INIT = 5'b00001;
parameter SPACE = 5'b00010;
parameter AF_DISABLE = 5'b00100;
parameter FULL = 5'b01000;
parameter AF_ENABLE = 5'b10000;

  (* FSM_ENCODING="ONE-HOT", SAFE_IMPLEMENTATION="YES", SAFE_RECOVERY_STATE="INIT" *) reg [4:0] state = INIT;

always@(posedge clk)
	if (reset) begin
		state <= INIT;
		enable <= 1'b1;
	end
	else
		(* PARALLEL_CASE *) case (state)
		INIT: begin
			if (almost_full == 1'b1 && full == 1'b1)
				state <= SPACE;
			else
				state  <= INIT;

			enable <= 1'b1;
		end

		SPACE : begin
			if (almost_full == 1'b1 && full == 1'b0)
				state <= AF_DISABLE;
			else
				state <= SPACE;

			enable <= 1'b1;
		end

		AF_DISABLE : begin
			if (almost_full == 1'b1 && full == 1'b1)
				state <= FULL;
			if (almost_full == 1'b0 && full == 1'b0)
				state <= SPACE;
			else
				state <= AF_DISABLE;
	
			enable <= 1'b0;
		end

		FULL : begin
			if (almost_full == 1'b1 && full == 1'b0)
				state <= AF_ENABLE;
			else
				state <= FULL;
	
			enable <= 1'b0;
		end

		AF_ENABLE : begin
			if (almost_full == 1'b0 && full == 1'b0)
				state <= SPACE;
			else if (almost_full == 1'b1 && full == 1'b1)
				state <= FULL;
			else
				state <= AF_ENABLE;

			enable <= 1'b1;
		end
	
		default: begin  // Fault Recovery
			state <= INIT;
			enable <= 1'b1;
		end
	endcase
endmodule							
