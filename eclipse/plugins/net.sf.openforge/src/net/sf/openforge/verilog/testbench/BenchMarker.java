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

package net.sf.openforge.verilog.testbench;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Task;
import net.sf.openforge.verilog.model.Always;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Compare;
import net.sf.openforge.verilog.model.ConditionalStatement;
import net.sf.openforge.verilog.model.Constant;
import net.sf.openforge.verilog.model.EventControl;
import net.sf.openforge.verilog.model.EventExpression;
import net.sf.openforge.verilog.model.FStatement;
import net.sf.openforge.verilog.model.InitialBlock;
import net.sf.openforge.verilog.model.IntegerWire;
import net.sf.openforge.verilog.model.Math;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.ProceduralTimingBlock;
import net.sf.openforge.verilog.model.RealWire;
import net.sf.openforge.verilog.model.SequentialBlock;
import net.sf.openforge.verilog.model.StringStatement;
import net.sf.openforge.verilog.pattern.CommaDelimitedStatement;

/**
 * BenchMarker add benchmarking features to the automatic testbench (atb) The
 * dumped out benchmark file (design).benchmarks.report file will contain
 * information like input/output/core utilization , zone utilization etc.
 * 
 * <p>
 * Created: Wed Jan 8 15:43:29 2003
 * 
 * @author gandhij, last modified by $Author: imiller $
 * @version $Id: BenchMarker.java 2 2005-06-09 20:00:48Z imiller $
 */

class BenchMarker {

	// The benchmark report file //
	private SimFileHandle reportFile;

	// benchmark related variables //
	IntegerWire benchClockCount;
	IntegerWire benchNumReads;
	IntegerWire benchConsumedBytes;
	IntegerWire benchNumWrites;
	IntegerWire benchProducedBytes;
	IntegerWire benchFirstReadCycle;
	IntegerWire benchLastWriteCycle;
	IntegerWire benchWorkInProgress;

	IntegerWire benchTotalCycles;
	IntegerWire benchCoreCyclesCounter;
	IntegerWire benchCoreCycles;
	IntegerWire benchIdleCyclesCounter;
	IntegerWire benchIdleCycles;
	IntegerWire benchIdleFlag;

	IntegerWire benchInReadZone;
	IntegerWire benchInWriteZone;
	IntegerWire benchInCoreZone;

	IntegerWire benchReadZoneCycles;
	IntegerWire benchReadZoneCyclesCounter;
	IntegerWire benchReadZoneCyclesCounterSave;

	IntegerWire benchWriteZoneCycles;
	IntegerWire benchWriteZoneCyclesCounter;
	IntegerWire benchWriteZoneCyclesCounterSave;

	IntegerWire benchCoreZoneCycles;
	IntegerWire benchCoreZoneCyclesCounter;

	RealWire benchThroughput;
	RealWire benchIdlePercentage;
	RealWire benchOverallInputUtil;
	RealWire benchOverallOutputUtil;
	RealWire benchOverallCoreUtil;
	RealWire benchZoneInputUtil;
	RealWire benchZoneOutputUtil;
	RealWire benchZoneCoreUtil;

