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

#ifndef __EMBEDDED_GET_OPT_H__
#define __EMBEDDED_GET_OPT_H__

#include <string>
#include <sstream>
#include <vector>
#include <map>

typedef std::map<std::string, std::vector<std::string> > Tokens;
typedef std::map<std::string, std::vector<std::string> >::const_iterator TokensIterator;

extern std::string input_file;
extern std::string write_file;
extern std::string config_file;

extern int nbLoops;
extern int nbFrames;

template<typename T>
inline void convert(const std::string& s, T& res)
{
	std::stringstream ss(s);
	ss >> res;
	if (ss.fail() || !ss.eof())
	{
	}
}

/* specialization for bool */
template <> 
inline void convert(const std::string& s, bool& res)
{
	if(s == "true")
		res = true;
	else if(s == "false")
		res = false;
	else
	{
	}
}

/* specialization for std::string */
template<>
inline void convert(const std::string& s, std::string& res)
{
	res = s;
}

template<typename T> class Options;

class GetOpt
{
public:
	GetOpt(int argc, char* argv[]);
	~GetOpt();

	void parse(int argc, char* argv[]);

	template<typename T> bool getOptionAs(const std::string&, T&);

	const Tokens& getTokens() const {return tokens;};	

	void getOptions();
private:
	Tokens tokens;
};

template<typename T>
bool GetOpt::getOptionAs(const std::string& s, T& res)
{
	return Options<T>(this)(s, res);
}

template<typename T>
class Options
{
public:
	Options<T>(const GetOpt* options) : options(options) {}
	
	bool operator () (const std::string& s, T& res)
	{
		TokensIterator it = options->getTokens().find(s);
		if(it != options->getTokens().end())
		{
			convert<T>((it->second)[0], res);
			return true;
		}
		else
		{
			return false;
		}
	}
private:
	const GetOpt* options;
};

template<typename T>
class Options<std::vector<T> >
{
public:
	Options<std::vector<T> >(const GetOpt* options) : options(options) {}

	void operator () (const std::string& s, std::vector<T>& res)
	{
		Tokens tokens = options->getTokens();
		TokensIterator it = tokens.find(s);
		if(it != tokens.end())
		{
			std::vector<std::string>::const_iterator vec_it;
			for(vec_it = it->second.begin(); vec_it != it->second.end(); vec_it++)
			{
				T item;
				convert<T>(*vec_it, item);
				res.push_back(item);
			}
		}
		else
		{
			// option not found
		}
	}
private:
	const GetOpt* options;

};


#endif
