// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks
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
#include <cc++/slog.h>
#include <iostream>
#include <cerrno>
#include <cstdlib>

#ifdef HAVE_GCC_CXX_BITS_ATOMIC
using namespace __gnu_cxx;
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

bool Mutex::_debug = false;

ThreadLock::ThreadLock()
{
#ifdef HAVE_PTHREAD_RWLOCK
    pthread_rwlockattr_t attr;

    pthread_rwlockattr_init(&attr);
    if(pthread_rwlock_init(&_lock, &attr)) {
        pthread_rwlockattr_destroy(&attr);
#ifdef  CCXX_EXCEPTIONS
        if(Thread::getException() == Thread::throwObject)
            throw(this);
#ifdef  COMMON_STD_EXCEPTION
        else if(Thread::getException() == Thread::throwException)
            throw(SyncException("Mutex constructor failure"));
#endif
#endif
    }
    else
        pthread_rwlockattr_destroy(&attr);
#endif
}

ThreadLock::~ThreadLock()
{
#ifdef HAVE_PTHREAD_RWLOCK
    pthread_rwlock_destroy(&_lock);
#endif
}

void ThreadLock::readLock(void)
{
#ifdef HAVE_PTHREAD_RWLOCK
    pthread_rwlock_rdlock(&_lock);
#else
    mutex.enterMutex();
#endif
}

void ThreadLock::writeLock(void)
{
#ifdef HAVE_PTHREAD_RWLOCK
    pthread_rwlock_wrlock(&_lock);
#else
    mutex.enterMutex();
#endif
}

void ThreadLock::unlock(void)
{
#ifdef HAVE_PTHREAD_RWLOCK
    pthread_rwlock_unlock(&_lock);
#else
    mutex.leaveMutex();
#endif
}

bool ThreadLock::tryReadLock(void)
{
#ifdef  HAVE_PTHREAD_RWLOCK
    if(pthread_rwlock_tryrdlock(&_lock))
        return false;
    return true;
#else
    return mutex.tryEnterMutex();
#endif
}

bool ThreadLock::tryWriteLock(void)
{
#ifdef  HAVE_PTHREAD_RWLOCK
    if(pthread_rwlock_trywrlock(&_lock))
        return false;
    return true;
#else
    return mutex.tryEnterMutex();
#endif
}

#ifndef WIN32
Conditional::Conditional(const char *id)
{
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutex_init(&_mutex, &attr);
    pthread_mutexattr_destroy(&attr);
    if(pthread_cond_init(&_cond, NULL) && Thread::getException() == Thread::throwObject)
        THROW(this);
}

Conditional::~Conditional()
{
    pthread_cond_destroy(&_cond);
    pthread_mutex_destroy(&_mutex);
}

bool Conditional::tryEnterMutex(void)
{
    if(pthread_mutex_trylock(&_mutex) != 0)
        return false;
    return true;
}

void Conditional::enterMutex(void)
{
    pthread_mutex_lock(&_mutex);
}

void Conditional::leaveMutex(void)
{
    pthread_mutex_unlock(&_mutex);
}

void Conditional::signal(bool broadcast)
{
    if(broadcast)
        pthread_cond_broadcast(&_cond);
    else
        pthread_cond_signal(&_cond);
}

bool Conditional::wait(timeout_t timeout, bool locked)
{
    struct timespec ts;
    int rc;

    if(!locked)
        enterMutex();
    if(!timeout) {
        pthread_cond_wait(&_cond, &_mutex);
        if(!locked)
            leaveMutex();
        return true;
    }
    getTimeout(&ts, timeout);
    rc = pthread_cond_timedwait(&_cond, &_mutex, &ts);
    if(!locked)
        leaveMutex();
    if(rc == ETIMEDOUT)
        return false;
    return true;
}

#endif

#ifndef WIN32
Mutex::Mutex(const char *name)
{
    pthread_mutexattr_t _attr;

    pthread_mutexattr_init(&_attr);
#ifdef  PTHREAD_MUTEXTYPE_RECURSIVE
    pthread_mutexattr_settype(&_attr, PTHREAD_MUTEXTYPE_RECURSIVE);
#endif
    pthread_mutex_init(&_mutex, &_attr);
    pthread_mutexattr_destroy(&_attr);

#ifndef PTHREAD_MUTEXTYPE_RECURSIVE
    _level = 0;
    _tid = NULL;
#endif
    _name = name;
}

Mutex::~Mutex()
{
    pthread_mutex_destroy(&_mutex);
}

