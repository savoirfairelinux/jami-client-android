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

/**
 * Thread classes and sychronization objects.
 * The theory behind ucommon thread classes is that they would be used
 * to create derived classes where thread-specific data can be stored as
 * member data of the derived class.  The run method is called when the
 * context is executed.  Since we use a pthread foundation, we support
 * both detached threads and joinable threads.  Objects based on detached
 * threads should be created with new, and will automatically delete when
 * the thread context exits.  Joinable threads will be joined with deleted.
 *
 * The theory behind ucommon sychronization objects is that all upper level
 * sychronization objects can be formed directly from a mutex and conditional.
 * This includes semaphores, barriers, rwlock, our own specialized conditional
 * lock, resource-bound locking, and recurive exclusive locks.  Using only
 * conditionals means we are not dependent on platform specific pthread
 * implimentations that may not impliment some of these, and hence improves
 * portability and consistency.  Given that our rwlocks are recursive access
 * locks, one can safely create read/write threading pairs where the read
 * threads need not worry about deadlocks and the writers need not either if
 * they only write-lock one instance at a time to change state.
 * @file ucommon/thread.h
 */

/**
 * An example of the thread queue class.  This may be relevant to producer-
 * consumer scenarios and realtime applications where queued messages are
 * stored on a re-usable object pool.
 * @example queue.cpp
 */

/**
 * A simple example of threading and join operation.
 * @example thread.cpp
 */

#ifndef _UCOMMON_THREAD_H_
#define _UCOMMON_THREAD_H_

#ifndef _UCOMMON_CPR_H_
#include <ucommon/cpr.h>
#endif

#ifndef _UCOMMON_ACCESS_H_
#include <ucommon/access.h>
#endif

#ifndef _UCOMMON_TIMERS_H_
#include <ucommon/timers.h>
#endif

#ifndef _UCOMMON_MEMORY_H_
#include <ucommon/memory.h>
#endif

NAMESPACE_UCOMMON

class SharedPointer;

/**
 * The conditional is a common base for other thread synchronizing classes.
 * Many of the complex sychronization objects, including barriers, semaphores,
 * and various forms of read/write locks are all built from the conditional.
 * This assures that the minimum functionality to build higher order thread
 * synchronizing objects is a pure conditional, and removes dependencies on
 * what may be optional features or functions that may have different
 * behaviors on different pthread implimentations and platforms.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT Conditional
{
private:
    friend class ConditionalAccess;

#if defined(_MSCONDITIONAL_)
    CRITICAL_SECTION mutex;
    CONDITION_VARIABLE cond;
#elif defined(_MSWINDOWS_)
    enum {SIGNAL = 0, BROADCAST = 1};
    HANDLE events[2];
    unsigned waiting;
    CRITICAL_SECTION mlock;
    CRITICAL_SECTION mutex;
#else
#ifndef __PTH__
    class __LOCAL attribute
    {
    public:
        pthread_condattr_t attr;
        attribute();
    };

    __LOCAL static attribute attr;
#endif

    pthread_cond_t cond;
    pthread_mutex_t mutex;
#endif

protected:
    friend class TimedEvent;

    /**
     * Conditional wait for signal on millisecond timeout.
     * @param timeout in milliseconds.
     * @return true if signalled, false if timer expired.
     */
    bool wait(timeout_t timeout);

    /**
     * Conditional wait for signal on timespec timeout.
     * @param timeout as a high resolution timespec.
     * @return true if signalled, false if timer expired.
     */
    bool wait(struct timespec *timeout);

#ifdef  _MSWINDOWS_
    inline void lock(void)
        {EnterCriticalSection(&mutex);};

    inline void unlock(void)
        {LeaveCriticalSection(&mutex);};

    void wait(void);
    void signal(void);
    void broadcast(void);

#else
    /**
     * Lock the conditional's supporting mutex.
     */
    inline void lock(void)
        {pthread_mutex_lock(&mutex);};

    /**
     * Unlock the conditional's supporting mutex.
     */
    inline void unlock(void)
        {pthread_mutex_unlock(&mutex);};

    /**
     * Wait (block) until signalled.
     */
    inline void wait(void)
        {pthread_cond_wait(&cond, &mutex);};

    /**
     * Signal the conditional to release one waiting thread.
     */
    inline void signal(void)
        {pthread_cond_signal(&cond);};

    /**
     * Signal the conditional to release all waiting threads.
     */
    inline void broadcast(void)
        {pthread_cond_broadcast(&cond);};
#endif

    /**
     * Initialize and construct conditional.
     */
    Conditional();

    /**
     * Destroy conditional, release any blocked threads.
     */
    ~Conditional();

public:
#if !defined(_MSWINDOWS_) && !defined(__PTH__)
    /**
     * Support function for getting conditional attributes for realtime
     * scheduling.
     * @return attributes to use for creating realtime conditionals.
     */
    static inline pthread_condattr_t *initializer(void)
        {return &attr.attr;};
#endif

    /**
     * Convert a millisecond timeout into use for high resolution
     * conditional timers.
     * @param hires timespec representation to set.
     * @param timeout to convert.
     */
    static void set(struct timespec *hires, timeout_t timeout);
};

/**
 * The conditional rw seperates scheduling for optizming behavior or rw locks.
 * This varient of conditonal seperates scheduling read (broadcast wakeup) and
 * write (signal wakeup) based threads.  This is used to form generic rwlock's
 * as well as the specialized condlock.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT ConditionalAccess : private Conditional
{
protected:
#if defined _MSCONDITIONAL_
    CONDITION_VARIABLE bcast;
#elif !defined(_MSWINDOWS_)
    pthread_cond_t bcast;
#endif

    unsigned pending, waiting, sharing;

    /**
     * Conditional wait for signal on millisecond timeout.
     * @param timeout in milliseconds.
     * @return true if signalled, false if timer expired.
     */
    bool waitSignal(timeout_t timeout);

    /**
     * Conditional wait for broadcast on millisecond timeout.
     * @param timeout in milliseconds.
     * @return true if signalled, false if timer expired.
     */
    bool waitBroadcast(timeout_t timeout);


    /**
     * Conditional wait for signal on timespec timeout.
     * @param timeout as a high resolution timespec.
     * @return true if signalled, false if timer expired.
     */
    bool waitSignal(struct timespec *timeout);

    /**
     * Conditional wait for broadcast on timespec timeout.
     * @param timeout as a high resolution timespec.
     * @return true if signalled, false if timer expired.
     */
    bool waitBroadcast(struct timespec *timeout);

    /**
     * Convert a millisecond timeout into use for high resolution
     * conditional timers.
     * @param hires timespec representation to set.
     * @param timeout to convert.
     */
    inline static void set(struct timespec *hires, timeout_t timeout)
        {Conditional::set(hires, timeout);};


