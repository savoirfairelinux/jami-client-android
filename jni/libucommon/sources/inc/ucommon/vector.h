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
 * Basic array and reusable object factory heap support.
 * This offers ucommon support for vector arrays, and for forming
 * reusable object pools.  Reusable object pools can be tied to local
 * heaps and offer a means to create type factories that do not require
 * global locking through malloc.
 * @file ucommon/vector.h
 */

#ifndef _UCOMMON_VECTOR_H_
#define _UCOMMON_VECTOR_H_

#ifndef _UCOMMON_THREAD_H_
#include <ucommon/thread.h>
#endif

typedef unsigned short vectorsize_t;

NAMESPACE_UCOMMON

/**
 * An array of reusable objects.  This class is used to support the
 * array_use template.  A pool of objects are created which can be
 * allocated as needed.  Deallocated objects are returned to the pool
 * so they can be reallocated later.  This is a private fixed size heap.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT ArrayReuse : public ReusableAllocator
{
private:
    size_t objsize;
    unsigned count, limit, used;
    caddr_t mem;

protected:
    ArrayReuse(size_t objsize, unsigned c);
    ArrayReuse(size_t objsize, unsigned c, void *memory);

public:
    /**
     * Destroy reusable private heap array.
     */
    ~ArrayReuse();

protected:
    bool avail(void);

    ReusableObject *get(timeout_t timeout);
    ReusableObject *get(void);
    ReusableObject *request(void);
};

/**
 * A mempager source of reusable objects.  This is used by the reuse_pager
 * template to allocate new objects either from a memory pager used as
 * a private heap, or from previously allocated objects that have been
 * returned for reuse.
  * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT PagerReuse : protected MemoryRedirect, protected ReusableAllocator
{
private:
    unsigned limit, count;
    size_t osize;

protected:
    PagerReuse(mempager *pager, size_t objsize, unsigned count);
    ~PagerReuse();

    bool avail(void);
    ReusableObject *get(void);
    ReusableObject *get(timeout_t timeout);
    ReusableObject *request(void);
};

/**
 * A managed vector for generic object pointers.  This vector is memory
 * managed at runtime by basic cow (copy-on-write) operations of a reference
 * counted object list.  This allows the size of the vector to be changed
 * at runtime and for the vector to be copied by managing reference counted
 * copies of the list of objects as needed.
 *
 * This class is somewhat analogous to the string class, but rather than
 * holding a string "array of chars" that may be re-sized and reallocated,
 * the Vector holds an array of Object pointers.  Since the object pointers
 * we store in the vector are objects inherited from Object, a vector can
 * itself act as a vector of smart pointers to  reference counted objects
 * (derived from CountedObject).
 * @author David Sugar <dyfet@gnutelephony.org>.
 */
class __EXPORT Vector
{
public:
    class __EXPORT array : public CountedObject
    {
    public:
#pragma pack(1)
        vectorsize_t max, len;
        ObjectProtocol *list[1];
#pragma pack()

        array(vectorsize_t size);
        void dealloc(void);
        void set(ObjectProtocol **items);
        void add(ObjectProtocol **list);
        void add(ObjectProtocol *obj);
        void purge(void);
        void inc(vectorsize_t adj);
        void dec(vectorsize_t adj);
    };

protected:
    array *data;

    array *create(vectorsize_t size) const;

    virtual void release(void);
    virtual void cow(vectorsize_t adj = 0);
    ObjectProtocol **list(void) const;

    friend class Vector::array;

protected:
    /**
     * Object handler for index outside vector range.
     * @return default object, often NULL.
     */
    virtual ObjectProtocol *invalid(void) const;

public:
    /**
     * npos is a constant for an "invalid" position value.
     */
    static const vectorsize_t npos;

    /**
     * Create an initially empty vector.
     */
    Vector();

    /**
     * Create a vector of size object pointers.
     * @param size of vector to create.
     */
    Vector(vectorsize_t size);

