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
#include <ucommon/object.h>
#include <ucommon/thread.h>
#include <ucommon/timers.h>
#include <ucommon/linked.h>
#include <errno.h>
#include <string.h>
#include <stdarg.h>
#include <limits.h>

#if _POSIX_PRIORITY_SCHEDULING > 0
#include <sched.h>
static int realtime_policy = SCHED_FIFO;
#endif

#undef  _POSIX_SPIN_LOCKS

static unsigned max_sharing = 0;

using namespace UCOMMON_NAMESPACE;

#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
extern int _posix_clocking;
int _posix_clocking = CLOCK_REALTIME;
#endif

struct mutex_entry
{
    pthread_mutex_t mutex;
    struct mutex_entry *next;
    const void *pointer;
    unsigned count;
};

class __LOCAL rwlock_entry : public ThreadLock
{
public:
    rwlock_entry();
    rwlock_entry *next;
    const void *object;
    unsigned count;
};

class __LOCAL mutex_index : public Mutex
{
public:
    struct mutex_entry *list;

    mutex_index();
};

class __LOCAL rwlock_index : public Mutex
{
public:
    rwlock_entry *list;

    rwlock_index();
};

static rwlock_index single_rwlock;
static rwlock_index *rwlock_table = &single_rwlock;
static mutex_index single_table;
static mutex_index *mutex_table = &single_table;
static unsigned mutex_indexing = 1;
static unsigned rwlock_indexing = 1;

#ifdef  __PTH__
static pth_key_t threadmap;
#else
#ifdef  _MSWINDOWS_
static DWORD threadmap;
#else
static pthread_key_t threadmap;
#endif
#endif

mutex_index::mutex_index() : Mutex()
{
    list = NULL;
}

rwlock_index::rwlock_index() : Mutex()
{
    list = NULL;
}

rwlock_entry::rwlock_entry() : ThreadLock()
{
    count = 0;
}

#if !defined(_MSWINDOWS_) && !defined(__PTH__)
Conditional::attribute Conditional::attr;
#endif

#ifdef  __PTH__
static int pthread_cond_timedwait(pthread_cond_t *cond, pthread_mutex_t *mutex, const struct timespec *abstime)
{
    static pth_key_t ev_key = PTH_KEY_INIT;
    pth_event_t ev = pth_event(PTH_EVENT_TIME|PTH_MODE_STATIC, &ev_key,
        pth_time(abstime->tv_sec, (abstime->tv_nsec) / 1000));

    if(!pth_cond_await(cond, mutex, ev))
        return errno;
    return 0;
}

static void pthread_shutdown(void)
{
    pth_kill();
}

inline pthread_t pthread_self(void)
    {return pth_self();};

#endif

static unsigned hash_address(const void *ptr, unsigned indexing)
{
    assert(ptr != NULL);

    unsigned key = 0;
    unsigned count = 0;
    const unsigned char *addr = (unsigned char *)(&ptr);

    if(indexing < 2)
        return 0;

    // skip lead zeros if little endian...
    while(count < sizeof(const void *) && *addr == 0) {
        ++count;
        ++addr;
    }

    while(count++ < sizeof(const void *) && *addr)
        key = (key << 1) ^ *(addr++);

    return key % indexing;
}

ReusableAllocator::ReusableAllocator() :
Conditional()
{
    freelist = NULL;
    waiting = 0;
}

void ReusableAllocator::release(ReusableObject *obj)
{
    assert(obj != NULL);

    LinkedObject **ru = (LinkedObject **)&freelist;

    obj->retain();
    obj->release();

    lock();
    obj->enlist(ru);

    if(waiting)
        signal();

    unlock();
}

void ConditionalAccess::limit_sharing(unsigned max)
{
    max_sharing = max;
}

void Conditional::set(struct timespec *ts, timeout_t msec)
{
    assert(ts != NULL);

#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
    clock_gettime(_posix_clocking, ts);
#else
    timeval tv;
    gettimeofday(&tv, NULL);
    ts->tv_sec = tv.tv_sec;
    ts->tv_nsec = tv.tv_usec * 1000l;
#endif
    ts->tv_sec += msec / 1000;
    ts->tv_nsec += (msec % 1000) * 1000000l;
    while(ts->tv_nsec >= 1000000000l) {
        ++ts->tv_sec;
        ts->tv_nsec -= 1000000000l;
    }
}

Semaphore::Semaphore(unsigned limit) :
Conditional()
{
    assert(limit > 0);

    count = limit;
    waits = 0;
    used = 0;
}

void Semaphore::_share(void)
{
    wait();
}

void Semaphore::_unlock(void)
{
    release();
}

bool Semaphore::wait(timeout_t timeout)
{
    bool result = true;
    struct timespec ts;
    Conditional::set(&ts, timeout);

    lock();
    while(used >= count && result) {
        ++waits;
        result = Conditional::wait(&ts);
        --waits;
    }
    if(result)
        ++used;
    unlock();
    return result;
}

void Semaphore::wait(void)
{
    lock();
    if(used >= count) {
        ++waits;
        Conditional::wait();
        --waits;
    }
    ++used;
    unlock();
}

void Semaphore::release(void)
{
    lock();
    if(used)
        --used;
    if(waits)
        signal();
    unlock();
}

void Semaphore::set(unsigned value)
{
    assert(value > 0);

    unsigned diff;

    lock();
    count = value;
    if(used >= count || !waits) {
        unlock();
        return;
    }
    diff = count - used;
    if(diff > waits)
        diff = waits;
    unlock();
    while(diff--) {
        lock();
        signal();
        unlock();
    }
}

#ifdef  _MSWINDOWS_

bool Thread::equal(pthread_t t1, pthread_t t2)
{
    return t1 == t2;
}

#ifdef  _MSCONDITIONAL_

