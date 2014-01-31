// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 3 of the License, or
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

/**
 * @file commoncpp/thread.h
 * @short Common C++ thread class and sychronization objects
 **/

#ifndef COMMONCPP_THREAD_H_
#define COMMONCPP_THREAD_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_STRING_H_
#include <commoncpp/string.h>
#endif

#define ENTER_CRITICAL  enterMutex();
#define LEAVE_CRITICAL  leaveMutex();

NAMESPACE_COMMONCPP

class __EXPORT Mutex : protected ucommon::RecursiveMutex
{
public:
    inline Mutex() : RecursiveMutex() {};

    inline void enterMutex(void)
        {RecursiveMutex::lock();};

    inline void leaveMutex(void)
        {RecursiveMutex::release();};

    inline bool tryEnterMutex(void)
        {return RecursiveMutex::lock(0l);};

    inline void enter(void)
        {RecursiveMutex::lock();};

    inline void leave(void)
        {RecursiveMutex::release();};

    inline bool test(void)
        {return RecursiveMutex::lock(0l);};

};

/**
 * The Mutex Counter is a counter variable which can safely be incremented
 * or decremented by multiple threads.  A Mutex is used to protect access
 * to the counter variable (an integer).  An initial value can be specified
 * for the counter, and it can be manipulated with the ++ and -- operators.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Thread protected integer counter.
 */
class __EXPORT MutexCounter : public Mutex
{
protected:
    volatile int    counter;

public:
    /**
     * Create and optionally name a mutex protected counter.
     */
    MutexCounter();

    /**
     * Create and optionally name a mutex protected counter with
     * an initial value.
     *
     * @param initial value of counter.
     */
    MutexCounter(int initial);

    int operator++();
    int operator--();
};

/**
 * The MutexLock class is used to protect a section of code so that at any
 * given time only a single thread can perform the protected operation.
 *
 * It use Mutex to protect operation. Using this class is usefull and
 * exception safe.  The mutex that has been locked is automatically
 * released when the function call stack falls out of scope, so one doesnt
 * have to remember to unlock the mutex at each function return.
 *
 * A common use is
 *
 * void func_to_protect()
 * {
 *   MutexLock lock(mutex);
 *   ... operation ...
 * }
 *
 * NOTE: do not declare variable as "MutexLock (mutex)", the mutex will be
 * released at statement end.
 *
 * @author Frediano Ziglio <freddy77@angelfire.com>
 * @short Mutex automatic locker for protected access.
 */
class __EXPORT MutexLock
{
private:
    Mutex& mutex;

public:
    /**
     * Acquire the mutex
     *
     * @param _mutex reference to mutex to aquire.
     */
    inline MutexLock( Mutex& _mutex ) : mutex( _mutex )
        { mutex.enterMutex(); }

    /**
     * Release the mutex automatically
     */
    // this should be not-virtual
    inline ~MutexLock()
        { mutex.leaveMutex(); }
};

class __EXPORT ThreadLock : protected ucommon::ThreadLock
{
public:
    inline ThreadLock() : ucommon::ThreadLock() {};

    inline void readLock(void)
        {ucommon::ThreadLock::access();};

    inline void writeLock(void)
        {ucommon::ThreadLock::modify();};

    inline void tryReadLock(void)
        {ucommon::ThreadLock::access(0);};

    inline void tryWriteLock(void)
        {ucommon::ThreadLock::modify(0);};

    inline void unlock(void)
        {ucommon::ThreadLock::release();};
};

/**
 * The ReadLock class is used to protect a section of code through
 * a ThreadLock for "read" access to the member function.  The
 * ThreadLock is automatically released when the object falls out of
 * scope.
 *
 * A common use is
 *
 * void func_to_protect()
 * {
 *   ReadLock lock(threadlock);
 *   ... operation ...
 * }
 *
 * NOTE: do not declare variable as "ReadLock (threadlock)", the
 * mutex will be released at statement end.
 *
 * @author David Sugar <dyfet@gnu.org>
 * @short Read mode automatic locker for protected access.
 */
class __EXPORT ReadLock
{
private:
    ThreadLock& tl;

public:
    /**
     * Wait for read access
     *
     * @param _tl reference to lock to aquire.
     */
    inline ReadLock( ThreadLock& _tl ) : tl( _tl )
        { tl.readLock(); }
    /**
     * Post the semaphore automatically
     */
    // this should be not-virtual
    inline ~ReadLock()
        { tl.unlock(); }
};