#ifdef  _MSWINDOWS_
    inline void lock(void)
        {EnterCriticalSection(&mutex);};

    inline void unlock(void)
        {LeaveCriticalSection(&mutex);};

    void waitSignal(void);
    void waitBroadcast(void);

    inline void signal(void)
        {Conditional::signal();};

    inline void broadcast(void)
        {Conditional::broadcast();};

#else
    /**
     * Lock the conditional's supporting mutex.
     */
    inline void lock(void)
        {pthread_mutex_lock(&mutex);};

    /**
     * Unlock the conditional's supporting mutex.
     */
    inline void unlock(void)
        {pthread_mutex_unlock(&mutex);};

    /**
     * Wait (block) until signalled.
     */
    inline void waitSignal(void)
        {pthread_cond_wait(&cond, &mutex);};

    /**
     * Wait (block) until broadcast.
     */
    inline void waitBroadcast(void)
        {pthread_cond_wait(&bcast, &mutex);};


    /**
     * Signal the conditional to release one signalled thread.
     */
    inline void signal(void)
        {pthread_cond_signal(&cond);};

    /**
     * Signal the conditional to release all broadcast threads.
     */
    inline void broadcast(void)
        {pthread_cond_broadcast(&bcast);};
#endif
public:
    /**
     * Initialize and construct conditional.
     */
    ConditionalAccess();

    /**
     * Destroy conditional, release any blocked threads.
     */
    ~ConditionalAccess();

    /**
     * Access mode shared thread scheduling.
     */
    void access(void);

    /**
     * Exclusive mode write thread scheduling.
     */
    void modify(void);

    /**
     * Release access mode read scheduling.
     */
    void release(void);

    /**
     * Complete exclusive mode write scheduling.
     */
    void commit(void);

    /**
     * Specify a maximum sharing (access) limit.  This can be used
     * to detect locking errors, such as when aquiring locks that are
     * not released.
     * @param max sharing level.
     */
    void limit_sharing(unsigned max);
};

/**
 * Event notification to manage scheduled realtime threads.  The timer
 * is advanced to sleep threads which then wakeup either when the timer
 * has expired or they are notified through the signal handler.  This can
 * be used to schedule and signal one-time completion handlers or for time
 * synchronized events signaled by an asychrononous I/O or event source.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT TimedEvent : public Timer
{
private:
#ifdef _MSWINDOWS_
    HANDLE event;
#else
    pthread_cond_t cond;
    bool signalled;
#endif
    pthread_mutex_t mutex;

protected:
    /**
     * Lock the object for wait or to manipulate derived data.  This is
     * relevant to manipulations in a derived class.
     */
    void lock(void);

    /**
     * Release the object lock after waiting.  This is relevent to
     * manipulations in a derived class.
     */
    void release(void);

    /**
     * Wait while locked.  This can be used in more complex derived
     * objects where we are concerned with synchronized access between
     * the signaling and event thread.  This can be used in place of
     * wait, but lock and release methods must be used around it.
     * @return true if time expired.
     */
    bool sync(void);

public:
    /**
     * Create event handler and timer for timing of events.
     */
    TimedEvent(void);

    /**
     * Create event handler and timer set to trigger a timeout.
     * @param timeout in milliseconds.
     */
    TimedEvent(timeout_t timeout);

    /**
     * Create event handler and timer set to trigger a timeout.
     * @param timeout in seconds.
     */
    TimedEvent(time_t timeout);

    /**
     * Destroy timer and release pending events.
     */
    ~TimedEvent();

    /**
     * Signal pending event.  Object may be locked or unlocked.  The
     * signalling thread may choose to lock and check a condition in
     * a derived class before signalling.
     */
    void signal(void);

    /**
     * Wait to be signalled or until timer expires.  This is a wrapper for
     * expire for simple completion events.
     * @param timeout to wait from last reset.
     * @return true if signaled, false if timeout.
     */
    bool wait(timeout_t timeout);

    /**
     * A simple wait until triggered.
     */
    void wait(void);

    /**
     * Reset triggered conditional.
     */
    void reset(void);
};

/**
 * Portable recursive exclusive lock.  This class is built from the
 * conditional and hence does not require support for non-standard and
 * platform specific extensions to pthread mutex to support recrusive
 * style mutex locking.  The exclusive protocol is implimented to support
 * exclusive_lock referencing.
 */
class __EXPORT RecursiveMutex : private Conditional, public ExclusiveAccess
{
protected:
    unsigned waiting;
    unsigned lockers;
    pthread_t locker;

    virtual void _lock(void);
    virtual void _unlock(void);

public:
    /**
     * Create rexlock.
     */
    RecursiveMutex();

    /**
     * Acquire or increase locking.
     */
    void lock(void);

    /**
     * Timed lock request.
     */
    bool lock(timeout_t timeout);

    /**
     * Release or decrease locking.
     */
    void release(void);
};