	/**
	 * 
	 */
	public BenchMarker(File benchmarkFile) {

		reportFile = new SimFileHandle(benchmarkFile, "benchmarkReportFile");

		benchClockCount = new IntegerWire("benchClockCount", 32);
		benchNumReads = new IntegerWire("benchNumReads", 32);
		benchConsumedBytes = new IntegerWire("benchConsumedBytes", 32);
		benchNumWrites = new IntegerWire("benchNumWrites", 32);
		benchProducedBytes = new IntegerWire("benchProducedBytes", 32);
		benchFirstReadCycle = new IntegerWire("benchFirstReadCycle", 32);
		benchLastWriteCycle = new IntegerWire("benchLastWriteCycle", 32);
		benchWorkInProgress = new IntegerWire("benchWorkInProgress", 32);
		benchTotalCycles = new IntegerWire("benchTotalCycles", 32);
		benchCoreCyclesCounter = new IntegerWire("benchCoreCyclesCounter", 32);
		benchCoreCycles = new IntegerWire("benchCoreCycles", 32);
		benchIdleCyclesCounter = new IntegerWire("benchIdleCyclesCounter", 32);
		benchIdleCycles = new IntegerWire("benchIdleCycles", 32);
		benchIdleFlag = new IntegerWire("benchIdleFlag", 32);
		benchInReadZone = new IntegerWire("benchInReadZone", 32);
		benchInWriteZone = new IntegerWire("benchInWriteZone", 32);
		benchInCoreZone = new IntegerWire("benchInCoreZone", 32);
		benchReadZoneCycles = new IntegerWire("benchReadZoneCycles", 32);
		benchReadZoneCyclesCounter = new IntegerWire(
				"benchReadZoneCyclesCounter", 32);
		benchReadZoneCyclesCounterSave = new IntegerWire(
				"benchReadZoneCyclesCounterSave", 32);
		benchWriteZoneCycles = new IntegerWire("benchWriteZoneCycles", 32);
		benchWriteZoneCyclesCounter = new IntegerWire(
				"benchWriteZoneCyclesCounter", 32);
		benchWriteZoneCyclesCounterSave = new IntegerWire(
				"benchWriteZoneCyclesCounterSave", 32);
		benchCoreZoneCycles = new IntegerWire("benchCoreZoneCycles", 32);
		benchCoreZoneCyclesCounter = new IntegerWire(
				"benchCoreZoneCyclesCounter", 32);
		// benchArgSizeBits = new IntegerWire("benchArgSizeBits", 1);
		// benchResSizeBits = new IntegerWire("benchResSizeBits", 1);
		benchThroughput = new RealWire("benchThroughput", 32);
		benchIdlePercentage = new RealWire("benchIdlePercentage", 32);
		benchOverallInputUtil = new RealWire("benchOverallInputUtil", 32);
		benchOverallOutputUtil = new RealWire("benchOverallOutputUtil", 32);
		benchOverallCoreUtil = new RealWire("benchOverallCoreUtil", 32);
		benchZoneInputUtil = new RealWire("benchZoneInputUtil", 32);
		benchZoneOutputUtil = new RealWire("benchZoneOutputUtil", 32);
		benchZoneCoreUtil = new RealWire("benchZoneCoreUtil", 32);

	}