Conditional::Conditional()
{
    InitializeCriticalSection(&mutex);
    InitializeConditionVariable(&cond);
}

Conditional::~Conditional()
{
    DeleteCriticalSection(&mutex);
}

void Conditional::wait(void)
{
    SleepConditionVariableCS(&cond, &mutex, INFINITE);
}

bool Conditional::wait(timeout_t timeout)
{
    if(SleepConditionVariableCS(&cond, &mutex, timeout))
        return true;

    return false;
}

void Conditional::signal(void)
{
    WakeConditionVariable(&cond);
}

void Conditional::broadcast(void)
{
    WakeAllConditionVariable(&cond);
}

#else
void Conditional::wait(void)
{
    int result;

    EnterCriticalSection(&mlock);
    ++waiting;
    LeaveCriticalSection(&mlock);
    LeaveCriticalSection(&mutex);
    result = WaitForMultipleObjects(2, events, FALSE, INFINITE);
    EnterCriticalSection(&mlock);
    --waiting;
    result = ((result == WAIT_OBJECT_0 + BROADCAST) && (waiting == 0));
    LeaveCriticalSection(&mlock);
    if(result)
        ResetEvent(&events[BROADCAST]);
    EnterCriticalSection(&mutex);
}

void Conditional::signal(void)
{
    EnterCriticalSection(&mlock);
    if(waiting)
        SetEvent(&events[SIGNAL]);
    LeaveCriticalSection(&mlock);
}

void Conditional::broadcast(void)
{
    EnterCriticalSection(&mlock);
    if(waiting)
        SetEvent(&events[BROADCAST]);
    LeaveCriticalSection(&mlock);

}

Conditional::Conditional()
{
    waiting = 0;

    InitializeCriticalSection(&mutex);
    InitializeCriticalSection(&mlock);
    events[SIGNAL] = CreateEvent(NULL, FALSE, FALSE, NULL);
    events[BROADCAST] = CreateEvent(NULL, TRUE, FALSE, NULL);
}

Conditional::~Conditional()
{
    DeleteCriticalSection(&mlock);
    DeleteCriticalSection(&mutex);
    CloseHandle(events[SIGNAL]);
    CloseHandle(events[BROADCAST]);
}

bool Conditional::wait(timeout_t timeout)
{
    int result;
    bool rtn = true;

    if(!timeout)
        return false;

    EnterCriticalSection(&mlock);
    ++waiting;
    LeaveCriticalSection(&mlock);
    LeaveCriticalSection(&mutex);
    result = WaitForMultipleObjects(2, events, FALSE, timeout);
    EnterCriticalSection(&mlock);
    --waiting;
    if(result == WAIT_OBJECT_0 || result == WAIT_OBJECT_0 + BROADCAST)
        rtn = true;
    result = ((result == WAIT_OBJECT_0 + BROADCAST) && (waiting == 0));
    LeaveCriticalSection(&mlock);
    if(result)
        ResetEvent(&events[BROADCAST]);
    EnterCriticalSection(&mutex);
    return rtn;
}
#endif

bool Conditional::wait(struct timespec *ts)
{
    assert(ts != NULL);

    return wait((timeout_t)(ts->tv_sec * 1000 + (ts->tv_nsec / 1000000l)));
}

#else

#include <stdio.h>
bool Thread::equal(pthread_t t1, pthread_t t2)
{
#ifdef  __PTH__
    return (t1 == t2);
#else
    return pthread_equal(t1, t2) != 0;
#endif
}

#ifndef __PTH__
Conditional::attribute::attribute()
{
    Thread::init();
    pthread_condattr_init(&attr);
#if _POSIX_TIMERS > 0 && defined(HAVE_PTHREAD_CONDATTR_SETCLOCK) && defined(POSIX_TIMERS)
#if defined(_POSIX_MONOTONIC_CLOCK)
    if(!pthread_condattr_setclock(&attr, CLOCK_MONOTONIC))
        _posix_clocking = CLOCK_MONOTONIC;
#else
    pthread_condattr_setclock(&attr, CLOCK_REALTIME);
#endif
#endif
}
#endif

Conditional::Conditional()
{
#ifdef  __PTH__
    Thread::init();
    pth_cond_init(&cond);
    pth_mutex_init(&mutex);
#else
    crit(pthread_cond_init(&cond, &attr.attr) == 0, "conditional init failed");
    crit(pthread_mutex_init(&mutex, NULL) == 0, "mutex init failed");
#endif
}

Conditional::~Conditional()
{
#ifndef __PTH__
    pthread_cond_destroy(&cond);
    pthread_mutex_destroy(&mutex);
#endif
}

bool Conditional::wait(timeout_t timeout)
{
    struct timespec ts;
    set(&ts, timeout);
    return wait(&ts);
}

bool Conditional::wait(struct timespec *ts)
{
    assert(ts != NULL);

    if(pthread_cond_timedwait(&cond, &mutex, ts) == ETIMEDOUT)
        return false;

    return true;
}

#endif


#if defined(_MSCONDITIONAL_)

ConditionalAccess::ConditionalAccess()
{
    waiting = pending = sharing = 0;
    InitializeConditionVariable(&bcast);
}

ConditionalAccess::~ConditionalAccess()
{
}

bool ConditionalAccess::waitBroadcast(timeout_t timeout)
{
    assert(ts != NULL);

    if(SleepConditionVariableCS(&bcast, &mutex, timeout))
        return true;

    return false;
}

bool ConditionalAccess::waitSignal(timeout_t timeout)
{
    assert(ts != NULL);

    if(SleepConditionVariableCS(&cond, &mutex, timeout))
        return true;

    return false;
}

