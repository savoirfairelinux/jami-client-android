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

/**
 * @file buffer.h
 * @short object passing services between threads.
 **/

#ifndef CCXX_BUFFER_H_
#define CCXX_BUFFER_H_

#ifndef CCXX_THREAD_H_
#include <cc++/thread.h>
#endif
#ifndef CCXX_STRING_H_
#include <cc++/string.h>
#endif
#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

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
#ifdef  WIN32
class __EXPORT Buffer : public Mutex
#else
class __EXPORT Buffer : public Conditional
#endif
{
private:
#ifdef  WIN32
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


#ifdef  CCXX_NAMESPACES
}
#endif

#endif
/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
