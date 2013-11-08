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


parameter INIT = 4'b0001;
parameter SPACE = 4'b0010;
parameter ALMOST_FULL = 4'b0100;
parameter FULL = 4'b1000;

  (* FSM_ENCODING="ONE-HOT", SAFE_IMPLEMENTATION="YES", SAFE_RECOVERY_STATE="<recovery_state_value>" *) reg [3:0] state = INIT;

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
               if (almost_full == 1'b0 && full == 1'b0)
                  state <= SPACE;
               else if (almost_full == 1'b1 && full == 1'b0)
                  state <= ALMOST_FULL;
               else if (almost_full == 1'b1 && full == 1'b1)
									state <= FULL;
							 else
									state <= INIT;
               enable <= 1'b1;
            end
            ALMOST_FULL : begin
               if (almost_full == 1'b0 && full == 1'b0)
                  state <= SPACE;
               else if (almost_full == 1'b1 && full == 1'b0)
                  state <= ALMOST_FULL;
               else if (almost_full == 1'b1 && full == 1'b1)
									state <= FULL;
							 else
									state <= INIT;
               enable <= 1'b0;
            end
            FULL : begin
               if (almost_full == 1'b0 && full == 1'b0)
                  state <= SPACE;
               else if (almost_full == 1'b1 && full == 1'b0)
                  state <= ALMOST_FULL;
               else if (almost_full == 1'b1 && full == 1'b1)
									state <= FULL;
							 else
									state <= INIT;
               enable <= 1'b0;
            end
            default: begin  // Fault Recovery
               state <= INIT;
               enable <= 1'b1;
	    end
         endcase
endmodule							