    /**
     * Create a vector of size objects from existing object pointers.
     * This allocates the vector and initializes the object pointers from
     * an existing array of object pointers.  Either a specific vector
     * size may be used, or the end of the vector will be found by a NULL
     * object pointer.
     * @param items to place into the vector.
     * @param size of the vector to create, or use NULL item for end.
     */
    Vector(ObjectProtocol **items, vectorsize_t size = 0);

    /**
     * Destroy the current reference counted vector of object pointers.
     */
    virtual ~Vector();

    /**
     * Get the size of the vector (number of active members).
     * @return number of active pointers in vector.
     */
    vectorsize_t len(void) const;

    /**
     * Get the effective allocation space used by the vector.  This is the
     * number of pointers it can hold before it needs to be resized.
     * @return storage size of vector.
     */
    vectorsize_t size(void) const;

    /**
     * Get an object pointer from a specified member of the vector.
     * @param index of member pointer to return.  Negative values from end.
     * @return object pointer of member.
     */
    ObjectProtocol *get(int index) const;

    /**
     * Copy the vector to an external pointer array.
     * @param mem array of external pointers to hold vector.
     * @param max size of the external array.
     * @return number of elements copied into external array.
     */
    vectorsize_t get(void **mem, vectorsize_t max) const;

    /**
     * Get the first object pointer contained in the vector.  Typically used
     * in iterations.
     * @return first object pointer.
     */
    ObjectProtocol *begin(void) const;

    /**
     * Get the last object pointer contained in the vector.  Typically used
     * in iterations.
     * @return last object pointer.
     */
    ObjectProtocol *end(void) const;

    /**
     * Find the first instance of a specific pointer in the vector.
     * @param pointer to locate in the vector.
     * @param offset to start searching in vector.
     * @return position of pointer in vector or npos if not found.
     */
    vectorsize_t find(ObjectProtocol *pointer, vectorsize_t offset = 0) const;

    /**
     * Split the vector at a specified offset.  All members after the split
     * are de-referenced and dropped from the vector.
     * @param position to split vector at.
     */
    void split(vectorsize_t position);

    /**
     * Split the vector after a specified offset.  All members before the split
     * are de-referenced and dropped.  The member starting at the split point
     * becomes the first member of the vector.
     * @param position to split vector at.
     */
    void rsplit(vectorsize_t position);

    /**
     * Set a member of the vector to an object.  If an existing member was
     * present and is being replaced, it is de-referenced.
     * @param position in vector to place object pointer.
     * @param pointer to place in vector.
     */
    void set(vectorsize_t position, ObjectProtocol *pointer);

    /**
     * Set the vector to a list of objects terminated by a NULL pointer.
     * @param list of object pointers.
     */
    void set(ObjectProtocol **list);

    /**
     * Add (append) a NULL terminated list of objects to the vector.
     * @param list of object pointers to add.
     */
    void add(ObjectProtocol **list);

    /**
     * Add (append) a single object pointer to the vector.
     * @param pointer to add to vector.
     */
    void add(ObjectProtocol *pointer);

    /**
     * De-reference and remove all pointers from the vector.
     */
    void clear(void);

    /**
     * Re-size & re-allocate the total (allocated) size of the vector.
     * @param size to allocate for vector.
     */
    virtual bool resize(vectorsize_t size);

    /**
     * Set (duplicate) an existing vector into our vector.
     * @param vector to duplicate.
     */
    inline void set(Vector &vector)
        {set(vector.list());};

    /**
     * Add (append) an existing vector to our vector.
     * @param vector to append.
     */
    inline void add(Vector &vector)
        {add(vector.list());};

    /**
     * Return a pointer from the vector by array reference.
     * @param index of vector member pointer to return.
     */
    inline ObjectProtocol *operator[](int index)
        {return get(index);};

    /**
     * Assign a member of the vector directly.
     * @param position to assign.
     * @param pointer to object to assign to vector.
     */
    inline void operator()(vectorsize_t position, ObjectProtocol *pointer)
        {set(position, pointer);};

