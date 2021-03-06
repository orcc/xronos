/**
 * ----------------------------------------------------------------------------
 * __  ___ __ ___  _ __   ___  ___
 * \ \/ / '__/ _ \| '_ \ / _ \/ __|
 *  >  <| | | (_) | | | | (_) \__ \
 * /_/\_\_|  \___/|_| |_|\___/|___/
 * ----------------------------------------------------------------------------
 * Copyright (C) 2015 EPFL SCI STI MM
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
 *
 */


// ------------------------------------------------------------
// PMU for Cortex-A/R (v7-A/R)
// ------------------------------------------------------------

#ifndef _V7_PMU_H
#define _V7_PMU_H

// Returns the number of progammable counters
extern "C" unsigned int getPMN(void);

// Sets the event for a programmable counter to record
// counter = r0 = Which counter to program  (e.g. 0 for PMN0, 1 for PMN1)
// event   = r1 = The event code (from appropiate TRM or ARM Architecture Reference Manual)
extern "C" void pmn_config(unsigned int counter, unsigned int event);

// Enables/disables the divider (1/64) on CCNT
// divider = r0 = If 0 disable divider, else enable dvider
extern "C" void ccnt_divider(int divider);

//
// Enables and disables
//

// Global PMU enable
// On ARM11 this enables the PMU, and the counters start immediately
// On Cortex this enables the PMU, there are individual enables for the counters
extern "C" void enable_pmu(void);

// Global PMU disable
// On Cortex, this overrides the enable state of the individual counters
extern "C" void disable_pmu(void);

// Enable the CCNT
extern "C" void enable_ccnt(void);

// Disable the CCNT
extern "C" void disable_ccnt(void);

// Enable PMN{n}
// counter = The counter to enable (e.g. 0 for PMN0, 1 for PMN1)
extern "C" void enable_pmn(unsigned int counter);

// Enable PMN{n}
// counter = The counter to enable (e.g. 0 for PMN0, 1 for PMN1)
extern "C" void disable_pmn(unsigned int counter);

//
// Read counter values
//

// Returns the value of CCNT
extern "C" unsigned int read_ccnt(void);

// Returns the value of PMN{n}
// counter = The counter to read (e.g. 0 for PMN0, 1 for PMN1)
extern "C" unsigned int read_pmn(unsigned int counter);

//
// Overflow and interrupts
//

// Returns the value of the overflow flags
extern "C" unsigned int read_flags(void);

// Writes the overflow flags
extern "C" void write_flags(unsigned int flags);

// Enables interrupt generation on overflow of the CCNT
extern "C" void enable_ccnt_irq(void);

// Disables interrupt generation on overflow of the CCNT
extern "C" void disable_ccnt_irq(void);

// Enables interrupt generation on overflow of PMN{x}
// counter = The counter to enable the interrupt for (e.g. 0 for PMN0, 1 for PMN1)
extern "C" void enable_pmn_irq(unsigned int counter);

// Disables interrupt generation on overflow of PMN{x}
// counter = r0 =  The counter to disable the interrupt for (e.g. 0 for PMN0, 1 for PMN1)
extern "C" void disable_pmn_irq(unsigned int counter);

//
// Counter reset functions
//

// Resets the programmable counters
extern "C" void reset_pmn(void);

// Resets the CCNT
extern "C" void reset_ccnt(void);

//
// Software Increment

// Writes to software increment register
// counter = The counter to increment (e.g. 0 for PMN0, 1 for PMN1)
extern "C" void pmu_software_increment(unsigned int counter);

//
// User mode access
//

// Enables User mode access to the PMU (must be called in a priviledged mode)
extern "C" void enable_pmu_user_access(void);

// Disables User mode access to the PMU (must be called in a priviledged mode)
extern "C" void disable_pmu_user_access(void);


#endif
// ------------------------------------------------------------
// End of v7_pmu.h
// ------------------------------------------------------------