/**
 * The WriteLock class is used to protect a section of code through
 * a ThreadLock for "write" access to the member function.  The
 * ThreadLock is automatically released when the object falls out of
 * scope.
 *
 * A common use is
 *
 * void func_to_protect()
 * {
 *   WriteLock lock(threadlock);
 *   ... operation ...
 * }
 *
 * NOTE: do not declare variable as "WriteLock (threadlock)", the
 * mutex will be released at statement end.
 *
 * @author David Sugar <dyfet@gnu.org>
 * @short Read mode automatic locker for protected access.
 */
class __EXPORT WriteLock
{
private:
    ThreadLock& tl;

public:
    /**
     * Wait for write access
     *
     * @param _tl reference to threadlock to aquire.
     */
    inline WriteLock( ThreadLock& _tl ) : tl( _tl )
        { tl.writeLock(); }
    /**
     * Post the semaphore automatically
     */
    // this should be not-virtual
    inline ~WriteLock()
        { tl.unlock(); }
};

class __EXPORT Conditional : private ucommon::Conditional
{
public:
    inline Conditional() : ucommon::Conditional() {};

    bool wait(timeout_t timeout, bool locked = false);

    void signal(bool broadcast);

    inline void enterMutex(void)
        {ucommon::Conditional::lock();};

    inline void leaveMutex(void)
        {ucommon::Conditional::unlock();};
};

class __EXPORT Semaphore : private ucommon::Semaphore
{
public:
    inline Semaphore(unsigned size=0) : ucommon::Semaphore(size) {};

    inline bool wait(timeout_t timeout = 0)
        {return ucommon::Semaphore::wait(timeout);};

    inline void post(void)
        {ucommon::Semaphore::release();};
};

/**
 * The SemaphoreLock class is used to protect a section of code through
 * a semaphore so that only x instances of the member function may
 * execute concurrently.
 *
 * A common use is
 *
 * void func_to_protect()
 * {
 *   SemaphoreLock lock(semaphore);
 *   ... operation ...
 * }
 *
 * NOTE: do not declare variable as "SemaohoreLock (semaphore)", the
 * mutex will be released at statement end.
 *
 * @author David Sugar <dyfet@gnu.org>
 * @short Semaphore automatic locker for protected access.
 */
class __EXPORT SemaphoreLock
{
private:
    Semaphore& sem;

public:
    /**
     * Wait for the semaphore
     */
    inline SemaphoreLock( Semaphore& _sem ) : sem( _sem )
        { sem.wait(); }
    /**
     * Post the semaphore automatically
     */
    // this should be not-virtual
    inline ~SemaphoreLock()
        { sem.post(); }
};

class __EXPORT Event : private ucommon::TimedEvent
{
public:
    inline Event() : TimedEvent() {};

    inline void wait(void)
        {ucommon::TimedEvent::wait(Timer::inf);};

    inline bool wait(timeout_t timeout)
        {return ucommon::TimedEvent::wait(timeout);};

    inline void signal(void)
        {ucommon::TimedEvent::signal();};

    inline void reset(void)
        {ucommon::TimedEvent::reset();};
};

class __EXPORT Thread : protected ucommon::JoinableThread
{
public:
    /**
     * How to raise error
     */
    typedef enum Throw {
        throwNothing,  /**< continue without throwing error */
        throwObject,   /**< throw object that cause error (throw this) */
        throwException /**< throw an object relative to error */
    } Throw;

private:
    friend class Slog;

    Throw exceptions;
    bool detached, terminated;
    Thread *parent;
    size_t msgpos;
    char msgbuf[128];

public:
    Thread(int pri = 0, size_t stack = 0);

    virtual ~Thread();

    inline void map(void)
        {JoinableThread::map();};

    virtual void initial(void);
    virtual void notify(Thread *thread);
    virtual void final(void);
    virtual void run(void) = 0;

    void terminate(void);
    void finalize(void);

    void detach(void);
    void start(void);
    void exit(void);

    inline void join(void)
        {JoinableThread::join();};

    inline void sync(void)
        {Thread::exit();};

    static inline Thread *get(void)
        {return (Thread *)JoinableThread::get();};

    inline static void yield(void)
        {ucommon::Thread::yield();};

    inline static void sleep(timeout_t msec = TIMEOUT_INF)
        {ucommon::Thread::sleep(msec);};

    bool isRunning(void);

    bool isThread(void);

    /**
     * Get exception mode of the current thread.
     *
     * @return exception mode.
     */
    static Throw getException(void);