bool ConditionalAccess::waitBroadcast(struct timespec *ts)
{
    assert(ts != NULL);

    return waitBroadcast((timeout_t)(ts->tv_sec * 1000 + (ts->tv_nsec / 1000000l)));
}

bool ConditionalAccess::waitSignal(struct timespec *ts)
{
    assert(ts != NULL);

    return waitSignal((timeout_t)(ts->tv_sec * 1000 + (ts->tv_nsec / 1000000l)));
}

#elif defined(_MSWINDOWS_)

void ConditionalAccess::waitSignal(void)
{
    LeaveCriticalSection(&mutex);
    WaitForSingleObject(&events[SIGNAL], INFINITE);
    EnterCriticalSection(&mutex);
}

void ConditionalAccess::waitBroadcast(void)
{
    int result;

    EnterCriticalSection(&mlock);
    ++waiting;
    LeaveCriticalSection(&mlock);
    LeaveCriticalSection(&mutex);
    result = WaitForSingleObject(&events[BROADCAST], INFINITE);
    EnterCriticalSection(&mlock);
    --waiting;
    result = ((result == WAIT_OBJECT_0) && (waiting == 0));
    LeaveCriticalSection(&mlock);
    if(result)
        ResetEvent(&events[BROADCAST]);
    EnterCriticalSection(&mutex);
}

ConditionalAccess::ConditionalAccess() : Conditional()
{
    pending = waiting = sharing = 0;
}

ConditionalAccess::~ConditionalAccess()
{
}

bool ConditionalAccess::waitSignal(timeout_t timeout)
{
    int result;
    bool rtn = true;

    if(!timeout)
        return false;

    LeaveCriticalSection(&mutex);
    result = WaitForSingleObject(events[SIGNAL], timeout);
    if(result == WAIT_OBJECT_0)
        rtn = true;
    EnterCriticalSection(&mutex);
    return rtn;
}

bool ConditionalAccess::waitSignal(struct timespec *ts)
{
    assert(ts != NULL);

    return waitSignal((timeout_t)(ts->tv_sec * 1000 + (ts->tv_nsec / 1000000l)));
}


bool ConditionalAccess::waitBroadcast(timeout_t timeout)
{
    int result;
    bool rtn = true;

    if(!timeout)
        return false;

    EnterCriticalSection(&mlock);
    ++waiting;
    LeaveCriticalSection(&mlock);
    LeaveCriticalSection(&mutex);
    result = WaitForSingleObject(events[BROADCAST], timeout);
    EnterCriticalSection(&mlock);
    --waiting;
    if(result == WAIT_OBJECT_0)
        rtn = true;
    result = ((result == WAIT_OBJECT_0) && (waiting == 0));
    LeaveCriticalSection(&mlock);
    if(result)
        ResetEvent(&events[BROADCAST]);
    EnterCriticalSection(&mutex);
    return rtn;
}

bool ConditionalAccess::waitBroadcast(struct timespec *ts)
{
    assert(ts != NULL);

    return waitBroadcast((timeout_t)(ts->tv_sec * 1000 + (ts->tv_nsec / 1000000l)));
}

#else

ConditionalAccess::ConditionalAccess()
{
    waiting = pending = sharing = 0;
#ifdef  __PTH__
    pth_cond_init(&bcast);
#else
    crit(pthread_cond_init(&bcast, &attr.attr) == 0, "conditional init failed");
#endif
}

ConditionalAccess::~ConditionalAccess()
{
#ifndef __PTH__
    pthread_cond_destroy(&bcast);
#endif
}

bool ConditionalAccess::waitSignal(timeout_t timeout)
{
    struct timespec ts;
    set(&ts, timeout);
    return waitSignal(&ts);
}

bool ConditionalAccess::waitBroadcast(struct timespec *ts)
{
    assert(ts != NULL);

    if(pthread_cond_timedwait(&bcast, &mutex, ts) == ETIMEDOUT)
        return false;

    return true;
}

bool ConditionalAccess::waitBroadcast(timeout_t timeout)
{
    struct timespec ts;
    set(&ts, timeout);
    return waitBroadcast(&ts);
}

bool ConditionalAccess::waitSignal(struct timespec *ts)
{
    assert(ts != NULL);

    if(pthread_cond_timedwait(&cond, &mutex, ts) == ETIMEDOUT)
        return false;

    return true;
}

#endif

// abstract class never runs...
bool Thread::is_active(void)
{
    return false;
}

bool JoinableThread::is_active(void)
{
#ifdef  _MSWINDOWS_
    return (running != INVALID_HANDLE_VALUE) && !joining;
#else
    return running && !joining;
#endif
}

bool DetachedThread::is_active(void)
{
    return active;
}

void ConditionalAccess::modify(void)
{
    lock();
    while(sharing) {
        ++pending;
        waitSignal();
        --pending;
    }
}

void ConditionalAccess::commit(void)
{
    if(pending)
        signal();
    else if(waiting)
        broadcast();
    unlock();
}

void ConditionalAccess::access(void)
{
    lock();
    assert(!max_sharing || sharing < max_sharing);
    while(pending) {
        ++waiting;
        waitBroadcast();
        --waiting;
    }
    ++sharing;
    unlock();
}

void ConditionalAccess::release(void)
{
   lock();
    assert(sharing);

    --sharing;
    if(pending && !sharing)
        signal();
    else if(waiting && !pending)
        broadcast();
    unlock();
}

RecursiveMutex::RecursiveMutex() :
Conditional()
{
    lockers = 0;
    waiting = 0;
}

void RecursiveMutex::_lock(void)
{
    lock();
}

void RecursiveMutex::_unlock(void)
{
    release();
}