    /**
     * Retrieve a member of the vector directly.
     * @param position to retrieve object from.
     * @return object pointer retrieved from vector.
     */
    inline ObjectProtocol *operator()(vectorsize_t position)
        {return get(position);};

    /**
     * Append a member to the vector directly.
     * @param pointer to object to add to vector.
     */
    inline void operator()(ObjectProtocol *pointer)
        {add(pointer);};

    /**
     * Assign (copy) into our existing vector from another vector.
     * @param vector to assign from.
     */
    inline void operator=(Vector &vector)
        {set(vector.list());};

    /**
     * Append into our existing vector from another vector.
     * @param vector to append from.
     */
    inline void operator+=(Vector &vector)
        {add(vector.list());};

    /**
     * Concatenate into our existing vector from assignment list.
     * @param vector to append from.
     */
    inline Vector& operator+(Vector &vector)
        {add(vector.list()); return *this;};

    /**
     * Release vector and concat vector from another vector.
     * @param vector to assign from.
     */
    Vector &operator^(Vector &vector);

    /**
     * Release our existing vector and duplicate from another vector.  This
     * differs from assign in that the allocated size of the vector is reset
     * to the new list.
     * @param vector to assign from.
     */
    void operator^=(Vector &vector);

    /**
     * Drop first member of vector.
     */
    void operator++();

    /**
     * Drop last member of the vector.
     */
    void operator--();

    /**
     * Drop first specified members from the vector.
     * @param count of members to drop.
     */
    void operator+=(vectorsize_t count);

    /**
     * Drop last specified members from the vector.
     * @param count of members to drop.
     */
    void operator-=(vectorsize_t count);

    /**
     * Compute the effective vector size of a list of object pointers.
     * The size is found as the NULL pointer in the list.
     * @return size of list.
     */
    static vectorsize_t size(void **list);
};

/**
 * Vector with fixed size member list.  This is analogous to the memstring
 * class and is used to tie a vector to a fixed list in memory.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT MemVector : public Vector
{
private:
    bool resize(vectorsize_t size);
    void cow(vectorsize_t adj = 0);
    void release(void);

    friend class Vector::array;

public:
    /**
     * Create and manage a vector stored in fixed memory.
     * @param pointer to where our vector list lives.
     * @param size of vector list in memory.
     */
    MemVector(void *pointer, vectorsize_t size);

    /**
     * Destroy the vector.
     */
    ~MemVector();

    /**
     * Assign an existing vector into our fixed vector list.
     * @param vector to copy from.
     */
    inline void operator=(Vector &vector)
        {set(vector);};

};

/**
 * A templated vector for a list of a specific Object subtype.  The
 * templated type must be derived from Object.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<class T>
class vectorof : public Vector
{
public:
    /**
     * Create an empty vector for specified type.
     */
    inline vectorof() : Vector() {};

    /**
     * Create an empty vector of allocated size for specified type.
     * @param size of vector to allocate.
     */
    inline vectorof(vectorsize_t size) : Vector(size) {};

    inline T& operator[](int index)
        {return static_cast<T&>(Vector::get(index));};

    inline const T& at(int index)
        {return static_cast<const T&>(Vector::get(index));};

    /**
     * Retrieve a typed member of the vector directly.
     * @param position to retrieve object from.
     * @return typed object pointer retrieved from vector.
     */
    inline T *operator()(vectorsize_t position)
        {return static_cast<T *>(Vector::get(position));};

    /**
     * Get the first typed object pointer contained in the vector.
     * @return first typed object pointer.
     */
    inline T *begin(void)
        {return static_cast<T *>(Vector::begin());};

    /**
     * Get the last typed object pointer contained in the vector.
     * @return last typed object pointer.
     */
    inline T *end(void)
        {return static_cast<T *>(Vector::end());};

    /**
     * Concatenate typed vector in an expression.
     * @param vector to concatenate.
     * @return effective object to continue in expression.
     */
    inline Vector &operator+(Vector &vector)
        {Vector::add(vector); return static_cast<Vector &>(*this);};
};

