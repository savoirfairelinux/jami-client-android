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
#include <ucommon/vector.h>
#include <ucommon/thread.h>
#include <string.h>
#include <stdarg.h>

using namespace UCOMMON_NAMESPACE;

const vectorsize_t Vector::npos = (vectorsize_t)(-1);

Vector::array::array(vectorsize_t size)
{
    assert(size > 0);

    max = size;
    len = 0;
    list[0] = 0;
}

void Vector::array::dealloc(void)
{
    purge();
    CountedObject::dealloc();
}

void Vector::array::purge(void)
{
    vectorsize_t pos = 0;
    while(list[pos])
        list[pos++]->release();
    len = 0;
    list[0] = 0;
}

void Vector::array::dec(vectorsize_t offset)
{
    if(!len)
        return;

    if(offset >= len) {
        purge();
        return;
    }

    while(offset--) {
        list[--len]->release();
        list[len] = 0;
    }
}

void Vector::array::inc(vectorsize_t offset)
{
    if(!offset)
        ++offset;

    if(offset >= len) {
        purge();
        return;
    }

    if(!len)
        return;

    for(vectorsize_t pos = 0; pos < offset; ++pos)
        list[pos]->release();

    memmove(list, &list[offset], (len - offset) * sizeof(void *));
    len -= offset;
    list[len] = 0;
}

void Vector::array::set(ObjectProtocol **items)
{
    assert(items != NULL);

    purge();
    add(items);
}

void Vector::array::add(ObjectProtocol **items)
{
    assert(items != NULL);

    vectorsize_t size = Vector::size((void **)(items));

    if(!size)
        return;

    if(len + size > max)
        size = max - len;

    if(size < 1)
        return;

    for(vectorsize_t pos = 0; pos < size; ++pos) {
        list[len++] = items[pos];
        items[pos]->retain();
    }
    list[len] = 0;
}

void Vector::array::add(ObjectProtocol *obj)
{
    assert(obj);

    if(len == max)
        return;

    obj->retain();
    list[len++] = obj;
    list[len] = 0;
}

vectorsize_t Vector::size(void) const
{
    if(!data)
        return 0;

    return data->max;
}

vectorsize_t Vector::len(void) const
{
    if(!data)
        return 0;

    return data->len;
}

Vector::Vector()
{
    data = NULL;
}

Vector::Vector(ObjectProtocol **items, vectorsize_t limit)
{
    assert(items);

    if(!limit)
        limit = size((void **)items);

    data = create(limit);
    data->retain();
    data->set(items);
}

Vector::Vector(vectorsize_t size)
{
    assert(size > 0);

    data = create(size);
    data->retain();
}

Vector::~Vector()
{
    release();
}

ObjectProtocol **Vector::list(void) const
{
    if(!data)
        return NULL;

    return data->list;
}

ObjectProtocol *Vector::invalid(void) const
{
    return NULL;
}

ObjectProtocol *Vector::get(int offset) const
{
    if(!data || !data->len)
        return invalid();

    if(offset >= (int)(data->len))
        return invalid();

    if(((vectorsize_t)(-offset)) >= data->len)
        return invalid();

    if(offset >= 0)
        return data->list[offset];

    return data->list[data->len + offset];
}

vectorsize_t Vector::get(void **target, vectorsize_t limit) const
{
    assert(target != NULL && limit > 0);

    vectorsize_t pos;
    if(!data) {
        target[0] = 0;
        return 0;
    }

    for(pos = 0; pos < data->len && pos < limit - 1; ++pos)
        target[pos] = data->list[pos];
    target[pos] = 0;
    return pos;
}

Vector::array *Vector::create(vectorsize_t size) const
{
    assert(size > 0);

    return new((size_t)size) array(size);
}

void Vector::release(void)
{
    if(data)
        data->release();
    data = NULL;
}

ObjectProtocol *Vector::begin(void) const
{
    if(!data)
        return NULL;

    return data->list[0];
}

ObjectProtocol *Vector::end(void) const
{
    if(!data || !data->len)
        return NULL;

    return data->list[data->len - 1];
}

vectorsize_t Vector::find(ObjectProtocol *obj, vectorsize_t pos) const
{
    assert(obj != NULL);

    if(!data)
        return npos;

    while(pos < data->len) {
        if(data->list[pos] == obj)
            return pos;
        ++pos;
    }
    return npos;
}

void Vector::split(vectorsize_t pos)
{
    if(!data || pos >= data->len)
        return;

    while(data->len > pos) {
        --data->len;
        data->list[data->len]->release();
    }
    data->list[data->len] = NULL;
}

void Vector::rsplit(vectorsize_t pos)
{
    vectorsize_t head = 0;

    if(!data || pos >= data->len || !pos)
        return;

    while(head < pos)
        data->list[head++]->release();

    head = 0;
    while(data->list[pos])
        data->list[head++] = data->list[pos++];

    data->len = head;
    data->list[head] = NULL;
}

void Vector::set(ObjectProtocol **list)
{
    assert(list);

    if(!data && list) {
        data = create(size((void **)list));
        data->retain();
    }
    if(data && list)
        data->set(list);
}

void Vector::set(vectorsize_t pos, ObjectProtocol *obj)
{
    assert(obj != NULL);

    if(!data || pos > data->len)
        return;

    if(pos == data->len && data->len < data->max) {
        data->list[data->len++] = obj;
        data->list[data->len] = NULL;
        obj->retain();
        return;
    }
    data->list[pos]->release();
    data->list[pos] = obj;
    obj->retain();
}

void Vector::add(ObjectProtocol **list)
{
    assert(list);

    if(data && list)
        data->add(list);
}

void Vector::add(ObjectProtocol *obj)
{
    assert(obj);

    if(data && obj)
        data->add(obj);
}

