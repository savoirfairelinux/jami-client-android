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
 * A common object base class with auto-pointer support.
 * A common object class is used which may be referenced counted and
 * associated with a smart auto-pointer class.  A lot of the things
 * found here were inspired by working with Objective-C.  Many of the
 * classes are designed to offer automatic heap management through
 * smart pointers and temporary objects controlled through the scope of
 * the stack frame of method calls.
 * @file ucommon/object.h
 */

#ifndef _UCOMMON_OBJECT_H_
#define _UCOMMON_OBJECT_H_

#ifndef _UCOMMON_CPR_H_
#include <ucommon/cpr.h>
#endif

#ifndef _UCOMMON_GENERICS_H_
#include <ucommon/generics.h>
#endif

#ifndef _UCOMMON_PROTOCOLS_H_
#include <ucommon/protocols.h>
#endif

#include <stdlib.h>

NAMESPACE_UCOMMON

/**
 * A base class for reference counted objects.  Reference counted objects
 * keep track of how many objects refer to them and fall out of scope when
 * they are no longer being referred to.  This can be used to achieve
 * automatic heap management when used in conjunction with smart pointers.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT CountedObject : public ObjectProtocol
{
private:
    volatile unsigned count;

protected:
    /**
     * Construct a counted object, mark initially as unreferenced.
     */
    CountedObject();

    /**
     * Construct a copy of a counted object.  Our instance is not a
     * reference to the original object but a duplicate, so we do not
     * retain the original and we do reset our count to mark as
     * initially unreferenced.
     */
    CountedObject(const ObjectProtocol &ref);

    /**
     * Dealloc object no longer referenced.  The dealloc routine would commonly
     * be used for a self delete to return the object back to a heap when
     * it is no longer referenced.
     */
    virtual void dealloc(void);

    /**
     * Force reset of count.
     */
    inline void reset(void)
        {count = 0;}

public:
    /**
     * Test if the object has copied references.  This means that more than
     * one object has a reference to our object.
     * @return true if referenced by more than one object.
     */
    inline bool is_copied(void)
        {return count > 1;};

    /**
     * Test if the object has been referenced (retained) by anyone yet.
     * @return true if retained.
     */
    inline bool is_retained(void)
        {return count > 0;};

    /**
     * Return the number of active references (retentions) to our object.
     * @return number of references to our object.
     */
    inline unsigned copied(void)
        {return count;};

    /**
     * Increase reference count when retained.
     */
    void retain(void);

    /**
     * Decrease reference count when released.  If no longer retained, then
     * the object is dealloc'd.
     */
    void release(void);
};

/**
 * A general purpose smart pointer helper class.  This is particularly
 * useful in conjunction with reference counted objects which can be
 * managed and automatically removed from the heap when they are no longer
 * being referenced by a smart pointer.  The smart pointer itself would
 * normally be constructed and initialized as an auto variable in a method
 * call, and will dereference the object when the pointer falls out of scope.
 * This is actually a helper class for the typed pointer template.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT auto_object
{
protected:
    ObjectProtocol *object;

    auto_object();

public:
    /**
     * Construct an auto-pointer referencing an existing object.
     * @param object we point to.
     */
    auto_object(ObjectProtocol *object);

    /**
     * Construct an auto-pointer as a copy of another pointer.  The
     * retention of the object being pointed to will be increased.
     * @param pointer we are a copy of.
     */
    auto_object(const auto_object &pointer);

    /**
     * Delete auto pointer.  When it falls out of scope, the retention
     * of the object it references is reduced.  If it falls to zero in
     * a reference counted object, then the object is auto-deleted.
     */
    ~auto_object();

    /**
     * Manually release the pointer.  This reduces the retention level
     * of the object and resets the pointer to point to nobody.
     */
    void release(void);

    /**
     * Test if the pointer is not set.
     * @return true if the pointer is not referencing anything.
     */
    bool operator!() const;

    /**
     * Test if the pointer is referencing an object.
     * @return true if the pointer is currently referencing an object.
     */
    operator bool() const;

    /**
     * test if the object being referenced is the same as the object we specify.
     * @param object we compare to.
     * @return true if this is the object our pointer references.
     */
    bool operator==(ObjectProtocol *object) const;

    /**
     * test if the object being referenced is not the same as the object we specify.
     * @param object we compare to.
     * @return true if this is not the object our pointer references.
     */
    bool operator!=(ObjectProtocol *object) const;

    /**
     * Set our pointer to a specific object.  If the pointer currently
     * references another object, that object is released.  The pointer
     * references our new object and that new object is retained.
     * @param object to assign to.
     */
    void operator=(ObjectProtocol *object);
};

/**
 * A sparse array of managed objects.  This might be used as a simple
 * array class for reference counted objects.  This class assumes that
 * objects in the array exist when assigned, and that gaps in the array
 * are positions that do not reference any object.  Objects are automatically
 * created (create on access/modify when an array position is referenced
 * for the first time.  This is an abstract class because it is a type
 * factory for objects who's derived class form constructor is not known
 * in advance and is a helper class for the sarray template.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT SparseObjects
{
private:
    ObjectProtocol **vector;
    unsigned max;

protected:
    /**
     * Object factory for creating members of the spare array when they
     * are initially requested.
     * @return new object.
     */
    virtual ObjectProtocol *create(void) = 0;

    /**
     * Purge the array by deleting all created objects.
     */
    void purge(void);

    virtual ObjectProtocol *invalid(void) const;

    /**
     * Get (reference) an object at a specified offset in the array.
     * @param offset in array.
     * @return new or existing object.
     */
    ObjectProtocol *get(unsigned offset);

    /**
     * Create a sparse array of known size.  No member objects are
     * created until they are referenced.
     * @param size of array.
     */
    SparseObjects(unsigned size);

    /**
     * Destroy sparse array and delete all generated objects.
     */
    virtual ~SparseObjects();