/**
 * A generic and portable implimentation of Read/Write locking.  This
 * class impliments classical read/write locking, including "timed" locks.
 * Support for scheduling threads to avoid writer starvation is also provided
 * for.  By building read/write locks from a conditional, we make them
 * available on pthread implimetations and other platforms which do not
 * normally include optional pthread rwlock's.  We also do not restrict
 * the number of threads that may use the lock.  Finally, both the exclusive
 * and shared protocols are implimented to support exclusive_lock and
 * shared_lock referencing.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT ThreadLock : private ConditionalAccess, public ExclusiveAccess, public SharedAccess
{
protected:
    unsigned writers;
    pthread_t writeid;

    virtual void _lock(void);
    virtual void _share(void);
    virtual void _unlock(void);

public:
    /**
     * Guard class to apply scope based access locking to objects.  The rwlock
     * is located from the rwlock pool rather than contained in the target
     * object, and the read lock is released when the guard object falls out of
     * scope.  This is essentially an automation mechanism for mutex::reader.
     * @author David Sugar <dyfet@gnutelephony.org>
     */
    class __EXPORT guard_reader
    {
    private:
        const void *object;

    public:
        /**
          * Create an unitialized instance of guard.  Usually used with a
          * guard = operator.
          */
        guard_reader();

        /**
         * Construct a guard for a specific object.
         * @param object to guard.
         */
        guard_reader(const void *object);

        /**
         * Release mutex when guard falls out of scope.
         */
        ~guard_reader();

        /**
         * Set guard to mutex lock a new object.  If a lock is currently
         * held, it is released.
         * @param object to guard.
         */
        void set(const void *object);

        /**
         * Prematurely release a guard.
         */
        void release(void);

        /**
         * Set guard to read lock a new object.  If a lock is currently
         * held, it is released.
         * @param pointer to object to guard.
         */
        inline void operator=(const void *pointer)
            {set(pointer);};
    };

    /**
     * Guard class to apply scope based exclusive locking to objects.  The rwlock
     * is located from the rwlock pool rather than contained in the target
     * object, and the write lock is released when the guard object falls out of
     * scope.  This is essentially an automation mechanism for mutex::writer.
     * @author David Sugar <dyfet@gnutelephony.org>
     */
    class __EXPORT guard_writer
    {
    private:
        const void *object;

    public:
        /**
          * Create an unitialized instance of guard.  Usually used with a
          * guard = operator.
          */
        guard_writer();

        /**
         * Construct a guard for a specific object.
         * @param object to guard.
         */
        guard_writer(const void *object);

        /**
         * Release mutex when guard falls out of scope.
         */
        ~guard_writer();

        /**
         * Set guard to mutex lock a new object.  If a lock is currently
         * held, it is released.
         * @param object to guard.
         */
        void set(const void *object);

        /**
         * Prematurely release a guard.
         */
        void release(void);

        /**
         * Set guard to read lock a new object.  If a lock is currently
         * held, it is released.
         * @param pointer to object to guard.
         */
        inline void operator=(const void *pointer)
            {set(pointer);};
    };

    /**
     * Create an instance of a rwlock.
     */
    ThreadLock();

    /**
     * Request modify (write) access through the lock.
     * @param timeout in milliseconds to wait for lock.
     * @return true if locked, false if timeout.
     */
    bool modify(timeout_t timeout = Timer::inf);

    /**
     * Request shared (read) access through the lock.
     * @param timeout in milliseconds to wait for lock.
     * @return true if locked, false if timeout.
     */
    bool access(timeout_t timeout = Timer::inf);

    /**
     * Specify hash table size for guard protection.  The default is 1.
     * This should be called at initialization time from the main thread
     * of the application before any other threads are created.
     * @param size of hash table used for guarding.
     */
    static void indexing(unsigned size);

    /**
      * Write protect access to an arbitrary object.  This is like the
      * protect function of mutex.
      * @param object to protect.
      * @param timeout in milliseconds to wait for lock.
      * @return true if locked, false if timeout.
      */
    static bool writer(const void *object, timeout_t timeout = Timer::inf);

    /**
     * Shared access to an arbitrary object.  This is based on the protect
     * function of mutex.
     * @param object to share.
     * @param timeout in milliseconds to wait for lock.
     * @return true if shared, false if timeout.
     */
    static bool reader(const void *object, timeout_t timeout = Timer::inf);

    /**
     * Release an arbitrary object that has been protected by a rwlock.
     * @param object to release.
     */
    static void release(const void *object);

    /**
     * Release the lock.
     */
    void release(void);
};

/**
 * Class for resource bound memory pools between threads.  This is used to
 * support a memory pool allocation scheme where a pool of reusable objects
 * may be allocated, and the pool renewed by releasing objects or back.
 * When the pool is used up, a pool consuming thread then must wait for
 * a resource to be freed by another consumer (or timeout).  This class is
 * not meant to be used directly, but rather to build the synchronizing
 * control between consumers which might be forced to wait for a resource.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT ReusableAllocator : protected Conditional
{
protected:
    ReusableObject *freelist;
    unsigned waiting;

    /**
     * Initialize reusable allocator through a conditional.  Zero free list.
     */
    ReusableAllocator();

    /**
     * Get next reusable object in the pool.
     * @param object from list.
     * @return next object.
     */
    inline ReusableObject *next(ReusableObject *object)
        {return object->getNext();};

    /**
     * Release resuable object
     * @param object being released.
     */
    void release(ReusableObject *object);
};

/**
 * An optimized and convertable shared lock.  This is a form of read/write
 * lock that has been optimized, particularly for shared access.  Support
 * for scheduling access around writer starvation is also included.  The
 * other benefits over traditional read/write locks is that the code is
 * a little lighter, and read (shared) locks can be converted to exclusive
 * (write) locks to perform brief modify operations and then returned to read
 * locks, rather than having to release and re-aquire locks to change mode.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT ConditionalLock : protected ConditionalAccess, public SharedAccess
{
protected:
    class Context : public LinkedObject
    {
    public:
        inline Context(LinkedObject **root) : LinkedObject(root) {};

        pthread_t thread;
        unsigned count;
    };

    LinkedObject *contexts;

    virtual void _share(void);
    virtual void _unlock(void);

    Context *getContext(void);

public:
    /**
     * Construct conditional lock for default concurrency.
     */
    ConditionalLock();

    /**
     * Destroy conditional lock.
     */
    ~ConditionalLock();

    /**
     * Acquire write (exclusive modify) lock.
     */
    void modify(void);

    /**
     * Commit changes / release a modify lock.
     */
    void commit(void);

    /**
     * Acquire access (shared read) lock.
     */
    void access(void);

    /**
     * Release a shared lock.
     */
    void release(void);

    /**
     * Convert read lock into exclusive (write/modify) access.  Schedule
     * when other readers sharing.
     */
    virtual void exclusive(void);

    /**
     * Return an exclusive access lock back to share mode.
     */
    virtual void share(void);
};

