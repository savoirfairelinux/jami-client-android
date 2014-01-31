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
#include <ucommon/memory.h>
#include <ucommon/thread.h>
#include <ucommon/containers.h>
#include <string.h>

using namespace UCOMMON_NAMESPACE;

LinkedAllocator::LinkedAllocator() : Conditional()
{
    freelist = NULL;
}

LinkedObject *LinkedAllocator::get(void)
{
    LinkedObject *node;
    lock();
    node = freelist;
    if(node)
        freelist = freelist->getNext();
    unlock();
    return node;
}

LinkedObject *LinkedAllocator::get(timeout_t timeout)
{
    struct timespec ts;
    bool rtn = true;
    LinkedObject *node = NULL;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(!freelist && rtn) {
        if(timeout == Timer::inf)
            Conditional::wait();
        else if(timeout)
            rtn = Conditional::wait(&ts);
        else
            rtn = false;
    }
    if(rtn && freelist) {
        node = freelist;
        if(node)
            freelist = freelist->getNext();
    }
    unlock();
    return node;
}

void LinkedAllocator::release(LinkedObject *node)
{
    lock();
    node->enlist(&freelist);
    signal();
    unlock();
}

LinkedAllocator::operator bool()
{
    bool rtn = false;

    lock();
    if(freelist)
        rtn = true;
    unlock();
    return rtn;
}

bool LinkedAllocator::operator!()
{
    bool rtn = false;

    lock();
    if(!freelist)
        rtn = true;
    unlock();
    return rtn;
}

Buffer::Buffer(size_t osize, size_t c) :
Conditional()
{
    assert(osize > 0 && c > 0);

    bufsize = osize * c;
    objsize = osize;
    objcount = 0;
    limit = c;

    if(osize) {
        buf = (char *)malloc(bufsize);
        crit(buf != NULL, "buffer alloc failed");
    }
    else
        buf = NULL;

    head = tail = buf;
}

Buffer::~Buffer()
{
    if(buf)
        free(buf);
    buf = NULL;
}

unsigned Buffer::count(void)
{
    unsigned bcount = 0;

    lock();
    if(tail > head)
        bcount = (unsigned)((size_t)(tail - head) / objsize);
    else if(head > tail)
        bcount = (unsigned)((((buf + bufsize) - head) + (tail - buf)) / objsize);
    unlock();
    return bcount;
}

unsigned Buffer::size(void)
{
    return bufsize / objsize;
}

void *Buffer::get(void)
{
    caddr_t dbuf;

    lock();
    while(!objcount)
        wait();
    dbuf = head;
    unlock();
    return dbuf;
}

void *Buffer::invalid(void) const
{
    return NULL;
}

void *Buffer::peek(unsigned offset)
{
    caddr_t dbuf;

    lock();
    if(offset >= objcount) {
        unlock();
        return invalid();
    }

    dbuf = head + (objsize * offset);
    if(dbuf >= buf + bufsize)
        dbuf -= bufsize;
    unlock();
    return dbuf;
}

void Buffer::copy(void *data)
{
    assert(data != NULL);

    void *ptr = get();
    memcpy(data, ptr, objsize);
    release();
}

bool Buffer::copy(void *data, timeout_t timeout)
{
    assert(data != NULL);

    void *ptr = get(timeout);
    if(!ptr)
        return false;

    memcpy(data, ptr, objsize);
    release();
    return true;
}

void *Buffer::get(timeout_t timeout)
{
    caddr_t dbuf = NULL;
    struct timespec ts;
    bool rtn = true;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(!objcount && rtn) {
        if(timeout == Timer::inf)
            wait();
        else if(timeout)
            rtn = wait(&ts);
        else
            rtn = false;
    }
    if(objcount && rtn)
        dbuf = head;
    unlock();
    return dbuf;
}

void Buffer::release(void)
{
    lock();
    head += objsize;
    if(head >= buf + bufsize)
        head = buf;
    --objcount;
    signal();
    unlock();
}

void Buffer::put(void *dbuf)
{
    assert(dbuf != NULL);

    lock();
    while(objcount == limit)
        wait();
    memcpy(tail, dbuf, objsize);
    tail += objsize;
    if(tail >= (buf + bufsize))
        tail = buf;
    ++objcount;
    signal();
    unlock();
}

bool Buffer::put(void *dbuf, timeout_t timeout)
{
    assert(dbuf != NULL);

    bool rtn = true;
    struct timespec ts;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(objcount == limit && rtn) {
        if(timeout == Timer::inf)
            wait();
        else if(timeout)
            rtn = wait(&ts);
        else
            rtn = false;
    }
    if(rtn && objcount < limit) {
        memcpy(tail, dbuf, objsize);
        tail += objsize;
        if(tail >= (buf + bufsize))
            tail = 0;
        ++objcount;
        signal();
    }
    unlock();
    return rtn;
}


Buffer::operator bool()
{
    bool rtn = false;

    lock();
    if(buf && head != tail)
        rtn = true;
    unlock();
    return rtn;
}

bool Buffer::operator!()
{
    bool rtn = false;

    lock();
    if(!buf || head == tail)
        rtn = true;
    unlock();
    return rtn;
}

Queue::member::member(Queue *q, ObjectProtocol *o) :
OrderedObject(q)
{
    assert(o != NULL);

    o->retain();
    object = o;
}

Queue::Queue(mempager *p, size_t size) :
OrderedIndex(), Conditional()
{
    assert(size > 0);

    pager = p;
    freelist = NULL;
    used = 0;
    limit = size;
}

Queue::~Queue()
{
    linked_pointer<member> mp;
    OrderedObject *next;

    if(pager)
        return;

    mp = freelist;
    while(is(mp)) {
        next = mp->getNext();
        delete *mp;
        mp = next;
    }

    mp = head;
    while(is(mp)) {
        next = mp->getNext();
        delete *mp;
        mp = next;
    }
}

bool Queue::remove(ObjectProtocol *o)
{
    assert(o != NULL);

    bool rtn = false;
    linked_pointer<member> node;
    lock();
    node = begin();
    while(node) {
        if(node->object == o)
            break;
        node.next();
    }
    if(node) {
        --used;
        rtn = true;
        node->object->release();
        node->delist(this);
        node->LinkedObject::enlist(&freelist);
    }
    unlock();
    return rtn;
}

ObjectProtocol *Queue::lifo(timeout_t timeout)
{
    struct timespec ts;
    bool rtn = true;
    member *member;
    ObjectProtocol *obj = NULL;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(!tail && rtn) {
        if(timeout == Timer::inf)
            Conditional::wait();
        else if(timeout)
            rtn = Conditional::wait(&ts);
        else
            rtn = false;
    }
    if(rtn && tail) {
        --used;
        member = static_cast<Queue::member *>(tail);
        obj = member->object;
        member->delist(this);
        member->LinkedObject::enlist(&freelist);
    }
    if(rtn)
        signal();
    unlock();
    return obj;
}

ObjectProtocol *Queue::fifo(timeout_t timeout)
{
    bool rtn = true;
    struct timespec ts;
    linked_pointer<member> node;
    ObjectProtocol *obj = NULL;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(rtn && !head) {
        if(timeout == Timer::inf)
            Conditional::wait();
        else if(timeout)
            rtn = Conditional::wait(&ts);
        else
            rtn = false;
    }

    if(rtn && head) {
        --used;
        node = begin();
        obj = node->object;
        head = head->getNext();
        if(!head)
            tail = NULL;
        node->LinkedObject::enlist(&freelist);
    }
    if(rtn)
        signal();
    unlock();
    return obj;
}

ObjectProtocol *Queue::invalid(void) const
{
    return NULL;
}

ObjectProtocol *Queue::get(unsigned back)
{
    linked_pointer<member> node;
    ObjectProtocol *obj;

    lock();

    node = begin();

    do {
        if(!is(node)) {
            obj = invalid();
            break;
        }
        obj = node->object;
        node.next();

    } while(back-- > 0);

    unlock();
    return obj;
}

bool Queue::post(ObjectProtocol *object, timeout_t timeout)
{
    assert(object != NULL);

    bool rtn = true;
    struct timespec ts;
    LinkedObject *mem;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(rtn && limit && used == limit) {
        if(timeout == Timer::inf)
            Conditional::wait();
        else if(timeout)
            rtn = Conditional::wait(&ts);
        else
            rtn = false;
    }

    if(!rtn) {
        unlock();
        return false;
    }

    ++used;
    if(freelist) {
        mem = freelist;
        freelist = freelist->getNext();
        new((caddr_t)mem) member(this, object);
    }
    else {
        if(pager)
            new((caddr_t)(pager->alloc(sizeof(member)))) member(this, object);
        else
            new member(this, object);
    }
    signal();
    unlock();
    return true;
}

size_t Queue::count(void)
{
    size_t qcount;
    lock();
    qcount = used;
    unlock();
    return qcount;
}


Stack::member::member(Stack *S, ObjectProtocol *o) :
LinkedObject((&S->usedlist))
{
    assert(o != NULL);

    o->retain();
    object = o;
}

Stack::Stack(mempager *p, size_t size) :
Conditional()
{
    assert(size > 0);

    pager = p;
    freelist = usedlist = NULL;
    limit = size;
    used = 0;
}

Stack::~Stack()
{
    linked_pointer<member> mp;
    LinkedObject *next;

    if(pager)
        return;

    mp = freelist;
    while(is(mp)) {
        next = mp->getNext();
        delete *mp;
        mp = next;
    }

    mp = usedlist;
    while(is(mp)) {
        next = mp->getNext();
        delete *mp;
        mp = next;
    }
}

bool Stack::remove(ObjectProtocol *o)
{
    assert(o != NULL);

    bool rtn = false;
    linked_pointer<member> node;
    lock();
    node = static_cast<member*>(usedlist);
    while(node) {
        if(node->object == o)
            break;
        node.next();
    }
    if(node) {
        --used;
        rtn = true;
        node->object->release();
        node->delist(&usedlist);
        node->enlist(&freelist);
    }
    unlock();
    return rtn;
}

ObjectProtocol *Stack::invalid(void) const
{
    return NULL;
}

ObjectProtocol *Stack::get(unsigned back)
{
    linked_pointer<member> node;
    ObjectProtocol *obj;

    lock();
    node = usedlist;

    do {
        if(!is(node)) {
            obj = invalid();
            break;
        }
        obj = node->object;
        node.next();

    } while(back-- > 0);

    unlock();
    return obj;
}

const ObjectProtocol *Stack::peek(timeout_t timeout)
{
    bool rtn = true;
    struct timespec ts;
    member *member;
    ObjectProtocol *obj = NULL;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(rtn && !usedlist) {
        if(timeout == Timer::inf)
            Conditional::wait();
        else if(timeout)
            rtn = Conditional::wait(&ts);
        else
            rtn = false;
    }
    if(!rtn) {
        unlock();
        return NULL;
    }
    if(usedlist) {
        member = static_cast<Stack::member *>(usedlist);
        obj = member->object;
    }
    if(rtn)
        signal();
    unlock();
    return obj;
}

ObjectProtocol *Stack::pull(timeout_t timeout)
{
    bool rtn = true;
    struct timespec ts;
    member *member;
    ObjectProtocol *obj = NULL;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(rtn && !usedlist) {
        if(timeout == Timer::inf)
            Conditional::wait();
        else if(timeout)
            rtn = Conditional::wait(&ts);
        else
            rtn = false;
    }
    if(!rtn) {
        unlock();
        return NULL;
    }
    if(usedlist) {
        member = static_cast<Stack::member *>(usedlist);
        obj = member->object;
        usedlist = member->getNext();
        member->enlist(&freelist);
    }
    if(rtn)
        signal();
    unlock();
    return obj;
}

bool Stack::push(ObjectProtocol *object, timeout_t timeout)
{
    assert(object != NULL);

    bool rtn = true;
    struct timespec ts;
    LinkedObject *mem;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(rtn && limit && used == limit) {
        if(timeout == Timer::inf)
            Conditional::wait();
        else if(timeout)
            rtn = Conditional::wait(&ts);
        else
            rtn = false;
    }

    if(!rtn) {
        unlock();
        return false;
    }

    ++used;
    if(freelist) {
        mem = freelist;
        freelist = freelist->getNext();
        new((caddr_t)mem) member(this, object);
    }
    else {
        if(pager) {
            caddr_t ptr = (caddr_t)pager->alloc(sizeof(member));
            new(ptr) member(this, object);
        }
        else
            new member(this, object);
    }
    signal();
    unlock();
    return true;
}

size_t Stack::count(void)
{
    size_t scount;
    lock();
    scount = used;
    unlock();
    return scount;
}