public:
    /**
     * Get count of array elements.
     * @return array elements.
     */
    unsigned count(void);
};

/**
 * Generate a typed sparse managed object array.  Members in the array
 * are created when they are first referenced.  The types for objects
 * that are generated by sarray must have Object as a base class.  Managed
 * sparse arrays differ from standard arrays in that the member elements
 * are not allocated from the heap when the array is created, but rather
 * as they are needed.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T>
class sarray : public SparseObjects
{
public:
    /**
     * Generate a sparse typed array of specified size.
     * @param size of array to create.
     */
    inline sarray(unsigned size) : SparseObjects(size) {};

    /**
     * Get typed member of array.  If the object does not exist, it is
     * created.
     * @param offset in array for object.
     * @return pointer to typed object.
     */
    inline T *get(unsigned offset)
        {return static_cast<T*>(SparseObjects::get(offset));}

    /**
     * Array operation to access member object.  If the object does not
     * exist, it is created.
     * @param offset in array for object.
     * @return pointer to typed object.
     */
    inline T& operator[](unsigned offset)
        {return get(offset);};

    inline const T* at(unsigned offset)
        {return static_cast<const T&>(SparseObjects::get(offset));}

private:
    __LOCAL ObjectProtocol *create(void)
        {return new T;};
};

/**
 * Template for embedding a data structure into a reference counted object.
 * This is a convenient means to create reference counted heap managed data
 * structure.  This template can be used for embedding data into other kinds
 * of managed object classes in addition to reference counting.  For example,
 * it can be used to embed a data structure into a linked list, as shown in
 * the linked_value template.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <typename T, class O = CountedObject>
class object_value : public O
{
protected:
    /**
     * Assign our value from a typed data object.  This is a helper method.
     * @param object to assign our value from.
     */
    inline void set(const T& object)
        {value = object;};

public:
    T value;    /**< Embedded data value */

    /**
     * Construct composite value object.
     */
    inline object_value() : O() {};

    /**
     * Construct composite value object and assign from existing data value.
     * @param existing typed value to assign.
     */
    inline object_value(T& existing) : O()
        {value = existing;};

    /**
     * Pointer reference to embedded data value.
     * @return embedded value.
     */
    inline T& operator*()
        {return value;};

    /**
     * Assign embedded data value.
     * @param data value to assign.
     */
    inline void operator=(const T& data)
        {value = data;};

    /**
     * Retrieve data value by casting reference.
     * @return embedded value.
     */
    inline operator T&()
        {return value;};

    inline T& operator()()
        {return value;};

    /**
     * Set data value by expression reference.
     * @param data value to assign.
     */
    inline void operator()(T& data)
        {value = data;};
};

/**
 * Typed smart pointer class.  This is used to manage references to
 * a specific typed object on the heap that is derived from the base Object
 * class.  This is most commonly used to manage references to reference
 * counted heap objects so their heap usage can be auto-managed while there
 * is active references to such objects.  Pointers are usually created on
 * the stack frame and used to reference an object during the life of a
 * member function.  They can be created in other objects that live on the
 * heap and can be used to maintain active references so long as the object
 * they are contained in remains in scope as well.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T, class P = auto_object>
class object_pointer : public P
{
public:
    /**
     * Create a pointer with no reference.
     */
    inline object_pointer() : P() {};

    /**
     * Create a pointer with a reference to a heap object.
     * @param object we are referencing.
     */
    inline object_pointer(T* object) : P(object) {};

    /**
     * Reference object we are pointing to through pointer indirection.
     * @return pointer to object we are pointing to.
     */
    inline T* operator*() const
        {return static_cast<T*>(P::object);};

    /**
     * Reference object we are pointing to through function reference.
     * @return object we are pointing to.
     */
    inline T& operator()() const
        {return *(static_cast<T*>(P::object));};

    /**
     * Reference member of object we are pointing to.
     * @return reference to member of pointed object.
     */
    inline T* operator->() const
        {return static_cast<T*>(P::object);};

    /**
     * Get pointer to object.
     * @return pointer or NULL if we are not referencing an object.
     */
    inline T* get(void) const
        {return static_cast<T*>(P::object);};

    /**
     * Iterate our pointer if we reference an array on the heap.
     * @return next object in array.
     */
    inline T* operator++()
        {P::operator++(); return get();};

    /**
     * Iterate our pointer if we reference an array on the heap.
     * @return previous object in array.
     */
    inline void operator--()
        {P::operator--(); return get();};

    /**
     * Perform assignment operator to existing object.
     * @param typed object to assign.
     */
    inline void operator=(T *typed)
        {P::operator=((ObjectProtocol *)typed);};

    /**
     * See if pointer is set.
     */
    inline operator bool()
        {return P::object != NULL;};

    /**
     * See if pointer is not set.
     */
    inline bool operator!()
        {return P::object == NULL;};
};

/**
 * Convenience function to access object retention.
 * @param object we are retaining.
 */
inline void retain(ObjectProtocol *object)
    {object->retain();}

/**
 * Convenience function to access object release.
 * @param object we are releasing.
 */
inline void release(ObjectProtocol *object)
    {object->release();}

/**
 * Convenience function to access object copy.
 * @param object we are copying.
 */
inline ObjectProtocol *copy(ObjectProtocol *object)
    {return object->copy();}

END_NAMESPACE

#endif