	/**
	 * Intialize the values for the various benchmark related variables...
	 */
	public void stateInits(InitialBlock ib) {
		ib.add(new Assign.Blocking(benchClockCount, new Constant(0,
				benchClockCount.getWidth())));
		ib.add(new Assign.Blocking(benchNumReads, new Constant(0, benchNumReads
				.getWidth())));
		ib.add(new Assign.Blocking(benchConsumedBytes, new Constant(0,
				benchConsumedBytes.getWidth())));
		ib.add(new Assign.Blocking(benchNumWrites, new Constant(0,
				benchNumWrites.getWidth())));
		ib.add(new Assign.Blocking(benchProducedBytes, new Constant(0,
				benchProducedBytes.getWidth())));
		ib.add(new Assign.Blocking(benchFirstReadCycle, new Constant(0,
				benchFirstReadCycle.getWidth())));
		ib.add(new Assign.Blocking(benchLastWriteCycle, new Constant(0,
				benchLastWriteCycle.getWidth())));
		ib.add(new Assign.Blocking(benchWorkInProgress, new Constant(0,
				benchWorkInProgress.getWidth())));
		ib.add(new Assign.Blocking(benchTotalCycles, new Constant(0,
				benchTotalCycles.getWidth())));
		ib.add(new Assign.Blocking(benchCoreCyclesCounter, new Constant(0,
				benchCoreCyclesCounter.getWidth())));
		ib.add(new Assign.Blocking(benchCoreCycles, new Constant(0,
				benchCoreCycles.getWidth())));
		ib.add(new Assign.Blocking(benchIdleCyclesCounter, new Constant(0,
				benchIdleCyclesCounter.getWidth())));
		ib.add(new Assign.Blocking(benchIdleCycles, new Constant(0,
				benchIdleCycles.getWidth())));
		ib.add(new Assign.Blocking(benchIdleFlag, new Constant(0, benchIdleFlag
				.getWidth())));
		ib.add(new Assign.Blocking(benchInReadZone, new Constant(0,
				benchInReadZone.getWidth())));
		ib.add(new Assign.Blocking(benchInWriteZone, new Constant(0,
				benchInWriteZone.getWidth())));
		ib.add(new Assign.Blocking(benchInCoreZone, new Constant(0,
				benchInCoreZone.getWidth())));
		ib.add(new Assign.Blocking(benchReadZoneCycles, new Constant(0,
				benchReadZoneCycles.getWidth())));
		ib.add(new Assign.Blocking(benchReadZoneCyclesCounter, new Constant(0,
				benchReadZoneCyclesCounter.getWidth())));
		ib.add(new Assign.Blocking(benchReadZoneCyclesCounterSave,
				new Constant(0, benchReadZoneCyclesCounterSave.getWidth())));
		ib.add(new Assign.Blocking(benchWriteZoneCycles, new Constant(0,
				benchWriteZoneCycles.getWidth())));
		ib.add(new Assign.Blocking(benchWriteZoneCyclesCounter, new Constant(0,
				benchWriteZoneCyclesCounter.getWidth())));
		ib.add(new Assign.Blocking(benchWriteZoneCyclesCounterSave,
				new Constant(0, benchWriteZoneCyclesCounterSave.getWidth())));
		ib.add(new Assign.Blocking(benchCoreZoneCycles, new Constant(0,
				benchCoreZoneCycles.getWidth())));
		ib.add(new Assign.Blocking(benchCoreZoneCyclesCounter, new Constant(0,
				benchCoreZoneCyclesCounter.getWidth())));
		// ib.add(new Assign.Blocking(this.benchArgSizeBits, new Constant(1,
		// this.benchArgSizeBits.getWidth())));
		// ib.add(new Assign.Blocking(this.benchResSizeBits, new Constant(1,
		// this.benchResSizeBits.getWidth())));

		ib.add(new Assign.Blocking(benchThroughput, new Constant(0,
				benchThroughput.getWidth())));
		ib.add(new Assign.Blocking(benchIdlePercentage, new Constant(0,
				benchIdlePercentage.getWidth())));
		ib.add(new Assign.Blocking(benchOverallInputUtil, new Constant(0,
				benchOverallInputUtil.getWidth())));
		ib.add(new Assign.Blocking(benchOverallOutputUtil, new Constant(0,
				benchOverallOutputUtil.getWidth())));
		ib.add(new Assign.Blocking(benchOverallCoreUtil, new Constant(0,
				benchOverallCoreUtil.getWidth())));
		ib.add(new Assign.Blocking(benchZoneInputUtil, new Constant(0,
				benchZoneInputUtil.getWidth())));
		ib.add(new Assign.Blocking(benchZoneOutputUtil, new Constant(0,
				benchZoneOutputUtil.getWidth())));
		ib.add(new Assign.Blocking(benchZoneCoreUtil, new Constant(0,
				benchZoneCoreUtil.getWidth())));

		reportFile.stateInits(ib);
	}

