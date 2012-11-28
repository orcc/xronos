/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package org.xronos.openforge.verilog.mapping;

import java.util.Map;

public class BootstrapMapper {

	public BootstrapMapper(Map<String, PrimitiveMappedModule> m) {
		for (int i = 0; i < parts.length; i++) {
			m.put(parts[i].primitive + "/" + parts[i].part, parts[i].getPMM());
		}
	}

	static class SinglePart {
		MappingName primitive;
		String part;
		String simInclude;
		String synthInclude;
		String moduleId;

		public SinglePart(MappingName primitive, String part,
				String simInclude, String synthInclude, String moduleId) {
			this.primitive = primitive;
			this.part = part;
			this.simInclude = simInclude;
			this.synthInclude = synthInclude;
			this.moduleId = moduleId;
		}

		PrimitiveMappedModule getPMM() {
			return new PrimitiveMappedModule(moduleId, simInclude, synthInclude);
		}
	}

	private static SinglePart[] parts = {
			new SinglePart(MappingName.FLOP_SYNC,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/FD.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"FD"),
			new SinglePart(MappingName.FLOP_SYNC_ENABLE,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/FDE.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"FDE"),
			new SinglePart(MappingName.FLOP_SYNC_RESET,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/FDR.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"FDR"),
			new SinglePart(MappingName.FLOP_SYNC_SET,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/FDS.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"FDS"),
			new SinglePart(MappingName.FLOP_SYNC_ENABLE_RESET,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/FDRE.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"FDRE"),
			new SinglePart(MappingName.FLOP_SYNC_ENABLE_SET,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/FDSE.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"FDSE"),
			new SinglePart(MappingName.FLOP_SYNC_SET_RESET,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/FDRS.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"FDRS"),
			new SinglePart(MappingName.FLOP_SYNC_ENABLE_SET_RESET,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/FDRSE.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"FDRSE"),
			new SinglePart(MappingName.SHIFT_REGISTER_LUT,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/SRL16.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"SRL16"),
			new SinglePart(MappingName.SHIFT_REGISTER_LUT_ENABLE,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/SRL16E.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"SRL16E"),
			new SinglePart(MappingName.SHIFT_REGISTER_LUT_NEG_EDGE,
					PrimitiveMapper.UNKNOWN_XILINX_PART,
					"$XILINX/verilog/src/unisims/SRL16_1.v", // sim
					"$XILINX/verilog/src/iSE/unisim_comp.v", // synth
					"SRL16_1"), };
}
