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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.Port;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.Type;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenation;

/**
 * A VHDL Testbench printer
 * 
 * @author Endri Bezati
 */
@SuppressWarnings("all")
public class VHDLTestbenchPrinter {
  private Boolean globalVerification = Boolean.valueOf(false);
  
  private Boolean doubleBuffering = Boolean.valueOf(false);
  
  /**
   * Contains a Map which indicates the index of the given clock
   */
  private Map<String, Integer> clockDomainsIndex;
  
  private String DEFAULT_CLOCK_DOMAIN = "CLK";
  
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
  
  public void setNetwork(final Network network) {
    this.vertex = network;
    String _simpleName = network.getSimpleName();
    this.name = _simpleName;
    Map<String, String> clkDomains = new HashMap<String, String>();
    boolean _containsKey = this.options.containsKey("clkDomains");
    if (_containsKey) {
      Object _get = this.options.get("clkDomains");
      clkDomains = ((Map<String, String>) _get);
    }
    boolean _containsKey_1 = this.options.containsKey("doubleBuffering");
    if (_containsKey_1) {
      Object _get_1 = this.options.get("doubleBuffering");
      this.doubleBuffering = ((Boolean) _get_1);
    }
    this.computeNetworkClockDomains(network, clkDomains);
  }
  
  public Map<String, Object> setOptions(final Map<String, Object> options) {
    return this.options = options;
  }
  
