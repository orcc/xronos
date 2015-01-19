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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Connection;
import net.sf.orcc.df.Entity;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.Port;
import net.sf.orcc.graph.Edge;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.IrUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.xronos.systemc.SystemCTemplate;

/**
 * SystemC Network Printer
 * 
 * @author Endri Bezati
 */
@SuppressWarnings("all")
public class NetworkPrinter extends SystemCTemplate {
  protected Network network;
  
  protected String name;
  
  protected Map<Connection, String> queueNames;
  
  protected Map<Connection, Type> queueTypes;
  
  protected Integer defaultQueueSize;
  
  protected Boolean addScope = Boolean.valueOf(true);
  
  public void setNetwork(final Network network) {
    this.network = network;
    String _simpleName = network.getSimpleName();
    this.name = _simpleName;
    this.retrieveQueueNames();
  }
  
  public void setOptions(final Map<String, Object> options) {
    super.setOptions(options);
    boolean _containsKey = options.containsKey("net.sf.orcc.fifoSize");
    if (_containsKey) {
      Object _get = options.get("net.sf.orcc.fifoSize");
      this.defaultQueueSize = ((Integer) _get);
    }
  }
  
  public CharSequence getContent() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader();
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    CharSequence _networkContent = this.getNetworkContent();
    _builder.append(_networkContent, "");
    _builder.newLineIfNotEmpty();
    return _builder;
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
      _builder.append("// Xronos SystemC, Network Generator");
      _builder.newLine();
      _builder.append("// Top level Network: ");
      String _simpleName = this.network.getSimpleName();
      _builder.append(_simpleName, "");
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
  
