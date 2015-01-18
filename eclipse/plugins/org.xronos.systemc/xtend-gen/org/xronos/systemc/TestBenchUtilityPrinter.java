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
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenation;

@SuppressWarnings("all")
public class TestBenchUtilityPrinter {
  private Network network;
  
  private Actor actor;
  
  private String name;
  
  private String prefix;
  
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
      _builder.append("// Xronos TestBench Utility Modules");
      _builder.newLine();
      _builder.append("// Date: ");
      String _format = dateFormat.format(date);
      _builder.append(_format, "");
      _builder.newLineIfNotEmpty();
      _builder.append("// ----------------------------------------------------------------------------");
      _builder.newLine();
      _builder.newLine();
      _builder.append("/* ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* XRONOS, High Level Synthesis of Streaming Applications");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* Copyright (C) 2014 EPFL SCI STI MM");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("*");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* This file is part of XRONOS.");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("*");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* XRONOS is free software: you can redistribute it and/or modify");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* it under the terms of the GNU General Public License as published by");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* the Free Software Foundation, either version 3 of the License, or");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* (at your option) any later version.");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("*");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* XRONOS is distributed in the hope that it will be useful,");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* but WITHOUT ANY WARRANTY; without even the implied warranty of");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* GNU General Public License for more details.");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("*");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* You should have received a copy of the GNU General Public License");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* Additional permission under GNU GPL version 3 section 7");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* If you modify this Program, or any covered work, by linking or combining it");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* with Eclipse (or a modified version of Eclipse or an Eclipse plugin or ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* an Eclipse library), containing parts covered by the terms of the ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* Eclipse Public License (EPL), the licensors of this Program grant you ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* additional permission to convey the resulting work.  Corresponding Source ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* for a non-source form of such a combination shall include the source code ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* for the parts of Eclipse libraries used as well as that of the  covered work.");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("* ");
      _builder.newLine();
      _builder.append(" ");
      _builder.append("*/");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence getKickerModule() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader();
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("#ifndef TB_KICKER_H");
    _builder.newLine();
    _builder.append("#define TB_KICKER_H");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#include <systemc.h>");
    _builder.newLine();
    _builder.append("#include <iostream>");
    _builder.newLine();
    _builder.newLine();
    _builder.append("using namespace std;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("SC_MODULE(tb_kicker) {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool> clk;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_out<bool> reset;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_out<bool> start;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("  ");
    _builder.append("SC_CTOR(tb_kicker)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("{");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("SC_CTHREAD(prc_reset, clk.pos());");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("//reset_signal_is(reset,true);");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void prc_reset() {");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("reset = true;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("start = false;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("reset = false;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("#ifndef __SYNTHESIS__");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("cout << \"INFO: @\" << sc_time_stamp() << \", \" << \"Reset Off = \" << reset << endl;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("#endif");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("#ifndef __SYNTHESIS__");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("cout << \"INFO: @\" << sc_time_stamp() << \", \" << \"Start On  = \" << start << endl;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("cout << \" \" << endl;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("#endif");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("start = true;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("wait(2);");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("start = false;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("};");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#endif // TB_KICKER_H");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getDriverModule() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader();
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("#ifndef TB_DRIVER_H");
    _builder.newLine();
    _builder.append("#define TB_DRIVER_H");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#include <systemc.h>");
    _builder.newLine();
    _builder.append("#include <iostream>");
    _builder.newLine();
    _builder.append("#include <fstream>");
    _builder.newLine();
    _builder.append("#include <sstream>");
    _builder.newLine();
    _builder.newLine();
    _builder.append("using namespace std;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("template<class T>");
    _builder.newLine();
    _builder.append("SC_MODULE(tb_driver) {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool> clk;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool> reset;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool> start;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_out<bool> done;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_fifo_out< T > dout;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("string file_name;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("SC_CTOR(tb_driver):");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("file_name(\"\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("{");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("SC_CTHREAD(prc_read, clk.pos());");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("reset_signal_is(reset,true);");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void set_file_name(string file_name){");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("this->file_name = file_name;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}\t");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void prc_read() {");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("done = false;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("do { wait(); } while ( !start.read() );");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("ifstream in_file;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("in_file.open(file_name.c_str());");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("if (in_file.fail()) {");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("cerr << \"unable to open file \" << file_name <<\" for reading\" << endl;");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("exit(1);");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("T n;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("while(!in_file.eof()){");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("string line;");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("getline(in_file, line);");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("if(line != \"\"){");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("istringstream iss(line);");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("iss >> n;");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("dout.write(n);");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("in_file.close();");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("done = true;");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("};");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#endif // TB_DRIVER_H");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getCompareModule() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader();
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("#ifndef TB_CAPTURE_H");
    _builder.newLine();
    _builder.append("#define TB_CAPTURE_H");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#include <systemc.h>");
    _builder.newLine();
    _builder.append("#include <iostream>");
    _builder.newLine();
    _builder.append("#include <fstream>");
    _builder.newLine();
    _builder.append("#include <sstream>");
    _builder.newLine();
    _builder.newLine();
    _builder.append("using namespace std;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("template<class T>");
    _builder.newLine();
    _builder.append("SC_MODULE(tb_compare) {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool> clk;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool> reset;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool> start;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_out<bool> done;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_fifo_in< T > din;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("string file_name;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("string port_name;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("SC_CTOR(tb_compare)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("{");
    _builder.newLine();
    _builder.append("    \t");
    _builder.append("SC_CTHREAD(prc_compare, clk.pos());");
    _builder.newLine();
    _builder.append(" \t\t");
    _builder.append("reset_signal_is(reset,true);");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void set_file_name(string file_name){");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("this->file_name = file_name;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void set_port_name(string port_name){");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("this->port_name = port_name;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void prc_compare() {");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("done = false;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("do { wait(); } while ( !start.read() );");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("ifstream in_file;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("in_file.open(file_name.c_str());");
    _builder.newLine();
    _builder.append("\t  \t");
    _builder.append("if (in_file.fail()) {");
    _builder.newLine();
    _builder.append("    \t\t");
    _builder.append("cerr << \"Unable to open file \" << file_name <<\" for reading\" << endl;");
    _builder.newLine();
    _builder.append("    \t\t");
    _builder.append("exit(1);");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("T golden;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("T sim;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("int line_counter = 0;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("while(!in_file.eof()){");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("string line;");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("getline(in_file, line);");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("if(line != \"\"){");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("// -- Read golden reference");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("istringstream iss(line);");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("iss >> golden;\t");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("// -- Read from the input");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("sim = din.read();");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("if( sim != golden){");
    _builder.newLine();
    _builder.append("\t\t\t\t\t");
    _builder.append("cout  << \"ERROR: @\" << sc_time_stamp() << \", \" << \"On port \" << port_name << \" incorrect value computed \" << sim << \" instead of \" << golden << \", sequence \" << line_counter << endl; ");
    _builder.newLine();
    _builder.append("\t\t\t\t\t");
    _builder.append("sc_stop();");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("}else{");
    _builder.newLine();
    _builder.append("\t\t\t\t\t");
    _builder.append("cout  << \"INFO: @\" << sc_time_stamp() << \", \" << \"On port \" << port_name << \" correct value computed (\" << sim << \"), sequence \" << line_counter << endl;");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("line_counter++;\t");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("wait();\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append(" \t\t");
    _builder.append("in_file.close();");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("done = true;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("};");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#endif // TB_CAPTURE_H");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getEndSimModule() {
    CharSequence _xblockexpression = null;
    {
      List<Port> outputs = null;
      boolean _notEquals = (!Objects.equal(this.network, null));
      if (_notEquals) {
        EList<Port> _outputs = this.network.getOutputs();
        outputs = _outputs;
      } else {
        EList<Port> _outputs_1 = this.actor.getOutputs();
        outputs = _outputs_1;
      }
      StringConcatenation _builder = new StringConcatenation();
      CharSequence _header = this.getHeader();
      _builder.append(_header, "");
      _builder.newLineIfNotEmpty();
      _builder.append("#ifndef TB_ENDSIM");
      {
        boolean _notEquals_1 = (!Objects.equal(this.network, null));
        if (_notEquals_1) {
          _builder.append("_N");
        } else {
          _builder.append("_A");
        }
      }
      _builder.append("_");
      String _upperCase = this.name.toUpperCase();
      _builder.append(_upperCase, "");
      _builder.append("_H");
      _builder.newLineIfNotEmpty();
      _builder.append("#define TB_ENDSIM");
      {
        boolean _notEquals_2 = (!Objects.equal(this.network, null));
        if (_notEquals_2) {
          _builder.append("_N");
        } else {
          _builder.append("_A");
        }
      }
      _builder.append("_");
      String _upperCase_1 = this.name.toUpperCase();
      _builder.append(_upperCase_1, "");
      _builder.append("_H");
      _builder.newLineIfNotEmpty();
      _builder.newLine();
      _builder.append("#include <systemc.h>");
      _builder.newLine();
      _builder.newLine();
      _builder.append("using namespace std;");
      _builder.newLine();
      _builder.newLine();
      _builder.append("SC_MODULE(tb_endsim");
      {
        boolean _notEquals_3 = (!Objects.equal(this.network, null));
        if (_notEquals_3) {
          _builder.append("_n");
        } else {
          _builder.append("_a");
        }
      }
      _builder.append("_");
      _builder.append(this.name, "");
      _builder.append(") {");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("sc_in<bool> clk;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_in<bool> reset;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_in<bool> start;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("sc_out<bool> done;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      {
        boolean _hasElements = false;
        for(final Port port : outputs) {
          if (!_hasElements) {
            _hasElements = true;
          } else {
            _builder.appendImmediate("\n", "\t");
          }
          _builder.append("\t");
          _builder.append("sc_in<bool> done_");
          String _name = port.getName();
          _builder.append(_name, "\t");
          _builder.append(";");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("\t\t\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("SC_CTOR(tb_endsim");
      {
        boolean _notEquals_4 = (!Objects.equal(this.network, null));
        if (_notEquals_4) {
          _builder.append("_n");
        } else {
          _builder.append("_a");
        }
      }
      _builder.append("_");
      _builder.append(this.name, "\t");
      _builder.append(")");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("{");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("SC_CTHREAD(prc_stop, clk.pos());");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("reset_signal_is(reset,true);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t");
      _builder.append("void prc_stop() {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("done = false;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("do { wait(); } while ( !start.read() );");
      _builder.newLine();
      _builder.newLine();
      {
        boolean _hasElements_1 = false;
        for(final Port port_1 : outputs) {
          if (!_hasElements_1) {
            _hasElements_1 = true;
          } else {
            _builder.appendImmediate("\n", "\t\t");
          }
          _builder.append("\t\t");
          _builder.append("do { wait(); } while ( !done_");
          String _name_1 = port_1.getName();
          _builder.append(_name_1, "\t\t");
          _builder.append(".read() );");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("wait();");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("cout << \"INFO: Finishing reading from Golden reference !!!\" << endl;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("sc_stop();");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("done = true;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("};");
      _builder.newLine();
      _builder.newLine();
      _builder.append("#endif // TB_ENDSIM");
      {
        boolean _notEquals_5 = (!Objects.equal(this.network, null));
        if (_notEquals_5) {
          _builder.append("_N");
        } else {
          _builder.append("_I");
        }
      }
      _builder.append("_");
      String _upperCase_2 = this.name.toUpperCase();
      _builder.append(_upperCase_2, "");
      _builder.append("_H");
      _builder.newLineIfNotEmpty();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
}