/**
 * An array of reusable types.  A pool of typed objects is created which can
 * be allocated as needed.  Deallocated typed objects are returned to the pool
 * so they can be reallocated later.  This is a private fixed size heap.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<class T>
class array_reuse : protected ArrayReuse
{
public:
    /**
     * Create private heap of reusable objects of specified type.
     * @param count of objects of specified type to allocate.
     */
    inline array_reuse(unsigned count) :
        ArrayReuse(sizeof(T), count) {};

    /**
     * Create reusable objects of specific type in preallocated memory.
     * @param count of objects of specified type in memory.
     * @param memory to use.
     */
    inline array_reuse(unsigned count, void *memory) :
        ArrayReuse(sizeof(T), count, memory) {};

    /**
     * Test if typed objects available in heap or re-use list.
     * @return true if objects still are available.
     */
    inline operator bool() const
        {return avail();};

    /**
     * Test if the entire heap has been allocated.
     * @return true if no objects are available.
     */
    inline bool operator!() const
        {return !avail();};

    /**
     * Request immediately next available typed object from the heap.
     * @return typed object pointer or NULL if heap is empty.
     */
    inline T* request(void)
        {return static_cast<T*>(ArrayReuse::request());};

    /**
     * Get a typed object from the heap.  This function blocks when the
     * heap is empty until an object is returned to the heap.
     * @return typed object pointer from heap.
     */
    inline T* get(void)
        {return static_cast<T*>(ArrayReuse::get());};

    /**
     * Create a typed object from the heap.  This function blocks when the
     * heap is empty until an object is returned to the heap.
     * @return typed object pointer from heap.
     */
    inline T* create(void)
        {return init<T>(static_cast<T*>(ArrayReuse::get()));};

    /**
     * Get a typed object from the heap.  This function blocks until the
     * the heap has an object to return or the timer has expired.
     * @param timeout to wait for heap in milliseconds.
     * @return typed object pointer from heap or NULL if timeout.
     */
    inline T* get(timeout_t timeout)
        {return static_cast<T*>(ArrayReuse::get(timeout));};

    /**
     * Create a typed object from the heap.  This function blocks until the
     * the heap has an object to return or the timer has expired.
     * @param timeout to wait for heap in milliseconds.
     * @return typed object pointer from heap or NULL if timeout.
     */
    inline T* create(timeout_t timeout)
        {return init<T>(static_cast<T*>(ArrayReuse::get(timeout)));};

    /**
     * Release (return) a typed object back to the heap for re-use.
     * @param object to return.
     */
    inline void release(T *object)
        {ArrayReuse::release(object);};

    /**
     * Get a typed object from the heap by type casting reference.  This
     * function blocks while the heap is empty.
     * @return typed object pointer from heap.
     */
    inline operator T*()
        {return array_reuse::get();};

    /**
     * Get a typed object from the heap by pointer reference.  This
     * function blocks while the heap is empty.
     * @return typed object pointer from heap.
     */
    inline T *operator*()
        {return array_reuse::get();};
};

