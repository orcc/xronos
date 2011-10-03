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

package net.sf.openforge.backend.edk;


import java.util.*;
import java.io.*;

import net.sf.openforge.app.*;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.backend.*;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.io.*;

/**
 * This Forge Core Descriptor implements the OutputEngine interface in
 * order to generate a correctly structured EDK project containing the
 * desgin being compiled.  This includes generation of the directory
 * structure, pao and mpd files.
 *
 * The Peripheral Analyze Order (PAO) file contains a list of HDL
 * modules and files that are needed for synthesis, and defines the
 * analyzing order for compilation.
 *
 * A Microprocessor Peripheral Description (MPD) file defines the
 * interface of the peripheral.  
 *
 * <p>Created: Wed Apr  7 13:25:39 2004
 *
 * @author cwu, last modified by $Author: imiller $
 * @version $Id: ForgeCoreDescriptor.java 112 2006-03-21 15:41:57Z imiller $
 */
public class ForgeCoreDescriptor implements OutputEngine
{
    private static final String _RCS_ = "$Rev: 112 $";
    
    private static final String VERSION = "v2_1_0";

    private Design design;

    // File handles for the various directories and files generated
    // for an EDK project.
    public static final ForgeFileKey EDK_ROOT_DIR =  new ForgeFileKey("EDK root dir");
    public static final ForgeFileKey EDK_HDL_DIR =  new ForgeFileKey("EDK HDL dir");
    public static final ForgeFileKey EDK_HDL_VER_DIR =  new ForgeFileKey("EDK HDL Verilog dir");
    public static final ForgeFileKey EDK_REPORT_DIR =  new ForgeFileKey("EDK report dir");
    public static final ForgeFileKey EDK_DATA_DIR =  new ForgeFileKey("EDK data dir");

    private static final ForgeFileKey PAO = new ForgeFileKey("EDK PAO file");
    private static final ForgeFileKey MPD = new ForgeFileKey("EDK MPD file");
    
    public ForgeCoreDescriptor () { }

    public void initEnvironment ()
    {
        // Because we need the design name in order to correctly
        // generate the file names, the files are not registered until
        // they are translated.  This works so long as the keys are
        // not needed by anything else, or are only used by something
        // that is guaranteed to be run after the translation phase of
        // this engine.
        // We do, however, want to initialize the directory structure
        //
        // root
        //   hdl
        //     verilog
        //   report
        //   data
        //     foo.pao
        //     foo.mpd
        //
        
        final GenericJob gj = EngineThread.getGenericJob();
        final ForgeFileHandler fileHandler = gj.getFileHandler();
        
        final String peName = gj.getOption(OptionRegistry.PE_NAME).getValue(CodeLabel.UNSCOPED).toString();
        final String peVersion = gj.getOption(OptionRegistry.PE_VERSION).getValue(CodeLabel.UNSCOPED).toString();

        final File rootDir = fileHandler.registerFile(EDK_ROOT_DIR, peName + "_" + peVersion);
        final File hdlDir = fileHandler.registerFile(EDK_HDL_DIR, rootDir, "hdl");
        fileHandler.registerFile(EDK_HDL_VER_DIR, hdlDir, "verilog");
        fileHandler.registerFile(EDK_REPORT_DIR, rootDir, "report");
        fileHandler.registerFile(EDK_DATA_DIR, rootDir, "data");
    }
    
    public void translate (Design design) throws IOException
    {
        final ForgeFileHandler fileHandler = EngineThread.getGenericJob().getFileHandler();
        final File destDir = fileHandler.getFile(EDK_DATA_DIR);
        fileHandler.getFile(EDK_DATA_DIR).mkdirs();
        fileHandler.getFile(EDK_REPORT_DIR).mkdirs();
        
        this.design = design;

        final String peName = design.showIDLogical();
        final File pao = fileHandler.registerFile(PAO, destDir, peName + "_" + VERSION + ".pao");
        final File mpd = fileHandler.registerFile(MPD, destDir, peName + "_" + VERSION + ".mpd");

        this.generatePAO(pao);
        this.generateMPD(mpd);
    }


    /**
     * Returns a string which uniquely identifies this phase of the
     * compiler output.
     *
     * @return a non-empty, non-null String
     */
    public String getOutputPhaseId () { return "EDK Project Files"; }
    
    /**
     * Generates Peripheral Analyze Order (PAO) file.
     *
     *@param File the directory where the genearted file will be
     *            stored.
     */
    private void generatePAO (File paoFile) throws IOException
    {
    	GenericJob gj = EngineThread.getGenericJob();
        final String peVersion = gj.getOption(OptionRegistry.PE_VERSION).getValue(CodeLabel.UNSCOPED).toString();
        String verilogFileName = gj.getOutputBaseName();

        final String peName = design.showIDLogical();

        /*
        final String paoFileName = peName + "_" + VERSION + ".pao";
        final File paoFile = new File(directory, paoFileName);
        */
        
        final FileOutputStream paoFos = openFile(paoFile);

        final PrintStream ps = new PrintStream(paoFos, true);
        
        ps.println("################################################################################");
        ps.println("##");
        ps.println("## " + paoFile.getName());
        ps.println("##");
        ps.println("## Peripheral Analyze Order");
        ps.println("##");
        ps.println("################################################################################");
        ps.println();
        ps.println("lib " + peName + "_" + peVersion + " " + verilogFileName);
        
        closeFile(paoFile, paoFos);
    }