bool RecursiveMutex::lock(timeout_t timeout)
{
    bool result = true;
    struct timespec ts;
    set(&ts, timeout);

    Conditional::lock();
    while(result && lockers) {
        if(Thread::equal(locker, pthread_self()))
            break;
        ++waiting;
        result = Conditional::wait(&ts);
        --waiting;
    }
    if(!lockers) {
        result = true;
        locker = pthread_self();
    }
    else
        result = false;
    ++lockers;
    Conditional::unlock();
    return result;
}

void RecursiveMutex::lock(void)
{
    Conditional::lock();
    while(lockers) {
        if(Thread::equal(locker, pthread_self()))
            break;
        ++waiting;
        Conditional::wait();
        --waiting;
    }
    if(!lockers)
        locker = pthread_self();
    ++lockers;
    Conditional::unlock();
    return;
}

void RecursiveMutex::release(void)
{
    Conditional::lock();
    --lockers;
    if(!lockers && waiting)
        Conditional::signal();
    Conditional::unlock();
}

ThreadLock::ThreadLock() :
ConditionalAccess()
{
    writers = 0;
}

void ThreadLock::_lock(void)
{
    modify();
}

void ThreadLock::_share(void)
{
    access();
}

void ThreadLock::_unlock(void)
{
    release();
}

bool ThreadLock::modify(timeout_t timeout)
{
    bool rtn = true;
    struct timespec ts;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while((writers || sharing) && rtn) {
        if(writers && Thread::equal(writeid, pthread_self()))
            break;
        ++pending;
        if(timeout == Timer::inf)
            waitSignal();
        else if(timeout)
            rtn = waitSignal(&ts);
        else
            rtn = false;
        --pending;
    }
    assert(!max_sharing || writers < max_sharing);
    if(rtn) {
        if(!writers)
            writeid = pthread_self();
        ++writers;
    }
    unlock();
    return rtn;
}

bool ThreadLock::access(timeout_t timeout)
{
    struct timespec ts;
    bool rtn = true;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while((writers || pending) && rtn) {
        ++waiting;
        if(timeout == Timer::inf)
            waitBroadcast();
        else if(timeout)
            rtn = waitBroadcast(&ts);
        else
            rtn = false;
        --waiting;
    }
    assert(!max_sharing || sharing < max_sharing);
    if(rtn)
        ++sharing;
    unlock();
    return rtn;
}

void ThreadLock::release(void)
{
    lock();
    assert(sharing || writers);

    if(writers) {
        assert(!sharing);
        --writers;
        if(pending && !writers)
            signal();
        else if(waiting && !writers)
            broadcast();
        unlock();
        return;
    }
    if(sharing) {
        assert(!writers);
        --sharing;
        if(pending && !sharing)
            signal();
        else if(waiting && !pending)
            broadcast();
    }
    unlock();
}

auto_protect::auto_protect()
{
    object = NULL;
}

auto_protect::auto_protect(const void *obj)
{
    object = obj;
    if(object)
        Mutex::protect(obj);
}

auto_protect::~auto_protect()
{
    release();
}

void auto_protect::release()
{
    if(object) {
        Mutex::release(object);
        object = NULL;
    }
}

void auto_protect::operator=(const void *obj)
{
    if(obj == object)
        return;

    release();
    object = obj;
    if(object)
        Mutex::protect(object);
}

Mutex::guard::guard()
{
    object = NULL;
}

Mutex::guard::guard(const void *obj)
{
    object = obj;
    if(obj)
        Mutex::protect(object);
}

Mutex::guard::~guard()
{
    release();
}

void Mutex::guard::set(const void *obj)
{
    release();
    object = obj;
    if(obj)
        Mutex::protect(object);
}

void Mutex::guard::release(void)
{
    if(object) {
        Mutex::release(object);
        object = NULL;
    }
}

Mutex::Mutex()
{
#ifdef  __PTH__
    pth_mutex_init(&mlock);
#else
    crit(pthread_mutex_init(&mlock, NULL) == 0, "mutex init failed");
#endif
}

Mutex::~Mutex()
{
    pthread_mutex_destroy(&mlock);
}

void Mutex::indexing(unsigned index)
{
    if(index > 1) {
        mutex_table = new mutex_index[index];
        mutex_indexing = index;
    }
}

void ThreadLock::indexing(unsigned index)
{
    if(index > 1) {
        rwlock_table = new rwlock_index[index];
        rwlock_indexing = index;
    }
}

ThreadLock::guard_reader::guard_reader()
{
    object = NULL;
}

ThreadLock::guard_reader::guard_reader(const void *obj)
{
    object = obj;
    if(obj)
        if(!ThreadLock::reader(object))
            object = NULL;
}

ThreadLock::guard_reader::~guard_reader()
{
    release();
}

void ThreadLock::guard_reader::set(const void *obj)
{
    release();
    object = obj;
    if(obj)
        if(!ThreadLock::reader(object))
            object = NULL;
}

void ThreadLock::guard_reader::release(void)
{
    if(object) {
        ThreadLock::release(object);
        object = NULL;
    }
}

ThreadLock::guard_writer::guard_writer()
{
    object = NULL;
}

ThreadLock::guard_writer::guard_writer(const void *obj)
{
    object = obj;
    if(obj)
        if(!ThreadLock::writer(object))
            object = NULL;
}

ThreadLock::guard_writer::~guard_writer()
{
    release();
}

void ThreadLock::guard_writer::set(const void *obj)
{
    release();
    object = obj;
    if(obj)
        if(!ThreadLock::writer(object))
            object = NULL;
}

void ThreadLock::guard_writer::release(void)
{
    if(object) {
        ThreadLock::release(object);
        object = NULL;
    }
}

