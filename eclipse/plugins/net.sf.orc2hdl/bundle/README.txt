EPFL's Orc2HDL: OpenForges Orcc frontend

Welcome to the generated code of Orc2HDL

The following tools are need:
	- Modelsim 6.6+ (for HDL simulation)
	- Xilinx ISE 13+ (for Xilinx libraries and synthesis)

Configuration of your system Before Simulation:
	- Xilinx Simulation Libraries: 
		-Linux : 
			- Run the "compxlib" (<Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/bin/lin with root privileges and follow the instructions
			- Add the following lines in the modelsim.ini (<ModelSim Installation folder>/modeltech/modelsim.ini)
				simprim = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/vhdl/mti_se/<Modelsim Version>/lin64/simprim
				unsim = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/vhdl/mti_se/<Modelsim Version>/lin64/unisim
				xilinxcorelib = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/vhdl/mti_se/<Modelsim Version>/lin64/xilinxcorelib
				simprims_ver = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/verilog/mti_se/<Modelsim Version>/lin64/simprims_ver
				unisims_ver = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/verilog/mti_se/<Modelsim Version>/lin64/unisims_ver
				xilinxcorelib_ver = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/verilog/mti_se/<Modelsim Version>/lin64/xilinxcorelib_ver
		-Windows
			- Run the "Run Simulation Library Compilation Wizard" found on the Xilinx ISE 
			- Add the following lines in the modelsim.ini (<ModelSim Installation folder>/modelsim.ini)
				simprim = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/vhdl/mti_se/<Modelsim Version>/nt/simprim
				unsim = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/vhdl/mti_se/<Modelsim Version>/nt/unisim
				xilinxcorelib = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/vhdl/mti_se/<Modelsim Version>/nt/xilinxcorelib
				simprims_ver = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/verilog/mti_se/<Modelsim Version>/nt/simprims_ver
				unisims_ver = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/verilog/mti_se/<Modelsim Version>/nt/unisims_ver
				xilinxcorelib_ver = <Xilinx Installation Folder>/<ISE Version>/ISE_DS/ISE/verilog/mti_se/<Modelsim Version>/nt/xilinxcorelib_ver
			
The folder structure:
	- <Output Folder Name> 
		-lib : The used libraries folder
			-simulation : HDL files need for the simulation
				- glbl.v : Xilinx provided simulation file
				- sim_packages.vhd : Different VHDL functions need for the Testbenches
			-systemActors : Different systemActors
				-io
					- Source.vhd : Source system actor
				-types
					- sa_types.vhd : Different VHDL functions
			-systemBuilder : The FIFO used for the inter-actor communication 
				-vhdl
					- sbfifo_behavioral.vhd : SystemBuilder FIFO Behavioral
					- sbfifo.vhd : SystemBuilder FIFO Entity declaration
					- sbtypes.vhd : Different VHDL functions need by SystemBuilder FIFO 
		
		-sim : the simulation folder
			- sim_<Top Network Name>.do : Overall simulation of the generated Network
			- generateWeights_<Top Network Name>.do (optional) : A do script that generates for each action a file which contains the time that is was activated
			- weights (optional folder): where the generateWeights_<Top Network Name>.do generates the txt files
		
		-rtl : the generated HDL code
			- <Top Network Name>.vhd : The Top VHDL Network, which instantiates the actors and the SystemBuilder FIFOs
			- <Instance Name>.v : Verilog Actor
			- goDoneRtl (optional folder): Contains the same files as above but with the inclusion of the Go And Done signal for each Top Module in the Verilog Actor File
				- <Top Network Name>.vhd : The Top VHDL Network, which instantiates the actors and the SystemBuilder FIFOs
				- <Instance Name>.v : Verilog Actor
		
		-testbench: Testing the result between the Orcc simulation and Modelsim simulation for each actor
			- traces: folder which the user should put the Orcc simulator traces
			- vhd: folder which contains the VHDL testbench files for each actor
			- <Instance Name>.tcl : ModelSim TCL script for launching an actors testbench 

 
If you have any questions about Orcc and Orc2HDL:
 	- mail to : orcc-list@lists.sourceforge.net
 	   
 	   
Current bugs with the "Faster Code Generation":

-Problems with repeats:
	- priority problems
	   a0: action In:[tokens] repeat 64 ==>
	   ...
	   
	   a1: action In:[token] ==>
	   
	  schedule fsm s0:
	 		s0 (a0) --> ..
	 		s0 (a1) --> ..
	  end
	  
	  for the moment it is possible to test if X tokens are in the FIFO, but only 1, in this situation even if token exists on the input 
	  both actions can fire (first problem), but here a0 is going to fire because a0 has a greater priority to a1 due it's name(ORCC bug/feature :) )
	
	- peek problem
	  a0: action In:[tokens] ==>
	  guard
	  	tokens[2] = 0
	  ...
	  
	  not supported for the moment, a peek can be done only on the first element of tokens
	   	
	- action In:[tokens] repeat 64 ==> Out:[tokens] repeat 64
	  do
	  	 ...
	  	 	tokens[i] := tokens[i] + ...	   
		...	 
	  end	
	  
	  for the moment this will cause some data corruption it is better create a new table and then copy the input values in it, ex
	  action In:[ll] repeat 64 ==> Out:[tokens] repeat 64
	  var 
	  	 int ll[64]
	  do
	  	 foreach int i 0 .. 64 do
	  	 	tokens[i] := ll[i];
	  	 end
	  	 ...
	  	 	tokens[i] := tokens[i] + ...	   
		...	 
	  end
	
	- action ==> Out[w>>4, h>>4] 
	  ...
	  
	  In some case this might not work is better to change it like this,
	  
	  action ==> Out[tokens] repeat 2
	  var
	  	int tokens[2]
	  ...	
- Arithmetic problems
	- shifting problems
		  a := (-1 << x);
		  
		  this will give a positive result, transform it to
		  
		  a:= -(1 << x)
	
	   
	  