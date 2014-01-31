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
#include <ucommon/protocols.h>
#include <ucommon/object.h>
#include <stdlib.h>
#include <string.h>

using namespace UCOMMON_NAMESPACE;

CountedObject::CountedObject()
{
    count = 0;
}

CountedObject::CountedObject(const ObjectProtocol &source)
{
    count = 0;
}

void CountedObject::dealloc(void)
{
    delete this;
}

void CountedObject::retain(void)
{
    ++count;
}

void CountedObject::release(void)
{
    if(count > 1) {
        --count;
        return;
    }
    dealloc();
}

auto_object::auto_object(ObjectProtocol *o)
{
    if(o)
        o->retain();

    object = o;
}

auto_object::auto_object()
{
    object = 0;
}

void auto_object::release(void)
{
    if(object)
        object->release();
    object = 0;
}

auto_object::~auto_object()
{
    release();
}

auto_object::auto_object(const auto_object &from)
{
    object = from.object;
    if(object)
        object->retain();
}

bool auto_object::operator!() const
{
    return (object == 0);
}

auto_object::operator bool() const
{
    return (object != 0);
}

bool auto_object::operator==(ObjectProtocol *o) const
{
    assert(o != NULL);
    return object == o;
}

bool auto_object::operator!=(ObjectProtocol *o) const
{
    assert(o != NULL);
    return object != o;
}

void auto_object::operator=(ObjectProtocol *o)
{
    if(object == o)
        return;

    if(o)
        o->retain();
    if(object)
        object->release();
    object = o;
}

SparseObjects::SparseObjects(unsigned m)
{
    assert(m > 0);
    max = m;
    vector = new ObjectProtocol *[m];
    memset(vector, 0, sizeof(ObjectProtocol *) * m);
}

SparseObjects::~SparseObjects()
{
    purge();
}

void SparseObjects::purge(void)
{
    if(!vector)
        return;

    for(unsigned pos = 0; pos < max; ++ pos) {
        if(vector[pos])
            vector[pos]->release();
    }
    delete[] vector;
    vector = NULL;
}

unsigned SparseObjects::count(void)
{
    unsigned c = 0;
    for(unsigned pos = 0; pos < max; ++pos) {
        if(vector[pos])
            ++c;
    }
    return c;
}

ObjectProtocol *SparseObjects::invalid(void) const
{
    return NULL;
}

ObjectProtocol *SparseObjects::get(unsigned pos)
{
    ObjectProtocol *obj;

    if(pos >= max)
        return invalid();

    if(!vector[pos]) {
        obj = create();
        if(!obj)
            return invalid();
        obj->retain();
        vector[pos] = obj;
    }
    return vector[pos];
}


