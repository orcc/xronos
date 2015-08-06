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

#include "get_opt.h"

std::string config_file;
std::string input_file;
std::string write_file;

int nbLoops=-1;
int nbFrames=-1;

GetOpt::GetOpt(int argc, char* argv[])
{
	parse(argc, argv);
}

GetOpt::~GetOpt()
{
}

void GetOpt::parse(int argc, char* argv[])
{
	std::vector<std::string> currOptionValues;
	std::string optionName;
	for (int i = 1; i < argc; i++)
	{
		if (argv[i][0] == '-')
		{
			optionName = &argv[i][1];
		}
		else
		{
			tokens[optionName].push_back(&argv[i][0]);
		}
	}
}

void GetOpt::getOptions()
{
	this->getOptionAs<std::string>("i", input_file);
	this->getOptionAs<std::string>("p", config_file);
	this->getOptionAs<std::string>("w", write_file);
	bool exists = this->getOptionAs<int>("l", nbLoops);
	if(!exists)
		nbLoops = -1;
}
