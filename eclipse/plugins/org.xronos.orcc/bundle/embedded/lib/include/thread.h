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

#ifndef __EMBEDDED_THREAD_H__
#define __EMBEDDED_THREAD_H__

#include <string>
#include <stdlib.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <pthread.h>
#include <signal.h>
#include <sched.h>
#include <unistd.h>
enum
{
	THREAD_PRIORITY_BELOW_NORMAL,
	THREAD_PRIORITY_NORMAL,
	THREAD_PRIORITY_ABOVE_NORMAL,
	THREAD_PRIORITY_TIME_CRITICAL,
	THREAD_PRIORITY_HIGHEST
};
#endif


class Thread
{
public:
	Thread(int priority = THREAD_PRIORITY_NORMAL);

	virtual ~Thread() = 0;

	void init();

	virtual void run(void *arg) = 0;

	void join();

	void sleep(int);

	void start(void *);

	void cancel();

	static void yield();

	static Thread* currentThread();

public:
#ifdef _WIN32
	static unsigned WINAPI entryPoint(void* pThreadInfo);

	HANDLE getHandle() const { return m_thread; }

	HANDLE m_thread;

	unsigned m_tid;

	static DWORD tls;

#else
	static void* entryPoint(void* pThreadInfo);

	pthread_t getHandle() const { return m_thread; }

	pthread_t m_thread;

	int m_tid;

	static pthread_key_t tls;
#endif

	int m_priority;

	void* m_args;
};
#endif
