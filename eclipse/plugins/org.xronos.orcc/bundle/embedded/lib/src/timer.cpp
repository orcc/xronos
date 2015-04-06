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

#include <string.h>
#include <algorithm>

#include "timer.h"


Timer::Timer() 
{
	reset();
}

Timer::~Timer() {
}

void Timer::reset() 
{
#if _WIN32
	QueryPerformanceFrequency(&mFrequency);
	QueryPerformanceCounter(&mStartTime);
	mStartTick = GetTickCount();
	mLastTime = 0;
	mZeroClock = clock();
#else
	mZeroClock = clock();
	gettimeofday(&start, NULL);
#endif
}

unsigned long Timer::getMilliseconds() 
{
#ifdef _WIN32
	LARGE_INTEGER curTime;
	QueryPerformanceCounter(&curTime);
	LONGLONG newTime = curTime.QuadPart - mStartTime.QuadPart;

	unsigned long newTicks = (unsigned long) (1000 * newTime / mFrequency.QuadPart);

	unsigned long check = GetTickCount() - mStartTick;
	signed long msecOff = (signed long)(newTicks - check);
	if (msecOff < -100 || msecOff > 100) {
		LONGLONG adjust = (std::min)(msecOff * mFrequency.QuadPart / 1000, newTime - mLastTime);
		mStartTime.QuadPart += adjust;
		newTime -= adjust;

		newTicks = (unsigned long) (1000 * newTime / mFrequency.QuadPart);
	}
	mLastTime = newTime;

	return newTicks;

#else
	struct timeval now;
	gettimeofday(&now, NULL);
	return (now.tv_sec-start.tv_sec)*1000+(now.tv_usec-start.tv_usec)/1000;
#endif
}

unsigned long Timer::getMicroseconds() 
{
#ifdef _WIN32
	LARGE_INTEGER curTime;

	QueryPerformanceCounter(&curTime);


	LONGLONG newTime = curTime.QuadPart - mStartTime.QuadPart;

	unsigned long newTicks = (unsigned long) (1000 * newTime / mFrequency.QuadPart);

	unsigned long check = GetTickCount() - mStartTick;
	signed long msecOff = (signed long)(newTicks - check);
	if (msecOff < -100 || msecOff > 100) {
		LONGLONG adjust = (std::min)(msecOff * mFrequency.QuadPart / 1000, newTime - mLastTime);
		mStartTime.QuadPart += adjust;
		newTime -= adjust;
	}

	mLastTime = newTime;

	return (unsigned long) (1000000 * newTime / mFrequency.QuadPart);
#else
	struct timeval now;
	gettimeofday(&now, NULL);
	return (now.tv_sec-start.tv_sec)*1000000+(now.tv_usec-start.tv_usec);
#endif
}

unsigned long Timer::getMillisecondsCPU() 
{
	clock_t newClock = clock();
	return (unsigned long)( (double)( newClock - mZeroClock ) / ( (double)CLOCKS_PER_SEC / 1000.0 ) );
}

unsigned long Timer::getMicrosecondsCPU() 
{
	clock_t newClock = clock();
	return (unsigned long)( (double)( newClock - mZeroClock ) / ( (double)CLOCKS_PER_SEC / 1000000.0 ) );
}

