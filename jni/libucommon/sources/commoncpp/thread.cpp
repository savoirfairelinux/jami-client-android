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

#include <ucommon-config.h>
#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/string.h>
#include <commoncpp/exception.h>
#include <commoncpp/thread.h>

#ifdef  _MSWINDOWS_
#define MAX_SEM_VALUE 1000000
#endif

using namespace COMMONCPP_NAMESPACE;

#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
extern int _posix_clocking;
#endif

static class __EXPORT MainThread : public Thread
{
private:
    void run(void);

public:
    MainThread();

    ~MainThread();
} _mainthread;

extern "C" {
#ifdef  _MSWINDOWS_
    static unsigned __stdcall exec_thread(void *obj) {
        assert(obj != NULL);

        ucommon::Thread *th = static_cast<ucommon::Thread *>(obj);
        Thread *cth = static_cast<Thread *>(obj);
        th->setPriority();
        cth->map();
        cth->initial();
        cth->run();
        cth->finalize();
        cth->exit();
        return 0;
    }
#else
    static void *exec_thread(void *obj)
    {
        assert(obj != NULL);

        ucommon::Thread *th = static_cast<ucommon::Thread *>(obj);
        Thread *cth = static_cast<Thread *>(obj);
        th->setPriority();
        cth->map();
        cth->initial();
        cth->run();
        cth->finalize();
        cth->exit();
        return NULL;
    }
#endif
}

MainThread::MainThread() : Thread()
{
    ucommon::Thread::init();
    ucommon::Socket::init();
    map();
}

MainThread::~MainThread()
{
}

void MainThread::run(void)
{
}

Thread::Thread(int pri, size_t stack) : ucommon::JoinableThread(stack)
{
    priority = pri;
    detached = false;
    terminated = false;
    msgpos = 0;

    if(this == &_mainthread) {
        parent = this;
        exceptions = throwObject;
    }
    else {
        parent = Thread::get();
        if(!parent)
            parent = &_mainthread;
        exceptions = parent->exceptions;
    }
}

Thread::~Thread()
{
    if(!detached)
        join();
    finalize();
}

bool Thread::isRunning(void)
{
    return !detached && running && !terminated;
}

void Thread::finalize(void)
{
    if(terminated)
        return;

    terminated = true;
    if(parent)
        parent->notify(this);
    final();
}

bool Thread::isThread(void)
{
    pthread_t self = pthread_self();

    if(equal(tid, self))
        return true;

    return false;
}

void Thread::terminate(void)
{
    pthread_t self = pthread_self();

    if(detached && equal(tid, self))
        ucommon::Thread::exit();
    else if(!detached)
        join();
}

void Thread::exit(void)
{
    pthread_t self = pthread_self();

    if(detached && equal(tid, self)) {
        delete this;
        ucommon::Thread::exit();
    }
    terminate();
}

void Thread::initial(void)
{
}

void Thread::final(void)
{
}

void Thread::notify(Thread *thread)
{
}

#ifdef  _MSWINDOWS_

void Thread::start(void)
{
    if(running != INVALID_HANDLE_VALUE)
        return;

    if(stack == 1)
        stack = 1024;

    running = (HANDLE)_beginthreadex(NULL, stack, &exec_thread, this, 0, (unsigned int *)&tid);
    if(!running)
        running = INVALID_HANDLE_VALUE;
    else
        terminated = false;
}

void Thread::detach(void)
{
    HANDLE hThread;;

    if(stack == 1)
        stack = 1024;

    hThread = (HANDLE)_beginthreadex(NULL, stack, &exec_thread, this, 0, (unsigned int *)&tid);
    CloseHandle(hThread);
}

#else