    /**
     * Set exception mode of the current thread.
     *
     * @return exception mode.
     */
    static void setException(Throw mode);

    /**
     * Get the thread id.
     */
    inline pthread_t getId(void)
        {return tid;};
};

/**
 * This class is used to access non-reentrant date and time functions in the
 * standard C library.
 *
 * The class has two purposes:
 * - 1 To be used internaly in CommonCpp's date and time classes to make them
 *     thread safe.
 * - 2 To be used by clients as thread safe replacements to the standard C
 *     functions, much like Thread::sleep() represents a thread safe version
 *     of the standard sleep() function.
 *
 * @note The class provides one function with the same name as its equivalent
 *       standard function and one with another, unique name. For new clients,
 *       the version with the unique name is recommended to make it easy to
 *       grep for accidental usage of the standard functions. The version with
 *       the standard name is provided for existing clients to sed replace their
 *       original version.
 *
 * @note Also note that some functions that returned pointers have been redone
 *       to take that pointer as an argument instead, making the caller
 *       responsible for memory allocation/deallocation. This is almost
 *       how POSIX specifies *_r functions (reentrant versions of the
 *       standard time functions), except the POSIX functions also return the
 *       given pointer while we do not. We don't use the *_r functions as they
 *       aren't all generally available on all platforms yet.
 *
 * @author Idar Tollefsen <idar@cognita.no>
 * @short Thread safe date and time functions.
 */
class __EXPORT SysTime
{
public:
    static time_t getTime(time_t *tloc = NULL);
    static time_t time(time_t *tloc)
        { return getTime(tloc); };

    static int getTimeOfDay(struct timeval *tp);
    static int gettimeofday(struct timeval *tp, struct timezone *)
        { return getTimeOfDay(tp); };

    static struct tm *getLocalTime(const time_t *clock, struct tm *result);
    static struct tm *locatime(const time_t *clock, struct tm *result)
        { return getLocalTime(clock, result); };

    static struct tm *getGMTTime(const time_t *clock, struct tm *result);
    static struct tm *gmtime(const time_t *clock, struct tm *result)
        { return getGMTTime(clock, result);};
};

/**
 * Timer ports are used to provide synchronized timing events when managed
 * under a "service thread" such as SocketService.  This is made into a
 * stand-alone base class since other derived libraries (such as the
 * serial handlers) may also use the pooled "service thread" model
 * and hence also require this code for managing timing.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short synchronized millisecond timing for service threads.
 */
class __EXPORT TimerPort
{
#ifndef _MSWINDOWS_
    struct timeval timer;
#else
    DWORD timer;
#endif
    bool active;

public:
    /**
     * Create a timer, mark it as inactive, and set the initial
     * "start" time to the creation time of the timer object.  This
     * allows "incTimer" to initially refer to time delays relative
     * to the original start time of the object.
     */
    TimerPort();

    /**
     * Set a new start time for the object based on when this call is
     * made and optionally activate the timer for a specified number
     * of milliseconds.  This can be used to set the starting time
     * of a realtime session.
     *
     * @param timeout delay in milliseconds from "now"
     */
    void setTimer(timeout_t timeout = 0);

    /**
     * Set a timeout based on the current time reference value either
     * from object creation or the last setTimer().  This reference
     * can be used to time synchronize realtime data over specified
     * intervals and force expiration when a new frame should be
     * released in a synchronized manner.
     *
     * @param timeout delay in milliseconds from reference.
     */
    void incTimer(timeout_t timeout);

    /**
     * Adjust a timeout based on the current time reference value either
     * from object creation or the last setTimer().  This reference
     * can be used to time synchronize realtime data over specified
     * intervals and force expiration when a new frame should be
     * released in a synchronized manner.
     *
     * @param timeout delay in milliseconds from reference.
     */
    void decTimer(timeout_t timeout);

    /**
     * Sleep until the current timer expires.  This is useful in time
     * syncing realtime periodic tasks.
     */
    void sleepTimer(void);

    /**
     * This is used to "disable" the service thread from expiring
     * the timer object.  It does not effect the reference time from
     * either creation or a setTimer().
     */
    void endTimer(void);

    /**
     * This is used by service threads to determine how much time
     * remains before the timer expires based on a timeout specified
     * in setTimer() or incTimer().  It can also be called after
     * setting a timeout with incTimer() to see if the current timeout
     * has already expired and hence that the application is already
     * delayed and should skip frame(s).
     *
     * return time remaining in milliseconds, or TIMEOUT_INF if
     * inactive.
     */
    timeout_t getTimer(void) const;

