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

import com.google.common.base.Objects;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.Type;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.xronos.systemc.SystemCTemplate;

/**
 * SystemC Testbench Printer for Network and Actor
 * 
 * @author Endri Bezati
 */
@SuppressWarnings("all")
public class TestbenchPrinter extends SystemCTemplate {
  private Network network;
  
  private Actor actor;
  
  private String name;
  
  private String prefix;
  
  public CharSequence getHeader() {
    CharSequence _xblockexpression = null;
    {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      Date date = new Date();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("// ----------------------------------------------------------------------------");
      _builder.newLine();
      _builder.append("// __  ___ __ ___  _ __   ___  ___ ");
      _builder.newLine();
      _builder.append("// \\ \\/ / \'__/ _ \\| \'_ \\ / _ \\/ __|");
      _builder.newLine();
      _builder.append("//  >  <| | | (_) | | | | (_) \\__ \\");
      _builder.newLine();
      _builder.append("// /_/\\_\\_|  \\___/|_| |_|\\___/|___/");
      _builder.newLine();
      _builder.append("// ----------------------------------------------------------------------------");
      _builder.newLine();
      _builder.append("// This file is generated automatically by Xronos HLS");
      _builder.newLine();
      _builder.append("// ----------------------------------------------------------------------------");
      _builder.newLine();
      _builder.append("// Xronos SystemC, Testbench Generator");
      _builder.newLine();
      _builder.append("// For ");
      {
        boolean _notEquals = (!Objects.equal(this.network, null));
        if (_notEquals) {
          _builder.append("Top level Network: ");
          _builder.append(this.name, "");
        } else {
          _builder.append(" Actor: ");
          _builder.append(this.name, "");
        }
      }
      _builder.append(" ");
      _builder.newLineIfNotEmpty();
      _builder.append("// Date: ");
      String _format = dateFormat.format(date);
      _builder.append(_format, "");
      _builder.newLineIfNotEmpty();
      _builder.append("// ----------------------------------------------------------------------------");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public Actor setNetwork(final Network network) {
    Actor _xblockexpression = null;
    {
      this.network = network;
      String _simpleName = network.getSimpleName();
      this.name = _simpleName;
      this.prefix = "n";
      _xblockexpression = this.actor = null;
    }
    return _xblockexpression;
  }
  
  public Network setActor(final Actor actor) {
    Network _xblockexpression = null;
    {
      this.actor = actor;
      String _simpleName = actor.getSimpleName();
      this.name = _simpleName;
      this.prefix = "a";
      _xblockexpression = this.network = null;
    }
    return _xblockexpression;
  }
  
  public CharSequence getContent() {
    CharSequence _xblockexpression = null;
    {
      List<Port> inputs = null;
      List<Port> outputs = null;
      boolean _notEquals = (!Objects.equal(this.network, null));
      if (_notEquals) {
        EList<Port> _inputs = this.network.getInputs();
        inputs = _inputs;
        EList<Port> _outputs = this.network.getOutputs();
        outputs = _outputs;
      } else {
        EList<Port> _inputs_1 = this.actor.getInputs();
        inputs = _inputs_1;
        EList<Port> _outputs_1 = this.actor.getOutputs();
        outputs = _outputs_1;
      }
      StringConcatenation _builder = new StringConcatenation();
      CharSequence _header = this.getHeader();
      _builder.append(_header, "");
      _builder.newLineIfNotEmpty();
      _builder.append("#include \"systemc.h\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("#ifdef __RTL_SIMULATION__");
      _builder.newLine();
      _builder.append("#include \"");
      _builder.append(this.name, "");
      _builder.append("_rtl_wrapper.h\"");
      _builder.newLineIfNotEmpty();
      _builder.append("#define ");
      _builder.append(this.name, "");
      _builder.append(" ");
      _builder.append(this.name, "");
      _builder.append("_rtl_wrapper ");
      _builder.newLineIfNotEmpty();
      _builder.append("#else");
      _builder.newLine();
      _builder.append("#include \"");
      _builder.append(this.name, "");
      _builder.append(".h\"");
      _builder.newLineIfNotEmpty();
      _builder.append("#endif");
      _builder.newLine();
      _builder.newLine();
      _builder.append("#include \"tb_kicker.h\"");
      _builder.newLine();
      _builder.append("#include \"tb_endsim");
      {
        boolean _notEquals_1 = (!Objects.equal(this.network, null));
        if (_notEquals_1) {
          _builder.append("_n");
        } else {
          _builder.append("_a");
        }
      }
      _builder.append("_");
      _builder.append(this.name, "");
      _builder.append(".h\"");
      _builder.newLineIfNotEmpty();
      {
        boolean _isEmpty = inputs.isEmpty();
        boolean _not = (!_isEmpty);
        if (_not) {
          _builder.append("#include \"tb_driver.h\"");
          _builder.newLine();
        }
      }
      {
        boolean _isEmpty_1 = outputs.isEmpty();
        boolean _not_1 = (!_isEmpty_1);
        if (_not_1) {
          _builder.append("#include \"tb_compare.h\"");
          _builder.newLine();
        }
      }
      _builder.newLine();
      _builder.append("int sc_main (int argc , char *argv[]) {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_report_handler::set_actions(\"/IEEE_Std_1666/deprecated\", SC_DO_NOTHING);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_report_handler::set_actions( SC_ID_LOGIC_X_TO_BOOL_, SC_LOG);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_report_handler::set_actions( SC_ID_VECTOR_CONTAINS_LOGIC_VALUE_, SC_LOG);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// sc_report_handler::set_actions( SC_ID_OBJECT_EXISTS_, SC_LOG);");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// -- Control Signals");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_signal<bool>    reset;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_signal<bool>    start;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// -- Queues");
      _builder.newLine();
      {
        boolean _isEmpty_2 = inputs.isEmpty();
        boolean _not_2 = (!_isEmpty_2);
        if (_not_2) {
          {
            for(final Port port : inputs) {
              _builder.append("\t");
              _builder.append("sc_fifo< ");
              Type _type = port.getType();
              CharSequence _doSwitch = this.doSwitch(_type);
              _builder.append(_doSwitch, "\t");
              _builder.append(" > q_");
              String _name = port.getName();
              _builder.append(_name, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      {
        boolean _isEmpty_3 = outputs.isEmpty();
        boolean _not_3 = (!_isEmpty_3);
        if (_not_3) {
          {
            for(final Port port_1 : outputs) {
              _builder.append("\t");
              _builder.append("sc_fifo< ");
              Type _type_1 = port_1.getType();
              CharSequence _doSwitch_1 = this.doSwitch(_type_1);
              _builder.append(_doSwitch_1, "\t");
              _builder.append(" > q_");
              String _name_1 = port_1.getName();
              _builder.append(_name_1, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// -- Create a 100ns period clock signal");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_clock s_clk(\"s_clk\", 10, SC_NS);");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      {
        boolean _notEquals_2 = (!Objects.equal(this.network, null));
        if (_notEquals_2) {
          _builder.append("\t");
          _builder.append("// -- Network module");
          _builder.newLine();
          _builder.append("\t");
          _builder.append(this.name, "\t");
          _builder.append(" n_");
          _builder.append(this.name, "\t");
          _builder.append("(\"n_");
          _builder.append(this.name, "\t");
          _builder.append("\");");
          _builder.newLineIfNotEmpty();
          _builder.append("\t");
          _builder.append("sc_signal<bool> done_n_");
          _builder.append(this.name, "\t");
          _builder.append(";");
          _builder.newLineIfNotEmpty();
        } else {
          _builder.append("\t");
          _builder.append("// -- Actor module");
          _builder.newLine();
          _builder.append("\t");
          _builder.append(this.name, "\t");
          _builder.append(" a_");
          _builder.append(this.name, "\t");
          _builder.append("(\"a_");
          _builder.append(this.name, "\t");
          _builder.append("\");");
          _builder.newLineIfNotEmpty();
          _builder.append("\t");
          _builder.append("sc_signal<bool> done_a_");
          _builder.append(this.name, "\t");
          _builder.append(";");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// -- Testbench Utilities Modules");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("tb_kicker i_tb_kicker(\"i_tb_icker\");");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("tb_endsim");
      {
        boolean _notEquals_3 = (!Objects.equal(this.network, null));
        if (_notEquals_3) {
          _builder.append("_n");
        } else {
          _builder.append("_a");
        }
      }
      _builder.append("_");
      _builder.append(this.name, "\t");
      _builder.append(" i_tb_endsim(\"i_tb_endsim\");");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("sc_signal<bool> done_i_tb_endsim;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      {
        boolean _isEmpty_4 = inputs.isEmpty();
        boolean _not_4 = (!_isEmpty_4);
        if (_not_4) {
          _builder.append("\t");
          _builder.append("// -- Input Drivers");
          _builder.newLine();
          {
            for(final Port port_2 : inputs) {
              _builder.append("\t");
              _builder.append("tb_driver< ");
              Type _type_2 = port_2.getType();
              CharSequence _doSwitch_2 = this.doSwitch(_type_2);
              _builder.append(_doSwitch_2, "\t");
              _builder.append(" > i_tb_driver_");
              String _name_2 = port_2.getName();
              _builder.append(_name_2, "\t");
              _builder.append("(\"i_tb_driver_");
              String _name_3 = port_2.getName();
              _builder.append(_name_3, "\t");
              _builder.append("\");");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_driver_");
              String _name_4 = port_2.getName();
              _builder.append(_name_4, "\t");
              _builder.append(".set_file_name(\"../testbench/traces/");
              {
                boolean _notEquals_4 = (!Objects.equal(this.actor, null));
                if (_notEquals_4) {
                  String _simpleName = this.actor.getSimpleName();
                  _builder.append(_simpleName, "\t");
                  _builder.append("_");
                }
              }
              String _name_5 = port_2.getName();
              _builder.append(_name_5, "\t");
              _builder.append(".txt\");");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("sc_signal<bool> done_i_tb_driver_");
              String _name_6 = port_2.getName();
              _builder.append(_name_6, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _builder.append("\t");
      _builder.newLine();
      {
        boolean _isEmpty_5 = outputs.isEmpty();
        boolean _not_5 = (!_isEmpty_5);
        if (_not_5) {
          _builder.append("\t");
          _builder.append("// -- Compare output with golden reference");
          _builder.newLine();
          {
            for(final Port port_3 : outputs) {
              _builder.append("\t");
              _builder.append("tb_compare< ");
              Type _type_3 = port_3.getType();
              CharSequence _doSwitch_3 = this.doSwitch(_type_3);
              _builder.append(_doSwitch_3, "\t");
              _builder.append(" > i_tb_compare_");
              String _name_7 = port_3.getName();
              _builder.append(_name_7, "\t");
              _builder.append("(\"i_tb_compare_");
              String _name_8 = port_3.getName();
              _builder.append(_name_8, "\t");
              _builder.append("\");");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_compare_");
              String _name_9 = port_3.getName();
              _builder.append(_name_9, "\t");
              _builder.append(".set_file_name(\"../testbench/traces/");
              {
                boolean _notEquals_5 = (!Objects.equal(this.actor, null));
                if (_notEquals_5) {
                  String _simpleName_1 = this.actor.getSimpleName();
                  _builder.append(_simpleName_1, "\t");
                  _builder.append("_");
                }
              }
              String _name_10 = port_3.getName();
              _builder.append(_name_10, "\t");
              _builder.append(".txt\");");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_compare_");
              String _name_11 = port_3.getName();
              _builder.append(_name_11, "\t");
              _builder.append(".set_port_name(\"");
              String _name_12 = port_3.getName();
              _builder.append(_name_12, "\t");
              _builder.append("\");");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("sc_signal<bool> done_i_tb_compare_");
              String _name_13 = port_3.getName();
              _builder.append(_name_13, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// -- Connection of Modules");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// -- Generate a reset & start");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("i_tb_kicker.clk(s_clk);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("i_tb_kicker.reset(reset);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("i_tb_kicker.start(start);");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      {
        boolean _isEmpty_6 = inputs.isEmpty();
        boolean _not_6 = (!_isEmpty_6);
        if (_not_6) {
          _builder.append("\t");
          _builder.append("// -- Driver Connections");
          _builder.newLine();
          {
            for(final Port port_4 : inputs) {
              _builder.append("\t");
              _builder.append("i_tb_driver_");
              String _name_14 = port_4.getName();
              _builder.append(_name_14, "\t");
              _builder.append(".clk(s_clk);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_driver_");
              String _name_15 = port_4.getName();
              _builder.append(_name_15, "\t");
              _builder.append(".reset(reset);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_driver_");
              String _name_16 = port_4.getName();
              _builder.append(_name_16, "\t");
              _builder.append(".start(start);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_driver_");
              String _name_17 = port_4.getName();
              _builder.append(_name_17, "\t");
              _builder.append(".done(done_i_tb_driver_");
              String _name_18 = port_4.getName();
              _builder.append(_name_18, "\t");
              _builder.append(");");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_driver_");
              String _name_19 = port_4.getName();
              _builder.append(_name_19, "\t");
              _builder.append(".dout(q_");
              String _name_20 = port_4.getName();
              _builder.append(_name_20, "\t");
              _builder.append(");");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _builder.append("\t");
      _builder.newLine();
      {
        boolean _isEmpty_7 = outputs.isEmpty();
        boolean _not_7 = (!_isEmpty_7);
        if (_not_7) {
          _builder.append("\t");
          _builder.append("// -- Compare Connections");
          _builder.newLine();
          {
            for(final Port port_5 : outputs) {
              _builder.append("\t");
              _builder.append("i_tb_compare_");
              String _name_21 = port_5.getName();
              _builder.append(_name_21, "\t");
              _builder.append(".clk(s_clk);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_compare_");
              String _name_22 = port_5.getName();
              _builder.append(_name_22, "\t");
              _builder.append(".reset(reset);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_compare_");
              String _name_23 = port_5.getName();
              _builder.append(_name_23, "\t");
              _builder.append(".start(start);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_compare_");
              String _name_24 = port_5.getName();
              _builder.append(_name_24, "\t");
              _builder.append(".done(done_i_tb_compare_");
              String _name_25 = port_5.getName();
              _builder.append(_name_25, "\t");
              _builder.append(");");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("i_tb_compare_");
              String _name_26 = port_5.getName();
              _builder.append(_name_26, "\t");
              _builder.append(".din(q_");
              String _name_27 = port_5.getName();
              _builder.append(_name_27, "\t");
              _builder.append(");");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("i_tb_endsim.clk(s_clk);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("i_tb_endsim.reset(reset);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("i_tb_endsim.start(start);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("i_tb_endsim.done(done_i_tb_endsim);");
      _builder.newLine();
      {
        for(final Port port_6 : outputs) {
          _builder.append("\t");
          _builder.append("i_tb_endsim.done_");
          String _name_28 = port_6.getName();
          _builder.append(_name_28, "\t");
          _builder.append("(done_i_tb_compare_");
          String _name_29 = port_6.getName();
          _builder.append(_name_29, "\t");
          _builder.append(");");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// -- ");
      _builder.append(this.name, "\t");
      _builder.append(" Connections");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append(this.prefix, "\t");
      _builder.append("_");
      _builder.append(this.name, "\t");
      _builder.append(".clk(s_clk);");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append(this.prefix, "\t");
      _builder.append("_");
      _builder.append(this.name, "\t");
      _builder.append(".reset(reset);");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append(this.prefix, "\t");
      _builder.append("_");
      _builder.append(this.name, "\t");
      _builder.append(".start(start);");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append(this.prefix, "\t");
      _builder.append("_");
      _builder.append(this.name, "\t");
      _builder.append(".done(done_");
      _builder.append(this.prefix, "\t");
      _builder.append("_");
      _builder.append(this.name, "\t");
      _builder.append(");");
      _builder.newLineIfNotEmpty();
      {
        boolean _isEmpty_8 = inputs.isEmpty();
        boolean _not_8 = (!_isEmpty_8);
        if (_not_8) {
          {
            for(final Port port_7 : inputs) {
              _builder.append("\t");
              _builder.append(this.prefix, "\t");
              _builder.append("_");
              _builder.append(this.name, "\t");
              _builder.append(".");
              String _name_30 = port_7.getName();
              _builder.append(_name_30, "\t");
              _builder.append("(q_");
              String _name_31 = port_7.getName();
              _builder.append(_name_31, "\t");
              _builder.append(");");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      {
        boolean _isEmpty_9 = outputs.isEmpty();
        boolean _not_9 = (!_isEmpty_9);
        if (_not_9) {
          {
            for(final Port port_8 : outputs) {
              _builder.append("\t");
              _builder.append(this.prefix, "\t");
              _builder.append("_");
              _builder.append(this.name, "\t");
              _builder.append(".");
              String _name_32 = port_8.getName();
              _builder.append(_name_32, "\t");
              _builder.append("(q_");
              String _name_33 = port_8.getName();
              _builder.append(_name_33, "\t");
              _builder.append(");");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("cout <<\"        __  ___ __ ___  _ __   ___  ___ \" << endl;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("cout <<\"        \\\\ \\\\/ / \'__/ _ \\\\| \'_ \\\\ / _ \\\\/ __|\" << endl;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("cout <<\"         >  <| | | (_) | | | | (_) \\\\__ \\\\\" << endl;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("cout <<\"        /_/\\\\_\\\\_|  \\\\___/|_| |_|\\\\___/|___/\" << endl;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("cout <<\"        CAL to SystemC Code Generator\" << endl;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("cout <<\"        Copyright (c) 2012-2014 EPFL SCI-STI-MM\" << endl;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("cout << \"\\nINFO: Start of Simulating \\n\" << endl;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("// -- Start Simulation ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_start();");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("cout << \"\\nINFO: End of Simulating \" << endl;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("return 0;");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
}