#ifdef PTHREAD_MUTEXTYPE_RECURSIVE

bool Mutex::tryEnterMutex(void)
{
    return (pthread_mutex_trylock(&_mutex) == 0) ? true : false;
}

void Mutex::enterMutex(void)
{
    if(_debug && _name)
        slog.debug() << Thread::get()->getName()
            << ": entering " << _name << std::endl;

    pthread_mutex_lock(&_mutex);
}

void Mutex::leaveMutex(void)
{
    pthread_mutex_unlock(&_mutex);
    if(_debug && _name)
        slog.debug() << Thread::get()->getName()
            << ": leaving" << _name << std::endl;

}

#else // !PTHREAD_MUTEXTYPE_RECURSIVE

void Mutex::enterMutex(void)
{
    if(_tid == Thread::get()) {
        ++_level;
        return;
    }
    if(_debug && _name)
        std::cerr << Thread::get()->getName() << ": entering" << _name << std::endl;

    pthread_mutex_lock(&_mutex);
    ++_level;
    _tid = Thread::get();
}

void Mutex::leaveMutex(void)
{
    if(_tid != Thread::get())
        return;
    if(--_level > 0)
        return;
    _tid = NULL;
    _level = 0;
    pthread_mutex_unlock(&_mutex);
    if(_debug && _name)
        std::cerr << Thread::get()->getName() << ": leaving" << _name << std::endl;
}

bool Mutex::tryEnterMutex(void)
{
    if(_tid == Thread::get()) {
        ++_level;
        return true;
    }
    if ( pthread_mutex_trylock(&_mutex) != 0 )
        return false;
    _tid = Thread::get();
    ++_level;
    return true;
}
#endif

#else // WIN32

Mutex::Mutex(const char *name)
#ifdef MUTEX_UNDERGROUND_WIN32_MUTEX
:_mutex(0)
#endif // MUTEX_UNDERGROUND_WIN32_MUTEX
{
#ifdef MUTEX_UNDERGROUND_WIN32_CRITICALSECTION
#if _WIN32_WINNT >= 0x0403
    if(!InitializeCriticalSectionAndSpinCount(&_criticalSection, 4000)) {
        THROW(this);
    }
#elif _WIN32_WINNT >= 0x0400
    // can rise STATUS_NO_MEMORY exception in low memory situations.
    InitializeCriticalSection(&_criticalSection);
#else
#error "Not supported Windows version"
#endif
#endif // MUTEX_UNDERGROUND_WIN32_CRITICALSECTION

#ifdef MUTEX_UNDERGROUND_WIN32_MUTEX
    _mutex = ::CreateMutex(NULL,FALSE,NULL);
    if(!_mutex)
        THROW(this);
#endif // MUTEX_UNDERGROUND_WIN32_MUTEX


    _name = name;
}

void Mutex::enterMutex(void)
{
    if(_debug && _name)
        slog.debug() << Thread::get()->getName()
            << ": entering " << _name << std::endl;

#ifdef MUTEX_UNDERGROUND_WIN32_CRITICALSECTION
    ::EnterCriticalSection(&_criticalSection);
#endif // MUTEX_UNDERGROUND_WIN32_CRITICALSECTION

#ifdef MUTEX_UNDERGROUND_WIN32_MUTEX
    Thread::waitThread(_mutex, INFINITE);
#endif // MUTEX_UNDERGROUND_WIN32_MUTEX

}

bool Mutex::tryEnterMutex(void)
{
#ifdef MUTEX_UNDERGROUND_WIN32_CRITICALSECTION
    return (::TryEnterCriticalSection(&_criticalSection) == TRUE);
#endif // MUTEX_UNDERGROUND_WIN32_CRITICALSECTION

#ifdef MUTEX_UNDERGROUND_WIN32_MUTEX
    return (Thread::waitThread(_mutex, 0) == WAIT_OBJECT_0);
#endif // MUTEX_UNDERGROUND_WIN32_MUTEX
}

Mutex::~Mutex()
{
#ifdef MUTEX_UNDERGROUND_WIN32_CRITICALSECTION
    ::DeleteCriticalSection(&_criticalSection);
#endif // MUTEX_UNDERGROUND_WIN32_CRITICALSECTION

#ifdef MUTEX_UNDERGROUND_WIN32_MUTEX
    ::CloseHandle(_mutex);
#endif // MUTEX_UNDERGROUND_WIN32_MUTEX

}