bool ThreadLock::reader(const void *ptr, timeout_t timeout)
{
    rwlock_index *index = &rwlock_table[hash_address(ptr, rwlock_indexing)];
    rwlock_entry *entry, *empty = NULL;

    if(!ptr)
        return false;

    index->acquire();
    entry = index->list;
    while(entry) {
        if(entry->count && entry->object == ptr)
            break;
        if(!entry->count)
            empty = entry;
        entry = entry->next;
    }
    if(!entry) {
        if(empty)
            entry = empty;
        else {
            entry = new rwlock_entry;
            entry->next = index->list;
            index->list = entry;
        }
    }
    entry->object = ptr;
    ++entry->count;
    index->release();
    if(entry->access(timeout))
        return true;
    index->acquire();
    --entry->count;
    index->release();
    return false;
}

bool ThreadLock::writer(const void *ptr, timeout_t timeout)
{
    rwlock_index *index = &rwlock_table[hash_address(ptr, rwlock_indexing)];
    rwlock_entry *entry, *empty = NULL;

    if(!ptr)
        return false;

    index->acquire();
    entry = index->list;
    while(entry) {
        if(entry->count && entry->object == ptr)
            break;
        if(!entry->count)
            empty = entry;
        entry = entry->next;
    }
    if(!entry) {
        if(empty)
            entry = empty;
        else {
            entry = new rwlock_entry;
            entry->next = index->list;
            index->list = entry;
        }
    }
    entry->object = ptr;
    ++entry->count;
    index->release();
    if(entry->modify(timeout))
        return true;
    index->acquire();
    --entry->count;
    index->release();
    return false;
}

void Mutex::protect(const void *ptr)
{
    mutex_index *index = &mutex_table[hash_address(ptr, mutex_indexing)];
    mutex_entry *entry, *empty = NULL;

    if(!ptr)
        return;

    index->acquire();
    entry = index->list;
    while(entry) {
        if(entry->count && entry->pointer == ptr)
            break;
        if(!entry->count)
            empty = entry;
        entry = entry->next;
    }
    if(!entry) {
        if(empty)
            entry = empty;
        else {
            entry = new struct mutex_entry;
            entry->count = 0;
            pthread_mutex_init(&entry->mutex, NULL);
            entry->next = index->list;
            index->list = entry;
        }
    }
    entry->pointer = ptr;
    ++entry->count;
//  printf("ACQUIRE %p, THREAD %d, POINTER %p, COUNT %d\n", entry, Thread::self(), entry->pointer, entry->count);
    index->release();
    pthread_mutex_lock(&entry->mutex);
}

void ThreadLock::release(const void *ptr)
{
    rwlock_index *index = &rwlock_table[hash_address(ptr, rwlock_indexing)];
    rwlock_entry *entry;

    if(!ptr)
        return;

    index->acquire();
    entry = index->list;
    while(entry) {
        if(entry->count && entry->object == ptr)
            break;
        entry = entry->next;
    }

    assert(entry);
    if(entry) {
        entry->release();
        --entry->count;
    }
    index->release();
}

void Mutex::release(const void *ptr)
{
    mutex_index *index = &mutex_table[hash_address(ptr, mutex_indexing)];
    mutex_entry *entry;

    if(!ptr)
        return;

    index->acquire();
    entry = index->list;
    while(entry) {
        if(entry->count && entry->pointer == ptr)
            break;
        entry = entry->next;
    }

    assert(entry);
    if(entry) {
//      printf("RELEASE %p, THREAD %d, POINTER %p COUNT %d\n", entry, Thread::self(), entry->pointer, entry->count);
        pthread_mutex_unlock(&entry->mutex);
        --entry->count;
    }
    index->release();
}

void Mutex::_lock(void)
{
    pthread_mutex_lock(&mlock);
}

void Mutex::_unlock(void)
{
    pthread_mutex_unlock(&mlock);
}

#ifdef  _MSWINDOWS_

TimedEvent::TimedEvent() :
Timer()
{
    event = CreateEvent(NULL, FALSE, FALSE, NULL);
    InitializeCriticalSection(&mutex);
    set();
}

TimedEvent::~TimedEvent()
{
    if(event != INVALID_HANDLE_VALUE) {
        CloseHandle(event);
        event = INVALID_HANDLE_VALUE;
    }
    DeleteCriticalSection(&mutex);
}

TimedEvent::TimedEvent(timeout_t timeout) :
Timer(timeout)
{
    event = CreateEvent(NULL, FALSE, FALSE, NULL);
    InitializeCriticalSection(&mutex);
}

TimedEvent::TimedEvent(time_t timer) :
Timer(timer)
{
    event = CreateEvent(NULL, FALSE, FALSE, NULL);
    InitializeCriticalSection(&mutex);
}

void TimedEvent::signal(void)
{
    SetEvent(event);
}

void TimedEvent::reset(void)
{
    set();
    ResetEvent(event);
}

bool TimedEvent::sync(void)
{
    int result;
    timeout_t timeout;

    timeout = get();
    if(!timeout)
        return false;

    LeaveCriticalSection(&mutex);
    result = WaitForSingleObject(event, timeout);
    EnterCriticalSection(&mutex);
    if(result != WAIT_OBJECT_0)
        return true;
    return false;
}

bool TimedEvent::wait(timeout_t timer)
{
    int result;
    timeout_t timeout;

    if(timer)
        operator+=(timer);

    timeout = get();
    if(!timeout)
        return false;

    result = WaitForSingleObject(event, timeout);
    if(result == WAIT_OBJECT_0)
        return true;
    return false;
}

void TimedEvent::wait(void)
{
    WaitForSingleObject(event, INFINITE);
}

#else

TimedEvent::TimedEvent() :
Timer()
{
    signalled = false;
#ifdef  __PTH__
    Thread::init();
    pth_cond_init(&cond);
    pth_mutex_init(&mutex);
#else
    crit(pthread_cond_init(&cond, Conditional::initializer()) == 0, "conditional init failed");
    crit(pthread_mutex_init(&mutex, NULL) == 0, "mutex init failed");
#endif
    set();
}