/**
 * A reusable private pool of reusable types.  A pool of typed objects is
 * created which can be allocated from a memory pager.  Deallocated typed
 * objects are also returned to this pool so they can be reallocated later.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T>
class paged_reuse : protected PagerReuse
{
public:
    /**
     * Create a managed reusable typed object pool.  This manages a heap of
     * typed objects that can either be reused from released objects or
     * allocate from an existing memory pager pool.
     * @param pager pool to allocate from.
     * @param count of objects of specified type to allocate.
     */
    inline paged_reuse(mempager *pager, unsigned count) :
        PagerReuse(pager, sizeof(T), count) {};

    /**
     * Test if typed objects available from the pager or re-use list.
     * @return true if objects still are available.
     */
    inline operator bool() const
        {return PagerReuse::avail();};

    /**
     * Test if no objects are available for reuse or the pager.
     * @return true if no objects are available.
     */
    inline bool operator!() const
        {return !PagerReuse::avail();};

    /**
     * Get a typed object from the pager heap.  This function blocks when the
     * heap is empty until an object is returned to the heap.
     * @return typed object pointer from heap.
     */
    inline T *get(void)
        {return static_cast<T*>(PagerReuse::get());};

    /**
     * Get a typed object from the pager heap.  This function blocks when the
     * heap is empty until an object is returned to the heap.  The objects
     * default constructor is used.
     * @return typed object pointer from heap.
     */
    inline T *create(void)
        {return init<T>(static_cast<T*>(PagerReuse::get()));};

    /**
     * Get a typed object from the heap.  This function blocks until the
     * the heap has an object to return or the timer has expired.
     * @param timeout to wait for heap in milliseconds.
     * @return typed object pointer from heap or NULL if timeout.
     */
    inline T *get(timeout_t timeout)
        {return static_cast<T*>(PagerReuse::get(timeout));};

    /**
     * Create a typed object from the heap.  This function blocks until the
     * the heap has an object to return or the timer has expired.  The
     * objects default constructor is used.
     * @param timeout to wait for heap in milliseconds.
     * @return typed object pointer from heap or NULL if timeout.
     */
    inline T *create(timeout_t timeout)
        {return init<T>(static_cast<T*>(PagerReuse::get(timeout)));};

    /**
     * Request immediately next available typed object from the pager heap.
     * @return typed object pointer or NULL if heap is empty.
     */
    inline T *request(void)
        {return static_cast<T*>(PagerReuse::request());};

    /**
     * Release (return) a typed object back to the pager heap for re-use.
     * @param object to return.
     */
    inline void release(T *object)
        {PagerReuse::release(object);};

    /**
     * Get a typed object from the pager heap by type casting reference.  This
     * function blocks while the heap is empty.
     * @return typed object pointer from heap.
     */
    inline T *operator*()
        {return paged_reuse::get();};

    /**
     * Get a typed object from the pager heap by pointer reference.  This
     * function blocks while the heap is empty.
     * @return typed object pointer from heap.
     */
    inline operator T*()
        {return paged_reuse::get();};
};

/**
 * Allocated vector list of a specified type.  This analogous to the stringbuf
 * class and allows one to create a vector with it's member list as a single
 * object that can live in the heap or that can be created at once and used as
 * a auto variable.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<typename T, vectorsize_t S>
class vectorbuf : public MemVector
{
private:
    char buffer[sizeof(array) + (S * sizeof(void *))];

public:
    /**
     * Construct fixed sized vector object in heap or stack.
     */
    inline vectorbuf() : MemVector(buffer, S) {};

    /**
     * Get object pointer of specified type from fixed vector.
     * @param index of typed member to return, < 0 to use from end of list.
     * @return typed object pointer of member.
     */
    inline const T& at(int index)
        {return static_cast<const T&>(Vector::get(index));};

    inline T& operator[](int index)
        {return static_cast<T&>(Vector::get(index));};

    /**
     * Retrieve a typed member of the fixed vector directly.
     * @param position to retrieve object from.
     * @return typed object pointer retrieved from vector.
     */
    inline T *operator()(vectorsize_t position)
        {return static_cast<T *>(Vector::get(position));};

    /**
     * Get the first typed object pointer contained in the fixed vector.
     * @return first typed object pointer.
     */
    inline T *begin(void)
        {return static_cast<T *>(Vector::begin());};

    /**
     * Get the last typed object pointer contained in the fixed vector.
     * @return last typed object pointer.
     */
    inline T *end(void)
        {return static_cast<T *>(Vector::end());};

    /**
     * Concatenate fixed typed vector in an expression.
     * @param vector to concatenate.
     * @return effective object to continue in expression.
     */
    inline Vector &operator+(Vector &vector)
        {Vector::add(vector); return static_cast<Vector &>(*this);};
};

END_NAMESPACE

#endif