    /**
     * Genarates the Microprocesser Peripheral Definition (MPD) file.
     *
     *@param File the directory where the genearted file will be
     *            stored.
     */
    private void generateMPD (File mpdFile) throws IOException
    {
        final String peName = design.showIDLogical();

        final FileOutputStream mpdFos = openFile(mpdFile);

        final PrintStream ps = new PrintStream(mpdFos, true);

        ps.println("################################################################################");
        ps.println("##");
        ps.println("## " + mpdFile.getName());
        ps.println("##");
        ps.println("## Microprocessor Peripheral Description");
        ps.println("##");
        ps.println("################################################################################");
        ps.println();
        ps.println("BEGIN " + peName);
        ps.println();
        ps.println("##====================");
        ps.println("## Peripheral Options");
        ps.println("##====================");
        ps.println();
        ps.println("OPTION IPTYPE = PERIPHERAL");
        ps.println("OPTION HDL = VERILOG");
        ps.println();
        
        ps.println("##================");
        ps.println("## Bus Interfaces");
        ps.println("##================");
        ps.println();
        for (Iterator fifoIFIter = design.getFifoInterfaces().iterator(); fifoIFIter.hasNext();)
        {
            FifoIF fifoIF = (FifoIF)fifoIFIter.next();
            if (fifoIF instanceof FifoInput)
            {
                ps.println("BUS_INTERFACE BUS = FSL_IN, BUS_STD = FSL, BUS_TYPE = SLAVE");
            }
            else if (fifoIF instanceof FifoOutput)
            {
                ps.println("BUS_INTERFACE BUS = FSL_OUT, BUS_STD = FSL, BUS_TYPE = MASTER");
            }
        }
        ps.println();
        
        ps.println("##=======");
        ps.println("## Ports");
        ps.println("##=======");
        ps.println();
        ps.println("PORT CLK = \"\", DIR=IN, SIGIS=CLK");
        ps.println();
        for (Iterator fifoIFIter = design.getFifoInterfaces().iterator(); fifoIFIter.hasNext();)
        {
            FifoIF fifoIF = (FifoIF)fifoIFIter.next();
            if (fifoIF instanceof FifoInput)
            {
                for(Iterator pinIter = fifoIF.getPins().iterator(); pinIter.hasNext();)
                {
                    SimplePin pin = (SimplePin)pinIter.next();
                    if (pin.showIDLogical().endsWith("CLK"))
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_S_Clk, DIR=OUT, SIGIS=CLK, BUS=FSL_IN");
                    }
                    else if (pin.showIDLogical().endsWith("READ"))
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_S_Read, DIR=OUT, BUS=FSL_IN");
                    }
                    else if(pin.showIDLogical().endsWith("DATA"))
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_S_Data, DIR=IN, VEC=[0:" + (pin.getWidth() - 1) + "], BUS=FSL_IN");
                    }
                    else if(pin.showIDLogical().endsWith("CONTROL"))
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_S_Control, DIR=IN, BUS=FSL_IN");
                    }
                    else
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_S_Exists, DIR=IN, BUS=FSL_IN");
                    }
                }
                ps.println();
            }
            else if (fifoIF instanceof FifoOutput)
            {
                for(Iterator pinIter = fifoIF.getPins().iterator(); pinIter.hasNext();)
                {
                    SimplePin pin = (SimplePin)pinIter.next();
                    if (pin.showIDLogical().endsWith("CLK"))
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_M_Clk, DIR=OUT, SIGIS=CLK, BUS=FSL_OUT");
                    }
                    else if (pin.showIDLogical().endsWith("WRITE"))
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_M_Write, DIR=OUT, BUS=FSL_OUT");
                    }
                    else if(pin.showIDLogical().endsWith("DATA"))
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_M_Data, DIR=OUT, VEC=[0:" + (pin.getWidth() - 1) + "], BUS=FSL_OUT");
                    }
                    else if(pin.showIDLogical().endsWith("CONTROL"))
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_M_Control, DIR=OUT, BUS=FSL_OUT");
                    }
                    else
                    {
                        ps.println("PORT " + pin.showIDLogical() + " = FSL_M_Full, DIR=IN, BUS=FSL_OUT");
                    }
                }
                ps.println();
            }
        }
        ps.println("END");
        
        closeFile(mpdFile, mpdFos);
    }

    private FileOutputStream openFile (File file) throws IOException
    {
        return new FileOutputStream(file);
    }
    
    private void closeFile (File file, FileOutputStream fos) throws IOException
    {
        fos.flush();
        fos.close();
    }
}