TimedEvent::TimedEvent(timeout_t timeout) :
Timer(timeout)
{
    signalled = false;
#ifdef  __PTH__
    Thread::init();
    pth_cond_init(&cond);
    pth_mutex_init(&mutex);
#else
    crit(pthread_cond_init(&cond, Conditional::initializer()) == 0, "conditional init failed");
    crit(pthread_mutex_init(&mutex, NULL) == 0, "mutex init failed");
#endif
}

TimedEvent::TimedEvent(time_t timer) :
Timer(timer)
{
    signalled = false;
#ifdef  __PTH__
    Thread::init();
    pth_cond_init(&cond);
    pth_mutex_init(&mutex);
#else
    crit(pthread_cond_init(&cond, Conditional::initializer()) == 0, "conditional init failed");
    crit(pthread_mutex_init(&mutex, NULL) == 0, "mutex init failed");
#endif
}

TimedEvent::~TimedEvent()
{
#ifndef __PTH__
    pthread_cond_destroy(&cond);
    pthread_mutex_destroy(&mutex);
#endif
}

void TimedEvent::reset(void)
{
    pthread_mutex_lock(&mutex);
    signalled = false;
    set();
    pthread_mutex_unlock(&mutex);
}

void TimedEvent::signal(void)
{
    pthread_mutex_lock(&mutex);
    signalled = true;
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mutex);
}

bool TimedEvent::sync(void)
{
    timeout_t timeout = get();
    struct timespec ts;

    if(signalled) {
        signalled = false;
        return true;
    }

    if(!timeout)
        return false;

    Conditional::set(&ts, timeout);

    if(pthread_cond_timedwait(&cond, &mutex, &ts) == ETIMEDOUT)
        return false;

    signalled = false;
    return true;
}

void TimedEvent::wait(void)
{
    pthread_mutex_lock(&mutex);
    if(signalled)
        signalled = false;
    else
        pthread_cond_wait(&cond, &mutex);
    pthread_mutex_unlock(&mutex);
}

bool TimedEvent::wait(timeout_t timeout)
{
    bool result = true;

    pthread_mutex_lock(&mutex);
    operator+=(timeout);
    result = sync();
    pthread_mutex_unlock(&mutex);
    return result;
}
#endif

void TimedEvent::lock(void)
{
    pthread_mutex_lock(&mutex);
}

void TimedEvent::release(void)
{
    pthread_mutex_unlock(&mutex);
}

ConditionalLock::ConditionalLock() :
ConditionalAccess()
{
    contexts = NULL;
}

ConditionalLock::~ConditionalLock()
{
    linked_pointer<Context> cp = contexts, next;
    while(cp) {
        next = cp->getNext();
        delete *cp;
        cp = next;
    }
}

ConditionalLock::Context *ConditionalLock::getContext(void)
{
    Context *slot = NULL;
    pthread_t tid = Thread::self();
    linked_pointer<Context> cp = contexts;

    while(cp) {
        if(cp->count && Thread::equal(cp->thread, tid))
            return *cp;
        if(!cp->count)
            slot = *cp;
        cp.next();
    }
    if(!slot) {
        slot = new Context(&this->contexts);
        slot->count = 0;
    }
    slot->thread = tid;
    return slot;
}

void ConditionalLock::_share(void)
{
    access();
}

void ConditionalLock::_unlock(void)
{
    release();
}

void ConditionalLock::modify(void)
{
    Context *context;

    lock();
    context = getContext();

    assert(context && sharing >= context->count);

    sharing -= context->count;
    while(sharing) {
        ++pending;
        waitSignal();
        --pending;
    }
    ++context->count;
}

void ConditionalLock::commit(void)
{
    Context *context = getContext();
    --context->count;

    if(context->count) {
        sharing += context->count;
        unlock();
    }
    else
        ConditionalAccess::commit();
}

void ConditionalLock::release(void)
{
    Context *context;

    lock();
    context = getContext();
    assert(sharing && context && context->count > 0);
    --sharing;
    --context->count;
    if(pending && !sharing)
        signal();
    else if(waiting && !pending)
        broadcast();
    unlock();
}

void ConditionalLock::access(void)
{
    Context *context;
    lock();
    context = getContext();
    assert(context && (!max_sharing || sharing < max_sharing));

    // reschedule if pending exclusives to make sure modify threads are not
    // starved.

    ++context->count;

    while(context->count < 2 && pending) {
        ++waiting;
        waitBroadcast();
        --waiting;
    }
    ++sharing;
    unlock();
}

void ConditionalLock::exclusive(void)
{
    Context *context;

    lock();
    context = getContext();
    assert(sharing && context && context->count > 0);
    sharing -= context->count;
    while(sharing) {
        ++pending;
        waitSignal();
        --pending;
    }
}

void ConditionalLock::share(void)
{
    Context *context = getContext();
    assert(!sharing && context && context->count);
    sharing += context->count;
    unlock();
}

barrier::barrier(unsigned limit) :
Conditional()
{
    count = limit;
    waits = 0;
}

barrier::~barrier()
{
    lock();
    if(waits)
        broadcast();
    unlock();
}

void barrier::set(unsigned limit)
{
    assert(limit > 0);

    lock();
    count = limit;
    if(count <= waits) {
        waits = 0;
        broadcast();
    }
    unlock();
}

void barrier::dec(void)
{
    lock();
    if(count)
        --count;
    unlock();
}

unsigned barrier::operator--(void)
{
    unsigned result;
    lock();
    if(count)
        --count;
    result = count;
    unlock();
    return result;
}

void barrier::inc(void)
{
    lock();
    count++;
    if(count <= waits) {
        waits = 0;
        broadcast();
    }
    unlock();
}

