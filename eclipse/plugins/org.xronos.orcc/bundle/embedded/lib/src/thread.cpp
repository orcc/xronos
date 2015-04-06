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
#include "thread.h"

#include <iostream>
#include <stdlib.h>

#ifdef _WIN32
#include <process.h>
#else
#include <sys/types.h>
#include <unistd.h> 
#endif



#ifdef _WIN32
DWORD Thread::tls;
#else
pthread_key_t Thread::tls;
#endif

Thread::Thread(const int priority) 
	: m_priority(priority)
{
	init();
}

Thread::~Thread()
{
#ifdef _WIN32
	TlsFree(tls);
#endif
}

void Thread::init()
{
#ifdef _WIN32
	tls = TlsAlloc();
#else
    pthread_key_create(&Thread::tls, NULL);
#endif
}

void Thread::cancel()
{
#ifdef _WIN32
	TerminateThread(m_thread, 0);
#else
	pthread_cancel(m_thread);
#endif
}

void Thread::start(void * args)
{
	m_args = args;
#ifdef _WIN32
	m_thread = (HANDLE) _beginthreadex(NULL, 0, Thread::entryPoint, this, 0, &m_tid);
#else
	if (pthread_create(&m_thread, NULL, Thread::entryPoint, this) != 0)
	{
		m_thread = 0;
	}
#endif
}

void Thread::join()
{
#ifdef _WIN32
	WaitForSingleObject(m_thread, INFINITE);
	CloseHandle(m_thread);
#else
	pthread_join(m_thread, NULL);
#endif
}

void Thread::yield()
{
#ifdef _WIN32
	Sleep(0);
#else
	sched_yield();
#endif
}

void Thread::sleep(int ms)
{
#ifdef _WIN32
	Sleep(ms);
#else
	usleep(1000*ms);
#endif
}

#ifdef _WIN32
unsigned WINAPI
#else
void*
#endif
	Thread::entryPoint(void* pthis)
{
	Thread* t = (Thread*) pthis;

#ifdef _WIN32
	TlsSetValue(tls, pthis);
#else
	pthread_setspecific(Thread::tls, pthis);
#endif
	t->run((void*) t->m_args);
	return 0;
}

Thread* Thread::currentThread()
{
#ifdef _WIN32
	return (Thread*) TlsGetValue(tls);
#else
    return (Thread*) pthread_getspecific(Thread::tls);
#endif
}
