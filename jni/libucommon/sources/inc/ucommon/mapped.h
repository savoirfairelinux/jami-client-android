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
 * Support for memory mapped objects.
 * Memory mapped objects can be used to publish information so that it may be
 * accessible directly by external programs.  The mapped memory objects will
 * usually be built as a vector vector or reusable type factory, in the latter
 * case using the allocated shared memory block itself as a local heap.  A
 * simple template can be used to view the mapped contents that have been
 * published by another process.
 * @file ucommon/mapped.h
 */

#ifndef _UCOMMON_MAPPED_H_
#define _UCOMMON_MAPPED_H_

#ifndef _UCOMMON_LINKED_H_
#include <ucommon/linked.h>
#endif

#ifndef _UCOMMON_THREAD_H_
#include <ucommon/thread.h>
#endif

#ifndef _UCOMMON_STRING_H_
#include <ucommon/string.h>
#endif

#ifndef _MSWINDOWS_
#include <signal.h>
#endif

NAMESPACE_UCOMMON

/**
 * Construct or access a named section of memory.  A logical name is used
 * which might map to something that is invoked from a call like shm_open
 * or a named w32 mapped swap segment.  This is meant to support mapping a
 * vector onto shared memory and is often used as a supporting class for our
 * shared memory access templates.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT MappedMemory
{
private:
    size_t mapsize;
    caddr_t map;
    fd_t fd;

protected:
    size_t size, used;
    char idname[65];
    bool erase;

    MappedMemory();

    /**
     * Supporting function to construct a new or access an existing
     * shared memory segment.  Used by primary constructors.
     * @param name of segment to create or access.
     * @param size of segment if creating new.  Use 0 for read-only access.
     */
    void create(const char *name, size_t size = (size_t)0);

    /**
     * Handler to invoke in derived class when accessing outside the
     * shared memory segment boundary.
     */
    virtual void *invalid(void) const;

    /**
     * Handler for failure to map (allocate) memory.
     */
    virtual void fault(void) const;

public:
    /**
     * Construct a read/write access mapped shared segment of memory of a
     * known size.  This constructs a new memory segment.
     * @param name of segment.
     * @param size of segment.
     */
    MappedMemory(const char *name, size_t size);

    /**
     * Provide read-only mapped access to an existing named shared memory
     * segment.  The size of the map is found by the size of the already
     * existing segment.
     * @param name of existing segment.
     */
    MappedMemory(const char *name);

    /**
     * Unmap memory segment.
     */
    virtual ~MappedMemory();

    /**
     * Unmap memory segment.
     */
    void release(void);

    /**
     * Destroy a previously existing memory segment under the specified name.
     * This is used both before creating a new one, and after a publishing
     * process unmaps the segment it created.
     * @param name of segment to remove.
     */
    static  void remove(const char *name);

    /**
     * Test if map active.
     * @return true if active map.
     */
    inline operator bool() const
        {return (size != 0);};

    /**
     * Test if map is inactive.
     * @return true if map inactive.
     */
    inline bool operator!() const
        {return (size == 0);};

    /**
     * Extend size of managed heap on shared memory segment.  This does not
     * change the size of the mapped segment in any way, only that of any
     * heap space that is being allocated and used from the mapped segment.
     * @return start of space from map.
     * @param size of space requested.  Will fault if past end of segment.
     */
    void *sbrk(size_t size);

    /**
     * Get memory from a specific offset within the mapped memory segment.
     * @param offset from start of segment.  Will fault if past end.
     * @return address of offset.
     */
    void *offset(size_t offset) const;

    /**
     * Copy memory from specific offset within the mapped memory segment.
     * This function assures the copy is not in the middle of being modified.
     * @param offset from start of segment.
     * @param buffer to copy into.
     * @param size of object to copy.
     * @return true on success.
     */
    bool copy(size_t offset, void *buffer, size_t size) const;

    /**
     * Get size of mapped segment.
     * @return size of mapped segment.
     */
    inline size_t len(void)
        {return size;};

    /**
     * Get starting address of mapped segment.
     * @return starting address of mapped segment.
     */
    inline caddr_t addr(void)
        {return map;};

    /**
     * An API that allows "disabling" of publishing shared memory maps.
     * This may be useful when an app doesn't want to use shared memory
     * as a runtime or build option, but does not want to have to be "recoded"
     * explicitly for non-shared memory either.  Basically it substitutes a
     * dummy map running on the local heap.
     */
    static void disable(void);
};

