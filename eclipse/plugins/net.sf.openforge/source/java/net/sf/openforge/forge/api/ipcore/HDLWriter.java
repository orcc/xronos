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

package net.sf.openforge.forge.api.ipcore;

import java.io.PrintWriter;
import java.util.List;

/**
 * The <code>HDLWriter</code> interface is implemented by a user's class
 * that supports Verilog generation for IP Cores.  Each
 * <code>HDLWriter</code> is registered with the IPCore using the method
 * <code>setWriter()</code> in the <code>IPCore</code> class.  During Verilog
 * generation, forge will call the <code>writeVerilog</code> method on
 * each registered <code>HDLWriter</code> for each uniquely named <code>IPCore</code>
 * included in the user's design.  The <code>writeVerilog</code>
 * method should generate all necessary Verilog code for the IP Core
 * module, including the module interface. 
 */
public interface HDLWriter
{ 
    /**
     *
     * This method is called during translation of
     * Verilog code for each <code>IPCore</code> that registered a
     * <code>HDLWriter</code>.  The <code>targetCore</code> parameter
     * can be used to identify which core is currently being
     * translated if this <code>HDLWriter</code> supports multiple
     * cores.  The <code>printWriter</code> supplied should be used to
     * write all the Verilog for this core since it is already
     * associated with the <code>OutputStream</code> that forge
     * created for the translation output.  Note, the
     * <code>printWriter</code> supplied doesn't support the
     * <code>close</code> method, it is overridden to do nothing;
     * forge will close the output stream at the end of translation.
     * The return value is a <code>java.util.List</code> that contains
     * <code>String</code>s that represent all the Xilinx UniSim
     * library modules required by the written Verilog. Forge will
     * automatically add the appropriate include statements to the
     * output Verilog for simulation and synthesis.  An example entry
     * in the return list might be <code>RAMB16_S1</code> to represent
     * the fact the this written Verilog hard instantiates the Xilinx
     * UniSim library module <code>RAMB16_S1</code>.  If the core
     * doesn't require any UniSim library elements, then an Empty list
     * or <code>null</code> can be returned.  The return list can also
     * contain file paths to be included, these are detected by the
     * use of a directory or absolute path in the
     * <code>String</code>.  Examples of a files are: myinclude/MI.v
     * or /home/me/myinclude/MI.v.  Lastly, if the <code>String</code>
     * is not a file as described above but starts with
     * <code>X_</code> then Forge assumes the element is a Xilinx
     * SimPrim which is a simulatable but not synthesizable element
     * and will prepend the appropriate path.  Elements from the
     * SimPrim library are required when design simulation Verilog
     * code is produced by the ISE ngd2ver program.  If these
     * simulation only netlists are used with Forge, be sure to
     * enclose the Verilog code with // synopsys translate_off and 
     * // synopsys translate_on comments and to attach the black box
     * attribute to the module (i.e. // synthesis attribute BOX_TYPE
     * of mymodule is "BLACK_BOX" ) so the resulting Verilog will
     * simulate and synthesize.  If the BLACK_BOX attribute is used,
     * be sure to copy the appropriate EDIF netlists into the xflow
     * directory so ngdbuild will find them during the link step.
     * <div>NOTE: an environment variable can be used as a the starting part of a
     * file name, it will be expanded by forge (i.e. $XILINX/acme_include/AI.v).
     * <div>NOTE: Forge will only call this method for the first
     * writer it encounters for IPCores with the same name.  If the
     * user registers multiple writers for different IPCores which
     * happen to have the same name, only 1 of the writers will get
     * called during translation.  This prevents multiple Verilog
     * modules of the same name from being written out.
     * @param targetCore the <code>IPCore</code> to write the Verilog for.
     * @param printWriter the <code>PrintWriter</code> to send the output to.
     * @return a <code>java.util.List</code> of <code>String</code>s
     * representing Xilinx UniSim library modules required by the
     * written Verilog. 
     */
    public List<String> writeVerilog(IPCore targetCore, PrintWriter printWriter);
}


