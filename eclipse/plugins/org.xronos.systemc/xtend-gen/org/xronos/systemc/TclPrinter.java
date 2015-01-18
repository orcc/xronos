/**
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
 */
package org.xronos.systemc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.graph.Vertex;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenation;

/**
 * SystemC TCL Script Printer for Network and Actor
 * 
 * @author Endri Bezati
 */
@SuppressWarnings("all")
public class TclPrinter extends DfVisitor<Void> {
  private Vertex vertex;
  
  private String name;
  
  private Map<String, Object> options;
  
  public String setActor(final Actor actor) {
    String _xblockexpression = null;
    {
      this.vertex = actor;
      String _simpleName = actor.getSimpleName();
      _xblockexpression = this.name = _simpleName;
    }
    return _xblockexpression;
  }
  
  public String setNetwork(final Network network) {
    String _xblockexpression = null;
    {
      this.vertex = network;
      String _simpleName = network.getSimpleName();
      _xblockexpression = this.name = _simpleName;
    }
    return _xblockexpression;
  }
  
  public Map<String, Object> setOptions(final Map<String, Object> options) {
    return this.options = options;
  }
  
  private CharSequence getHeader(final String hlsTool) {
    CharSequence _xblockexpression = null;
    {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      Date date = new Date();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("## ############################################################################");
      _builder.newLine();
      _builder.append("## __  ___ __ ___  _ __   ___  ___ ");
      _builder.newLine();
      _builder.append("## \\ \\/ / \'__/ _ \\| \'_ \\ / _ \\/ __|");
      _builder.newLine();
      _builder.append("##  >  <| | | (_) | | | | (_) \\__ \\");
      _builder.newLine();
      _builder.append("## /_/\\_\\_|  \\___/|_| |_|\\___/|___/");
      _builder.newLine();
      _builder.append("## ############################################################################");
      _builder.newLine();
      _builder.append("## This file is generated automatically by Xronos HLS");
      _builder.newLine();
      _builder.append("## ############################################################################");
      _builder.newLine();
      _builder.append("## Xronos SystemC, ");
      _builder.append(hlsTool, "");
      _builder.append(" TCL Script");
      _builder.newLineIfNotEmpty();
      {
        if ((this.vertex instanceof Network)) {
          _builder.append("## TCL Script file for Network: ");
          _builder.append(this.name, "");
          _builder.append(" ");
          _builder.newLineIfNotEmpty();
        } else {
          _builder.append("## TCL Script file for Actor: ");
          _builder.append(this.name, "");
          _builder.append(" ");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("## Date: ");
      String _format = dateFormat.format(date);
      _builder.append(_format, "");
      _builder.newLineIfNotEmpty();
      _builder.append("## ############################################################################");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence getContentForVivado() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader("Vivado HLS");
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    _builder.append("set SrcPath \"../../src/\"");
    _builder.newLine();
    _builder.append("set SrcHeaderPath \"../../src/header\"");
    _builder.newLine();
    _builder.append("set TbPath \"../../testbench\"");
    _builder.newLine();
    _builder.append("set SrcTbPath \"../../testbench/src\"");
    _builder.newLine();
    _builder.append("set HeaderTbPath \"../../testbench/src/header\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("## -- Create Project");
    _builder.newLine();
    _builder.append("open_project proj_");
    _builder.append(this.name, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("## -- Add Design Files");
    _builder.newLine();
    {
      if ((this.vertex instanceof Network)) {
        _builder.append("## -- Actor Modules");
        _builder.newLine();
        {
          EList<Vertex> _children = ((Network)this.vertex).getChildren();
          for(final Vertex child : _children) {
            _builder.append("add_files $SrcPath/");
            String _label = child.getLabel();
            _builder.append(_label, "");
            _builder.append(".cpp -cflags \"-I$SrcHeaderPath\"");
            _builder.newLineIfNotEmpty();
          }
        }
        _builder.append("## -- Network Top Module");
        _builder.newLine();
        _builder.append("add_files $SrcPath/");
        _builder.append(this.name, "");
        _builder.append(".cpp -cflags \"-I$SrcHeaderPath\"");
        _builder.newLineIfNotEmpty();
      } else {
        _builder.append("add_files $SrcPath/");
        _builder.append(this.name, "");
        _builder.append(".cpp -cflags \"-I$SrcHeaderPath\"");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.append("## -- Add TestBench File");
    _builder.newLine();
    _builder.append("add_files -tb $HeaderTbPath/tb_kicker.h");
    _builder.newLine();
    _builder.append("add_files -tb $HeaderTbPath/tb_driver.h");
    _builder.newLine();
    _builder.append("add_files -tb $HeaderTbPath/tb_compare.h");
    _builder.newLine();
    _builder.append("add_files -tb $HeaderTbPath/tb_endsim");
    {
      if ((this.vertex instanceof Network)) {
        _builder.append("_n");
      } else {
        _builder.append("_a");
      }
    }
    _builder.append("_");
    _builder.append(this.name, "");
    _builder.append(".h");
    _builder.newLineIfNotEmpty();
    _builder.append("add_files -tb $SrcTbPath/tb_");
    _builder.append(this.name, "");
    _builder.append(".cpp -cflags \"-I$SrcHeaderPath -I$HeaderTbPath/\"");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("## -- Set Top Level");
    _builder.newLine();
    _builder.append("set_top ");
    _builder.append(this.name, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("## -- Create Solution");
    _builder.newLine();
    _builder.append("open_solution -reset xronos_solution_1");
    _builder.newLine();
    _builder.newLine();
    _builder.append("## -- Define Xilinx Technology (TBD: add option of FPGA technology)");
    _builder.newLine();
    _builder.append("set_part  {xc7z020clg484-1}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("## -- Define Clock period");
    _builder.newLine();
    _builder.append("create_clock -period 10 -name clk");
    _builder.newLine();
    _builder.newLine();
    _builder.append("## -- Compilation and Pre Synthesis");
    _builder.newLine();
    _builder.append("csim_design");
    _builder.newLine();
    _builder.newLine();
    _builder.append("# -- Run Synthesis");
    _builder.newLine();
    _builder.append("csynth_design");
    _builder.newLine();
    _builder.newLine();
    _builder.append("## -- RTL Simulation");
    _builder.newLine();
    _builder.append("cosim_design -rtl systemc");
    _builder.newLine();
    _builder.newLine();
    _builder.append("## -- Export RTL implementation");
    _builder.newLine();
    _builder.append("export_design -format ip_catalog");
    _builder.newLine();
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getContentForCatapult() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader("Catapult");
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence getContentForModelSim() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader("ModelSim");
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("## -- Set RTL Folder");
    _builder.newLine();
    _builder.append("set RTL \"../../rtl\"");
    _builder.newLine();
    _builder.append("set TB \"../../testbench/vhdl\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("## -- Create the work design library");
    _builder.newLine();
    _builder.append("if {[file exist work_");
    _builder.append(this.name, "");
    _builder.append("]} {vdel -all -lib work_");
    _builder.append(this.name, "");
    _builder.append("}");
    _builder.newLineIfNotEmpty();
    _builder.append("vlib work_");
    _builder.append(this.name, "");
    _builder.newLineIfNotEmpty();
    _builder.append("vmap work_");
    _builder.append(this.name, "");
    _builder.append(" work_");
    _builder.append(this.name, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.newLine();
    _builder.append("## -- Compile network and actors add them to work library");
    _builder.newLine();
    {
      if ((this.vertex instanceof Network)) {
        _builder.append("vcom -93 -check_synthesis -quiet -work work_");
        _builder.append(this.name, "");
        _builder.append(" $RTL/*.vhd");
        _builder.newLineIfNotEmpty();
        _builder.append("##vlog -work work_");
        _builder.append(this.name, "");
        _builder.append(" $RTL/*.v");
        _builder.newLineIfNotEmpty();
      } else {
        _builder.append("vcom -93 -check_synthesis -quiet -work work_");
        _builder.append(this.name, "");
        _builder.append(" $RTL/");
        _builder.append(this.name, "");
        _builder.append("*.vhd");
        _builder.newLineIfNotEmpty();
        _builder.append("##vlog -work work_");
        _builder.append(this.name, "");
        _builder.append("  $RTL/");
        _builder.append(this.name, "");
        _builder.append("*.v");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.append("## -- Compile Sim Package");
    _builder.newLine();
    _builder.append("vcom -93 -check_synthesis -quiet -work work_");
    _builder.append(this.name, "");
    _builder.append(" $TB/sim_package.vhd");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("## -- Compile the Tesbench");
    _builder.newLine();
    _builder.append("vcom -93 -check_synthesis -quiet -work work_");
    _builder.append(this.name, "");
    _builder.append(" $TB/");
    _builder.append(this.name, "");
    _builder.append("_tb.vhd");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("## -- Start VSIM");
    _builder.newLine();
    _builder.append("vsim -voptargs=\"+acc\" -t ns work_");
    _builder.append(this.name, "");
    _builder.append(".");
    _builder.append(this.name, "");
    _builder.append("_tb");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("## Add clock(s) and reset signal");
    _builder.newLine();
    _builder.append("add wave -noupdate -divider -height 20 \"CLK & RESET\"");
    _builder.newLine();
    _builder.append("add wave sim:/");
    _builder.append(this.name, "");
    _builder.append("_tb/CLK");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    return _builder;
  }
}