void Thread::start(void)
{
    int result;

    if(running)
        return;

#ifndef __PTH__
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
    #ifndef ANDROID
    pthread_attr_setinheritsched(&attr, PTHREAD_INHERIT_SCHED);
    #endif
#endif
// we typically use "stack 1" for min stack...
#ifdef  PTHREAD_STACK_MIN
    if(stack && stack < PTHREAD_STACK_MIN)
        stack = PTHREAD_STACK_MIN;
#else
    if(stack && stack < 2)
        stack = 0;
#endif
#ifdef  __PTH__
    pth_attr_t attr = PTH_ATTR_DEFAULT;
    pth_attr_set(attr, PTH_ATTR_JOINABLE);
    tid = pth_spawn(attr, &exec_thread, this);
#else
    if(stack)
        pthread_attr_setstacksize(&attr, stack);
    result = pthread_create(&tid, &attr, &exec_thread, this);
    pthread_attr_destroy(&attr);
    if(!result) {
        terminated = false;
        running = true;
    }
#endif
}

void Thread::detach(void)
{
#ifndef __PTH__
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    #ifndef ANDROID
    pthread_attr_setinheritsched(&attr, PTHREAD_INHERIT_SCHED);
    #endif
#endif
// we typically use "stack 1" for min stack...
#ifdef  PTHREAD_STACK_MIN
    if(stack && stack < PTHREAD_STACK_MIN)
        stack = PTHREAD_STACK_MIN;
#else
    if(stack && stack < 2)
        stack = 0;
#endif
#ifdef  __PTH__
    tid = pth_spawn(PTH_ATTR_DEFAULT, &exec_thread, this);
#else
    if(stack)
        pthread_attr_setstacksize(&attr, stack);
    pthread_create(&tid, &attr, &exec_thread, this);
    pthread_attr_destroy(&attr);
#endif
}

#endif

Thread::Throw Thread::getException(void)
{
    Thread *th = Thread::get();
    if(!th)
        return throwNothing;
    return th->exceptions;
}

void Thread::setException(Thread::Throw _throw)
{
    Thread *th = Thread::get();
    if(th)
        th->exceptions = _throw;
}

void Conditional::signal(bool broadcast)
{
    if(broadcast)
        ucommon::Conditional::broadcast();
    else
        ucommon::Conditional::signal();
}

bool Conditional::wait(timeout_t timeout, bool locked)
{
    bool result;
    if(!locked)
        ucommon::Conditional::lock();
    result = ucommon::Conditional::wait(timeout);
    if(!locked)
        ucommon::Conditional::unlock();
    return result;
}

static pthread_mutex_t mlock;

time_t SysTime::getTime(time_t *tloc)
{
    time_t ret;
    pthread_mutex_lock(&mlock);
    time_t temp;
    ::time(&temp);
    memcpy(&ret, &temp, sizeof(time_t));
    if (tloc != NULL)
        memcpy(tloc, &ret, sizeof(time_t));
    pthread_mutex_unlock(&mlock);
    return ret;
}

int SysTime::getTimeOfDay(struct timeval *tp)
{
    struct timeval temp;
    int ret(0);
    pthread_mutex_lock(&mlock);

#ifdef  _MSWINDOWS_
    // We could use _ftime(), but it is not available on WinCE.
    // (WinCE also lacks time.h)
    // Note also that the average error of _ftime is around 20 ms :)
    time_t now;
    time(&now);
    temp.tv_sec = (long)now;
    temp.tv_usec = (GetTickCount() % 1000) * 1000;
    memcpy(tp, &temp, sizeof(struct timeval));
#else
    ret = ::gettimeofday(&temp, NULL);
    if(ret == 0)
        memcpy(tp, &temp, sizeof(struct timeval));
#endif

    pthread_mutex_unlock(&mlock);
    return ret;
}

struct tm *SysTime::getLocalTime(const time_t *clock, struct tm* result)
{
    pthread_mutex_lock(&mlock);
    struct tm *temp = ::localtime(clock);
    memcpy(result, temp, sizeof(struct tm));
    pthread_mutex_unlock(&mlock);
    return result;
}

struct tm *SysTime::getGMTTime(const time_t *clock, struct tm* result)
{
    pthread_mutex_lock(&mlock);
    struct tm *temp = ::gmtime(clock);
    memcpy(result, temp, sizeof(struct tm));
    pthread_mutex_unlock(&mlock);
    return result;
}