/**
 * A portable implimentation of "barrier" thread sychronization.  A barrier
 * waits until a specified number of threads have all reached the barrier,
 * and then releases all the threads together.  This implimentation works
 * regardless of whether the thread library supports barriers since it is
 * built from conditional.  It also differs in that the number of threads
 * required can be changed dynamically at runtime, unlike pthread barriers
 * which, when supported, have a fixed limit defined at creation time.  Since
 * we use conditionals, another feature we can add is optional support for a
 * wait with timeout.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT barrier : private Conditional
{
private:
    unsigned count;
    unsigned waits;

public:
    /**
     * Construct a barrier with an initial size.
     * @param count of threads required.
     */
    barrier(unsigned count);

    /**
     * Destroy barrier and release pending threads.
     */
    ~barrier();

    /**
     * Dynamically alter the number of threads required.  If the size is
     * set below the currently waiting threads, then the barrier releases.
     * @param count of threads required.
     */
    void set(unsigned count);

    /**
     * Dynamically increment the number of threads required.
     */
    void inc(void);

    /**
     * Reduce the number of threads required.
     */
    void dec(void);

    /**
     * Alternative prefix form of the same increment operation.
     * @return the current amount of threads.
     */
    unsigned operator++(void);

    unsigned operator--(void);

    /**
     * Wait at the barrier until the count of threads waiting is reached.
     */
    void wait(void);

    /**
     * Wait at the barrier until either the count of threads waiting is
     * reached or a timeout has occurred.
     * @param timeout to wait in milliseconds.
     * @return true if barrier reached, false if timer expired.
     */
    bool wait(timeout_t timeout);
};

/**
 * A portable counting semaphore class.  A semaphore will allow threads
 * to pass through it until the count is reached, and blocks further threads.
 * Unlike pthread semaphore, our semaphore class supports it's count limit
 * to be altered during runtime and the use of timed waits.  This class also
 * implements the shared_lock protocol.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT Semaphore : public SharedAccess, protected Conditional
{
protected:
    unsigned count, waits, used;

    virtual void _share(void);
    virtual void _unlock(void);

public:
    /**
     * Construct a semaphore with an initial count of threads to permit.
     */
    Semaphore(unsigned count = 0);

    /**
     * Wait until the semphore usage count is less than the thread limit.
     * Increase used count for our thread when unblocked.
     */
    void wait(void);

    /**
     * Wait until the semphore usage count is less than the thread limit.
     * Increase used count for our thread when unblocked, or return without
     * changing if timed out.
     * @param timeout to wait in millseconds.
     * @return true if success, false if timeout.
     */
    bool wait(timeout_t timeout);

    /**
     * Alter semaphore limit at runtime
     * @param count of threads to allow.
     */
    void set(unsigned count);

    /**
     * Release the semaphore after waiting for it.
     */
    void release(void);

    /**
     * Convenience operator to wait on a counting semaphore.
     */
    inline void operator++(void)
        {wait();};

    /**
     * Convenience operator to release a counting semaphore.
     */
    inline void operator--(void)
        {release();};
};

/**
 * Generic non-recursive exclusive lock class.  This class also impliments
 * the exclusive_lock protocol.  In addition, an interface is offered to
 * support dynamically managed mutexes which are internally pooled.  These
 * can be used to protect and serialize arbitrary access to memory and
 * objects on demand.  This offers an advantage over embedding mutexes to
 * serialize access to individual objects since the maximum number of
 * mutexes will never be greater than the number of actually running threads
 * rather than the number of objects being potentially protected.  The
 * ability to hash the pointer address into an indexed table further optimizes
 * access by reducing the chance for collisions on the primary index mutex.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT Mutex : public ExclusiveAccess
{
protected:
    pthread_mutex_t mlock;

    virtual void _lock(void);
    virtual void _unlock(void);

public:
    /**
     * Guard class to apply scope based mutex locking to objects.  The mutex
     * is located from the mutex pool rather than contained in the target
     * object, and the lock is released when the guard object falls out of
     * scope.  This is essentially an automation mechanism for mutex::protect.
     * @author David Sugar <dyfet@gnutelephony.org>
     */
    class __EXPORT guard
    {
    private:
        const void *object;

    public:
        /**
          * Create an unitialized instance of guard.  Usually used with a
          * guard = operator.
          */
        guard();

        /**
         * Construct a guard for a specific object.
         * @param object to guard.
         */
        guard(const void *object);

        /**
         * Release mutex when guard falls out of scope.
         */
        ~guard();

        /**
         * Set guard to mutex lock a new object.  If a lock is currently
         * held, it is released.
         * @param object to guard.
         */
        void set(const void *object);

        /**
         * Prematurely release a guard.
         */
        void release(void);

        /**
         * Set guard to mutex lock a new object.  If a lock is currently
         * held, it is released.
         * @param pointer to object to guard.
         */
        inline void operator=(void *pointer)
            {set(pointer);};
    };


    /**
     * Create a mutex lock.
     */
    Mutex();

    /**
     * Destroy mutex lock, release waiting threads.
     */
    ~Mutex();

    /**
     * Acquire mutex lock.  This is a blocking operation.
     */
    inline void acquire(void)
        {pthread_mutex_lock(&mlock);};

    /**
     * Acquire mutex lock.  This is a blocking operation.
     */
    inline void lock(void)
        {pthread_mutex_lock(&mlock);};

    /**
     * Release acquired lock.
     */
    inline void unlock(void)
        {pthread_mutex_unlock(&mlock);};

    /**
     * Release acquired lock.
     */
    inline void release(void)
        {pthread_mutex_unlock(&mlock);};

    /**
     * Convenience function to acquire os native mutex lock directly.
     * @param lock to acquire.
     */
    inline static void acquire(pthread_mutex_t *lock)
        {pthread_mutex_lock(lock);};

    /**
     * Convenience function to release os native mutex lock directly.
     * @param lock to release.
     */
    inline static void release(pthread_mutex_t *lock)
        {pthread_mutex_unlock(lock);};

    /**
     * Specify hash table size for guard protection.  The default is 1.
     * This should be called at initialization time from the main thread
     * of the application before any other threads are created.
     * @param size of hash table used for guarding.
     */
    static void indexing(unsigned size);

    /**
     * Specify pointer/object/resource to guard protect.  This uses a
     * dynamically managed mutex.
     * @param pointer to protect.
     */
    static void protect(const void *pointer);

    /**
     * Specify a pointer/object/resource to release.
     * @param pointer to release.
     */
    static void release(const void *pointer);
};

