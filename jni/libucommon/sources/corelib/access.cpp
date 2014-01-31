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
#include <ucommon/access.h>

using namespace UCOMMON_NAMESPACE;

UnlockAccess::~UnlockAccess()
{
}

SharedAccess::~SharedAccess()
{
}

ExclusiveAccess::~ExclusiveAccess()
{
}

void SharedAccess::exclusive(void)
{
}

void SharedAccess::share(void)
{
}

shared_access::shared_access(SharedAccess *obj)
{
    assert(obj != NULL);
    lock = obj;
    modify = false;
    lock->shared_lock();
}

exclusive_access::exclusive_access(ExclusiveAccess *obj)
{
    assert(obj != NULL);
    lock = obj;
    lock->exclusive_lock();
}

shared_access::~shared_access()
{
    if(lock) {
        if(modify)
            lock->share();
        lock->release_share();
        lock = NULL;
        modify = false;
    }
}

exclusive_access::~exclusive_access()
{
    if(lock) {
        lock->release_exclusive();
        lock = NULL;
    }
}

void shared_access::release()
{
    if(lock) {
        if(modify)
            lock->share();
        lock->release_share();
        lock = NULL;
        modify = false;
    }
}

void exclusive_access::release()
{
    if(lock) {
        lock->release_exclusive();
        lock = NULL;
    }
}

void shared_access::exclusive(void)
{
    if(lock && !modify) {
        lock->exclusive();
        modify = true;
    }
}

void shared_access::share(void)
{
    if(lock && modify) {
        lock->share();
        modify = false;
    }
}