void Vector::clear(void)
{
    if(data)
        data->purge();
}

bool Vector::resize(vectorsize_t size)
{
    if(!size) {
        release();
        data = NULL;
        return true;
    }

    if(data->is_copied() || data->max < size) {
        data->release();
        data = create(size);
        data->retain();
    }
    return true;
}

void Vector::cow(vectorsize_t size)
{
    if(data) {
        size += data->len;
    }

    if(!size)
        return;

    if(!data || !data->max || data->is_copied() || size > data->max) {
        array *a = create(size);
        a->len = data->len;
        memcpy(a->list, data->list, data->len * sizeof(ObjectProtocol *));
        a->list[a->len] = 0;
        a->retain();
        data->release();
        data = a;
    }
}

void Vector::operator^=(Vector &v)
{
    release();
    set(v.list());
}

Vector &Vector::operator^(Vector &v)
{
    vectorsize_t vs = v.len();

    if(!vs)
        return *this;

    if(data && data->len + vs > data->max)
        cow();

    add(v.list());
    return *this;
}

void Vector::operator++()
{
    if(!data)
        return;

    data->inc(1);
}

void Vector::operator--()
{
    if(!data)
        return;

    data->dec(1);
}

void Vector::operator+=(vectorsize_t inc)
{
    if(!data)
        return;

    data->inc(inc);
}

void Vector::operator-=(vectorsize_t dec)
{
    if(!data)
        return;

    data->inc(dec);
}

MemVector::MemVector(void *mem, vectorsize_t size)
{
    assert(mem != NULL);
    assert(size > 0);

    data = new((caddr_t)mem) array(size);
}

MemVector::~MemVector()
{
    data = NULL;
}

void MemVector::release(void)
{
    data = NULL;
}

bool MemVector::resize(vectorsize_t size)
{
    return false;
}

void MemVector::cow(vectorsize_t adj)
{
}

ArrayReuse::ArrayReuse(size_t size, unsigned c, void *memory) :
ReusableAllocator()
{
    assert(c > 0 && size > 0 && memory != NULL);

    objsize = size;
    count = 0;
    limit = c;
    used = 0;
    mem = (caddr_t)memory;
}

ArrayReuse::ArrayReuse(size_t size, unsigned c) :
ReusableAllocator()
{
    assert(c > 0 && size > 0);

    objsize = size;
    count = 0;
    limit = c;
    used = 0;
    mem = (caddr_t)malloc(size * c);
    crit(mem != NULL, "vector reuse alloc failed");
}

ArrayReuse::~ArrayReuse()
{
    if(mem) {
        free(mem);
        mem = NULL;
    }
}

bool ArrayReuse::avail(void)
{
    bool rtn = false;
    lock();
    if(count < limit)
        rtn = true;
    unlock();
    return rtn;
}

ReusableObject *ArrayReuse::get(timeout_t timeout)
{
    bool rtn = true;
    struct timespec ts;
    ReusableObject *obj = NULL;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(!freelist && used >= limit && rtn) {
        ++waiting;
        if(timeout == Timer::inf)
            wait();
        else if(timeout)
            rtn = wait(&ts);
        else
            rtn = false;
        --waiting;
    }

    if(!rtn) {
        unlock();
        return NULL;
    }

    if(freelist) {
        obj = freelist;
        freelist = next(obj);
    } else if(used < limit) {
        obj = (ReusableObject *)&mem[used * objsize];
        ++used;
    }
    if(obj)
        ++count;
    unlock();
    return obj;
}

ReusableObject *ArrayReuse::get(void)
{
    return get(Timer::inf);
}

ReusableObject *ArrayReuse::request(void)
{
    ReusableObject *obj = NULL;

    lock();
    if(freelist) {
        obj = freelist;
        freelist = next(obj);
    }
    else if(used < limit) {
        obj = (ReusableObject *)(mem + (used * objsize));
        ++used;
    }
    if(obj)
        ++count;
    unlock();
    return obj;
}

vectorsize_t Vector::size(void **list)
{
    assert(list != NULL);

    vectorsize_t pos = 0;
    while(list[pos])
        ++pos;
    return pos;
}

PagerReuse::PagerReuse(mempager *p, size_t objsize, unsigned c) :
MemoryRedirect(p), ReusableAllocator()
{
    assert(objsize > 0 && c > 0);

    limit = c;
    count = 0;
    osize = objsize;
}

PagerReuse::~PagerReuse()
{
}

bool PagerReuse::avail(void)
{
    bool rtn = false;

    if(!limit)
        return true;

    lock();
    if(count < limit)
        rtn = true;
    unlock();
    return rtn;
}

ReusableObject *PagerReuse::request(void)
{
    ReusableObject *obj = NULL;
    lock();
    if(!limit || count < limit) {
        if(freelist) {
            ++count;
            obj = freelist;
            freelist = next(obj);
        }
        else {
            ++count;
            unlock();
            return (ReusableObject *)_alloc(osize);
        }
    }
    unlock();
    return obj;
}

ReusableObject *PagerReuse::get(void)
{
    return get(Timer::inf);
}

ReusableObject *PagerReuse::get(timeout_t timeout)
{
    bool rtn = true;
    struct timespec ts;
    ReusableObject *obj;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(rtn && limit && count >= limit) {
        ++waiting;
        if(timeout == Timer::inf)
            wait();
        else if(timeout)
            rtn = wait(&ts);
        else
            rtn = false;
        --waiting;
    }
    if(!rtn) {
        unlock();
        return NULL;
    }
    if(freelist) {
        obj = freelist;
        freelist = next(obj);
    }
    else {
        ++count;
        unlock();
        return (ReusableObject *)_alloc(osize);
    }
    if(obj)
        ++count;
    unlock();
    return obj;
}