/**
 * A mutex locked object smart pointer helper class.  This is particularly
 * useful in referencing objects which will be protected by the mutex
 * protect function.  When the pointer falls out of scope, the protecting
 * mutex is also released.  This is meant to be used by the typed
 * mutex_pointer template.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT auto_protect
{
private:
    // cannot copy...
    inline auto_protect(const auto_object &pointer) {};

protected:
    const void *object;

    auto_protect();

public:
    /**
     * Construct a protected pointer referencing an existing object.
     * @param object we point to.
     */
    auto_protect(const void *object);

    /**
     * Delete protected pointer.  When it falls out of scope the associated
     * mutex is released.
     */
    ~auto_protect();

    /**
     * Manually release the pointer.  This releases the mutex.
     */
    void release(void);

    /**
     * Test if the pointer is not set.
     * @return true if the pointer is not referencing anything.
     */
    inline bool operator!() const
        {return object == NULL;};

    /**
     * Test if the pointer is referencing an object.
     * @return true if the pointer is currently referencing an object.
     */
    inline operator bool() const
        {return object != NULL;};

    /**
     * Set our pointer to a specific object.  If the pointer currently
     * references another object, the associated mutex is released.  The
     * pointer references our new object and that new object is locked.
     * @param object to assign to.
     */
    void operator=(const void *object);
};

/**
 * An object pointer that uses mutex to assure thread-safe singleton use.
 * This class is used to support a threadsafe replacable pointer to a object.
 * This class is used to form and support the templated locked_pointer class
 * and used with the locked_release class.  An example of where this might be
 * used is in config file parsers, where a seperate thread may process and
 * generate a new config object for new threads to refernce, while the old
 * configuration continues to be used by a  reference counted instance that
 * goes away when it falls out of scope.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT LockedPointer
{
private:
    friend class locked_release;
    pthread_mutex_t mutex;
    ObjectProtocol *pointer;

protected:
    /**
     * Create an instance of a locked pointer.
     */
    LockedPointer();

    /**
     * Replace existing object with a new one for next request.
     * @param object to register with pointer.
     */
    void replace(ObjectProtocol *object);

    /**
     * Create a duplicate reference counted instance of the current object.
     * @return duplicate reference counted object.
     */
    ObjectProtocol *dup(void);

    /**
     * Replace existing object through assignment.
     * @param object to assign.
     */
    inline void operator=(ObjectProtocol *object)
        {replace(object);};
};

/**
 * Shared singleton object.  A shared singleton object is a special kind of
 * object that may be shared by multiple threads but which only one active
 * instance is allowed to exist.  The shared object is managed by the
 * templated shared pointer class, and is meant to be inherited as a base
 * class for the derived shared singleton type.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT SharedObject
{
protected:
    friend class SharedPointer;

    /**
     * Commit is called when a shared singleton is accepted and replaces
     * a prior instance managed by a shared pointer.  Commit occurs
     * when replace is called on the shared pointer, and is assured to
     * happen only when no threads are accessing either the current
     * or the prior instance that was previously protected by the pointer.
     * @param pointer that now holds the object.
     */
    virtual void commit(SharedPointer *pointer);

public:
    /**
     * Allows inherited virtual.
     */
    virtual ~SharedObject();
};

/**
 * The shared pointer is used to manage a singleton instance of shared object.
 * This class is used to support the templated shared_pointer class and the
 * shared_release class, and is not meant to be used directly or as a base
 * for anything else.  One or more threads may aquire a shared lock to the
 * singleton object through this pointer, and it can only be replaced with a
 * new singleton instance when no threads reference it.  The conditional lock
 * is used to manage shared access for use and exclusive access when modified.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT SharedPointer : protected ConditionalAccess
{
private:
    friend class shared_release;
    SharedObject *pointer;

protected:
    /**
     * Created shared locking for pointer.  Must be assigned by replace.
     */
    SharedPointer();

    /**
     * Destroy lock and release any blocked threads.
     */
    ~SharedPointer();

    /**
     * Replace existing singleton instance with new one.  This happens
     * during exclusive locking, and the commit method of the object
     * will be called.
     * @param object being set.
     */
    void replace(SharedObject *object);

    /**
     * Acquire a shared reference to the singleton object.  This is a
     * form of shared access lock.  Derived classes and templates access
     * "release" when the shared pointer is no longer needed.
     * @return shared object.
     */
    SharedObject *share(void);
};

/**
 * An abstract class for defining classes that operate as a thread.  A derived
 * thread class has a run method that is invoked with the newly created
 * thread context, and can use the derived object to store all member data
 * that needs to be associated with that context.  This means the derived
 * object can safely hold thread-specific data that is managed with the life
 * of the object, rather than having to use the clumsy thread-specific data
 * management and access functions found in thread support libraries.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT Thread
{
protected:
// may be used in future if we need cancelable threads...
#ifdef  _MSWINDOWS_
    HANDLE cancellor;
#else
    void *cancellor;
#endif

    enum {} reserved;   // cancel mode?
    pthread_t tid;
    size_t stack;
    int priority;

    /**
     * Create a thread object that will have a preset stack size.  If 0
     * is used, then the stack size is os defined/default.
     * @param stack size to use or 0 for default.
     */
    Thread(size_t stack = 0);

    /**
     * Map thread for get method.  This should be called from start of the
     * run() method of a derived class.
     */
    void map(void);

    /**
     * Check if running.
     */
    virtual bool is_active(void);