void Mutex::leaveMutex(void)
{
#ifdef MUTEX_UNDERGROUND_WIN32_CRITICALSECTION
    ::LeaveCriticalSection(&_criticalSection);
#endif // MUTEX_UNDERGROUND_WIN32_CRITICALSECTION

#ifdef MUTEX_UNDERGROUND_WIN32_MUTEX
    if (!ReleaseMutex(_mutex))
        THROW(this);
#endif // MUTEX_UNDERGROUND_WIN32_MUTEX

    if(_debug && _name)
        slog.debug() << Thread::get()->getName()
            << ": leaving" << _name << std::endl;
}
#endif  // WIN32 MUTEX

#ifdef WIN32
MutexCounter::MutexCounter(const char *id) : Mutex(id)
{
    counter = 0;
};
#endif

MutexCounter::MutexCounter(int initial, const char *id) : Mutex(id)
{
    counter = initial;
}

int operator++(MutexCounter &mc)
{
    int rtn;

    mc.enterMutex();
    rtn = mc.counter++;
    mc.leaveMutex();
    return rtn;
}

// ??? why cannot be < 0 ???
int operator--(MutexCounter &mc)
{
    int rtn = 0;

    mc.enterMutex();
    if(mc.counter) {
        rtn = --mc.counter;
        if(!rtn) {
            mc.leaveMutex();
            THROW(mc);
        }
    }
    mc.leaveMutex();
    return rtn;
}

#ifndef CCXX_USE_WIN32_ATOMIC
#ifdef  HAVE_ATOMIC
AtomicCounter::AtomicCounter()
{
#if     defined(HAVE_ATOMIC_AIX) || defined(HAVE_GCC_BITS_ATOMIC) \
    || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    counter = 0;
#else
    atomic.counter = 0;
#endif
}

AtomicCounter::AtomicCounter(int value)
{
#if     defined(HAVE_ATOMIC_AIX) || defined(HAVE_GCC_BITS_ATOMIC) \
    || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    counter = 0;
#else
    atomic.counter = value;
#endif
}

AtomicCounter::~AtomicCounter() {};

int AtomicCounter::operator++(void)
{
#ifdef  HAVE_ATOMIC_AIX
    return fetch_and_add((atomic_p)&counter, 1);
#elif   defined(HAVE_GCC_BITS_ATOMIC) || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    // Modified by JCE from 2v1.3.8 source, 30/Mar/2005
    // BUG FIX: __exchange_and_add() does not seem to return updated <counter>
    __exchange_and_add(&counter, 1);
    return counter;
    // end modification by JCE
#else
    atomic_inc(&atomic);
    return atomic_read(&atomic);
#endif
}

int AtomicCounter::operator--(void)
{
#ifdef  HAVE_ATOMIC_AIX
    return fetch_and_add((atomic_p)&counter, -1);
#elif   defined(HAVE_GCC_BITS_ATOMIC) || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    // Modified by JCE from 2v1.3.8 source, 30/Mar/2005
    // BUG FIX: __exchange_and_add() does not seem to return updated <counter>
    __exchange_and_add(&counter, -1);
    return counter;
    // end modification by JCE
#else
    int chk = atomic_dec_and_test(&atomic);
    if(chk)
        return 0;
    chk = atomic_read(&atomic);
    if(!chk)
        ++chk;
    return chk;
#endif
}

int AtomicCounter::operator+=(int change)
{
#ifdef  HAVE_ATOMIC_AIX
    return fetch_and_add((atomic_p)&counter, change);
#elif   defined(HAVE_GCC_BITS_ATOMIC) || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    // Modified by JCE from 2v1.3.8 source, 30/Mar/2005
    // BUG FIX: __exchange_and_add() does not seem to return updated <counter>
    __exchange_and_add(&counter, change);
    return counter;
    // end modification by JCE
#else
    atomic_add(change, &atomic);
    return atomic_read(&atomic);
#endif
}

int AtomicCounter::operator-=(int change)
{
#ifdef  HAVE_ATOMIC_AIX
    return fetch_and_add((atomic_p)&counter, -change);
#elif   defined(HAVE_GCC_BITS_ATOMIC) || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    // Modified by JCE from 2v1.3.8 source, 30/Mar/2005
    // BUG FIX: __exchange_and_add() does not seem to return updated <counter>
    __exchange_and_add(&counter, -change);
    return counter;
    // end modification by JCE
#else
    atomic_sub(change, &atomic);
    return atomic_read(&atomic);
#endif
}