	public void stateLogic(Module module, StateMachine mach, Design design) {
		/*
		 * if(allGos) begin <statements> end
		 */
		SequentialBlock AllGos = new SequentialBlock();
		ConditionalStatement ifAllGos = new ConditionalStatement(
				mach.getAllGoWire(), AllGos);

		// benchNumReads = benchNumReads + 1; //
		AllGos.add(new Assign.Blocking(benchNumReads,
				new net.sf.openforge.verilog.model.Math.Add(benchNumReads,
						new Constant(1, benchNumReads.getWidth()))));
		// benchInReadZone = 1; //
		AllGos.add(new Assign.Blocking(benchInReadZone, new Constant(1,
				benchInReadZone.getWidth())));

		/*
		 * if(benchWorkInProgress === 0) begin benchWorkInProgress = 1;
		 * benchFirstReadCycle = benchClockCount ; end
		 */
		SequentialBlock bwp = new SequentialBlock();
		bwp.add(new Assign.Blocking(benchWorkInProgress, new Constant(1,
				benchInReadZone.getWidth())));
		bwp.add(new Assign.Blocking(benchFirstReadCycle, benchClockCount));

		Compare.EQ eq1 = new Compare.EQ(benchWorkInProgress, new Constant(0,
				benchWorkInProgress.getWidth()));
		ConditionalStatement ifbwp = new ConditionalStatement(eq1, bwp);
		AllGos.add(ifbwp);

		// benchReadZoneCyclesCounterSave = benchReadZoneCyclesCounter;
		AllGos.add(new Assign.Blocking(benchReadZoneCyclesCounterSave,
				benchReadZoneCyclesCounter));
		// benchCoreCyclesCounter = 0;
		AllGos.add(new Assign.Blocking(benchCoreCyclesCounter, new Constant(0,
				benchCoreCyclesCounter.getWidth())));

		/*
		 * if(benchInWriteZone === 1) begin benchWriteZoneCycles =
		 * benchWriteZoneCycles + benchWrietZoneCyclesCounterSave + 1;
		 * benchWriteZoneCYlcesCounter = 0; benchInWriteZone = 0; end
		 */
		SequentialBlock bwz = new SequentialBlock();
		bwz.add(new Assign.Blocking(benchWriteZoneCycles, new Math.Add(
				new Math.Add(benchWriteZoneCycles,
						benchWriteZoneCyclesCounterSave), new Constant(1, 32))));
		bwz.add(new Assign.Blocking(benchWriteZoneCyclesCounter, new Constant(
				0, benchWriteZoneCyclesCounter.getWidth())));
		bwz.add(new Assign.Blocking(benchInWriteZone, new Constant(0,
				benchInWriteZone.getWidth())));
		Compare.EQ eq2 = new Compare.EQ(benchInWriteZone, new Constant(1,
				benchInWriteZone.getWidth()));
		ConditionalStatement ifbwz = new ConditionalStatement(eq2, bwz);
		AllGos.add(ifbwz);

		/*
		 * if(benchIdleFlag == 1) begin benchIdleCycles = benchIdleCycles +
		 * benchIdleCyclesCounter - 1; benchIdleFlag = 0; end
		 */
		SequentialBlock bif = new SequentialBlock();
		bif.add(new Assign.Blocking(benchIdleCycles, new Math.Subtract(
				new Math.Add(benchIdleCycles, benchIdleCyclesCounter),
				new Constant(1, benchIdleCycles.getWidth()))));
		bif.add(new Assign.Blocking(benchIdleFlag, new Constant(0,
				benchIdleFlag.getWidth())));
		Compare.EQ eq3 = new Compare.EQ(benchIdleFlag, new Constant(1,
				benchIdleFlag.getWidth()));
		ConditionalStatement ifbif = new ConditionalStatement(eq3, bif);
		AllGos.add(ifbif);

		/*
		 * if(allDones) begin <statements> end
		 */
		SequentialBlock AllDones = new SequentialBlock();
		ConditionalStatement ifAllDones = new ConditionalStatement(
				mach.getAllDoneWire(), AllDones);

		// benchNumWrites = benchNumWrites + 1; //
		AllDones.add(new Assign.Blocking(benchNumWrites, new Math.Add(
				benchNumWrites, new Constant(1, benchNumWrites.getWidth()))));
		// benchInWriteZone = 1; //
		AllDones.add(new Assign.Blocking(benchInWriteZone, new Constant(1,
				benchInWriteZone.getWidth())));

		/*
		 * if(benchInReadZone === 1) begin benchReadZoneCycles =
		 * benchReadZoneCycles + benchReadZoneCyclesCounterSave + 1;
		 * benchReadZoneCyclesCounter = 0; benchInReadZone = 0;
		 * benchWriteZoneCyclesCounter = 0; benchCoreCycles = benchCoreCycles +
		 * benchCoreCyclesCounetr end
		 */
		SequentialBlock brz = new SequentialBlock();
		brz.add(new Assign.Blocking(benchReadZoneCycles, new Math.Add(
				new Math.Add(benchReadZoneCycles,
						benchReadZoneCyclesCounterSave), new Constant(1,
						benchReadZoneCycles.getWidth()))));
		brz.add(new Assign.Blocking(benchReadZoneCyclesCounter, new Constant(0,
				benchReadZoneCyclesCounter.getWidth())));
		brz.add(new Assign.Blocking(benchInReadZone, new Constant(0,
				benchInReadZone.getWidth())));
		brz.add(new Assign.Blocking(benchWriteZoneCyclesCounter, new Constant(
				0, benchWriteZoneCyclesCounter.getWidth())));
		brz.add(new Assign.Blocking(benchCoreCycles, new Math.Add(
				benchCoreCycles, benchCoreCyclesCounter)));
		Compare.EQ eq4 = new Compare.EQ(benchInReadZone, new Constant(1,
				benchInReadZone.getWidth()));
		ConditionalStatement ifbrz = new ConditionalStatement(eq4, brz);
		AllDones.add(ifbrz);

		/*
		 * if(allGos==1) begin benchCoreCycles = benchCoreCycles + 1; end
		 */
		AllDones.add(new ConditionalStatement(new Compare.EQ(mach
				.getAllGoWire(), new Constant(1, 1)), new Assign.Blocking(
				benchCoreCycles, new Math.Add(benchCoreCycles, new Constant(1,
						benchCoreCycles.getWidth())))));

		// benchWriteZoneCyclesCounterSave = benchWriteZoneCyclesCounter;
		AllDones.add(new Assign.Blocking(benchWriteZoneCyclesCounterSave,
				benchWriteZoneCyclesCounter));
		// benchLastWriteCycle = benchClockCount + 1;
		AllDones.add(new Assign.Blocking(benchLastWriteCycle, new Math.Add(
				benchClockCount, new Constant(1, benchClockCount.getWidth()))));
		// benchIdleFlag = 1;
		AllDones.add(new Assign.Blocking(benchIdleFlag, new Constant(1,
				benchIdleFlag.getWidth())));
		// benchIdleCyclesCounter = 0;
		AllDones.add(new Assign.Blocking(benchIdleCyclesCounter, new Constant(
				0, benchIdleCyclesCounter.getWidth())));

		/*
		 * if(done) begin <statements> end
		 */
		SequentialBlock done = new SequentialBlock();
		ConditionalStatement ifdone = new ConditionalStatement(mach.getDone(),
				done);

		// benchWriteZoneCycles = benchWriteZoneCycles +
		// benchWriteZoneCyclesCounterSave + 1;
		done.add(new Assign.Blocking(benchWriteZoneCycles, new Math.Add(
				new Math.Add(benchWriteZoneCycles,
						benchWriteZoneCyclesCounterSave), new Constant(1,
						benchWriteZoneCycles.getWidth()))));
		// benchTotalCycles = benchLastWriteCycle - benchFirstReadCycle;
		done.add(new Assign.Blocking(benchTotalCycles, new Math.Subtract(
				benchLastWriteCycle, benchFirstReadCycle)));

		// Find the number of input bytes and number of outputbytes //
		Collection<Task> tasks = design.getTasks();
		// System.out.println("Number of tasks = " + tasks.size());
		int inputsize = 0, outputsize = 0;
		Iterator<Task> iter = tasks.iterator();
		while (iter.hasNext()) {
			Task task = iter.next();
			Call call = task.getCall();

			for (Port port : call.getDataPorts()) {
				inputsize += port.getSize();
			}

			// System.out.println("Input Size Bits = " + inputsize);

			for (Bus bus : call.getExit(Exit.DONE).getDataBuses()) {
				outputsize += bus.getSize();
			}

			// System.out.println("Output Size Bits = " + outputsize);
		}
		Constant benchArgSizeBytes = new Constant(inputsize / 8, 32);
		Constant benchResSizeBytes = new Constant(outputsize / 8, 32);

		Constant hunderd = new Constant(100, 32);

		// benchThroughput = (benchNumReads *
		// benchArgSizeBits/8.0)/(benchTotalCycles);
		done.add(new Assign.Blocking(benchThroughput, new Math.Divide(
				new Math.Multiply(benchNumReads, benchArgSizeBytes),
				benchTotalCycles)));
		// benchOverallInputUtil = (benchNumReads / benchTotalCycles) * 100.0;
		done.add(new Assign.Blocking(benchOverallInputUtil, new Math.Multiply(
				new Math.Divide(benchNumReads, benchTotalCycles), hunderd)));

		// benchOverallOutputUtil = (benchNumWrites / benchTotalCycles) * 100.0;
		done.add(new Assign.Blocking(benchOverallOutputUtil, new Math.Multiply(
				new Math.Divide(benchNumWrites, benchTotalCycles), hunderd)));

		// benchOverallCoreUtil = (benchCoreCycles / benchTotalCycles) * 100.0;
		done.add(new Assign.Blocking(benchOverallCoreUtil, new Math.Multiply(
				new Math.Divide(benchCoreCycles, benchTotalCycles), hunderd)));

		// benchIdlePercentage = (benchIdleCycles / benchTotalCycles) * 100.0;
		done.add(new Assign.Blocking(benchIdlePercentage, new Math.Multiply(
				new Math.Divide(benchIdleCycles, benchTotalCycles), hunderd)));

		// benchZoneInputUtil = (benchNumReads/benchReadZoneCycles) * 100.0;
		done.add(new Assign.Blocking(benchZoneInputUtil, new Math.Multiply(
				new Math.Divide(benchNumReads, benchReadZoneCycles), hunderd)));

		// benchZoneOutputUtil = (benchNumWrites/benchWriteZoneCycles) * 100.0;
		done.add(new Assign.Blocking(benchZoneOutputUtil, new Math.Multiply(
				new Math.Divide(benchNumWrites, benchWriteZoneCycles), hunderd)));

		// benchZoneCoreUtil = (benchCoreCycles/benchCoreCycles) * 100.0;
		done.add(new Assign.Blocking(benchZoneCoreUtil, new Math.Multiply(
				new Math.Divide(benchCoreCycles, benchCoreCycles), hunderd)));

		// WUSIWUG ! //
		CommaDelimitedStatement cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"**********************************************************\\n"));
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"******************** BENCHMARK REPORT ********************\\n"));
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"**********************************************************\\n"));
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Total Number of Cycles               : %0d\n", benchClockCount);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Total Number of Cycles               : %0d\\n"));
		cds.append(benchClockCount);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Data Crunching Cycles                : %0d\n", benchTotalCycles);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Data Crunching Cycles                : %0d\\n"));
		cds.append(benchTotalCycles);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Number of Idle Cycles                : %0d\n", benchIdleCycles);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Number of Idle Cycles                : %0d\\n"));
		cds.append(benchIdleCycles);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Number of Reads                      : %0d\n", benchNumReads);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Number of Reads                      : %0d\\n"));
		cds.append(benchNumReads);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Bytes Consumed                       : %0g\n", benchNumReads *
		// benchArgSizeBits/8.0);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Bytes Consumed                       : %0d\\n"));
		cds.append(new Math.Multiply(benchNumReads, benchArgSizeBytes));
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Number of Writes                     : %0d\n", benchNumWrites);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Number of Writes                     : %0d\\n"));
		cds.append(benchNumWrites);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Bytes Produced                       : %0g\n", benchNumWrites *
		// benchResSizeBits/8.0);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Bytes Produced                       : %0d\\n"));
		cds.append(new Math.Multiply(benchNumWrites, benchResSizeBytes));
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "First Read Cycle                     : %0d\n", benchFirstReadCycle);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"First Read Cycle                     : %0d\\n"));
		cds.append(benchFirstReadCycle);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Last Write Cycle                     : %0d\n", benchLastWriteCycle);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Last Write Cycle                     : %0d\\n"));
		cds.append(benchLastWriteCycle);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Throughput(Bytes Consumed/Cycle)     : %0g\n", benchThroughput);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Throughput(Bytes Consumed/Cycle)     : %0g\\n"));
		cds.append(benchThroughput);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "****** Overall *******************************************\n");
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"****** Overall *******************************************\\n"));
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		// $fwrite (benchmarkReportFile,
		// "Overall Input Link Utilization       : %0g\n",
		// benchOverallInputUtil);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Overall Input Link Utilization       : %0g\\n"));
		cds.append(benchOverallInputUtil);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		// $fwrite (benchmarkReportFile,
		// "Overall Core Utilization             : %0g\n",
		// benchOverallCoreUtil);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Overall Core Utilization             : %0g\\n"));
		cds.append(benchOverallCoreUtil);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		// $fwrite (benchmarkReportFile,
		// "Overall Output Link Utilization      : %0g\n",
		// benchOverallOutputUtil);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Overall Output Link Utilization      : %0g\\n"));
		cds.append(benchOverallOutputUtil);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		// $fwrite (benchmarkReportFile,
		// "Percentage of Idle Cycles            : %0g\n", benchIdlePercentage);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Percentage of Idle Cycles            : %0g\\n"));
		cds.append(benchIdlePercentage);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		// $fwrite (benchmarkReportFile,
		// "******** Zonal *******************************************\n");
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"******** Zonal *******************************************\\n"));
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		// $fwrite (benchmarkReportFile,
		// "Zone Input Link Utilization          : %0g\n", benchZoneInputUtil);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Zone Input Link Utilization          : %0g\\n"));
		cds.append(benchZoneInputUtil);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		// $fwrite (benchmarkReportFile,
		// "Zone Core Utilization                : %0g\n", benchZoneCoreUtil);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Zone Core Utilization                : %0g\\n"));
		cds.append(benchZoneCoreUtil);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		// $fwrite (benchmarkReportFile,
		// "Zone Output Link Utilization         : %0g\n", benchZoneOutputUtil);
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"Zone Output Link Utilization         : %0g\\n"));
		cds.append(benchZoneOutputUtil);
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));
		// $fwrite (benchmarkReportFile,
		// "**********************************************************\n");
		cds = new CommaDelimitedStatement();
		cds.append(new StringStatement(
				"**********************************************************\\n"));
		done.add(new FStatement.FWrite(reportFile.getHandle(), cds));

		SequentialBlock sblock = new SequentialBlock();
		sblock.add(ifAllGos);
		sblock.add(ifAllDones);
		sblock.add(ifdone);

		// benchClockCount = benchClockCount + 1;
		sblock.add(new Assign.Blocking(benchClockCount, new Math.Add(
				benchClockCount, new Constant(1, benchClockCount.getWidth()))));
		// benchIdleCyclesCounter = benchIdleFlag == 1 ? benchIdleCyclesCounter
		// + 1 : benchIdleCyclesCounter;
		sblock.add(new ConditionalStatement(new Compare.EQ(benchIdleFlag,
				new Constant(1, benchIdleFlag.getWidth())),
				new Assign.Blocking(benchIdleCyclesCounter, new Math.Add(
						benchIdleCyclesCounter, new Constant(1,
								benchIdleCyclesCounter.getWidth()))),
				new Assign.Blocking(benchIdleCyclesCounter,
						benchIdleCyclesCounter)));
		// benchCoreCyclesCounter = benchCoreCyclesCounter + 1;
		sblock.add(new Assign.Blocking(benchCoreCyclesCounter, new Math.Add(
				benchCoreCyclesCounter, new Constant(1, benchCoreCyclesCounter
						.getWidth()))));
		// benchReadZoneCyclesCounter = benchInReadZone == 1 ?
		// benchReadZoneCyclesCounter + 1 : benchReadZoneCyclesCounter;
		sblock.add(new ConditionalStatement(new Compare.EQ(benchInReadZone,
				new Constant(1, benchInReadZone.getWidth())),
				new Assign.Blocking(benchReadZoneCyclesCounter, new Math.Add(
						benchReadZoneCyclesCounter, new Constant(1,
								benchReadZoneCyclesCounter.getWidth()))),
				new Assign.Blocking(benchReadZoneCyclesCounter,
						benchReadZoneCyclesCounter)));
		// benchWriteZoneCyclesCounter = benchInWriteZone == 1 ?
		// benchWriteZoneCyclesCounter + 1 : benchWriteZoneCyclesCounter;
		sblock.add(new ConditionalStatement(new Compare.EQ(benchInWriteZone,
				new Constant(1, benchInWriteZone.getWidth())),
				new Assign.Blocking(benchWriteZoneCyclesCounter, new Math.Add(
						benchWriteZoneCyclesCounter, new Constant(1,
								benchWriteZoneCyclesCounter.getWidth()))),
				new Assign.Blocking(benchWriteZoneCyclesCounter,
						benchWriteZoneCyclesCounter)));

		ProceduralTimingBlock ptb = new ProceduralTimingBlock(new EventControl(
				new EventExpression.PosEdge(mach.getClock())), sblock);

		module.state(new Always(ptb));

	}

}