public:
    /**
     * Set thread priority without disrupting scheduling if possible.
     * Based on scheduling policy.  It is recommended that the process
     * is set for realtime scheduling, and this method is actually for
     * internal use.
     */
    void setPriority(void);

    /**
     * Yield execution context of the current thread. This is a static
     * and may be used anywhere.
     */
    static void yield(void);

    /**
     * Sleep current thread for a specified time period.
     * @param timeout to sleep for in milliseconds.
     */
    static void sleep(timeout_t timeout);

    /**
     * Get mapped thread object.  This returns the mapped base class of the
     * thread object of the current executing context.  You will need to
     * cast to the correct derived class to access derived thread-specific
     * storage.  If the current thread context is not mapped NULL is returned.
     */
    static Thread *get(void);

    /**
     * Abstract interface for thread context run method.
     */
    virtual void run(void) = 0;

    /**
     * Destroy thread object, thread-specific data, and execution context.
     */
    virtual ~Thread();

    /**
     * Exit the thread context.  This function should NO LONGER be called
     * directly to exit a running thread.  Instead this method will only be
     * used to modify the behavior of the thread context at thread exit,
     * including detached threads which by default delete themselves.  This
     * documented usage was changed to support Mozilla NSPR exit behavior
     * in case we support NSPR as an alternate thread runtime in the future.
     */
    virtual void exit(void);

    /**
     * Used to initialize threading library.  May be needed for some platforms.
     */
    static void init(void);

    /**
     * Used to specify scheduling policy for threads above priority "0".
     * Normally we apply static realtime policy SCHED_FIFO (default) or
     * SCHED_RR.  However, we could apply SCHED_OTHER, etc.
     */
    static void policy(int polid);

    /**
     * Set concurrency level of process.  This is essentially a portable
     * wrapper for pthread_setconcurrency.
     */
    static void concurrency(int level);

    /**
     * Determine if two thread identifiers refer to the same thread.
     * @param thread1 to test.
     * @param thread2 to test.
     * @return true if both are the same context.
     */
    static bool equal(pthread_t thread1, pthread_t thread2);

    /**
     * Get current thread id.
     * @return thread id.
     */
    static pthread_t self(void);

    inline operator bool()
        {return is_active();}

    inline bool operator!()
        {return !is_active();}

    inline bool isRunning(void)
        {return is_active();}
};

/**
 * A child thread object that may be joined by parent.  A child thread is
 * a type of thread in which the parent thread (or process main thread) can
 * then wait for the child thread to complete and then delete the child object.
 * The parent thread can wait for the child thread to complete either by
 * calling join, or performing a "delete" of the derived child object.  In
 * either case the parent thread will suspend execution until the child thread
 * exits.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT JoinableThread : public Thread
{
protected:
#ifdef  _MSWINDOWS_
    HANDLE running;
#else
    volatile bool running;
#endif
    volatile bool joining;

    /**
     * Create a joinable thread with a known context stack size.
     * @param size of stack for thread context or 0 for default.
     */
    JoinableThread(size_t size = 0);

    /**
     * Delete child thread.  Parent thread suspends until child thread
     * run method completes or child thread calls it's exit method.
     */
    virtual ~JoinableThread();

    /**
     * Join thread with parent.  Calling from a child thread to exit is
     * now depreciated behavior and in the future will not be supported.
     * Threads should always return through their run() method.
     */
    void join(void);

    bool is_active(void);

    virtual void run(void) = 0;

public:

    /**
     * Start execution of child context.  This must be called after the
     * child object is created (perhaps with "new") and before it can be
     * joined.  This method actually begins the new thread context, which
     * then calls the object's run method.  Optionally raise the priority
     * of the thread when it starts under realtime priority.
     * @param priority of child thread.
     */
    void start(int priority = 0);

    /**
     * Start execution of child context as background thread.  This is
     * assumed to be off main thread, with a priority lowered by one.
     */
    inline void background(void)
        {start(-1);};
};

/**
 * A detached thread object that is stand-alone.  This object has no
 * relationship with any other running thread instance will be automatically
 * deleted when the running thread instance exits, either by it's run method
 * exiting, or explicity calling the exit member function.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT DetachedThread : public Thread
{
protected:
    bool active;

    /**
     * Create a detached thread with a known context stack size.
     * @param size of stack for thread context or 0 for default.
     */
    DetachedThread(size_t size = 0);

    /**
     * Destroys object when thread context exits.  Never externally
     * deleted.  Derived object may also have destructor to clean up
     * thread-specific member data.
     */
    ~DetachedThread();

    /**
     * Exit context of detached thread.  Thread object will be deleted.
     * This function should NO LONGER be called directly to exit a running
     * thread.  Instead, the thread should only "return" through the run()
     * method to exit.  The documented usage was changed so that exit() can
     * still be used to modify the "delete this" behavior of detached threads
     * while merging thread exit behavior with Mozilla NSPR.
     */
    void exit(void);

    bool is_active(void);

    virtual void run(void) = 0;

public:
    /**
     * Start execution of detached context.  This must be called after the
     * object is created (perhaps with "new"). This method actually begins
     * the new thread context, which then calls the object's run method.
     * @param priority to start thread with.
     */
    void start(int priority = 0);
};

/**
 * Auto-pointer support class for locked objects.  This is used as a base
 * class for the templated locked_instance class that uses the managed
 * LockedPointer to assign a reference to an object.  When the locked
 * instance falls out of scope, the object is derefenced.  Ideally the
 * pointer typed object should be based on the reference counted object class.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT locked_release
{
protected:
    ObjectProtocol *object; /**< locked object protected by locked_release */

    /**
     * Create an unassigned locked object pointer base.
     */
    locked_release();

    /**
     * Construct a locked object instance base from an existing instance.  This
     * will create a duplicate (retained) reference.
     * @param object to copy from.
     */
    locked_release(const locked_release &object);

public:
    /**
     * Construct a locked object instance base from a LockedPointer.  References
     * a retained instance of the underlying object from the LockedPointer.
     * @param pointer of locked pointer to assign from.
     */
    locked_release(LockedPointer &pointer);

    /**
     * Auto-release pointer to locked object instance.  This is used to release
     * a reference when the pointer template falls out of scope.
     */
    ~locked_release();

    /**
     * Manually release the object reference.
     */
    void release(void);

    /**
     * Assign a locked object pointer.  If an existing object is already
     * assigned, the existing pointer is released.
     * @param pointer reference through locked object.
     */
    locked_release &operator=(LockedPointer &pointer);
};

/**
 * Auto-pointer support class for shared singleton objects.  This is used as
 * a base class for the templated shared_instance class that uses shared
 * access locking through the SharedPointer class.  When the shared instance
 * falls out of scope, the SharedPointer lock is released.  The pointer
 * typed object must be based on the SharedObject class.
 * @author David Sugar <dyfet@gnutelephony.org>
 */

class __EXPORT shared_release
{
protected:
    SharedPointer *ptr; /**< Shared lock for protected singleton */

