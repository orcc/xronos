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
#ifndef __EMBEDDED_FIFO_H__
#define __EMBEDDED_FIFO_H__

#include <string.h>

#include "barrier.h"

/*! \class Fifo fifo.h
 *  \brief A template class that implements a non-bocking ring buffer.
 */
template <typename T, int nb_reader>
class Fifo
{
public:
	Fifo(int size=4096, int threshold=1024);
	~Fifo();

	T* write_address() const;

	void write_advance();

	void write_advance(unsigned int nb_data);

	T* read_address(int reader_id) const;

	T* read_address(int reader_id, unsigned nb_data);

	void read_advance(int reader_id, unsigned int nb_data=1);
	
	unsigned int count(int reader_id) const
	{
		return (size + wr_ptr - rd_ptr[reader_id]) & (size - 1);
	}

	unsigned int rooms() const
	{
		unsigned int min_rooms = 0xFFFFFFFF;
		for (int i = 0; i < nb_reader; i++) {
			unsigned int rooms = (size + rd_ptr[i] - wr_ptr - 1) & (size - 1);
			min_rooms = min_rooms < rooms ? min_rooms : rooms;
		}
		return min_rooms;
	}

private:
	T * buffer;

	unsigned int rd_ptr[nb_reader];

	unsigned int wr_ptr;

	unsigned int size;
};


template <typename T, int nb_reader>
Fifo<T, nb_reader>::Fifo(int size, int threshold)
	: buffer(new T[size + threshold])
	, wr_ptr(0)
	, size(size) 
{
	for(int i=0; i<nb_reader; i++)
		rd_ptr[i] = 0;
}

template <typename T, int nb_reader>
Fifo<T, nb_reader>::~Fifo()
{
	delete [] buffer;
}

template <typename T, int nb_reader>
inline T* Fifo<T, nb_reader>::write_address() const
{
	return buffer + wr_ptr;
}

template <typename T, int nb_reader>
void Fifo<T, nb_reader>::write_advance()
{
	++ wr_ptr;
	wr_ptr &= (size - 1);
}

template <typename T, int nb_reader>
void Fifo<T, nb_reader>::write_advance(unsigned int nb_val)
{
	int rest = wr_ptr + nb_val - size;
	if(rest > 0)
	{
		memcpy(buffer, buffer + size, rest*sizeof(T));
	}
	wr_ptr += nb_val;
	wr_ptr &= (size - 1);
}

template <typename T, int nb_reader>
inline T* Fifo<T, nb_reader>::read_address(int reader_id) const
{
	return buffer + rd_ptr[reader_id];
}

template <typename T, int nb_reader>
inline T* Fifo<T, nb_reader>::read_address(int reader_id, unsigned uNbVal)
{
	T * pVal = buffer + rd_ptr[reader_id];
	int rest = rd_ptr[reader_id] + uNbVal - size;
	if(rest > 0)
	{
		memcpy(buffer + size, buffer, rest*sizeof(T)); 
	} 
	return pVal;
}

template <typename T, int nb_reader>
void Fifo<T, nb_reader>::read_advance(int reader_id, unsigned int nb_val)
{
	rd_ptr[reader_id] += nb_val;
	rd_ptr[reader_id] &= (size - 1);
}

#endif

