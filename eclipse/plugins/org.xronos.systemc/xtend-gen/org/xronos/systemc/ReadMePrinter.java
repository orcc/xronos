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

import net.sf.orcc.df.Network;
import org.eclipse.xtend2.lib.StringConcatenation;

/**
 * A README Printer
 * 
 * @author Endri Bezati
 */
@SuppressWarnings("all")
public class ReadMePrinter {
  private Network network;
  
  public Network setNetwork(final Network network) {
    return this.network = network;
  }
  
  public CharSequence getContent() {
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
    _builder.append("// README for Network: ");
    String _simpleName = this.network.getSimpleName();
    _builder.append(_simpleName, "");
    _builder.newLineIfNotEmpty();
    _builder.append("// ----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.newLine();
    _builder.append("You have generated code using the Xronos synthesizable SystemC Code generation");
    _builder.newLine();
    _builder.newLine();
    _builder.append("Xronos generates three folders src, rtl and testbench");
    _builder.newLine();
    _builder.newLine();
    _builder.append("- src, ");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("is where your generated code is stored");
    _builder.newLine();
    _builder.newLine();
    _builder.append("- rtl, ");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("is the place when all the synthesizable code ");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("after HLS synthesis(Catapult, Cynthesizer, Vivado HLS, ...)");
    _builder.newLine();
    _builder.newLine();
    _builder.append("- tesbench, ");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("is the place where all SystemC testbenches are stored");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("- traces,");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("where you should put all the traces, golden references");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("provided by Orcc C backend or the Orcc simulator, a ");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("refactoring of the traces name should be given ");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("- network,");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("where the network testbench is located");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("- actors,");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("where all the actor testbenched are located");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.newLine();
    _builder.append("- scripts,");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("where all tcl scipts for launching the HLS synthesis and simulation");
    _builder.newLine();
    _builder.newLine();
    _builder.append("Tools that you need:");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("- G++ or Clnag");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("- SystemC 2.3.1 or later, for abstract simulation of the generated code");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("- Vivado HLS 2014.3 or later for HLS synthesis (Vivado System Edition or");
    _builder.newLine();
    _builder.append("\t   ");
    _builder.append("Web Edition)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("- Calypto Catapult, Cynthesizer, CyberWorkBench, C-To-Verilog or other Tool");
    _builder.newLine();
    _builder.newLine();
    _builder.append("We provide scripts only for Vivado HLS for the moment, if other tool tested, ");
    _builder.newLine();
    _builder.append("we are going to add the necessary tcl scripts.");
    _builder.newLine();
    _builder.newLine();
    _builder.newLine();
    _builder.append("HowTo Vivado HLS :");
    _builder.newLine();
    _builder.newLine();
    _builder.append("Launch Vivado HLS from command line");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("- On Linux : Open the command prompt");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("- On Windows : On start menu choose Vivado HLS \"Version\" Command Prompt");
    _builder.newLine();
    _builder.newLine();
    _builder.append("On the terminal type");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("> vivado_hls tcl_\"name of the top or actor\".tcl");
    _builder.newLine();
    _builder.newLine();
    _builder.append("This script will launch the SystemC simulation for verification, Synthesis, ");
    _builder.newLine();
    _builder.append("Co-Simulation and will export the design.");
    _builder.newLine();
    _builder.newLine();
    _builder.append("If you would like another clk period than 10ns, and not all the steps described ");
    _builder.newLine();
    _builder.append("above you can comment with \"#\" the necessary lines. ");
    _builder.newLine();
    _builder.newLine();
    _builder.newLine();
    _builder.append("For all questions on SystemC code generator please contact :");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("- endri.bezati@epfl.ch");
    _builder.newLine();
    _builder.newLine();
    return _builder;
  }
}