#ifndef _MSWINDOWS_

#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
TimerPort::TimerPort()
{
    struct timespec ts;
    active = false;

    ::clock_gettime(_posix_clocking, &ts);
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
#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
    struct timespec ts;
    ::clock_gettime(_posix_clocking, &ts);
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
    if(timer.tv_usec >= 1000000l) {
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

#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
void TimerPort::sleepTimer(void)
{
    struct timespec ts;
    ts.tv_sec = timer.tv_sec;
    ts.tv_nsec = timer.tv_usec * 1000l;

    ::clock_nanosleep(_posix_clocking, TIMER_ABSTIME, &ts, NULL);
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
#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
    struct timespec now;
#else
    struct timeval now;
#endif
    long diff;

    if(!active)
        return TIMEOUT_INF;

#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
    ::clock_gettime(_posix_clocking, &now);
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
#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
    struct timespec now;
#else
    struct timeval now;
#endif
    long diff;

    if(!active)
        return TIMEOUT_INF;

#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
    ::clock_gettime(_posix_clocking, &now);
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

MutexCounter::MutexCounter() : Mutex()
{
    counter = 0;
}

MutexCounter::MutexCounter(int initial) : Mutex()
{
    counter = initial;
}

int MutexCounter::operator++()
{
    int rtn;

    enterMutex();
    rtn = counter++;
    leaveMutex();
    return rtn;
}

// ??? why cannot be < 0 ???
int MutexCounter::operator--()
{
    int rtn = 0;

    enterMutex();
    if(counter) {
        rtn = --counter;
        if(!rtn) {
            leaveMutex();
            THROW(mc);
        }
    }
    leaveMutex();
    return rtn;
}

#ifndef _MSWINDOWS_

timespec *getTimeout(struct timespec *spec, timeout_t timer)
{
    static  struct timespec myspec;

    if(spec == NULL)
        spec = &myspec;

#ifdef  PTHREAD_GET_EXPIRATION_NP
    struct timespec offset;

    offset.tv_sec = timer / 1000;
    offset.tv_nsec = (timer % 1000) * 1000000;
    pthread_get_expiration_np(&offset, spec);
#else
    struct timeval current;

    SysTime::getTimeOfDay(&current);
    spec->tv_sec = current.tv_sec + ((timer + current.tv_usec / 1000) / 1000);
    spec->tv_nsec = ((current.tv_usec / 1000 + timer) % 1000) * 1000000;

#endif
    return spec;
}

#endif

const size_t Buffer::timeout = ((size_t)(-1l));

#ifdef  _MSWINDOWS_
Buffer::Buffer(size_t capacity) : Mutex()
#else
Buffer::Buffer(size_t capacity) : Conditional()
#endif
{
#ifdef  _MSWINDOWS_
    sem_head = ::CreateSemaphore((LPSECURITY_ATTRIBUTES)NULL, 0, MAX_SEM_VALUE, (LPCTSTR)NULL);
    sem_tail = ::CreateSemaphore((LPSECURITY_ATTRIBUTES)NULL, (LONG)capacity, MAX_SEM_VALUE, (LPCTSTR)NULL);
#endif
    _size = capacity;
    _used = 0;
}

Buffer::~Buffer()
{
#ifdef  _MSWINDOWS_
    ::CloseHandle(sem_head);
    ::CloseHandle(sem_tail);
#endif
}

#ifdef  _MSWINDOWS_

size_t Buffer::wait(void *buf, timeout_t timeout)
{
    size_t  rc;

    if(!timeout)
            timeout = INFINITE;
    if(WaitForSingleObject(sem_head, timeout) != WAIT_OBJECT_0)
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

    if(WaitForSingleObject(sem_tail, timeout) != WAIT_OBJECT_0)
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
    else if(!buf && Thread::getException() == Thread::throwException)
        throw(SyncException("fixed buffer failure"));
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
Mutex(), Thread(pri, stack), Semaphore(0), name(id)
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