    /**
     * This is used to determine how much time has elapsed since a
     * timer port setTimer benchmark time was initially set.  This
     * allows one to use setTimer() to set the timer to the current
     * time and then measure elapsed time from that point forward.
     *
     * return time elapsed in milliseconds, or TIMEOUT_INF if
     * inactive.
     */
    timeout_t getElapsed(void) const;
};

#ifndef _MSWINDOWS_
struct  timespec *getTimeout(struct timespec *spec, timeout_t timeout);
#endif

inline struct tm *localtime_r(const time_t *t, struct tm *b)
    {return SysTime::getLocalTime(t, b);}

inline char *ctime_r(const time_t *t, char *buf)
    {return ctime(t);}

inline struct tm *gmtime_r(const time_t *t, struct tm *b)
    {return SysTime::getGMTTime(t, b);}

inline char *asctime_r(const struct tm *tm, char *b)
    {return asctime(tm);}

inline Thread *getThread(void)
    {return Thread::get();}

/**
 * The buffer class represents an IPC service that is built upon a buffer
 * of fixed capacity that can be used to transfer objects between one or
 * more producer and consumer threads.  Producer threads post objects
 * into the buffer, and consumer threads wait for and receive objects from
 * the buffer.  Semaphores are used to to block the buffer from overflowing
 * and indicate when there is data available, and mutexes are used to protect
 * multiple consumers and producer threads from stepping over each other.
 *
 * The buffer class is an abstract class in that the actual data being
 * buffered is not directly specified within the buffer class itself.  The
 * buffer class should be used as a base class for a class that actually
 * impliments buffering and which may be aware of the data types actually
 * are being buffered.  A template class could be created based on buffer
 * for this purpose.  Another possibility is to create a class derived
 * from both Thread and Buffer which can be used to implement message passing
 * threads.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Producer/Consumer buffer for use between threads.
 */
#ifdef  _MSWINDOWS_
class __EXPORT Buffer : public Mutex
#else
class __EXPORT Buffer : public Conditional
#endif
{
private:
#ifdef  _MSWINDOWS_
    HANDLE  sem_head, sem_tail;
#endif
    size_t _size;
    size_t _used;

protected:
    /**
     * Invoke derived class buffer peeking method.
     * @return size of object found.
     * @param buf pointer to copy contents of head of buffer to.
     */
    virtual size_t onPeek(void *buf) = 0;

    /**
     * Invoke derived class object request from buffer.
     * @return size of object returned.
     * @param buf pointer to hold object returned from the buffer.
     */
    virtual size_t onWait(void *buf) = 0;

    /**
     * Invoke derived class posting of object to buffer.
     * @return size of object posted.
     * @param buf pointer to object being posted to the buffer.
     */
    virtual size_t onPost(void *buf) = 0;

public:
    /**
     * value to return when a timed operation returned with a
     * timeout.
     */
    static const size_t timeout;

    /**
     * Create a buffer object of known capacity.
     * @param capacity is the integer capacity of the buffer.
     */
    Buffer(size_t capacity);
    /**
     * In derived functions, may be used to free the actual memory
     * used to hold buffered data.
     */
    virtual ~Buffer();

    /**
     * Return the capacity of the buffer as specified at creation.
     * @return size of buffer.
     */
    inline size_t getSize(void)
        {return _size;};

    /**
     * Return the current capacity in use for the buffer.  Free space
     * is technically getSize() - getUsed().
     * @return integer used capacity of the buffer.
     * @see #getSize
     */
    inline size_t getUsed(void)
        {return _used;};

    /**
     * Let one or more threads wait for an object to become available
     * in the buffer.  The waiting thread(s) will wait forever if no
     * object is ever placed into the buffer.
     *
     * @return size of object passed by buffer in bytes.
     * @param buf pointer to store object retrieved from the buffer.
     * @param timeout time to wait.
     */
    size_t wait(void *buf, timeout_t timeout = 0);

    /**
     * Post an object into the buffer and enable a waiting thread to
     * receive it.
     *
     * @return size of object posted in bytes.
     * @param buf pointer to object to store in the buffer.
     * @param timeout time to wait.
     */
    size_t post(void *buf, timeout_t timeout = 0);

    /**
     * Peek at the current content (first object) in the buffer.
     *
     * @return size of object in the buffer.
     * @param buf pointer to store object found in the buffer.
     */
    size_t peek(void *buf);

    /**
     * New virtual to test if buffer is a valid object.
     * @return true if object is valid.
     */
    virtual bool isValid(void);
};