    /**
     * Create an unassigned shared singleton object pointer base.
     */
    shared_release();

    /**
     * Construct a shared object instance base from an existing instance.  This
     * will assign an additional shared lock.
     * @param object to copy from.
     */
    shared_release(const shared_release &object);

public:
    /**
     * Access lock a shared singleton instance from a SharedPointer.
     * @param pointer of shared pointer to assign from.
     */
    shared_release(SharedPointer &pointer);

    /**
     * Auto-unlock shared lock for singleton instance protected by shared
     * pointer.  This is used to unlock when the instance template falls out
     * of scope.
     */
    ~shared_release();

    /**
     * Manually release access to shared singleton object.
     */
    void release(void);

    /**
     * Get pointer to singleton object that we have shared lock for.
     * @return shared object singleton.
     */
    SharedObject *get(void);

    /**
     * Assign shared lock access to shared singleton.  If an existing
     * shared lock is held for another pointer, it is released.
     * @param pointer access for shared object.
     */
    shared_release &operator=(SharedPointer &pointer);
};

/**
 * Templated shared pointer for singleton shared objects of specific type.
 * This is used as typed template for the SharedPointer object reference
 * management class.  This is used to supply a typed singleton shared
 * instance to the typed shared_instance template class.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<class T>
class shared_pointer : public SharedPointer
{
public:
    /**
     * Created shared locking for typed singleton pointer.
     */
    inline shared_pointer() : SharedPointer() {};

    /**
     * Acquire a shared (duplocate) reference to the typed singleton object.
     * This is a form of shared access lock.  Derived classes and templates
     * access conditionallock "release" when the shared pointer is no longer
     * needed.
     * @return typed shared object.
     */
    inline const T *dup(void)
        {return static_cast<const T*>(SharedPointer::share());};

    /**
     * Replace existing typed singleton instance with new one.  This happens
     * during exclusive locking, and the commit method of the typed object
     * will be called.
     * @param object being set.
     */
    inline void replace(T *object)
        {SharedPointer::replace(object);};

    /**
     * Replace existing typed singleton object through assignment.
     * @param object to assign.
     */
    inline void operator=(T *object)
        {replace(object);};

    /**
     * Access shared lock typed singleton object by pointer reference.
     * @return typed shared object.
     */
    inline T *operator*()
        {return dup();};
};

/**
 * Templated locked pointer for referencing locked objects of specific type.
 * This is used as typed template for the LockedPointer object reference
 * management class.  This is used to supply a typed locked instances
 * to the typed locked_instance template class.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<class T>
class locked_pointer : public LockedPointer
{
public:
    /**
     * Create an instance of a typed locked pointer.
     */
    inline locked_pointer() : LockedPointer() {};

    /**
     * Create a duplicate reference counted instance of the current typed
     * object.
     * @return duplicate reference counted typed object.
     */
    inline T* dup(void)
        {return static_cast<T *>(LockedPointer::dup());};

    /**
     * Replace existing typed object with a new one for next request.
     * @param object to register with pointer.
     */
    inline void replace(T *object)
        {LockedPointer::replace(object);};

    /**
     * Replace existing object through assignment.
     * @param object to assign.
     */
    inline void operator=(T *object)
        {replace(object);};

    /**
     * Create a duplicate reference counted instance of the current typed
     * object by pointer reference.
     * @return duplicate reference counted typed object.
     */
    inline T *operator*()
        {return dup();};
};

/**
 * A templated smart pointer instance for lock protected objects.
 * This is used to reference an instance of a typed locked_pointer.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<class T>
class locked_instance : public locked_release
{
public:
    /**
     * Construct empty locked instance of typed object.
     */
    inline locked_instance() : locked_release() {};

    /**
     * Construct locked instance of typed object from matching locked_pointer.
     * @param pointer to get instance from.
     */
    inline locked_instance(locked_pointer<T> &pointer) : locked_release(pointer) {};

    /**
     * Extract instance of locked typed object by pointer reference.
     * @return instance of typed object.
     */
    inline T& operator*() const
        {return *(static_cast<T&>(object));};

    /**
     * Access member of instance of locked typed object by member reference.
     * @return instance of typed object.
     */
    inline T* operator->() const
        {return static_cast<T*>(object);};

    /**
     * Get pointer to instance of locked typed object.
     * @return instance of typed object.
     */
    inline T* get(void) const
        {return static_cast<T*>(object);};
};

/**
 * A templated smart pointer instance for shared singleton typed objects.
 * This is used to access the shared lock instance of the singleton.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<class T>
class shared_instance : public shared_release
{
public:
    /**
     * Construct empty instance to reference shared typed singleton.
     */
    inline shared_instance() : shared_release() {};

    /**
     * Construct shared access instance of shared typed singleton from matching
     * shared_pointer.
     * @param pointer to get instance from.
     */
    inline shared_instance(shared_pointer<T> &pointer) : shared_release(pointer) {};

    /**
     * Access shared typed singleton object this instance locks and references.
     */
    inline const T& operator*() const
        {return *(static_cast<const T&>(ptr->pointer));};

    /**
     * Access member of shared typed singleton object this instance locks and
     * references.
     */
    inline const T* operator->() const
        {return static_cast<const T*>(ptr->pointer);};

    /**
     * Access pointer to typed singleton object this instance locks and
     * references.
     */
    inline const T* get(void) const
        {return static_cast<const T*>(ptr->pointer);};
};

/**
 * Typed smart locked pointer class.  This is used to manage references to
 * objects which are protected by an auto-generated mutex.  The mutex is
 * released when the pointer falls out of scope.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T>
class mutex_pointer : public auto_protect
{
public:
    /**
     * Create a pointer with no reference.
     */
    inline mutex_pointer() : auto_protect() {};

    /**
     * Create a pointer with a reference to a heap object.
     * @param object we are referencing.
     */
    inline mutex_pointer(T* object) : auto_protect(object) {};

    /**
     * Reference object we are pointing to through pointer indirection.
     * @return object we are pointing to.
     */
    inline T& operator*() const
        {return *(static_cast<T&>(auto_protect::object));};

    /**
     * Reference member of object we are pointing to.
     * @return reference to member of pointed object.
     */
    inline T* operator->() const
        {return static_cast<T*>(auto_protect::object);};

    /**
     * Get pointer to object.
     * @return pointer or NULL if we are not referencing an object.
     */
    inline T* get(void) const
        {return static_cast<T*>(auto_protect::object);};
};