  private CharSequence getHeader() {
    CharSequence _xblockexpression = null;
    {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      Date date = new Date();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("-- ----------------------------------------------------------------------------");
      _builder.newLine();
      _builder.append("-- __  ___ __ ___  _ __   ___  ___ ");
      _builder.newLine();
      _builder.append("-- \\ \\/ / \'__/ _ \\| \'_ \\ / _ \\/ __|");
      _builder.newLine();
      _builder.append("--  >  <| | | (_) | | | | (_) \\__ \\");
      _builder.newLine();
      _builder.append("-- /_/\\_\\_|  \\___/|_| |_|\\___/|___/");
      _builder.newLine();
      _builder.append("-- ----------------------------------------------------------------------------");
      _builder.newLine();
      _builder.append("-- Xronos SystemC, VHDL TestBench");
      _builder.newLine();
      {
        if ((this.vertex instanceof Network)) {
          _builder.append("\t");
          _builder.append("-- TestBench file for Network: ");
          _builder.append(this.name, "\t");
          _builder.append(" ");
          _builder.newLineIfNotEmpty();
        } else {
          _builder.append("\t");
          _builder.append("-- TestBench file for Actor: ");
          _builder.append(this.name, "\t");
          _builder.append(" ");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("-- Date: ");
      String _format = dateFormat.format(date);
      _builder.append(_format, "");
      _builder.newLineIfNotEmpty();
      _builder.append("-- ----------------------------------------------------------------------------");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence addLibraries() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("library ieee;");
    _builder.newLine();
    _builder.append("use ieee.std_logic_1164.all;");
    _builder.newLine();
    _builder.append("use ieee.std_logic_unsigned.all;");
    _builder.newLine();
    _builder.append("use ieee.numeric_std.all;");
    _builder.newLine();
    _builder.append("use std.textio.all;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("library work;");
    _builder.newLine();
    _builder.append("use work.sim_package.all;");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence addEntity(final Vertex vertex) {
    CharSequence _xblockexpression = null;
    {
      String name = null;
      if ((vertex instanceof Actor)) {
        String _simpleName = ((Actor) vertex).getSimpleName();
        name = _simpleName;
      } else {
        if ((vertex instanceof Network)) {
          String _simpleName_1 = ((Network) vertex).getSimpleName();
          name = _simpleName_1;
        }
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("entity ");
      _builder.append(name, "");
      _builder.append("_tb is");
      _builder.newLineIfNotEmpty();
      _builder.append("end ");
      _builder.append(name, "");
      _builder.append("_tb;");
      _builder.newLineIfNotEmpty();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence addArchitecture(final Vertex vertex) {
    CharSequence _xblockexpression = null;
    {
      String name = null;
      if ((vertex instanceof Actor)) {
        String _simpleName = ((Actor) vertex).getSimpleName();
        name = _simpleName;
      } else {
        if ((vertex instanceof Network)) {
          String _simpleName_1 = ((Network) vertex).getSimpleName();
          name = _simpleName_1;
        }
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("architecture arch_");
      _builder.append(name, "");
      _builder.append("_tb of ");
      _builder.append(name, "");
      _builder.append("_tb is");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      CharSequence _addArchitectureComponent = this.addArchitectureComponent(vertex);
      _builder.append(_addArchitectureComponent, "\t");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      CharSequence _addArchitectureSignals = this.addArchitectureSignals(vertex);
      _builder.append(_addArchitectureSignals, "\t");
      _builder.newLineIfNotEmpty();
      _builder.append("begin");
      _builder.newLine();
      CharSequence _addBeginBody = this.addBeginBody(vertex);
      _builder.append(_addBeginBody, "");
      _builder.newLineIfNotEmpty();
      _builder.append("end architecture arch_");
      _builder.append(name, "");
      _builder.append("_tb; ");
      _builder.newLineIfNotEmpty();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence addArchitectureComponent(final Vertex vertex) {
    CharSequence _xblockexpression = null;
    {
      String name = null;
      List<Port> inputPorts = null;
      List<Port> outputPorts = null;
      if ((vertex instanceof Actor)) {
        String _simpleName = ((Actor) vertex).getSimpleName();
        name = _simpleName;
        EList<Port> _inputs = ((Actor) vertex).getInputs();
        inputPorts = _inputs;
        EList<Port> _outputs = ((Actor) vertex).getOutputs();
        outputPorts = _outputs;
      } else {
        if ((vertex instanceof Network)) {
          String _simpleName_1 = ((Network) vertex).getSimpleName();
          name = _simpleName_1;
          EList<Port> _inputs_1 = ((Network) vertex).getInputs();
          inputPorts = _inputs_1;
          EList<Port> _outputs_1 = ((Network) vertex).getOutputs();
          outputPorts = _outputs_1;
        }
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("\t");
      _builder.append("-----------------------------------------------------------------------");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("-- Component declaration");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("-----------------------------------------------------------------------");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("component ");
      _builder.append(name, "\t");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("port(");
      _builder.newLine();
      {
        for(final Port port : inputPorts) {
          _builder.append("\t    ");
          CharSequence _addComponentPort = this.addComponentPort(port, "IN", "OUT");
          _builder.append(_addComponentPort, "\t    ");
          _builder.newLineIfNotEmpty();
        }
      }
      {
        for(final Port port_1 : outputPorts) {
          _builder.append("\t    ");
          CharSequence _addComponentPort_1 = this.addComponentPort(port_1, "OUT", "IN");
          _builder.append(_addComponentPort_1, "\t    ");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.newLine();
      _builder.append("\t    ");
      _builder.append("clk: IN std_logic;");
      _builder.newLine();
      _builder.append("\t    ");
      _builder.append("reset: IN std_logic;");
      _builder.newLine();
      _builder.append("\t    ");
      _builder.append("start: IN std_logic;");
      _builder.newLine();
      _builder.append("\t    ");
      _builder.append("done: OUT std_logic);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("end component ");
      _builder.append(name, "\t");
      _builder.append(";");
      _builder.newLineIfNotEmpty();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence addComponentPort(final Port port, final String dirA, final String dirB) {
    StringConcatenation _builder = new StringConcatenation();
    {
      boolean _or = false;
      Type _type = port.getType();
      boolean _isBool = _type.isBool();
      if (_isBool) {
        _or = true;
      } else {
        Type _type_1 = port.getType();
        int _sizeInBits = _type_1.getSizeInBits();
        boolean _equals = (_sizeInBits == 1);
        _or = _equals;
      }
      if (_or) {
        CharSequence _portName = this.getPortName(port);
        _builder.append(_portName, "");
        _builder.append("_");
        {
          boolean _equals_1 = dirA.equals("IN");
          if (_equals_1) {
            _builder.append("dout");
          } else {
            _builder.append("din");
          }
        }
        _builder.append(" : ");
        _builder.append(dirA, "");
        _builder.append(" std_logic;");
        _builder.newLineIfNotEmpty();
      } else {
        CharSequence _portName_1 = this.getPortName(port);
        _builder.append(_portName_1, "");
        _builder.append("_");
        {
          boolean _equals_2 = dirA.equals("IN");
          if (_equals_2) {
            _builder.append("dout");
          } else {
            _builder.append("din");
          }
        }
        _builder.append(" : ");
        _builder.append(dirA, "");
        _builder.append(" std_logic_vector(");
        Type _type_2 = port.getType();
        int _sizeInBits_1 = _type_2.getSizeInBits();
        int _minus = (_sizeInBits_1 - 1);
        _builder.append(_minus, "");
        _builder.append(" downto 0);");
        _builder.newLineIfNotEmpty();
      }
    }
    CharSequence _portName_2 = this.getPortName(port);
    _builder.append(_portName_2, "");
    _builder.append("_");
    {
      boolean _equals_3 = dirA.equals("IN");
      if (_equals_3) {
        _builder.append("empty_n");
      } else {
        _builder.append("full_n");
      }
    }
    _builder.append(" : IN std_logic;");
    _builder.newLineIfNotEmpty();
    CharSequence _portName_3 = this.getPortName(port);
    _builder.append(_portName_3, "");
    _builder.append("_");
    {
      boolean _equals_4 = dirA.equals("IN");
      if (_equals_4) {
        _builder.append("read");
      } else {
        _builder.append("write");
      }
    }
    _builder.append(" : OUT std_logic;");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence addArchitectureSignals(final Vertex vertex) {
    CharSequence _xblockexpression = null;
    {
      String name = null;
      String traceName = null;
      List<Port> inputPorts = null;
      List<Port> outputPorts = null;
      if ((vertex instanceof Actor)) {
        String _simpleName = ((Actor) vertex).getSimpleName();
        name = _simpleName;
        String _name = ((Actor) vertex).getName();
        traceName = _name;
        EList<Port> _inputs = ((Actor) vertex).getInputs();
        inputPorts = _inputs;
        EList<Port> _outputs = ((Actor) vertex).getOutputs();
        outputPorts = _outputs;
      } else {
        if ((vertex instanceof Network)) {
          String _simpleName_1 = ((Network) vertex).getSimpleName();
          name = _simpleName_1;
          String _simpleName_2 = ((Network) vertex).getSimpleName();
          traceName = _simpleName_2;
          EList<Port> _inputs_1 = ((Network) vertex).getInputs();
          inputPorts = _inputs_1;
          EList<Port> _outputs_1 = ((Network) vertex).getOutputs();
          outputPorts = _outputs_1;
        }
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("-----------------------------------------------------------------------");
      _builder.newLine();
      _builder.append("-- Achitecure signals & constants");
      _builder.newLine();
      _builder.append("-----------------------------------------------------------------------");
      _builder.newLine();
      {
        if ((vertex instanceof Network)) {
          {
            Set<String> _keySet = this.clockDomainsIndex.keySet();
            boolean _hasElements = false;
            for(final String string : _keySet) {
              if (!_hasElements) {
                _hasElements = true;
              } else {
                _builder.appendImmediate("\n", "");
              }
              _builder.append("constant ");
              _builder.append(string, "");
              _builder.append("_PERIOD : time := 100 ns;");
              _builder.newLineIfNotEmpty();
              _builder.append("constant ");
              _builder.append(string, "");
              _builder.append("_DUTY_CYCLE : real := 0.5;");
              _builder.newLineIfNotEmpty();
            }
          }
          _builder.append("constant OFFSET : time := 100 ns;");
          _builder.newLine();
        } else {
          _builder.append("constant CLK_PERIOD : time := 100 ns;");
          _builder.newLine();
          _builder.append("constant CLK_DUTY_CYCLE : real := 0.5;");
          _builder.newLine();
          _builder.append("constant OFFSET : time := 100 ns;");
          _builder.newLine();
        }
      }
      _builder.append("-- Severity level and testbench type types");
      _builder.newLine();
      _builder.append("type severity_level is (note, warning, error, failure);");
      _builder.newLine();
      _builder.append("type tb_type is (after_reset, read_file, CheckRead);");
      _builder.newLine();
      _builder.newLine();
      _builder.append("-- Component input(s) signals");
      _builder.newLine();
      {
        for(final Port port : inputPorts) {
          _builder.append("signal tb_FSM_");
          CharSequence _portName = this.getPortName(port);
          _builder.append(_portName, "");
          _builder.append(" : tb_type;");
          _builder.newLineIfNotEmpty();
          _builder.append("file sim_file_");
          _builder.append(name, "");
          _builder.append("_");
          CharSequence _portName_1 = this.getPortName(port);
          _builder.append(_portName_1, "");
          _builder.append(" : text is \"../../testbench/traces/");
          _builder.append(traceName, "");
          _builder.append("_");
          String _name_1 = port.getName();
          _builder.append(_name_1, "");
          _builder.append(".txt\";");
          _builder.newLineIfNotEmpty();
          {
            boolean _or = false;
            Type _type = port.getType();
            boolean _isBool = _type.isBool();
            if (_isBool) {
              _or = true;
            } else {
              Type _type_1 = port.getType();
              int _sizeInBits = _type_1.getSizeInBits();
              boolean _equals = (_sizeInBits == 1);
              _or = _equals;
            }
            if (_or) {
              _builder.append("signal ");
              CharSequence _portName_2 = this.getPortName(port);
              _builder.append(_portName_2, "");
              _builder.append("_dout : std_logic := \'0\';");
              _builder.newLineIfNotEmpty();
            } else {
              _builder.append("signal ");
              CharSequence _portName_3 = this.getPortName(port);
              _builder.append(_portName_3, "");
              _builder.append("_dout : std_logic_vector(");
              Type _type_2 = port.getType();
              int _sizeInBits_1 = _type_2.getSizeInBits();
              int _minus = (_sizeInBits_1 - 1);
              _builder.append(_minus, "");
              _builder.append(" downto 0) := (others => \'0\');");
              _builder.newLineIfNotEmpty();
            }
          }
          _builder.append("signal ");
          CharSequence _portName_4 = this.getPortName(port);
          _builder.append(_portName_4, "");
          _builder.append("_empty_n : std_logic := \'0\';");
          _builder.newLineIfNotEmpty();
          _builder.append("signal ");
          CharSequence _portName_5 = this.getPortName(port);
          _builder.append(_portName_5, "");
          _builder.append("_read : std_logic;");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.newLine();
      _builder.append("-- Component Output(s) signals");
      _builder.newLine();
      {
        for(final Port port_1 : outputPorts) {
          _builder.append("signal tb_FSM_");
          CharSequence _portName_6 = this.getPortName(port_1);
          _builder.append(_portName_6, "");
          _builder.append(" : tb_type;");
          _builder.newLineIfNotEmpty();
          _builder.append("file sim_file_");
          _builder.append(name, "");
          _builder.append("_");
          CharSequence _portName_7 = this.getPortName(port_1);
          _builder.append(_portName_7, "");
          _builder.append(" : text is \"../../testbench/traces/");
          _builder.append(traceName, "");
          _builder.append("_");
          String _name_2 = port_1.getName();
          _builder.append(_name_2, "");
          _builder.append(".txt\";");
          _builder.newLineIfNotEmpty();
          {
            boolean _or_1 = false;
            Type _type_3 = port_1.getType();
            boolean _isBool_1 = _type_3.isBool();
            if (_isBool_1) {
              _or_1 = true;
            } else {
              Type _type_4 = port_1.getType();
              int _sizeInBits_2 = _type_4.getSizeInBits();
              boolean _equals_1 = (_sizeInBits_2 == 1);
              _or_1 = _equals_1;
            }
            if (_or_1) {
              _builder.append("signal ");
              CharSequence _portName_8 = this.getPortName(port_1);
              _builder.append(_portName_8, "");
              _builder.append("_din : std_logic := \'0\';");
              _builder.newLineIfNotEmpty();
            } else {
              _builder.append("signal ");
              CharSequence _portName_9 = this.getPortName(port_1);
              _builder.append(_portName_9, "");
              _builder.append("_din : std_logic_vector(");
              Type _type_5 = port_1.getType();
              int _sizeInBits_3 = _type_5.getSizeInBits();
              int _minus_1 = (_sizeInBits_3 - 1);
              _builder.append(_minus_1, "");
              _builder.append(" downto 0) := (others => \'0\');");
              _builder.newLineIfNotEmpty();
            }
          }
          _builder.append("signal ");
          CharSequence _portName_10 = this.getPortName(port_1);
          _builder.append(_portName_10, "");
          _builder.append("_full_n : std_logic := \'0\';");
          _builder.newLineIfNotEmpty();
          _builder.append("signal ");
          CharSequence _portName_11 = this.getPortName(port_1);
          _builder.append(_portName_11, "");
          _builder.append("_write : std_logic;");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.newLine();
      {
        if ((this.globalVerification).booleanValue()) {
          _builder.append("-- Actors output file");
          _builder.newLine();
          {
            if ((vertex instanceof Network)) {
              {
                EList<Vertex> _vertices = ((Network) vertex).getVertices();
                for(final Vertex v : _vertices) {
                  {
                    if ((v instanceof Actor)) {
                      {
                        EList<Port> _outputs_2 = ((Actor) v).getOutputs();
                        for(final Port port_2 : _outputs_2) {
                          _builder.append("\t");
                          _builder.append("file sim_file_");
                          _builder.append(name, "\t");
                          _builder.append("_");
                          CharSequence _portName_12 = this.getPortName(port_2);
                          _builder.append(_portName_12, "\t");
                          _builder.append(" : text is \"fifoTraces/");
                          String _simpleName_3 = ((Actor) v).getSimpleName();
                          _builder.append(_simpleName_3, "\t");
                          _builder.append("_");
                          CharSequence _portName_13 = this.getPortName(port_2);
                          _builder.append(_portName_13, "\t");
                          _builder.append(".txt\";");
                          _builder.newLineIfNotEmpty();
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      _builder.newLine();
      _builder.append("signal count : integer range 255 downto 0 := 0;");
      _builder.newLine();
      {
        if ((vertex instanceof Network)) {
          {
            Set<String> _keySet_1 = this.clockDomainsIndex.keySet();
            for(final String string_1 : _keySet_1) {
              _builder.append("signal ");
              _builder.append(string_1, "");
              _builder.append(" : std_logic := \'0\';");
              _builder.newLineIfNotEmpty();
            }
          }
        } else {
          _builder.append("signal clk : std_logic := \'0\';");
          _builder.newLine();
        }
      }
      _builder.append("signal reset : std_logic := \'0\';");
      _builder.newLine();
      _builder.append("signal start : std_logic := \'0\';");
      _builder.newLine();
      _builder.newLine();
      {
        if ((this.doubleBuffering).booleanValue()) {
          _builder.append("signal CG_EN : std_logic := \'1\';");
          _builder.newLine();
        }
      }
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence addBeginBody(final Vertex vertex) {
    CharSequence _xblockexpression = null;
    {
      String name = null;
      List<Port> inputPorts = null;
      List<Port> outputPorts = null;
      if ((vertex instanceof Actor)) {
        String _simpleName = ((Actor) vertex).getSimpleName();
        name = _simpleName;
        EList<Port> _inputs = ((Actor) vertex).getInputs();
        inputPorts = _inputs;
        EList<Port> _outputs = ((Actor) vertex).getOutputs();
        outputPorts = _outputs;
      } else {
        if ((vertex instanceof Network)) {
          String _simpleName_1 = ((Network) vertex).getSimpleName();
          name = _simpleName_1;
          EList<Port> _inputs_1 = ((Network) vertex).getInputs();
          inputPorts = _inputs_1;
          EList<Port> _outputs_1 = ((Network) vertex).getOutputs();
          outputPorts = _outputs_1;
        }
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("i_");
      _builder.append(name, "\t");
      _builder.append(" : ");
      _builder.append(name, "\t");
      _builder.append(" ");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("port map(");
      _builder.newLine();
      {
        boolean _hasElements = false;
        for(final Port port : inputPorts) {
          if (!_hasElements) {
            _hasElements = true;
          } else {
            _builder.appendImmediate("\n", "\t\t");
          }
          _builder.append("\t\t");
          CharSequence _portName = this.getPortName(port);
          _builder.append(_portName, "\t\t");
          _builder.append("_dout => ");
          CharSequence _portName_1 = this.getPortName(port);
          _builder.append(_portName_1, "\t\t");
          _builder.append("_dout,");
          _builder.newLineIfNotEmpty();
          _builder.append("\t\t");
          CharSequence _portName_2 = this.getPortName(port);
          _builder.append(_portName_2, "\t\t");
          _builder.append("_empty_n => ");
          CharSequence _portName_3 = this.getPortName(port);
          _builder.append(_portName_3, "\t\t");
          _builder.append("_empty_n,");
          _builder.newLineIfNotEmpty();
          _builder.append("\t\t");
          CharSequence _portName_4 = this.getPortName(port);
          _builder.append(_portName_4, "\t\t");
          _builder.append("_read => ");
          CharSequence _portName_5 = this.getPortName(port);
          _builder.append(_portName_5, "\t\t");
          _builder.append("_read,");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("\t\t");
      _builder.newLine();
      {
        boolean _hasElements_1 = false;
        for(final Port port_1 : outputPorts) {
          if (!_hasElements_1) {
            _hasElements_1 = true;
          } else {
            _builder.appendImmediate("\n", "\t\t");
          }
          _builder.append("\t\t");
          CharSequence _portName_6 = this.getPortName(port_1);
          _builder.append(_portName_6, "\t\t");
          _builder.append("_din => ");
          CharSequence _portName_7 = this.getPortName(port_1);
          _builder.append(_portName_7, "\t\t");
          _builder.append("_din,");
          _builder.newLineIfNotEmpty();
          _builder.append("\t\t");
          CharSequence _portName_8 = this.getPortName(port_1);
          _builder.append(_portName_8, "\t\t");
          _builder.append("_full_n => ");
          CharSequence _portName_9 = this.getPortName(port_1);
          _builder.append(_portName_9, "\t\t");
          _builder.append("_full_n,");
          _builder.newLineIfNotEmpty();
          _builder.append("\t\t");
          CharSequence _portName_10 = this.getPortName(port_1);
          _builder.append(_portName_10, "\t\t");
          _builder.append("_write => ");
          CharSequence _portName_11 = this.getPortName(port_1);
          _builder.append(_portName_11, "\t\t");
          _builder.append("_write,");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("\t\t");
      _builder.newLine();
      {
        if ((this.doubleBuffering).booleanValue()) {
          _builder.append("\t\t");
          _builder.append("CG_EN => CG_EN,");
          _builder.newLine();
        }
      }
      _builder.append("\t\t");
      _builder.append("start => start,");
      _builder.newLine();
      {
        if ((vertex instanceof Network)) {
          {
            Set<String> _keySet = this.clockDomainsIndex.keySet();
            for(final String string : _keySet) {
              _builder.append("\t\t");
              _builder.append(string, "\t\t");
              _builder.append(" => ");
              _builder.append(string, "\t\t");
              _builder.append(",");
              _builder.newLineIfNotEmpty();
            }
          }
        } else {
          _builder.append("\t\t");
          _builder.append("CLK => CLK,");
          _builder.newLine();
        }
      }
      _builder.append("\t\t");
      _builder.append("reset => reset);");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t");
      _builder.append("-- Clock process");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("clockProcess : process");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("begin");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("wait for OFFSET;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("clockLOOP : loop");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("CLK <= \'0\';");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("wait for (CLK_PERIOD - (CLK_PERIOD * CLK_DUTY_CYCLE));");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("CLK <= \'1\';");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("wait for (CLK_PERIOD * CLK_DUTY_CYCLE);");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("end loop clockLOOP;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("end process;");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("-- Reset process");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("resetProcess : process");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("begin");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("wait for OFFSET;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("-- reset state for 100 ns.");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("reset <= \'1\';");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("start <= \'1\';");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("wait for 100 ns;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("reset <= \'0\';");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("wait;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("end process;");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("-- Input(s) Waveform Generation");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("WaveGen_Proc_In : process (CLK)");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("variable Input_bit : integer range 2147483647 downto - 2147483648;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("variable line_number : line;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("begin");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("if rising_edge(CLK) then");
      _builder.newLine();
      {
        boolean _hasElements_2 = false;
        for(final Port port_2 : inputPorts) {
          if (!_hasElements_2) {
            _hasElements_2 = true;
          } else {
            _builder.appendImmediate("\n", "\t\t");
          }
          {
            boolean _isNative = port_2.isNative();
            boolean _not = (!_isNative);
            if (_not) {
              _builder.append("\t\t");
              _builder.append("-- Input port: ");
              CharSequence _portName_12 = this.getPortName(port_2);
              _builder.append(_portName_12, "\t\t");
              _builder.append(" Waveform Generation");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t");
              _builder.append("case tb_FSM_");
              CharSequence _portName_13 = this.getPortName(port_2);
              _builder.append(_portName_13, "\t\t\t");
              _builder.append(" is");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t");
              _builder.append("when after_reset =>");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("count <= count + 1;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("if (count = 15) then");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("tb_FSM_");
              CharSequence _portName_14 = this.getPortName(port_2);
              _builder.append(_portName_14, "\t\t\t\t\t\t");
              _builder.append(" <= read_file;");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("count <= 0;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("end if;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t");
              _builder.append("when read_file =>");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("if (not endfile (sim_file_");
              _builder.append(name, "\t\t\t\t\t");
              _builder.append("_");
              CharSequence _portName_15 = this.getPortName(port_2);
              _builder.append(_portName_15, "\t\t\t\t\t");
              _builder.append(")) then");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("readline(sim_file_");
              _builder.append(name, "\t\t\t\t\t\t");
              _builder.append("_");
              CharSequence _portName_16 = this.getPortName(port_2);
              _builder.append(_portName_16, "\t\t\t\t\t\t");
              _builder.append(", line_number);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("if (line_number\'length > 0 and line_number(1) /= \'/\') then");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t\t");
              _builder.append("read(line_number, input_bit);");
              _builder.newLine();
              {
                boolean _or = false;
                Type _type = port_2.getType();
                boolean _isBool = _type.isBool();
                if (_isBool) {
                  _or = true;
                } else {
                  Type _type_1 = port_2.getType();
                  int _sizeInBits = _type_1.getSizeInBits();
                  boolean _equals = (_sizeInBits == 1);
                  _or = _equals;
                }
                if (_or) {
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("if (input_bit = 1) then");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("\t");
                  CharSequence _portName_17 = this.getPortName(port_2);
                  _builder.append(_portName_17, "\t\t\t\t\t\t\t\t");
                  _builder.append("_data <= \'1\';");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("else");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("\t");
                  CharSequence _portName_18 = this.getPortName(port_2);
                  _builder.append(_portName_18, "\t\t\t\t\t\t\t\t");
                  _builder.append("_data <= \'0\';");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("end if;");
                  _builder.newLine();
                } else {
                  {
                    Type _type_2 = port_2.getType();
                    boolean _isUint = _type_2.isUint();
                    if (_isUint) {
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t\t");
                      CharSequence _portName_19 = this.getPortName(port_2);
                      _builder.append(_portName_19, "\t\t\t\t\t\t\t");
                      _builder.append("_dout <= std_logic_vector(to_unsigned(input_bit, ");
                      Type _type_3 = port_2.getType();
                      int _sizeInBits_1 = _type_3.getSizeInBits();
                      _builder.append(_sizeInBits_1, "\t\t\t\t\t\t\t");
                      _builder.append("));");
                      _builder.newLineIfNotEmpty();
                    } else {
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t\t");
                      CharSequence _portName_20 = this.getPortName(port_2);
                      _builder.append(_portName_20, "\t\t\t\t\t\t\t");
                      _builder.append("_dout <= std_logic_vector(to_signed(input_bit, ");
                      Type _type_4 = port_2.getType();
                      int _sizeInBits_2 = _type_4.getSizeInBits();
                      _builder.append(_sizeInBits_2, "\t\t\t\t\t\t\t");
                      _builder.append("));");
                      _builder.newLineIfNotEmpty();
                    }
                  }
                }
              }
              _builder.append("\t\t");
              _builder.append("\t\t\t\t\t");
              CharSequence _portName_21 = this.getPortName(port_2);
              _builder.append(_portName_21, "\t\t\t\t\t\t\t");
              _builder.append("_empty_n <= \'1\';");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t\t");
              _builder.append("tb_FSM_");
              CharSequence _portName_22 = this.getPortName(port_2);
              _builder.append(_portName_22, "\t\t\t\t\t\t\t");
              _builder.append(" <= CheckRead;");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("end if;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("end if;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t");
              _builder.append("when CheckRead =>");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("if (not endfile (sim_file_");
              _builder.append(name, "\t\t\t\t\t");
              _builder.append("_");
              CharSequence _portName_23 = this.getPortName(port_2);
              _builder.append(_portName_23, "\t\t\t\t\t");
              _builder.append(")) and ");
              CharSequence _portName_24 = this.getPortName(port_2);
              _builder.append(_portName_24, "\t\t\t\t\t");
              _builder.append("_read = \'1\' then");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("readline(sim_file_");
              _builder.append(name, "\t\t\t\t\t\t");
              _builder.append("_");
              CharSequence _portName_25 = this.getPortName(port_2);
              _builder.append(_portName_25, "\t\t\t\t\t\t");
              _builder.append(", line_number);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("if (line_number\'length > 0 and line_number(1) /= \'/\') then");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t\t");
              _builder.append("read(line_number, input_bit);");
              _builder.newLine();
              {
                boolean _or_1 = false;
                Type _type_5 = port_2.getType();
                boolean _isBool_1 = _type_5.isBool();
                if (_isBool_1) {
                  _or_1 = true;
                } else {
                  Type _type_6 = port_2.getType();
                  int _sizeInBits_3 = _type_6.getSizeInBits();
                  boolean _equals_1 = (_sizeInBits_3 == 1);
                  _or_1 = _equals_1;
                }
                if (_or_1) {
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("if (input_bit = 1) then");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("\t");
                  CharSequence _portName_26 = this.getPortName(port_2);
                  _builder.append(_portName_26, "\t\t\t\t\t\t\t\t");
                  _builder.append("_dout <= \'1\';");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("else");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("\t");
                  CharSequence _portName_27 = this.getPortName(port_2);
                  _builder.append(_portName_27, "\t\t\t\t\t\t\t\t");
                  _builder.append("_dout <= \'0\';");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t\t");
                  _builder.append("end if;");
                  _builder.newLine();
                } else {
                  {
                    Type _type_7 = port_2.getType();
                    boolean _isUint_1 = _type_7.isUint();
                    if (_isUint_1) {
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t\t");
                      CharSequence _portName_28 = this.getPortName(port_2);
                      _builder.append(_portName_28, "\t\t\t\t\t\t\t");
                      _builder.append("_dout <= std_logic_vector(to_unsigned(input_bit, ");
                      Type _type_8 = port_2.getType();
                      int _sizeInBits_4 = _type_8.getSizeInBits();
                      _builder.append(_sizeInBits_4, "\t\t\t\t\t\t\t");
                      _builder.append("));");
                      _builder.newLineIfNotEmpty();
                    } else {
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t\t");
                      CharSequence _portName_29 = this.getPortName(port_2);
                      _builder.append(_portName_29, "\t\t\t\t\t\t\t");
                      _builder.append("_dout <= std_logic_vector(to_signed(input_bit, ");
                      Type _type_9 = port_2.getType();
                      int _sizeInBits_5 = _type_9.getSizeInBits();
                      _builder.append(_sizeInBits_5, "\t\t\t\t\t\t\t");
                      _builder.append("));");
                      _builder.newLineIfNotEmpty();
                    }
                  }
                }
              }
              _builder.append("\t\t");
              _builder.append("\t\t\t\t\t");
              CharSequence _portName_30 = this.getPortName(port_2);
              _builder.append(_portName_30, "\t\t\t\t\t\t\t");
              _builder.append("_empty_n <= \'1\';");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("end if;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("elsif (endfile (sim_file_");
              _builder.append(name, "\t\t\t\t\t");
              _builder.append("_");
              CharSequence _portName_31 = this.getPortName(port_2);
              _builder.append(_portName_31, "\t\t\t\t\t");
              _builder.append(")) then");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              CharSequence _portName_32 = this.getPortName(port_2);
              _builder.append(_portName_32, "\t\t\t\t\t\t");
              _builder.append("_empty_n <= \'0\';");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("end if;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t");
              _builder.append("when others => null;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t");
              _builder.append("end case;");
              _builder.newLine();
            }
          }
        }
      }
      _builder.append("\t\t");
      _builder.append("end if;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("end process WaveGen_Proc_In;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      {
        boolean _isEmpty = outputPorts.isEmpty();
        boolean _not_1 = (!_isEmpty);
        if (_not_1) {
          _builder.append("\t");
          _builder.append("-- Output(s) waveform Generation");
          _builder.newLine();
        }
      }
      {
        boolean _hasElements_3 = false;
        for(final Port port_3 : outputPorts) {
          if (!_hasElements_3) {
            _hasElements_3 = true;
          } else {
            _builder.appendImmediate("\n", "\t");
          }
          {
            boolean _isNative_1 = port_3.isNative();
            boolean _not_2 = (!_isNative_1);
            if (_not_2) {
              _builder.append("\t");
              CharSequence _portName_33 = this.getPortName(port_3);
              _builder.append(_portName_33, "\t");
              _builder.append("_full_n <= \'1\';");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("WaveGen_Proc_Out : process (CLK)");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("variable Input_bit   : integer range 2147483647 downto - 2147483648;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("variable line_number : line;");
      _builder.newLine();
      {
        boolean _hasElements_4 = false;
        for(final Port port_out : outputPorts) {
          if (!_hasElements_4) {
            _hasElements_4 = true;
          } else {
            _builder.appendImmediate("\n", "\t\t");
          }
          _builder.append("\t\t");
          _builder.append("variable sequence_");
          CharSequence _portName_34 = this.getPortName(port_out);
          _builder.append(_portName_34, "\t\t");
          _builder.append(" : integer := 0;");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("\t");
      _builder.append("begin");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("if (rising_edge(CLK)) then");
      _builder.newLine();
      {
        boolean _hasElements_5 = false;
        for(final Port port_4 : outputPorts) {
          if (!_hasElements_5) {
            _hasElements_5 = true;
          } else {
            _builder.appendImmediate("\n", "\t\t");
          }
          {
            boolean _isNative_2 = port_4.isNative();
            boolean _not_3 = (!_isNative_2);
            if (_not_3) {
              _builder.append("\t\t");
              _builder.append("-- Output port: ");
              CharSequence _portName_35 = this.getPortName(port_4);
              _builder.append(_portName_35, "\t\t");
              _builder.append(" Waveform Generation");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t");
              _builder.append("if (not endfile (sim_file_");
              _builder.append(name, "\t\t\t");
              _builder.append("_");
              CharSequence _portName_36 = this.getPortName(port_4);
              _builder.append(_portName_36, "\t\t\t");
              _builder.append(") and ");
              CharSequence _portName_37 = this.getPortName(port_4);
              _builder.append(_portName_37, "\t\t\t");
              _builder.append("_write = \'1\') then");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t");
              _builder.append("readline(sim_file_");
              _builder.append(name, "\t\t\t\t");
              _builder.append("_");
              CharSequence _portName_38 = this.getPortName(port_4);
              _builder.append(_portName_38, "\t\t\t\t");
              _builder.append(", line_number);");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("if (line_number\'length > 0 and line_number(1) /= \'/\') then");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t\t\t\t");
              _builder.append("read(line_number, input_bit);");
              _builder.newLine();
              {
                boolean _or_2 = false;
                Type _type_10 = port_4.getType();
                boolean _isBool_2 = _type_10.isBool();
                if (_isBool_2) {
                  _or_2 = true;
                } else {
                  Type _type_11 = port_4.getType();
                  int _sizeInBits_6 = _type_11.getSizeInBits();
                  boolean _equals_2 = (_sizeInBits_6 == 1);
                  _or_2 = _equals_2;
                }
                if (_or_2) {
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("if (input_bit = 1) then");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("assert (");
                  CharSequence _portName_39 = this.getPortName(port_4);
                  _builder.append(_portName_39, "\t\t\t\t\t\t\t");
                  _builder.append("_din = \'1\')");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("report \"on port ");
                  CharSequence _portName_40 = this.getPortName(port_4);
                  _builder.append(_portName_40, "\t\t\t\t\t\t\t");
                  _builder.append(" incorrect value computed : \'0\' instead of : \'1\' sequence \" & str(sequence_");
                  CharSequence _portName_41 = this.getPortName(port_4);
                  _builder.append(_portName_41, "\t\t\t\t\t\t\t");
                  _builder.append(")");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("severity failure;");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("assert (");
                  CharSequence _portName_42 = this.getPortName(port_4);
                  _builder.append(_portName_42, "\t\t\t\t\t\t\t");
                  _builder.append("_din = \'0\')");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("report \"on port ");
                  CharSequence _portName_43 = this.getPortName(port_4);
                  _builder.append(_portName_43, "\t\t\t\t\t\t\t");
                  _builder.append(" correct value computed : \'1\' instead of : \'1\' sequence \" & str(sequence_");
                  CharSequence _portName_44 = this.getPortName(port_4);
                  _builder.append(_portName_44, "\t\t\t\t\t\t\t");
                  _builder.append(")");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("severity note;");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("else");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("assert (");
                  CharSequence _portName_45 = this.getPortName(port_4);
                  _builder.append(_portName_45, "\t\t\t\t\t\t\t");
                  _builder.append("_din = \'0\')");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("report \"on port ");
                  CharSequence _portName_46 = this.getPortName(port_4);
                  _builder.append(_portName_46, "\t\t\t\t\t\t\t");
                  _builder.append(" incorrect value computed : \'1\' instead of : \'0\' sequence \" & str(sequence_");
                  CharSequence _portName_47 = this.getPortName(port_4);
                  _builder.append(_portName_47, "\t\t\t\t\t\t\t");
                  _builder.append(")");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("severity failure;");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("assert (");
                  CharSequence _portName_48 = this.getPortName(port_4);
                  _builder.append(_portName_48, "\t\t\t\t\t\t\t");
                  _builder.append("_din = \'1\')");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("report \"on port ");
                  CharSequence _portName_49 = this.getPortName(port_4);
                  _builder.append(_portName_49, "\t\t\t\t\t\t\t");
                  _builder.append(" correct value computed : \'0\' instead of : \'0\' sequence \" & str(sequence_");
                  CharSequence _portName_50 = this.getPortName(port_4);
                  _builder.append(_portName_50, "\t\t\t\t\t\t\t");
                  _builder.append(")");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("\t");
                  _builder.append("severity note;");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("end if;");
                  _builder.newLine();
                  _builder.append("\t\t");
                  _builder.append("\t\t\t\t");
                  _builder.append("sequence_");
                  CharSequence _portName_51 = this.getPortName(port_4);
                  _builder.append(_portName_51, "\t\t\t\t\t\t");
                  _builder.append(" := sequence_");
                  CharSequence _portName_52 = this.getPortName(port_4);
                  _builder.append(_portName_52, "\t\t\t\t\t\t");
                  _builder.append(" + 1;");
                  _builder.newLineIfNotEmpty();
                } else {
                  Type _type_12 = port_4.getType();
                  boolean _isInt = _type_12.isInt();
                  if (_isInt) {
                    _builder.append("\t\t");
                    _builder.append("\t\t\t\t");
                    _builder.append("assert (");
                    CharSequence _portName_53 = this.getPortName(port_4);
                    _builder.append(_portName_53, "\t\t\t\t\t\t");
                    _builder.append("_din  = std_logic_vector(to_signed(input_bit, ");
                    Type _type_13 = port_4.getType();
                    int _sizeInBits_7 = _type_13.getSizeInBits();
                    _builder.append(_sizeInBits_7, "\t\t\t\t\t\t");
                    _builder.append(")))");
                    _builder.newLineIfNotEmpty();
                    _builder.append("\t\t");
                    _builder.append("\t\t\t\t");
                    _builder.append("report \"on port ");
                    CharSequence _portName_54 = this.getPortName(port_4);
                    _builder.append(_portName_54, "\t\t\t\t\t\t");
                    _builder.append(" incorrect value computed : \" & str(to_integer(signed(");
                    CharSequence _portName_55 = this.getPortName(port_4);
                    _builder.append(_portName_55, "\t\t\t\t\t\t");
                    _builder.append("_din))) & \" instead of : \" & str(input_bit) & \" sequence \" & str(sequence_");
                    CharSequence _portName_56 = this.getPortName(port_4);
                    _builder.append(_portName_56, "\t\t\t\t\t\t");
                    _builder.append(")");
                    _builder.newLineIfNotEmpty();
                    _builder.append("\t\t");
                    _builder.append("\t\t\t\t");
                    _builder.append("severity failure;");
                    _builder.newLine();
                    _builder.append("\t\t");
                    _builder.append("\t\t\t\t");
                    _builder.newLine();
                    _builder.append("\t\t");
                    _builder.append("\t\t\t\t");
                    _builder.append("assert (");
                    CharSequence _portName_57 = this.getPortName(port_4);
                    _builder.append(_portName_57, "\t\t\t\t\t\t");
                    _builder.append("_din /= std_logic_vector(to_signed(input_bit, ");
                    Type _type_14 = port_4.getType();
                    int _sizeInBits_8 = _type_14.getSizeInBits();
                    _builder.append(_sizeInBits_8, "\t\t\t\t\t\t");
                    _builder.append(")))");
                    _builder.newLineIfNotEmpty();
                    _builder.append("\t\t");
                    _builder.append("\t\t\t\t");
                    _builder.append("report \"on port ");
                    CharSequence _portName_58 = this.getPortName(port_4);
                    _builder.append(_portName_58, "\t\t\t\t\t\t");
                    _builder.append(" correct value computed : \" & str(to_integer(signed(");
                    CharSequence _portName_59 = this.getPortName(port_4);
                    _builder.append(_portName_59, "\t\t\t\t\t\t");
                    _builder.append("_din))) & \" equals : \" & str(input_bit) & \" sequence \" & str(sequence_");
                    CharSequence _portName_60 = this.getPortName(port_4);
                    _builder.append(_portName_60, "\t\t\t\t\t\t");
                    _builder.append(")");
                    _builder.newLineIfNotEmpty();
                    _builder.append("\t\t");
                    _builder.append("\t\t\t\t");
                    _builder.append("severity note;");
                    _builder.newLine();
                    _builder.append("\t\t");
                    _builder.append("\t\t\t\t");
                    _builder.append("sequence_");
                    CharSequence _portName_61 = this.getPortName(port_4);
                    _builder.append(_portName_61, "\t\t\t\t\t\t");
                    _builder.append(" := sequence_");
                    CharSequence _portName_62 = this.getPortName(port_4);
                    _builder.append(_portName_62, "\t\t\t\t\t\t");
                    _builder.append(" + 1;");
                    _builder.newLineIfNotEmpty();
                  } else {
                    Type _type_15 = port_4.getType();
                    boolean _isUint_2 = _type_15.isUint();
                    if (_isUint_2) {
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t");
                      _builder.append("assert (");
                      CharSequence _portName_63 = this.getPortName(port_4);
                      _builder.append(_portName_63, "\t\t\t\t\t\t");
                      _builder.append("_din  = std_logic_vector(to_unsigned(input_bit, ");
                      Type _type_16 = port_4.getType();
                      int _sizeInBits_9 = _type_16.getSizeInBits();
                      _builder.append(_sizeInBits_9, "\t\t\t\t\t\t");
                      _builder.append(")))");
                      _builder.newLineIfNotEmpty();
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t");
                      _builder.append("report \"on port ");
                      CharSequence _portName_64 = this.getPortName(port_4);
                      _builder.append(_portName_64, "\t\t\t\t\t\t");
                      _builder.append(" incorrect value computed : \" & str(to_integer(unsigned(");
                      CharSequence _portName_65 = this.getPortName(port_4);
                      _builder.append(_portName_65, "\t\t\t\t\t\t");
                      _builder.append("_din))) & \" instead of : \" & str(input_bit) & \" sequence \" & str(sequence_");
                      CharSequence _portName_66 = this.getPortName(port_4);
                      _builder.append(_portName_66, "\t\t\t\t\t\t");
                      _builder.append(")");
                      _builder.newLineIfNotEmpty();
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t");
                      _builder.append("severity failure;");
                      _builder.newLine();
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t");
                      _builder.newLine();
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t");
                      _builder.append("assert (");
                      CharSequence _portName_67 = this.getPortName(port_4);
                      _builder.append(_portName_67, "\t\t\t\t\t\t");
                      _builder.append("_din /= std_logic_vector(to_unsigned(input_bit, ");
                      Type _type_17 = port_4.getType();
                      int _sizeInBits_10 = _type_17.getSizeInBits();
                      _builder.append(_sizeInBits_10, "\t\t\t\t\t\t");
                      _builder.append(")))");
                      _builder.newLineIfNotEmpty();
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t");
                      _builder.append("report \"on port ");
                      CharSequence _portName_68 = this.getPortName(port_4);
                      _builder.append(_portName_68, "\t\t\t\t\t\t");
                      _builder.append(" correct value computed : \" & str(to_integer(unsigned(");
                      CharSequence _portName_69 = this.getPortName(port_4);
                      _builder.append(_portName_69, "\t\t\t\t\t\t");
                      _builder.append("_din))) & \" equals : \" & str(input_bit) & \" sequence \" & str(sequence_");
                      CharSequence _portName_70 = this.getPortName(port_4);
                      _builder.append(_portName_70, "\t\t\t\t\t\t");
                      _builder.append(")");
                      _builder.newLineIfNotEmpty();
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t");
                      _builder.append("severity note;");
                      _builder.newLine();
                      _builder.append("\t\t");
                      _builder.append("\t\t\t\t");
                      _builder.append("sequence_");
                      CharSequence _portName_71 = this.getPortName(port_4);
                      _builder.append(_portName_71, "\t\t\t\t\t\t");
                      _builder.append(" := sequence_");
                      CharSequence _portName_72 = this.getPortName(port_4);
                      _builder.append(_portName_72, "\t\t\t\t\t\t");
                      _builder.append(" + 1;");
                      _builder.newLineIfNotEmpty();
                    }
                  }
                }
              }
              _builder.append("\t\t");
              _builder.append("\t\t\t");
              _builder.append("end if;");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("\t");
              _builder.append("end if;");
              _builder.newLine();
            }
          }
        }
      }
      _builder.append("\t\t");
      _builder.append("end if;\t\t\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("end process WaveGen_Proc_Out;");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence getContent() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader();
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    CharSequence _addLibraries = this.addLibraries();
    _builder.append(_addLibraries, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    CharSequence _addEntity = this.addEntity(this.vertex);
    _builder.append(_addEntity, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    CharSequence _addArchitecture = this.addArchitecture(this.vertex);
    _builder.append(_addArchitecture, "");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence getPortName(final Port port) {
    CharSequence _xblockexpression = null;
    {
      String name = port.getName();
      boolean _or = false;
      boolean _or_1 = false;
      boolean _or_2 = false;
      boolean _or_3 = false;
      boolean _or_4 = false;
      boolean _equals = name.equals("In");
      if (_equals) {
        _or_4 = true;
      } else {
        boolean _equals_1 = name.equals("Out");
        _or_4 = _equals_1;
      }
      if (_or_4) {
        _or_3 = true;
      } else {
        boolean _equals_2 = name.equals("IN");
        _or_3 = _equals_2;
      }
      if (_or_3) {
        _or_2 = true;
      } else {
        boolean _equals_3 = name.equals("OUT");
        _or_2 = _equals_3;
      }
      if (_or_2) {
        _or_1 = true;
      } else {
        boolean _equals_4 = name.equals("Bit");
        _or_1 = _equals_4;
      }
      if (_or_1) {
        _or = true;
      } else {
        boolean _equals_5 = name.equals("BIT");
        _or = _equals_5;
      }
      if (_or) {
        String _name = port.getName();
        String _plus = (_name + "_r");
        name = _plus;
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append(name, "");
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public void computeNetworkClockDomains(final Network network, final Map<String, String> clockDomains) {
    HashMap<String, Integer> _hashMap = new HashMap<String, Integer>();
    this.clockDomainsIndex = _hashMap;
    int clkIndex = 0;
    this.clockDomainsIndex.put(this.DEFAULT_CLOCK_DOMAIN, Integer.valueOf(clkIndex));
    clkIndex = (clkIndex + 1);
    Collection<String> _values = clockDomains.values();
    for (final String string : _values) {
      boolean _and = false;
      boolean _isEmpty = string.isEmpty();
      boolean _not = (!_isEmpty);
      if (!_not) {
        _and = false;
      } else {
        boolean _containsKey = this.clockDomainsIndex.containsKey(string);
        boolean _not_1 = (!_containsKey);
        _and = _not_1;
      }
      if (_and) {
        this.clockDomainsIndex.put(string, Integer.valueOf(clkIndex));
        clkIndex = (clkIndex + 1);
      }
    }
  }
}