  public CharSequence getNetworkContent() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("#ifndef SC_");
    _builder.append(this.name, "");
    _builder.append("_H");
    _builder.newLineIfNotEmpty();
    _builder.append("#define SC_");
    _builder.append(this.name, "");
    _builder.append("_H");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("#include \"systemc.h\"");
    _builder.newLine();
    _builder.newLine();
    {
      EList<Vertex> _children = this.network.getChildren();
      for(final Vertex child : _children) {
        _builder.append("#include \"");
        String _label = child.getLabel();
        _builder.append(_label, "");
        _builder.append(".h\" ");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.append("SC_MODULE(");
    _builder.append(this.name, "");
    _builder.append("){");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Control Ports");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool>   clk;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool>   reset;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_in<bool>   start;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("sc_out<bool>  done;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Network Input Ports");
    _builder.newLine();
    {
      EList<Port> _inputs = this.network.getInputs();
      for(final Port port : _inputs) {
        _builder.append("\t");
        CharSequence _portDeclaration = this.getPortDeclaration("in", port);
        _builder.append(_portDeclaration, "\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Network Output Ports");
    _builder.newLine();
    {
      EList<Port> _outputs = this.network.getOutputs();
      for(final Port port_1 : _outputs) {
        _builder.append("\t");
        CharSequence _portDeclaration_1 = this.getPortDeclaration("out", port_1);
        _builder.append(_portDeclaration_1, "\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Queues");
    _builder.newLine();
    _builder.append("\t");
    CharSequence _queuesDeclarationContent = this.getQueuesDeclarationContent();
    _builder.append(_queuesDeclarationContent, "\t");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    {
      EList<Var> _parameters = this.network.getParameters();
      boolean _isEmpty = _parameters.isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        _builder.append("\t");
        _builder.append("// -- Network Parameters ");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("// -- TBD");
        _builder.newLine();
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Actors Start / Done signals");
    _builder.newLine();
    {
      EList<Vertex> _children_1 = this.network.getChildren();
      for(final Vertex child_1 : _children_1) {
        _builder.append("\t");
        _builder.append("sc_signal<bool> done_");
        String _label_1 = child_1.getLabel();
        _builder.append(_label_1, "\t");
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Actors");
    _builder.newLine();
    {
      EList<Vertex> _children_2 = this.network.getChildren();
      for(final Vertex child_2 : _children_2) {
        _builder.append("\t");
        String _label_2 = child_2.getLabel();
        _builder.append(_label_2, "\t");
        _builder.append(" i_");
        String _label_3 = child_2.getLabel();
        _builder.append(_label_3, "\t");
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    {
      boolean _or = false;
      EList<Port> _inputs_1 = this.network.getInputs();
      boolean _isEmpty_1 = _inputs_1.isEmpty();
      boolean _not_1 = (!_isEmpty_1);
      if (_not_1) {
        _or = true;
      } else {
        EList<Port> _outputs_1 = this.network.getOutputs();
        boolean _isEmpty_2 = _outputs_1.isEmpty();
        boolean _not_2 = (!_isEmpty_2);
        _or = _not_2;
      }
      if (_or) {
        _builder.append("\t");
        _builder.append("// Queue processes");
        _builder.newLine();
      }
    }
    {
      EList<Port> _inputs_2 = this.network.getInputs();
      boolean _isEmpty_3 = _inputs_2.isEmpty();
      boolean _not_3 = (!_isEmpty_3);
      if (_not_3) {
        {
          EList<Port> _inputs_3 = this.network.getInputs();
          boolean _hasElements = false;
          for(final Port port_2 : _inputs_3) {
            if (!_hasElements) {
              _hasElements = true;
            } else {
              _builder.appendImmediate("\n", "\t");
            }
            _builder.append("\t");
            _builder.append("void port_");
            String _name = port_2.getName();
            _builder.append(_name, "\t");
            _builder.append("_reader();");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.newLine();
    {
      EList<Port> _outputs_2 = this.network.getOutputs();
      boolean _isEmpty_4 = _outputs_2.isEmpty();
      boolean _not_4 = (!_isEmpty_4);
      if (_not_4) {
        {
          EList<Port> _outputs_3 = this.network.getOutputs();
          boolean _hasElements_1 = false;
          for(final Port port_3 : _outputs_3) {
            if (!_hasElements_1) {
              _hasElements_1 = true;
            } else {
              _builder.appendImmediate("\n", "\t");
            }
            _builder.append("\t");
            _builder.append("void port_");
            String _name_1 = port_3.getName();
            _builder.append(_name_1, "\t");
            _builder.append("_writer();");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Constructor");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("SC_CTOR(");
    _builder.append(this.name, "\t");
    _builder.append(")");
    _builder.newLineIfNotEmpty();
    _builder.append("\t\t");
    _builder.append(":clk(\"clk\")");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append(",reset(\"reset\")");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append(",start(\"start\")");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append(",done(\"done\")");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("// -- Actors");
    _builder.newLine();
    {
      EList<Vertex> _children_3 = this.network.getChildren();
      for(final Vertex child_3 : _children_3) {
        _builder.append("\t\t");
        _builder.append(",i_");
        String _label_4 = child_3.getLabel();
        _builder.append(_label_4, "\t\t");
        _builder.append("(\"i_");
        String _label_5 = child_3.getLabel();
        _builder.append(_label_5, "\t\t");
        _builder.append("\")");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t\t");
    _builder.append("// -- Queues");
    _builder.newLine();
    {
      EList<Connection> _connections = this.network.getConnections();
      for(final Connection connection : _connections) {
        _builder.append("\t\t");
        _builder.append(",");
        String _get = this.queueNames.get(connection);
        _builder.append(_get, "\t\t");
        _builder.append("(\"");
        String _get_1 = this.queueNames.get(connection);
        _builder.append(_get_1, "\t\t");
        _builder.append("\", ");
        {
          Integer _size = connection.getSize();
          boolean _equals = Objects.equal(_size, null);
          if (_equals) {
            _builder.append(this.defaultQueueSize, "\t\t");
          } else {
            Integer _size_1 = connection.getSize();
            _builder.append(_size_1, "\t\t");
          }
        }
        _builder.append(")");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.append("{");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("// -- Connnections");
    _builder.newLine();
    {
      EList<Vertex> _children_4 = this.network.getChildren();
      boolean _hasElements_2 = false;
      for(final Vertex child_4 : _children_4) {
        if (!_hasElements_2) {
          _hasElements_2 = true;
        } else {
          _builder.appendImmediate("\n", "\t\t");
        }
        _builder.append("\t\t");
        CharSequence _childConnections = this.getChildConnections(((Actor) child_4));
        _builder.append(_childConnections, "\t\t");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      boolean _or_1 = false;
      EList<Port> _inputs_4 = this.network.getInputs();
      boolean _isEmpty_5 = _inputs_4.isEmpty();
      boolean _not_5 = (!_isEmpty_5);
      if (_not_5) {
        _or_1 = true;
      } else {
        EList<Port> _outputs_4 = this.network.getOutputs();
        boolean _isEmpty_6 = _outputs_4.isEmpty();
        boolean _not_6 = (!_isEmpty_6);
        _or_1 = _not_6;
      }
      if (_or_1) {
        _builder.append("\t\t");
        _builder.append("// -- Port Readers/Writers Process Registration");
        _builder.newLine();
        {
          EList<Port> _inputs_5 = this.network.getInputs();
          for(final Port port_4 : _inputs_5) {
            _builder.append("\t\t");
            _builder.append("SC_CTHREAD(port_");
            String _name_2 = port_4.getName();
            _builder.append(_name_2, "\t\t");
            _builder.append("_reader, clk.pos());");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t");
            _builder.append("reset_signal_is(reset,true);");
            _builder.newLine();
          }
        }
        {
          EList<Port> _outputs_5 = this.network.getOutputs();
          for(final Port port_5 : _outputs_5) {
            _builder.append("\t\t");
            _builder.append("SC_CTHREAD(port_");
            String _name_3 = port_5.getName();
            _builder.append(_name_3, "\t\t");
            _builder.append("_writer, clk.pos());");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t");
            _builder.append("reset_signal_is(reset,true);");
            _builder.newLine();
          }
        }
      }
    }
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("};");
    _builder.newLine();
    _builder.newLine();
    _builder.newLine();
    _builder.append("#endif //SC_");
    _builder.append(this.name, "");
    _builder.append("_H");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence getNetworkContentSource() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _header = this.getHeader();
    _builder.append(_header, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("#include \"");
    _builder.append(this.name, "");
    _builder.append(".h\"");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    {
      boolean _or = false;
      EList<Port> _inputs = this.network.getInputs();
      boolean _isEmpty = _inputs.isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        _or = true;
      } else {
        EList<Port> _outputs = this.network.getOutputs();
        boolean _isEmpty_1 = _outputs.isEmpty();
        boolean _not_1 = (!_isEmpty_1);
        _or = _not_1;
      }
      if (_or) {
        _builder.append("// -- Queue Readers / Writers Processes");
        _builder.newLine();
      }
    }
    {
      EList<Port> _inputs_1 = this.network.getInputs();
      boolean _isEmpty_2 = _inputs_1.isEmpty();
      boolean _not_2 = (!_isEmpty_2);
      if (_not_2) {
        CharSequence _inputQueueReaders = this.getInputQueueReaders();
        _builder.append(_inputQueueReaders, "");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    {
      EList<Port> _outputs_1 = this.network.getOutputs();
      boolean _isEmpty_3 = _outputs_1.isEmpty();
      boolean _not_3 = (!_isEmpty_3);
      if (_not_3) {
        CharSequence _outputQueueWriters = this.getOutputQueueWriters();
        _builder.append(_outputQueueWriters, "");
        _builder.newLineIfNotEmpty();
      }
    }
    return _builder;
  }
  
  public CharSequence getPortDeclaration(final String direction, final Port port) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("sc_fifo_");
    _builder.append(direction, "");
    _builder.append("< ");
    Type _type = port.getType();
    CharSequence _doSwitch = this.doSwitch(_type);
    _builder.append(_doSwitch, "");
    _builder.append(" > ");
    String _name = port.getName();
    _builder.append(_name, "");
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence getQueuesDeclarationContent() {
    StringConcatenation _builder = new StringConcatenation();
    {
      EList<Connection> _connections = this.network.getConnections();
      for(final Connection connection : _connections) {
        _builder.append("sc_fifo< ");
        Type _get = this.queueTypes.get(connection);
        CharSequence _doSwitch = this.doSwitch(_get);
        _builder.append(_doSwitch, "");
        _builder.append(" > ");
        String _get_1 = this.queueNames.get(connection);
        _builder.append(_get_1, "");
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    return _builder;
  }
  
  public CharSequence getInputQueueReaders() {
    StringConcatenation _builder = new StringConcatenation();
    {
      EList<Port> _inputs = this.network.getInputs();
      boolean _hasElements = false;
      for(final Port port : _inputs) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate("\n", "");
        }
        _builder.append("void ");
        {
          if ((this.addScope).booleanValue()) {
            _builder.append(this.name, "");
            _builder.append("::");
          }
        }
        _builder.append("port_");
        String _name = port.getName();
        _builder.append(_name, "");
        _builder.append("_reader(){");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("do {");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("wait();");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("} while (!start.read());");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("while (true) {");
        _builder.newLine();
        _builder.append("\t\t");
        Type _type = port.getType();
        CharSequence _doSwitch = this.doSwitch(_type);
        _builder.append(_doSwitch, "\t\t");
        _builder.append(" data = ");
        String _name_1 = port.getName();
        _builder.append(_name_1, "\t\t");
        _builder.append(".read();");
        _builder.newLineIfNotEmpty();
        {
          EList<Connection> _connections = this.network.getConnections();
          for(final Connection connection : _connections) {
            {
              Vertex _source = connection.getSource();
              boolean _equals = _source.equals(port);
              if (_equals) {
                _builder.append("\t\t");
                String _get = this.queueNames.get(connection);
                _builder.append(_get, "\t\t");
                _builder.append(".write(data);\t\t\t\t");
                _builder.newLineIfNotEmpty();
              }
            }
          }
        }
        _builder.append("\t\t");
        _builder.append("wait();");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("}");
        _builder.newLine();
        _builder.append("}");
        _builder.newLine();
      }
    }
    return _builder;
  }
  
  public CharSequence getOutputQueueWriters() {
    StringConcatenation _builder = new StringConcatenation();
    {
      EList<Port> _outputs = this.network.getOutputs();
      boolean _hasElements = false;
      for(final Port port : _outputs) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate("\n", "");
        }
        {
          EList<Connection> _connections = this.network.getConnections();
          for(final Connection connection : _connections) {
            {
              Vertex _target = connection.getTarget();
              boolean _equals = _target.equals(port);
              if (_equals) {
                _builder.append("void ");
                {
                  if ((this.addScope).booleanValue()) {
                    _builder.append(this.name, "");
                    _builder.append("::");
                  }
                }
                _builder.append("port_");
                String _name = port.getName();
                _builder.append(_name, "");
                _builder.append("_writer(){");
                _builder.newLineIfNotEmpty();
                _builder.append("\t");
                _builder.append("do {");
                _builder.newLine();
                _builder.append("\t\t");
                _builder.append("wait();");
                _builder.newLine();
                _builder.append("\t");
                _builder.append("} while (!start.read());");
                _builder.newLine();
                _builder.append("\t");
                _builder.append("while (true) {");
                _builder.newLine();
                _builder.append("\t\t");
                String _name_1 = port.getName();
                _builder.append(_name_1, "\t\t");
                _builder.append(".write(");
                String _get = this.queueNames.get(connection);
                _builder.append(_get, "\t\t");
                _builder.append(".read());");
                _builder.newLineIfNotEmpty();
                _builder.append("\t\t");
                _builder.append("wait();");
                _builder.newLine();
                _builder.append("\t");
                _builder.append("}");
                _builder.newLine();
                _builder.append("}");
                _builder.newLine();
              }
            }
          }
        }
      }
    }
    return _builder;
  }
  
  public CharSequence getChildConnections(final Actor child) {
    CharSequence _xblockexpression = null;
    {
      Entity _adapter = child.<Entity>getAdapter(Entity.class);
      Map<Port, List<Connection>> portConnection = _adapter.getOutgoingPortMap();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("i_");
      String _label = child.getLabel();
      _builder.append(_label, "");
      _builder.append(".clk(clk);");
      _builder.newLineIfNotEmpty();
      _builder.append("i_");
      String _label_1 = child.getLabel();
      _builder.append(_label_1, "");
      _builder.append(".reset(reset);");
      _builder.newLineIfNotEmpty();
      _builder.append("i_");
      String _label_2 = child.getLabel();
      _builder.append(_label_2, "");
      _builder.append(".start(start);");
      _builder.newLineIfNotEmpty();
      _builder.append("i_");
      String _label_3 = child.getLabel();
      _builder.append(_label_3, "");
      _builder.append(".done(done_");
      String _label_4 = child.getLabel();
      _builder.append(_label_4, "");
      _builder.append(");");
      _builder.newLineIfNotEmpty();
      {
        EList<Edge> _incoming = child.getIncoming();
        for(final Edge connection : _incoming) {
          _builder.append("i_");
          String _label_5 = child.getLabel();
          _builder.append(_label_5, "");
          _builder.append(".");
          Port _targetPort = ((Connection) connection).getTargetPort();
          String _name = _targetPort.getName();
          _builder.append(_name, "");
          _builder.append("(");
          String _get = this.queueNames.get(connection);
          _builder.append(_get, "");
          _builder.append(");");
          _builder.newLineIfNotEmpty();
        }
      }
      {
        EList<Port> _outputs = child.getOutputs();
        for(final Port port : _outputs) {
          {
            List<Connection> _get_1 = portConnection.get(port);
            int _size = _get_1.size();
            boolean _greaterThan = (_size > 1);
            if (_greaterThan) {
              {
                List<Connection> _get_2 = portConnection.get(port);
                for(final Connection connection_1 : _get_2) {
                  {
                    Vertex _target = connection_1.getTarget();
                    if ((_target instanceof Actor)) {
                      _builder.append("i_");
                      String _label_6 = child.getLabel();
                      _builder.append(_label_6, "");
                      _builder.append(".");
                      String _name_1 = port.getName();
                      _builder.append(_name_1, "");
                      _builder.append("_f_");
                      Vertex _target_1 = connection_1.getTarget();
                      String _name_2 = ((Actor) _target_1).getName();
                      _builder.append(_name_2, "");
                      _builder.append("(");
                      String _get_3 = this.queueNames.get(connection_1);
                      _builder.append(_get_3, "");
                      _builder.append(");");
                      _builder.newLineIfNotEmpty();
                    } else {
                      _builder.append("i_");
                      String _label_7 = child.getLabel();
                      _builder.append(_label_7, "");
                      _builder.append(".");
                      String _name_3 = port.getName();
                      _builder.append(_name_3, "");
                      _builder.append("_f_");
                      Vertex _target_2 = connection_1.getTarget();
                      String _name_4 = ((Port) _target_2).getName();
                      _builder.append(_name_4, "");
                      _builder.append("(");
                      String _get_4 = this.queueNames.get(connection_1);
                      _builder.append(_get_4, "");
                      _builder.append(");");
                      _builder.newLineIfNotEmpty();
                    }
                  }
                }
              }
            } else {
              _builder.append("i_");
              String _label_8 = child.getLabel();
              _builder.append(_label_8, "");
              _builder.append(".");
              String _name_5 = port.getName();
              _builder.append(_name_5, "");
              _builder.append("(");
              List<Connection> _get_5 = portConnection.get(port);
              Connection _get_6 = _get_5.get(0);
              String _get_7 = this.queueNames.get(_get_6);
              _builder.append(_get_7, "");
              _builder.append(");");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public void retrieveQueueNames() {
    HashMap<Connection, String> _hashMap = new HashMap<Connection, String>();
    this.queueNames = _hashMap;
    HashMap<Connection, Type> _hashMap_1 = new HashMap<Connection, Type>();
    this.queueTypes = _hashMap_1;
    EList<Connection> _connections = this.network.getConnections();
    for (final Connection connection : _connections) {
      Vertex _source = connection.getSource();
      if ((_source instanceof Port)) {
        Vertex _target = connection.getTarget();
        if ((_target instanceof Actor)) {
          Vertex _source_1 = connection.getSource();
          String _name = ((Port) _source_1).getName();
          String _plus = ("q_" + _name);
          String _plus_1 = (_plus + "_");
          Vertex _target_1 = connection.getTarget();
          String _name_1 = ((Actor) _target_1).getName();
          String _plus_2 = (_plus_1 + _name_1);
          String _plus_3 = (_plus_2 + "_");
          Port _targetPort = connection.getTargetPort();
          String _name_2 = _targetPort.getName();
          String _plus_4 = (_plus_3 + _name_2);
          this.queueNames.put(connection, _plus_4);
          Vertex _source_2 = connection.getSource();
          Type _type = ((Port) _source_2).getType();
          Type _copy = IrUtil.<Type>copy(_type);
          this.queueTypes.put(connection, _copy);
        }
      } else {
        Vertex _source_3 = connection.getSource();
        if ((_source_3 instanceof Actor)) {
          Vertex _target_2 = connection.getTarget();
          if ((_target_2 instanceof Port)) {
            Vertex _source_4 = connection.getSource();
            String _name_3 = ((Actor) _source_4).getName();
            String _plus_5 = ("q_" + _name_3);
            String _plus_6 = (_plus_5 + "_");
            Port _sourcePort = connection.getSourcePort();
            String _name_4 = _sourcePort.getName();
            String _plus_7 = (_plus_6 + _name_4);
            String _plus_8 = (_plus_7 + "_");
            Vertex _target_3 = connection.getTarget();
            String _name_5 = ((Port) _target_3).getName();
            String _plus_9 = (_plus_8 + _name_5);
            this.queueNames.put(connection, _plus_9);
            Port _sourcePort_1 = connection.getSourcePort();
            Type _type_1 = ((Port) _sourcePort_1).getType();
            Type _copy_1 = IrUtil.<Type>copy(_type_1);
            this.queueTypes.put(connection, _copy_1);
          } else {
            Vertex _target_4 = connection.getTarget();
            if ((_target_4 instanceof Actor)) {
              Vertex _source_5 = connection.getSource();
              String _name_6 = ((Actor) _source_5).getName();
              String _plus_10 = ("q_" + _name_6);
              String _plus_11 = (_plus_10 + "_");
              Port _sourcePort_2 = connection.getSourcePort();
              String _name_7 = _sourcePort_2.getName();
              String _plus_12 = (_plus_11 + _name_7);
              String _plus_13 = (_plus_12 + "_");
              Vertex _target_5 = connection.getTarget();
              String _name_8 = ((Actor) _target_5).getName();
              String _plus_14 = (_plus_13 + _name_8);
              String _plus_15 = (_plus_14 + "_");
              Port _targetPort_1 = connection.getTargetPort();
              String _name_9 = _targetPort_1.getName();
              String _plus_16 = (_plus_15 + _name_9);
              this.queueNames.put(connection, _plus_16);
              Port _sourcePort_3 = connection.getSourcePort();
              Type _type_2 = ((Port) _sourcePort_3).getType();
              Type _copy_2 = IrUtil.<Type>copy(_type_2);
              this.queueTypes.put(connection, _copy_2);
            }
          }
        }
      }
    }
  }
}
