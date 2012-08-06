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
#include <cc++/thread.h>
#include "private.h"

#include <cerrno>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

Event::Event()
{
#ifdef WIN32
    cond = ::CreateEvent(NULL, true, false, NULL);
    if(cond == NULL)
        THROW(this);
#else
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutex_init(&_mutex, &attr);
    pthread_mutexattr_destroy(&attr);
    pthread_cond_init(&_cond, NULL);
    _signaled = false;
    _count = 0;
#endif
}

Event::~Event()
{
#ifdef WIN32
    ::CloseHandle(cond);
#else
    pthread_cond_destroy(&_cond);
    pthread_mutex_destroy(&_mutex);
#endif
}

void Event::signal(void)
{
#ifdef WIN32
    ::SetEvent(cond);
#else
    pthread_mutex_lock(&_mutex);
    _signaled = true;
    ++_count;
    pthread_cond_broadcast(&_cond);
    pthread_mutex_unlock(&_mutex);
#endif
}

void Event::reset(void)
{
#ifdef WIN32
    ::ResetEvent(cond);
#else
    _signaled = false;
#endif
}

bool Event::wait(void)
{
#ifdef WIN32
    return (Thread::waitThread(cond, INFINITE) == WAIT_OBJECT_0);
#else
    return wait(TIMEOUT_INF);
#endif
}

bool Event::wait(timeout_t timer)
{
#ifdef WIN32
    return (Thread::waitThread(cond, timer) == WAIT_OBJECT_0);
#else
    int rc = 0;
    struct  timespec spec;

    pthread_mutex_lock(&_mutex);
    long count = _count;
    while(!_signaled && _count == count) {
        if(timer != TIMEOUT_INF)
            rc = pthread_cond_timedwait(
                &_cond, &_mutex, getTimeout(&spec, timer));
        else
            pthread_cond_wait(&_cond, &_mutex);
        if(rc == ETIMEDOUT)
            break;
    }
    pthread_mutex_unlock(&_mutex);
    if(rc == ETIMEDOUT)
        return false;
    return true;
#endif
}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
