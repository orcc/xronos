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

#include "condition.h"

Condition::Condition()
{
#ifdef _WIN32
	m_waiters = 0;
	m_event = CreateEvent(NULL, FALSE, FALSE, NULL);
	m_sema = CreateSemaphore(NULL,0, 0x7fffffff,NULL);
#else
	pthread_cond_init(&mHandle, NULL);
#endif
}

Condition::~Condition()
{
#ifdef _WIN32
	CloseHandle(m_event);
	CloseHandle(m_sema);
#else
	pthread_cond_destroy(&mHandle);
#endif
}

void Condition::wait(Mutex& mutex)
{
#ifdef _WIN32
	InterlockedIncrement(&m_waiters);

	mutex.unlock();

	WaitForSingleObject(m_sema, INFINITE);

	InterlockedDecrement(&m_waiters);
	long w = InterlockedExchangeAdd(&m_waiters, 0);
	if (m_was_broadcast && w == 0)
		SetEvent(m_event);

	mutex.lock();
#else
	pthread_cond_wait(&mHandle, mutex.getHandle());
#endif
}

void Condition::signal()
{
#ifdef _WIN32
	long w = InterlockedExchangeAdd(&m_waiters, 0);
	int have_waiters = w > 0;

	if (have_waiters)
		ReleaseSemaphore(m_sema, 1, NULL);
#else
	pthread_cond_signal(&mHandle);
#endif
}

void Condition::broadcast()
{
#ifdef _WIN32
	int have_waiters = 0;
	long w = InterlockedExchangeAdd(&m_waiters, 0);
	if (w > 0)
	{
		m_was_broadcast = 1;
		have_waiters = 1;
	}

	int result = 0;
	if (have_waiters)
	{
		ReleaseSemaphore(m_sema, m_waiters, NULL);
		WaitForSingleObject(m_event, INFINITE) ;
		m_was_broadcast = 0;
	}
#else
	pthread_cond_broadcast(&mHandle);
#endif
}
