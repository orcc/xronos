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
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.orcc.backends.ir.BlockFor;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Connection;
import net.sf.orcc.df.Entity;
import net.sf.orcc.df.FSM;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.State;
import net.sf.orcc.df.Transition;
import net.sf.orcc.graph.Edge;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.Arg;
import net.sf.orcc.ir.ArgByRef;
import net.sf.orcc.ir.ArgByVal;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Param;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.ValueUtil;
import net.sf.orcc.util.Attribute;
import net.sf.orcc.util.util.EcoreHelper;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.ExclusiveRange;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.xronos.systemc.SystemCTemplate;

@SuppressWarnings("all")
public class InstancePrinter extends SystemCTemplate {
  protected Actor actor;
  
  protected Instance instance;
  
  protected String name;
  
  protected static Boolean actionAsProcess = Boolean.valueOf(false);
  
  protected static boolean addScope = true;
  
  private Map<Port, List<String>> fanoutPortConenction;
  
  public String setInstance(final Instance instance) {
    String _xblockexpression = null;
    {
      this.instance = instance;
      Actor _actor = instance.getActor();
      this.actor = _actor;
      String _simpleName = instance.getSimpleName();
      _xblockexpression = this.name = _simpleName;
    }
    return _xblockexpression;
  }
  
  public void setActor(final Actor actor) {
    this.actor = actor;
    String _simpleName = actor.getSimpleName();
    this.name = _simpleName;
    this.getFanoutPortNames();
  }
  