/**
 * Convenience function to start a joinable thread.
 * @param thread to start.
 * @param priority of thread.
 */
inline void start(JoinableThread *thread, int priority = 0)
    {thread->start(priority);}

/**
 * Convenience function to start a detached thread.
 * @param thread to start.
 * @param priority of thread.
 */
inline void start(DetachedThread *thread, int priority = 0)
    {thread->start(priority);}

/**
 * Convenience type for using conditional locks.
 */
typedef ConditionalLock condlock_t;

/**
 * Convenience type for scheduling access.
 */
typedef ConditionalAccess accesslock_t;

/**
 * Convenience type for using timed events.
 */
typedef TimedEvent timedevent_t;

/**
 * Convenience type for using exclusive mutex locks.
 */
typedef Mutex mutex_t;

/**
 * Convenience type for using read/write locks.
 */
typedef ThreadLock rwlock_t;

/**
 * Convenience type for using recursive exclusive locks.
 */
typedef RecursiveMutex rexlock_t;

/**
 * Convenience type for using counting semaphores.
 */
typedef Semaphore semaphore_t;

/**
 * Convenience type for using thread barriers.
 */
typedef barrier barrier_t;

/**
 * Convenience function to wait on a barrier.
 * @param barrier to wait.
 */
inline void wait(barrier_t &barrier)
    {barrier.wait();}

/**
 * Convenience function to wait on a semaphore.
 * @param semaphore to wait on.
 * @param timeout to wait for.
 */
inline void wait(semaphore_t &semaphore, timeout_t timeout = Timer::inf)
    {semaphore.wait(timeout);}

/**
 * Convenience function to release a semaphore.
 * @param semaphore to release.
 */
inline void release(semaphore_t &semaphore)
    {semaphore.release();}

/**
 * Convenience function to acquire a mutex.
 * @param mutex to acquire.
 */
inline void acquire(mutex_t &mutex)
    {mutex.lock();}

/**
 * Convenience function to release a mutex.
 * @param mutex to release.
 */
inline void release(mutex_t &mutex)
    {mutex.release();}

/**
 * Convenience function to exclusively schedule conditional access.
 * @param lock to make exclusive.
 */
inline void modify(accesslock_t &lock)
    {lock.modify();}

/**
 * Convenience function to shared read schedule conditional access.
 * @param lock to access shared.
 */
inline void access(accesslock_t &lock)
    {lock.access();}

/**
 * Convenience function to release an access lock.
 * @param lock to release.
 */
inline void release(accesslock_t &lock)
    {lock.release();}

/**
 * Convenience function to commit an exclusive access lock.
 * lock.
 * @param lock to commit.
 */
inline void commit(accesslock_t &lock)
    {lock.commit();}

/**
 * Convenience function to exclusively lock shared conditional lock.
 * @param lock to make exclusive.
 */
inline void exclusive(condlock_t &lock)
    {lock.exclusive();}

/**
 * Convenience function to restore shared access on a conditional lock.
 * @param lock to make shared.
 */
inline void share(condlock_t &lock)
    {lock.share();}

/**
 * Convenience function to exclusively aquire a conditional lock.
 * @param lock to acquire for modify.
 */
inline void modify(condlock_t &lock)
    {lock.modify();}

/**
 * Convenience function to commit and release an exclusively locked conditional
 * lock.
 * @param lock to commit.
 */
inline void commit(condlock_t &lock)
    {lock.commit();}

/**
 * Convenience function for shared access to a conditional lock.
 * @param lock to access.
 */
inline void access(condlock_t &lock)
    {lock.access();}

/**
 * Convenience function to release shared access to a conditional lock.
 * @param lock to release.
 */
inline void release(condlock_t &lock)
    {lock.release();}

/**
 * Convenience function for exclusive write access to a read/write lock.
 * @param lock to write lock.
 * @param timeout to wait for exclusive locking.
 */
inline bool exclusive(rwlock_t &lock, timeout_t timeout = Timer::inf)
    {return lock.modify(timeout);}

/**
 * Convenience function for shared read access to a read/write lock.
 * @param lock to share read lock.
 * @param timeout to wait for shared access.
 */
inline bool share(rwlock_t &lock, timeout_t timeout = Timer::inf)
    {return lock.access(timeout);}

/**
 * Convenience function to release a shared lock.
 * @param lock to release.
 */
inline void release(rwlock_t &lock)
    {lock.release();}

/**
 * Convenience function to lock a shared recursive mutex lock.
 * @param lock to acquire.
 */
inline void lock(rexlock_t &lock)
    {lock.lock();}

/**
 * Convenience function to release a shared recursive mutex lock.
 * @param lock to release.
 */
inline void release(rexlock_t &lock)
    {lock.release();}

inline bool _sync_protect_(const void *obj)
{
    Mutex::protect(obj);
    return true;
}

inline bool _sync_release_(const void *obj)
{
    Mutex::release(obj);
    return false;
}

inline bool _rw_reader_(const void *obj)
{
    ThreadLock::reader(obj);
    return true;
}

inline bool _rw_writer_(const void *obj)
{
    ThreadLock::writer(obj);
    return true;
}

inline bool _rw_release_(const void *obj)
{
    ThreadLock::release(obj);
    return false;
}

#define ENTER_EXCLUSIVE \
    do { static pthread_mutex_t __sync__ = PTHREAD_MUTEX_INITIALIZER; \
        pthread_mutex_lock(&__sync__);

#define LEAVE_EXCLUSIVE \
    pthread_mutex_unlock(&__sync__);} while(0);

#define SYNC(obj) for(bool _sync_flag_ = _sync_protect_(obj); _sync_flag_; _sync_flag_ = _sync_release_(obj))

#define SHARED(obj) for(bool _sync_flag_ = _rw_reader_(obj); _sync_flag_; _sync_flag_ = _rw_release_(obj))

#define EXCLUSIVE(obj) for(bool _sync_flag_ = _rw_writer_(obj); _sync_flag_; _sync_flag_ = _rw_release_(obj))

END_NAMESPACE

#endif
