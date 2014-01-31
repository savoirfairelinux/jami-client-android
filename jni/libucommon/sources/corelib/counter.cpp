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
#include <ucommon/counter.h>
#include <stdlib.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

using namespace UCOMMON_NAMESPACE;

counter::counter()
{
    cycle = value = 0;
}

counter::counter(unsigned max)
{
    assert(max > 0);
    cycle = max;
    value = 0;
}

void counter::operator=(unsigned v)
{
    if(!cycle || v < cycle)
        value = v;
}

unsigned counter::get(void)
{
    unsigned v = value++;
    if(cycle && value >= cycle)
        value = 0;
    return v;
}

SeqCounter::SeqCounter(void *base, size_t size, unsigned limit) :
counter(limit)
{
    assert(base != NULL);
    assert(size > 0);
    assert(limit > 0);
    item = base;
    offset = size;
}

void *SeqCounter::get(void)
{
    unsigned pos = counter::get();
    return (caddr_t)item + (pos * offset);
}

void *SeqCounter::get(unsigned pos)
{
    if(pos >= range())
        return NULL;

    return (caddr_t)item + (pos * offset);
}

bool toggle::get(void)
{
    bool v = value;
    if(value)
        value = false;
    else
        value = true;

    return v;
}