int AtomicCounter::operator+(int change)
{
#if     defined(HAVE_ATOMIC_AIX) || defined(HAVE_GCC_BITS_ATOMIC) \
    || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    return counter + change;
#else
    return atomic_read(&atomic) + change;
#endif
}

int AtomicCounter::operator-(int change)
{
#if     defined(HAVE_ATOMIC_AIX) || defined(HAVE_GCC_BITS_ATOMIC) \
    || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    return counter - change;
#else
    return atomic_read(&atomic) - change;
#endif
}

int AtomicCounter::operator=(int value)
{
#if     defined(HAVE_ATOMIC_AIX) || defined(HAVE_GCC_BITS_ATOMIC) \
    || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    return counter = value;
#else
    atomic_set(&atomic, value);
    return atomic_read(&atomic);
#endif
}

bool AtomicCounter::operator!(void)
{
#if     defined(HAVE_ATOMIC_AIX) || defined(HAVE_GCC_BITS_ATOMIC) \
    || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    int value = counter;
#else
    int value = atomic_read(&atomic);
#endif
    if(value)
        return false;
    return true;
}

AtomicCounter::operator int()
{
#if     defined(HAVE_ATOMIC_AIX) || defined(HAVE_GCC_BITS_ATOMIC) \
    || defined(HAVE_GCC_CXX_BITS_ATOMIC)
    return counter;
#else
    return atomic_read(&atomic);
#endif
}

#else // !HAVE_ATOMIC

AtomicCounter::AtomicCounter()
{
    counter = 0;

    pthread_mutexattr_t _attr;
    pthread_mutexattr_init(&_attr);
    pthread_mutex_init(&_mutex, &_attr);
    pthread_mutexattr_destroy(&_attr);
}

AtomicCounter::AtomicCounter(int value)
{
    counter = value;

    pthread_mutexattr_t _attr;
    pthread_mutexattr_init(&_attr);
    pthread_mutex_init(&_mutex, &_attr);
    pthread_mutexattr_destroy(&_attr);
}

AtomicCounter::~AtomicCounter()
{
    pthread_mutex_destroy(&_mutex);
}

int AtomicCounter::operator++(void)
{
    int value;

    pthread_mutex_lock(&_mutex);
    value = ++counter;
    pthread_mutex_unlock(&_mutex);
    return value;
}

int AtomicCounter::operator--(void)
{
    int value;
    pthread_mutex_lock(&_mutex);
    value = --counter;
    pthread_mutex_unlock(&_mutex);
    return value;
}

int AtomicCounter::operator+=(int change)
{
    int value;
    pthread_mutex_lock(&_mutex);
    counter += change;
    value = counter;
    pthread_mutex_unlock(&_mutex);
    return value;
}

int AtomicCounter::operator-=(int change)
{
    int value;
    pthread_mutex_lock(&_mutex);
    counter -= change;
    value = counter;
    pthread_mutex_unlock(&_mutex);
    return value;
}

int AtomicCounter::operator+(int change)
{
    int value;
    pthread_mutex_lock(&_mutex);
    value = counter + change;
    pthread_mutex_unlock(&_mutex);
    return value;
}

int AtomicCounter::operator-(int change)
{
    int value;
    pthread_mutex_lock(&_mutex);
    value = counter - change;
    pthread_mutex_unlock(&_mutex);
    return value;
}

AtomicCounter::operator int()
{
    int value;
    pthread_mutex_lock(&_mutex);
    value = counter;
    pthread_mutex_unlock(&_mutex);
    return value;
}

int AtomicCounter::operator=(int value)
{
    int ret;
    pthread_mutex_lock(&_mutex);
    ret = counter;
    counter = value;
    pthread_mutex_unlock(&_mutex);
    return ret;
}

bool AtomicCounter::operator!(void)
{
    int value;
    pthread_mutex_lock(&_mutex);
    value = counter;
    pthread_mutex_unlock(&_mutex);
    if(value)
        return false;
    return true;
}
#endif // HAVE_ATOMIC
#else // WIN32
int AtomicCounter::operator+=(int change)
{
    // FIXME: enhance with InterlockExchangeAdd
    while(--change>=0)
        InterlockedIncrement(&atomic);

    return atomic;
}

int AtomicCounter::operator-=(int change)
{
    // FIXME: enhance with InterlockExchangeAdd
    while(--change>=0)
        InterlockedDecrement(&atomic);

    return atomic;
}
#endif // !WIN32

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */

