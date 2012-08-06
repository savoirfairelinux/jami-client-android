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
#include <cc++/buffer.h>
#include <cstdio>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

const size_t Buffer::timeout = ((size_t)(-1l));

#ifdef  WIN32
Buffer::Buffer(size_t capacity) : Mutex()
#else
Buffer::Buffer(size_t capacity) : Conditional()
#endif
{
#ifdef  WIN32
    sem_head = ::CreateSemaphore((LPSECURITY_ATTRIBUTES)NULL, 0, MAX_SEM_VALUE, (LPCTSTR)NULL);
    sem_tail = ::CreateSemaphore((LPSECURITY_ATTRIBUTES)NULL, (LONG)capacity, MAX_SEM_VALUE, (LPCTSTR)NULL);
#endif
    _size = capacity;
    _used = 0;
}

Buffer::~Buffer()
{
#ifdef  WIN32
    ::CloseHandle(sem_head);
    ::CloseHandle(sem_tail);
#endif
}

#ifdef  WIN32

size_t Buffer::wait(void *buf, timeout_t timeout)
{
    size_t  rc;

    if(!timeout)
            timeout = INFINITE;
    if(Thread::waitThread(sem_head, timeout) != WAIT_OBJECT_0)
        return Buffer::timeout;
    enterMutex();
    rc = onWait(buf);
    --_used;
    leaveMutex();
    ::ReleaseSemaphore(sem_tail, 1, (LPLONG)NULL);
    return rc;
}

size_t Buffer::post(void *buf, timeout_t timeout)
{
    size_t  rc;

    if(!timeout)
            timeout = INFINITE;

    if(Thread::waitThread(sem_tail, timeout) != WAIT_OBJECT_0)
        return Buffer::timeout;
    enterMutex();
    rc = onPost(buf);
    ++_used;
    leaveMutex();
    ::ReleaseSemaphore(sem_head, 1, (LPLONG)NULL);
    return rc;
}

#else

size_t Buffer::wait(void *buf, timeout_t timeout)
{
    size_t rc = 0;
    enterMutex();
    while(!_used) {
        if(!Conditional::wait(timeout, true)) {
            leaveMutex();
            return Buffer::timeout;
        }
    }
    rc = (ssize_t)onWait(buf);
    --_used;
    Conditional::signal(false);
    leaveMutex();
    return rc;
}

size_t Buffer::post(void *buf, timeout_t timeout)
{
    size_t rc = 0;

    enterMutex();
    while(_used == _size) {
        if(!Conditional::wait(timeout, true)) {
            leaveMutex();
            return Buffer::timeout;
        }
    }
    rc = (ssize_t)onPost(buf);
    ++_used;
    Conditional::signal(false);
    leaveMutex();
    return rc;
}

size_t Buffer::peek(void *buf)
{
    size_t rc;

    enterMutex();
    if(!_used) {
        leaveMutex();
        return 0;
    }
    rc = onPeek(buf);
    leaveMutex();
    return rc;
}

#endif

bool Buffer::isValid(void)
{
    return true;
}

FixedBuffer::FixedBuffer(size_t capacity, size_t osize) :
Buffer(capacity)
{
    objsize = osize;
    buf = new char[capacity * objsize];

#ifdef  CCXX_EXCEPTIONS
    if(!buf && Thread::getException() == Thread::throwObject)
        throw(this);
#ifdef  COMMON_STD_EXCEPTION
    else if(!buf && Thread::getException() == Thread::throwException)
        throw(SyncException("fixed buffer failure"));
#endif
#endif

    head = tail = buf;
}

FixedBuffer::~FixedBuffer()
{
    if(buf)
        delete[] buf;
}

bool FixedBuffer::isValid(void)
{
    if(head && tail)
        return true;

    return false;
}

#define MAXBUF  (buf + (getSize() * objsize))

size_t FixedBuffer::onWait(void *data)
{
    memcpy(data, head, objsize);
    if((head += objsize) >= MAXBUF)
        head = buf;
    return objsize;
}

size_t FixedBuffer::onPost(void *data)
{
    memcpy(tail, data, objsize);
    if((tail += objsize) >= MAXBUF)
        tail = buf;
    return objsize;
}

size_t FixedBuffer::onPeek(void *data)
{
    memcpy(data, head, objsize);
    return objsize;
}

ThreadQueue::ThreadQueue(const char *id, int pri, size_t stack) :
Mutex(), Thread(pri, stack), Semaphore(), name(id)
{
    first = last = NULL;
    started = false;
    timeout = 0;
}

ThreadQueue::~ThreadQueue()
{
    data_t *data, *next;
    if(started) {
        started = false;
    }
    data = first;
    while(data) {
        next = data->next;
        delete[] data;
        data = next;
    }
}

void ThreadQueue::run(void)
{
    bool posted;
    data_t *prev;
    started = true;
    for(;;) {
        posted = Semaphore::wait(timeout);
        if(!posted) {
            onTimer();
            if(!first)
                continue;
        }
        if(!started)
            sleep((timeout_t)~0);
        startQueue();
        while(first) {
            runQueue(first->data);
            enterMutex();
            prev = first;
            first = first->next;
            delete[] prev;
            if(!first)
                last = NULL;
            leaveMutex();
            if(first)
                Semaphore::wait(); // demark semaphore
        }
        stopQueue();
    }
}

void ThreadQueue::final()
{
#ifndef WIN32
    // Unlock is needed to unlock the mutex in the case of a cancel during Semaphore::wait()
    // see PTHREAD_COND_TIMEDWAIT(3P)
    Semaphore::force_unlock_after_cancellation();
#endif
}

void ThreadQueue::onTimer(void)
{
}

void ThreadQueue::setTimer(timeout_t timed)
{
    enterMutex();
    timeout = timed;
    leaveMutex();
    if(!started) {
        start();
        started = true;
    }
    else if(!first)
        Semaphore::post();
}

void ThreadQueue::post(const void *dp, unsigned len)
{
    data_t *data = (data_t *)new char[sizeof(data_t) + len];
    memcpy(data->data, dp, len);
    data->len = len;
    data->next = NULL;
    enterMutex();
    if(!first)
        first = data;
    if(last)
        last->next = data;
    last = data;
    if(!started) {
        start();
        started = true;
    }
    leaveMutex();
    Semaphore::post();
}

void ThreadQueue::startQueue(void)
{
}

void ThreadQueue::stopQueue(void)
{
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
