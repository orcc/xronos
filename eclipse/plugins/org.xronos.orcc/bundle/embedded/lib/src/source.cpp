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

#define _CRT_SECURE_NO_WARNINGS

#include <iostream>
#include <fstream>
#include <string>

#include "get_opt.h"
#include "scheduler.h"

static std::ifstream file;

static int loopsCount;

void source_init() 
{
	if (input_file.empty())
	{
		std::cerr << "No input file given!" << std::endl;
		exit(1);
	}

	file.open(input_file.c_str(), std::ios::binary);
	if (!file.is_open())
	{
		std::cerr << "could not open file "<<  input_file << std::endl;
		exit(1);
	}

	loopsCount = nbLoops;
}

int source_sizeOfFile()
{ 
	file.seekg(0L, std::ios::end);
	long size = file.tellg();
	file.seekg(0L, std::ios::beg);
	return size;
}

void source_rewind()
{
	file.clear();
	file.seekg(0, std::ios::beg);
}

unsigned int source_readByte()
{
	return file.get();
}

void source_readNBytes(unsigned char outTable[], unsigned int nbTokenToRead)
{
	file.read((char *)outTable, nbTokenToRead);
}

unsigned int source_getNbLoop(void)
{
	return nbLoops;
}

void source_decrementNbLoops()
{
	--loopsCount;
}

bool source_isMaxLoopsReached()
{
	return nbLoops != -1 && loopsCount <= 0;
}

void source_exit(int exitCode)
{
	file.close();
	Scheduler* current_thread = (Scheduler*) Thread::currentThread();
	current_thread->done();
}