unsigned barrier::operator++(void)
{
    unsigned result;
    lock();
    count++;
    if(count <= waits) {
        waits = 0;
        broadcast();
    }
    result = count;
    unlock();
    return result;
}

bool barrier::wait(timeout_t timeout)
{
    bool result;

    Conditional::lock();
    if(!count) {
        Conditional::unlock();
        return true;
    }
    if(++waits >= count) {
        waits = 0;
        Conditional::broadcast();
        Conditional::unlock();
        return true;
    }
    result = Conditional::wait(timeout);
    Conditional::unlock();
    return result;
}

void barrier::wait(void)
{
    Conditional::lock();
    if(!count) {
        Conditional::unlock();
        return;
    }
    if(++waits >= count) {
        waits = 0;
        Conditional::broadcast();
        Conditional::unlock();
        return;
    }
    Conditional::wait();
    Conditional::unlock();
}

LockedPointer::LockedPointer()
{
#ifdef  _MSWINDOWS_
    InitializeCriticalSection(&mutex);
#else
#ifdef  __PTH__
    static pthread_mutex_t lock = PTH_MUTEX_INIT;
#else
    static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
#endif
    memcpy(&mutex, &lock, sizeof(mutex));
#endif
    pointer = NULL;
}

void LockedPointer::replace(ObjectProtocol *obj)
{
    assert(obj != NULL);

    pthread_mutex_lock(&mutex);
    obj->retain();
    if(pointer)
        pointer->release();
    pointer = obj;
    pthread_mutex_unlock(&mutex);
}

ObjectProtocol *LockedPointer::dup(void)
{
    ObjectProtocol *temp;
    pthread_mutex_lock(&mutex);
    temp = pointer;
    if(temp)
        temp->retain();
    pthread_mutex_unlock(&mutex);
    return temp;
}

SharedObject::~SharedObject()
{
}

SharedPointer::SharedPointer() :
ConditionalAccess()
{
    pointer = NULL;
}

SharedPointer::~SharedPointer()
{
}

void SharedPointer::replace(SharedObject *ptr)
{
    modify();

    if(pointer)
        delete pointer;
    pointer = ptr;
    if(ptr)
        ptr->commit(this);

    commit();
}

SharedObject *SharedPointer::share(void)
{
    access();
    return pointer;
}

Thread::Thread(size_t size)
{
    stack = size;
    priority = 0;
#ifdef  _MSWINDOWS_
    cancellor = INVALID_HANDLE_VALUE;
#else
    cancellor = NULL;
#endif
    init();
}

#if defined(_MSWINDOWS_)

void Thread::setPriority(void)
{
    HANDLE hThread = GetCurrentThread();
    priority += THREAD_PRIORITY_NORMAL;
    if(priority < THREAD_PRIORITY_LOWEST)
        priority = THREAD_PRIORITY_LOWEST;
    else if(priority > THREAD_PRIORITY_HIGHEST)
        priority = THREAD_PRIORITY_HIGHEST;

    SetThreadPriority(hThread, priority);
}
#elif _POSIX_PRIORITY_SCHEDULING > 0

void Thread::setPriority(void)
{
#ifndef __PTH__
    int policy;
    struct sched_param sp;
    pthread_t ptid = pthread_self();
    int pri = 0;

    if(!priority)
        return;

    if(pthread_getschedparam(ptid, &policy, &sp))
        return;

    if(priority > 0) {
        policy = realtime_policy;
        if(realtime_policy == SCHED_OTHER)
            pri = sp.sched_priority + priority;
        else
            pri = sched_get_priority_min(policy) + priority;
        policy = realtime_policy;
        if(pri > sched_get_priority_max(policy))
            pri = sched_get_priority_max(policy);
    } else if(priority < 0) {
        pri = sp.sched_priority - priority;
        if(pri < sched_get_priority_min(policy))
            pri = sched_get_priority_min(policy);
    }

    sp.sched_priority = pri;
    pthread_setschedparam(ptid, policy, &sp);
#endif
}

#else
void Thread::setPriority(void) {}
#endif

void Thread::concurrency(int level)
{
#if defined(HAVE_PTHREAD_SETCONCURRENCY) && !defined(_MSWINDOWS_) && !defined(ANDROID)
    pthread_setconcurrency(level);
#endif
}

void Thread::policy(int polid)
{
#if _POSIX_PRIORITY_SCHEDULING > 0
    realtime_policy = polid;
#endif
}

JoinableThread::JoinableThread(size_t size)
{
#ifdef  _MSWINDOWS_
    cancellor = INVALID_HANDLE_VALUE;
    running = INVALID_HANDLE_VALUE;
    joining = false;
#else
    joining = false;
    running = false;
    cancellor = NULL;
#endif
    stack = size;
}

DetachedThread::DetachedThread(size_t size)
{
#ifdef  _MSWINDOWS_
    cancellor = INVALID_HANDLE_VALUE;
#else
    cancellor = NULL;
#endif
    active = false;
    stack = size;
}

void Thread::sleep(timeout_t timeout)
{
#if defined(__PTH__)
    pth_usleep(timeout * 1000);
#elif defined(HAVE_PTHREAD_DELAY)
    timespec ts;
    ts.tv_sec = timeout / 1000l;
    ts.tv_nsec = (timeout % 1000l) * 1000000l;
    pthread_delay(&ts);
#elif defined(HAVE_PTHREAD_DELAY_NP)
    timespec ts;
    ts.tv_sec = timeout / 1000l;
    ts.tv_nsec = (timeout % 1000l) * 1000000l;
    pthread_delay_np(&ts);
#elif defined(_MSWINDOWS_)
    ::Sleep(timeout);
#else
    usleep(timeout * 1000);
#endif
}