/**
 * Map a reusable allocator over a named shared memory segment.  This may be
 * used to form a resource bound fixed size managed heap in shared memory.
 * The request can either be fulfilled from the object reuse pool or from a
 * new section of memory, and if all memory in the segment has been exhausted,
 * it can wait until more objects are returned by another thread to the reuse
 * pool.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT MappedReuse : protected ReusableAllocator, protected MappedMemory
{
private:
    unsigned objsize;
    unsigned reading;
    mutex_t mutex;

protected:
    MappedReuse(size_t osize);

    inline void create(const char *fname, unsigned count)
        {MappedMemory::create(fname, count * objsize);};

public:
    /**
     * Construct a named memory segment for use with managed fixed size
     * reusable objects.  The segment is created as writable.  There is no
     * read-only version of mapped reuse since the mapped segment can be read
     * by another process directly as a mapped read-only vector.  The actual
     * mapped type will be derived from ReusableObject to meet the needs of
     * the reusable allocator.  The template version should be used to
     * assure type correctness rather than using this class directly.
     * @param name of shared memory segment.
     * @param size of the object type being mapped.
     * @param count of the maximum number of active mapped objects.
     */
    MappedReuse(const char *name, size_t size, unsigned count);

    /**
     * Check whether there are objects available to be allocated.
     * @return true if objects are available.
     */
    bool avail(void);

    /**
     * Request a reusable object from the free list or mapped space.
     * @return free object or NULL if pool is exhausted.
     */
    ReusableObject *request(void);

    /**
     * Request a reusable object from the free list or mapped space.
     * This method blocks until an object becomes available.
     * @return free object.
     */
    ReusableObject *get(void);

    /**
     * Request a reusable object from the free list or mapped space.
     * This method blocks until an object becomes available or the
     * timeout has expired.
     * @param timeout to wait in milliseconds.
     * @return free object or NULL if timeout.
     */
    ReusableObject *getTimed(timeout_t timeout);

    /**
     * Used to get an object from the reuse pool when the mutex lock is
     * already held.
     * @return object from pool or NULL if exhausted.
     */
    ReusableObject *getLocked(void);

    /**
     * Used to return an object to the reuse pool when the mutex lock is
     * already held.
     * @param object being returned.
     */
    void removeLocked(ReusableObject *object);
};

/**
 * Template class to map typed vector into shared memory.  This is used to
 * construct a typed read/write vector of objects that are held in a named
 * shared memory segment.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T>
class mapped_array : public MappedMemory
{
protected:
    inline mapped_array() : MappedMemory() {};

    inline void create(const char *fn, unsigned members)
        {MappedMemory::create(fn, members * sizeof(T));};

public:
    /**
     * Construct mapped vector array of typed objects.  This is constructed
     * for read/write access.  mapped_view is used in all cases for read-only
     * access to mapped data.  Member objects are linearly allocated from
     * the shared memory segment, or may simply be directly accessed by offset.
     * @param name of mapped segment to construct.
     * @param number of objects in the mapped vector.
     */
    inline mapped_array(const char *name, unsigned number) :
        MappedMemory(name, number * sizeof(T)) {};

    /**
     * Initialize typed data in mapped array.  Assumes default constructor
     * for type.
     */
    inline void initialize(void)
        {new((caddr_t)offset(0)) T[size / sizeof(T)];};

    /**
     * Add mapped space while holding lock for one object.
     * @return address of object.
     */
    inline void *addLock(void)
        {return sbrk(sizeof(T));};

    /**
     * Get typed pointer to member object of vector in mapped segment.
     * @param member to access.
     * @return typed pointer or NULL if past end of array.
     */
    inline T *operator()(unsigned member)
        {return static_cast<T*>(offset(member * sizeof(T)));}

    /**
     * Allocate mapped space for one object.
     * @return address of object.
     */
    inline T *operator()(void)
        {return static_cast<T*>(sbrk(sizeof(T)));};

    /**
     * Reference typed object of vector in mapped segment.
     * @param member to access.
     * @return typed reference.
     */
    inline T& operator[](unsigned member)
        {return *(operator()(member));};

    /**
     * Get member size of typed objects that can be held in mapped vector.
     * @return members mapped in segment.
     */
    inline unsigned max(void)
        {return (unsigned)(size / sizeof(T));};
};

