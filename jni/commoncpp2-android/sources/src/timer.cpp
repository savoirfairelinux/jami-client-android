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

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifndef WIN32

#ifdef  HAVE_HIRES_TIMER
TimerPort::TimerPort()
{
    struct timespec ts;
    active = false;

#if defined(CLOCK_MONOTONIC) && defined(USE_MONOTONIC_TIMER)
    ::clock_gettime(CLOCK_MONOTONIC, &ts);
#else
    ::clock_gettime(CLOCK_REALTIME, &ts);
#endif
    timer.tv_sec = ts.tv_sec;
    timer.tv_usec = ts.tv_nsec / 1000;
}
#else
TimerPort::TimerPort()
{
    active = false;
       SysTime::getTimeOfDay(&timer);
}
#endif

void TimerPort::setTimer(timeout_t timeout)
{
#ifdef  HAVE_HIRES_TIMER
    struct timespec ts;
#if defined(CLOCK_MONOTONIC) && defined(USE_MONOTONIC_TIMER)
    ::clock_gettime(CLOCK_MONOTONIC, &ts);
#else
    ::clock_gettime(CLOCK_REALTIME, &ts);
#endif
    timer.tv_sec = ts.tv_sec;
    timer.tv_usec = ts.tv_nsec / 1000l;
#else
       SysTime::getTimeOfDay(&timer);
#endif
    active = false;
    if(timeout)
        incTimer(timeout);
}

void TimerPort::incTimer(timeout_t timeout)
{
    int secs = timeout / 1000;
    int usecs = (timeout % 1000) * 1000;

    timer.tv_usec += usecs;
    if(timer.tv_usec > 1000000l) {
        ++timer.tv_sec;
        timer.tv_usec %= 1000000l;
    }
    timer.tv_sec += secs;
    active = true;
}

void TimerPort::decTimer(timeout_t timeout)
{
    int secs = timeout / 1000;
    int usecs = (timeout % 1000) * 1000;

    if(timer.tv_usec < usecs) {
        --timer.tv_sec;
        timer.tv_usec = 1000000l + timer.tv_usec - usecs;
    }
    else
        timer.tv_usec -= usecs;

    timer.tv_sec -= secs;
    active = true;
}

#ifdef  HAVE_HIRES_TIMER
void TimerPort::sleepTimer(void)
{
    struct timespec ts;
    ts.tv_sec = timer.tv_sec;
    ts.tv_nsec = timer.tv_usec * 1000l;

#if defined(CLOCK_MONOTONIC) && defined(USE_MONOTONIC_TIMER)
    ::clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &ts, NULL);
#else
    ::clock_nanosleep(CLOCK_REALTIME, TIMER_ABSTIME, &ts, NULL);
#endif
}
#else
void TimerPort::sleepTimer(void)
{
    timeout_t remaining = getTimer();
    if(remaining && remaining != TIMEOUT_INF)
        Thread::sleep(remaining);
}
#endif

void TimerPort::endTimer(void)
{
    active = false;
}

timeout_t TimerPort::getTimer(void) const
{
#ifdef  HAVE_HIRES_TIMER
    struct timespec now;
#else
    struct timeval now;
#endif
    long diff;

    if(!active)
        return TIMEOUT_INF;

#ifdef  HAVE_HIRES_TIMER
#if defined(CLOCK_MONOTONIC) && defined(USE_MONOTONIC_TIMER)
    ::clock_gettime(CLOCK_MONOTONIC, &now);
#else
    ::clock_gettime(CLOCK_REALTIME, &now);
#endif
    diff = (timer.tv_sec - now.tv_sec) * 1000l;
    diff += (timer.tv_usec - (now.tv_nsec / 1000)) / 1000l;
#else
       SysTime::getTimeOfDay(&now);
    diff = (timer.tv_sec - now.tv_sec) * 1000l;
    diff += (timer.tv_usec - now.tv_usec) / 1000l;
#endif

    if(diff < 0)
        return 0l;

    return diff;
}

timeout_t TimerPort::getElapsed(void) const
{
#ifdef  HAVE_HIRES_TIMER
    struct timespec now;
#else
    struct timeval now;
#endif
    long diff;

    if(!active)
        return TIMEOUT_INF;

#ifdef  HAVE_HIRES_TIMER
#if defined(CLOCK_MONOTONIC) && defined(USE_MONOTONIC_TIMER)
    ::clock_gettime(CLOCK_MONOTONIC, &now);
#else
    ::clock_gettime(CLOCK_REALTIME, &now);
#endif
    diff = (now.tv_sec - timer.tv_sec) * 1000l;
    diff += ((now.tv_nsec / 1000l) - timer.tv_usec) / 1000l;
#else
       SysTime::getTimeOfDay(&now);
    diff = (now.tv_sec -timer.tv_sec) * 1000l;
    diff += (now.tv_usec - timer.tv_usec) / 1000l;
#endif
    if(diff < 0)
        return 0;
    return diff;
}
#else // WIN32
TimerPort::TimerPort()
{
    active = false;
    timer = GetTickCount();
}

void TimerPort::setTimer(timeout_t timeout)
{
    timer = GetTickCount();
    active = false;
    if(timeout)
        incTimer(timeout);
}

void TimerPort::incTimer(timeout_t timeout)
{
    timer += timeout;
    active = true;
}

void TimerPort::decTimer(timeout_t timeout)
{
    timer -= timeout;
    active = true;
}

void TimerPort::sleepTimer(void)
{
    timeout_t remaining = getTimer();
    if(remaining && remaining != TIMEOUT_INF)
        Thread::sleep(remaining);
}

void TimerPort::endTimer(void)
{
    active = false;
}

timeout_t TimerPort::getTimer(void) const
{
    DWORD now;
    long diff;

    if(!active)
        return TIMEOUT_INF;

    now = GetTickCount();
    diff = timer - now;

    if(diff < 0)
        return 0l;

    return diff;
}

timeout_t TimerPort::getElapsed(void) const
{
    DWORD now;
    long diff;

    if(!active)
        return TIMEOUT_INF;

    now = GetTickCount();
    diff = now - timer;

    if(diff < 0)
        return 0l;

    return diff;
}
#endif

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
