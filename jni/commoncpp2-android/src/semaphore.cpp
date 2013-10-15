// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// Common C++.  If you copy code from other releases into a copy of GNU
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

#include <cc++/config.h>
#include <cc++/export.h>
#include <cc++/exception.h>
#include <cc++/thread.h>
#include "private.h"
#include <cstdio>
#include <cstdlib>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifndef WIN32

extern "C" {
#include <sys/types.h>
}

#include <cerrno>

Semaphore::Semaphore(unsigned resource)
{
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutex_init(&_mutex, &attr);
    pthread_mutexattr_destroy(&attr);
    if(pthread_cond_init(&_cond, NULL) && Thread::getException() == Thread::throwObject)
        THROW(this);

    _count = resource;
    _waiters = 0;
}

Semaphore::~Semaphore()
{
    // Unlock is needed to unlock the mutex in the case of a cancel during Semaphore::wait()
    // see PTHREAD_COND_TIMEDWAIT(3P)
    pthread_mutex_unlock(&_mutex);
    pthread_cond_destroy(&_cond);
    pthread_mutex_destroy(&_mutex);
}

void Semaphore::force_unlock_after_cancellation()
{
   pthread_mutex_unlock(&_mutex);
}

bool Semaphore::wait(timeout_t timeout)
{
    struct timespec ts;
    bool flag = true;
    int rc;
    pthread_mutex_lock(&_mutex);
    ++_waiters;

    if(_count)
        goto exiting;

    if(!timeout) {
        while(_count == 0)
            pthread_cond_wait(&_cond, &_mutex);
        goto exiting;
    }

    getTimeout(&ts, timeout);
    rc = pthread_cond_timedwait(&_cond, &_mutex, &ts);
    if(rc == ETIMEDOUT || _count == 0)
        flag = false;

exiting:
    --_waiters;
    if(_count)
        --_count;
    pthread_mutex_unlock(&_mutex);
    return flag;
}

void Semaphore::post(void)
{
    pthread_mutex_lock(&_mutex);
    if(_waiters > 0)
        pthread_cond_signal(&_cond);
    ++_count;
    pthread_mutex_unlock(&_mutex);
}

#if 0   // stripped since only in posix
int Semaphore::getValue(void)
{
    int value;

    pthread_mutex_lock(&_mutex);
    value = _count;
    pthread_mutex_unlock(&_mutex);
    return value;
}
#endif

#else

Semaphore::Semaphore(unsigned resource)
{
    semObject = ::CreateSemaphore((LPSECURITY_ATTRIBUTES)NULL, (LONG)resource, MAX_SEM_VALUE, (LPCTSTR)NULL);
}

Semaphore::~Semaphore()
{
    ::CloseHandle(semObject);
}

bool Semaphore::wait(timeout_t timeout)
{
    if(!timeout)
        timeout = INFINITE;

    return Thread::waitThread(semObject, timeout) == WAIT_OBJECT_0;
}

void Semaphore::post(void)
{
    ::ReleaseSemaphore(semObject, 1, (LPLONG)NULL);
}


#endif //WIN32

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