/**
 * A buffer class that holds a known capacity of fixed sized objects defined
 * during creation.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short producer/consumer buffer for fixed size objects.
 */
class __EXPORT FixedBuffer : public Buffer
{
private:
    char *buf, *head, *tail;
    size_t objsize;

protected:
    /**
     * Return the first object in the buffer.
     * @return predefined size of this buffers objects.
     * @param buf pointer to copy contents of head of buffer to.
     */
    size_t onPeek(void *buf);

    /**
     * Wait for and return a fixed object in the buffer.
     * @return predefined size of this buffers objects.
     * @param buf pointer to hold object returned from the buffer.
     */
    size_t onWait(void *buf);

    /**
     * Post an object of the appropriate size into the buffer.
     * @return predefined size of this buffers objects.
     * @param buf pointer to data to copy into the buffer.
     */
    size_t onPost(void *buf);

public:
    /**
     * Create a buffer of known capacity for objects of a specified
     * size.
     *
     * @param capacity of the buffer.
     * @param objsize for each object held in the buffer.
     */
    FixedBuffer(size_t capacity, size_t objsize);

    /**
     * Create a copy of an existing fixed size buffer and duplicate
     * it's contents.
     *
     * @param fb existing FixedBuffer object.
     */
    FixedBuffer(const FixedBuffer &fb);

    /**
     * Destroy the fixed buffer and free the memory used to store objects.
     */
    virtual ~FixedBuffer();

    FixedBuffer &operator=(const FixedBuffer &fb);

    bool isValid(void);
};

/**
 * Somewhat generic queue processing class to establish a producer
 * consumer queue.  This may be used to buffer cdr records, or for
 * other purposes where an in-memory queue is needed for rapid
 * posting.  This class is derived from Mutex and maintains a linked
 * list.  A thread is used to dequeue data and pass it to a callback
 * method that is used in place of "run" for each item present on the
 * queue.  The conditional is used to signal the run thread when new
 * data is posted.
 *
 * This class was changed by Angelo Naselli to have a timeout on the queue
 *
 * @short in memory data queue interface.
 * @author David Sugar <dyfet@ostel.com>
 */
class __EXPORT ThreadQueue : public Mutex, public Thread, public Semaphore
{
private:
    void run(void);         // private run method

protected:
    typedef struct _data {
        struct _data *next;
        unsigned len;
        char data[1];
    }   data_t;

    timeout_t timeout;
    bool started;

    data_t *first, *last;       // head/tail of list

    String name;

    /*
     * Overloading of final(). It demarks Semaphore to avoid deadlock.
     */
    virtual void final();

    /**
     * Start of dequeing.  Maybe we need to connect a database
     * or something, so we have a virtual...
     */
    virtual void startQueue(void);

    /**
     * End of dequeing, we expect the queue is empty for now.  Maybe
     * we need to disconnect a database or something, so we have
     * another virtual.
     */
    virtual void stopQueue(void);

    /**
     * A derivable method to call when the timout is expired.
    */
    virtual void onTimer(void);

    /**
     * Virtual callback method to handle processing of a queued
     * data items.  After the item is processed, it is deleted from
     * memory.  We can call multiple instances of runQueue in order
     * if multiple items are waiting.
     *
     * @param data item being dequed.
     */
    virtual void runQueue(void *data) = 0;

public:
    /**
     * Create instance of our queue and give it a process priority.
     *
     * @param id queue ID.
     * @param pri process priority.
     * @param stack stack size.
     */
    ThreadQueue(const char *id, int pri, size_t stack = 0);

    /**
     * Destroy the queue.
     */
    virtual ~ThreadQueue();

    /**
     * Set the queue timeout.
     * When the timer expires, the onTimer() method is called
     * for the thread
     *
     * @param timeout timeout in milliseconds.
     */
    void setTimer(timeout_t timeout);

    /**
     * Put some unspecified data into this queue.  A new qd
     * structure is created and sized to contain a copy of
     * the actual content.
     *
     * @param data pointer to data.
     * @param len size of data.
     */
    void post(const void *data, unsigned len);
};


/** @relates Buffer */
inline size_t get(Buffer &b, void *o, timeout_t t = 0)
    {return b.wait(o, t);}

/** @relates Buffer */
inline size_t put(Buffer &b, void *o, timeout_t t = 0)
    {return b.post(o, t);}

/** @relates Buffer */
inline size_t peek(Buffer &b, void *o)
    {return b.peek(o);}

END_NAMESPACE

#endif