/**
 * Template class to map typed reusable objects into shared memory heap.
 * This is used to construct a read/write heap of objects that are held in a
 * named shared memory segment.  Member objects are allocated from a reusable
 * heap but are stored in the shared memory segment as a vector.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T>
class mapped_reuse : public MappedReuse
{
protected:
    inline mapped_reuse() :
        MappedReuse(sizeof(T)) {};

public:
    /**
     * Construct mapped reuse array of typed objects.  This is constructed
     * for read/write access.  mapped_view is used in all cases for read-only
     * access to mapped data.
     * @param name of mapped segment to construct.
     * @param number of objects in the mapped vector.
     */
    inline mapped_reuse(const char *name, unsigned number) :
        MappedReuse(name, sizeof(T), number) {};

    /**
     * Initialize typed data in mapped array.  Assumes default constructor
     * for type.
     */
    inline void initialize(void)
        {new((caddr_t)pos(0)) T[size / sizeof(T)];};

    /**
     * Check whether there are typed objects available to be allocated.
     * @return true if objects are available.
     */
    inline operator bool() const
        {return MappedReuse::avail();};

    /**
     * Check whether there are typed objects available to be allocated.
     * @return true if no more typed objects are available.
     */
    inline bool operator!() const
        {return !MappedReuse::avail();};

    /**
     * Request a typed reusable object from the free list or mapped space.
     * This method blocks until an object becomes available.
     * @return free object.
     */
    inline operator T*()
        {return mapped_reuse::get();};

    /**
     * Request a typed reusable object from the free list or mapped space by
     * pointer reference.  This method blocks until an object becomes available.
     * @return free object.
     */
    inline T* operator*()
        {return mapped_reuse::get();};

    /**
     * Get typed object from a specific member offset within the mapped segment.
     * @param member offset from start of segment.  Will fault if past end.
     * @return typed object pointer.
     */
    inline T *pos(size_t member)
        {return static_cast<T*>(MappedReuse::offset(member * sizeof(T)));};

    /**
     * Request a typed reusable object from the free list or mapped space.
     * This method blocks until an object becomes available.
     * @return free typed object.
     */
    inline T *get(void)
        {return static_cast<T*>(MappedReuse::get());};

    /**
     * Request a typed reusable object from the free list or mapped space.
     * This method blocks until an object becomes available from another
     * thread or the timeout expires.
     * @param timeout in milliseconds.
     * @return free typed object.
     */
    inline T *getTimed(timeout_t timeout)
        {return static_cast<T*>(MappedReuse::getTimed(timeout));};

    /**
     * Request a typed reusable object from the free list or mapped space.
     * This method does not block or wait.
     * @return free typed object if available or NULL.
     */
    inline T *request(void)
        {return static_cast<T*>(MappedReuse::request());};

    /**
     * Used to return a typed object to the reuse pool when the mutex lock is
     * already held.
     * @param object being returned.
     */
    inline void removeLocked(T *object)
        {MappedReuse::removeLocked(object);};

    /**
     * Used to get a typed object from the reuse pool when the mutex lock is
     * already held.
     * @return typed object from pool or NULL if exhausted.
     */
    inline T *getLocked(void)
        {return static_cast<T*>(MappedReuse::getLocked());};

    /**
     * Used to release a typed object back to the reuse typed object pool.
     * @param object being released.
     */
    inline void release(T *object)
        {ReusableAllocator::release(object);};
};

/**
 * Class to access a named mapped segment published from another process.
 * This offers a simple typed vector interface to access the shared memory
 * segment in read-only mode.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T>
class mapped_view : protected MappedMemory
{
public:
    /**
     * Map existing named memory segment.  The size of the map is derived
     * from the existing map alone.
     * @param name of memory segment to map.
     */
    inline mapped_view(const char *name) :
        MappedMemory(name) {};

    /**
     * Access typed member object in the mapped segment.
     * @param member to access.
     * @return typed object pointer.
     */
    inline volatile const T *operator()(unsigned member)
        {return static_cast<const T*>(offset(member * sizeof(T)));}

    /**
     * Reference typed member object in the mapped segment.
     * @param member to access.
     * @return typed object reference.
     */
    inline volatile const T &operator[](unsigned member)
        {return *(operator()(member));};

    inline volatile const T *get(unsigned member)
        {return static_cast<const T*>(offset(member * sizeof(T)));};

    inline void copy(unsigned member, T& buffer)
        {MappedMemory::copy(member * sizeof(T), &buffer, sizeof(T));};

    /**
     * Get count of typed member objects held in this map.
     * @return count of typed member objects.
     */
    inline unsigned count(void)
        {return (unsigned)(size / sizeof(T));};
};

END_NAMESPACE

#endif