  public CharSequence getFileHeader() {
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
      _builder.append("// Xronos SystemC, Actor Generator");
      _builder.newLine();
      _builder.append("// Actor: ");
      _builder.append(this.name, "");
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
  
  public CharSequence getActorHeaderContent() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _fileHeader = this.getFileHeader();
    _builder.append(_fileHeader, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
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
    _builder.append("SC_MODULE(");
    _builder.append(this.name, "");
    _builder.append("){");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// --------------------------------------------------------------------------");
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
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Actor Input Ports");
    _builder.newLine();
    {
      EList<Port> _inputs = this.actor.getInputs();
      for(final Port port : _inputs) {
        _builder.append("\t");
        CharSequence _portDeclaration = this.getPortDeclaration("in", port);
        _builder.append(_portDeclaration, "\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Actor Output Ports");
    _builder.newLine();
    {
      EList<Port> _outputs = this.actor.getOutputs();
      for(final Port port_1 : _outputs) {
        _builder.append("\t");
        CharSequence _portDeclaration_1 = this.getPortDeclaration("out", port_1);
        _builder.append(_portDeclaration_1, "\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Scheduler States");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("enum state_t { // enumerate states");
    _builder.newLine();
    _builder.append("\t\t");
    {
      FSM _fsm = this.actor.getFsm();
      EList<State> _states = _fsm.getStates();
      boolean _hasElements = false;
      for(final State state : _states) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate(", ", "\t\t");
        }
        _builder.append("s_");
        String _label = state.getLabel();
        _builder.append(_label, "\t\t");
      }
    }
    {
      EList<Port> _inputs_1 = this.actor.getInputs();
      boolean _isEmpty = _inputs_1.isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        _builder.append(", s_READ");
      }
    }
    {
      EList<Port> _outputs_1 = this.actor.getOutputs();
      boolean _isEmpty_1 = _outputs_1.isEmpty();
      boolean _not_1 = (!_isEmpty_1);
      if (_not_1) {
        _builder.append(", s_WRITE");
      }
    }
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("};");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("state_t state, old_state;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    {
      EList<Var> _stateVars = this.actor.getStateVars();
      for(final Var stateVar : _stateVars) {
        _builder.append("\t");
        CharSequence _declareStateVar = this.declareStateVar(stateVar);
        _builder.append(_declareStateVar, "\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Constructor");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("SC_CTOR(");
    _builder.append(this.name, "\t");
    _builder.append(")");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append(":clk(\"clk\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append(",reset(\"reset\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append(",start(\"start\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append(",done(\"done\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("{");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("// -- Actions Scheduler Registration");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("SC_CTHREAD(scheduler, clk.pos());");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("reset_signal_is(reset, true);");
    _builder.newLine();
    {
      if ((InstancePrinter.actionAsProcess).booleanValue()) {
        _builder.append("\t\t");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("// -- Actions Registration");
        _builder.newLine();
        {
          EList<Action> _actions = this.actor.getActions();
          boolean _hasElements_1 = false;
          for(final Action action : _actions) {
            if (!_hasElements_1) {
              _hasElements_1 = true;
            } else {
              _builder.appendImmediate("\n", "\t\t\t");
            }
            _builder.append("\t\t");
            _builder.append("\t");
            _builder.append("SC_CTHREAD(");
            Procedure _body = action.getBody();
            String _name = _body.getName();
            _builder.append(_name, "\t\t\t");
            _builder.append(", clk.pos());");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t");
            _builder.append("\t");
            _builder.append("reset_signal_is(reset, true);");
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
    {
      EList<Procedure> _procs = this.actor.getProcs();
      boolean _isEmpty_2 = _procs.isEmpty();
      boolean _not_2 = (!_isEmpty_2);
      if (_not_2) {
        _builder.append("\t");
        _builder.append("// --------------------------------------------------------------------------");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("// -- Procedure / Functions");
        _builder.newLine();
        {
          EList<Procedure> _procs_1 = this.actor.getProcs();
          for(final Procedure procedure : _procs_1) {
            _builder.append("\t");
            CharSequence _declare = this.declare(procedure);
            _builder.append(_declare, "\t");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Actions Body");
    _builder.newLine();
    {
      EList<Action> _actions_1 = this.actor.getActions();
      boolean _hasElements_2 = false;
      for(final Action action_1 : _actions_1) {
        if (!_hasElements_2) {
          _hasElements_2 = true;
        } else {
          _builder.appendImmediate("\n", "\t");
        }
        {
          Procedure _body_1 = action_1.getBody();
          EList<Block> _blocks = _body_1.getBlocks();
          boolean _isEmpty_3 = _blocks.isEmpty();
          boolean _not_3 = (!_isEmpty_3);
          if (_not_3) {
            _builder.append("\t");
            Procedure _body_2 = action_1.getBody();
            CharSequence _declare_1 = this.declare(_body_2);
            _builder.append(_declare_1, "\t");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Actions isSchedulable");
    _builder.newLine();
    {
      EList<Action> _actions_2 = this.actor.getActions();
      boolean _hasElements_3 = false;
      for(final Action action_2 : _actions_2) {
        if (!_hasElements_3) {
          _hasElements_3 = true;
        } else {
          _builder.appendImmediate("\n", "\t");
        }
        {
          Procedure _body_3 = action_2.getBody();
          EList<Block> _blocks_1 = _body_3.getBlocks();
          boolean _isEmpty_4 = _blocks_1.isEmpty();
          boolean _not_4 = (!_isEmpty_4);
          if (_not_4) {
            _builder.append("\t");
            Procedure _scheduler = action_2.getScheduler();
            CharSequence _declare_2 = this.declare(_scheduler);
            _builder.append(_declare_2, "\t");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Initialize Members");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void intializeMembers();");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Action(s) Scheduler");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void scheduler();");
    _builder.newLine();
    _builder.newLine();
    _builder.append("};");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#endif //SC_");
    _builder.append(this.name, "");
    _builder.append("_H");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence getActorSourceContent() {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _fileHeader = this.getFileHeader();
    _builder.append(_fileHeader, "");
    _builder.newLineIfNotEmpty();
    _builder.append("#include \"");
    _builder.append(this.name, "");
    _builder.append(".h\"");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    {
      EList<Procedure> _procs = this.actor.getProcs();
      boolean _isEmpty = _procs.isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        _builder.append("// --------------------------------------------------------------------------");
        _builder.newLine();
        _builder.append("// -- Procedure / Functions");
        _builder.newLine();
        {
          EList<Procedure> _procs_1 = this.actor.getProcs();
          for(final Procedure procedure : _procs_1) {
            CharSequence _procedureContent = this.getProcedureContent(procedure);
            _builder.append(_procedureContent, "");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.newLine();
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("// -- Actions Body");
    _builder.newLine();
    {
      EList<Action> _actions = this.actor.getActions();
      boolean _hasElements = false;
      for(final Action action : _actions) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate("\n", "");
        }
        {
          if ((InstancePrinter.actionAsProcess).booleanValue()) {
            {
              Procedure _body = action.getBody();
              EList<Block> _blocks = _body.getBlocks();
              boolean _isEmpty_1 = _blocks.isEmpty();
              boolean _not_1 = (!_isEmpty_1);
              if (_not_1) {
                Procedure _body_1 = action.getBody();
                CharSequence _actionBodyContentAsProcess = this.getActionBodyContentAsProcess(_body_1);
                _builder.append(_actionBodyContentAsProcess, "");
                _builder.newLineIfNotEmpty();
              }
            }
          } else {
            Procedure _body_2 = action.getBody();
            CharSequence _actionBodyContent = this.getActionBodyContent(_body_2);
            _builder.append(_actionBodyContent, "");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.newLine();
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("// -- Actions isSchedulable");
    _builder.newLine();
    {
      EList<Action> _actions_1 = this.actor.getActions();
      boolean _hasElements_1 = false;
      for(final Action action_1 : _actions_1) {
        if (!_hasElements_1) {
          _hasElements_1 = true;
        } else {
          _builder.appendImmediate("\n", "");
        }
        {
          Procedure _body_3 = action_1.getBody();
          EList<Block> _blocks_1 = _body_3.getBlocks();
          boolean _isEmpty_2 = _blocks_1.isEmpty();
          boolean _not_2 = (!_isEmpty_2);
          if (_not_2) {
            Procedure _scheduler = action_1.getScheduler();
            CharSequence _procedureContent_1 = this.getProcedureContent(_scheduler);
            _builder.append(_procedureContent_1, "");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.newLine();
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("// -- Initialize Members");
    _builder.newLine();
    CharSequence _memberIntializaion = this.getMemberIntializaion();
    _builder.append(_memberIntializaion, "");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("// --------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("// -- Action(s) Scheduler");
    _builder.newLine();
    CharSequence _schedulerContent = this.getSchedulerContent();
    _builder.append(_schedulerContent, "");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence getPortDeclaration(final String direction, final Port port) {
    StringConcatenation _builder = new StringConcatenation();
    {
      boolean _containsKey = this.fanoutPortConenction.containsKey(port);
      if (_containsKey) {
        {
          List<String> _get = this.fanoutPortConenction.get(port);
          for(final String name : _get) {
            _builder.append("sc_fifo_");
            _builder.append(direction, "");
            _builder.append("< ");
            Type _type = port.getType();
            CharSequence _doSwitch = this.doSwitch(_type);
            _builder.append(_doSwitch, "");
            _builder.append(" > ");
            _builder.append(name, "");
            _builder.append(";");
            _builder.newLineIfNotEmpty();
          }
        }
      } else {
        _builder.append("sc_fifo_");
        _builder.append(direction, "");
        _builder.append("< ");
        Type _type_1 = port.getType();
        CharSequence _doSwitch_1 = this.doSwitch(_type_1);
        _builder.append(_doSwitch_1, "");
        _builder.append(" > ");
        String _name = port.getName();
        _builder.append(_name, "");
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    return _builder;
  }
  
  public CharSequence getStateVariableDeclarationContent(final Var variable) {
    StringConcatenation _builder = new StringConcatenation();
    CharSequence _declare = this.declare(variable);
    _builder.append(_declare, "");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence getActionBodyContentAsProcess(final Procedure procedure) {
    StringConcatenation _builder = new StringConcatenation();
    Type _returnType = procedure.getReturnType();
    CharSequence _doSwitch = this.doSwitch(_returnType);
    _builder.append(_doSwitch, "");
    _builder.append(" ");
    {
      if (InstancePrinter.addScope) {
        _builder.append(this.name, "");
        _builder.append("::");
      }
    }
    String _name = procedure.getName();
    _builder.append(_name, "");
    _builder.append("(");
    EList<Param> _parameters = procedure.getParameters();
    final Function1<Param, CharSequence> _function = new Function1<Param, CharSequence>() {
      public CharSequence apply(final Param it) {
        return InstancePrinter.this.declare(it);
      }
    };
    String _join = IterableExtensions.<Param>join(_parameters, ", ", _function);
    _builder.append(_join, "");
    _builder.append(") {");
    _builder.newLineIfNotEmpty();
    {
      EList<Var> _locals = procedure.getLocals();
      for(final Var variable : _locals) {
        _builder.append("\t");
        CharSequence _declare = this.declare(variable);
        _builder.append(_declare, "\t");
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.append("// -- Reset Done");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("done_");
    String _name_1 = procedure.getName();
    _builder.append(_name_1, "\t");
    _builder.append(" = false; ");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("while(true){");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("do { wait(); } while ( !start_");
    String _name_2 = procedure.getName();
    _builder.append(_name_2, "\t\t");
    _builder.append(".read() );");
    _builder.newLineIfNotEmpty();
    {
      EList<Block> _blocks = procedure.getBlocks();
      for(final Block block : _blocks) {
        _builder.append("\t\t");
        CharSequence _doSwitch_1 = this.doSwitch(block);
        _builder.append(_doSwitch_1, "\t\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("done_");
    String _name_3 = procedure.getName();
    _builder.append(_name_3, "\t\t");
    _builder.append(" = true;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t\t");
    _builder.append("wait(); ");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getActionBodyContent(final Procedure procedure) {
    StringConcatenation _builder = new StringConcatenation();
    Type _returnType = procedure.getReturnType();
    CharSequence _doSwitch = this.doSwitch(_returnType);
    _builder.append(_doSwitch, "");
    _builder.append(" ");
    {
      if (InstancePrinter.addScope) {
        _builder.append(this.name, "");
        _builder.append("::");
      }
    }
    String _name = procedure.getName();
    _builder.append(_name, "");
    _builder.append("(");
    EList<Param> _parameters = procedure.getParameters();
    final Function1<Param, CharSequence> _function = new Function1<Param, CharSequence>() {
      public CharSequence apply(final Param it) {
        return InstancePrinter.this.declare(it);
      }
    };
    String _join = IterableExtensions.<Param>join(_parameters, ", ", _function);
    _builder.append(_join, "");
    _builder.append(") {");
    _builder.newLineIfNotEmpty();
    _builder.append("#pragma HLS inline off");
    _builder.newLine();
    {
      EList<Var> _locals = procedure.getLocals();
      for(final Var variable : _locals) {
        _builder.append("\t");
        CharSequence _declare = this.declare(variable);
        _builder.append(_declare, "\t");
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Block> _blocks = procedure.getBlocks();
      for(final Block block : _blocks) {
        _builder.append("\t");
        CharSequence _doSwitch_1 = this.doSwitch(block);
        _builder.append(_doSwitch_1, "\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getProcedureContent(final Procedure procedure) {
    StringConcatenation _builder = new StringConcatenation();
    Type _returnType = procedure.getReturnType();
    CharSequence _doSwitch = this.doSwitch(_returnType);
    _builder.append(_doSwitch, "");
    _builder.append(" ");
    {
      if (InstancePrinter.addScope) {
        _builder.append(this.name, "");
        _builder.append("::");
      }
    }
    String _name = procedure.getName();
    _builder.append(_name, "");
    _builder.append("(");
    EList<Param> _parameters = procedure.getParameters();
    final Function1<Param, CharSequence> _function = new Function1<Param, CharSequence>() {
      public CharSequence apply(final Param it) {
        return InstancePrinter.this.declare(it);
      }
    };
    String _join = IterableExtensions.<Param>join(_parameters, ", ", _function);
    _builder.append(_join, "");
    _builder.append(") {");
    _builder.newLineIfNotEmpty();
    {
      EList<Var> _locals = procedure.getLocals();
      for(final Var variable : _locals) {
        _builder.append("\t");
        CharSequence _declare = this.declare(variable);
        _builder.append(_declare, "\t");
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    {
      EList<Block> _blocks = procedure.getBlocks();
      for(final Block block : _blocks) {
        _builder.append("\t");
        CharSequence _doSwitch_1 = this.doSwitch(block);
        _builder.append(_doSwitch_1, "\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getMemberIntializaion() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("void ");
    {
      if (InstancePrinter.addScope) {
        _builder.append(this.name, "");
        _builder.append("::");
      }
    }
    _builder.append("intializeMembers(){");
    _builder.newLineIfNotEmpty();
    {
      EList<Var> _stateVars = this.actor.getStateVars();
      for(final Var variable : _stateVars) {
        {
          boolean _isInitialized = variable.isInitialized();
          if (_isInitialized) {
            {
              Type _type = variable.getType();
              boolean _isList = _type.isList();
              if (_isList) {
                _builder.append("\t");
                CharSequence _memberInitializationArray = this.getMemberInitializationArray(variable);
                _builder.append(_memberInitializationArray, "\t");
                _builder.newLineIfNotEmpty();
              } else {
                _builder.append("\t");
                String _name = variable.getName();
                _builder.append(_name, "\t");
                _builder.append(" = ");
                Expression _initialValue = variable.getInitialValue();
                CharSequence _doSwitch = this.doSwitch(_initialValue);
                _builder.append(_doSwitch, "\t");
                _builder.append("; ");
                _builder.newLineIfNotEmpty();
              }
            }
          }
        }
      }
    }
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getMemberInitializationArray(final Var v) {
    CharSequence _xblockexpression = null;
    {
      List<String> array = new ArrayList<String>();
      Type _type = v.getType();
      TypeList typeList = ((TypeList) _type);
      Type type = typeList.getInnermostType();
      List<Integer> _dimensions = typeList.getDimensions();
      List<Integer> listDimension = new ArrayList<Integer>(_dimensions);
      Object obj = v.getValue();
      String varName = v.getName();
      if (InstancePrinter.addScope) {
        String _name = v.getName();
        String _plus = ((this.name + "::") + _name);
        varName = _plus;
      }
      this.makeArray(varName, "", array, obj, listDimension, type);
      StringConcatenation _builder = new StringConcatenation();
      {
        for(final String value : array) {
          _builder.append(value, "");
          _builder.newLineIfNotEmpty();
        }
      }
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  @SuppressWarnings("Object")
  public Boolean makeArray(final String name, final String prefix, final List<String> array, final Object obj, final List<Integer> dimension, final Type type) {
    boolean _xifexpression = false;
    int _size = dimension.size();
    boolean _greaterThan = (_size > 1);
    if (_greaterThan) {
      List<Integer> newListDimension = new ArrayList<Integer>(dimension);
      Integer firstDim = dimension.get(0);
      newListDimension.remove(0);
      ExclusiveRange _doubleDotLessThan = new ExclusiveRange(0, (firstDim).intValue(), true);
      for (final int i : _doubleDotLessThan) {
        {
          String newPrefix = (((prefix + "[") + Integer.valueOf(i)) + "]");
          Object _get = Array.get(obj, i);
          this.makeArray(name, newPrefix, array, _get, newListDimension, type);
        }
      }
    } else {
      boolean _xifexpression_1 = false;
      Integer _get = dimension.get(0);
      boolean _equals = _get.equals(Integer.valueOf(1));
      if (_equals) {
        boolean _xblockexpression = false;
        {
          BigInteger value = BigInteger.valueOf(0);
          boolean _isBool = type.isBool();
          if (_isBool) {
            Object _get_1 = ValueUtil.get(type, obj, Integer.valueOf(0));
            if ((((Boolean) _get_1)).booleanValue()) {
              BigInteger _valueOf = BigInteger.valueOf(1);
              value = _valueOf;
            } else {
              BigInteger _valueOf_1 = BigInteger.valueOf(0);
              value = _valueOf_1;
            }
          }
          String valueString = value.toString();
          _xblockexpression = array.add(valueString);
        }
        _xifexpression_1 = _xblockexpression;
      } else {
        Integer _get_1 = dimension.get(0);
        ExclusiveRange _doubleDotLessThan_1 = new ExclusiveRange(0, (_get_1).intValue(), true);
        for (final int i_1 : _doubleDotLessThan_1) {
          {
            BigInteger value = BigInteger.valueOf(0);
            boolean _isBool = type.isBool();
            if (_isBool) {
              Object _get_2 = ValueUtil.get(type, obj, Integer.valueOf(0));
              if ((((Boolean) _get_2)).booleanValue()) {
                BigInteger _valueOf = BigInteger.valueOf(1);
                value = _valueOf;
              } else {
                BigInteger _valueOf_1 = BigInteger.valueOf(0);
                value = _valueOf_1;
              }
            } else {
              Object _get_3 = ValueUtil.get(type, obj, Integer.valueOf(i_1));
              value = ((BigInteger) _get_3);
            }
            String _string = value.toString();
            String _plus = ((((((name + prefix) + "[") + Integer.valueOf(i_1)) + "]") + " = ") + _string);
            String valueString = (_plus + ";");
            array.add(valueString);
          }
        }
      }
      _xifexpression = _xifexpression_1;
    }
    return Boolean.valueOf(_xifexpression);
  }
  
  public CharSequence getSchedulerContent() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("void ");
    {
      if (InstancePrinter.addScope) {
        _builder.append(this.name, "");
        _builder.append("::");
      }
    }
    _builder.append("scheduler(){");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("// -- Ports indexes");
    _builder.newLine();
    {
      EList<Port> _inputs = this.actor.getInputs();
      for(final Port port : _inputs) {
        _builder.append("\t");
        _builder.append("sc_uint<32> p_");
        String _name = port.getName();
        _builder.append(_name, "\t");
        _builder.append("_token_index = 0;");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("sc_uint<32> p_");
        String _name_1 = port.getName();
        _builder.append(_name_1, "\t");
        _builder.append("_token_index_read = 0;");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("bool p_");
        String _name_2 = port.getName();
        _builder.append(_name_2, "\t");
        _builder.append("_consume = false;");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Port> _outputs = this.actor.getOutputs();
      for(final Port port_1 : _outputs) {
        _builder.append("\t");
        _builder.append("sc_uint<32> p_");
        String _name_3 = port_1.getName();
        _builder.append(_name_3, "\t");
        _builder.append("_token_index = 0;");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("sc_uint<32> p_");
        String _name_4 = port_1.getName();
        _builder.append(_name_4, "\t");
        _builder.append("_token_index_write = 0;");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("bool p_");
        String _name_5 = port_1.getName();
        _builder.append(_name_5, "\t");
        _builder.append("_produce = false;");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Action guards");
    _builder.newLine();
    {
      EList<Action> _actions = this.actor.getActions();
      for(final Action action : _actions) {
        _builder.append("\t");
        _builder.append("bool guard_");
        String _name_6 = action.getName();
        _builder.append(_name_6, "\t");
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("done = false;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Initialize Members");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("intializeMembers();");
    _builder.newLine();
    _builder.append("\t ");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("state = s_");
    FSM _fsm = this.actor.getFsm();
    State _initialState = _fsm.getInitialState();
    String _label = _initialState.getLabel();
    _builder.append(_label, "\t");
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("old_state = s_");
    FSM _fsm_1 = this.actor.getFsm();
    State _initialState_1 = _fsm_1.getInitialState();
    String _label_1 = _initialState_1.getLabel();
    _builder.append(_label_1, "\t");
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// -- Wait For Start singal");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("do { wait(); } while ( !start.read() );");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("while(true){");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("// -- Calculate all guards");
    _builder.newLine();
    {
      EList<Action> _actions_1 = this.actor.getActions();
      for(final Action action_1 : _actions_1) {
        _builder.append("\t\t");
        _builder.append("guard_");
        String _name_7 = action_1.getName();
        _builder.append(_name_7, "\t\t");
        _builder.append(" = ");
        Procedure _scheduler = action_1.getScheduler();
        String _name_8 = _scheduler.getName();
        _builder.append(_name_8, "\t\t");
        _builder.append("();");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("switch (state){");
    _builder.newLine();
    {
      EList<Port> _inputs_1 = this.actor.getInputs();
      boolean _isEmpty = _inputs_1.isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        _builder.append("\t\t\t");
        _builder.append("case (s_READ):");
        _builder.newLine();
        {
          EList<Port> _inputs_2 = this.actor.getInputs();
          for(final Port port_2 : _inputs_2) {
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("if(p_");
            String _name_9 = port_2.getName();
            _builder.append(_name_9, "\t\t\t\t");
            _builder.append("_consume && ( ");
            String _name_10 = port_2.getName();
            _builder.append(_name_10, "\t\t\t\t");
            _builder.append(".num_available() > 0 ) ){");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t");
            _builder.append("for(int i = 0; i < p_");
            String _name_11 = port_2.getName();
            _builder.append(_name_11, "\t\t\t\t\t");
            _builder.append("_token_index_read; i++){");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t\t");
            _builder.append("p_");
            String _name_12 = port_2.getName();
            _builder.append(_name_12, "\t\t\t\t\t\t");
            _builder.append("[i] = ");
            String _name_13 = port_2.getName();
            _builder.append(_name_13, "\t\t\t\t\t\t");
            _builder.append(".read();");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t\t");
            _builder.append("p_");
            String _name_14 = port_2.getName();
            _builder.append(_name_14, "\t\t\t\t\t\t");
            _builder.append("_token_index++;");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t\t");
            _builder.append("p_");
            String _name_15 = port_2.getName();
            _builder.append(_name_15, "\t\t\t\t\t\t");
            _builder.append("_consume = false;");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t");
            _builder.append("}");
            _builder.newLine();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("}");
            _builder.newLine();
          }
        }
        _builder.append("\t\t\t");
        _builder.append("\t");
        _builder.append("state = old_state;");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("break;");
        _builder.newLine();
      }
    }
    _builder.append("\t\t\t");
    _builder.newLine();
    {
      EList<Port> _outputs_1 = this.actor.getOutputs();
      boolean _isEmpty_1 = _outputs_1.isEmpty();
      boolean _not_1 = (!_isEmpty_1);
      if (_not_1) {
        _builder.append("\t\t\t");
        _builder.append("case (s_WRITE):");
        _builder.newLine();
        {
          EList<Port> _outputs_2 = this.actor.getOutputs();
          for(final Port port_3 : _outputs_2) {
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("if(p_");
            String _name_16 = port_3.getName();
            _builder.append(_name_16, "\t\t\t\t");
            _builder.append("_produce){");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t");
            _builder.append("for(int i = 0; i < p_");
            String _name_17 = port_3.getName();
            _builder.append(_name_17, "\t\t\t\t\t");
            _builder.append("_token_index_write; i++){");
            _builder.newLineIfNotEmpty();
            {
              List<String> _get = this.fanoutPortConenction.get(port_3);
              for(final String name : _get) {
                _builder.append("\t\t\t");
                _builder.append("\t");
                _builder.append("\t\t");
                _builder.append(name, "\t\t\t\t\t\t");
                _builder.append(".write(p_");
                String _name_18 = port_3.getName();
                _builder.append(_name_18, "\t\t\t\t\t\t");
                _builder.append("[i]);");
                _builder.newLineIfNotEmpty();
              }
            }
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t\t");
            _builder.append("p_");
            String _name_19 = port_3.getName();
            _builder.append(_name_19, "\t\t\t\t\t\t");
            _builder.append("_token_index++;");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t");
            _builder.append("}");
            _builder.newLine();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("\t");
            _builder.append("p_");
            String _name_20 = port_3.getName();
            _builder.append(_name_20, "\t\t\t\t\t");
            _builder.append("_produce = false;");
            _builder.newLineIfNotEmpty();
            _builder.append("\t\t\t");
            _builder.append("\t");
            _builder.append("}");
            _builder.newLine();
          }
        }
        _builder.append("\t\t\t");
        _builder.append("\t");
        _builder.append("state = old_state;");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("break;");
        _builder.newLine();
      }
    }
    _builder.append("\t\t\t");
    _builder.newLine();
    {
      FSM _fsm_2 = this.actor.getFsm();
      EList<State> _states = _fsm_2.getStates();
      boolean _hasElements = false;
      for(final State state : _states) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate("\n", "\t\t\t");
        }
        _builder.append("\t\t\t");
        CharSequence _stateContent = this.getStateContent(state);
        _builder.append(_stateContent, "\t\t\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t\t\t");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("default:");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("state = s_");
    FSM _fsm_3 = this.actor.getFsm();
    State _initialState_2 = _fsm_3.getInitialState();
    String _label_2 = _initialState_2.getLabel();
    _builder.append(_label_2, "\t\t\t\t");
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.append("\t\t\t");
    _builder.append("break;");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("wait();");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("done = true;");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence getStateContent(final State state) {
    CharSequence _xblockexpression = null;
    {
      Map<Port, Integer> maxPortTokens = new HashMap<Port, Integer>();
      Boolean actionsHaveInputPrts = Boolean.valueOf(false);
      EList<Edge> _outgoing = state.getOutgoing();
      for (final Edge edge : _outgoing) {
        {
          Action action = ((Transition) edge).getAction();
          Pattern _inputPattern = action.getInputPattern();
          EList<Port> _ports = _inputPattern.getPorts();
          boolean _isEmpty = _ports.isEmpty();
          boolean _not = (!_isEmpty);
          if (_not) {
            Pattern _inputPattern_1 = action.getInputPattern();
            EMap<Port, Integer> inputNumTokens = _inputPattern_1.getNumTokensMap();
            Set<Port> _keySet = inputNumTokens.keySet();
            for (final Port port : _keySet) {
              boolean _containsKey = maxPortTokens.containsKey(port);
              if (_containsKey) {
                Integer oldValue = maxPortTokens.get(port);
                Integer _get = inputNumTokens.get(port);
                boolean _lessThan = (oldValue.compareTo(_get) < 0);
                if (_lessThan) {
                  Integer _get_1 = inputNumTokens.get(port);
                  maxPortTokens.put(port, _get_1);
                }
              } else {
                Integer _get_2 = inputNumTokens.get(port);
                maxPortTokens.put(port, _get_2);
              }
            }
            actionsHaveInputPrts = Boolean.valueOf(true);
          }
        }
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("case(s_");
      String _label = state.getLabel();
      _builder.append(_label, "");
      _builder.append("):");
      _builder.newLineIfNotEmpty();
      {
        EList<Edge> _outgoing_1 = state.getOutgoing();
        boolean _hasElements = false;
        for(final Edge edge_1 : _outgoing_1) {
          if (!_hasElements) {
            _hasElements = true;
          } else {
            _builder.appendImmediate(" else", "\t");
          }
          _builder.append("\t");
          CharSequence _transitionContent = this.getTransitionContent(((Transition) edge_1));
          _builder.append(_transitionContent, "\t");
          _builder.newLineIfNotEmpty();
          _builder.append("\t");
          _builder.append("}");
        }
      }
      {
        if ((actionsHaveInputPrts).booleanValue()) {
          _builder.append(" else {");
          _builder.newLineIfNotEmpty();
          {
            Set<Port> _keySet = maxPortTokens.keySet();
            for(final Port port : _keySet) {
              _builder.append("\t");
              _builder.append("\t");
              _builder.append("if( p_");
              String _name = port.getName();
              _builder.append(_name, "\t\t");
              _builder.append("_token_index < ");
              Integer _get = maxPortTokens.get(port);
              _builder.append(_get, "\t\t");
              _builder.append(" ){");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("\t");
              _builder.append("\t");
              _builder.append("p_");
              String _name_1 = port.getName();
              _builder.append(_name_1, "\t\t\t");
              _builder.append("_token_index_read = ");
              Integer _get_1 = maxPortTokens.get(port);
              _builder.append(_get_1, "\t\t\t");
              _builder.append(" - p_");
              String _name_2 = port.getName();
              _builder.append(_name_2, "\t\t\t");
              _builder.append("_token_index;");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("\t");
              _builder.append("\t");
              _builder.append("p_");
              String _name_3 = port.getName();
              _builder.append(_name_3, "\t\t\t");
              _builder.append("_consume = true;");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("\t");
              _builder.append("}");
              _builder.newLine();
            }
          }
          _builder.append("\t");
          _builder.append("\t");
          _builder.append("old_state = s_");
          String _label_1 = state.getLabel();
          _builder.append(_label_1, "\t\t");
          _builder.append(";");
          _builder.newLineIfNotEmpty();
          _builder.append("\t");
          _builder.append("\t");
          _builder.append("state = s_READ;");
          _builder.newLine();
          _builder.append("\t");
          _builder.append("}");
        }
      }
      _builder.newLineIfNotEmpty();
      _builder.append("break;");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence getTransitionContent(final Transition transition) {
    CharSequence _xblockexpression = null;
    {
      Action action = transition.getAction();
      State tState = transition.getTarget();
      Pattern _inputPattern = action.getInputPattern();
      EMap<Port, Integer> inputNumTokens = _inputPattern.getNumTokensMap();
      Pattern _outputPattern = action.getOutputPattern();
      EMap<Port, Integer> outputNumTokens = _outputPattern.getNumTokensMap();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("if(guard_");
      String _name = action.getName();
      _builder.append(_name, "");
      {
        boolean _isEmpty = inputNumTokens.isEmpty();
        boolean _not = (!_isEmpty);
        if (_not) {
          _builder.append(" && ");
          {
            Set<Port> _keySet = inputNumTokens.keySet();
            boolean _hasElements = false;
            for(final Port port : _keySet) {
              if (!_hasElements) {
                _hasElements = true;
              } else {
                _builder.appendImmediate(" && ", "");
              }
              _builder.append("p_");
              String _name_1 = port.getName();
              _builder.append(_name_1, "");
              _builder.append("_token_index == ");
              Integer _get = inputNumTokens.get(port);
              _builder.append(_get, "");
            }
          }
        }
      }
      _builder.append("){");
      _builder.newLineIfNotEmpty();
      _builder.append("\t\t\t\t");
      _builder.newLine();
      {
        if ((InstancePrinter.actionAsProcess).booleanValue()) {
          _builder.append("\t");
          _builder.append("do { wait(); } while ( !done_");
          Procedure _body = action.getBody();
          String _name_2 = _body.getName();
          _builder.append(_name_2, "\t");
          _builder.append(".read() );");
          _builder.newLineIfNotEmpty();
        } else {
          _builder.append("\t");
          Procedure _body_1 = action.getBody();
          String _name_3 = _body_1.getName();
          _builder.append(_name_3, "\t");
          _builder.append("();");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("\t");
      _builder.newLine();
      {
        boolean _isEmpty_1 = inputNumTokens.isEmpty();
        boolean _not_1 = (!_isEmpty_1);
        if (_not_1) {
          {
            Set<Port> _keySet_1 = inputNumTokens.keySet();
            for(final Port port_1 : _keySet_1) {
              _builder.append("\t");
              _builder.append("p_");
              String _name_4 = port_1.getName();
              _builder.append(_name_4, "\t");
              _builder.append("_token_index = 0;");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      {
        boolean _isEmpty_2 = outputNumTokens.isEmpty();
        if (_isEmpty_2) {
          _builder.append("\t");
          _builder.append("state = s_");
          String _label = tState.getLabel();
          _builder.append(_label, "\t");
          _builder.append(";");
          _builder.newLineIfNotEmpty();
        } else {
          {
            Set<Port> _keySet_2 = outputNumTokens.keySet();
            for(final Port port_2 : _keySet_2) {
              _builder.append("\t");
              _builder.append("p_");
              String _name_5 = port_2.getName();
              _builder.append(_name_5, "\t");
              _builder.append("_token_index_write = ");
              Integer _get_1 = outputNumTokens.get(port_2);
              _builder.append(_get_1, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("p_");
              String _name_6 = port_2.getName();
              _builder.append(_name_6, "\t");
              _builder.append("_produce = true;");
              _builder.newLineIfNotEmpty();
            }
          }
          _builder.append("\t");
          _builder.append("old_state = s_");
          String _label_1 = tState.getLabel();
          _builder.append(_label_1, "\t");
          _builder.append(";");
          _builder.newLineIfNotEmpty();
          _builder.append("\t");
          _builder.append("state = s_WRITE;");
          _builder.newLine();
        }
      }
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence caseBlockBasic(final BlockBasic block) {
    StringConcatenation _builder = new StringConcatenation();
    {
      EList<Instruction> _instructions = block.getInstructions();
      for(final Instruction instr : _instructions) {
        CharSequence _doSwitch = this.doSwitch(instr);
        _builder.append(_doSwitch, "");
        _builder.newLineIfNotEmpty();
      }
    }
    return _builder;
  }
  
  public CharSequence caseBlockIf(final BlockIf block) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("if (");
    Expression _condition = block.getCondition();
    CharSequence _doSwitch = this.doSwitch(_condition);
    _builder.append(_doSwitch, "");
    _builder.append(") {");
    _builder.newLineIfNotEmpty();
    {
      EList<Block> _thenBlocks = block.getThenBlocks();
      for(final Block thenBlock : _thenBlocks) {
        _builder.append("\t");
        CharSequence _doSwitch_1 = this.doSwitch(thenBlock);
        _builder.append(_doSwitch_1, "\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("}");
    {
      boolean _isElseRequired = block.isElseRequired();
      if (_isElseRequired) {
        _builder.append(" else {");
        _builder.newLineIfNotEmpty();
        {
          EList<Block> _elseBlocks = block.getElseBlocks();
          for(final Block elseBlock : _elseBlocks) {
            _builder.append("\t");
            CharSequence _doSwitch_2 = this.doSwitch(elseBlock);
            _builder.append(_doSwitch_2, "\t");
            _builder.newLineIfNotEmpty();
          }
        }
        _builder.append("\t\t");
        _builder.append("}");
        _builder.newLine();
      }
    }
    return _builder;
  }
  
  public CharSequence caseBlockWhile(final BlockWhile blockWhile) {
    CharSequence _xblockexpression = null;
    {
      Attribute _attribute = blockWhile.getAttribute("loopLabel");
      Object _objectValue = _attribute.getObjectValue();
      final Integer loopIndex = ((Integer) _objectValue);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("loop_");
      _builder.append(loopIndex, "");
      _builder.append(": while (");
      Expression _condition = blockWhile.getCondition();
      CharSequence _doSwitch = this.doSwitch(_condition);
      _builder.append(_doSwitch, "");
      _builder.append(") {");
      _builder.newLineIfNotEmpty();
      {
        EList<Block> _blocks = blockWhile.getBlocks();
        for(final Block block : _blocks) {
          _builder.append("\t");
          CharSequence _doSwitch_1 = this.doSwitch(block);
          _builder.append(_doSwitch_1, "\t");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("}");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public String caseBlockFor(final BlockFor blockFor) {
    String _xblockexpression = null;
    {
      Attribute _attribute = blockFor.getAttribute("loopLabel");
      Object _objectValue = _attribute.getObjectValue();
      final Integer loopIndex = ((Integer) _objectValue);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("loop_");
      _builder.append(loopIndex, "");
      _builder.append(": for (");
      EList<Instruction> _init = blockFor.getInit();
      final Function1<Instruction, String> _function = new Function1<Instruction, String>() {
        public String apply(final Instruction it) {
          StringConcatenation _builder = new StringConcatenation();
          String _expression = InstancePrinter.this.toExpression(it);
          _builder.append(_expression, "");
          return _builder.toString();
        }
      };
      String _join = IterableExtensions.<Instruction>join(_init, ", ", _function);
      _builder.append(_join, "");
      _builder.append(" ; ");
      Expression _condition = blockFor.getCondition();
      CharSequence _doSwitch = this.doSwitch(_condition);
      _builder.append(_doSwitch, "");
      _builder.append(" ; ");
      EList<Instruction> _step = blockFor.getStep();
      final Function1<Instruction, String> _function_1 = new Function1<Instruction, String>() {
        public String apply(final Instruction it) {
          StringConcatenation _builder = new StringConcatenation();
          String _expression = InstancePrinter.this.toExpression(it);
          _builder.append(_expression, "");
          return _builder.toString();
        }
      };
      String _join_1 = IterableExtensions.<Instruction>join(_step, ", ", _function_1);
      _builder.append(_join_1, "");
      _builder.append(") {");
      _builder.newLineIfNotEmpty();
      {
        EList<Block> _blocks = blockFor.getBlocks();
        for(final Block contentBlock : _blocks) {
          _builder.append("\t");
          CharSequence _doSwitch_1 = this.doSwitch(contentBlock);
          _builder.append(_doSwitch_1, "\t");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("}");
      _builder.newLine();
      _xblockexpression = _builder.toString();
    }
    return _xblockexpression;
  }
  
  public CharSequence caseInstAssign(final InstAssign inst) {
    StringConcatenation _builder = new StringConcatenation();
    Def _target = inst.getTarget();
    Var _variable = _target.getVariable();
    String _name = _variable.getName();
    _builder.append(_name, "");
    _builder.append(" = ");
    Expression _value = inst.getValue();
    CharSequence _doSwitch = this.doSwitch(_value);
    _builder.append(_doSwitch, "");
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public CharSequence caseInstCall(final InstCall call) {
    CharSequence _xblockexpression = null;
    {
      Map<Param, Arg> paramArg = new HashMap<Param, Arg>();
      int i = 0;
      Procedure _procedure = call.getProcedure();
      EList<Param> _parameters = _procedure.getParameters();
      for (final Param param : _parameters) {
        {
          EList<Arg> _arguments = call.getArguments();
          Arg _get = _arguments.get(i);
          paramArg.put(param, _get);
          i++;
        }
      }
      StringConcatenation _builder = new StringConcatenation();
      {
        boolean _isPrint = call.isPrint();
        if (_isPrint) {
          _builder.append("#ifndef __SYNTHESIS__");
          _builder.newLine();
          _builder.append("\t");
          _builder.append("std::cout << ");
          {
            EList<Arg> _arguments = call.getArguments();
            boolean _hasElements = false;
            for(final Arg arg : _arguments) {
              if (!_hasElements) {
                _hasElements = true;
              } else {
                _builder.appendImmediate(" << ", "\t");
              }
              CharSequence _coutArg = this.coutArg(arg);
              _builder.append(_coutArg, "\t");
            }
          }
          _builder.append(";");
          _builder.newLineIfNotEmpty();
          _builder.append("#endif");
          _builder.newLine();
        } else {
          {
            boolean _hasAttribute = call.hasAttribute("memberCall");
            boolean _not = (!_hasAttribute);
            if (_not) {
              {
                Def _target = call.getTarget();
                boolean _notEquals = (!Objects.equal(_target, null));
                if (_notEquals) {
                  Def _target_1 = call.getTarget();
                  Var _variable = _target_1.getVariable();
                  String _name = _variable.getName();
                  _builder.append(_name, "");
                  _builder.append(" = ");
                }
              }
              Procedure _procedure_1 = call.getProcedure();
              String _name_1 = _procedure_1.getName();
              _builder.append(_name_1, "");
              _builder.append("(");
              {
                Set<Param> _keySet = paramArg.keySet();
                boolean _hasElements_1 = false;
                for(final Param param_1 : _keySet) {
                  if (!_hasElements_1) {
                    _hasElements_1 = true;
                  } else {
                    _builder.appendImmediate(", ", "");
                  }
                  Arg _get = paramArg.get(param_1);
                  CharSequence _printWithCast = this.printWithCast(param_1, _get);
                  _builder.append(_printWithCast, "");
                }
              }
              _builder.append(");");
              _builder.newLineIfNotEmpty();
            } else {
              EList<Arg> _arguments_1 = call.getArguments();
              Arg _get_1 = _arguments_1.get(0);
              CharSequence _print = this.print(_get_1);
              _builder.append(_print, "");
              _builder.append(".");
              Procedure _procedure_2 = call.getProcedure();
              String _name_2 = _procedure_2.getName();
              _builder.append(_name_2, "");
              _builder.append("();");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence caseInstLoad(final InstLoad load) {
    CharSequence _xblockexpression = null;
    {
      Def _target = load.getTarget();
      final Var target = _target.getVariable();
      Use _source = load.getSource();
      final Var source = _source.getVariable();
      StringConcatenation _builder = new StringConcatenation();
      String _name = target.getName();
      _builder.append(_name, "");
      _builder.append(" = ");
      {
        if (InstancePrinter.addScope) {
          _builder.append(this.name, "");
          _builder.append("::");
        }
      }
      String _name_1 = source.getName();
      _builder.append(_name_1, "");
      EList<Expression> _indexes = load.getIndexes();
      String _printArrayIndexes = this.printArrayIndexes(_indexes);
      _builder.append(_printArrayIndexes, "");
      _builder.append(";");
      _builder.newLineIfNotEmpty();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence caseInstReturn(final InstReturn ret) {
    CharSequence _xblockexpression = null;
    {
      Procedure procedure = EcoreHelper.<Procedure>getContainerOfType(ret, Procedure.class);
      Type type = procedure.getReturnType();
      StringConcatenation _builder = new StringConcatenation();
      {
        Expression _value = ret.getValue();
        boolean _notEquals = (!Objects.equal(_value, null));
        if (_notEquals) {
          _builder.append("return ( ");
          CharSequence _doSwitch = this.doSwitch(type);
          _builder.append(_doSwitch, "");
          _builder.append(" ) (");
          Expression _value_1 = ret.getValue();
          CharSequence _doSwitch_1 = this.doSwitch(_value_1);
          _builder.append(_doSwitch_1, "");
          _builder.append(");");
          _builder.newLineIfNotEmpty();
        }
      }
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence caseInstStore(final InstStore store) {
    CharSequence _xblockexpression = null;
    {
      Def _target = store.getTarget();
      final Var target = _target.getVariable();
      StringConcatenation _builder = new StringConcatenation();
      {
        if (InstancePrinter.addScope) {
          _builder.append(this.name, "");
          _builder.append("::");
        }
      }
      String _name = target.getName();
      _builder.append(_name, "");
      EList<Expression> _indexes = store.getIndexes();
      String _printArrayIndexes = this.printArrayIndexes(_indexes);
      _builder.append(_printArrayIndexes, "");
      _builder.append(" = ");
      Expression _value = store.getValue();
      CharSequence _doSwitch = this.doSwitch(_value);
      _builder.append(_doSwitch, "");
      _builder.append(";");
      _builder.newLineIfNotEmpty();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence caseExprBinary(final ExprBinary expr) {
    CharSequence _xblockexpression = null;
    {
      final OpBinary op = expr.getOp();
      int _xifexpression = (int) 0;
      boolean _or = false;
      boolean _equals = Objects.equal(op, OpBinary.SHIFT_LEFT);
      if (_equals) {
        _or = true;
      } else {
        boolean _equals_1 = Objects.equal(op, OpBinary.SHIFT_RIGHT);
        _or = _equals_1;
      }
      if (_or) {
        _xifexpression = Integer.MIN_VALUE;
      } else {
        _xifexpression = op.getPrecedence();
      }
      int nextPrec = _xifexpression;
      boolean _xifexpression_1 = false;
      Expression _e2 = expr.getE2();
      if ((_e2 instanceof ExprBinary)) {
        _xifexpression_1 = true;
      } else {
        _xifexpression_1 = false;
      }
      final boolean nextOpBin = _xifexpression_1;
      StringConcatenation _builder = new StringConcatenation();
      Expression _e1 = expr.getE1();
      CharSequence _printExpr = this.printExpr(_e1, nextPrec, 0);
      _builder.append(_printExpr, "");
      _builder.append(" ");
      String _stringRepresentation = this.stringRepresentation(op);
      _builder.append(_stringRepresentation, "");
      _builder.append(" ");
      {
        if (nextOpBin) {
          _builder.append("(");
          Expression _e2_1 = expr.getE2();
          Type _type = _e2_1.getType();
          CharSequence _doSwitch = this.doSwitch(_type);
          _builder.append(_doSwitch, "");
          _builder.append(") (");
        }
      }
      Expression _e2_2 = expr.getE2();
      CharSequence _printExpr_1 = this.printExpr(_e2_2, nextPrec, 1);
      _builder.append(_printExpr_1, "");
      {
        if (nextOpBin) {
          _builder.append(" )");
        }
      }
      final String resultingExpr = _builder.toString();
      CharSequence _xifexpression_2 = null;
      boolean _needsParentheses = op.needsParentheses(this.precedence, this.branch);
      if (_needsParentheses) {
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("(");
        _builder_1.append(resultingExpr, "");
        _builder_1.append(")");
        _xifexpression_2 = _builder_1;
      } else {
        _xifexpression_2 = resultingExpr;
      }
      _xblockexpression = _xifexpression_2;
    }
    return _xblockexpression;
  }
  
  private CharSequence print(final Arg arg) {
    CharSequence _xifexpression = null;
    boolean _isByRef = arg.isByRef();
    if (_isByRef) {
      Use _use = ((ArgByRef) arg).getUse();
      Var _variable = _use.getVariable();
      String _name = _variable.getName();
      String _plus = ("&" + _name);
      EList<Expression> _indexes = ((ArgByRef) arg).getIndexes();
      String _printArrayIndexes = this.printArrayIndexes(_indexes);
      _xifexpression = (_plus + _printArrayIndexes);
    } else {
      Expression _value = ((ArgByVal) arg).getValue();
      _xifexpression = this.doSwitch(_value);
    }
    return _xifexpression;
  }
  
  public CharSequence printWithCast(final Param param, final Arg arg) {
    CharSequence _xblockexpression = null;
    {
      final Var variable = param.getVariable();
      CharSequence _xifexpression = null;
      boolean _isByRef = arg.isByRef();
      if (_isByRef) {
        StringConcatenation _builder = new StringConcatenation();
        _builder.append("(");
        Type _type = variable.getType();
        CharSequence _doSwitch = this.doSwitch(_type);
        _builder.append(_doSwitch, "");
        _builder.append(") &");
        Use _use = ((ArgByRef) arg).getUse();
        Var _variable = _use.getVariable();
        String _name = _variable.getName();
        _builder.append(_name, "");
        EList<Expression> _indexes = ((ArgByRef) arg).getIndexes();
        String _printArrayIndexes = this.printArrayIndexes(_indexes);
        _builder.append(_printArrayIndexes, "");
        _xifexpression = _builder;
      } else {
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("(");
        Type _type_1 = variable.getType();
        CharSequence _doSwitch_1 = this.doSwitch(_type_1);
        _builder_1.append(_doSwitch_1, "");
        _builder_1.append(") ");
        Expression _value = ((ArgByVal) arg).getValue();
        CharSequence _doSwitch_2 = this.doSwitch(_value);
        _builder_1.append(_doSwitch_2, "");
        _xifexpression = _builder_1;
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
  
  protected CharSequence _coutArg(final ArgByRef arg) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("&");
    Use _use = arg.getUse();
    Var _variable = _use.getVariable();
    CharSequence _doSwitch = this.doSwitch(_variable);
    _builder.append(_doSwitch, "");
    {
      EList<Expression> _indexes = arg.getIndexes();
      for(final Expression index : _indexes) {
        _builder.append("[");
        CharSequence _doSwitch_1 = this.doSwitch(index);
        _builder.append(_doSwitch_1, "");
        _builder.append("]");
      }
    }
    return _builder;
  }
  
  protected CharSequence _coutArg(final ArgByVal arg) {
    Expression _value = arg.getValue();
    return this.doSwitch(_value);
  }
  
  private CharSequence declare(final Param param) {
    CharSequence _xblockexpression = null;
    {
      final Var variable = param.getVariable();
      StringConcatenation _builder = new StringConcatenation();
      Type _type = variable.getType();
      CharSequence _doSwitch = this.doSwitch(_type);
      _builder.append(_doSwitch, "");
      _builder.append(" ");
      String _name = variable.getName();
      _builder.append(_name, "");
      Type _type_1 = variable.getType();
      List<Expression> _dimensionsExpr = _type_1.getDimensionsExpr();
      String _printArrayIndexes = this.printArrayIndexes(_dimensionsExpr);
      _builder.append(_printArrayIndexes, "");
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence declare(final Var variable) {
    CharSequence _xblockexpression = null;
    {
      String _xifexpression = null;
      boolean _and = false;
      boolean _isAssignable = variable.isAssignable();
      boolean _not = (!_isAssignable);
      if (!_not) {
        _and = false;
      } else {
        boolean _isGlobal = variable.isGlobal();
        _and = _isGlobal;
      }
      if (_and) {
        _xifexpression = "const ";
      }
      final String const_ = _xifexpression;
      String _xifexpression_1 = null;
      boolean _isGlobal_1 = variable.isGlobal();
      if (_isGlobal_1) {
        _xifexpression_1 = "static ";
      }
      final String global = _xifexpression_1;
      final Type type = variable.getType();
      Type _type = variable.getType();
      List<Expression> _dimensionsExpr = _type.getDimensionsExpr();
      final String dims = this.printArrayIndexes(_dimensionsExpr);
      String _xifexpression_2 = null;
      boolean _isInitialized = variable.isInitialized();
      if (_isInitialized) {
        Expression _initialValue = variable.getInitialValue();
        CharSequence _doSwitch = this.doSwitch(_initialValue);
        _xifexpression_2 = (" = " + _doSwitch);
      }
      final String init = _xifexpression_2;
      String _xifexpression_3 = null;
      boolean _isGlobal_2 = variable.isGlobal();
      if (_isGlobal_2) {
        _xifexpression_3 = ";";
      }
      final String end = _xifexpression_3;
      StringConcatenation _builder = new StringConcatenation();
      _builder.append(global, "");
      _builder.append(const_, "");
      CharSequence _doSwitch_1 = this.doSwitch(type);
      _builder.append(_doSwitch_1, "");
      _builder.append(" ");
      String _name = variable.getName();
      _builder.append(_name, "");
      _builder.append(dims, "");
      _builder.append(init, "");
      _builder.append(end, "");
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence declareSource(final Var variable) {
    CharSequence _xblockexpression = null;
    {
      String _xifexpression = null;
      boolean _and = false;
      boolean _isAssignable = variable.isAssignable();
      boolean _not = (!_isAssignable);
      if (!_not) {
        _and = false;
      } else {
        boolean _isGlobal = variable.isGlobal();
        _and = _isGlobal;
      }
      if (_and) {
        _xifexpression = "const ";
      }
      final String const_ = _xifexpression;
      final Type type = variable.getType();
      Type _type = variable.getType();
      List<Expression> _dimensionsExpr = _type.getDimensionsExpr();
      final String dims = this.printArrayIndexes(_dimensionsExpr);
      String _xifexpression_1 = null;
      boolean _isInitialized = variable.isInitialized();
      if (_isInitialized) {
        Expression _initialValue = variable.getInitialValue();
        CharSequence _doSwitch = this.doSwitch(_initialValue);
        _xifexpression_1 = (" = " + _doSwitch);
      }
      final String init = _xifexpression_1;
      String _xifexpression_2 = null;
      boolean _isGlobal_1 = variable.isGlobal();
      if (_isGlobal_1) {
        _xifexpression_2 = ";";
      }
      final String end = _xifexpression_2;
      CharSequence _xifexpression_3 = null;
      boolean _isInitialized_1 = variable.isInitialized();
      if (_isInitialized_1) {
        StringConcatenation _builder = new StringConcatenation();
        _builder.append(const_, "");
        CharSequence _doSwitch_1 = this.doSwitch(type);
        _builder.append(_doSwitch_1, "");
        _builder.append(" ");
        String _name = variable.getName();
        _builder.append(_name, "");
        _builder.append(dims, "");
        _builder.append(init, "");
        _builder.append(end, "");
        _xifexpression_3 = _builder;
      }
      _xblockexpression = _xifexpression_3;
    }
    return _xblockexpression;
  }
  
  public CharSequence declareStateVar(final Var variable) {
    CharSequence _xblockexpression = null;
    {
      final Type type = variable.getType();
      Type _type = variable.getType();
      List<Expression> _dimensionsExpr = _type.getDimensionsExpr();
      final String dims = this.printArrayIndexes(_dimensionsExpr);
      String _xifexpression = null;
      boolean _isGlobal = variable.isGlobal();
      if (_isGlobal) {
        _xifexpression = ";";
      }
      final String end = _xifexpression;
      StringConcatenation _builder = new StringConcatenation();
      CharSequence _doSwitch = this.doSwitch(type);
      _builder.append(_doSwitch, "");
      _builder.append(" ");
      String _name = variable.getName();
      _builder.append(_name, "");
      _builder.append(dims, "");
      _builder.append(end, "");
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence declareNoInit(final Var variable) {
    CharSequence _xblockexpression = null;
    {
      String _xifexpression = null;
      boolean _and = false;
      boolean _isAssignable = variable.isAssignable();
      boolean _not = (!_isAssignable);
      if (!_not) {
        _and = false;
      } else {
        boolean _isGlobal = variable.isGlobal();
        _and = _isGlobal;
      }
      if (_and) {
        _xifexpression = "const ";
      }
      final String const_ = _xifexpression;
      final Type type = variable.getType();
      Type _type = variable.getType();
      List<Expression> _dimensionsExpr = _type.getDimensionsExpr();
      final String dims = this.printArrayIndexes(_dimensionsExpr);
      String _xifexpression_1 = null;
      boolean _isGlobal_1 = variable.isGlobal();
      if (_isGlobal_1) {
        _xifexpression_1 = ";";
      }
      final String end = _xifexpression_1;
      StringConcatenation _builder = new StringConcatenation();
      _builder.append(const_, "");
      CharSequence _doSwitch = this.doSwitch(type);
      _builder.append(_doSwitch, "");
      _builder.append(" ");
      String _name = variable.getName();
      _builder.append(_name, "");
      _builder.append(dims, "");
      _builder.append(end, "");
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence declare(final Procedure procedure) {
    StringConcatenation _builder = new StringConcatenation();
    Type _returnType = procedure.getReturnType();
    CharSequence _doSwitch = this.doSwitch(_returnType);
    _builder.append(_doSwitch, "");
    _builder.append(" ");
    String _name = procedure.getName();
    _builder.append(_name, "");
    _builder.append("(");
    EList<Param> _parameters = procedure.getParameters();
    final Function1<Param, CharSequence> _function = new Function1<Param, CharSequence>() {
      public CharSequence apply(final Param it) {
        return InstancePrinter.this.declare(it);
      }
    };
    String _join = IterableExtensions.<Param>join(_parameters, ", ", _function);
    _builder.append(_join, "");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public void getFanoutPortNames() {
    Entity _adapter = this.actor.<Entity>getAdapter(Entity.class);
    Map<Port, List<Connection>> portConnection = _adapter.getOutgoingPortMap();
    HashMap<Port, List<String>> _hashMap = new HashMap<Port, List<String>>();
    this.fanoutPortConenction = _hashMap;
    EList<Port> _outputs = this.actor.getOutputs();
    for (final Port port : _outputs) {
      List<Connection> _get = portConnection.get(port);
      int _size = _get.size();
      boolean _greaterThan = (_size > 1);
      if (_greaterThan) {
        List<String> portNames = new ArrayList<String>();
        List<Connection> _get_1 = portConnection.get(port);
        for (final Connection connection : _get_1) {
          Vertex _target = connection.getTarget();
          if ((_target instanceof Actor)) {
            String _name = port.getName();
            String _plus = (_name + "_f_");
            Vertex _target_1 = connection.getTarget();
            String _name_1 = ((Actor) _target_1).getName();
            String _plus_1 = (_plus + _name_1);
            portNames.add(_plus_1);
          } else {
            String _name_2 = port.getName();
            String _plus_2 = (_name_2 + "_f_");
            Vertex _target_2 = connection.getTarget();
            String _name_3 = ((Port) _target_2).getName();
            String _plus_3 = (_plus_2 + _name_3);
            portNames.add(_plus_3);
          }
        }
        this.fanoutPortConenction.put(port, portNames);
      } else {
        String _name_4 = port.getName();
        List<String> _asList = Arrays.<String>asList(_name_4);
        this.fanoutPortConenction.put(port, _asList);
      }
    }
  }
  
  public CharSequence coutArg(final Arg arg) {
    if (arg instanceof ArgByRef) {
      return _coutArg((ArgByRef)arg);
    } else if (arg instanceof ArgByVal) {
      return _coutArg((ArgByVal)arg);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(arg).toString());
    }
  }
}
