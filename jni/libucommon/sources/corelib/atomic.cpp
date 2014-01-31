// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/atomic.h>
#include <ucommon/thread.h>

using namespace UCOMMON_NAMESPACE;

atomic::counter::counter(long init)
{
    value = init;
}

atomic::spinlock::spinlock()
{
    value = 0;
}

#ifdef HAVE_GCC_ATOMICS

long atomic::counter::operator++()
{
    return __sync_add_and_fetch(&value, 1);
}

long atomic::counter::operator--()
{
    return __sync_sub_and_fetch(&value, 1);
}

long atomic::counter::operator+=(long change)
{
    return __sync_add_and_fetch(&value, change);
}

long atomic::counter::operator-=(long change)
{
    return __sync_sub_and_fetch(&value, change);
}

bool atomic::spinlock::acquire(void)
{
    // if not locked by another already, then we acquired it...
    return (__sync_lock_test_and_set(&value, 1) == 0);
}

void atomic::spinlock::release(void)
{
    __sync_lock_release(&value);
}

#else

#define SIMULATED true

long atomic::counter::operator++()
{
    long rval;
    Mutex::protect((void *)&value);
    rval = (long)(++value);
    Mutex::release((void *)&value);
    return rval;
}

long atomic::counter::operator--()
{
    long rval;
    Mutex::protect((void *)&value);
    rval = (long)(--value);
    Mutex::release((void *)&value);
    return rval;
}

long atomic::counter::operator+=(long change)
{
    long rval;
    Mutex::protect((void *)&value);
    rval = (long)(value += change);
    Mutex::release((void *)&value);
    return rval;
}

long atomic::counter::operator-=(long change)
{
    long rval;
    Mutex::protect((void *)&value);
    rval = (long)(value -= change);
    Mutex::release((void *)&value);
    return rval;
}

bool atomic::spinlock::acquire(void)
{
    bool rtn = true;

    Mutex::protect((void *)&value);
    if(value == 1)
        rtn = false;
    else
        value = 1;
    Mutex::release((void *)&value);
    return rtn;
}

void atomic::spinlock::release(void)
{
    Mutex::protect((void *)&value);
    value = 0;
    Mutex::release((void *)&value);
}

#endif

#ifdef SIMULATED
const bool atomic::simulated = true;
#else
const bool atomic::simulated = false;
#endif