void Thread::yield(void)
{
#if defined(_MSWINDOWS_)
    SwitchToThread();
#elif defined(__PTH__)
    pth_yield(NULL);
#elif defined(HAVE_PTHREAD_YIELD_NP) && !defined(ANDROID)
    pthread_yield_np();
#elif defined(HAVE_PTHREAD_YIELD) && !defined(ANDROID)
    pthread_yield();
#else
    sched_yield();
#endif
}

void DetachedThread::exit(void)
{
    delete this;
    pthread_exit(NULL);
}

Thread::~Thread()
{
}

JoinableThread::~JoinableThread()
{
    join();
}

DetachedThread::~DetachedThread()
{
}

extern "C" {
#ifdef  _MSWINDOWS_
    static unsigned __stdcall exec_thread(void *obj) {
        assert(obj != NULL);

        Thread *th = static_cast<Thread *>(obj);
        th->setPriority();
        th->run();
        th->exit();
        return 0;
    }
#else
    static void *exec_thread(void *obj)
    {
        assert(obj != NULL);

        Thread *th = static_cast<Thread *>(obj);
        th->setPriority();
        th->run();
        th->exit();
        return NULL;
    }
#endif
}

#ifdef  _MSWINDOWS_
void JoinableThread::start(int adj)
{
    if(running != INVALID_HANDLE_VALUE)
        return;

    priority = adj;

    if(stack == 1)
        stack = 1024;

    joining = false;
    running = (HANDLE)_beginthreadex(NULL, stack, &exec_thread, this, 0, (unsigned int *)&tid);
    if(!running)
        running = INVALID_HANDLE_VALUE;
}

void DetachedThread::start(int adj)
{
    HANDLE hThread;;

    priority = adj;

    if(stack == 1)
        stack = 1024;

    hThread = (HANDLE)_beginthreadex(NULL, stack, &exec_thread, this, 0, (unsigned int *)&tid);
    if(hThread != INVALID_HANDLE_VALUE)
        active = true;
    CloseHandle(hThread);
}

void JoinableThread::join(void)
{
    pthread_t self = pthread_self();
    int rc;

    // already joined, so we ignore...
    if(running == INVALID_HANDLE_VALUE)
        return;

    // self join does cleanup...
    if(equal(tid, self)) {
        CloseHandle(running);
        running = INVALID_HANDLE_VALUE;
        Thread::exit();
    }

    joining = true;
    rc = WaitForSingleObject(running, INFINITE);
    if(rc == WAIT_OBJECT_0 || rc == WAIT_ABANDONED) {
        CloseHandle(running);
        running = INVALID_HANDLE_VALUE;
    }
}

#else

void JoinableThread::start(int adj)
{
    int result;

    if(running)
        return;

    joining = false;
    priority = adj;

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
    if(!result)
        running = true;
#endif
}

void DetachedThread::start(int adj)
{
    priority = adj;
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
    active = true;
}

void JoinableThread::join(void)
{
    pthread_t self = pthread_self();

    // already joined, so we ignore...
    if(!running)
        return;

    if(equal(tid, self)) {
        running = false;
        Thread::exit();
    }

    joining = true;

#ifdef  __PTH__
    if(pth_join(tid, NULL))
        running = false;
#else
    if(!pthread_join(tid, NULL))
        running = false;
#endif
}

#endif

void Thread::exit(void)
{
    pthread_exit(NULL);
}

locked_release::locked_release(const locked_release &copy)
{
    object = copy.object;
    object->retain();
}

locked_release::locked_release()
{
    object = NULL;
}

locked_release::locked_release(LockedPointer &p)
{
    object = p.dup();
}

locked_release::~locked_release()
{
    release();
}

void locked_release::release(void)
{
    if(object)
        object->release();
    object = NULL;
}

locked_release &locked_release::operator=(LockedPointer &p)
{
    release();
    object = p.dup();
    return *this;
}

shared_release::shared_release(const shared_release &copy)
{
    ptr = copy.ptr;
}

shared_release::shared_release()
{
    ptr = NULL;
}

SharedObject *shared_release::get(void)
{
    if(ptr)
        return ptr->pointer;
    return NULL;
}

void SharedObject::commit(SharedPointer *spointer)
{
}

shared_release::shared_release(SharedPointer &p)
{
    ptr = &p;
    p.share(); // create rdlock
}

shared_release::~shared_release()
{
    release();
}

void shared_release::release(void)
{
    if(ptr)
        ptr->release();
    ptr = NULL;
}

shared_release &shared_release::operator=(SharedPointer &p)
{
    release();
    ptr = &p;
    p.share();
    return *this;
}

void Thread::map(void)
{
    Thread::init();
#ifdef  __PTH__
    pth_key_setdata(threadmap, this);
#else
#ifdef  _MSWINDOWS_
    TlsSetValue(threadmap, this);
#else
    pthread_setspecific(threadmap, this);
#endif
#endif
}

Thread *Thread::get(void)
{
#ifdef  __PTH__
    return (Thread *)pth_key_setdata(threadmap);
#else
#ifdef  _MSWINDOWS_
    return (Thread *)TlsGetValue(threadmap);
#else
    return (Thread *)pthread_getspecific(threadmap);
#endif
#endif
}

void Thread::init(void)
{
    static volatile bool initialized = false;

    if(!initialized) {
#ifdef  __PTH__
        pth_init();
        pth_key_create(&threadmap, NULL);
        atexit(pthread_shutdown);
#else
#ifdef  _MSWINDOWS_
        threadmap = TlsAlloc();
#else
        pthread_key_create(&threadmap, NULL);
#endif
#endif
        initialized = true;
    }
}

#ifdef  __PTH__
pthread_t Thread::self(void)
{
    return pth_self();
}
#else
pthread_t Thread::self(void)
{
    return pthread_self();
}
#endif


